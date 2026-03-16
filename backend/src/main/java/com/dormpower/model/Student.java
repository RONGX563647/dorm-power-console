package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 学生/住户信息模型
 * 管理宿舍住户的基本信息和入住状态
 */
@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
public class Student {

    @Id
    private String id;

    /** 学号/工号 */
    @NotNull
    @Pattern(regexp = "^\\d{10,20}$", message = "学号必须是10-20位数字")
    private String studentNumber;

    /** 姓名 */
    @NotNull
    private String name;

    /** 性别：MALE(男)、FEMALE(女) */
    @NotNull
    private String gender;

    /** 院系/部门 */
    @NotNull
    private String department;

    /** 专业 */
    private String major;

    /** 班级 */
    private String className;

    /** 联系电话 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 身份证号（加密存储） */
    private String idCard;

    /** 紧急联系人 */
    private String emergencyContact;

    /** 紧急联系人电话 */
    private String emergencyPhone;

    /** 当前入住房间ID */
    private String roomId;

    /** 状态：ACTIVE(在读/在职)、GRADUATED(已毕业/离职)、SUSPENDED(休学/停职) */
    @NotNull
    private String status;

    /** 类型：UNDERGRADUATE(本科生)、POSTGRADUATE(研究生)、STAFF(教职工) */
    private String type;

    /** 入学年份 */
    private int enrollmentYear;

    /** 预计毕业年份 */
    private int expectedGraduationYear;

    /** 照片URL */
    private String photoUrl;

    /** 备注 */
    private String remark;

    /** 是否启用 */
    @NotNull
    private boolean enabled;

    @NotNull
    private long createdAt;

    private long updatedAt;
}