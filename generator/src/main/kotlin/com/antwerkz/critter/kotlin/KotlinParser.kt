package com.antwerkz.critter.kotlin

import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.classes
import java.io.File

class KotlinParser(val context: KotlinContext) {
    fun parse(file: File) {
        val fileSpec = Kibble.parse(file.absolutePath)
        return fileSpec.classes.forEach {
            context.add(KotlinClass(context, fileSpec, it, file))
        }
    }
}