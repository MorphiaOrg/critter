package dev.morphia.critter.kotlin

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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.asClassName
import dev.morphia.Datastore
import dev.morphia.critter.CritterAnnotation
import dev.morphia.critter.CritterProperty
import dev.morphia.critter.CritterType
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.methodCase
import dev.morphia.critter.titleCase
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.MorphiaCodecProvider
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.EntityModelBuilder
import dev.morphia.mapping.codec.pojo.TypeData
import dev.morphia.mapping.codec.pojo.experimental.EntityModelImporter
import org.bson.codecs.pojo.PropertyAccessor
import java.util.concurrent.atomic.AtomicInteger

@OptIn(DelicateKotlinPoetApi::class)
class ModelImporter(val context: KotlinContext) : SourceBuilder {
    private lateinit var utilName: String
    private lateinit var util: Builder
    private lateinit var source: KotlinClass
    private lateinit var properties: List<CritterProperty>
    private lateinit var importer: Builder
    private lateinit var importerName: ClassName
    private val builders = AtomicInteger(1)
    override fun build() {
        importerName = ClassName("dev.morphia.mapping.codec.pojo", "CritterModelImporter")
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
//            .returns(
//                List::class.java.asClassName()
//                    .parameterizedBy(EntityModel::class.java.asClassName())
//            )

        method.addCode("return listOf(")
        method.addCode(
            context.entities().values
                .filter { !it.isAbstract() }
                .joinToString(",\n\t\t") { source ->
                    "build${source.name.titleCase()}Model(mapper)"
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
                this.properties = source.properties
                this.utilName = "${source.name}Util"
                this.util = TypeSpec.objectBuilder(utilName)
                    .addModifiers(INTERNAL)
                val builder = builder("build${source.name.titleCase()}Model")
                    .addModifiers(PRIVATE)
                    .addParameter("mapper", Mapper::class.java)
                    .returns(EntityModel::class.java)

                builder
                    .addCode("var modelBuilder = %T(mapper, %T::class.java)\n", EntityModelBuilder::class.java, source.qualifiedName.className())

                annotations(builder)
                properties(builder)
                builder.addStatement("return modelBuilder.build()")

                importer.addFunction(builder.build())
                importer.addType(util.build())
            }

        context.buildFile(importer.build())
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

        method.addStatement("var builder = %T.builder(type)", TypeData::class.java)
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
                    .name("${property.name}")
                    .accessor(${accessor(property)} as PropertyAccessor<in Any>)
                """
                )
                typeData(builder, property)
                property.annotations.forEach {
                    if (it.values.isNotEmpty()) {
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

    private fun accessor(property: CritterProperty): String {
        val name = "${property.name.methodCase()}Accessor"
        try {
            val method = builder(name)
                .returns(PropertyAccessor::class.java.asClassName()
                    .parameterizedBy(STAR))
                .addCode(
                    """
                        return object: %T<%T> {
                            override fun <S : Any> set(instance: S, `value`: %T?) {
                            
                    """.trimIndent(), PropertyAccessor::class.java, property.type.typeName(), property.type.typeName()
                )
            if(!property.isFinal) {
                method.addStatement("(instance as ${source.name}).${property.name} = value as %T", property.type.typeName())
            } else {
                method.addStatement("throw %T(\"${property.name} is immutable.\")", IllegalStateException::class.java)
            }
            method.addCode(
                """
                    }
                        override fun <S : Any> get(instance: S): %T? {
                            return (instance as ${source.name}).${property.name}
                        }
                    }
                """.trimIndent(), property.type.typeName()
            )
            util.addFunction(method.build())
        } catch (e: NullPointerException) {
            println("property = [${property}]")
            throw e
        }

        return "$utilName.$name()"
    }

    private fun methodName(property: CritterProperty) = (property.name).methodCase()
    private fun typeData(builder: FunSpec.Builder, property: CritterProperty) {
        if (!property.type.isParameterized()) {
            builder.addCode(".typeData(%T.builder(%T::class.java).build())\n", TypeData::class.java, property.type.typeName())
        } else {
            builder.addCode(".typeData(${typeDataGenerics(property)})")
        }
    }

    private fun typeDataGenerics(property: CritterProperty): String {
        val name = "${methodName(property)}TypeData"
        val method = builder(name)
            .addModifiers(INTERNAL)
            .returns(TypeData::class.java.asClassName()
                .parameterizedBy(STAR))
        val typeCount = AtomicInteger(0)
        val argument = property.type

        method.addCode("return ")
        emitTypeData(method, typeCount, argument)

        util.addFunction(method.build())
        return "$utilName.$name()"
    }

    private fun emitTypeData(method: FunSpec.Builder, typeCount: AtomicInteger, type: CritterType) {
        method.addCode("typeData(%T::class.java", type.name.className())

        type.typeParameters.forEach {
            method.addCode(", ")
            emitTypeData(method, typeCount, it)
        }
        method.addCode(")")
    }

    private fun annotations(builder: FunSpec.Builder) {
        source.annotations
            .filter { it.type.packageName.startsWith("dev.morphia") }
            .forEach {
                if (it.values.isNotEmpty()) {
                    builder.addCode("\n.annotation(${buildAnnotation(it)}())")
                } else {
                    val name = annotationBuilderName(it)
                    builder.addCode(
                        "\n.annotation(%T.${name.simpleName.methodCase()}().build())\n",
                        annotationBuilderName(it)
                    )
                }
            }
    }

    private fun buildAnnotation(annotation: CritterAnnotation): String {
        val builderName = annotationBuilderName(annotation)
        val methodName = "annotation${annotation.type.simpleName}${builders.getAndIncrement()}"
        val builder = builder(methodName)
            .addModifiers(PRIVATE)
            .returns(annotation.type.name.className())

        builder.addStatement("var builder = %T.${builderName.simpleName.methodCase()}()", builderName)
        annotation.values
            .forEach { pair ->
                val name = pair.key
                var value = annotation.literalValue(name)
                val arrayValue = annotation.annotationArrayValue()
                val annotationValue = annotation.annotationValue(name)
                if (annotationValue != null) {
                    value = buildAnnotation(annotationValue) + "()"
                } else if (arrayValue != null) {
                    value = arrayValue.joinToString(", ") {
                        buildAnnotation(it) + "()"
                    }
                }
                builder.addStatement("builder.$name($value)")
            }

        builder.addStatement("return builder.build()")

        util.addFunction(builder.build())
        return "$utilName.$methodName"
    }

    private fun annotationBuilderName(it: CritterAnnotation): ClassName {
        var name = it.type.name
        name = name.substringBeforeLast('.') + ".builders." + name.substringAfterLast('.')
        return (name + "Builder").className()
    }
}
