package dev.morphia.critter

import com.mongodb.client.model.geojson.Geometry
import dev.morphia.mapping.Mapper
import org.bson.Document
import org.jboss.forge.roaster.model.Type
import java.time.temporal.Temporal
import java.util.Date

data class CritterType(val name: String, val typeParameters: List<CritterType> = listOf()) {
    companion object {
        val DOCUMENT = CritterType(Document::class.java.name)
        val MAPPER = CritterType(Mapper::class.java.name)
        internal val CONTAINER_TYPES = listOf("List", "Set")
            .map { listOf(it, "java.util.$it", "Mutable$it") }
            .flatten()
        internal val GEO_TYPES = listOf("double[]", "Double[]").explodeTypes()
        internal val NUMERIC_TYPES = listOf("Float", "Double", "Long", "Int", "Integer", "Byte", "Short", "Number").explodeTypes()
        internal val TEXT_TYPES = listOf("String").explodeTypes()
        private fun List<String>.explodeTypes(): List<String> {
            return flatMap {
                listOf(it, "$it?", "java.lang.$it", "java.lang.$it?", "kotlin.$it", "kotlin.$it?")
            }
        }

        fun isNumeric(type: String): Boolean {
            return NUMERIC_TYPES.contains(type)
                || try {
                val clazz = Class.forName(type)
                Temporal::class.java.isAssignableFrom(clazz)
                    || Date::class.java.isAssignableFrom(clazz)
            } catch (_: Exception) {
                false
            }
        }

        fun isText(type: String) = TEXT_TYPES.contains(type)
    }

    init {
        if (name != "void" && !name.contains(".")) {
            throw IllegalArgumentException("$name is not fully qualified")
        }
    }
    fun isContainer() = name in CONTAINER_TYPES
    fun isGeoCompatible(): Boolean {
        return name in GEO_TYPES || return try {
            Geometry::class.java.isAssignableFrom(Class.forName(name))
        } catch (_: Exception) {
            false
        }
    }

    fun isNumeric() = isNumeric(name)
    fun isText() = isText(name)
    fun concreteType() = typeParameters.lastOrNull()?.name ?: name
    fun isParameterized(): Boolean {
        return typeParameters.isNotEmpty()
    }

    override fun toString(): String {
        return name + (if (typeParameters.isNotEmpty()) typeParameters.joinToString(", ", "<", ">") else "")
    }
}

fun Type<*>.toCritter(): CritterType {
    val name = if (name != "void") qualifiedName else name
    return CritterType(name, typeArguments.map { it.toCritter() })
}