package com.antwerkz.critter.test

import xyz.morphia.annotations.Entity

@Entity
class User : AbstractPerson() {
    var email: String? = null
}
