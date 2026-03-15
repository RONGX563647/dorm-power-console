package com.dormpower.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * IP访问控制实体
 */
@Entity
@Table(name = "ip_access_control", indexes = {
    @Index(name = "idx_ip_address", columnList = "ipAddress", unique = true),
    @Index(name = "idx_ip_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
public class IpAccessControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String ipAddress;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 200)
    private String description;

    private boolean enabled = true;

    private long expiresAt;

    private String createdBy;

    private long createdAt = System.currentTimeMillis() / 1000;

    private long updatedAt = System.currentTimeMillis() / 1000;

    /**
     * 便捷构造函数
     */
    public IpAccessControl(String ipAddress, String type) {
        this.ipAddress = ipAddress;
        this.type = type;
    }
}