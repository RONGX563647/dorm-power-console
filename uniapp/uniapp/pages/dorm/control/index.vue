<template>
    <view class="control-container">
        <view class="bg-grid"></view>
        <view class="bg-glow"></view>
        
        <view class="control-header">
            <view class="header-content">
                <view class="header-left">
                    <view class="title-wrapper">
                        <text class="title">设备控制中心</text>
                        <view class="title-line"></view>
                    </view>
                    <text class="subtitle">DEVICE CONTROL CENTER</text>
                </view>
                <view class="header-right">
                    <view class="status-indicator">
                        <view class="indicator-ring">
                            <view class="ring-inner"></view>
                            <view class="ring-pulse"></view>
                        </view>
                        <view class="status-info">
                            <text class="status-label">SYSTEM STATUS</text>
                            <text class="status-value">{{ deviceStatus.online ? 'ONLINE' : 'OFFLINE' }}</text>
                        </view>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="device-selector">
            <scroll-view scroll-x class="device-scroll" show-scrollbar="false">
                <view 
                    class="device-card" 
                    :class="{ active: currentDevice && currentDevice.id === device.id }"
                    v-for="device in devices" 
                    :key="device.id"
                    @click="selectDevice(device)"
                >
                    <view class="device-hologram">
                        <view class="holo-ring"></view>
                        <view class="holo-core">
                            <uni-icons type="home" size="24" :color="currentDevice && currentDevice.id === device.id ? '#00F5FF' : '#2A7965'"></uni-icons>
                        </view>
                    </view>
                    <view class="device-info">
                        <text class="device-name">{{ device.name }}</text>
                        <view class="device-status">
                            <view class="status-dot" :class="{ online: device.online }"></view>
                            <text class="status-text">{{ device.online ? 'ONLINE' : 'OFFLINE' }}</text>
                        </view>
                    </view>
                </view>
            </scroll-view>
        </view>
        
        <view class="power-monitor" v-if="currentDevice">
            <view class="monitor-header">
                <view class="header-line">
                    <text class="line-label">POWER MONITORING</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="power-display">
                <view class="power-hologram">
                    <view class="holo-circle">
                        <view class="circle-ring ring-1"></view>
                        <view class="circle-ring ring-2"></view>
                        <view class="circle-ring ring-3"></view>
                        <view class="circle-core">
                            <text class="power-value">{{ deviceStatus.total_power_w || 0 }}</text>
                            <text class="power-unit">WATTS</text>
                        </view>
                    </view>
                </view>
                
                <view class="power-stats">
                    <view class="stat-item">
                        <view class="stat-icon">
                            <uni-icons type="checkbox" size="20" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="stat-content">
                            <text class="stat-value">{{ activeSocketsCount }}</text>
                            <text class="stat-label">ACTIVE SOCKETS</text>
                        </view>
                    </view>
                    
                    <view class="stat-item">
                        <view class="stat-icon">
                            <uni-icons type="list" size="20" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="stat-content">
                            <text class="stat-value">{{ totalSocketsCount }}</text>
                            <text class="stat-label">TOTAL SOCKETS</text>
                        </view>
                    </view>
                    
                    <view class="stat-item">
                        <view class="stat-icon">
                            <uni-icons type="fire" size="20" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="stat-content">
                            <text class="stat-value">{{ estimatedDailyEnergy }}</text>
                            <text class="stat-label">DAILY kWh</text>
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
        
        <view class="socket-control" v-if="currentDevice">
            <view class="control-header">
                <view class="header-line">
                    <text class="line-label">SOCKET CONTROL</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="socket-grid">
                <view 
                    class="socket-card" 
                    :class="{ active: status, disabled: !deviceStatus.online }"
                    v-for="(status, index) in socket_status || [false, false, false, false]" 
                    :key="index"
                >
                    <view class="socket-hologram">
                        <view class="holo-frame">
                            <view class="frame-corner corner-tl"></view>
                            <view class="frame-corner corner-tr"></view>
                            <view class="frame-corner corner-bl"></view>
                            <view class="frame-corner corner-br"></view>
                        </view>
                        
                        <view class="socket-content">
                            <view class="socket-header">
                                <text class="socket-id">SOCKET-{{ String(index + 1).padStart(2, '0') }}</text>
                                <view class="socket-status" :class="{ active: status }">
                                    <view class="status-indicator"></view>
                                    <text class="status-text">{{ status ? 'ON' : 'OFF' }}</text>
                                </view>
                            </view>
                            
                            <view class="socket-power" v-if="status">
                                <text class="power-value">{{ getSocketPower(index) }}</text>
                                <text class="power-unit">W</text>
                            </view>
                            
                            <view class="socket-switch">
                                <view 
                                    class="switch-track" 
                                    :class="{ active: status }"
                                    @click="toggleSocket(index + 1, !status)"
                                >
                                    <view class="switch-thumb"></view>
                                    <view class="switch-glow"></view>
                                </view>
                            </view>
                            
                            <view class="socket-actions">
                                <button 
                                    class="action-btn on" 
                                    :disabled="!deviceStatus.online || controlling || status"
                                    @click.stop="turnOn(index + 1)"
                                >
                                    <text class="btn-text">ACTIVATE</text>
                                </button>
                                <button 
                                    class="action-btn off" 
                                    :disabled="!deviceStatus.online || controlling || !status"
                                    @click.stop="turnOff(index + 1)"
                                >
                                    <text class="btn-text">DEACTIVATE</text>
                                </button>
                            </view>
                        </view>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="quick-actions" v-if="currentDevice">
            <view class="action-grid">
                <view class="action-card" @click="allOn">
                    <view class="card-hologram">
                        <view class="holo-icon">
                            <uni-icons type="checkbox-filled" size="32" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="holo-scan"></view>
                    </view>
                    <view class="card-content">
                        <text class="card-title">ACTIVATE ALL</text>
                        <text class="card-desc">Enable all sockets</text>
                    </view>
                </view>
                
                <view class="action-card" @click="allOff">
                    <view class="card-hologram">
                        <view class="holo-icon">
                            <uni-icons type="closeempty" size="32" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="holo-scan"></view>
                    </view>
                    <view class="card-content">
                        <text class="card-title">DEACTIVATE ALL</text>
                        <text class="card-desc">Disable all sockets</text>
                    </view>
                </view>
                
                <view class="action-card" @click="refreshStatus">
                    <view class="card-hologram">
                        <view class="holo-icon">
                            <uni-icons type="refresh" size="32" color="#00F5FF"></uni-icons>
                        </view>
                        <view class="holo-scan"></view>
                    </view>
                    <view class="card-content">
                        <text class="card-title">SYNC DATA</text>
                        <text class="card-desc">Refresh status</text>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="empty-state" v-if="!currentDevice && devices.length === 0">
            <view class="empty-hologram">
                <view class="holo-circle">
                    <uni-icons type="info" size="64" color="#00F5FF"></uni-icons>
                </view>
            </view>
            <text class="empty-title">NO DEVICES DETECTED</text>
            <text class="empty-desc">Please add devices or contact administrator</text>
        </view>
        
        <view class="loading-overlay" v-if="controlling">
            <view class="loading-hologram">
                <view class="holo-spinner">
                    <view class="spinner-ring"></view>
                    <view class="spinner-ring"></view>
                    <view class="spinner-ring"></view>
                </view>
                <text class="loading-text">PROCESSING...</text>
            </view>
        </view>
    </view>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import api from '@/api'

export default {
    data() {
        return {
            currentDevice: null,
            deviceStatus: {
                online: false,
                total_power_w: 0,
                sockets: []
            },
            controlling: false,
            powerHistory: [20, 35, 45, 30, 50, 40, 60, 55, 45, 65, 50, 40]
        }
    },
    
    computed: {
        ...mapState({
            devices: state => state.devices || []
        }),
        
        onlineDevicesCount() {
            return this.devices.filter(d => d.online).length
        },
        
        activeSocketsCount() {
            return (this.deviceStatus.sockets || []).filter(s => s.on).length
        },
        
        totalSocketsCount() {
            return (this.deviceStatus.sockets || []).length
        },
        
        estimatedDailyEnergy() {
            const hours = 24
            const power = this.deviceStatus.total_power_w || 0
            return ((power * hours) / 1000).toFixed(2)
        },
        
        socket_status() {
            return (this.deviceStatus.sockets || []).map(s => s.on)
        }
    },
    
    onLoad() {
        this.loadDevices()
    },
    
    onShow() {
        if (this.currentDevice) {
            this.refreshStatus()
        }
    },
    
    methods: {
        ...mapActions(['fetchDevices']),
        
        async loadDevices() {
            try {
                await this.fetchDevices()
                if (this.devices.length > 0) {
                    this.selectDevice(this.devices[0])
                }
            } catch (e) {
                console.error('Load devices failed:', e)
                uni.showToast({
                    title: '加载设备失败',
                    icon: 'none'
                })
            }
        },
        
        selectDevice(device) {
            this.currentDevice = device
            this.refreshStatus()
        },
        
        async refreshStatus() {
            if (!this.currentDevice) return
            
            try {
                const res = await api.device.getDeviceStatus(this.currentDevice.id)
                this.deviceStatus = res
                
                const power = res.total_power_w || 0
                this.updatePowerHistory(power)
            } catch (e) {
                console.error('Refresh status failed:', e)
                uni.showToast({
                    title: '刷新状态失败',
                    icon: 'none'
                })
            }
        },
        
        updatePowerHistory(power) {
            const maxPower = 2000
            const percentage = Math.min((power / maxPower) * 100, 100)
            this.powerHistory.shift()
            this.powerHistory.push(percentage)
        },
        
        getSocketPower(index) {
            const socket = (this.deviceStatus.sockets || [])[index]
            return socket ? socket.power_w || 0 : 0
        },
        
        async toggleSocket(socketIndex, targetStatus) {
            if (targetStatus) {
                await this.turnOn(socketIndex)
            } else {
                await this.turnOff(socketIndex)
            }
        },
        
        async turnOn(socketIndex) {
            if (!this.currentDevice || this.controlling) return
            
            this.controlling = true
            try {
                await api.command.sendCommand(this.currentDevice.id, {
                    action: 'toggle',
                    socket: socketIndex,
                    value: true
                })
                await this.refreshStatus()
                uni.showToast({
                    title: '开启成功',
                    icon: 'success'
                })
            } catch (e) {
                console.error('Turn on failed:', e)
                uni.showToast({
                    title: '开启失败',
                    icon: 'none'
                })
            } finally {
                this.controlling = false
            }
        },
        
        async turnOff(socketIndex) {
            if (!this.currentDevice || this.controlling) return
            
            this.controlling = true
            try {
                await api.command.sendCommand(this.currentDevice.id, {
                    action: 'toggle',
                    socket: socketIndex,
                    value: false
                })
                await this.refreshStatus()
                uni.showToast({
                    title: '关闭成功',
                    icon: 'success'
                })
            } catch (e) {
                console.error('Turn off failed:', e)
                uni.showToast({
                    title: '关闭失败',
                    icon: 'none'
                })
            } finally {
                this.controlling = false
            }
        },
        
        async allOn() {
            if (!this.currentDevice || this.controlling) return
            
            this.controlling = true
            try {
                // 批量开启所有插座
                const sockets = this.deviceStatus.sockets || []
                for (let i = 0; i < sockets.length; i++) {
                    await api.command.sendCommand(this.currentDevice.id, {
                        action: 'toggle',
                        socket: i + 1,
                        value: true
                    })
                }
                await this.refreshStatus()
                uni.showToast({
                    title: '全部开启成功',
                    icon: 'success'
                })
            } catch (e) {
                console.error('All on failed:', e)
                uni.showToast({
                    title: '全部开启失败',
                    icon: 'none'
                })
            } finally {
                this.controlling = false
            }
        },
        
        async allOff() {
            if (!this.currentDevice || this.controlling) return
            
            this.controlling = true
            try {
                // 批量关闭所有插座
                const sockets = this.deviceStatus.sockets || []
                for (let i = 0; i < sockets.length; i++) {
                    await api.command.sendCommand(this.currentDevice.id, {
                        action: 'toggle',
                        socket: i + 1,
                        value: false
                    })
                }
                await this.refreshStatus()
                uni.showToast({
                    title: '全部关闭成功',
                    icon: 'success'
                })
            } catch (e) {
                console.error('All off failed:', e)
                uni.showToast({
                    title: '全部关闭失败',
                    icon: 'none'
                })
            } finally {
                this.controlling = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.control-container {
    min-height: 100vh;
    background: linear-gradient(180deg, #0A1F1A 0%, #0F2A23 50%, #0A1F1A 100%);
    position: relative;
    overflow: hidden;
}

.bg-grid {
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
}

@keyframes gridMove {
    0% { transform: translateY(0); }
    100% { transform: translateY(40rpx); }
}

.bg-glow {
    position: absolute;
    top: -200rpx;
    left: 50%;
    transform: translateX(-50%);
    width: 800rpx;
    height: 800rpx;
    background: radial-gradient(circle, rgba(0, 245, 255, 0.15) 0%, transparent 70%);
    pointer-events: none;
}

.control-header {
    position: relative;
    padding: 40rpx 32rpx;
    z-index: 10;
    
    .header-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    
    .header-left {
        .title-wrapper {
            position: relative;
            margin-bottom: 8rpx;
            
            .title {
                font-size: 48rpx;
                font-weight: 700;
                color: #00F5FF;
                letter-spacing: 4rpx;
                text-shadow: 0 0 20rpx rgba(0, 245, 255, 0.5);
            }
            
            .title-line {
                position: absolute;
                bottom: -8rpx;
                left: 0;
                width: 100%;
                height: 2rpx;
                background: linear-gradient(90deg, #00F5FF 0%, transparent 100%);
            }
        }
        
        .subtitle {
            font-size: 20rpx;
            color: #00F5FF;
            letter-spacing: 6rpx;
            opacity: 0.6;
        }
    }
    
    .header-right {
        .status-indicator {
            display: flex;
            align-items: center;
            gap: 16rpx;
            padding: 16rpx 24rpx;
            background: rgba(0, 245, 255, 0.05);
            border: 1rpx solid rgba(0, 245, 255, 0.2);
            border-radius: 16rpx;
            
            .indicator-ring {
                position: relative;
                width: 48rpx;
                height: 48rpx;
                
                .ring-inner {
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    transform: translate(-50%, -50%);
                    width: 16rpx;
                    height: 16rpx;
                    background: #00F5FF;
                    border-radius: 50%;
                }
                
                .ring-pulse {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    border: 2rpx solid #00F5FF;
                    border-radius: 50%;
                    animation: pulse 2s ease-out infinite;
                }
            }
            
            .status-info {
                .status-label {
                    display: block;
                    font-size: 18rpx;
                    color: rgba(0, 245, 255, 0.6);
                    letter-spacing: 2rpx;
                }
                
                .status-value {
                    display: block;
                    font-size: 24rpx;
                    font-weight: 600;
                    color: #00F5FF;
                    letter-spacing: 2rpx;
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
        transform: scale(1.8);
        opacity: 0;
    }
}

.device-selector {
    position: relative;
    padding: 0 32rpx;
    margin-bottom: 32rpx;
    z-index: 10;
    
    .device-scroll {
        white-space: nowrap;
    }
    
    .device-card {
        display: inline-flex;
        align-items: center;
        gap: 20rpx;
        padding: 24rpx;
        margin-right: 20rpx;
        background: rgba(0, 245, 255, 0.03);
        border: 1rpx solid rgba(0, 245, 255, 0.1);
        border-radius: 20rpx;
        transition: all 0.3s ease;
        
        &.active {
            background: rgba(0, 245, 255, 0.08);
            border-color: rgba(0, 245, 255, 0.4);
            box-shadow: 0 0 30rpx rgba(0, 245, 255, 0.2);
            
            .holo-ring {
                border-color: #00F5FF;
                animation: rotate 3s linear infinite;
            }
        }
        
        .device-hologram {
            position: relative;
            width: 64rpx;
            height: 64rpx;
            
            .holo-ring {
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                border: 2rpx solid rgba(0, 245, 255, 0.3);
                border-radius: 50%;
            }
            
            .holo-core {
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
            }
        }
        
        .device-info {
            .device-name {
                display: block;
                font-size: 28rpx;
                font-weight: 600;
                color: #00F5FF;
                margin-bottom: 8rpx;
            }
            
            .device-status {
                display: flex;
                align-items: center;
                gap: 8rpx;
                
                .status-dot {
                    width: 12rpx;
                    height: 12rpx;
                    border-radius: 50%;
                    background: #666;
                    
                    &.online {
                        background: #00F5FF;
                        box-shadow: 0 0 10rpx rgba(0, 245, 255, 0.8);
                    }
                }
                
                .status-text {
                    font-size: 20rpx;
                    color: rgba(0, 245, 255, 0.6);
                    letter-spacing: 2rpx;
                }
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
    
    .power-display {
        display: flex;
        gap: 24rpx;
        margin-bottom: 24rpx;
        
        .power-hologram {
            flex: 0 0 280rpx;
            display: flex;
            align-items: center;
            justify-content: center;
            
            .holo-circle {
                position: relative;
                width: 240rpx;
                height: 240rpx;
                
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

.socket-control {
    position: relative;
    padding: 0 32rpx;
    margin-bottom: 32rpx;
    z-index: 10;
    
    .control-header {
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
    
    .socket-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 20rpx;
        
        .socket-card {
            background: rgba(0, 245, 255, 0.03);
            border: 1rpx solid rgba(0, 245, 255, 0.1);
            border-radius: 20rpx;
            padding: 24rpx;
            transition: all 0.3s ease;
            
            &.active {
                background: rgba(0, 245, 255, 0.08);
                border-color: rgba(0, 245, 255, 0.4);
                box-shadow: 0 0 30rpx rgba(0, 245, 255, 0.2);
            }
            
            &.disabled {
                opacity: 0.4;
                pointer-events: none;
            }
            
            .socket-hologram {
                position: relative;
                
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
                
                .socket-content {
                    .socket-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 16rpx;
                        
                        .socket-id {
                            font-size: 22rpx;
                            font-weight: 600;
                            color: #00F5FF;
                            letter-spacing: 2rpx;
                        }
                        
                        .socket-status {
                            display: flex;
                            align-items: center;
                            gap: 8rpx;
                            
                            .status-indicator {
                                width: 8rpx;
                                height: 8rpx;
                                border-radius: 50%;
                                background: #666;
                            }
                            
                            .status-text {
                                font-size: 18rpx;
                                color: rgba(0, 245, 255, 0.6);
                                letter-spacing: 2rpx;
                            }
                            
                            &.active {
                                .status-indicator {
                                    background: #00F5FF;
                                    box-shadow: 0 0 10rpx rgba(0, 245, 255, 0.8);
                                }
                            }
                        }
                    }
                    
                    .socket-power {
                        text-align: center;
                        margin: 20rpx 0;
                        
                        .power-value {
                            font-size: 40rpx;
                            font-weight: 700;
                            color: #00F5FF;
                            text-shadow: 0 0 20rpx rgba(0, 245, 255, 0.5);
                        }
                        
                        .power-unit {
                            font-size: 18rpx;
                            color: rgba(0, 245, 255, 0.6);
                            margin-left: 8rpx;
                        }
                    }
                    
                    .socket-switch {
                        display: flex;
                        justify-content: center;
                        margin: 24rpx 0;
                        
                        .switch-track {
                            position: relative;
                            width: 120rpx;
                            height: 48rpx;
                            background: rgba(0, 245, 255, 0.1);
                            border: 1rpx solid rgba(0, 245, 255, 0.2);
                            border-radius: 24rpx;
                            cursor: pointer;
                            transition: all 0.3s ease;
                            
                            &.active {
                                background: rgba(0, 245, 255, 0.2);
                                border-color: rgba(0, 245, 255, 0.4);
                                
                                .switch-thumb {
                                    left: calc(100% - 40rpx);
                                }
                                
                                .switch-glow {
                                    opacity: 1;
                                }
                            }
                            
                            .switch-thumb {
                                position: absolute;
                                top: 4rpx;
                                left: 4rpx;
                                width: 40rpx;
                                height: 40rpx;
                                background: #00F5FF;
                                border-radius: 50%;
                                transition: all 0.3s ease;
                            }
                            
                            .switch-glow {
                                position: absolute;
                                top: 50%;
                                left: 50%;
                                transform: translate(-50%, -50%);
                                width: 80rpx;
                                height: 80rpx;
                                background: radial-gradient(circle, rgba(0, 245, 255, 0.3) 0%, transparent 70%);
                                opacity: 0;
                                transition: opacity 0.3s ease;
                            }
                        }
                    }
                    
                    .socket-actions {
                        display: flex;
                        gap: 12rpx;
                        
                        .action-btn {
                            flex: 1;
                            height: 56rpx;
                            background: rgba(0, 245, 255, 0.1);
                            border: 1rpx solid rgba(0, 245, 255, 0.2);
                            border-radius: 12rpx;
                            font-size: 20rpx;
                            color: #00F5FF;
                            letter-spacing: 1rpx;
                            transition: all 0.3s ease;
                            
                            .btn-text {
                                color: #00F5FF;
                            }
                            
                            &:active:not([disabled]) {
                                background: rgba(0, 245, 255, 0.2);
                                transform: scale(0.95);
                            }
                            
                            &[disabled] {
                                opacity: 0.3;
                            }
                        }
                    }
                }
            }
        }
    }
}

.quick-actions {
    position: relative;
    padding: 0 32rpx;
    margin-bottom: 32rpx;
    z-index: 10;
    
    .action-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 16rpx;
        
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
                    font-size: 22rpx;
                    font-weight: 600;
                    color: #00F5FF;
                    letter-spacing: 2rpx;
                    margin-bottom: 8rpx;
                }
                
                .card-desc {
                    display: block;
                    font-size: 18rpx;
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

.empty-state {
    text-align: center;
    padding: 120rpx 40rpx;
    
    .empty-hologram {
        width: 160rpx;
        height: 160rpx;
        margin: 0 auto 32rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        
        .holo-circle {
            position: relative;
            width: 100%;
            height: 100%;
            border: 2rpx solid rgba(0, 245, 255, 0.2);
            border-radius: 50%;
            animation: pulse 2s ease-out infinite;
        }
    }
    
    .empty-title {
        display: block;
        font-size: 32rpx;
        font-weight: 600;
        color: #00F5FF;
        letter-spacing: 4rpx;
        margin-bottom: 12rpx;
    }
    
    .empty-desc {
        display: block;
        font-size: 24rpx;
        color: rgba(0, 245, 255, 0.6);
    }
}

.loading-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(10, 31, 26, 0.9);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 999;
    
    .loading-hologram {
        text-align: center;
        
        .holo-spinner {
            position: relative;
            width: 120rpx;
            height: 120rpx;
            margin: 0 auto 32rpx;
            
            .spinner-ring {
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                border: 2rpx solid transparent;
                border-top-color: #00F5FF;
                border-radius: 50%;
                
                &:nth-child(1) {
                    animation: rotate 1s linear infinite;
                }
                
                &:nth-child(2) {
                    width: 80%;
                    height: 80%;
                    top: 10%;
                    left: 10%;
                    animation: rotate 1.5s linear infinite reverse;
                }
                
                &:nth-child(3) {
                    width: 60%;
                    height: 60%;
                    top: 20%;
                    left: 20%;
                    animation: rotate 2s linear infinite;
                }
            }
        }
        
        .loading-text {
            font-size: 28rpx;
            color: #00F5FF;
            letter-spacing: 4rpx;
        }
    }
}
</style>
