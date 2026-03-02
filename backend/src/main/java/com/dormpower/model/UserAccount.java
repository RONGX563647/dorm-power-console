package com.dormpower.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * 用户账户模型
 */
@Entity
@Table(name = "user_account", indexes = {
        @Index(name = "idx_email", columnList = "email", unique = true)
})
public class UserAccount {

    @Id
    private String username;

    @NotNull
    private String email;

    @NotNull
    private String passwordHash;

    @NotNull
    private String role;

    @NotNull
    private String resetCodeHash;

    @NotNull
    private long resetExpiresAt;

    @NotNull
    private long createdAt;

    @NotNull
    private long updatedAt;
    
    @NotNull
    private boolean enabled;

    public UserAccount() {
        this.enabled = true;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getResetCodeHash() {
        return resetCodeHash;
    }

    public void setResetCodeHash(String resetCodeHash) {
        this.resetCodeHash = resetCodeHash;
    }

    public long getResetExpiresAt() {
        return resetExpiresAt;
    }

    public void setResetExpiresAt(long resetExpiresAt) {
        this.resetExpiresAt = resetExpiresAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}