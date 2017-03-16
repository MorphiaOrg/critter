package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.TypeSafeFieldEnd
import com.antwerkz.critter.Visible
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleProperty
import com.antwerkz.kibble.model.Visibility.INTERNAL
import com.antwerkz.kibble.model.Visibility.PRIVATE
import com.antwerkz.kibble.model.Visibility.PROTECTED
import com.antwerkz.kibble.model.Visibility.PUBLIC
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Reference
import org.mongodb.morphia.query.Criteria
import org.mongodb.morphia.query.FieldEndImpl
import org.mongodb.morphia.query.QueryImpl

class KotlinField(private val context: CritterContext, val source: KibbleProperty) : CritterField {
    override val shortParameterTypes = mutableListOf<String>()
    override val fullParameterTypes = mutableListOf<String>()
    override val fullType: String = source.type.toString()
    override val name: String
        get() = source.name
    override val parameterTypes: List<String>
        get() = source.type.parameters.map { it.toString() }
    override val parameterizedType: String
        get() = source.type.toString()

    override val fullyQualifiedType: String
        get() = source.type.qualifiedName

    init {
        if (source.isParameterized()) {
            source.type.parameters.forEach {
                shortParameterTypes.add(it.name)
                fullParameterTypes.add(it.qualifiedName)
            }
        }
    }

    override fun isPublic() = source.isPublic()
    override fun setPublic(): CritterField {
        source.visibility = PUBLIC
        return this
    }

    override fun isPrivate() = source.isPrivate()
    override fun setPrivate(): CritterField {
        source.visibility = PRIVATE
        return this
    }

    override fun isProtected() = source.isProtected()
    override fun setProtected(): CritterField {
        source.visibility = PROTECTED
        return this
    }

    override fun isInternal() = source.isInternal()
    override fun setInternal(): CritterField {
        source.visibility = INTERNAL
        return this
    }

    override fun isPackagePrivate() = false
    override fun setPackagePrivate() = throw Visible.invalid("package private", "kotlin")

    override fun hasAnnotation(aClass: Class<out Annotation>) = source.hasAnnotation(aClass)

    override fun isStatic() = false

    override fun build(sourceClass: CritterClass, targetClass: CritterClass) {
        if (source.hasAnnotation(Reference::class.java)) {
            buildReference(targetClass)
        } else if (hasAnnotation(Embedded::class.java)) {
            buildEmbed(targetClass)
        } else {
            buildField(sourceClass, targetClass)
        }
    }

    override fun buildReference(criteriaClass: CritterClass) {
        criteriaClass.addMethod()
                .setPublic()
                .setName(source.name)
                .setReturnType(criteriaClass.qualifiedName)
                .setBody("""query.filter("${source.name} = ", reference)
return this""")
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
        method.setBody("return ${method.getReturnType()}(query, \"${source.name}\");")
    }

    override fun buildField(critterClass: CritterClass, criteriaClass: CritterClass) {
        val qualifiedName = critterClass.qualifiedName
        criteriaClass.addImport(qualifiedName)
        criteriaClass.addImport(fullType)
        criteriaClass.addImport(Criteria::class.java)
        criteriaClass.addImport(FieldEndImpl::class.java)
        criteriaClass.addImport(QueryImpl::class.java)

        var name = "\"" + source.name + "\""
        val parent = source.parent
        if (source.hasAnnotation(Embedded::class.java) ||
                if(parent is KibbleClass) context.isEmbedded(parent.name) else false) {
            name = "prefix + " + name
        }

        criteriaClass.addMethod()
                .setPublic()
                .setName(source.name)
                .setReturnType(java.lang.String.format("%s<%s, %s, %s>", TypeSafeFieldEnd::class.java.name, criteriaClass.qualifiedName,
                        critterClass.qualifiedName, fullType)).setBody(
                "return TypeSafeFieldEnd<${criteriaClass.getName()}, ${critterClass.getName()}, $fullType>(this, query, $name)")

        val method = criteriaClass.addMethod()
                .setPublic()
                .setName(source.name)
                .setReturnType(Criteria::class.java)
                .setBody(java.lang.String.format("return TypeSafeFieldEnd<%s, %s, %s>(this, query, %s).equal(value)",
                        criteriaClass.getName(), critterClass.getName(), fullType, name))
        method.addParameter(parameterizedType, "value")
    }

    override fun extract(name: String, ann: Class<out Annotation>): String {
        TODO("not implemented")
    }

    override fun setStatic(): CritterField = throw Visible.invalid("static", "kotlin")

    override fun setFinal(): CritterField {
        source.markFinal()
        return this
    }

    override fun setStringLiteralInitializer(initializer: String): CritterField {
        source.initializer = initializer
        return this
    }

    override fun setLiteralInitializer(initializer: String): CritterField {
        source.initializer = initializer
        return this
    }

    override fun compareTo(other: CritterField): Int {
        TODO("not implemented")
    }
}