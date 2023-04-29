package dev.morphia.critter.kotlin

import java.io.File

class SpotlessFormatter() {
    val formatter = Kotlin().newFormatter()
    fun format(file: File) {
        val compute = formatter.compute(file.readText(), file)
        val text = formatter.computeLineEndings(compute, file)
        file.writeText(text)
    }
}