package dev.morphia.critter

import org.testng.Assert
import org.testng.annotations.Test

class FilterSieveTest {
    @Test
    fun methods() {
        FilterSieve.filters()
            .forEach {
                Assert.assertNotNull(Handler.get(it.name), it.toString())
            }
    }
}
