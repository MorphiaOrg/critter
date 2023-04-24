package dev.morphia.critter.test

import dev.morphia.annotations.Entity

@Entity
class User : Person() {
    var email: String? = null
}
