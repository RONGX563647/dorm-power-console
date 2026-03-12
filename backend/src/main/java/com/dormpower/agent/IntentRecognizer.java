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
        // 基础意图类型
        INTENT_PATTERNS.put("DEVICE_CONTROL", new String[]{
            "开", "关", "打开", "关闭", "启动", "停止",
            "控制", "设置", "调节", "限制", "开启", "关闭",
            "打开", "关掉", "启动", "停止", "切换", "翻转"
        });
        INTENT_PATTERNS.put("POWER_QUERY", new String[]{
            "用电", "电量", "功率", "耗电", "用电量", "电度",
            "用了多少", "消费", "余额", "耗电情况", "用电统计",
            "电费", "电力", "能耗"
        });
        INTENT_PATTERNS.put("DEVICE_STATUS", new String[]{
            "状态", "在线", "离线", "设备", "工作", "运行",
            "正常", "异常", "情况", "状态如何", "是否在线",
            "是否工作", "运行状态", "设备状态"
        });
        INTENT_PATTERNS.put("BILL_QUERY", new String[]{
            "账单", "缴费", "欠费", "催缴", "支付", "费用",
            "结算", "电费", "账单查询", "缴费情况", "欠费情况",
            "支付记录", "缴费记录", "账单详情"
        });
        INTENT_PATTERNS.put("ALARM_QUERY", new String[]{
            "告警", "报警", "异常", "故障", "警告", "问题",
            "错误", "警报", "提醒", "通知", "警告信息",
            "告警记录", "报警记录", "异常情况"
        });
        INTENT_PATTERNS.put("SCENE_CONTROL", new String[]{
            "场景", "模式", "智能", "自动", "节能", "定时",
            "场景控制", "模式切换", "智能场景", "自动模式",
            "节能模式", "定时模式", "场景设置"
        });
        INTENT_PATTERNS.put("SYSTEM_HELP", new String[]{
            "帮助", "怎么", "如何", "功能", "介绍", "说明",
            "使用方法", "帮助信息", "功能介绍", "使用指南",
            "系统帮助", "如何使用", "功能说明"
        });
        
        // 新增意图类型
        INTENT_PATTERNS.put("ENERGY_SAVING", new String[]{
            "节能", "省电", "节能建议", "省电技巧", "能耗优化",
            "节能策略", "省电模式", "节能方案", "能耗降低",
            "节能措施", "省电方法", "节能提示"
        });
        INTENT_PATTERNS.put("ROOM_MANAGEMENT", new String[]{
            "房间", "宿舍", "寝室", "房间信息", "宿舍管理",
            "寝室信息", "房间状态", "宿舍状态", "寝室管理"
        });
        INTENT_PATTERNS.put("STUDENT_MANAGEMENT", new String[]{
            "学生", "入住", "退宿", "学生信息", "入住记录",
            "退宿记录", "学生管理", "住宿管理"
        });
        INTENT_PATTERNS.put("POWER_CONTROL", new String[]{
            "断电", "供电", "恢复供电", "断电记录", "供电状态",
            "断电原因", "恢复供电", "断电管理"
        });
        INTENT_PATTERNS.put("FIRMWARE_UPGRADE", new String[]{
            "固件", "升级", "更新", "固件升级", "固件更新",
            "版本", "升级状态", "固件版本"
        });

        // 实体类型
        ENTITY_PATTERNS.put("ROOM", new String[]{
            "房间", "寝室", "宿舍", "室", "房间号", "寝室号",
            "宿舍号", "房间编号", "寝室编号", "宿舍编号"
        });
        ENTITY_PATTERNS.put("DEVICE", new String[]{
            "空调", "灯", "插座", "排插", "风扇", "电视",
            "电脑", "冰箱", "热水器", "设备", "电器", "家电",
            "照明", "空调设备", "照明设备", "插座设备"
        });
        ENTITY_PATTERNS.put("TIME", new String[]{
            "今天", "昨天", "本周", "上周", "本月", "上月",
            "小时", "天", "周", "月", "年", "现在", "当前",
            "最近", "过去", "未来", "明天", "后天", "下周",
            "下个月", "上半年", "下半年", "季度", "半年度", "年度"
        });
        ENTITY_PATTERNS.put("ACTION", new String[]{
            "开", "关", "打开", "关闭", "启动", "停止",
            "调节", "设置", "控制", "切换", "翻转", "开启",
            "关掉", "启动", "停止", "暂停", "恢复"
        });
        ENTITY_PATTERNS.put("SCENE", new String[]{
            "场景", "模式", "智能", "自动", "节能", "定时",
            "睡眠", "离家", "回家", "起床", "睡前", "早晨",
            "晚上", "白天", "工作", "学习", "娱乐", "休息"
        });
        ENTITY_PATTERNS.put("POWER", new String[]{
            "电量", "功率", "耗电", "用电量", "电度", "电力",
            "能耗", "电能", "电流", "电压", "电功率", "电能量"
        });
        ENTITY_PATTERNS.put("BILL", new String[]{
            "账单", "缴费", "欠费", "费用", "电费", "账单金额",
            "缴费金额", "欠费金额", "费用明细", "账单详情"
        });
        ENTITY_PATTERNS.put("ALARM", new String[]{
            "告警", "报警", "异常", "故障", "警告", "问题",
            "错误", "警报", "提醒", "通知", "警告信息", "告警信息"
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

        // 提取房间ID
        Pattern roomPattern = Pattern.compile("([A-Za-z]-?\\d{3,4})");
        var roomMatcher = roomPattern.matcher(query);
        if (roomMatcher.find()) {
            entities.put("roomId", roomMatcher.group(1).toUpperCase());
        }

        // 提取设备ID
        Pattern devicePattern = Pattern.compile("(device_\\w+)");
        var deviceMatcher = devicePattern.matcher(query);
        if (deviceMatcher.find()) {
            entities.put("deviceId", deviceMatcher.group(1));
        }

        // 提取数字（如功率、时间等）
        Pattern numberPattern = Pattern.compile("(\\d+(\\.\\d+)?)");
        var numberMatcher = numberPattern.matcher(query);
        if (numberMatcher.find()) {
            entities.put("number", numberMatcher.group(1));
        }

        // 提取时间段
        if (query.contains("今天")) entities.put("period", "today");
        else if (query.contains("昨天")) entities.put("period", "yesterday");
        else if (query.contains("本周")) entities.put("period", "week");
        else if (query.contains("上周")) entities.put("period", "lastWeek");
        else if (query.contains("本月")) entities.put("period", "month");
        else if (query.contains("上月")) entities.put("period", "lastMonth");
        else if (query.contains("最近")) entities.put("period", "recent");
        else if (query.contains("过去")) entities.put("period", "past");

        // 提取操作类型
        if (query.contains("开") || query.contains("打开") || query.contains("开启")) {
            entities.put("action", "on");
        } else if (query.contains("关") || query.contains("关闭") || query.contains("关掉")) {
            entities.put("action", "off");
        } else if (query.contains("调节") || query.contains("设置")) {
            entities.put("action", "adjust");
        } else if (query.contains("查询") || query.contains("查看") || query.contains("检查")) {
            entities.put("action", "query");
        }

        // 提取其他实体
        for (Map.Entry<String, String[]> entry : ENTITY_PATTERNS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (query.contains(keyword)) {
                    entities.putIfAbsent(entry.getKey(), keyword);
                }
            }
        }

        // 提取场景类型
        if (query.contains("睡眠") || query.contains("睡前")) {
            entities.put("scene", "sleep");
        } else if (query.contains("离家") || query.contains("外出")) {
            entities.put("scene", "away");
        } else if (query.contains("回家") || query.contains("到家")) {
            entities.put("scene", "home");
        } else if (query.contains("节能") || query.contains("省电")) {
            entities.put("scene", "energySaving");
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
        String deviceId = entities.getOrDefault("deviceId", "");
        
        // 使用 switch 表达式简化代码，箭头语法更简洁
        return switch (intent) {
            case "DEVICE_CONTROL" -> "/api/devices/control";
            case "POWER_QUERY" -> roomId.isEmpty() ? null : "/api/telemetry/room/" + roomId + "/stats";
            case "DEVICE_STATUS" -> roomId.isEmpty() ? null : "/api/devices/room/" + roomId;
            case "BILL_QUERY" -> roomId.isEmpty() ? null : "/api/bills/room/" + roomId;
            case "ALARM_QUERY" -> "/api/alarms";
            case "SCENE_CONTROL" -> roomId.isEmpty() ? null : "/api/saving/strategies/" + roomId;
            case "ENERGY_SAVING" -> roomId.isEmpty() ? null : "/api/saving/strategies/" + roomId;
            case "ROOM_MANAGEMENT" -> roomId.isEmpty() ? "/api/dorm/rooms" : "/api/dorm/rooms/" + roomId;
            case "STUDENT_MANAGEMENT" -> roomId.isEmpty() ? "/api/students" : "/api/students/room/" + roomId;
            case "POWER_CONTROL" -> roomId.isEmpty() ? "/api/power-control/cutoff-rooms" : "/api/power-control/status/" + roomId;
            case "FIRMWARE_UPGRADE" -> deviceId.isEmpty() ? "/api/firmware/pending" : "/api/firmware/device/" + deviceId;
            default -> null;
        };
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
