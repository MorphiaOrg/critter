package com.antwerkz.critter

import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Entity
import java.io.File
import java.lang.String.format
import java.util.logging.Level
import java.util.logging.Logger

abstract class CritterClass(var context: CritterContext): Visible<CritterClass> {
    companion object {
        private val LOG = Logger.getLogger(CritterClass::class.java.name)
    }

    var primaryConstructor: CritterConstructor? = null

    val constructors: List<CritterConstructor> = listOf()

    val qualifiedName: String by lazy {
        "${getPackage()}.${getName()}"
    }

    var fields = mutableListOf<CritterField>()

    var isEmbedded: Boolean = false

    var lastModified: Long = Long.MIN_VALUE

    abstract fun hasAnnotation(aClass: Class<out Annotation>): Boolean

    abstract fun getName(): String
    abstract fun setName(name: String): CritterClass

    abstract fun getPackage(): String?
    abstract fun setPackage(name: String?): CritterClass

    abstract fun getSuperType(): String?
    abstract fun setSuperType(name: String): CritterClass

    open fun build(directory: File) {
        try {
            if (hasAnnotation(Entity::class.java) || hasAnnotation(Embedded::class.java)) {
                buildCriteria(directory)
                buildDescriptor(directory)
            }
        } catch (e: Exception) {
            LOG.log(Level.SEVERE, format("Failed to generate criteria class for %s: %s", getName(), e.message), e)
        }
    }

    abstract fun buildCriteria(directory: File)

    abstract fun buildDescriptor(directory: File)

    abstract fun createClass(pkgName: String? = getPackage(), name: String): CritterClass

    abstract fun toSource(): String

    abstract fun addImport(klass: Class<*>)

    abstract fun addImport(name: String)

    open fun addNestedType(type: CritterClass): CritterClass {
        return this
    }

    abstract fun addField(name : String, type: String): CritterField

    abstract fun addConstructor(): CritterConstructor

    abstract fun addMethod(): CritterMethod
}
