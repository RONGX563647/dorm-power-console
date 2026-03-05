<template>
  <div class="command-history-view">
    <a-page-header
      title="命令历史管理"
      sub-title="查看设备命令执行历史"
      style="padding: 0 0 16px 0"
    >
      <template #extra>
        <a-space>
          <a-select
            v-model:value="selectedDevice"
            placeholder="选择设备"
            style="width: 200px"
            @change="handleDeviceChange"
          >
            <a-select-option v-for="device in devices" :key="device.id" :value="device.id">
              {{ device.name }}
            </a-select-option>
          </a-select>
          <a-button type="primary" @click="getDeviceCommands" :loading="loading">
            查询历史
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-card title="命令历史">
      <a-table
        v-if="commands && commands.length > 0"
        :data-source="commands"
        :columns="commandColumns"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="getCommandDetails(record.id)">
              详情
            </a-button>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'success' ? 'green' : 'red'">
              {{ record.status === 'success' ? '成功' : '失败' }}
            </a-tag>
          </template>
        </template>
      </a-table>
      <a-empty v-else description="暂无命令历史" :image="aEmptyImage" />
    </a-card>

    <a-modal
      v-model:open="detailVisible"
      title="命令详情"
      width="600px"
    >
      <a-descriptions :column="1" size="small" v-if="commandDetail">
        <a-descriptions-item label="命令ID">
          {{ commandDetail.id }}
        </a-descriptions-item>
        <a-descriptions-item label="设备ID">
          {{ commandDetail.deviceId }}
        </a-descriptions-item>
        <a-descriptions-item label="命令类型">
          {{ commandDetail.commandType }}
        </a-descriptions-item>
        <a-descriptions-item label="命令参数">
          <pre>{{ JSON.stringify(commandDetail.commandJson, null, 2) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="执行时间">
          {{ commandDetail.createTime }}
        </a-descriptions-item>
        <a-descriptions-item label="执行状态">
          <a-tag :color="commandDetail.status === 'success' ? 'green' : 'red'">
            {{ commandDetail.status === 'success' ? '成功' : '失败' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item v-if="commandDetail.responseJson" label="执行结果">
          <pre>{{ JSON.stringify(commandDetail.responseJson, null, 2) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item v-if="commandDetail.errorMsg" label="错误信息">
          {{ commandDetail.errorMsg }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { commandHistoryApi, deviceApi } from '@/api'
import { Empty } from 'ant-design-vue'

const aEmptyImage = Empty.PRESENTED_IMAGE_SIMPLE

const selectedDevice = ref<string>()
const devices = ref<any[]>([])
const commands = ref<any[]>([])
const commandDetail = ref<any>(null)
const detailVisible = ref(false)
const loading = ref(false)

const commandColumns = [
  { title: '命令ID', dataIndex: 'id', key: 'id' },
  { title: '命令类型', dataIndex: 'commandType', key: 'commandType' },
  { title: '执行时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '执行状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', width: 100 }
]

onMounted(async () => {
  await loadDevices()
})

const loadDevices = async () => {
  try {
    const data = await deviceApi.getDevices()
    devices.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载设备失败:', error)
  }
}

const handleDeviceChange = () => {
  commands.value = []
}

const getDeviceCommands = async () => {
  if (!selectedDevice.value) {
    message.warning('请先选择设备')
    return
  }

  loading.value = true
  try {
    const result = await commandHistoryApi.getDeviceCommands(selectedDevice.value)
    commands.value = Array.isArray(result) ? result : []
  } catch (error) {
    console.error('获取命令历史失败:', error)
    message.error('获取失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const getCommandDetails = async (cmdId: string) => {
  try {
    const result = await commandHistoryApi.getCommandDetails(cmdId)
    commandDetail.value = result
    detailVisible.value = true
  } catch (error) {
    console.error('获取命令详情失败:', error)
    message.error('获取失败，请稍后再试')
  }
}
</script>

<style scoped>
.command-history-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}

:deep(.ant-descriptions pre) {
  margin: 0;
  padding: 8px;
  background-color: #f5f5f5;
  border-radius: 4px;
  overflow-x: auto;
}
</style>