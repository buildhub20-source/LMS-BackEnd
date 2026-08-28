package com.lms.student.dto.request;

import com.lms.common.domain.Address;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequest {

    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    private String line1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String line2;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;
}
