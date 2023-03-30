package dev.morphia.critter.kotlin

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Dependencies.Companion.ALL_FILES
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind.CLASS
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Entity
import dev.morphia.critter.CritterConfig
import java.io.File

class CritterProcessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private var config = CritterConfig()

    init {
        val options = environment.options
        config.outputDirectory = File(".")
        config.resourceOutput = File(".")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotated = resolver.getNewFiles()
            .flatMap {
                it.declarations.filterIsInstance<KSClassDeclaration>()
                    .filter { klass -> klass.classKind == CLASS }
            }
            .filter { declaration ->
                declaration.annotations.any { annotation ->
                    annotation.className() == Entity::class.java.name
                        || annotation.className() == Embedded::class.java.name
                }
            }
            .toList()

//        annotated.map { it.parent }.filterIsInstance<KSFile>().forEach { println(it) }

        if (annotated.isNotEmpty())
            KotlinContext(config, environment, ALL_FILES).scan(annotated)

        return listOf()
    }

    private fun KSAnnotation.className() =
        annotationType.resolve().declaration.qualifiedName?.asString()
}


