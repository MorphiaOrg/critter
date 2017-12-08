package com.antwerkz.critter

import java.util.HashMap

@Suppress("UNCHECKED_CAST")
class CritterContext(val criteriaPkg: String? = null, var force: Boolean = false) {
    val classes = HashMap<String, CritterClass>()

    fun shouldGenerate(source: Long?, output: Long?): Boolean {
        return force || source == null || output == null || output <= source
    }

    fun add(critterClass: CritterClass) {
        classes.put("${critterClass.pkgName}.${critterClass.name}", critterClass)
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

    fun isEmbedded(currentPkg: String? = null, name: String): Boolean {
        return resolve(currentPkg, name)?.isEmbedded ?: false
    }
}