package com.dormpower.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * 资源实体
 */
@Entity
@Table(name = "resources", indexes = {
    @Index(name = "idx_resource_code", columnList = "code", unique = true)
})
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
    private Long parentId;
    
    @NotNull
    private int sortOrder;
    
    @NotNull
    private boolean enabled;
    
    @NotNull
    private long createdAt;
    
    @NotNull
    private long updatedAt;
    
    @OneToMany(mappedBy = "resource", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"resource", "hibernateLazyInitializer", "handler"})
    private Set<Permission> permissions;
    
    public Resource() {
        this.enabled = true;
        this.sortOrder = 0;
        this.parentId = 0L;
        this.createdAt = System.currentTimeMillis() / 1000;
        this.updatedAt = System.currentTimeMillis() / 1000;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public int getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
    
    public Set<Permission> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
