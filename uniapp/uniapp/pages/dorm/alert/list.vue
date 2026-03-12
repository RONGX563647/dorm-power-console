<template>
    <view class="alert-container">
        <view class="filter-bar">
            <view 
                class="filter-item" 
                :class="{ active: filterStatus === 'all' }"
                @click="filterStatus = 'all'"
            >全部</view>
            <view 
                class="filter-item" 
                :class="{ active: filterStatus === 'unresolved' }"
                @click="filterStatus = 'unresolved'"
            >未解决</view>
            <view 
                class="filter-item" 
                :class="{ active: filterStatus === 'resolved' }"
                @click="filterStatus = 'resolved'"
            >已解决</view>
        </view>
        
        <scroll-view 
            scroll-y 
            class="alert-scroll"
            refresher-enabled
            :refresher-triggered="refreshing"
            @refresherrefresh="onRefresh"
        >
            <view class="alert-list" v-if="filteredAlerts.length > 0">
                <view 
                    class="alert-item" 
                    :class="{ resolved: item.resolved }"
                    v-for="item in filteredAlerts" 
                    :key="item.id"
                >
                    <view class="alert-header">
                        <view class="alert-level" :class="item.level">
                            {{ getLevelText(item.level) }}
                        </view>
                        <text class="alert-time">{{ formatTime(item.timestamp) }}</text>
                    </view>
                    
                    <view class="alert-content">
                        <text class="alert-device">{{ item.deviceName }}</text>
                        <text class="alert-message">{{ item.message }}</text>
                    </view>
                    
                    <view class="alert-actions" v-if="!item.resolved">
                        <button class="resolve-btn" @click="resolveAlert(item)">标记已解决</button>
                    </view>
                </view>
            </view>
            
            <view class="empty-state" v-else>
                <uni-icons type="info" size="64" color="#ccc"></uni-icons>
                <text class="empty-text">暂无告警</text>
            </view>
        </scroll-view>
    </view>
</template>

<script>
import api from '@/api'
import { mapState, mapActions } from 'vuex'

export default {
    data() {
        return {
            deviceId: '',
            filterStatus: 'all',
            refreshing: false,
            alerts: []
        }
    },
    computed: {
        filteredAlerts() {
            if (this.filterStatus === 'unresolved') {
                return this.alerts.filter(a => !a.resolved)
            } else if (this.filterStatus === 'resolved') {
                return this.alerts.filter(a => a.resolved)
            }
            return this.alerts
        }
    },
    onLoad(options) {
        if (options.deviceId) {
            this.deviceId = options.deviceId
        }
    },
    onShow() {
        this.loadAlerts()
    },
    methods: {
        ...mapActions(['fetchAlerts']),
        
        async loadAlerts() {
            try {
                if (this.deviceId) {
                    this.alerts = await api.alert.getDeviceAlerts(this.deviceId)
                } else {
                    this.alerts = await api.alert.getUnresolvedAlerts()
                }
            } catch (error) {
                console.error('加载告警失败:', error)
            }
        },
        
        async onRefresh() {
            this.refreshing = true
            await this.loadAlerts()
            this.refreshing = false
        },
        
        async resolveAlert(alert) {
            try {
                // 调用API解决告警
                alert.resolved = true
                
                uni.showToast({
                    title: '已标记为已解决',
                    icon: 'success'
                })
            } catch (error) {
                console.error('解决告警失败:', error)
            }
        },
        
        getLevelText(level) {
            const map = {
                'critical': '严重',
                'warning': '警告',
                'info': '提示'
            }
            return map[level] || level
        },
        
        formatTime(timestamp) {
            const date = new Date(timestamp)
            return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
        }
    }
}
</script>

<style lang="scss" scoped>
.alert-container {
    min-height: 100vh;
    background: #F2F7F5;
    display: flex;
    flex-direction: column;
}

.filter-bar {
    display: flex;
    background: #fff;
    padding: 20rpx 30rpx;
    border-bottom: 1rpx solid #eee;
    
    .filter-item {
        padding: 12rpx 30rpx;
        margin-right: 20rpx;
        font-size: 26rpx;
        color: #666;
        border-radius: 30rpx;
        background: #F2F7F5;
        
        &.active {
            color: #fff;
            background: #2A7965;
        }
    }
}

.alert-scroll {
    flex: 1;
    height: 0;
    padding: 20rpx;
}

.alert-list {
    .alert-item {
        background: #fff;
        border-radius: 16rpx;
        padding: 30rpx;
        margin-bottom: 20rpx;
        
        &.resolved {
            opacity: 0.6;
        }
        
        .alert-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20rpx;
            
            .alert-level {
                font-size: 24rpx;
                padding: 4rpx 16rpx;
                border-radius: 20rpx;
                
                &.critical {
                    color: #fff;
                    background: #1F5C4D;
                }
                
                &.warning {
                    color: #fff;
                    background: #3D967E;
                }
                
                &.info {
                    color: #fff;
                    background: #2A7965;
                }
            }
            
            .alert-time {
                font-size: 24rpx;
                color: #999;
            }
        }
        
        .alert-content {
            .alert-device {
                display: block;
                font-size: 28rpx;
                color: #333;
                font-weight: bold;
                margin-bottom: 8rpx;
            }
            
            .alert-message {
                font-size: 26rpx;
                color: #666;
                line-height: 1.4;
            }
        }
        
        .alert-actions {
            margin-top: 20rpx;
            padding-top: 20rpx;
            border-top: 1rpx solid #F2F7F5;
            
            .resolve-btn {
                font-size: 26rpx;
                color: #2A7965;
                background: rgba(102, 126, 234, 0.1);
                border: none;
                padding: 12rpx 30rpx;
                border-radius: 30rpx;
            }
        }
    }
}

.empty-state {
    text-align: center;
    padding: 100rpx 0;
    
    .empty-text {
        display: block;
        margin-top: 20rpx;
        font-size: 28rpx;
        color: #999;
    }
}
</style>
