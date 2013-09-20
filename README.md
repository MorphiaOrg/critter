critter
=======

Critter will look at your morphia object model and generate type safe criteria builders for
each model object.  To use it, you typically won't need to do anything.  It's standard
annotation processor so the compiler should find it automatically.  If you have disabled annotation
processing for whatever reason, you can add the following to your pom.xml to enable processing:

    <plugin>
        <groupId>org.bsc.maven</groupId>
        <artifactId>maven-processor-plugin</artifactId>
        <version>2.1.0</version>
        <executions>
            <execution>
                <id>process</id>
                <goals>
                    <goal>process</goal>
                </goals>
                <phase>compile</phase>
            </execution>
        </executions>
    </plugin>

For now, it generates straight in to src/main/java.  It's a little less than ideal.  I'd rather have it generate in to
say, src/main/generated/critter, but maven wasn't cooperating and it wasn't that big of a deal.  The packages for the
generated classes are computed by the packages of the entity objects so their sequestered packagewise in any case.

Include the dependency in your pom.xml like this:

    <dependency>
        <groupId>com.antwerkz.critter</groupId>
        <artifactId>critter</artifactId>
        <version>1.2.0</version>
    </dependency>

What difference does it make?
-----------------------------
Before critter, your criteria might look something like this:

    com.google.code.morphia.query.Query<Query> query = ds.createQuery(Query.class);
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
methods you're familiar with from morphia's criteria API are all there but now only take the type of the field itself.
With this code in place if the model object changes, the code above runs the risk of failing to compile allowing you to
catch model/query conflicts at compile time rather than waiting for things to fail at runtime (or in your tests if you're
lucky enough to those).

You can see a working example in the [tests](https://github.com/evanchooly/critter/tree/master/src/test) and in critter's
own [pom.xml](https://github.com/evanchooly/critter/blob/master/pom.xml).
