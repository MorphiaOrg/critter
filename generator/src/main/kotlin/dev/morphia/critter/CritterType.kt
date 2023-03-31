package dev.morphia.critter

import com.mongodb.client.model.geojson.Geometry
import com.squareup.kotlinpoet.ClassName
import dev.morphia.mapping.Mapper
import java.time.temporal.Temporal
import java.util.Date
import org.bson.Document
import org.jboss.forge.roaster.model.Type

data class CritterType(val name: String, val typeParameters: List<CritterType> = listOf(), val nullable: Boolean = false) {
    companion object {
        val DOCUMENT = CritterType(Document::class.java.name, nullable = false)
        val MAPPER = CritterType(Mapper::class.java.name, nullable = false)
        internal val LIST_TYPES = listOf("List", "MutableList").explodeTypes(listOf("java.util", "kotlin.collections"))
        internal val SET_TYPES = listOf("Set", "MutableSet").explodeTypes(listOf("java.util", "kotlin.collections"))
        internal val MAP_TYPES = listOf("Map", "MutableMap").explodeTypes(listOf("java.util", "kotlin.collections"))
        internal val CONTAINER_TYPES = LIST_TYPES + SET_TYPES

        internal val GEO_TYPES = listOf("double[]", "Double[]").explodeTypes()
        internal val NUMERIC_TYPES = listOf("Float", "Double", "Long", "Int", "Integer", "Byte", "Short", "Number").explodeTypes()
        internal val TEXT_TYPES = listOf("String").explodeTypes()
        private fun List<String>.explodeTypes(packages: List<String> = listOf("java.lang", "kotlin")): List<String> {
            return flatMap {
                listOf(it) + packages.map { pkg -> "$pkg.$it"}
            }
        }
    }

    private val className: ClassName by lazy {
        ClassName.bestGuess(name)
    }
    val packageName: String by lazy {
        className.packageName
    }
    val simpleName: String by lazy {
        className.simpleName
    }

    val isContainer: Boolean by lazy {
        name in CONTAINER_TYPES || try {
            val klass = Class.forName(name)
            List::class.java.isAssignableFrom(klass) || Set::class.java.isAssignableFrom(klass)
        } catch (_: Exception) {
            false
        }
    }

    val isGeoCompatible: Boolean by lazy {
        name in GEO_TYPES || try {
            Geometry::class.java.isAssignableFrom(Class.forName(name))
        } catch (_: Exception) {
            false
        }
    }

    val isNumeric: Boolean by lazy {
        NUMERIC_TYPES.contains(name)
            || try {
            val clazz = Class.forName(name)
            Temporal::class.java.isAssignableFrom(clazz)
                || Date::class.java.isAssignableFrom(clazz)
        } catch (_: Exception) {
            false
        }
    }

    val isText: Boolean by lazy {
        TEXT_TYPES.contains(name)
    }

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
    return CritterType(name, typeArguments.map { it.toCritter() }, nullable = true)
}

