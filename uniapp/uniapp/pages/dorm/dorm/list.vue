<template>
    <view class="dorm-container">
        <view class="building-section">
            <view class="section-header">
                <text class="section-title">楼栋列表</text>
                <text class="section-add" @click="showAddBuilding">添加楼栋</text>
            </view>
            
            <view class="building-list">
                <view 
                    class="building-item" 
                    :class="{ active: selectedBuilding === item.id }"
                    v-for="item in buildings" 
                    :key="item.id"
                    @click="selectBuilding(item.id)"
                >
                    <text class="building-name">{{ item.name }}</text>
                    <text class="building-count">{{ item.roomCount || 0 }}间房</text>
                </view>
            </view>
        </view>
        
        <view class="room-section">
            <view class="section-header">
                <text class="section-title">房间列表</text>
                <text class="section-add" @click="showAddRoom">添加房间</text>
            </view>
            
            <scroll-view 
                scroll-y 
                class="room-scroll"
                refresher-enabled
                :refresher-triggered="refreshing"
                @refresherrefresh="onRefresh"
            >
                <view class="room-list" v-if="rooms.length > 0">
                    <view 
                        class="room-item" 
                        v-for="room in rooms" 
                        :key="room.id"
                        @click="goToRoomDetail(room.id)"
                    >
                        <view class="room-info">
                            <text class="room-number">{{ room.number }}</text>
                            <text class="room-status" :class="room.status">
                                {{ getStatusText(room.status) }}
                            </text>
                        </view>
                        <view class="room-occupants">
                            <text class="occupant-count">{{ room.occupants || 0 }}/{{ room.capacity || 4 }}人</text>
                        </view>
                        <uni-icons type="forward" size="20" color="#ccc"></uni-icons>
                    </view>
                </view>
                
                <view class="empty-state" v-else>
                    <uni-icons type="home" size="64" color="#ccc"></uni-icons>
                    <text class="empty-text">暂无房间</text>
                </view>
            </scroll-view>
        </view>
    </view>
</template>

<script>
import api from '@/api'
import { mapState, mapActions, mapGetters } from 'vuex'

export default {
    data() {
        return {
            selectedBuilding: '',
            rooms: [],
            refreshing: false
        }
    },
    computed: {
        ...mapState(['buildings']),
        ...mapGetters(['isAdmin'])
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
        this.loadBuildings()
    },
    methods: {
        ...mapActions(['fetchBuildings']),
        
        async loadBuildings() {
            try {
                await this.fetchBuildings()
                if (this.buildings.length > 0) {
                    this.selectedBuilding = this.buildings[0].id
                    this.loadRooms()
                }
            } catch (error) {
                console.error('加载楼栋失败:', error)
            }
        },
        
        async loadRooms() {
            if (!this.selectedBuilding) return
            
            try {
                this.rooms = await api.dorm.getRooms({ buildingId: this.selectedBuilding })
            } catch (error) {
                console.error('加载房间失败:', error)
            }
        },
        
        selectBuilding(buildingId) {
            this.selectedBuilding = buildingId
            this.loadRooms()
        },
        
        async onRefresh() {
            this.refreshing = true
            await this.loadRooms()
            this.refreshing = false
        },
        
        getStatusText(status) {
            const map = {
                'empty': '空闲',
                'partial': '部分入住',
                'full': '已满'
            }
            return map[status] || status
        },
        
        goToRoomDetail(roomId) {
            uni.navigateTo({
                url: `/pages/dorm/dorm/room-detail?id=${roomId}`
            })
        },
        
        showAddBuilding() {
            uni.showModal({
                title: '添加楼栋',
                editable: true,
                placeholderText: '请输入楼栋名称',
                success: (res) => {
                    if (res.confirm && res.content) {
                        this.addBuilding(res.content)
                    }
                }
            })
        },
        
        async addBuilding(name) {
            try {
                // 调用API添加楼栋
                uni.showToast({
                    title: '添加成功',
                    icon: 'success'
                })
                this.loadBuildings()
            } catch (error) {
                console.error('添加失败:', error)
            }
        },
        
        showAddRoom() {
            if (!this.selectedBuilding) {
                uni.showToast({
                    title: '请先选择楼栋',
                    icon: 'none'
                })
                return
            }
            
            uni.showModal({
                title: '添加房间',
                editable: true,
                placeholderText: '请输入房间号',
                success: (res) => {
                    if (res.confirm && res.content) {
                        this.addRoom(res.content)
                    }
                }
            })
        },
        
        async addRoom(number) {
            try {
                // 调用API添加房间
                uni.showToast({
                    title: '添加成功',
                    icon: 'success'
                })
                this.loadRooms()
            } catch (error) {
                console.error('添加失败:', error)
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.dorm-container {
    min-height: 100vh;
    background: #F2F7F5;
    display: flex;
    flex-direction: column;
}

.building-section {
    background: #fff;
    
    .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 30rpx;
        border-bottom: 1rpx solid #F2F7F5;
        
        .section-title {
            font-size: 32rpx;
            font-weight: bold;
            color: #333;
        }
        
        .section-add {
            font-size: 26rpx;
            color: #2A7965;
        }
    }
    
    .building-list {
        display: flex;
        padding: 20rpx 30rpx;
        overflow-x: auto;
        
        .building-item {
            flex-shrink: 0;
            padding: 16rpx 30rpx;
            margin-right: 20rpx;
            background: #F2F7F5;
            border-radius: 30rpx;
            text-align: center;
            
            &.active {
                background: #2A7965;
                
                .building-name, .building-count {
                    color: #fff;
                }
            }
            
            .building-name {
                display: block;
                font-size: 28rpx;
                color: #333;
                margin-bottom: 4rpx;
            }
            
            .building-count {
                font-size: 22rpx;
                color: #999;
            }
        }
    }
}

.room-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    margin-top: 20rpx;
    background: #fff;
    
    .room-scroll {
        flex: 1;
        height: 0;
    }
    
    .room-list {
        .room-item {
            display: flex;
            align-items: center;
            padding: 30rpx;
            border-bottom: 1rpx solid #F2F7F5;
            
            .room-info {
                flex: 1;
                
                .room-number {
                    display: block;
                    font-size: 30rpx;
                    color: #333;
                    margin-bottom: 8rpx;
                }
                
                .room-status {
                    font-size: 24rpx;
                    padding: 4rpx 16rpx;
                    border-radius: 20rpx;
                    
                    &.empty {
                        color: #2A7965;
                        background: rgba(76, 175, 80, 0.1);
                    }
                    
                    &.partial {
                        color: #3D967E;
                        background: rgba(255, 152, 0, 0.1);
                    }
                    
                    &.full {
                        color: #1F5C4D;
                        background: rgba(245, 90, 90, 0.1);
                    }
                }
            }
            
            .room-occupants {
                margin-right: 20rpx;
                
                .occupant-count {
                    font-size: 26rpx;
                    color: #666;
                }
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
        color: #999;
    }
}
</style>
