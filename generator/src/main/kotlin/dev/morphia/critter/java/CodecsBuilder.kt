package dev.morphia.critter.java

import dev.morphia.critter.SourceBuilder

class CodecsBuilder(val context: JavaContext) : SourceBuilder {
    companion object {
        val CODECS_PACKAGE = "codecs"
    }

    override fun build() {
        EncoderBuilder(context).build()
        DecoderBuilder(context).build()
        InstanceCreatorBuilder(context).build()
        CodecProviderBuilder(context).build()
        ModelImporter(context).build()
    }
}