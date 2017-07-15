package com.antwerkz.critter

import com.antwerkz.critter.kotlin.KotlinClass
import com.antwerkz.kibble.model.KibbleImport
import java.lang.String.format
import java.util.HashMap

open class CritterContext<T: CritterClass>(val criteriaPkg: String? = null, var force: Boolean = false) {
    val classes = HashMap<String, T>()

    fun add(critterClass: T) {
        classes.put(format("%s.%s", critterClass.getPackage(), critterClass.getName()), critterClass)
    }

    fun resolve(currentPkg: String, name: String): T? {
        return classes[name] ?: classes["$currentPkg.$name"]
    }

    fun isEmbedded(currentPkg: String, name: String): Boolean {
        return resolve(currentPkg, name)?.isEmbedded ?: false
    }
}

class CritterKotlinContext(criteriaPkg: String? = null, force: Boolean): CritterContext<KotlinClass>(criteriaPkg, force) {
}