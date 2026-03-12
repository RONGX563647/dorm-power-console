package com.dormpower.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 响应工具类
 */
public class ResponseUtil {

    /**
     * 成功响应
     * @param data 数据
     * @return 响应Map
     */
    public static Map<String, Object> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }

    /**
     * 失败响应
     * @param code 状态码
     * @param message 消息
     * @return 响应Map
     */
    public static Map<String, Object> error(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        return response;
    }

}