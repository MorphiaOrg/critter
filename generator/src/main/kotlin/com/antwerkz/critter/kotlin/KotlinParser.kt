package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterClass
import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.KibbleContext
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleFile
import java.io.File

class KotlinParser {
    private val kibbleContext = KibbleContext()

    fun parse(file: File) : List<CritterClass> {
        val kibbleFile = Kibble.parse(file.absolutePath, kibbleContext)
        return kibbleFile.classes.map {
            parse(kibbleFile, it)
        }
    }

    private fun parse(kibbleFile: KibbleFile, kibble: KibbleClass): CritterClass {
        return KotlinClass(kibbleFile.pkgName, kibble.name, kibble)
    }
}