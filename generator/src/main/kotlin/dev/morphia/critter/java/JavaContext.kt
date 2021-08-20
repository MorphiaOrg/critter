package dev.morphia.critter.java

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import dev.morphia.critter.CritterContext
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.io.File

@Suppress("UNCHECKED_CAST")
class JavaContext(criteriaPkg: String? = null, force: Boolean = false, format: Boolean = false, outputDirectory: File):
    CritterContext<JavaClass, TypeSpec>(
    criteriaPkg,
    force,
    format,
    outputDirectory
) {
    override fun buildFile(typeSpec: TypeSpec, vararg staticImports: Pair<Class<*>, String>) {
        val packageName = CodecsBuilder.packageName
        val builder = JavaFile
            .builder(packageName, typeSpec)
        staticImports.forEach {
            builder.addStaticImport(it.first, it.second)
        }
        val javaFile = builder.build()
        javaFile.writeTo(outputDirectory)

        if (format) {
            val pkgDir = File(outputDirectory, packageName.replace('.', '/'))
            formatSource(File(pkgDir, typeSpec.name + ".java"))
        }
    }

    private fun formatSource(sourceFile: File) {
        val parsed = Roaster.parse(JavaClassSource::class.java, sourceFile)
        val pathname = sourceFile.path.replace(".java", "2.java")
        File(pathname)
        sourceFile.writeText(parsed.toString())
    }
}
