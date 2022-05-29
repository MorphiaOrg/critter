package dev.morphia.critter.java

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.MethodSpec.methodBuilder
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeSpec.Builder
import dev.morphia.Datastore
import dev.morphia.critter.SourceBuilder
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.EntityModelBuilder
import dev.morphia.mapping.codec.pojo.TypeData
import dev.morphia.mapping.codec.pojo.experimental.EntityModelImporter
import org.bson.codecs.pojo.PropertyAccessor
import org.jboss.forge.roaster.model.Type
import org.jboss.forge.roaster.model.source.AnnotationSource
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.PropertySource
import java.util.concurrent.atomic.AtomicInteger
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

class ModelImporter(val context: JavaContext) : SourceBuilder {
    private lateinit var utilName: String
    private lateinit var util: Builder
    private lateinit var source: JavaClassSource
    private lateinit var properties: List<PropertySource<JavaClassSource>>
    private lateinit var importer: Builder
    private lateinit var importerName: ClassName
    private val builders = AtomicInteger(1)
    override fun build() {
        importerName = ClassName.get("dev.morphia.mapping.codec.pojo", "CritterModelImporter")
        importer = TypeSpec.classBuilder(importerName)
            .addModifiers(PUBLIC, Modifier.FINAL)
            .addSuperinterface(EntityModelImporter::class.java)
        val method = methodBuilder("importModels")
            .addModifiers(PUBLIC)
            .addParameter(Datastore::class.java, "datastore")
            .returns(ParameterizedTypeName.get(List::class.java, EntityModel::class.java))

        method.addCode("return List.of(")
        method.addCode(
            context.classes.values
                .filter { !it.isAbstract() }
                .joinToString(",\n\t\t") { source ->
                    "build${source.name.titleCase()}Model(datastore)"
                }
        )
        method.addCode(");")
        importer.addMethod(method.build())

        typeData()

        importer.addMethod(methodBuilder("importCodecProvider")
            .addModifiers(PUBLIC)
            .addParameter(Datastore::class.java, "datastore")
            .addCode("""
                        ${"$"}T mapper = datastore.getMapper();
                        mapper.register(new CritterCodecProvider(mapper, datastore));
                        datastore.updateDatabaseWithRegistry();
                    """.trimIndent(), Mapper::class.java)
            .build())

        context.classes.values
            .filter { !it.isAbstract() }
            .map { it.sourceClass }
            .forEach { source ->
                this.source = source
                this.properties = discoverProperties()
                this.utilName = "${source.name}Util"
                this.util = TypeSpec.classBuilder(utilName)
                    .addModifiers(PRIVATE, STATIC)
                val builder = methodBuilder("build${source.name.titleCase()}Model")
                    .addModifiers(PRIVATE)
                    .addParameter(Datastore::class.java, "datastore")
                    .returns(EntityModel::class.java)

                builder
                    .addCode("var modelBuilder = new \$T(datastore)\n", EntityModelBuilder::class.java)
                    .addCode(".type(\$T.class)", source.qualifiedName.className())

                annotations(builder)
                properties(builder)
                builder.addStatement("return modelBuilder.build()")

                importer.addMethod(builder.build())
                importer.addType(util.build())
            }

        context.buildFile(importer.build())
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

    private fun properties(builder: MethodSpec.Builder) {
        properties.forEach { property ->
            builder.addCode("""modelBuilder.addProperty()
                    .name("${property.name}")
                    .accessor(${accessor(property)})
                """)
            typeData(builder, property)
            discoverAnnotations(property).forEach {
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

    private fun accessor(property: PropertySource<JavaClassSource>): String {
        val name = "${methodName(property)}Accessor"
        util.addMethod(
            methodBuilder(name)
                .addModifiers(PRIVATE, STATIC)
                .returns(PropertyAccessor::class.java)
                .addCode(
                    """
                    return new ${"$"}T() {
                        @Override
                        public void set(Object instance, Object value) {
                            ((${source.name})instance).${property.mutator.name}((${"$"}T)value);
                        }
                        
                        @Override
                        public Object get(Object instance) {
                            return ((${source.name})instance).${property.accessor.name}();
                        }
                    };
                """.trimIndent(), PropertyAccessor::class.java, property.type.qualifiedName.className()
                )
                .build()
        )

        return "$utilName.$name()"
    }

    private fun methodName(property: PropertySource<JavaClassSource>) =
        (property.name).methodCase()

    private fun typeData(builder: MethodSpec.Builder, property: PropertySource<JavaClassSource>) {
        if (!property.type.isParameterized) {
            builder.addCode(".typeData(\$T.builder(${property.type}.class).build())\n", TypeData::class.java)
        } else {
            builder.addCode(".typeData(${typeDataGenerics(property)})")
        }
    }

    private fun typeDataGenerics(property: PropertySource<JavaClassSource>): String {
        val name = "${methodName(property)}TypeData"
        val method = methodBuilder(name)
            .addModifiers(PRIVATE, STATIC)
            .returns(TypeData::class.java)
        val typeCount = AtomicInteger(0)
        val argument = property.field.type

        method.addCode("return ")
        emitTypeData(method, typeCount, argument)
        method.addCode(";")

        util.addMethod(method.build())
        return "$utilName.$name()"
    }

    private fun emitTypeData(method: MethodSpec.Builder, typeCount: AtomicInteger, type: Type<JavaClassSource>) {
        method.addCode("typeData(\$T.class", type.qualifiedName.className())

        val arguments = type.typeArguments.forEachIndexed { i, it ->
            method.addCode(", ")
            emitTypeData(method, typeCount, it)
        }
        method.addCode(")")
    }
    private fun emitTypeData2(method: MethodSpec.Builder, typeCount: AtomicInteger, type: Type<JavaClassSource>): String {
        val varName = "type${typeCount.getAndIncrement()}"

        val arguments = type.typeArguments.map {
            emitTypeData(method, typeCount, it)
        }

        method.addCode("var $varName = \$T.builder(\$T.class)", TypeData::class.java, type.qualifiedName.className())
        arguments.forEach {
            method.addCode(".addTypeParameter($it)")
        }
        method.addCode(".build();")

        return varName
    }

    private fun typeArguments(argument: Type<JavaClassSource>): List<Type<JavaClassSource>> {
        val args = mutableListOf<Type<JavaClassSource>>()

        var type = argument.typeArguments.lastOrNull()
        while(type != null) {
            args += type
            type = type.typeArguments.lastOrNull()
        }

        return args
    }

    private fun discoverAnnotations(property: PropertySource<JavaClassSource>):
        List<AnnotationSource<JavaClassSource>> {
        return property.field.annotations +
            listOf(property.accessor, property.mutator)
                .flatMap { it.annotations }
    }

    private fun annotations(builder: MethodSpec.Builder) {
        source.annotations
            .filter { it.qualifiedName.startsWith("dev.morphia")}
            .forEach {
            if (it.values.isNotEmpty()) {
                builder.addCode("\n.annotation(${buildAnnotation(it)}())")
            } else {
                val name = annotationBuilderName(it)
                builder.addCode("\n.annotation(\$T.${name.simpleName().methodCase()}().build())\n",
                    annotationBuilderName(it))
            }
        }
        builder.addCode(";")

    }

    private fun buildAnnotation(annotation: AnnotationSource<JavaClassSource>): String {
        val builderName = annotationBuilderName(annotation)
        val methodName = "annotation${annotation.name.titleCase()}${builders.getAndIncrement()}"
        val builder = methodBuilder(methodName)
            .addModifiers(PRIVATE, STATIC)
            .returns(annotation.qualifiedName.className())

        builder.addStatement("var ${annotation.name.methodCase()} = ${"$"}T.${builderName.simpleName().methodCase()}()", builderName)
        annotation.values
            .forEach { pair ->
                val name = pair.name
                var value = annotation.getLiteralValue(name)
                val arrayValue = annotation.annotationArrayValue
                val annotationValue = annotation.getAnnotationValue(name)
                if (annotationValue != null) {
                    value = buildAnnotation(annotationValue) + "()"
                } else if (arrayValue != null) {
                    value = arrayValue.joinToString(", ") {
                        buildAnnotation(it) + "()"
                    }
                }
                builder.addStatement("${annotation.name.methodCase()}.$name($value)")
            }

        builder.addStatement("return ${annotation.name.methodCase()}.build()")

        util.addMethod(builder.build())
        return "$utilName.$methodName"
    }

    private fun annotationBuilderName(it: AnnotationSource<JavaClassSource>): ClassName {
        var name = it.qualifiedName
        name = name.substringBeforeLast('.') + ".builders." + name.substringAfterLast('.')
        return (name + "Builder").className()
    }
}
