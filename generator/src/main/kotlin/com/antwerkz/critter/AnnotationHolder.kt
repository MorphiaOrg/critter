package com.antwerkz.critter

interface AnnotationHolder {
    val annotations: MutableList<CritterAnnotation>

    fun getValue(ann: Class<out Annotation>, defaultValue: String): String {
        return annotations.firstOrNull { it.matches(ann) }
                ?.getValue() ?: defaultValue
    }

    fun hasAnnotation(aClass: Class<out Annotation>): Boolean {
        return annotations.any { it.matches(aClass) }
    }
}
