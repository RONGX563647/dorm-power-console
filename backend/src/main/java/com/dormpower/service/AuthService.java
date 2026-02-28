package com.dormpower.service;

import com.dormpower.model.UserAccount;
import com.dormpower.repository.UserAccountRepository;
import com.dormpower.util.EncryptionUtil;
import com.dormpower.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 */
@Service
public class AuthService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.email:admin@dorm.local}")
    private String adminEmail;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    /**
     * 用户登录
     * @param account 用户名或邮箱
     * @param password 密码
     * @return 登录结果
     */
    public Map<String, Object> login(String account, String password) {
        String normalizedAccount = account.trim();
        String normalizedAdminUsername = adminUsername.trim();
        String normalizedAdminEmail = adminEmail.trim().toLowerCase();

        if (!normalizedAdminUsername.equals(normalizedAccount) && 
            !normalizedAdminEmail.equals(normalizedAccount.toLowerCase())) {
            throw new RuntimeException("Invalid credentials");
        }

        UserAccount user = userAccountRepository.findById(normalizedAdminUsername).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (!EncryptionUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", Map.of(
            "username", user.getUsername(),
            "email", user.getEmail(),
            "role", user.getRole()
        ));
        return response;
    }

    /**
     * 确保默认管理员账户存在
     */
    public void ensureDefaultAdmin() {
        if (!userAccountRepository.existsById(adminUsername)) {
            UserAccount admin = new UserAccount();
            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail);
            admin.setPasswordHash(EncryptionUtil.hashPassword(adminPassword));
            admin.setRole("admin");
            admin.setCreatedAt(System.currentTimeMillis());
            admin.setUpdatedAt(System.currentTimeMillis());
            userAccountRepository.save(admin);
        }
    }

}
