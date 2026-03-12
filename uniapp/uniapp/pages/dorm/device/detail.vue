<template>
    <view class="device-detail-container">
        <view class="device-header" v-if="device">
            <view class="device-icon" :class="{ online: device.online }">
                <uni-icons type="staff" size="48" color="#fff"></uni-icons>
            </view>
            <view class="device-info">
                <text class="device-name">{{ device.name }}</text>
                <text class="device-room">{{ device.room }}</text>
            </view>
            <view class="device-status" :class="{ online: device.online }">
                {{ device.online ? '在线' : '离线' }}
            </view>
        </view>
        
        <view class="status-card" v-if="status">
            <view class="status-item">
                <text class="status-label">总功率</text>
                <text class="status-value">{{ status.total_power_w }}W</text>
            </view>
            <view class="status-divider"></view>
            <view class="status-item">
                <text class="status-label">最后更新</text>
                <text class="status-value">{{ formatTime(status.last_update) }}</text>
            </view>
        </view>
        
        <view class="socket-section" v-if="status">
            <view class="section-header">
                <text class="section-title">插座控制</text>
            </view>
            
            <view class="socket-list">
                <view 
                    class="socket-item" 
                    v-for="(item, index) in status.socket_status" 
                    :key="index"
                >
                    <view class="socket-info">
                        <text class="socket-name">插座 {{ index + 1 }}</text>
                        <text class="socket-status" :class="{ on: item }">
                            {{ item ? '已开启' : '已关闭' }}
                        </text>
                    </view>
                    <switch 
                        :checked="item" 
                        @change="toggleSocket(index + 1, $event)"
                        :disabled="!device.online"
                        color="#2A7965"
                    />
                </view>
            </view>
        </view>
        
        <view class="quick-actions">
            <button class="action-btn" @click="goToTelemetry">
                <uni-icons type="bars" size="20" color="#2A7965"></uni-icons>
                <text>用电数据</text>
            </button>
            <button class="action-btn" @click="goToAIReport">
                <uni-icons type="chatbubble" size="20" color="#2A7965"></uni-icons>
                <text>AI报告</text>
            </button>
            <button class="action-btn" @click="goToAlerts">
                <uni-icons type="info" size="20" color="#2A7965"></uni-icons>
                <text>告警记录</text>
            </button>
        </view>
        
        <view class="command-history">
            <view class="section-header">
                <text class="section-title">操作历史</text>
            </view>
            
            <view class="history-list" v-if="commandHistory.length > 0">
                <view class="history-item" v-for="cmd in commandHistory" :key="cmd.id">
                    <view class="history-icon">
                        <uni-icons 
                            :type="cmd.action === 'on' ? 'checkmarkempty' : 'close'" 
                            size="20" 
                            :color="cmd.status === 'EXECUTED' ? '#4CAF50' : '#999'"
                        ></uni-icons>
                    </view>
                    <view class="history-content">
                        <text class="history-action">
                            {{ cmd.action === 'on' ? '开启' : '关闭' }}插座 {{ cmd.socket }}
                        </text>
                        <text class="history-time">{{ formatTime(cmd.timestamp) }}</text>
                    </view>
                    <view class="history-status" :class="cmd.status.toLowerCase()">
                        {{ getStatusText(cmd.status) }}
                    </view>
                </view>
            </view>
            
            <view class="empty-state" v-else>
                <text class="empty-text">暂无操作记录</text>
            </view>
        </view>
    </view>
</template>

<script>
import api from '@/api'

export default {
    data() {
        return {
            deviceId: '',
            device: null,
            status: null,
            commandHistory: [],
            loading: false
        }
    },
    onLoad(options) {
        this.deviceId = options.id
        this.loadDeviceData()
    },
    onShow() {
        this.loadDeviceStatus()
    },
    onPullDownRefresh() {
        this.loadDeviceData().finally(() => {
            uni.stopPullDownRefresh()
        })
    },
    methods: {
        async loadDeviceData() {
            this.loading = true
            try {
                const devices = await api.device.getDeviceList()
                this.device = devices.find(d => d.id === this.deviceId)
                
                await this.loadDeviceStatus()
                await this.loadCommandHistory()
            } catch (error) {
                console.error('加载设备数据失败:', error)
            } finally {
                this.loading = false
            }
        },
        
        async loadDeviceStatus() {
            try {
                this.status = await api.device.getDeviceStatus(this.deviceId)
            } catch (error) {
                console.error('加载设备状态失败:', error)
            }
        },
        
        async loadCommandHistory() {
            try {
                this.commandHistory = await api.command.getDeviceCommands(this.deviceId)
            } catch (error) {
                console.error('加载命令历史失败:', error)
            }
        },
        
        async toggleSocket(socketIndex, event) {
            const action = event.detail.value ? 'on' : 'off'
            
            try {
                const result = await api.command.sendCommand(this.deviceId, {
                    action,
                    socket: socketIndex
                })
                
                uni.showToast({
                    title: '命令已发送',
                    icon: 'success'
                })
                
                await this.pollCommandStatus(result.cmdId)
            } catch (error) {
                uni.showToast({
                    title: '发送命令失败',
                    icon: 'none'
                })
                this.loadDeviceStatus()
            }
        },
        
        async pollCommandStatus(cmdId) {
            const maxAttempts = 10
            let attempts = 0
            
            const poll = async () => {
                attempts++
                try {
                    const result = await api.command.getCommandStatus(cmdId)
                    
                    if (result.status === 'EXECUTED') {
                        uni.showToast({
                            title: '操作成功',
                            icon: 'success'
                        })
                        this.loadDeviceStatus()
                        this.loadCommandHistory()
                        return
                    }
                    
                    if (result.status === 'FAILED') {
                        uni.showToast({
                            title: '操作失败',
                            icon: 'none'
                        })
                        this.loadDeviceStatus()
                        return
                    }
                    
                    if (attempts < maxAttempts) {
                        setTimeout(poll, 1000)
                    }
                } catch (error) {
                    console.error('查询命令状态失败:', error)
                }
            }
            
            poll()
        },
        
        formatTime(timestamp) {
            if (!timestamp) return '--'
            const date = new Date(timestamp)
            return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
        },
        
        getStatusText(status) {
            const map = {
                'PENDING': '处理中',
                'EXECUTED': '已执行',
                'FAILED': '失败'
            }
            return map[status] || status
        },
        
        goToTelemetry() {
            uni.navigateTo({
                url: `/pages/dorm/telemetry/index?deviceId=${this.deviceId}`
            })
        },
        
        goToAIReport() {
            uni.navigateTo({
                url: `/pages/dorm/ai-report/index?roomId=${this.device?.room}`
            })
        },
        
        goToAlerts() {
            uni.navigateTo({
                url: `/pages/dorm/alert/list?deviceId=${this.deviceId}`
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.device-detail-container {
    min-height: 100vh;
    background: #F2F7F5;
    padding-bottom: 40rpx;
}

.device-header {
    background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
    padding: 40rpx 30rpx;
    display: flex;
    align-items: center;
    
    .device-icon {
        width: 100rpx;
        height: 100rpx;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.2);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 24rpx;
        
        &.online {
            background: rgba(42, 121, 101, 0.3);
        }
    }
    
    .device-info {
        flex: 1;
        
        .device-name {
            display: block;
            font-size: 36rpx;
            font-weight: bold;
            color: #fff;
            margin-bottom: 8rpx;
        }
        
        .device-room {
            font-size: 26rpx;
            color: rgba(255, 255, 255, 0.8);
        }
    }
    
    .device-status {
        font-size: 24rpx;
        color: #fff;
        padding: 8rpx 24rpx;
        border-radius: 20rpx;
        background: rgba(255, 255, 255, 0.2);
        
        &.online {
            background: rgba(42, 121, 101, 0.3);
        }
    }
}

.status-card {
    background: #fff;
    margin: -30rpx 30rpx 30rpx;
    border-radius: 20rpx;
    padding: 30rpx;
    display: flex;
    justify-content: space-around;
    box-shadow: 0 4rpx 20rpx rgba(42, 121, 101, 0.15);
    
    .status-item {
        text-align: center;
        
        .status-label {
            display: block;
            font-size: 24rpx;
            color: #66A392;
            margin-bottom: 10rpx;
        }
        
        .status-value {
            font-size: 36rpx;
            font-weight: bold;
            color: #2A7965;
        }
    }
    
    .status-divider {
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

.socket-section {
    background: #fff;
    margin: 0 30rpx 30rpx;
    border-radius: 20rpx;
    
    .socket-list {
        .socket-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 30rpx;
            border-bottom: 1rpx solid #E8F5F0;
            
            &:last-child {
                border-bottom: none;
            }
            
            .socket-info {
                .socket-name {
                    display: block;
                    font-size: 30rpx;
                    color: #2A7965;
                    margin-bottom: 8rpx;
                }
                
                .socket-status {
                    font-size: 24rpx;
                    color: #66A392;
                    
                    &.on {
                        color: #2A7965;
                    }
                }
            }
        }
    }
}

.quick-actions {
    display: flex;
    justify-content: space-around;
    padding: 30rpx;
    
    .action-btn {
        display: flex;
        flex-direction: column;
        align-items: center;
        background: #fff;
        border: none;
        padding: 20rpx 30rpx;
        border-radius: 20rpx;
        box-shadow: 0 4rpx 12rpx rgba(42, 121, 101, 0.1);
        
        text {
            font-size: 24rpx;
            color: #66A392;
            margin-top: 10rpx;
        }
    }
}

.command-history {
    background: #fff;
    margin: 0 30rpx;
    border-radius: 20rpx;
    
    .history-list {
        .history-item {
            display: flex;
            align-items: center;
            padding: 30rpx;
            border-bottom: 1rpx solid #E8F5F0;
            
            &:last-child {
                border-bottom: none;
            }
            
            .history-icon {
                margin-right: 20rpx;
            }
            
            .history-content {
                flex: 1;
                
                .history-action {
                    display: block;
                    font-size: 28rpx;
                    color: #2A7965;
                    margin-bottom: 8rpx;
                }
                
                .history-time {
                    font-size: 24rpx;
                    color: #66A392;
                }
            }
            
            .history-status {
                font-size: 24rpx;
                padding: 4rpx 16rpx;
                border-radius: 20rpx;
                
                &.executed {
                    color: #2A7965;
                    background: rgba(42, 121, 101, 0.1);
                }
                
                &.pending {
                    color: #3D967E;
                    background: rgba(51, 126, 204, 0.1);
                }
                
                &.failed {
                    color: #1F5C4D;
                    background: rgba(232, 111, 80, 0.1);
                }
            }
        }
    }
}

.empty-state {
    text-align: center;
    padding: 60rpx 0;
    
    .empty-text {
        font-size: 28rpx;
        color: #66A392;
    }
}
</style>
