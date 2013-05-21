package com.antwerkz.critter;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity
public class Person {
  @Id
  private ObjectId id;

  private String first;

  private String last;

  private Long age;

  public Person() {
  }

  public Person(String first, String last) {
    this.first = first;
    this.last = last;
  }

  public ObjectId getId() {
    return id;
  }

  public void setId(final ObjectId id) {
    this.id = id;
  }

  public Long getAge() {
    return age;
  }

  public void setAge(final Long age) {
    this.age = age;
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
