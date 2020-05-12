package com.antwerkz.critter

import org.testng.Assert
import org.testng.annotations.Test

class UpdatesSieveTest {
    @Test
    @ExperimentalStdlibApi
    fun methods() {
        UpdateSieve.filters()
                .forEach {
                    Assert.assertNotNull(Updates.get(it.name), it.toString())
                }
    }

}