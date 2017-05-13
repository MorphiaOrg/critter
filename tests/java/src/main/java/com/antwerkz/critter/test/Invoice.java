/**
 * Copyright (C) 2012-2013 Justin Lee <jlee@antwerkz.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antwerkz.critter.test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

@Entity
public class Invoice {
  @Id
  private ObjectId id = new ObjectId();

  private LocalDateTime date;

  @Reference
  private Person person;

  @Embedded
  private List<Address> addresses;

  private Double total = 0.0;

  private List<Item> items;

  public Invoice() {
  }

  public Invoice(LocalDateTime date, Person person, Address address, Item... items) {
    this.date = date;
    this.person = person;
    add(address);
    for (Item item : items) {
      add(item);
    }
  }

  public LocalDateTime getDate() {
    return date;
  }

  public void setDate(LocalDateTime date) {
    this.date = date;
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

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  public List<Address> getAddresses() {
    return addresses;
  }

  @Override
  public String toString() {
    return "Invoice{" +
               "id=" + id +
               ", date=" + date +
               ", person=" + person +
               ", addresses=" + addresses +
               ", total=" + total +
               ", items=" + items +
               '}';
  }
}
