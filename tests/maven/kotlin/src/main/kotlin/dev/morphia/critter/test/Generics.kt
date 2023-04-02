package dev.morphia.critter.test

import dev.morphia.annotations.Entity
import java.util.TreeMap

@Entity
class Generics() {
    constructor(address: Address) : this() {
        mapList["1"] = listOf(address)
        listListList = mutableListOf(mutableListOf(listOf(address)))
    }

    var listListList: List<List<List<Address>>> = mutableListOf()
    var mapList: TreeMap<String, List<Address>> = TreeMap()
}