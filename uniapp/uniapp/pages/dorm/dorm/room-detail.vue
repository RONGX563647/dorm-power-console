<template>
    <view class="room-detail-container">
        <view class="room-header" v-if="room">
            <text class="room-number">{{ room.number }}</text>
            <text class="room-building">{{ room.buildingName }}</text>
            <view class="room-status" :class="room.status">
                {{ getStatusText(room.status) }}
            </view>
        </view>
        
        <view class="occupants-section">
            <view class="section-header">
                <text class="section-title">入住人员 ({{ room?.occupants || 0 }}/{{ room?.capacity || 4 }})</text>
                <text class="section-add" @click="showCheckIn" v-if="canCheckIn">办理入住</text>
            </view>
            
            <view class="occupant-list" v-if="occupants.length > 0">
                <view class="occupant-item" v-for="item in occupants" :key="item.id">
                    <view class="occupant-avatar">
                        <uni-icons type="person-filled" size="24" color="#fff"></uni-icons>
                    </view>
                    <view class="occupant-info">
                        <text class="occupant-name">{{ item.name }}</text>
                        <text class="occupant-phone">{{ item.phone }}</text>
                    </view>
                    <button class="check-out-btn" @click="handleCheckOut(item)">退宿</button>
                </view>
            </view>
            
            <view class="empty-state" v-else>
                <text class="empty-text">暂无入住人员</text>
            </view>
        </view>
        
        <view class="device-section">
            <view class="section-header">
                <text class="section-title">关联设备</text>
            </view>
            
            <view class="device-list" v-if="devices.length > 0">
                <view 
                    class="device-item" 
                    v-for="device in devices" 
                    :key="device.id"
                    @click="goToDevice(device.id)"
                >
                    <view class="device-icon" :class="{ online: device.online }">
                        <uni-icons type="staff" size="24" color="#fff"></uni-icons>
                    </view>
                    <view class="device-info">
                        <text class="device-name">{{ device.name }}</text>
                        <text class="device-status">{{ device.online ? '在线' : '离线' }}</text>
                    </view>
                    <uni-icons type="forward" size="20" color="#ccc"></uni-icons>
                </view>
            </view>
            
            <view class="empty-state" v-else>
                <text class="empty-text">暂无关联设备</text>
            </view>
        </view>
    </view>
</template>

<script>
import api from '@/api'

export default {
    data() {
        return {
            roomId: '',
            room: null,
            occupants: [],
            devices: []
        }
    },
    computed: {
        canCheckIn() {
            return this.room && this.room.occupants < this.room.capacity
        }
    },
    onLoad(options) {
        this.roomId = options.id
        this.loadRoomData()
    },
    methods: {
        async loadRoomData() {
            try {
                // 加载房间详情
                // this.room = await api.dorm.getRoomDetail(this.roomId)
                // this.occupants = await api.dorm.getOccupants(this.roomId)
                this.devices = await api.device.getDevicesByRoom(this.roomId)
            } catch (error) {
                console.error('加载房间数据失败:', error)
            }
        },
        
        getStatusText(status) {
            const map = {
                'empty': '空闲',
                'partial': '部分入住',
                'full': '已满'
            }
            return map[status] || status
        },
        
        showCheckIn() {
            uni.showModal({
                title: '办理入住',
                content: '请输入入住人员信息',
                editable: true,
                placeholderText: '请输入姓名',
                success: (res) => {
                    if (res.confirm && res.content) {
                        this.handleCheckIn(res.content)
                    }
                }
            })
        },
        
        async handleCheckIn(name) {
            try {
                await api.dorm.checkIn(this.roomId, { name })
                
                uni.showToast({
                    title: '入住成功',
                    icon: 'success'
                })
                
                this.loadRoomData()
            } catch (error) {
                console.error('入住失败:', error)
            }
        },
        
        handleCheckOut(occupant) {
            uni.showModal({
                title: '确认退宿',
                content: `确定要为 ${occupant.name} 办理退宿吗？`,
                success: (res) => {
                    if (res.confirm) {
                        this.doCheckOut(occupant.id)
                    }
                }
            })
        },
        
        async doCheckOut(occupantId) {
            try {
                await api.dorm.checkOut(this.roomId, { occupantId })
                
                uni.showToast({
                    title: '退宿成功',
                    icon: 'success'
                })
                
                this.loadRoomData()
            } catch (error) {
                console.error('退宿失败:', error)
            }
        },
        
        goToDevice(deviceId) {
            uni.navigateTo({
                url: `/pages/dorm/device/detail?id=${deviceId}`
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.room-detail-container {
    min-height: 100vh;
    background: #F2F7F5;
    padding-bottom: 40rpx;
}

.room-header {
    background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
    padding: 40rpx 30rpx;
    
    .room-number {
        display: block;
        font-size: 48rpx;
        font-weight: bold;
        color: #fff;
        margin-bottom: 10rpx;
    }
    
    .room-building {
        display: block;
        font-size: 28rpx;
        color: rgba(255, 255, 255, 0.8);
        margin-bottom: 20rpx;
    }
    
    .room-status {
        display: inline-block;
        font-size: 24rpx;
        color: #fff;
        padding: 8rpx 24rpx;
        border-radius: 20rpx;
        background: rgba(255, 255, 255, 0.2);
        
        &.empty {
            background: rgba(76, 175, 80, 0.3);
        }
        
        &.partial {
            background: rgba(255, 152, 0, 0.3);
        }
        
        &.full {
            background: rgba(245, 90, 90, 0.3);
        }
    }
}

.occupants-section, .device-section {
    background: #fff;
    margin: 20rpx;
    border-radius: 16rpx;
    overflow: hidden;
    
    .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 30rpx;
        border-bottom: 1rpx solid #F2F7F5;
        
        .section-title {
            font-size: 30rpx;
            font-weight: bold;
            color: #333;
        }
        
        .section-add {
            font-size: 26rpx;
            color: #2A7965;
        }
    }
}

.occupant-list {
    .occupant-item {
        display: flex;
        align-items: center;
        padding: 30rpx;
        border-bottom: 1rpx solid #F2F7F5;
        
        &:last-child {
            border-bottom: none;
        }
        
        .occupant-avatar {
            width: 80rpx;
            height: 80rpx;
            border-radius: 50%;
            background: linear-gradient(135deg, #2A7965 0%, #3D967E 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            margin-right: 20rpx;
        }
        
        .occupant-info {
            flex: 1;
            
            .occupant-name {
                display: block;
                font-size: 30rpx;
                color: #333;
                margin-bottom: 8rpx;
            }
            
            .occupant-phone {
                font-size: 24rpx;
                color: #999;
            }
        }
        
        .check-out-btn {
            font-size: 24rpx;
            color: #1F5C4D;
            background: rgba(245, 90, 90, 0.1);
            border: none;
            padding: 12rpx 24rpx;
            border-radius: 30rpx;
        }
    }
}

.device-list {
    .device-item {
        display: flex;
        align-items: center;
        padding: 30rpx;
        border-bottom: 1rpx solid #F2F7F5;
        
        &:last-child {
            border-bottom: none;
        }
        
        .device-icon {
            width: 70rpx;
            height: 70rpx;
            border-radius: 50%;
            background: #999;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-right: 20rpx;
            
            &.online {
                background: #2A7965;
            }
        }
        
        .device-info {
            flex: 1;
            
            .device-name {
                display: block;
                font-size: 30rpx;
                color: #333;
                margin-bottom: 8rpx;
            }
            
            .device-status {
                font-size: 24rpx;
                color: #999;
            }
        }
    }
}

.empty-state {
    text-align: center;
    padding: 60rpx 0;
    
    .empty-text {
        font-size: 28rpx;
        color: #999;
    }
}
</style>
