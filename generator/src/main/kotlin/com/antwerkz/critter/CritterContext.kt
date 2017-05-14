package com.antwerkz.critter

import java.lang.String.format
import java.util.HashMap

class CritterContext(val criteriaPkg: String? = null, val force: Boolean = false) {
    val classes = HashMap<String, CritterClass>()

    fun add(critterClass: CritterClass) {
        classes.put(format("%s.%s", critterClass.getPackage(), critterClass.getName()), critterClass)
    }

    fun resolve(currentPkg: String, name: String): CritterClass? {
        return classes[name] ?: classes["$currentPkg.$name"]
    }

    fun isEmbedded(currentPkg: String, name: String): Boolean {
        return resolve(currentPkg, name)?.isEmbedded ?: false
    }

    operator fun get(name: String): CritterClass? {
        return classes[name]
    }
}
