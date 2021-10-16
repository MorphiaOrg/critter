@file:Suppress("DEPRECATION")

package dev.morphia.critter

import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Id
import dev.morphia.annotations.Property

class CritterProperty(
    val name: String, var type: CritterType,
    val annotations: List<CritterAnnotation>,
    var isFinal: Boolean = false,
    var stringLiteralInitializer: String? = null
) {
    var accessor: CritterMethod? = null
    var mutator: CritterMethod? = null

    fun hasAnnotation(aClass: Class<out Annotation>): Boolean {
        return annotations.any { it.matches(aClass) }
    }

    fun getAnnotation(ann: Class<out Annotation>): CritterAnnotation? {
        return annotations.firstOrNull { it.matches(ann) }
    }

    fun mappedName(): String {
        return if (hasAnnotation(Id::class.java)) {
            "\"_id\""
        } else {
            getAnnotation(Embedded::class.java)?.value()
                ?: getAnnotation(Property::class.java)?.value()
                ?: "\"$name\""
        }
    }
    override fun toString(): String {
        return "CritterProperty(name='$name', type=$type, annotations=$annotations, isFinal=$isFinal, " +
            "stringLiteralInitializer=$stringLiteralInitializer, accessor=$accessor, mutator=$mutator)"
    }
}
