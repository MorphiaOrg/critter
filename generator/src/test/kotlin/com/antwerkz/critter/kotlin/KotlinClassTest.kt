package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterKotlinContext
import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleFunction
import com.antwerkz.kibble.model.KibbleObject
import com.antwerkz.kibble.model.KibbleProperty
import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.annotations.Property
import org.testng.Assert
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test
import java.io.File

class KotlinClassTest {
    lateinit var critterContext: CritterKotlinContext
    val files = Kibble.parse(listOf(File("../tests/kotlin/src/main/kotlin/")))
    val directory = File("target/kotlinClassTest/")

    @BeforeTest
    fun scan() {
        critterContext = CritterKotlinContext(force = true)
        files.forEach { file ->
            file.classes.forEach { klass ->
                critterContext.add(KotlinClass(critterContext, klass))
            }
        }
        critterContext.classes.values.forEach {
            it.build(directory)
        }
    }

    @Test
    fun build() {
        val personClass = critterContext.resolve("com.antwerkz.critter.test", "Person")
        Assert.assertNotNull(personClass)
        personClass as KotlinClass
        Assert.assertEquals(personClass.fields.size, 5)

        val criteriaFiles = Kibble.parse(listOf(directory))
        validatePersonCriteria(criteriaFiles.find { it.name == "PersonCriteria.kt" }!!.classes[0],
                critterContext.resolve("com.antwerkz.critter.test", "Person")!!.source)
        validateAddressCriteria(criteriaFiles.find { it.name == "AddressCriteria.kt" }!!.classes[0])
        validateInvoiceCriteria(criteriaFiles.find { it.name == "InvoiceCriteria.kt" }!!.classes[0])
    }

    private fun validateInvoiceCriteria(invoiceCriteria: KibbleClass) {
        shouldImport(invoiceCriteria, "com.antwerkz.critter.test.Address")
        shouldImport(invoiceCriteria, "com.antwerkz.critter.test.Invoice")
        shouldImport(invoiceCriteria, "com.antwerkz.critter.test.Item")
        shouldImport(invoiceCriteria, "com.antwerkz.critter.test.Person")
    }

    private fun validateAddressCriteria(addressCriteria: KibbleClass) {
        shouldNotImport(addressCriteria, "com.antwerkz.critter.test.AbstractPerson")
        shouldImport(addressCriteria, "com.antwerkz.critter.test.Address")
    }

    private fun validatePersonCriteria(personCriteria: KibbleClass, personClass: KibbleClass) {
        shouldImport(personCriteria, ObjectId::class.java.name)
        shouldNotImport(personCriteria, "com.antwerkz.critter.test.AbstractPerson")
        shouldImport(personCriteria, "com.antwerkz.critter.test.Person")
        shouldImport(personCriteria, "com.antwerkz.critter.test.SSN")

        val companion = personCriteria.companion() as KibbleObject
        val properties = findAllProperties(personClass)
        Assert.assertEquals(companion.properties.size, properties.size)
        properties.forEach {
            Assert.assertEquals(
                    (companion.getProperty(it.name) as KibbleProperty).initializer?.replace("\"", ""),
                    extractName(it))
        }

        properties.forEach {
            val functions = personCriteria.getFunctions(it.name)
            Assert.assertEquals(functions.size, 2)
            Assert.assertEquals(functions[0].parameters.size, 0)
            Assert.assertEquals(functions[1].parameters.size, 1)
        }

        val updater = personCriteria.getClass("PersonUpdater") as KibbleClass

        validatePersonUpdater(updater)
    }

    private fun validatePersonUpdater(updater: KibbleClass) {
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
        check(functions[0], listOf("value" to "Long?"), "PersonUpdater")

        functions = updater.getFunctions("unsetAge")
        Assert.assertEquals(1, functions.size)
        check(functions[0], listOf<Pair<String, String>>(), "PersonUpdater")

        functions = updater.getFunctions("incAge")
        Assert.assertEquals(2, functions.size)
        check(functions[0], listOf<Pair<String, String>>(), "PersonUpdater")
        check(functions[1], listOf("value" to "Long?"), "PersonUpdater")

        functions = updater.getFunctions("decAge")
        Assert.assertEquals(2, functions.size)
        check(functions[0], listOf<Pair<String, String>>(), "PersonUpdater")
        check(functions[1], listOf("value" to "Long?"), "PersonUpdater")

        listOf("first", "last").forEach {
            functions = updater.getFunctions(it)
            Assert.assertEquals(1, functions.size, "Should have found $it")
            check(functions[0], listOf("value" to "String?"), "PersonUpdater")

            functions = updater.getFunctions("unset${it.capitalize()}")
            Assert.assertEquals(1, functions.size, "Should have found unset${it.capitalize()}")
            check(functions[0], listOf<Pair<String, String>>(), "PersonUpdater")
        }
    }

    private fun findAllProperties(kotlinClass: KibbleClass): MutableList<KibbleProperty> {
        val list = kotlinClass.properties
        kotlinClass.superType?.let {
            list += findAllProperties((critterContext.resolve(kotlinClass.pkgName ?: "", it.className) as KotlinClass).source)
        }
        kotlinClass.superTypes.forEach {
            critterContext.resolve(kotlinClass.pkgName ?: "", it.className)?.let {
                list += findAllProperties(it.source)
            }
        }
        return list
    }

    private fun extractName(property: KibbleProperty): String {
        return if (property.hasAnnotation(Id::class.java)) {
            "_id"
        } else if (property.hasAnnotation(Property::class.java)) {
            val annotation = property.getAnnotation(Property::class.java)!!
            annotation["value"]?.replace("\"", "") ?: property.name
        } else {
            property.name
        }
    }

    private fun shouldImport(kibble: KibbleClass, type: String?) {
        println("type = ${type}")
        Assert.assertNotNull(kibble.file.imports.firstOrNull {
            println("it = ${it}")
            it.type.fqcn == type
        }, "Should find an import for $type in ${kibble.file.name}")
    }

    private fun shouldNotImport(kibble: KibbleClass, type: String?) {
        Assert.assertNull(kibble.file.imports.firstOrNull { it.type.fqcn == type }, "Should not find an import for $type " +
                "in ${kibble.file.name}")
    }

    private fun check(function: KibbleFunction, parameters: List<Pair<String, String>>, type: String) {
        Assert.assertEquals(function.parameters.size, parameters.size)
        Assert.assertEquals(function.type, type)
        println("function.toSource() = ${function.toSource()}")
        parameters.forEachIndexed { p, (first, second) ->
            val functionParam = function.parameters[p]
            Assert.assertEquals(functionParam.name, first)
            Assert.assertEquals(functionParam.type?.name, second)
        }
    }
}