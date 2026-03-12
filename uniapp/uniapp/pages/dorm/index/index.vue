<template>
    <view class="cyber-container">
        <view class="cyber-grid"></view>
        <view class="cyber-glow"></view>
        
        <view class="cyber-header">
            <view class="header-content">
                <view class="user-avatar">
                    <view class="avatar-ring"></view>
                    <image class="avatar-img" src="/static/logo.png" mode="aspectFill"></image>
                </view>
                <view class="user-info">
                    <text class="greeting">{{ getGreeting() }}</text>
                    <text class="username">{{ user ? user.username : 'UNIDENTIFIED' }}</text>
                </view>
            </view>
            <view class="header-actions">
                <view class="action-btn" @click="goToNotifications">
                    <uni-icons type="notification" size="22" color="#00F5FF"></uni-icons>
                    <uni-badge v-if="unreadCount > 0" :text="unreadCount" absolute="rightTop" size="small"></uni-badge>
                </view>
                <view class="action-btn" @click="goToSettings">
                    <uni-icons type="gear" size="22" color="#00F5FF"></uni-icons>
                </view>
            </view>
        </view>
        
        <view class="power-monitor">
            <view class="monitor-header">
                <view class="header-line">
                    <text class="line-label">POWER MONITORING</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="power-hologram">
                <view class="holo-circle">
                    <view class="circle-ring ring-1"></view>
                    <view class="circle-ring ring-2"></view>
                    <view class="circle-ring ring-3"></view>
                    <view class="circle-core">
                        <text class="power-value">{{ currentPower }}</text>
                        <text class="power-unit">WATTS</text>
                    </view>
                </view>
                
                <view class="power-stats">
                    <view class="stat-item">
                        <view class="stat-icon">
                            <uni-icons type="checkbox" size="18" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="stat-content">
                            <text class="stat-value">{{ onlineDevices.length }}</text>
                            <text class="stat-label">ONLINE</text>
                        </view>
                    </view>
                    
                    <view class="stat-item">
                        <view class="stat-icon">
                            <uni-icons type="list" size="18" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="stat-content">
                            <text class="stat-value">{{ devices.length }}</text>
                            <text class="stat-label">TOTAL</text>
                        </view>
                    </view>
                    
                    <view class="stat-item">
                        <view class="stat-icon">
                            <uni-icons type="fire" size="18" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="stat-content">
                            <text class="stat-value">{{ todayEnergy }}</text>
                            <text class="stat-label">TODAY kWh</text>
                        </view>
                    </view>
                </view>
            </view>
            
            <view class="power-chart">
                <view class="chart-header">
                    <text class="chart-title">POWER HISTORY</text>
                    <text class="chart-time">LAST 12 CYCLES</text>
                </view>
                <view class="chart-container">
                    <view 
                        class="chart-bar" 
                        v-for="(item, index) in powerHistory" 
                        :key="index"
                        :style="{ height: item + '%' }"
                    >
                        <view class="bar-glow"></view>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="control-center" @click="goToControl">
            <view class="control-hologram">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="control-content">
                    <view class="control-icon">
                        <view class="icon-ring"></view>
                        <uni-icons type="power" size="40" color="#00F5FF"></uni-icons>
                    </view>
                    <view class="control-text">
                        <text class="control-title">DEVICE CONTROL CENTER</text>
                        <text class="control-desc">Manage all smart sockets</text>
                    </view>
                    <view class="control-arrow">
                        <uni-icons type="forward" size="24" color="#00F5FF"></uni-icons>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="eco-monitor">
            <view class="eco-header">
                <view class="header-line">
                    <text class="line-label">ECO MONITORING</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="eco-card">
                <view class="eco-content">
                    <view class="eco-icon">
                        <view class="icon-pulse"></view>
                        <uni-icons type="heart-filled" size="28" color="#00FF88"></uni-icons>
                    </view>
                    <view class="eco-info">
                        <view class="eco-title-row">
                            <text class="eco-title">ENERGY SAVED</text>
                            <view class="eco-badge">EXCELLENT</view>
                        </view>
                        <text class="eco-desc">2.3 kWh · -1.8kg CO₂</text>
                    </view>
                </view>
                
                <view class="eco-progress">
                    <view class="progress-header">
                        <text class="progress-label">TARGET ACHIEVEMENT</text>
                        <text class="progress-value">65%</text>
                    </view>
                    <view class="progress-bar">
                        <view class="progress-fill" :style="{ width: '65%' }">
                            <view class="progress-flow"></view>
                        </view>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="quick-actions">
            <view class="actions-header">
                <view class="header-line">
                    <text class="line-label">QUICK ACCESS</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="action-grid">
                <view class="action-card" @click="goToTelemetry">
                    <view class="card-hologram">
                        <view class="holo-icon">
                            <uni-icons type="bars" size="32" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="holo-scan"></view>
                    </view>
                    <view class="card-content">
                        <text class="card-title">TELEMETRY</text>
                        <text class="card-desc">Power statistics</text>
                    </view>
                </view>
                
                <view class="action-card" @click="goToAIReport">
                    <view class="card-hologram">
                        <view class="holo-icon">
                            <uni-icons type="chatbubble" size="32" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="holo-scan"></view>
                    </view>
                    <view class="card-content">
                        <text class="card-title">AI REPORT</text>
                        <text class="card-desc">Smart analysis</text>
                    </view>
                </view>
                
                <view class="action-card" @click="goToAlerts">
                    <view class="card-hologram">
                        <view class="holo-icon">
                            <uni-icons type="info" size="32" color="#FF6B35"></uni-icons>
                        </view>
                        <view class="holo-scan"></view>
                    </view>
                    <view class="card-content">
                        <text class="card-title">ALERTS</text>
                        <text class="card-desc">{{ alerts.length }} warnings</text>
                    </view>
                </view>
                
                <view class="action-card" @click="goToDeviceList">
                    <view class="card-hologram">
                        <view class="holo-icon">
                            <uni-icons type="list" size="32" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="holo-scan"></view>
                    </view>
                    <view class="card-content">
                        <text class="card-title">DEVICES</text>
                        <text class="card-desc">Device management</text>
                    </view>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import { mapState, mapActions } from 'vuex'

export default {
    data() {
        return {
            currentPower: 1234,
            powerHistory: [20, 35, 45, 30, 50, 40, 60, 55, 45, 65, 50, 40],
            todayEnergy: '29.6'
        }
    },
    
    computed: {
        ...mapState({
            user: state => state.user,
            devices: state => state.devices || [],
            alerts: state => state.alerts || [],
            unreadCount: state => state.unreadCount || 0
        }),
        
        onlineDevices() {
            return this.devices.filter(d => d.online)
        }
    },
    
    onLoad() {
        this.loadData()
    },
    
    onShow() {
        this.refreshData()
    },
    
    methods: {
        ...mapActions(['fetchDevices', 'fetchAlerts', 'fetchUnreadCount']),
        
        getGreeting() {
            const hour = new Date().getHours()
            if (hour < 6) return 'GOOD NIGHT'
            if (hour < 12) return 'GOOD MORNING'
            if (hour < 18) return 'GOOD AFTERNOON'
            return 'GOOD EVENING'
        },
        
        async loadData() {
            await this.refreshData()
        },
        
        async refreshData() {
            try {
                // 并行获取数据
                await Promise.all([
                    this.fetchDevices(),
                    this.fetchAlerts(),
                    this.fetchUnreadCount()
                ])
                
                // 模拟当前功率数据
                this.currentPower = Math.floor(Math.random() * 1500) + 500
                this.todayEnergy = (Math.random() * 30 + 10).toFixed(1)
                this.updatePowerHistory(this.currentPower)
            } catch (e) {
                console.error('Load data failed:', e)
            }
        },
        
        updatePowerHistory(power) {
            const maxPower = 2000
            const percentage = Math.min((power / maxPower) * 100, 100)
            this.powerHistory.shift()
            this.powerHistory.push(percentage)
        },
        
        goToControl() {
            uni.switchTab({ url: '/pages/dorm/control/index' })
        },
        
        goToTelemetry() {
            uni.navigateTo({ url: '/pages/dorm/telemetry/index' })
        },
        
        goToAIReport() {
            uni.navigateTo({ url: '/pages/dorm/ai-report/index' })
        },
        
        goToAlerts() {
            uni.navigateTo({ url: '/pages/dorm/alert/list' })
        },
        
        goToDeviceList() {
            uni.navigateTo({ url: '/pages/dorm/device/detail' })
        },
        
        goToNotifications() {
            uni.navigateTo({ url: '/pages/dorm/notification/list' })
        },
        
        goToSettings() {
            uni.navigateTo({ url: '/pages/dorm/settings/index' })
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

.cyber-header {
    position: relative;
    padding: 40rpx 32rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;
    z-index: 10;
    
    .header-content {
        display: flex;
        align-items: center;
        gap: 20rpx;
        
        .user-avatar {
            position: relative;
            width: 80rpx;
            height: 80rpx;
            
            .avatar-ring {
                position: absolute;
                top: -4rpx;
                left: -4rpx;
                right: -4rpx;
                bottom: -4rpx;
                border: 2rpx solid rgba(0, 245, 255, 0.3);
                border-radius: 50%;
                animation: rotate 4s linear infinite;
            }
            
            .avatar-img {
                width: 100%;
                height: 100%;
                border-radius: 50%;
                border: 2rpx solid rgba(0, 245, 255, 0.5);
            }
        }
        
        .user-info {
            .greeting {
                display: block;
                font-size: 20rpx;
                color: rgba(0, 245, 255, 0.6);
                letter-spacing: 2rpx;
                margin-bottom: 4rpx;
            }
            
            .username {
                display: block;
                font-size: 32rpx;
                font-weight: 600;
                color: #00F5FF;
                text-shadow: 0 0 20rpx rgba(0, 245, 255, 0.5);
            }
        }
    }
    
    .header-actions {
        display: flex;
        gap: 16rpx;
        
        .action-btn {
            width: 64rpx;
            height: 64rpx;
            background: rgba(0, 245, 255, 0.05);
            border: 1rpx solid rgba(0, 245, 255, 0.2);
            border-radius: 16rpx;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.3s ease;
            
            &:active {
                background: rgba(0, 245, 255, 0.1);
                transform: scale(0.95);
            }
        }
    }
}

@keyframes rotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.power-monitor {
    position: relative;
    padding: 0 32rpx;
    margin-bottom: 32rpx;
    z-index: 10;
    
    .monitor-header {
        margin-bottom: 24rpx;
        
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
    
    .power-hologram {
        display: flex;
        gap: 24rpx;
        margin-bottom: 24rpx;
        
        .holo-circle {
            position: relative;
            width: 240rpx;
            height: 240rpx;
            flex-shrink: 0;
            
            .circle-ring {
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                border: 2rpx solid rgba(0, 245, 255, 0.3);
                border-radius: 50%;
                
                &.ring-1 {
                    width: 100%;
                    height: 100%;
                    animation: rotate 8s linear infinite;
                }
                
                &.ring-2 {
                    width: 80%;
                    height: 80%;
                    animation: rotate 6s linear infinite reverse;
                }
                
                &.ring-3 {
                    width: 60%;
                    height: 60%;
                    animation: rotate 4s linear infinite;
                }
            }
            
            .circle-core {
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                text-align: center;
                
                .power-value {
                    display: block;
                    font-size: 64rpx;
                    font-weight: 700;
                    color: #00F5FF;
                    text-shadow: 0 0 30rpx rgba(0, 245, 255, 0.8);
                }
                
                .power-unit {
                    display: block;
                    font-size: 18rpx;
                    color: rgba(0, 245, 255, 0.6);
                    letter-spacing: 4rpx;
                }
            }
        }
        
        .power-stats {
            flex: 1;
            display: flex;
            flex-direction: column;
            gap: 16rpx;
            
            .stat-item {
                display: flex;
                align-items: center;
                gap: 16rpx;
                padding: 20rpx;
                background: rgba(0, 245, 255, 0.03);
                border: 1rpx solid rgba(0, 245, 255, 0.1);
                border-radius: 16rpx;
                
                .stat-icon {
                    width: 48rpx;
                    height: 48rpx;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: rgba(0, 245, 255, 0.1);
                    border-radius: 12rpx;
                }
                
                .stat-content {
                    .stat-value {
                        display: block;
                        font-size: 32rpx;
                        font-weight: 600;
                        color: #00F5FF;
                    }
                    
                    .stat-label {
                        display: block;
                        font-size: 18rpx;
                        color: rgba(0, 245, 255, 0.6);
                        letter-spacing: 2rpx;
                    }
                }
            }
        }
    }
    
    .power-chart {
        background: rgba(0, 245, 255, 0.03);
        border: 1rpx solid rgba(0, 245, 255, 0.1);
        border-radius: 20rpx;
        padding: 24rpx;
        
        .chart-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20rpx;
            
            .chart-title {
                font-size: 22rpx;
                font-weight: 600;
                color: #00F5FF;
                letter-spacing: 2rpx;
            }
            
            .chart-time {
                font-size: 18rpx;
                color: rgba(0, 245, 255, 0.6);
                letter-spacing: 2rpx;
            }
        }
        
        .chart-container {
            display: flex;
            align-items: flex-end;
            justify-content: space-between;
            height: 160rpx;
            gap: 8rpx;
            
            .chart-bar {
                flex: 1;
                background: linear-gradient(180deg, #00F5FF 0%, rgba(0, 245, 255, 0.3) 100%);
                border-radius: 4rpx 4rpx 0 0;
                position: relative;
                transition: height 0.3s ease;
                
                .bar-glow {
                    position: absolute;
                    top: 0;
                    left: 0;
                    right: 0;
                    height: 20rpx;
                    background: #00F5FF;
                    filter: blur(10rpx);
                    opacity: 0.5;
                }
            }
        }
    }
}

.control-center {
    position: relative;
    padding: 0 32rpx;
    margin-bottom: 32rpx;
    z-index: 10;
    
    .control-hologram {
        position: relative;
        background: linear-gradient(135deg, rgba(0, 245, 255, 0.08) 0%, rgba(0, 212, 255, 0.08) 100%);
        border: 1rpx solid rgba(0, 245, 255, 0.3);
        border-radius: 24rpx;
        padding: 32rpx;
        box-shadow: 0 0 30rpx rgba(0, 245, 255, 0.2);
        
        .holo-frame {
            position: absolute;
            top: -12rpx;
            left: -12rpx;
            right: -12rpx;
            bottom: -12rpx;
            
            .frame-corner {
                position: absolute;
                width: 32rpx;
                height: 32rpx;
                border: 2rpx solid rgba(0, 245, 255, 0.5);
                
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
        
        .control-content {
            display: flex;
            align-items: center;
            gap: 24rpx;
            
            .control-icon {
                position: relative;
                width: 96rpx;
                height: 96rpx;
                background: rgba(0, 245, 255, 0.1);
                border-radius: 24rpx;
                display: flex;
                align-items: center;
                justify-content: center;
                
                .icon-ring {
                    position: absolute;
                    top: -4rpx;
                    left: -4rpx;
                    right: -4rpx;
                    bottom: -4rpx;
                    border: 2rpx solid rgba(0, 245, 255, 0.3);
                    border-radius: 28rpx;
                    animation: pulse 2s ease-out infinite;
                }
            }
            
            .control-text {
                flex: 1;
                
                .control-title {
                    display: block;
                    font-size: 32rpx;
                    font-weight: 600;
                    color: #00F5FF;
                    letter-spacing: 2rpx;
                    margin-bottom: 8rpx;
                }
                
                .control-desc {
                    display: block;
                    font-size: 22rpx;
                    color: rgba(0, 245, 255, 0.6);
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
        opacity: 0;
    }
}

.eco-monitor {
    position: relative;
    padding: 0 32rpx;
    margin-bottom: 32rpx;
    z-index: 10;
    
    .eco-header {
        margin-bottom: 24rpx;
        
        .header-line {
            display: flex;
            align-items: center;
            gap: 16rpx;
            
            .line-label {
                font-size: 24rpx;
                font-weight: 600;
                color: #00FF88;
                letter-spacing: 4rpx;
            }
            
            .line-decoration {
                flex: 1;
                height: 1rpx;
                background: linear-gradient(90deg, rgba(0, 255, 136, 0.3) 0%, transparent 100%);
            }
        }
    }
    
    .eco-card {
        background: rgba(0, 255, 136, 0.03);
        border: 1rpx solid rgba(0, 255, 136, 0.2);
        border-radius: 20rpx;
        padding: 24rpx;
        
        .eco-content {
            display: flex;
            align-items: center;
            gap: 20rpx;
            margin-bottom: 24rpx;
            
            .eco-icon {
                position: relative;
                width: 72rpx;
                height: 72rpx;
                background: rgba(0, 255, 136, 0.1);
                border-radius: 20rpx;
                display: flex;
                align-items: center;
                justify-content: center;
                
                .icon-pulse {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    border: 2rpx solid rgba(0, 255, 136, 0.3);
                    border-radius: 20rpx;
                    animation: pulse 2s ease-out infinite;
                }
            }
            
            .eco-info {
                flex: 1;
                
                .eco-title-row {
                    display: flex;
                    align-items: center;
                    gap: 12rpx;
                    margin-bottom: 8rpx;
                    
                    .eco-title {
                        font-size: 28rpx;
                        font-weight: 600;
                        color: #00FF88;
                        letter-spacing: 2rpx;
                    }
                    
                    .eco-badge {
                        padding: 4rpx 12rpx;
                        background: rgba(0, 255, 136, 0.15);
                        border: 1rpx solid rgba(0, 255, 136, 0.3);
                        border-radius: 8rpx;
                        font-size: 18rpx;
                        color: #00FF88;
                        letter-spacing: 1rpx;
                    }
                }
                
                .eco-desc {
                    font-size: 22rpx;
                    color: rgba(0, 255, 136, 0.6);
                }
            }
        }
        
        .eco-progress {
            .progress-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 12rpx;
                
                .progress-label {
                    font-size: 20rpx;
                    color: rgba(0, 255, 136, 0.6);
                    letter-spacing: 2rpx;
                }
                
                .progress-value {
                    font-size: 24rpx;
                    font-weight: 600;
                    color: #00FF88;
                }
            }
            
            .progress-bar {
                height: 12rpx;
                background: rgba(0, 255, 136, 0.1);
                border-radius: 6rpx;
                overflow: hidden;
                
                .progress-fill {
                    height: 100%;
                    background: linear-gradient(90deg, #00FF88 0%, #00D4FF 100%);
                    border-radius: 6rpx;
                    position: relative;
                    
                    .progress-flow {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        background: linear-gradient(
                            90deg,
                            transparent 0%,
                            rgba(255, 255, 255, 0.3) 50%,
                            transparent 100%
                        );
                        animation: dataFlow 2s linear infinite;
                    }
                }
            }
        }
    }
}

@keyframes dataFlow {
    0% {
        transform: translateX(-100%);
    }
    100% {
        transform: translateX(100%);
    }
}

.quick-actions {
    position: relative;
    padding: 0 32rpx;
    z-index: 10;
    
    .actions-header {
        margin-bottom: 24rpx;
        
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
    
    .action-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 20rpx;
        
        .action-card {
            background: rgba(0, 245, 255, 0.03);
            border: 1rpx solid rgba(0, 245, 255, 0.1);
            border-radius: 20rpx;
            padding: 24rpx;
            text-align: center;
            transition: all 0.3s ease;
            
            &:active {
                background: rgba(0, 245, 255, 0.08);
                transform: scale(0.95);
            }
            
            .card-hologram {
                position: relative;
                width: 80rpx;
                height: 80rpx;
                margin: 0 auto 16rpx;
                
                .holo-icon {
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    transform: translate(-50%, -50%);
                }
                
                .holo-scan {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    border: 2rpx solid rgba(0, 245, 255, 0.2);
                    border-radius: 50%;
                    animation: scan 2s ease-in-out infinite;
                }
            }
            
            .card-content {
                .card-title {
                    display: block;
                    font-size: 24rpx;
                    font-weight: 600;
                    color: #00F5FF;
                    letter-spacing: 2rpx;
                    margin-bottom: 8rpx;
                }
                
                .card-desc {
                    display: block;
                    font-size: 20rpx;
                    color: rgba(0, 245, 255, 0.6);
                }
            }
        }
    }
}

@keyframes scan {
    0%, 100% {
        transform: scale(1);
        opacity: 0.2;
    }
    50% {
        transform: scale(1.1);
        opacity: 0.4;
    }
}
</style>
