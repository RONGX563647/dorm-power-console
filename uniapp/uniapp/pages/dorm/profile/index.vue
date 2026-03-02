<template>
    <view class="profile-container">
        <view class="profile-header">
            <view class="header-bg"></view>
            <view class="user-card">
                <image class="avatar" src="/static/logo.png" mode="aspectFill"></image>
                <view class="user-info">
                    <text class="username">{{ user ? user.username : '未登录' }}</text>
                    <text class="role-badge">{{ getRoleText(user?.role) }}</text>
                </view>
            </view>
        </view>
        
        <view class="menu-section">
            <view class="menu-group">
                <view class="menu-item" @click="goToSettings">
                    <view class="menu-icon primary">
                        <uni-icons type="gear" size="20" color="#fff"></uni-icons>
                    </view>
                    <text class="menu-text">设置</text>
                    <uni-icons type="forward" size="16" color="#ccc"></uni-icons>
                </view>
                
                <view class="menu-item" @click="goToAbout">
                    <view class="menu-icon secondary">
                        <uni-icons type="info" size="20" color="#fff"></uni-icons>
                    </view>
                    <text class="menu-text">关于</text>
                    <uni-icons type="forward" size="16" color="#ccc"></uni-icons>
                </view>
            </view>
        </view>
        
        <view class="logout-section">
            <button class="logout-btn" @click="logout">退出登录</button>
        </view>
    </view>
</template>

<script>
import { mapState, mapActions } from 'vuex'

export default {
    data() {
        return {}
    },
    computed: {
        ...mapState(['user'])
    },
    onShow() {
        this.loadUser()
    },
    methods: {
        ...mapActions(['getCurrentUser', 'logoutAction']),
        
        async loadUser() {
            try {
                await this.getCurrentUser()
            } catch (error) {
                console.error('加载用户信息失败:', error)
            }
        },
        
        getRoleText(role) {
            const roleMap = {
                'ADMIN': '管理员',
                'admin': '管理员',
                'USER': '普通用户',
                'user': '普通用户'
            }
            return roleMap[role] || '用户'
        },
        
        goToSettings() {
            uni.navigateTo({
                url: '/pages/dorm/settings/index'
            })
        },
        
        goToAbout() {
            uni.navigateTo({
                url: '/pages/dorm/about/index'
            })
        },
        
        logout() {
            uni.showModal({
                title: '提示',
                content: '确定要退出登录吗？',
                success: async (res) => {
                    if (res.confirm) {
                        await this.logoutAction()
                        uni.reLaunch({
                            url: '/pages/common/login'
                        })
                    }
                }
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.profile-container {
    min-height: 100vh;
    background: #F2F7F5;
}

.profile-header {
    position: relative;
    padding-top: 20rpx;
    padding-bottom: 40rpx;
    
    .header-bg {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 200rpx;
        background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
        z-index: 0;
    }
    
    .user-card {
        position: relative;
        z-index: 1;
        background: #fff;
        margin: 0 24rpx;
        padding: 40rpx;
        border-radius: 20rpx;
        display: flex;
        align-items: center;
        box-shadow: 0 8rpx 24rpx rgba(42, 121, 101, 0.15);
        
        .avatar {
            width: 100rpx;
            height: 100rpx;
            border-radius: 50%;
            margin-right: 24rpx;
            border: 4rpx solid #E8F5F0;
        }
        
        .user-info {
            flex: 1;
            
            .username {
                display: block;
                font-size: 32rpx;
                font-weight: 600;
                color: #2A7965;
                margin-bottom: 8rpx;
            }
            
            .role-badge {
                display: inline-block;
                padding: 6rpx 16rpx;
                background: #E8F5F0;
                color: #2A7965;
                font-size: 24rpx;
                border-radius: 20rpx;
            }
        }
    }
}

.menu-section {
    padding: 24rpx;
    
    .menu-group {
        background: #fff;
        border-radius: 20rpx;
        overflow: hidden;
        
        .menu-item {
            display: flex;
            align-items: center;
            padding: 28rpx 24rpx;
            border-bottom: 1rpx solid #E8F5F0;
            
            &:last-child {
                border-bottom: none;
            }
            
            .menu-icon {
                width: 48rpx;
                height: 48rpx;
                border-radius: 12rpx;
                display: flex;
                align-items: center;
                justify-content: center;
                margin-right: 16rpx;
                
                &.primary { background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%); }
                &.secondary { background: linear-gradient(135deg, #3D967E 0%, #4AAB8F 100%); }
            }
            
            .menu-text {
                flex: 1;
                font-size: 28rpx;
                color: #2A7965;
            }
        }
    }
}

.logout-section {
    padding: 48rpx 24rpx;
    
    .logout-btn {
        width: 100%;
        height: 88rpx;
        line-height: 88rpx;
        background: #fff;
        color: #1F5C4D;
        font-size: 30rpx;
        border-radius: 44rpx;
        border: 2rpx solid #1F5C4D;
    }
}
</style>
