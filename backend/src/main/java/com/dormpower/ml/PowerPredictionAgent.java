package com.dormpower.ml;

import com.dormpower.model.Device;
import com.dormpower.model.Telemetry;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 用电预测Agent
 * 
 * 核心功能：
 * 1. 预测每日/每周用电量
 * 2. 预测各时段用电趋势
 * 3. 识别用电异常趋势
 * 4. 生成节能建议
 */
@Service
public class PowerPredictionAgent {

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private static final int HISTORY_DAYS = 7;
    private static final double EWMA_ALPHA = 0.3;

    /**
     * 用电预测结果
     */
    public static class PowerPrediction {
        public String roomId;
        public long predictionTime;
        public double predictedDailyKwh;
        public double predictedWeeklyKwh;
        public double predictedMonthlyKwh;
        public List<HourlyPrediction> hourlyPredictions;
        public double confidence;
        public double savingsPotential;
        public String recommendation;
        public Map<String, Object> metadata;
    }

    /**
     * 小时预测
     */
    public static class HourlyPrediction {
        public int hour;
        public double predictedPower;
        public double confidence;
        public String level;
    }

    /**
     * 节能策略
     */
    public static class EnergySavingStrategy {
        public String strategyId;
        public String name;
        public String description;
        public String triggerCondition;
        public List<String> actions;
        public double estimatedSavingKwh;
        public double estimatedSavingPercent;
        public int priority;
        public boolean autoExecute;
    }

    /**
     * 预测房间用电量
     */
    public PowerPrediction predictPower(String roomId, int days) {
        List<Device> devices = deviceRepository.findByRoom(roomId);
        
        PowerPrediction prediction = new PowerPrediction();
        prediction.roomId = roomId;
        prediction.predictionTime = System.currentTimeMillis() / 1000;
        prediction.hourlyPredictions = new ArrayList<>();
        prediction.metadata = new HashMap<>();

        if (devices.isEmpty()) {
            prediction.predictedDailyKwh = 0;
            prediction.confidence = 0;
            prediction.recommendation = "房间无设备";
            return prediction;
        }

        long now = System.currentTimeMillis() / 1000;
        long startTs = now - days * 24 * 3600L;

        List<Telemetry> allData = new ArrayList<>();
        for (Device d : devices) {
            List<Telemetry> deviceData = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(d.getId(), startTs, now);
            allData.addAll(deviceData);
        }

        if (allData.size() < 100) {
            prediction.predictedDailyKwh = 0;
            prediction.confidence = 0;
            prediction.recommendation = "数据不足，无法预测";
            return prediction;
        }

        Map<Integer, List<Double>> hourlyPower = aggregateByHour(allData);
        Map<Integer, Double> hourlyAvg = new HashMap<>();
        Map<Integer, Double> hourlyTrend = new HashMap<>();

        for (int h = 0; h < 24; h++) {
            List<Double> powers = hourlyPower.getOrDefault(h, new ArrayList<>());
            if (!powers.isEmpty()) {
                double avg = MLUtils.mean(powers);
                hourlyAvg.put(h, avg);
                
                if (powers.size() >= 3) {
                    double trend = calculateTrend(powers);
                    hourlyTrend.put(h, trend);
                }
            }
        }

        double totalDailyWh = 0;
        for (int h = 0; h < 24; h++) {
            HourlyPrediction hp = new HourlyPrediction();
            hp.hour = h;
            
            double basePower = hourlyAvg.getOrDefault(h, 30.0);
            double trend = hourlyTrend.getOrDefault(h, 0.0);
            hp.predictedPower = Math.max(0, basePower * (1 + trend * 0.1));
            
            hp.confidence = calculateConfidence(hourlyPower.getOrDefault(h, new ArrayList<>()));
            hp.level = getPowerLevel(hp.predictedPower, hourlyAvg);
            
            prediction.hourlyPredictions.add(hp);
            totalDailyWh += hp.predictedPower;
        }

        prediction.predictedDailyKwh = totalDailyWh / 1000.0;
        prediction.predictedWeeklyKwh = prediction.predictedDailyKwh * 7;
        prediction.predictedMonthlyKwh = prediction.predictedDailyKwh * 30;

        prediction.savingsPotential = calculateSavingsPotential(prediction, hourlyAvg);

        prediction.confidence = calculateOverallConfidence(hourlyPower);

        prediction.recommendation = generateRecommendation(prediction);

        return prediction;
    }

    /**
     * 生成节能策略
     */
    public List<EnergySavingStrategy> generateSavingStrategies(String roomId, PowerPrediction prediction) {
        List<EnergySavingStrategy> strategies = new ArrayList<>();

        if (prediction.savingsPotential > 0.1) {
            EnergySavingStrategy strategy = new EnergySavingStrategy();
            strategy.strategyId = "strategy_auto_" + System.currentTimeMillis();
            strategy.name = "智能节能模式";
            strategy.description = String.format("预计可节省 %.2f kWh/天 (%.1f%%)", 
                    prediction.predictedDailyKwh * prediction.savingsPotential,
                    prediction.savingsPotential * 100);
            strategy.triggerCondition = "daily_prediction > threshold";
            strategy.actions = Arrays.asList(
                    "低用电时段降低待机功耗",
                    "高用电时段限制大功率设备",
                    "夜间自动关闭非必要设备"
            );
            strategy.estimatedSavingKwh = prediction.predictedDailyKwh * prediction.savingsPotential;
            strategy.estimatedSavingPercent = prediction.savingsPotential * 100;
            strategy.priority = 1;
            strategy.autoExecute = true;
            strategies.add(strategy);
        }

        for (HourlyPrediction hp : prediction.hourlyPredictions) {
            if ("HIGH".equals(hp.level) && hp.predictedPower > 50) {
                EnergySavingStrategy strategy = new EnergySavingStrategy();
                strategy.strategyId = "strategy_peak_" + hp.hour;
                strategy.name = String.format("高峰时段节能 (%02d:00)", hp.hour);
                strategy.description = String.format("预测 %02d:00 用电 %.1fW，建议限电", 
                        hp.hour, hp.predictedPower);
                strategy.triggerCondition = String.format("hour == %d && power > %.0f", hp.hour, hp.predictedPower * 0.8);
                strategy.actions = Arrays.asList(
                        "限制功率上限为" + (hp.predictedPower * 0.7) + "W",
                        "发送用电提醒通知",
                        "记录高峰用电日志"
                );
                strategy.estimatedSavingKwh = hp.predictedPower * 0.3 / 1000.0;
                strategy.estimatedSavingPercent = 30;
                strategy.priority = 2;
                strategy.autoExecute = true;
                strategies.add(strategy);
            }
        }

        for (HourlyPrediction hp : prediction.hourlyPredictions) {
            if ("LOW".equals(hp.level) && hp.hour >= 0 && hp.hour < 6) {
                EnergySavingStrategy strategy = new EnergySavingStrategy();
                strategy.strategyId = "strategy_night_" + hp.hour;
                strategy.name = String.format("夜间待机优化 (%02d:00-%02d:00)", hp.hour, hp.hour + 1);
                strategy.description = "夜间低用电时段，自动降低待机功耗";
                strategy.triggerCondition = String.format("hour >= %d && hour < 6", hp.hour);
                strategy.actions = Arrays.asList(
                        "关闭待机设备",
                        "设置最低功耗模式",
                        "保留必要供电"
                );
                strategy.estimatedSavingKwh = hp.predictedPower * 0.5 / 1000.0;
                strategy.estimatedSavingPercent = 50;
                strategy.priority = 3;
                strategy.autoExecute = true;
                strategies.add(strategy);
                break;
            }
        }

        strategies.sort((a, b) -> Integer.compare(a.priority, b.priority));

        return strategies;
    }

    /**
     * 按小时聚合数据
     */
    private Map<Integer, List<Double>> aggregateByHour(List<Telemetry> data) {
        Map<Integer, List<Double>> hourlyPower = new HashMap<>();
        
        for (int i = 0; i < 24; i++) {
            hourlyPower.put(i, new ArrayList<>());
        }

        for (Telemetry t : data) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(t.getTs() * 1000);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            hourlyPower.get(hour).add(t.getPowerW());
        }

        return hourlyPower;
    }

    /**
     * 计算趋势
     */
    private double calculateTrend(List<Double> values) {
        if (values.size() < 2) return 0;
        
        double firstHalf = 0, secondHalf = 0;
        int mid = values.size() / 2;
        
        for (int i = 0; i < mid; i++) {
            firstHalf += values.get(i);
        }
        for (int i = mid; i < values.size(); i++) {
            secondHalf += values.get(i);
        }
        
        firstHalf /= mid;
        secondHalf /= (values.size() - mid);
        
        if (firstHalf == 0) return 0;
        return (secondHalf - firstHalf) / firstHalf;
    }

    /**
     * 计算置信度
     */
    private double calculateConfidence(List<Double> values) {
        if (values.isEmpty()) return 0;
        if (values.size() < 5) return 0.3;
        
        double stdDev = MLUtils.stdDev(values);
        double mean = MLUtils.mean(values);
        
        if (mean == 0) return 0.5;
        
        double cv = stdDev / mean;
        return Math.max(0.1, Math.min(0.95, 1.0 - cv * 0.5));
    }

    /**
     * 获取功率等级
     */
    private String getPowerLevel(double power, Map<Integer, Double> avgPower) {
        double overallAvg = avgPower.values().stream().mapToDouble(Double::doubleValue).average().orElse(30);
        
        if (power > overallAvg * 1.5) return "HIGH";
        if (power < overallAvg * 0.5) return "LOW";
        return "MEDIUM";
    }

    /**
     * 计算节能潜力
     */
    private double calculateSavingsPotential(PowerPrediction prediction, Map<Integer, Double> hourlyAvg) {
        double potential = 0;
        
        double avgPower = hourlyAvg.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        for (HourlyPrediction hp : prediction.hourlyPredictions) {
            if ("HIGH".equals(hp.level)) {
                potential += 0.15;
            }
            if ("LOW".equals(hp.level) && (hp.hour < 6 || hp.hour >= 23)) {
                potential += 0.1;
            }
        }
        
        return Math.min(potential, 0.5);
    }

    /**
     * 计算整体置信度
     */
    private double calculateOverallConfidence(Map<Integer, List<Double>> hourlyPower) {
        double totalConfidence = 0;
        int count = 0;
        
        for (List<Double> powers : hourlyPower.values()) {
            if (!powers.isEmpty()) {
                totalConfidence += calculateConfidence(powers);
                count++;
            }
        }
        
        return count > 0 ? totalConfidence / count : 0;
    }

    /**
     * 生成建议
     */
    private String generateRecommendation(PowerPrediction prediction) {
        if (prediction.predictedDailyKwh < 0.5) {
            return "用电量较低，无需特别优化";
        } else if (prediction.predictedDailyKwh < 1.5) {
            return "用电量适中，建议关注高峰时段";
        } else if (prediction.predictedDailyKwh < 3.0) {
            return "用电量偏高，建议开启智能节能模式";
        } else {
            return "用电量过高，强烈建议执行自动节能策略";
        }
    }
}
