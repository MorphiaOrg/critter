@file:Suppress("DEPRECATION")
package dev.morphia.critter.java

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Entity
import dev.morphia.critter.CritterContext
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.io.File
import java.io.FileNotFoundException

@Suppress("UNCHECKED_CAST")
class JavaContext(criteriaPkg: String? = null, force: Boolean = false, format: Boolean = true,
                  sourceOutputDirectory: File = File("target/generated-sources/critter"),
                  resourceOutputDirectory: File = File("target/generated-resources/critter")):
    CritterContext<JavaClass, TypeSpec>(criteriaPkg, force, format, sourceOutputDirectory, resourceOutputDirectory) {
    override fun scan(directory: File) {
        if (!directory.exists()) {
            throw FileNotFoundException(directory.toString())
        }
        directory
            .walkTopDown()
            .filter { it.name.endsWith(".java") }
            .map { it to Roaster.parse(it) }
            .filter { it.second is JavaClassSource }
            .forEach { add(it.first, it.second as JavaClassSource) }
    }

    private fun add(file: File, type: JavaClassSource) {
        val klass = JavaClass(this, file, type)
        add("${klass.pkgName}.${klass.name}", klass)
    }

    override fun entities(): Map<String, JavaClass> {
        val map = classes
            .filter {
                it.value.hasAnnotation(Entity::class.java)
                    || it.value.hasAnnotation(Embedded::class.java)
            } /*+ classes
            .values
            .map { loadParent(it.superClass) }
            .flatten()
            .toMap()*/

        return map
    }

    private fun loadParent(type: JavaClass?): List<Pair<String, JavaClass>> {
        return if(type != null) {
            listOf(type.name to type) + loadParent(type.superClass)
        } else {
            listOf()
        }
    }

    override fun buildFile(typeSpec: TypeSpec, vararg staticImports: Pair<Class<*>, String>) {
        val packageName = "dev.morphia.critter.codecs"
        val builder = JavaFile
            .builder(packageName, typeSpec)
        staticImports.forEach {
            builder.addStaticImport(it.first, it.second)
        }

        builder
            .indent("")
            .build()
            .writeTo(outputDirectory)

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
