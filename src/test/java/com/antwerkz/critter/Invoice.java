package com.antwerkz.critter;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Invoice {
    @Id
    private ObjectId id = new ObjectId();
    private Date date;
    @Embedded
    private Person person;
    private Double total = 0.0;
    private List<Item> items;

    public Invoice() {
    }

    public Invoice(Date date, Person person, Item... items) {
        this.date = date;
        this.person = person;
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
    public static class Person {
        private String first;
        private String last;

        public Person() {
        }

        public Person(String first, String last) {
            this.first = first;
            this.last = last;
        }

        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getLast() {
            return last;
        }

        public void setLast(String last) {
            this.last = last;
        }
    }
}
