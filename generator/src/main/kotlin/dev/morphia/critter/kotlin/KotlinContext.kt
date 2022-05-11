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
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException

@Suppress("UNCHECKED_CAST")
class KotlinContext(
    criteriaPkg: String? = null,
    force: Boolean = false,
    format: Boolean = false,
    outputDirectory: File,
    resourceOutput: File
)
    : CritterContext<KotlinClass, TypeSpec>(criteriaPkg, force, format, outputDirectory, resourceOutput) {

    companion object {
        private val LOG = LoggerFactory.getLogger(KotlinContext::class.java)
/*
        val ruleSets: List<RuleSet> by lazy {
            ServiceLoader.load(RuleSetProvider::class.java).map<RuleSetProvider, RuleSet> { it.get() }
                .sortedWith(Comparator.comparingInt<RuleSet> { if (it.id == "standard") 0 else 1 }.thenComparing(RuleSet::id))
        }
*/
    }

    override fun add(file: File) {
        if(!file.exists()) throw FileNotFoundException(file.absolutePath)
        println("file.absolutePath = ${file.absolutePath}")
        val fileSpec = Kibble.parse(file.absolutePath)
        fileSpec.classes.forEach {
            if (!it.isAnnotation && !it.isEnum) {
                with(KotlinClass(this, fileSpec, it, file)) {
                    add("${pkgName}.${name}", this)
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
        val packageName = CodecsBuilder.packageName
        val builder = FileSpec
            .builder(packageName, "${typeSpec.name}")
            .addType(typeSpec)

        staticImports.forEach {
            builder.addImport(it.first, it.second)
        }

        val file = builder
            .build()
        file.writeTo(outputDirectory)

        if (format) {
            val pkgDir = File(outputDirectory, packageName.replace('.', '/'))
            formatSource(File(pkgDir, typeSpec.name + ".kt"))
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun formatSource(sourceFile: File) {
/*
        val cb: (LintError, Boolean) -> Unit = { (line, col, ruleId, detail), corrected ->
            if (!corrected) {
                LOG.debug("Could not correct formatting error: ($line:$col) [$ruleId] $sourceFile: $detail")
            }
        }
        LOG.debug("Formatting generated file: $sourceFile")
        sourceFile.writeText(KtLint.format(sourceFile.readText(), ruleSets, mapOf(), cb))
*/
    }
}

fun CritterType.typeName(): TypeName {
    val typeName: ClassName = name.className()

    return if(typeParameters.isEmpty()) typeName else
        typeName.parameterizedBy(typeParameters.map { it.typeName() })
}