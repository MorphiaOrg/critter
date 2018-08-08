package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterContext
import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleFile
import com.antwerkz.kibble.model.KibbleFunction
import com.antwerkz.kibble.model.KibbleObject
import com.mongodb.WriteConcern
import org.bson.types.ObjectId
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File

class KotlinClassTest {
    var critterContext= CritterContext(force = true)
    val directory = File("target/kotlinClassTest/")

    @Test
    fun build() {
        val files = Kibble.parse(listOf(File("../tests/kotlin/src/main/kotlin/")))
        files.forEach { file ->
            file.classes.forEach { klass ->
                critterContext.add(KotlinClass(file.pkgName, klass.name, klass, file.file))
            }
        }

        KotlinBuilder(critterContext).build(directory)

        val personClass = critterContext.resolve("com.antwerkz.critter.test", "Person")
        Assert.assertNotNull(personClass)
        personClass as KotlinClass
        Assert.assertEquals(personClass.fields.size, 5, "Found: \n${personClass.fields.joinToString(",\n")}")

        val criteriaFiles = Kibble.parse(listOf(directory))
        validatePersonCriteria(criteriaFiles.find { it.name == "PersonCriteria.kt" }!!)
        validateAddressCriteria(criteriaFiles.find { it.name == "AddressCriteria.kt" }!!)
        validateInvoiceCriteria(criteriaFiles.find { it.name == "InvoiceCriteria.kt" }!!)
    }

    @Test
    fun parentProperties() {
        val file = Kibble.parseSource("""package properties

class Parent(val name: String)

class Child(val age: Int, name: String, val nickNames: List<String>): Parent(name)
""")

        val context = CritterContext(force = true)
        file.classes.forEach { klass ->
            context.add(KotlinClass(file.pkgName, klass.name, klass, file.file))
        }

        val parent = context.resolve("properties", "Parent")!! as KotlinClass
        val child = context.resolve("properties", "Child")!! as KotlinClass
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

    private fun validateInvoiceCriteria(file: KibbleFile) {
        val invoiceCriteria = file.classes[0]
        val addresses = invoiceCriteria.getFunctions("addresses")[0]
        Assert.assertEquals(addresses.type?.toString(), "AddressCriteria")
        shouldImport(file, "com.antwerkz.critter.test.Address")
        shouldImport(file, "com.antwerkz.critter.test.Invoice")
        shouldImport(file, "com.antwerkz.critter.test.Item")
        shouldImport(file, "com.antwerkz.critter.test.Person")
    }

    private fun validateAddressCriteria(file: KibbleFile) {
        shouldNotImport(file, "com.antwerkz.critter.test.AbstractPerson")
        shouldImport(file, "com.antwerkz.critter.test.Address")
    }

    private fun validatePersonCriteria(file: KibbleFile) {
        shouldImport(file, ObjectId::class.java.name)
        shouldNotImport(file, "com.antwerkz.critter.test.AbstractPerson")
        shouldImport(file, "com.antwerkz.critter.test.Person")
        shouldImport(file, "com.antwerkz.critter.test.SSN")

        val personCriteria = file.classes[0]
        val companion = personCriteria.companion() as KibbleObject
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

        val updater = personCriteria.getClass("PersonUpdater")

        validatePersonUpdater(updater)
    }

    private fun validatePersonUpdater(updater: KibbleClass) {
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

    private fun shouldImport(kibble: KibbleFile, type: String?) {
        Assert.assertNotNull(kibble.imports.firstOrNull {
            it.type.fqcn() == type
        }, "Should find an import for $type in ${kibble.name}: ${kibble.imports}")
    }

    private fun shouldNotImport(kibble: KibbleFile, type: String?) {
        Assert.assertNull(kibble.imports.firstOrNull { it.type.fqcn() == type }, "Should not find an import for $type " +
                "in ${kibble.name}")
    }

    private fun check(function: KibbleFunction, parameters: List<Pair<String, String>>, type: String) {
        Assert.assertEquals(function.parameters.size, parameters.size)
        Assert.assertEquals(function.type.toString(), type)
        
        parameters.forEachIndexed { p, (first, second) ->
            val functionParam = function.parameters[p]
            Assert.assertEquals(functionParam.name, first)
            Assert.assertEquals(functionParam.type?.toString(), second)
        }
    }
}