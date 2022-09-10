package dev.morphia.critter.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.FINAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import dev.morphia.Datastore
import dev.morphia.critter.SourceBuilder
import dev.morphia.mapping.codec.MorphiaCodecProvider
import dev.morphia.mapping.codec.MorphiaInstanceCreator
import dev.morphia.mapping.codec.pojo.EntityDecoder
import dev.morphia.mapping.codec.pojo.MorphiaCodec
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistry

@OptIn(DelicateKotlinPoetApi::class)
class CodecProviderBuilder(val context: KotlinContext) : SourceBuilder {
    private val provider = TypeSpec.classBuilder("CritterCodecProvider")
        .addModifiers(PUBLIC, FINAL)

    override fun build() {
        provider.superclass(MorphiaCodecProvider::class.java.asClassName())
        provider.addAnnotation(
            AnnotationSpec.builder(Suppress::class.java)
                .addMember("\"UNCHECKED_CAST\"")
                .build()
        )

        buildConstructor()
        get()
        refreshCodecs()

        context.buildFile(provider.build())
    }

    private fun get() {
        val method = FunSpec.builder("get")
            .addModifiers(OVERRIDE)
            .addTypeVariable(TypeVariableName("T"))
            .addParameter(
                "type", Class::class.java.asClassName()
                    .parameterizedBy(TypeVariableName("T"))
            )
            .addParameter("registry", CodecRegistry::class.java)
            .returns(
                Codec::class.java.asClassName()
                    .parameterizedBy(TypeVariableName("T"))
                    .copy(nullable = true)
            )
            .addCode("return (codecs[type] ?:")

        method.beginControlFlow("when (type)")
        context.entities().values
            .filter { !it.isAbstract() }
            .forEach { javaClass ->
                method.beginControlFlow("%T::class.java ->", javaClass.qualifiedName.className())
                method.addStatement(
                    "val codec = MorphiaCodec<%T>(datastore, mapper.getEntityModel(type), propertyCodecProviders, mapper" +
                        ".discriminatorLookup, registry)", javaClass.qualifiedName.className())
                method.addStatement("""codec.setEncoder(${javaClass.name}Encoder(codec))
                    |.setDecoder(${javaClass.name}Decoder(codec))""".trimMargin())
                method.addStatement("codec")
                method.endControlFlow()
            }
        method.addStatement("else -> null")
        method.endControlFlow()
        method.addCode(") as %T<T>?", MorphiaCodec::class.java)

        provider.addFunction(method.build())
    }

    private fun buildConstructor() {
        provider.primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("datastore", Datastore::class.java)
                .build()
        )
        provider.addSuperclassConstructorParameter("datastore")
    }

    private fun refreshCodecs() {
        val function = FunSpec.builder("getRefreshCodec")
            .addModifiers(OVERRIDE)
            .addTypeVariable(
                TypeVariableName("T")
                    .copy(bounds = listOf(ClassName("kotlin", "Any")))
            )
            .addParameter("entity", TypeVariableName("T"))
            .addParameter("registry", CodecRegistry::class.java)
            .returns(Codec::class.java.asClassName().parameterizedBy(TypeVariableName("T"))
                .copy(nullable = true))

        function.addStatement("var type = entity::class.java as Class<T>")
        function.addStatement("var model = mapper.getEntityModel(entity::class.java)")
        function.beginControlFlow("when (type)")
        context.entities().values
            .filter { !it.isAbstract() }
            .forEachIndexed { index, type ->
                function.beginControlFlow("%T::class.java ->", type.qualifiedName.className())
                function.addStatement(
                    "var codec: MorphiaCodec<%T> = MorphiaCodec(datastore, model, propertyCodecProviders," +
                        " mapper.discriminatorLookup, registry)", type.qualifiedName.className()
                )
                function.addCode(
                    """
                    codec.setDecoder(object: %T<%T>(codec) {
                        protected override fun getInstanceCreator(): %T {
                            return object: ${type.name}InstanceCreator() {
                                override fun getInstance(): ${type.name} {
                                    return entity as ${type.name}
                                }
                            }
                        }
                    })
                    
                    return codec as Codec<T>
                """.trimIndent(), EntityDecoder::class.java, type.qualifiedName.className(), MorphiaInstanceCreator::class.java
                )
                function.endControlFlow()
            }

        function.endControlFlow()
        function.addStatement("return null")
        provider.addFunction(function.build())
    }
}
