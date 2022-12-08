package dev.morphia.critter.kotlin

import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.classes
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Entity
import dev.morphia.critter.CritterContext
import dev.morphia.critter.CritterType
import java.io.File
import java.io.FileNotFoundException
import org.slf4j.LoggerFactory

@Suppress("UNCHECKED_CAST")
class KotlinContext(criteriaPkg: String? = null, force: Boolean = false, format: Boolean = true,
                    sourceOutputDirectory: File = File("target/generated-sources/critter"),
                    resourceOutputDirectory: File = File("target/generated-resources/critter"))
    : CritterContext<KotlinClass, TypeSpec>(criteriaPkg, force, format, sourceOutputDirectory, resourceOutputDirectory) {

    companion object {
        private val LOG = LoggerFactory.getLogger(KotlinContext::class.java)
    }

    val formatter = SpotlessFormatter(Kotlin())

    override fun scan(directory: File) {
        if (!directory.exists()) {
            throw FileNotFoundException(directory.toString())
        }
        directory
            .walkTopDown()
            .filter { it.name.endsWith(".kt") }
            .map { it to Kibble.parse(it.absolutePath) }
            .forEach { (file, fileSpec) ->
                fileSpec.classes.forEach {
                    if (!it.isAnnotation && !it.isEnum) {
                        with(KotlinClass(this, fileSpec, it, file)) {
                            add("${pkgName}.${name}", this)
                        }
                    }
                }
            }
    }

    override fun entities(): Map<String, KotlinClass> = classes.filter {
        it.value.annotations.any { ann ->
            ann.type.name == Entity::class.java.name || ann.type.name == Embedded::class.java.name
        }
    }

    private fun TypeSpec.hasAnnotation(java: Class<*>): Boolean {
        return annotationSpecs.contains(AnnotationSpec.builder(java.asClassName()).build())
    }

    override fun buildFile(typeSpec: TypeSpec, vararg staticImports: Pair<Class<*>, String>) {
        buildFile(buildFileSpec(typeSpec, staticImports))
    }

    fun buildFile(file: FileSpec) {
        file.writeTo(outputDirectory)

        if (format) {
            format(file)
        }
    }

    private fun buildFileSpec(typeSpec: TypeSpec, staticImports: Array<out Pair<Class<*>, String>>): FileSpec {
        val packageName = "dev.morphia.critter.codecs"
        val builder = FileSpec
            .builder(packageName, "${typeSpec.name}")
            .addType(typeSpec)

        staticImports.forEach {
            builder.addImport(it.first, it.second)
        }
        return builder.build()
    }

    fun format(typeSpec: FileSpec) {
        val pkgDir = File(outputDirectory, typeSpec.packageName.replace('.', '/'))
        val sourceFile = File(pkgDir, typeSpec.name + ".kt")
        LOG.debug("Formatting generated file: ${sourceFile}")
        formatter.format(sourceFile)
    }
}

fun CritterType.typeName(): TypeName {
    val typeName: ClassName = name.className()

    return if(typeParameters.isEmpty()) typeName else
        typeName.parameterizedBy(typeParameters.map { it.typeName() })
}