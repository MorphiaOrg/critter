package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.kotlin.model.AbstractKotlinPerson
import com.antwerkz.critter.kotlin.model.Address
import com.antwerkz.critter.kotlin.model.Invoice
import com.antwerkz.critter.kotlin.model.Item
import com.antwerkz.critter.kotlin.model.Person
import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleFunction
import com.antwerkz.kibble.model.KibbleObject
import com.antwerkz.kibble.model.KibbleProperty
import org.bson.types.ObjectId
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File

class KotlinClassTest {
    private val properties = mapOf("first" to "\"f\"",
            "last" to "\"last\"",
            "id" to "\"_id\"",
            "age" to "\"age\"")

    @Test
    fun build() {
        val context = CritterContext(force = true)

        var files = Kibble.parse(File("src/test/kotlin/com/antwerkz/critter/kotlin/model"))
        files.forEach { file ->
            file.classes.forEach { klass ->
                context.add(KotlinClass(context, klass))
            }
        }
        val personClass = context.resolve("com.antwerkz.critter.kotlin.model", Person::class.java.name)
        Assert.assertNotNull(personClass)
        personClass as KotlinClass
        Assert.assertEquals(personClass.fields.size, 4)

        val directory = File("target/kotlinClassTest/")
        context.classes.values.forEach {
            it.build(directory)
        }
        files = Kibble.parse(directory)
        validatePersonCriteria(files.find { it.name == "PersonCriteria.kt" }!!.classes[0])
        validateAddressCriteria(files.find { it.name == "AddressCriteria.kt" }!!.classes[0])
        validateInvoiceCriteria(files.find { it.name == "InvoiceCriteria.kt" }!!.classes[0])
    }

    private fun validateInvoiceCriteria(invoiceCriteria: KibbleClass) {
        shouldImport(invoiceCriteria, Address::class.java.name)
        shouldImport(invoiceCriteria, Invoice::class.java.name)
        shouldImport(invoiceCriteria, Item::class.java.name)
        shouldImport(invoiceCriteria, Person::class.java.name)
    }

    private fun validateAddressCriteria(addressCriteria: KibbleClass) {
        shouldNotImport(addressCriteria, AbstractKotlinPerson::class.java.name )
        shouldImport(addressCriteria, Address::class.java.name)
    }

    private fun validatePersonCriteria(personCriteria: KibbleClass) {
        shouldImport(personCriteria, ObjectId::class.java.name)
        shouldNotImport(personCriteria, AbstractKotlinPerson::class.java.name)
        shouldImport(personCriteria, Person::class.java.name )

        val companion = personCriteria.companion() as KibbleObject
        properties.forEach {
            Assert.assertEquals((companion.getProperty(it.key) as KibbleProperty).initializer, it.value)
        }

        properties.forEach {
            val functions = personCriteria.getFunctions(it.key)
            Assert.assertEquals(functions.size, 2)
            Assert.assertEquals(functions[0].parameters.size, 0)
            Assert.assertEquals(functions[1].parameters.size, 1)

        }

        val updater = personCriteria.getClass("PersonUpdater") as KibbleClass

        var functions = updater.getFunctions("query")
        check(functions[0], listOf<Pair<String, String>>(), "Query<Person>")

        functions = updater.getFunctions("updateAll")
        check(functions[0], listOf<Pair<String, String>>(), "org.mongodb.morphia.query.UpdateResults")
        check(functions[1], listOf("wc" to "com.mongodb.WriteConcern"), "org.mongodb.morphia.query.UpdateResults")

        functions = updater.getFunctions("updateFirst")
        check(functions[0], listOf<Pair<String, String>>(), "org.mongodb.morphia.query.UpdateResults")
        check(functions[1], listOf("wc" to "com.mongodb.WriteConcern"), "org.mongodb.morphia.query.UpdateResults")

        functions = updater.getFunctions("upsert")
        check(functions[0], listOf<Pair<String, String>>(), "org.mongodb.morphia.query.UpdateResults")
        check(functions[1], listOf("wc" to "com.mongodb.WriteConcern"), "org.mongodb.morphia.query.UpdateResults")

        functions = updater.getFunctions("remove")
        check(functions[0], listOf<Pair<String, String>>(), "com.mongodb.WriteResult")
        check(functions[1], listOf("wc" to "com.mongodb.WriteConcern"), "com.mongodb.WriteResult")

        functions = updater.getFunctions("age")
        Assert.assertEquals(1, functions.size)
        check(functions[0], listOf("value" to "Long"), "PersonUpdater")

        functions = updater.getFunctions("unsetAge")
        Assert.assertEquals(1, functions.size)
        check(functions[0], listOf<Pair<String, String>>(), "PersonUpdater")

        functions = updater.getFunctions("incAge")
        Assert.assertEquals(2, functions.size)
        check(functions[0], listOf<Pair<String, String>>(), "PersonUpdater")
        check(functions[1], listOf("value" to "Long"), "PersonUpdater")

        functions = updater.getFunctions("decAge")
        Assert.assertEquals(2, functions.size)
        check(functions[0], listOf<Pair<String, String>>(), "PersonUpdater")
        check(functions[1], listOf("value" to "Long"), "PersonUpdater")

        listOf("first", "last").forEach {
            functions = updater.getFunctions(it)
            Assert.assertEquals(1, functions.size, "Should have found $it")
            check(functions[0], listOf("value" to "String"), "PersonUpdater")

            functions = updater.getFunctions("unset${it.capitalize()}")
            Assert.assertEquals(1, functions.size, "Should have found unset${it.capitalize()}")
            check(functions[0], listOf<Pair<String, String>>(), "PersonUpdater")
        }
    }

    private fun shouldImport(kibble: KibbleClass, type: String?) {
        Assert.assertNotNull(kibble.file.imports.firstOrNull { it.type.name == type }, "Should find an import for $type " +
                "in ${kibble.file.name}")
    }

    private fun shouldNotImport(kibble: KibbleClass, type: String?) {
        Assert.assertNull(kibble.file.imports.firstOrNull { it.type.name == type }, "Should not find an import for $type " +
                        "in ${kibble.file.name}")
    }

    private fun check(function: KibbleFunction, parameters: List<Pair<String, String>>, type: String) {
        Assert.assertEquals(parameters.size, function.parameters.size)
        Assert.assertEquals(type, function.type)
        Assert.assertEquals(parameters.size, function.parameters.size)
        parameters.forEachIndexed { p, param ->
            val functionParam = function.parameters[p]
            Assert.assertEquals(param.first, functionParam.name)
            Assert.assertEquals(param.second, functionParam.type?.name)
        }
    }
}