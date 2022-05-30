package dev.morphia.critter.test;

import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import org.bson.types.ObjectId;

import java.util.Objects;
import java.util.StringJoiner;

@Entity(cap = @CappedAt(count = 12))
@Indexes({@Index(fields = @Field("1")), @Index(fields = @Field("2")), @Index(fields = @Field("3"))})
public class Person extends AbstractPerson {
    @Id
    private ObjectId id;

    private String firstName;

    private String lastName;

    private String ssn;

    public Person() {
    }

    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(final ObjectId id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Person)) {
            return false;
        }
        Person person = (Person) o;
        return Objects.equals(id, person.id) && Objects.equals(firstName, person.firstName) && Objects.equals(lastName, person.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Person.class.getSimpleName() + "[", "]").add("age=" + getAge())
                                                                              .add("id=" + id)
                                                                              .add("firstName='" + firstName + "'")
                                                                              .add("lastName='" + lastName + "'")
                                                                              .toString();
    }
}
