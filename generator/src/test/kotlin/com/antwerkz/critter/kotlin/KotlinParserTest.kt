package com.antwerkz.critter.kotlin

import com.antwerkz.kibble.Kibble
import org.testng.annotations.Test
import java.io.File

class KotlinParserTest {
    @Test
    fun parse() {
        val files = Kibble.parse(listOf(File("../tests/kotlin/src/main/kotlin/Person.kt")))

    }
}