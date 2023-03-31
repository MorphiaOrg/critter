package dev.morphia.critter.kotlin

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Entity
import dev.morphia.critter.CritterConfig
import dev.morphia.critter.CritterContext
import dev.morphia.critter.kotlin.extensions.className
import org.slf4j.LoggerFactory

@Suppress("UNCHECKED_CAST")
class KotlinContext(config: CritterConfig, environment: SymbolProcessorEnvironment, val dependencies: Dependencies)
    : CritterContext<KSClassDeclaration, TypeSpec>(config) {

    val codeGenerator = environment.codeGenerator

    val formatter = SpotlessFormatter(Kotlin())

    fun scan(classes: List<KSClassDeclaration>) {
        this.classes += classes.map {klass ->
            klass.qualifiedName!!.asString() to klass
        }
/*
        classes.forEach { klass ->
            with(KotlinClass(this, klass, File((klass.location as FileLocation).filePath))) {
                add("${pkgName}.${name}", this)
            }
        }
*/

        KotlinCriteriaBuilder(this).build()
        KotlinCodecsBuilder(this).build()
    }

    override fun entities(): Map<String, KSClassDeclaration> = classes.filter {
        it.value.annotations.any { ann ->
            val className = ann.annotationType.className()
            className == Entity::class.java.name || className == Embedded::class.java.name
        }
    }

    private fun TypeSpec.hasAnnotation(java: Class<*>): Boolean {
        return annotationSpecs.contains(AnnotationSpec.builder(java.asClassName()).build())
    }

    override fun buildFile(typeSpec: TypeSpec, vararg staticImports: Pair<Class<*>, String>) {
        buildFile(buildFileSpec(typeSpec, staticImports))
    }

    override fun generateServiceLoader(model: Class<*>, impl: String) {
        codeGenerator.createNewFileByPath(Dependencies.ALL_FILES, "META-INF/services/${model.name}", "").use {
            val writer = it.writer()
            writer.write(impl + "\n")
            writer.flush()
        }
    }

    fun buildFile(fileSpec: FileSpec) {
/*
        if (format) {
            formatter.format(outputFile)
        }
*/
        fileSpec.writeTo(codeGenerator, Dependencies(true))
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
}

