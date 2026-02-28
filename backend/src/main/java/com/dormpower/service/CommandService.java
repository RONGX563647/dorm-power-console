package com.dormpower.service;

import com.dormpower.model.CommandRecord;
import com.dormpower.model.Device;
import com.dormpower.repository.CommandRecordRepository;
import com.dormpower.repository.DeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 命令服务
 */
@Service
public class CommandService {

    @Autowired
    private CommandRecordRepository commandRecordRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int CMD_TIMEOUT_SECONDS = 30;

    /**
     * 创建命令记录
     * @param deviceId 设备ID
     * @param request 命令请求
     * @return 命令记录
     */
    public CommandRecord createCommandRecord(String deviceId, Map<String, Object> request) {
        String cmdId = "cmd-" + System.currentTimeMillis();
        long now = System.currentTimeMillis();

        CommandRecord commandRecord = new CommandRecord();
        commandRecord.setCmdId(cmdId);
        commandRecord.setDeviceId(deviceId);
        commandRecord.setAction((String) request.getOrDefault("action", "toggle"));
        
        Integer socket = request.get("socket") != null ? ((Number) request.get("socket")).intValue() : null;
        commandRecord.setSocket(socket);

        try {
            commandRecord.setPayloadJson(objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            commandRecord.setPayloadJson("{}");
        }

        commandRecord.setState("pending");
        commandRecord.setMessage("");
        commandRecord.setCreatedAt(now);
        commandRecord.setUpdatedAt(now);
        commandRecord.setExpiresAt(now + CMD_TIMEOUT_SECONDS * 1000L);

        return commandRecordRepository.save(commandRecord);
    }

    /**
     * 检查是否存在冲突的待处理命令
     * @param deviceId 设备ID
     * @param socket 插座编号
     * @return 是否存在冲突
     */
    public boolean hasPendingConflict(String deviceId, Integer socket) {
        List<CommandRecord> pendingCommands = commandRecordRepository.findByDeviceIdAndState(deviceId, "pending");
        
        for (CommandRecord cmd : pendingCommands) {
            if (socket == null && cmd.getSocket() == null) {
                return true;
            }
            if (socket != null && socket.equals(cmd.getSocket())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 更新命令状态
     * @param cmdId 命令ID
     * @param state 状态
     * @param message 消息
     */
    public void updateCommandState(String cmdId, String state, String message) {
        commandRecordRepository.findById(cmdId).ifPresent(cmd -> {
            cmd.setState(state);
            cmd.setMessage(message != null ? message : "");
            cmd.setUpdatedAt(System.currentTimeMillis());
            
            if ("success".equals(state) || "failed".equals(state)) {
                cmd.setDurationMs((int) (System.currentTimeMillis() - cmd.getCreatedAt()));
            }
            
            commandRecordRepository.save(cmd);
        });
    }

    /**
     * 标记超时的命令
     */
    public void markTimeouts() {
        long now = System.currentTimeMillis();
        List<CommandRecord> pendingCommands = commandRecordRepository.findByState("pending");
        
        for (CommandRecord cmd : pendingCommands) {
            if (cmd.getExpiresAt() < now) {
                cmd.setState("timeout");
                cmd.setMessage("Command timed out");
                cmd.setUpdatedAt(now);
                commandRecordRepository.save(cmd);
            }
        }
    }

    /**
     * 发送命令
     * @param deviceId 设备ID
     * @param request 命令请求
     * @return 命令响应
     */
    public Map<String, Object> sendCommand(String deviceId, Map<String, Object> request) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            throw new RuntimeException("Device not found");
        }

        Integer socket = request.get("socket") != null ? ((Number) request.get("socket")).intValue() : null;
        if (hasPendingConflict(deviceId, socket)) {
            throw new RuntimeException("Pending command exists for target");
        }

        CommandRecord commandRecord = createCommandRecord(deviceId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("cmdId", commandRecord.getCmdId());
        response.put("stripId", deviceId);
        response.put("acceptedAt", commandRecord.getCreatedAt());
        return response;
    }

    /**
     * 获取命令状态
     * @param cmdId 命令ID
     * @return 命令状态
     */
    public Map<String, Object> getCommandStatus(String cmdId) {
        CommandRecord commandRecord = commandRecordRepository.findById(cmdId).orElse(null);
        
        if (commandRecord == null) {
            return null;
        }

        Map<String, Object> status = new HashMap<>();
        status.put("cmdId", commandRecord.getCmdId());
        status.put("state", commandRecord.getState());
        status.put("message", commandRecord.getMessage());
        status.put("updatedAt", commandRecord.getUpdatedAt());
        if (commandRecord.getDurationMs() != null) {
            status.put("durationMs", commandRecord.getDurationMs());
        }
        
        return status;
    }

    /**
     * 构建命令负载
     * @param cmd 命令记录
     * @param request 原始请求
     * @return 命令负载
     */
    public Map<String, Object> buildCommandPayload(CommandRecord cmd, Map<String, Object> request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cmdId", cmd.getCmdId());
        payload.put("ts", System.currentTimeMillis() / 1000);
        payload.put("type", cmd.getAction().toUpperCase());
        payload.put("socketId", cmd.getSocket());
        payload.put("payload", request.getOrDefault("payload", new HashMap<>()));
        payload.put("mode", request.getOrDefault("mode", "immediate"));
        payload.put("duration", request.getOrDefault("duration", 0));
        payload.put("source", "web");
        return payload;
    }

    /**
     * 获取设备的命令历史
     * @param deviceId 设备ID
     * @return 命令历史列表
     */
    public List<CommandRecord> getCommandsByDeviceId(String deviceId) {
        return commandRecordRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId);
    }

}
