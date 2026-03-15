package com.dormpower.cache.bloom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * 布隆过滤器服务
 * 
 * 管理各类数据的布隆过滤器
 * 用于防止缓存穿透
 * 
 * 使用场景：
 * 1. 设备ID查询
 * 2. 用户ID查询
 * 3. 房间ID查询
 * 
 * @author dormpower team
 * @version 1.0
 */
@Service
public class BloomFilterService {

    private static final Logger logger = LoggerFactory.getLogger(BloomFilterService.class);

    public static final String DEVICE_FILTER = "device";
    public static final String USER_FILTER = "user";
    public static final String ROOM_FILTER = "room";
    public static final String BUILDING_FILTER = "building";

    @Autowired
    private RedisBloomFilter redisBloomFilter;

    /**
     * 初始化布隆过滤器
     * 实际项目中应该从数据库加载数据初始化
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing bloom filters...");
    }

    /**
     * 添加设备ID到布隆过滤器
     */
    public void addDevice(String deviceId) {
        redisBloomFilter.put(DEVICE_FILTER, deviceId);
    }

    /**
     * 批量添加设备ID
     */
    public void addDevices(List<String> deviceIds) {
        redisBloomFilter.putAll(DEVICE_FILTER, deviceIds);
    }

    /**
     * 检查设备ID是否可能存在
     */
    public boolean mightContainDevice(String deviceId) {
        return redisBloomFilter.mightContain(DEVICE_FILTER, deviceId);
    }

    /**
     * 添加用户ID到布隆过滤器
     */
    public void addUser(String userId) {
        redisBloomFilter.put(USER_FILTER, userId);
    }

    /**
     * 批量添加用户ID
     */
    public void addUsers(List<String> userIds) {
        redisBloomFilter.putAll(USER_FILTER, userIds);
    }

    /**
     * 检查用户ID是否可能存在
     */
    public boolean mightContainUser(String userId) {
        return redisBloomFilter.mightContain(USER_FILTER, userId);
    }

    /**
     * 添加房间ID到布隆过滤器
     */
    public void addRoom(String roomId) {
        redisBloomFilter.put(ROOM_FILTER, roomId);
    }

    /**
     * 批量添加房间ID
     */
    public void addRooms(List<String> roomIds) {
        redisBloomFilter.putAll(ROOM_FILTER, roomIds);
    }

    /**
     * 检查房间ID是否可能存在
     */
    public boolean mightContainRoom(String roomId) {
        return redisBloomFilter.mightContain(ROOM_FILTER, roomId);
    }

    /**
     * 添加楼栋ID到布隆过滤器
     */
    public void addBuilding(String buildingId) {
        redisBloomFilter.put(BUILDING_FILTER, buildingId);
    }

    /**
     * 批量添加楼栋ID
     */
    public void addBuildings(List<String> buildingIds) {
        redisBloomFilter.putAll(BUILDING_FILTER, buildingIds);
    }

    /**
     * 检查楼栋ID是否可能存在
     */
    public boolean mightContainBuilding(String buildingId) {
        return redisBloomFilter.mightContain(BUILDING_FILTER, buildingId);
    }

    /**
     * 清空指定过滤器
     */
    public void clearFilter(String filterName) {
        redisBloomFilter.clear(filterName);
    }

    /**
     * 清空所有过滤器
     */
    public void clearAllFilters() {
        List<String> filters = Arrays.asList(DEVICE_FILTER, USER_FILTER, ROOM_FILTER, BUILDING_FILTER);
        for (String filter : filters) {
            redisBloomFilter.clear(filter);
        }
        logger.info("Cleared all bloom filters");
    }
}
