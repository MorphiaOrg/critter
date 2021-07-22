/**
 * Copyright (C) 2012-2020 Justin Lee <jlee@antwerkz.com>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.morphia.critter.test;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

@Entity
public class Invoice {
    @Id
    private ObjectId id = new ObjectId();

    private LocalDateTime orderDate;

    @Reference
    private Person person;

    private List<List<List<Address>>> listListList;
    private List<Address> addresses;
    private Map<String, List<Address>> mapList;

    private Double total = 0.0;

    private List<Item> items;

    public Invoice() {
    }

    public Invoice(LocalDateTime orderDate, Person person, Address address, Item... items) {
        this.orderDate = orderDate;
        this.person = person;
        add(address);
        for (Item item : items) {
            add(item);
        }
    }

    public void add(Item item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        total += item.getPrice();
    }

    public void add(Address address) {
        if (addresses == null) {
            addresses = new ArrayList<>();
            listListList = new ArrayList<>();
            mapList = new LinkedHashMap<>();
            List<List<Address>> list = new ArrayList<>();
            listListList.add(list);
            list.add(addresses);
        }
        addresses.add(address);
        mapList.put(addresses.size() + "", new ArrayList<>(addresses));
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<List<List<Address>>> getListListList() {
        return listListList;
    }

    public void setListListList(List<List<List<Address>>> listListList) {
        this.listListList = listListList;
    }

    public Map<String, List<Address>> getMapList() {
        return mapList;
    }

    public void setMapList(Map<String, List<Address>> mapList) {
        this.mapList = mapList;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate.withNano(0);
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Invoice.class.getSimpleName() + "[", "]")
                   .add("id=" + id)
                   .add("orderDate=" + orderDate)
                   .add("person=" + person)
                   .add("listListList=" + listListList)
                   .add("addresses=" + addresses)
                   .add("mapList=" + mapList)
                   .add("total=" + total)
                   .add("items=" + items)
                   .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Invoice)) {
            return false;
        }
        Invoice invoice = (Invoice) o;
        return Objects.equals(id, invoice.id) && Objects.equals(orderDate, invoice.orderDate) &&
               Objects.equals(person, invoice.person) && Objects.equals(listListList, invoice.listListList) &&
               Objects.equals(addresses, invoice.addresses) && Objects.equals(mapList, invoice.mapList) &&
               Objects.equals(total, invoice.total) && Objects.equals(items, invoice.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderDate, person, listListList, addresses, mapList, total, items);
    }
}
