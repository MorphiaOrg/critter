package com.antwerkz.critter

import com.antwerkz.critter.Visibility.PUBLIC
import org.mongodb.morphia.annotations.Embedded

abstract class CritterClass(var pkgName: String?, var name: String) : AnnotationHolder, Visible {
    val qualifiedName: String by lazy {
        pkgName?.let { "${pkgName}.${name}" } ?: name
    }

    abstract val fields: List<CritterField>

    var isEmbedded: Boolean = false

    abstract fun lastModified(): Long

    override var visibility: Visibility = PUBLIC

    val nested = mutableListOf<CritterClass>()

    init {
        isEmbedded = hasAnnotation(Embedded::class.java)
    }
}

class CritterAnnotation(val name: String, val values: Map<String, Any> = mapOf<String, Any>()) {

    constructor(klass: Class<out Annotation>) : this(klass::javaClass.name) {
        this.klass = klass
    }

    var klass: Class<out Annotation>? = null

    fun matches(aClass: Class<out Annotation>): Boolean {
        return aClass == klass || aClass.name == name
    }

    fun getValue(): String? {
        return values["value"] as String?
    }

}
