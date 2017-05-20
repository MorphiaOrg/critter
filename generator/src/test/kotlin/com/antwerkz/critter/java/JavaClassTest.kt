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
    lateinit var critterContext: CritterContext
    val files = File("../tests/java/src/main/java/").walkTopDown()
            .filter { it.name.endsWith(".java") }

    val directory = File("target/javaClassTest/")

    @BeforeTest
    fun scan() {
        critterContext = CritterContext(force = true)
        files.forEach { critterContext.add(JavaClass(critterContext, it)) }
        critterContext.classes.values.forEach {
            it.build(directory)
        }
    }

    @Test
    fun build() {
        val personClass = critterContext.resolve("com.antwerkz.critter.test", "Person")
        Assert.assertNotNull(personClass)
        personClass as JavaClass
        Assert.assertEquals(personClass.fields.size, 4)

        val criteriaFiles = directory.walkTopDown()
                .filter { it.name.endsWith(".java") }
                .map { Roaster.parse(it) }
                .toList()

        validatePersonCriteria(criteriaFiles)
//        validateAddressCriteria(criteriaFiles.find { it.name == "AddressCriteria.kt" }!!.classes[0])
//        validateInvoiceCriteria(criteriaFiles.find { it.name == "InvoiceCriteria.kt" }!!.classes[0])

    }

    private fun validatePersonCriteria(criteriaFiles: List<JavaType<*>>) {
        val personCriteria = criteriaFiles.find { it.getName() == "PersonCriteria" } as JavaClassSource
        val personClass =(critterContext.resolve("com.antwerkz.critter.test", "Person")!! as JavaClass)

        shouldImport(personCriteria, ObjectId::class.java.name)
        shouldNotImport(personCriteria, "com.antwerkz.critter.test.AbstractPerson")
        shouldImport(personCriteria, "com.antwerkz.critter.test.Person")

        val descriptor = criteriaFiles.find { it.getName() == "PersonDescriptor" } as JavaClassSource
        val fields = personClass.fields
        Assert.assertEquals(descriptor.fields.size, fields.size)
        fields.forEach {
            val field = descriptor.getField(it.name)
            val stringInitializer = field.stringInitializer
            Assert.assertEquals(
                    stringInitializer?.replace("\"", ""),
                    extractName(it as JavaField))
        }

        fields.forEach { field ->
            val functions = personCriteria.methods.filter { it.name == field.name}
            Assert.assertEquals(functions.size, 2)
            Assert.assertEquals(functions[0].parameters.size, 0)
            Assert.assertEquals(functions[1].parameters.size, 1)
        }

        validatePersonUpdater(personCriteria.getNestedType("PersonUpdater") as JavaClassSource)
    }

    private fun validatePersonUpdater(updater: JavaClassSource) {
        var functions = updater.getMethods("updateAll")
        check(functions[0], listOf<Pair<String, String>>(), "UpdateResults")
        check(functions[1], listOf("wc" to "WriteConcern"), "UpdateResults")

        functions = updater.getMethods("updateFirst")
        check(functions[0], listOf<Pair<String, String>>(), "UpdateResults")
        check(functions[1], listOf("wc" to "com.mongodb.WriteConcern"), "UpdateResults")

        functions = updater.getMethods("upsert")
        check(functions[0], listOf<Pair<String, String>>(), "UpdateResults")
        check(functions[1], listOf("wc" to "com.mongodb.WriteConcern"), "UpdateResults")

        functions = updater.getMethods("remove")
        check(functions[0], listOf<Pair<String, String>>(), "WriteResult")
        check(functions[1], listOf("wc" to "com.mongodb.WriteConcern"), "WriteResult")

        functions = updater.getMethods("age")
        Assert.assertEquals(1, functions.size)
        check(functions[0], listOf("value" to "java.lang.Long"), "PersonUpdater")

        functions = updater.getMethods("unsetAge")
        Assert.assertEquals(1, functions.size)
        check(functions[0], listOf<Pair<String, String>>(), "PersonUpdater")

        functions = updater.getMethods("incAge")
        Assert.assertEquals(2, functions.size)
        check(functions[0], listOf<Pair<String, String>>(), "PersonUpdater")
        check(functions[1], listOf("value" to "java.lang.Long"), "PersonUpdater")

        functions = updater.getMethods("decAge")
        Assert.assertEquals(2, functions.size)
        check(functions[0], listOf<Pair<String, String>>(), "PersonUpdater")
        check(functions[1], listOf("value" to "java.lang.Long"), "PersonUpdater")

        listOf("first", "last").forEach {
            functions = updater.getMethods(it)
            Assert.assertEquals(1, functions.size, "Should have found $it")
            check(functions[0], listOf("value" to "java.lang.String"), "PersonUpdater")

            functions = updater.getMethods("unset${it.capitalize()}")
            Assert.assertEquals(1, functions.size, "Should have found unset${it.capitalize()}")
            check(functions[0], listOf<Pair<String, String>>(), "PersonUpdater")
        }
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
          Assert.assertNotNull(javaClass.imports.firstOrNull { it.qualifiedName == type },
                  "Should find an import for $type in ${javaClass.name}")
      }

      private fun shouldNotImport(javaClass: JavaClassSource, type: String?) {
          Assert.assertNull(javaClass.imports.firstOrNull { it.qualifiedName == type },
                  "Should not find an import for $type in ${javaClass.name}")
      }

    private fun findAllProperties(javaClass: JavaClass): MutableList<CritterField> {
        val list = javaClass.fields
        javaClass.getSuperType()?.let {
            val type = critterContext.resolve(javaClass.getPackage() ?: "", it) as JavaClass?
            type?.let {
                list += findAllProperties(type)
            }
        }
//        javaClass.superTypes.forEach {
//            critterContext.resolve(javaClass.pkgName ?: "", it.fullName)?.let {
//                list += findAllProperties((it as KotlinClass).source)
//            }
//        }
        return list
    }
    private fun extractName(property: JavaField): String {
        return if (property.hasAnnotation(Id::class.java)) {
            "_id"
        } else if (property.hasAnnotation(Property::class.java)) {
            val annotation = property.getAnnotation(Property::class.java)!!
            annotation.getStringValue("value")?.replace("\"", "") ?: property.name
        } else {
            property.name
        }
    }

    fun JavaClassSource.getMethods(name: String): List<MethodSource<JavaClassSource>> {
        return methods.filter { it.name == name }
    }
}
