package dev.morphia.critter.java

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeSpec.Builder
import dev.morphia.Datastore
import dev.morphia.critter.SourceBuilder
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

class ModelImporter(val context: JavaContext) : SourceBuilder {
    private lateinit var source: JavaClassSource
    private lateinit var importer: Builder
    private lateinit var importerName: ClassName
    private val builders = AtomicInteger(1)
    override fun build() {
        importerName = ClassName.get("dev.morphia.mapping.codec.pojo", "CritterModelImporter")
        importer = TypeSpec.classBuilder(importerName)
            .addModifiers(PUBLIC, Modifier.FINAL)
            .addSuperinterface(EntityModelImporter::class.java)
        val method = MethodSpec.methodBuilder("importModels")
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

        context.classes.values
            .filter { !it.isAbstract() }
            .map { it.sourceClass }
            .forEach { source ->
                this.source = source
                val builder = MethodSpec.methodBuilder("build${source.name.titleCase()}Model")
                    .addModifiers(PRIVATE)
                    .addParameter(Datastore::class.java, "datastore")
                    .returns(EntityModel::class.java)
                    .addStatement("var modelBuilder = new \$T(datastore)", EntityModelBuilder::class.java)
                    .addStatement("modelBuilder.type(\$T.class)", source.qualifiedName.className())

                annotations(source, builder)
                properties(source, builder)
                builder.addStatement("return modelBuilder.build()")

                importer.addMethod(builder.build())
            }

        JavaFile
            .builder(CodecsBuilder.packageName, importer.build())
            .build()
            .writeTo(context.outputDirectory)
    }

    private fun properties(source: JavaClassSource, builder: MethodSpec.Builder) {
        source.properties.forEach { property ->
            builder.addCode("""
                var ${property.name} = modelBuilder.addProperty()
                            .name("${property.name}")
                            .accessor(new ${"$"}T() {
                                @Override
                                public void set(Object instance, Object value) {
                                    ((${source.name})instance).${property.mutator.name}((${"$"}T)value);
                                }
                                
                                @Override
                                public Object get(Object instance) {
                                    return ((${source.name})instance).${property.accessor.name}();
                                }
                            });
                            
                """.trimIndent(), PropertyAccessor::class.java, property.type.qualifiedName.className())
            /*
TypeData<?> typeData = entityModelBuilder.getTypeData(type, TypeData.newInstance(methods.getter),
methods.getter.getGenericReturnType());
             */
            typeData(builder, property)
            discoverAnnotations(property).forEach {
                if (it.values.isNotEmpty()) {
                    val methodCase = buildAnnotation(it)
                    builder.addStatement("modelBuilder.annotation($methodCase())")
                } else {
                    builder.addStatement("modelBuilder.annotation(${"$"}T.builder())", annotationBuilderName(it))
                }
            }
            builder.addStatement("${property.name}.discoverMappedName()")
        }
    }

    private fun typeData(builder: MethodSpec.Builder, property: PropertySource<JavaClassSource>) {
        if (!property.type.isParameterized) {
            builder.addStatement("${property.name}.typeData(\$T.builder(${property.type}.class).build())", TypeData::class.java)
        } else {
            var argument = property.type
            while (argument.isParameterized) {
                argument = argument.typeArguments[0]
            }
            val varName = "${property.name}Type"
            builder.addStatement("var $varName = \$T.builder(${argument}.class)", TypeData::class.java)
            argument = property.type
            while (argument.isParameterized) {
                builder.addStatement("$varName.addTypeParameter(TypeData.newInstance(null, \$T.class))",
                    argument.qualifiedName.className())
                argument = argument.typeArguments[0]
            }

            builder.addStatement("${property.name}.typeData($varName.build())")

        }
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
                builder.addStatement("${"modelBuilder"}.annotation($methodCase())")
            } else {
                builder.addStatement("${"modelBuilder"}.annotation(${"$"}T.builder())", annotationBuilderName(it))
            }
        }
    }

    private fun buildAnnotation(annotation: AnnotationSource<JavaClassSource>): String {
        val builderName = annotationBuilderName(annotation)
        val methodName = annotation.name.methodCase() + builders.getAndIncrement()
        val builder = MethodSpec.methodBuilder(methodName)
            .addModifiers(PRIVATE)
            .returns(annotation.qualifiedName.className())

        builder.addStatement("var builder = ${"$"}T.builder()", builderName)

        annotation.values
            .map { pair -> pair.name }
            .forEach { name: String ->
                var value = annotation.getLiteralValue(name)
                if (value.startsWith("@")) {
                    value = buildAnnotation(annotation.getAnnotationValue(name)) + "()"
                }
                builder.addStatement("builder.$name($value)")
            }

        builder.addStatement("return builder")

        importer.addMethod(builder.build())
        return methodName
    }

    private fun annotationBuilderName(it: AnnotationSource<JavaClassSource>): ClassName {
        var name = it.qualifiedName
        name = name.substringBeforeLast('.') + ".experimental." + name.substringAfterLast('.')
        return (name + "Builder").className()
    }
}
