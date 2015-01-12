package com.antwerkz.critter.test;

import com.antwerkz.critter.test.criteria.PersonCriteria.PersonUpdater;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

public class ModelTest {
    @Test
    public void api() {
        final Map<String, Method> methods = getFields(PersonUpdater.class);
        assertNull(methods, "addAllToAge");
        assertNull(methods, "incLast");
    }

    private void assertNull(final Map<String, Method> methods, final String name) {
        Assert.assertNull(methods.get(name), "Should not find " + name);
    }

    private Map<String, Method> getFields(final Class klass) {
        final Method[] declaredFields = klass.getDeclaredMethods();
        final Map<String, Method> map = new TreeMap<>();
        for (Method method : declaredFields) {
            map.put(method.getName(), method);
        }
        return map;
    }
}
