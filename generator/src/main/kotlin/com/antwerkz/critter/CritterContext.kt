package com.antwerkz.critter

import com.antwerkz.kibble.model.KibbleClass
import java.io.File
import java.util.HashMap

@Suppress("UNCHECKED_CAST")
class CritterContext(val criteriaPkg: String? = "criteria", var force: Boolean = false) {
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

    fun isEmbedded(currentPkg: String? = null, name: String): Boolean {
        return resolve(currentPkg, name)?.isEmbedded ?: false
    }

    fun resolveFile(resolve: KibbleClass?): File? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}