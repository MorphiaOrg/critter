package dev.morphia.critter.kotlin

import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.classes
import com.antwerkz.kibble.companion
import com.antwerkz.kibble.getFunctions
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File

class KotlinClassTest {
    private val directory = File("../tests/kotlin/target/generated-sources/critter")
    private var context = KotlinContext(force = true, outputDirectory = directory)

    @Test
    fun build() {
        val files = File("../tests/maven/kotlin/src/main/kotlin/").walkTopDown().iterator().asSequence().toList()
        files.forEach {
            Kibble.parse(listOf(it)).forEach { file ->
                file.classes.forEach { klass ->
                    context.add(KotlinClass(context, file, klass, it))
                }
            }
        }

        KotlinCriteriaBuilder(context).build()
        val personClass = context.resolve("dev.morphia.critter.test", "Person")
        Assert.assertNotNull(personClass)
        personClass as KotlinClass
        Assert.assertEquals(personClass.fields.size, 5, "Found: \n${personClass.fields.joinToString(",\n")}")
        val criteriaFiles = Kibble.parse(listOf(directory))
        validatePersonCriteria(criteriaFiles.find { it.name == "PersonCriteria.kt" }!!)
        validateInvoiceCriteria(criteriaFiles.find { it.name == "InvoiceCriteria.kt" }!!)
    }

    @Test
    fun parentProperties() {
        val file = Kibble.parseSource(
            """package properties

class Parent(val name: String)

class Child(val age: Int, name: String, val nickNames: List<String>): Parent(name)
"""
        )
        val context = KotlinContext(force = true, outputDirectory = directory)
        file.classes.forEach { klass ->
            context.add(KotlinClass(context, file, klass, File("")))
        }
        val parent = context.resolve("properties", "Parent")!!
        val child = context.resolve("properties", "Child")!!
        Assert.assertEquals(parent.fields.size, 1, "Found: \n${parent.fields.joinToString(",\n")}")
        Assert.assertEquals(child.fields.size, 3, "Found: \n${child.fields.joinToString(",\n")}")
        val builder = KotlinCriteriaBuilder(context)
        val directory = File("target/properties/")
        builder.build()
    }

    private fun validateInvoiceCriteria(file: FileSpec) {
        val invoiceCriteria = file.classes[0]
        val addresses = invoiceCriteria.getFunctions("addresses")[0]
        Assert.assertNotNull(addresses)
    }

    private fun validatePersonCriteria(file: FileSpec) {
        val personCriteria = file.classes[0]
        val companion = personCriteria.companion() as TypeSpec
        Assert.assertEquals(companion.propertySpecs.size, 6, companion.propertySpecs.toString())
        val sorted = companion.propertySpecs.map { it.name }.sorted()
        Assert.assertEquals(sorted, listOf("age", "first", "id", "last", "ssn", "__criteria").sorted(), sorted.toString())
        listOf("age", "first", "id", "last", "ssn").forEach {
            val functions = personCriteria.getFunctions(it)
            Assert.assertEquals(functions.size, 1, it)
            Assert.assertEquals(functions[0].parameters.size, 0)
        }
    }

    private fun check(function: FunSpec, parameters: List<Pair<String, String>>, type: String) {
        Assert.assertEquals(function.parameters.size, parameters.size)
        Assert.assertEquals(function.returnType.toString(), type)

        parameters.forEachIndexed { p, (first, second) ->
            val functionParam = function.parameters[p]
            Assert.assertEquals(functionParam.name, first)
            Assert.assertEquals(functionParam.type.toString(), second)
        }
    }
}
