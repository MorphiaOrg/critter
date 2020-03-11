/**
 * Copyright (C) 2012-2013 Justin Lee <jlee@antwerkz.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antwerkz.critter.test;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity
public class Person extends AbstractPerson {
  @Id
  private ObjectId objectId;

  private String first;

  private String last;

  public Person() {
  }

  public Person(String first, String last) {
    this.first = first;
    this.last = last;
  }

  public ObjectId getObjectId() {
    return objectId;
  }

  public void setObjectId(final ObjectId objectId) {
    this.objectId = objectId;
  }

  public String getFirst() {
    return first;
  }

  public void setFirst(String first) {
    this.first = first;
  }

  public String getLast() {
    return last;
  }

  public void setLast(String last) {
    this.last = last;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Person person = (Person) o;

    if (first != null ? !first.equals(person.first) : person.first != null) {
      return false;
    }
    if (objectId != null ? !objectId.equals(person.objectId) : person.objectId != null) {
      return false;
    }
    if (last != null ? !last.equals(person.last) : person.last != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = objectId != null ? objectId.hashCode() : 0;
    result = 31 * result + (first != null ? first.hashCode() : 0);
    result = 31 * result + (last != null ? last.hashCode() : 0);
    return result;
  }
}
