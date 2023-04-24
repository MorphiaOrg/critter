package dev.morphia.critter.java

import dev.morphia.critter.FilterSieve
import dev.morphia.critter.UpdateSieve
import dev.morphia.critter.java.types.CritterClass
import dev.morphia.critter.java.types.CritterInterface
import dev.morphia.critter.methodCase
import org.jboss.forge.roaster.model.source.AnnotationSource
import org.jboss.forge.roaster.model.source.InterfaceCapableSource
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaInterfaceSource
import org.jboss.forge.roaster.model.source.JavaSource
import org.jboss.forge.roaster.model.source.MethodHolderSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.jboss.forge.roaster.model.source.PropertySource

abstract class CritterType(val context: JavaContext, private val type: JavaSource<*>) {
    companion object {
        fun of(context: JavaContext, source: JavaSource<*>): CritterType {
            return when (source) {
                is JavaClassSource -> CritterClass(context, source)
                is JavaInterfaceSource -> CritterInterface(context, source)
                else -> throw IllegalArgumentException("${source::class.java} is not a supported type")
            }
        }
    }

    val modelName = "${type.name.methodCase()}Model"
    val name = type.name
    val qualifiedName = type.qualifiedName
    val `package` = type.`package`

    fun annotations(): List<AnnotationSource<*>> {
        val local: List<AnnotationSource<*>> = this.type.annotations
            .filter { it.qualifiedName.startsWith("dev.morphia") }

        val onInterfaces: List<AnnotationSource<*>> = interfaces().flatMap { it.annotations() }
        val onSuperTypes: List<AnnotationSource<*>> = superTypes().flatMap { it.annotations() }

        return (local + onInterfaces + onSuperTypes)
    }
    fun getAnnotation(type: Class<out Annotation>): AnnotationSource<*>? {
        val local: AnnotationSource<*>? = this.type.getAnnotation(type)
        val onInterfaces: AnnotationSource<*>? = interfaces().mapNotNull { it.getAnnotation(type) }.firstOrNull()
        val onSuperTypes: AnnotationSource<*>? = superTypes().mapNotNull { it.getAnnotation(type) }.firstOrNull()

        return local ?: onInterfaces ?: onSuperTypes
    }

    fun hasAnnotations(vararg annotations: Class<out Annotation>): Boolean {
        return annotations.any { type.hasAnnotation(it) }
            || superTypes().any { it.hasAnnotations(*annotations) }
            || interfaces().any { it.hasAnnotations(*annotations) }
    }

    fun interfaces(): List<CritterType> {
        type as InterfaceCapableSource<*>
        return type.interfaces.map {
            context.classes[it]
        }.filterNotNull()
    }

    open fun superType(): CritterType? = null

    open fun superTypes(): List<CritterType> = listOf()

    open fun allProperties(): List<PropertySource<JavaClassSource>> = listOf()

    fun methods(annotation: Class<out Annotation>): List<MethodSource<*>> {
        val list = listOf(type as MethodHolderSource<*>) +
            superTypes().filterIsInstance<MethodHolderSource<*>>()
        return list.flatMap { it.methods }
            .filter { it.hasAnnotation(annotation) }
    }

    open fun constructors(): List<MethodSource<*>> = listOf()
    open fun bestConstructor(): MethodSource<JavaClassSource>? = null
    open fun isAbstract() = true
}

