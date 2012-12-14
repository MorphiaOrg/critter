package ${package}.criteria;

import ${fqcn};
import com.google.code.morphia.*;
import com.google.code.morphia.query.*;
import com.google.code.morphia.Datastore;
import java.util.*;
import org.bson.types.CodeWScope;
<#--
<#list fields as field>
import ${field.type};
</#list>
-->
public class ${name}Criteria implements Query<${name}>{
  private Query<${name}> query;

  public Query<${name}> query() {
    return query;
  }

  public ${name}Criteria(Datastore ds) {
    query = ds.find(${name}.class);
  }

  @Override
  public CriteriaContainer or(Criteria... criteria) {
    return query.or(criteria);
  }

  @Override
  public CriteriaContainer and(Criteria... criteria) {
    return query.and(criteria);
  }

  @Override
  public FieldEnd<? extends Query<${name}>> field(String field) {
    return query.field(field);
  }

  @Override
  public FieldEnd<? extends CriteriaContainerImpl> criteria(String field) {
    return query.criteria(field);
  }


<#list fields as field>
  public FieldEnd<? extends CriteriaContainerImpl> ${field.name}() {
    return query.criteria("${field.name}");
  }
</#list>

  @Override
  public Query<${name}> filter(String condition, Object value) {
    return query.filter(condition, value);
  }

  @Override
  public Query<${name}> where(String js) {
    return query.where(js);
  }

  @Override
  public Query<${name}> where(CodeWScope js) {
    return query.where(js);
  }

  @Override
  public Query<${name}> order(String condition) {
    return query.order(condition);
  }

  @Override
  public Query<${name}> limit(int value) {
    return query.limit(value);
  }

  @Override
  public Query<${name}> batchSize(int value) {
    return query.batchSize(value);
  }

  @Override
  public Query<${name}> offset(int value) {
    return query.offset(value);
  }

  @Override
  @Deprecated
  public Query<${name}> skip(int value) {
    return query.skip(value);
  }

  @Override
  public Query<${name}> enableValidation() {
    return query.enableValidation();
  }

  @Override
  public Query<${name}> disableValidation() {
    return query.disableValidation();
  }

  @Override
  public Query<${name}> hintIndex(String idxName) {
    return query.hintIndex(idxName);
  }

  @Override
  public Query<${name}> retrievedFields(boolean include, String... fields) {
    return query.retrievedFields(include, fields);
  }

  @Override
  public Query<${name}> enableSnapshotMode() {
    return query.enableSnapshotMode();
  }

  @Override
  public Query<${name}> disableSnapshotMode() {
    return query.disableSnapshotMode();
  }

  @Override
  public Query<${name}> queryNonPrimary() {
    return query.queryNonPrimary();
  }

  @Override
  public Query<${name}> queryPrimaryOnly() {
    return query.queryPrimaryOnly();
  }

  @Override
  public Query<${name}> disableTimeout() {
    return query.disableTimeout();
  }

  @Override
  public Query<${name}> enableTimeout() {
    return query.enableTimeout();
  }

  @Override
  public Class getEntityClass() {
    return query.getEntityClass();
  }

  @Override
  public ${name} get() {
    return query.get();
  }

  @Override
  public Key getKey() {
    return query.getKey();
  }

  @Override
  public List asList() {
    return query.asList();
  }

  @Override
  public List asKeyList() {
    return query.asKeyList();
  }

  @Override
  public Iterable fetch() {
    return query.fetch();
  }

  @Override
  public Iterable fetchEmptyEntities() {
    return query.fetchEmptyEntities();
  }

  @Override
  public Iterable fetchKeys() {
    return query.fetchKeys();
  }

  @Override
  public long countAll() {
    return query.countAll();
  }

  @Override
  public Iterator<${name}> iterator() {
    return query.iterator();
  }

  @Override
  public Query<${name}> clone() {
    return query.clone();
  }
}
