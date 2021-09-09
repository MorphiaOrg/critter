package dev.morphia.critter.test

import dev.morphia.annotations.Embedded
import dev.morphia.annotations.Property

@Embedded
data class Address(@Property("c") val city: String, val state: String, val zip: String)
