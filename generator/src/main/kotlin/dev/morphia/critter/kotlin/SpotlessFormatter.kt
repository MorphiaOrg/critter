package dev.morphia.critter.kotlin

import java.io.File

class SpotlessFormatter(formatterFactory: FormatterFactory) {
    val formatter = formatterFactory.newFormatter()
    fun format(file: File) {
        val compute = formatter.compute(file.readText(), file)
        val text = formatter.computeLineEndings(compute, file)
        file.writeText(text)
    }
}