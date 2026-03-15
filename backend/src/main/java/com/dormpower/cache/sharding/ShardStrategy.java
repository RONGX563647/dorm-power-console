package com.dormpower.cache.sharding;

import java.util.List;

/**
 * 分片策略接口
 * 
 * 定义缓存分片的路由规则
 * 
 * @author dormpower team
 * @version 1.0
 */
public interface ShardStrategy {

    /**
     * 获取分片Key
     * 
     * @param cacheName 缓存名称
     * @param key 原始Key
     * @return 分片Key
     */
    String getShardKey(String cacheName, Object key);

    /**
     * 获取所有分片
     * 
     * @param cacheName 缓存名称
     * @return 所有分片列表
     */
    List<String> getAllShards(String cacheName);

    /**
     * 获取分片数量
     * 
     * @return 分片数量
     */
    int getShardCount();
}
