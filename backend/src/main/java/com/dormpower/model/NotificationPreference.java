package com.dormpower.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户通知偏好设置实体
 * 存储用户对各类通知的偏好设置
 */
@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_notification_pref_user", columnList = "username", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String username;

    @NotNull
    private boolean emailEnabled = true;

    @NotNull
    private boolean systemEnabled = true;

    @NotNull
    private boolean alertEnabled = true;

    @NotNull
    private boolean billingEnabled = true;

    @NotNull
    private boolean maintenanceEnabled = true;

    @NotNull
    private boolean quietHoursEnabled = false;

    private String quietHoursStart = "22:00";

    private String quietHoursEnd = "08:00";

    @NotNull
    private String alertLevel = "warning";

    @NotNull
    private long createdAt = System.currentTimeMillis() / 1000;

    @NotNull
    private long updatedAt = System.currentTimeMillis() / 1000;

    /**
     * 便捷构造函数
     */
    public NotificationPreference(String username) {
        this.username = username;
    }
}