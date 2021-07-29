package dev.morphia.critter.java

import dev.morphia.critter.CritterAnnotation
import dev.morphia.critter.CritterClass
import dev.morphia.critter.CritterMethod
import dev.morphia.critter.CritterProperty
import dev.morphia.critter.toCritter
import org.jboss.forge.roaster.Roaster.parse
import org.jboss.forge.roaster.model.Visibility.PUBLIC
import org.jboss.forge.roaster.model.source.FieldSource
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.jboss.forge.roaster.model.source.PropertySource
import java.io.File
import java.util.TreeMap

class JavaClass(
    val context: JavaContext, file: File,
    val sourceClass: JavaClassSource = parse(JavaClassSource::class.java, file)
) : CritterClass(sourceClass.name, sourceClass.`package`, file) {
    companion object {
        fun PropertySource<JavaClassSource>.ignored(): Boolean {
            return field.isStatic || field.isTransient ||
                hasAnnotation(Transient::class.java) ||
                hasAnnotation(dev.morphia.annotations.Transient::class.java)
        }
    }

    val superClass: JavaClass? by lazy {
        context.resolve(sourceClass.`package`, sourceClass.superType)
    }
    var visibility = PUBLIC
    val annotations = mutableListOf<CritterAnnotation>()
    val properties: List<CritterProperty> by lazy {
        val parent = context.resolve(name = sourceClass.superType)
        (parent?.properties ?: listOf()) + listFields(sourceClass)
            .filter { !it.isStatic && !it.isTransient }
            .map { javaField ->
                CritterProperty(
                    javaField.name,
                    javaField.type.toCritter(),
                    javaField.annotations.map { it.toCritter() },
                    javaField.isFinal,
                    javaField.visibility,
                    javaField.literalInitializer,
                ).also {
                    it.accessor = sourceClass.methods.firstOrNull { isGetter(it, javaField) }
                        ?.toCritter()
                    it.mutator = sourceClass.methods.firstOrNull { isSetter(it, javaField) }
                        ?.toCritter()
                }
            }
            .sortedBy(CritterProperty::name)
            .toMutableList()
    }

    private fun isGetter(method: MethodSource<JavaClassSource>, field: FieldSource<JavaClassSource>): Boolean {
        var matches = field.type.qualifiedName == method.returnType?.qualifiedName
        if (matches) {
            if (field.type.name.endsWith("Boolean", true)) {
                matches = method.name == "is${field.name.titleCase()}"
            } else {
                matches = method.name == "get${field.name.titleCase()}"
            }
        }
        return matches
    }

    private fun isSetter(method: MethodSource<JavaClassSource>, field: FieldSource<JavaClassSource>): Boolean {
        val set = "set${field.name.titleCase()}"
        val parameters = method.parameters
        return method.name == set &&
            parameters.size == 1 && parameters[0].type.qualifiedName == field.type.qualifiedName
    }

    val constructors: List<CritterMethod> by lazy {
        sourceClass.methods
            .filter { it.isConstructor }
            .map { it.toCritter() }
    }

    val qualifiedName: String by lazy {
        pkgName?.let { "$pkgName.$name" } ?: name
    }

    fun listFields(type: JavaClassSource): List<FieldSource<JavaClassSource>> {
        type.properties.forEach {
            it.field
        }
        return if (type.name != "java.lang.Object") {
            mutableListOf<FieldSource<JavaClassSource>>() + type.fields
        } else listOf()
    }

    init {
        annotations += sourceClass.annotations.map { it.toCritter() }
        visibility = sourceClass.visibility
    }

    fun isAbstract() = sourceClass.isAbstract

    fun lastModified(): Long {
        return Math.min(file.lastModified(), superClass?.lastModified() ?: Long.MAX_VALUE)
    }

    fun hasAnnotation(aClass: Class<out Annotation>): Boolean {
        return annotations.any { it.matches(aClass) }
    }

    override fun toString(): String {
        return "JavaClass<$name>"
    }

    fun methods(annotation: Class<out Annotation>): List<CritterMethod> {
        return sourceClass.methods
            .filter { it.hasAnnotation(annotation) }
            .map { it.toCritter() }
    }
    fun bestConstructor(): CritterMethod? {
        val propertyMap = properties
            .map { it.name to it.type }
            .toMap(TreeMap())
        val matches = constructors
            .filter { it.parameters.all { param -> propertyMap[param.name] == param.type } }
            .map { it to it.parameters.size }
            .sortedBy { it.second }
            .reversed()

        return matches.firstOrNull()?.first

    }
}