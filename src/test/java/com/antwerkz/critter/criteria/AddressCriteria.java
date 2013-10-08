package com.antwerkz.critter.criteria;

import com.antwerkz.critter.Address;

public class AddressCriteria {
  private final org.mongodb.morphia.query.Query query;
  private final String prefix;

  public AddressCriteria(org.mongodb.morphia.query.Query query, String prefix) {
    this.query = query;
    this.prefix = prefix + ".";
  }


  public com.antwerkz.critter.TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String> city() {
    return new com.antwerkz.critter.TypeSafeFieldEnd<>(this, query, prefix + "city");
  }

  public AddressCriteria city(java.lang.String value) {
    new com.antwerkz.critter.TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "city").equal(value);
    return this;
  }

  public com.antwerkz.critter.TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String> state() {
    return new com.antwerkz.critter.TypeSafeFieldEnd<>(this, query, prefix + "state");
  }

  public AddressCriteria state(java.lang.String value) {
    new com.antwerkz.critter.TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "state").equal(value);
    return this;
  }

  public com.antwerkz.critter.TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String> zip() {
    return new com.antwerkz.critter.TypeSafeFieldEnd<>(this, query, prefix + "zip");
  }

  public AddressCriteria zip(java.lang.String value) {
    new com.antwerkz.critter.TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "zip").equal(value);
    return this;
  }
}
