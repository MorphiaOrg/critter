package dev.morphia.critter

import com.mongodb.client.model.geojson.Geometry
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Id
import dev.morphia.annotations.Property
import org.jboss.forge.roaster.model.Visibility
import org.jboss.forge.roaster.model.Visibility.PUBLIC
import java.time.temporal.Temporal
import java.util.Date

class CritterField(val name: String, val type: String) {
    companion object {
        internal val CONTAINER_TYPES = listOf("List", "Set").map { listOf(it, "java.util.$it", "Mutable$it") }.flatMap { it }
        internal val GEO_TYPES = listOf("double[]", "Double[]").explodeTypes()
        internal val NUMERIC_TYPES = listOf("Float", "Double", "Long", "Int", "Integer", "Byte", "Short", "Number").explodeTypes()
        internal val TEXT_TYPES = listOf("String").explodeTypes()
        private fun List<String>.explodeTypes(): List<String> {
            return map {
                listOf(it, "$it?", "java.lang.$it", "java.lang.$it?", "kotlin.$it", "kotlin.$it?")
            }
                    .flatMap { it }
        }

        fun isNumeric(type: String): Boolean {
            var numeric = NUMERIC_TYPES.contains(type)
            if (!numeric) {
                numeric = try {
                    val clazz = Class.forName(type)
                    Temporal::class.java.isAssignableFrom(clazz)
                            || Date::class.java.isAssignableFrom(clazz)
                } catch (_: java.lang.Exception) {
                    false
                }
            }

            return numeric
        }

        fun isText(type: String) = TEXT_TYPES.contains(type)
    }

    val shortParameterTypes = mutableListOf<String>()
    val fullParameterTypes = mutableListOf<String>()
    val annotations = mutableListOf<CritterAnnotation>()
    var isStatic = false
    var isFinal = false
    var stringLiteralInitializer: String? = null
    var visibility: Visibility = PUBLIC
    var parameterizedType = type
    fun isContainer() = type.substringBefore("<") in CONTAINER_TYPES
    fun isGeoCompatible(): Boolean {
        try {
            val forName = Class.forName(type)
            if (Geometry::class.java.isAssignableFrom(forName)) {
                return true
            }
        } catch (ignored: Exception) {
        }
        return type in GEO_TYPES
    }

    fun isNumeric() = isNumeric(type)
    fun isText() = isText(type)
    fun hasAnnotation(aClass: Class<out Annotation>): Boolean {
        return annotations.any { it.matches(aClass) }
    }

    fun getValue(ann: Class<out Annotation>, defaultValue: String): String {
        return annotations.firstOrNull { it.matches(ann) }?.getValue() ?: defaultValue
    }

    fun mappedName(): String {
        return if (hasAnnotation(Id::class.java)) {
            "\"_id\""
        } else {
            getValue(Embedded::class.java, getValue(Property::class.java, "\"$name\""))
        }
    }

    override fun toString(): String {
        return "CritterField(name='$name', type='$type', parameterizedType='$fullParameterTypes')"
    }
}

fun String.nameCase(): String {
    return substring(0, 1).uppercase() + substring(1)
}
