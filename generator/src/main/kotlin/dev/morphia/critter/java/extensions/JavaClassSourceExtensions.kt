package dev.morphia.critter.java.extensions

import dev.morphia.critter.FilterSieve
import dev.morphia.critter.UpdateSieve
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.PropertySource

fun JavaClassSource.attachFilters(property: PropertySource<JavaClassSource>) {
    FilterSieve.handlers(property, this)
}

fun JavaClassSource.attachUpdates(property: PropertySource<JavaClassSource>) {
    UpdateSieve.handlers(this, property)
}







