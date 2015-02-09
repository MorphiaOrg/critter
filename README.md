critter
=======

Critter will look at your source code and generate type safe criteria builders for
each model object.  To use it, you simply need to add a plugin to your maven pom:

    <plugin>
        <groupId>com.antwerkz.critter</groupId>
        <artifactId>critter-maven</artifactId>
        <version>${project.version}</version>
        <executions>
            <execution>
                <id>critter</id>
                <goals>
                    <goal>generate</goal>
                </goals>
                <configuration>
                    <criteriaPackage>com.antwerkz.critter.criteria</criteriaPackage>
                    <includes>**/*.java</includes>
                    <sourceDirectory>src/main/java</sourceDirectory>
                </configuration>
            </execution>
        </executions>
    </plugin>

This will generate your criteria classes straight in to src/main/java.  It's a little less than ideal but it
is [changing](https://github.com/evanchooly/critter/issues/4) soon.  If the `criteriaPkg` option is left out, the code
will be generated using `com.antwerkz.critter.criteria`.  (The options are shown here for documentation
purposes using the default values.  They can be left out altogether if the defaults are acceptable.)

Include the dependency in your pom.xml like this:

    <dependency>
        <groupId>com.antwerkz.critter</groupId>
        <artifactId>critter-core</artifactId>
        <version>2.0.0</version>
    </dependency>

What difference does it make?
-----------------------------
Before critter, your criteria might look something like this:

    org.mongodb.morphia.query.Query<Query> query = ds.createQuery(Query.class);
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
