<template>
  <div class="firmware-view">
    <a-page-header
      title="固件管理"
      sub-title="设备固件升级管理"
      style="padding: 0 0 16px 0"
    >
      <template #extra>
        <a-space>
          <a-button type="primary" @click="showUpgradeModal()">
            创建升级任务
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="pending" tab="待升级">
        <a-card>
          <a-table
            :data-source="pendingTasks"
            :columns="taskColumns"
            :loading="loadingPending"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag color="orange">待升级</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="sendFirmware(record.id)" :loading="record.sending">
                    发送
                  </a-button>
                  <a-button type="link" size="small" danger @click="cancelUpgrade(record.id)">
                    取消
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="active" tab="进行中">
        <a-card>
          <a-table
            :data-source="activeTasks"
            :columns="activeColumns"
            :loading="loadingActive"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'progress'">
                <a-progress :percent="record.progress" :status="record.progress === 100 ? 'success' : 'active'" />
              </template>
              <template v-if="column.key === 'status'">
                <a-tag color="blue">进行中</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="completeUpgrade(record.id, true)">
                    完成
                  </a-button>
                  <a-button type="link" size="small" danger @click="cancelUpgrade(record.id)">
                    取消
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="history" tab="历史记录">
        <a-card>
          <a-space style="margin-bottom: 16px">
            <a-select
              v-model:value="selectedDevice"
              placeholder="选择设备"
              style="width: 200px"
              allow-clear
              @change="loadDeviceHistory"
            >
              <a-select-option v-for="device in devices" :key="device.id" :value="device.id">
                {{ device.name }}
              </a-select-option>
            </a-select>
          </a-space>

          <a-table
            :data-source="historyTasks"
            :columns="historyColumns"
            :loading="loadingHistory"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.success ? 'green' : 'red'">
                  {{ record.success ? '成功' : '失败' }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="upgradeModalVisible"
      title="创建升级任务"
      @ok="handleUpgradeSubmit"
      :confirm-loading="submitting"
    >
      <a-form :model="upgradeForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="设备" required>
          <a-select v-model:value="upgradeForm.deviceId" placeholder="请选择设备">
            <a-select-option v-for="device in devices" :key="device.id" :value="device.id">
              {{ device.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="固件版本" required>
          <a-input v-model:value="upgradeForm.version" placeholder="请输入固件版本" />
        </a-form-item>
        <a-form-item label="固件URL" required>
          <a-input v-model:value="upgradeForm.firmwareUrl" placeholder="请输入固件下载地址" />
        </a-form-item>
        <a-form-item label="升级说明">
          <a-textarea v-model:value="upgradeForm.description" placeholder="请输入升级说明" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { firmwareApi, deviceApi } from '@/api'

const activeTab = ref('pending')
const loadingPending = ref(false)
const loadingActive = ref(false)
const loadingHistory = ref(false)
const submitting = ref(false)

const pendingTasks = ref<any[]>([])
const activeTasks = ref<any[]>([])
const historyTasks = ref<any[]>([])
const devices = ref<any[]>([])
const selectedDevice = ref<string>()

const upgradeModalVisible = ref(false)

const upgradeForm = ref({
  deviceId: '',
  version: '',
  firmwareUrl: '',
  description: ''
})

const taskColumns = [
  { title: '设备名称', dataIndex: 'deviceName', key: 'deviceName' },
  { title: '固件版本', dataIndex: 'version', key: 'version' },
  { title: '状态', key: 'status' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 150 }
]

const activeColumns = [
  { title: '设备名称', dataIndex: 'deviceName', key: 'deviceName' },
  { title: '固件版本', dataIndex: 'version', key: 'version' },
  { title: '进度', key: 'progress' },
  { title: '状态', key: 'status' },
  { title: '开始时间', dataIndex: 'startedAt', key: 'startedAt' },
  { title: '操作', key: 'action', width: 150 }
]

const historyColumns = [
  { title: '设备名称', dataIndex: 'deviceName', key: 'deviceName' },
  { title: '固件版本', dataIndex: 'version', key: 'version' },
  { title: '状态', key: 'status' },
  { title: '完成时间', dataIndex: 'completedAt', key: 'completedAt' },
  { title: '耗时(秒)', dataIndex: 'duration', key: 'duration' }
]

onMounted(async () => {
  await loadDevices()
  await Promise.all([
    loadPendingTasks(),
    loadActiveTasks()
  ])
})

const loadDevices = async () => {
  try {
    const data = await deviceApi.getDevices()
    devices.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载设备失败:', error)
  }
}

const loadPendingTasks = async () => {
  loadingPending.value = true
  try {
    const data = await firmwareApi.getPending()
    pendingTasks.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载待升级任务失败')
    console.error(error)
  } finally {
    loadingPending.value = false
  }
}

const loadActiveTasks = async () => {
  loadingActive.value = true
  try {
    const data = await firmwareApi.getActive()
    activeTasks.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载进行中任务失败')
    console.error(error)
  } finally {
    loadingActive.value = false
  }
}

const loadDeviceHistory = async () => {
  if (!selectedDevice.value) {
    historyTasks.value = []
    return
  }

  loadingHistory.value = true
  try {
    const data = await firmwareApi.getDeviceHistory(selectedDevice.value)
    historyTasks.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载历史记录失败')
    console.error(error)
  } finally {
    loadingHistory.value = false
  }
}

const showUpgradeModal = () => {
  upgradeForm.value = {
    deviceId: '',
    version: '',
    firmwareUrl: '',
    description: ''
  }
  upgradeModalVisible.value = true
}

const handleUpgradeSubmit = async () => {
  if (!upgradeForm.value.deviceId || !upgradeForm.value.version || !upgradeForm.value.firmwareUrl) {
    message.warning('请填写必填项')
    return
  }

  submitting.value = true
  try {
    await firmwareApi.createUpgrade(upgradeForm.value)
    message.success('升级任务创建成功')
    upgradeModalVisible.value = false
    await loadPendingTasks()
  } catch (error) {
    message.error('创建失败')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const sendFirmware = async (taskId: string) => {
  const task = pendingTasks.value.find(t => t.id === taskId)
  if (task) {
    task.sending = true
  }

  try {
    await firmwareApi.sendFirmware(taskId)
    message.success('固件发送成功')
    await Promise.all([loadPendingTasks(), loadActiveTasks()])
  } catch (error) {
    message.error('发送失败')
    console.error(error)
  } finally {
    if (task) {
      task.sending = false
    }
  }
}

const completeUpgrade = async (taskId: string, success: boolean) => {
  try {
    await firmwareApi.completeUpgrade(taskId, success)
    message.success('升级已完成')
    await Promise.all([loadActiveTasks(), loadDeviceHistory()])
  } catch (error) {
    message.error('操作失败')
    console.error(error)
  }
}

const cancelUpgrade = async (taskId: string) => {
  try {
    await firmwareApi.cancelUpgrade(taskId)
    message.success('升级已取消')
    await Promise.all([loadPendingTasks(), loadActiveTasks()])
  } catch (error) {
    message.error('取消失败')
    console.error(error)
  }
}
</script>

<style scoped>
.firmware-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>
