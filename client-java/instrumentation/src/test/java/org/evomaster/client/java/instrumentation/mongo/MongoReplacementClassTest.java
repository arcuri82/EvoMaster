package org.evomaster.client.java.instrumentation.mongo;

import com.foo.somedifferentpackage.examples.methodreplacement.mongo.MockedFindIterable;
import com.foo.somedifferentpackage.examples.methodreplacement.mongo.MockedMongoCollection;
import com.mongodb.MongoNamespace;
import com.mongodb.client.FindIterable;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class MongoReplacementClassTest {

    static class Person {
        String address;
        String name;

        public Person(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }

    @Test
    public void testFind() {
        MockedMongoCollection mockedMongoCollection = new MockedMongoCollection();
        mockedMongoCollection.setNamespace(new MongoNamespace("mydb.mycollection"));
        MockedFindIterable mockedFindIterable = new MockedFindIterable();
        Person person0 = new Person("John Doe", "Lorem ipsum 21");
        Person person1 = new Person("Jane Doe", "Lorem ipsum 21");

        mockedFindIterable.addElement(person0);
        mockedFindIterable.addElement(person1);

        mockedMongoCollection.setFindIterable(mockedFindIterable);

        FindIterable findIterable = MongoReplacementClass.find(mockedMongoCollection, new BsonDocument(), Person.class);
        Iterator<Person> it = findIterable.iterator();
        assertTrue(it.hasNext());
        assertEquals(person0, it.next());
        assertTrue(it.hasNext());
        assertEquals(person1, it.next());
        assertFalse(it.hasNext());
    }
}
