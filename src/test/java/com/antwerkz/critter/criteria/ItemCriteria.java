package com.antwerkz.critter.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;
import org.bson.types.ObjectId;

public class ItemCriteria {
  private final Query<com.antwerkz.critter.Item> query;
  private final String prefix;

  public ItemCriteria(Query query, String prefix) {
    this.query = query;
    this.prefix = prefix + ".";
  }


  public TypeSafeFieldEnd<ItemCriteria, Item, java.lang.String> name() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "name");
  }

  public ItemCriteria name(java.lang.String value) {
    new TypeSafeFieldEnd<>(this, query, prefix + "name").equal(value);
    return this;
  }

  public TypeSafeFieldEnd<ItemCriteria, Item, java.lang.Double> price() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "price");
  }

  public ItemCriteria price(java.lang.Double value) {
    new TypeSafeFieldEnd<>(this, query, prefix + "price").equal(value);
    return this;
  }
}
