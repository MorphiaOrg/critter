package dev.morphia.critter.java

import com.mongodb.lang.NonNull
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.MethodSpec.methodBuilder
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeSpec.Builder
import dev.morphia.Datastore
import dev.morphia.critter.Critter.DEFAULT_PACKAGE
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.methodCase
import dev.morphia.critter.titleCase
import dev.morphia.mapping.EntityModelImporter
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.MorphiaCodecProvider
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.EntityModelBuilder
import dev.morphia.mapping.codec.pojo.TypeData
import java.util.concurrent.atomic.AtomicInteger
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC
import org.bson.codecs.pojo.PropertyAccessor
import org.jboss.forge.roaster.model.Annotation
import org.jboss.forge.roaster.model.Type
import org.jboss.forge.roaster.model.source.JavaSource
import org.jboss.forge.roaster.model.source.PropertySource

class ModelImporter(val context: JavaContext) : SourceBuilder {
    private lateinit var utilName: String
    private lateinit var util: Builder
    private lateinit var source: CritterType
    private lateinit var properties: List<PropertySource<*>>
    private lateinit var importer: Builder
    private lateinit var importerName: ClassName
    private lateinit var packageName: String
    private val builders = AtomicInteger(1)
    override fun build() {
        packageName = DEFAULT_PACKAGE
        importerName = ClassName.get(packageName, "CritterModelImporter")
        importer = TypeSpec.classBuilder(importerName)
            .addModifiers(PUBLIC, Modifier.FINAL)
            .addSuperinterface(EntityModelImporter::class.java)
        importer.addAnnotation(
            AnnotationSpec.builder(SuppressWarnings::class.java)
                .addMember("value", CodeBlock.of("""{"unchecked", "rawtypes"}"""))
                .build()
        )

        getModels()

        typeData()

        getCodecProvider()

        context.buildFile(packageName, importer.build())
        context.generateServiceLoader(EntityModelImporter::class.java, importerName.toString())
    }

    private fun getCodecProvider() {
        importer.addMethod(
            methodBuilder("getCodecProvider")
                .addAnnotation(NonNull::class.java)
                .addModifiers(PUBLIC)
                .addAnnotation(Override::class.java)
                .addParameter(
                    ParameterSpec.builder(Datastore::class.java, "datastore")
                        .addAnnotation(NonNull::class.java)
                        .build()
                )
                .addStatement("return new CritterCodecProvider(datastore)")
                .returns(MorphiaCodecProvider::class.java)
                .build()
        )
    }

    private fun buildModel(source: CritterType) {
        this.source = source
        this.properties = source.allProperties()
        this.utilName = "${source.name}Util"
        this.util = TypeSpec.classBuilder(utilName)
            .addModifiers(PRIVATE, STATIC)
        val builder = methodBuilder("build${source.name.titleCase()}Model")
            .addModifiers(PRIVATE)
            .addParameter(Mapper::class.java, "mapper")
            .returns(EntityModel::class.java)

        builder.addCode("var modelBuilder = new \$T(mapper, \$T.class)\n",
                EntityModelBuilder::class.java, source.qualifiedName.className())

        annotations(builder)
        properties(builder)
        builder.addStatement("return modelBuilder.build()")

        importer.addMethod(builder.build())
        importer.addType(util.build())
    }

    private fun getModels() {
        val method = methodBuilder("getModels")
            .addAnnotation(NonNull::class.java)
            .addModifiers(PUBLIC)
            .addParameter(
                ParameterSpec.builder(Mapper::class.java, "mapper")
                    .addAnnotation(NonNull::class.java)
                    .build()
            )
            .returns(ParameterizedTypeName.get(List::class.java, EntityModel::class.java))
        val values: MutableList<CritterType> = context.entities().values.toMutableList()
        val buckets = bucketEntities(values)

        val models = mutableMapOf<String, String>()
        val build = mutableListOf<() -> Unit>()

        values.forEach { entity ->
            models[entity.qualifiedName] = setUpModel(entity, method, build)
        }

        buckets.forEach { entry ->
            val model = models[entry.key]
            if(model != null) {
                entry.value.forEach { subtype ->
                    method.addCode("${subtype.modelName}.setSuperClass($model);\n")
                }
            }

        }

        method.addCode("return List.of(${models.values.joinToString(", ")});")

        importer.addMethod(method.build())

        build.forEach { it() }
    }

    private fun setUpModel(
        entity: CritterType,
        method: MethodSpec.Builder,
        build: MutableList<() -> Unit>,
    ): String {
        val model = entity.modelName
        method.addCode("var $model = build${entity.name.titleCase()}Model(mapper);\n")
        build += { buildModel(entity) }
        return model
    }

    private fun bucketEntities(entities: MutableList<CritterType>): Map<String, MutableSet<CritterType>> {
        val map = HashMap<String, MutableSet<CritterType>>()
        for (entity in entities) {
            entity.superType()?.let {
                map.getOrPut(it.qualifiedName) { mutableSetOf() }.add(entity)
            }
            entity.interfaces().forEach {
                map.getOrPut(it.qualifiedName) { mutableSetOf() }.add(entity)
            }
        }
        return map
    }

    private fun typeData() {
        val method = methodBuilder("typeData")
            .addModifiers(PRIVATE, STATIC)
            .addParameter(Class::class.java, "type")
            .addParameter(arrayOf<TypeData<*>>()::class.java, "arguments")
            .varargs(true)
            .returns(TypeData::class.java)

        method.addStatement("var builder = \$T.builder(type)", TypeData::class.java)
        method.beginControlFlow("for(TypeData argument: arguments)")
        method.addStatement("builder.addTypeParameter(argument)", TypeData::class.java)
        method.endControlFlow()
        method.addStatement("return builder.build()")

        importer.addMethod(method.build())
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

    private fun properties(builder: MethodSpec.Builder) {
        properties
            .forEach { property ->
            builder.addCode(
                """modelBuilder.addProperty()
                    .name("${property.name}")
                    .accessor(${accessor(property)})
                """
            )
            typeData(builder, property)
            property.annotations.forEach {
                if (it.values.isNotEmpty()) {
                    val name = buildAnnotation(it)
                    builder.addCode(".annotation($name())\n")
                } else {
                    val name = annotationBuilderName(it)
                    builder.addCode(".annotation(${"$"}T.${name.simpleName().methodCase()}().build())\n", name)
                }
            }
            builder.addCode(".discoverMappedName();\n")
        }
    }

    private fun accessor(property: PropertySource<*>): String {
        val name = "${property.name.methodCase()}Accessor"
        val method = methodBuilder(name)
            .addModifiers(PRIVATE, STATIC)
            .returns(PropertyAccessor::class.java)

        method.addCode("return new ${"$"}T() { ", PropertyAccessor::class.java)

        accessorSet(method, property)
        accessorGet(method, property)

        method.addCode("};")
        util.addMethod(method.build())

        return "$utilName.$name()"
    }

    private fun accessorGet(method: MethodSpec.Builder, property: PropertySource<*>) {
        method.addCode(
            """
                @Override
                public Object get(Object instance) {
            """.trimIndent())
        property.mutator?.let {
            method.addCode("return ((${source.name})instance).${property.accessor.name}();")
        } ?: run {
            method.addStatement("throw new \$T(\"${property.name} does not have a get method.\")", IllegalStateException::class.java)
        }

        method.addCode("""
                        }
                """.trimIndent()
        )
    }

    private fun accessorSet(method: MethodSpec.Builder, property: PropertySource<*>) {
        method.addCode("""
            @Override
            public void set(Object instance, Object value) {
            """.trimIndent())
        property.mutator?.let {
            method.addCode("((${source.name})instance).${it.name}((${"$"}T)value);", property.type.qualifiedName.className())
        } ?: run {
            method.addStatement("throw new \$T(\"${property.name} does not have a set method.\")", IllegalStateException::class.java)
        }
        method.addCode("""}
        """.trimMargin())
    }

    private fun methodName(property: PropertySource<*>) = (property.name).methodCase()

    private fun typeData(builder: MethodSpec.Builder, property: PropertySource<*>) {
        if (!property.type.isParameterized) {
            builder.addCode(".typeData(\$T.builder(\$T.class).build())\n", TypeData::class.java, property.type.qualifiedName.className())
        } else {
            builder.addCode(".typeData(${typeDataGenerics(property)})")
        }
    }

    private fun typeDataGenerics(property: PropertySource<*>): String {
        val name = "${methodName(property)}TypeData"
        val method = methodBuilder(name)
            .addModifiers(PRIVATE, STATIC)
            .returns(TypeData::class.java)
        val typeCount = AtomicInteger(0)
        val argument = property.type

        method.addCode("return ")
        emitTypeData(method, typeCount, argument)
        method.addCode(";")

        util.addMethod(method.build())
        return "$utilName.$name()"
    }

    private fun emitTypeData(method: MethodSpec.Builder, typeCount: AtomicInteger, type: Type<out JavaSource<*>>) {
        method.addCode("typeData(\$T.class", type.qualifiedName.className())

        type.typeArguments.forEach {
            method.addCode(", ")
            emitTypeData(method, typeCount, it)
        }
        method.addCode(")")
    }

    private fun annotations(builder: MethodSpec.Builder) {
        source.annotations()
            .forEach {
                if (it.values.isNotEmpty()) {
                    builder.addCode("\n.annotation(${buildAnnotation(it)}())")
                } else {
                    val name = annotationBuilderName(it)
                    builder.addCode(
                        "\n.annotation(\$T.${name.simpleName().methodCase()}().build())\n",
                        annotationBuilderName(it)
                    )
                }
            }
        builder.addCode(";")
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildAnnotation(annotation: Annotation<*>): String {
        val builderName = annotationBuilderName(annotation)
        val methodName = "annotation${annotation.name}${builders.getAndIncrement()}"
        val builder = methodBuilder(methodName)
            .addModifiers(PRIVATE, STATIC)
            .returns(annotation.qualifiedName.className())


        builder.addStatement("var builder = ${"$"}T.${builderName.simpleName().methodCase()}()", builderName)
        val types = Class.forName(annotation.getQualifiedName())
            .declaredMethods
            .associate { it.name to it.returnType }

        annotation.values
            .forEach { pair ->
                val name = pair.name
                val type = types[name]!!
                val value: Any = when {
                    type.isEnum() -> {
                        val enumValue = annotation.getEnumValue(type as Class<out Enum<*>>, name)
                        CodeBlock.of("${"$"}T", (type.name + "." + enumValue).className())
                    }
                    type.isArray -> annotation.getAnnotationArrayValue(name)
                        .joinToString(", ") {
                            buildAnnotation(it) + "()"
                        }
                    type.isAnnotation -> buildAnnotation(annotation.getAnnotationValue(name)) + "()"
                    else -> annotation.getLiteralValue(name)
                        .replace("$", "${"$"}$")
                }
                builder.addStatement("builder.$name($value)")
            }

        builder.addStatement("return builder.build()")

        util.addMethod(builder.build())
        return "$utilName.$methodName"
    }

    private fun annotationBuilderName(it: Annotation<*>): ClassName {
        var name = it.qualifiedName
        name = name.substringBeforeLast('.') + ".internal." + name.substringAfterLast('.')
        return (name + "Builder").className()
    }
}
