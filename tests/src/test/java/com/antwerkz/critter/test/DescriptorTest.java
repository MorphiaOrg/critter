package com.antwerkz.critter.test;

import java.lang.reflect.Field;

import static java.lang.String.format;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DescriptorTest {
  @Test
  public void address() throws ReflectiveOperationException {
    final Class<?> descriptor = Class.forName("com.antwerkz.critter.test.criteria.AddressDescriptor");

    for (Field field : Address.class.getDeclaredFields()) {
      try {
        descriptor.getDeclaredField(field.getName());
      } catch (NoSuchFieldException e) {
        Assert.fail(format("Could not find a field named '%s' on PersonDescriptor", field.getName()));
      }
    }

    Assert.assertEquals(descriptor.getDeclaredField("city").get(null), "c");
    Assert.assertEquals(descriptor.getDeclaredField("state").get(null), "state");
  }

  @Test
  public void person() throws ReflectiveOperationException {
    final Class<?> descriptor = Class.forName("com.antwerkz.critter.test.criteria.PersonDescriptor");

    for (Field field : Person.class.getDeclaredFields()) {
      try {
        descriptor.getDeclaredField(field.getName());
      } catch (NoSuchFieldException e) {
        Assert.fail(format("Could not find a field named '%s' on PersonDescriptor", field.getName()));
      }
    }
  }
}
