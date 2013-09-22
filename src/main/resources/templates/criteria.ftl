<#--

    Copyright (C) 2012-2013 Justin Lee <jlee@antwerkz.com>

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
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
import java.util.List;

public class ${name}Criteria {
  private final Query<${fqcn}> query;
  private final Datastore ds;
  private String prefix = "";

  public ${name}Criteria(Datastore ds) {
    this.ds = ds;
    query = ds.find(${fqcn}.class);
  }

  public Query<${fqcn}> query() {
    return query;
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

<#include "fields.ftl">

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

  public ${name}Updater getUpdater() {
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

    public ${name}Updater unset${field.name?cap_first}(${field.type} value) {
      updateOperations.unset("${field.name}");
      return this;
    }

    public ${name}Updater add${field.name?cap_first}(${field.type} value) {
      updateOperations.add("${field.name}", value);
      return this;
    }

    public ${name}Updater add${field.name?cap_first}(String fieldExpr, ${field.type} value, boolean addDups) {
      updateOperations.add("${field.name}", value, addDups);
      return this;
    }

    public ${name}Updater addAllTo${field.name?cap_first}(List<${field.type}> values, boolean addDups) {
      updateOperations.addAll("${field.name}", values, addDups);
      return this;
    }
  
    public ${name}Updater removeFirst${field.name?cap_first}() {
      updateOperations.removeFirst("${field.name}");
      return this;
    }
  
    public ${name}Updater removeLast${field.name?cap_first}() {
      updateOperations.removeLast("${field.name}");
      return this;
    }
  
    public ${name}Updater removeFrom${field.name?cap_first}(${field.type} value) {
      updateOperations.removeAll("${field.name}", value);
      return this;
    }

    public ${name}Updater removeAllFrom${field.name?cap_first}(List<${field.type}> values) {
      updateOperations.removeAll("${field.name}", values);
      return this;
    }
 
    public ${name}Updater dec${field.name?cap_first}() {
      updateOperations.dec("${field.name}");
      return this;
    }

    public ${name}Updater inc${field.name?cap_first}() {
      updateOperations.inc("${field.name}");
      return this;
    }

    public ${name}Updater inc${field.name?cap_first}(Number value) {
      updateOperations.inc("${field.name}", value);
      return this;
    }
  </#list>
  }
}
