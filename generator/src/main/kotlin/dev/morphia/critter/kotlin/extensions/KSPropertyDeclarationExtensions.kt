package dev.morphia.critter.kotlin.extensions

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier.JAVA_TRANSIENT
import com.squareup.kotlinpoet.TypeName
import dev.morphia.annotations.Transient
import dev.morphia.critter.CritterType
import dev.morphia.critter.kotlin.getAnnotation
import java.time.temporal.Temporal
import java.util.Date

fun KSPropertyDeclaration.isNotTransient() =
    !modifiers.contains(JAVA_TRANSIENT) && !hasAnnotation(Transient::class.java)

fun KSPropertyDeclaration.isText(): Boolean = CritterType.TEXT_TYPES.contains(type.className())

//fun KSPropertyDeclaration.isContainer(): Boolean = type.className() in CritterType.CONTAINER_TYPES
fun KSPropertyDeclaration.isContainer(): Boolean {
    return isList() || isSet()
}

fun KSPropertyDeclaration.isList(): Boolean {
    return type.className() in CritterType.LIST_TYPES || try {
        List::class.java.isAssignableFrom(Class.forName(type.className()))
    } catch (_: Exception) {
        false
    }
}

fun KSPropertyDeclaration.isSet(): Boolean {
    return type.className() in CritterType.SET_TYPES || try {
        Set::class.java.isAssignableFrom(Class.forName(type.className()))
    } catch (_: Exception) {
        false
    }
}

fun KSPropertyDeclaration.isMap(): Boolean {
    return type.className() in CritterType.MAP_TYPES || try {
        Map::class.java.isAssignableFrom(Class.forName(type.className()))
    } catch (_: Exception) {
        false
    }
}

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

fun KSPropertyDeclaration.fullyQualified(): TypeName {
    return type.resolve().fullyQualified()
}

