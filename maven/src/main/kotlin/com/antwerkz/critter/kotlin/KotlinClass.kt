package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterConstructor
import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
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

class KotlinClass(context: CritterContext, val source: KibbleClass) : CritterClass(context) {

    private var kibbleFile: KibbleFile
    private lateinit var criteriaClass: KibbleClass
    private lateinit var outputFile: File

    init {
        val criteriaPkg = context.criteriaPkg ?: source.pkgName + ".criteria"
        kibbleFile = KibbleFile(getName() + "Criteria.kt", criteriaPkg)
        source.file.imports.forEach {
            kibbleFile.addImport(it.type.name, it.alias)
        }
        lastModified = 0 //Math.min(
//                sourceFile?.lastModified() ?: 0,
//                context[source.superType?.qualifiedName]?.lastModified ?: 0)

        isEmbedded = hasAnnotation(Embedded::class.java)
        addFields(context, source)
    }

    private fun addFields(context: CritterContext, kibble: KibbleClass) {
        fields.addAll(kibble.properties
                .map { f -> KotlinField(context, kibble, f) }
                .sortedBy { f -> f.name }
                .toMutableList())
        kibble.superType?.let {
            (context.resolve(getPackage(), it.name) as KotlinClass?)?.let {
                addFields(context, it.source)
            }
        }
        kibble.superTypes.forEach {
            (context.resolve(getPackage(), it.name) as KotlinClass?)?.let {
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

    override fun getSuperType() = source.superType.toString()
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
        return KotlinField(context, source, source.addProperty(name, type))
    }

    override fun addMethod(): CritterMethod {
        return KotlinMethod(source.addFunction())
    }

    override fun createClass(pkgName: String?, name: String): KotlinClass {
        return KotlinClass(context, source.addClass(name))
    }

    override fun build(directory: File) {
        outputFile = kibbleFile.outputFile(directory)

        if (context.force || !outputFile.exists() /*|| outputFile.lastModified() > lastModified*/) {
            super.build(directory)
            generate(kibbleFile, outputFile)
        }
    }

    override fun buildCriteria(directory: File) {
        criteriaClass = kibbleFile.addClass(getName() + "Criteria")

        kibbleFile.addImport(TypeSafeFieldEnd::class.java)
        kibbleFile.addImport(source.pkgName + "." + source.name)
        val primary = criteriaClass.constructor
        if (!hasAnnotation(Embedded::class.java)) {
            criteriaClass.superType = KibbleType.from(BaseCriteria::class.java.name + "<" + qualifiedName + ">")
            criteriaClass.superCallArgs = listOf("ds", "${getName()}::class.java")
            primary.addParameter("ds", Datastore::class.java.name)
        } else {
            criteriaClass.addProperty("query", "Query<${getName()}>", mutability = VAR, visibility = PRIVATE, constructorParam = true)
            criteriaClass.addProperty("prefix", "String", mutability = VAR, visibility = PRIVATE, constructorParam = true)

            kibbleFile.addImport(Query::class.java)
            val ctor = criteriaClass.addSecondaryConstructor()
            ctor.visibility = PUBLIC
            ctor.body = "this.prefix = prefix + \".\""
        }

        val targetClass = KotlinClass(context, criteriaClass)
        fields.forEach { it.build(this, targetClass) }
        if (!hasAnnotation(Embedded::class.java)) {
            KotlinUpdaterBuilder(this, targetClass)
        }
    }

    override fun buildDescriptor(directory: File) {
        val companion = criteriaClass.addCompanionObject()

        fields.forEach { field ->
            companion.addProperty(field.name, modality = FINAL, initializer = field.mappedName())
        }
    }

    private fun generate(kibbleFile: KibbleFile, file: File) {
        file.parentFile.mkdirs()
            kibbleFile.toSource(SourceWriter())
                    .toFile(file)
    }

    override fun toSource(): String {
        return source.toSource().toString()
    }

    override fun toString(): String {
        return "KotlinClass(${source.name})"
    }
}

