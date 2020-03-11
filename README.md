critter
=======

Critter will look at your source code and generate type safe criteria builders for
each model object.  To use it, you simply need to add a plugin to your maven pom:

    <plugin>
        <groupId>com.antwerkz.critter</groupId>
        <artifactId>critter-maven</artifactId>
        <version>${critter.version}</version>
        <executions>
            <execution>
                <id>critter</id>
                <goals>
                    <goal>generate</goal>
                </goals>
                <configuration>
                    <force>false</force>
                    <criteriaPackage>com.bob.bar</criteriaPackage>
                </configuration>
            </execution>
        </executions>
    </plugin>

This will generate your criteria classes in `target/generated-sources/critter` and add the directory as a source
directory of your maven project.  If the `criteriaPackage` option is left out, the code
will be generated using the package of your entities with `.criteria` appended. (The options are shown here for
documentation purposes using the default values.  They can be left out altogether if the defaults are acceptable.)

Include the dependency in your pom.xml like this:

    <dependency>
        <groupId>com.antwerkz.critter</groupId>
        <artifactId>critter-core</artifactId>
        <version>${critter.version}</version>
    </dependency>

*Critter also requires morphia 1.3.2 and 3.6.0 of the mongo-java-driver and is built for Java 8.*

Gradle users can look [here](gradle/README.md) for details on using critter with gradle.

What difference does it make?
-----------------------------
Before critter, your criteria might look something like this:

    dev.morphia.query.Query<Query> query = ds.createQuery(Query.class);
    query.and(
      query.criteria("bookmark").equal(bookmark),
      query.criteria("database").equal(database)
    );

But using critter, it would look like this:

    QueryCriteria criteria = new QueryCriteria(datastore);
    criteria.and(
      criteria.bookmark(bookmark),
      criteria.database(database)
    );
    Query query = criteria.query().get();

Notice how bookmark() and database() methods were created based on the model object Query's fields.  The comparison
methods you're familiar with from Morphia's criteria API are all there but now only take the type of the field itself.
With this code in place if the model object changes, the code above runs the risk of failing to compile allowing you to
catch model/query conflicts at compile time rather than waiting for things to fail at runtime (or in your tests if you're
lucky enough to those).

You can see a working example in the [tests](https://github.com/evanchooly/critter/tree/master/tests).


IDEA Users
----------

IDEA users will need to enable the plugin registry in the maven configuration options for IDEA to pick up the plugin.
