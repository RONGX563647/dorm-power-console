package com.dormpower.controller;

import com.dormpower.service.CommandService;
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
 * 命令查询控制器 - 与Python后端保持一致的路由 /api/cmd/{cmdId}
 */
@RestController
@RequestMapping("/api")
public class CommandQueryController {

    @Autowired
    private CommandService commandService;

    /**
     * 查询命令状态 - 与Python后端保持一致的路由
     * @param cmdId 命令ID
     * @return 命令状态
     */
    @GetMapping("/cmd/{cmdId}")
    public ResponseEntity<Map<String, Object>> getCommandStatus(@PathVariable String cmdId) {
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
