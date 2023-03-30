package dev.morphia.critter

import java.io.File

class CritterConfig {
    val criteriaPkg: String? = null
    var force: Boolean = false
    var format: Boolean = true
    lateinit var outputDirectory: File
    lateinit var resourceOutput: File
}