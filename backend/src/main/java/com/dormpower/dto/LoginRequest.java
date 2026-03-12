package com.dormpower.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 */
@Schema(description = "登录请求")
public record LoginRequest(
    @NotBlank(message = "账号不能为空")
    @Schema(description = "账号", example = "admin", required = true)
    String account,

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "admin123", required = true)
    String password
) {
    public String getUsername() {
        return account;
    }
}