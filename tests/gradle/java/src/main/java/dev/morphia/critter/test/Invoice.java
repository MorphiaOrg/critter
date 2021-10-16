package dev.morphia.critter.test;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Reference;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Arrays.asList;

@Entity
public class Invoice {
  @Id
  private ObjectId id = new ObjectId();
  private LocalDateTime orderDate;
  @Reference
  private Person person;
  private List<List<List<Address>>> listListList = new ArrayList<>();
  private List<Address> addresses = new ArrayList<>();
  private Map<String, List<Address>> mapList = new LinkedHashMap<>();
  private Double total = 0.0;
  private List<Item> items = new ArrayList<>();
  private transient boolean postLoad;
  private transient boolean preLoad;
  private transient boolean prePersist;
  private transient boolean postPersist;

  Invoice() {
  }

  public Invoice(LocalDateTime orderDate, Person person, Address addresses, Item... items) {
    this.orderDate = orderDate.withNano(0);
    this.person = person;
    if (addresses != null) {
      this.addresses.add(addresses);
      mapList.put("1", this.addresses);
      listListList = List.of(List.of(this.addresses));
    }
    this.items.addAll(asList(items));
  }

  public Invoice(LocalDateTime orderDate, Person person, List<Address> addresses, List<Item> items) {
    setOrderDate(orderDate);
    this.person = person;
    if (addresses != null) {
      this.addresses.addAll(addresses);
    }
    if (items != null) {
      this.items.addAll(items);
    }
  }

  public void add(Item item) {
    if (items == null) {
      items = new ArrayList<>();
    }
    items.add(item);
    total += item.getPrice();
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
    this.orderDate = orderDate != null ? orderDate.withNano(0) : null;
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
    return Objects.hash(id, orderDate, person, listListList, addresses, mapList, total, items);
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

  public boolean isPostLoad() {
    return postLoad;
  }

  public boolean isPostPersist() {
    return postPersist;
  }

  public boolean isPreLoad() {
    return preLoad;
  }

  public boolean isPrePersist() {
    return prePersist;
  }

  @PostLoad
  public void postLoad() {
    postLoad = true;
  }

  @PostPersist
  public void postPersist() {
    postPersist = true;
  }

  @PreLoad
  public void preLoad() {
    preLoad = true;
  }

  @PrePersist
  public void prePersist() {
    prePersist = true;
  }
}
