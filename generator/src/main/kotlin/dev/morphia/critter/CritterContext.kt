package dev.morphia.critter

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import dev.morphia.critter.java.CodecsBuilder
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.io.File

abstract class CritterContext<C : CritterClass, T>(
    val criteriaPkg: String?,
    var force: Boolean = false,
    var format: Boolean = false,
    val outputDirectory: File) {
    val classes: MutableMap<String, C> = mutableMapOf()

    open fun shouldGenerate(sourceTimestamp: Long?, outputTimestamp: Long?): Boolean {
        return force || sourceTimestamp == null || outputTimestamp == null || sourceTimestamp >= outputTimestamp
    }

    open fun add(klass: C) {
        classes["${klass.pkgName}.${klass.name}"] = klass
    }

    open fun resolve(currentPkg: String? = null, name: String): C? {
        return classes[name] ?: currentPkg?.let { classes["$currentPkg.$name"] }
    }

    open fun resolveFile(name: String): File? {
        return classes[name]?.file
    }

    abstract fun buildFile(typeSpec: T, vararg staticImports: Pair<Class<*>, String>)
}
