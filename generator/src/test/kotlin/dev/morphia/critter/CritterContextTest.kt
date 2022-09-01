package dev.morphia.critter

import dev.morphia.critter.java.CriteriaBuilder
import dev.morphia.critter.java.JavaContext
import dev.morphia.critter.kotlin.KotlinContext
import dev.morphia.critter.kotlin.KotlinCriteriaBuilder
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

class CritterContextTest {
    @Test(dataProvider = "forceScenarios")
    fun force(force: Boolean, result: Boolean, sourceTimestamp: Long?, outputTimestamp: Long?) {
        Assert.assertEquals(
            JavaContext(force = force,
                sourceOutputDirectory = File.createTempFile("ddd", "ddd"),
                resourceOutputDirectory = File.createTempFile("eee", "eee"))
                .shouldGenerate(sourceTimestamp, outputTimestamp), result
        )
    }

    @DataProvider(name = "forceScenarios")
    private fun forceScenarios(): Array<Array<out Any?>> {
        return arrayOf(
            arrayOf(false, true, null, null), // both virtual
            arrayOf(false, true, 100L, null), // new output
            arrayOf(false, true, null, 100L), // virtual in, existing out
            arrayOf(false, true, 100L, 100L), // same ages
            arrayOf(false, false, 100L, 1000L), // output is newer
            arrayOf(false, true, 1000L, 100L), // input is newer
            arrayOf(true, true, null, null), // both virtual
            arrayOf(true, true, 100L, null), // new output
            arrayOf(true, true, null, 100L), // virtual in, existing out
            arrayOf(true, true, 100L, 100L), // same ages
            arrayOf(true, true, 100L, 1000L), // output is newer
            arrayOf(true, true, 1000L, 100L) // input is newer
        )
    }

    @Test
    fun forceJava() {
        val directory = File("target/javaClassTest/")
        val critterContext = JavaContext(force = true, sourceOutputDirectory = directory,
            resourceOutputDirectory = File("target/javaClassResource/"))
        critterContext.scan(File("../tests/maven/java/src/main/java/"))

        val builder = CriteriaBuilder(critterContext)
        builder.build()
        val file = File(directory, "dev/morphia/critter/test/criteria/PersonCriteria.java")
        Assert.assertTrue(file.exists())
        Assert.assertFalse(file.readLines().contains("test update"))

        file.writeText("test update")
        Assert.assertTrue(file.readLines().contains("test update"))
        builder.build()
        Assert.assertFalse(file.readLines().contains("test update"))

        critterContext.force = false
        file.writeText("test update")
        Assert.assertTrue(file.readLines().contains("test update"))
        builder.build()
        Assert.assertTrue(file.readLines().contains("test update"))

        critterContext.force = true
        builder.build()
        Assert.assertFalse(file.readLines().contains("test update"))
    }

    @Test
    fun forceKotlin() {
        val directory = File("target/kotlinClassTest/")
        val context = KotlinContext(
            force = true, sourceOutputDirectory = directory,
            resourceOutputDirectory = File("target/kotlinClassResource/")
        )
        context.scan(File("../tests/maven/kotlin/src/main/kotlin/"))

        val kotlinBuilder = KotlinCriteriaBuilder(context)
        kotlinBuilder.build()
        val file = File(directory, "dev/morphia/critter/test/criteria/PersonCriteria.kt")
        Assert.assertTrue(file.exists())
        Assert.assertFalse(file.readLines().contains("test update"))

        file.writeText("test update")
        Assert.assertTrue(file.readLines().contains("test update"))
        kotlinBuilder.build()
        Assert.assertFalse(file.readLines().contains("test update"))

        context.force = false
        file.writeText("test update")
        Assert.assertTrue(file.readLines().contains("test update"))
        kotlinBuilder.build()
        Assert.assertTrue(file.readLines().contains("test update"))

        context.force = true
        kotlinBuilder.build()
        Assert.assertFalse(file.readLines().contains("test update"))
    }
}
