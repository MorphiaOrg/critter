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
    return new com.antwerkz.critter.TypeSafeFieldEnd<>(this, query, prefix + "name");
  }

  public ItemCriteria name(java.lang.String value) {
    new com.antwerkz.critter.TypeSafeFieldEnd<ItemCriteria, Item, java.lang.String>(this, query, prefix + "name").equal(value);
    return this;
  }

  public com.antwerkz.critter.TypeSafeFieldEnd<ItemCriteria, Item, java.lang.Double> price() {
    return new com.antwerkz.critter.TypeSafeFieldEnd<>(this, query, prefix + "price");
  }

  public ItemCriteria price(java.lang.Double value) {
    new com.antwerkz.critter.TypeSafeFieldEnd<ItemCriteria, Item, java.lang.Double>(this, query, prefix + "price").equal(value);
    return this;
  }
}
