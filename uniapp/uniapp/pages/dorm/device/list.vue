<template>
    <view class="device-list-container">
        <view class="search-bar">
            <uni-search-bar 
                v-model="searchText" 
                placeholder="搜索设备" 
                @confirm="handleSearch"
                @clear="handleClear"
            />
        </view>
        
        <view class="filter-bar">
            <view 
                class="filter-item" 
                :class="{ active: filterStatus === 'all' }"
                @click="filterStatus = 'all'"
            >
                全部
            </view>
            <view 
                class="filter-item" 
                :class="{ active: filterStatus === 'online' }"
                @click="filterStatus = 'online'"
            >
                在线
            </view>
            <view 
                class="filter-item" 
                :class="{ active: filterStatus === 'offline' }"
                @click="filterStatus = 'offline'"
            >
                离线
            </view>
        </view>
        
        <scroll-view 
            scroll-y 
            class="device-scroll"
            @scrolltolower="loadMore"
            refresher-enabled
            :refresher-triggered="refreshing"
            @refresherrefresh="onRefresh"
        >
            <view class="device-list" v-if="filteredDevices.length > 0">
                <view 
                    class="device-card" 
                    v-for="device in filteredDevices" 
                    :key="device.id"
                    @click="goToDetail(device.id)"
                >
                    <view class="device-header">
                        <view class="device-icon" :class="{ online: device.online }">
                            <uni-icons type="staff" size="32" color="#fff"></uni-icons>
                        </view>
                        <view class="device-info">
                            <text class="device-name">{{ device.name }}</text>
                            <text class="device-room">{{ device.room }}</text>
                        </view>
                        <view class="device-status" :class="{ online: device.online }">
                            {{ device.online ? '在线' : '离线' }}
                        </view>
                    </view>
                    
                    <view class="device-power" v-if="device.power !== undefined">
                        <text class="power-label">当前功率</text>
                        <text class="power-value">{{ device.power }}W</text>
                    </view>
                </view>
            </view>
            
            <view class="empty-state" v-else>
                <uni-icons type="search" size="64" color="#ccc"></uni-icons>
                <text class="empty-text">暂无设备</text>
            </view>
            
            <view class="load-more" v-if="hasMore">
                <uni-load-more :status="loadStatus" />
            </view>
        </scroll-view>
    </view>
</template>

<script>
import { mapState, mapActions } from 'vuex'

export default {
    data() {
        return {
            searchText: '',
            filterStatus: 'all',
            refreshing: false,
            loading: false,
            hasMore: false,
            loadStatus: 'more'
        }
    },
    computed: {
        ...mapState(['devices']),
        filteredDevices() {
            let result = this.devices
            
            if (this.searchText) {
                result = result.filter(d => 
                    d.name.includes(this.searchText) || 
                    d.room.includes(this.searchText)
                )
            }
            
            if (this.filterStatus === 'online') {
                result = result.filter(d => d.online)
            } else if (this.filterStatus === 'offline') {
                result = result.filter(d => !d.online)
            }
            
            return result
        }
    },
    onShow() {
        this.loadDevices()
    },
    methods: {
        ...mapActions(['fetchDevices']),
        
        async loadDevices() {
            this.loading = true
            try {
                await this.fetchDevices()
            } catch (error) {
                console.error('加载设备失败:', error)
            } finally {
                this.loading = false
            }
        },
        
        async onRefresh() {
            this.refreshing = true
            await this.loadDevices()
            this.refreshing = false
        },
        
        loadMore() {
            if (this.hasMore && !this.loading) {
                this.loadStatus = 'loading'
                setTimeout(() => {
                    this.loadStatus = 'noMore'
                }, 1000)
            }
        },
        
        handleSearch() {
            // 搜索已通过计算属性自动处理
        },
        
        handleClear() {
            this.searchText = ''
        },
        
        goToDetail(deviceId) {
            uni.navigateTo({
                url: `/pages/dorm/device/detail?id=${deviceId}`
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.device-list-container {
    min-height: 100vh;
    background: #F2F7F5;
    display: flex;
    flex-direction: column;
}

.search-bar {
    background: #fff;
    padding: 20rpx;
}

.filter-bar {
    display: flex;
    background: #fff;
    padding: 20rpx 30rpx;
    border-bottom: 1rpx solid #E8F5F0;
    
    .filter-item {
        padding: 12rpx 30rpx;
        margin-right: 20rpx;
        font-size: 26rpx;
        color: #66A392;
        border-radius: 30rpx;
        background: #F2F7F5;
        
        &.active {
            color: #fff;
            background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
        }
    }
}

.device-scroll {
    flex: 1;
    height: 0;
}

.device-list {
    padding: 20rpx;
    
    .device-card {
        background: #fff;
        border-radius: 20rpx;
        padding: 30rpx;
        margin-bottom: 20rpx;
        box-shadow: 0 4rpx 12rpx rgba(42, 121, 101, 0.1);
        
        .device-header {
            display: flex;
            align-items: center;
            margin-bottom: 20rpx;
            
            .device-icon {
                width: 80rpx;
                height: 80rpx;
                border-radius: 50%;
                background: #999;
                display: flex;
                align-items: center;
                justify-content: center;
                margin-right: 20rpx;
                
                &.online {
                    background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
                }
            }
            
            .device-info {
                flex: 1;
                
                .device-name {
                    display: block;
                    font-size: 30rpx;
                    color: #2A7965;
                    font-weight: bold;
                    margin-bottom: 8rpx;
                }
                
                .device-room {
                    font-size: 24rpx;
                    color: #66A392;
                }
            }
            
            .device-status {
                font-size: 24rpx;
                color: #66A392;
                padding: 8rpx 20rpx;
                border-radius: 20rpx;
                background: #F2F7F5;
                
                &.online {
                    color: #2A7965;
                    background: rgba(42, 121, 101, 0.1);
                }
            }
        }
        
        .device-power {
            display: flex;
            justify-content: space-between;
            padding-top: 20rpx;
            border-top: 1rpx solid #E8F5F0;
            
            .power-label {
                font-size: 26rpx;
                color: #66A392;
            }
            
            .power-value {
                font-size: 26rpx;
                color: #2A7965;
                font-weight: bold;
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

.load-more {
    padding: 20rpx;
}
</style>
