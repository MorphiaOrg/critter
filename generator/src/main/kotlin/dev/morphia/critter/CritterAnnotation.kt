package dev.morphia.critter

import org.jboss.forge.roaster.model.source.AnnotationSource

abstract class CritterAnnotation(val type: CritterType, val values: Map<String, Any> = mapOf()) {
    val klass: Class<*>? by lazy {
        try {
            Class.forName(type.name)
        } catch (ignored: ClassNotFoundException) {
            null
        }
    }

    abstract fun literalValue(name: String): Any?
    abstract fun annotationArrayValue(): List<CritterAnnotation>?
    abstract fun annotationValue(name: String): CritterAnnotation?

    fun value(): Any? = literalValue("value")
    fun valueAsString(): String? = value()?.toString()

    fun matches(aClass: Class<out Annotation>): Boolean {
        return aClass == klass || aClass.name == type.name
    }

    override fun toString(): String {
        return "CritterAnnotation<${type.name.substringAfterLast('.')}>"
    }
}

fun AnnotationSource<*>.toCritter(): CritterAnnotation {
    val ann = this

    return object : CritterAnnotation(CritterType(qualifiedName, nullable = false), values.associate { it.name to it.stringValue }) {
        override fun literalValue(name: String): String = ann.getLiteralValue(name)
        override fun annotationArrayValue(): List<CritterAnnotation>? {
            return ann.annotationArrayValue?.map { it.toCritter() }
        }

        override fun annotationValue(name: String): CritterAnnotation? {
            return ann.getAnnotationValue(name)?.toCritter()
        }
    }
}