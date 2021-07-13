package dev.morphia.critter

interface AnnotationHolder {
    val annotations: MutableList<CritterAnnotation>

    fun getValue(ann: Class<out Annotation>, defaultValue: String): String {
        return annotations.firstOrNull { it.matches(ann) }?.getValue() ?: defaultValue
    }

    fun hasAnnotation(aClass: Class<out Annotation>): Boolean {
        return annotations.any { it.matches(aClass) }
    }
}

class CritterAnnotation(val name: String, val values: Map<String, Any> = mapOf()) {

    var klass: Class<out Annotation>? = null

    init {
        if (name.contains(".")) {
            @Suppress("UNCHECKED_CAST") try {
                klass = Class.forName(name) as Class<out Annotation>
            } catch (ignored: ClassNotFoundException) {
            }
        }
    }

    fun matches(aClass: Class<out Annotation>): Boolean {
        return aClass == klass || aClass.name == name
    }

    fun getValue(): String? {
        return values["value"] as String?
    }

    override fun toString(): String {
        return "CritterAnnotation<${name.substringAfterLast('.')}>"
    }
}
