package com.antwerkz.test.kotlin

import org.testng.Assert
import org.testng.annotations.Test

import java.lang.reflect.Method
import java.util.TreeMap

class KotlinModelTest {
    @Test
    fun api() {
        val methods = getFields("com.antwerkz.critter.test.criteria.PersonCriteria\$PersonUpdater")
        assertNull(methods, "addAllToAge")
        assertNull(methods, "incLast")
    }

    @Test
    fun inheritance() {
        val methods = getFields("com.antwerkz.critter.test.criteria.UserCriteria")
        assertNotNull(methods, "age")
        assertNotNull(methods, "email")
        assertNotNull(methods, "last")
    }

    private fun assertNotNull(methods: Map<String, Method>, name: String) {
        Assert.assertNotNull(methods[name], "Should find " + name)
    }

    private fun assertNull(methods: Map<String, Method>, name: String) {
        Assert.assertNull(methods[name], "Should not find " + name)
    }

    private fun getFields(klass: String): Map<String, Method> {
        val map = TreeMap<String, Method>()
        for (method in Class.forName(klass).declaredMethods) {
            map.put(method.name, method)
        }
        return map
    }
}
