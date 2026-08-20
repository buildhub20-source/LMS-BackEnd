package com.lms.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 20)
    @Pattern(regexp = "^$|^[+0-9 ()-]{6,20}$", message = "Phone number format is not valid")
    private String phone;

    @Size(max = 500)
    private String profileImageUrl;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String name, String phone, String profileImageUrl) {
        this.name = name;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
