package com.antwerkz.critter.java

import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import org.bson.types.ObjectId
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaType
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.annotations.Property
import org.testng.Assert
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test
import java.io.File

class JavaClassTest {
    @BeforeTest
    fun scan() {
    }

    @Test
    fun parents() {
        val context = CritterContext(force = true)

        context.add(JavaClass(context, File("../tests/java/src/main/java/com/antwerkz/critter/test/AbstractPerson.java")))
        context.add(JavaClass(context, File("../tests/java/src/main/java/com/antwerkz/critter/test/Person.java")))
        val personClass = context.resolve("com.antwerkz.critter.test", "Person") as JavaClass

        val directory = File("target/parentTest/")

        JavaBuilder(context).build(directory)

        val criteriaFiles = list(directory)

        validatePersonCriteria(personClass, criteriaFiles.find { it.getName() == "PersonCriteria" } as JavaClassSource)
    }

    @Test
    fun build() {
        val files = File("../tests/java/src/main/java/").walkTopDown().filter { it.name.endsWith(".java") }

        val directory = File("target/javaClassTest/")
        val context = CritterContext(force = true)

        files.forEach { context.add(JavaClass(context, it)) }
        JavaBuilder(context).build(directory)

        val personClass = context.resolve("com.antwerkz.critter.test", "Person") as JavaClass
        Assert.assertEquals(personClass.fields.size, 4)

        val criteriaFiles = list(directory)

        validatePersonCriteria(personClass, criteriaFiles.find { it.getName() == "PersonCriteria" } as JavaClassSource)
        validateAddressCriteria(criteriaFiles)
        validateInvoiceCriteria(criteriaFiles)
    }

    private fun list(directory: File): List<JavaType<*>> {
        return directory.walkTopDown().filter { it.name.endsWith(".java") }.map { Roaster.parse(it) }.toList()
    }

    private fun validatePersonCriteria(personClass: JavaClass, personCriteria: JavaClassSource) {

        shouldImport(personCriteria, ObjectId::class.java.name)
        shouldNotImport(personCriteria, "com.antwerkz.critter.test.AbstractPerson")
        shouldImport(personCriteria, "com.antwerkz.critter.test.Person")

        val origFields = personClass.fields
        val criteriaFields = personCriteria.fields.filter { it.isStatic }
        Assert.assertEquals(
                criteriaFields.size, origFields.size, "Criteria fields: $criteriaFields.\n person fields: ${origFields.joinToString("\n")}"
        )
        val names = criteriaFields.map { it.name }.sortedBy { it }
        Assert.assertEquals(names, listOf("age", "first", "last", "objectId"), "Found instead:  $names")
        origFields.forEach {
            val field = personCriteria.getField(it.name)
            val stringInitializer = field.stringInitializer
            Assert.assertEquals(stringInitializer?.replace("\"", ""), extractName(it))
        }

        origFields.forEach { field ->
            val functions = personCriteria.methods.filter { it.name == field.name }
            Assert.assertEquals(functions.size, 2, "Can't find methods named ${field.name}")
            Assert.assertEquals(functions[0].parameters.size, 0)
            Assert.assertEquals(functions[1].parameters.size, 1)
        }

        validatePersonUpdater(personCriteria.getNestedType("PersonUpdater") as JavaClassSource)
    }

    private fun validatePersonUpdater(updater: JavaClassSource) {
        var functions = updater.getMethods("updateAll")
        check(functions[0], listOf(), "UpdateResults")
        check(functions[1], listOf("wc" to "WriteConcern"), "UpdateResults")

        functions = updater.getMethods("updateFirst")
        check(functions[0], listOf(), "UpdateResults")
        check(functions[1], listOf("wc" to "WriteConcern"), "UpdateResults")

        functions = updater.getMethods("upsert")
        check(functions[0], listOf(), "UpdateResults")
        check(functions[1], listOf("wc" to "WriteConcern"), "UpdateResults")

        functions = updater.getMethods("remove")
        check(functions[0], listOf(), "WriteResult")
        check(functions[1], listOf("wc" to "WriteConcern"), "WriteResult")

        functions = updater.getMethods("age")
        Assert.assertEquals(1, functions.size)
        check(functions[0], listOf("__newValue" to "java.lang.Long"), "PersonUpdater")

        functions = updater.getMethods("unsetAge")
        Assert.assertEquals(1, functions.size)
        check(functions[0], listOf(), "PersonUpdater")

        functions = updater.getMethods("incAge")
        Assert.assertEquals(2, functions.size)
        check(functions[0], listOf(), "PersonUpdater")
        check(functions[1], listOf("__newValue" to "java.lang.Long"), "PersonUpdater")

        functions = updater.getMethods("decAge")
        Assert.assertEquals(2, functions.size)
        check(functions[0], listOf(), "PersonUpdater")
        check(functions[1], listOf("__newValue" to "java.lang.Long"), "PersonUpdater")

        listOf("first", "last").forEach {
            functions = updater.getMethods(it)
            Assert.assertEquals(1, functions.size, "Should have found $it")
            check(functions[0], listOf("__newValue" to "java.lang.String"), "PersonUpdater")

            functions = updater.getMethods("unset${it.capitalize()}")
            Assert.assertEquals(1, functions.size, "Should have found unset${it.capitalize()}")
            check(functions[0], listOf(), "PersonUpdater")
        }
    }

    private fun validateAddressCriteria(criteriaFiles: List<JavaType<*>>) {
        val addressCriteria = criteriaFiles.find { it.getName() == "AddressCriteria" } as JavaClassSource
        shouldNotImport(addressCriteria, "com.antwerkz.critter.test.AbstractPerson")
        shouldImport(addressCriteria, "com.antwerkz.critter.test.Address")
    }

    private fun validateInvoiceCriteria(criteriaFiles: List<JavaType<*>>) {
        val invoiceCriteria = criteriaFiles.find { it.getName() == "InvoiceCriteria" } as JavaClassSource

        val addresses = invoiceCriteria.getMethod("addresses")
        Assert.assertEquals(addresses.returnType.toString(), "AddressCriteria")

        shouldImport(invoiceCriteria, "com.antwerkz.critter.test.Address")
        shouldImport(invoiceCriteria, "com.antwerkz.critter.test.Invoice")
        shouldImport(invoiceCriteria, "com.antwerkz.critter.test.Item")
        shouldImport(invoiceCriteria, "com.antwerkz.critter.test.Person")
    }

    private fun check(function: MethodSource<JavaClassSource>, parameters: List<Pair<String, String>>, type: String) {
        Assert.assertEquals(parameters.size, function.parameters.size)
        Assert.assertEquals(type, function.returnType.toString())
        Assert.assertEquals(parameters.size, function.parameters.size)
        parameters.forEachIndexed { p, (first, second) ->
            val functionParam = function.parameters[p]
            Assert.assertEquals(functionParam.name, first)
            Assert.assertEquals(functionParam.type?.name, second)
        }
    }

    private fun shouldImport(javaClass: JavaClassSource, type: String?) {
        Assert.assertNotNull(
                javaClass.imports.firstOrNull { it.qualifiedName == type }, "Should find an import for $type in ${javaClass.name}"
        )
    }

    private fun shouldNotImport(javaClass: JavaClassSource, type: String?) {
        Assert.assertNull(
                javaClass.imports.firstOrNull { it.qualifiedName == type }, "Should not find an import for $type in ${javaClass.name}"
        )
    }

    private fun extractName(property: CritterField): String {
        return if (property.hasAnnotation(Id::class.java)) {
            "_id"
        } else {
            property.getValue(Property::class.java, property.name).replace("\"", "")
        }
    }

    fun JavaClassSource.getMethods(name: String): List<MethodSource<JavaClassSource>> {
        return methods.filter { it.name == name }
    }
}
