package dev.morphia.critter.test

import dev.morphia.annotations.Entity
import dev.morphia.annotations.Id
import org.bson.types.ObjectId

@Entity
interface TestEntity
open class RootParent : TestEntity {
    @Id var id: ObjectId? = null
}
@Entity
open class ChildLevel1a: RootParent()
class ChildLevel2a: ChildLevel1a()
open class ChildLevel2b: ChildLevel1a()
class ChildLevel3a: ChildLevel2b()
class ChildLevel1b: RootParent()
class ChildLevel1c: RootParent()