package com.dormpower.ml;

import com.dormpower.model.Device;
import com.dormpower.model.Telemetry;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 异常检测Agent
 * 
 * 核心功能：
 * 1. 实时异常检测（点异常）
 * 2. 上下文异常检测（基于时段）
 * 3. 趋势异常检测（预测性）
 * 4. 设备故障预测
 * 5. 安全风险评估
 */
@Service
public class AnomalyDetectionAgent {

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private static final int WINDOW_SIZE = 30;
    private static final double Z_SCORE_THRESHOLD = 3.0;
    private static final double IQR_K = 1.5;
    private static final double EWMA_ALPHA = 0.3;
    private static final int MIN_SAMPLES = 10;

    /**
     * 异常类型
     */
    public enum AnomalyType {
        POINT_ANOMALY,           // 点异常：单个数据点异常
        CONTEXTUAL_ANOMALY,      // 上下文异常：在特定时段异常
        TREND_ANOMALY,           // 趋势异常：趋势突变
        COLLECTIVE_ANOMALY,      // 集合异常：连续异常点
        PREDICTIVE_ANOMALY       // 预测性异常：预测即将发生
    }

    /**
     * 异常严重级别
     */
    public enum Severity {
        LOW,      // 低风险
        MEDIUM,   // 中风险
        HIGH,     // 高风险
        CRITICAL  // 严重
    }

    /**
     * 检测到的异常
     */
    public static class DetectedAnomaly {
        public String anomalyId;
        public String deviceId;
        public String roomId;
        public AnomalyType type;
        public Severity severity;
        public String description;
        public double value;
        public double expectedValue;
        public double deviationScore;
        public long timestamp;
        public List<String> recommendations;
        public Map<String, Object> metadata;
    }

    /**
     * 设备健康状态
     */
    public static class DeviceHealthStatus {
        public String deviceId;
        public String deviceName;
        public double healthScore;
        public String status;
        public List<DetectedAnomaly> recentAnomalies;
        public Map<String, Double> metrics;
        public List<String> warnings;
        public long lastChecked;
    }

    /**
     * 实时异常检测
     */
    public DetectedAnomaly detectRealtime(String deviceId, double currentPower) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            return null;
        }

        long now = System.currentTimeMillis() / 1000;
        long startTs = now - 3600;

        List<Telemetry> recentData = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(
                deviceId, startTs, now);

        if (recentData.size() < MIN_SAMPLES) {
            return null;
        }

        List<Double> powers = new ArrayList<>();
        for (Telemetry t : recentData) {
            powers.add(t.getPowerW());
        }

        double mean = MLUtils.mean(powers);
        double std = MLUtils.stdDev(powers);

        DetectedAnomaly anomaly = null;

        if (MLUtils.isAnomalyByZScore(currentPower, mean, std, Z_SCORE_THRESHOLD)) {
            anomaly = new DetectedAnomaly();
            anomaly.anomalyId = "anomaly_" + System.currentTimeMillis();
            anomaly.deviceId = deviceId;
            anomaly.roomId = device.getRoom();
            anomaly.type = AnomalyType.POINT_ANOMALY;
            anomaly.value = currentPower;
            anomaly.expectedValue = mean;
            anomaly.deviationScore = Math.abs((currentPower - mean) / (std > 0 ? std : 1));
            anomaly.timestamp = now;
            anomaly.recommendations = new ArrayList<>();

            if (currentPower > mean + 3 * std) {
                anomaly.severity = Severity.HIGH;
                anomaly.description = String.format("功率异常升高: 当前 %.1fW, 预期 %.1fW (偏差 %.1f%%)",
                        currentPower, mean, (currentPower - mean) / mean * 100);
                anomaly.recommendations.add("检查是否有大功率设备接入");
                anomaly.recommendations.add("确认设备运行状态");
            } else if (currentPower < mean - 3 * std && currentPower < 5) {
                anomaly.severity = Severity.MEDIUM;
                anomaly.description = String.format("功率异常降低: 当前 %.1fW, 预期 %.1fW",
                        currentPower, mean);
                anomaly.recommendations.add("检查设备是否正常工作");
                anomaly.recommendations.add("确认是否有设备断电");
            } else {
                anomaly.severity = Severity.LOW;
                anomaly.description = String.format("功率波动: 当前 %.1fW, 预期 %.1fW",
                        currentPower, mean);
            }
        }

        return anomaly;
    }

    /**
     * 批量异常检测（历史数据）
     */
    public List<DetectedAnomaly> detectBatch(String deviceId, long startTs, long endTs) {
        List<DetectedAnomaly> anomalies = new ArrayList<>();

        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) return anomalies;

        List<Telemetry> data = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(
                deviceId, startTs, endTs);

        if (data.size() < MIN_SAMPLES) return anomalies;

        List<Double> powers = new ArrayList<>();
        for (Telemetry t : data) {
            powers.add(t.getPowerW());
        }

        anomalies.addAll(detectPointAnomalies(device, data, powers));
        anomalies.addAll(detectTrendAnomalies(device, data, powers));
        anomalies.addAll(detectContextualAnomalies(device, data, powers));

        return anomalies;
    }

    /**
     * 点异常检测
     */
    private List<DetectedAnomaly> detectPointAnomalies(Device device, List<Telemetry> data, List<Double> powers) {
        List<DetectedAnomaly> anomalies = new ArrayList<>();

        double mean = MLUtils.mean(powers);
        double std = MLUtils.stdDev(powers);

        if (std == 0) return anomalies;

        for (int i = 0; i < data.size(); i++) {
            double power = powers.get(i);
            double zScore = Math.abs((power - mean) / std);

            if (zScore > Z_SCORE_THRESHOLD) {
                Telemetry t = data.get(i);

                DetectedAnomaly anomaly = new DetectedAnomaly();
                anomaly.anomalyId = "anomaly_point_" + i + "_" + System.currentTimeMillis();
                anomaly.deviceId = device.getId();
                anomaly.roomId = device.getRoom();
                anomaly.type = AnomalyType.POINT_ANOMALY;
                anomaly.value = power;
                anomaly.expectedValue = mean;
                anomaly.deviationScore = zScore;
                anomaly.timestamp = t.getTs();
                anomaly.recommendations = new ArrayList<>();
                anomaly.metadata = new HashMap<>();

                if (power > mean) {
                    anomaly.severity = power > mean + 4 * std ? Severity.CRITICAL : Severity.HIGH;
                    anomaly.description = String.format("功率突增: %.1fW (均值 %.1fW)", power, mean);
                    anomaly.recommendations.add("检查是否接入违规电器");
                } else {
                    anomaly.severity = Severity.MEDIUM;
                    anomaly.description = String.format("功率骤降: %.1fW (均值 %.1fW)", power, mean);
                    anomaly.recommendations.add("检查设备是否正常");
                }

                anomalies.add(anomaly);
            }
        }

        return anomalies;
    }

    /**
     * 趋势异常检测
     */
    private List<DetectedAnomaly> detectTrendAnomalies(Device device, List<Telemetry> data, List<Double> powers) {
        List<DetectedAnomaly> anomalies = new ArrayList<>();

        if (powers.size() < WINDOW_SIZE * 2) return anomalies;

        List<Double> movingAvg = MLUtils.movingAverage(powers, WINDOW_SIZE);

        double overallMean = MLUtils.mean(powers);
        double overallStd = MLUtils.stdDev(powers);

        for (int i = WINDOW_SIZE; i < movingAvg.size(); i++) {
            double ma = movingAvg.get(i - WINDOW_SIZE);
            double deviation = Math.abs(ma - overallMean) / (overallStd > 0 ? overallStd : 1);

            if (deviation > 2.0) {
                Telemetry t = data.get(i);

                DetectedAnomaly anomaly = new DetectedAnomaly();
                anomaly.anomalyId = "anomaly_trend_" + i + "_" + System.currentTimeMillis();
                anomaly.deviceId = device.getId();
                anomaly.roomId = device.getRoom();
                anomaly.type = AnomalyType.TREND_ANOMALY;
                anomaly.value = ma;
                anomaly.expectedValue = overallMean;
                anomaly.deviationScore = deviation;
                anomaly.timestamp = t.getTs();
                anomaly.severity = Severity.MEDIUM;
                anomaly.description = String.format("用电趋势异常: 移动平均 %.1fW, 偏离均值 %.1f%%",
                        ma, deviation * 100);
                anomaly.recommendations = Arrays.asList(
                        "关注用电趋势变化",
                        "检查是否有新设备接入"
                );
                anomaly.metadata = new HashMap<>();
                anomaly.metadata.put("movingAverage", ma);
                anomaly.metadata.put("windowSize", WINDOW_SIZE);

                anomalies.add(anomaly);
            }
        }

        return anomalies;
    }

    /**
     * 上下文异常检测（基于时段）
     */
    private List<DetectedAnomaly> detectContextualAnomalies(Device device, List<Telemetry> data, List<Double> powers) {
        List<DetectedAnomaly> anomalies = new ArrayList<>();

        Map<Integer, List<Double>> hourlyPowers = new HashMap<>();
        Map<Integer, List<Telemetry>> hourlyData = new HashMap<>();

        for (int i = 0; i < data.size(); i++) {
            Telemetry t = data.get(i);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(t.getTs() * 1000);
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            hourlyPowers.computeIfAbsent(hour, k -> new ArrayList<>()).add(powers.get(i));
            hourlyData.computeIfAbsent(hour, k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<Integer, List<Double>> entry : hourlyPowers.entrySet()) {
            int hour = entry.getKey();
            List<Double> hourPowers = entry.getValue();

            if (hourPowers.size() < MIN_SAMPLES) continue;

            double hourMean = MLUtils.mean(hourPowers);
            double hourStd = MLUtils.stdDev(hourPowers);

            if (hourStd == 0) continue;

            for (int i = 0; i < hourPowers.size(); i++) {
                double power = hourPowers.get(i);
                double zScore = Math.abs((power - hourMean) / hourStd);

                if (zScore > Z_SCORE_THRESHOLD) {
                    Telemetry t = hourlyData.get(hour).get(i);

                    DetectedAnomaly anomaly = new DetectedAnomaly();
                    anomaly.anomalyId = "anomaly_context_" + hour + "_" + i + "_" + System.currentTimeMillis();
                    anomaly.deviceId = device.getId();
                    anomaly.roomId = device.getRoom();
                    anomaly.type = AnomalyType.CONTEXTUAL_ANOMALY;
                    anomaly.value = power;
                    anomaly.expectedValue = hourMean;
                    anomaly.deviationScore = zScore;
                    anomaly.timestamp = t.getTs();
                    anomaly.severity = Severity.MEDIUM;
                    anomaly.description = String.format("%02d:00时段异常: %.1fW (该时段均值 %.1fW)",
                            hour, power, hourMean);
                    anomaly.recommendations = Arrays.asList(
                            String.format("关注%02d:00时段用电", hour),
                            "检查该时段设备使用情况"
                    );
                    anomaly.metadata = new HashMap<>();
                    anomaly.metadata.put("hour", hour);

                    anomalies.add(anomaly);
                }
            }
        }

        return anomalies;
    }

    /**
     * 预测性异常检测
     */
    public DetectedAnomaly predictAnomaly(String deviceId) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) return null;

        long now = System.currentTimeMillis() / 1000;
        long startTs = now - 7200;

        List<Telemetry> recentData = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(
                deviceId, startTs, now);

        if (recentData.size() < MIN_SAMPLES) return null;

        List<Double> powers = new ArrayList<>();
        for (Telemetry t : recentData) {
            powers.add(t.getPowerW());
        }

        double predicted = MLUtils.ewmaPredict(powers, EWMA_ALPHA);
        double currentMean = MLUtils.mean(powers);
        double currentStd = MLUtils.stdDev(powers);

        double deviation = Math.abs(predicted - currentMean) / (currentStd > 0 ? currentStd : 1);

        if (deviation > 1.5) {
            DetectedAnomaly anomaly = new DetectedAnomaly();
            anomaly.anomalyId = "anomaly_predict_" + System.currentTimeMillis();
            anomaly.deviceId = deviceId;
            anomaly.roomId = device.getRoom();
            anomaly.type = AnomalyType.PREDICTIVE_ANOMALY;
            anomaly.value = predicted;
            anomaly.expectedValue = currentMean;
            anomaly.deviationScore = deviation;
            anomaly.timestamp = now;
            anomaly.severity = Severity.LOW;
            anomaly.description = String.format("预测未来功率可能异常: 预测 %.1fW, 当前均值 %.1fW",
                    predicted, currentMean);
            anomaly.recommendations = Arrays.asList(
                    "持续监控设备状态",
                    "准备应对可能的异常"
            );
            anomaly.metadata = new HashMap<>();
            anomaly.metadata.put("predictionMethod", "EWMA");
            anomaly.metadata.put("alpha", EWMA_ALPHA);

            return anomaly;
        }

        return null;
    }

    /**
     * 获取设备健康状态
     */
    public DeviceHealthStatus getDeviceHealth(String deviceId) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) return null;

        long now = System.currentTimeMillis() / 1000;
        long startTs = now - 86400;

        List<Telemetry> data = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(
                deviceId, startTs, now);

        DeviceHealthStatus status = new DeviceHealthStatus();
        status.deviceId = deviceId;
        status.deviceName = device.getName();
        status.lastChecked = now;
        status.recentAnomalies = new ArrayList<>();
        status.metrics = new HashMap<>();
        status.warnings = new ArrayList<>();

        if (data.size() < MIN_SAMPLES) {
            status.healthScore = 50;
            status.status = "数据不足";
            status.warnings.add("设备数据采集不足，无法准确评估");
            return status;
        }

        List<Double> powers = new ArrayList<>();
        for (Telemetry t : data) {
            powers.add(t.getPowerW());
        }

        double mean = MLUtils.mean(powers);
        double std = MLUtils.stdDev(powers);
        double max = powers.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double min = powers.stream().mapToDouble(Double::doubleValue).min().orElse(0);

        status.metrics.put("avgPower", mean);
        status.metrics.put("stdPower", std);
        status.metrics.put("maxPower", max);
        status.metrics.put("minPower", min);
        status.metrics.put("sampleCount", (double) data.size());

        List<DetectedAnomaly> anomalies = detectBatch(deviceId, startTs, now);
        status.recentAnomalies = anomalies.stream()
                .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
                .limit(10)
                .toList();

        double healthScore = 100.0;

        double stabilityScore = 100.0;
        double cv = std / (mean > 0 ? mean : 1);
        if (cv > 0.5) {
            stabilityScore -= 20;
            status.warnings.add("用电波动较大，建议检查设备稳定性");
        } else if (cv > 0.3) {
            stabilityScore -= 10;
        }
        
        if (mean < 1 && device.isOnline()) {
            stabilityScore -= 15;
            status.warnings.add("设备在线但功率极低，可能处于待机或故障状态");
        }

        double anomalyPenalty = 0;
        long criticalCount = anomalies.stream().filter(a -> a.severity == Severity.CRITICAL).count();
        long highCount = anomalies.stream().filter(a -> a.severity == Severity.HIGH).count();
        long mediumCount = anomalies.stream().filter(a -> a.severity == Severity.MEDIUM).count();

        anomalyPenalty += criticalCount * 5;
        anomalyPenalty += highCount * 3;
        anomalyPenalty += mediumCount * 1;
        
        anomalyPenalty = Math.min(anomalyPenalty, 40);

        healthScore = stabilityScore * 0.6 + (100 - anomalyPenalty) * 0.4;

        if (criticalCount > 0) {
            status.warnings.add(String.format("检测到%d个严重异常", criticalCount));
        }

        healthScore = Math.max(0, Math.min(100, healthScore));

        status.healthScore = Math.round(healthScore * 10.0) / 10.0;

        if (healthScore >= 80) {
            status.status = "健康";
        } else if (healthScore >= 60) {
            status.status = "良好";
        } else if (healthScore >= 40) {
            status.status = "一般";
        } else {
            status.status = "异常";
        }

        return status;
    }

    /**
     * 房间级异常检测
     */
    public Map<String, Object> detectRoomAnomalies(String roomId) {
        List<Device> devices = deviceRepository.findByRoom(roomId);

        List<DetectedAnomaly> allAnomalies = new ArrayList<>();
        Map<String, DeviceHealthStatus> deviceStatuses = new HashMap<>();

        for (Device device : devices) {
            long now = System.currentTimeMillis() / 1000;
            long startTs = now - 3600;

            List<DetectedAnomaly> deviceAnomalies = detectBatch(device.getId(), startTs, now);
            allAnomalies.addAll(deviceAnomalies);

            DeviceHealthStatus health = getDeviceHealth(device.getId());
            if (health != null) {
                deviceStatuses.put(device.getId(), health);
            }
        }

        double roomRiskScore = calculateRoomRiskScore(allAnomalies, deviceStatuses);

        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("riskScore", roomRiskScore);
        result.put("riskLevel", getRiskLevel(roomRiskScore));
        result.put("totalAnomalies", allAnomalies.size());
        result.put("anomalies", allAnomalies.stream()
                .sorted((a, b) -> Double.compare(b.deviationScore, a.deviationScore))
                .limit(20)
                .toList());
        result.put("deviceStatuses", deviceStatuses);
        result.put("timestamp", System.currentTimeMillis() / 1000);

        return result;
    }

    /**
     * 计算房间风险评分
     */
    private double calculateRoomRiskScore(List<DetectedAnomaly> anomalies,
                                          Map<String, DeviceHealthStatus> deviceStatuses) {
        double score = 0;

        for (DetectedAnomaly anomaly : anomalies) {
            switch (anomaly.severity) {
                case CRITICAL -> score += 30;
                case HIGH -> score += 15;
                case MEDIUM -> score += 5;
                case LOW -> score += 1;
            }
        }

        for (DeviceHealthStatus status : deviceStatuses.values()) {
            score += (100 - status.healthScore) * 0.2;
        }

        return Math.min(100, score);
    }

    /**
     * 获取风险等级
     */
    private String getRiskLevel(double score) {
        if (score >= 80) return "严重";
        if (score >= 60) return "高";
        if (score >= 40) return "中";
        if (score >= 20) return "低";
        return "正常";
    }

    /**
     * 设备故障预测
     */
    public Map<String, Object> predictDeviceFailure(String deviceId) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            return Map.of("success", false, "message", "设备不存在");
        }

        long now = System.currentTimeMillis() / 1000;
        long startTs = now - 86400 * 7;

        List<Telemetry> data = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(
                deviceId, startTs, now);

        if (data.size() < MIN_SAMPLES) {
            return Map.of("success", false, "message", "数据不足");
        }

        List<Double> powers = new ArrayList<>();
        for (Telemetry t : data) {
            powers.add(t.getPowerW());
        }

        List<Double> dailyAvgs = new ArrayList<>();
        int dailyBucket = 288;
        for (int i = 0; i < powers.size(); i += dailyBucket) {
            int end = Math.min(i + dailyBucket, powers.size());
            List<Double> dayPowers = powers.subList(i, end);
            dailyAvgs.add(MLUtils.mean(dayPowers));
        }

        double failureProbability = 0;
        List<String> riskFactors = new ArrayList<>();

        if (dailyAvgs.size() >= 3) {
            double trend = 0;
            for (int i = 1; i < dailyAvgs.size(); i++) {
                trend += dailyAvgs.get(i) - dailyAvgs.get(i - 1);
            }
            trend /= (dailyAvgs.size() - 1);

            if (Math.abs(trend) > 5) {
                failureProbability += 20;
                riskFactors.add(String.format("功率趋势异常: 日均变化 %.2fW", trend));
            }
        }

        double cv = MLUtils.stdDev(powers) / (MLUtils.mean(powers) > 0 ? MLUtils.mean(powers) : 1);
        if (cv > 0.5) {
            failureProbability += 25;
            riskFactors.add(String.format("功率波动大: 变异系数 %.2f", cv));
        }

        List<Integer> changePoints = MLUtils.detectChangePoints(powers, 2.0);
        if (changePoints.size() > 3) {
            failureProbability += 15;
            riskFactors.add("检测到多次功率突变");
        }

        DeviceHealthStatus health = getDeviceHealth(deviceId);
        if (health != null) {
            failureProbability += (100 - health.healthScore) * 0.3;
        }

        failureProbability = Math.min(100, failureProbability);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("deviceId", deviceId);
        result.put("deviceName", device.getName());
        result.put("failureProbability", failureProbability);
        result.put("riskLevel", getRiskLevel(failureProbability));
        result.put("riskFactors", riskFactors);
        result.put("recommendations", generateMaintenanceRecommendations(failureProbability, riskFactors));
        result.put("timestamp", now);

        return result;
    }

    /**
     * 生成维护建议
     */
    private List<String> generateMaintenanceRecommendations(double probability, List<String> riskFactors) {
        List<String> recommendations = new ArrayList<>();

        if (probability > 70) {
            recommendations.add("建议立即检查设备");
            recommendations.add("考虑更换设备");
        } else if (probability > 40) {
            recommendations.add("建议近期安排检修");
            recommendations.add("加强监控频率");
        } else if (probability > 20) {
            recommendations.add("持续关注设备状态");
            recommendations.add("记录异常情况");
        } else {
            recommendations.add("设备运行正常");
        }

        if (riskFactors.stream().anyMatch(f -> f.contains("波动"))) {
            recommendations.add("检查电源稳定性");
        }
        if (riskFactors.stream().anyMatch(f -> f.contains("趋势"))) {
            recommendations.add("分析用电变化原因");
        }

        return recommendations;
    }
}
