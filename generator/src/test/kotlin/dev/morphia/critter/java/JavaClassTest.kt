package dev.morphia.critter.java

import dev.morphia.critter.CritterField
import dev.morphia.annotations.Id
import dev.morphia.annotations.Property
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaType
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.testng.Assert.assertEquals
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test
import java.io.File
import kotlin.test.assertTrue

class JavaClassTest {
    @BeforeTest
    fun scan() {
    }

    @Test
    fun parents() {
        val directory = File("target/parentTest/")
        val context = JavaContext(outputDirectory = directory)

        context.add(JavaClass(context, File("../tests/maven/java/src/main/java/dev/morphia/critter/test/AbstractPerson.java")))
        context.add(JavaClass(context, File("../tests/maven/java/src/main/java/dev/morphia/critter/test/Person.java")))
        val personClass = context.resolve("dev.morphia.critter.test", "Person") as JavaClass


        JavaCriteriaBuilder(context).build()

        val criteriaFiles = list(directory)

        validatePersonCriteria(personClass, criteriaFiles.find { it.getName() == "PersonCriteria" } as JavaClassSource)
    }

    @Test
    fun build() {
        val files = File("../tests/maven/java/src/main/java/").walkTopDown().filter { it.name.endsWith(".java") }

        val directory = File("../tests/maven/java/target/generated-sources/critter")
        val context = JavaContext(outputDirectory = directory)

        files.forEach { context.add(JavaClass(context, it)) }
        JavaCriteriaBuilder(context).build()

        val personClass = context.resolve("dev.morphia.critter.test", "Person") as JavaClass
        assertEquals(personClass.fields.size, 4)

        val criteriaFiles = list(directory)

        validatePersonCriteria(personClass, criteriaFiles.find { it.getName() == "PersonCriteria" } as JavaClassSource)
    }

    private fun list(directory: File): List<JavaType<*>> {
        return directory.walkTopDown().filter { it.name.endsWith(".java") }.map { Roaster.parse(it) }.toList()
    }

    private fun validatePersonCriteria(personClass: JavaClass, personCriteria: JavaClassSource) {
        val origFields = personClass.fields
        val criteriaFields = personCriteria.fields.filter { it.isStatic && it.name != "instance" }
        assertEquals(criteriaFields.size, origFields.size, "Criteria fields: $criteriaFields.\n " +
                "person fields: ${origFields.joinToString("\n")}")
        val names = criteriaFields.map { it.name }.sortedBy { it }
        assertEquals(names, listOf("age", "firstName", "id", "lastName"), "Found instead:  $names")
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

    private fun JavaClassSource.shouldImport(type: String) {
        assertTrue(imports.any { it.qualifiedName == type }, "Should find an import for $type in ${name}")
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
