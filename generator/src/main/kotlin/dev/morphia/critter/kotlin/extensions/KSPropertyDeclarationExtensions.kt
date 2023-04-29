package dev.morphia.critter.kotlin.extensions

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier.JAVA_TRANSIENT
import com.mongodb.client.model.geojson.Geometry
import com.squareup.kotlinpoet.TypeName
import dev.morphia.annotations.Transient
import dev.morphia.critter.kotlin.KotlinContext.Companion.GEO_TYPES
import dev.morphia.critter.kotlin.KotlinContext.Companion.LIST_TYPES
import dev.morphia.critter.kotlin.KotlinContext.Companion.MAP_TYPES
import dev.morphia.critter.kotlin.KotlinContext.Companion.NUMERIC_TYPES
import dev.morphia.critter.kotlin.KotlinContext.Companion.SET_TYPES
import dev.morphia.critter.kotlin.KotlinContext.Companion.TEXT_TYPES
import dev.morphia.critter.kotlin.getAnnotation
import java.time.temporal.Temporal
import java.util.Date

fun KSPropertyDeclaration.isNotTransient() =
    !modifiers.contains(JAVA_TRANSIENT) && !hasAnnotation(Transient::class.java)

fun KSPropertyDeclaration.isText(): Boolean = TEXT_TYPES.contains(type.className())

fun KSPropertyDeclaration.isContainer(): Boolean {
    return isList() || isSet()
}

fun KSPropertyDeclaration.isList(): Boolean {
    return type.className() in LIST_TYPES || try {
        List::class.java.isAssignableFrom(Class.forName(type.className()))
    } catch (_: Exception) {
        false
    }
}

fun KSPropertyDeclaration.isSet(): Boolean {
    return type.className() in SET_TYPES || try {
        Set::class.java.isAssignableFrom(Class.forName(type.className()))
    } catch (_: Exception) {
        false
    }
}

fun KSPropertyDeclaration.isMap(): Boolean {
    return type.className() in MAP_TYPES || try {
        Map::class.java.isAssignableFrom(Class.forName(type.className()))
    } catch (_: Exception) {
        false
    }
}
fun KSPropertyDeclaration.isGeoCompatible(): Boolean {
    return name() in GEO_TYPES || try {
        Geometry::class.java.isAssignableFrom(Class.forName(type.className()))
    } catch (_: Exception) {
        false
    }
}

fun KSPropertyDeclaration.isNumeric(): Boolean {
    val name = type.className()
    return NUMERIC_TYPES.contains(name) ||
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

