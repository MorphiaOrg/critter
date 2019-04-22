package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterField
import com.antwerkz.critter.TypeSafeFieldEnd
import com.antwerkz.critter.nameCase
import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider
import com.mongodb.WriteConcern
import com.mongodb.WriteResult
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.FunSpec.Builder
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import dev.morphia.Datastore
import dev.morphia.DeleteOptions
import dev.morphia.UpdateOptions
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Id
import dev.morphia.annotations.Property
import dev.morphia.annotations.Reference
import dev.morphia.query.Criteria
import dev.morphia.query.CriteriaContainer
import dev.morphia.query.Query
import dev.morphia.query.UpdateOperations
import dev.morphia.query.UpdateResults
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Comparator.comparingInt
import java.util.ServiceLoader

class KotlinBuilder(val context: KotlinContext) {
    companion object {
        private val STRING = String::class.asClassName()
        private val BOOLEAN = Boolean::class.asClassName()
        private val CRITERIA = Criteria::class.asClassName()
        private val CRITERIA_CONTAINER = CriteriaContainer::class.asClassName()
        private val DELETE_OPTIONS = DeleteOptions::class.asClassName()
        private val QUERY = Query::class.asClassName()
        private val TYPESAFE_FIELD_END = TypeSafeFieldEnd::class.asClassName()
        private val UPDATE_OPERATIONS = UpdateOperations::class.asClassName()
        private val UPDATE_OPTIONS = UpdateOptions::class.asClassName()
        private val UPDATE_RESULTS = UpdateResults::class.asClassName()
        private val WRITE_CONCERN = WriteConcern::class.asClassName()
        private val WRITE_RESULT = WriteResult::class.asClassName()
        private val LOG = LoggerFactory.getLogger(KotlinBuilder::class.java)
    }

    fun build(directory: File) {
        context.classes.values.forEach {
            build(directory, it)
        }
    }

    private val ruleSets: List<RuleSet> by lazy {
        ServiceLoader.load(RuleSetProvider::class.java).map<RuleSetProvider, RuleSet> { it.get() }
            .sortedWith(comparingInt<RuleSet> { if (it.id == "standard") 0 else 1 }.thenComparing(RuleSet::id))
    }

    private fun build(directory: File, source: KotlinClass) {
        val criteriaPkg = context.criteriaPkg ?: "${source.fileSpec.packageName}.criteria"

        val fileBuilder = FileSpec.builder(criteriaPkg, "${source.name}Criteria")
        val replace = criteriaPkg.replace('.', '/')
        val outputFile = File(directory, "$replace/${fileBuilder.name}.kt")

        try {
            val srcMod = source.lastModified()
            val outMod = outputFile.lastModified()
            if (!source.isAbstract() && !source.isEnum() && context.shouldGenerate(srcMod, outMod)) {

                val criteriaClass = com.squareup.kotlinpoet.TypeSpec.classBuilder("${source.name}Criteria")
                criteriaClass.addAnnotation(
                        AnnotationSpec.builder(Suppress::class.java).addMember(CodeBlock.of("\"UNCHECKED_CAST\"")).build()
                )

                val constructorBuilder = FunSpec.constructorBuilder()

                addConstructorProperty(constructorBuilder, criteriaClass, "ds", Datastore::class.java.asTypeName(), PRIVATE)
                addConstructorProperty(
                        constructorBuilder, criteriaClass, "query", QUERY.parameterizedBy(TypeVariableName("*")), PRIVATE
                )

                criteriaClass.addFunction(
                        FunSpec.constructorBuilder().addParameter(ParameterSpec.builder("ds", Datastore::class).build()).addParameter(
                                ParameterSpec.builder(
                                        "fieldName", ClassName("kotlin", "String").copy(nullable = true)
                                ).defaultValue("null").build()
                        ).callThisConstructor("ds", "ds.find(${source.name}::class.java)", "fieldName").build()
                )

                addCriteriaMethods(source, criteriaClass)
                addPrefixProperty(criteriaClass, constructorBuilder)

                if (source.fields.isNotEmpty()) {
                    val companion = TypeSpec.companionObjectBuilder()
                    source.fields.forEach { field ->
                        companion.addProperty(PropertySpec.builder(field.name, STRING).initializer(""""${field.mappedName()}"""").build())
                        addField(source, criteriaClass, field)
                    }
                    criteriaClass.addType(companion.build())
                }

                buildUpdater(source, criteriaClass)

                criteriaClass.primaryConstructor(constructorBuilder.build())
                fileBuilder.addType(criteriaClass.build())

                val fileSpec = fileBuilder.build()
                fileSpec.writeTo(directory)

                formatOutput(directory, fileSpec)
            }
        } catch (e: Exception) {
            LOG.error("Failed to process ${source.fileSpec.packageName}.${source.name}")
            throw e
        }
    }

    private fun formatOutput(directory: File, fileSpec: FileSpec) {
        val path = fileSpec.toJavaFileObject().toUri().path
        val file = File(directory, path)
        val cb: (LintError, Boolean) -> Unit = { (line, col, ruleId, detail), corrected ->
            if (!corrected) {
                LOG.debug("Could not correct formatting error: ($line:$col) [$ruleId] $path: $detail")
            }
        }
        LOG.debug("Formatting generated file: $file")
        file.writeText(KtLint.format(file.readText(), ruleSets, mapOf(), cb))
    }

    private fun KotlinClass.asTypeName(): TypeName {
        val className = ClassName(fileSpec.packageName, name)
        if (source.typeVariables.isNotEmpty()) {
            className.parameterizedBy(*source.typeVariables.toTypedArray())
        }
        return className
    }

    private fun addCriteriaMethods(source: KotlinClass, criteriaClass: TypeSpec.Builder) {
        criteriaClass.addFunction(
                FunSpec.builder("query").returns(QUERY.parameterizedBy(source.asTypeName()))
                    .addCode("return query as Query<${source.name}>").build()
        )

        criteriaClass.addFunction(
                FunSpec.builder("delete")
                    .addParameter(
                        ParameterSpec.builder("options", DELETE_OPTIONS)
                            .defaultValue("DeleteOptions()").build())
                    .returns(WRITE_RESULT)
                    .addCode("""return ds.delete(query, options)""")
                    .build()
        )

        criteriaClass.addFunction(
                FunSpec.builder("or").returns(CRITERIA_CONTAINER).addParameter(
                        ParameterSpec.builder(
                                "criteria", CRITERIA
                        ).addModifiers(VARARG).build()
                ).addCode("""return query.or(*criteria)""").build()
        )

        criteriaClass.addFunction(
                FunSpec.builder("and").returns(CRITERIA_CONTAINER).addParameter(
                        ParameterSpec.builder(
                                "criteria", CRITERIA
                        ).addModifiers(VARARG).build()
                ).addCode("""return query.and(*criteria)""").build()
        )
    }

    private fun addPrefixProperty(criteriaClass: TypeSpec.Builder, constructorBuilder: FunSpec.Builder) {
        criteriaClass.addProperty(
                PropertySpec.builder(
                        "prefix", STRING
                ).addModifiers(PRIVATE).initializer("""fieldName?.let { fieldName + "." } ?: "" """).build()
        )
        constructorBuilder.addParameter(ParameterSpec.builder("fieldName", STRING.copy(nullable = true)).build())
    }

    private fun addField(source: KotlinClass, criteriaClass: TypeSpec.Builder, field: PropertySpec) {
        when {
            source.source.hasAnnotation(Reference::class.java) -> {
                criteriaClass.addFunction(
                        FunSpec.builder(field.name).addCode("""query.filter("${field.name} = ", reference)""").addParameter(
                                ParameterSpec.builder("reference", field.type).build()
                        ).returns(ClassName("${source.name}Criteria", "")).build()
                )
            }
            field.hasAnnotation(Embedded::class.java) -> {
                var type = field.type
                if (field.isContainer()) {
                    if (type is ParameterizedTypeName && type.typeArguments.isNotEmpty()) {
                        type = type.typeArguments.last()
                    }
                }

                val pkg = context.criteriaPkg ?: "${source.fileSpec.packageName}.criteria"
                val criteriaType = ClassName(pkg, "${(type as ClassName).simpleName}Criteria")
                criteriaClass.addFunction(
                        FunSpec.builder(field.name).returns(criteriaType).addCode(
                                """return %T(ds, query, "${field.name}")""", criteriaType
                        ).build()
                )
            }
            else -> {
                val name = if (field.hasAnnotation(Embedded::class.java) || source.source.hasAnnotation(Embedded::class.java)) {
                    "prefix + ${field.name}"
                } else {
                    field.name
                }
                val criteria = source.name + "Criteria"
                criteriaClass.addFunction(
                        FunSpec.builder(field.name).returns(
                                TYPESAFE_FIELD_END.parameterizedBy(
                                        TypeVariableName(criteria), field.type
                                )
                        ).addCode(CodeBlock.of("return TypeSafeFieldEnd(this, query, $name)")).build()
                )

                criteriaClass.addFunction(
                        FunSpec.builder(field.name).addParameter(
                                ParameterSpec.builder(
                                        "__newValue", field.type
                                ).build()
                        ).returns(CRITERIA).addCode(
                                CodeBlock.of(
                                        "return TypeSafeFieldEnd<$criteria, %T>(this, query, $name).equal(__newValue)", field.type
                                )
                        ).build()
                )
            }
        }
    }

    private fun TypeSpec.Builder.addFunction(name: String, returns: TypeName, body: String, vararg parameters: ParameterSpec) {
        addFunction(FunSpec.builder(name).returns(returns).addCode(CodeBlock.of(body)).addParameters(parameters.toList()).build())
    }

    private fun parameter(name: String, type: TypeName, defaultValue: String? = null): ParameterSpec {
        val builder = ParameterSpec.builder(name, type)
        defaultValue?.let {
            builder.defaultValue(defaultValue)
        }
        return builder.build()
    }

    private fun buildUpdater(sourceClass: KotlinClass, criteriaClass: TypeSpec.Builder) {
        val updaterType = ClassName("", "${sourceClass.name}Updater")
        criteriaClass.addFunction(
                FunSpec.builder("updater").returns(updaterType).addCode(
                        CodeBlock.of(
                                """return $updaterType(ds, query, ds.createUpdateOperations(${sourceClass.name}::class.java),
                    |if(prefix.isNotEmpty()) prefix else null)""".trimMargin()
                        )
                ).build()
        )

        val updater = TypeSpec.classBuilder(updaterType)
        val updaterCtor = FunSpec.constructorBuilder()

        addConstructorProperty(updaterCtor, updater, "ds", Datastore::class.java.asTypeName(), PRIVATE)
        addConstructorProperty(updaterCtor, updater, "query", QUERY.parameterizedBy(TypeVariableName("*")), PRIVATE)
        addConstructorProperty(
                updaterCtor, updater, "updateOperations", UPDATE_OPERATIONS.parameterizedBy(TypeVariableName("*")), PRIVATE
        )

        addPrefixProperty(updater, updaterCtor)

        updater.primaryConstructor(updaterCtor.build())
        if (!sourceClass.source.hasAnnotation(Embedded::class.java)) {
            updater.addFunction(
                    "update",
                    UPDATE_RESULTS, "return ds.update(query as Query<Any>, updateOperations as UpdateOperations<Any>, options)",
                    parameter("options", UPDATE_OPTIONS, "UpdateOptions()")
            )

            updater.addFunction(
                    "delete", WRITE_RESULT, "return ds.delete(query as Query<Any>, options)",
                    parameter("options", DELETE_OPTIONS, "DeleteOptions()")
            )
        }
        sourceClass.listProperties().forEach { field ->
            if (!field.hasAnnotation(Id::class.java)) {
                updater.addFunction(
                        field.name, updaterType, """
                            updateOperations.set(prefix + ${field.name}, __newValue)
                            return this
                            """.trimIndent(), parameter("__newValue", field.type)
                )

                updater.addFunction(
                        "unset${field.name.nameCase()}", updaterType, """
                            updateOperations.unset(prefix + ${field.name})
                            return this
                            """.trimIndent()
                )

                numbers(updaterType, updater, field)
                containers(updaterType, updater, field)
            }
        }
        criteriaClass.addType(updater.build())
    }

    private fun addConstructorProperty(
        ctorBuilder: Builder,
        classBuilder: TypeSpec.Builder,
        name: String,
        type: TypeName,
        vararg modifiers: KModifier
    ) {
        ctorBuilder.addParameter(ParameterSpec.builder(name, type).build())
        classBuilder.addProperty(PropertySpec.builder(name, type).initializer(name).addModifiers(*modifiers).build())
    }

    private fun numbers(type: ClassName, updater: TypeSpec.Builder, field: PropertySpec) {
        if (field.isNumeric()) {
            updater.addFunction(
                    "inc${field.name.nameCase()}", type, """
                updateOperations.inc(prefix + ${field.name}, __newValue)
                return this""".trimIndent(), parameter("__newValue", field.type, "1.to${field.type}()")
            )
        }
    }

    private fun containers(type: ClassName, updater: TypeSpec.Builder, field: PropertySpec) {
        if (field.isContainer()) {

            val type1 = field.type
            type1 as ParameterizedTypeName
            updater.addFunction(
                    "addTo${field.name.nameCase()}", type, """updateOperations.addToSet(prefix + ${field.name}, __newValue)
                        |return this""".trimMargin(), parameter("__newValue", type1)
            )

            updater.addFunction(
                    "pushTo${field.name.nameCase()}", type, """updateOperations.push(prefix + ${field.name}, __newValue)
                        |return this """.trimMargin(), parameter("__newValue", type1)
            )

            updater.addFunction(
                    "pushTo${field.name.nameCase()}", type, """updateOperations.push(prefix + ${field.name}, __newValue)
                            |return this """.trimMargin(), parameter("__newValue", type1.typeArguments.last())
            )

            updater.addFunction(
                    "removeFirstFrom${field.name.nameCase()}", type, """updateOperations.removeFirst(prefix + ${field.name})
                        |return this """.trimMargin()
            )

            updater.addFunction(
                    "removeLastFrom${field.name.nameCase()}", type, """updateOperations.removeLast(prefix + ${field.name})
                        |return this """.trimMargin()
            )

            updater.addFunction(
                    "removeFrom${field.name.nameCase()}", type, """updateOperations.removeAll(prefix + ${field.name}, __newValue)
                        |return this """.trimMargin(), parameter("__newValue", type1)
            )

            updater.addFunction(
                    "removeAllFrom${field.name.nameCase()}", type, """updateOperations.removeAll(prefix + ${field.name}, values)
                        |return this """.trimMargin(), parameter("values", type1)
            )
        }
    }
}

private fun PropertySpec.isContainer() = type.toString().substringBefore("<") in CritterField.CONTAINER_TYPES

private fun PropertySpec.isNumeric() = CritterField.NUMERIC_TYPES.contains(type.toString())

fun <T : Annotation> PropertySpec.getAnnotation(annotation: Class<T>): AnnotationSpec? {
    return annotations.firstOrNull { it.className == annotation.asTypeName() }
}

fun <T : Annotation> PropertySpec.hasAnnotation(annotation: Class<T>): Boolean {
    return getAnnotation(annotation) != null
}

fun <T : Annotation> TypeSpec.getAnnotation(annotation: Class<T>): AnnotationSpec? {
    return annotationSpecs.firstOrNull { it.className == annotation.asTypeName() }
}

fun <T : Annotation> TypeSpec.hasAnnotation(annotation: Class<T>): Boolean {
    return getAnnotation(annotation) != null
}

fun AnnotationSpec.getValue(name: String = "value"): String? {
    return members.map { it.toPair() }.firstOrNull { it.first == name }?.second
}

fun PropertySpec.mappedName(): String {
    val annotation = getAnnotation(Id::class.java)
    return if (annotation != null) {
        "_id"
    } else {
        getAnnotation(Embedded::class.java)?.getValue() ?: getAnnotation(Property::class.java)?.getValue() ?: name
    }
}
