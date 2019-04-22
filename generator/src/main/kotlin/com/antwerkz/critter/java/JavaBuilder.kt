package com.antwerkz.critter.java

import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.TypeSafeFieldEnd
import com.antwerkz.critter.nameCase
import com.mongodb.WriteConcern
import com.mongodb.WriteResult
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import dev.morphia.Datastore
import dev.morphia.DeleteOptions
import dev.morphia.UpdateOptions
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Id
import dev.morphia.annotations.Reference
import dev.morphia.query.Criteria
import dev.morphia.query.CriteriaContainer
import dev.morphia.query.FieldEndImpl
import dev.morphia.query.Query
import dev.morphia.query.QueryImpl
import dev.morphia.query.UpdateOperations
import dev.morphia.query.UpdateResults
import java.io.File
import java.io.PrintWriter

class JavaBuilder(private val context: CritterContext) {

    fun build(directory: File) {
        context.classes.values.forEach { source ->
            val criteriaClass = Roaster.create(JavaClassSource::class.java)
                    .setPackage(source.pkgName + ".criteria")
                    .setName(source.name + "Criteria")

            val outputFile = File(directory, criteriaClass.qualifiedName.replace('.', '/') + ".java")
            if (!source.isAbstract() && context.shouldGenerate(source.lastModified(), outputFile.lastModified())) {
                criteriaClass.addImport(Datastore::class.java)
                criteriaClass.addImport(Query::class.java)
                criteriaClass.addImport(String::class.java)

                criteriaClass.addField("private Datastore ds")
                criteriaClass.addField("private Query<?> query")
                criteriaClass.addField("private String prefix")

                criteriaClass.addMethod("""
                    public ${criteriaClass.name}(Datastore ds) {
                        this(ds, ds.createQuery(${source.name}.class), null);
                    }""")
                        .isConstructor = true

                criteriaClass.addMethod("""
                    protected ${criteriaClass.name}(Datastore ds, String fieldName) {
                        this(ds, ds.createQuery(${source.name}.class), fieldName);
                    }""")
                        .isConstructor = true

                criteriaClass.addMethod("""
                    protected ${criteriaClass.name}(Datastore ds, Query<?> query, String fieldName) {
                        this.ds = ds;
                        this.query = query;
                        this.prefix = fieldName != null ? fieldName + "." : "";
                    }""")
                        .isConstructor = true

                addCriteriaMethods(source, criteriaClass)
                extractFields(source, criteriaClass)
                buildUpdater(source, criteriaClass)
                generate(outputFile, criteriaClass)
            }
        }
    }

    private fun addCriteriaMethods(source: JavaClass, criteriaClass: JavaClassSource) {
        criteriaClass.addImport(CriteriaContainer::class.java)
        criteriaClass.addImport(DeleteOptions::class.java)

        criteriaClass.addMethod("""public Query<${source.name}> query() {
            return (Query<${source.name}>) query;
        }""")
        criteriaClass.addMethod("""public Datastore datastore() {
            return ds;
        }""")
        criteriaClass.addMethod("""public WriteResult delete() {
            return ds.delete(query());
        }""")
        criteriaClass.addMethod("""public WriteResult delete(DeleteOptions options) {
            return ds.delete(query(), options);
        }""")
        criteriaClass.addMethod("""public CriteriaContainer or(Criteria... criteria) {
            return query.or(criteria);
        }""")
        criteriaClass.addMethod("""public CriteriaContainer and(Criteria... criteria) {
            return query.and(criteria);
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
        criteriaClass.addImport(UpdateOperations::class.java)
        criteriaClass.addImport(UpdateOptions::class.java)
        criteriaClass.addImport(UpdateResults::class.java)
        criteriaClass.addImport(WriteConcern::class.java)
        criteriaClass.addImport(WriteResult::class.java)
        criteriaClass.addImport(TypeSafeFieldEnd::class.java)

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

        updater.addMethod("""public AddressUpdater(Datastore ds, Query<?> query, UpdateOperations updateOperations, String fieldName) {
            this.ds = ds;
            this.query = query;
            this.prefix = fieldName != null ? fieldName + "." : "";
            this.updateOperations = ds.createUpdateOperations(${source.name}.class);
        }""")
                .isConstructor = true

        updater.addMethod("""public UpdateResults update() {
                return ds.update(query, updateOperations, new UpdateOptions());
            }""")
        updater.addMethod("""public UpdateResults update(UpdateOptions options) {
                return ds.update(query, updateOperations, options);
            }""")

        source.fields
                .filter { field -> !field.isStatic }
                .forEach { field ->
                    field.fullParameterTypes.forEach { criteriaClass.addImport(it) }

                    criteriaClass.addImport(field.type)
                    if (!field.hasAnnotation(Id::class.java)) {
                        updater.addMethod("""public $type ${field.name}(${field.parameterizedType} __newValue) {
                            updateOperations.set(prefix + "${field.name}", __newValue);
                            return this;
                        }""")

                        updater.addMethod("""public $type unset${field.name.nameCase()}() {
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
            updater.addMethod("""public $type dec${field.name.nameCase()}() {
                updateOperations.dec("${field.name}");
                return this;
            }""")

            updater.addMethod("""public $type dec${field.name.nameCase()}(${field.type} __newValue) {
                updateOperations.dec("${field.name}", __newValue);
                return this;
            }""")

            updater.addMethod("""public $type inc${field.name.nameCase()}() {
                updateOperations.inc("${field.name}");
                return this;
            }""")

            updater.addMethod("""public $type inc${field.name.nameCase()}(${field.type} __newValue) {
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
            updater.addMethod("""public $type addTo$nameCase($parameterType __newValue) {
                updateOperations.addToSet("${field.name}", __newValue);
                return this;
            }""")

            updater.addMethod("""public $type pushTo$nameCase($parameterType __newValue) {
                updateOperations.push("${field.name}", __newValue);
                return this;
            }""")

            updater.addMethod("""public $type addTo$nameCase($fieldType __newValue, boolean addDups) {
                updateOperations.addToSet("${field.name}", __newValue);
                return this;
            }""")

            updater.addMethod("""public $type pushTo$nameCase($fieldType __newValue, boolean addDups) {
                updateOperations.push("${field.name}", __newValue);
                return this;
            }""")

            updater.addMethod("""public $type removeFirstFrom$nameCase() {
                updateOperations.removeFirst("${field.name}");
                return this;
            }""")

            updater.addMethod("""public $type removeLastFrom$nameCase() {
                updateOperations.removeLast("${field.name}");
                return this;
            }""")

            updater.addMethod("""public $type removeFrom$nameCase($fieldType __newValue) {
                updateOperations.removeAll("${field.name}", __newValue);
                return this;
            }""")

            updater.addMethod("""public $type removeAllFrom$nameCase($fieldType values) {
                updateOperations.removeAll("${field.name}", values);
                return this;
            }""")
        }
    }

    private fun addFieldMethods(source: JavaClass, criteriaClass: JavaClassSource, field: CritterField) {
        if (source.hasAnnotation(Reference::class.java)) {
            criteriaClass.addMethod("""public ${criteriaClass.qualifiedName}(${field.type} reference) {
                query.filter("${source.name} = ", reference);
                return this;
            }""")
        } else if (field.hasAnnotation(Embedded::class.java)) {
            criteriaClass.addImport(Criteria::class.java)
            val criteriaType: String = extractType(field, criteriaClass)
            criteriaClass.addMethod("""public $criteriaType ${field.name}() {
                return new $criteriaType(ds, query, "${field.name}");
            }""")
        } else if (!field.isStatic) {
            criteriaClass.addImport(field.type)
            field.fullParameterTypes.forEach {
                criteriaClass.addImport(it)
            }
            criteriaClass.addImport(Criteria::class.java)
            criteriaClass.addImport(FieldEndImpl::class.java)
            criteriaClass.addImport(QueryImpl::class.java)
            criteriaClass.addMethod("""public ${TypeSafeFieldEnd::class.java.name}<${criteriaClass.qualifiedName}, ${field.type}> ${field.name}() {
                return new TypeSafeFieldEnd<>(this, query, prefix + "${field.name}");
            }""")
            criteriaClass.addMethod("""public ${Criteria::class.java.name} ${field.name}(${field.type} __newValue) {
                return new TypeSafeFieldEnd<>(this, query, prefix + "${field.name}").equal(__newValue);
            }""")
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
