<template>
    <view class="notification-settings-container">
        <view class="settings-section">
            <view class="setting-item">
                <view class="setting-info">
                    <text class="setting-title">推送通知</text>
                    <text class="setting-desc">接收系统推送通知</text>
                </view>
                <switch :checked="settings.pushEnabled" @change="onPushChange" color="#2A7965" />
            </view>
            
            <view class="setting-item">
                <view class="setting-info">
                    <text class="setting-title">告警通知</text>
                    <text class="setting-desc">设备告警时推送通知</text>
                </view>
                <switch :checked="settings.alertEnabled" @change="onAlertChange" color="#2A7965" />
            </view>
            
            <view class="setting-item">
                <view class="setting-info">
                    <text class="setting-title">用电报告</text>
                    <text class="setting-desc">每日用电报告推送</text>
                </view>
                <switch :checked="settings.reportEnabled" @change="onReportChange" color="#2A7965" />
            </view>
            
            <view class="setting-item">
                <view class="setting-info">
                    <text class="setting-title">邮件通知</text>
                    <text class="setting-desc">重要通知发送邮件</text>
                </view>
                <switch :checked="settings.emailEnabled" @change="onEmailChange" color="#2A7965" />
            </view>
        </view>
        
        <view class="quiet-section">
            <view class="section-title">免打扰时段</view>
            
            <view class="quiet-item">
                <text class="quiet-label">开始时间</text>
                <picker mode="time" :value="settings.quietStart" @change="onQuietStartChange">
                    <view class="picker-value">
                        {{ settings.quietStart || '22:00' }}
                        <uni-icons type="arrowdown" size="16" color="#666"></uni-icons>
                    </view>
                </picker>
            </view>
            
            <view class="quiet-item">
                <text class="quiet-label">结束时间</text>
                <picker mode="time" :value="settings.quietEnd" @change="onQuietEndChange">
                    <view class="picker-value">
                        {{ settings.quietEnd || '08:00' }}
                        <uni-icons type="arrowdown" size="16" color="#666"></uni-icons>
                    </view>
                </picker>
            </view>
        </view>
        
        <button class="save-btn" type="primary" @click="handleSave" :loading="loading">保存设置</button>
    </view>
</template>

<script>
import api from '@/api'
import { mapState } from 'vuex'

export default {
    data() {
        return {
            settings: {
                pushEnabled: true,
                alertEnabled: true,
                reportEnabled: false,
                emailEnabled: false,
                quietStart: '22:00',
                quietEnd: '08:00'
            },
            loading: false
        }
    },
    computed: {
        ...mapState(['user'])
    },
    onLoad() {
        this.loadSettings()
    },
    methods: {
        async loadSettings() {
            try {
                const data = await api.notification.getNotificationPreferences({
                    username: this.user?.username
                })
                if (data) {
                    this.settings = { ...this.settings, ...data }
                }
            } catch (error) {
                console.error('加载设置失败:', error)
            }
        },
        
        onPushChange(e) {
            this.settings.pushEnabled = e.detail.value
        },
        
        onAlertChange(e) {
            this.settings.alertEnabled = e.detail.value
        },
        
        onReportChange(e) {
            this.settings.reportEnabled = e.detail.value
        },
        
        onEmailChange(e) {
            this.settings.emailEnabled = e.detail.value
        },
        
        onQuietStartChange(e) {
            this.settings.quietStart = e.detail.value
        },
        
        onQuietEndChange(e) {
            this.settings.quietEnd = e.detail.value
        },
        
        async handleSave() {
            this.loading = true
            try {
                await api.notification.updateNotificationPreferences(this.settings)
                
                uni.showToast({
                    title: '保存成功',
                    icon: 'success'
                })
            } catch (error) {
                console.error('保存失败:', error)
            } finally {
                this.loading = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.notification-settings-container {
    min-height: 100vh;
    background: #F2F7F5;
    padding: 20rpx;
    padding-bottom: 120rpx;
}

.settings-section {
    background: #fff;
    border-radius: 16rpx;
    overflow: hidden;
    
    .setting-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 30rpx;
        border-bottom: 1rpx solid #F2F7F5;
        
        &:last-child {
            border-bottom: none;
        }
        
        .setting-info {
            flex: 1;
            
            .setting-title {
                display: block;
                font-size: 30rpx;
                color: #333;
                margin-bottom: 8rpx;
            }
            
            .setting-desc {
                font-size: 24rpx;
                color: #999;
            }
        }
    }
}

.quiet-section {
    background: #fff;
    border-radius: 16rpx;
    margin-top: 20rpx;
    padding: 30rpx;
    
    .section-title {
        font-size: 30rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 30rpx;
    }
    
    .quiet-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20rpx;
        
        &:last-child {
            margin-bottom: 0;
        }
        
        .quiet-label {
            font-size: 28rpx;
            color: #666;
        }
        
        .picker-value {
            display: flex;
            align-items: center;
            font-size: 28rpx;
            color: #333;
            padding: 16rpx 24rpx;
            background: #F2F7F5;
            border-radius: 8rpx;
        }
    }
}

.save-btn {
    position: fixed;
    left: 30rpx;
    right: 30rpx;
    bottom: 40rpx;
    border-radius: 50rpx;
    height: 88rpx;
    line-height: 88rpx;
    font-size: 32rpx;
}
</style>
