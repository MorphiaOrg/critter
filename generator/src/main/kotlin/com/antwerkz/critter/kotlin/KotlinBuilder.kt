package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.TypeSafeFieldEnd
import com.antwerkz.critter.nameCase
import com.antwerkz.kibble.SourceWriter
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleFile
import com.antwerkz.kibble.model.Modality.FINAL
import com.antwerkz.kibble.model.Mutability.VAR
import com.antwerkz.kibble.model.Visibility.PRIVATE
import com.mongodb.WriteConcern
import com.mongodb.WriteResult
import org.mongodb.morphia.Datastore
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.annotations.Reference
import org.mongodb.morphia.query.Criteria
import org.mongodb.morphia.query.CriteriaContainer
import org.mongodb.morphia.query.Query
import org.mongodb.morphia.query.UpdateResults
import java.io.File

class KotlinBuilder(val context: CritterContext) {
    companion object {
        private val UPDATE_RESULTS: String = UpdateResults::class.java.name
        private val WRITE_CONCERN: String = WriteConcern::class.java.name
    }

    fun build(directory: File) {
        context.classes.values.forEach {
            build(directory, it)
        }
    }

    private fun build(directory: File, source: CritterClass) {
        val criteriaPkg = context.criteriaPkg ?: source.pkgName + ".criteria"
        val kibbleFile = KibbleFile("${source.name}Criteria.kt", criteriaPkg)
        val outputFile = kibbleFile.outputFile(directory)

        if (!source.isAbstract() && context.shouldGenerate(source.lastModified(), outputFile.lastModified())) {
            val criteriaClass = kibbleFile.addClass("${source.name}Criteria")
            criteriaClass.file.addImport(source.qualifiedName)
            val companion = criteriaClass.addCompanionObject()

            criteriaClass.addProperty("datastore", Datastore::class.java.name, constructorParam = true)
            criteriaClass.addProperty("query", "org.mongodb.morphia.query.Query<*>", visibility = PRIVATE, constructorParam = true)
            val secondary = criteriaClass.addSecondaryConstructor()
            secondary.addParameter("ds", Datastore::class.java.name)
            secondary.addParameter("fieldName", "String?", "null")
            secondary.addDelegationArguments("ds", "ds.find(${source.name}::class.java)", "fieldName")

            addCriteriaMethods(criteriaClass)
            addPrefixProperty(criteriaClass)

            source.fields.forEach { field ->
                companion.addProperty(field.name, modality = FINAL, initializer = field.mappedName())
                addField(source, criteriaClass, field)
            }

            buildUpdater(source, criteriaClass)
            outputFile.parentFile.mkdirs()
            kibbleFile.toSource(SourceWriter())
                    .toFile(outputFile)
        }
    }

    private fun addCriteriaMethods(criteriaClass: KibbleClass) {
        val query = criteriaClass.addFunction("query", Query::class.java.name + "<T>", "return query as Query<T>")
        query.addAnnotation(Suppress::class.java, mapOf("value" to "\"UNCHECKED_CAST\""))
        query.addTypeParameter("T")

        criteriaClass.addFunction("delete", WriteResult::class.java.name, """return datastore.delete(query, wc)""")
                .addParameter("wc", WriteConcern::class.java.name, "datastore.defaultWriteConcern")
        criteriaClass.addFunction("or", CriteriaContainer::class.java.name, """return query.or(*criteria)""")
                .addParameter("criteria", Criteria::class.java.name, varargs = true)
        criteriaClass.addFunction("and", CriteriaContainer::class.java.name, """return query.and(*criteria)""")
                .addParameter("criteria", Criteria::class.java.name, varargs = true)
    }

    private fun addPrefixProperty(criteriaClass: KibbleClass) {
        criteriaClass.addProperty("prefix", mutability = VAR, visibility = PRIVATE, initializer = """
                                    if (fieldName != null) fieldName + "." else ""
                    """.trim())
        criteriaClass.constructor
                .addParameter("fieldName", "String?", "null")
    }

    private fun addField(source: CritterClass, criteriaClass: KibbleClass, field: CritterField) {
        when {
            source.hasAnnotation(Reference::class.java) -> {
                val function = criteriaClass.addFunction(field.name, criteriaClass.name,
                        """query.filter("${field.name} = ", reference)""")
                function.addParameter("reference", field.type)
                function.parameters.first().type
                println("function = ${function}")
            }
            field.hasAnnotation(Embedded::class.java) -> {
                criteriaClass.addFunction(field.name, criteriaClass.name,
                        """return ${criteriaClass.name}(query, "${field.name}")""")
            }
            else -> {
                val name =
                        if (field.hasAnnotation(Embedded::class.java) || source.hasAnnotation(Embedded::class.java)) {
                            "prefix + ${field.name}"
                        } else {
                            field.name
                        }
                criteriaClass.addFunction(field.name,
                        "${TypeSafeFieldEnd::class.java.name}<${criteriaClass.name}, ${field.type}>",
                        "return TypeSafeFieldEnd(this, query, $name)")
                criteriaClass.addFunction(field.name, Criteria::class.java.name,
                        "return ${TypeSafeFieldEnd::class.java.name}<${criteriaClass.name}, ${field.type}>(this, query, $name).equal(value)")
                        .addParameter("value", field.parameterizedType)
            }
        }
    }

    private fun buildUpdater(sourceClass: CritterClass, criteriaClass: KibbleClass) {
        val updaterType = "${sourceClass.name}Updater"
        criteriaClass.addFunction("updater", updaterType,
                "return $updaterType(datastore.createUpdateOperations(${sourceClass.name}::class.java))")

        val updater = criteriaClass.addClass(name = updaterType)
        if (!sourceClass.hasAnnotation(Embedded::class.java)) {
            updater.addProperty("ds", visibility = PRIVATE, type = Datastore::class.java.name, constructorParam = true)
            updater.addProperty("query", visibility = PRIVATE, type = Query::class.java.name + "<Any>", constructorParam = true)
        }
        updater.addProperty("updateOperations", visibility = PRIVATE, type = "org.mongodb.morphia.query.UpdateOperations<Any>",
                constructorParam = true)
        addPrefixProperty(updater)

        if (!sourceClass.hasAnnotation(Embedded::class.java)) {
            updater.addFunction("updateAll", UPDATE_RESULTS, "return ds.update(query, updateOperations, false, wc)")
                    .addParameter("wc", WRITE_CONCERN, initializer = "ds.defaultWriteConcern")

            updater.addFunction("updateFirst", UPDATE_RESULTS, "return ds.updateFirst(query, updateOperations, false, wc)")
                    .addParameter("wc", WRITE_CONCERN, initializer = "ds.defaultWriteConcern")

            updater.addFunction("upsert", UPDATE_RESULTS, "return ds.update(query, updateOperations, true, wc)")
                    .addParameter("wc", WRITE_CONCERN, initializer = "ds.defaultWriteConcern")

            updater.addFunction("remove", WriteResult::class.java.name, "return ds.delete(query, wc)")
                    .addParameter("wc", WRITE_CONCERN, initializer = "ds.defaultWriteConcern")
        }
        sourceClass.fields
                .filter({ field -> !field.isStatic })
                .forEach { field ->
                    if (!field.hasAnnotation(Id::class.java)) {
                        updater.addFunction(field.name, updaterType, """
                            updateOperations.set(prefix + "${field.name}", value)
                            return this
                            """.trimMargin())
                                .addParameter("value", field.parameterizedType)

                        updater.addFunction("unset${field.name.nameCase()}", updaterType, """
                            updateOperations.unset(prefix + "${field.name}")
                            return this
                            """)

                        numerics(updaterType, updater, field)
                        containers(updaterType, updater, field)
                    }
                }
    }

    private fun numerics(type: String, updater: KibbleClass, field: CritterField) {
        if (field.isNumeric()) {
            updater.addFunction("inc${field.name.nameCase()}", type, """
                updateOperations.inc(prefix + "${field.name}")
                return this
                """.trimIndent())
                    .addParameter("value", field.type, "1")
        }
    }

    private fun containers(type: String, updater: KibbleClass, field: CritterField) {
        if (field.isContainer()) {

            updater.addFunction("addTo${field.name.nameCase()}", type, "updateOperations.add(prefix + \"${field.name}\", value)")
                    .addParameter("value", field.parameterizedType)

            updater.addFunction("addTo${field.name.nameCase()}", type,
                    "updateOperations.add(prefix + \"${field.name}\", value, addDups)").apply {
                addParameter("value", field.parameterizedType)
                addParameter("addDups", "boolean")
            }

            updater.addFunction("addAllTo${field.name.nameCase()}", type,
                    "updateOperations.addAll(prefix + \"${field.name}\", values, addDups)").apply {
                addParameter("values", field.parameterizedType)
                addParameter("addDups", "boolean")
            }

            updater.addFunction("removeFirstFrom${field.name.nameCase()}", type,
                    "updateOperations.removeFirst(prefix + \"${field.name}\")")

            updater.addFunction("removeLastFrom${field.name.nameCase()}", type,
                    "updateOperations.removeLast(prefix + \"${field.name}\")")

            updater.addFunction("removeFrom${field.name.nameCase()}", type,
                    "updateOperations.removeAll(prefix + \"${field.name}\", value)")
                    .addParameter("value", field.parameterizedType)

            updater.addFunction("removeAllFrom${field.name.nameCase()}", type,
                    "updateOperations.removeAll(prefix + \"${field.name}\", values)")
                    .addParameter("values", field.parameterizedType)
        }
    }

}
