@file:Suppress("DEPRECATION")

package dev.morphia.critter.java

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Entity
import dev.morphia.critter.Critter
import dev.morphia.critter.CritterContext
import java.io.File
import java.io.FileNotFoundException
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaInterfaceSource
import org.jboss.forge.roaster.model.source.JavaSource

class JavaContext(
    criteriaPkg: String? = null, format: Boolean = true,
    sourceOutputDirectory: File = File("target/generated-sources/critter"),
    resourceOutputDirectory: File = File("target/generated-resources/critter"),
) :
    CritterContext<CritterType, TypeSpec>(criteriaPkg, format, sourceOutputDirectory, resourceOutputDirectory) {
    companion object {
        internal val LIST_TYPES = listOf("List", "MutableList").explodeTypes(listOf("java.util"))
        internal val SET_TYPES = listOf("Set", "MutableSet").explodeTypes(listOf("java.util"))
        internal val MAP_TYPES = listOf("Map", "MutableMap").explodeTypes(listOf("java.util"))
        internal val CONTAINER_TYPES = LIST_TYPES + SET_TYPES

        internal val GEO_TYPES = listOf("double[]", "Double[]").explodeTypes()
        internal val NUMERIC_TYPES = listOf("Float", "Double", "Long", "Int", "Integer", "Byte", "Short", "Number").explodeTypes()
        internal val TEXT_TYPES = listOf("String").explodeTypes()

        private fun List<String>.explodeTypes(packages: List<String> = listOf("java.lang")): List<String> {
            return flatMap {
                listOf(it) + packages.map { pkg -> "$pkg.$it"}
            }
        }
    }

    init {
        Critter.javaContext = this
    }

    private var entities: Map<String, CritterType>? = null

    override fun scan(directory: File) {
        if (!directory.exists()) {
            throw FileNotFoundException(directory.toString())
        }
        directory
            .walkTopDown()
            .filter { it.name.endsWith(".java") }
            .map { it to Roaster.parse(it) }
            .filter { it.second is JavaClassSource ||
                it.second is JavaInterfaceSource }
            .forEach {
                val type = CritterType.of(this, it.second as JavaSource<*>)
                add(type.qualifiedName, type)
            }
    }

    override fun entities(): Map<String, CritterType> {
        var map = entities
        if (map == null) {
            map = classes
                .filter {
                    it.value.hasAnnotations(Entity::class.java, Embedded::class.java)
                }
                .toSortedMap()
            entities = map
        }

        return map
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
