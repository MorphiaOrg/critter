package com.antwerkz.critter

import org.jboss.forge.roaster.model.Visibility
import org.jboss.forge.roaster.model.Visibility.PUBLIC
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Id
import dev.morphia.annotations.Property

class CritterField(val name: String, val type: String) {
    companion object {
        val NUMERIC_TYPES = listOf("Float", "Double", "Long", "Integer", "Byte", "Short", "Number").map {
            listOf(it, "$it?", "java.lang.$it", "java.lang.$it?", "kotlin.$it", "kotlin.$it?")
        }.flatMap { it } + "Int"

        val CONTAINER_TYPES = listOf("List", "Set").map { listOf(it, "java.util.$it", "Mutable$it") }.flatMap { it }
    }

    val shortParameterTypes = mutableListOf<String>()

    val fullParameterTypes = mutableListOf<String>()

    val annotations = mutableListOf<CritterAnnotation>()

    var isStatic = false

    var isFinal = false

    var stringLiteralInitializer: String? = null

    var visibility: Visibility = PUBLIC

    var parameterizedType = type

    fun isContainer() = type.substringBefore("<") in CritterField.CONTAINER_TYPES

    fun isNumeric() = CritterField.NUMERIC_TYPES.contains(type)

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
    return substring(0, 1).toUpperCase() + substring(1)
}
