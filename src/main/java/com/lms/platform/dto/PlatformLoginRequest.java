package com.lms.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PlatformLoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
