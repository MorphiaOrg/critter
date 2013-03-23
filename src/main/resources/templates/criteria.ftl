package ${package}.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;
import com.mongodb.DBRef;
import org.bson.types.ObjectId;
<#--
<#list fields as field>
import ${field.type};
</#list>
-->

public class ${name}Criteria {
  private Query<${fqcn}> query;
  private Datastore ds;

  public Query<${fqcn}> query() {
    return query;
  }

  public ${name}Criteria(Datastore ds) {
    this.ds = ds;
    query = ds.find(${fqcn}.class);
  }

  public CriteriaContainer or(Criteria... criteria) {
    return query.or(criteria);
  }

  public CriteriaContainer and(Criteria... criteria) {
    return query.and(criteria);
  }
<#list fields as field>

  public TypeSafeFieldEnd<? extends CriteriaContainer, ${fqcn}, ${field.type}> ${field.name}() {
    return new TypeSafeFieldEnd<>(query, query.criteria("${field.name}"));
  }

  public ${name}Criteria distinct${field.name?cap_first}() {
    ((QueryImpl) query).getCollection().distinct("${field.name}");
    return this;
  }
</#list>
<#list embeddeds as embed>

  public ${embed.type}Criteria ${embed.name}() {
    return new ${embed.type}Criteria(query, "${embed.name}");
  }
</#list>
<#list references as reference>

  public ${name}Criteria ${reference.name}(${reference.type} reference) {
    query.filter("${reference.name} = ", reference);
    return this;
  }
</#list>
}
