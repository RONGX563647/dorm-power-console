package com.dormpower.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 */
@Schema(description = "登录请求")
public class LoginRequest {

    @Schema(description = "账号", example = "admin")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "admin123", required = true)
    private String password;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return account;
    }

}
