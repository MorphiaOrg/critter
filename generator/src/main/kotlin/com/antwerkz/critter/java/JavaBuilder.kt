package com.antwerkz.critter.java

import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.TypeSafeFieldEnd
import com.antwerkz.critter.nameCase
import com.mongodb.WriteConcern
import com.mongodb.WriteResult
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.mongodb.morphia.Datastore
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.annotations.Reference
import org.mongodb.morphia.query.Criteria
import org.mongodb.morphia.query.CriteriaContainer
import org.mongodb.morphia.query.FieldEndImpl
import org.mongodb.morphia.query.Query
import org.mongodb.morphia.query.QueryImpl
import org.mongodb.morphia.query.UpdateOperations
import org.mongodb.morphia.query.UpdateResults
import java.io.File
import java.io.PrintWriter
import org.jboss.forge.roaster.model.Visibility.PACKAGE_PRIVATE as rPACKAGE_PRIVATE
import org.jboss.forge.roaster.model.Visibility.PRIVATE as rPRIVATE
import org.jboss.forge.roaster.model.Visibility.PROTECTED as rPROTECTED
import org.jboss.forge.roaster.model.Visibility.PUBLIC as rPUBLIC

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
                extractFields(source as JavaClass, criteriaClass)
                buildUpdater(source, criteriaClass)
                generate(outputFile, criteriaClass)
            }
        }
    }

    private fun addCriteriaMethods(source: CritterClass, criteriaClass: JavaClassSource) {
        criteriaClass.addImport(CriteriaContainer::class.java)

        criteriaClass.addMethod("""public Query<${source.name}> query() {
            return (Query<${source.name}>) query;
        }""")
        criteriaClass.addMethod("""public Datastore datastore() {
            return ds;
        }""")
        criteriaClass.addMethod("""public WriteResult delete() {
            return ds.delete(query());
        }""")
        criteriaClass.addMethod("""public WriteResult delete(WriteConcern wc) {
            return ds.delete(query(), wc);
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

    private fun buildUpdater(source: CritterClass, criteriaClass: JavaClassSource) {
        criteriaClass.addImport(source.qualifiedName)
        criteriaClass.addImport(Query::class.java)
        criteriaClass.addImport(UpdateOperations::class.java)
        criteriaClass.addImport(UpdateResults::class.java)
        criteriaClass.addImport(WriteConcern::class.java)
        criteriaClass.addImport(WriteResult::class.java)
        criteriaClass.addImport(TypeSafeFieldEnd::class.java)

        val type = source.name + "Updater"
        //language=JAVA
        criteriaClass.addMethod("""public ${type} getUpdater() {
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

        updaterMethod(updater, "updateAll", "update", "UpdateResults", false)
        updaterMethod(updater, "updateFirst", "updateFirst", "UpdateResults", false)
        updaterMethod(updater, "upsert", "update", "UpdateResults", true)
        updater.addMethod("""public WriteResult remove() {
                return ds.delete(query);
            }""")
        updater.addMethod("""public WriteResult remove(WriteConcern wc) {
                return ds.delete(query, wc);
            }""")

        source.fields
                .filter({ field -> !field.isStatic })
                .forEach { field ->
                    var fieldType = field.type
                    if(field.isContainer() && field.fullParameterTypes.isNotEmpty()) {
                        fieldType = field.fullParameterTypes.joinToString(", ", prefix = "$type<", postfix = ">")
                    }

                    field.fullParameterTypes
                            .forEach { criteriaClass.addImport(it) }

                    criteriaClass.addImport(field.type)
                    if (!field.hasAnnotation(Id::class.java)) {
                        updater.addMethod("""public $type ${field.name}(${field.parameterizedType} value) {
                            updateOperations.set(prefix + "${field.name}", value);
                            return this;
                        }""")

                        updater.addMethod("""public $type unset${field.name.nameCase()}() {
                            updateOperations.unset(prefix + "${field.name}");
                            return this;
                        }""")

                        numerics(type, updater, field)
                        containers(type, updater, field)
                    }
                }
    }

    private fun updaterMethod(updater: JavaClassSource, name: String, dsMethod: String, type: String, createIfMissing: Boolean) {
        updater.addMethod("""public $type $name() {
                return ds.$dsMethod(query, updateOperations, $createIfMissing);
            }""")

        updater.addMethod("""public $type $name(${WriteConcern::class.java.simpleName} wc) {
               return ds.$dsMethod(query, updateOperations, $createIfMissing, wc);
            }""")
    }

    private fun numerics(type: String, updater: JavaClassSource, field: CritterField) {
        if (field.isNumeric()) {
            updater.addMethod("""public $type dec${field.name.nameCase()}() {
                updateOperations.dec("${field.name}");
                return this;
            }""")

            updater.addMethod("""public $type dec${field.name.nameCase()}(${field.type} value) {
                updateOperations.dec("${field.name}", value);
                return this;
            }""")

            updater.addMethod("""public $type inc${field.name.nameCase()}() {
                updateOperations.inc("${field.name}");
                return this;
            }""")

            updater.addMethod("""public $type inc${field.name.nameCase()}(${field.type} value) {
                updateOperations.inc("${field.name}", value);
                return this;
            }""")
        }
    }

    private fun containers(type: String, updater: JavaClassSource, field: CritterField) {
        if (field.isContainer()) {

            val nameCase = field.name.nameCase()
            val fieldType = field.parameterizedType
            updater.addMethod("""public $type addTo$nameCase($fieldType value) {
                updateOperations.add("${field.name}", value);
                return this;
            }""")

            updater.addMethod("""public $type addTo$nameCase($fieldType value, boolean addDups) {
                updateOperations.add("${field.name}", value, addDups);
                return this;
            }""")

            updater.addMethod("""public $type addAllTo$nameCase($fieldType value, boolean addDups) {
                updateOperations.addAll("${field.name}", value, addDups);
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

            updater.addMethod("""public $type removeFrom$nameCase($fieldType value) {
                updateOperations.removeAll("${field.name}", value);
                return this;
            }""")

            updater.addMethod("""public $type removeAllFrom$nameCase($fieldType values) {
                updateOperations.removeAll("${field.name}", values);
                return this;
            }""")
        }
    }

    private fun addFieldMethods(source: CritterClass, criteriaClass: JavaClassSource, field: CritterField) {
        if (source.hasAnnotation(Reference::class.java)) {
            criteriaClass.addMethod("""public ${criteriaClass.qualifiedName}(${field.type} reference) {
                query.filter("${source.name} = ", reference);
                return this;
            }""")
        } else if (field.hasAnnotation(Embedded::class.java)) {
            criteriaClass.addImport(Criteria::class.java)
            val criteriaType: String = extractType(field, criteriaClass)
            criteriaClass.addMethod("""public ${criteriaType} ${field.name}() {
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
            criteriaClass.addMethod("""public ${Criteria::class.java.name} ${field.name}(${field.type} value) {
                return new TypeSafeFieldEnd<>(this, query, prefix + "${field.name}").equal(value);
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
