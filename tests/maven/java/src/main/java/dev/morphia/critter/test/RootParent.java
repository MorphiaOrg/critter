package dev.morphia.critter.test;

import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

public class RootParent implements TestEntity {
    @Id
    ObjectId id;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }
}