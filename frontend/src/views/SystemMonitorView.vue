<template>
  <div class="system-monitor-view">
    <a-page-header
      title="系统监控"
      sub-title="实时监控系统状态和性能指标"
      :back-icon="false"
    />

    <div class="content-wrapper">
      <!-- 系统状态卡片 -->
      <a-row :gutter="16" class="stats-row">
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="CPU 使用率"
              :value="systemStatus?.cpuUsage || 0"
              :precision="1"
              suffix="%"
              :value-style="{ color: getUsageColor(systemStatus?.cpuUsage || 0) }"
            >
              <template #prefix>
                <DesktopOutlined />
              </template>
            </a-statistic>
            <a-progress
              :percent="Math.round(systemStatus?.cpuUsage || 0)"
              :status="getProgressStatus(systemStatus?.cpuUsage || 0)"
              size="small"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="内存使用率"
              :value="systemStatus?.memory?.usagePercent || 0"
              :precision="1"
              suffix="%"
              :value-style="{ color: getUsageColor(systemStatus?.memory?.usagePercent || 0) }"
            >
              <template #prefix>
                <DatabaseOutlined />
              </template>
            </a-statistic>
            <a-progress
              :percent="Math.round(systemStatus?.memory?.usagePercent || 0)"
              :status="getProgressStatus(systemStatus?.memory?.usagePercent || 0)"
              size="small"
            />
            <div class="detail-info">
              {{ formatSize(systemStatus?.memory?.used || 0) }} / {{ formatSize(systemStatus?.memory?.total || 0) }}
            </div>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="磁盘使用率"
              :value="systemStatus?.disk?.usagePercent || 0"
              :precision="1"
              suffix="%"
              :value-style="{ color: getUsageColor(systemStatus?.disk?.usagePercent || 0) }"
            >
              <template #prefix>
                <HddOutlined />
              </template>
            </a-statistic>
            <a-progress
              :percent="Math.round(systemStatus?.disk?.usagePercent || 0)"
              :status="getProgressStatus(systemStatus?.disk?.usagePercent || 0)"
              size="small"
            />
            <div class="detail-info">
              {{ formatSize(systemStatus?.disk?.used || 0) }} / {{ formatSize(systemStatus?.disk?.total || 0) }}
            </div>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="系统运行时间"
              :value="formatUptime(systemStatus?.uptime || 0)"
              :value-style="{ color: '#722ed1', fontSize: '16px' }"
            >
              <template #prefix>
                <ClockCircleOutlined />
              </template>
            </a-statistic>
            <div class="detail-info">
              CPU核心数: {{ systemStatus?.cpuCores || 0 }}
            </div>
          </a-card>
        </a-col>
      </a-row>

      <!-- 设备监控状态 -->
      <a-row :gutter="16" class="device-stats-row">
        <a-col :span="12">
          <a-card title="设备状态概览">
            <a-row :gutter="16">
              <a-col :span="8">
                <a-statistic
                  title="总设备数"
                  :value="deviceStatus?.totalDevices || 0"
                  :value-style="{ color: '#1890ff' }"
                >
                  <template #prefix>
                    <AppstoreOutlined />
                  </template>
                </a-statistic>
              </a-col>
              <a-col :span="8">
                <a-statistic
                  title="在线设备"
                  :value="deviceStatus?.onlineDevices || 0"
                  :value-style="{ color: '#52c41a' }"
                >
                  <template #prefix>
                    <CheckCircleOutlined />
                  </template>
                </a-statistic>
              </a-col>
              <a-col :span="8">
                <a-statistic
                  title="离线设备"
                  :value="deviceStatus?.offlineDevices || 0"
                  :value-style="{ color: '#ff4d4f' }"
                >
                  <template #prefix>
                    <CloseCircleOutlined />
                  </template>
                </a-statistic>
              </a-col>
            </a-row>
            <div class="online-rate">
              在线率: {{ calculateOnlineRate() }}%
            </div>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card title="功率统计">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-statistic
                  title="总功率"
                  :value="deviceStatus?.totalPower || 0"
                  :precision="1"
                  suffix="W"
                  :value-style="{ color: '#fa8c16' }"
                >
                  <template #prefix>
                    <ThunderboltOutlined />
                  </template>
                </a-statistic>
              </a-col>
              <a-col :span="12">
                <a-statistic
                  title="平均功率"
                  :value="deviceStatus?.avgPowerPerDevice || 0"
                  :precision="1"
                  suffix="W"
                  :value-style="{ color: '#13c2c2' }"
                >
                  <template #prefix>
                    <LineChartOutlined />
                  </template>
                </a-statistic>
              </a-col>
            </a-row>
          </a-card>
        </a-col>
      </a-row>

      <!-- API性能统计 -->
      <a-card class="api-performance-card" title="API性能统计">
        <template #extra>
          <a-space>
            <a-select v-model:value="performanceHours" style="width: 120px" @change="loadAPIPerformance">
              <a-select-option :value="1">最近1小时</a-select-option>
              <a-select-option :value="6">最近6小时</a-select-option>
              <a-select-option :value="24">最近24小时</a-select-option>
              <a-select-option :value="168">最近7天</a-select-option>
            </a-select>
            <a-button @click="handleRefresh">
              <ReloadOutlined />
              刷新
            </a-button>
          </a-space>
        </template>

        <a-table
          :columns="performanceColumns"
          :data-source="apiPerformance"
          :loading="performanceLoading"
          :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          row-key="endpoint"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'avgResponseTime'">
              <span :style="{ color: getResponseTimeColor(record.avgResponseTime) }">
                {{ record.avgResponseTime.toFixed(2) }} ms
              </span>
            </template>
            <template v-if="column.key === 'errorRate'">
              <span :style="{ color: record.errorRate > 5 ? '#ff4d4f' : record.errorRate > 1 ? '#faad14' : '#52c41a' }">
                {{ record.errorRate.toFixed(2) }}%
              </span>
            </template>
            <template v-if="column.key === 'requestCount'">
              {{ record.requestCount.toLocaleString() }}
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  DesktopOutlined,
  DatabaseOutlined,
  HddOutlined,
  ClockCircleOutlined,
  AppstoreOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ThunderboltOutlined,
  LineChartOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import { monitorApi } from '@/api'
import type { SystemMonitorStatus, DeviceMonitorStatus, APIPerformance } from '@/types'

const loading = ref(false)
const performanceLoading = ref(false)
const systemStatus = ref<SystemMonitorStatus | null>(null)
const deviceStatus = ref<DeviceMonitorStatus | null>(null)
const apiPerformance = ref<APIPerformance[]>([])
const performanceHours = ref(24)
let refreshTimer: number | null = null

const performanceColumns = [
  {
    title: '端点',
    dataIndex: 'endpoint',
    key: 'endpoint'
  },
  {
    title: '方法',
    dataIndex: 'method',
    key: 'method',
    width: 80
  },
  {
    title: '平均响应时间',
    dataIndex: 'avgResponseTime',
    key: 'avgResponseTime',
    width: 140,
    sorter: (a: APIPerformance, b: APIPerformance) => a.avgResponseTime - b.avgResponseTime
  },
  {
    title: '最大响应时间',
    dataIndex: 'maxResponseTime',
    key: 'maxResponseTime',
    width: 140,
    render: (value: number) => `${value.toFixed(2)} ms`
  },
  {
    title: '请求数',
    dataIndex: 'requestCount',
    key: 'requestCount',
    width: 100,
    sorter: (a: APIPerformance, b: APIPerformance) => a.requestCount - b.requestCount
  },
  {
    title: '错误数',
    dataIndex: 'errorCount',
    key: 'errorCount',
    width: 100
  },
  {
    title: '错误率',
    dataIndex: 'errorRate',
    key: 'errorRate',
    width: 100
  }
]

const getUsageColor = (usage: number) => {
  if (usage >= 90) return '#ff4d4f'
  if (usage >= 70) return '#faad14'
  return '#52c41a'
}

const getProgressStatus = (usage: number) => {
  if (usage >= 90) return 'exception'
  if (usage >= 70) return 'normal'
  return 'success'
}

const getResponseTimeColor = (time: number) => {
  if (time >= 1000) return '#ff4d4f'
  if (time >= 500) return '#faad14'
  return '#52c41a'
}

const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatUptime = (seconds: number) => {
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)

  if (days > 0) {
    return `${days}天${hours}小时`
  } else if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  } else {
    return `${minutes}分钟`
  }
}

const calculateOnlineRate = () => {
  if (!deviceStatus.value || deviceStatus.value.totalDevices === 0) {
    return 0
  }
  return ((deviceStatus.value.onlineDevices / deviceStatus.value.totalDevices) * 100).toFixed(1)
}

const loadSystemStatus = async () => {
  try {
    systemStatus.value = await monitorApi.getSystemStatus()
  } catch (error) {
    console.error('加载系统状态失败', error)
  }
}

const loadDeviceStatus = async () => {
  try {
    deviceStatus.value = await monitorApi.getDeviceStatus()
  } catch (error) {
    console.error('加载设备状态失败', error)
  }
}

const loadAPIPerformance = async () => {
  performanceLoading.value = true
  try {
    apiPerformance.value = await monitorApi.getAPIPerformance(performanceHours.value)
  } catch (error) {
    message.error('加载API性能数据失败')
    console.error(error)
  } finally {
    performanceLoading.value = false
  }
}

const handleRefresh = () => {
  loadSystemStatus()
  loadDeviceStatus()
  loadAPIPerformance()
}

const startAutoRefresh = () => {
  refreshTimer = window.setInterval(() => {
    loadSystemStatus()
    loadDeviceStatus()
  }, 30000) // 每30秒刷新一次
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  handleRefresh()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped lang="scss">
.system-monitor-view {
  padding: 24px;

  .stats-row,
  .device-stats-row {
    margin-bottom: 24px;
  }

  .detail-info {
    margin-top: 8px;
    font-size: 12px;
    color: #666;
  }

  .online-rate {
    margin-top: 16px;
    text-align: center;
    font-size: 16px;
    font-weight: bold;
    color: #1890ff;
  }

  .api-performance-card {
    margin-top: 24px;
  }
}
</style>
