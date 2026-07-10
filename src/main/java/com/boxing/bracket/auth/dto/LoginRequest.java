package com.boxing.bracket.auth.dto;

import javax.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "loginId is required")
    private String loginId;

    @NotBlank(message = "password is required")
    private String password;

    protected LoginRequest() {
    }

    public LoginRequest(String loginId, String password) {
        this.loginId = loginId;
        this.password = password;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }
}
