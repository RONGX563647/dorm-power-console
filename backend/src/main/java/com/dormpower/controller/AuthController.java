package com.dormpower.controller;

import com.dormpower.dto.LoginRequest;
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
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

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
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            String account = request.getAccount();
            String password = request.getPassword();

            Map<String, Object> result = authService.login(account, password);
            
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "UNAUTHORIZED");
            error.put("message", "invalid account or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * 登出接口
     */
    @Operation(summary = "用户登出", description = "退出登录状态", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登出成功")
    })
    @PostMapping("/logout")
    public Map<String, Object> logout(
            @Parameter(description = "认证令牌", required = false)
            @RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户", description = "获取当前登录用户的信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(
            @Parameter(description = "认证令牌", required = false)
            @RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", "admin");
        user.put("role", "admin");
        user.put("email", "admin@dorm.local");
        user.put("createdAt", System.currentTimeMillis() - 86400000L * 30);
        return user;
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
