package com.dormpower.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 资源实体
 */
@Entity
@Table(name = "resources", indexes = {
    @Index(name = "idx_resource_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class Resource {

    @Id
    private String id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String code;

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String type;

    private String url;

    private String method;

    @NotNull
    private Long parentId = 0L;

    @NotNull
    private int sortOrder = 0;

    @NotNull
    private boolean enabled = true;

    @NotNull
    private long createdAt = System.currentTimeMillis() / 1000;

    @NotNull
    private long updatedAt = System.currentTimeMillis() / 1000;

    @OneToMany(mappedBy = "resource", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"resource", "hibernateLazyInitializer", "handler"})
    private Set<Permission> permissions;
}