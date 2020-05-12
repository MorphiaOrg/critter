package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterField
import com.antwerkz.critter.FilterSieve
import com.antwerkz.critter.UpdateSieve
import com.antwerkz.critter.java.toMethodCase
import com.antwerkz.critter.java.toTitleCase
import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.core.RuleSet
import com.github.shyiko.ktlint.core.RuleSetProvider
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INLINE
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Id
import dev.morphia.annotations.Property
import dev.morphia.annotations.Reference
import dev.morphia.query.experimental.filters.Filters
import dev.morphia.query.experimental.updates.UpdateOperators
import org.jetbrains.kotlin.resolve.calls.smartcasts.IdentifierInfo.NULL
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Comparator.comparingInt
import java.util.ServiceLoader

@ExperimentalStdlibApi
class KotlinBuilder(val context: KotlinContext) {
    companion object {
        private val STRING = String::class.asClassName()
        private val NULLABLE_STRING = STRING.copy(nullable = true)
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
                val criteriaName = "${source.name}Criteria"
                val criteriaClass = TypeSpec.classBuilder(criteriaName)
                criteriaClass.addAnnotation(
                        AnnotationSpec.builder(Suppress::class.java).addMember(CodeBlock.of("\"UNCHECKED_CAST\"")).build()
                )

                criteriaClass.primaryConstructor(
                        FunSpec.constructorBuilder()
                                .addModifiers(INTERNAL)
                                .addParameter(ParameterSpec.builder("prefix", NULLABLE_STRING)
                                        .defaultValue("null")
                                        .build())
                                .build())

                criteriaClass.addProperty(
                        PropertySpec.builder("prefix", NULLABLE_STRING)
                                .initializer("prefix")
                                .addModifiers(PRIVATE)
                                .build())

                if (source.fields.isNotEmpty()) {
                    TypeSpec.companionObjectBuilder().apply {
                        val propertyName = source.name.toMethodCase()
                        addProperty(PropertySpec.builder(propertyName, ClassName("", criteriaName), PRIVATE)
                                .initializer("""${criteriaName}()""")
                                .build())

                        source.fields.forEach { field ->
                            addProperty(PropertySpec.builder(field.name, STRING).initializer(""""${field.mappedName()}"""")
                                    .build())
                            addFunction(FunSpec.builder(field.name)
                                    .addCode(CodeBlock.of("return ${propertyName}.${field.name}()"))
                                    .build())
                        }

                        addFunction(FunSpec.builder("extendPath")
                                .addModifiers(PRIVATE, INLINE)
                                .addParameter("prefix", NULLABLE_STRING)
                                .addParameter("path", STRING)
                                .addCode("""return prefix?.let { prefix + "." + path } ?: path""")
                                .build())
                        criteriaClass.addType(build())
                    }
                }

                source.fields.forEach { field ->
                    addField(criteriaClass, field)
                }

                fileBuilder.addType(criteriaClass.build())
                val fileSpec = fileBuilder
                        .addImport(Filters::class.java.packageName, "Filters", "Filter")
                        .addImport(UpdateOperators::class.java.packageName, "UpdateOperators", "UpdateOperator")
                        .build()
                fileSpec.writeTo(directory)

                formatOutput(directory, fileSpec)
            }
        } catch (e: Exception) {
            LOG.error("Failed to process ${source.fileSpec.packageName}.${source.name}")
            throw e
        }
    }

    private fun Builder.addFieldCriteriaMethod(field: PropertySpec) {
        val concreteType = field.type.concreteType()
        val annotations = context.classes[concreteType.canonicalName]?.annotations
        val fieldCriteriaName = if (annotations == null) {
            field.name.toTitleCase() + "FieldCriteria"
        } else {
            concreteType.simpleName + "Criteria"
        }
        var prefix = "prefix"
        if(field.isMappedType()) {
            prefix = """extendPath(prefix, "${field.name}")"""
        }
        addFunction(FunSpec.builder(field.name)
                .addCode(CodeBlock.of("return ${fieldCriteriaName}(${prefix})"))
                .build())
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

    private fun addField(criteriaClass: Builder, field: PropertySpec) {
        if (field.hasAnnotation(Reference::class.java)) {
            addReferenceCriteria(criteriaClass, field)
        } else {
            criteriaClass.addFieldCriteriaMethod(field)
            if (!field.isMappedType()) {
                addFieldCriteriaClass(field, criteriaClass)
            }
        }
    }
    private fun addFieldCriteriaClass(field: PropertySpec, criteriaClass: Builder) {
        TypeSpec.classBuilder("${field.name.toTitleCase()}FieldCriteria")
                .apply {
                    primaryConstructor(FunSpec.constructorBuilder()
                            .addModifiers(INTERNAL)
                            .addParameter(ParameterSpec.builder("prefix", NULLABLE_STRING)
                                    .defaultValue("null")
                                    .build())
                            .build())
                            .addProperty(PropertySpec.builder("prefix", NULLABLE_STRING)
                                    .initializer("prefix")
                                    .addModifiers(PRIVATE)
                                    .build())

                    attachFilters(field)
                    attachUpdates(field)
                    addFunction(FunSpec.builder("path")
                            .addCode("""return extendPath(prefix, "${field.name}")""")
                            .build())
                    criteriaClass.addType(build())
                }
    }

    private fun addReferenceCriteria(criteriaClass: Builder, field: PropertySpec) {
        val fieldCriteriaName = field.name.toTitleCase() + "FieldCriteria"
        criteriaClass.addFunction(FunSpec.builder(field.name)
                .addCode(CodeBlock.of("return ${fieldCriteriaName}(prefix)"))
                .build())
        addFieldCriteriaClass(field, criteriaClass)
    }

    fun PropertySpec.mappedType(): KotlinClass? {
        return context.classes[type.concreteType().canonicalName]
    }

    fun PropertySpec.isMappedType(): Boolean {
        return mappedType() != null
    }
}

private fun TypeName.concreteType(): ClassName {
    return when (this) {
        is ClassName -> this
        is ParameterizedTypeName -> typeArguments.last().concreteType()
        else -> TODO(toString())
    }
}

fun PropertySpec.isContainer() = type.toString().substringBefore("<") in CritterField.CONTAINER_TYPES
fun PropertySpec.isNumeric() = CritterField.NUMERIC_TYPES.contains(type.toString())
fun PropertySpec.isText() = CritterField.TEXT_TYPES.contains(type.toString())
fun <T : Annotation> PropertySpec.getAnnotation(annotation: Class<T>): AnnotationSpec? {
    return annotations.firstOrNull { it.className == annotation.asTypeName() }
}

fun <T : Annotation> PropertySpec.hasAnnotation(annotation: Class<T>): Boolean {
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

@ExperimentalStdlibApi
private fun Builder.attachFilters(field: PropertySpec) {
    FilterSieve.handlers(field, this)
}

@ExperimentalStdlibApi
private fun Builder.attachUpdates(field: PropertySpec) {
    UpdateSieve.handlers(field, this)
}

