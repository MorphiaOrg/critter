critter
=======

Critter will look at your morphia object model and generate type safe criteria builders for
each model object.  To use it, you typically won't need to do anything.  It's standard
annotation processor so the compiler should find it automatically.  If have disabled annotation
processing for whatever reason, you can add the following to your pom.xml to enabled processing:

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