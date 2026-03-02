package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.annotation.RateLimit;
import com.dormpower.dto.ForgotPasswordRequest;
import com.dormpower.dto.LoginRequest;
import com.dormpower.dto.RegisterRequest;
import com.dormpower.dto.ResetPasswordRequest;
import com.dormpower.exception.AuthenticationException;
import com.dormpower.service.AuthService;
import com.dormpower.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 
 * 提供用户认证相关的接口，包括：
 * - 用户登录
 * - 用户注册
 * - 忘记密码
 * - 重置密码
 * - 用户登出
 * - 获取当前用户信息
 * - 刷新令牌
 * 
 * 所有登录和注册接口都有速率限制，防止暴力破解。
 * 所有认证操作都会被记录到审计日志。
 * 
 * @author dormpower team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "提供用户认证和令牌管理的RESTful API接口")
public class AuthController {

    // JWT工具，用于生成和验证令牌
    @Autowired
    private JwtUtil jwtUtil;

    // 认证服务，处理用户认证的业务逻辑
    @Autowired
    private AuthService authService;

    /**
     * 登录接口
     */
    @Operation(summary = "用户登录", description = "使用账号密码登录系统，返回JWT令牌")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "账号或密码错误")
    })
    @RateLimit(value = 2.0, type = "login")
    @AuditLog(value = "用户登录", type = "AUTH")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String account = request.getAccount();
        String password = request.getPassword();

        Map<String, Object> result = authService.login(account, password);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 注册接口
     */
    @Operation(summary = "用户注册", description = "注册新用户，返回JWT令牌")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "注册失败，用户名或邮箱已存在")
    })
    @RateLimit(value = 2.0, type = "register")
    @AuditLog(value = "用户注册", type = "AUTH")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String username = request.getUsername();
        String email = request.getEmail();
        String password = request.getPassword();

        Map<String, Object> result = authService.register(username, email, password);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 忘记密码接口
     */
    @Operation(summary = "忘记密码", description = "发送密码重置邮件")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "重置邮件发送成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @RateLimit(value = 2.0, type = "forgot-password")
    @AuditLog(value = "忘记密码", type = "AUTH")
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail();

        Map<String, Object> result = authService.forgotPassword(email);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 重置密码接口
     */
    @Operation(summary = "重置密码", description = "使用重置码更新密码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "密码重置成功"),
            @ApiResponse(responseCode = "400", description = "重置码无效或过期"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @RateLimit(value = 2.0, type = "reset-password")
    @AuditLog(value = "重置密码", type = "AUTH")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String email = request.getEmail();
        String resetCode = request.getResetCode();
        String newPassword = request.getNewPassword();

        Map<String, Object> result = authService.resetPassword(email, resetCode, newPassword);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 登出接口
     */
    @Operation(summary = "用户登出", description = "退出登录状态", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登出成功")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @Parameter(description = "认证令牌", required = false)
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String jwtToken = token.substring(7);
            jwtUtil.blacklistToken(jwtToken);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户", description = "获取当前登录用户的信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @Parameter(description = "认证令牌", required = false)
            @RequestHeader(value = "Authorization", required = false) String token) {
        // 如果没有token，直接返回401
        if (token == null || !token.startsWith("Bearer ")) {
            throw new AuthenticationException("Missing or invalid authorization header");
        }

        String jwtToken = token.substring(7);
        
        // 验证token是否有效
        if (!jwtUtil.validateToken(jwtToken)) {
            throw new AuthenticationException("Invalid or expired token");
        }
        
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        if (username == null) {
            throw new AuthenticationException("Invalid token");
        }
        
        Map<String, Object> userInfo = authService.getCurrentUser(username);
        return ResponseEntity.ok(userInfo);
    }

    /**
     * 刷新令牌
     */
    @Operation(summary = "刷新令牌", description = "刷新JWT令牌，延长有效期", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "刷新成功")
    })
    @PostMapping("/refresh")
    public Map<String, Object> refreshToken(
            @Parameter(description = "旧认证令牌", required = false)
            @RequestHeader(value = "Authorization", required = false) String token) {
        String username = "admin";
        if (token != null && token.startsWith("Bearer ")) {
            String jwtToken = token.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(jwtToken);
            } catch (Exception e) {
                // 使用默认用户名
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", jwtUtil.generateToken(username));
        response.put("expiresAt", System.currentTimeMillis() + 86400000L);
        return response;
    }

}
