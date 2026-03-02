<template>
    <view class="telemetry-container">
        <view class="filter-section">
            <picker 
                mode="selector" 
                :range="deviceOptions" 
                range-key="name"
                @change="onDeviceChange"
            >
                <view class="picker-value">
                    {{ selectedDevice ? selectedDevice.name : '选择设备' }}
                    <uni-icons type="arrowdown" size="16" color="#666"></uni-icons>
                </view>
            </picker>
            
            <view class="range-tabs">
                <view 
                    class="range-tab" 
                    :class="{ active: range === '60s' }"
                    @click="range = '60s'"
                >1分钟</view>
                <view 
                    class="range-tab" 
                    :class="{ active: range === '24h' }"
                    @click="range = '24h'"
                >24小时</view>
                <view 
                    class="range-tab" 
                    :class="{ active: range === '7d' }"
                    @click="range = '7d'"
                >7天</view>
                <view 
                    class="range-tab" 
                    :class="{ active: range === '30d' }"
                    @click="range = '30d'"
                >30天</view>
            </view>
        </view>
        
        <view class="stats-card" v-if="statistics">
            <view class="stat-item">
                <text class="stat-label">平均功率</text>
                <text class="stat-value">{{ statistics.avg_power_w || 0 }}W</text>
            </view>
            <view class="stat-divider"></view>
            <view class="stat-item">
                <text class="stat-label">峰值功率</text>
                <text class="stat-value highlight">{{ statistics.peak_power_w || 0 }}W</text>
            </view>
            <view class="stat-divider"></view>
            <view class="stat-item">
                <text class="stat-label">总用电量</text>
                <text class="stat-value">{{ statistics.total_kwh || 0 }}度</text>
            </view>
        </view>
        
        <view class="chart-section">
            <view class="section-header">
                <text class="section-title">功率曲线</text>
            </view>
            
            <view class="chart-container" v-if="telemetryData.length > 0">
                <qiun-data-charts 
                    type="line" 
                    :chartData="chartData"
                    :opts="chartOpts"
                />
            </view>
            
            <view class="empty-state" v-else>
                <uni-icons type="bars" size="64" color="#ccc"></uni-icons>
                <text class="empty-text">暂无数据</text>
            </view>
        </view>
        
        <view class="data-list">
            <view class="section-header">
                <text class="section-title">数据详情</text>
            </view>
            
            <view class="data-table" v-if="telemetryData.length > 0">
                <view class="table-header">
                    <text class="table-cell">时间</text>
                    <text class="table-cell">功率(W)</text>
                </view>
                <view class="table-body">
                    <view class="table-row" v-for="(item, index) in telemetryData" :key="index">
                        <text class="table-cell">{{ formatTime(item.ts) }}</text>
                        <text class="table-cell">{{ item.power_w }}</text>
                    </view>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import api from '@/api'
import { mapState } from 'vuex'

export default {
    data() {
        return {
            deviceId: '',
            selectedDevice: null,
            range: '24h',
            telemetryData: [],
            statistics: null,
            loading: false,
            chartOpts: {
                color: ['#2A7965'],
                padding: [15, 15, 0, 5],
                enableScroll: false,
                legend: {},
                xAxis: {
                    disableGrid: true
                },
                yAxis: {
                    data: [{ min: 0 }]
                },
                extra: {
                    line: {
                        type: 'curve',
                        width: 2,
                        activeType: 'hollow'
                    }
                }
            }
        }
    },
    computed: {
        ...mapState(['devices']),
        deviceOptions() {
            return this.devices
        },
        chartData() {
            const categories = this.telemetryData.map(item => this.formatTime(item.ts))
            const series = [{
                name: '功率',
                data: this.telemetryData.map(item => item.power_w)
            }]
            
            return {
                categories,
                series
            }
        }
    },
    onLoad(options) {
        if (options.deviceId) {
            this.deviceId = options.deviceId
        }
    },
    onShow() {
        this.initData()
    },
    onPullDownRefresh() {
        this.loadData().finally(() => {
            uni.stopPullDownRefresh()
        })
    },
    watch: {
        range() {
            this.loadData()
        }
    },
    methods: {
        async initData() {
            if (this.devices.length === 0) {
                await this.$store.dispatch('fetchDevices')
            }
            
            if (this.deviceId) {
                this.selectedDevice = this.devices.find(d => d.id === this.deviceId)
            } else if (this.devices.length > 0) {
                this.selectedDevice = this.devices[0]
                this.deviceId = this.selectedDevice.id
            }
            
            this.loadData()
        },
        
        async loadData() {
            if (!this.deviceId) return
            
            this.loading = true
            try {
                const [telemetry, stats] = await Promise.all([
                    api.telemetry.getTelemetry({
                        device: this.deviceId,
                        range: this.range
                    }),
                    api.telemetry.getStatistics({
                        device: this.deviceId,
                        range: this.range
                    })
                ])
                
                this.telemetryData = telemetry
                this.statistics = stats
            } catch (error) {
                console.error('加载数据失败:', error)
            } finally {
                this.loading = false
            }
        },
        
        onDeviceChange(e) {
            const index = e.detail.value
            this.selectedDevice = this.devices[index]
            this.deviceId = this.selectedDevice.id
            this.loadData()
        },
        
        formatTime(timestamp) {
            const date = new Date(timestamp)
            const month = date.getMonth() + 1
            const day = date.getDate()
            const hours = String(date.getHours()).padStart(2, '0')
            const minutes = String(date.getMinutes()).padStart(2, '0')
            
            if (this.range === '60s') {
                return `${hours}:${minutes}`
            }
            return `${month}/${day} ${hours}:${minutes}`
        }
    }
}
</script>

<style lang="scss" scoped>
.telemetry-container {
    min-height: 100vh;
    background: #F2F7F5;
    padding-bottom: 40rpx;
}

.filter-section {
    background: #fff;
    padding: 20rpx 30rpx;
    
    .picker-value {
        display: flex;
        align-items: center;
        font-size: 30rpx;
        color: #2A7965;
        margin-bottom: 20rpx;
    }
    
    .range-tabs {
        display: flex;
        
        .range-tab {
            flex: 1;
            text-align: center;
            padding: 16rpx 0;
            font-size: 26rpx;
            color: #66A392;
            border: 1rpx solid #E8F5F0;
            margin-right: -1rpx;
            
            &:first-child {
                border-radius: 8rpx 0 0 8rpx;
            }
            
            &:last-child {
                border-radius: 0 8rpx 8rpx 0;
            }
            
            &.active {
                color: #fff;
                background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
                border-color: #2A7965;
            }
        }
    }
}

.stats-card {
    background: #fff;
    margin: 20rpx 30rpx;
    border-radius: 20rpx;
    padding: 30rpx;
    display: flex;
    justify-content: space-around;
    
    .stat-item {
        text-align: center;
        
        .stat-label {
            display: block;
            font-size: 24rpx;
            color: #66A392;
            margin-bottom: 10rpx;
        }
        
        .stat-value {
            font-size: 32rpx;
            font-weight: bold;
            color: #2A7965;
            
            &.highlight {
                color: #3D967E;
            }
        }
    }
    
    .stat-divider {
        width: 1rpx;
        background: #E8F5F0;
    }
}

.section-header {
    padding: 30rpx;
    
    .section-title {
        font-size: 32rpx;
        font-weight: bold;
        color: #2A7965;
    }
}

.chart-section {
    background: #fff;
    margin: 0 30rpx 30rpx;
    border-radius: 20rpx;
    
    .chart-container {
        padding: 20rpx;
        height: 500rpx;
    }
}

.empty-state {
    text-align: center;
    padding: 80rpx 0;
    
    .empty-text {
        display: block;
        margin-top: 20rpx;
        font-size: 28rpx;
        color: #66A392;
    }
}

.data-list {
    background: #fff;
    margin: 0 30rpx;
    border-radius: 20rpx;
    
    .data-table {
        .table-header {
            display: flex;
            background: #F2F7F5;
            padding: 20rpx 30rpx;
            
            .table-cell {
                flex: 1;
                font-size: 26rpx;
                color: #66A392;
                font-weight: bold;
            }
        }
        
        .table-body {
            max-height: 600rpx;
            overflow-y: auto;
            
            .table-row {
                display: flex;
                padding: 20rpx 30rpx;
                border-bottom: 1rpx solid #E8F5F0;
                
                &:last-child {
                    border-bottom: none;
                }
                
                .table-cell {
                    flex: 1;
                    font-size: 26rpx;
                    color: #2A7965;
                }
            }
        }
    }
}
</style>
