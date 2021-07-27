package dev.morphia.critter

import org.jboss.forge.roaster.model.source.AnnotationSource

abstract class CritterAnnotation(val name: String, val values: Map<String, Any> = mapOf()) {
    var klass: Class<*>? = null

    init {
        if (name.contains(".")) {
            try {
                klass = Class.forName(name)
            } catch (ignored: ClassNotFoundException) {
            }
        }
    }

    abstract fun literalValue(name: String): String
    abstract fun annotationArrayValue(): List<CritterAnnotation>?
    abstract fun annotationValue(name: String): CritterAnnotation?

    fun value(): String = literalValue("value")

    fun matches(aClass: Class<out Annotation>): Boolean {
        return aClass == klass || aClass.name == name
    }

    override fun toString(): String {
        return "CritterAnnotation<${name.substringAfterLast('.')}>"
    }
}

fun AnnotationSource<*>.toCritter(): CritterAnnotation {
    val ann = this

    return object : CritterAnnotation(qualifiedName, values.associate { it.name to it.stringValue }) {
        override fun literalValue(name: String): String = ann.getLiteralValue(name)
        override fun annotationArrayValue(): List<CritterAnnotation>? {
            return ann.getAnnotationArrayValue()?.map { it.toCritter() }
        }

        override fun annotationValue(name: String): CritterAnnotation? {
            return ann.getAnnotationValue(name)?.toCritter()
        }
    }
}