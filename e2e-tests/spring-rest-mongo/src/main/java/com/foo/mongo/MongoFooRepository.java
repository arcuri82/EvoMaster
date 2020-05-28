package com.foo.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MongoFooRepository extends MongoRepository<MongoFooEntity, String> {

    MongoFooEntity findByFirstName(String firstName);

    List<MongoFooEntity> findByLastName(String lastName);

}