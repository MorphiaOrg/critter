package dev.morphia.critter.java

import com.mongodb.lang.Nullable
import dev.morphia.aggregation.experimental.codecs.ExpressionHelper
import dev.morphia.annotations.Id
import dev.morphia.annotations.LoadOnly
import dev.morphia.annotations.NotSaved
import dev.morphia.critter.CritterField
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.nameCase
import dev.morphia.mapping.codec.pojo.MorphiaCodec
import org.bson.BsonWriter
import org.bson.codecs.EncoderContext
import org.bson.codecs.IdGenerator
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.io.File

class JavaCodecBuilder(private val context: JavaContext) : SourceBuilder {
    private lateinit var source: JavaClass
    private lateinit var encoder: JavaClassSource

    override fun build() {
        context.classes.values.forEach { source ->
            this.source = source
            encoder = Roaster.create(JavaClassSource::class.java)
                .setPackage("dev.morphia.mapping.codec.pojo")
                .setName(source.name + "Encoder")
                .setFinal(true)
            val outputFile = File(context.outputDirectory, encoder.qualifiedName.replace('.', '/') + ".java")
            val sourceTimestamp = source.lastModified()
            val timestamp = outputFile.lastModified()
            if (!source.isAbstract() && context.shouldGenerate(sourceTimestamp, timestamp)) {
                encoder.superType = "dev.morphia.mapping.codec.pojo.EntityEncoder<${source.name}>"

                imports()
                buildConstructor()
                encoderClassMethod()
                encodeMethod()
                idGenerator();

                encoder.write(outputFile)
            }
        }
    }
    private fun imports() {
        encoder.addImport(source.qualifiedName)
        val document = encoder.addImport("dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document")
        document.isStatic = true;
        encoder.addImport(Nullable::class.java)
        encoder.addImport(IdGenerator::class.java)
    }

    private fun encodeMethod() {
        val method = encoder.addMethod()
            .setName("encode")
            .setPublic()
        method.addAnnotation(Override::class.java)
        method.addParameter(BsonWriter::class.java, "writer")
        method.addParameter(source.name, "instance")
        method.addParameter(EncoderContext::class.java, "encoderContext")
        method.body = """
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
    }

    private fun outputProperties(): String {
        val lines = mutableListOf<String>()
        lines += "EntityModel model = getMorphiaCodec().getEntityModel();"
        lines += "encodeIdProperty(writer, instance, encoderContext);"
        lines += """
            if (model.useDiscriminator()) {
                writer.writeString(model.getDiscriminatorKey(), model.getDiscriminator());
            }
        """.trimIndent()
        source.fields.forEach { field ->
            if (!(field.hasAnnotation(Id::class.java)
                    || field.hasAnnotation(LoadOnly::class.java)
                    || field.hasAnnotation(NotSaved::class.java))) {
                lines += """
                    encodeValue(writer, encoderContext, model.getProperty("${field.name}"), instance.${getter(field)});
                """.trimIndent()
            }
        }
        val s = """                 
                            for (PropertyModel propertyModel : model.getProperties()) {
                                if (propertyModel.equals(idModel)) {
                                    continue;
                                }
                                encodeValue(writer, encoderContext, propertyModel, propertyModel.getAccessor().get(value));
                            }
        """
        return lines.joinToString("\n")
    }

    fun encodeId() {
        """
        protected void encodeIdProperty(BsonWriter writer, Object instance, EncoderContext encoderContext, @Nullable PropertyModel idModel) {
            if (idModel != null) {
                IdGenerator generator = getIdGenerator();
                if (generator == null) {
                    encodeValue(writer, encoderContext, idModel, idModel.getAccessor().get(instance));
                } else {
                    Object id = idModel.getAccessor().get(instance);
                    if (id == null && encoderContext.isEncodingCollectibleDocument()) {
                        id = generator.generate();
                        idModel.getAccessor().set(instance, id);
                    }
                    encodeValue(writer, encoderContext, idModel, id);
                }
            }
        }
        """.trimIndent()
    }

    private fun getter(field: CritterField): String {
        val name = field.name
        val ending = name.nameCase() + "()"
        val methodName = if (field.type.equals("boolean", true)) "is${ending}" else "get${ending}"

//        source.
        return methodName
    }

    private fun idGenerator() {
        encoder.addMethod("""
                @Nullable
    protected IdGenerator getIdGenerator() {
        if (idGenerator == null) {
            PropertyModel idModel = morphiaCodec.getEntityModel().getIdProperty();
            if (idModel != null && idModel.getNormalizedType().isAssignableFrom(ObjectId.class)) {
                idGenerator = OBJECT_ID_GENERATOR;
            }
        }

        return idGenerator;
    }
        """.trimIndent())

    }
    private fun encoderClassMethod() {
        val method = encoder.addMethod()
            .setName("getEncoderClass")
            .setPublic()
            .setBody("return ${source.name}.class;")
            .setReturnType(Class::class.java)
        method.addAnnotation(Override::class.java)
    }

    private fun buildConstructor() {
        encoder.addImport(MorphiaCodec::class.java)
        val ctor = encoder.addMethod()
            .setConstructor(true)
            .setPublic()
            .setBody("super(codec);")
        ctor.addParameter(MorphiaCodec::class.java, "codec")
    }

}

private fun JavaClassSource.write(outputFile: File) {
    outputFile.parentFile.mkdirs()
    outputFile.writeText(toString())
}
