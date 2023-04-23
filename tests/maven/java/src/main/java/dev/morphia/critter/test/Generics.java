package dev.morphia.critter.test;

import dev.morphia.annotations.Entity;

import java.util.List;
import java.util.TreeMap;

@Entity
public class Generics {
    List<List<List<Address>>> listListList;
    TreeMap<String, List<Address>> mapList  = new TreeMap<>();

    public Generics(List<List<List<Address>>> listListList, TreeMap<String, List<Address>> mapList) {
        this.listListList = listListList;
        this.mapList = mapList;
    }

    public Generics(Address address) {
        mapList.put("1", List.of(address));
        listListList = List.of(List.of(List.of(address)));
    }

    public List<List<List<Address>>> getListListList() {
        return listListList;
    }

    public void setListListList(List<List<List<Address>>> listListList) {
        this.listListList = listListList;
    }

    public TreeMap<String, List<Address>> getMapList() {
        return mapList;
    }

    public void setMapList(TreeMap<String, List<Address>> mapList) {
        this.mapList = mapList;
    }
}