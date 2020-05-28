package com.foo.mongo;

import org.springframework.data.annotation.Id;

/**
 * An Entity that is used by this Mongo application
 */
public class MongoFooEntity {

    @Id
    public String id;
    public String firstName;
    public String lastName;

    public MongoFooEntity() {
    }

    public MongoFooEntity(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return String.format(
                "Customer[firstName='%s', lastName='%s']",
                firstName, lastName);
    }

}
