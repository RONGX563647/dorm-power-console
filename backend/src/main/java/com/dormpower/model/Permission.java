package com.dormpower.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 权限实体
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permission_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    private String id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String code;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    @JsonIgnoreProperties({"permissions", "hibernateLazyInitializer", "handler"})
    private Resource resource;

    @NotNull
    private String action;

    @NotNull
    private boolean enabled = true;

    @NotNull
    private long createdAt = System.currentTimeMillis() / 1000;

    @NotNull
    private long updatedAt = System.currentTimeMillis() / 1000;
}