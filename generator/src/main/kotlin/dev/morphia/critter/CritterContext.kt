@file:Suppress("DEPRECATION")

package dev.morphia.critter

import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Entity
import dev.morphia.critter.java.JavaClass
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.io.File

@Suppress("UNCHECKED_CAST")
class CritterContext(val criteriaPkg: String? = null, var force: Boolean = false) {
    private val classes = mutableMapOf<String, JavaClass>()
    private var total: Map<String, JavaClass>? = null

    fun shouldGenerate(sourceTimestamp: Long?, Timestamp: Long?): Boolean {
        return force || sourceTimestamp == null || Timestamp == null || Timestamp <= sourceTimestamp
    }

    fun add(file: File) {
        total = null
        val type = Roaster.parse(file)
        if (type is JavaClassSource) {
            val klass = JavaClass(this, file, type)
            classes["${klass.pkgName}.${klass.name}"] = klass
        }
    }

    fun resolve(currentPkg: String? = null, name: String): JavaClass? {
        return classes[name] ?: currentPkg?.let { classes["$currentPkg.$name"] }
    }

    fun classes(): Map<String, JavaClass> {
        if (total == null) {
            val filter = classes
                .filter { e -> e.value.hasAnnotation(Entity::class.java) || e.value.hasAnnotation(Embedded::class.java) }
            total = filter + filter
                .values
                .map { loadParent(it.superClass) }
                .flatten()
                .toMap()
        }
        return total!!
    }

    private fun loadParent(type: JavaClass?): List<Pair<String, JavaClass>> {
        return if(type != null) {
            listOf(type.name to type) + loadParent(type.superClass)
        } else {
            listOf()
        }
    }
}
