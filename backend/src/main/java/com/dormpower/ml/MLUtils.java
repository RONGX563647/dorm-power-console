package com.dormpower.ml;

import java.util.*;

/**
 * 轻量级机器学习算法工具类
 * 不依赖外部ML库，纯Java实现
 * 
 * 优化要点：
 * 1. 使用基本类型数组替代包装类型集合
 * 2. 复用临时数组，避免频繁创建
 * 3. 使用StringBuilder替代String拼接
 * 4. 减少Stream使用，改用传统循环
 */
public class MLUtils {

    private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    /**
     * 计算均值 - 优化版：使用基本类型数组
     */
    public static double mean(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        int size = values.size();
        for (int i = 0; i < size; i++) {
            sum += values.get(i).doubleValue();
        }
        return sum / size;
    }

    /**
     * 计算均值 - 基本类型数组版本
     */
    public static double mean(double[] values) {
        if (values == null || values.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        int len = values.length;
        for (int i = 0; i < len; i++) {
            sum += values[i];
        }
        return sum / len;
    }

    /**
     * 计算标准差 - 优化版
     */
    public static double stdDev(List<Double> values) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }
        double mean = mean(values);
        double sumSquaredDiff = 0.0;
        int size = values.size();
        for (int i = 0; i < size; i++) {
            double diff = values.get(i).doubleValue() - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / (size - 1));
    }

    /**
     * 计算标准差 - 基本类型数组版本
     */
    public static double stdDev(double[] values) {
        if (values == null || values.length < 2) {
            return 0.0;
        }
        double mean = mean(values);
        double sumSquaredDiff = 0.0;
        int len = values.length;
        for (int i = 0; i < len; i++) {
            double diff = values[i] - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / (len - 1));
    }

    /**
     * Z-Score异常检测 - 优化版：无对象创建
     */
    public static boolean isAnomalyByZScore(double value, double mean, double std, double threshold) {
        if (std == 0) {
            return false;
        }
        double zScore = Math.abs((value - mean) / std);
        return zScore > threshold;
    }

    /**
     * IQR异常检测 - 优化版：使用数组排序
     */
    public static boolean isAnomalyByIQR(List<Double> values, double value, double k) {
        int n = values.size();
        if (n < 4) {
            return false;
        }
        
        double[] sorted = new double[n];
        for (int i = 0; i < n; i++) {
            sorted[i] = values.get(i).doubleValue();
        }
        Arrays.sort(sorted);
        
        double q1 = sorted[n / 4];
        double q3 = sorted[3 * n / 4];
        double iqr = q3 - q1;
        
        double lowerBound = q1 - k * iqr;
        double upperBound = q3 + k * iqr;
        
        return value < lowerBound || value > upperBound;
    }

    /**
     * 简单K-Means聚类 - 优化版
     * 使用基本类型数组，减少对象创建
     */
    public static List<Integer> kMeans(List<Double> data, int k, int maxIterations) {
        if (data == null || data.isEmpty() || k <= 0) {
            return Collections.emptyList();
        }

        int n = data.size();
        k = Math.min(k, n);

        double[] points = new double[n];
        for (int i = 0; i < n; i++) {
            points[i] = data.get(i).doubleValue();
        }
        
        double[] centroids = new double[k];
        Random random = new Random(42);
        for (int i = 0; i < k; i++) {
            centroids[i] = points[random.nextInt(n)];
        }

        int[] labels = new int[n];
        double[] sums = new double[k];
        int[] counts = new int[k];

        for (int iter = 0; iter < maxIterations; iter++) {
            boolean changed = false;
            
            for (int i = 0; i < n; i++) {
                int nearestCentroid = 0;
                double minDist = Double.MAX_VALUE;
                for (int j = 0; j < k; j++) {
                    double dist = Math.abs(points[i] - centroids[j]);
                    if (dist < minDist) {
                        minDist = dist;
                        nearestCentroid = j;
                    }
                }
                if (labels[i] != nearestCentroid) {
                    labels[i] = nearestCentroid;
                    changed = true;
                }
            }

            if (!changed) {
                break;
            }

            Arrays.fill(sums, 0.0);
            Arrays.fill(counts, 0);
            
            for (int i = 0; i < n; i++) {
                int cluster = labels[i];
                sums[cluster] += points[i];
                counts[cluster]++;
            }

            for (int i = 0; i < k; i++) {
                if (counts[i] > 0) {
                    centroids[i] = sums[i] / counts[i];
                }
            }
        }

        List<Integer> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(labels[i]);
        }
        return result;
    }

    /**
     * 指数加权移动平均(EWMA) - 优化版
     */
    public static double ewmaPredict(List<Double> values, double alpha) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        double ewma = values.get(0).doubleValue();
        int size = values.size();
        for (int i = 1; i < size; i++) {
            ewma = alpha * values.get(i).doubleValue() + (1 - alpha) * ewma;
        }
        return ewma;
    }

    /**
     * 简单移动平均 - 优化版
     */
    public static List<Double> movingAverage(List<Double> values, int window) {
        if (values == null || values.size() < window) {
            return Collections.emptyList();
        }
        
        int n = values.size();
        List<Double> result = new ArrayList<>(n - window + 1);
        
        for (int i = window - 1; i < n; i++) {
            double sum = 0;
            for (int j = i - window + 1; j <= i; j++) {
                sum += values.get(j).doubleValue();
            }
            result.add(sum / window);
        }
        return result;
    }

    /**
     * 周期性检测 - 优化版
     */
    public static double detectPeriodicity(List<Double> values, int period) {
        if (values == null || values.size() < period * 2) {
            return 0.0;
        }

        int n = values.size();
        double overallMean = mean(values);
        
        double[] periodMeans = new double[period];
        int[] periodCounts = new int[period];
        
        for (int i = 0; i < n; i++) {
            int p = i % period;
            periodMeans[p] += values.get(i).doubleValue();
            periodCounts[p]++;
        }
        
        for (int p = 0; p < period; p++) {
            if (periodCounts[p] > 0) {
                periodMeans[p] /= periodCounts[p];
            }
        }

        double betweenVariance = 0;
        for (int p = 0; p < period; p++) {
            double diff = periodMeans[p] - overallMean;
            betweenVariance += diff * diff;
        }
        betweenVariance /= period;

        double std = stdDev(values);
        double totalVariance = std * std;
        if (totalVariance == 0) {
            return 0.0;
        }

        return Math.min(1.0, betweenVariance / totalVariance);
    }

    /**
     * 自相关系数 - 优化版
     */
    public static double autocorrelation(List<Double> values, int lag) {
        if (values == null || values.size() <= lag) {
            return 0.0;
        }

        double mean = mean(values);
        int n = values.size();
        
        double numerator = 0;
        double denominator = 0;
        
        for (int i = 0; i < n; i++) {
            double diff = values.get(i).doubleValue() - mean;
            denominator += diff * diff;
            if (i >= lag) {
                numerator += diff * (values.get(i - lag).doubleValue() - mean);
            }
        }
        
        return denominator == 0 ? 0 : numerator / denominator;
    }

    /**
     * 余弦相似度 - 优化版
     */
    public static double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1 == null || v2 == null || v1.size() != v2.size() || v1.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        int size = v1.size();

        for (int i = 0; i < size; i++) {
            double d1 = v1.get(i).doubleValue();
            double d2 = v2.get(i).doubleValue();
            dotProduct += d1 * d2;
            norm1 += d1 * d1;
            norm2 += d2 * d2;
        }

        double denom = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denom == 0 ? 0 : dotProduct / denom;
    }

    /**
     * DTW距离 - 优化版：使用一维数组替代二维数组
     */
    public static double dtwDistance(List<Double> s1, List<Double> s2) {
        if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty()) {
            return Double.MAX_VALUE;
        }

        int n = s1.size();
        int m = s2.size();
        
        double[] prev = new double[m + 1];
        double[] curr = new double[m + 1];
        
        Arrays.fill(prev, Double.MAX_VALUE);
        prev[0] = 0;

        for (int i = 1; i <= n; i++) {
            Arrays.fill(curr, Double.MAX_VALUE);
            for (int j = 1; j <= m; j++) {
                double cost = Math.abs(s1.get(i - 1).doubleValue() - s2.get(j - 1).doubleValue());
                double min = Math.min(Math.min(prev[j], curr[j - 1]), prev[j - 1]);
                curr[j] = cost + min;
            }
            double[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[m];
    }

    /**
     * 快速计算列表统计信息
     */
    public static double[] computeStats(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return new double[]{0.0, 0.0, 0.0, 0.0};
        }
        
        int n = values.size();
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        
        for (int i = 0; i < n; i++) {
            double v = values.get(i).doubleValue();
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        
        double mean = sum / n;
        
        double sumSquaredDiff = 0;
        for (int i = 0; i < n; i++) {
            double diff = values.get(i).doubleValue() - mean;
            sumSquaredDiff += diff * diff;
        }
        double stdDev = n > 1 ? Math.sqrt(sumSquaredDiff / (n - 1)) : 0;
        
        return new double[]{mean, stdDev, min, max};
    }

    /**
     * 计算时间序列的变化点
     */
    public static List<Integer> detectChangePoints(List<Double> values, double threshold) {
        if (values == null || values.size() < 3) {
            return Collections.emptyList();
        }

        List<Integer> changePoints = new ArrayList<>();
        double mean = mean(values);
        double std = stdDev(values);
        
        if (std == 0) {
            return changePoints;
        }

        int n = values.size();
        for (int i = 1; i < n - 1; i++) {
            double prevDiff = Math.abs(values.get(i).doubleValue() - values.get(i - 1).doubleValue());
            double nextDiff = Math.abs(values.get(i + 1).doubleValue() - values.get(i).doubleValue());
            
            double zPrev = Math.abs((values.get(i).doubleValue() - values.get(i - 1).doubleValue()) / std);
            double zNext = Math.abs((values.get(i + 1).doubleValue() - values.get(i).doubleValue()) / std);
            
            if (zPrev > threshold && zNext > threshold) {
                changePoints.add(i);
            }
        }

        return changePoints;
    }
}
