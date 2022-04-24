@file:Suppress("DEPRECATION")

package dev.morphia.critter.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.FINAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.PROTECTED
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import dev.morphia.EntityInterceptor
import dev.morphia.aggregation.codecs.ExpressionHelper
import dev.morphia.annotations.Id
import dev.morphia.annotations.LoadOnly
import dev.morphia.annotations.NotSaved
import dev.morphia.annotations.PostPersist
import dev.morphia.annotations.PrePersist
import dev.morphia.critter.CritterProperty
import dev.morphia.critter.SourceBuilder
import dev.morphia.mapping.codec.pojo.EntityEncoder
import dev.morphia.mapping.codec.pojo.MorphiaCodec
import dev.morphia.mapping.codec.writer.DocumentWriter
import org.bson.BsonWriter
import org.bson.Document
import org.bson.codecs.Codec
import org.bson.codecs.EncoderContext
import java.io.File

@OptIn(DelicateKotlinPoetApi::class)
class EncoderBuilder(val context: KotlinContext) : SourceBuilder {
    private lateinit var source: KotlinClass
    private lateinit var encoder: TypeSpec.Builder
    private lateinit var encoderName: ClassName
    private lateinit var entityName: ClassName
    override fun build() {
        context.entities().values.forEach { source ->
            this.source = source

            entityName = ClassName.bestGuess(source.qualifiedName)
            encoderName = ClassName("dev.morphia.mapping.codec.pojo", "${source.name}Encoder")
            encoder = TypeSpec.classBuilder(encoderName)
                .addModifiers(PUBLIC, FINAL)
            val sourceTimestamp = source.lastModified()
            val encoderFile = File(context.outputDirectory, encoderName.canonicalName.replace('.', '/') + ".java")

            if (!source.isAbstract() && context.shouldGenerate(sourceTimestamp, encoderFile.lastModified())) {
                buildEncoder()
            }
        }
    }

    private fun buildEncoder() {
        encoder.superclass(
            ClassName.bestGuess(EntityEncoder::class.java.name)
                .parameterizedBy(entityName)
        )
        encoder.addAnnotation(AnnotationSpec.builder(Suppress::class)
            .addMember("\"UNCHECKED_CAST\"")
            .build())
        buildConstructor()
        encoderClassMethod()
        encodeMethod()
        encodeProperties()
        lifecycle()
        encodeId()

        context.buildFile(encoder.build(), ExpressionHelper::class.java to "document")
    }

    private fun encodeMethod() {
        val builder = FunSpec.builder("encode")
            .addModifiers(OVERRIDE)
            .addParameter("writer", BsonWriter::class.java)
            .addParameter(ParameterSpec.builder("instance", entityName).build())
            .addParameter("encoderContext", EncoderContext::class.java)
        builder.beginControlFlow("if (areEquivalentTypes(instance::class.java, %T::class.java))", source.qualifiedName.className())
        val eventMethods = source.functions(PrePersist::class.java) + source.functions(PostPersist::class.java)
        if (eventMethods.isNotEmpty()) {
            builder.addStatement("lifecycle(writer, instance, encoderContext)")
        } else {
            builder.beginControlFlow("if (getMorphiaCodec().getMapper().hasInterceptors())")
            builder.addStatement("lifecycle(writer, instance, encoderContext)")
            builder.nextControlFlow(" else ")
            builder.addStatement("encodeProperties(writer, instance, encoderContext)")
            builder.endControlFlow()
        }
        builder.nextControlFlow(" else ")
        builder.addStatement("val codec = getMorphiaCodec().getRegistry().get(instance::class.java) as %T<%T>", Codec::class, entityName)
        builder.addStatement("codec.encode(writer, instance, encoderContext)")
        builder.endControlFlow()

        encoder.addFunction(builder.build())
    }

    private fun encodeProperties() {

        encoder.addFunction(
            FunSpec.builder("encodeProperties")
                .addModifiers(PRIVATE)
                .addParameter("writer", BsonWriter::class.java)
                .addParameter(ParameterSpec.builder("instance", entityName).build())
                .addParameter("encoderContext", EncoderContext::class.java)
                .addCode(
                    """
                    document(writer, {
                        ${outputProperties()}
                    })
                    """.trimIndent()
                ).build()
        )
    }

    private fun lifecycle() {
        val builder = FunSpec.builder("lifecycle")
            .addModifiers(PRIVATE)
            .addParameter("writer", BsonWriter::class.java)
            .addParameter(ParameterSpec.builder("instance", entityName).build())
            .addParameter("encoderContext", EncoderContext::class.java)
        builder.addStatement("var codec = getMorphiaCodec()")
        builder.addStatement("var mapper = codec.getMapper()")
        builder.addStatement("var document = %T()", Document::class.java)
        builder.addCode("// call PrePersist methods\n")
        source.functions(PrePersist::class.java).forEach {
            val params = it.parameterNames().joinToString(", ", prefix = "(", postfix = ")")
            builder.addStatement("instance.${it.name}${params}\n")
        }
        builder.beginControlFlow("mapper.getInterceptors().forEach", EntityInterceptor::class.java)
        builder.addStatement("it.prePersist(instance, document, mapper)")
        builder.endControlFlow()
        builder.addStatement("var documentWriter = %T(mapper, document)", DocumentWriter::class.java)
        builder.addStatement("encodeProperties(documentWriter, instance, encoderContext)")
        builder.addStatement("document = documentWriter.getDocument()")
        builder.addCode("// call PostPersist methods\n")
        source.functions(PostPersist::class.java).forEach {
            val params = it.parameterNames().joinToString(", ", prefix = "(", postfix = ")")
            builder.addStatement("instance.${it.name}${params}\n")
        }

        builder.beginControlFlow("mapper.getInterceptors().forEach", EntityInterceptor::class.java)
        builder.addStatement("it.postPersist(instance, document, mapper)")
        builder.endControlFlow()
        builder.addStatement("codec.getRegistry().get(Document::class.java).encode(writer, document, encoderContext)")
        encoder.addFunction(builder.build())
    }

    private fun outputProperties(): String {
        val lines = mutableListOf<String>()
        lines += "var model = getMorphiaCodec().getEntityModel()"
        if (idProperty() != null) {
            lines += "encodeId(writer, instance, encoderContext)"
        }
        lines += """
            if (model.useDiscriminator()) {
                writer.writeString(model.getDiscriminatorKey(), model.getDiscriminator())
            }
        """.trimIndent()
        source.properties.forEach { field ->
            if (!(field.hasAnnotation(Id::class.java)
                    || field.hasAnnotation(LoadOnly::class.java)
                    || field.hasAnnotation(NotSaved::class.java))
            ) {
                lines += "encodeValue(writer, encoderContext, model.getProperty(\"${field.name}\")!!, instance.${field.name})"
            }
        }
        return lines.joinToString("\n")
    }

    fun encodeId() {
        idProperty()?.let {
            val method = FunSpec.builder("encodeId")
                .addModifiers(PROTECTED)
                .addParameter("writer", BsonWriter::class.java)
                .addParameter(ParameterSpec.builder("instance", entityName).build())
                .addParameter("encoderContext", EncoderContext::class.java)
            val idType = ClassName.bestGuess(it.type.name)
            method.addCode(
                """
                val id: %T? = instance.${it.name}
                if (id == null && encoderContext.isEncodingCollectibleDocument()) {
                    instance.${it.name} = getIdGenerator()?.generate() as %T
                }
                val idModel = getMorphiaCodec().getEntityModel().getIdProperty()!!
                encodeValue(writer, encoderContext, idModel, instance.${it.name})
                """.trimIndent(), idType, idType)
            encoder.addFunction(method.build())
        }
    }

    private fun idProperty(): CritterProperty? {
        return source.properties
            .filter { it.hasAnnotation(Id::class.java) }
            .firstOrNull()
    }

    private fun encoderClassMethod() {
        encoder.addFunction(
            FunSpec.builder("getEncoderClass")
                .addModifiers(OVERRIDE)
                .addStatement("return ${source.name}::class.java")
                .returns(Class::class.java.asClassName().parameterizedBy(entityName))
                .build()
        )
    }

    private fun buildConstructor() {
        encoder.primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("codec", MorphiaCodec::class.java.asClassName().parameterizedBy(entityName))
                .build()
        )
            .addSuperclassConstructorParameter("codec")
    }
}