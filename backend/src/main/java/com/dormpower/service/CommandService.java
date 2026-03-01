package com.dormpower.service;

import com.dormpower.model.CommandRecord;
import com.dormpower.model.Device;
import com.dormpower.repository.CommandRecordRepository;
import com.dormpower.repository.DeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 命令服务
 */
@Service
public class CommandService {

    private static final int CMD_TIMEOUT_SECONDS = 30;
    private static final String STATE_PENDING = "pending";
    private static final String STATE_SUCCESS = "success";
    private static final String STATE_FAILED = "failed";
    private static final String STATE_TIMEOUT = "timeout";

    @Autowired
    private CommandRecordRepository commandRecordRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public CommandRecord createCommandRecord(String deviceId, Map<String, Object> request) {
        final long now = System.currentTimeMillis();
        final String cmdId = "cmd-" + now;

        CommandRecord commandRecord = new CommandRecord();
        commandRecord.setCmdId(cmdId);
        commandRecord.setDeviceId(deviceId);
        commandRecord.setAction((String) request.getOrDefault("action", "toggle"));
        
        Object socketObj = request.get("socket");
        Integer socket = socketObj != null ? ((Number) socketObj).intValue() : null;
        commandRecord.setSocket(socket);

        try {
            commandRecord.setPayloadJson(objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            commandRecord.setPayloadJson("{}");
        }

        commandRecord.setState(STATE_PENDING);
        commandRecord.setMessage("");
        commandRecord.setCreatedAt(now);
        commandRecord.setUpdatedAt(now);
        commandRecord.setExpiresAt(now + CMD_TIMEOUT_SECONDS * 1000L);

        return commandRecordRepository.save(commandRecord);
    }

    public boolean hasPendingConflict(String deviceId, Integer socket) {
        List<CommandRecord> pendingCommands = commandRecordRepository.findByDeviceIdAndState(deviceId, STATE_PENDING);
        
        for (CommandRecord cmd : pendingCommands) {
            Integer cmdSocket = cmd.getSocket();
            if (socket == null && cmdSocket == null) {
                return true;
            }
            if (socket != null && socket.equals(cmdSocket)) {
                return true;
            }
        }
        
        return false;
    }

    public void updateCommandState(String cmdId, String state, String message) {
        commandRecordRepository.findById(cmdId).ifPresent(cmd -> {
            final long now = System.currentTimeMillis();
            cmd.setState(state);
            cmd.setMessage(message != null ? message : "");
            cmd.setUpdatedAt(now);
            
            if (STATE_SUCCESS.equals(state) || STATE_FAILED.equals(state)) {
                cmd.setDurationMs((int) (now - cmd.getCreatedAt()));
            }
            
            commandRecordRepository.save(cmd);
        });
    }

    public void markTimeouts() {
        final long now = System.currentTimeMillis();
        List<CommandRecord> pendingCommands = commandRecordRepository.findByState(STATE_PENDING);
        
        for (CommandRecord cmd : pendingCommands) {
            if (cmd.getExpiresAt() < now) {
                cmd.setState(STATE_TIMEOUT);
                cmd.setMessage("Command timed out");
                cmd.setUpdatedAt(now);
                commandRecordRepository.save(cmd);
            }
        }
    }

    public Map<String, Object> sendCommand(String deviceId, Map<String, Object> request) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            throw new RuntimeException("Device not found");
        }

        Object socketObj = request.get("socket");
        Integer socket = socketObj != null ? ((Number) socketObj).intValue() : null;
        
        if (hasPendingConflict(deviceId, socket)) {
            throw new RuntimeException("Pending command exists for target");
        }

        CommandRecord commandRecord = createCommandRecord(deviceId, request);

        Map<String, Object> response = new HashMap<>(4);
        response.put("ok", true);
        response.put("cmdId", commandRecord.getCmdId());
        response.put("stripId", deviceId);
        response.put("acceptedAt", commandRecord.getCreatedAt());
        return response;
    }

    public Map<String, Object> getCommandStatus(String cmdId) {
        CommandRecord commandRecord = commandRecordRepository.findById(cmdId).orElse(null);
        
        if (commandRecord == null) {
            return null;
        }

        Map<String, Object> status = new HashMap<>(4);
        status.put("cmdId", commandRecord.getCmdId());
        status.put("state", commandRecord.getState());
        status.put("message", commandRecord.getMessage());
        status.put("updatedAt", commandRecord.getUpdatedAt());
        
        Integer duration = commandRecord.getDurationMs();
        if (duration != null) {
            status.put("durationMs", duration);
        }
        
        return status;
    }

    public Map<String, Object> buildCommandPayload(CommandRecord cmd, Map<String, Object> request) {
        Map<String, Object> payload = new HashMap<>(8);
        payload.put("cmdId", cmd.getCmdId());
        payload.put("ts", System.currentTimeMillis() / 1000);
        payload.put("type", cmd.getAction().toUpperCase());
        payload.put("socketId", cmd.getSocket());
        payload.put("payload", request.getOrDefault("payload", Collections.emptyMap()));
        payload.put("mode", request.getOrDefault("mode", "immediate"));
        payload.put("duration", request.getOrDefault("duration", 0));
        payload.put("source", "web");
        return payload;
    }

    public List<CommandRecord> getCommandsByDeviceId(String deviceId) {
        return commandRecordRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId);
    }

}
