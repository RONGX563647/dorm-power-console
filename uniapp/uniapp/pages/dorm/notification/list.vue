<template>
    <view class="notification-container">
        <view class="filter-bar">
            <view 
                class="filter-item" 
                :class="{ active: filterType === 'all' }"
                @click="filterType = 'all'"
            >全部</view>
            <view 
                class="filter-item" 
                :class="{ active: filterType === 'unread' }"
                @click="filterType = 'unread'"
            >未读</view>
        </view>
        
        <scroll-view 
            scroll-y 
            class="notification-scroll"
            @scrolltolower="loadMore"
            refresher-enabled
            :refresher-triggered="refreshing"
            @refresherrefresh="onRefresh"
        >
            <view class="notification-list" v-if="filteredNotifications.length > 0">
                <view 
                    class="notification-item" 
                    :class="{ unread: !item.read }"
                    v-for="item in filteredNotifications" 
                    :key="item.id"
                    @click="markAsRead(item)"
                >
                    <view class="notification-icon" :class="item.type">
                        <uni-icons 
                            :type="getIconType(item.type)" 
                            size="24" 
                            color="#fff"
                        ></uni-icons>
                    </view>
                    <view class="notification-content">
                        <text class="notification-title">{{ item.title }}</text>
                        <text class="notification-message">{{ item.message }}</text>
                        <text class="notification-time">{{ formatTime(item.timestamp) }}</text>
                    </view>
                    <view class="unread-dot" v-if="!item.read"></view>
                </view>
            </view>
            
            <view class="empty-state" v-else>
                <uni-icons type="notification" size="64" color="#ccc"></uni-icons>
                <text class="empty-text">暂无通知</text>
            </view>
            
            <view class="load-more" v-if="hasMore">
                <uni-load-more :status="loadStatus" />
            </view>
        </scroll-view>
        
        <view class="action-bar" v-if="notifications.length > 0">
            <button class="mark-all-btn" @click="markAllAsRead">全部标为已读</button>
        </view>
    </view>
</template>

<script>
import api from '@/api'
import { mapState, mapActions } from 'vuex'

export default {
    data() {
        return {
            filterType: 'all',
            refreshing: false,
            loading: false,
            hasMore: false,
            loadStatus: 'more',
            page: 0,
            pageSize: 20
        }
    },
    computed: {
        ...mapState(['user', 'notifications']),
        filteredNotifications() {
            if (this.filterType === 'unread') {
                return this.notifications.filter(n => !n.read)
            }
            return this.notifications
        }
    },
    onShow() {
        this.loadNotifications()
    },
    methods: {
        ...mapActions(['fetchNotifications', 'fetchUnreadCount']),
        
        async loadNotifications() {
            this.loading = true
            try {
                await this.fetchNotifications({
                    username: this.user?.username,
                    page: this.page,
                    size: this.pageSize
                })
            } catch (error) {
                console.error('加载通知失败:', error)
            } finally {
                this.loading = false
            }
        },
        
        async onRefresh() {
            this.refreshing = true
            this.page = 0
            await this.loadNotifications()
            this.refreshing = false
        },
        
        loadMore() {
            if (this.hasMore && !this.loading) {
                this.page++
                this.loadStatus = 'loading'
                this.loadNotifications().finally(() => {
                    this.loadStatus = 'noMore'
                })
            }
        },
        
        async markAsRead(notification) {
            if (notification.read) return
            
            try {
                // 调用API标记已读
                notification.read = true
                await this.fetchUnreadCount({ username: this.user?.username })
            } catch (error) {
                console.error('标记已读失败:', error)
            }
        },
        
        async markAllAsRead() {
            try {
                // 调用API全部标记已读
                this.notifications.forEach(n => n.read = true)
                await this.fetchUnreadCount({ username: this.user?.username })
                
                uni.showToast({
                    title: '已全部标为已读',
                    icon: 'success'
                })
            } catch (error) {
                console.error('标记已读失败:', error)
            }
        },
        
        getIconType(type) {
            const map = {
                'alert': 'info-filled',
                'info': 'info',
                'warning': 'flag',
                'success': 'checkmarkempty'
            }
            return map[type] || 'notification'
        },
        
        formatTime(timestamp) {
            const date = new Date(timestamp)
            const now = new Date()
            const diff = now - date
            
            if (diff < 60000) return '刚刚'
            if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
            if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
            return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
        }
    }
}
</script>

<style lang="scss" scoped>
.notification-container {
    min-height: 100vh;
    background: #F2F7F5;
    display: flex;
    flex-direction: column;
}

.filter-bar {
    display: flex;
    background: #fff;
    padding: 20rpx 30rpx;
    border-bottom: 1rpx solid #eee;
    
    .filter-item {
        padding: 12rpx 30rpx;
        margin-right: 20rpx;
        font-size: 26rpx;
        color: #666;
        border-radius: 30rpx;
        background: #F2F7F5;
        
        &.active {
            color: #fff;
            background: #2A7965;
        }
    }
}

.notification-scroll {
    flex: 1;
    height: 0;
}

.notification-list {
    .notification-item {
        display: flex;
        align-items: flex-start;
        background: #fff;
        padding: 30rpx;
        border-bottom: 1rpx solid #F2F7F5;
        
        &.unread {
            background: #f0f5ff;
        }
        
        .notification-icon {
            width: 80rpx;
            height: 80rpx;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-right: 20rpx;
            
            &.alert {
                background: #1F5C4D;
            }
            
            &.warning {
                background: #3D967E;
            }
            
            &.info {
                background: #2A7965;
            }
            
            &.success {
                background: #2A7965;
            }
        }
        
        .notification-content {
            flex: 1;
            
            .notification-title {
                display: block;
                font-size: 30rpx;
                color: #333;
                font-weight: bold;
                margin-bottom: 8rpx;
            }
            
            .notification-message {
                display: block;
                font-size: 26rpx;
                color: #666;
                margin-bottom: 8rpx;
                line-height: 1.4;
            }
            
            .notification-time {
                font-size: 24rpx;
                color: #999;
            }
        }
        
        .unread-dot {
            width: 16rpx;
            height: 16rpx;
            border-radius: 50%;
            background: #1F5C4D;
            margin-top: 10rpx;
        }
    }
}

.empty-state {
    text-align: center;
    padding: 100rpx 0;
    
    .empty-text {
        display: block;
        margin-top: 20rpx;
        font-size: 28rpx;
        color: #999;
    }
}

.load-more {
    padding: 20rpx;
}

.action-bar {
    padding: 20rpx 30rpx;
    background: #fff;
    border-top: 1rpx solid #eee;
    
    .mark-all-btn {
        width: 100%;
        height: 80rpx;
        line-height: 80rpx;
        background: #2A7965;
        color: #fff;
        border: none;
        border-radius: 40rpx;
        font-size: 28rpx;
    }
}
</style>
