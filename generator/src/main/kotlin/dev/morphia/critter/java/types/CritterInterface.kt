package dev.morphia.critter.java.types

import dev.morphia.critter.java.CritterType
import dev.morphia.critter.java.JavaContext
import org.jboss.forge.roaster.model.source.JavaInterfaceSource

class CritterInterface(context: JavaContext, val source: JavaInterfaceSource) : CritterType(context, source)