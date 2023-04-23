package dev.morphia.critter.java.extensions

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier.JAVA_TRANSIENT
import com.mongodb.client.model.geojson.Geometry
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Id
import dev.morphia.annotations.Property
import dev.morphia.critter.Critter
import dev.morphia.critter.java.JavaContext
import dev.morphia.critter.kotlin.extensions.hasAnnotation
import java.time.temporal.Temporal
import java.util.Date
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.PropertySource

fun PropertySource<*>.mappedName(): String {
    return if (hasAnnotation(Id::class.java)) {
        "\"_id\""
    } else {
        getAnnotation(Embedded::class.java)?.getLiteralValue("value")
            ?: getAnnotation(Property::class.java)?.getLiteralValue("value")
            ?: "\"$name\""
    }
}

fun PropertySource<*>.mappedType(context: JavaContext): JavaClassSource? {
    return context.entities()[concreteType().getQualifiedName()]
}

fun PropertySource<*>.isMappedType(context: JavaContext): Boolean {
    return mappedType(context) != null
}

fun PropertySource<*>.concreteType() = type.getTypeArguments().lastOrNull() ?: type

fun PropertySource<*>.isContainer(): Boolean {
    return name in JavaContext.CONTAINER_TYPES || try {
        val klass = Class.forName(name)
        List::class.java.isAssignableFrom(klass) || Set::class.java.isAssignableFrom(klass)
    } catch (_: Exception) {
        false
    }
}

fun PropertySource<*>.isGeoCompatible(): Boolean {
    return name in JavaContext.GEO_TYPES || try {
        Geometry::class.java.isAssignableFrom(Class.forName(name))
    } catch (_: Exception) {
        false
    }
}

fun PropertySource<*>.isNumeric(): Boolean {
    return JavaContext.NUMERIC_TYPES.contains(type.qualifiedName)
        || try {
        val clazz = Class.forName(type.qualifiedName)
        Temporal::class.java.isAssignableFrom(clazz)
            || Date::class.java.isAssignableFrom(clazz)
    } catch (_: Exception) {
        false
    }
}

fun PropertySource<*>.isText() = JavaContext.TEXT_TYPES.contains(type.qualifiedName)

fun PropertySource<JavaClassSource>.ignored(): Boolean {
    return field.isStatic || field.isTransient ||
        hasAnnotation(Transient::class.java) ||
        hasAnnotation(dev.morphia.annotations.Transient::class.java)
}