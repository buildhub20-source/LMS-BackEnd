package com.lms.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 100)
    private String fullName;

    @Size(max = 100)
    private String name;

    @Size(max = 20)
    @Pattern(regexp = "^$|^[+0-9 ()-]{6,20}$", message = "Phone number format is not valid")
    private String phone;

    @Size(max = 100)
    private String jobTitle;

    @Size(max = 1000)
    private String bio;

    @Size(max = 500)
    private String profileImageUrl;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String fullName, String name, String phone, String jobTitle, String bio, String profileImageUrl) {
        this.fullName = fullName;
        this.name = name;
        this.phone = phone;
        this.jobTitle = jobTitle;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
    }

    public String getFullName() {
        return fullName != null ? fullName : name;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getName() {
        return name != null ? name : fullName;
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

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
