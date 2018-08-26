package com.antwerkz.critter

import com.antwerkz.critter.java.JavaClass
import java.io.File

@Suppress("UNCHECKED_CAST")
class CritterContext(val criteriaPkg: String? = null, var force: Boolean = false) {
    val classes = mutableMapOf<String, JavaClass>()

    fun shouldGenerate(source: Long?, output: Long?): Boolean {
        return force || source == null || output == null || output <= source
    }

    fun add(klass: JavaClass) {
        classes["${klass.pkgName}.${klass.name}"] = klass
    }

/*
    fun lookup(name: String): CritterClass? {
        var className: String
        var pkgName: String = null

        if (name.contains(".")) {

        } else {
            className = name
        }

        return resolve(pkgName, className)
    }
*/

    fun resolve(currentPkg: String? = null, name: String): JavaClass? {
        return classes[name] ?: currentPkg?.let { classes["$currentPkg.$name"] }
    }

    fun resolveFile(name: String): File? {
        return classes[name]?.file
    }
}