package com.antwerkz.critter.test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ModelTest {
  @Test
  public void api() {
    final Map<String, Method> methods = getFields("com.antwerkz.critter.test.criteria.PersonCriteria$PersonUpdater");
    assertNull(methods, "addAllToAge");
    assertNull(methods, "incLast");
  }

  @Test
  public void inheritance() {
    final Map<String, Method> methods = getFields("com.antwerkz.critter.test.criteria.UserCriteria");
    assertNotNull(methods, "age");
    assertNotNull(methods, "email");
    assertNotNull(methods, "last");
  }

  private void assertNotNull(final Map<String, Method> methods, final String name) {
    Assert.assertNotNull(methods.get(name), "Should find " + name);
  }

  private void assertNull(final Map<String, Method> methods, final String name) {
    Assert.assertNull(methods.get(name), "Should not find " + name);
  }

  private Map<String, Method> getFields(final String klass) {
    final Method[] declaredFields;
    try {
      declaredFields = Class.forName(klass).getDeclaredMethods();
      final Map<String, Method> map = new TreeMap<>();
      for (Method method : declaredFields) {
        map.put(method.getName(), method);
      }
      return map;
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
