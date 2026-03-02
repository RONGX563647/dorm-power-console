<template>
  <div class="device-group-view">
    <div class="page-header">
      <h1>设备分组管理</h1>
      <a-button type="primary" @click="showCreateModal">
        <template #icon><PlusOutlined /></template>
        创建分组
      </a-button>
    </div>

    <a-row :gutter="16">
      <a-col :span="16">
        <a-card title="分组列表">
          <a-table
            :columns="groupColumns"
            :data-source="groups"
            :loading="loading"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
            :row-key="(record: DeviceGroup) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <span style="color: #1a1a1a; font-weight: 500">{{ record.name }}</span>
              </template>
              <template v-if="column.key === 'description'">
                <span style="color: #1a1a1a">{{ record.description || '--' }}</span>
              </template>
              <template v-if="column.key === 'deviceCount'">
                <a-badge :count="getDeviceCount(record.id)" :number-style="{ backgroundColor: '#00d4ff' }" />
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="viewGroup(record.id)">
                    查看
                  </a-button>
                  <a-button type="link" size="small" @click="editGroup(record)">
                    编辑
                  </a-button>
                  <a-popconfirm
                    title="确定要删除这个分组吗？"
                    @confirm="deleteGroup(record.id)"
                  >
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card title="分组统计">
          <a-statistic
            title="总分组数"
            :value="groups.length"
            :value-style="{ color: '#00d4ff' }"
          >
            <template #prefix><PartitionOutlined /></template>
          </a-statistic>
          <div style="margin-top: 24px">
            <a-statistic
              title="总设备数"
              :value="totalDeviceCount"
              :value-style="{ color: '#52c41a' }"
            >
              <template #prefix><DesktopOutlined /></template>
            </a-statistic>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 创建/编辑分组模态框 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingGroup ? '编辑分组' : '创建分组'"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-form-item label="分组名称" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入分组名称" />
        </a-form-item>
        <a-form-item label="分组描述" name="description">
          <a-textarea
            v-model:value="formData.description"
            placeholder="请输入分组描述"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分组详情模态框 -->
    <a-modal
      v-model:open="detailVisible"
      title="分组详情"
      width="800px"
      :footer="null"
    >
      <div v-if="selectedGroup">
        <a-descriptions bordered :column="2">
          <a-descriptions-item label="分组名称">{{ selectedGroup.name }}</a-descriptions-item>
          <a-descriptions-item label="分组描述">{{ selectedGroup.description || '--' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDate(selectedGroup.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatDate(selectedGroup.updatedAt) }}</a-descriptions-item>
        </a-descriptions>
        <a-divider>分组设备</a-divider>
        <a-table
          :columns="deviceColumns"
          :data-source="groupDevices"
          :loading="devicesLoading"
          :pagination="false"
          size="small"
          :row-key="(record: Device) => record.id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <router-link :to="`/devices/${record.id}`" style="color: #00d4ff">
                {{ record.name }}
              </router-link>
            </template>
            <template v-if="column.key === 'room'">
              <span style="color: #1a1a1a">{{ record.room }}</span>
            </template>
            <template v-if="column.key === 'online'">
              <a-badge :status="record.online ? 'success' : 'error'" :text="record.online ? '在线' : '离线'" />
            </template>
            <template v-if="column.key === 'action'">
              <a-popconfirm
                title="确定要从分组中移除这个设备吗？"
                @confirm="removeDeviceFromGroup(record.id)"
              >
                <a-button type="link" size="small" danger>移除</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
        <div style="margin-top: 16px; text-align: right">
          <a-button @click="showAddDeviceModal">
            <template #icon><PlusOutlined /></template>
            添加设备
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- 添加设备模态框 -->
    <a-modal
      v-model:open="addDeviceVisible"
      title="添加设备到分组"
      @ok="handleAddDeviceOk"
      @cancel="addDeviceVisible = false"
    >
      <a-select
        v-model:value="selectedDeviceIds"
        mode="multiple"
        placeholder="请选择设备"
        style="width: 100%"
        :options="availableDevices.map(d => ({ label: d.name, value: d.id }))"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, PartitionOutlined, DesktopOutlined } from '@ant-design/icons-vue'
import { deviceGroupApi, deviceApi } from '@/api'
import type { DeviceGroup, Device } from '@/types'

const loading = ref(false)
const devicesLoading = ref(false)
const groups = ref<DeviceGroup[]>([])
const allDevices = ref<Device[]>([])
const groupDevices = ref<Device[]>([])
const selectedGroup = ref<DeviceGroup | null>(null)
const modalVisible = ref(false)
const detailVisible = ref(false)
const addDeviceVisible = ref(false)
const editingGroup = ref<DeviceGroup | null>(null)
const selectedDeviceIds = ref<string[]>([])

const formData = ref({
  name: '',
  description: ''
})

const formRules = {
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }]
}

const groupColumns = [
  { title: '分组名称', dataIndex: 'name', key: 'name' },
  { title: '分组描述', dataIndex: 'description', key: 'description' },
  { title: '设备数量', key: 'deviceCount' },
  { title: '操作', key: 'action', width: 200 }
]

const deviceColumns = [
  { title: '设备名称', dataIndex: 'name', key: 'name' },
  { title: '房间', dataIndex: 'room', key: 'room' },
  { title: '在线状态', key: 'online' },
  { title: '操作', key: 'action', width: 100 }
]

const totalDeviceCount = computed(() => {
  return groups.value.reduce((sum, group) => sum + getDeviceCount(group.id), 0)
})

const availableDevices = computed(() => {
  if (!selectedGroup.value) return []
  const groupDeviceIds = groupDevices.value.map(d => d.id)
  return allDevices.value.filter(d => !groupDeviceIds.includes(d.id))
})

const deviceCountMap = ref<Record<string, number>>({})

const getDeviceCount = (groupId: string): number => {
  return deviceCountMap.value[groupId] || 0
}

const loadGroups = async () => {
  try {
    loading.value = true
    groups.value = await deviceGroupApi.getGroups()
  } catch (error: any) {
    message.error('加载分组列表失败')
  } finally {
    loading.value = false
  }
}

const loadDevices = async () => {
  try {
    allDevices.value = await deviceApi.getDevices()
  } catch (error: any) {
    message.error('加载设备列表失败')
  }
}

const loadGroupDevices = async (groupId: string) => {
  try {
    devicesLoading.value = true
    groupDevices.value = await deviceGroupApi.getGroupDevices(groupId)
  } catch (error: any) {
    message.error('加载分组设备失败')
  } finally {
    devicesLoading.value = false
  }
}

const showCreateModal = () => {
  editingGroup.value = null
  formData.value = { name: '', description: '' }
  modalVisible.value = true
}

const editGroup = (group: DeviceGroup) => {
  editingGroup.value = group
  formData.value = { name: group.name, description: group.description || '' }
  modalVisible.value = true
}

const handleModalOk = async () => {
  try {
    if (editingGroup.value) {
      await deviceGroupApi.updateGroup(editingGroup.value.id, formData.value)
      message.success('分组更新成功')
    } else {
      await deviceGroupApi.createGroup(formData.value)
      message.success('分组创建成功')
    }
    modalVisible.value = false
    await loadGroups()
  } catch (error: any) {
    message.error(editingGroup.value ? '分组更新失败' : '分组创建失败')
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
}

const viewGroup = async (groupId: string) => {
  try {
    selectedGroup.value = await deviceGroupApi.getGroup(groupId)
    await loadGroupDevices(groupId)
    detailVisible.value = true
  } catch (error: any) {
    message.error('加载分组详情失败')
  }
}

const deleteGroup = async (groupId: string) => {
  try {
    await deviceGroupApi.deleteGroup(groupId)
    message.success('分组删除成功')
    await loadGroups()
  } catch (error: any) {
    message.error('分组删除失败')
  }
}

const removeDeviceFromGroup = async (deviceId: string) => {
  if (!selectedGroup.value) return
  try {
    await deviceGroupApi.removeDeviceFromGroup(selectedGroup.value.id, deviceId)
    message.success('设备移除成功')
    await loadGroupDevices(selectedGroup.value.id)
  } catch (error: any) {
    message.error('设备移除失败')
  }
}

const showAddDeviceModal = () => {
  selectedDeviceIds.value = []
  addDeviceVisible.value = true
}

const handleAddDeviceOk = async () => {
  if (!selectedGroup.value) return
  try {
    const promises = selectedDeviceIds.value.map(deviceId => 
      deviceGroupApi.addDeviceToGroup(selectedGroup.value!.id, deviceId)
    )
    await Promise.all(promises)
    message.success('设备添加成功')
    addDeviceVisible.value = false
    await loadGroupDevices(selectedGroup.value.id)
  } catch (error: any) {
    message.error('设备添加失败')
  }
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '--'
  return new Date(dateString).toLocaleString()
}

onMounted(() => {
  loadGroups()
  loadDevices()
})
</script>

<style scoped>
.device-group-view {
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
}

:deep(.ant-card) {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
}

:deep(.ant-card-head) {
  color: #1a1a1a;
  border-bottom-color: rgba(0, 212, 255, 0.15);
}

:deep(.ant-table) {
  color: #1a1a1a;
}

:deep(.ant-table-thead > tr > th) {
  color: #1a1a1a;
  background: rgba(0, 212, 255, 0.05);
}

:deep(.ant-table-tbody > tr) {
  border-bottom-color: rgba(0, 212, 255, 0.1);
}

:deep(.ant-table-tbody > tr:hover) {
  background: rgba(0, 212, 255, 0.05);
}

:deep(.ant-statistic-title) {
  color: #1a1a1a;
}

:deep(.ant-statistic-content) {
  color: #1a1a1a;
}

:deep(.ant-modal-content) {
  background: rgba(16, 24, 40, 0.95);
}

:deep(.ant-modal-header) {
  background: rgba(16, 24, 40, 0.95);
  border-bottom-color: rgba(0, 212, 255, 0.15);
}

:deep(.ant-modal-title) {
  color: #1a1a1a;
}

:deep(.ant-descriptions-item-label) {
  color: #1a1a1a;
}

:deep(.ant-descriptions-item-content) {
  color: #1a1a1a;
}
</style>