package com.dormpower.dto;

import java.util.Map;

/**
 * 登录响应DTO
 */
public class AuthLoginResponse {

    private String token;
    private Map<String, Object> user;

    // Getters and setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Map<String, Object> getUser() {
        return user;
    }

    public void setUser(Map<String, Object> user) {
        this.user = user;
    }

}