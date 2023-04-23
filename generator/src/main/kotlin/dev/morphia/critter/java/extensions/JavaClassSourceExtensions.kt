package dev.morphia.critter.java.extensions

import dev.morphia.critter.Critter
import dev.morphia.critter.FilterSieve
import dev.morphia.critter.UpdateSieve
import dev.morphia.critter.methodCase
import java.util.TreeMap
import org.jboss.forge.roaster.model.source.AnnotationSource
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaInterfaceSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.jboss.forge.roaster.model.source.PropertySource

fun JavaClassSource.attachFilters(property: PropertySource<JavaClassSource>) {
    FilterSieve.handlers(property, this)
}

fun JavaClassSource.attachUpdates(property: PropertySource<JavaClassSource>) {
    UpdateSieve.handlers(this, property)
}

fun JavaClassSource.methods(annotation: Class<out Annotation>): List<MethodSource<JavaClassSource>> {
    return methods
        .filter { it.hasAnnotation(annotation) }
}

fun JavaClassSource.constructors() = methods
    .filter { it.isConstructor }

fun JavaClassSource.allProperties(): List<PropertySource<JavaClassSource>> {
    return (Critter.javaContext.classes[this.superType]?.allProperties() ?: listOf()) +
        this.properties
            .filter { !it.ignored() }

}

fun JavaClassSource.bestConstructor(): MethodSource<JavaClassSource>? {
    val propertyMap = allProperties()
        .map { it.name to it.type }
        .toMap(TreeMap())
    val matches = constructors()
        .filter {
            it.parameters.all { param ->
                propertyMap[param.name].toString() == param.type.toString()
            }
        }
        .sortedBy { it.parameters.size }
        .reversed()

    return matches.firstOrNull()
}

fun JavaClassSource.modelName() = "${name.methodCase()}Model"

fun JavaClassSource.superType(): JavaClassSource? = Critter.javaContext.classes[this.superType]

fun JavaClassSource.interfaces(): List<JavaInterfaceSource> = this.interfaces
    .map { Critter.javaContext.interfaces[it] }
    .filterNotNull()


fun JavaClassSource?.superTypes(): List<JavaClassSource> = this?.superType()?.let { type ->
    listOf(type) + type.superTypes() } ?: listOf()

fun JavaClassSource.annotation(annotation: Class<out Annotation>): AnnotationSource<*>? {
    return getAnnotation(annotation) ?: interfaces()
        .mapNotNull { it.annotation(annotation) }
        .firstOrNull()
}

fun JavaClassSource.hasAnnotations(vararg annotations: Class<out Annotation>): Boolean {
    return annotations.any { hasAnnotation(it) }
        || superTypes().any { it.hasAnnotations(*annotations)}
        || interfaces.any { Critter.javaContext.interfaces[it].hasAnnotations(*annotations)}
}