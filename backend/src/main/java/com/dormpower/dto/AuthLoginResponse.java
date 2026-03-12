package com.dormpower.dto;

import java.util.Map;

/**
 * 登录响应DTO
 */
public record AuthLoginResponse(String token, Map<String, Object> user) {}