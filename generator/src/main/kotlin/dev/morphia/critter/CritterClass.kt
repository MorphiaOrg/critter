package dev.morphia.critter

import java.io.File

open class CritterClass(
    var name: String,
    var pkgName: String? = null,
    val file: File
)
