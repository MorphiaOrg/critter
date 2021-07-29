package dev.morphia.critter.java

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec.methodBuilder
import com.squareup.javapoet.TypeSpec
import dev.morphia.critter.SourceBuilder
import dev.morphia.mapping.codec.Conversions
import dev.morphia.mapping.codec.MorphiaInstanceCreator
import dev.morphia.mapping.codec.pojo.PropertyModel
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
        val ctor = source.bestConstructor()
        val params = ctor?.parameters?.map { it.name } ?: emptyList()
        val properties = source.properties
            .toMutableList()
        properties.removeIf { it.name in params }
        val method = methodBuilder("getInstance")
            .addModifiers(PUBLIC)
            .returns(entityName)
            .beginControlFlow("if (instance == null)")
            .addStatement("instance = new ${entityName.simpleName()}(${params.joinToString()})")
        properties.forEach { property ->
            property.mutator?.let {
                method.addStatement("instance.${it.name}(${property.name})")
            }
        }
        method.endControlFlow()

        creator.addMethod(method.addStatement("return instance").build())
    }

    private fun set() {
        val method = methodBuilder("set")
            .addModifiers(PUBLIC)
            .addParameter(Object::class.java, "value")
            .addParameter(PropertyModel::class.java, "model")

        source.properties.forEachIndexed { index, property ->
            val ifStmt = "if (\"${property.name}\".equals(model.getName()))"
            if (index == 0) {
                method.beginControlFlow(ifStmt)
            } else {
                method.nextControlFlow("else $ifStmt")
            }
            method.addStatement("${property.name} = (\$T)value", property.type.name.className())
            property.mutator?.let {
                method.beginControlFlow("if(instance != null)")
                method.addStatement("instance.${it.name}(${property.name})")
                method.endControlFlow()
            }
        }
        method.endControlFlow()


        creator.addMethod(method.build())
    }
}