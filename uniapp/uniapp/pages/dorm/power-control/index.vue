<template>
    <view class="cyber-container">
        <view class="cyber-grid"></view>
        <view class="cyber-glow"></view>
        
        <view class="power-header">
            <view class="header-content">
                <view class="title-wrapper">
                    <text class="title" data-text="POWER CONTROL">POWER CONTROL</text>
                    <view class="title-line"></view>
                </view>
                <text class="subtitle">EMERGENCY POWER MANAGEMENT</text>
            </view>
        </view>
        
        <view class="room-section">
            <view class="section-header">
                <view class="header-line">
                    <text class="line-label">SELECT ROOM</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="room-hologram">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="room-content">
                    <picker 
                        mode="selector" 
                        :range="roomOptions" 
                        range-key="name"
                        @change="onRoomChange"
                    >
                        <view class="picker-value">
                            <uni-icons type="location" size="20" color="#00F5FF"></uni-icons>
                            <text class="picker-text">{{ selectedRoom ? selectedRoom.name : 'SELECT ROOM' }}</text>
                            <uni-icons type="arrowdown" size="16" color="#00F5FF"></uni-icons>
                        </view>
                    </picker>
                    
                    <view class="room-info" v-if="selectedRoom">
                        <view class="info-item">
                            <text class="info-label">ROOM ID:</text>
                            <text class="info-value">{{ selectedRoom.id }}</text>
                        </view>
                        <view class="info-item">
                            <text class="info-label">STATUS:</text>
                            <text class="info-value" :class="{ active: selectedRoom.status === 'online', offline: selectedRoom.status === 'offline' }">
                                {{ selectedRoom.status === 'online' ? 'ONLINE' : 'OFFLINE' }}
                            </text>
                        </view>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="control-section">
            <view class="section-header">
                <view class="header-line">
                    <text class="line-label">POWER ACTION</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="control-hologram">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="control-content">
                    <view class="action-card">
                        <view class="action-icon danger">
                            <uni-icons type="close" size="32" color="#FF4757"></uni-icons>
                        </view>
                        <view class="action-info">
                            <text class="action-title">CUT OFF POWER</text>
                            <text class="action-desc">Emergency power shutdown</text>
                        </view>
                        <button 
                            class="action-btn danger-btn"
                            :disabled="!selectedRoom || cuttingOff"
                            @click="confirmCutoff"
                        >
                            {{ cuttingOff ? 'PROCESSING...' : 'CUT OFF' }}
                        </button>
                    </view>
                    
                    <view class="action-card">
                        <view class="action-icon success">
                            <uni-icons type="refresh" size="32" color="#26DE81"></uni-icons>
                        </view>
                        <view class="action-info">
                            <text class="action-title">RESTORE POWER</text>
                            <text class="action-desc">Restore power supply</text>
                        </view>
                        <button 
                            class="action-btn success-btn"
                            :disabled="!selectedRoom || restoring"
                            @click="confirmRestore"
                        >
                            {{ restoring ? 'PROCESSING...' : 'RESTORE' }}
                        </button>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="history-section">
            <view class="section-header">
                <view class="header-line">
                    <text class="line-label">POWER HISTORY</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="history-hologram" v-if="powerHistory.length > 0">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="history-content">
                    <view class="history-item" v-for="(item, index) in powerHistory" :key="index">
                        <view class="history-time">{{ formatTime(item.timestamp) }}</view>
                        <view class="history-action" :class="{ danger: item.action === 'cutoff', success: item.action === 'restore' }">
                            {{ item.action === 'cutoff' ? 'POWER CUT OFF' : 'POWER RESTORED' }}
                        </view>
                        <view class="history-reason">{{ item.reason || 'Emergency action' }}</view>
                    </view>
                </view>
            </view>
            
            <view class="empty-state" v-else>
                <view class="empty-hologram">
                    <view class="holo-circle">
                        <uni-icons type="time" size="64" color="#00F5FF"></uni-icons>
                    </view>
                </view>
                <text class="empty-title">NO HISTORY</text>
                <text class="empty-desc">No power control history available</text>
            </view>
        </view>
        
        <!-- Confirm Dialog -->
        <uni-popup 
            ref="confirmPopup" 
            type="center"
            :animation="false"
        >
            <view class="popup-hologram">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                <view class="popup-content">
                    <text class="popup-title">{{ confirmTitle }}</text>
                    <text class="popup-message">{{ confirmMessage }}</text>
                    <textarea 
                        v-if="showReason" 
                        v-model="reason" 
                        class="reason-input" 
                        placeholder="Enter reason for action" 
                        placeholder-style="color: rgba(0, 245, 255, 0.4)"
                    ></textarea>
                    <view class="popup-buttons">
                        <button class="btn cancel-btn" @click="$refs.confirmPopup.close()">CANCEL</button>
                        <button class="btn confirm-btn" @click="executeAction">{{ confirmAction }}</button>
                    </view>
                </view>
            </view>
        </uni-popup>
    </view>
</template>

<script>
import api from '@/api'
import { mapState, mapActions } from 'vuex'

export default {
    data() {
        return {
            selectedRoom: null,
            cuttingOff: false,
            restoring: false,
            powerHistory: [],
            confirmTitle: '',
            confirmMessage: '',
            confirmAction: '',
            showReason: false,
            reason: '',
            actionType: ''
        }
    },
    computed: {
        ...mapState(['rooms']),
        roomOptions() {
            return this.rooms
        }
    },
    onShow() {
        this.initData()
    },
    onPullDownRefresh() {
        this.initData().finally(() => {
            uni.stopPullDownRefresh()
        })
    },
    methods: {
        ...mapActions(['fetchRooms']),
        
        async initData() {
            if (this.rooms.length === 0) {
                await this.fetchRooms()
            }
            
            if (this.rooms.length > 0 && !this.selectedRoom) {
                this.selectedRoom = this.rooms[0]
                this.loadPowerHistory()
            }
        },
        
        onRoomChange(e) {
            const index = e.detail.value
            this.selectedRoom = this.rooms[index]
            this.loadPowerHistory()
        },
        
        async loadPowerHistory() {
            if (!this.selectedRoom) return
            
            try {
                const history = await api.powerControl.getPowerHistory(this.selectedRoom.id)
                this.powerHistory = history
            } catch (error) {
                console.error('加载断电历史失败:', error)
            }
        },
        
        confirmCutoff() {
            this.confirmTitle = 'CONFIRM POWER CUTOFF'
            this.confirmMessage = `Are you sure you want to cut off power for room ${this.selectedRoom.name}?`
            this.confirmAction = 'CUT OFF'
            this.showReason = true
            this.actionType = 'cutoff'
            this.$refs.confirmPopup.open()
        },
        
        confirmRestore() {
            this.confirmTitle = 'CONFIRM POWER RESTORE'
            this.confirmMessage = `Are you sure you want to restore power for room ${this.selectedRoom.name}?`
            this.confirmAction = 'RESTORE'
            this.showReason = true
            this.actionType = 'restore'
            this.$refs.confirmPopup.open()
        },
        
        async executeAction() {
            if (!this.selectedRoom) return
            
            try {
                if (this.actionType === 'cutoff') {
                    this.cuttingOff = true
                    await api.powerControl.cutoffPower(this.selectedRoom.id, {
                        reason: this.reason
                    })
                    uni.showToast({
                        title: 'Power cut off successfully',
                        icon: 'success'
                    })
                } else if (this.actionType === 'restore') {
                    this.restoring = true
                    await api.powerControl.restorePower(this.selectedRoom.id, {
                        reason: this.reason
                    })
                    uni.showToast({
                        title: 'Power restored successfully',
                        icon: 'success'
                    })
                }
                
                this.$refs.confirmPopup.close()
                this.reason = ''
                await this.loadPowerHistory()
            } catch (error) {
                console.error('Action failed:', error)
                uni.showToast({
                    title: 'Action failed',
                    icon: 'none'
                })
            } finally {
                this.cuttingOff = false
                this.restoring = false
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

.power-header {
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

.room-section {
    position: relative;
    padding: 0 32rpx 32rpx;
    z-index: 10;
    
    .room-hologram {
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
        
        .room-content {
            .picker-value {
                display: flex;
                align-items: center;
                gap: 16rpx;
                padding: 20rpx;
                background: rgba(0, 245, 255, 0.05);
                border: 1rpx solid rgba(0, 245, 255, 0.2);
                border-radius: 16rpx;
                margin-bottom: 24rpx;
                
                .picker-text {
                    flex: 1;
                    font-size: 28rpx;
                    color: #00F5FF;
                    letter-spacing: 2rpx;
                }
            }
            
            .room-info {
                background: rgba(0, 245, 255, 0.03);
                border: 1rpx solid rgba(0, 245, 255, 0.1);
                border-radius: 16rpx;
                padding: 20rpx;
                
                .info-item {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 16rpx;
                    
                    &:last-child {
                        margin-bottom: 0;
                    }
                    
                    .info-label {
                        font-size: 24rpx;
                        color: rgba(0, 245, 255, 0.6);
                    }
                    
                    .info-value {
                        font-size: 24rpx;
                        color: #00F5FF;
                        
                        &.active {
                            color: #26DE81;
                        }
                        
                        &.offline {
                            color: #FF4757;
                        }
                    }
                }
            }
        }
    }
}

.control-section {
    position: relative;
    padding: 0 32rpx 32rpx;
    z-index: 10;
    
    .control-hologram {
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
        
        .control-content {
            .action-card {
                display: flex;
                align-items: center;
                gap: 24rpx;
                padding: 24rpx;
                background: rgba(0, 245, 255, 0.03);
                border: 1rpx solid rgba(0, 245, 255, 0.1);
                border-radius: 16rpx;
                margin-bottom: 20rpx;
                
                &:last-child {
                    margin-bottom: 0;
                }
                
                .action-icon {
                    width: 80rpx;
                    height: 80rpx;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border-radius: 16rpx;
                    
                    &.danger {
                        background: rgba(255, 71, 87, 0.1);
                        border: 1rpx solid rgba(255, 71, 87, 0.3);
                    }
                    
                    &.success {
                        background: rgba(38, 222, 129, 0.1);
                        border: 1rpx solid rgba(38, 222, 129, 0.3);
                    }
                }
                
                .action-info {
                    flex: 1;
                    
                    .action-title {
                        display: block;
                        font-size: 28rpx;
                        font-weight: 600;
                        color: #00F5FF;
                        margin-bottom: 8rpx;
                    }
                    
                    .action-desc {
                        display: block;
                        font-size: 22rpx;
                        color: rgba(0, 245, 255, 0.6);
                    }
                }
                
                .action-btn {
                    padding: 16rpx 32rpx;
                    border-radius: 12rpx;
                    font-size: 24rpx;
                    font-weight: 600;
                    letter-spacing: 2rpx;
                    
                    &.danger-btn {
                        background: rgba(255, 71, 87, 0.2);
                        border: 1rpx solid rgba(255, 71, 87, 0.4);
                        color: #FF4757;
                    }
                    
                    &.success-btn {
                        background: rgba(38, 222, 129, 0.2);
                        border: 1rpx solid rgba(38, 222, 129, 0.4);
                        color: #26DE81;
                    }
                    
                    &:disabled {
                        opacity: 0.5;
                    }
                }
            }
        }
    }
}

.history-section {
    position: relative;
    padding: 0 32rpx;
    z-index: 10;
    
    .history-hologram {
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
        
        .history-content {
            .history-item {
                padding: 24rpx 32rpx;
                border-bottom: 1rpx solid rgba(0, 245, 255, 0.1);
                
                &:last-child {
                    border-bottom: none;
                }
                
                &:nth-child(even) {
                    background: rgba(0, 245, 255, 0.02);
                }
                
                .history-time {
                    font-size: 22rpx;
                    color: rgba(0, 245, 255, 0.6);
                    margin-bottom: 8rpx;
                }
                
                .history-action {
                    font-size: 26rpx;
                    font-weight: 600;
                    margin-bottom: 8rpx;
                    
                    &.danger {
                        color: #FF4757;
                    }
                    
                    &.success {
                        color: #26DE81;
                    }
                }
                
                .history-reason {
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

.popup-hologram {
    position: relative;
    background: rgba(10, 14, 39, 0.95);
    border: 1rpx solid rgba(0, 245, 255, 0.2);
    border-radius: 20rpx;
    padding: 32rpx;
    width: 80%;
    
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
    
    .popup-content {
        .popup-title {
            display: block;
            font-size: 28rpx;
            font-weight: 600;
            color: #00F5FF;
            text-align: center;
            margin-bottom: 16rpx;
        }
        
        .popup-message {
            display: block;
            font-size: 24rpx;
            color: rgba(0, 245, 255, 0.8);
            text-align: center;
            margin-bottom: 24rpx;
        }
        
        .reason-input {
            width: 100%;
            background: rgba(0, 245, 255, 0.05);
            border: 1rpx solid rgba(0, 245, 255, 0.2);
            border-radius: 12rpx;
            padding: 16rpx;
            color: #00F5FF;
            font-size: 24rpx;
            margin-bottom: 24rpx;
            min-height: 120rpx;
        }
        
        .popup-buttons {
            display: flex;
            gap: 16rpx;
            
            .btn {
                flex: 1;
                padding: 16rpx 0;
                border-radius: 12rpx;
                font-size: 24rpx;
                font-weight: 600;
                
                &.cancel-btn {
                    background: rgba(0, 245, 255, 0.1);
                    border: 1rpx solid rgba(0, 245, 255, 0.2);
                    color: #00F5FF;
                }
                
                &.confirm-btn {
                    background: rgba(0, 245, 255, 0.2);
                    border: 1rpx solid rgba(0, 245, 255, 0.4);
                    color: #00F5FF;
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