package dev.morphia.critter.java

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.MethodSpec.methodBuilder
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import dev.morphia.annotations.Id
import dev.morphia.annotations.LoadOnly
import dev.morphia.annotations.NotSaved
import dev.morphia.critter.CritterProperty
import dev.morphia.critter.SourceBuilder
import dev.morphia.mapping.codec.Conversions
import dev.morphia.mapping.codec.pojo.EntityDecoder
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.MorphiaCodec
import dev.morphia.mapping.codec.pojo.PropertyModel
import org.bson.BsonInvalidOperationException
import org.bson.BsonReader
import org.bson.BsonReaderMark
import org.bson.BsonType
import org.bson.codecs.DecoderContext
import java.io.File
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC

class DecoderBuilder(private val context: JavaContext) : SourceBuilder {
    private lateinit var source: JavaClass
    private lateinit var decoder: TypeSpec.Builder
    private lateinit var decoderName: ClassName
    private lateinit var entityName: ClassName
    override fun build() {
        context.classes.values.forEach { source ->
            this.source = source
            entityName = ClassName.get(source.pkgName, source.name)
            decoderName = ClassName.get("dev.morphia.mapping.codec.pojo", "${source.name}Decoder")
            decoder = TypeSpec.classBuilder(decoderName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            val sourceTimestamp = source.lastModified()
            val decoderFile = File(context.outputDirectory, decoderName.canonicalName().replace('.', '/') + ".java")

            if (!source.isAbstract() && context.shouldGenerate(sourceTimestamp, decoderFile.lastModified())) {
                decoder.superclass(ParameterizedTypeName.get(ClassName.get(EntityDecoder::class.java), entityName))
                buildConstructor()
                decodeMethod()
                decodeProperties()
                decodeModel()

                context.buildFile(decoder.build(), Conversions::class.java to "convert")
            }
        }
    }

    private fun decodeMethod() {
        decoder.addMethod(
            methodBuilder("decode")
                .addModifiers(PUBLIC)
                .addAnnotation(Override::class.java)
                .addParameter(BsonReader::class.java, "reader")
                .addParameter(DecoderContext::class.java, "decoderContext")
                .returns(entityName)
                .addCode(
                    """
                        EntityModel classModel = getMorphiaCodec().getEntityModel();
                        if (!decoderContext.hasCheckedDiscriminator()) {
                            MorphiaCodec<${entityName.simpleName()}> morphiaCodec = getMorphiaCodec();
                            return getCodecFromDocument(reader, classModel.useDiscriminator(), classModel.getDiscriminatorKey(),
                                morphiaCodec.getRegistry(), morphiaCodec.getDiscriminatorLookup(), morphiaCodec)
                                     .decode(reader, DecoderContext.builder().checkedDiscriminator(true).build());
                        }
                        return decodeProperties(reader, decoderContext, classModel);
                    """.trimIndent()
                )
                .build()
        )
    }

    private fun decodeProperties() {
        fun ifLadder(): String {
            var body = "" //""switch(model.getName()) {\n"
            source.properties.forEachIndexed { index, it ->
                if(index != 0) {
                    body += " else "
                }
                body += """
                    if("${it.name}".equals(model.getName())) ${it.name} = decodeModel(reader, decoderContext, model);
                """.trimIndent()
            }
//            body += "}"
            return body
        }

        val method = methodBuilder("decodeProperties")
            .addModifiers(PROTECTED)
            .addParameter(BsonReader::class.java, "reader")
            .addParameter(DecoderContext::class.java, "decoderContext")
            .addParameter(EntityModel::class.java, "classModel")
            .returns(entityName)

        source.properties.forEach {
            method.addStatement("\$T ${it.name}", it.type.name.className());
        }
        method.addCode(
            """
                
        ${entityName.simpleName()} entity = null;
        reader.readStartDocument();
        while (reader.readBsonType() != ${"$"}T.END_OF_DOCUMENT) {
            String _docFieldName = reader.readName();
            if (classModel.useDiscriminator() && classModel.getDiscriminatorKey().equals(_docFieldName)) {
                reader.readString();
            } else {
                PropertyModel model = classModel.getProperty(_docFieldName);
                ${ifLadder()}
            }
        }
        reader.readEndDocument();

        return entity;
        """.trimIndent(), BsonType::class.java
        )

        decoder.addMethod(method.build())
    }

    private fun decodeModel() {
        val method = methodBuilder("decodeModel")
            .addModifiers(PROTECTED)
            .addTypeVariable(TypeVariableName.get("T"))
            .returns(TypeVariableName.get("T"))
            .addParameter(BsonReader::class.java, "reader")
            .addParameter(DecoderContext::class.java, "decoderContext")
            .addParameter(PropertyModel::class.java, "model")
            .addCode(
                """
        if (model != null) {
            final ${"$"}T mark = reader.getMark();
            Object value = null;
            try {
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    reader.readNull();
                } else {
                    value = decoderContext.decodeWithChildContext(model.getCachedCodec(), reader);
                }
            } catch (${"$"}T e) {
                mark.reset();
                value = getMorphiaCodec().getMapper().getCodecRegistry().get(Object.class).decode(reader, decoderContext);
                value = convert(value, model.getTypeData().getType());
            }
            return (T)value;
        } else {
            reader.skipValue();
            return null;
        }
        """.trimIndent(), BsonReaderMark::class.java, BsonInvalidOperationException::class.java
            )

        decoder.addMethod(method.build())
    }

    private fun outputProperties(): String {
        var indent = "        ".repeat(3)
        indent = ""
        val lines = mutableListOf<String>()
        lines += "EntityModel model = getMorphiaCodec().getEntityModel();"
        lines += "decodeId(writer, instance, decoderContext);"
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
                lines += "decodeValue(writer, decoderContext, model.getProperty(\"${field.name}\"), instance.${getter(field)});"
            }
        }
        return lines.joinToString("\n", transform = { s -> indent + s })
    }

    private fun idProperty(): CritterProperty? {
        return source.properties
            .filter { it.hasAnnotation(Id::class.java) }
            .firstOrNull()
    }

    private fun getter(property: CritterProperty): String {
        val name = property.name
        val ending = name.nameCase() + "()"
        val methodName = if (property.type.name.equals("boolean", true)) "is${ending}" else "get${ending}"

        return methodName
    }

    private fun setter(property: CritterProperty): String {
        return "set${property.name.nameCase()}"
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