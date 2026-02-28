package com.dormpower.controller;

import com.dormpower.dto.LoginRequest;
import com.dormpower.exception.AuthenticationException;
import com.dormpower.service.AuthService;
import com.dormpower.util.JwtUtil;
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
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    /**
     * 登录接口
     * @param request 登录请求
     * @return 登录响应
     */
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
     * @param token 认证令牌
     * @return 登出响应
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 获取当前用户信息
     * @param token 认证令牌
     * @return 用户信息
     */
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", "admin");
        user.put("role", "admin");
        user.put("email", "admin@dorm.local");
        user.put("createdAt", System.currentTimeMillis() - 86400000L * 30);
        return user;
    }

    /**
     * 刷新令牌
     * @param token 旧令牌
     * @return 新令牌
     */
    @PostMapping("/refresh")
    public Map<String, Object> refreshToken(@RequestHeader(value = "Authorization", required = false) String token) {
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
