<template>
    <view class="cyber-container">
        <view class="cyber-grid"></view>
        <view class="cyber-glow"></view>
        
        <view class="monitor-header">
            <view class="header-content">
                <view class="title-wrapper">
                    <text class="title" data-text="SYSTEM MONITORING">SYSTEM MONITORING</text>
                    <view class="title-line"></view>
                </view>
                <text class="subtitle">REAL-TIME SYSTEM STATUS</text>
            </view>
        </view>
        
        <view class="status-section">
            <view class="section-header">
                <view class="header-line">
                    <text class="line-label">SYSTEM STATUS</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="status-hologram" v-if="systemStatus">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="status-content">
                    <view class="status-grid">
                        <view class="status-card">
                            <view class="status-icon" :class="systemStatus.status">
                                <uni-icons type="star" size="32" :color="systemStatus.status === 'healthy' ? '#26DE81' : '#FF4757'"></uni-icons>
                            </view>
                            <view class="status-info">
                                <text class="status-label">SYSTEM</text>
                                <text class="status-value" :class="systemStatus.status">
                                    {{ systemStatus.status === 'healthy' ? 'HEALTHY' : 'CRITICAL' }}
                                </text>
                            </view>
                        </view>
                        
                        <view class="status-card">
                            <view class="status-icon info">
                                <uni-icons type="clock" size="32" color="#00F5FF"></uni-icons>
                            </view>
                            <view class="status-info">
                                <text class="status-label">UPTIME</text>
                                <text class="status-value">{{ formatUptime(systemStatus.uptime) }}</text>
                            </view>
                        </view>
                        
                        <view class="status-card">
                            <view class="status-icon info">
                                <uni-icons type="server" size="32" color="#00F5FF"></uni-icons>
                            </view>
                            <view class="status-info">
                                <text class="status-label">CPU</text>
                                <text class="status-value">{{ systemStatus.cpu_usage }}%</text>
                            </view>
                        </view>
                        
                        <view class="status-card">
                            <view class="status-icon info">
                                <uni-icons type="cloud" size="32" color="#00F5FF"></uni-icons>
                            </view>
                            <view class="status-info">
                                <text class="status-label">MEMORY</text>
                                <text class="status-value">{{ systemStatus.memory_usage }}%</text>
                            </view>
                        </view>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="device-section">
            <view class="section-header">
                <view class="header-line">
                    <text class="line-label">DEVICE STATUS</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="device-hologram" v-if="deviceStatus">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="device-content">
                    <view class="device-stats">
                        <view class="stat-item">
                            <text class="stat-value">{{ deviceStatus.total }}</text>
                            <text class="stat-label">TOTAL DEVICES</text>
                        </view>
                        <view class="stat-divider"></view>
                        <view class="stat-item">
                            <text class="stat-value active">{{ deviceStatus.online }}</text>
                            <text class="stat-label">ONLINE</text>
                        </view>
                        <view class="stat-divider"></view>
                        <view class="stat-item">
                            <text class="stat-value inactive">{{ deviceStatus.offline }}</text>
                            <text class="stat-label">OFFLINE</text>
                        </view>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="alert-section">
            <view class="section-header">
                <view class="header-line">
                    <text class="line-label">ACTIVE ALERTS</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="alert-hologram" v-if="alerts.length > 0">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="alert-content">
                    <view class="alert-item" v-for="(alert, index) in alerts" :key="index">
                        <view class="alert-icon" :class="alert.severity">
                            <uni-icons 
                                :type="alert.severity === 'critical' ? 'close' : 'warn'" 
                                size="24" 
                                :color="alert.severity === 'critical' ? '#FF4757' : '#FED330'"
                            ></uni-icons>
                        </view>
                        <view class="alert-info">
                            <text class="alert-title">{{ alert.title }}</text>
                            <text class="alert-message">{{ alert.message }}</text>
                            <text class="alert-time">{{ formatTime(alert.timestamp) }}</text>
                        </view>
                        <button class="alert-btn" @click="acknowledgeAlert(alert.id)">
                            ACK
                        </button>
                    </view>
                </view>
            </view>
            
            <view class="empty-state" v-else>
                <view class="empty-hologram">
                    <view class="holo-circle">
                        <uni-icons type="success" size="64" color="#00F5FF"></uni-icons>
                    </view>
                </view>
                <text class="empty-title">NO ALERTS</text>
                <text class="empty-desc">No active alerts at the moment</text>
            </view>
        </view>
        
        <view class="logs-section">
            <view class="section-header">
                <view class="header-line">
                    <text class="line-label">SYSTEM LOGS</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="logs-hologram" v-if="logs.length > 0">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="logs-content">
                    <view class="log-item" v-for="(log, index) in logs" :key="index">
                        <text class="log-time">{{ formatTime(log.timestamp) }}</text>
                        <text class="log-level" :class="log.level">
                            {{ log.level.toUpperCase() }}
                        </text>
                        <text class="log-message">{{ log.message }}</text>
                    </view>
                </view>
            </view>
            
            <view class="empty-state" v-else>
                <view class="empty-hologram">
                    <view class="holo-circle">
                        <uni-icons type="document" size="64" color="#00F5FF"></uni-icons>
                    </view>
                </view>
                <text class="empty-title">NO LOGS</text>
                <text class="empty-desc">No system logs available</text>
            </view>
        </view>
    </view>
</template>

<script>
import api from '@/api'

export default {
    data() {
        return {
            systemStatus: null,
            deviceStatus: null,
            alerts: [],
            logs: [],
            loading: false
        }
    },
    onShow() {
        this.initData()
        // 定时刷新数据
        this.timer = setInterval(() => {
            this.initData()
        }, 30000) // 每30秒刷新一次
    },
    onHide() {
        if (this.timer) {
            clearInterval(this.timer)
        }
    },
    onPullDownRefresh() {
        this.initData().finally(() => {
            uni.stopPullDownRefresh()
        })
    },
    methods: {
        async initData() {
            try {
                const [system, device, alerts, logs] = await Promise.all([
                    api.monitor.getSystemStatus(),
                    api.monitor.getDeviceStatus(),
                    api.monitor.getActiveAlerts(),
                    api.monitor.getSystemLogs()
                ])
                
                this.systemStatus = system
                this.deviceStatus = device
                this.alerts = alerts
                this.logs = logs
            } catch (error) {
                console.error('加载监控数据失败:', error)
            }
        },
        
        async acknowledgeAlert(alertId) {
            try {
                await api.monitor.acknowledgeAlert(alertId)
                await this.initData()
                uni.showToast({
                    title: 'Alert acknowledged',
                    icon: 'success'
                })
            } catch (error) {
                console.error('Acknowledge alert failed:', error)
            }
        },
        
        formatUptime(seconds) {
            const days = Math.floor(seconds / (24 * 3600))
            const hours = Math.floor((seconds % (24 * 3600)) / 3600)
            const minutes = Math.floor((seconds % 3600) / 60)
            
            if (days > 0) {
                return `${days}d ${hours}h`
            } else if (hours > 0) {
                return `${hours}h ${minutes}m`
            } else {
                return `${minutes}m`
            }
        },
        
        formatTime(timestamp) {
            const date = new Date(timestamp)
            const month = date.getMonth() + 1
            const day = date.getDate()
            const hours = String(date.getHours()).padStart(2, '0')
            const minutes = String(date.getMinutes()).padStart(2, '0')
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

.monitor-header {
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

.status-section {
    position: relative;
    padding: 0 32rpx 32rpx;
    z-index: 10;
    
    .status-hologram {
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
        
        .status-content {
            .status-grid {
                display: grid;
                grid-template-columns: repeat(2, 1fr);
                gap: 16rpx;
                
                .status-card {
                    display: flex;
                    align-items: center;
                    gap: 16rpx;
                    padding: 24rpx;
                    background: rgba(0, 245, 255, 0.05);
                    border: 1rpx solid rgba(0, 245, 255, 0.2);
                    border-radius: 16rpx;
                    
                    .status-icon {
                        width: 64rpx;
                        height: 64rpx;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        border-radius: 16rpx;
                        
                        &.healthy {
                            background: rgba(38, 222, 129, 0.1);
                            border: 1rpx solid rgba(38, 222, 129, 0.3);
                        }
                        
                        &.critical {
                            background: rgba(255, 71, 87, 0.1);
                            border: 1rpx solid rgba(255, 71, 87, 0.3);
                        }
                        
                        &.info {
                            background: rgba(0, 245, 255, 0.1);
                            border: 1rpx solid rgba(0, 245, 255, 0.3);
                        }
                    }
                    
                    .status-info {
                        flex: 1;
                        
                        .status-label {
                            display: block;
                            font-size: 20rpx;
                            color: rgba(0, 245, 255, 0.6);
                            margin-bottom: 4rpx;
                        }
                        
                        .status-value {
                            font-size: 28rpx;
                            font-weight: 600;
                            color: #00F5FF;
                            
                            &.healthy {
                                color: #26DE81;
                            }
                            
                            &.critical {
                                color: #FF4757;
                            }
                        }
                    }
                }
            }
        }
    }
}

.device-section {
    position: relative;
    padding: 0 32rpx 32rpx;
    z-index: 10;
    
    .device-hologram {
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
        
        .device-content {
            .device-stats {
                display: flex;
                justify-content: space-around;
                
                .stat-item {
                    flex: 1;
                    text-align: center;
                    
                    .stat-value {
                        display: block;
                        font-size: 36rpx;
                        font-weight: 700;
                        color: #00F5FF;
                        margin-bottom: 8rpx;
                        
                        &.active {
                            color: #26DE81;
                        }
                        
                        &.inactive {
                            color: #FF4757;
                        }
                    }
                    
                    .stat-label {
                        display: block;
                        font-size: 20rpx;
                        color: rgba(0, 245, 255, 0.6);
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
}

.alert-section {
    position: relative;
    padding: 0 32rpx 32rpx;
    z-index: 10;
    
    .alert-hologram {
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
        
        .alert-content {
            .alert-item {
                display: flex;
                align-items: flex-start;
                gap: 16rpx;
                padding: 24rpx 32rpx;
                border-bottom: 1rpx solid rgba(0, 245, 255, 0.1);
                
                &:last-child {
                    border-bottom: none;
                }
                
                &:nth-child(even) {
                    background: rgba(0, 245, 255, 0.02);
                }
                
                .alert-icon {
                    width: 48rpx;
                    height: 48rpx;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border-radius: 12rpx;
                    margin-top: 4rpx;
                    
                    &.critical {
                        background: rgba(255, 71, 87, 0.1);
                        border: 1rpx solid rgba(255, 71, 87, 0.3);
                    }
                    
                    &.warning {
                        background: rgba(254, 211, 48, 0.1);
                        border: 1rpx solid rgba(254, 211, 48, 0.3);
                    }
                }
                
                .alert-info {
                    flex: 1;
                    
                    .alert-title {
                        display: block;
                        font-size: 24rpx;
                        font-weight: 600;
                        color: #00F5FF;
                        margin-bottom: 8rpx;
                    }
                    
                    .alert-message {
                        display: block;
                        font-size: 22rpx;
                        color: rgba(0, 245, 255, 0.8);
                        margin-bottom: 8rpx;
                    }
                    
                    .alert-time {
                        display: block;
                        font-size: 20rpx;
                        color: rgba(0, 245, 255, 0.6);
                    }
                }
                
                .alert-btn {
                    padding: 8rpx 16rpx;
                    background: rgba(0, 245, 255, 0.1);
                    border: 1rpx solid rgba(0, 245, 255, 0.2);
                    border-radius: 8rpx;
                    font-size: 20rpx;
                    font-weight: 600;
                    color: #00F5FF;
                    margin-top: 4rpx;
                }
            }
        }
    }
}

.logs-section {
    position: relative;
    padding: 0 32rpx;
    z-index: 10;
    
    .logs-hologram {
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
        
        .logs-content {
            .log-item {
                padding: 16rpx 32rpx;
                border-bottom: 1rpx solid rgba(0, 245, 255, 0.1);
                
                &:last-child {
                    border-bottom: none;
                }
                
                &:nth-child(even) {
                    background: rgba(0, 245, 255, 0.02);
                }
                
                .log-time {
                    display: block;
                    font-size: 20rpx;
                    color: rgba(0, 245, 255, 0.6);
                    margin-bottom: 4rpx;
                }
                
                .log-level {
                    display: inline-block;
                    font-size: 18rpx;
                    font-weight: 600;
                    padding: 2rpx 8rpx;
                    border-radius: 6rpx;
                    margin-bottom: 8rpx;
                    margin-right: 12rpx;
                    
                    &.error {
                        color: #FF4757;
                        background: rgba(255, 71, 87, 0.1);
                        border: 1rpx solid rgba(255, 71, 87, 0.3);
                    }
                    
                    &.warn {
                        color: #FED330;
                        background: rgba(254, 211, 48, 0.1);
                        border: 1rpx solid rgba(254, 211, 48, 0.3);
                    }
                    
                    &.info {
                        color: #00F5FF;
                        background: rgba(0, 245, 255, 0.1);
                        border: 1rpx solid rgba(0, 245, 255, 0.3);
                    }
                }
                
                .log-message {
                    display: block;
                    font-size: 22rpx;
                    color: rgba(0, 245, 255, 0.8);
                }
            }
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