package dev.morphia.critter

import java.io.File
import java.io.FileNotFoundException

abstract class CritterContext<C : CritterClass, T>(
    val criteriaPkg: String?,
    var force: Boolean = false,
    var format: Boolean = false,
    val outputDirectory: File,
    val resourceOutput: File
) {
    protected val classes: MutableMap<String, C> = mutableMapOf()

    abstract fun add(file: File)

    protected fun add(name: String, type: C) {
        classes[name] = type
    }
    abstract fun entities(): Map<String, C>

    open fun shouldGenerate(sourceTimestamp: Long?, outputTimestamp: Long?): Boolean {
        return force || sourceTimestamp == null || outputTimestamp == null || sourceTimestamp >= outputTimestamp
    }

    open fun resolve(currentPkg: String? = null, name: String): C? {
        return classes[name] ?: currentPkg?.let { classes["$currentPkg.$name"] }
    }

    open fun resolveFile(name: String): File? {
        return classes[name]?.file
    }

    abstract fun buildFile(typeSpec: T, vararg staticImports: Pair<Class<*>, String>)
    fun generateServiceLoader(model: Class<*>, impl: String) {
        val serviceFile = File(resourceOutput.canonicalFile, "META-INF/services/${model.name}")
        val parentFile = serviceFile.parentFile.canonicalFile
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw FileNotFoundException("could not create ${parentFile.absolutePath}")
        }
        serviceFile.writeText(impl + "\n")
    }
}
