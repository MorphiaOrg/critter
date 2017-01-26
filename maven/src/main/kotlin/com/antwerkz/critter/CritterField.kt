package com.antwerkz.critter

import org.jboss.forge.roaster.model.Field
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.util.Strings
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Property
import org.mongodb.morphia.annotations.Reference
import org.mongodb.morphia.query.Criteria
import org.mongodb.morphia.query.FieldEndImpl
import org.mongodb.morphia.query.QueryImpl
import java.lang.String.format
import java.util.ArrayList

class CritterField(private val context: CritterContext, val source: Field<JavaClassSource>) : Comparable<CritterField> {
    private val shortParameterTypes = ArrayList<String>()

    private val fullParameterTypes = ArrayList<String>()

    val fullType: String

    init {
        val nestedType = source.origin.getNestedType(source.type.name)
        fullType = nestedType?.getCanonicalName() ?: source.type.qualifiedName


        if (source.type.isParameterized) {
            val typeArguments = source.type.typeArguments
            for (typeArgument in typeArguments) {
                shortParameterTypes.add(typeArgument.name)
                fullParameterTypes.add(typeArgument.qualifiedName)
            }
        }
    }

    override fun compareTo(other: CritterField): Int {
        return source.name.compareTo(other.source.name)
    }

    val name: String = source.name

    fun hasAnnotation(aClass: Class<out Annotation>): Boolean {
        return source.hasAnnotation(aClass)
    }

    val isContainer: Boolean?
        get() {
            val qualifiedName = source.type.qualifiedName
            return qualifiedName == "java.util.List" || qualifiedName == "java.util.Set"
        }

    val isNumeric: Boolean
        get() = NUMERIC_TYPES.contains(source.type.qualifiedName)

    val parameterTypes: List<String>
        get() = fullParameterTypes

    val parameterizedType: String
        get() {
            if (parameterTypes.isEmpty()) {
                return fullType
            } else {
                return format("%s<%s>", fullType, Strings.join(fullParameterTypes, ", "))
            }
        }

    val fullyQualifiedType: String
        get() {
            val qualifiedName = source.type.qualifiedName
            val typeArguments = source.type.typeArguments
            val types = if (typeArguments.isEmpty())
                ""
            else
                "<" + typeArguments
                        .map { it.getQualifiedName() }
                        .joinToString(",") + ">"
            return format("%s%s", qualifiedName, types)
        }

    override fun toString(): String {
        return "CritterField{" +
                "source=" + source +
                '}'
    }

    fun build(critterClass: CritterClass, criteriaClass: JavaClassSource) {
        if (source.hasAnnotation(Reference::class.java)) {
            buildReference(criteriaClass)
        } else if (hasAnnotation(Embedded::class.java)) {
            buildEmbed(criteriaClass)
        } else if (!source.isStatic) {
            buildField(critterClass, criteriaClass)
        }
    }

    fun buildReference(criteriaClass: JavaClassSource) {
        criteriaClass.addMethod()
                .setPublic()
                .setName(source.name)
                .setReturnType(criteriaClass)
                .setBody("""query.filter("${source.name} = ", reference);
return this;""")
                .addParameter(source.type.qualifiedName, "reference")
    }

    fun buildEmbed(criteriaClass: JavaClassSource) {
        criteriaClass.addImport(Criteria::class.java)
        val criteriaType: String
        if (!shortParameterTypes.isEmpty()) {
            criteriaType = shortParameterTypes[0] + "Criteria"
            criteriaClass.addImport(criteriaClass.`package` + "." + criteriaType)
        } else {
            val type = source.type
            criteriaType = type.qualifiedName + "Criteria"
            criteriaClass.addImport(source.type.qualifiedName)
        }
        val method = criteriaClass.addMethod()
                .setPublic()
                .setName(source.name)
                .setReturnType(criteriaType)
        method.body = format("return new %s(query, \"%s\");", method.returnType.name, source.name)
    }

    fun buildField(critterClass: CritterClass, criteriaClass: JavaClassSource) {
        val qualifiedName = critterClass.sourceClass.qualifiedName
        criteriaClass.addImport(qualifiedName)
        criteriaClass.addImport(fullType)
        criteriaClass.addImport(Criteria::class.java)
        criteriaClass.addImport(FieldEndImpl::class.java)
        criteriaClass.addImport(QueryImpl::class.java)

        var name = "\"" + source.name + "\""
        if (source.origin.hasAnnotation(Embedded::class.java) || context.isEmbedded(source.origin)) {
            name = "prefix + " + name
        }

        criteriaClass.addMethod()
                .setPublic()
                .setName(source.name)
                .setReturnType(format("%s<%s, %s, %s>", TypeSafeFieldEnd::class.java.name, criteriaClass.qualifiedName,
                        critterClass.sourceClass.qualifiedName, fullType)).body = format("return new TypeSafeFieldEnd<%s, %s, %s>(this, query, %s);",
                criteriaClass.name, critterClass.sourceClass.name, fullType, name)

        val method = criteriaClass.addMethod()
                .setName(source.name)
                .setPublic()
                .setReturnType(Criteria::class.java)
                .setBody(format("return new TypeSafeFieldEnd<%s, %s, %s>(this, query, %s).equal(value);",
                        criteriaClass.name, critterClass.sourceClass.name, fullType, name))
        method.addParameter(parameterizedType, "value")
    }

    fun mappedName(): String {
        var name = name
        name = extract(name, Property::class.java)
        name = extract(name, Embedded::class.java)

        return name
    }

    private fun extract(name: String, ann: Class<out Annotation>): String {
        val annotation = source.getAnnotation(ann)
        return if (annotation != null && annotation.getStringValue("value") != null)
            annotation.getStringValue("value")
        else
            name
    }

    companion object {

        val NUMERIC_TYPES: MutableList<String> = ArrayList()

        init {
            NUMERIC_TYPES.add("java.lang.Float")
            NUMERIC_TYPES.add("java.lang.Double")
            NUMERIC_TYPES.add("java.lang.Long")
            NUMERIC_TYPES.add("java.lang.Integer")
            NUMERIC_TYPES.add("java.lang.Byte")
            NUMERIC_TYPES.add("java.lang.Short")
            NUMERIC_TYPES.add("java.lang.Number")
        }
    }
}
