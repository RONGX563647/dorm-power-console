package com.dormpower.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 数据字典实体
 */
@Entity
@Table(name = "data_dict", indexes = {
    @Index(name = "idx_dict_type", columnList = "dictType"),
    @Index(name = "idx_dict_code", columnList = "dictCode", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
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

    private long createdAt = System.currentTimeMillis() / 1000;

    private long updatedAt = System.currentTimeMillis() / 1000;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("parent")
    private List<DataDict> children = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "parentCode", referencedColumnName = "dictCode", insertable = false, updatable = false)
    @JsonIgnoreProperties("children")
    private DataDict parent;

    /**
     * 便捷构造函数
     */
    public DataDict(String dictType, String dictCode, String dictLabel) {
        this.dictType = dictType;
        this.dictCode = dictCode;
        this.dictLabel = dictLabel;
    }
}