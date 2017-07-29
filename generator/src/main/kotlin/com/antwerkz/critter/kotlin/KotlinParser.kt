package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterClass
import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.KibbleContext
import com.antwerkz.kibble.model.KibbleClass
import java.io.File

class KotlinParser {
    val kibbleContext = KibbleContext()

    fun parse(file: File) : List<CritterClass> {
        return Kibble.parse(file, kibbleContext).classes.map {
            parse(it)
        }
    }

    private fun parse(kibble: KibbleClass): CritterClass {
        return KotlinClass(kibble.pkgName, kibble.name, kibble)
    }
}