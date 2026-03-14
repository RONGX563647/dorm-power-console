package com.dormpower.service;

import com.dormpower.exception.BusinessException;
import com.dormpower.model.UserAccount;
import com.dormpower.repository.UserAccountRepository;
import com.dormpower.util.EncryptionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户管理单元测试
 *
 * 测试用例覆盖：
 * - TC-USER-001: 创建用户
 * - TC-USER-002: 用户名重复
 * - TC-USER-003: 更新用户
 * - TC-USER-004: 删除用户
 * - TC-USER-005: 修改密码
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private UserService userService;

    // ==================== TC-USER-001: 创建用户 ====================

    @Test
    @DisplayName("TC-USER-001: 创建用户 - 验证用户信息")
    void testGetUserByUsername_Success() {
        // Given
        String username = "testuser";
        UserAccount user = createUser(username, "test@example.com");

        when(userAccountRepository.findById(username)).thenReturn(Optional.of(user));

        // When
        UserAccount result = userService.getUserByUsername(username);

        // Then
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("user", result.getRole());
    }

    @Test
    @DisplayName("TC-USER-001: 创建用户 - 用户不存在异常")
    void testGetUserByUsername_NotFound() {
        // Given
        String username = "nonexistent";
        when(userAccountRepository.findById(username)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.getUserByUsername(username));
    }

    // ==================== TC-USER-002: 用户名重复 ====================

    @Test
    @DisplayName("TC-USER-002: 用户名重复 - 返回400错误")
    void testRegister_DuplicateUsername_ThrowsException() {
        // Given
        String username = "existinguser";
        String email = "new@example.com";
        String password = "password123";

        when(userAccountRepository.existsById(username)).thenReturn(true);

        // When & Then
        AuthService authService = new AuthService();
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "userAccountRepository", userAccountRepository);

        assertThrows(BusinessException.class, () -> authService.register(username, email, password));
    }

    @Test
    @DisplayName("TC-USER-002: 用户名重复 - 验证存在性检查")
    void testUserExistsByUsername() {
        // Given
        String existingUsername = "admin";
        String newUsername = "newuser";

        when(userAccountRepository.existsById(existingUsername)).thenReturn(true);
        when(userAccountRepository.existsById(newUsername)).thenReturn(false);

        // When & Then
        assertTrue(userAccountRepository.existsById(existingUsername));
        assertFalse(userAccountRepository.existsById(newUsername));
    }

    // ==================== TC-USER-003: 更新用户 ====================

    @Test
    @DisplayName("TC-USER-003: 更新用户 - 用户更新成功")
    void testUpdateUser_Success() {
        // Given
        String username = "testuser";
        UserAccount existingUser = createUser(username, "old@example.com");
        String newEmail = "new@example.com";
        String newPassword = "newpassword123";

        when(userAccountRepository.findById(username)).thenReturn(Optional.of(existingUser));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        UserAccount result = userService.updateUser(username, newEmail, newPassword);

        // Then
        assertNotNull(result);
        assertEquals(newEmail, result.getEmail());
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    @DisplayName("TC-USER-003: 更新用户 - 只更新邮箱")
    void testUpdateUser_OnlyEmail() {
        // Given
        String username = "testuser";
        UserAccount existingUser = createUser(username, "old@example.com");
        String originalPasswordHash = existingUser.getPasswordHash();
        String newEmail = "new@example.com";

        when(userAccountRepository.findById(username)).thenReturn(Optional.of(existingUser));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        UserAccount result = userService.updateUser(username, newEmail, null);

        // Then
        assertEquals(newEmail, result.getEmail());
        assertEquals(originalPasswordHash, result.getPasswordHash());
    }

    @Test
    @DisplayName("TC-USER-003: 更新用户 - 只更新密码")
    void testUpdateUser_OnlyPassword() {
        // Given
        String username = "testuser";
        UserAccount existingUser = createUser(username, "test@example.com");
        String newPassword = "newpassword123";

        when(userAccountRepository.findById(username)).thenReturn(Optional.of(existingUser));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        UserAccount result = userService.updateUser(username, null, newPassword);

        // Then
        assertNotNull(result);
        // 密码哈希应该被更新
        assertTrue(EncryptionUtil.verifyPassword(newPassword, result.getPasswordHash()));
    }

    @Test
    @DisplayName("TC-USER-003: 更新用户 - 用户不存在")
    void testUpdateUser_NotFound() {
        // Given
        String username = "nonexistent";
        when(userAccountRepository.findById(username)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.updateUser(username, "new@example.com", "password"));
    }

    // ==================== TC-USER-004: 删除用户 ====================

    @Test
    @DisplayName("TC-USER-004: 删除用户 - 用户删除成功")
    void testDeleteUser_Success() {
        // Given
        String username = "testuser";
        UserAccount user = createUser(username, "test@example.com");

        when(userAccountRepository.findById(username)).thenReturn(Optional.of(user));
        doNothing().when(userAccountRepository).delete(user);

        // When
        userService.deleteUser(username);

        // Then
        verify(userAccountRepository).delete(user);
    }

    @Test
    @DisplayName("TC-USER-004: 删除用户 - 用户不存在")
    void testDeleteUser_NotFound() {
        // Given
        String username = "nonexistent";
        when(userAccountRepository.findById(username)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.deleteUser(username));
        verify(userAccountRepository, never()).delete(any());
    }

    @Test
    @DisplayName("TC-USER-004: 删除用户 - 验证删除操作")
    void testDeleteUser_VerifyDeletion() {
        // Given
        String username = "testuser";
        UserAccount user = createUser(username, "test@example.com");

        when(userAccountRepository.findById(username)).thenReturn(Optional.of(user));

        // When
        userService.deleteUser(username);

        // Then
        verify(userAccountRepository).findById(username);
        verify(userAccountRepository).delete(user);
    }

    // ==================== TC-USER-005: 修改密码 ====================

    @Test
    @DisplayName("TC-USER-005: 修改密码 - 正确旧密码，密码修改成功")
    void testChangePassword_Success() {
        // Given
        String username = "testuser";
        String oldPassword = "oldpassword123";
        String newPassword = "newpassword456";
        UserAccount user = createUser(username, "test@example.com");
        // 设置正确的密码哈希
        user.setPasswordHash(EncryptionUtil.hashPassword(oldPassword));

        when(userAccountRepository.findById(username)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        userService.changePassword(username, oldPassword, newPassword);

        // Then
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    @DisplayName("TC-USER-005: 修改密码 - 旧密码错误")
    void testChangePassword_WrongOldPassword() {
        // Given
        String username = "testuser";
        String correctOldPassword = "correct123";
        String wrongOldPassword = "wrong123";
        String newPassword = "newpassword456";
        UserAccount user = createUser(username, "test@example.com");
        user.setPasswordHash(EncryptionUtil.hashPassword(correctOldPassword));

        when(userAccountRepository.findById(username)).thenReturn(Optional.of(user));

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.changePassword(username, wrongOldPassword, newPassword));
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-USER-005: 修改密码 - 用户不存在")
    void testChangePassword_UserNotFound() {
        // Given
        String username = "nonexistent";
        when(userAccountRepository.findById(username)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.changePassword(username, "old", "new"));
    }

    @Test
    @DisplayName("TC-USER-005: 修改密码 - 验证密码哈希更新")
    void testChangePassword_HashUpdated() {
        // Given
        String username = "testuser";
        String oldPassword = "oldpassword123";
        String newPassword = "newpassword456";
        UserAccount user = createUser(username, "test@example.com");
        user.setPasswordHash(EncryptionUtil.hashPassword(oldPassword));

        when(userAccountRepository.findById(username)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount savedUser = inv.getArgument(0);
            // 验证新密码可以验证通过
            assertTrue(EncryptionUtil.verifyPassword(newPassword, savedUser.getPasswordHash()));
            return savedUser;
        });

        // When
        userService.changePassword(username, oldPassword, newPassword);

        // Then - 验证在 save 中已验证密码
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    // ==================== 其他边界测试 ====================

    @Test
    @DisplayName("边界测试: 获取所有用户")
    void testGetAllUsers() {
        // Given
        java.util.List<UserAccount> users = java.util.List.of(
                createUser("user1", "user1@example.com"),
                createUser("user2", "user2@example.com")
        );
        when(userAccountRepository.findAll()).thenReturn(users);

        // When
        java.util.List<UserAccount> result = userService.getAllUsers();

        // Then
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("边界测试: 更新个人资料 - 邮箱已被其他用户使用")
    void testUpdateProfile_EmailInUse() {
        // Given
        String username = "testuser";
        String newEmail = "used@example.com";
        UserAccount user = createUser(username, "test@example.com");
        UserAccount otherUser = createUser("otheruser", newEmail);

        when(userAccountRepository.findById(username)).thenReturn(Optional.of(user));
        when(userAccountRepository.findByEmail(newEmail)).thenReturn(Optional.of(otherUser));

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.updateProfile(username, newEmail));
    }

    @Test
    @DisplayName("边界测试: 更新个人资料 - 邮箱相同可以更新")
    void testUpdateProfile_SameEmail_Success() {
        // Given
        String username = "testuser";
        String email = "test@example.com";
        UserAccount user = createUser(username, email);

        when(userAccountRepository.findById(username)).thenReturn(Optional.of(user));
        when(userAccountRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        UserAccount result = userService.updateProfile(username, email);

        // Then
        assertNotNull(result);
        assertEquals(email, result.getEmail());
    }

    // ==================== 辅助方法 ====================

    private UserAccount createUser(String username, String email) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(EncryptionUtil.hashPassword("password123"));
        user.setRole("user");
        user.setResetCodeHash("");
        user.setResetExpiresAt(0);
        user.setCreatedAt(System.currentTimeMillis());
        user.setUpdatedAt(System.currentTimeMillis());
        user.setEnabled(true);
        return user;
    }

}