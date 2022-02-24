/**
 * Copyright (C) 2012-2020 Justin Lee <jlee></jlee>@antwerkz.com>

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
}

@Entity
class SSN(var value: String) {
    private constructor() : this("")
}
