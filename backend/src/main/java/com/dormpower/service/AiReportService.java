package com.dormpower.service;

import com.dormpower.model.Device;
import com.dormpower.model.Telemetry;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI分析报告服务
 */
@Service
public class AiReportService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TelemetryRepository telemetryRepository;

    /**
     * 获取AI分析报告
     * @param roomId 房间ID
     * @param period 时间范围
     * @return AI分析报告
     */
    public Map<String, Object> getAiReport(String roomId, String period) {
        List<Device> devices = deviceRepository.findByRoom(roomId);
        List<String> deviceIds = new ArrayList<>();
        for (Device d : devices) {
            deviceIds.add(d.getId());
        }

        Map<String, Object> report = new HashMap<>();
        report.put("room_id", roomId);

        if (deviceIds.isEmpty()) {
            report.put("summary", "No device data in this room yet.");
            report.put("anomalies", List.of("No analyzable sample found."));
            report.put("recommendations", List.of("Ensure devices upload status and telemetry periodically."));
            report.put("generated_at", System.currentTimeMillis() / 1000);
            report.put("power_stats", Map.of(
                "avg_power_w", 0.0,
                "peak_power_w", 0.0,
                "peak_time", "",
                "total_kwh", 0.0
            ));
            return report;
        }

        int days = "7d".equals(period) ? 7 : 30;
        long startTs = System.currentTimeMillis() / 1000 - days * 24 * 3600L;

        List<Telemetry> data = new ArrayList<>();
        for (String deviceId : deviceIds) {
            data.addAll(telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(deviceId, startTs, System.currentTimeMillis() / 1000));
        }

        if (data.isEmpty()) {
            report.put("summary", "Devices are online but telemetry coverage is insufficient.");
            report.put("anomalies", List.of("Not enough telemetry points in selected period."));
            report.put("recommendations", List.of("Increase telemetry frequency to every 1-5 seconds."));
            report.put("generated_at", System.currentTimeMillis() / 1000);
            report.put("power_stats", Map.of(
                "avg_power_w", 0.0,
                "peak_power_w", 0.0,
                "peak_time", "",
                "total_kwh", 0.0
            ));
            return report;
        }

        double avgPower = data.stream().mapToDouble(Telemetry::getPowerW).average().orElse(0.0);
        double peakPower = data.stream().mapToDouble(Telemetry::getPowerW).max().orElse(0.0);
        
        Telemetry peakTelemetry = data.stream()
            .max((a, b) -> Double.compare(a.getPowerW(), b.getPowerW()))
            .orElse(null);
        
        String peakTime = "";
        if (peakTelemetry != null) {
            peakTime = java.time.Instant.ofEpochSecond(peakTelemetry.getTs()).toString();
        }

        double totalKwh = (avgPower * data.size() * 60) / (1000.0 * 3600.0);

        List<String> anomalies = new ArrayList<>();
        if (peakPower > 200) {
            anomalies.add(String.format("Peak power reached %.1fW. Check high-load periods.", peakPower));
        }
        if (avgPower > 150) {
            anomalies.add(String.format("Average power is high at %.1fW. Consider reducing load.", avgPower));
        }

        List<String> recommendations = new ArrayList<>();
        recommendations.add("Enable auto off for low-priority sockets after 00:30.");
        recommendations.add("Set alerts for periods above baseline by 20%.");
        if (avgPower > 100) {
            recommendations.add("Consider scheduling high-power devices during off-peak hours.");
        }

        report.put("summary", String.format("Average power is about %.1fW, peak is about %.1fW.", avgPower, peakPower));
        report.put("anomalies", anomalies);
        report.put("recommendations", recommendations);
        report.put("generated_at", System.currentTimeMillis() / 1000);
        report.put("power_stats", Map.of(
            "avg_power_w", Math.round(avgPower * 10.0) / 10.0,
            "peak_power_w", Math.round(peakPower * 10.0) / 10.0,
            "peak_time", peakTime,
            "total_kwh", Math.round(totalKwh * 100.0) / 100.0
        ));

        return report;
    }

}
