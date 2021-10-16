package dev.morphia.critter.kotlin

import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.classes
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import dev.morphia.aggregation.experimental.codecs.ExpressionHelper
import dev.morphia.critter.CritterContext
import dev.morphia.critter.CritterType
import org.slf4j.LoggerFactory
import java.io.File
import java.util.ServiceLoader

@Suppress("UNCHECKED_CAST")
class KotlinContext(criteriaPkg: String? = null, force: Boolean = false, format: Boolean = false, outputDirectory: File)
    : CritterContext<KotlinClass, TypeSpec>(criteriaPkg, force, format, outputDirectory) {

    companion object {
        private val LOG = LoggerFactory.getLogger(KotlinContext::class.java)
/*
        val ruleSets: List<RuleSet> by lazy {
            ServiceLoader.load(RuleSetProvider::class.java).map<RuleSetProvider, RuleSet> { it.get() }
                .sortedWith(Comparator.comparingInt<RuleSet> { if (it.id == "standard") 0 else 1 }.thenComparing(RuleSet::id))
        }
*/
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

    fun parse(file: File) {
        val fileSpec = Kibble.parse(file.absolutePath)
        return fileSpec.classes.forEach {
            add(KotlinClass(this, fileSpec, it, file))
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
    var typeName: ClassName = name.className()

    return if(typeParameters.isEmpty()) typeName else
        typeName.parameterizedBy(typeParameters.map { it.typeName() })
}