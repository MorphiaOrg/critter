package com.antwerkz.critter.java

import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.TypeSafeFieldEnd
import com.antwerkz.critter.Visible
import org.jboss.forge.roaster.model.source.FieldSource
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

class JavaField(private val context: CritterContext, val source: FieldSource<JavaClassSource>) : CritterField {
    override val shortParameterTypes = ArrayList<String>()

    override val fullParameterTypes = ArrayList<String>()

    override val fullType: String

    override val name: String = source.name

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
        return name.compareTo(other.name)
    }

    override fun hasAnnotation(aClass: Class<out Annotation>): Boolean {
        return source.hasAnnotation(aClass)
    }

    override fun isStatic(): Boolean {
        return source.isStatic
    }

    override val parameterTypes: List<String>
        get() = fullParameterTypes

    override val parameterizedType: String
        get() = if (parameterTypes.isEmpty()) fullType else format("%s<%s>", fullType, Strings.join(fullParameterTypes, ", "))

    override val fullyQualifiedType: String by lazy {
        val qualifiedName = source.type.qualifiedName
        val typeArguments = source.type.typeArguments
        val types = if (typeArguments.isEmpty())
            ""
        else
            "<" + typeArguments
                    .map { it.qualifiedName }
                    .joinToString(",") + ">"
        "$qualifiedName$types"
    }

    override fun toString(): String {
        return "JavaField {source=$source}"
    }

    override fun build(sourceClass: CritterClass, targetClass: CritterClass) {
        if (source.hasAnnotation(Reference::class.java)) {
            buildReference(targetClass)
        } else if (hasAnnotation(Embedded::class.java)) {
            buildEmbed(targetClass)
        } else if (!source.isStatic) {
            buildField(sourceClass, targetClass)
        }
    }

    override fun buildReference(criteriaClass: CritterClass) {
        criteriaClass.addMethod()
                .setPublic()
                .setName(source.name)
                .setReturnType(criteriaClass.qualifiedName)
                .setBody("""query.filter("${source.name} = ", reference);
return this;""")
                .addParameter(source.type.qualifiedName, "reference")
    }

    override fun buildEmbed(criteriaClass: CritterClass) {
        criteriaClass.addImport(Criteria::class.java)
        val criteriaType: String
        if (!shortParameterTypes.isEmpty()) {
            criteriaType = shortParameterTypes[0] + "Criteria"
            criteriaClass.addImport("${criteriaClass.getPackage()}.$criteriaType")
        } else {
            val type = source.type
            criteriaType = type.qualifiedName + "Criteria"
            criteriaClass.addImport(source.type.qualifiedName)
        }
        val method = criteriaClass.addMethod()
                .setPublic()
                .setName(source.name)
                .setReturnType(criteriaType)
        method.setBody("return new ${method.getReturnType()}(query, \"${source.name}\");")
    }

    override fun buildField(critterClass: CritterClass, criteriaClass: CritterClass) {
        val qualifiedName = critterClass.qualifiedName
        criteriaClass.addImport(qualifiedName)
        criteriaClass.addImport(fullType)
        criteriaClass.addImport(Criteria::class.java)
        criteriaClass.addImport(FieldEndImpl::class.java)
        criteriaClass.addImport(QueryImpl::class.java)

        var name = "\"" + source.name + "\""
        val origin = source.origin
        if (origin.hasAnnotation(Embedded::class.java) || context.isEmbedded(origin.`package`, origin.name)) {
            name = "prefix + " + name
        }

        criteriaClass.addMethod()
                .setPublic()
                .setName(source.name)
                .setReturnType("${TypeSafeFieldEnd::class.java.name}<${criteriaClass.qualifiedName}, $fullType>")
                .setBody("return new TypeSafeFieldEnd<${criteriaClass.getName()}, $fullType>(this, query, $name);")

        val method = criteriaClass.addMethod()
                .setName(source.name)
                .setPublic()
                .setReturnType(Criteria::class.java)
                .setBody("return new TypeSafeFieldEnd<${criteriaClass.getName()}, $fullType>(this, query, $name).equal(value);")
        method.addParameter(parameterizedType, "value")
    }

    override fun mappedName(): String {
        var name = name
        name = extract(name, Property::class.java)
        name = extract(name, Embedded::class.java)

        return name
    }

    override fun extract(name: String, ann: Class<out Annotation>): String {
        return source.getAnnotation(ann)?.getStringValue("value") ?: name
    }

    override fun isPublic() = source.isPublic
    override fun setPublic(): CritterField {
        source.setPublic()
        return this
    }

    override fun isPrivate() = source.isPrivate
    override fun setPrivate(): CritterField {
        source.setPrivate()
        return this
    }

    override fun isPackagePrivate() = source.isPackagePrivate
    override fun setPackagePrivate(): CritterField {
        source.setPackagePrivate()
        return this
    }

    override fun isProtected() = source.isProtected
    override fun setProtected(): CritterField {
        source.setProtected()
        return this
    }

    override fun isInternal() = false
    override fun setInternal() = throw Visible.invalid("internal", "java")

    override fun setStatic(): CritterField {
        source.isStatic = true
        return this
    }

    override fun setFinal(): CritterField {
        source.isFinal = true
        return this
    }

    override fun setLiteralInitializer(initializer: String): CritterField {
        source.literalInitializer = initializer
        return this
    }

    override fun setStringLiteralInitializer(initializer: String): CritterField {
        source.stringInitializer = initializer
        return this
    }
}
