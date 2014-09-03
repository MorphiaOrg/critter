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
<#list fields as field>

  public TypeSafeFieldEnd<${criteriaName}, ${name}, ${field.type}> ${field.name}() {
    return new TypeSafeFieldEnd<${criteriaName}, ${name}, ${field.type}>(this, query, prefix + "${field.name}");
  }

  public Criteria ${field.name}(${field.type} value) {
    return new TypeSafeFieldEnd<${criteriaName}, ${name}, ${field.type}>(this, query, prefix + "${field.name}").equal(value);
  }
</#list>