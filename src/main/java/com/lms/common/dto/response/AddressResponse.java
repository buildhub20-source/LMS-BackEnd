package com.lms.common.dto.response;

import lombok.Data;

@Data
public class AddressResponse {

    private String line1;
    private String line2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
}
