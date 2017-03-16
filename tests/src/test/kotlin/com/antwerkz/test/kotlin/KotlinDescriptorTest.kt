package com.antwerkz.test.kotlin

import com.antwerkz.critter.test.Address
import com.antwerkz.critter.test.Person
import org.testng.Assert
import org.testng.annotations.Test
import java.lang.String.format

class KotlinDescriptorTest {
    @Test
    fun address() {
        val descriptor = Class.forName("com.antwerkz.critter.test.criteria.AddressDescriptor")

        for (field in Address::class.java.declaredFields) {
            try {
                descriptor.getDeclaredField(field.name)
            } catch (e: NoSuchFieldException) {
                Assert.fail(format("Could not find a field named '%s' on PersonDescriptor", field.name))
            }

        }

        Assert.assertEquals(descriptor.getDeclaredField("city").get(null), "c")
        Assert.assertEquals(descriptor.getDeclaredField("state").get(null), "state")
    }

    @Test
    fun person() {
        val descriptor = Class.forName("com.antwerkz.critter.test.criteria.PersonDescriptor")

        for (field in Person::class.java.declaredFields) {
            try {
                descriptor.getDeclaredField(field.name)
            } catch (e: NoSuchFieldException) {
                Assert.fail(format("Could not find a field named '%s' on PersonDescriptor", field.name))
            }

        }
    }
}
