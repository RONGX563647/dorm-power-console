package com.dormpower.service;

import com.dormpower.model.Device;
import com.dormpower.model.Telemetry;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI分析报告服务
 */
@Service
public class AiReportService {

    private static final String NO_DEVICE_SUMMARY = "No device data in this room yet.";
    private static final String NO_DATA_SUMMARY = "Devices are online but telemetry coverage is insufficient.";
    
    private static final List<String> NO_DEVICE_ANOMALIES = Collections.singletonList("No analyzable sample found.");
    private static final List<String> NO_DATA_ANOMALIES = Collections.singletonList("Not enough telemetry points in selected period.");
    
    private static final List<String> NO_DEVICE_RECOMMENDATIONS = Collections.singletonList("Ensure devices upload status and telemetry periodically.");
    private static final List<String> NO_DATA_RECOMMENDATIONS = Collections.singletonList("Increase telemetry frequency to every 1-5 seconds.");

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TelemetryRepository telemetryRepository;

    public Map<String, Object> getAiReport(String roomId, String period) {
        List<Device> devices = deviceRepository.findByRoom(roomId);
        
        if (devices.isEmpty()) {
            return createEmptyReport(roomId, NO_DEVICE_SUMMARY, NO_DEVICE_ANOMALIES, NO_DEVICE_RECOMMENDATIONS);
        }

        final int days = "7d".equals(period) ? 7 : 30;
        final long nowTs = System.currentTimeMillis() / 1000;
        final long startTs = nowTs - days * 24 * 3600L;

        List<Telemetry> allData = new ArrayList<>();
        for (Device d : devices) {
            List<Telemetry> deviceData = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(d.getId(), startTs, nowTs);
            allData.addAll(deviceData);
        }

        if (allData.isEmpty()) {
            return createEmptyReport(roomId, NO_DATA_SUMMARY, NO_DATA_ANOMALIES, NO_DATA_RECOMMENDATIONS);
        }

        return analyzeData(roomId, allData);
    }

    private Map<String, Object> createEmptyReport(String roomId, String summary, List<String> anomalies, List<String> recommendations) {
        Map<String, Object> report = new HashMap<>(6);
        report.put("room_id", roomId);
        report.put("summary", summary);
        report.put("anomalies", anomalies);
        report.put("recommendations", recommendations);
        report.put("generated_at", System.currentTimeMillis() / 1000);
        report.put("power_stats", createEmptyPowerStats());
        return report;
    }

    private Map<String, Object> createEmptyPowerStats() {
        Map<String, Object> stats = new HashMap<>(4);
        stats.put("avg_power_w", 0.0);
        stats.put("peak_power_w", 0.0);
        stats.put("peak_time", "");
        stats.put("total_kwh", 0.0);
        return stats;
    }

    private Map<String, Object> analyzeData(String roomId, List<Telemetry> data) {
        final int dataSize = data.size();
        
        double sumPower = 0.0;
        double peakPower = 0.0;
        long peakTs = 0;
        
        for (int i = 0; i < dataSize; i++) {
            Telemetry t = data.get(i);
            double power = t.getPowerW();
            sumPower += power;
            if (power > peakPower) {
                peakPower = power;
                peakTs = t.getTs();
            }
        }
        
        final double avgPower = sumPower / dataSize;
        final String peakTime = peakTs > 0 ? java.time.Instant.ofEpochSecond(peakTs).toString() : "";
        final double totalKwh = (avgPower * dataSize * 60) / (1000.0 * 3600.0);

        List<String> anomalies = new ArrayList<>(2);
        if (peakPower > 200) {
            anomalies.add(String.format("Peak power reached %.1fW. Check high-load periods.", peakPower));
        }
        if (avgPower > 150) {
            anomalies.add(String.format("Average power is high at %.1fW. Consider reducing load.", avgPower));
        }

        List<String> recommendations = new ArrayList<>(3);
        recommendations.add("Enable auto off for low-priority sockets after 00:30.");
        recommendations.add("Set alerts for periods above baseline by 20%.");
        if (avgPower > 100) {
            recommendations.add("Consider scheduling high-power devices during off-peak hours.");
        }

        Map<String, Object> report = new HashMap<>(6);
        report.put("room_id", roomId);
        report.put("summary", String.format("Average power is about %.1fW, peak is about %.1fW.", avgPower, peakPower));
        report.put("anomalies", anomalies);
        report.put("recommendations", recommendations);
        report.put("generated_at", System.currentTimeMillis() / 1000);
        report.put("power_stats", createPowerStats(avgPower, peakPower, peakTime, totalKwh));
        
        return report;
    }

    private Map<String, Object> createPowerStats(double avgPower, double peakPower, String peakTime, double totalKwh) {
        Map<String, Object> stats = new HashMap<>(4);
        stats.put("avg_power_w", Math.round(avgPower * 10.0) / 10.0);
        stats.put("peak_power_w", Math.round(peakPower * 10.0) / 10.0);
        stats.put("peak_time", peakTime);
        stats.put("total_kwh", Math.round(totalKwh * 100.0) / 100.0);
        return stats;
    }

}
