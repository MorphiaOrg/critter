package dev.morphia.critter.java

import dev.morphia.annotations.Reference
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.Critter.addMethods
import dev.morphia.critter.CritterField
import dev.morphia.critter.FilterSieve
import dev.morphia.critter.UpdateSieve
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.io.File
import java.io.PrintWriter
import java.util.Locale

class JavaCriteriaBuilder(private val context: JavaContext): SourceBuilder {
    private var nested = mutableListOf<JavaClassSource>()

    override fun build() {
        context.classes.values.forEach { source ->
            nested.clear()
            val criteriaClass = Roaster.create(JavaClassSource::class.java)
                    .setPackage(context.criteriaPkg ?: source.pkgName + ".criteria")
                    .setName(source.name + "Criteria")
                    .setFinal(true)

            val outputFile = File(context.outputDirectory, criteriaClass.qualifiedName.replace('.', '/') + ".java")
            val sourceTimestamp = source.lastModified()
            val timestamp = outputFile.lastModified()
            if (!source.isAbstract() && context.shouldGenerate(sourceTimestamp, timestamp)) {
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

    private fun processFields(source: JavaClass, criteriaClass: JavaClassSource, impl: JavaClassSource) {
        source.fields.forEach { field ->
            criteriaClass.addField("public static final String ${field.name} = ${field.mappedName()}; ")
            addField(criteriaClass, impl, field)
        }
        impl.addMethod("""
            public String path() {
                return path;
            }""".trimIndent())

    }

    private fun addField(criteriaClass: JavaClassSource, impl: JavaClassSource, field: CritterField) {
        if (field.hasAnnotation(Reference::class.java)) {
            addReferenceCriteria(criteriaClass, impl, field)
        } else {
            impl.addFieldCriteriaMethod(criteriaClass, field)
            if (!field.isMappedType()) {
                addFieldCriteriaClass(field)
            }
        }
    }

    private fun addFieldCriteriaClass(field: CritterField) {
        addNestedType(Roaster.create(JavaClassSource::class.java))
                .apply {
                    name = "${field.name.toTitleCase()}FieldCriteria"
                    isStatic = true
                    isFinal = true
                    addField("private String path")

                    addMethod("""${name}(String path) {
                        |this.path = path;
                        |}""".trimMargin()).isConstructor = true
                    attachFilters(field)
                    attachUpdates(field)
                    addMethod("""
                        public String path() {
                            return path;
                        }""")
                }
    }

    private fun addReferenceCriteria(criteriaClass: JavaClassSource, impl: JavaClassSource, field: CritterField) {
        val fieldCriteriaName = field.name.toTitleCase() + "FieldCriteria"
        criteriaClass.addImport(fieldCriteriaName)
        criteriaClass.addMethod("""
            public static ${fieldCriteriaName} ${field.name}() {
                return instance.${field.name}();
            }""")
        impl.addMethod("""
            public ${fieldCriteriaName} ${field.name}() {
                return new ${fieldCriteriaName}(extendPath(path, "${field.name}"));
            }""")
        addFieldCriteriaClass(field)
    }

    private fun addNestedType(nestedClass: JavaClassSource): JavaClassSource {
        nested.add(nestedClass)
        return nestedClass
    }

    private fun generate(outputFile: File, criteriaClass: JavaClassSource) {
        outputFile.parentFile.mkdirs()
        PrintWriter(outputFile).use { writer -> writer.println(criteriaClass.toString()) }
    }

    fun CritterField.mappedType(): JavaClass? {
        return context.classes[concreteType()]
    }

    fun CritterField.isMappedType(): Boolean {
        return mappedType() != null
    }

    private fun JavaClassSource.addFieldCriteriaMethod(criteriaClass: JavaClassSource, field: CritterField) {
        val concreteType = field.concreteType()
        val annotations = context.classes[concreteType]?.annotations
        val fieldCriteriaName = if (annotations == null) {
            field.name.toTitleCase() + "FieldCriteria"
        } else {
            val outer = concreteType.substringAfterLast('.') + "Criteria"
            val impl = concreteType.substringAfterLast('.') + "CriteriaImpl"

            criteriaClass.addImport("${criteriaClass.`package`}.${outer}.${impl}")
            impl
        }
        criteriaClass.addMethods("""
            public static ${fieldCriteriaName} ${field.name}() {
                return instance.${field.name}();
            }""".trimIndent())

        val path = """extendPath(path, "${field.name}")"""
        addMethods("""
            public ${fieldCriteriaName} ${field.name}() {
                return new ${fieldCriteriaName}(${path});
            }""".trimIndent())

    }

}

fun CritterField.concreteType() = if(fullParameterTypes.isNotEmpty()) fullParameterTypes.last() else type

private fun JavaClassSource.attachFilters(field: CritterField) {
    FilterSieve.handlers(field, this)
}

private fun JavaClassSource.attachUpdates(field: CritterField) {
    UpdateSieve.handlers(field, this)
}

fun String.toTitleCase(): String {
    return substring(0, 1).uppercase(Locale.getDefault()) + substring(1)
}

fun String.toMethodCase(): String {
    return substring(0, 1).toLowerCase() + substring(1)
}
