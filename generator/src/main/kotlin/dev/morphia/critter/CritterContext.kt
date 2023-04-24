package dev.morphia.critter

import java.io.File
import java.io.FileNotFoundException

abstract class CritterContext<C, T>(
    val criteriaPkg: String?,
    var format: Boolean,
    val outputDirectory: File,
    val resourceOutput: File,
) {
    constructor(config: CritterConfig) : this(config.criteriaPkg, config.format, config.outputDirectory, config.resourceOutput)

    val classes: MutableMap<String, C> = mutableMapOf()

    protected fun add(name: String, type: C) {
        classes[name] = type
    }

    abstract fun entities(): Map<String, C>

    open fun resolve(currentPkg: String? = null, name: String): C? {
        return classes[name] ?: currentPkg?.let { classes["$currentPkg.$name"] }
    }

    open fun resolveFile(name: String): File? {
        TODO()
//        return classes[name]?.file
    }

    abstract fun buildFile(typeSpec: T, vararg staticImports: Pair<Class<*>, String>)
    open fun generateServiceLoader(model: Class<*>, impl: String) {
        val serviceFile = File(resourceOutput.canonicalFile, "META-INF/services/${model.name}")
        val parentFile = serviceFile.parentFile.canonicalFile
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw FileNotFoundException("could not create ${parentFile.absolutePath}")
        }
        serviceFile.writeText(impl + "\n")
    }

    open fun scan(directory: File) {}
}
