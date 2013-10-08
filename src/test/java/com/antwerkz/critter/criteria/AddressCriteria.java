package com.antwerkz.critter.criteria;

import java.lang.String;

import com.antwerkz.critter.Address;
import com.antwerkz.critter.TypeSafeFieldEnd;
import org.mongodb.morphia.query.Query;

public class AddressCriteria {
  private final Query<Address> query;
  private final String prefix;

  public AddressCriteria(Query<Address> query, String prefix) {
    this.query = query;
    this.prefix = prefix + ".";
  }


  public TypeSafeFieldEnd<AddressCriteria, Address, String> city() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "city");
  }

  public AddressCriteria city(String value) {
    new TypeSafeFieldEnd<AddressCriteria, Address, String>(this, query, prefix + "city").equal(value);
    return this;
  }

  public TypeSafeFieldEnd<AddressCriteria, Address, String> state() {
    return new TypeSafeFieldEnd<AddressCriteria, Address, String>(this, query, prefix + "state");
  }

  public AddressCriteria state(String value) {
    new TypeSafeFieldEnd<AddressCriteria, Address, String>(this, query, prefix + "state").equal(value);
    return this;
  }

  public TypeSafeFieldEnd<AddressCriteria, Address, String> zip() {
    return new TypeSafeFieldEnd<AddressCriteria, Address, String>(this, query, prefix + "zip");
  }

  public AddressCriteria zip(String value) {
    new TypeSafeFieldEnd<AddressCriteria, Address, String>(this, query, prefix + "zip").equal(value);
    return this;
  }
}
