package dev.morphia.critter.java

import dev.morphia.annotations.Id
import dev.morphia.annotations.Property
import dev.morphia.critter.CritterProperty
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaType
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.testng.Assert
import org.testng.Assert.assertEquals
import org.testng.annotations.Test
import java.io.File

class JavaClassTest {
    @Test
    fun parents() {
        val directory = File("${outputRoot("parents")}/parentTest/")
        val resourceOutput = File("${outputRoot("parents")}/parentTestRes/")
        val context = JavaContext(force = true, sourceOutputDirectory = directory, resourceOutputDirectory = resourceOutput)

        context.add(File("../tests/maven/java/src/main/java/dev/morphia/critter/test/AbstractPerson.java"))
        context.add(File("../tests/maven/java/src/main/java/dev/morphia/critter/test/Person.java"))
        context.add(File("../tests/maven/java/src/main/java/dev/morphia/critter/test/Invoice.java"))
        val personClass = context.resolve("dev.morphia.critter.test", "Person") as JavaClass
        assertEquals(personClass.properties.map { it.name }.toSortedSet(), sortedSetOf("age", "firstName", "id", "lastName", "ssn"))

        CriteriaBuilder(context).build()
        val criteriaFiles = list(directory)

        validatePersonCriteria(personClass, criteriaFiles.first { it.name == "PersonCriteria" } as JavaClassSource)
        validateInvoiceCriteria(criteriaFiles.first { it.name == "InvoiceCriteria" } as JavaClassSource)
    }

    @Test
    fun build() {
        val files = File("../tests/maven/java/src/main/java/").walkTopDown().filter { it.name.endsWith(".java") }
        val directory = File("${outputRoot("build")}/generated-sources/critter")
        val resourceOutput = File("/${outputRoot("build")}/generated-resources/critter")
        val context = JavaContext(sourceOutputDirectory = directory, resourceOutputDirectory = resourceOutput)

        files.forEach { context.add(it) }
        CriteriaBuilder(context).build()
        val personClass = context.resolve("dev.morphia.critter.test", "Person") as JavaClass
        assertEquals(personClass.properties.size, 5)
        val criteriaFiles = list(directory)

        validatePersonCriteria(personClass, criteriaFiles.find { it.name == "PersonCriteria" } as JavaClassSource)
    }

    private fun outputRoot(sub: String) = "target/unittests/${sub}"

    @Test
    fun codecs() {
        val context = JavaContext(
            force = true, format = true,
            sourceOutputDirectory = File("${outputRoot("codecs")}/generated-sources/critter"),
            resourceOutputDirectory = File("${outputRoot("codecs")}/generated-resources/critter")
        )
        File("../tests/maven/java/src/main/java/")
            .walkTopDown()
            .filter { it.name.endsWith(".java") }
            .forEach { context.add(it) }
        CodecsBuilder(context).build()
    }

    @Test
    fun modelImporter() {
        val context = JavaContext(
            force = true, format = true,
            sourceOutputDirectory = File("${outputRoot("modelImporter")}/generated-sources/critter"),
            resourceOutputDirectory = File("${outputRoot("modelImporter")}/generated-resources/critter")
        )
        File("../tests/maven/java/src/main/java/")
            .walkTopDown()
            .filter { it.name.endsWith(".java") }
            .forEach { context.add(it) }
        ModelImporter(context).build()
        val spi = File(context.resourceOutput, "META-INF/services/${dev.morphia.mapping.EntityModelImporter::class.java.name}")
        val source = File(context.outputDirectory, "dev/morphia/critter/codec/CritterModelImporter.java")
        val parse = Roaster.parse(source)
        assertEquals(spi.readText().trim(), parse.qualifiedName)
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
        assertEquals(criteriaFields.size, origFields.size, "Criteria fields: ${criteriaFields.map { it.name }}.\n " +
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
