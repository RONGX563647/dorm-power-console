package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户账户模型
 */
@Entity
@Table(name = "user_account", indexes = {
        @Index(name = "idx_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
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
    private boolean enabled = true;
}