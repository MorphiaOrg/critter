package dev.morphia.critter.java

import dev.morphia.critter.SourceBuilder

class JavaCodecsBuilder(val context: JavaContext) : SourceBuilder {
    companion object {
        val packageName = "dev.morphia.mapping.codec.pojo"
    }

    override fun build() {
        JavaEncoderBuilder(context).build()
        JavaDecoderBuilder(context).build()
        JavaCodecProviderBuilder(context).build()
    }
}