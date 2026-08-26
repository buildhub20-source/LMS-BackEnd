package com.lms.common.dto.response;

import lombok.Data;

@Data
public class EmergencyContactResponse {

    private String name;
    private String relation;
    private String phone;
    private String email;
}
