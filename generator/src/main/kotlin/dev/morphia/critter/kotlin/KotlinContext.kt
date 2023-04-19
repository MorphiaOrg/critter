package dev.morphia.critter.kotlin

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Entity
import dev.morphia.critter.CritterConfig
import dev.morphia.critter.CritterContext
import dev.morphia.critter.kotlin.extensions.className

@Suppress("UNCHECKED_CAST")
class KotlinContext(config: CritterConfig, environment: SymbolProcessorEnvironment, val dependencies: Dependencies)
    : CritterContext<KSClassDeclaration, TypeSpec>(config) {

    companion object {
        val ENTITY_MAPPINGS = listOf(Entity::class.java.name, Embedded::class.java.name)
    }

    val codeGenerator = environment.codeGenerator

    val formatter = SpotlessFormatter(Kotlin())
    private lateinit var entities: Map<String, KSClassDeclaration>

    fun scan(list: List<KSClassDeclaration>) {

        classes += list.map { it.qualifiedName!!.asString() to it }.toMap()

        //        annotated.map { it.parent }.filterIsInstance<KSFile>().forEach { println(it) }
        entities = classes.values
            .filter { !it.isAbstract() && hasMappingAnnotation(it) }
            .associateBy { it.qualifiedName!!.asString() }

        if (entities.isNotEmpty()) {
            KotlinCriteriaBuilder(this).build()
            KotlinCodecsBuilder(this).build()
        }
    }

    fun hasMappingAnnotation(klass: KSClassDeclaration?): Boolean {
        return klass != null &&
            (klass.annotations.any { it.annotationType.className() in ENTITY_MAPPINGS } ||
                hasMappingAnnotation(klass.superClass()))
    }

    private fun KSClassDeclaration.superClass(): KSClassDeclaration? {
        return superTypes
            .map { classes[it.resolve().declaration.className()] }
            .filterNotNull()
            .firstOrNull()
    }
    override fun entities(): Map<String, KSClassDeclaration> = entities

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

