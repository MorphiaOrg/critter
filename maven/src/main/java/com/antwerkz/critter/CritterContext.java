package com.antwerkz.critter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CritterContext {
  private Map<String, CritterClass> classes = new HashMap<>();

  public void add(CritterClass critterClass) {
    classes.put(critterClass.getName(), critterClass);
  }

  public Collection<CritterClass> getClasses() {
    return classes.values();
  }
}
