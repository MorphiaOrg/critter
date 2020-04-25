package com.antwerkz.critter.java

import com.antwerkz.critter.Critter.addMethods
import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.FilterSieve
import com.antwerkz.critter.nameCase
import com.mongodb.WriteConcern
import com.mongodb.WriteResult
import dev.morphia.DeleteOptions
import dev.morphia.UpdateOptions
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Id
import dev.morphia.annotations.Reference
import dev.morphia.query.Query
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.io.File
import java.io.PrintWriter

@ExperimentalStdlibApi
class JavaBuilder(private val context: CritterContext) {
    companion object {
        val FIELDEND = "REPLACEME" // TypeSafeFieldEnd::class.java.name
    }

    fun build(directory: File) {
        context.classes.values.forEach { source ->
            val criteriaClass = Roaster.create(JavaClassSource::class.java)
                    .setPackage(source.pkgName + ".criteria")
                    .setName(source.name + "Criteria")

            val outputFile = File(directory, criteriaClass.qualifiedName.replace('.', '/') + ".java")
            if (!source.isAbstract() && context.shouldGenerate(source.lastModified(), outputFile.lastModified())) {
/*
                criteriaClass.addImport(Datastore::class.java)
                criteriaClass.addImport(Query::class.java)
                criteriaClass.addImport(String::class.java)

                criteriaClass.addField("private Datastore ds")
                criteriaClass.addField("private Query<?> query")
                criteriaClass.addField("private String prefix")

                criteriaClass.addMethods("""
                    public ${criteriaClass.name}(Datastore ds) {
                        this(ds, ds.createQuery(${source.name}.class), null);
                    }

                    protected ${criteriaClass.name}(Datastore ds, String fieldName) {
                        this(ds, ds.createQuery(${source.name}.class), fieldName);
                    }

                    protected ${criteriaClass.name}(Datastore ds, Query<?> query, String fieldName) {
                        this.ds = ds;
                        this.query = query;
                        this.prefix = fieldName != null ? fieldName + "." : "";
                    }""").forEach {

                    it.isConstructor = true
                }
*/

//                addCriteriaMethods(source, criteriaClass)
                extractFields(source, criteriaClass)
                buildUpdater(source, criteriaClass)
                generate(outputFile, criteriaClass)
            }
        }
    }

    private fun addCriteriaMethods(source: JavaClass, criteriaClass: JavaClassSource) {
        criteriaClass.addImport(DeleteOptions::class.java)

        criteriaClass.addMethods("""
           public Query<${source.name}> query() {
             return (Query<${source.name}>) query;
           }
                    
           public WriteResult delete() {
             return query().delete();
           }
           
           public WriteResult delete(DeleteOptions options) {
             return query().delete(options);
           }""")
    }


    private fun extractFields(source: JavaClass, criteriaClass: JavaClassSource) {
        source.fields.forEach { field ->
            criteriaClass.addField("public static final String ${field.name} = ${field.mappedName()}; ")

            addFieldMethods(source, criteriaClass, field)
        }
    }

    private fun buildUpdater(source: JavaClass, criteriaClass: JavaClassSource) {
        criteriaClass.addImport(source.qualifiedName)
        criteriaClass.addImport(Query::class.java)
        criteriaClass.addImport(UpdateOptions::class.java)
        criteriaClass.addImport(WriteConcern::class.java)
        criteriaClass.addImport(WriteResult::class.java)

        val type = source.name + "Updater"
        // language=JAVA
        criteriaClass.addMethod("""public $type getUpdater() {
           return new $type(ds,query,ds.createUpdateOperations(${source.name}.class),!prefix.equals("")?prefix:null);
        }""")

        val updater = criteriaClass.addNestedType(JavaClassSource::class.java)
        updater.name = type
        updater.addField("private final Datastore ds;")
        updater.addField("private final Query<?> query;")
        updater.addField("private final String prefix;")
        updater.addField("private final UpdateOperations updateOperations;")

        updater.addMethods("""
            public ${updater.name}(Datastore ds, Query<?> query, UpdateOperations updateOperations, String fieldName) {
                this.ds = ds;
                this.query = query;
                this.prefix = fieldName != null ? fieldName + "." : "";
                this.updateOperations = ds.createUpdateOperations(${source.name}.class);
            }

            public UpdateResults update() {
                return ds.update(query, updateOperations, new UpdateOptions());
            }

            public UpdateResults update(UpdateOptions options) {
                return ds.update(query, updateOperations, options);
            }""")
                .first().isConstructor = true

        source.fields
                .filter { field -> !field.isStatic }
                .forEach { field ->
                    field.fullParameterTypes.forEach { criteriaClass.addImport(it) }

                    criteriaClass.addImport(field.type)
                    if (!field.hasAnnotation(Id::class.java)) {
                        updater.addMethods("""
                            public $type ${field.name}(${field.parameterizedType} __newValue) {
                                updateOperations.set(prefix + "${field.name}", __newValue);
                                return this;
                            }
    
                            public $type unset${field.name.nameCase()}() {
                                updateOperations.unset(prefix + "${field.name}");
                                return this;
                            }""")

                        numbers(type, updater, field)
                        containers(type, updater, field)
                    }
                }
    }

    private fun numbers(type: String, updater: JavaClassSource, field: CritterField) {
        if (field.isNumeric()) {
            updater.addMethods("""
                public $type dec${field.name.nameCase()}() {
                    updateOperations.dec("${field.name}");
                    return this;
                }
    
                public $type dec${field.name.nameCase()}(${field.type} __newValue) {
                    updateOperations.dec("${field.name}", __newValue);
                    return this;
                }
    
                public $type inc${field.name.nameCase()}() {
                    updateOperations.inc("${field.name}");
                    return this;
                }
    
                public $type inc${field.name.nameCase()}(${field.type} __newValue) {
                    updateOperations.inc("${field.name}", __newValue);
                    return this;
                }""")
        }
    }

    private fun containers(type: String, updater: JavaClassSource, field: CritterField) {
        if (field.isContainer()) {

            val nameCase = field.name.nameCase()
            val fieldType = field.parameterizedType
            val parameterType = field.fullParameterTypes.last()
            updater.addMethods("""
                public $type addTo$nameCase($parameterType __newValue) {
                    updateOperations.addToSet("${field.name}", __newValue);
                    return this;
                }
    
                public $type pushTo$nameCase($parameterType __newValue) {
                    updateOperations.push("${field.name}", __newValue);
                    return this;
                }
    
                public $type addTo$nameCase($fieldType __newValue, boolean addDups) {
                    updateOperations.addToSet("${field.name}", __newValue);
                    return this;
                }
    
                public $type pushTo$nameCase($fieldType __newValue, boolean addDups) {
                    updateOperations.push("${field.name}", __newValue);
                    return this;
                }
    
                public $type removeFirstFrom$nameCase() {
                    updateOperations.removeFirst("${field.name}");
                    return this;
                }
    
                public $type removeLastFrom$nameCase() {
                    updateOperations.removeLast("${field.name}");
                    return this;
                }
    
                public $type removeFrom$nameCase($fieldType __newValue) {
                    updateOperations.removeAll("${field.name}", __newValue);
                    return this;
                }
    
                public $type removeAllFrom$nameCase($fieldType values) {
                    updateOperations.removeAll("${field.name}", values);
                    return this;
                }""")
        }
    }

    private fun addFieldMethods(source: JavaClass, criteriaClass: JavaClassSource, field: CritterField) {
        if (source.hasAnnotation(Reference::class.java)) {
            criteriaClass.addMethod("""
                public ${criteriaClass.qualifiedName}(${field.type} reference) {
                    query.filter("${source.name} = ", reference);
                    return this;
                }""")
        } else if (field.hasAnnotation(Embedded::class.java)) {
            val criteriaType: String = extractType(field, criteriaClass)
            criteriaClass.addMethod("""
                public $criteriaType ${field.name}() {
                    return new $criteriaType(ds, query, "${field.name}");
                }""")
        } else if (!field.isStatic) {
            addFieldElements(criteriaClass, field)
        }
    }

    private fun addFieldElements(criteriaClass: JavaClassSource, field: CritterField) {
        criteriaClass.addImport(field.type)
        field.fullParameterTypes.forEach {
            criteriaClass.addImport(it)
        }
        criteriaClass.addMethods("""
            public static ${field.name.toTitleCase()}Filters ${field.name}() {
                return new ${field.name.toTitleCase()}Filters();
            }

            public Filter ${field.name}(${field.type} __newValue) {
                return ${field.name}().eq(__newValue);
            }""".trimIndent())

        criteriaClass.addNestedType(Roaster.create(JavaClassSource::class.java))
                .apply {
                    name = "${field.name.toTitleCase()}Filters"
                    isFinal = true
                    addMethod("""${name}() {}""").isConstructor = true
                    attachFilters(field)
                }
    }

    private fun extractType(field: CritterField, criteriaClass: JavaClassSource): String {
        val criteriaType: String
        if (!field.shortParameterTypes.isEmpty()) {
            criteriaType = field.shortParameterTypes[0] + "Criteria"
            criteriaClass.addImport("${criteriaClass.`package`}.$criteriaType")
        } else {
            criteriaType = field.type + "Criteria"
            criteriaClass.addImport(field.type)
        }
        return criteriaType
    }

    private fun generate(outputFile: File, criteriaClass: JavaClassSource) {
        outputFile.parentFile.mkdirs()
        PrintWriter(outputFile).use { writer -> writer.println(criteriaClass.toString()) }
    }
}

@ExperimentalStdlibApi
private fun JavaClassSource.attachFilters(field: CritterField) {
    FilterSieve.handlers(field, this)
}

fun String.toTitleCase(): String {
    return substring(0, 1).toUpperCase() + substring(1)
}