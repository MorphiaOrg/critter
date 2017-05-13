package com.antwerkz.critter.test

import org.testng.Assert.assertNotNull
import org.testng.Assert.assertNull
import org.testng.annotations.Test
import java.lang.reflect.Method
import java.util.TreeMap

class KotlinModelTest {
    @Test
    fun api() {
        val methods = methods("com.antwerkz.critter.test.criteria.PersonCriteria\$PersonUpdater")
        assertNull(methods, "addAllToAge")
        assertNull(methods, "incLast")
    }

    @Test
    fun inheritance() {
        val methods = methods("com.antwerkz.critter.test.criteria.UserCriteria")
        assertNotNull(methods, "age")
        assertNotNull(methods, "email")
        assertNotNull(methods, "first")
        assertNotNull(methods, "last")
    }

    private fun assertNotNull(methods: Map<String, Method>, name: String) {
        assertNotNull(methods[name], "Should find " + name)
    }

    private fun assertNull(methods: Map<String, Method>, name: String) {
        assertNull(methods[name], "Should not find " + name)
    }

    private fun methods(klass: String): Map<String, Method> {
        val map = TreeMap<String, Method>()
        for (method in Class.forName(klass).declaredMethods) {
            map.put(method.name, method)
        }
        return map
    }
}
