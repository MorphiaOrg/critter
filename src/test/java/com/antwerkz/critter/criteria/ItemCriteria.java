package com.antwerkz.critter.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import org.bson.types.ObjectId;

public class ItemCriteria {
  private Query<com.antwerkz.critter.Item> query;
  private String prefix;

  public ItemCriteria(Query query, String prefix) {
    this.query = query;
    this.prefix = prefix;
  }


  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Item, java.lang.String> name() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".name"));
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Item, java.lang.Double> price() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".price"));
  }
}
