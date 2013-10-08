package com.antwerkz.critter.criteria;

import com.antwerkz.critter.Item;
import com.antwerkz.critter.TypeSafeFieldEnd;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;
import org.bson.types.ObjectId;

public class ItemCriteria {
  private final Query query;
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
