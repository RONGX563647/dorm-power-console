package com.dormpower.agent;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * AI客服Agent - 意图识别模块
 * 
 * 功能：
 * 1. 识别用户意图（设备控制、查询、账单等）
 * 2. 提取关键实体（房间号、设备名、时间等）
 * 3. 决定调用本地API还是大模型
 * 
 * 优化：轻量级规则匹配，不依赖大模型
 */
@Service
public class IntentRecognizer {

    private static final Map<String, String[]> INTENT_PATTERNS = new HashMap<>();
    private static final Map<String, String[]> ENTITY_PATTERNS = new HashMap<>();

    static {
        INTENT_PATTERNS.put("DEVICE_CONTROL", new String[]{
            "开", "关", "打开", "关闭", "启动", "停止",
            "控制", "设置", "调节", "限制"
        });
        INTENT_PATTERNS.put("POWER_QUERY", new String[]{
            "用电", "电量", "功率", "耗电",
            "用了多少", "消费", "余额"
        });
        INTENT_PATTERNS.put("DEVICE_STATUS", new String[]{
            "状态", "在线", "离线", "设备", "工作",
            "运行", "正常", "异常"
        });
        INTENT_PATTERNS.put("BILL_QUERY", new String[]{
            "账单", "缴费", "欠费", "催缴", "支付",
            "费用", "结算", "电费"
        });
        INTENT_PATTERNS.put("ALARM_QUERY", new String[]{
            "告警", "报警", "异常", "故障", "警告",
            "问题", "错误"
        });
        INTENT_PATTERNS.put("SCENE_CONTROL", new String[]{
            "场景", "模式", "智能", "自动", "节能",
            "定时"
        });
        INTENT_PATTERNS.put("SYSTEM_HELP", new String[]{
            "帮助", "怎么", "如何", "功能",
            "介绍", "说明", "使用方法"
        });

        ENTITY_PATTERNS.put("ROOM", new String[]{
            "房间", "寝室", "宿舍", "室"
        });
        ENTITY_PATTERNS.put("DEVICE", new String[]{
            "空调", "灯", "插座", "排插", "风扇",
            "电视", "电脑", "冰箱", "热水器"
        });
        ENTITY_PATTERNS.put("TIME", new String[]{
            "今天", "昨天", "本周", "上周", "本月", "上月",
            "小时", "天", "周", "月"
        });
    }

    public static class IntentResult {
        public String intent = "UNKNOWN";
        public double confidence = 0.0;
        public Map<String, String> entities = new HashMap<>();
        public boolean needLLM = false;
        public String apiEndpoint;
        public String response;

        public IntentResult() {
        }
    }

    /**
     * 识别意图
     */
    public IntentResult recognize(String query) {
        IntentResult result = new IntentResult();
        String normalizedQuery = query.toLowerCase().trim();

        String matchedIntent = null;
        int maxMatches = 0;

        for (Map.Entry<String, String[]> entry : INTENT_PATTERNS.entrySet()) {
            int matches = 0;
            for (String keyword : entry.getValue()) {
                if (normalizedQuery.contains(keyword)) {
                    matches++;
                }
            }
            if (matches > maxMatches) {
                maxMatches = matches;
                matchedIntent = entry.getKey();
            }
        }

        if (matchedIntent != null && maxMatches > 0) {
            result.intent = matchedIntent;
            result.confidence = Math.min(0.5 + maxMatches * 0.15, 0.95);
            result.entities = extractEntities(normalizedQuery);
            result.needLLM = shouldUseLLM(matchedIntent, maxMatches);
            result.apiEndpoint = getApiEndpoint(matchedIntent, result.entities);
        } else {
            result.intent = "UNKNOWN";
            result.confidence = 0.0;
            result.needLLM = true;
        }

        return result;
    }

    /**
     * 提取实体
     */
    private Map<String, String> extractEntities(String query) {
        Map<String, String> entities = new HashMap<>();

        Pattern roomPattern = Pattern.compile("([A-Za-z]-?\\d{3,4})");
        var roomMatcher = roomPattern.matcher(query);
        if (roomMatcher.find()) {
            entities.put("roomId", roomMatcher.group(1).toUpperCase());
        }

        for (Map.Entry<String, String[]> entry : ENTITY_PATTERNS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (query.contains(keyword)) {
                    entities.putIfAbsent(entry.getKey(), keyword);
                    break;
                }
            }
        }

        if (query.contains("今天")) entities.put("period", "today");
        else if (query.contains("昨天")) entities.put("period", "yesterday");
        else if (query.contains("本周")) entities.put("period", "week");
        else if (query.contains("本月")) entities.put("period", "month");

        if (query.contains("开") || query.contains("打开")) {
            entities.put("action", "on");
        } else if (query.contains("关") || query.contains("关闭")) {
            entities.put("action", "off");
        }

        return entities;
    }

    /**
     * 判断是否需要大模型
     */
    private boolean shouldUseLLM(String intent, int matches) {
        if ("UNKNOWN".equals(intent)) return true;
        if ("SYSTEM_HELP".equals(intent)) return true;
        if (matches < 2) return true;
        return false;
    }

    /**
     * 获取API端点
     */
    private String getApiEndpoint(String intent, Map<String, String> entities) {
        String roomId = entities.getOrDefault("roomId", "");
        
        switch (intent) {
            case "DEVICE_CONTROL":
                return "/api/devices/control";
            case "POWER_QUERY":
                return "/api/telemetry/room/" + roomId + "/stats";
            case "DEVICE_STATUS":
                return "/api/devices/room/" + roomId;
            case "BILL_QUERY":
                return "/api/bills/room/" + roomId;
            case "ALARM_QUERY":
                return "/api/alarms";
            case "SCENE_CONTROL":
                return "/api/saving/strategies/" + roomId;
            default:
                return null;
        }
    }

    /**
     * 快速匹配 - 用于高频简单查询
     */
    public static String quickMatch(String query) {
        String q = query.toLowerCase();
        
        if (q.contains("你好") || q.contains("在吗") || q.contains("有人吗")) {
            return "您好！我是宿舍用电管理助手，有什么可以帮您？";
        }
        if (q.contains("谢谢") || q.contains("感谢")) {
            return "不客气，有其他问题随时问我！";
        }
        if (q.contains("再见") || q.contains("拜拜")) {
            return "再见！祝您生活愉快！";
        }
        
        return null;
    }
}
