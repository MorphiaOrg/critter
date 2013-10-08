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

import ${fqcn};
import com.antwerkz.critter.TypeSafeFieldEnd;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;
import org.bson.types.ObjectId;
<#--
<#list fields as field>
import ${field.type};
</#list>
-->

public class ${criteriaName} {
  private final Query<${fqcn}> query;
  private final String prefix;

  public ${criteriaName}(Query<${fqcn}> query, String prefix) {
    this.query = query;
    this.prefix = prefix + ".";
  }

<#include "fields.ftl">
}
