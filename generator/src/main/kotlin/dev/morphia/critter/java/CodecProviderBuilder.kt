package dev.morphia.critter.java

import com.mongodb.lang.NonNull
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import dev.morphia.Datastore
import dev.morphia.critter.SourceBuilder
import dev.morphia.mapping.codec.MorphiaCodecProvider
import dev.morphia.mapping.codec.MorphiaInstanceCreator
import dev.morphia.mapping.codec.pojo.EntityDecoder
import dev.morphia.mapping.codec.pojo.EntityEncoder
import dev.morphia.mapping.codec.pojo.EntityModel
import dev.morphia.mapping.codec.pojo.MorphiaCodec
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistry
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PUBLIC

class CodecProviderBuilder(val context: JavaContext) : SourceBuilder {
    private val provider = TypeSpec.classBuilder("CritterCodecProvider").addModifiers(PUBLIC, FINAL)

    override fun build() {
        provider.superclass(ClassName.get(MorphiaCodecProvider::class.java))

        provider.addAnnotation(
            AnnotationSpec.builder(SuppressWarnings::class.java).addMember("value", CodeBlock.of("""{"unchecked", "rawtypes"}""")).build()
        )

        buildConstructor()
        buildGet()
        refreshCodecs()

        context.buildFile(provider.build())
    }

    private fun buildGet() {
        val method = MethodSpec.methodBuilder("get").addModifiers(PUBLIC).addTypeVariable(TypeVariableName.get("T")).addParameter(
            ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Class::class.java), TypeVariableName.get("T")), "type")
                .addAnnotation(NonNull::class.java).build()
        ).addParameter(
            ParameterSpec.builder(ClassName.get(CodecRegistry::class.java), "registry")
                .addAnnotation(NonNull::class.java)
                .build()
        )
            .returns(ParameterizedTypeName.get(ClassName.get(Codec::class.java), TypeVariableName.get("T")))
            .addStatement("\$T<T> found = (MorphiaCodec<T>) getCodecs().get(type)", MorphiaCodec::class.java)

        method.beginControlFlow("if (found != null)")
        method.addStatement("return found")

        context.entities().values.filter { !it.isAbstract() }.forEachIndexed { _, javaClass ->
            method.nextControlFlow("else if (type.equals(\$T.class))", javaClass.qualifiedName.className())
            method.addStatement("\$T model = getMapper().getEntityModel(type)", EntityModel::class.java)
            method.addStatement(
                "MorphiaCodec<\$T> codec = new MorphiaCodec<>(getDatastore(), model, getPropertyCodecProviders(), " + "getMapper().getDiscriminatorLookup(), registry)",
                javaClass.qualifiedName.className()
            )
            method.addStatement("codec.setEncoder(new ${javaClass.name}Encoder(codec))", EntityEncoder::class.java)
            method.addStatement("codec.setDecoder(new ${javaClass.name}Decoder(codec))", EntityDecoder::class.java)
            method.addStatement("return (MorphiaCodec<T>)codec")
        }
        method.endControlFlow()
        method.addStatement("return null")

        provider.addMethod(method.build())
    }

    private fun buildConstructor() {
        provider.addMethod(
            MethodSpec.constructorBuilder().addModifiers(PUBLIC).addParameter(Datastore::class.java, "datastore")
                .addStatement("super(datastore)").build()
        )
    }

    private fun refreshCodecs() {
        val method = MethodSpec.methodBuilder("getRefreshCodec")
            .addModifiers(PUBLIC)
            .addTypeVariable(TypeVariableName.get("T"))
            .addParameter(TypeVariableName.get("T"), "entity")
            .addParameter(
                ParameterSpec.builder(ClassName.get(CodecRegistry::class.java), "registry")
                    .addAnnotation(NonNull::class.java)
                    .build()
            )
            .returns(ParameterizedTypeName.get(ClassName.get(Codec::class.java), TypeVariableName.get("T")))

        method.addStatement("var type = (Class<T>)entity.getClass()")
        method.addStatement("var model = getMapper().getEntityModel(entity.getClass())")
        method.addStatement(
            "MorphiaCodec<T> codec = new MorphiaCodec<>(getDatastore(), model, getPropertyCodecProviders()," + " getMapper().getDiscriminatorLookup(), registry)"
        )
        context.entities().values.filter { !it.isAbstract() }.forEachIndexed { index, javaClass ->
            val ifStmt = "if (type.equals(${"$"}T.class))"
            if (index == 0) {
                method.beginControlFlow(ifStmt, javaClass.qualifiedName.className())
            } else {
                method.nextControlFlow("else $ifStmt", javaClass.qualifiedName.className())
            }
            method.addCode(
                """
                    codec.setDecoder(new ${"$"}T(codec) {
                        @NonNull
                        @Override
                        protected ${"$"}T getInstanceCreator() {
                            return new ${javaClass.name}InstanceCreator() {
                                @Override
                                public ${javaClass.name} getInstance() {
                                    return (${javaClass.name})entity;
                                }
                            };
                        }
                    });
                """.trimIndent(), EntityDecoder::class.java, MorphiaInstanceCreator::class.java
            )
        }

        method.endControlFlow()
        method.addStatement("return codec")
        provider.addMethod(method.build())
    }
}
