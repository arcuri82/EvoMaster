package com.foo.mongo.person;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class PersonRest {

    @Autowired
    private PersonRepository repository;

    @RequestMapping(path = "/api/mongoperson/add",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            method = {RequestMethod.POST})
    public void add(@RequestBody PersonDto dto) {
        Person person = Person.fromDto(dto);
        repository.save(person);
    }

    @RequestMapping(path = "/api/mongoperson/findByLastName/{lastName}",
            method = {RequestMethod.GET})
    public List<PersonDto> findByLastName(@PathVariable String lastName) {
        List<Person> persons = repository.findByLastName(lastName);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByAgeGreaterThan/{age}",
            method = {RequestMethod.GET})
    public List<PersonDto> findByAgeGreaterThan(@PathVariable int age) {
        List<Person> persons = repository.findByAgeGreaterThan(age);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findAll",
            method = {RequestMethod.GET})
    public List<PersonDto> findAll() {
        List<Person> persons = new ArrayList<>();
        repository.findAll().forEach(persons::add);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByAgeLessThan/{age}",
            method = {RequestMethod.GET})
    public List<PersonDto> findByAgeLessThan(@PathVariable int age) {
        List<Person> persons = repository.findByAgeLessThan(age);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByAge/{age}",
            method = {RequestMethod.GET})
    public List<PersonDto> findByAge(@PathVariable int age) {
        List<Person> persons = repository.findByAge(age);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }


    @RequestMapping(path = "/api/mongoperson/findByAgeBetween/{from}/{to}",
            method = {RequestMethod.GET})
    public List<PersonDto> findByAgeBetween(@PathVariable int from, @PathVariable int to) {
        List<Person> persons = repository.findByAgeBetween(from, to);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByAddressNotNull",
            method = {RequestMethod.GET})
    public List<PersonDto> findByAddressNotNull() {
        List<Person> persons = repository.findByAddressNotNull();
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByFirstNameNull",
            method = {RequestMethod.GET})
    public List<PersonDto> findByFirstNameNull() {
        List<Person> persons = repository.findByFirstNameNull();
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByFirstNameLike/{name}",
            method = {RequestMethod.GET})
    public List<PersonDto> findByFirstNameLike(@PathVariable String name) {
        List<Person> persons = repository.findByFirstNameLike(name);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/findByFirstNameRegex/{name}",
            method = {RequestMethod.GET})
    public List<PersonDto> findByFirstNameRegex(@PathVariable String name) {
        List<Person> persons = repository.findByFirstNameRegex(name);
        List<PersonDto> personDtos = persons.stream().map(Person::toDto).collect(Collectors.toList());
        return personDtos;
    }

    @RequestMapping(path = "/api/mongoperson/addJoeBlack",
            method = {RequestMethod.POST})
    public void addJoeBlack() {
        Person person = new Person();
        person.setFirstName("Joe");
        person.setLastName("Black");
        person.setAge(33);
        repository.save(person);
    }


}
