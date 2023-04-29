package dev.morphia.critter.java

import com.squareup.javapoet.ClassName
import dev.morphia.annotations.Reference
import dev.morphia.critter.Critter.addMethods
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.java.extensions.attachFilters
import dev.morphia.critter.java.extensions.attachUpdates
import dev.morphia.critter.java.extensions.concreteType
import dev.morphia.critter.java.extensions.isMappedType
import dev.morphia.critter.java.extensions.mappedName
import dev.morphia.critter.titleCase
import java.io.File
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.PropertySource

class JavaCriteriaBuilder(val context: JavaContext): SourceBuilder {
    private var nested = mutableListOf<JavaClassSource>()

    override fun build() {
        context.entities().values.forEach { source ->
            nested.clear()
            val criteriaClass = Roaster.create(JavaClassSource::class.java)
                    .setPackage(context.criteriaPkg ?: (source.`package` + ".criteria"))
                    .setName(source.name + "Criteria")
                    .setFinal(true)

            val outputFile = File(context.outputDirectory, criteriaClass.qualifiedName.replace('.', '/') + ".java")
            if (!source.isAbstract()) {
                criteriaClass.addField("private static final ${criteriaClass.name}Impl instance = new ${criteriaClass.name}Impl()")
                val impl = Roaster.create(JavaClassSource::class.java)
                        .setName(source.name + "CriteriaImpl")
                        .setStatic(true)
                        .setFinal(true)
                        .apply {
                            addField("private final String path")
                            addMethods("""
                                ${name}() {
                                    this.path = null;
                                }
            
                                ${name}(String fieldName) {
                                    this.path = fieldName;
                                }"""
                            ).forEach { it.isConstructor = true }
                        }

                criteriaClass.addMethod("""
                    private static String extendPath(String path, String addition) {
                        return path != null ? path + "." + addition : addition;
                    }
                """)
                processFields(source, criteriaClass, impl)
                criteriaClass.addNestedType(impl)

                nested.forEach { type ->
                    criteriaClass.addNestedType(type)
                    type.imports.forEach { criteriaClass.addImport(it)}
                }

                generate(outputFile, criteriaClass)
            }
        }
    }

    private fun processFields(source: CritterType, criteriaClass: JavaClassSource, impl: JavaClassSource) {
        source.allProperties().forEach { field ->
            criteriaClass.addField("public static final String ${field.name} = ${field.mappedName()}; ")
            addField(criteriaClass, impl, field)
        }
        impl.addMethod("""
            public String path() {
                return path;
            }""".trimIndent())

    }

    private fun addField(criteriaClass: JavaClassSource, impl: JavaClassSource, property: PropertySource<JavaClassSource>) {
        if (property.hasAnnotation(Reference::class.java)) {
            addReferenceCriteria(criteriaClass, impl, property)
        } else {
            impl.addFieldCriteriaMethod(criteriaClass, property)
            if (!property.isMappedType(context)) {
                addFieldCriteriaClass(property)
            }
        }
    }

    private fun addFieldCriteriaClass(property: PropertySource<JavaClassSource>) {
        addNestedType(Roaster.create(JavaClassSource::class.java))
                .apply {
                    name = "${property.name.titleCase()}FieldCriteria"
                    isStatic = true
                    isFinal = true
                    addField("private String path")

                    addMethod("""${name}(String path) {
                        |this.path = path;
                        |}""".trimMargin()).isConstructor = true
                    attachFilters(property)
                    attachUpdates(property)
                    addMethod("""
                        public String path() {
                            return path;
                        }""")
                }
    }

    private fun addReferenceCriteria(criteriaClass: JavaClassSource, impl: JavaClassSource, property: PropertySource<JavaClassSource>) {
        val fieldCriteriaName = property.name.titleCase() + "FieldCriteria"
        criteriaClass.addImport(fieldCriteriaName)
        criteriaClass.addMethod("""
            public static ${fieldCriteriaName} ${property.name}() {
                return instance.${property.name}();
            }""")
        impl.addMethod("""
            public ${fieldCriteriaName} ${property.name}() {
                return new ${fieldCriteriaName}(extendPath(path, "${property.name}"));
            }""")
        addFieldCriteriaClass(property)
    }

    private fun addNestedType(nestedClass: JavaClassSource): JavaClassSource {
        nested.add(nestedClass)
        return nestedClass
    }

    private fun generate(outputFile: File, criteriaClass: JavaClassSource) {
        outputFile.parentFile.mkdirs()
        outputFile.writeText(criteriaClass.toString())
    }

    private fun JavaClassSource.addFieldCriteriaMethod(criteriaClass: JavaClassSource, property: PropertySource<JavaClassSource>) {
        val concreteType = property.concreteType()
        val annotations = context.entities()[concreteType.qualifiedName]?.annotations()
        val fieldCriteriaName = if (annotations == null) {
            property.name.titleCase() + "FieldCriteria"
        } else {
            val outer = concreteType.name + "Criteria"
            val impl = concreteType.name + "CriteriaImpl"

            criteriaClass.addImport("${criteriaClass.`package`}.${outer}.${impl}")
            impl
        }
        criteriaClass.addMethods("""
            public static ${fieldCriteriaName} ${property.name}() {
                return instance.${property.name}();
            }""".trimIndent())

        val path = """extendPath(path, "${property.name}")"""
        addMethods("""
            public ${fieldCriteriaName} ${property.name}() {
                return new ${fieldCriteriaName}(${path});
            }""".trimIndent())

    }

}

fun String.className(): ClassName {
    return ClassName.get(if (contains(".")) substringBeforeLast('.') else "", substringAfterLast('.'))
}
