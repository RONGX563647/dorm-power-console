package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.annotation.RateLimit;
import com.dormpower.dto.RegisterRequest;
import com.dormpower.model.UserAccount;
import com.dormpower.service.AuthService;
import com.dormpower.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    /**
     * 获取用户列表
     */
    @Operation(summary = "获取用户列表", description = "获取所有用户的列表", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping
    public ResponseEntity<?> getUsers() {
        try {
            List<UserAccount> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "INTERNAL_ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 创建用户
     */
    @Operation(summary = "创建用户", description = "创建新用户", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "创建失败")
    })
    @RateLimit(value = 2.0, type = "create-user")
    @AuditLog(value = "创建用户", type = "USER")
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterRequest request) {
        try {
            String username = request.getUsername();
            String email = request.getEmail();
            String password = request.getPassword();

            Map<String, Object> result = authService.register(username, email, password);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "BAD_REQUEST");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取用户详情
     */
    @Operation(summary = "获取用户详情", description = "根据用户名获取用户详情", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/{username}")
    public ResponseEntity<?> getUser(@Parameter(description = "用户名", required = true) @PathVariable String username) {
        try {
            UserAccount user = userService.getUserByUsername(username);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "NOT_FOUND");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * 更新用户
     */
    @Operation(summary = "更新用户", description = "更新用户信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "更新失败"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @AuditLog(value = "更新用户", type = "USER")
    @PutMapping("/{username}")
    public ResponseEntity<?> updateUser(@Parameter(description = "用户名", required = true) @PathVariable String username, @Valid @RequestBody RegisterRequest request) {
        try {
            UserAccount user = userService.updateUser(username, request.getEmail(), request.getPassword());
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "BAD_REQUEST");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "删除用户", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @AuditLog(value = "删除用户", type = "USER")
    @DeleteMapping("/{username}")
    public ResponseEntity<?> deleteUser(@Parameter(description = "用户名", required = true) @PathVariable String username) {
        try {
            userService.deleteUser(username);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "NOT_FOUND");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "用户自主修改密码", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "400", description = "修改失败")
    })
    @AuditLog(value = "修改密码", type = "USER")
    @PostMapping("/{username}/password")
    public ResponseEntity<?> changePassword(
            @Parameter(description = "用户名", required = true) @PathVariable String username,
            @Parameter(description = "密码修改请求", required = true)
            @RequestBody Map<String, String> request) {
        try {
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            
            if (oldPassword == null || newPassword == null) {
                throw new RuntimeException("Old password and new password are required");
            }
            
            userService.changePassword(username, oldPassword, newPassword);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "BAD_REQUEST");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 更新用户个人资料
     */
    @Operation(summary = "更新个人资料", description = "修改用户个人信息", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @AuditLog(value = "更新个人资料", type = "USER")
    @PatchMapping("/{username}/profile")
    public ResponseEntity<?> updateProfile(
            @Parameter(description = "用户名", required = true) @PathVariable String username,
            @Parameter(description = "个人资料更新请求", required = true)
            @RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            if (email == null) {
                throw new RuntimeException("Email is required");
            }
            
            UserAccount user = userService.updateProfile(username, email);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "BAD_REQUEST");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

}
