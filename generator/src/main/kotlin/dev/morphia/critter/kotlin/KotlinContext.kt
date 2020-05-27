package dev.morphia.critter.kotlin

import java.io.File

@Suppress("UNCHECKED_CAST")
class KotlinContext(val criteriaPkg: String? = null, var force: Boolean = false) {
    val classes = mutableMapOf<String, KotlinClass>()

    fun shouldGenerate(source: Long?, output: Long?): Boolean {
        return force || source == null || output == null || output <= source
    }

    fun add(clazz: KotlinClass) {
        clazz.context = this
        classes["${clazz.fileSpec.packageName}.${clazz.name}"] = clazz
    }

    fun resolve(currentPkg: String? = null, name: String): KotlinClass? {
        return classes[name] ?: currentPkg?.let { classes["$currentPkg.$name"] }
    }

    fun resolveFile(name: String): File? {
        return classes[name]?.file
    }
}