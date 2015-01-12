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
package ${packageName}.criteria;

<#list imports as import>
import ${import};
</#list>

public class ${criteriaName} extends BaseCriteria<${name}> {
  private String prefix = "";

  public ${criteriaName}(Datastore ds) {
    super(ds, ${name}.class);
  }

<#include "fields.ftl">

<#list embeddeds as embedded>

  public ${embedded.criteriaType} ${embedded.name}() {
    return new ${embedded.criteriaType}(query, "${embedded.name}");
  }
</#list>
<#list references as reference>

  public ${criteriaName} ${reference.name}(${reference.type} reference) {
    query.filter("${reference.name} = ", reference);
    return this;
  }
</#list>

  public ${name}Updater getUpdater() {
    return new ${name}Updater();
  }

  public class ${name}Updater {
    UpdateOperations<${name}> updateOperations;

    public ${name}Updater() {
      updateOperations = ds.createUpdateOperations(${name}.class);
    }

    public UpdateResults update() {
      return ds.update(query(), updateOperations, false);
    }

    public UpdateResults update(WriteConcern wc) {
      return ds.update(query(), updateOperations, false, wc);
    }

    public UpdateResults upsert() {
      return ds.update(query(), updateOperations, true);
    }

    public UpdateResults upsert(WriteConcern wc) {
      return ds.update(query(), updateOperations, true, wc);
    }

    // Updater Methods
  <#list fields as field>
    <#if !field.isId() >
    public ${name}Updater ${field.name}(${field.type} value) {
    updateOperations.set("${field.name}", value);
    return this;
    }

    public ${name}Updater unset${field.name?cap_first}() {
    updateOperations.unset("${field.name}");
    return this;
    }

      <#if field.isContainerType()>
      public ${name}Updater add${field.name?cap_first}(${field.type} value) {
      updateOperations.add("${field.name}", value);
      return this;
      }

      public ${name}Updater add${field.name?cap_first}(${field.type} value, boolean addDups) {
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
      </#if>

      <#if field.isNumericType()>
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
      </#if>
    </#if>
  </#list>
    // Updater Methods
  }
}
