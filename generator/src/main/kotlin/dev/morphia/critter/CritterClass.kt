package dev.morphia.critter

import dev.morphia.annotations.PrePersist
import java.io.File

open class CritterClass(
    var name: String,
    var pkgName: String? = null,
    val file: File
) {
}
