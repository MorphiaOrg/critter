package dev.morphia.critter.kotlin

import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.classes
import com.antwerkz.kibble.companion
import com.antwerkz.kibble.getFunctions
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaClass
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File

private const val GENERATED_ROOT = "target/testing-generated"

class KotlinClassTest {
    @Test
    fun build() {
        val generatedSources = File("${GENERATED_ROOT}/critter-sources")
        val context = KotlinContext(force = true, sourceOutputDirectory = generatedSources,
            resourceOutputDirectory = File("${GENERATED_ROOT}/critter-resources")
        )
        context.scan(File("../tests/maven/kotlin/src/main/kotlin/"))

        KotlinCriteriaBuilder(context).build()
        val personClass = context.resolve("dev.morphia.critter.test", "Person")
        Assert.assertNotNull(personClass)
        personClass as KotlinClass
        Assert.assertEquals(personClass.properties.map {  it.name }.toSortedSet(), sortedSetOf("age", "first", "id", "last", "ssn"))
        val criteriaFiles = Kibble.parse(listOf(generatedSources))
        validatePersonCriteria(criteriaFiles.first { it.name == "PersonCriteria.kt" })
        validateInvoiceCriteria(criteriaFiles.first { it.name == "InvoiceCriteria.kt" })
    }

    @Test
    fun codecs() {
        val context = KotlinContext(force = true, sourceOutputDirectory = File("${GENERATED_ROOT}/codecs-sources"),
            resourceOutputDirectory = File("${GENERATED_ROOT}/codecs-resources")
        )
        context.scan(File("../tests/maven/kotlin/src/main/kotlin/"))

        CodecsBuilder(context).build()
    }

    @Test
    fun modelImporter() {
        val context = KotlinContext(force = true,
            sourceOutputDirectory = File("${GENERATED_ROOT}/model-importer-source"),
            resourceOutputDirectory = File("${GENERATED_ROOT}/model-importer-resource")
        )
        context.scan(File("../tests/maven/kotlin/src/main/kotlin/"))
        ModelImporter(context).build()
        val spi = File(context.resourceOutput, "META-INF/services/${dev.morphia.mapping.EntityModelImporter::class.java.name}")
        val source = File(context.outputDirectory, "dev/morphia/critter/codec/CritterModelImporter.kt")
        val parse: JavaClass<*> = Roaster.parse(source) as JavaClass<*>
        Assert.assertEquals(spi.readText().trim(), parse.qualifiedName)
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
