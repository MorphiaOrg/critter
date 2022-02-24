package dev.morphia.critter.test;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;

@Entity
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
  public String toString() {
    return "Address{" +
               "city='" + city + '\'' +
               ", state='" + state + '\'' +
               ", zip='" + zip + '\'' +
               '}';
  }
}
