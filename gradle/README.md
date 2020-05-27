Gradle Support
=======

To enable this plugin, add the following line to your buildscript dependencies section:

```groovy
    classpath group: 'dev.morphia.critter', name: 'critter-gradle', version: '${critter.version}', changing: true
```

The plugin can be configured as follows:

```groovy
critter {
  force = true
  criteriaPackage "com.bob.bar"
}
```

Both values are optional:

* `force` will ignore timestamps and generate the critter files always.  By default, new critter files are only
    generated when the entity's source changes.
* `criteriaPackage` defines the package name used in the generated source.  By default, the entity's package name will
 be used with `.criteria` appended.

IDEA
----
If you'll need to add the following to your `build.gradle`:

```groovy
idea {
  module {
    excludeDirs -= file('build')
  }
}
```

Unless the build dir is excluded, the generated source directory doesn't show up as a source directory idea.  This is
less than ideal but for now is necessary.

Once that's done, you'll need to add dependency in your project's dependencies as well:

```groovy
  compile "dev.morphia.critter:critter-core:${critter.version}"
```