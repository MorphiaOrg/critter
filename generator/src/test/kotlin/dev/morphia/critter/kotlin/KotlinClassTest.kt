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
        File("../tests/maven/kotlin/src/main/kotlin/")
            .walkTopDown()
            .forEach {
                context.add(it)
            }

        KotlinCriteriaBuilder(context).build()
        val personClass = context.resolve("dev.morphia.critter.test", "Person")
        Assert.assertNotNull(personClass)
        personClass as KotlinClass
        Assert.assertEquals(personClass.properties.map {  it.name }.toSortedSet(), sortedSetOf("age", "first", "id", "last", "ssn"))
        val criteriaFiles = Kibble.parse(listOf(directory))
        validatePersonCriteria(criteriaFiles.first { it.name == "PersonCriteria.kt" })
        validateInvoiceCriteria(criteriaFiles.first { it.name == "InvoiceCriteria.kt" })
    }

    @Test
    fun codecs() {
        val context = KotlinContext(format = true, force = true, outputDirectory = File("../tests/maven/kotlin/target/generated-sources/critter"))
        File("../tests/maven/kotlin/src/main/kotlin/")
            .walkTopDown()
            .filter { it.name.endsWith(".kt") }
            .forEach { context.add(it) }
        CodecsBuilder(context).build()
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
