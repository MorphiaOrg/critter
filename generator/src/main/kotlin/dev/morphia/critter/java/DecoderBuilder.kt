package dev.morphia.critter.java

import com.mongodb.lang.NonNull
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.MethodSpec.methodBuilder
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import dev.morphia.annotations.PostLoad
import dev.morphia.annotations.PreLoad
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.titleCase
import dev.morphia.mapping.codec.MorphiaInstanceCreator
import dev.morphia.mapping.codec.pojo.EntityDecoder
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.MorphiaCodec
import dev.morphia.mapping.codec.reader.DocumentReader
import org.bson.BsonReader
import org.bson.Document
import org.bson.codecs.DecoderContext
import java.io.File
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC

class DecoderBuilder(private val context: JavaContext) : SourceBuilder {
    private lateinit var source: JavaClass
    private lateinit var decoder: TypeSpec.Builder
    private lateinit var decoderName: ClassName
    private lateinit var entityName: ClassName
    override fun build() {
        context.entities().values.forEach { source ->
            this.source = source
            entityName = ClassName.get(source.pkgName, source.name)
            decoderName = ClassName.get("dev.morphia.mapping.codec.pojo", "${source.name}Decoder")
            decoder = TypeSpec.classBuilder(decoderName)
                .addModifiers(PUBLIC, FINAL)
            val sourceTimestamp = source.lastModified()
            val decoderFile = File(context.outputDirectory, decoderName.canonicalName().replace('.', '/') + ".java")

            decoder.addAnnotation(
                AnnotationSpec.builder(SuppressWarnings::class.java)
                    .addMember("value", "\"unchecked\"")
                    .build()
            )

            if (!source.isAbstract() && context.shouldGenerate(sourceTimestamp, decoderFile.lastModified())) {
                decoder.superclass(ParameterizedTypeName.get(ClassName.get(EntityDecoder::class.java), entityName))
                buildConstructor()
                decodeMethod()
                getInstanceCreator()
                lifecycle()
                context.buildFile(decoder.build())
            }
        }
    }

    private fun lifecycle() {
        val method = methodBuilder("lifecycle")
            .addModifiers(PRIVATE)
            .addParameter(BsonReader::class.java, "reader")
            .addParameter(EntityModel::class.java, "model")
            .addParameter(DecoderContext::class.java, "decoderContext")
            .returns(entityName)
            .addStatement("var codec = getMorphiaCodec()")
            .addStatement("var mapper = codec.getMapper()")
            .addStatement("var document = codec.getRegistry().get(\$T.class).decode(reader, decoderContext)",
                Document::class.java
            )
            .addStatement("var instanceCreator = new ${entityName.simpleName().titleCase()}InstanceCreator()")
            .addStatement("var instance = instanceCreator.getInstance()")
        source.methods(PreLoad::class.java).forEach {
            val params = it.parameterNames().joinToString(", ", prefix = "(", postfix = ")")
            method.addStatement("instance.${it.name}${params}\n")
        }
        method.beginControlFlow("for (var ei : mapper.getInterceptors())")
        method.addStatement("ei.preLoad(instance, document, mapper)")
        method.endControlFlow()

        method
            .addStatement(
                "decodeProperties(new \$T(document), decoderContext, instanceCreator, model)",
                DocumentReader::class.java
            )
        source.methods(PostLoad::class.java).forEach {
            val params = it.parameterNames().joinToString(", ", prefix = "(", postfix = ")")
            method.addStatement("instance.${it.name}${params}\n")
        }
        method.beginControlFlow("for (var ei : mapper.getInterceptors())")
        method.addStatement("ei.postLoad(instance, document, mapper)")
        method.endControlFlow()

        method.addStatement("return instanceCreator.getInstance()")
        decoder.addMethod(method.build())
    }

    private fun decodeMethod() {
        val eventMethods = source.methods(PreLoad::class.java) + source.methods(PostLoad::class.java)
        val method = methodBuilder("decode")
            .addModifiers(PUBLIC)
            .addAnnotation(NonNull::class.java)
            .addAnnotation(Override::class.java)
            .addParameter(
                ParameterSpec.builder(BsonReader::class.java, "reader")
                    .addAnnotation(NonNull::class.java)
                    .build()
            )
            .addParameter(DecoderContext::class.java, "decoderContext")
            .returns(entityName)
            .addStatement("var model = getMorphiaCodec().getEntityModel()")


        method.beginControlFlow("if (decoderContext.hasCheckedDiscriminator())")
        if (eventMethods.isNotEmpty()) {
            method.addStatement("return lifecycle(reader, model, decoderContext)")
        } else {
            method.beginControlFlow("if (getMorphiaCodec().getMapper().hasInterceptors())")
                .addStatement("return lifecycle(reader, model, decoderContext)")
                .nextControlFlow(" else ")
                .addStatement("var instanceCreator = new ${entityName.simpleName().titleCase()}InstanceCreator()")
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

        decoder.addMethod(method.build())
    }

    private fun getInstanceCreator() {
        decoder.addMethod(
            methodBuilder("getInstanceCreator")
                .addModifiers(PROTECTED)
                .addAnnotation(NonNull::class.java)
                .addAnnotation(Override::class.java)
                .returns(MorphiaInstanceCreator::class.java)
                .addStatement("return  new ${entityName.simpleName().titleCase()}InstanceCreator()")
                .build()
        )
    }

    private fun buildConstructor() {
        decoder.addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addStatement("super(codec)")
                .addParameter(MorphiaCodec::class.java, "codec")
                .build()
        )
    }
}