package dev.morphia.critter.java

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import dev.morphia.Datastore
import dev.morphia.critter.SourceBuilder
import dev.morphia.critter.java.CodecsBuilder.Companion.packageName
import dev.morphia.mapping.Mapper
import dev.morphia.mapping.codec.MorphiaCodecProvider
import dev.morphia.mapping.codec.pojo.MorphiaCodec
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistry
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PUBLIC

class CodecProviderBuilder(val context: JavaContext) : SourceBuilder {
    private val provider = TypeSpec.classBuilder("CritterCodecProvider")
        .addModifiers(PUBLIC, FINAL)

    override fun build() {
        provider.superclass(ClassName.get(MorphiaCodecProvider::class.java))

        buildConstructor()
        buildGet()

        JavaFile
            .builder(packageName, provider.build())
            .build()
            .writeTo(context.outputDirectory)
    }

    private fun buildGet() {
        val method = MethodSpec.methodBuilder("get")
            .addModifiers(PUBLIC)
            .addTypeVariable(TypeVariableName.get("T"))
            .addParameter(ParameterizedTypeName.get(ClassName.get(Class::class.java), TypeVariableName.get("T")), "type")
            .addParameter(CodecRegistry::class.java, "registry")
            .returns(ParameterizedTypeName.get(ClassName.get(Codec::class.java), TypeVariableName.get("T")))
            .addStatement("\$T<T> codec = (MorphiaCodec<T>) getCodecs().get(type)", MorphiaCodec::class.java)

        method.beginControlFlow("if (codec == null)")
        method.addStatement("EntityModel model = getMapper().getEntityModel(type)")
        method.addStatement(
            "codec = new MorphiaCodec<>(getDatastore(), model, getPropertyCodecProviders(), " +
                "getMapper().getDiscriminatorLookup(), registry)"
        )

        context.classes.values
            .filter { !it.isAbstract() }
            .forEachIndexed { index, javaClass ->
                val ifStmt = "if (type.equals(${javaClass.qualifiedName}.class))"
                if (index == 0) {
                    method.beginControlFlow(ifStmt)
                } else {
                    method.nextControlFlow("else $ifStmt")
                }
                method.addStatement("codec.setEncoder((EntityEncoder<T>)new ${javaClass.name}Encoder(codec))")
                method.addStatement("codec.setDecoder((EntityDecoder<T>)new ${javaClass.name}Decoder(codec))")
            }

        method.endControlFlow()
        method.endControlFlow()
        method.addStatement("return codec")
        provider.addMethod(method.build())
        /*

        MorphiaCodec<T> codec = (MorphiaCodec<T>) codecs.get(type);
        if (codec == null && (mapper.isMapped(type) || mapper.isMappable(type))) {
            EntityModel model = mapper.getEntityModel(type);
            codec = new MorphiaCodec<>(datastore, model, propertyCodecProviders, mapper.getDiscriminatorLookup(), registry);
            if (model.hasLifecycle(PostPersist.class) || model.hasLifecycle(PrePersist.class) || mapper.hasInterceptors()) {
                codec.setEncoder(new LifecycleEncoder(codec));
            }
            if (model.hasLifecycle(PreLoad.class) || model.hasLifecycle(PostLoad.class) || mapper.hasInterceptors()) {
                codec.setDecoder(new LifecycleDecoder(codec));
            }
            codecs.put(type, codec);
        }

        return codec;
    }
         */
    }

    private fun buildConstructor() {
        provider.addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(Mapper::class.java, "mapper")
                .addParameter(Datastore::class.java, "datastore")
                .addStatement("super(mapper, datastore)")
                .build()
        )
    }
}
