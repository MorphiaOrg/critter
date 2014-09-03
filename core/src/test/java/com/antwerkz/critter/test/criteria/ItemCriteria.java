package com.antwerkz.critter.test.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.antwerkz.critter.test.Item;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;


public class ItemCriteria {
  private final Query query;
  private final String prefix;

  public ItemCriteria(Query query, String prefix) {
    this.query = query;
    this.prefix = prefix + ".";
  }


  public TypeSafeFieldEnd<ItemCriteria, Item, java.lang.String> name() {
    return new TypeSafeFieldEnd<ItemCriteria, Item, java.lang.String>(this, query, prefix + "name");
  }

  public Criteria name(java.lang.String value) {
    return new TypeSafeFieldEnd<ItemCriteria, Item, java.lang.String>(this, query, prefix + "name").equal(value);
  }

  public TypeSafeFieldEnd<ItemCriteria, Item, java.lang.Double> price() {
    return new TypeSafeFieldEnd<ItemCriteria, Item, java.lang.Double>(this, query, prefix + "price");
  }

  public Criteria price(java.lang.Double value) {
    return new TypeSafeFieldEnd<ItemCriteria, Item, java.lang.Double>(this, query, prefix + "price").equal(value);
  }
}
