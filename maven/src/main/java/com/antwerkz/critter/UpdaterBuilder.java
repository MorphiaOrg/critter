package com.antwerkz.critter;

import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import static java.lang.String.format;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

public class UpdaterBuilder {
  public UpdaterBuilder(final CritterClass critterClass, final JavaClassSource criteriaClass) {
    final String type = critterClass.getName() + "Updater";
    final MethodSource<JavaClassSource> getUpdater = criteriaClass.addMethod()
        .setPublic()
        .setName("getUpdater")
        .setReturnType(type);
    getUpdater
        .setBody(format("return new %s();", getUpdater.getReturnType()));

    final JavaClassSource updater = Roaster.create(JavaClassSource.class)
        .setPublic()
        .setName(type);

    criteriaClass.addImport(UpdateOperations.class);
    criteriaClass.addImport(UpdateResults.class);
    criteriaClass.addImport(WriteConcern.class);
    criteriaClass.addImport(WriteResult.class);

    final FieldSource<JavaClassSource> updateOperations = updater.addField()
        .setType(format("UpdateOperations<%s>", critterClass.getName()))
        .setLiteralInitializer(format("ds.createUpdateOperations(%s.class);", critterClass.getName()));
    updateOperations.setName("updateOperations");

    updater.addMethod()
        .setPublic()
        .setName("updateAll")
        .setReturnType(UpdateResults.class)
        .setBody("return ds.update(query(), updateOperations, false);");

    updater.addMethod()
        .setPublic()
        .setName("updateFirst")
        .setReturnType(UpdateResults.class)
        .setBody("return ds.updateFirst(query(), updateOperations, false);");

    updater.addMethod()
        .setPublic()
        .setName("updateAll")
        .setReturnType(UpdateResults.class)
        .setBody("return ds.update(query(), updateOperations, false, wc);")
        .addParameter(WriteConcern.class, "wc");

    updater.addMethod()
        .setPublic()
        .setName("updateFirst")
        .setReturnType(UpdateResults.class)
        .setBody("return ds.updateFirst(query(), updateOperations, false, wc);")
        .addParameter(WriteConcern.class, "wc");

    updater.addMethod()
        .setPublic()
        .setName("upsert")
        .setReturnType(UpdateResults.class)
        .setBody("return ds.update(query(), updateOperations, true);");

    updater.addMethod()
        .setPublic()
        .setName("upsert")
        .setReturnType(UpdateResults.class)
        .setBody("return ds.update(query(), updateOperations, true, wc);")
        .addParameter(WriteConcern.class, "wc");

    updater.addMethod()
        .setPublic()
        .setName("remove")
        .setReturnType(WriteResult.class)
        .setBody("return ds.delete(query());");

    updater.addMethod()
        .setPublic()
        .setName("remove")
        .setReturnType(WriteResult.class)
        .setBody("return ds.delete(query(), wc);")
        .addParameter(WriteConcern.class, "wc");

    for (CritterField field : critterClass.getFields()) {
      if (!field.getParameterTypes().isEmpty()) {
        field.getParameterTypes()
            .stream()
            .forEach(criteriaClass::addImport);
      }

      criteriaClass.addImport(field.getFullType());
      if (!field.hasAnnotation(Id.class)) {
        updater.addMethod()
            .setPublic()
            .setName(field.getName())
            .setReturnType(type)
            .setBody(format("updateOperations.set(\"%s\", value);\nreturn this;", field.getName()))
            .addParameter(field.getParameterizedType(), "value");

        updater.addMethod()
            .setPublic()
            .setName(format("unset%s", nameCase(field.getName())))
            .setReturnType(type)
            .setBody(format("updateOperations.unset(\"%s\");\nreturn this;", field.getName()));

        numerics(type, updater, field);
        containers(type, updater, field);
      }
    }

    criteriaClass.addNestedType(updater);
  }

  private void numerics(final String type, final JavaClassSource updater, final CritterField field) {
    if(field.isNumeric()) {
      updater.addMethod()
          .setPublic()
          .setName(format("dec%s", nameCase(field.getName())))
          .setReturnType(type)
          .setBody(format("updateOperations.dec(\"%s\");\nreturn this;", field.getName()));

      updater.addMethod()
          .setPublic()
          .setName(format("inc%s", nameCase(field.getName())))
          .setReturnType(type)
          .setBody(format("updateOperations.inc(\"%s\");\nreturn this;", field.getName()));


      updater.addMethod()
          .setPublic()
          .setName(format("inc%s", nameCase(field.getName())))
          .setReturnType(type)
          .setBody(format("updateOperations.inc(\"%s\", value);\nreturn this;", field.getName()))
          .addParameter(field.getFullType(), "value");
    }
  }
  private void containers(final String type, final JavaClassSource updater, final CritterField field) {
    if(field.isContainer()) {

      updater.addMethod()
          .setPublic()
          .setName(format("addTo%s", nameCase(field.getName())))
          .setReturnType(type)
          .setBody(format("updateOperations.add(\"%s\", value);\nreturn this;", field.getName()))
          .addParameter(field.getParameterizedType(), "value");

      MethodSource<JavaClassSource> addItems = updater.addMethod()
          .setPublic()
          .setName(format("addTo%s", nameCase(field.getName())))
          .setReturnType(type)
          .setBody(format("updateOperations.add(\"%s\", value, addDups);\nreturn this;", field.getName()));
      addItems
          .addParameter(field.getParameterizedType(), "value");
      addItems
          .addParameter("boolean", "addDups");

      addItems = updater.addMethod()
          .setPublic()
          .setName(format("addAllTo%s", nameCase(field.getName())))
          .setReturnType(type)
          .setBody(format("updateOperations.addAll(\"%s\", values, addDups);\nreturn this;", field.getName()));
      addItems.addParameter(field.getParameterizedType(), "values");
      addItems.addParameter("boolean", "addDups");

      updater.addMethod()
          .setPublic()
          .setName(format("removeFirstFrom%s", nameCase(field.getName())))
          .setReturnType(type)
          .setBody(format("updateOperations.removeFirst(\"%s\");\nreturn this;", field.getName()));

      updater.addMethod()
          .setPublic()
          .setName(format("removeLastFrom%s", nameCase(field.getName())))
          .setReturnType(type)
          .setBody(format("updateOperations.removeLast(\"%s\");\nreturn this;", field.getName()));

      updater.addMethod()
          .setPublic()
          .setName(format("removeFrom%s", nameCase(field.getName())))
          .setReturnType(type)
          .setBody(format("updateOperations.removeAll(\"%s\", value);\nreturn this;", field.getName()))
          .addParameter(field.getParameterizedType(), "value");

      MethodSource<JavaClassSource> removeAll = updater.addMethod()
          .setPublic()
          .setName(format("removeAllFrom%s", nameCase(field.getName())))
          .setReturnType(type)
          .setBody(format("updateOperations.removeAll(\"%s\", values);\nreturn this;", field.getName()));
      removeAll.addParameter(field.getParameterizedType(), "values");
    }
  }

  private String nameCase(final String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }
}
