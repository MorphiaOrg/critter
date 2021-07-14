package dev.morphia.critter.java

import dev.morphia.critter.CritterContext
import java.io.File

@Suppress("UNCHECKED_CAST")
class JavaContext(criteriaPkg: String? = null, force: Boolean = false, format: Boolean = false, outputDirectory: File):
    CritterContext<JavaClass>(
    criteriaPkg,
    force,
    format,
    outputDirectory
)
