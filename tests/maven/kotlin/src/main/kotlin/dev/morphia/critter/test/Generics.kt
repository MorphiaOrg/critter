package dev.morphia.critter.test

import dev.morphia.annotations.Entity

//@Entity
class Generics() {
    constructor(address: Address) : this() {
        mapList["1"] = listOf(address)
        listListList = mutableListOf(mutableListOf(listOf(address)))
    }

    var listListList: List<List<List<Address>>> = mutableListOf()
    var mapList: MutableMap<String, List<Address>> = LinkedHashMap()
}