package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterConstructor
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.CritterKotlinContext
import com.antwerkz.critter.CritterMethod
import com.antwerkz.critter.KotlinUpdaterBuilder
import com.antwerkz.critter.TypeSafeFieldEnd
import com.antwerkz.critter.Visible
import com.antwerkz.critter.criteria.BaseCriteria
import com.antwerkz.kibble.SourceWriter
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleFile
import com.antwerkz.kibble.model.KibbleType
import com.antwerkz.kibble.model.Modality.FINAL
import com.antwerkz.kibble.model.Mutability.VAR
import com.antwerkz.kibble.model.Visibility.INTERNAL
import com.antwerkz.kibble.model.Visibility.PRIVATE
import com.antwerkz.kibble.model.Visibility.PROTECTED
import com.antwerkz.kibble.model.Visibility.PUBLIC
import org.mongodb.morphia.Datastore
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.query.Query
import java.io.File

class KotlinClass(context: CritterKotlinContext, val source: KibbleClass) : CritterClass(context) {

    private var kibbleFile: KibbleFile
    private lateinit var criteriaClass: KibbleClass

    init {
        val criteriaPkg = context.criteriaPkg ?: source.pkgName + ".criteria"
        kibbleFile = KibbleFile(getName() + "Criteria.kt", criteriaPkg)
        source.file.imports.forEach {
            kibbleFile.addImport(it.type.name, it.alias)
        }
        kibbleFile.addImport(source.pkgName + "." + source.name)
        lastModified = 0 //Math.min(
//                sourceFile?.lastModified() ?: 0,
//                context[source.superType?.qualifiedName]?.lastModified ?: 0)

        isEmbedded = hasAnnotation(Embedded::class.java)
        addFields(context, source)
    }

    private fun addFields(context: CritterKotlinContext, kibble: KibbleClass) {
        val elements = kibble.properties
                .map { KotlinField(context, kibble, it) }
                .sortedBy(KotlinField::name)
                .toMutableList()
        kibble.properties
                .map { it.type }
                .filterNotNull()
                .map { kibble.file.resolve(it).name}
                .forEach { addImport(it) }
        fields.addAll(elements)

        kibble.superType?.let {
            context.resolve(getPackage(), it.name)?.let {
                it.source.file.imports.forEach {
                    kibbleFile.addImport(it.type.name, it.alias)
                }
                addFields(context, it.source)
            }
        }
        kibble.superTypes.forEach {
            context.resolve(getPackage(), it.name)?.let {
                it.source.file.imports.forEach {
                    kibbleFile.addImport(it.type.name, it.alias)
                }
                addFields(context, it.source)
            }
        }
    }

    override fun hasAnnotation(aClass: Class<out Annotation>) = source.hasAnnotation(aClass)

    override fun getName() = source.name
    override fun setName(name: String): CritterClass {
        source.name = name
        return this
    }

    override fun getPackage() = source.pkgName!!
    override fun setPackage(name: String?): CritterClass {
        source.pkgName = name
        return this
    }

    override fun getSuperType() = source.superType?.toString()
    override fun setSuperType(name: String): CritterClass {
        source.superType = KibbleType.from(name)
        return this
    }

    override fun isPublic() = source.isPublic()
    override fun setPublic(): CritterClass {
        source.visibility = PUBLIC
        return this
    }

    override fun isPrivate() = source.isPrivate()
    override fun setPrivate(): CritterClass {
        source.visibility = PRIVATE
        return this
    }

    override fun isProtected() = source.isProtected()
    override fun setProtected(): CritterClass {
        source.visibility = PROTECTED
        return this
    }

    override fun isInternal() = source.isInternal()
    override fun setInternal(): CritterClass {
        source.visibility = INTERNAL
        return this
    }

    override fun isPackagePrivate() = false
    override fun setPackagePrivate() = throw Visible.invalid("package private", "kotlin")

    override fun addImport(klass: Class<*>) {
        source.file.addImport(klass.name)
    }

    override fun addImport(name: String) {
        if (name.contains(".")) {
            source.file.addImport(name)
        }
    }

    override fun addConstructor(): CritterConstructor {
        return SecondaryConstructor(source.addSecondaryConstructor())
    }

    override fun addField(name: String, type: String): CritterField {
        return KotlinField(context as CritterKotlinContext, source, source.addProperty(name, type))
    }

    override fun addMethod(): CritterMethod {
        return KotlinMethod(source.addFunction())
    }

    override fun createClass(pkgName: String?, name: String): KotlinClass {
        return KotlinClass(context as CritterKotlinContext, source.addClass(name))
    }

    override fun build(directory: File) {
        outputFile = kibbleFile.outputFile(directory)

        super.build(directory)
    }

    override fun buildCriteria(directory: File) {
        criteriaClass = kibbleFile.addClass(getName() + "Criteria")

        kibbleFile.addImport(Datastore::class.java)
        kibbleFile.addImport(BaseCriteria::class.java)
        kibbleFile.addImport(TypeSafeFieldEnd::class.java)
        kibbleFile.addImport("${source.pkgName}.${source.name}")

        val companion = criteriaClass.addCompanionObject()

        fields.forEach { field ->
            companion.addProperty(field.name, modality = FINAL, initializer = field.mappedName())
        }

        val primary = criteriaClass.constructor
        if (!hasAnnotation(Embedded::class.java)) {
            criteriaClass.superType = KibbleType.from("${BaseCriteria::class.java.simpleName}<${getName()}>")
            criteriaClass.superCallArgs = listOf("ds", "${getName()}::class.java")
            primary.addParameter("ds", Datastore::class.java.simpleName)
        } else {
            criteriaClass.addProperty("query", "Query<*>", mutability = VAR, visibility = PRIVATE, constructorParam = true)
            criteriaClass.addProperty("prefix", "String", mutability = VAR, visibility = PRIVATE, constructorParam = true)

            kibbleFile.addImport(Query::class.java)
            criteriaClass.initBlock = "this.prefix = prefix + \".\""
        }

        val targetClass = KotlinClass(context as CritterKotlinContext, criteriaClass)
        fields.forEach { it.build(this, targetClass) }
        if (!hasAnnotation(Embedded::class.java)) {
            KotlinUpdaterBuilder(this, targetClass)
        }
        outputFile.parentFile.mkdirs()
        kibbleFile.toSource(SourceWriter())
                .toFile(outputFile)
    }

    override fun toSource(): String {
        return source.toSource().toString()
    }

    override fun toString(): String {
        return "KotlinClass(${source.name})"
    }
}

