package dev.morphia.critter.java

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.MethodSpec.methodBuilder
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
                    .addCode(".type(\$T.class)\n", source.qualifiedName.className())

                annotations(source, builder)
                properties(builder)
                builder.addStatement("return modelBuilder.build()")

                importer.addMethod(builder.build())
                importer.addType(util.build())
            }

        context.buildFile(importer.build())
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
        val argument = property.field.type
        val varName = "${property.name}Type"
        method.addStatement("var $varName = \$T.builder(\$T.class)", TypeData::class.java, argument.qualifiedName.className())
        argument.typeArguments.forEach {
            method.addStatement(
                "$varName.addTypeParameter(TypeData.builder(\$T.class).build())",
                it.qualifiedName.className()
            )
        }
        method.addStatement("return $varName.build()")

        util.addMethod(method.build())
        return "$utilName.$name()"
    }

    private fun discoverAnnotations(property: PropertySource<JavaClassSource>):
        List<AnnotationSource<JavaClassSource>> {
        return property.field.annotations +
            listOf(property.accessor, property.mutator)
                .flatMap { it.annotations }
    }

    private fun annotations(source: JavaClassSource, builder: MethodSpec.Builder) {
        source.annotations.forEach {
            if (it.values.isNotEmpty()) {
                val methodCase = buildAnnotation(it)
                builder.addCode(".annotation($methodCase());\n")
            } else {
                val name = annotationBuilderName(it)
                builder.addCode(".annotation(\$T.${name.simpleName().methodCase()}().build());\n", annotationBuilderName(it))
            }
        }
    }

    private fun buildAnnotation(annotation: AnnotationSource<JavaClassSource>): String {
        val builderName = annotationBuilderName(annotation)
        val methodName = annotation.name.methodCase() + builders.getAndIncrement()
        val builder = methodBuilder(methodName)
            .addModifiers(PRIVATE, STATIC)
            .returns(annotation.qualifiedName.className())

        builder.addStatement("var builder = ${"$"}T.${builderName.simpleName().methodCase()}()", builderName)

        annotation.values
            .map { pair -> pair.name }
            .forEach { name: String ->
                var value = annotation.getLiteralValue(name)
                if (value.startsWith("@")) {
                    value = buildAnnotation(annotation.getAnnotationValue(name)) + "()"
                }
                builder.addStatement("builder.$name($value)")
            }

        builder.addStatement("return builder.build()")

        util.addMethod(builder.build())
        return "$utilName.$methodName"
    }

    private fun annotationBuilderName(it: AnnotationSource<JavaClassSource>): ClassName {
        var name = it.qualifiedName
        name = name.substringBeforeLast('.') + ".builders." + name.substringAfterLast('.')
        return (name + "Builder").className()
    }
}
