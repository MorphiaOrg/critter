package dev.morphia.critter.java

import com.mongodb.lang.NonNull
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec.methodBuilder
import com.squareup.javapoet.TypeSpec
import dev.morphia.critter.SourceBuilder
import dev.morphia.mapping.codec.MorphiaInstanceCreator
import dev.morphia.mapping.codec.pojo.PropertyModel
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC

class InstanceCreatorBuilder(val context: JavaContext) : SourceBuilder {
    private lateinit var source: CritterType
    private lateinit var creator: TypeSpec.Builder
    private lateinit var creatorName: ClassName
    private lateinit var entityName: ClassName
    override fun build() {
        context.entities().values
            .filter { !it.isAbstract() }
            .forEach { source ->
            this.source = source
            entityName = ClassName.get(source.`package`, source.name)
            creatorName = ClassName.get("dev.morphia.mapping.codec.pojo", "${source.name}InstanceCreator")
            creator = TypeSpec.classBuilder(creatorName)
                .addModifiers(PUBLIC)

            creator.addAnnotation(
                AnnotationSpec.builder(SuppressWarnings::class.java)
                    .addMember("value", CodeBlock.of("""{"rawtypes", "unchecked"}"""))
                    .build()
            )

            if (!source.isAbstract()) {
                creator.addSuperinterface(ClassName.get(MorphiaInstanceCreator::class.java))
                fields()
                getInstance()
                set()

                context.buildFile(creator.build())
            }
        }
    }

    private fun fields() {
        source.allProperties().forEach {
            creator.addField(FieldSpec.builder(it.type.qualifiedName.className(), it.name, PRIVATE).build())
        }
    }

    private fun getInstance() {
        creator.addField(FieldSpec.builder(entityName, "instance", PRIVATE).build())
        val ctor = source.bestConstructor()
        val params = ctor?.parameters?.map { it.name } ?: emptyList()
        val properties = source.allProperties()
            .toMutableList()
        properties.removeIf { it.name in params }
        val method = methodBuilder("getInstance")
            .addAnnotation(NonNull::class.java)
            .addAnnotation(Override::class.java)
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

        method.beginControlFlow("switch (model.getName())")
        source.allProperties().forEach { property ->
            method.addCode("case \"${property.name}\":")
            method.addStatement("${property.name} = (\$T)value", property.type.name.className())
            method.beginControlFlow("if(instance != null)")
            val mutator = property.mutator
            if (mutator != null) {
                method.addStatement("instance.${mutator.name}(${property.name})")
            } else {
                method.addStatement("throw new \$T(\"An instance has already been created and can not be updated because ${property
                    .name} does not have a set method.\")",
                    IllegalStateException::class.java)
            }
            method.endControlFlow()
            method.addStatement("break")
        }
        method.endControlFlow()


        creator.addMethod(method.build())
    }
}