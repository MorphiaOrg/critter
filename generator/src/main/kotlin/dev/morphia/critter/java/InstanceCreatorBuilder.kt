package dev.morphia.critter.java

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.MethodSpec.methodBuilder
import com.squareup.javapoet.TypeSpec
import dev.morphia.annotations.experimental.Name
import dev.morphia.critter.SourceBuilder
import dev.morphia.mapping.codec.Conversions
import dev.morphia.mapping.codec.MorphiaInstanceCreator
import dev.morphia.mapping.codec.pojo.MorphiaCodec
import dev.morphia.mapping.codec.pojo.PropertyModel
import org.jboss.forge.roaster.model.Parameter
import java.io.File
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC

class InstanceCreatorBuilder(val context: JavaContext) : SourceBuilder {
    private lateinit var source: JavaClass
    private lateinit var creator: TypeSpec.Builder
    private lateinit var creatorName: ClassName
    private lateinit var entityName: ClassName
    override fun build() {
        context.classes.values.forEach { source ->
            this.source = source
            entityName = ClassName.get(source.pkgName, source.name)
            creatorName = ClassName.get("dev.morphia.mapping.codec.pojo", "${source.name}InstanceCreator")
            creator = TypeSpec.classBuilder(creatorName)
                .addModifiers(PUBLIC)
            val sourceTimestamp = source.lastModified()
            val decoderFile = File(context.outputDirectory, creatorName.canonicalName().replace('.', '/') + ".java")

            if (!source.isAbstract() && context.shouldGenerate(sourceTimestamp, decoderFile.lastModified())) {
                creator.addSuperinterface(ClassName.get(MorphiaInstanceCreator::class.java))
                fields()
                buildConstructor()
                getInstance()
                set()

                context.buildFile(creator.build(), Conversions::class.java to "convert")
            }
        }
    }

    private fun fields() {
        source.properties.forEach {
            creator.addField(FieldSpec.builder(it.type.name.className(), it.name, PRIVATE).build())
        }
    }

    private fun getInstance() {
        creator.addField(FieldSpec.builder(entityName, "instance", PRIVATE).build())
        creator.addMethod(
            methodBuilder("getInstance")
                .addModifiers(PUBLIC)
                .addCode(
                    """
                    if (instance == null) {
                        ${createAndPopulate()}
                    }
                    
                    return instance;
                """.trimIndent()
                )
                .returns(entityName)
                .build()
        )
    }

    private fun createAndPopulate(): String {
        val fields = source.properties.map { it.name }
            .toMutableList()
        val ctor = source.constructors
            .sortedBy { it.parameters.size }
            .reversed()
            .find {
                it.parameters
                    .map { it.name() in fields }
                    .all { it }
            }
        val params = ctor?.parameters?.map { it.name() } ?: emptyList()
        fields.removeAll(params)
        var body = """
            instance = new ${entityName.simpleName()}(${params.joinToString()});
        """.trimIndent()
        fields.forEach {
            body += "\ninstance.set${it.nameCase()}(${it});"
        }
        return body
    }

    private fun Parameter<*>.name(): String {
        return (getAnnotation(Name::class.java) as Name?)?.value ?: getName()
    }

    private fun set() {
        val method = methodBuilder("set")
            .addModifiers(PUBLIC)
            .addParameter(Object::class.java, "value")
            .addParameter(PropertyModel::class.java, "model")

        source.properties.forEachIndexed { index, field ->
            val ifStmt = "if (\"${field.name}\".equals(model.getName()))"
            if (index == 0) {
                method.beginControlFlow(ifStmt)
            } else {
                method.nextControlFlow("else $ifStmt")
            }
            method.addStatement("${field.name} = (\$T)value", field.type.name.className())
        }
        method.endControlFlow()


        creator.addMethod(method.build())
    }

    private fun buildConstructor() {
        creator.addField(FieldSpec.builder(MorphiaCodec::class.java, "codec", PRIVATE).build())
        creator.addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(MorphiaCodec::class.java, "codec")
                .addCode(
                    """
                    this.codec = codec;
                """.trimIndent()
                )
                .build()
        )
    }
}