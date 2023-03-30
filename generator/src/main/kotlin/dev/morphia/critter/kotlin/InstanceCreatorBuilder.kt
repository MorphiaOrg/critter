package dev.morphia.critter.kotlin

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.LATEINIT
import com.squareup.kotlinpoet.KModifier.OPEN
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.kotlin.extensions.bestConstructor
import dev.morphia.critter.kotlin.extensions.className
import dev.morphia.mapping.codec.Conversions
import dev.morphia.mapping.codec.MorphiaInstanceCreator
import dev.morphia.mapping.codec.pojo.PropertyModel

@OptIn(DelicateKotlinPoetApi::class)
class InstanceCreatorBuilder(val context: KotlinContext) : SourceBuilder {
    companion object {
        val defaultValues = mapOf(
            "kotlin.Boolean" to "false",
            "kotlin.Char" to "0.toChar()",
            "kotlin.Int" to "0",
            "kotlin.Long" to "0L",
            "kotlin.Short" to "0",
            "kotlin.Float" to "0.0F",
            "kotlin.Double" to "0.0")
    }

    private lateinit var source: KSClassDeclaration
    private lateinit var creator: TypeSpec.Builder
    private lateinit var creatorName: ClassName
    private lateinit var entityName: ClassName

    override fun build() {
        context.entities().values.forEach { source ->
            this.source = source
            entityName = ClassName.bestGuess(source.className())
            creatorName = ClassName("dev.morphia.mapping.codec.pojo", "${source.name()}InstanceCreator")
            creator = TypeSpec.classBuilder(creatorName)
                .addAnnotation(
                    AnnotationSpec.builder(Suppress::class.java)
                        .addMember("\"UNCHECKED_CAST\"")
                        .build()
                )
                .addModifiers(OPEN)

            if (!source.isAbstract()) {
                creator.addSuperinterface(MorphiaInstanceCreator::class.java)
                getInstance()
                set()

                context.buildFile(creator.build(), Conversions::class.java to "convert")
            }
        }
    }

    private fun getInstance() {
        creator.addProperty(
            PropertySpec.builder("instance", entityName, PRIVATE, LATEINIT)
                .mutable(true)
                .build()
        )
        val ctor = source.bestConstructor()
        val params = ctor?.parameters?.map { it } ?: emptyList()

        val entityProperties = mutableListOf<PropertySpec>()

        val method = FunSpec.builder("getInstance")
            .addModifiers(OVERRIDE)
            .returns(entityName)
            .beginControlFlow("if (!::instance.isInitialized)")
            method.addStatement("instance = %T(${params.joinToString { param -> param.name!!.asString()}})", entityName)

        declareProperties(params, entityProperties)
        if (entityProperties.isNotEmpty()) {
            method.beginControlFlow(".also")
            entityProperties.forEach { property ->
                if(source.getAllProperties().first { prop -> prop.name() == property.name }.isOpen()) {
                    if(property.modifiers.contains(LATEINIT)) {
                        method.beginControlFlow("if (::${property.name}.isInitialized)")
                    }

                    method.addStatement("it.${property.name} = ${property.name}")

                    if(property.modifiers.contains(LATEINIT)) {
                        method.endControlFlow()
                    }
                }
            }
            method.endControlFlow()
        }
        method.endControlFlow()

        creator.addFunction(method.addStatement("return instance").build())
    }

    /**
     * @return the properties not included in the ctor call
     */
    private fun declareProperties(
        params: List<KSValueParameter>,
        entityProperties: MutableList<PropertySpec>
    ) {
        source.getAllProperties().forEach {
            val initializer = defaultValues[it.type.className()]
            val type = it.type

            val ctorParam = params.firstOrNull { param -> param.name?.asString() == it.name() }
            val nullable = it.type.resolve().isMarkedNullable

            val property = PropertySpec.builder(it.name(), type.toTypeName(), PRIVATE)
                .mutable(true)

            if (initializer != null) {
                property.initializer(initializer)
            } else {
                if (nullable) {
                    property.initializer("null")
                } else {
                    property.addModifiers(LATEINIT)
                }
            }
            property.build().also { propertySpec ->
                if (ctorParam == null) entityProperties += propertySpec
                creator.addProperty(propertySpec)
            }
        }
    }

    private fun set() {
        val method = FunSpec.builder("set")
            .addModifiers(OVERRIDE)
            .addParameter("value", ClassName("kotlin", "Any").copy(nullable = true))
            .addParameter("model", PropertyModel::class.java)

        method.beginControlFlow("when(model.name)")
        source.getAllProperties().forEach { property ->
            method.beginControlFlow("\"${property.name()}\" ->")
            method.addStatement("this.${property.name()} = value as %T", property.type.toTypeName())
            method.beginControlFlow("if(::instance.isInitialized)")
            if (property.isOpen()) {
                method.addStatement("instance.${property.name()} = value")
            } else {
                method.addStatement("""throw %T("${property.name()} is immutable.")""", IllegalStateException::class.java)
            }
            method.endControlFlow()
            method.endControlFlow()
        }
        method.endControlFlow()


        creator.addFunction(method.build())
    }
}