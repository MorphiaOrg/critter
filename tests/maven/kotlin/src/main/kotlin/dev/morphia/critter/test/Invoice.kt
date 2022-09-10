package dev.morphia.critter.test


import dev.morphia.annotations.Entity
import dev.morphia.annotations.Id
import dev.morphia.annotations.PostLoad
import dev.morphia.annotations.PostPersist
import dev.morphia.annotations.PreLoad
import dev.morphia.annotations.PrePersist
import dev.morphia.annotations.Reference
import dev.morphia.annotations.Transient
import org.bson.types.ObjectId
import java.time.LocalDateTime
import java.util.Objects
import java.util.StringJoiner

@Entity
class Invoice() {
    @Id
    var id: ObjectId? = null

    var orderDate: LocalDateTime? = null
        set(value: LocalDateTime?) {
            field = value?.withNano(0)
        }

    @Reference
    var person: Person? = null
    var listListList: List<List<List<Address>>> = mutableListOf()
    var addresses: MutableList<Address> = mutableListOf()
    var mapList: MutableMap<String, List<Address>> = LinkedHashMap()
    var total: Double = 0.0
    var items: MutableList<Item> = mutableListOf()

    @Transient
    var isPostLoad: Boolean = false
        private set

    @Transient
    var isPreLoad: Boolean = false
        private set

    @Transient
    var isPrePersist: Boolean = false
        private set

    @Transient
    var isPostPersist: Boolean = false
        private set

    constructor(orderDate: LocalDateTime, person: Person, addresses: MutableList<Address>, items: MutableList<Item>) : this() {
        this.orderDate = orderDate
        this.person = person
        this.addresses.addAll(addresses)
        this.items.addAll(items)
    }

    constructor(orderDate: LocalDateTime, person: Person, addresses: Address, vararg items: Item) : this() {
        this.orderDate = orderDate
        this.person = person
        this.addresses.add(addresses)
        mapList["1"] = this.addresses
        listListList = mutableListOf(mutableListOf<List<Address>>(this.addresses))
        this.items.addAll(items)
    }

    fun add(item: Item) {
        items.add(item)
        total += item.price
    }

    @PostLoad
    fun postLoad() {
        isPostLoad = true
    }

    @PostPersist
    fun postPersist() {
        isPostPersist = true
    }

    @PreLoad
    fun preLoad() {
        isPreLoad = true
    }

    @PrePersist
    fun prePersist() {
        isPrePersist = true
    }

    override fun toString(): String {
        return StringJoiner(", ", Invoice::class.java.simpleName + "[", "]")
            .add("id=$id")
            .add("orderDate=$orderDate")
            .add("person=$person")
            .add("listListList=$listListList")
            .add("addresses=$addresses")
            .add("mapList=$mapList")
            .add("total=$total")
            .add("items=$items")
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Invoice) return false

        if (id != other.id) return false
        if (orderDate != other.orderDate) return false
        if (person != other.person) return false
        if (listListList != other.listListList) return false
        if (addresses != other.addresses) return false
        if (mapList != other.mapList) return false
        if (total != other.total) return false
        if (items != other.items) return false

        return true
    }
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (orderDate?.hashCode() ?: 0)
        result = 31 * result + (person?.hashCode() ?: 0)
        result = 31 * result + listListList.hashCode()
        result = 31 * result + addresses.hashCode()
        result = 31 * result + mapList.hashCode()
        result = 31 * result + total.hashCode()
        result = 31 * result + items.hashCode()
        return result
    }
}
