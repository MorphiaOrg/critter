package dev.morphia.critter.kotlin

import dev.morphia.critter.SourceBuilder

class CodecsBuilder(val context: KotlinContext) : SourceBuilder {
    companion object {
        val packageName = "dev.morphia.critter.codec"
    }

    override fun build() {
        EncoderBuilder(context).build()
        DecoderBuilder(context).build()
        InstanceCreatorBuilder(context).build()
        CodecProviderBuilder(context).build()
        ModelImporter(context).build()
    }
}