package com.dormpower.service;

import com.dormpower.model.UserAccount;
import com.dormpower.repository.UserAccountRepository;
import com.dormpower.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务
 */
@Service
public class UserService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    /**
     * 获取所有用户
     * @return 用户列表
     */
    public List<UserAccount> getAllUsers() {
        return userAccountRepository.findAll();
    }

    /**
     * 根据用户名获取用户
     * @param username 用户名
     * @return 用户
     */
    public UserAccount getUserByUsername(String username) {
        return userAccountRepository.findById(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * 更新用户
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     * @return 更新后的用户
     */
    public UserAccount updateUser(String username, String email, String password) {
        UserAccount user = userAccountRepository.findById(username).orElseThrow(() -> new RuntimeException("User not found"));

        // 更新邮箱
        if (email != null && !email.isEmpty()) {
            user.setEmail(email);
        }

        // 更新密码
        if (password != null && !password.isEmpty()) {
            user.setPasswordHash(EncryptionUtil.hashPassword(password));
        }

        user.setUpdatedAt(System.currentTimeMillis());
        return userAccountRepository.save(user);
    }

    /**
     * 删除用户
     * @param username 用户名
     */
    public void deleteUser(String username) {
        UserAccount user = userAccountRepository.findById(username).orElseThrow(() -> new RuntimeException("User not found"));
        userAccountRepository.delete(user);
    }

    /**
     * 修改密码
     * @param username 用户名
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    public void changePassword(String username, String oldPassword, String newPassword) {
        UserAccount user = userAccountRepository.findById(username).orElseThrow(() -> new RuntimeException("User not found"));
        
        // 验证旧密码
        if (!EncryptionUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Old password is incorrect");
        }
        
        // 更新密码
        user.setPasswordHash(EncryptionUtil.hashPassword(newPassword));
        user.setUpdatedAt(System.currentTimeMillis());
        userAccountRepository.save(user);
    }

    /**
     * 更新个人资料
     * @param username 用户名
     * @param email 邮箱
     * @return 更新后的用户
     */
    public UserAccount updateProfile(String username, String email) {
        UserAccount user = userAccountRepository.findById(username).orElseThrow(() -> new RuntimeException("User not found"));
        
        // 检查邮箱是否已被其他用户使用
        if (userAccountRepository.findByEmail(email).isPresent()) {
            UserAccount existingUser = userAccountRepository.findByEmail(email).get();
            if (!existingUser.getUsername().equals(username)) {
                throw new RuntimeException("Email already in use");
            }
        }
        
        // 更新邮箱
        user.setEmail(email);
        user.setUpdatedAt(System.currentTimeMillis());
        return userAccountRepository.save(user);
    }

}
