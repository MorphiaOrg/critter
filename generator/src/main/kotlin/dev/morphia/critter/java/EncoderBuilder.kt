package dev.morphia.critter.java

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import dev.morphia.aggregation.experimental.codecs.ExpressionHelper
import dev.morphia.annotations.Id
import dev.morphia.annotations.LoadOnly
import dev.morphia.annotations.NotSaved
import dev.morphia.critter.CritterField
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.java.CodecsBuilder.Companion.packageName
import dev.morphia.critter.nameCase
import dev.morphia.mapping.codec.pojo.EntityEncoder
import dev.morphia.mapping.codec.pojo.MorphiaCodec
import dev.morphia.mapping.codec.pojo.PropertyModel
import org.bson.BsonWriter
import org.bson.codecs.EncoderContext
import org.bson.codecs.IdGenerator
import java.io.File
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC

class EncoderBuilder(private val context: JavaContext) : SourceBuilder {
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
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)

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

        JavaFile
            .builder(packageName, encoder.build())
            .addStaticImport(ExpressionHelper::class.java, "document")
            .build()
            .writeTo(context.outputDirectory)
    }

    private fun encodeMethod() {
        encoder.addMethod(
            MethodSpec.methodBuilder("encode")
                .addModifiers(PUBLIC)
                .addAnnotation(Override::class.java)
                .addParameter(BsonWriter::class.java, "writer")
                .addParameter(ParameterSpec.builder(entityName, "instance").build())
                .addParameter(EncoderContext::class.java, "encoderContext")
                .addCode(
                    """
                if (areEquivalentTypes(instance.getClass(), ${source.name}.class)) {
                    document(writer, () -> {
                        ${outputProperties()}
                    });
                } else {
                    getMorphiaCodec().getRegistry()
                                .get((Class) instance.getClass())
                                .encode(writer, instance, encoderContext);
                }
                """.trimIndent()
                )
                .build()
        )
    }

    private fun outputProperties(): String {
        var indent = "        ".repeat(3)
        indent = ""
        val lines = mutableListOf<String>()
        lines += "EntityModel model = getMorphiaCodec().getEntityModel();"
        lines += "encodeId(writer, instance, encoderContext);"
        lines += """
            if (model.useDiscriminator()) {
                writer.writeString(model.getDiscriminatorKey(), model.getDiscriminator());
            }
        """.trimIndent()
        source.fields.forEach { field ->
            if (!(field.hasAnnotation(Id::class.java)
                    || field.hasAnnotation(LoadOnly::class.java)
                    || field.hasAnnotation(NotSaved::class.java))
            ) {
                lines += "encodeValue(writer, encoderContext, model.getProperty(\"${field.name}\"), instance.${getter(field)});"
            }
        }
        return lines.joinToString("\n", transform = { s -> indent + s })
    }

    fun encodeId() {
        val method = MethodSpec.methodBuilder("encodeId")
            .addModifiers(PROTECTED)
            .addParameter(BsonWriter::class.java, "writer")
            .addParameter(ParameterSpec.builder(entityName, "instance").build())
            .addParameter(EncoderContext::class.java, "encoderContext")
        idProperty()?.let {
            val idType = ClassName.get(it.type.substringBeforeLast('.'), it.type.substringAfterLast('.'))
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
        }

        encoder.addMethod(method.build())
    }

    private fun idProperty(): CritterField? {
        return source.fields
            .filter { it.hasAnnotation(Id::class.java) }
            .firstOrNull()
    }

    private fun getter(field: CritterField): String {
        val name = field.name
        val ending = name.nameCase() + "()"
        val methodName = if (field.type.equals("boolean", true)) "is${ending}" else "get${ending}"

        return methodName
    }

    private fun setter(field: CritterField): String {
        return "set${field.name.nameCase()}"
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