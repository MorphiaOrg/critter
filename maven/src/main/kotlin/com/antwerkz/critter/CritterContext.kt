package com.antwerkz.critter

import org.jboss.forge.roaster.model.source.JavaClassSource
import java.lang.String.format
import java.util.HashMap

class CritterContext(private val criteriaPkg: String?, val isForce: Boolean) {
    val classes = HashMap<String, CritterClass>()

    fun add(aPackage: String, critterClass: CritterClass) {
        classes.put(format("%s.%s", aPackage, critterClass.name), critterClass)
        if (criteriaPkg != null) {
            critterClass.pkgName = criteriaPkg
        }
    }

    operator fun get(name: String): CritterClass? {
        return classes[name]
    }

    fun isEmbedded(clazz: JavaClassSource): Boolean {
        val critterClass = get(clazz.name)
        return critterClass != null && critterClass.isEmbedded
    }
}
