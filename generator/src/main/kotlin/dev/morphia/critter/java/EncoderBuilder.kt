@file:Suppress("DEPRECATION")

package dev.morphia.critter.java

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import dev.morphia.EntityInterceptor
import dev.morphia.aggregation.experimental.codecs.ExpressionHelper
import dev.morphia.annotations.Id
import dev.morphia.annotations.LoadOnly
import dev.morphia.annotations.NotSaved
import dev.morphia.annotations.PostPersist
import dev.morphia.annotations.PrePersist
import dev.morphia.critter.CritterProperty
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.nameCase
import dev.morphia.mapping.codec.pojo.EntityEncoder
import dev.morphia.mapping.codec.pojo.MorphiaCodec
import dev.morphia.mapping.codec.pojo.PropertyModel
import dev.morphia.mapping.codec.writer.DocumentWriter
import org.bson.BsonWriter
import org.bson.Document
import org.bson.codecs.EncoderContext
import org.bson.codecs.IdGenerator
import java.io.File
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC

class EncoderBuilder(val context: JavaContext) : SourceBuilder {
    private lateinit var source: JavaClass
    private lateinit var encoder: TypeSpec.Builder
    private lateinit var encoderName: ClassName
    private lateinit var entityName: ClassName
    override fun build() {
        context.classes.values.forEach { source ->
            this.source = source
            entityName = ClassName.get(source.pkgName, source.name)
            encoderName = ClassName.get("dev.morphia.mapping.codec.pojo", "${source.name}Encoder")
            encoder = TypeSpec.classBuilder(encoderName)
                .addModifiers(PUBLIC, FINAL)
            val sourceTimestamp = source.lastModified()
            val encoderFile = File(context.outputDirectory, encoderName.canonicalName().replace('.', '/') + ".java")

            if (!source.isAbstract() && context.shouldGenerate(sourceTimestamp, encoderFile.lastModified())) {
                buildEncoder()
            }
        }
    }

    private fun buildEncoder() {
        encoder.superclass(ParameterizedTypeName.get(ClassName.get(EntityEncoder::class.java), entityName))

        buildConstructor()
        encoderClassMethod()
        encodeMethod()
        encodeId()

        context.buildFile(encoder.build(), ExpressionHelper::class.java to "document")
    }

    private fun encodeMethod() {
        val builder = MethodSpec.methodBuilder("encode")
            .addModifiers(PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(BsonWriter::class.java, "writer")
            .addParameter(ParameterSpec.builder(entityName, "instance").build())
            .addParameter(EncoderContext::class.java, "encoderContext")
        builder.beginControlFlow("if (areEquivalentTypes(instance.getClass(), \$T.class))", source.qualifiedName.className())
        val eventMethods = source.methods(PrePersist::class.java) + source.methods(PostPersist::class.java)
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
        builder.addStatement("getMorphiaCodec().getRegistry().get((Class) instance.getClass()).encode(writer, instance, encoderContext)")
        builder.endControlFlow()

        encoder.addMethod(builder.build())
        encodeProperties()
        lifecycle()
    }

    private fun encodeProperties() {
        encoder.addMethod(
            MethodSpec.methodBuilder("encodeProperties")
                .addModifiers(PRIVATE)
                .addParameter(BsonWriter::class.java, "writer")
                .addParameter(ParameterSpec.builder(entityName, "instance").build())
                .addParameter(EncoderContext::class.java, "encoderContext")
                .addCode(
                    """
                             document(writer, () -> {
                                        ${outputProperties()}
                                    });
                        """.trimIndent()
                ).build()
        )
    }

    private fun lifecycle() {
        val builder = MethodSpec.methodBuilder("lifecycle")
            .addModifiers(PRIVATE)
            .addParameter(BsonWriter::class.java, "writer")
            .addParameter(ParameterSpec.builder(entityName, "instance").build())
            .addParameter(EncoderContext::class.java, "encoderContext")
        builder.addStatement("var codec = getMorphiaCodec()")
        builder.addStatement("var model = codec.getEntityModel()")
        builder.addStatement("var mapper = codec.getMapper()")
        builder.addStatement("var document = new \$T()", Document::class.java)
        builder.addCode("// call PrePersist methods\n")
        source.methods(PrePersist::class.java).forEach {
            val params = it.parameterNames().joinToString(", ", prefix = "(", postfix = ")")
            builder.addStatement("instance.${it.name}${params}\n")
        }
        builder.beginControlFlow("for (\$T ei : mapper.getInterceptors())", EntityInterceptor::class.java)
        builder.addStatement("ei.prePersist(instance, document, mapper)")
        builder.endControlFlow()
        builder.addStatement("var documentWriter = new \$T(mapper, document)", DocumentWriter::class.java)
        builder.addStatement("encodeProperties(documentWriter, instance, encoderContext)")
        builder.addStatement("document = documentWriter.getDocument()")
        builder.addCode("// call PostPersist methods\n")
        source.methods(PostPersist::class.java).forEach {
            val params = it.parameterNames().joinToString(", ", prefix = "(", postfix = ")")
            builder.addStatement("instance.${it.name}${params}\n")
        }

        builder.beginControlFlow("for (\$T ei : mapper.getInterceptors())", EntityInterceptor::class.java)
        builder.addStatement("ei.postPersist(instance, document, mapper)")
        builder.endControlFlow()
        builder.addStatement("codec.getRegistry().get(Document.class).encode(writer, document, encoderContext)")


        //          ;

        //        ;

        encoder.addMethod(builder.build())
    }

    private fun outputProperties(): String {
        val lines = mutableListOf<String>()
        lines += "var model = getMorphiaCodec().getEntityModel();"
        if (idProperty() != null) {
            lines += "encodeId(writer, instance, encoderContext);"
        }
        lines += """
            if (model.useDiscriminator()) {
                writer.writeString(model.getDiscriminatorKey(), model.getDiscriminator());
            }
        """.trimIndent()
        source.properties.forEach { field ->
            if (!(field.hasAnnotation(Id::class.java)
                    || field.hasAnnotation(LoadOnly::class.java)
                    || field.hasAnnotation(NotSaved::class.java))
            ) {
                lines += "encodeValue(writer, encoderContext, model.getProperty(\"${field.name}\"), instance.${getter(field)});"
            }
        }
        return lines.joinToString("\n")
    }

    private fun encodeId() {
        idProperty()?.let {
            val method = MethodSpec.methodBuilder("encodeId")
                .addModifiers(PROTECTED)
                .addParameter(BsonWriter::class.java, "writer")
                .addParameter(ParameterSpec.builder(entityName, "instance").build())
                .addParameter(EncoderContext::class.java, "encoderContext")
            val idType = ClassName.get(it.type.name.substringBeforeLast('.'), it.type.name.substringAfterLast('.'))
            method.addCode(
                """
                Object id = instance.${getter(it)};
                if (id == null && encoderContext.isEncodingCollectibleDocument()) {
                    ${"$"}T generator = getIdGenerator();
                    if (generator != null) {
                        instance.${setter(it)}((${"$"}T)generator.generate());
                    }
                }
                ${"$"}T idModel = getMorphiaCodec().getEntityModel().getIdProperty();
                encodeValue(writer, encoderContext, idModel, instance.${getter(it)});
                """.trimIndent(), IdGenerator::class.java, idType, PropertyModel::class.java
            )
            encoder.addMethod(method.build())
        }
    }

    private fun idProperty(): CritterProperty? {
        return source.properties.firstOrNull { it.hasAnnotation(Id::class.java) }
    }
    private fun getter(property: CritterProperty): String {
        val name = property.name
        val ending = name.nameCase() + "()"

        return if (property.type.name.equals("boolean", true)) "is${ending}" else "get${ending}"
    }

    private fun setter(property: CritterProperty): String {
        return "set${property.name.nameCase()}"
    }

    private fun encoderClassMethod() {
        encoder.addMethod(
            MethodSpec.methodBuilder("getEncoderClass")
                .addModifiers(PUBLIC)
                .addStatement("return ${source.name}.class")
                .returns(Class::class.java)
                .addAnnotation(Override::class.java)
                .build()
        )
    }

    private fun buildConstructor() {
        encoder.addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addStatement("super(codec)")
                .addParameter(MorphiaCodec::class.java, "codec")
                .build()
        )
    }
}