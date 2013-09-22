package com.antwerkz.critter.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;
import org.bson.types.ObjectId;

public class Invoice_AddressCriteria {
  private final Query<com.antwerkz.critter.Invoice.Address> query;
  private final String prefix;

  public Invoice_AddressCriteria(Query query, String prefix) {
    this.query = query;
    this.prefix = prefix + ".";
  }


  public TypeSafeFieldEnd<Invoice_AddressCriteria, com.antwerkz.critter.Invoice.Address, java.lang.String> city() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "city");
  }

  public Invoice_AddressCriteria city(java.lang.String value) {
    new TypeSafeFieldEnd<>(this, query, prefix + "city").equal(value);
    return this;
  }

  public TypeSafeFieldEnd<Invoice_AddressCriteria, com.antwerkz.critter.Invoice.Address, java.lang.String> state() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "state");
  }

  public Invoice_AddressCriteria state(java.lang.String value) {
    new TypeSafeFieldEnd<>(this, query, prefix + "state").equal(value);
    return this;
  }

  public TypeSafeFieldEnd<Invoice_AddressCriteria, com.antwerkz.critter.Invoice.Address, java.lang.String> zip() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "zip");
  }

  public Invoice_AddressCriteria zip(java.lang.String value) {
    new TypeSafeFieldEnd<>(this, query, prefix + "zip").equal(value);
    return this;
  }
}
