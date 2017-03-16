package com.antwerkz.critter

import com.mongodb.WriteConcern
import com.mongodb.WriteResult
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.query.UpdateOperations
import org.mongodb.morphia.query.UpdateResults

class UpdaterBuilder(sourceClass: CritterClass, targetClass: CritterClass) {
    init {
        val type = sourceClass.getName() + "Updater"
        val method = targetClass.addMethod()
                .setPublic()
                .setName("getUpdater")
                .setReturnType(type)
        method.setBody("return new ${method.getReturnType()}();")

        val updater = targetClass.createClass(name = type)

        targetClass.addImport(UpdateOperations::class.java)
        targetClass.addImport(UpdateResults::class.java)
        targetClass.addImport(WriteConcern::class.java)
        targetClass.addImport(WriteResult::class.java)

        updater.addField("updateOperations", "UpdateOperations<${sourceClass.getName()}>")
                .setLiteralInitializer("ds.createUpdateOperations(${sourceClass.getName()}.class);")

        updater.addMethod()
                .setPublic()
                .setName("updateAll")
                .setReturnType(UpdateResults::class.java)
                .setBody("return ds.update(query(), updateOperations, false);")

        updater.addMethod()
                .setPublic()
                .setName("updateFirst")
                .setReturnType(UpdateResults::class.java)
                .setBody("return ds.updateFirst(query(), updateOperations, false);")

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
                .setReturnType(UpdateResults::class.java)
                .setBody("return ds.update(query(), updateOperations, true);")

        updater.addMethod()
                .setPublic()
                .setName("upsert")
                .setReturnType(UpdateResults::class.java)
                .setBody("return ds.update(query(), updateOperations, true, wc);")
                .addParameter(WriteConcern::class.java, "wc")

        updater.addMethod()
                .setPublic()
                .setName("remove")
                .setReturnType(WriteResult::class.java)
                .setBody("return ds.delete(query());")

        updater.addMethod()
                .setPublic()
                .setName("remove")
                .setReturnType(WriteResult::class.java)
                .setBody("return ds.delete(query(), wc);")
                .addParameter(WriteConcern::class.java, "wc")

        sourceClass.fields
                .filter({ field -> !field.isStatic() })
                .forEach { field ->
                    if (!field.parameterTypes.isEmpty()) {
                        field.parameterTypes
                                .forEach { targetClass.addImport(it) }
                    }

                    targetClass.addImport(field.fullType)
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
                                .setReturnType(type)
                                .setBody("updateOperations.unset(\"${field.name}\");\nreturn this;")

                        numerics(type, updater, field)
                        containers(type, updater, field)
                    }
                }
        targetClass.addNestedType(updater)
    }

    private fun numerics(type: String, updater: CritterClass, field: CritterField) {
        if (field.isNumeric()) {
            updater.addMethod()
                    .setPublic()
                    .setName("dec${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.dec(\"${field.name}\");\nreturn this;")

            updater.addMethod()
                    .setPublic()
                    .setName("inc${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.inc(\"${field.name}\");\nreturn this;")

            updater.addMethod()
                    .setPublic()
                    .setName("inc${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.inc(\"${field.name}\", value);\nreturn this;")
                    .addParameter(field.fullType, "value")
        }
    }

    private fun containers(type: String, updater: CritterClass, field: CritterField) {
        if (field.isContainer()) {

            updater.addMethod()
                    .setPublic()
                    .setName("addTo${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.add(\"${field.name}\", value);\nreturn this;")
                    .addParameter(field.parameterizedType, "value")

            updater.addMethod()
                    .setPublic()
                    .setName("addTo${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.add(\"${field.name}\", value, addDups);\nreturn this;")
                    .addParameter(field.parameterizedType, "value")
                    .addParameter("boolean", "addDups")

            updater.addMethod()
                    .setPublic()
                    .setName("addAllTo${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.addAll(\"${field.name}\", values, addDups);\nreturn this;")
                    .addParameter(field.parameterizedType, "values")
                    .addParameter("boolean", "addDups")

            updater.addMethod()
                    .setPublic()
                    .setName("removeFirstFrom${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.removeFirst(\"${field.name}\");\nreturn this;")

            updater.addMethod()
                    .setPublic()
                    .setName("removeLastFrom${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.removeLast(\"${field.name}\");\nreturn this;")

            updater.addMethod()
                    .setPublic()
                    .setName("removeFrom${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.removeAll(\"${field.name}\", value);\nreturn this;")
                    .addParameter(field.parameterizedType, "value")

            updater.addMethod()
                    .setPublic()
                    .setName("removeAllFrom${nameCase(field.name)}")
                    .setReturnType(type)
                    .setBody("updateOperations.removeAll(\"${field.name}\", values);\nreturn this;")
                    .addParameter(field.parameterizedType, "values")
        }
    }

    private fun nameCase(name: String): String {
        return name.substring(0, 1).toUpperCase() + name.substring(1)
    }
}
