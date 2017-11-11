package com.antwerkz.critter

import com.antwerkz.critter.Visibility.PUBLIC
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.annotations.Property

class CritterField(val name: String, val type: String) : AnnotationHolder,/* Comparable<CritterField>,*/ Visible {
    companion object {
        val NUMERIC_TYPES =
                listOf("Float", "Double", "Long", "Integer", "Byte", "Short", "Number")
                .map { listOf(it, "${it}?", "java.lang.${it}", "java.lang.${it}?", "kotlin.${it}", "kotlin.${it}?") }
                .flatMap { it } + "Int"

        val CONTAINER_TYPES =
                listOf("List", "Set")
                .map { listOf(it, "java.util.$it", "Mutable$it")}
                .flatMap { it }
    }

    val shortParameterTypes = mutableListOf<String>()

    val fullParameterTypes = mutableListOf<String>()

    val parameterTypes = mutableListOf<String>()

    override val annotations = mutableListOf<CritterAnnotation>()

    var isStatic = false

    var isFinal = false

    var stringLiteralInitializer: String? = null

    lateinit var parameterizedType: String
    lateinit var fullyQualifiedType: String
    override var visibility: Visibility = PUBLIC

    fun isContainer() = type.substringBefore("<") in CritterField.CONTAINER_TYPES

    fun isNumeric() = CritterField.NUMERIC_TYPES.contains(type)

    fun mappedName(): String {
        return if (hasAnnotation(Id::class.java)) {
            "\"_id\""
        } else {
            getValue(Embedded::class.java, getValue(Property::class.java, "\"${name}\""))
        }
    }

    override fun toString(): String {
        return "CritterField(name='$name', type='$type', parameterizedType='$parameterizedType', fullyQualifiedType='$fullyQualifiedType')"
    }

}
