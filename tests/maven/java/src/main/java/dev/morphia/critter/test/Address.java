package dev.morphia.critter.test;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Property;

import java.util.Objects;
import java.util.StringJoiner;

@Embedded
public class Address {
  @Property("c")
  private final String city;
  private final String state;
  private final String zip;


  public Address(final String city, final String state, final String zip) {
    this.city = city;
    this.state = state;
    this.zip = zip;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getZip() {
    return zip;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Address)) {
      return false;
    }
    Address address = (Address) o;
    boolean equals = Objects.equals(city, address.city);
    boolean equals1 = Objects.equals(state, address.state);
    boolean equals2 = Objects.equals(zip, address.zip);
    return equals && equals1 &&
           equals2;
  }

  @Override
  public int hashCode() {
    return Objects.hash(city, state, zip);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Address.class.getSimpleName() + "[", "]")
               .add("city='" + city + "'")
               .add("state='" + state + "'")
               .add("zip='" + zip + "'")
               .toString();
  }
}
