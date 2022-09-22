package dev.morphia.critter.java

import dev.morphia.annotations.Id
import dev.morphia.annotations.Property
import dev.morphia.critter.CritterProperty
import dev.morphia.critter.kotlin.assertFilePath
import dev.morphia.critter.kotlin.readPackage
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaType
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.testng.Assert
import org.testng.Assert.assertEquals
import org.testng.Assert.fail
import org.testng.annotations.Test
import java.io.File

private const val GENERATED_ROOT = "target/java/testing-generated"

class JavaClassTest {
    @Test
    fun build() {
        val directory = File("${GENERATED_ROOT}/critter-sources")
        val context = JavaContext(force= true, sourceOutputDirectory = directory,
            resourceOutputDirectory = File("${GENERATED_ROOT}/critter-resources")
        )
        context.scan(File("../tests/maven/java/src/main/java/"))

        CriteriaBuilder(context).build()
        val personClass = context.resolve("dev.morphia.critter.test", "Person") as JavaClass
        assertEquals(personClass.properties.map { it.name }.toSortedSet(), sortedSetOf("age", "firstName", "id", "lastName", "ssn"))
        val criteriaFiles = list(directory)

        validatePersonCriteria(personClass, criteriaFiles.find { it.name == "PersonCriteria" } as JavaClassSource)
        validateInvoiceCriteria(criteriaFiles.find { it.name == "InvoiceCriteria" } as JavaClassSource)
    }

    @Test
    fun codecs() {
        val context = JavaContext(criteriaPkg = "abc.def", force = true,
            sourceOutputDirectory = File("${GENERATED_ROOT}/codecs-sources"),
            resourceOutputDirectory = File("${GENERATED_ROOT}/codecs-resources")
        )

        context.scan(File("../tests/maven/java/src/main/java/"))
        CodecsBuilder(context).build()
        for (name in listOf("Encoder", "Decoder", "InstanceCreator")) {
            assertFilePath(context.outputDirectory, File(context.outputDirectory, "dev/morphia/critter/codecs/Address${name}.java"))
        }
    }

    @Test
    fun modelImporter() {
        val context = JavaContext(criteriaPkg = "abc.def",
            force = true,
            sourceOutputDirectory = File("${GENERATED_ROOT}/model-importer-source"),
            resourceOutputDirectory = File("${GENERATED_ROOT}/model-importer-resource"),

        )
        context.scan(File("../tests/maven/java/src/main/java/"))

        ModelImporter(context).build()
        val spi = File(context.resourceOutput, "META-INF/services/${dev.morphia.mapping.EntityModelImporter::class.java.name}")
        val source = File(context.outputDirectory, "dev/morphia/critter/codecs/CritterModelImporter.java")
        assertFilePath(context.outputDirectory, source)
        
        assertEquals(spi.readText().trim(), "dev.morphia.critter.codecs.CritterModelImporter")

    }

    @Test
    fun parents() {
        val directory = File("${GENERATED_ROOT}/parentTest/")
        val resourceOutput = File("${GENERATED_ROOT}/parentTestRes/")
        val context = JavaContext(force = true, sourceOutputDirectory = directory, resourceOutputDirectory = resourceOutput)

        context.scan(File("../tests/maven/java/src/main/java"))
        val personClass = context.resolve("dev.morphia.critter.test", "Person") as JavaClass
        assertEquals(personClass.properties.map { it.name }.toSortedSet(), sortedSetOf("age", "firstName", "id", "lastName", "ssn"))

        CriteriaBuilder(context).build()
        val criteriaFiles = list(directory)

        validatePersonCriteria(personClass, criteriaFiles.first { it.name == "PersonCriteria" } as JavaClassSource)
        validateInvoiceCriteria(criteriaFiles.first { it.name == "InvoiceCriteria" } as JavaClassSource)
    }

    private fun list(directory: File): List<JavaType<*>> {
        return directory.walkTopDown().filter { it.name.endsWith(".java") }.map { Roaster.parse(it) }.toList()
    }

    private fun validateInvoiceCriteria(invoiceCriteria: JavaClassSource) {
        Assert.assertNotNull(invoiceCriteria.getMethod("addresses"))
    }

    private fun validatePersonCriteria(personClass: JavaClass, personCriteria: JavaClassSource) {
        val origFields = personClass.properties
        val criteriaFields = personCriteria.fields.filter { it.isStatic && it.name != "instance" }
        assertEquals(
            criteriaFields.size, origFields.size, "Criteria fields: ${criteriaFields.map { it.name }}.\n " +
                "person fields: ${origFields.map { it.name }}"
        )
        val names = criteriaFields.map { it.name }.sortedBy { it }
        assertEquals(names, listOf("age", "firstName", "id", "lastName", "ssn"), "Found instead:  $names")
        origFields.forEach {
            val field = personCriteria.getField(it.name)
            val stringInitializer = field.stringInitializer
            assertEquals(stringInitializer?.replace("\"", ""), extractName(it))
        }

        origFields.forEach { field ->
            val functions = personCriteria.methods.filter { it.name == field.name }
            assertEquals(functions.size, 1, "Can't find methods named ${field.name}")
            assertEquals(functions[0].parameters.size, 0)
        }
    }

    private fun extractName(property: CritterProperty): String {
        return if (property.hasAnnotation(Id::class.java)) {
            "_id"
        } else {
            (property.getAnnotation(Property::class.java)?.literalValue("value") ?: property.name).replace("\"", "")
        }
    }
}
