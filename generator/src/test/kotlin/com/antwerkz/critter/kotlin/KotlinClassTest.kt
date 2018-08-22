package com.antwerkz.critter.kotlin

import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.classes
import com.antwerkz.kibble.companion
import com.antwerkz.kibble.functions
import com.antwerkz.kibble.getClass
import com.antwerkz.kibble.getFunctions
import com.antwerkz.kibble.properties
import com.mongodb.WriteConcern
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File

class KotlinClassTest {
    private var context= KotlinContext(force = true)
    private val directory = File("target/kotlinClassTest/")

    @Test(enabled = false)
    fun build() {
        val files = File("../tests/kotlin/src/main/kotlin/").walkTopDown().iterator().asSequence().toList()
        files.forEach {
            Kibble.parse(listOf(it)).forEach { file ->
                file.classes.forEach { klass ->
                    context.add(KotlinClass(context, file, klass, it))
                }
            }

        }

        KotlinBuilder(context).build(directory)

        val personClass = context.resolve("com.antwerkz.critter.test", "Person")
        Assert.assertNotNull(personClass)
        personClass as KotlinClass
        Assert.assertEquals(personClass.fields.size, 5, "Found: \n${personClass.fields.joinToString(",\n")}")

        val criteriaFiles = Kibble.parse(listOf(directory))
        validatePersonCriteria(criteriaFiles.find { it.name == "PersonCriteria.kt" }!!)
        validateInvoiceCriteria(criteriaFiles.find { it.name == "InvoiceCriteria.kt" }!!)
    }

    @Test
    fun parentProperties() {
        val file = Kibble.parseSource("""package properties

class Parent(val name: String)

class Child(val age: Int, name: String, val nickNames: List<String>): Parent(name)
""")

        val context = KotlinContext(force = true)
        file.classes.forEach { klass ->
            context.add(KotlinClass(context, file, klass, File("")))
        }

        val parent = context.resolve("properties", "Parent")!!
        val child = context.resolve("properties", "Child")!!
        Assert.assertEquals(parent.fields.size, 1, "Found: \n${parent.fields.joinToString(",\n")}")
        Assert.assertEquals(child.fields.size, 3, "Found: \n${child.fields.joinToString(",\n")}")

        val builder = KotlinBuilder(context)

        val directory = File("target/properties/")
        builder.build(directory)
        val criteria = Kibble.parse(listOf(directory))
                .flatMap { it.classes }
                .associateBy { it.name }
        val updater = criteria["ChildCriteria"]!!.classes.first { it.name == "ChildUpdater" }
        Assert.assertNotNull(updater.functions.firstOrNull { it.name == "incAge" })
        Assert.assertNotNull(updater.functions.firstOrNull { it.name == "addToNickNames" })
    }

    private fun validateInvoiceCriteria(file: FileSpec) {
        val invoiceCriteria = file.classes[0]
        val addresses = invoiceCriteria.getFunctions("addresses")[0]
        Assert.assertEquals(addresses.returnType?.toString(), "AddressCriteria")
    }

    private fun validatePersonCriteria(file: FileSpec) {
        val personCriteria = file.classes[0]
        val companion = personCriteria.companion() as TypeSpec
        Assert.assertEquals(companion.properties.size, 5)
        val sorted = companion.properties
                .map { it.name }
                .sorted()
        Assert.assertEquals(sorted, listOf("age", "first", "id", "last", "ssn"))
        listOf("age", "first", "id", "last", "ssn").forEach {
            val functions = personCriteria.getFunctions(it)
            Assert.assertEquals(functions.size, 2)
            Assert.assertEquals(functions[0].parameters.size, 0)
            Assert.assertEquals(functions[1].parameters.size, 1)
        }

        val updater = personCriteria.getClass("PersonUpdater")!!

        validatePersonUpdater(updater)
    }

    private fun validatePersonUpdater(updater: TypeSpec) {
        var functions = updater.getFunctions("updateAll")
        check(functions[0], listOf("wc" to WriteConcern::class.java.name), "UpdateResults")

        functions = updater.getFunctions("updateFirst")
        check(functions[0], listOf("wc" to WriteConcern::class.java.name), "UpdateResults")

        functions = updater.getFunctions("upsert")
        check(functions[0], listOf("wc" to WriteConcern::class.java.name), "UpdateResults")

        functions = updater.getFunctions("remove")
        check(functions[0], listOf("wc" to WriteConcern::class.java.name), "WriteResult")

        functions = updater.getFunctions("age")
        Assert.assertEquals(1, functions.size)
        check(functions[0], listOf("__newValue" to "Long"), "PersonUpdater")

        functions = updater.getFunctions("unsetAge")
        Assert.assertEquals(functions.size, 1)
        check(functions[0], listOf(), "PersonUpdater")

        functions = updater.getFunctions("incAge")
        Assert.assertEquals(functions.size, 1)
        check(functions[0], listOf("__newValue" to "Long"), "PersonUpdater")

        listOf("first", "last").forEach {
            functions = updater.getFunctions(it)
            Assert.assertEquals(1, functions.size, "Should have found $it")
            check(functions[0], listOf("__newValue" to "String?"), "PersonUpdater")

            functions = updater.getFunctions("unset${it.capitalize()}")
            Assert.assertEquals(1, functions.size, "Should have found unset${it.capitalize()}")
            check(functions[0], listOf(), "PersonUpdater")
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