package dev.morphia.critter.kotlin

import com.antwerkz.kibble.Kibble
import com.antwerkz.kibble.classes
import com.antwerkz.kibble.companion
import com.antwerkz.kibble.getFunctions
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaClass
import org.testng.Assert.*
import org.testng.annotations.Test
import java.io.File

private const val GENERATED_ROOT = "target/kotlin/testing-generated"

class KotlinClassTest {
    @Test
    fun build() {
        val generatedSources = File("${GENERATED_ROOT}/critter-sources")
        val context = KotlinContext(force = true, sourceOutputDirectory = generatedSources,
            resourceOutputDirectory = File("${GENERATED_ROOT}/critter-resources")
        )
        context.scan(File("../tests/maven/kotlin/src/main/kotlin/"))

        KotlinCriteriaBuilder(context).build()
        val personClass = context.resolve("dev.morphia.critter.test", "Person") as KotlinClass
        assertEquals(personClass.properties.map { it.name }.toSortedSet(), sortedSetOf("age", "first", "id", "last", "ssn"))
        val criteriaFiles = Kibble.parse(listOf(generatedSources))
        validatePersonCriteria(criteriaFiles.first { it.name == "PersonCriteria.kt" })
        validateInvoiceCriteria(criteriaFiles.first { it.name == "InvoiceCriteria.kt" })
    }

    @Test
    fun codecs() {
        val context = KotlinContext(force = true,
            sourceOutputDirectory = File("${GENERATED_ROOT}/codecs-sources"),
            resourceOutputDirectory = File("${GENERATED_ROOT}/codecs-resources"),
            criteriaPkg = "abc.def"
        )

        context.scan(File("../tests/maven/kotlin/src/main/kotlin/"))
        CodecsBuilder(context).build()
        for (name in listOf("Encoder", "Decoder", "InstanceCreator")) {
            assertFilePath(context.outputDirectory, File(context.outputDirectory, "dev/morphia/critter/codecs/Address${name}.kt"))
        }
    }

    @Test
    fun modelImporter() {
        val context = KotlinContext(force = true,
            sourceOutputDirectory = File("${GENERATED_ROOT}/model-importer-source"),
            resourceOutputDirectory = File("${GENERATED_ROOT}/model-importer-resource"),
            criteriaPkg = "abc.def"
        )
        context.scan(File("../tests/maven/kotlin/src/main/kotlin/"))

        ModelImporter(context).build()

        val source = File(context.outputDirectory, "dev/morphia/critter/codecs/CritterModelImporter.kt")
        assertFilePath(context.outputDirectory, source)

        val spi = File(context.resourceOutput, "META-INF/services/${dev.morphia.mapping.EntityModelImporter::class.java.name}")
        assertEquals(spi.readText().trim(), "dev.morphia.critter.codecs.CritterModelImporter")

    }

    private fun validateInvoiceCriteria(file: FileSpec) {
        val invoiceCriteria = file.classes[0]
        val addresses = invoiceCriteria.getFunctions("addresses")[0]
        assertNotNull(addresses)
    }

    private fun validatePersonCriteria(file: FileSpec) {
        val personCriteria = file.classes[0]
        val companion = personCriteria.companion() as TypeSpec
        assertEquals(companion.propertySpecs.size, 6, companion.propertySpecs.toString())
        val sorted = companion.propertySpecs.map { it.name }.sorted()
        assertEquals(sorted, listOf("age", "first", "id", "last", "ssn", "__criteria").sorted(), sorted.toString())
        listOf("age", "first", "id", "last", "ssn").forEach {
            val functions = personCriteria.getFunctions(it)
            assertEquals(functions.size, 1, it)
            assertEquals(functions[0].parameters.size, 0)
        }
    }
}

fun assertFilePath(root: File, target: File) {
    fun find(root: File, name: String) = root
        .walkTopDown()
        .first { it.name == name }

    assertEquals(find(root, target.name), target)
    val packageName = readPackage(target)
    assertEquals(packageName, target.parentFile.relativeTo(root).path.replace(File.separator, "."))
}

fun readPackage(target: File) =
    target.readLines()
        .first()
        .substringAfter(" ")
        .substringBefore(";")
