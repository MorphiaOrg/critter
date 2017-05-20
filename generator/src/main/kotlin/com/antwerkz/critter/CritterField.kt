package com.antwerkz.critter

import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.annotations.Property

interface CritterField : Comparable<CritterField>, Visible<CritterField> {
    companion object {

        val NUMERIC_TYPES = listOf("java.lang.Float",
                "java.lang.Double",
                "java.lang.Long",
                "java.lang.Integer",
                "java.lang.Byte",
                "java.lang.Short",
                "java.lang.Number")
    }

    val shortParameterTypes: MutableList<String>

    val fullParameterTypes: MutableList<String>

    val fullType: String

    val name: String

    val parameterTypes: List<String>

    val parameterizedType: String

    val fullyQualifiedType: String


    fun hasAnnotation(aClass: Class<out Annotation>): Boolean

    fun isContainer(): Boolean = fullType == "java.util.List" || fullType == "java.util.Set"

    fun isNumeric(): Boolean = CritterField.NUMERIC_TYPES.contains(fullType)

    fun isStatic(): Boolean
    fun setStatic(): CritterField

    fun setFinal(): CritterField

    fun build(sourceClass: CritterClass, targetClass: CritterClass)

    fun buildReference(criteriaClass: CritterClass)

    fun buildEmbed(criteriaClass: CritterClass)

    fun buildField(critterClass: CritterClass, criteriaClass: CritterClass)

    fun mappedName(): String {
        return if (hasAnnotation(Id::class.java)) {
            "_id"
        } else {
            val fieldName = extract("\"$name\"", Property::class.java)
            extract(fieldName, Embedded::class.java)
        }
    }

    fun extract(name: String, ann: Class<out Annotation>): String

    fun setStringLiteralInitializer(initializer: String): CritterField
    fun setLiteralInitializer(initializer: String): CritterField
}
