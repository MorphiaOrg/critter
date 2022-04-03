package dev.morphia.critter.test

import dev.morphia.critter.CritterType
import org.testng.Assert
import org.testng.annotations.Test
import java.time.LocalDateTime

class CritterTypeTest {
    @Test
    fun types() {
        Assert.assertTrue(CritterType(LocalDateTime::class.java.name).isNumeric())
        Assert.assertTrue(CritterType(LocalDateTime::class.java.name, nullable = true).isNumeric())
    }
}