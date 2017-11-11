package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.TypeSafeFieldEnd
import com.antwerkz.critter.criteria.BaseCriteria
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

        if (!context.force && source.lastModified() < outputFile.lastModified()) {
            return
        }

        val criteriaClass = kibbleFile.addClass("${source.name}Criteria")

//        kibbleFile.addImport(source.pkgName + "." + source.name)

//        kibbleFile.addImport(Datastore::class.java)
//        kibbleFile.addImport(BaseCriteria::class.java)
//        kibbleFile.addImport(TypeSafeFieldEnd::class.java)
//        kibbleFile.addImport("${source.pkgName}.${source.name}")

        val companion = criteriaClass.addCompanionObject()

        source.fields.forEach { field ->
            //            kibbleFile.addImport(field.type)
            companion.addProperty(field.name, modality = FINAL, initializer = field.mappedName())
            addField(source, criteriaClass, field)
        }

        val primary = criteriaClass.constructor
        if (!source.hasAnnotation(Embedded::class.java)) {
            criteriaClass.addSuperType("${BaseCriteria::class.java.name}<${source.qualifiedName}>")
            criteriaClass.superCallArgs = listOf("ds", "${source.name}::class.java")
            primary?.addParameter("ds", Datastore::class.java.name)
        } else {
            criteriaClass.addProperty("query", "org.mongodb.morphia.query.Query<*>", mutability = VAR, visibility = PRIVATE, constructorParam = true)
            criteriaClass.addProperty("prefix", "String", mutability = VAR, visibility = PRIVATE, constructorParam = true)

            kibbleFile.addImport(Query::class.java)
            criteriaClass.initBlock = "this.prefix = prefix + \".\""
        }

        buildUpdater(source, criteriaClass)
        outputFile.parentFile.mkdirs()
        kibbleFile.toSource(SourceWriter())
                .toFile(outputFile)
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

    fun buildUpdater(sourceClass: CritterClass, criteriaClass: KibbleClass) {
        val updaterType = "${sourceClass.name}Updater"
        criteriaClass.addFunction("updater", updaterType, "return $updaterType(this)")

        val updater = criteriaClass.addClass(name = updaterType)
        updater.addProperty("criteria", criteriaClass.name, visibility = PRIVATE, constructorParam = true)
        updater.addProperty("ds", visibility = PRIVATE, initializer = "criteria.datastore()")
        updater.addProperty("query", visibility = PRIVATE, initializer = "criteria.query")
        updater.addProperty("updateOperations", visibility = PRIVATE,
                initializer = "ds.createUpdateOperations(${sourceClass.name}::class.java)")

        val queryFun = updater.addFunction("query", "org.mongodb.morphia.query.Query<${sourceClass.name}>",
                "return criteria.query")
        queryFun.visibility = PRIVATE

        updater.addFunction("updateAll", UPDATE_RESULTS, "return ds.update(query(), updateOperations, false)")
        updater.addFunction("updateAll", UPDATE_RESULTS, "return ds.update(query(), updateOperations, false, wc)")
                .addParameter("wc", WRITE_CONCERN)

        updater.addFunction("updateFirst", UPDATE_RESULTS,
                "return ds.updateFirst(query(), updateOperations, false)")
        updater.addFunction("updateFirst", UPDATE_RESULTS, "return ds.updateFirst(query(), updateOperations, false, wc)")
                .addParameter("wc", WRITE_CONCERN)

        updater.addFunction("upsert", UPDATE_RESULTS, "return ds.update(query(), updateOperations, true)")
        updater.addFunction("upsert", UPDATE_RESULTS, "return ds.update(query(), updateOperations, true, wc)")
                .addParameter("wc", WRITE_CONCERN)

        updater.addFunction("remove", WriteResult::class.java.name, "return ds.delete(query())")
        updater.addFunction("remove", WriteResult::class.java.name, "return ds.delete(query(), wc)")
                .addParameter("wc", WRITE_CONCERN)

        sourceClass.fields
                .filter({ field -> !field.isStatic })
                .forEach { field ->
                    /*
                                        if (!field.parameterTypes.isEmpty()) {
                                            field.parameterTypes
                                                    .forEach { criteriaClass.file.addImport(it) }
                                        }
                    */

//                    criteriaClass.file.addImport(field.fullyQualifiedType)
                    if (!field.hasAnnotation(Id::class.java)) {
                        updater.addFunction(field.name, updaterType, """
                            updateOperations.set("${field.name}", value)
                            return this
                            """.trimMargin())
                                .addParameter("value", field.parameterizedType)

                        updater.addFunction("unset${field.name.nameCase()}", updaterType, """
                            updateOperations.unset("${field.name}")
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
                updateOperations.inc("${field.name}")
                return this
                """.trimIndent())
                    .addParameter("value", field.type, "1")
        }
    }

    private fun containers(type: String, updater: KibbleClass, field: CritterField) {
        if (field.isContainer()) {

            updater.addFunction("addTo${field.name.nameCase()}", type, "updateOperations.add(\"${field.name}\", value)")
                    .addParameter("value", field.parameterizedType)

            updater.addFunction("addTo${field.name.nameCase()}", type,
                    "updateOperations.add(\"${field.name}\", value, addDups)").apply {
                addParameter("value", field.parameterizedType)
                addParameter("addDups", "boolean")
            }

            updater.addFunction("addAllTo${field.name.nameCase()}", type,
                    "updateOperations.addAll(\"${field.name}\", values, addDups)").apply {
                addParameter("values", field.parameterizedType)
                addParameter("addDups", "boolean")
            }

            updater.addFunction("removeFirstFrom${field.name.nameCase()}", type,
                    "updateOperations.removeFirst(\"${field.name}\")")

            updater.addFunction("removeLastFrom${field.name.nameCase()}", type, "updateOperations.removeLast(\"${field.name}\")")

            updater.addFunction("removeFrom${field.name.nameCase()}", type,
                    "updateOperations.removeAll(\"${field.name}\", value)")
                    .addParameter("value", field.parameterizedType)

            updater.addFunction("removeAllFrom${field.name.nameCase()}", type,
                    "updateOperations.removeAll(\"${field.name}\", values)")
                    .addParameter("values", field.parameterizedType)
        }
    }

}
