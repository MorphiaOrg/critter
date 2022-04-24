package dev.morphia.critter.kotlin

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
import java.io.File

@OptIn(DelicateKotlinPoetApi::class)
class DecoderBuilder(private val context: KotlinContext) : SourceBuilder {
    private lateinit var source: KotlinClass
    private lateinit var decoder: TypeSpec.Builder
    private lateinit var decoderName: ClassName
    private lateinit var entityName: ClassName
    override fun build() {
        context.entities().values.forEach { source ->
            this.source = source
            entityName = ClassName.bestGuess(source.qualifiedName)
            decoderName = ClassName("dev.morphia.mapping.codec.pojo", "${source.name}Decoder")
            decoder = TypeSpec.classBuilder(decoderName)
                .addModifiers(PUBLIC, FINAL)
            val sourceTimestamp = source.lastModified()
            val decoderFile = File(context.outputDirectory, decoderName.canonicalName.replace('.', '/') + ".java")

            if (!source.isAbstract() && context.shouldGenerate(sourceTimestamp, decoderFile.lastModified())) {
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
            .addStatement("var codec = getMorphiaCodec()")
            .addStatement("var mapper = codec.getMapper()")
            .addStatement("var document = codec.getRegistry().get(%T::class.java).decode(reader, decoderContext)",
                Document::class.java
            )
            .addStatement("var instanceCreator = ${entityName.simpleName.titleCase()}InstanceCreator()")
            .addStatement("var instance = instanceCreator.getInstance()")
        source.functions(PreLoad::class.java).forEach {
            val params = it.parameterNames().joinToString(", ", prefix = "(", postfix = ")")
            function.addStatement("instance.${it.name}${params}\n")
        }
        function.beginControlFlow("for (ei in mapper.getInterceptors())")
        function.addStatement("ei.preLoad(instance, document, mapper)")
        function.endControlFlow()

        function
            .addStatement(
                "decodeProperties(%T(document), decoderContext, instanceCreator, model)",
                DocumentReader::class.java
            )
        source.functions(PostLoad::class.java).forEach {
            val params = it.parameterNames().joinToString(", ", prefix = "(", postfix = ")")
            function.addStatement("instance.${it.name}${params}\n")
        }
        function.beginControlFlow("for (ei in mapper.getInterceptors())")
        function.addStatement("ei.postLoad(instance, document, mapper)")
        function.endControlFlow()

        function.addStatement("return instanceCreator.getInstance()")
        decoder.addFunction(function.build())
    }

    private fun decodeMethod() {
        val eventMethods = source.functions(PreLoad::class.java) + source.functions(PostLoad::class.java)
        val method = FunSpec.builder("decode")
            .addModifiers(PUBLIC, OVERRIDE)
            .addParameter("reader", BsonReader::class.java)
            .addParameter("decoderContext", DecoderContext::class.java)
            .returns(entityName)
            .addStatement("var model = getMorphiaCodec().getEntityModel()")


        method.beginControlFlow("if (decoderContext.hasCheckedDiscriminator())")
        if (eventMethods.isNotEmpty()) {
            method.addStatement("return lifecycle(reader, model, decoderContext)")
        } else {
            method.beginControlFlow("if (getMorphiaCodec().getMapper().hasInterceptors())")
                .addStatement("return lifecycle(reader, model, decoderContext)")
                .nextControlFlow(" else ")
                .addStatement("var instanceCreator = ${entityName.simpleName.titleCase()}InstanceCreator()")
                .addStatement("decodeProperties(reader, decoderContext, instanceCreator, model)")
                .addStatement("return instanceCreator.getInstance()")
                .endControlFlow()
        }
        method.nextControlFlow("else")
            .addStatement("var morphiaCodec = getMorphiaCodec()")
            .addStatement(
                """return getCodecFromDocument(reader, model.useDiscriminator(), model.getDiscriminatorKey(),
                                morphiaCodec.getRegistry(), morphiaCodec.getDiscriminatorLookup(), morphiaCodec)
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