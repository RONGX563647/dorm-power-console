package com.dormpower.service;

import com.dormpower.model.Telemetry;
import com.dormpower.repository.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 遥测数据服务
 */
@Service
public class TelemetryService {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryService.class);

    @Autowired
    private TelemetryRepository telemetryRepository;

    private static final Map<String, RangeConfig> RANGE_CONFIG;
    
    static {
        Map<String, RangeConfig> config = new HashMap<>(4);
        config.put("60s", new RangeConfig(60, 1));
        config.put("24h", new RangeConfig(96, 900));
        config.put("7d", new RangeConfig(168, 3600));
        config.put("30d", new RangeConfig(120, 21600));
        RANGE_CONFIG = Collections.unmodifiableMap(config);
    }

    @Cacheable(value = "telemetry", key = "#deviceId + '_' + #range")
    public List<Map<String, Object>> getTelemetry(String deviceId, String range) {
        RangeConfig config = RANGE_CONFIG.get(range);
        if (config == null) {
            throw new IllegalArgumentException("Invalid range: " + range);
        }

        final int points = config.points;
        final int step = config.step;
        final long nowTs = System.currentTimeMillis() / 1000;
        final long startTs = nowTs - (long) (points - 1) * step;

        List<Telemetry> rows = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(deviceId, startTs, nowTs);

        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        if (!range.equals("60s")) {
            return processNon60sRange(rows, points);
        }

        return process60sRange(deviceId, rows, points, step, startTs);
    }

    private List<Map<String, Object>> processNon60sRange(List<Telemetry> rows, int points) {
        final int size = rows.size();
        
        if (size <= points) {
            List<Map<String, Object>> result = new ArrayList<>(size);
            for (Telemetry r : rows) {
                Map<String, Object> data = new HashMap<>(2);
                data.put("ts", r.getTs());
                data.put("power_w", roundPower(r.getPowerW()));
                result.add(data);
            }
            return result;
        }

        List<Map<String, Object>> result = new ArrayList<>(points);
        final double stepIdx = (double) (size - 1) / (points - 1);
        
        for (int i = 0; i < points; i++) {
            int idx = (int) Math.round(i * stepIdx);
            Telemetry r = rows.get(idx);
            Map<String, Object> data = new HashMap<>(2);
            data.put("ts", r.getTs());
            data.put("power_w", roundPower(r.getPowerW()));
            result.add(data);
        }
        return result;
    }

    private List<Map<String, Object>> process60sRange(String deviceId, List<Telemetry> rows, int points, int step, long startTs) {
        List<Map<String, Object>> result = new ArrayList<>(points);
        
        Telemetry prevRow = telemetryRepository.findFirstByDeviceIdAndTsLessThanOrderByTsDesc(deviceId, startTs);
        double prevPower = prevRow != null ? prevRow.getPowerW() : 0.0;

        final int rowsSize = rows.size();
        int rowIdx = 0;
        
        for (int i = 0; i < points; i++) {
            long slotTs = startTs + (long) i * step;
            double power = prevPower;

            while (rowIdx < rowsSize) {
                Telemetry row = rows.get(rowIdx);
                if (row.getTs() <= slotTs) {
                    power = row.getPowerW();
                    rowIdx++;
                } else {
                    break;
                }
            }

            Map<String, Object> data = new HashMap<>(2);
            data.put("ts", slotTs);
            data.put("power_w", roundPower(power));
            result.add(data);
        }

        return result;
    }

    private static double roundPower(double power) {
        return Math.round(power * 1000.0) / 1000.0;
    }

    public void saveTelemetry(String deviceId, long ts, double powerW, double voltageV, double currentA) {
        Telemetry telemetry = new Telemetry();
        telemetry.setDeviceId(deviceId);
        telemetry.setTs(ts);
        telemetry.setPowerW(powerW);
        telemetry.setVoltageV(voltageV);
        telemetry.setCurrentA(currentA);
        telemetryRepository.save(telemetry);
    }

    private static final class RangeConfig {
        final int points;
        final int step;

        RangeConfig(int points, int step) {
            this.points = points;
            this.step = step;
        }
    }

}
