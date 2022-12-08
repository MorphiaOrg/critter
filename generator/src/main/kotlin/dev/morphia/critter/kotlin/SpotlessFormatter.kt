package dev.morphia.critter.kotlin

import java.io.File

class SpotlessFormatter(formatterFactory: FormatterFactory) {
    val formatter = formatterFactory.newFormatter()
    fun format(file: File) {
        file.writeText(formatter.computeLineEndings(formatter.compute(file.readText(), file), file))
    }
}