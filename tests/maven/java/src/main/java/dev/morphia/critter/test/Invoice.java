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
import java.util.List;
import java.util.Set;

@Entity
public class Invoice {
    @Id
    private ObjectId id = new ObjectId();

    private LocalDateTime orderDate;

    @Reference
    private Person person;

    private List<Set<List<Address>>> dummy;
    private List<Address> addresses;

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
        }
        addresses.add(address);
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

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
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
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Invoice invoice = (Invoice) o;
        if (!id.equals(invoice.id)) {
            return false;
        }
        return true;
    }

    public List<Set<List<Address>>> getDummy() {
        return dummy;
    }

    public void setDummy(List<Set<List<Address>>> dummy) {
        this.dummy = dummy;
    }

    @Override
    public String toString() {
        return "Invoice{" +
               "id=" + id +
               ", date=" + orderDate +
               ", person=" + person +
               ", addresses=" + addresses +
               ", total=" + total +
               ", items=" + items +
               '}';
    }
}
