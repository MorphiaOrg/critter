package com.antwerkz.critter

import com.antwerkz.critter.java.JavaClass
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File

class CritterContextTest {
    @Test
    fun force() {
        val files = File("../tests/java/src/main/java/").walkTopDown()
                .filter { it.name.endsWith(".java") }
        val directory = File("target/javaClassTest/")

        val critterContext = CritterContext(force = true)
        files.forEach { critterContext.add(JavaClass(critterContext, it)) }
        critterContext.classes.values.forEach {
            it.build(directory)
        }

        val file = File(directory, "com/antwerkz/critter/test/criteria/PersonCriteria.java")
        Assert.assertTrue(file.exists())
        Assert.assertFalse(file.readLines().contains("test update"))

        file.writeText("test update")
        Assert.assertTrue(file.readLines().contains("test update"))
        critterContext.classes.values.forEach {
            it.build(directory)
        }
        Assert.assertFalse(file.readLines().contains("test update"))

        critterContext.force = false
        file.writeText("test update")
        Assert.assertTrue(file.readLines().contains("test update"))
        critterContext.classes.values.forEach {
            it.build(directory)
        }
        Assert.assertTrue(file.readLines().contains("test update"))

        critterContext.force = true
        critterContext.classes.values.forEach {
            it.build(directory)
        }
        Assert.assertFalse(file.readLines().contains("test update"))
    }
}