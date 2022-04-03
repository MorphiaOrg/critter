package dev.morphia.critter.test;

public abstract class AbstractPerson {
  private Long age;

  public Long getAge() {
    return age;
  }

  public void setAge(final Long age) {
    this.age = age;
  }
}
