package com.foo.mongo.person;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface PersonRepository extends PagingAndSortingRepository<Person, Long> {

    List<Person> findByLastName(String lastName);

    Page<Person> findByFirstName(String firstName, Pageable pageable);

    Person findByAddress(Address address);

    List<Person> findByAgeGreaterThan(int age);

    List<Person> findByAgeLessThan(int age);

    List<Person> findByAgeBetween(int from, int to);

    List<Person> findByAddressNotNull();

    List<Person> findByFirstNameNull();

    List<Person> findByFirstNameLike(String name);

    List<Person> findByFirstNameRegex(String name);

    List<Person> findByAge(int age);
}
