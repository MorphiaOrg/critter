package dev.morphia.critter.kotlin

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.FINAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.PROTECTED
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import dev.morphia.annotations.PostLoad
import dev.morphia.annotations.PreLoad
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.kotlin.extensions.className
import dev.morphia.critter.kotlin.extensions.functions
import dev.morphia.critter.titleCase
import dev.morphia.mapping.codec.Conversions
import dev.morphia.mapping.codec.MorphiaInstanceCreator
import dev.morphia.mapping.codec.pojo.EntityDecoder
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.MorphiaCodec
import dev.morphia.mapping.codec.reader.DocumentReader
import org.bson.BsonReader
import org.bson.Document
import org.bson.codecs.DecoderContext

@OptIn(DelicateKotlinPoetApi::class)
class DecoderBuilder(private val context: KotlinContext) : SourceBuilder {
    private lateinit var source: KSClassDeclaration
    private lateinit var decoder: TypeSpec.Builder
    private lateinit var decoderName: ClassName
    private lateinit var entityName: ClassName
    override fun build() {
        context.entities().values.forEach { source ->
            this.source = source
            entityName = ClassName.bestGuess(source.className())
            decoderName = ClassName("dev.morphia.mapping.codec.pojo", "${source.name()}Decoder")
            decoder = TypeSpec.classBuilder(decoderName)
                .addModifiers(PUBLIC, FINAL)

            if (!source.isAbstract()) {
                decoder.superclass(EntityDecoder::class.java.asClassName()
                    .parameterizedBy(entityName))
                buildConstructor()
                decodeMethod()
                getInstanceCreator()
                lifecycle()
                context.buildFile(decoder.build(), Conversions::class.java to "convert")
            }
        }
    }

    private fun lifecycle() {
        val function = FunSpec.builder("lifecycle")
            .addModifiers(PRIVATE)
            .addParameter("reader", BsonReader::class.java)
            .addParameter("model", EntityModel::class.java)
            .addParameter("decoderContext", DecoderContext::class.java)
            .returns(entityName)
            .addStatement("val codec = morphiaCodec")
            .addStatement("val mapper = codec.mapper")
            .addStatement("val document = codec.registry[%T::class.java].decode(reader, decoderContext)",
                Document::class.java
            )
            .addStatement("val instanceCreator = ${entityName.simpleName.titleCase()}InstanceCreator()")
            .addStatement("val instance = instanceCreator.instance")
        source.functions(PreLoad::class.java).forEach {
            val params = it.parameters.joinToString(", ", prefix = "(", postfix = ")")
            function.addStatement("instance.${it.name()}${params}\n")
        }
        function.beginControlFlow("for (ei in mapper.interceptors)")
        function.addStatement("ei.preLoad(instance, document, codec.datastore)")
        function.endControlFlow()

        function
            .addStatement(
                "decodeProperties(%T(document), decoderContext, instanceCreator, model)",
                DocumentReader::class.java
            )
        source.functions(PostLoad::class.java).forEach {
            val params = it.parameters.joinToString(", ", prefix = "(", postfix = ")")
            function.addStatement("instance.${it.name()}${params}\n")
        }
        function.beginControlFlow("for (ei in mapper.interceptors)")
        function.addStatement("ei.postLoad(instance, document, codec.datastore)")
        function.endControlFlow()

        function.addStatement("return instanceCreator.instance")
        decoder.addFunction(function.build())
    }

    private fun decodeMethod() {
        val eventMethods = source.functions(PreLoad::class.java) + source.functions(PostLoad::class.java)
        val method = FunSpec.builder("decode")
            .addModifiers(PUBLIC, OVERRIDE)
            .addParameter("reader", BsonReader::class.java)
            .addParameter("decoderContext", DecoderContext::class.java)
            .returns(entityName)
            .addStatement("val model = morphiaCodec.entityModel")


        method.beginControlFlow("if (decoderContext.hasCheckedDiscriminator())")
        if (eventMethods.isNotEmpty()) {
            method.addStatement("return lifecycle(reader, model, decoderContext)")
        } else {
            method.beginControlFlow("if (morphiaCodec.mapper.hasInterceptors())")
                .addStatement("return lifecycle(reader, model, decoderContext)")
                .nextControlFlow(" else ")
                .addStatement("val instanceCreator = ${entityName.simpleName.titleCase()}InstanceCreator()")
                .addStatement("decodeProperties(reader, decoderContext, instanceCreator, model)")
                .addStatement("return instanceCreator.instance")
                .endControlFlow()
        }
        method.nextControlFlow("else")
            .addStatement(
                """return getCodecFromDocument(reader, model.useDiscriminator(), model.discriminatorKey,
                                morphiaCodec.registry, morphiaCodec.discriminatorLookup, morphiaCodec)
                                     .decode(reader, DecoderContext.builder().checkedDiscriminator(true).build())""")
            .endControlFlow()

        decoder.addFunction(method.build())
    }

    private fun getInstanceCreator() {
        decoder.addFunction(
            FunSpec.builder("getInstanceCreator")
                .addModifiers(PROTECTED, OVERRIDE)
                .returns(MorphiaInstanceCreator::class.java)
                .addStatement("return  ${entityName.simpleName.titleCase()}InstanceCreator()")
                .build()
        )
    }

    private fun buildConstructor() {
        decoder.primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("codec", MorphiaCodec::class.java.asClassName().parameterizedBy(entityName))
                .build()
        )
            .addSuperclassConstructorParameter("codec")

    }

}

