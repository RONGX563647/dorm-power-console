package com.dormpower.controller;

import com.dormpower.service.CommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 命令查询控制器
 */
@RestController
@RequestMapping("/api")
@Tag(name = "命令查询", description = "设备控制命令状态查询接口")
public class CommandQueryController {

    @Autowired
    private CommandService commandService;

    /**
     * 查询命令状态
     */
    @Operation(summary = "查询命令状态", 
               description = "根据命令ID查询设备控制命令的执行状态", 
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "命令不存在")
    })
    @GetMapping("/cmd/{cmdId}")
    public ResponseEntity<Map<String, Object>> getCommandStatus(
            @Parameter(description = "命令ID", required = true, example = "cmd_123456")
            @PathVariable String cmdId) {
        Map<String, Object> status = commandService.getCommandStatus(cmdId);
        if (status == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("code", "NOT_FOUND");
            error.put("message", "cmd not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(status);
    }

}
