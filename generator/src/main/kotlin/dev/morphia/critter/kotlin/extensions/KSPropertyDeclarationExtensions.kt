package dev.morphia.critter.kotlin.extensions

import className
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import dev.morphia.critter.CritterType
import dev.morphia.critter.kotlin.getAnnotation
import java.time.temporal.Temporal
import java.util.Date

fun KSPropertyDeclaration.isText(): Boolean = CritterType.TEXT_TYPES.contains(type.className())

fun KSPropertyDeclaration.isContainer(): Boolean = type.className() in CritterType.CONTAINER_TYPES

fun KSPropertyDeclaration.isNumeric(): Boolean {
    val name = type.className()
    return CritterType.NUMERIC_TYPES.contains(name) ||
        try {
            val clazz = Class.forName(name)
            Temporal::class.java.isAssignableFrom(clazz)
                || Date::class.java.isAssignableFrom(clazz)
        } catch (_: Exception) {
            false
        }
}

fun <T : Annotation> KSPropertyDeclaration.hasAnnotation(annotation: Class<T>): Boolean {
    return getAnnotation(annotation) != null
}
