package com.antwerkz.critter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jboss.forge.roaster.model.source.JavaClassSource;

public class CritterContext {
  private Map<String, CritterClass> classes = new HashMap<>();

  private String criteriaPkg;

  public CritterContext(final String criteriaPkg) {

    this.criteriaPkg = criteriaPkg;
  }

  public void add(CritterClass critterClass) {
    classes.put(critterClass.getName(), critterClass);
    critterClass.setPackage(criteriaPkg);
  }

  public CritterClass get(String name) {
    return classes.get(name);
  }

  public Collection<CritterClass> getClasses() {
    return classes.values();
  }

  public boolean isEmbedded(final JavaClassSource clazz) {
    final CritterClass critterClass = get(clazz.getName());
    return critterClass != null && critterClass.isEmbedded();
  }
}
