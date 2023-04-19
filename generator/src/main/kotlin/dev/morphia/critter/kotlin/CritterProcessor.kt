package dev.morphia.critter.kotlin

import com.google.devtools.ksp.processing.Dependencies.Companion.ALL_FILES
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind.CLASS
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
        KotlinContext(config, environment, ALL_FILES)
                .scan(resolver.getNewFiles()
                    .flatMap {
                        it.declarations.filterIsInstance<KSClassDeclaration>()
                            .filter { klass -> klass.classKind == CLASS || klass.classKind == INTERFACE }
                    }
                    .toList())

        return listOf()
    }
}


