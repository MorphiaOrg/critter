package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterContext
import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleFunction
import com.antwerkz.kibble.model.KibbleImport
import com.antwerkz.kibble.model.KibbleObject
import com.antwerkz.kibble.model.KibbleProperty
import org.intellij.lang.annotations.Language
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File

class KotlinClassTest {
    private val properties = mapOf("first" to "\"f\"",
            "last" to "\"last\"",
            "objectId" to "\"_id\"",
            "age" to "\"age\"")

    @Test
    fun build() {
        val context = CritterContext(force = true)

        @Language("kotlin")
        val file = Kibble.parseSource("""
package critter.test.source

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.annotations.Id

open class AbstractKotlinPerson {
    var age: Long = 0
}

@Entity
open class Person : AbstractKotlinPerson {
    @Id
    var objectId: ObjectId? = null

    @Property("f")
    var first: String? = null

    var last: String? = null
}

""")
        file.classes.forEach { klass ->
            context.add(KotlinClass(context, klass))
        }
        val personClass = context.resolve("critter.test.source", "critter.test.source.Person")
        Assert.assertNotNull(personClass)
        personClass as KotlinClass
        Assert.assertEquals(personClass.fields.size, 4)

        val directory = File("target")
        personClass.build(directory)
        val files = Kibble.parse(directory)
        val criteria = files[0].classes[0]

        validateImports(files[0].imports)
        validateCompanion(criteria.companion() as KibbleObject)
        validateCriteria(criteria)
        validateUpdater(criteria.getClass("PersonUpdater") as KibbleClass)
    }

    private fun validateImports(imports: Set<KibbleImport>) {
        Assert.assertNotNull(imports.firstOrNull { it.type.name == "org.bson.types.ObjectId" })
        Assert.assertNull(imports.firstOrNull { it.type.name == "critter.test.source.AbstractKotlinPerson" })
        Assert.assertNotNull(imports.firstOrNull { it.type.name == "critter.test.source.Person" })
    }

    private fun validateCompanion(companion: KibbleObject) {
        properties.forEach {
            val property = companion.getProperty(it.key) as KibbleProperty
            Assert.assertEquals(property.initializer, it.value)
        }
    }

    private fun validateCriteria(criteria: KibbleClass) {
        properties.forEach {
            val functions = criteria.getFunctions(it.key)
            Assert.assertEquals(functions.size, 2)
            Assert.assertEquals(functions[0].parameters.size, 0)
            Assert.assertEquals(functions[1].parameters.size, 1)

        }
    }

    private fun validateUpdater(updater: KibbleClass) {
        var functions = updater.getFunctions("query")
        check(functions[0], listOf(), "Query<Person>")

        functions = updater.getFunctions("updateAll")
        check(functions[0], listOf(), "org.mongodb.morphia.query.UpdateResults")
        check(functions[1], listOf("wc" to "com.mongodb.WriteConcern"), "org.mongodb.morphia.query.UpdateResults")

        functions = updater.getFunctions("updateFirst")
        check(functions[0], listOf(), "org.mongodb.morphia.query.UpdateResults")
        check(functions[1], listOf("wc" to "com.mongodb.WriteConcern"), "org.mongodb.morphia.query.UpdateResults")

        functions = updater.getFunctions("upsert")
        check(functions[0], listOf(), "org.mongodb.morphia.query.UpdateResults")
        check(functions[1], listOf("wc" to "com.mongodb.WriteConcern"), "org.mongodb.morphia.query.UpdateResults")

        functions = updater.getFunctions("remove")
        check(functions[0], listOf(), "com.mongodb.WriteResult")
        check(functions[1], listOf("wc" to "com.mongodb.WriteConcern"), "com.mongodb.WriteResult")

        functions = updater.getFunctions("age")
        Assert.assertEquals(1, functions.size)
        check(functions[0], listOf("value" to "Long"), "PersonUpdater")

        functions = updater.getFunctions("unsetAge")
        Assert.assertEquals(1, functions.size)
        check(functions[0], listOf(), "PersonUpdater")

        functions = updater.getFunctions("incAge")
        Assert.assertEquals(2, functions.size)
        check(functions[0], listOf(), "PersonUpdater")
        check(functions[1], listOf("value" to "Long"), "PersonUpdater")

        functions = updater.getFunctions("decAge")
        Assert.assertEquals(2, functions.size)
        check(functions[0], listOf(), "PersonUpdater")
        check(functions[1], listOf("value" to "Long"), "PersonUpdater")

        listOf("first", "last").forEach {
            functions = updater.getFunctions(it)
            Assert.assertEquals(1, functions.size, "Should have found $it")
            check(functions[0], listOf("value" to "String"), "PersonUpdater")

            functions = updater.getFunctions("unset${it.capitalize()}")
            Assert.assertEquals(1, functions.size, "Should have found unset${it.capitalize()}")
            check(functions[0], listOf(), "PersonUpdater")
        }
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