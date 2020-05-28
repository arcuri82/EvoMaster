package com.foo.mongo.person;

public class Address {

    private String streetName;

    private int streetNumber;

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public void setStreetNumber(int streetNumber) {
        this.streetNumber = streetNumber;
    }

    public int getStreetNumber() {
        return streetNumber;
    }

    public String getStreetName() {
        return streetName;
    }

    public AddressDto toDto() {
        AddressDto dto = new AddressDto();
        dto.streetName = streetName;
        dto.streetNumber = streetNumber;
        return dto;
    }

    public static Address fromDto(AddressDto dto) {
        Address address = new Address();
        address.streetName = dto.streetName;
        address.streetNumber = dto.streetNumber;
        return address;
    }

}

