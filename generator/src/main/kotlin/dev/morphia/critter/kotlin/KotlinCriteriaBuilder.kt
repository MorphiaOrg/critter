@file:Suppress("DEPRECATION")

package dev.morphia.critter.kotlin

import com.google.devtools.ksp.hasAnnotation
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.mongodb.client.model.geojson.Geometry
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
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
import dev.morphia.critter.FilterSieve
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.UpdateSieve
import dev.morphia.critter.titleCase
import dev.morphia.query.filters.Filters
import dev.morphia.query.updates.UpdateOperators
import java.time.temporal.Temporal
import java.util.Date
import org.jboss.forge.roaster._shade.org.eclipse.jdt.internal.compiler.parser.Parser.name
import org.slf4j.LoggerFactory

class KotlinCriteriaBuilder(val context: KotlinContext) : SourceBuilder {
    companion object {
        private val STRING = String::class.asClassName()
        private val NULLABLE_STRING = STRING.copy(nullable = true)
        private val LOG = LoggerFactory.getLogger(KotlinCriteriaBuilder::class.java)
    }

    override fun build() {
        context.entities().values.forEach {
            build(it)
        }
    }

    private fun build(source: KSClassDeclaration) {
        val criteriaPkg = context.criteriaPkg ?: "${source.packageName.asString()}.criteria"
        val fileBuilder = FileSpec.builder(criteriaPkg, "${source.name()}Criteria")

        try {
            if (!source.isAbstract()) {
                val criteriaName = "${source.name()}Criteria"
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
                val properties = source.getAllProperties().iterator()
                if (properties.hasNext()) {
                    buildCompanionObject(criteriaName, source, criteriaClass)
                }

                properties.forEach { property ->
                    addField(criteriaClass, property)
                }

                fileBuilder.addType(criteriaClass.build())
                context.buildFile(fileBuilder
                        .addImport(Filters::class.java.packageName, "Filters", "Filter")
                        .addImport(UpdateOperators::class.java.packageName, "UpdateOperators", "UpdateOperator")
                        .build()
                )
            }
        } catch (e: Exception) {
            LOG.error("Failed to process ${source.packageName.asString()}.${source.name()}")
            throw e
        }
    }

    private fun buildCompanionObject(
        criteriaName: String,
        source: KSClassDeclaration,
        criteriaClass: Builder
    ) {
        TypeSpec.companionObjectBuilder().apply {
            val propertyName = "__criteria"
            addProperty(
                PropertySpec.builder(propertyName, ClassName("", criteriaName), PRIVATE)
                    .initializer("""${criteriaName}()""")
                    .build()
            )

            source.getAllProperties().forEach { property ->
                val mappedName = property.mappedName()
                addProperty(
                    PropertySpec.builder(property.name(), STRING)
                        .initializer("""${mappedName}""")
                        .build()
                )
                addFunction(
                    FunSpec.builder(property.name())
                        .addCode(CodeBlock.of("return ${propertyName}.${property.name()}()"))
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

    private fun Builder.addFieldCriteriaMethod(property: KSPropertyDeclaration) {
        val concreteType = property.type.className()
        val annotations = context.entities()[concreteType]?.annotations
        val none = annotations?.none {
            it.annotationType.packageName().startsWith("dev.morphia.annotations")
        } ?: true
        val fieldCriteriaName = if (none) {
            property.name().titleCase() + "FieldCriteria"
        } else {
            property.type.simpleName() + "Criteria"
        }
        val path = """extendPath(path, "${property.name()}")"""
        addFunction(
            FunSpec.builder(property.name())
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

    private fun addField(criteriaClass: Builder, property: KSPropertyDeclaration) {
        if (property.hasAnnotation(Reference::class.java)) {
            addReferenceCriteria(criteriaClass, property)
        } else {
            criteriaClass.addFieldCriteriaMethod(property)

            if (!property.isMappedType()) {
                addFieldCriteriaClass(property, criteriaClass)
            }
        }
    }

    private fun addFieldCriteriaClass(property: KSPropertyDeclaration, criteriaClass: Builder) {
        TypeSpec.classBuilder("${property.name().titleCase()}FieldCriteria")
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

                attachFilters(property)
                attachUpdates(property)
                criteriaClass.addType(build())
            }
    }

    private fun addReferenceCriteria(criteriaClass: Builder, property: KSPropertyDeclaration) {
        val fieldCriteriaName = property.name().titleCase() + "FieldCriteria"
        criteriaClass.addFunction(
            FunSpec.builder(property.name())
                .addCode(CodeBlock.of("return ${fieldCriteriaName}(extendPath(path, \"${property.name()}\"))"))
                .build()
        )
        addFieldCriteriaClass(property, criteriaClass)
    }

    private fun KSPropertyDeclaration.mappedType(): KSClassDeclaration? {
        return context.entities()[type.className()]
    }

    private fun KSPropertyDeclaration.isMappedType(): Boolean {
        return mappedType()?.annotations?.any {
            it.annotationType.packageName() in listOf(Entity::class.java.simpleName, Embedded::class.java.simpleName)
        } ?: false
    }
}

internal fun KSDeclaration.name() = simpleName.asString()

fun <T : Annotation> PropertySpec.getAnnotation(annotation: Class<T>): AnnotationSpec? {
    return annotations.firstOrNull { it.typeName == annotation.asTypeName() }
}
fun <T : Annotation> KSPropertyDeclaration.getAnnotation(annotation: Class<T>): KSAnnotation? {
    return annotations.firstOrNull { it.annotationType.className() == annotation.name }
}

fun <T : Annotation> PropertySpec.hasAnnotation(annotation: Class<T>): Boolean {
    return getAnnotation(annotation) != null
}
fun <T : Annotation> KSPropertyDeclaration.hasAnnotation(annotation: Class<T>): Boolean {
    return getAnnotation(annotation) != null
}

fun AnnotationSpec.getValue(name: String = "value"): String? {
    return members.map { it.toPair() }.firstOrNull { it.first == name }?.second
}

private fun Builder.attachFilters(property: KSPropertyDeclaration) {
    FilterSieve.handlers(property, this)
}

private fun Builder.attachUpdates(property: KSPropertyDeclaration) {
    UpdateSieve.handlers(property, this)
}

fun String.className(): ClassName {
    return ClassName.bestGuess(this)
}
fun KSPropertyDeclaration.mappedName(): String {
    return if (hasAnnotation(Id::class.java.name)) {
        "\"_id\""
    } else {
        val embedAsString = getAnnotation(Embedded::class.java)?.valueAsString()
        val propertyAsString = getAnnotation(Property::class.java)?.valueAsString()
        val name = simpleName()
        embedAsString
            ?: propertyAsString
            ?: "\"$name\""
    }
}

private fun KSAnnotation.valueAsString(): String? {
    val value = arguments.firstOrNull { argument ->
        val name = argument.name?.asString()
        name == null || name == "value"
    }?.value?.toString()
    return value?.let { "\"$it\"" }
}

fun KSPropertyDeclaration.isContainer(): Boolean = type.className() in CritterType.CONTAINER_TYPES

fun KSPropertyDeclaration.isGeoCompatible(): Boolean {
    return name() in CritterType.GEO_TYPES || try {
        Geometry::class.java.isAssignableFrom(Class.forName(type.className()))
    } catch (_: Exception) {
        false
    }
}

fun KSPropertyDeclaration.isNumeric(): Boolean {
return    CritterType.NUMERIC_TYPES.contains(name())
        || try {
        val clazz = Class.forName(type.className())
        Temporal::class.java.isAssignableFrom(clazz)
            || Date::class.java.isAssignableFrom(clazz)
    } catch (_: Exception) {
        false
    }
}

fun KSPropertyDeclaration.isText(): Boolean = CritterType.TEXT_TYPES.contains(name())
