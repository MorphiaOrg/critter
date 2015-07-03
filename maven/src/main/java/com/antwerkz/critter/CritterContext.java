package com.antwerkz.critter;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import org.jboss.forge.roaster.model.source.JavaClassSource;

public class CritterContext {
  private Map<String, CritterClass> classes = new HashMap<>();

  private String criteriaPkg;

  private boolean force;

  public CritterContext(final String criteriaPkg, final boolean force) {
    this.criteriaPkg = criteriaPkg;
    this.force = force;
  }

  public void add(final String aPackage, CritterClass critterClass) {
    classes.put(format("%s.%s", aPackage, critterClass.getName()), critterClass);
    if(criteriaPkg != null) {
      critterClass.setPackage(criteriaPkg);
    }
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

  public boolean isForce() {
    return force;
  }
}
