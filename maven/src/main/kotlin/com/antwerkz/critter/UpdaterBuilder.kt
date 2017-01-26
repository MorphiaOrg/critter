package com.antwerkz.critter

import com.mongodb.WriteConcern
import com.mongodb.WriteResult
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.query.UpdateOperations
import org.mongodb.morphia.query.UpdateResults

class UpdaterBuilder(critterClass: CritterClass, criteriaClass: JavaClassSource) {
    init {
        val type = critterClass.name + "Updater"
        val method = criteriaClass.addMethod()
                .setPublic()
                .setName("getUpdater")
                .setReturnType(type)
        method.body = "return new ${method.returnType}();"

        val updater = Roaster.create(JavaClassSource::class.java)
                .setPublic()
                .setName(type)

        criteriaClass.addImport(UpdateOperations::class.java)
        criteriaClass.addImport(UpdateResults::class.java)
        criteriaClass.addImport(WriteConcern::class.java)
        criteriaClass.addImport(WriteResult::class.java)

        val updateOperations = updater.addField()
                .setType("UpdateOperations<${critterClass.name}>")
                .setLiteralInitializer("ds.createUpdateOperations(${critterClass.name}.class);")
        updateOperations.name = "updateOperations"

        updater.addMethod()
                .setPublic()
                .setName("updateAll")
                .setReturnType(UpdateResults::class.java).body = "return ds.update(query(), updateOperations, false);"

        updater.addMethod()
                .setPublic()
                .setName("updateFirst")
                .setReturnType(UpdateResults::class.java).body = "return ds.updateFirst(query(), updateOperations, false);"

        updater.addMethod()
                .setPublic()
                .setName("updateAll")
                .setReturnType(UpdateResults::class.java)
                .setBody("return ds.update(query(), updateOperations, false, wc);")
                .addParameter(WriteConcern::class.java, "wc")

        updater.addMethod()
                .setPublic()
                .setName("updateFirst")
                .setReturnType(UpdateResults::class.java)
                .setBody("return ds.updateFirst(query(), updateOperations, false, wc);")
                .addParameter(WriteConcern::class.java, "wc")

        updater.addMethod()
                .setPublic()
                .setName("upsert")
                .setReturnType(UpdateResults::class.java).body = "return ds.update(query(), updateOperations, true);"

        updater.addMethod()
                .setPublic()
                .setName("upsert")
                .setReturnType(UpdateResults::class.java)
                .setBody("return ds.update(query(), updateOperations, true, wc);")
                .addParameter(WriteConcern::class.java, "wc")

        updater.addMethod()
                .setPublic()
                .setName("remove")
                .setReturnType(WriteResult::class.java).body = "return ds.delete(query());"

        updater.addMethod()
                .setPublic()
                .setName("remove")
                .setReturnType(WriteResult::class.java)
                .setBody("return ds.delete(query(), wc);")
                .addParameter(WriteConcern::class.java, "wc")

        critterClass.fields
                .filter({ field -> !field.source.isStatic })
                .forEach { field ->
                    if (!field.parameterTypes.isEmpty()) {
                        field.parameterTypes
                                .forEach { criteriaClass.addImport(it) }
                    }

                    criteriaClass.addImport(field.fullType)
                    if (!field.hasAnnotation(Id::class.java)) {
                        updater.addMethod()
                                .setPublic()
                                .setName(field.name)
                                .setReturnType(type)
                                .setBody("updateOperations.set(\"${field.name}\", value);\nreturn this;")
                                .addParameter(field.parameterizedType, "value")

                        updater.addMethod()
                                .setPublic()
                                .setName("unset${nameCase(field.name)}")
                                .setReturnType(type).body = "updateOperations.unset(\"${field.name}\");\nreturn this;"

                        numerics(type, updater, field)
                        containers(type, updater, field)
                    }
                }

        criteriaClass.addNestedType(updater)
    }

    private fun numerics(type: String, updater: JavaClassSource, field: CritterField) {
        if (field.isNumeric) {
            updater.addMethod()
                    .setPublic()
                    .setName("dec${nameCase(field.name)}")
                    .setReturnType(type).body = "updateOperations.dec(\"${field.name}\");\nreturn this;"

            updater.addMethod()
                    .setPublic()
                    .setName("inc${nameCase(field.name)}")
                    .setReturnType(type).body = "updateOperations.inc(\"${field.name}\");\nreturn this;"

            updater.addMethod()
                    .setPublic()
                    .setName("inc${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.inc(\"${field.name}\", value);\nreturn this;")
                    .addParameter(field.fullType, "value")
        }
    }

    private fun containers(type: String, updater: JavaClassSource, field: CritterField) {
        if (field.isContainer!!) {

            updater.addMethod()
                    .setPublic()
                    .setName("addTo${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.add(\"${field.name}\", value);\nreturn this;")
                    .addParameter(field.parameterizedType, "value")

            var addItems = updater.addMethod()
                    .setPublic()
                    .setName("addTo${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.add(\"${field.name}\", value, addDups);\nreturn this;" )
            addItems
                    .addParameter(field.parameterizedType, "value")
            addItems
                    .addParameter("boolean", "addDups")

            addItems = updater.addMethod()
                    .setPublic()
                    .setName("addAllTo${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.addAll(\"${field.name}\", values, addDups);\nreturn this;")
            addItems.addParameter(field.parameterizedType, "values")
            addItems.addParameter("boolean", "addDups")

            updater.addMethod()
                    .setPublic()
                    .setName("removeFirstFrom${nameCase(field.name)}")
                    .setReturnType(type).body = "updateOperations.removeFirst(\"${field.name}\");\nreturn this;"

            updater.addMethod()
                    .setPublic()
                    .setName("removeLastFrom${nameCase(field.name)}")
                    .setReturnType(type).body = "updateOperations.removeLast(\"${field.name}\");\nreturn this;"

            updater.addMethod()
                    .setPublic()
                    .setName("removeFrom${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.removeAll(\"${field.name}\", value);\nreturn this;")
                    .addParameter(field.parameterizedType, "value")

            val removeAll = updater.addMethod()
                    .setPublic()
                    .setName("removeAllFrom${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.removeAll(\"${field.name}\", values);\nreturn this;")
            removeAll.addParameter(field.parameterizedType, "values")
        }
    }

    private fun nameCase(name: String): String {
        return name.substring(0, 1).toUpperCase() + name.substring(1)
    }
}
