<template>
    <view class="about-container">
        <view class="header">
            <view class="logo-section">
                <image class="logo" src="/static/logo.png" mode="aspectFit"></image>
                <text class="app-name">宿舍用电管理系统</text>
                <text class="app-version">版本 v1.0.0</text>
            </view>
        </view>
        
        <view class="info-section">
            <view class="info-card">
                <view class="info-item">
                    <text class="info-label">开发者</text>
                    <text class="info-value">宿舍用电管理团队</text>
                </view>
                <view class="info-item">
                    <text class="info-label">联系邮箱</text>
                    <text class="info-value">support@dorm.local</text>
                </view>
                <view class="info-item">
                    <text class="info-label">官方网站</text>
                    <text class="info-value">www.dorm.local</text>
                </view>
            </view>
        </view>
        
        <view class="intro-section">
            <view class="section-title">应用简介</view>
            <view class="intro-text">
                <text>宿舍用电管理系统是一款专为宿舍设计的智能用电监控和管理应用。</text>
            </view>
            <view class="feature-list">
                <view class="feature-item">
                    <uni-icons type="checkmarkempty" size="18" color="#2A7965"></uni-icons>
                    <text>实时监测设备用电情况</text>
                </view>
                <view class="feature-item">
                    <uni-icons type="checkmarkempty" size="18" color="#2A7965"></uni-icons>
                    <text>远程控制设备开关</text>
                </view>
                <view class="feature-item">
                    <uni-icons type="checkmarkempty" size="18" color="#2A7965"></uni-icons>
                    <text>智能AI用电分析报告</text>
                </view>
                <view class="feature-item">
                    <uni-icons type="checkmarkempty" size="18" color="#2A7965"></uni-icons>
                    <text>节能降耗，绿色环保</text>
                </view>
            </view>
        </view>
        
        <view class="copyright">
            <text>© 2024 宿舍用电管理系统. All rights reserved.</text>
        </view>
    </view>
</template>

<script>
export default {
    data() {
        return {}
    }
}
</script>

<style lang="scss" scoped>
.about-container {
    min-height: 100vh;
    background: #F2F7F5;
}

.header {
    background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
    padding: 60rpx 0 80rpx;
    
    .logo-section {
        display: flex;
        flex-direction: column;
        align-items: center;
        
        .logo {
            width: 160rpx;
            height: 160rpx;
            border-radius: 40rpx;
            background: #fff;
            padding: 20rpx;
            margin-bottom: 20rpx;
        }
        
        .app-name {
            font-size: 36rpx;
            font-weight: 600;
            color: #fff;
            margin-bottom: 8rpx;
        }
        
        .app-version {
            font-size: 24rpx;
            color: rgba(255, 255, 255, 0.7);
        }
    }
}

.info-section {
    padding: 24rpx;
    
    .info-card {
        background: #fff;
        border-radius: 20rpx;
        padding: 24rpx;
        
        .info-item {
            display: flex;
            justify-content: space-between;
            padding: 20rpx 0;
            border-bottom: 1rpx solid #E8F5F0;
            
            &:last-child {
                border-bottom: none;
            }
            
            .info-label {
                font-size: 28rpx;
                color: #2A7965;
            }
            
            .info-value {
                font-size: 26rpx;
                color: #66A392;
            }
        }
    }
}

.intro-section {
    padding: 0 24rpx 24rpx;
    
    .section-title {
        font-size: 30rpx;
        font-weight: 600;
        color: #2A7965;
        margin-bottom: 16rpx;
    }
    
    .intro-text {
        background: #fff;
        border-radius: 20rpx;
        padding: 24rpx;
        margin-bottom: 16rpx;
        
        text {
            font-size: 28rpx;
            color: #2A7965;
            line-height: 1.6;
        }
    }
    
    .feature-list {
        background: #fff;
        border-radius: 20rpx;
        padding: 24rpx;
        
        .feature-item {
            display: flex;
            align-items: center;
            padding: 16rpx 0;
            
            text {
                margin-left: 12rpx;
                font-size: 28rpx;
                color: #2A7965;
            }
        }
    }
}

.copyright {
    text-align: center;
    padding: 40rpx 0;
    
    text {
        font-size: 24rpx;
        color: #66A392;
    }
}
</style>
