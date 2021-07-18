/**
 * Copyright (C) 2012-2020 Justin Lee <jlee@antwerkz.com>
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
package dev.morphia.critter.test;

import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import org.bson.types.ObjectId;

import java.util.Objects;

@Entity(cap = @CappedAt(count = 12))
@Indexes({
    @Index(fields = @Field("1")),
    @Index(fields = @Field("2")),
    @Index(fields = @Field("3"))})
public class Person extends AbstractPerson {
  @Id
  private ObjectId id;

  private String firstName;

  private String lastName;

  public Person() {
  }

  public Person(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public ObjectId getId() {
    return id;
  }

  public void setId(final ObjectId id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Person)) {
      return false;
    }
    Person person = (Person) o;
    return Objects.equals(id, person.id) && Objects.equals(firstName, person.firstName) &&
           Objects.equals(lastName, person.lastName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, firstName, lastName);
  }
}
