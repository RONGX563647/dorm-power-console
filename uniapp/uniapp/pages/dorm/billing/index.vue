<template>
    <view class="cyber-container">
        <view class="cyber-grid"></view>
        <view class="cyber-glow"></view>
        
        <view class="billing-header">
            <view class="header-content">
                <view class="title-wrapper">
                    <text class="title" data-text="BILLING MANAGEMENT">BILLING MANAGEMENT</text>
                    <view class="title-line"></view>
                </view>
                <text class="subtitle">POWER CONSUMPTION BILLING</text>
            </view>
        </view>
        
        <view class="balance-section">
            <view class="section-header">
                <view class="header-line">
                    <text class="line-label">ACCOUNT BALANCE</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="balance-hologram">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="balance-content">
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
                    
                    <view class="balance-card" v-if="balance">
                        <view class="balance-info">
                            <text class="balance-label">CURRENT BALANCE</text>
                            <text class="balance-amount">¥{{ balance.amount || 0 }}</text>
                            <text class="balance-status" :class="{ warning: balance.amount < 50, danger: balance.amount < 10 }">
                                {{ balance.amount < 10 ? 'LOW BALANCE' : balance.amount < 50 ? 'WARNING' : 'SUFFICIENT' }}
                            </text>
                        </view>
                        <view class="balance-actions">
                            <button class="balance-btn" @click="showRechargeDialog">RECHARGE</button>
                            <button class="balance-btn" @click="refreshBalance">REFRESH</button>
                        </view>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="consumption-section">
            <view class="section-header">
                <view class="header-line">
                    <text class="line-label">CONSUMPTION STATISTICS</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="consumption-hologram" v-if="consumption">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="consumption-content">
                    <view class="consumption-grid">
                        <view class="consumption-item">
                            <text class="consumption-label">TODAY</text>
                            <text class="consumption-value">{{ consumption.today_kwh || 0 }} kWh</text>
                            <text class="consumption-cost">¥{{ consumption.today_cost || 0 }}</text>
                        </view>
                        <view class="consumption-item">
                            <text class="consumption-label">THIS MONTH</text>
                            <text class="consumption-value">{{ consumption.month_kwh || 0 }} kWh</text>
                            <text class="consumption-cost">¥{{ consumption.month_cost || 0 }}</text>
                        </view>
                        <view class="consumption-item">
                            <text class="consumption-label">TOTAL</text>
                            <text class="consumption-value">{{ consumption.total_kwh || 0 }} kWh</text>
                            <text class="consumption-cost">¥{{ consumption.total_cost || 0 }}</text>
                        </view>
                    </view>
                </view>
            </view>
        </view>
        
        <view class="bills-section">
            <view class="section-header">
                <view class="header-line">
                    <text class="line-label">BILLING HISTORY</text>
                    <view class="line-decoration"></view>
                </view>
            </view>
            
            <view class="bills-hologram" v-if="bills.length > 0">
                <view class="holo-frame">
                    <view class="frame-corner corner-tl"></view>
                    <view class="frame-corner corner-tr"></view>
                    <view class="frame-corner corner-bl"></view>
                    <view class="frame-corner corner-br"></view>
                </view>
                
                <view class="bills-content">
                    <view class="bill-item" v-for="(bill, index) in bills" :key="index">
                        <view class="bill-header">
                            <text class="bill-period">{{ bill.period }}</text>
                            <text class="bill-status" :class="bill.status">
                                {{ bill.status.toUpperCase() }}
                            </text>
                        </view>
                        <view class="bill-details">
                            <view class="bill-detail">
                                <text class="detail-label">CONSUMPTION:</text>
                                <text class="detail-value">{{ bill.kwh }} kWh</text>
                            </view>
                            <view class="bill-detail">
                                <text class="detail-label">AMOUNT:</text>
                                <text class="detail-value">¥{{ bill.amount }}</text>
                            </view>
                            <view class="bill-detail" v-if="bill.paid_at">
                                <text class="detail-label">PAID AT:</text>
                                <text class="detail-value">{{ formatTime(bill.paid_at) }}</text>
                            </view>
                        </view>
                        <button 
                            v-if="bill.status === 'unpaid'" 
                            class="pay-btn"
                            @click="payBill(bill.id)"
                        >
                            PAY NOW
                        </button>
                    </view>
                </view>
            </view>
            
            <view class="empty-state" v-else>
                <view class="empty-hologram">
                    <view class="holo-circle">
                        <uni-icons type="receipt" size="64" color="#00F5FF"></uni-icons>
                    </view>
                </view>
                <text class="empty-title">NO BILLS</text>
                <text class="empty-desc">No billing history available</text>
            </view>
        </view>
        
        <!-- Recharge Dialog -->
        <uni-popup 
            ref="rechargePopup" 
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
                    <text class="popup-title">RECHARGE ACCOUNT</text>
                    <text class="popup-message">Enter recharge amount for {{ selectedRoom ? selectedRoom.name : 'room' }}</text>
                    <input 
                        v-model="rechargeAmount" 
                        class="amount-input" 
                        type="number" 
                        placeholder="Enter amount" 
                        placeholder-style="color: rgba(0, 245, 255, 0.4)"
                    />
                    <view class="popup-buttons">
                        <button class="btn cancel-btn" @click="$refs.rechargePopup.close()">CANCEL</button>
                        <button class="btn confirm-btn" @click="confirmRecharge">RECHARGE</button>
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
            balance: null,
            consumption: null,
            bills: [],
            rechargeAmount: '',
            loading: false
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
                this.loadBillingData()
            }
        },
        
        onRoomChange(e) {
            const index = e.detail.value
            this.selectedRoom = this.rooms[index]
            this.loadBillingData()
        },
        
        async loadBillingData() {
            if (!this.selectedRoom) return
            
            try {
                const [balance, consumption, bills] = await Promise.all([
                    api.billing.getRoomBalance(this.selectedRoom.id),
                    api.billing.getConsumption(this.selectedRoom.id),
                    api.billing.getBills(this.selectedRoom.id)
                ])
                
                this.balance = balance
                this.consumption = consumption
                this.bills = bills
            } catch (error) {
                console.error('加载计费数据失败:', error)
            }
        },
        
        async refreshBalance() {
            if (!this.selectedRoom) return
            
            try {
                this.balance = await api.billing.getRoomBalance(this.selectedRoom.id)
                uni.showToast({
                    title: 'Balance refreshed',
                    icon: 'success'
                })
            } catch (error) {
                console.error('刷新余额失败:', error)
            }
        },
        
        showRechargeDialog() {
            this.rechargeAmount = ''
            this.$refs.rechargePopup.open()
        },
        
        async confirmRecharge() {
            if (!this.selectedRoom || !this.rechargeAmount) return
            
            try {
                await api.billing.recharge(this.selectedRoom.id, {
                    amount: parseFloat(this.rechargeAmount)
                })
                
                uni.showToast({
                    title: 'Recharge successful',
                    icon: 'success'
                })
                
                this.$refs.rechargePopup.close()
                await this.loadBillingData()
            } catch (error) {
                console.error('Recharge failed:', error)
                uni.showToast({
                    title: 'Recharge failed',
                    icon: 'none'
                })
            }
        },
        
        async payBill(billId) {
            try {
                await api.billing.payBill(billId)
                
                uni.showToast({
                    title: 'Payment successful',
                    icon: 'success'
                })
                
                await this.loadBillingData()
            } catch (error) {
                console.error('Payment failed:', error)
                uni.showToast({
                    title: 'Payment failed',
                    icon: 'none'
                })
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

.billing-header {
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

.balance-section {
    position: relative;
    padding: 0 32rpx 32rpx;
    z-index: 10;
    
    .balance-hologram {
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
        
        .balance-content {
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
            
            .balance-card {
                background: rgba(0, 245, 255, 0.05);
                border: 1rpx solid rgba(0, 245, 255, 0.2);
                border-radius: 16rpx;
                padding: 32rpx;
                
                .balance-info {
                    text-align: center;
                    margin-bottom: 32rpx;
                    
                    .balance-label {
                        display: block;
                        font-size: 24rpx;
                        color: rgba(0, 245, 255, 0.6);
                        margin-bottom: 16rpx;
                    }
                    
                    .balance-amount {
                        display: block;
                        font-size: 48rpx;
                        font-weight: 700;
                        color: #00F5FF;
                        margin-bottom: 16rpx;
                        text-shadow: 0 0 20rpx rgba(0, 245, 255, 0.5);
                    }
                    
                    .balance-status {
                        display: block;
                        font-size: 24rpx;
                        font-weight: 600;
                        
                        &.warning {
                            color: #FED330;
                        }
                        
                        &.danger {
                            color: #FF4757;
                        }
                    }
                }
                
                .balance-actions {
                    display: flex;
                    gap: 16rpx;
                    
                    .balance-btn {
                        flex: 1;
                        padding: 16rpx 0;
                        background: rgba(0, 245, 255, 0.1);
                        border: 1rpx solid rgba(0, 245, 255, 0.2);
                        border-radius: 12rpx;
                        font-size: 24rpx;
                        font-weight: 600;
                        color: #00F5FF;
                    }
                }
            }
        }
    }
}

.consumption-section {
    position: relative;
    padding: 0 32rpx 32rpx;
    z-index: 10;
    
    .consumption-hologram {
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
        
        .consumption-content {
            .consumption-grid {
                display: grid;
                grid-template-columns: repeat(3, 1fr);
                gap: 16rpx;
                
                .consumption-item {
                    background: rgba(0, 245, 255, 0.05);
                    border: 1rpx solid rgba(0, 245, 255, 0.2);
                    border-radius: 16rpx;
                    padding: 24rpx;
                    text-align: center;
                    
                    .consumption-label {
                        display: block;
                        font-size: 20rpx;
                        color: rgba(0, 245, 255, 0.6);
                        margin-bottom: 12rpx;
                    }
                    
                    .consumption-value {
                        display: block;
                        font-size: 28rpx;
                        font-weight: 600;
                        color: #00F5FF;
                        margin-bottom: 8rpx;
                    }
                    
                    .consumption-cost {
                        display: block;
                        font-size: 24rpx;
                        color: #26DE81;
                    }
                }
            }
        }
    }
}

.bills-section {
    position: relative;
    padding: 0 32rpx;
    z-index: 10;
    
    .bills-hologram {
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
        
        .bills-content {
            .bill-item {
                padding: 24rpx 32rpx;
                border-bottom: 1rpx solid rgba(0, 245, 255, 0.1);
                
                &:last-child {
                    border-bottom: none;
                }
                
                &:nth-child(even) {
                    background: rgba(0, 245, 255, 0.02);
                }
                
                .bill-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 16rpx;
                    
                    .bill-period {
                        font-size: 24rpx;
                        font-weight: 600;
                        color: #00F5FF;
                    }
                    
                    .bill-status {
                        font-size: 20rpx;
                        font-weight: 600;
                        padding: 4rpx 12rpx;
                        border-radius: 8rpx;
                        
                        &.unpaid {
                            color: #FF4757;
                            background: rgba(255, 71, 87, 0.1);
                            border: 1rpx solid rgba(255, 71, 87, 0.3);
                        }
                        
                        &.paid {
                            color: #26DE81;
                            background: rgba(38, 222, 129, 0.1);
                            border: 1rpx solid rgba(38, 222, 129, 0.3);
                        }
                    }
                }
                
                .bill-details {
                    margin-bottom: 20rpx;
                    
                    .bill-detail {
                        display: flex;
                        justify-content: space-between;
                        margin-bottom: 8rpx;
                        
                        &:last-child {
                            margin-bottom: 0;
                        }
                        
                        .detail-label {
                            font-size: 22rpx;
                            color: rgba(0, 245, 255, 0.6);
                        }
                        
                        .detail-value {
                            font-size: 22rpx;
                            color: #00F5FF;
                        }
                    }
                }
                
                .pay-btn {
                    width: 100%;
                    padding: 12rpx 0;
                    background: rgba(0, 245, 255, 0.1);
                    border: 1rpx solid rgba(0, 245, 255, 0.2);
                    border-radius: 12rpx;
                    font-size: 22rpx;
                    font-weight: 600;
                    color: #00F5FF;
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
        
        .amount-input {
            width: 100%;
            background: rgba(0, 245, 255, 0.05);
            border: 1rpx solid rgba(0, 245, 255, 0.2);
            border-radius: 12rpx;
            padding: 16rpx;
            color: #00F5FF;
            font-size: 24rpx;
            margin-bottom: 24rpx;
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