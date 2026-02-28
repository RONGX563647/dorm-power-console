package com.dormpower.service;

import com.dormpower.model.Telemetry;
import com.dormpower.repository.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 遥测数据服务
 */
@Service
public class TelemetryService {

    @Autowired
    private TelemetryRepository telemetryRepository;

    private static final Map<String, RangeConfig> RANGE_CONFIG = Map.of(
        "60s", new RangeConfig(60, 1),
        "24h", new RangeConfig(96, 900),
        "7d", new RangeConfig(168, 3600),
        "30d", new RangeConfig(120, 21600)
    );

    /**
     * 获取遥测数据
     * @param deviceId 设备ID
     * @param range 时间范围
     * @return 遥测数据列表
     */
    public List<Map<String, Object>> getTelemetry(String deviceId, String range) {
        RangeConfig config = RANGE_CONFIG.get(range);
        if (config == null) {
            throw new IllegalArgumentException("Invalid range: " + range);
        }

        int points = config.points;
        int step = config.step;
        long nowTs = System.currentTimeMillis() / 1000;
        long startTs = nowTs - (long) (points - 1) * step;

        List<Telemetry> rows = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(
            deviceId, startTs, nowTs
        );

        if (!range.equals("60s")) {
            if (rows.isEmpty()) {
                return new ArrayList<>();
            }
            
            if (rows.size() <= points) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Telemetry r : rows) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("ts", r.getTs());
                    data.put("power_w", Math.round(r.getPowerW() * 1000.0) / 1000.0);
                    result.add(data);
                }
                return result;
            }

            List<Map<String, Object>> result = new ArrayList<>();
            double stepIdx = (double) (rows.size() - 1) / (points - 1);
            for (int i = 0; i < points; i++) {
                int idx = (int) Math.round(i * stepIdx);
                Telemetry r = rows.get(idx);
                Map<String, Object> data = new HashMap<>();
                data.put("ts", r.getTs());
                data.put("power_w", Math.round(r.getPowerW() * 1000.0) / 1000.0);
                result.add(data);
            }
            return result;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        
        Telemetry prevRow = telemetryRepository.findFirstByDeviceIdAndTsLessThanOrderByTsDesc(deviceId, startTs);
        Double prevPower = prevRow != null ? prevRow.getPowerW() : 0.0;

        int rowIdx = 0;
        for (int i = 0; i < points; i++) {
            long slotTs = startTs + (long) i * step;
            double power = prevPower;

            while (rowIdx < rows.size()) {
                Telemetry row = rows.get(rowIdx);
                if (row.getTs() <= slotTs) {
                    power = row.getPowerW();
                    rowIdx++;
                } else {
                    break;
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("ts", slotTs);
            data.put("power_w", Math.round(power * 1000.0) / 1000.0);
            result.add(data);
        }

        return result;
    }

    /**
     * 保存遥测数据
     * @param deviceId 设备ID
     * @param ts 时间戳
     * @param powerW 功率
     * @param voltageV 电压
     * @param currentA 电流
     */
    public void saveTelemetry(String deviceId, long ts, double powerW, double voltageV, double currentA) {
        Telemetry telemetry = new Telemetry();
        telemetry.setDeviceId(deviceId);
        telemetry.setTs(ts);
        telemetry.setPowerW(powerW);
        telemetry.setVoltageV(voltageV);
        telemetry.setCurrentA(currentA);
        telemetryRepository.save(telemetry);
    }

    private static class RangeConfig {
        final int points;
        final int step;

        RangeConfig(int points, int step) {
            this.points = points;
            this.step = step;
        }
    }

}
