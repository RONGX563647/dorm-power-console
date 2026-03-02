<template>
    <view class="user-manage-container">
        <view class="search-bar">
            <uni-search-bar 
                v-model="searchText" 
                placeholder="搜索用户" 
                @confirm="handleSearch"
                @clear="handleClear"
            />
        </view>
        
        <scroll-view 
            scroll-y 
            class="user-scroll"
            refresher-enabled
            :refresher-triggered="refreshing"
            @refresherrefresh="onRefresh"
        >
            <view class="user-list" v-if="filteredUsers.length > 0">
                <view 
                    class="user-item" 
                    v-for="user in filteredUsers" 
                    :key="user.id"
                    @click="goToUserDetail(user.id)"
                >
                    <view class="user-avatar">
                        <uni-icons type="person-filled" size="32" color="#fff"></uni-icons>
                    </view>
                    <view class="user-info">
                        <text class="user-name">{{ user.username }}</text>
                        <text class="user-email">{{ user.email }}</text>
                    </view>
                    <view class="user-role" :class="user.role.toLowerCase()">
                        {{ getRoleText(user.role) }}
                    </view>
                    <uni-icons type="forward" size="20" color="#ccc"></uni-icons>
                </view>
            </view>
            
            <view class="empty-state" v-else>
                <uni-icons type="person" size="64" color="#ccc"></uni-icons>
                <text class="empty-text">暂无用户</text>
            </view>
        </scroll-view>
        
        <view class="fab-btn" @click="goToAddUser">
            <uni-icons type="plus" size="28" color="#fff"></uni-icons>
        </view>
    </view>
</template>

<script>
import api from '@/api'
import { mapGetters } from 'vuex'

export default {
    data() {
        return {
            searchText: '',
            refreshing: false,
            users: []
        }
    },
    computed: {
        ...mapGetters(['isAdmin']),
        filteredUsers() {
            if (!this.searchText) return this.users
            return this.users.filter(u => 
                u.username.includes(this.searchText) || 
                u.email.includes(this.searchText)
            )
        }
    },
    onShow() {
        if (!this.isAdmin) {
            uni.showToast({
                title: '无权限访问',
                icon: 'none'
            })
            setTimeout(() => {
                uni.navigateBack()
            }, 1500)
            return
        }
        this.loadUsers()
    },
    methods: {
        async loadUsers() {
            try {
                this.users = await api.user.getUserList()
            } catch (error) {
                console.error('加载用户失败:', error)
            }
        },
        
        async onRefresh() {
            this.refreshing = true
            await this.loadUsers()
            this.refreshing = false
        },
        
        handleSearch() {
            // 搜索已通过计算属性自动处理
        },
        
        handleClear() {
            this.searchText = ''
        },
        
        getRoleText(role) {
            const map = {
                'ADMIN': '管理员',
                'USER': '普通用户'
            }
            return map[role] || role
        },
        
        goToUserDetail(userId) {
            uni.navigateTo({
                url: `/pages/dorm/user/detail?id=${userId}`
            })
        },
        
        goToAddUser() {
            uni.navigateTo({
                url: '/pages/dorm/user/add'
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.user-manage-container {
    min-height: 100vh;
    background: #F2F7F5;
    display: flex;
    flex-direction: column;
}

.search-bar {
    background: #fff;
    padding: 20rpx;
}

.user-scroll {
    flex: 1;
    height: 0;
}

.user-list {
    .user-item {
        display: flex;
        align-items: center;
        background: #fff;
        padding: 30rpx;
        border-bottom: 1rpx solid #E8F5F0;
        
        .user-avatar {
            width: 80rpx;
            height: 80rpx;
            border-radius: 50%;
            background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            margin-right: 20rpx;
        }
        
        .user-info {
            flex: 1;
            
            .user-name {
                display: block;
                font-size: 30rpx;
                color: #2A7965;
                margin-bottom: 8rpx;
            }
            
            .user-email {
                font-size: 24rpx;
                color: #66A392;
            }
        }
        
        .user-role {
            font-size: 24rpx;
            padding: 4rpx 16rpx;
            border-radius: 20rpx;
            margin-right: 20rpx;
            
            &.admin {
                color: #fff;
                background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
            }
            
            &.user {
                color: #66A392;
                background: #F2F7F5;
            }
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
        color: #66A392;
    }
}

.fab-btn {
    position: fixed;
    right: 40rpx;
    bottom: 120rpx;
    width: 100rpx;
    height: 100rpx;
    border-radius: 50%;
    background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8rpx 20rpx rgba(42, 121, 101, 0.4);
}
</style>
