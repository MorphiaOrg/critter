package com.antwerkz.critter

import java.io.File

@Suppress("UNCHECKED_CAST")
class CritterContext(val criteriaPkg: String? = null, var force: Boolean = false) {
    val classes = mutableMapOf<String, CritterClass>()

    fun shouldGenerate(source: Long?, output: Long?): Boolean {
        return force || source == null || output == null || output <= source
    }

    fun add(critterClass: CritterClass) {
        critterClass.context = this
        classes["${critterClass.pkgName}.${critterClass.name}"] = critterClass
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

    fun resolve(currentPkg: String? = null, name: String): CritterClass? {
        return classes[name] ?: currentPkg?.let { classes["$currentPkg.$name"] }
    }

    fun resolveFile(name: String): File? {
        return classes[name]?.file
    }
}