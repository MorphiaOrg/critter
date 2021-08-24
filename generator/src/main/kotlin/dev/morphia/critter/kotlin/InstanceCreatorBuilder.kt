package dev.morphia.critter.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.LATEINIT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.morphia.critter.SourceBuilder
import dev.morphia.mapping.codec.Conversions
import dev.morphia.mapping.codec.MorphiaInstanceCreator
import dev.morphia.mapping.codec.pojo.PropertyModel
import java.io.File

class InstanceCreatorBuilder(val context: KotlinContext) : SourceBuilder {
    private lateinit var source: KotlinClass
    private lateinit var creator: TypeSpec.Builder
    private lateinit var creatorName: ClassName
    private lateinit var entityName: ClassName
    override fun build() {
        context.classes.values.forEach { source ->
            this.source = source
            entityName = ClassName.bestGuess(source.qualifiedName)
            creatorName = ClassName("dev.morphia.mapping.codec.pojo", "${source.name}InstanceCreator")
            creator = TypeSpec.classBuilder(creatorName)
            val sourceTimestamp = source.lastModified()
            val decoderFile = File(context.outputDirectory, creatorName.canonicalName.replace('.', '/') + ".java")

            creator.addAnnotation(AnnotationSpec.builder(Suppress::class.java)
//                .addMember("\"UNNECESSARY_NOT_NULL_ASSERTION\"")
                .addMember("\"UNCHECKED_CAST\"")
                .build())
            if (!source.isAbstract() && context.shouldGenerate(sourceTimestamp, decoderFile.lastModified())) {
                creator.addSuperinterface(MorphiaInstanceCreator::class.java)
                properties()
                getInstance()
                set()

                context.buildFile(creator.build(), Conversions::class.java to "convert")
            }
        }
    }

    private fun properties() {
        source.properties.forEach {
            var type = it.type.typeName()
            val property = PropertySpec.builder(it.name, type, PRIVATE)
                .mutable(true)

            if(it.stringLiteralInitializer == "null") {
                property.addModifiers(LATEINIT)
            } else {
                property.initializer(it.stringLiteralInitializer)
            }

            creator.addProperty(
                property
                    .build()
            )
        }
    }

    private fun getInstance() {
        creator.addProperty(PropertySpec.builder("instance", entityName.copy(true), PRIVATE)
            .mutable(true)
            .initializer("null")
            .build())
        val ctor = source.bestConstructor()
        val params = ctor?.parameters?.map { it.name } ?: emptyList()
        val properties = source.properties
            .toMutableList()
        properties.removeIf { it.name in params }
        val method = FunSpec.builder("getInstance")
            .addModifiers(OVERRIDE)
            .returns(entityName)
            .beginControlFlow("if (instance == null)")
            .beginControlFlow("instance = ${entityName.simpleName}(${params.joinToString()}).also")
        properties.forEach { property ->
            if (!property.isFinal) {
                method.addStatement("it.${property.name} = ${property.name}")
            }
        }
        method.endControlFlow()
        method.endControlFlow()

        creator.addFunction(method.addStatement("return instance!!").build())
    }

    private fun set() {
        val method = FunSpec.builder("set")
            .addModifiers(OVERRIDE)
            .addParameter("value", ClassName("kotlin", "Any"))
            .addParameter("model", PropertyModel::class.java)

        source.properties.forEachIndexed { index, property ->
            val ifStmt = "if (\"${property.name}\".equals(model.getName()))"
            if (index == 0) {
                method.beginControlFlow(ifStmt)
            } else {
                method.nextControlFlow("else $ifStmt")
            }
            method.addStatement("${property.name} = value as %T", property.type.typeName())
            method.beginControlFlow("if(instance != null)")
            if (!property.isFinal) {
                method.addStatement("instance!!.${property.name} = value")
            } else {
                method.addStatement(
                    "throw %T(\"An instance has already been created and can not be updated because ${property.name} " +
                        "does not have a set method.\")", IllegalStateException::class.java
                )
            }
            method.endControlFlow()
        }
        method.endControlFlow()


        creator.addFunction(method.build())
    }
}