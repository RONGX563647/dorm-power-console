package com.dormpower.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 命令请求DTO
 */
@Schema(description = "设备控制命令请求")
public record CommandRequest(
    @NotBlank(message = "操作类型不能为空")
    @Schema(description = "操作类型", example = "turn_on", allowableValues = {"turn_on", "turn_off", "toggle", "timer_on", "timer_off"})
    String action,

    @Schema(description = "插座ID（可选）", example = "1")
    Integer socketId,

    @Schema(description = "附加参数（JSON格式）", example = "{\"duration\": 3600}")
    String payload
) {}