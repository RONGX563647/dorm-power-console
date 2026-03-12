package com.dormpower.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据字典实体
 */
@Entity
@Table(name = "data_dict", indexes = {
    @Index(name = "idx_dict_type", columnList = "dictType"),
    @Index(name = "idx_dict_code", columnList = "dictCode", unique = true)
})
public class DataDict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String dictType;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String dictCode;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String dictLabel;

    @Column(length = 200)
    private String dictValue;

    @Column(length = 50)
    private String parentCode;

    private Integer sort = 0;

    @Column(length = 500)
    private String description;

    private boolean enabled = true;

    private boolean isDefault = false;

    private String cssClass;

    private String listClass;

    private boolean isSystem = false;

    private long createdAt;

    private long updatedAt;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("parent")
    private List<DataDict> children = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "parentCode", referencedColumnName = "dictCode", insertable = false, updatable = false)
    @JsonIgnoreProperties("children")
    private DataDict parent;

    public DataDict() {
        long now = System.currentTimeMillis() / 1000;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public DataDict(String dictType, String dictCode, String dictLabel) {
        this();
        this.dictType = dictType;
        this.dictCode = dictCode;
        this.dictLabel = dictLabel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
    }

    public String getDictLabel() {
        return dictLabel;
    }

    public void setDictLabel(String dictLabel) {
        this.dictLabel = dictLabel;
    }

    public String getDictValue() {
        return dictValue;
    }

    public void setDictValue(String dictValue) {
        this.dictValue = dictValue;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getListClass() {
        return listClass;
    }

    public void setListClass(String listClass) {
        this.listClass = listClass;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
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

    public List<DataDict> getChildren() {
        return children;
    }

    public void setChildren(List<DataDict> children) {
        this.children = children;
    }

    public DataDict getParent() {
        return parent;
    }

    public void setParent(DataDict parent) {
        this.parent = parent;
    }
}
