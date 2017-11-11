package com.antwerkz.critter

import java.util.HashMap

@Suppress("UNCHECKED_CAST")
class CritterContext(val criteriaPkg: String? = null, var force: Boolean = false) {
    val classes = HashMap<String, CritterClass>()

    fun add(critterClass: CritterClass) {
        classes.put("${critterClass.pkgName}.${critterClass.name}", critterClass)
    }

    fun resolve(currentPkg: String? = null, name: String): CritterClass? {
        return classes[name] ?: currentPkg?.let { classes["$currentPkg.$name"] }
    }

    fun isEmbedded(currentPkg: String? = null, name: String): Boolean {
        return resolve(currentPkg, name)?.isEmbedded ?: false
    }
}