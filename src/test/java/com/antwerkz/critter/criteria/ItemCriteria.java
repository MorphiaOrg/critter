package com.antwerkz.critter.criteria;

import com.antwerkz.critter.Item;

public class ItemCriteria {
  private final org.mongodb.morphia.query.Query query;
  private final String prefix;

  public ItemCriteria(org.mongodb.morphia.query.Query query, String prefix) {
    this.query = query;
    this.prefix = prefix + ".";
  }


  public com.antwerkz.critter.TypeSafeFieldEnd<ItemCriteria, Item, java.lang.String> name() {
    return new com.antwerkz.critter.TypeSafeFieldEnd<ItemCriteria, Item, java.lang.String>(this, query, prefix + "name");
  }

  public org.mongodb.morphia.query.Criteria name(java.lang.String value) {
    return new com.antwerkz.critter.TypeSafeFieldEnd<ItemCriteria, Item, java.lang.String>(this, query, prefix + "name").equal(value);
  }

  public com.antwerkz.critter.TypeSafeFieldEnd<ItemCriteria, Item, java.lang.Double> price() {
    return new com.antwerkz.critter.TypeSafeFieldEnd<ItemCriteria, Item, java.lang.Double>(this, query, prefix + "price");
  }

  public org.mongodb.morphia.query.Criteria price(java.lang.Double value) {
    return new com.antwerkz.critter.TypeSafeFieldEnd<ItemCriteria, Item, java.lang.Double>(this, query, prefix + "price").equal(value);
  }
}
