package dev.morphia.critter

import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.classes
import dev.morphia.critter.java.JavaCriteriaBuilder
import dev.morphia.critter.java.JavaClass
import dev.morphia.critter.java.JavaContext
import dev.morphia.critter.kotlin.KotlinCriteriaBuilder
import dev.morphia.critter.kotlin.KotlinClass
import dev.morphia.critter.kotlin.KotlinContext
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

class CritterContextTest {
    @Test(dataProvider = "forceScenarios")
    fun force(sourceTimestamp: Long?, outputTimestamp: Long?, force: Boolean, result: Boolean) {
        Assert.assertEquals(
            JavaContext(outputDirectory = File.createTempFile("ddd", "ddd"))
                .shouldGenerate(sourceTimestamp, outputTimestamp), result
        )
    }

    @DataProvider(name = "forceScenarios")
    private fun forceScenarios(): Array<Array<out Any?>> {
        return arrayOf(
            arrayOf(null, null, false, true), // both virtual
            arrayOf(100L, null, false, true), // new output
            arrayOf(null, 100L, false, true), // virtual in, existing out
            arrayOf(100L, 100L, false, true), // same ages
            arrayOf(100L, 1000L, false, false), // output is newer
            arrayOf(1000L, 100L, false, true), // input is newer
            arrayOf(null, null, true, true), // both virtual
            arrayOf(100L, null, true, true), // new output
            arrayOf(null, 100L, true, true), // virtual in, existing out
            arrayOf(100L, 100L, true, true), // same ages
            arrayOf(100L, 1000L, true, true), // output is newer
            arrayOf(1000L, 100L, true, true) // input is newer
        )
    }

    @Test
    fun forceJava() {
        val files = File("../tests/maven/java/src/main/java/").walkTopDown().filter { it.name.endsWith(".java") }
        val directory = File("target/javaClassTest/")
        val critterContext = JavaContext(outputDirectory = directory)
        files.forEach { critterContext.add(JavaClass(critterContext, it)) }
        val builder = JavaCriteriaBuilder(critterContext)
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
        val files = File("../tests/maven/kotlin/src/main/kotlin/").walkTopDown().filter { it.name.endsWith(".kt") }.toList()
        val directory = File("target/kotlinClassTest/")
        val context = KotlinContext(force = true, outputDirectory = directory)
        files.forEach { file ->
            Kibble.parse(files).forEach { fileSpec ->
                fileSpec.classes.forEach {
                    context.add(KotlinClass(context, fileSpec, it, file))
                }
            }
        }
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
        Assert.assertFalse(file.readLines().contains("test update"))

        context.force = true
        kotlinBuilder.build()
        Assert.assertFalse(file.readLines().contains("test update"))
    }
}
