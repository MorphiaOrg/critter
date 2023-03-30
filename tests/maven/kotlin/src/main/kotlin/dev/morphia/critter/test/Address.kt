package dev.morphia.critter.test

import dev.morphia.annotations.Entity
import dev.morphia.annotations.Property

@Entity
data class Address(
    @Property("c")
    val city: String,
    val state: String,
    val zip: String,
)
