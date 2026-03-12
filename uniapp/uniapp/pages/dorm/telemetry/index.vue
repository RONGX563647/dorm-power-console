<template>
    <view class="cyber-container">
        <view class="cyber-grid"></view>
        <view class="cyber-glow"></view>
        
        <view class="telemetry-header">
            <view class="header-content">
                <view class="title-wrapper">
                    <text class="title" data-text="TELEMETRY ANALYSIS">TELEMETRY ANALYSIS</text>
                    <view class="title-line"></view>
                </view>
                <text class="subtitle">POWER DATA VISUALIZATION</text>
            </view>
        </view>
        
        <view class="filter-section">
            <view class="filter-hologram">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="filter-content">
                    <view class="device-selector">
                        <picker 
                            mode="selector" 
                            :range="deviceOptions" 
                            range-key="name"
                            @change="onDeviceChange"
                        >
                            <view class="picker-value">
                                <uni-icons type="home" size="20" color="#00F5FF"></uni-icons>
                                <text class="picker-text">{{ selectedDevice ? selectedDevice.name : 'SELECT DEVICE' }}</text>
                                <uni-icons type="arrowdown" size="16" color="#00F5FF"></uni-icons>
                            </view>
                        </picker>
                    </view>
                    
                    <view class="range-tabs">
                        <view 
                            class="range-tab" 
                            :class="{ active: range === '60s' }"
                            @click="range = '60s'"
                        >1 MIN</view>
                        <view 
                            class="range-tab" 
                            :class="{ active: range === '24h' }"
                            @click="range = '24h'"
                        >24 HRS</view>
                        <view 
                            class="range-tab" 
                            :class="{ active: range === '7d' }"
                            @click="range = '7d'"
                        >7 DAYS</view>
                        <view 
                            class="range-tab" 
                            :class="{ active: range === '30d' }"
                            @click="range = '30d'"
                        >30 DAYS</view>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="stats-card" v-if="statistics">
            <view class="card-hologram">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="stats-content">
                    <view class="stat-item">
                        <view class="stat-icon">
                            <uni-icons type="trending-up" size="24" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="stat-info">
                            <text class="stat-label">AVERAGE POWER</text>
                            <text class="stat-value">{{ statistics.avg_power_w || 0 }}W</text>
                        </view>
                    </view>
                    
                    <view class="stat-divider"></view>
                    
                    <view class="stat-item">
                        <view class="stat-icon">
                            <uni-icons type="flag" size="24" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="stat-info">
                            <text class="stat-label">PEAK POWER</text>
                            <text class="stat-value highlight">{{ statistics.peak_power_w || 0 }}W</text>
                        </view>
                    </view>
                    
                    <view class="stat-divider"></view>
                    
                    <view class="stat-item">
                        <view class="stat-icon">
                            <uni-icons type="calculator" size="24" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="stat-info">
                            <text class="stat-label">TOTAL ENERGY</text>
                            <text class="stat-value">{{ statistics.total_kwh || 0 }} kWh</text>
                        </view>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="chart-section">
            <view class="section-header">
                <view class="header-line">
                    <text class="line-label">POWER CURVE</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="chart-hologram" v-if="telemetryData.length > 0">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="chart-container">
                    <u-charts 
                        id="powerChart" 
                        canvas2d="true"
                        :option="chartOptions"
                        :loading="loading"
                        height="500rpx"
                    />
                </view>
            </view>
            
            <view class="empty-state" v-else>
                <view class="empty-hologram">
                    <view class="holo-circle">
                        <uni-icons type="bars" size="64" color="#00F5FF"></uni-icons>
                    </view>
                </view>
                <text class="empty-title">NO DATA AVAILABLE</text>
                <text class="empty-desc">Please select a device and time range</text>
            </view>
        </view>
        
        <view class="data-list">
            <view class="section-header">
                <view class="header-line">
                    <text class="line-label">DATA DETAILS</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="data-hologram" v-if="telemetryData.length > 0">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="data-table">
                    <view class="table-header">
                        <text class="table-cell">TIMESTAMP</text>
                        <text class="table-cell">POWER (W)</text>
                    </view>
                    <view class="table-body">
                        <view class="table-row" v-for="(item, index) in telemetryData" :key="index">
                            <text class="table-cell">{{ formatTime(item.timestamp || item.ts) }}</text>
                            <text class="table-cell">{{ item.power || item.power_w }}</text>
                        </view>
                    </view>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import api from '@/api'
import { mapState, mapActions } from 'vuex'

export default {
    data() {
        return {
            deviceId: '',
            selectedDevice: null,
            range: '24h',
            telemetryData: [],
            statistics: null,
            loading: false
        }
    },
    computed: {
        ...mapState(['devices']),
        deviceOptions() {
            return this.devices
        },
        chartOptions() {
            const categories = this.telemetryData.map(item => this.formatTime(item.timestamp || item.ts))
            const series = [{
                name: 'Power',
                type: 'line',
                data: this.telemetryData.map(item => item.power || item.power_w),
                smooth: true,
                lineStyle: {
                    width: 3,
                    color: '#00F5FF'
                },
                areaStyle: {
                    color: {
                        type: 'linear',
                        x: 0,
                        y: 0,
                        x2: 0,
                        y2: 1,
                        colorStops: [{
                            offset: 0,
                            color: 'rgba(0, 245, 255, 0.3)'
                        }, {
                            offset: 1,
                            color: 'rgba(0, 245, 255, 0.05)'
                        }]
                    }
                },
                itemStyle: {
                    color: '#00F5FF'
                }
            }]
            
            return {
                tooltip: {
                    trigger: 'axis',
                    backgroundColor: 'rgba(10, 14, 39, 0.8)',
                    borderColor: 'rgba(0, 245, 255, 0.3)',
                    textStyle: {
                        color: '#00F5FF'
                    }
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    containLabel: true
                },
                xAxis: {
                    type: 'category',
                    boundaryGap: false,
                    data: categories,
                    axisLine: {
                        lineStyle: {
                            color: 'rgba(0, 245, 255, 0.3)'
                        }
                    },
                    axisLabel: {
                        color: 'rgba(0, 245, 255, 0.6)',
                        fontSize: 12
                    }
                },
                yAxis: {
                    type: 'value',
                    axisLine: {
                        lineStyle: {
                            color: 'rgba(0, 245, 255, 0.3)'
                        }
                    },
                    axisLabel: {
                        color: 'rgba(0, 245, 255, 0.6)'
                    },
                    splitLine: {
                        lineStyle: {
                            color: 'rgba(0, 245, 255, 0.1)'
                        }
                    }
                },
                series: series
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
        ...mapActions(['fetchDevices']),
        
        async initData() {
            if (this.devices.length === 0) {
                await this.fetchDevices()
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
                uni.showToast({
                    title: '加载数据失败',
                    icon: 'none'
                })
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
.cyber-container {
    min-height: 100vh;
    background: linear-gradient(180deg, #0A0E27 0%, #0F1629 50%, #0A0E27 100%);
    position: relative;
    overflow: hidden;
    padding-bottom: 120rpx;
}

.cyber-grid {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: 
        linear-gradient(rgba(0, 245, 255, 0.03) 1px, transparent 1px),
        linear-gradient(90deg, rgba(0, 245, 255, 0.03) 1px, transparent 1px);
    background-size: 40rpx 40rpx;
    animation: gridMove 20s linear infinite;
    pointer-events: none;
}

@keyframes gridMove {
    0% { transform: translateY(0); }
    100% { transform: translateY(40rpx); }
}

.cyber-glow {
    position: absolute;
    top: -200rpx;
    left: 50%;
    transform: translateX(-50%);
    width: 800rpx;
    height: 800rpx;
    background: radial-gradient(circle, rgba(0, 245, 255, 0.15) 0%, transparent 70%);
    pointer-events: none;
}

.telemetry-header {
    position: relative;
    padding: 40rpx 32rpx;
    z-index: 10;
    
    .header-content {
        .title-wrapper {
            position: relative;
            margin-bottom: 16rpx;
            
            .title {
                font-size: 48rpx;
                font-weight: 700;
                color: #00F5FF;
                letter-spacing: 4rpx;
                text-shadow: 0 0 20rpx rgba(0, 245, 255, 0.5);
            }
            
            .title-line {
                position: absolute;
                bottom: -12rpx;
                left: 0;
                width: 80%;
                height: 2rpx;
                background: linear-gradient(90deg, #00F5FF 0%, transparent 100%);
            }
        }
        
        .subtitle {
            font-size: 22rpx;
            color: rgba(0, 245, 255, 0.6);
            letter-spacing: 4rpx;
        }
    }
}

.filter-section {
    position: relative;
    padding: 0 32rpx 32rpx;
    z-index: 10;
    
    .filter-hologram {
        position: relative;
        background: rgba(0, 245, 255, 0.03);
        border: 1rpx solid rgba(0, 245, 255, 0.1);
        border-radius: 20rpx;
        padding: 24rpx;
        
        .holo-frame {
            position: absolute;
            top: -12rpx;
            left: -12rpx;
            right: -12rpx;
            bottom: -12rpx;
            
            .frame-corner {
                position: absolute;
                width: 24rpx;
                height: 24rpx;
                border: 2rpx solid rgba(0, 245, 255, 0.3);
                
                &.corner-tl {
                    top: 0;
                    left: 0;
                    border-right: none;
                    border-bottom: none;
                }
                
                &.corner-tr {
                    top: 0;
                    right: 0;
                    border-left: none;
                    border-bottom: none;
                }
                
                &.corner-bl {
                    bottom: 0;
                    left: 0;
                    border-right: none;
                    border-top: none;
                }
                
                &.corner-br {
                    bottom: 0;
                    right: 0;
                    border-left: none;
                    border-top: none;
                }
            }
        }
        
        .filter-content {
            .device-selector {
                margin-bottom: 24rpx;
                
                .picker-value {
                    display: flex;
                    align-items: center;
                    gap: 16rpx;
                    padding: 20rpx;
                    background: rgba(0, 245, 255, 0.05);
                    border: 1rpx solid rgba(0, 245, 255, 0.2);
                    border-radius: 16rpx;
                    
                    .picker-text {
                        flex: 1;
                        font-size: 28rpx;
                        color: #00F5FF;
                        letter-spacing: 2rpx;
                    }
                }
            }
            
            .range-tabs {
                display: flex;
                gap: 12rpx;
                
                .range-tab {
                    flex: 1;
                    text-align: center;
                    padding: 16rpx 0;
                    font-size: 24rpx;
                    color: rgba(0, 245, 255, 0.6);
                    background: rgba(0, 245, 255, 0.05);
                    border: 1rpx solid rgba(0, 245, 255, 0.2);
                    border-radius: 12rpx;
                    transition: all 0.3s ease;
                    
                    &.active {
                        color: #00F5FF;
                        background: rgba(0, 245, 255, 0.1);
                        border-color: rgba(0, 245, 255, 0.4);
                        box-shadow: 0 0 20rpx rgba(0, 245, 255, 0.2);
                    }
                    
                    &:active:not(.active) {
                        background: rgba(0, 245, 255, 0.08);
                    }
                }
            }
        }
    }
}

.stats-card {
    position: relative;
    padding: 0 32rpx 32rpx;
    z-index: 10;
    
    .card-hologram {
        position: relative;
        background: rgba(0, 245, 255, 0.03);
        border: 1rpx solid rgba(0, 245, 255, 0.1);
        border-radius: 20rpx;
        padding: 32rpx;
        
        .holo-frame {
            position: absolute;
            top: -12rpx;
            left: -12rpx;
            right: -12rpx;
            bottom: -12rpx;
            
            .frame-corner {
                position: absolute;
                width: 24rpx;
                height: 24rpx;
                border: 2rpx solid rgba(0, 245, 255, 0.3);
                
                &.corner-tl {
                    top: 0;
                    left: 0;
                    border-right: none;
                    border-bottom: none;
                }
                
                &.corner-tr {
                    top: 0;
                    right: 0;
                    border-left: none;
                    border-bottom: none;
                }
                
                &.corner-bl {
                    bottom: 0;
                    left: 0;
                    border-right: none;
                    border-top: none;
                }
                
                &.corner-br {
                    bottom: 0;
                    right: 0;
                    border-left: none;
                    border-top: none;
                }
            }
        }
        
        .stats-content {
            display: flex;
            justify-content: space-around;
            
            .stat-item {
                flex: 1;
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 16rpx;
                
                .stat-icon {
                    width: 64rpx;
                    height: 64rpx;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: rgba(0, 245, 255, 0.1);
                    border: 1rpx solid rgba(0, 245, 255, 0.2);
                    border-radius: 16rpx;
                }
                
                .stat-info {
                    text-align: center;
                    
                    .stat-label {
                        display: block;
                        font-size: 20rpx;
                        color: rgba(0, 245, 255, 0.6);
                        letter-spacing: 2rpx;
                        margin-bottom: 8rpx;
                    }
                    
                    .stat-value {
                        font-size: 32rpx;
                        font-weight: 600;
                        color: #00F5FF;
                        
                        &.highlight {
                            text-shadow: 0 0 20rpx rgba(0, 245, 255, 0.8);
                        }
                    }
                }
            }
            
            .stat-divider {
                width: 1rpx;
                background: rgba(0, 245, 255, 0.2);
                margin: 0 16rpx;
            }
        }
    }
}

.section-header {
    position: relative;
    padding: 32rpx 32rpx 16rpx;
    z-index: 10;
    
    .header-line {
        display: flex;
        align-items: center;
        gap: 16rpx;
        
        .line-label {
            font-size: 24rpx;
            font-weight: 600;
            color: #00F5FF;
            letter-spacing: 4rpx;
        }
        
        .line-decoration {
            flex: 1;
            height: 1rpx;
            background: linear-gradient(90deg, rgba(0, 245, 255, 0.3) 0%, transparent 100%);
        }
    }
}

.chart-section {
    position: relative;
    padding: 0 32rpx 32rpx;
    z-index: 10;
    
    .chart-hologram {
        position: relative;
        background: rgba(0, 245, 255, 0.03);
        border: 1rpx solid rgba(0, 245, 255, 0.1);
        border-radius: 20rpx;
        padding: 24rpx;
        
        .holo-frame {
            position: absolute;
            top: -12rpx;
            left: -12rpx;
            right: -12rpx;
            bottom: -12rpx;
            
            .frame-corner {
                position: absolute;
                width: 24rpx;
                height: 24rpx;
                border: 2rpx solid rgba(0, 245, 255, 0.3);
                
                &.corner-tl {
                    top: 0;
                    left: 0;
                    border-right: none;
                    border-bottom: none;
                }
                
                &.corner-tr {
                    top: 0;
                    right: 0;
                    border-left: none;
                    border-bottom: none;
                }
                
                &.corner-bl {
                    bottom: 0;
                    left: 0;
                    border-right: none;
                    border-top: none;
                }
                
                &.corner-br {
                    bottom: 0;
                    right: 0;
                    border-left: none;
                    border-top: none;
                }
            }
        }
        
        .chart-container {
            height: 500rpx;
        }
    }
}

.empty-state {
    position: relative;
    padding: 80rpx 32rpx;
    text-align: center;
    z-index: 10;
    
    .empty-hologram {
        margin-bottom: 32rpx;
        
        .holo-circle {
            position: relative;
            width: 128rpx;
            height: 128rpx;
            margin: 0 auto;
            border: 2rpx solid rgba(0, 245, 255, 0.3);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            animation: pulse 2s ease-out infinite;
        }
    }
    
    .empty-title {
        display: block;
        font-size: 28rpx;
        font-weight: 600;
        color: #00F5FF;
        letter-spacing: 4rpx;
        margin-bottom: 16rpx;
    }
    
    .empty-desc {
        display: block;
        font-size: 22rpx;
        color: rgba(0, 245, 255, 0.6);
    }
}

.data-list {
    position: relative;
    padding: 0 32rpx;
    z-index: 10;
    
    .data-hologram {
        position: relative;
        background: rgba(0, 245, 255, 0.03);
        border: 1rpx solid rgba(0, 245, 255, 0.1);
        border-radius: 20rpx;
        
        .holo-frame {
            position: absolute;
            top: -12rpx;
            left: -12rpx;
            right: -12rpx;
            bottom: -12rpx;
            
            .frame-corner {
                position: absolute;
                width: 24rpx;
                height: 24rpx;
                border: 2rpx solid rgba(0, 245, 255, 0.3);
                
                &.corner-tl {
                    top: 0;
                    left: 0;
                    border-right: none;
                    border-bottom: none;
                }
                
                &.corner-tr {
                    top: 0;
                    right: 0;
                    border-left: none;
                    border-bottom: none;
                }
                
                &.corner-bl {
                    bottom: 0;
                    left: 0;
                    border-right: none;
                    border-top: none;
                }
                
                &.corner-br {
                    bottom: 0;
                    right: 0;
                    border-left: none;
                    border-top: none;
                }
            }
        }
        
        .data-table {
            .table-header {
                display: flex;
                background: rgba(0, 245, 255, 0.05);
                padding: 20rpx 32rpx;
                border-bottom: 1rpx solid rgba(0, 245, 255, 0.1);
                
                .table-cell {
                    flex: 1;
                    font-size: 24rpx;
                    font-weight: 600;
                    color: #00F5FF;
                    letter-spacing: 2rpx;
                }
            }
            
            .table-body {
                max-height: 600rpx;
                overflow-y: auto;
                
                .table-row {
                    display: flex;
                    padding: 20rpx 32rpx;
                    border-bottom: 1rpx solid rgba(0, 245, 255, 0.1);
                    
                    &:last-child {
                        border-bottom: none;
                    }
                    
                    &:nth-child(even) {
                        background: rgba(0, 245, 255, 0.02);
                    }
                    
                    .table-cell {
                        flex: 1;
                        font-size: 24rpx;
                        color: rgba(0, 245, 255, 0.8);
                    }
                }
            }
        }
    }
}

@keyframes pulse {
    0% {
        transform: scale(1);
        opacity: 1;
    }
    100% {
        transform: scale(1.1);
        opacity: 0.7;
    }
}
</style>
