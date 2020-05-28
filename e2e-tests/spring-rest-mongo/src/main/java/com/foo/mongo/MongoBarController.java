package com.foo.mongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/mongobar")
public class MongoBarController {

    @Autowired
    private MongoFooRepository repository;

    @RequestMapping(
            method = RequestMethod.POST
    )
    public ResponseEntity post() {
        MongoFooEntity entity = new MongoFooEntity("Alice", "Smith");
        repository.save(entity);
        return ResponseEntity.ok().build();
    }


    @RequestMapping(
            method = RequestMethod.GET
    )
    public ResponseEntity get() {
        if (repository.findAll().isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok().build();
        }
    }

    @RequestMapping(
            method = RequestMethod.DELETE
    )
    public ResponseEntity delete() {
        repository.deleteAll();
        return ResponseEntity.ok().build();
    }

}

