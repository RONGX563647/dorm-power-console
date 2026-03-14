package com.dormpower.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 设备注册请求DTO
 *
 * 用于设备自动注册时接收设备信息，包含参数校验规则。
 *
 * 校验规则：
 * - 设备ID：只能包含字母、数字、下划线，长度不超过64字符
 * - 设备名称：不能为空，长度不超过100字符
 * - 房间号：格式为 楼栋号-房间号（如：A1-301）
 *
 * @author dormpower team
 * @version 1.0
 */
@Schema(description = "设备注册请求")
public class DeviceRegistrationRequest {

    @NotBlank(message = "设备ID不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{1,64}$", message = "设备ID只能包含字母、数字、下划线，长度不超过64字符")
    @Schema(description = "设备ID", example = "device_001", required = true)
    private String id;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100字符")
    @Schema(description = "设备名称", example = "A1-301智能插座", required = true)
    private String name;

    @NotBlank(message = "房间号不能为空")
    @Pattern(regexp = "^[A-Za-z]\\d{1,2}-\\d{3,4}$", message = "房间格式错误，正确格式：A1-301")
    @Schema(description = "房间号", example = "A1-301", required = true)
    private String room;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

}