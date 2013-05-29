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
package com.antwerkz.critter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Reference;
import org.bson.types.ObjectId;

@Entity
public class Invoice {
  @Id
  private ObjectId id = new ObjectId();

  private Date date;

  @Reference
  private Person person;

  @Embedded
  private Address address;

  private Double total = 0.0;

  private List<Item> items;

  public Invoice() {
  }

  public Invoice(Date date, Person person, Address address, Item... items) {
    this.date = date;
    this.person = person;
    this.address = address;
    for (Item item : items) {
      add(item);
    }
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Person getPerson() {
    return person;
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

  @Embedded
  public static class Address {
    private String city;
    private String state;
    private String zip;

    public Address() {
    }

    public Address(final String city, final String state, final String zip) {
      this.city = city;
      this.state = state;
      this.zip = zip;
    }

    public String getCity() {
      return city;
    }

    public void setCity(final String city) {
      this.city = city;
    }

    public String getState() {
      return state;
    }

    public void setState(final String state) {
      this.state = state;
    }

    public String getZip() {
      return zip;
    }

    public void setZip(final String zip) {
      this.zip = zip;
    }
  }
}
