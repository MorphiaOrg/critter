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
import dev.morphia.critter.CritterAnnotation
import dev.morphia.critter.CritterProperty
import dev.morphia.critter.CritterType
import dev.morphia.critter.SourceBuilder
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
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

class ModelImporter(val context: JavaContext) : SourceBuilder {
    private lateinit var utilName: String
    private lateinit var util: Builder
    private lateinit var source: JavaClass
    private lateinit var properties: List<CritterProperty>
    private lateinit var importer: Builder
    private lateinit var importerName: ClassName
    private val builders = AtomicInteger(1)
    override fun build() {
        importerName = ClassName.get(CodecsBuilder.packageName, "CritterModelImporter")
        importer = TypeSpec.classBuilder(importerName)
            .addModifiers(PUBLIC, Modifier.FINAL)
            .addSuperinterface(EntityModelImporter::class.java)
        importer.addAnnotation(
            AnnotationSpec.builder(SuppressWarnings::class.java)
                .addMember("value", CodeBlock.of("""{"unchecked", "rawtypes"}"""))
                .build()
        )

        val method = methodBuilder("getModels")
            .addAnnotation(NonNull::class.java)
            .addModifiers(PUBLIC)
            .addParameter(
                ParameterSpec.builder(Mapper::class.java, "mapper")
                    .addAnnotation(NonNull::class.java)
                    .build()
            )
            .returns(ParameterizedTypeName.get(List::class.java, EntityModel::class.java))

        method.addCode("return List.of(")
        method.addCode(
            context.entities().values
                .filter { !it.isAbstract() }
                .joinToString(",\n\t\t") { source ->
                    "build${source.name.titleCase()}Model(mapper)"
                }
        )
        method.addCode(");")
        importer.addMethod(method.build())

        typeData()

        importer.addMethod(
            methodBuilder("getCodecProvider")
                .addAnnotation(NonNull::class.java)
                .addModifiers(PUBLIC)
                .addAnnotation(Override::class.java)
                .addParameter(ParameterSpec.builder(Datastore::class.java, "datastore")
                    .addAnnotation(NonNull::class.java)
                    .build()
                )
                .addStatement("return new CritterCodecProvider(datastore)")
                .returns(MorphiaCodecProvider::class.java)
                .build()
        )

        context.entities().values
            .filter { !it.isAbstract() }
            .forEach { source ->
                this.source = source
                this.properties = source.properties
                this.utilName = "${source.name}Util"
                this.util = TypeSpec.classBuilder(utilName)
                    .addModifiers(PRIVATE, STATIC)
                val builder = methodBuilder("build${source.name.titleCase()}Model")
                    .addModifiers(PRIVATE)
                    .addParameter(Mapper::class.java, "mapper")
                    .returns(EntityModel::class.java)

                builder
                    .addCode("var modelBuilder = new \$T(mapper, \$T.class)\n",
                        EntityModelBuilder::class.java, source.qualifiedName.className())

                annotations(builder)
                properties(builder)
                builder.addStatement("return modelBuilder.build()")

                importer.addMethod(builder.build())
                importer.addType(util.build())
            }

        val type = importer.build()
        context.buildFile(type)
        context.generateServiceLoader(EntityModelImporter::class.java, importerName.toString())
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

    private fun accessor(property: CritterProperty): String {
        val name = "${property.name.methodCase()}Accessor"
        val method = methodBuilder(name)
            .addModifiers(PRIVATE, STATIC)
            .returns(PropertyAccessor::class.java)
            .addCode(
                """
                    return new ${"$"}T() {
                        @Override
                        public void set(Object instance, Object value) {
                """.trimIndent(), PropertyAccessor::class.java
            )
        property.mutator?.let {
            method.addCode("((${source.name})instance).${it.name}((${"$"}T)value);", property.type.name.className())
        } ?: run {
            method.addStatement("throw new \$T(\"${property.name} does not have a set method.\")", IllegalStateException::class.java)
        }
        method.addCode(
            """
                }
                    @Override
                    public Object get(Object instance) {
                        return ((${source.name})instance).${property.accessor?.name}();
                    }
                };
            """.trimIndent()
        )
        util.addMethod(method.build())

        return "$utilName.$name()"
    }

    private fun methodName(property: CritterProperty) = (property.name).methodCase()

    private fun typeData(builder: MethodSpec.Builder, property: CritterProperty) {
        if (!property.type.isParameterized()) {
            builder.addCode(".typeData(\$T.builder(\$T.class).build())\n", TypeData::class.java, property.type.name.className())
        } else {
            builder.addCode(".typeData(${typeDataGenerics(property)})")
        }
    }

    private fun typeDataGenerics(property: CritterProperty): String {
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

    private fun emitTypeData(method: MethodSpec.Builder, typeCount: AtomicInteger, type: CritterType) {
        method.addCode("typeData(\$T.class", type.name.className())

        type.typeParameters.forEach {
            method.addCode(", ")
            emitTypeData(method, typeCount, it)
        }
        method.addCode(")")
    }

    private fun annotations(builder: MethodSpec.Builder) {
        source.annotations
            .filter { it.type.packageName.startsWith("dev.morphia") }
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

    private fun buildAnnotation(annotation: CritterAnnotation): String {
        val builderName = annotationBuilderName(annotation)
        val methodName = "annotation${annotation.type.simpleName}${builders.getAndIncrement()}"
        val builder = methodBuilder(methodName)
            .addModifiers(PRIVATE, STATIC)
            .returns(annotation.type.name.className())

        builder.addStatement("var builder = ${"$"}T.${builderName.simpleName().methodCase()}()", builderName)
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

        util.addMethod(builder.build())
        return "$utilName.$methodName"
    }

    private fun annotationBuilderName(it: CritterAnnotation): ClassName {
        var name = it.type.name
        name = name.substringBeforeLast('.') + ".internal." + name.substringAfterLast('.')
        return (name + "Builder").className()
    }
}
