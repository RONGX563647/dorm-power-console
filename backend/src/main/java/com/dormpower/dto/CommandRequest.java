package com.dormpower.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 命令请求DTO
 */
@Schema(description = "设备控制命令请求")
public class CommandRequest {

    @NotBlank(message = "操作类型不能为空")
    @Schema(description = "操作类型", example = "turn_on", allowableValues = {"turn_on", "turn_off", "toggle", "timer_on", "timer_off"})
    private String action;

    @Schema(description = "插座ID（可选）", example = "1")
    private Integer socketId;

    @Schema(description = "附加参数（JSON格式）", example = "{\"duration\": 3600}")
    private String payload;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getSocketId() {
        return socketId;
    }

    public void setSocketId(Integer socketId) {
        this.socketId = socketId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

}
