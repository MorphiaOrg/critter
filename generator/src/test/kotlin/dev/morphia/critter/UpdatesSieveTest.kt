package dev.morphia.critter

import org.testng.Assert
import org.testng.annotations.Test

class UpdatesSieveTest {
    @Test
    fun methods() {
        UpdateSieve.updates()
            .forEach {
                Assert.assertNotNull(Updates[it.name], it.toString())
            }
    }
}
