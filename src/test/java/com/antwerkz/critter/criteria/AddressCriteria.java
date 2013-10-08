package com.antwerkz.critter.criteria;

import com.antwerkz.critter.Address;
import com.antwerkz.critter.TypeSafeFieldEnd;
import org.mongodb.morphia.query.Query;

public class AddressCriteria {
  private final Query query;
  private final String prefix;

  public AddressCriteria(Query query, String prefix) {
    this.query = query;
    this.prefix = prefix + ".";
  }


  public TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String> city() {
    return new TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "city");
  }

  public AddressCriteria city(java.lang.String value) {
    new TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "city").equal(value);
    return this;
  }

  public TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String> state() {
    return new TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "state");
  }

  public AddressCriteria state(java.lang.String value) {
    new TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "state").equal(value);
    return this;
  }

  public TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String> zip() {
    return new TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "zip");
  }

  public AddressCriteria zip(java.lang.String value) {
    new TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "zip").equal(value);
    return this;
  }
}
