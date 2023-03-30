package dev.morphia.critter.kotlin

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.FunSpec.Companion.builder
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.morphia.Datastore
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.kotlin.extensions.toTypeName
import dev.morphia.critter.methodCase
import dev.morphia.critter.titleCase
import dev.morphia.mapping.EntityModelImporter
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.MorphiaCodecProvider
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.EntityModelBuilder
import dev.morphia.mapping.codec.pojo.TypeData
import org.bson.codecs.pojo.PropertyAccessor
import java.util.concurrent.atomic.AtomicInteger
import nullable

@OptIn(DelicateKotlinPoetApi::class)
class ModelImporter(val context: KotlinContext) : SourceBuilder {
    private lateinit var utilName: String
    private lateinit var util: Builder
    private lateinit var source: KSDeclaration
    private lateinit var properties: List<KSPropertyDeclaration>
    private lateinit var importer: Builder
    private lateinit var importerName: ClassName
    private val builders = AtomicInteger(1)
    override fun build() {
        importerName = ClassName("dev.morphia.critter.codecs", "CritterModelImporter")
        importer = TypeSpec.classBuilder(importerName)
            .addSuperinterface(EntityModelImporter::class.java)
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class.java)
                    .addMember("\"UNCHECKED_CAST\"")
                    .build()
            )
        val method = builder("getModels")
            .addModifiers(OVERRIDE)
            .addParameter("mapper", Mapper::class.java)

        method.addCode("return listOf(")
        method.addCode(
            context.entities().values
                .filter { !it.isAbstract() }
                .joinToString(",\n\t\t") { source ->
                    "build${source.name().titleCase()}Model(mapper)"
                }
        )
        method.addCode(")")
        importer.addFunction(method.build())

        typeData()

        importer.addFunction(
            builder("getCodecProvider")
                .addModifiers(OVERRIDE)
                .addParameter("datastore", Datastore::class.java)
                .addStatement("return CritterCodecProvider(datastore)")
                .returns(MorphiaCodecProvider::class.java)
                .build()
        )

        context.entities().values
            .filter { !it.isAbstract() }
            .forEach { source ->
                this.source = source
                this.properties = source.getAllProperties().toList()
                this.utilName = "${source.name()}Util"
                this.util = TypeSpec.objectBuilder(utilName)
                    .addModifiers(INTERNAL)
                val builder = builder("build${source.name().titleCase()}Model")
                    .addModifiers(PRIVATE)
                    .addParameter("mapper", Mapper::class.java)
                    .returns(EntityModel::class.java)

                builder
                    .addCode("val modelBuilder = %T(mapper, %T::class.java)\n", EntityModelBuilder::class.java, source.toTypeName())

                annotations(builder)
                properties(builder)
                builder.addStatement("return modelBuilder.build()")

                importer.addFunction(builder.build())
                importer.addType(util.build())
            }
        val type = importer.build()
        context.buildFile(type)
        context.generateServiceLoader(EntityModelImporter::class.java, importerName.toString())
    }

    private fun typeData() {
        val objectBuilder = TypeSpec.companionObjectBuilder()
        val method = builder("typeData")
            .addParameter("type", Class::class.java.asClassName()
                .parameterizedBy(STAR))
            .addParameter(
                ParameterSpec.builder("arguments", TypeData::class.java.asClassName()
                    .parameterizedBy(STAR))
                    .addModifiers(VARARG)
                    .build()
            )
            .returns(TypeData::class.java.asClassName()
                .parameterizedBy(STAR))

        method.addStatement("val builder = %T.builder(type)", TypeData::class.java)
        method.beginControlFlow("for(argument in arguments)")
        method.addStatement("builder.addTypeParameter(argument)", TypeData::class.java)
        method.endControlFlow()
        method.addStatement("return builder.build()")

        objectBuilder.addFunction(method.build())
        importer.addType(objectBuilder.build())
    }

/*
    private fun discoverProperties(): List<PropertySource<JavaClassSource>> {
        fun props(type: JavaClassSource): List<PropertySource<JavaClassSource>> {
            val parent = context.resolve(name = type.superType)
            val list = parent?.let {
                props(it.sourceClass)
            } ?: emptyList()

            return list + type.properties
        }
        return props(source)
    }
*/
    private fun properties(builder: FunSpec.Builder) {
        properties
            .forEach { property ->
                builder.addCode(
                    """modelBuilder.addProperty()
                    .name("${property.name()}")
                    .accessor(${accessor(property)} as PropertyAccessor<in Any>)
                """
                )
                typeData(builder, property)
                property.annotations.forEach {
                    if (it.arguments.isNotEmpty()) {
                        val name = buildAnnotation(it)
                        builder.addCode(".annotation($name())\n")
                    } else {
                        val name = annotationBuilderName(it)
                        builder.addCode(".annotation(%T.${name.simpleName.methodCase()}().build())\n", name)
                    }
                }
                builder.addCode(".discoverMappedName()\n")
            }
    }

    private fun accessor(property: KSPropertyDeclaration): String {
        val name = "${property.name().methodCase()}Accessor"
        val propertyType = property.type.toTypeName()
            .copy(nullable = property.type.nullable())
        val method = builder(name)
            .returns(PropertyAccessor::class.java.asClassName()
                .parameterizedBy(STAR))
            .addCode("""
                    return object: %T<%T> {
                        override fun <S : Any> set(instance: S, newValue: %T) {
                        
                """.trimIndent(), PropertyAccessor::class.java, propertyType, propertyType)
        if(property.isOpen()) {
            method.addStatement("(instance as ${source.name()}).${property.name()} = newValue")
        } else {
            method.addStatement("throw %T(\"${property.name()} is immutable.\")", IllegalStateException::class.java)
        }
        method.addCode(
            """
                }
                    override fun <S : Any> get(instance: S): %T {
                        return (instance as ${source.name()}).${property.name()}
                    }
                }
            """.trimIndent(), propertyType)
        util.addFunction(method.build())

        return "$utilName.$name()"
    }

    private fun methodName(property: KSPropertyDeclaration) = (property.name()).methodCase()
    private fun typeData(builder: FunSpec.Builder, property: KSPropertyDeclaration) {
        if (!property.type.isParameterized()) {
            var type = property.type.toTypeName()
            if ( type.isNullable ) {
                 type = type.copy(nullable = false)
            }
            builder.addCode(".typeData(%T.builder(%T::class.java).build())\n", TypeData::class.java, type)
        } else {
            builder.addCode(".typeData(${typeDataGenerics(property)})")
        }
    }

    private fun typeDataGenerics(property: KSPropertyDeclaration): String {
        val name = "${methodName(property)}TypeData"
        val method = builder(name)
            .addModifiers(INTERNAL)
            .returns(TypeData::class.java.asClassName()
                .parameterizedBy(STAR))
        val argument = property.type

        method.addCode("return ")
        emitTypeData(method, argument.toTypeName())

        util.addFunction(method.build())
        return "$utilName.$name()"
    }

    private fun emitTypeData(method: FunSpec.Builder, typeName: TypeName) {
        val typeArguments = mutableListOf<TypeName>()
        var baseType = typeName
        if (typeName is ParameterizedTypeName) {
            typeArguments += typeName.typeArguments
            baseType = typeName.rawType
        }

        method.addCode("typeData(%T::class.java", baseType)

        typeArguments.forEach {
            method.addCode(", ")
            emitTypeData(method, it)
        }
        method.addCode(")")
    }

    private fun annotations(builder: FunSpec.Builder) {
        source.annotations
            .filter { it.annotationType.packageName().startsWith("dev.morphia") }
            .forEach {
                if (it.arguments.isNotEmpty()) {
                    builder.addCode(".annotation(${buildAnnotation(it)}())\n")
                } else {
                    val name = annotationBuilderName(it)
                    builder.addCode(
                        "\n.annotation(%T.${name.simpleName.methodCase()}().build())\n",
                        annotationBuilderName(it)
                    )
                }
            }
    }

    private fun buildAnnotation(annotation: KSAnnotation): String {
        val builderName = annotationBuilderName(annotation)
        val methodName = "annotation${annotation.annotationType.simpleName()}${builders.getAndIncrement()}"
        val builder = builder(methodName)
            .returns(annotation.annotationType.toTypeName())

        builder.addStatement("val builder = %T.${builderName.simpleName.methodCase()}()", builderName)
        annotation.arguments
            .forEach { argument ->
                val name = argument.name?.asString() ?: "value"
                var value = argument.value
                value = when (value) {
                    is String -> "\"${value}\""
                    is KSAnnotation -> buildAnnotation(value) + "()"
                    is Array<*> -> value.joinToString(", ") {
                        buildAnnotation(it as KSAnnotation) + "()"
                    }

                    is KSType -> "${value.declaration.className()}::class.java"
                    else -> value
                }
                builder.addStatement(".$name($value)")
            }

        builder.addStatement("return builder.build()")

        util.addFunction(builder.build())
        return "$utilName.$methodName"
    }

    private fun annotationBuilderName(it: KSAnnotation): ClassName {
        var name = it.annotationType.className()
        name = name.substringBeforeLast('.') + ".internal." + name.substringAfterLast('.')
        return (name + "Builder").className()
    }
}

fun KSTypeReference.isParameterized() = element?.typeArguments?.isNotEmpty() ?: false
