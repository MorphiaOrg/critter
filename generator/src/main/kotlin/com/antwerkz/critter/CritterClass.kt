package com.antwerkz.critter

import com.antwerkz.critter.Visibility.PUBLIC
import org.mongodb.morphia.annotations.Embedded

abstract class CritterClass(var pkgName: String? = null, var name: String) : AnnotationHolder, Visible {
    lateinit var context: CritterContext
    val qualifiedName: String by lazy {
        pkgName?.let { "${pkgName}.${name}" } ?: name
    }

    abstract val fields: List<CritterField>

    val isEmbedded: Boolean by lazy {
        hasAnnotation(Embedded::class.java)
    }

    override var visibility: Visibility = PUBLIC

    abstract fun isAbstract(): Boolean

    open fun isEnum() = false

    abstract fun lastModified(): Long
}