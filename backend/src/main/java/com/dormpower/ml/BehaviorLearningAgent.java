package com.dormpower.ml;

import com.dormpower.model.Device;
import com.dormpower.model.Telemetry;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用电行为学习Agent
 * 
 * 核心功能：
 * 1. 学习用户用电习惯（时段、功率模式）
 * 2. 检测周期性规律（日/周模式）
 * 3. 识别设备使用关联
 * 4. 自动生成智能场景建议
 */
@Service
public class BehaviorLearningAgent {

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private static final int HOUR_BUCKETS = 24;
    private static final int DAY_BUCKETS = 7;
    private static final double SIMILARITY_THRESHOLD = 0.7;
    private static final int MIN_SAMPLES_FOR_LEARNING = 100;

    /**
     * 用户行为画像
     */
    public static class UserProfile {
        public String roomId;
        public Map<Integer, Double> hourlyAvgPower;
        public Map<Integer, Double> hourlyStdPower;
        public Map<Integer, Double> weeklyPattern;
        public List<UsagePattern> patterns;
        public List<DeviceAssociation> deviceAssociations;
        public double totalAnomalyScore;
        public long lastUpdated;
        public boolean hasEnoughData;
        public String message;
        public int dataPoints;
        public int deviceCount;
    }

    /**
     * 用电模式
     */
    public static class UsagePattern {
        public String patternId;
        public String name;
        public String description;
        public int startHour;
        public int endHour;
        public double avgPower;
        public double confidence;
        public List<String> typicalDevices;
        public int occurrence;
    }

    /**
     * 设备关联
     */
    public static class DeviceAssociation {
        public String deviceId1;
        public String deviceId2;
        public double correlation;
        public String description;
    }

    /**
     * 智能场景建议
     */
    public static class SceneSuggestion {
        public String sceneId;
        public String name;
        public String description;
        public String triggerType;
        public String triggerCondition;
        public List<String> actions;
        public double confidence;
        public double estimatedSaving;
    }

    /**
     * 学习房间用电行为
     */
    public UserProfile learnBehavior(String roomId, int days) {
        List<Device> devices = deviceRepository.findByRoom(roomId);
        
        UserProfile profile = new UserProfile();
        profile.roomId = roomId;
        profile.hourlyAvgPower = new HashMap<>();
        profile.hourlyStdPower = new HashMap<>();
        profile.weeklyPattern = new HashMap<>();
        profile.patterns = new ArrayList<>();
        profile.deviceAssociations = new ArrayList<>();
        profile.lastUpdated = System.currentTimeMillis() / 1000;
        profile.deviceCount = devices.size();
        profile.hasEnoughData = false;

        if (devices.isEmpty()) {
            profile.message = "该房间没有设备，请先添加设备";
            return profile;
        }

        long nowTs = System.currentTimeMillis() / 1000;
        long startTs = nowTs - days * 24 * 3600L;

        List<Telemetry> allData = new ArrayList<>();
        for (Device d : devices) {
            List<Telemetry> deviceData = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(d.getId(), startTs, nowTs);
            allData.addAll(deviceData);
        }

        profile.dataPoints = allData.size();

        if (allData.size() < MIN_SAMPLES_FOR_LEARNING) {
            profile.message = String.format("数据不足，当前%d条，需要至少%d条数据进行分析", 
                    allData.size(), MIN_SAMPLES_FOR_LEARNING);
            return profile;
        }

        profile.hasEnoughData = true;
        profile.message = "分析完成";

        Map<Integer, List<Double>> hourlyPower = aggregateByHour(allData);
        for (Map.Entry<Integer, List<Double>> entry : hourlyPower.entrySet()) {
            profile.hourlyAvgPower.put(entry.getKey(), MLUtils.mean(entry.getValue()));
            profile.hourlyStdPower.put(entry.getKey(), MLUtils.stdDev(entry.getValue()));
        }

        Map<Integer, List<Double>> weeklyPower = aggregateByDayOfWeek(allData);
        for (Map.Entry<Integer, List<Double>> entry : weeklyPower.entrySet()) {
            profile.weeklyPattern.put(entry.getKey(), MLUtils.mean(entry.getValue()));
        }

        profile.patterns = detectUsagePatterns(hourlyPower);

        profile.deviceAssociations = detectDeviceAssociations(devices, startTs, nowTs);

        profile.totalAnomalyScore = calculateAnomalyScore(profile);

        return profile;
    }

    /**
     * 生成智能场景建议
     */
    public List<SceneSuggestion> generateSceneSuggestions(String roomId, int days) {
        List<SceneSuggestion> suggestions = new ArrayList<>();
        UserProfile profile = learnBehavior(roomId, days);

        suggestions.addAll(generateTimeBasedScenes(profile));
        suggestions.addAll(generateEnergySavingScenes(profile));
        suggestions.addAll(generateSafetyScenes(profile));

        suggestions.sort((a, b) -> Double.compare(b.confidence, a.confidence));

        return suggestions.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * 按小时聚合数据
     */
    private Map<Integer, List<Double>> aggregateByHour(List<Telemetry> data) {
        Map<Integer, List<Double>> hourlyPower = new HashMap<>();
        
        for (int i = 0; i < HOUR_BUCKETS; i++) {
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
     * 按星期聚合数据
     */
    private Map<Integer, List<Double>> aggregateByDayOfWeek(List<Telemetry> data) {
        Map<Integer, List<Double>> weeklyPower = new HashMap<>();
        
        for (int i = 0; i < DAY_BUCKETS; i++) {
            weeklyPower.put(i, new ArrayList<>());
        }

        for (Telemetry t : data) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(t.getTs() * 1000);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            weeklyPower.get(dayOfWeek).add(t.getPowerW());
        }

        return weeklyPower;
    }

    /**
     * 检测用电模式
     */
    private List<UsagePattern> detectPatterns(Map<Integer, List<Double>> hourlyPower) {
        List<UsagePattern> patterns = new ArrayList<>();

        List<Double> avgPowers = new ArrayList<>();
        for (int i = 0; i < HOUR_BUCKETS; i++) {
            avgPowers.add(MLUtils.mean(hourlyPower.getOrDefault(i, new ArrayList<>())));
        }

        List<Integer> labels = MLUtils.kMeans(avgPowers, 3, 100);

        Map<Integer, List<Integer>> clusterHours = new HashMap<>();
        Map<Integer, Double> clusterAvgPower = new HashMap<>();
        
        for (int i = 0; i < labels.size(); i++) {
            int cluster = labels.get(i);
            clusterHours.computeIfAbsent(cluster, k -> new ArrayList<>()).add(i);
        }

        for (Map.Entry<Integer, List<Integer>> entry : clusterHours.entrySet()) {
            double sum = 0;
            for (int h : entry.getValue()) {
                sum += avgPowers.get(h);
            }
            clusterAvgPower.put(entry.getKey(), sum / entry.getValue().size());
        }

        List<Integer> sortedClusters = new ArrayList<>(clusterAvgPower.keySet());
        sortedClusters.sort((a, b) -> Double.compare(clusterAvgPower.get(a), clusterAvgPower.get(b)));

        Map<Integer, Integer> clusterToLevel = new HashMap<>();
        for (int i = 0; i < sortedClusters.size(); i++) {
            clusterToLevel.put(sortedClusters.get(i), i);
        }

        String[] patternNames = {"低用电时段", "中等用电时段", "高用电时段"};
        String[] patternDescs = {
            "此时段用电量较低，适合安排大功率设备充电",
            "此时段用电量适中，设备正常运行",
            "此时段用电量较高，注意用电安全"
        };

        int patternId = 0;
        for (Map.Entry<Integer, List<Integer>> entry : clusterHours.entrySet()) {
            List<Integer> hours = entry.getValue();
            if (hours.isEmpty()) continue;

            int level = clusterToLevel.get(entry.getKey());

            Collections.sort(hours);

            List<List<Integer>> consecutiveGroups = groupConsecutiveHours(hours);

            for (List<Integer> group : consecutiveGroups) {
                if (group.size() < 2) continue;

                UsagePattern pattern = new UsagePattern();
                pattern.patternId = "pattern_" + patternId++;
                pattern.startHour = group.get(0);
                pattern.endHour = group.get(group.size() - 1);
                pattern.name = patternNames[level];
                pattern.description = patternDescs[level];

                double sumPower = 0;
                for (int h : group) {
                    sumPower += avgPowers.get(h);
                }
                pattern.avgPower = sumPower / group.size();
                pattern.confidence = calculatePatternConfidence(group, hourlyPower);
                pattern.occurrence = group.size();

                patterns.add(pattern);
            }
        }

        return patterns;
    }

    /**
     * 检测用电模式（对外方法）
     */
    private List<UsagePattern> detectUsagePatterns(Map<Integer, List<Double>> hourlyPower) {
        return detectPatterns(hourlyPower);
    }

    /**
     * 将小时分组为连续时段
     */
    private List<List<Integer>> groupConsecutiveHours(List<Integer> hours) {
        List<List<Integer>> groups = new ArrayList<>();
        if (hours.isEmpty()) return groups;

        List<Integer> currentGroup = new ArrayList<>();
        currentGroup.add(hours.get(0));

        for (int i = 1; i < hours.size(); i++) {
            if (hours.get(i) == hours.get(i - 1) + 1) {
                currentGroup.add(hours.get(i));
            } else {
                groups.add(currentGroup);
                currentGroup = new ArrayList<>();
                currentGroup.add(hours.get(i));
            }
        }
        groups.add(currentGroup);

        return groups;
    }

    /**
     * 计算模式置信度
     */
    private double calculatePatternConfidence(List<Integer> hours, Map<Integer, List<Double>> hourlyPower) {
        List<Double> allPowers = new ArrayList<>();
        for (int h : hours) {
            allPowers.addAll(hourlyPower.getOrDefault(h, new ArrayList<>()));
        }

        if (allPowers.size() < 10) return 0.3;

        double stdDev = MLUtils.stdDev(allPowers);
        double mean = MLUtils.mean(allPowers);

        if (mean == 0) return 0.5;

        double cv = stdDev / mean;
        return Math.max(0.1, 1.0 - cv);
    }

    /**
     * 检测设备关联
     */
    private List<DeviceAssociation> detectDeviceAssociations(List<Device> devices, long startTs, long endTs) {
        List<DeviceAssociation> associations = new ArrayList<>();

        if (devices.size() < 2) return associations;

        Map<String, List<Double>> devicePowerSeries = new HashMap<>();
        int bucketCount = 24;

        for (Device device : devices) {
            List<Telemetry> telemetry = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(
                    device.getId(), startTs, endTs);

            double[] buckets = new double[bucketCount];
            int[] counts = new int[bucketCount];

            for (Telemetry t : telemetry) {
                int bucket = (int) ((t.getTs() - startTs) / (3600.0 * (endTs - startTs) / bucketCount));
                bucket = Math.min(bucket, bucketCount - 1);
                buckets[bucket] += t.getPowerW();
                counts[bucket]++;
            }

            List<Double> series = new ArrayList<>();
            for (int i = 0; i < bucketCount; i++) {
                series.add(counts[i] > 0 ? buckets[i] / counts[i] : 0.0);
            }
            devicePowerSeries.put(device.getId(), series);
        }

        List<String> deviceIds = new ArrayList<>(devicePowerSeries.keySet());
        for (int i = 0; i < deviceIds.size(); i++) {
            for (int j = i + 1; j < deviceIds.size(); j++) {
                String id1 = deviceIds.get(i);
                String id2 = deviceIds.get(j);

                double similarity = MLUtils.cosineSimilarity(
                        devicePowerSeries.get(id1),
                        devicePowerSeries.get(id2)
                );

                if (similarity > SIMILARITY_THRESHOLD) {
                    DeviceAssociation assoc = new DeviceAssociation();
                    assoc.deviceId1 = id1;
                    assoc.deviceId2 = id2;
                    assoc.correlation = similarity;
                    assoc.description = String.format("设备 %s 和 %s 用电模式相似度 %.1f%%",
                            id1, id2, similarity * 100);
                    associations.add(assoc);
                }
            }
        }

        return associations;
    }

    /**
     * 计算异常评分
     */
    private double calculateAnomalyScore(UserProfile profile) {
        double score = 0;

        double avgStdDev = profile.hourlyStdPower.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        double avgPower = profile.hourlyAvgPower.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(1);

        if (avgPower > 0) {
            score += Math.min(avgStdDev / avgPower, 1.0) * 0.3;
        }

        double periodicity = MLUtils.detectPeriodicity(
                new ArrayList<>(profile.hourlyAvgPower.values()), 24);
        score += (1 - periodicity) * 0.3;

        score += profile.deviceAssociations.size() * 0.05;

        return Math.min(score, 1.0);
    }

    /**
     * 生成基于时间的场景
     */
    private List<SceneSuggestion> generateTimeBasedScenes(UserProfile profile) {
        List<SceneSuggestion> scenes = new ArrayList<>();

        for (UsagePattern pattern : profile.patterns) {
            if (pattern.name.contains("低用电") && pattern.confidence > 0.2) {
                SceneSuggestion scene = new SceneSuggestion();
                scene.sceneId = "scene_time_" + System.currentTimeMillis();
                scene.name = String.format("省电模式 (%02d:00-%02d:00)",
                        pattern.startHour, pattern.endHour + 1);
                scene.description = String.format("检测到 %02d:00-%02d:00 用电量较低(%.1fW)，建议关闭非必要设备",
                        pattern.startHour, pattern.endHour + 1, pattern.avgPower);
                scene.triggerType = "TIME";
                scene.triggerCondition = String.format("%02d:00", pattern.startHour);
                scene.actions = Arrays.asList("关闭待机设备", "降低空调温度");
                scene.confidence = pattern.confidence;
                scene.estimatedSaving = pattern.avgPower * (pattern.endHour - pattern.startHour + 1) * 0.3;
                scenes.add(scene);
            }
            
            if (pattern.name.contains("高用电") && pattern.confidence > 0.5) {
                SceneSuggestion scene = new SceneSuggestion();
                scene.sceneId = "scene_high_" + System.currentTimeMillis();
                scene.name = String.format("用电高峰提醒 (%02d:00-%02d:00)",
                        pattern.startHour, pattern.endHour + 1);
                scene.description = String.format("检测到 %02d:00-%02d:00 用电量较高(%.1fW)，注意用电安全",
                        pattern.startHour, pattern.endHour + 1, pattern.avgPower);
                scene.triggerType = "TIME";
                scene.triggerCondition = String.format("%02d:00", pattern.startHour);
                scene.actions = Arrays.asList("监控用电状态", "提醒用户注意安全");
                scene.confidence = pattern.confidence;
                scene.estimatedSaving = 0;
                scenes.add(scene);
            }
        }

        return scenes;
    }

    /**
     * 生成节能场景
     */
    private List<SceneSuggestion> generateEnergySavingScenes(UserProfile profile) {
        List<SceneSuggestion> scenes = new ArrayList<>();

        double avgDailyPower = profile.hourlyAvgPower.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        if (avgDailyPower > 30) {
            SceneSuggestion scene = new SceneSuggestion();
            scene.sceneId = "scene_energy_" + System.currentTimeMillis();
            scene.name = "智能节能模式";
            scene.description = String.format("日均用电 %.1fW，建议开启智能节能模式", avgDailyPower);
            scene.triggerType = "POWER_THRESHOLD";
            scene.triggerCondition = "power > 100W for 30min";
            scene.actions = Arrays.asList("关闭高功率设备", "通知用户", "记录用电日志");
            scene.confidence = 0.75;
            scene.estimatedSaving = avgDailyPower * 24 * 0.15;
            scenes.add(scene);
        }

        return scenes;
    }

    /**
     * 生成安全场景
     */
    private List<SceneSuggestion> generateSafetyScenes(UserProfile profile) {
        List<SceneSuggestion> scenes = new ArrayList<>();

        Double nightPower = profile.hourlyAvgPower.getOrDefault(2, 0.0);
        Double dayPower = profile.hourlyAvgPower.getOrDefault(14, 0.0);

        if (nightPower != null && dayPower != null && nightPower > dayPower * 0.8) {
            SceneSuggestion scene = new SceneSuggestion();
            scene.sceneId = "scene_safety_" + System.currentTimeMillis();
            scene.name = "夜间安全模式";
            scene.description = "检测到深夜用电量较高，建议开启夜间安全监控";
            scene.triggerType = "TIME";
            scene.triggerCondition = "23:00-06:00";
            scene.actions = Arrays.asList("开启异常检测", "降低待机功耗", "发送安全报告");
            scene.confidence = 0.7;
            scene.estimatedSaving = 0;
            scenes.add(scene);
        }

        return scenes;
    }

    /**
     * 创建空画像
     */
    private UserProfile createEmptyProfile(String roomId) {
        UserProfile profile = new UserProfile();
        profile.roomId = roomId;
        profile.hourlyAvgPower = new HashMap<>();
        profile.hourlyStdPower = new HashMap<>();
        profile.weeklyPattern = new HashMap<>();
        profile.patterns = new ArrayList<>();
        profile.deviceAssociations = new ArrayList<>();
        profile.totalAnomalyScore = 0;
        profile.lastUpdated = System.currentTimeMillis() / 1000;
        profile.hasEnoughData = false;
        profile.message = "无数据";
        profile.dataPoints = 0;
        profile.deviceCount = 0;
        return profile;
    }

    /**
     * 比较两个时间段的用电相似度
     */
    public double comparePeriods(String roomId, long start1, long end1, long start2, long end2) {
        List<Device> devices = deviceRepository.findByRoom(roomId);
        if (devices.isEmpty()) return 0;

        List<Double> series1 = extractPowerSeries(devices, start1, end1);
        List<Double> series2 = extractPowerSeries(devices, start2, end2);

        if (series1.isEmpty() || series2.isEmpty()) return 0;

        return MLUtils.cosineSimilarity(series1, series2);
    }

    /**
     * 提取功率序列
     */
    private List<Double> extractPowerSeries(List<Device> devices, long startTs, long endTs) {
        List<Double> series = new ArrayList<>();
        int buckets = 24;
        double[] bucketSums = new double[buckets];
        int[] bucketCounts = new int[buckets];

        for (Device device : devices) {
            List<Telemetry> telemetry = telemetryRepository.findByDeviceIdAndTsBetweenOrderByTsAsc(
                    device.getId(), startTs, endTs);

            for (Telemetry t : telemetry) {
                int bucket = (int) ((t.getTs() - startTs) * buckets / (endTs - startTs));
                bucket = Math.max(0, Math.min(bucket, buckets - 1));
                bucketSums[bucket] += t.getPowerW();
                bucketCounts[bucket]++;
            }
        }

        for (int i = 0; i < buckets; i++) {
            series.add(bucketCounts[i] > 0 ? bucketSums[i] / bucketCounts[i] : 0.0);
        }

        return series;
    }

    /**
     * 预测未来用电
     */
    public Map<String, Object> predictUsage(String roomId, int hoursAhead) {
        UserProfile profile = learnBehavior(roomId, 7);

        if (profile.hourlyAvgPower.isEmpty()) {
            return Map.of("success", false, "message", "数据不足");
        }

        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);

        List<Double> predictions = new ArrayList<>();
        List<Double> historicalAvg = new ArrayList<>();

        for (int i = 0; i < hoursAhead; i++) {
            int targetHour = (currentHour + i) % 24;
            Double avgPower = profile.hourlyAvgPower.getOrDefault(targetHour, 0.0);
            predictions.add(avgPower);
            historicalAvg.add(avgPower);
        }

        double predictedTotal = predictions.stream().mapToDouble(Double::doubleValue).sum();

        return Map.of(
                "success", true,
                "predictions", predictions,
                "hoursAhead", hoursAhead,
                "predictedTotalKwh", predictedTotal * hoursAhead / 1000.0,
                "confidence", 1.0 - profile.totalAnomalyScore
        );
    }
}
