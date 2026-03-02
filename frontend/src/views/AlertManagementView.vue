<template>
  <div class="alert-management-view">
    <div class="page-header">
      <h1>告警管理</h1>
      <a-space>
        <a-button @click="loadAlerts">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button type="primary" @click="showConfigModal">
          <template #icon><SettingOutlined /></template>
          告警配置
        </a-button>
      </a-space>
    </div>

    <a-row :gutter="16">
      <a-col :span="16">
        <a-card title="告警列表">
          <div class="filter-bar">
            <a-space>
              <a-select
                v-model:value="statusFilter"
                style="width: 150px"
                @change="handleFilterChange"
              >
                <a-select-option value="all">全部状态</a-select-option>
                <a-select-option value="unresolved">未解决</a-select-option>
                <a-select-option value="resolved">已解决</a-select-option>
              </a-select>
              <a-select
                v-model:value="severityFilter"
                style="width: 150px"
                @change="handleFilterChange"
              >
                <a-select-option value="all">全部级别</a-select-option>
                <a-select-option value="CRITICAL">严重</a-select-option>
                <a-select-option value="ERROR">错误</a-select-option>
                <a-select-option value="WARNING">警告</a-select-option>
                <a-select-option value="INFO">信息</a-select-option>
              </a-select>
            </a-space>
          </div>

          <a-table
            :columns="alertColumns"
            :data-source="filteredAlerts"
            :loading="loading"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
            :row-key="(record: Alert) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'type'">
                <a-tag :color="getTypeColor(record.type)">{{ getTypeText(record.type) }}</a-tag>
              </template>
              <template v-if="column.key === 'severity'">
                <a-tag :color="getSeverityColor(record.severity)">{{ getSeverityText(record.severity) }}</a-tag>
              </template>
              <template v-if="column.key === 'message'">
                <span style="color: #1a1a1a">{{ record.message }}</span>
              </template>
              <template v-if="column.key === 'createdAt'">
                <span style="color: #1a1a1a">{{ formatDate(record.createdAt) }}</span>
              </template>
              <template v-if="column.key === 'resolved'">
                <a-badge :status="record.resolved ? 'success' : 'error'" :text="record.resolved ? '已解决' : '未解决'" />
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button v-if="!record.resolved" type="link" size="small" @click="resolveAlert(record.id)">
                    解决
                  </a-button>
                  <a-button type="link" size="small" @click="viewAlert(record)">
                    详情
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card title="告警统计">
          <a-statistic
            title="未解决告警"
            :value="unresolvedCount"
            :value-style="{ color: '#ff4d4f' }"
          >
            <template #prefix><AlertOutlined /></template>
          </a-statistic>
          <div style="margin-top: 24px">
            <a-statistic
              title="总告警数"
              :value="alerts.length"
              :value-style="{ color: '#00d4ff' }"
            >
              <template #prefix><BellOutlined /></template>
            </a-statistic>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 告警配置模态框 -->
    <a-modal
      v-model:open="configVisible"
      title="告警配置"
      width="600px"
      @ok="handleConfigOk"
      @cancel="configVisible = false"
    >
      <a-form
        ref="configFormRef"
        :model="configData"
        layout="vertical"
      >
        <a-form-item label="设备">
          <a-select
            v-model:value="configData.deviceId"
            placeholder="请选择设备"
            style="width: 100%"
            :options="devices.map(d => ({ label: d.name, value: d.id }))"
          />
        </a-form-item>
        <a-form-item label="高功率阈值 (W)">
          <a-input-number
            v-model:value="configData.highPowerThreshold"
            :min="0"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="离线超时 (分钟)">
          <a-input-number
            v-model:value="configData.offlineTimeout"
            :min="0"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="过载阈值 (W)">
          <a-input-number
            v-model:value="configData.overloadThreshold"
            :min="0"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="启用告警">
          <a-switch v-model:checked="configData.enabled" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 告警详情模态框 -->
    <a-modal
      v-model:open="detailVisible"
      title="告警详情"
      width="600px"
      :footer="null"
    >
      <div v-if="selectedAlert">
        <a-descriptions bordered :column="2">
          <a-descriptions-item label="告警类型">
            <a-tag :color="getTypeColor(selectedAlert.type)">{{ getTypeText(selectedAlert.type) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="告警级别">
            <a-tag :color="getSeverityColor(selectedAlert.severity)">{{ getSeverityText(selectedAlert.severity) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="告警消息" :span="2">
            {{ selectedAlert.message }}
          </a-descriptions-item>
          <a-descriptions-item label="当前值">
            {{ selectedAlert.value || '--' }}
          </a-descriptions-item>
          <a-descriptions-item label="阈值">
            {{ selectedAlert.threshold || '--' }}
          </a-descriptions-item>
          <a-descriptions-item label="设备ID" :span="2">
            {{ selectedAlert.deviceId }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatDate(selectedAlert.createdAt) }}
          </a-descriptions-item>
          <a-descriptions-item label="解决时间">
            {{ selectedAlert.resolvedAt ? formatDate(selectedAlert.resolvedAt) : '未解决' }}
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SettingOutlined, AlertOutlined, BellOutlined } from '@ant-design/icons-vue'
import { alertApi, deviceApi } from '@/api'
import type { Alert, Device, AlertConfig } from '@/types'

const loading = ref(false)
const alerts = ref<Alert[]>([])
const devices = ref<Device[]>([])
const statusFilter = ref('all')
const severityFilter = ref('all')
const configVisible = ref(false)
const detailVisible = ref(false)
const selectedAlert = ref<Alert | null>(null)
const configData = ref<AlertConfig>({
  deviceId: '',
  highPowerThreshold: 1000,
  offlineTimeout: 30,
  overloadThreshold: 2000,
  enabled: true
})

const alertColumns = [
  { title: '告警类型', key: 'type', width: 120 },
  { title: '告警级别', key: 'severity', width: 100 },
  { title: '告警消息', key: 'message' },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '状态', key: 'resolved', width: 100 },
  { title: '操作', key: 'action', width: 150 }
]

const unresolvedCount = computed(() => {
  return alerts.value.filter(a => !a.resolved).length
})

const filteredAlerts = computed(() => {
  return alerts.value
    .filter(alert => {
      if (statusFilter.value === 'all') return true
      if (statusFilter.value === 'unresolved') return !alert.resolved
      return alert.resolved
    })
    .filter(alert => {
      if (severityFilter.value === 'all') return true
      return alert.severity === severityFilter.value
    })
})

const loadAlerts = async () => {
  try {
    loading.value = true
    alerts.value = await alertApi.getUnresolvedAlerts()
  } catch (error: any) {
    message.error('加载告警列表失败')
  } finally {
    loading.value = false
  }
}

const loadDevices = async () => {
  try {
    devices.value = await deviceApi.getDevices()
  } catch (error: any) {
    message.error('加载设备列表失败')
  }
}

const resolveAlert = async (alertId: string) => {
  try {
    await alertApi.resolveAlert(alertId)
    message.success('告警已解决')
    await loadAlerts()
  } catch (error: any) {
    message.error('解决告警失败')
  }
}

const viewAlert = (alert: Alert) => {
  selectedAlert.value = alert
  detailVisible.value = true
}

const showConfigModal = () => {
  if (devices.value.length > 0) {
    configData.value.deviceId = devices.value[0].id
  }
  configVisible.value = true
}

const handleConfigOk = async () => {
  try {
    await alertApi.updateAlertConfig(configData.value.deviceId, configData.value)
    message.success('告警配置更新成功')
    configVisible.value = false
  } catch (error: any) {
    message.error('告警配置更新失败')
  }
}

const handleFilterChange = () => {
  // 过滤已在 computed 中处理
}

const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    'HIGH_POWER': 'orange',
    'OFFLINE': 'red',
    'OVERLOAD': 'purple',
    'ABNORMAL': 'blue'
  }
  return colors[type] || 'default'
}

const getTypeText = (type: string) => {
  const texts: Record<string, string> = {
    'HIGH_POWER': '高功率',
    'OFFLINE': '离线',
    'OVERLOAD': '过载',
    'ABNORMAL': '异常'
  }
  return texts[type] || type
}

const getSeverityColor = (severity: string) => {
  const colors: Record<string, string> = {
    'CRITICAL': 'red',
    'ERROR': 'orange',
    'WARNING': 'yellow',
    'INFO': 'blue'
  }
  return colors[severity] || 'default'
}

const getSeverityText = (severity: string) => {
  const texts: Record<string, string> = {
    'CRITICAL': '严重',
    'ERROR': '错误',
    'WARNING': '警告',
    'INFO': '信息'
  }
  return texts[severity] || severity
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString()
}

onMounted(() => {
  loadAlerts()
  loadDevices()
})
</script>

<style scoped>
.alert-management-view {
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

.filter-bar {
  margin-bottom: 16px;
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