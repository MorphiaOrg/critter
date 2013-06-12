package com.antwerkz.critter.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;
import org.bson.types.ObjectId;

public class ItemCriteria {
  private Query<com.antwerkz.critter.Item> query;
  private String prefix;

  public ItemCriteria(Query query, String prefix) {
    this.query = query;
    this.prefix = prefix + ".";
  }


  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Item, java.lang.String> name() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + "name"));
  }

  public ItemCriteria name(java.lang.String value) {
    new TypeSafeFieldEnd<>(query, query.criteria(prefix + "name")).equal(value);
    return this;
  }

  public ItemCriteria orderByName() {
    return orderByName(true);
  }

  public ItemCriteria orderByName(boolean ascending) {
    query.order((!ascending ? "-" : "") + prefix + "name");
    return this;
  }

  public ItemCriteria distinctName() {
    ((QueryImpl) query).getCollection().distinct(prefix + "name");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Item, java.lang.Double> price() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + "price"));
  }

  public ItemCriteria price(java.lang.Double value) {
    new TypeSafeFieldEnd<>(query, query.criteria(prefix + "price")).equal(value);
    return this;
  }

  public ItemCriteria orderByPrice() {
    return orderByPrice(true);
  }

  public ItemCriteria orderByPrice(boolean ascending) {
    query.order((!ascending ? "-" : "") + prefix + "price");
    return this;
  }

  public ItemCriteria distinctPrice() {
    ((QueryImpl) query).getCollection().distinct(prefix + "price");
    return this;
  }
}
