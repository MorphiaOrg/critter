package dev.morphia.critter.test

import dev.morphia.annotations.Entity
import dev.morphia.annotations.Id
import dev.morphia.annotations.Property
import org.bson.types.ObjectId

@Entity
open class Person : AbstractPerson {
    @Id
    var id: ObjectId? = null

    @Property("f")
    var first: String? = null

    var last: String? = null

    var ssn: SSN? = null

    @Suppress("unused")
    constructor()

    constructor(first: String, last: String) {
        this.first = first
        this.last = last
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Person

        if (id != other.id) return false
        if (first != other.first) return false
        if (last != other.last) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (first?.hashCode() ?: 0)
        result = 31 * result + (last?.hashCode() ?: 0)
        return result
    }
    override fun toString(): String {
        return "Person(id=$id, first=$first, last=$last, ssn=$ssn)"
    }
}

@Entity
class SSN(var content: String) {
    private constructor() : this("")
}
