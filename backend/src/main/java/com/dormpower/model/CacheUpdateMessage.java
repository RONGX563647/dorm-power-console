package com.dormpower.model;

/**
 * 缓存更新消息
 * 
 * 用于Kafka消息队列异步更新缓存
 * 支持多节点部署下的缓存一致性
 * 
 * @author dormpower team
 * @version 2.0
 */
public class CacheUpdateMessage {

    private String cacheName;
    private String key;
    private Object value;
    private String operation;
    private long timestamp;
    
    private String nodeId;
    private boolean broadcast;

    public CacheUpdateMessage() {
        this.timestamp = System.currentTimeMillis();
        this.broadcast = true;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    @Override
    public String toString() {
        return "CacheUpdateMessage{" +
                "cacheName='" + cacheName + '\'' +
                ", key='" + key + '\'' +
                ", operation='" + operation + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", broadcast=" + broadcast +
                ", timestamp=" + timestamp +
                '}';
    }
}
