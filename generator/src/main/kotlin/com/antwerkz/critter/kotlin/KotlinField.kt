package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.CritterKotlinContext
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

class KotlinField(private val context: CritterKotlinContext, val parent: KibbleClass, val property: KibbleProperty) : CritterField {
    companion object {
        val NUMERIC_TYPES = listOf("Float",
                "Double",
                "Long",
                "Integer",
                "Byte",
                "Short",
                "Number")
                .map { listOf(it, "${it}?", "kotlin.${it}", "kotlin.${it}?") }
                .flatMap { it }
    }

    override val shortParameterTypes = mutableListOf<String>()
    override val fullParameterTypes = mutableListOf<String>()
    override val fullType: String
        get() = parent.file.resolve(property.type!!).name
    override val name: String
        get() = property.name
    override val parameterTypes: List<String>
        get() = property.type!!.typeParameters.map { it.toString() }
    override val parameterizedType: String
        get() = property.type.toString()

    override val fullyQualifiedType: String
        get() = property.type!!.name

    init {
        property.type?.typeParameters?.forEach {
            shortParameterTypes.add(it.name)
            fullParameterTypes.add(it.name)
        }
    }

    override fun isPublic() = property.isPublic()
    override fun setPublic(): CritterField {
        property.visibility = PUBLIC
        return this
    }

    override fun isPrivate() = property.isPrivate()
    override fun setPrivate(): CritterField {
        property.visibility = PRIVATE
        return this
    }

    override fun isProtected() = property.isProtected()
    override fun setProtected(): CritterField {
        property.visibility = PROTECTED
        return this
    }

    override fun isInternal() = property.isInternal()
    override fun setInternal(): CritterField {
        property.visibility = INTERNAL
        return this
    }

    override fun isPackagePrivate() = false
    override fun setPackagePrivate() = throw Visible.invalid("package private", "kotlin")

    override fun hasAnnotation(aClass: Class<out Annotation>): Boolean {
        return property.hasAnnotation(aClass)
                || resolveFieldType()?.hasAnnotation(aClass)
                ?: false
    }

    private fun resolveFieldType(): CritterClass? {
        return if (!shortParameterTypes.isEmpty()) {
            context.resolve(parent.pkgName!!, fullParameterTypes[0])
        } else {
            context.resolve(parent.pkgName!!, property.type!!.name)
        }
    }

    override fun isStatic() = false

    override fun isContainer(): Boolean {
        return super.isContainer()
    }

    override fun isNumeric(): Boolean {
        return NUMERIC_TYPES.contains(fullType) || super.isNumeric()
    }

    override fun  build(sourceClass: CritterClass, targetClass: CritterClass) {
        sourceClass as KotlinClass
        val resolve = sourceClass.source.file.resolve(property.type!!)
        when {
            property.hasAnnotation(Reference::class.java) -> buildReference(targetClass)
            hasAnnotation(Embedded::class.java) -> buildEmbed(targetClass)
            else -> buildField(sourceClass, targetClass)
        }
    }

    override fun buildReference(criteriaClass: CritterClass) {
        resolveFieldType()?.let {
            criteriaClass.addImport(it.qualifiedName)
        }
        criteriaClass.addMethod()
                .setPublic()
                .setName(property.name)
                .setReturnType(criteriaClass.qualifiedName)
                .setBody("""query.filter("${property.name} = ", reference)
return this""")
                .addParameter(property.type!!.name, "reference")
    }

    override fun buildEmbed(criteriaClass: CritterClass) {
        criteriaClass.addImport(Criteria::class.java)
        val criteriaType: String
        val resolved = if (!shortParameterTypes.isEmpty()) {
            criteriaType = shortParameterTypes[0] + "Criteria"
//            criteriaClass.addImport("${criteriaClass.getPackage()}.$criteriaType")
            context.resolve(parent.pkgName!!, fullParameterTypes[0])
        } else {
            val type = property.type!!
            criteriaType = type.name + "Criteria"
            context.resolve(parent.pkgName!!, type.name)
        }
        resolved?.let {
            criteriaClass.addImport(it.qualifiedName)
        }
        val method = criteriaClass.addMethod()
                .setPublic()
                .setName(property.name)
                .setReturnType(criteriaType)
        method.setBody("return ${method.getReturnType()}(query, \"${property.name}\");")
    }

    override fun buildField(critterClass: CritterClass, criteriaClass: CritterClass) {
        val qualifiedName = critterClass.qualifiedName
        criteriaClass.addImport(qualifiedName)
        criteriaClass.addImport(fullType)
        criteriaClass.addImport(Criteria::class.java)

        var name = "\"" + property.name + "\""
        if (property.hasAnnotation(Embedded::class.java) || context.isEmbedded(parent.pkgName!!, parent.name)) {
            name = "prefix + " + name
        }

        criteriaClass.addMethod()
                .setPublic()
                .setName(property.name)
                .setReturnType("${TypeSafeFieldEnd::class.java.simpleName}<${criteriaClass.getName()}, ${fullType}>")
                .setBody("return TypeSafeFieldEnd(this, query, $name)")

        val criteriaName = criteriaClass.getName()
        criteriaClass.addMethod()
                .setPublic()
                .setName(property.name)
                .setReturnType(Criteria::class.java.simpleName)
                .setBody("return TypeSafeFieldEnd<$criteriaName, $fullType>(this, query, $name).equal(value)")
                .addParameter(parameterizedType, "value")
    }

    override fun extract(name: String, ann: Class<out Annotation>): String {
        return property.getAnnotation(ann)?.get("value") ?: name
    }

    override fun setStatic(): CritterField = throw Visible.invalid("static", "kotlin")

    override fun setFinal(): CritterField {
        property.markFinal()
        return this
    }

    override fun setStringLiteralInitializer(initializer: String): CritterField {
        property.initializer = initializer
        return this
    }

    override fun setLiteralInitializer(initializer: String): CritterField {
        property.initializer = initializer
        return this
    }

    override fun compareTo(other: CritterField): Int {
        TODO("not implemented")
    }

    override fun toString(): String {
        return "KotlinField(property=$property)"
    }
}