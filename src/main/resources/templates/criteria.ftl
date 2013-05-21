package ${package}.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;
import com.google.code.morphia.query.UpdateOperations;
import com.google.code.morphia.query.UpdateResults;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

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

  public WriteResult delete() {
     return ds.delete(query());
  }

  public WriteResult delete(WriteConcern wc) {
     return ds.delete(query(), wc);
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

  public ${name}Criteria ${field.name}(${field.type} value) {
    new TypeSafeFieldEnd<>(query, query.criteria("${field.name}")).equal(value);
    return this;
  }

  public ${name}Criteria orderBy${field.name?cap_first}() {
    return orderBy${field.name?cap_first}(true);
  }

  public ${name}Criteria orderBy${field.name?cap_first}(boolean ascending) {
    query.order((!ascending ? "-" : "") + "${field.name}");
    return this;
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

  public ${name}Updater update() {
    return new ${name}Updater();
  }

  public class ${name}Updater {
    UpdateOperations<${fqcn}> updateOperations;

    public ${name}Updater() {
      updateOperations = ds.createUpdateOperations(${fqcn}.class);
    }

    public UpdateResults<${fqcn}> update() {
      return ds.update(query(), updateOperations, false);
    }

    public UpdateResults<${fqcn}> update(WriteConcern wc) {
      return ds.update(query(), updateOperations, false, wc);
    }

    public UpdateResults<${fqcn}> upsert() {
      return ds.update(query(), updateOperations, true);
    }

    public UpdateResults<${fqcn}> upsert(WriteConcern wc) {
      return ds.update(query(), updateOperations, true, wc);
    }

  <#list fields as field>
    public ${name}Updater ${field.name}(${field.type} value) {
      updateOperations.set("${field.name}", value);
      return this;
    }
  </#list>
  }
}
