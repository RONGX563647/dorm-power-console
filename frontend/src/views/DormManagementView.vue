<template>
  <div class="dorm-management-view">
    <div class="page-header">
      <h1>宿舍管理</h1>
    </div>

    <a-row :gutter="16" style="margin-bottom: 24px">
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="总楼栋" :value="statistics?.totalBuildings || 0" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="总房间" :value="statistics?.totalRooms || 0" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="已入住" :value="statistics?.occupiedRooms || 0" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="空置" :value="statistics?.vacantRooms || 0" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="维修中" :value="statistics?.maintenanceRooms || 0" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="入住率" :value="statistics?.occupancyRate || 0" suffix="%" />
        </a-card>
      </a-col>
    </a-row>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="buildings" tab="楼栋管理">
        <a-card>
          <div class="filter-bar">
            <a-button type="primary" @click="showBuildingModal()">
              <template #icon><PlusOutlined /></template>
              新增楼栋
            </a-button>
          </div>

          <a-table
            :columns="buildingColumns"
            :data-source="buildings"
            :loading="buildingsLoading"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
            :row-key="(record: Building) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="viewBuildingRooms(record)">查看房间</a-button>
                  <a-button type="link" size="small" @click="showBuildingModal(record)">编辑</a-button>
                  <a-popconfirm title="确定删除此楼栋？" @confirm="deleteBuilding(record.id)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="rooms" tab="房间管理">
        <a-card>
          <div class="filter-bar">
            <a-space>
              <a-select
                v-model:value="roomFilter.buildingId"
                style="width: 200px"
                placeholder="选择楼栋"
                allowClear
                @change="handleBuildingFilter"
              >
                <a-select-option v-for="b in buildings" :key="b.id" :value="b.id">
                  {{ b.name }}
                </a-select-option>
              </a-select>
              <a-select
                v-model:value="roomFilter.status"
                style="width: 150px"
                placeholder="房间状态"
                allowClear
                @change="handleRoomFilter"
              >
                <a-select-option value="OCCUPIED">已入住</a-select-option>
                <a-select-option value="VACANT">空置</a-select-option>
                <a-select-option value="MAINTENANCE">维修中</a-select-option>
              </a-select>
              <a-button type="primary" @click="showRoomModal()">
                <template #icon><PlusOutlined /></template>
                新增房间
              </a-button>
            </a-space>
          </div>

          <a-table
            :columns="roomColumns"
            :data-source="filteredRooms"
            :loading="roomsLoading"
            :pagination="{ pageSize: 10 }"
            :row-key="(record: DormRoom) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="getRoomStatusColor(record.status)">
                  {{ getRoomStatusName(record.status) }}
                </a-tag>
              </template>
              <template v-if="column.key === 'occupancy'">
                {{ record.currentOccupants }} / {{ record.capacity }}
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="showRoomModal(record)">编辑</a-button>
                  <a-button
                    type="link"
                    size="small"
                    v-if="record.status === 'VACANT'"
                    @click="showCheckInModal(record)"
                  >
                    入住
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    v-if="record.status === 'OCCUPIED'"
                    @click="handleCheckOut(record.id)"
                  >
                    退宿
                  </a-button>
                  <a-popconfirm title="确定删除此房间？" @confirm="deleteRoom(record.id)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="buildingModalVisible"
      :title="editingBuilding ? '编辑楼栋' : '新增楼栋'"
      @ok="handleBuildingOk"
    >
      <a-form ref="buildingFormRef" :model="buildingForm" :rules="buildingRules" layout="vertical">
        <a-form-item label="楼栋名称" name="name">
          <a-input v-model:value="buildingForm.name" placeholder="请输入楼栋名称" />
        </a-form-item>
        <a-form-item label="楼栋编号" name="code">
          <a-input v-model:value="buildingForm.code" placeholder="请输入楼栋编号" />
        </a-form-item>
        <a-form-item label="楼层数" name="totalFloors">
          <a-input-number v-model:value="buildingForm.totalFloors" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="地址" name="address">
          <a-input v-model:value="buildingForm.address" placeholder="请输入地址" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="buildingForm.description" :rows="3" placeholder="请输入描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="roomModalVisible"
      :title="editingRoom ? '编辑房间' : '新增房间'"
      @ok="handleRoomOk"
    >
      <a-form ref="roomFormRef" :model="roomForm" :rules="roomRules" layout="vertical">
        <a-form-item label="所属楼栋" name="buildingId">
          <a-select v-model:value="roomForm.buildingId" placeholder="请选择楼栋">
            <a-select-option v-for="b in buildings" :key="b.id" :value="b.id">
              {{ b.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="房间号" name="roomNumber">
          <a-input v-model:value="roomForm.roomNumber" placeholder="请输入房间号" />
        </a-form-item>
        <a-form-item label="楼层" name="floor">
          <a-input-number v-model:value="roomForm.floor" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="容纳人数" name="capacity">
          <a-input-number v-model:value="roomForm.capacity" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="roomForm.status">
            <a-select-option value="VACANT">空置</a-select-option>
            <a-select-option value="OCCUPIED">已入住</a-select-option>
            <a-select-option value="MAINTENANCE">维修中</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="checkInModalVisible"
      title="办理入住"
      @ok="handleCheckIn"
    >
      <a-form :model="checkInForm" layout="vertical">
        <a-form-item label="入住人数" required>
          <a-input-number v-model:value="checkInForm.currentOccupants" :min="1" :max="checkInForm.maxCapacity" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { dormApi } from '@/api'
import type { Building, DormRoom, RoomStatistics } from '@/types'

const activeTab = ref('buildings')
const buildingsLoading = ref(false)
const roomsLoading = ref(false)

const buildings = ref<Building[]>([])
const rooms = ref<DormRoom[]>([])
const statistics = ref<RoomStatistics | null>(null)

const roomFilter = ref<{ buildingId?: string; status?: string }>({})

const buildingColumns = [
  { title: '楼栋ID', dataIndex: 'id', key: 'id' },
  { title: '楼栋名称', dataIndex: 'name', key: 'name' },
  { title: '楼栋编号', dataIndex: 'code', key: 'code' },
  { title: '楼层数', dataIndex: 'totalFloors', key: 'totalFloors' },
  { title: '地址', dataIndex: 'address', key: 'address' },
  { title: '操作', key: 'action', width: 200 }
]

const roomColumns = [
  { title: '房间ID', dataIndex: 'id', key: 'id' },
  { title: '房间号', dataIndex: 'roomNumber', key: 'roomNumber' },
  { title: '楼栋', dataIndex: 'buildingName', key: 'buildingName' },
  { title: '楼层', dataIndex: 'floor', key: 'floor' },
  { title: '入住情况', key: 'occupancy' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', width: 250 }
]

const filteredRooms = computed(() => {
  let result = rooms.value
  if (roomFilter.value.buildingId) {
    result = result.filter(r => r.buildingId === roomFilter.value.buildingId)
  }
  if (roomFilter.value.status) {
    result = result.filter(r => r.status === roomFilter.value.status)
  }
  return result
})

const buildingModalVisible = ref(false)
const editingBuilding = ref<Building | null>(null)
const buildingFormRef = ref()
const buildingForm = ref<Partial<Building>>({
  name: '',
  code: '',
  totalFloors: 6,
  address: '',
  description: ''
})
const buildingRules = {
  name: [{ required: true, message: '请输入楼栋名称' }],
  code: [{ required: true, message: '请输入楼栋编号' }],
  totalFloors: [{ required: true, message: '请输入楼层数' }]
}

const roomModalVisible = ref(false)
const editingRoom = ref<DormRoom | null>(null)
const roomFormRef = ref()
const roomForm = ref<Partial<DormRoom>>({
  buildingId: '',
  roomNumber: '',
  floor: 1,
  capacity: 4,
  status: 'VACANT'
})
const roomRules = {
  buildingId: [{ required: true, message: '请选择楼栋' }],
  roomNumber: [{ required: true, message: '请输入房间号' }],
  floor: [{ required: true, message: '请输入楼层' }],
  capacity: [{ required: true, message: '请输入容纳人数' }]
}

const checkInModalVisible = ref(false)
const checkInForm = ref({ roomId: '', currentOccupants: 1, maxCapacity: 4 })

const loadBuildings = async () => {
  buildingsLoading.value = true
  try {
    buildings.value = await dormApi.getBuildings()
  } catch (error: any) {
    message.error('加载楼栋失败')
  } finally {
    buildingsLoading.value = false
  }
}

const loadRooms = async () => {
  roomsLoading.value = true
  try {
    rooms.value = await dormApi.getAllRooms()
  } catch (error: any) {
    message.error('加载房间失败')
  } finally {
    roomsLoading.value = false
  }
}

const loadStatistics = async () => {
  try {
    statistics.value = await dormApi.getRoomStatistics()
  } catch (error: any) {
    console.error('加载统计失败', error)
  }
}

const handleBuildingFilter = async (buildingId: string) => {
  if (buildingId) {
    roomsLoading.value = true
    try {
      rooms.value = await dormApi.getRoomsByBuilding(buildingId)
    } catch (error: any) {
      message.error('加载房间失败')
    } finally {
      roomsLoading.value = false
    }
  } else {
    loadRooms()
  }
}

const handleRoomFilter = () => {
}

const showBuildingModal = (building?: Building) => {
  editingBuilding.value = building || null
  buildingForm.value = building ? { ...building } : { name: '', code: '', totalFloors: 6, address: '', description: '' }
  buildingModalVisible.value = true
}

const handleBuildingOk = async () => {
  try {
    await buildingFormRef.value?.validate()
    if (editingBuilding.value) {
      await dormApi.updateBuilding(editingBuilding.value.id, buildingForm.value)
      message.success('更新成功')
    } else {
      await dormApi.createBuilding(buildingForm.value)
      message.success('创建成功')
    }
    buildingModalVisible.value = false
    loadBuildings()
    loadStatistics()
  } catch (error: any) {
    message.error('操作失败')
  }
}

const deleteBuilding = async (id: string) => {
  try {
    await dormApi.deleteBuilding(id)
    message.success('删除成功')
    loadBuildings()
    loadStatistics()
  } catch (error: any) {
    message.error('删除失败')
  }
}

const viewBuildingRooms = (building: Building) => {
  roomFilter.value.buildingId = building.id
  activeTab.value = 'rooms'
  handleBuildingFilter(building.id)
}

const showRoomModal = (room?: DormRoom) => {
  editingRoom.value = room || null
  roomForm.value = room ? { ...room } : { buildingId: '', roomNumber: '', floor: 1, capacity: 4, status: 'VACANT' }
  roomModalVisible.value = true
}

const handleRoomOk = async () => {
  try {
    await roomFormRef.value?.validate()
    if (editingRoom.value) {
      await dormApi.updateRoom(editingRoom.value.id, roomForm.value)
      message.success('更新成功')
    } else {
      await dormApi.createRoom(roomForm.value)
      message.success('创建成功')
    }
    roomModalVisible.value = false
    loadRooms()
    loadStatistics()
  } catch (error: any) {
    message.error('操作失败')
  }
}

const deleteRoom = async (id: string) => {
  try {
    await dormApi.deleteRoom(id)
    message.success('删除成功')
    loadRooms()
    loadStatistics()
  } catch (error: any) {
    message.error('删除失败')
  }
}

const showCheckInModal = (room: DormRoom) => {
  checkInForm.value = { roomId: room.id, currentOccupants: 1, maxCapacity: room.capacity }
  checkInModalVisible.value = true
}

const handleCheckIn = async () => {
  try {
    await dormApi.checkIn(checkInForm.value.roomId, checkInForm.value.currentOccupants)
    message.success('入住成功')
    checkInModalVisible.value = false
    loadRooms()
    loadStatistics()
  } catch (error: any) {
    message.error('入住失败')
  }
}

const handleCheckOut = async (roomId: string) => {
  try {
    await dormApi.checkOut(roomId)
    message.success('退宿成功')
    loadRooms()
    loadStatistics()
  } catch (error: any) {
    message.error('退宿失败')
  }
}

const getRoomStatusName = (status: string) => {
  const map: Record<string, string> = { OCCUPIED: '已入住', VACANT: '空置', MAINTENANCE: '维修中' }
  return map[status] || status
}

const getRoomStatusColor = (status: string) => {
  const map: Record<string, string> = { OCCUPIED: 'green', VACANT: 'blue', MAINTENANCE: 'orange' }
  return map[status] || 'default'
}

onMounted(() => {
  loadBuildings()
  loadRooms()
  loadStatistics()
})
</script>

<style scoped>
.dorm-management-view {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  color: #1a1a1a;
  margin: 0;
  font-size: 24px;
}

.filter-bar {
  margin-bottom: 16px;
}
</style>
