package dev.morphia.critter.test;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Property;

import java.util.Objects;

@Embedded
public class Address {
  @Property("c")
  private String city;
  private String state;
  private String zip;

  public Address() {
  }

  public Address(final String city, final String state, final String zip) {
    this.city = city;
    this.state = state;
    this.zip = zip;
  }

  public String getCity() {
    return city;
  }

  public void setCity(final String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(final String state) {
    this.state = state;
  }

  public String getZip() {
    return zip;
  }

  public void setZip(final String zip) {
    this.zip = zip;
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
    return Objects.equals(city, address.city) && Objects.equals(state, address.state) &&
           Objects.equals(zip, address.zip);
  }

  @Override
  public int hashCode() {
    return Objects.hash(city, state, zip);
  }

  @Override
  public String toString() {
    return "Address{" +
               "city='" + city + '\'' +
               ", state='" + state + '\'' +
               ", zip='" + zip + '\'' +
               '}';
  }
}
