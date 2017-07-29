package com.antwerkz.critter

interface AnnotationHolder {
    val annotations: List<CritterAnnotation>

    fun hasAnnotation(aClass: Class<out Annotation>): Boolean {
        return annotations.any { it.matches(aClass) }
    }

    fun getValue(ann: Class<out Annotation>, defaultValue: String): String {
        return annotations.firstOrNull { it.matches(ann) }
                ?.getValue() ?: defaultValue
    }
}