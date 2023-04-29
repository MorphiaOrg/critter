package dev.morphia.critter

import java.io.File

class CritterConfig {
    val criteriaPkg: String? = null
    var format: Boolean = true
    lateinit var outputDirectory: File
    lateinit var resourceOutput: File
}