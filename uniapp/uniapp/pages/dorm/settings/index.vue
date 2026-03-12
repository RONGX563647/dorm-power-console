<template>
    <view class="settings-container">
        <view class="settings-section">
            <view class="section-title">账号设置</view>
            <view class="settings-list">
                <view class="setting-item" @click="goToEditProfile">
                    <text class="setting-label">编辑资料</text>
                    <uni-icons type="forward" size="16" color="#ccc"></uni-icons>
                </view>
                <view class="setting-item" @click="goToChangePassword">
                    <text class="setting-label">修改密码</text>
                    <uni-icons type="forward" size="16" color="#ccc"></uni-icons>
                </view>
                <view class="setting-item" @click="goToNotificationSettings">
                    <text class="setting-label">通知设置</text>
                    <uni-icons type="forward" size="16" color="#ccc"></uni-icons>
                </view>
            </view>
        </view>
        
        <view class="settings-section">
            <view class="section-title">应用设置</view>
            <view class="settings-list">
                <view class="setting-item">
                    <text class="setting-label">深色模式</text>
                    <switch :checked="darkMode" @change="toggleDarkMode" color="#2A7965" />
                </view>
                <view class="setting-item">
                    <text class="setting-label">自动刷新</text>
                    <switch :checked="autoRefresh" @change="toggleAutoRefresh" color="#2A7965" />
                </view>
            </view>
        </view>
        
        <view class="settings-section">
            <view class="section-title">关于</view>
            <view class="settings-list">
                <view class="setting-item" @click="goToAbout">
                    <text class="setting-label">关于我们</text>
                    <uni-icons type="forward" size="16" color="#ccc"></uni-icons>
                </view>
                <view class="setting-item">
                    <text class="setting-label">版本号</text>
                    <text class="setting-value">v1.0.0</text>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
export default {
    data() {
        return {
            darkMode: false,
            autoRefresh: true
        }
    },
    methods: {
        goToEditProfile() {
            uni.navigateTo({
                url: '/pages/dorm/profile/edit'
            })
        },
        
        goToChangePassword() {
            uni.navigateTo({
                url: '/pages/dorm/profile/password'
            })
        },
        
        goToNotificationSettings() {
            uni.navigateTo({
                url: '/pages/dorm/profile/notification-settings'
            })
        },
        
        goToAbout() {
            uni.navigateTo({
                url: '/pages/dorm/about/index'
            })
        },
        
        toggleDarkMode(e) {
            this.darkMode = e.detail.value
            uni.showToast({
                title: '深色模式切换成功',
                icon: 'none'
            })
        },
        
        toggleAutoRefresh(e) {
            this.autoRefresh = e.detail.value
            uni.showToast({
                title: this.autoRefresh ? '已开启自动刷新' : '已关闭自动刷新',
                icon: 'none'
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.settings-container {
    min-height: 100vh;
    background: #F2F7F5;
    padding: 24rpx;
}

.settings-section {
    background: #fff;
    border-radius: 20rpx;
    margin-bottom: 24rpx;
    overflow: hidden;
    
    .section-title {
        padding: 24rpx 24rpx 12rpx;
        font-size: 26rpx;
        color: #66A392;
    }
    
    .settings-list {
        .setting-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 28rpx 24rpx;
            border-top: 1rpx solid #E8F5F0;
            
            .setting-label {
                font-size: 28rpx;
                color: #2A7965;
            }
            
            .setting-value {
                font-size: 26rpx;
                color: #66A392;
            }
        }
    }
}
</style>
