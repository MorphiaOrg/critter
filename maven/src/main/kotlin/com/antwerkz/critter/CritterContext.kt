package com.antwerkz.critter

import org.jboss.forge.roaster.model.source.JavaClassSource
import java.lang.String.format
import java.util.HashMap

class CritterContext(private val criteriaPkg: String?, val isForce: Boolean) {
    val classes = HashMap<String, CritterClass>()

    fun add(pkgName: String?, critterClass: CritterClass) {
        classes.put(format("%s.%s", pkgName, critterClass.getName()), critterClass)
        if (criteriaPkg != null) {
            critterClass.setPackage(criteriaPkg)
        }
    }

    operator fun get(name: String?): CritterClass? {
        return classes[name]
    }

    fun isEmbedded(name: String?): Boolean {
        return get(name)?.isEmbedded ?: false
    }
}
