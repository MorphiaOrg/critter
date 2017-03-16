package com.antwerkz.critter.test;

import org.mongodb.morphia.annotations.Entity;

@Entity
public class User extends Person {
  private String email;

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }
}
