package dev.morphia.critter

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import dev.morphia.critter.java.CodecsBuilder
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.io.File

open class CritterContext<T : CritterClass>(
    val criteriaPkg: String?,
    var force: Boolean = false,
    var format: Boolean = false,
    val outputDirectory: File) {
    val classes: MutableMap<String, T> = mutableMapOf()

    fun buildFile(typeSpec: TypeSpec, vararg staticImports: Pair<Class<*>, String>) {
        val packageName = CodecsBuilder.packageName
        val builder = JavaFile
            .builder(packageName, typeSpec)
        staticImports.forEach {
            builder.addStaticImport(it.first, it.second)
        }
        val javaFile = builder.build()
        javaFile.writeTo(outputDirectory)

        if (format) {
            var pkgDir = File(outputDirectory, packageName.replace('.', '/'))
            formatSource(File(pkgDir, typeSpec.name + ".java"))
        }
    }
    private fun formatSource(sourceFile: File) {
        val parsed = Roaster.parse(JavaClassSource::class.java, sourceFile)
        val pathname = sourceFile.path.replace(".java", "2.java")
        File(pathname)
        sourceFile.writeText(parsed.toString())
    }

    open fun shouldGenerate(sourceTimestamp: Long?, outputTimestamp: Long?): Boolean {
        return force || sourceTimestamp == null || outputTimestamp == null || sourceTimestamp >= outputTimestamp
    }

    open fun add(klass: T) {
        classes["${klass.pkgName}.${klass.name}"] = klass
    }

    open fun resolve(currentPkg: String? = null, name: String): T? {
        return classes[name] ?: currentPkg?.let { classes["$currentPkg.$name"] }
    }

    open fun resolveFile(name: String): File? {
        return classes[name]?.file
    }
}
