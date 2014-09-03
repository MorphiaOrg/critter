package com.antwerkz.critter.test.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.antwerkz.critter.test.Address;
import org.mongodb.morphia.query.Criteria;
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

  public Criteria city(java.lang.String value) {
    return new TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "city").equal(value);
  }

  public TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String> state() {
    return new TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "state");
  }

  public Criteria state(java.lang.String value) {
    return new TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "state").equal(value);
  }

  public TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String> zip() {
    return new TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "zip");
  }

  public Criteria zip(java.lang.String value) {
    return new TypeSafeFieldEnd<AddressCriteria, Address, java.lang.String>(this, query, prefix + "zip").equal(value);
  }
}
