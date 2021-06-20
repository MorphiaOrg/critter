package dev.morphia.critter.kotlin

import dev.morphia.critter.CritterContext
import java.io.File

@Suppress("UNCHECKED_CAST")
class KotlinContext(criteriaPkg: String? = null, force: Boolean = false, outputDirectory: File)
    : CritterContext<KotlinClass>(criteriaPkg, force, outputDirectory)
