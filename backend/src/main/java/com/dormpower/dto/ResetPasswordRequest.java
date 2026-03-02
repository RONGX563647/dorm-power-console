package com.dormpower.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 密码重置请求DTO
 */
@Schema(description = "密码重置请求")
public class ResetPasswordRequest {

    @NotBlank(message = "邮箱不能为空")
    @Schema(description = "邮箱", example = "user@example.com", required = true)
    private String email;

    @NotBlank(message = "重置码不能为空")
    @Schema(description = "重置码", example = "123456", required = true)
    private String resetCode;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "新密码长度至少为6位")
    @Schema(description = "新密码", example = "newpassword123", required = true)
    private String newPassword;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getResetCode() {
        return resetCode;
    }

    public void setResetCode(String resetCode) {
        this.resetCode = resetCode;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

}
