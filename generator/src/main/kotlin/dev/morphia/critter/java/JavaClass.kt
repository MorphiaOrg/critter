package dev.morphia.critter.java

import dev.morphia.critter.CritterAnnotation
import dev.morphia.critter.CritterClass
import dev.morphia.critter.CritterField
import org.jboss.forge.roaster.Roaster.parse
import org.jboss.forge.roaster.model.Method
import org.jboss.forge.roaster.model.Visibility.PUBLIC
import org.jboss.forge.roaster.model.source.AnnotationSource
import org.jboss.forge.roaster.model.source.FieldSource
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.io.File

class JavaClass(val context: JavaContext, file: File,
                val sourceClass: JavaClassSource = parse(JavaClassSource::class.java, file))
    : CritterClass(sourceClass.name, sourceClass.`package`, file) {

    val superClass: JavaClass? by lazy {
        context.resolve(sourceClass.`package`, sourceClass.superType)
    }

    var visibility = PUBLIC
    val annotations = mutableListOf<CritterAnnotation>()
    val fields: List<CritterField> by lazy {
        val parent = context.resolve(name = sourceClass.superType)
        (parent?.fields ?: listOf()) + listFields(sourceClass).map { javaField ->
            CritterField(javaField.name, javaField.type.qualifiedName).apply {
                javaField.type?.typeArguments?.let {
                    if (it.isNotEmpty()) {
                        parameterizedType = it.joinToString(", ", prefix = "${javaField.type.qualifiedName}<",
                                postfix = ">") {
                            it.qualifiedName
                        }
                    }

                    it.forEach { type ->
                        shortParameterTypes.add(type.name)
                        fullParameterTypes.add(type.qualifiedName)
                    }
                }
                annotations += javaField.annotations.map { ann ->
                    CritterAnnotation(ann.qualifiedName, ann.values.map { it.name to it.literalValue }.toMap())
                }
            }
        }
                .sortedBy(CritterField::name)
                .toMutableList()
    }
    val constructors: List<Method<*, *>> by lazy {
        sourceClass.methods.filter { it.isConstructor }
    }
    val qualifiedName: String by lazy {
        pkgName?.let { "$pkgName.$name" } ?: name
    }

    fun listFields(type: JavaClassSource?): List<FieldSource<JavaClassSource>> {
        return if (type != null && type.name != "java.lang.Object") {
            mutableListOf<FieldSource<JavaClassSource>>() + type.fields
        } else listOf()
    }

    init {
        annotations += sourceClass.annotations.map { ann ->
            CritterAnnotation(ann.qualifiedName, ann.values.map { Pair<String, Any>(it.name, it.stringValue) }
                    .toMap())
//            ann.toCritter()
        }
        visibility = sourceClass.visibility
    }

/*
    private fun AnnotationSource<*>.toCritter(): CritterAnnotation {
        return CritterAnnotation(qualifiedName).also { ann ->
            values.forEach { value ->
                ann.values[value.name] = get getAnnotationValue(value.name).toCritter()
            }
        }
    }
*/

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
}