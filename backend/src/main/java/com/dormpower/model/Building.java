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
 * 楼栋模型
 */
@Entity
@Table(name = "buildings")
@Getter
@Setter
@NoArgsConstructor
public class Building {

    @Id
    private String id;

    /** 楼栋名称 */
    @NotNull
    private String name;

    /** 楼栋编号 */
    @NotNull
    private String code;

    /** 描述 */
    private String description;

    /** 总楼层数 */
    private int totalFloors;

    /** 地址 */
    private String address;

    /** 管理员 */
    private String manager;

    /** 联系电话 */
    private String contact;

    /** 是否启用 */
    @NotNull
    private boolean enabled;

    @NotNull
    private long createdAt;

    private long updatedAt;
}