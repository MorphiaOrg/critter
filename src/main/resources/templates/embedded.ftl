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
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import org.bson.types.ObjectId;
<#--
<#list fields as field>
import ${field.type};
</#list>
-->

public class ${name}Criteria {
  private Query<${fqcn}> query;
  private String prefix;

  public ${name}Criteria(Query query, String prefix) {
    this.query = query;
    this.prefix = prefix;
  }

<#list fields as field>

  public TypeSafeFieldEnd<? extends CriteriaContainer, ${fqcn}, ${field.type}> ${field.name}() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".${field.name}"));
  }

  public ${name}Criteria ${field.name}(${field.type} value) {
    new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".${field.name}")).equal(value);
    return this;
  }
</#list>
}
