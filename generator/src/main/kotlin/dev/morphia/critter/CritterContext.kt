package dev.morphia.critter

import java.io.File

open class CritterContext<T : CritterClass>(
    val criteriaPkg: String?,
    var force: Boolean = false,
    val outputDirectory: File) {
    val classes: MutableMap<String, T> = mutableMapOf()

    open fun shouldGenerate(sourceTimestamp: Long?, outputTimestamp: Long?): Boolean {
        return force || sourceTimestamp == null || outputTimestamp == null || sourceTimestamp >= outputTimestamp
    }

    open fun add(klass: T) {
        classes["${klass.pkgName}.${klass.name}"] = klass
    }

    open fun resolve(currentPkg: String? = null, name: String): T? {
        return classes[name] ?: currentPkg?.let { classes["$currentPkg.$name"] }
    }

    open fun resolveFile(name: String): File? {
        return classes[name]?.file
    }
}
