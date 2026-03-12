<template>
    <view class="ai-report-container">
        <view class="filter-section">
            <picker 
                mode="selector" 
                :range="roomOptions" 
                range-key="room"
                @change="onRoomChange"
            >
                <view class="picker-value">
                    {{ selectedRoom || '选择房间' }}
                    <uni-icons type="arrowdown" size="16" color="#666"></uni-icons>
                </view>
            </picker>
            
            <view class="period-tabs">
                <view 
                    class="period-tab" 
                    :class="{ active: period === '7d' }"
                    @click="period = '7d'"
                >本周</view>
                <view 
                    class="period-tab" 
                    :class="{ active: period === '30d' }"
                    @click="period = '30d'"
                >本月</view>
            </view>
        </view>
        
        <view class="report-content" v-if="report">
            <view class="summary-card">
                <view class="summary-header">
                    <uni-icons type="chatbubble-filled" size="24" color="#2A7965"></uni-icons>
                    <text class="summary-title">AI 分析摘要</text>
                </view>
                <text class="summary-text">{{ report.summary }}</text>
            </view>
            
            <view class="stats-section">
                <view class="stat-card">
                    <text class="stat-value">{{ report.power_stats?.avg_power_w || 0 }}</text>
                    <text class="stat-label">平均功率(W)</text>
                </view>
                <view class="stat-card">
                    <text class="stat-value highlight">{{ report.power_stats?.peak_power_w || 0 }}</text>
                    <text class="stat-label">峰值功率(W)</text>
                </view>
                <view class="stat-card">
                    <text class="stat-value">{{ report.power_stats?.total_kwh || 0 }}</text>
                    <text class="stat-label">总用电量(度)</text>
                </view>
            </view>
            
            <view class="section-card" v-if="report.anomalies && report.anomalies.length > 0">
                <view class="section-header">
                    <uni-icons type="info-filled" size="20" color="#1F5C4D"></uni-icons>
                    <text class="section-title">异常检测</text>
                </view>
                <view class="section-content">
                    <view class="list-item" v-for="(item, index) in report.anomalies" :key="index">
                        <text class="item-dot"></text>
                        <text class="item-text">{{ item }}</text>
                    </view>
                </view>
            </view>
            
            <view class="section-card" v-if="report.recommendations && report.recommendations.length > 0">
                <view class="section-header">
                    <uni-icons type="flag-filled" size="20" color="#2A7965"></uni-icons>
                    <text class="section-title">节能建议</text>
                </view>
                <view class="section-content">
                    <view class="list-item" v-for="(item, index) in report.recommendations" :key="index">
                        <text class="item-dot green"></text>
                        <text class="item-text">{{ item }}</text>
                    </view>
                </view>
            </view>
            
            <view class="peak-info" v-if="report.power_stats?.peak_time">
                <text class="peak-label">峰值时间</text>
                <text class="peak-time">{{ formatTime(report.power_stats.peak_time) }}</text>
            </view>
        </view>
        
        <view class="empty-state" v-else-if="!loading">
            <uni-icons type="chatbubble" size="64" color="#ccc"></uni-icons>
            <text class="empty-text">请选择房间查看AI报告</text>
        </view>
        
        <view class="loading-state" v-if="loading">
            <uni-load-more status="loading" />
        </view>
    </view>
</template>

<script>
import api from '@/api'
import { mapState, mapActions } from 'vuex'

export default {
    data() {
        return {
            roomId: '',
            selectedRoom: '',
            period: '7d',
            report: null,
            loading: false
        }
    },
    computed: {
        ...mapState(['devices']),
        roomOptions() {
            const rooms = [...new Set(this.devices.map(d => d.room))]
            return rooms.map(room => ({ room }))
        }
    },
    onLoad(options) {
        if (options.roomId) {
            this.selectedRoom = options.roomId
            this.roomId = options.roomId
        }
    },
    onShow() {
        this.initData()
    },
    watch: {
        period() {
            this.loadReport()
        }
    },
    methods: {
        ...mapActions(['fetchDevices']),
        
        async initData() {
            if (this.devices.length === 0) {
                await this.fetchDevices()
            }
            
            if (this.roomId) {
                this.loadReport()
            }
        },
        
        async loadReport() {
            if (!this.roomId) return
            
            this.loading = true
            try {
                this.report = await api.ai.getAIRoomReport(this.roomId, {
                    period: this.period
                })
            } catch (error) {
                console.error('加载报告失败:', error)
            } finally {
                this.loading = false
            }
        },
        
        onRoomChange(e) {
            const index = e.detail.value
            this.selectedRoom = this.roomOptions[index].room
            this.roomId = this.selectedRoom
            this.loadReport()
        },
        
        formatTime(timestamp) {
            const date = new Date(timestamp)
            return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
        }
    }
}
</script>

<style lang="scss" scoped>
.ai-report-container {
    min-height: 100vh;
    background: #F2F7F5;
    padding-bottom: 40rpx;
}

.filter-section {
    background: #fff;
    padding: 20rpx 30rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .picker-value {
        display: flex;
        align-items: center;
        font-size: 30rpx;
        color: #333;
    }
    
    .period-tabs {
        display: flex;
        
        .period-tab {
            padding: 12rpx 30rpx;
            font-size: 26rpx;
            color: #666;
            border: 1rpx solid #ddd;
            margin-right: -1rpx;
            
            &:first-child {
                border-radius: 8rpx 0 0 8rpx;
            }
            
            &:last-child {
                border-radius: 0 8rpx 8rpx 0;
            }
            
            &.active {
                color: #fff;
                background: #2A7965;
                border-color: #2A7965;
            }
        }
    }
}

.report-content {
    padding: 20rpx 30rpx;
}

.summary-card {
    background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
    border-radius: 16rpx;
    padding: 30rpx;
    margin-bottom: 30rpx;
    
    .summary-header {
        display: flex;
        align-items: center;
        margin-bottom: 20rpx;
        
        .summary-title {
            font-size: 30rpx;
            font-weight: bold;
            color: #fff;
            margin-left: 10rpx;
        }
    }
    
    .summary-text {
        font-size: 28rpx;
        color: rgba(255, 255, 255, 0.9);
        line-height: 1.6;
    }
}

.stats-section {
    display: flex;
    margin-bottom: 30rpx;
    
    .stat-card {
        flex: 1;
        background: #fff;
        border-radius: 16rpx;
        padding: 30rpx 20rpx;
        text-align: center;
        margin-right: 20rpx;
        
        &:last-child {
            margin-right: 0;
        }
        
        .stat-value {
            display: block;
            font-size: 40rpx;
            font-weight: bold;
            color: #333;
            margin-bottom: 10rpx;
            
            &.highlight {
                color: #2A7965;
            }
        }
        
        .stat-label {
            font-size: 22rpx;
            color: #999;
        }
    }
}

.section-card {
    background: #fff;
    border-radius: 16rpx;
    padding: 30rpx;
    margin-bottom: 30rpx;
    
    .section-header {
        display: flex;
        align-items: center;
        margin-bottom: 20rpx;
        
        .section-title {
            font-size: 30rpx;
            font-weight: bold;
            color: #333;
            margin-left: 10rpx;
        }
    }
    
    .section-content {
        .list-item {
            display: flex;
            align-items: flex-start;
            margin-bottom: 16rpx;
            
            &:last-child {
                margin-bottom: 0;
            }
            
            .item-dot {
                width: 12rpx;
                height: 12rpx;
                border-radius: 50%;
                background: #1F5C4D;
                margin-top: 12rpx;
                margin-right: 16rpx;
                
                &.green {
                    background: #2A7965;
                }
            }
            
            .item-text {
                flex: 1;
                font-size: 28rpx;
                color: #666;
                line-height: 1.5;
            }
        }
    }
}

.peak-info {
    background: #fff;
    border-radius: 16rpx;
    padding: 30rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .peak-label {
        font-size: 28rpx;
        color: #666;
    }
    
    .peak-time {
        font-size: 28rpx;
        color: #2A7965;
        font-weight: bold;
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

.loading-state {
    padding: 60rpx 0;
}
</style>
