package dev.morphia.critter.test

import dev.morphia.annotations.Entity

@Entity
class User : AbstractPerson() {
    var email: String? = null
}
