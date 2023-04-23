package dev.morphia.critter.java.extensions

import dev.morphia.critter.Critter
import org.jboss.forge.roaster.model.source.AnnotationSource
import org.jboss.forge.roaster.model.source.JavaInterfaceSource

fun JavaInterfaceSource.interfaces(): List<JavaInterfaceSource> = this.interfaces
    .map { Critter.javaContext.interfaces[it] }
    .filterNotNull()

fun JavaInterfaceSource?.hasAnnotations(vararg annotations: Class<out Annotation>): Boolean {
    return this?.let {
        annotations.any { hasAnnotation(it) }
            || interfaces().any { it.hasAnnotations(*annotations) }
    } ?: false
}

fun JavaInterfaceSource.annotation(annotation: Class<out Annotation>): AnnotationSource<*>? {
    return getAnnotation(annotation) ?: interfaces()
        .mapNotNull {
            val annotation1 = it.annotation(annotation);
            annotation1
        }
        .firstOrNull()
}
