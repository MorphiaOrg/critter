@file:Suppress("DEPRECATION")

package dev.morphia.critter.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
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
import dev.morphia.annotations.Entity
import dev.morphia.annotations.Id
import dev.morphia.annotations.Property
import dev.morphia.annotations.Reference
import dev.morphia.critter.CritterType
import dev.morphia.critter.CritterType.Companion.isNumeric
import dev.morphia.critter.FilterSieve
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.UpdateSieve
import dev.morphia.critter.titleCase
import dev.morphia.query.filters.Filters
import dev.morphia.query.updates.UpdateOperators
import org.slf4j.LoggerFactory
import java.io.File

class KotlinCriteriaBuilder(val context: KotlinContext) : SourceBuilder {
    companion object {
        private val STRING = String::class.asClassName()
        private val NULLABLE_STRING = STRING.copy(nullable = true)
        private val LOG = LoggerFactory.getLogger(KotlinCriteriaBuilder::class.java)
    }

    override fun build() {
        context.entities().values.forEach {
            build(context.outputDirectory, it)
        }
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

                criteriaClass.primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addModifiers(INTERNAL)
                        .addParameter(
                            ParameterSpec.builder("path", NULLABLE_STRING)
                                .defaultValue("null")
                                .build()
                        )
                        .build()
                )

                criteriaClass.addProperty(
                    PropertySpec.builder("path", NULLABLE_STRING)
                        .initializer("path")
                        .build()
                )

                if (source.properties.isNotEmpty()) {
                    buildCompanionObject(criteriaName, source, criteriaClass)
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
//                formatOutput(directory, fileSpec)
            }
        } catch (e: Exception) {
            LOG.error("Failed to process ${source.fileSpec.packageName}.${source.name}")
            throw e
        }
    }

    private fun buildCompanionObject(
        criteriaName: String,
        source: KotlinClass,
        criteriaClass: Builder
    ) {
        TypeSpec.companionObjectBuilder().apply {
            val propertyName = "__criteria"
            addProperty(
                PropertySpec.builder(propertyName, ClassName("", criteriaName), PRIVATE)
                    .initializer("""${criteriaName}()""")
                    .build()
            )

            source.properties.forEach { field ->
                addProperty(
                    PropertySpec.builder(field.name, STRING)
                        .initializer("""${field.mappedName()}""")
                        .build()
                )
                addFunction(
                    FunSpec.builder(field.name)
                        .addCode(CodeBlock.of("return ${propertyName}.${field.name}()"))
                        .build()
                )
            }

            addFunction(
                FunSpec.builder("extendPath")
                    .addModifiers(PRIVATE)
                    .addParameter("path", NULLABLE_STRING)
                    .addParameter("addition", STRING)
                    .addCode("""return path?.let { path + "." + addition } ?: addition""")
                    .build()
            )
            criteriaClass.addType(build())
        }
    }

    private fun Builder.addFieldCriteriaMethod(field: PropertySpec) {
        val concreteType = field.type.concreteType()
        val annotations = context.entities()[concreteType.canonicalName]?.annotations
        val none = annotations?.none { it.type.packageName.startsWith("dev.morphia.annotations") } ?: true
        val fieldCriteriaName = if (none) {
            field.name.titleCase() + "FieldCriteria"
        } else {
            concreteType.simpleName + "Criteria"
        }
        var path = """extendPath(path, "${field.name}")"""
        addFunction(
            FunSpec.builder(field.name)
                .addCode(CodeBlock.of("return ${fieldCriteriaName}(${path})"))
                .build()
        )
    }

/*
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
*/

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
        TypeSpec.classBuilder("${field.name.titleCase()}FieldCriteria")
            .apply {
                primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addModifiers(INTERNAL)
                        .addParameter(
                            ParameterSpec.builder("path", STRING)
                                .build()
                        )
                        .build()
                )
                    .addProperty(
                        PropertySpec.builder("path", STRING)
                            .initializer("path")
                            .build()
                    )

                attachFilters(field)
                attachUpdates(field)
                criteriaClass.addType(build())
            }
    }

    private fun addReferenceCriteria(criteriaClass: Builder, field: PropertySpec) {
        val fieldCriteriaName = field.name.titleCase() + "FieldCriteria"
        criteriaClass.addFunction(
            FunSpec.builder(field.name)
                .addCode(CodeBlock.of("return ${fieldCriteriaName}(extendPath(path, \"${field.name}\"))"))
                .build()
        )
        addFieldCriteriaClass(field, criteriaClass)
    }

    fun PropertySpec.mappedType(): KotlinClass? {
        return context.entities()[type.concreteType().canonicalName]
    }

    fun PropertySpec.isMappedType(): Boolean {
        val mappedType = mappedType()
        return mappedType != null && mappedType.annotations.any {
            it.type.packageName in listOf(Entity::class.java.simpleName, Embedded::class.java.simpleName)
        }
    }
}

private fun TypeName.concreteType(): ClassName {
    return when (this) {
        is ClassName -> this
        is ParameterizedTypeName -> typeArguments.last().concreteType()
        else -> TODO(toString())
    }
}

fun PropertySpec.isContainer(): Boolean {
    return type.toString().substringBefore("<") in CritterType.CONTAINER_TYPES
}
fun PropertySpec.isNumeric() = isNumeric(type.concreteType().canonicalName)
fun PropertySpec.isText() = CritterType.TEXT_TYPES.contains(type.concreteType().canonicalName)
fun <T : Annotation> PropertySpec.getAnnotation(annotation: Class<T>): AnnotationSpec? {
    return annotations.firstOrNull { it.typeName == annotation.asTypeName() }
}

fun <T : Annotation> PropertySpec.hasAnnotation(annotation: Class<T>): Boolean {
    return getAnnotation(annotation) != null
}

fun AnnotationSpec.getValue(name: String = "value"): String? {
    return members.map { it.toPair() }.firstOrNull { it.first == name }?.second
}

@Suppress("DEPRECATION")
fun PropertySpec.mappedName(): String {
    val annotation = getAnnotation(Id::class.java)
    return if (annotation != null) {
        "_id"
    } else {
        getAnnotation(Embedded::class.java)?.getValue() ?: getAnnotation(Property::class.java)?.getValue() ?: name
    }
}

private fun Builder.attachFilters(field: PropertySpec) {
    FilterSieve.handlers(field, this)
}

private fun Builder.attachUpdates(field: PropertySpec) {
    UpdateSieve.handlers(field, this)
}

fun String.className(): ClassName {
    return ClassName.bestGuess(this)
}
