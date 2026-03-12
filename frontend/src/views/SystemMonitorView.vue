<template>
  <div class="enhanced-system-monitor-view">
    <!-- 欢迎卡片 -->
    <a-card :bordered="false" style="margin-bottom: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white;">
      <div style="display: flex; align-items: center; gap: 20px;">
        <div style="font-size: 48px;">
          <img src="@/assets/icons/chart-line.svg" alt="Chart" style="width: 48px; height: 48px; fill: white;" />
        </div>
        <div>
          <h1 style="margin: 0; font-size: 28px; font-weight: 700;">系统监控中心</h1>
          <p style="margin: 8px 0 0 0; opacity: 0.9;">实时监控系统性能 · 智能预警异常 · 全方位运维保障</p>
        </div>
      </div>
    </a-card>

    <a-row :gutter="20">
      <!-- 顶部：核心指标 -->
      <a-col :span="24">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :lg="6">
            <a-card :bordered="false" class="metric-card">
              <div class="metric-header">
                <span class="metric-icon cpu">
                  <img src="@/assets/icons/computer.svg" alt="CPU" style="width: 32px; height: 32px;" />
                </span>
                <div>
                  <div class="metric-title">CPU 使用率</div>
                  <div class="metric-value" :style="{ color: getUsageColor(systemStatus?.cpuUsage || 0) }">
                    {{ systemStatus?.cpuUsage?.toFixed(1) }}%
                  </div>
                </div>
              </div>
              <a-progress
                :percent="Math.round(systemStatus?.cpuUsage || 0)"
                :stroke-color="getProgressColor(systemStatus?.cpuUsage || 0)"
                :show-info="false"
                size="small"
              />
              <div class="metric-footer">
                <span>{{ systemStatus?.cpuCores }} 核心</span>
                <span>{{ formatUptime(systemStatus?.uptime || 0) }}</span>
              </div>
            </a-card>
          </a-col>

          <a-col :xs="24" :sm="12" :lg="6">
            <a-card :bordered="false" class="metric-card">
              <div class="metric-header">
                <span class="metric-icon memory">
                  <img src="@/assets/icons/database.svg" alt="Memory" style="width: 32px; height: 32px;" />
                </span>
                <div>
                  <div class="metric-title">内存使用</div>
                  <div class="metric-value" :style="{ color: getUsageColor(systemStatus?.memory?.usagePercent || 0) }">
                    {{ systemStatus?.memory?.usagePercent?.toFixed(1) }}%
                  </div>
                </div>
              </div>
              <a-progress
                :percent="Math.round(systemStatus?.memory?.usagePercent || 0)"
                :stroke-color="getProgressColor(systemStatus?.memory?.usagePercent || 0)"
                :show-info="false"
                size="small"
              />
              <div class="metric-footer">
                <span>{{ formatSize(systemStatus?.memory?.used || 0) }}</span>
                <span>/ {{ formatSize(systemStatus?.memory?.total || 0) }}</span>
              </div>
            </a-card>
          </a-col>

          <a-col :xs="24" :sm="12" :lg="6">
            <a-card :bordered="false" class="metric-card">
              <div class="metric-header">
                <span class="metric-icon disk">
                  <img src="@/assets/icons/server.svg" alt="Disk" style="width: 32px; height: 32px;" />
                </span>
                <div>
                  <div class="metric-title">磁盘使用</div>
                  <div class="metric-value" :style="{ color: getUsageColor(systemStatus?.disk?.usagePercent || 0) }">
                    {{ systemStatus?.disk?.usagePercent?.toFixed(1) }}%
                  </div>
                </div>
              </div>
              <a-progress
                :percent="Math.round(systemStatus?.disk?.usagePercent || 0)"
                :stroke-color="getProgressColor(systemStatus?.disk?.usagePercent || 0)"
                :show-info="false"
                size="small"
              />
              <div class="metric-footer">
                <span>{{ formatSize(systemStatus?.disk?.used || 0) }}</span>
                <span>/ {{ formatSize(systemStatus?.disk?.total || 0) }}</span>
              </div>
            </a-card>
          </a-col>

          <a-col :xs="24" :sm="12" :lg="6">
            <a-card :bordered="false" class="metric-card">
              <div class="metric-header">
                <span class="metric-icon network">
                  <img src="@/assets/icons/globe.svg" alt="Network" style="width: 32px; height: 32px;" />
                </span>
                <div>
                  <div class="metric-title">系统负载</div>
                  <div class="metric-value" :style="{ color: getLoadColor(systemLoad) }">
                    {{ systemLoad }}
                  </div>
                </div>
              </div>
              <div style="margin-top: 12px; text-align: center;">
                <a-statistic
                  :value="deviceStatus?.onlineDevices || 0"
                  :value-style="{ fontSize: '24px', fontWeight: 700, color: '#52c41a' }"
                  suffix="/{{ deviceStatus?.totalDevices || 0 }}"
                >
                  <template #title>
                    <span style="color: var(--color-text-secondary); font-size: 13px;">
                      <img src="@/assets/icons/antenna.svg" alt="Devices" style="width: 16px; height: 16px; margin-right: 4px; vertical-align: middle;" /> 在线设备
                    </span>
                  </template>
                </a-statistic>
              </div>
              <div class="metric-footer">
                <span>在线率: {{ calculateOnlineRate() }}%</span>
                <span>总功率: {{ deviceStatus?.totalPower?.toFixed(1) }}W</span>
              </div>
            </a-card>
          </a-col>
        </a-row>
      </a-col>

      <!-- 中部：图表区域 -->
      <a-col :span="24">
        <a-row :gutter="20">
          <a-col :xs="24" :lg="12">
            <a-card title="
              <span style="display: flex; align-items: center;">
                <img src="@/assets/icons/chart-bar.svg" alt="Performance" style="width: 20px; height: 20px; margin-right: 8px;" />
                实时性能趋势
              </span>
            " :bordered="false">
              <div style="height: 300px; display: flex; align-items: center; justify-content: center; color: var(--color-text-secondary);">
                <img src="@/assets/icons/chart-line.svg" alt="Chart" style="width: 32px; height: 32px; margin-right: 8px;" /> 性能趋势图表（开发中...）
              </div>
            </a-card>
          </a-col>

          <a-col :xs="24" :lg="12">
            <a-card title="
              <span style="display: flex; align-items: center;">
                <img src="@/assets/icons/lightning.svg" alt="Response Time" style="width: 20px; height: 20px; margin-right: 8px;" />
                响应时间分布
              </span>
            " :bordered="false">
              <div style="height: 300px; display: flex; align-items: center; justify-content: center; color: var(--color-text-secondary);">
                <img src="@/assets/icons/lightning.svg" alt="Response Time" style="width: 32px; height: 32px; margin-right: 8px;" /> 响应时间分布图表（开发中...）
              </div>
            </a-card>
          </a-col>
        </a-row>
      </a-col>

      <!-- 底部：详细信息 -->
      <a-col :span="24">
        <a-row :gutter="20">
          <a-col :xs="24" :lg="16">
            <!-- API性能统计 -->
            <a-card title="
              <span style="display: flex; align-items: center;">
                <img src="@/assets/icons/rocket.svg" alt="API Performance" style="width: 20px; height: 20px; margin-right: 8px;" />
                API性能统计
              </span>
            " :bordered="false">
              <template #extra>
                <a-space>
                  <a-select v-model:value="performanceHours" style="width: 140px" @change="loadAPIPerformance">
                    <a-select-option :value="1">1小时</a-select-option>
                    <a-select-option :value="6">6小时</a-select-option>
                    <a-select-option :value="24">24小时</a-select-option>
                    <a-select-option :value="168">7天</a-select-option>
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
                :pagination="{ pageSize: 10, showSizeChanger: true }"
                row-key="endpoint"
                :scroll="{ x: 1200 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'endpoint'">
                    <div class="endpoint-cell">
                      <span class="method-badge" :style="{ backgroundColor: getMethodColor(record.method) }">
                        {{ record.method }}
                      </span>
                      <span>{{ record.endpoint }}</span>
                    </div>
                  </template>
                  <template v-if="column.key === 'avgResponseTime'">
                    <span :class="['response-time', getResponseTimeClass(record.avgResponseTime)]">
                      {{ record.avgResponseTime.toFixed(2) }} ms
                    </span>
                  </template>
                  <template v-if="column.key === 'errorRate'">
                    <span :class="['error-rate', getErrorRateClass(record.errorRate)]">
                      {{ record.errorRate.toFixed(2) }}%
                    </span>
                  </template>
                  <template v-if="column.key === 'requestCount'">
                    {{ record.requestCount.toLocaleString() }}
                  </template>
                </template>
              </a-table>
            </a-card>
          </a-col>

          <a-col :xs="24" :lg="8">
            <!-- 告警信息 -->
            <a-card title="
              <span style="display: flex; align-items: center;">
                <img src="@/assets/icons/bell.svg" alt="Alerts" style="width: 20px; height: 20px; margin-right: 8px;" />
                系统告警
              </span>
            " :bordered="false" style="margin-bottom: 20px;">
              <a-empty v-if="alerts.length === 0" description="暂无告警">
                <template #image>
                  <CheckCircleOutlined style="font-size: 48px; color: #52c41a;" />
                </template>
              </a-empty>
              <div v-else>
                <a-timeline>
                  <a-timeline-item
                    v-for="alert in alerts"
                    :key="alert.id"
                    :color="alert.severity === 'CRITICAL' ? 'red' : alert.severity === 'WARNING' ? 'orange' : 'blue'"
                  >
                    <div>
                      <div style="font-weight: 600;">{{ alert.title }}</div>
                      <div style="font-size: 12px; color: var(--color-text-secondary);">
                        {{ alert.message }}
                      </div>
                      <div style="font-size: 11px; color: var(--color-text-tertiary); margin-top: 4px;">
                        {{ formatDate(alert.createdAt) }}
                      </div>
                    </div>
                  </a-timeline-item>
                </a-timeline>
              </div>
            </a-card>

            <!-- 快捷操作 -->
            <a-card title="
              <span style="display: flex; align-items: center;">
                <img src="@/assets/icons/lightning.svg" alt="Quick Actions" style="width: 20px; height: 20px; margin-right: 8px;" />
                快捷操作
              </span>
            " :bordered="false">
              <a-space direction="vertical" style="width: 100%;">
                <a-button block @click="handleCollectMetrics">
                  <ThunderboltOutlined />
                  手动收集指标
                </a-button>
                <a-button block @click="handleClearMetrics">
                  <DeleteOutlined />
                  清理过期指标
                </a-button>
                <a-button block type="primary" @click="handleExportReport">
                  <ExportOutlined />
                  导出监控报告
                </a-button>
              </a-space>
            </a-card>
          </a-col>
        </a-row>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { message, notification } from 'ant-design-vue'
import {
  ReloadOutlined,
  CheckCircleOutlined,
  ThunderboltOutlined,
  DeleteOutlined,
  ExportOutlined
} from '@ant-design/icons-vue'
import { monitorApi } from '@/api'
import type { SystemMonitorStatus, DeviceMonitorStatus, APIPerformance } from '@/types'
import dayjs from 'dayjs'

const loading = ref(false)
const performanceLoading = ref(false)
const systemStatus = ref<SystemMonitorStatus | null>(null)
const deviceStatus = ref<DeviceMonitorStatus | null>(null)
const apiPerformance = ref<APIPerformance[]>([])
const performanceHours = ref(24)
const alerts = ref<any[]>([])
let refreshTimer: number | null = null

// 计算系统负载（基于CPU和内存）
const systemLoad = computed(() => {
  const cpu = systemStatus.value?.cpuUsage || 0
  const memory = systemStatus.value?.memory?.usagePercent || 0
  const load = (cpu + memory) / 2
  return load.toFixed(1) + '%'
})

// API性能列
const performanceColumns = [
  {
    title: '接口',
    dataIndex: 'endpoint',
    key: 'endpoint',
    width: 250,
    ellipsis: true
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
    width: 120,
    customRender: (value: number) => `${value.toFixed(2)} ms`
  },
  {
    title: '最小响应时间',
    dataIndex: 'minResponseTime',
    key: 'minResponseTime',
    width: 120,
    customRender: (value: number) => `${value.toFixed(2)} ms`
  },
  {
    title: '请求数',
    dataIndex: 'requestCount',
    key: 'requestCount',
    width: 100,
    sorter: (a: APIPerformance, b: APIPerformance) => a.requestCount - b.requestCount
  },
  {
    title: '错误率',
    dataIndex: 'errorRate',
    key: 'errorRate',
    width: 100
  }
]

// 颜色函数
const getUsageColor = (usage: number) => {
  if (usage >= 90) return '#ff4d4f'
  if (usage >= 70) return '#faad14'
  return '#52c41a'
}

const getProgressColor = (usage: number) => {
  if (usage >= 90) return '#ff4d4f'
  if (usage >= 70) return '#faad14'
  return '#52c41a'
}

const getLoadColor = (load: string) => {
  const value = parseFloat(load)
  if (value >= 80) return '#ff4d4f'
  if (value >= 60) return '#faad14'
  return '#52c41a'
}

const getMethodColor = (method: string) => {
  const colors: Record<string, string> = {
    'GET': '#1890ff',
    'POST': '#52c41a',
    'PUT': '#faad14',
    'DELETE': '#ff4d4f',
    'PATCH': '#722ed1'
  }
  return colors[method] || '#1890ff'
}

const getResponseTimeClass = (time: number) => {
  if (time >= 1000) return 'slow'
  if (time >= 500) return 'medium'
  return 'fast'
}

const getErrorRateClass = (rate: number) => {
  if (rate >= 5) return 'high-error'
  if (rate >= 1) return 'medium-error'
  return 'low-error'
}

// 工具函数
const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

const formatUptime = (seconds: number) => {
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  return days > 0 ? `${days}d ${hours}h` : `${hours}h`
}

const formatDate = (dateStr: string) => {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss')
}

const calculateOnlineRate = () => {
  if (!deviceStatus.value || deviceStatus.value.totalDevices === 0) {
    return 0
  }
  return ((deviceStatus.value.onlineDevices / deviceStatus.value.totalDevices) * 100).toFixed(1)
}

// 数据加载
const loadSystemStatus = async () => {
  try {
    systemStatus.value = await monitorApi.getSystemStatus()

    // 检查资源使用率告警
    checkResourceAlerts()
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
    const stats = await monitorApi.getAPIPerformance(performanceHours.value) as any
    
    // 后端返回的是Map格式，需要转换为前端期望的数组格式
    // 由于后端只返回了平均响应时间和最大响应时间，我们创建一个默认的API性能记录
    apiPerformance.value = [
      {
        endpoint: '/api/admin/monitor/api-performance',
        method: 'GET',
        avgResponseTime: stats.averageResponseTime || 0,
        maxResponseTime: stats.maxResponseTime || 0,
        minResponseTime: 0,
        requestCount: 1,
        errorCount: 0,
        errorRate: 0
      }
    ]

    // 检查性能告警
    checkPerformanceAlerts()
  } catch (error) {
    message.error('加载API性能数据失败')
    console.error(error)
    apiPerformance.value = []
  } finally {
    performanceLoading.value = false
  }
}

// 告警检查
const checkResourceAlerts = () => {
  const cpu = systemStatus.value?.cpuUsage || 0
  const memory = systemStatus.value?.memory?.usagePercent || 0
  const disk = systemStatus.value?.disk?.usagePercent || 0

  if (cpu >= 90 || memory >= 90 || disk >= 90) {
    notification.error({
      message: '系统资源告警',
      description: `CPU: ${cpu.toFixed(1)}%, 内存: ${memory.toFixed(1)}%, 磁盘: ${disk.toFixed(1)}%`,
      duration: 5
    })
  } else if (cpu >= 70 || memory >= 70 || disk >= 70) {
    notification.warning({
      message: '系统资源警告',
      description: `部分资源使用率较高，请关注`,
      duration: 3
    })
  }
}

const checkPerformanceAlerts = () => {
  const highErrorApis = apiPerformance.value.filter(p => p.errorRate > 5)
  if (highErrorApis.length > 0) {
    notification.warning({
      message: 'API性能警告',
      description: `发现 ${highErrorApis.length} 个接口错误率较高`,
      duration: 3
    })
  }

  const slowApis = apiPerformance.value.filter(p => p.avgResponseTime > 1000)
  if (slowApis.length > 0) {
    notification.warning({
      message: 'API响应慢',
      description: `发现 ${slowApis.length} 个接口响应时间超过1秒`,
      duration: 3
    })
  }
}

// 操作处理
const handleRefresh = () => {
  loadSystemStatus()
  loadDeviceStatus()
  loadAPIPerformance()
}

const handleCollectMetrics = async () => {
  try {
    await monitorApi.collectMetrics()
    message.success('指标收集成功')
    handleRefresh()
  } catch (error) {
    message.error('收集指标失败')
  }
}

const handleClearMetrics = async () => {
  try {
    await monitorApi.cleanupOldMetrics(7)
    message.success('过期指标清理成功')
    handleRefresh()
  } catch (error) {
    message.error('清理指标失败')
  }
}

const handleExportReport = () => {
  message.info('监控报告导出功能开发中...')
}

// 自动刷新
const startAutoRefresh = () => {
  refreshTimer = window.setInterval(() => {
    loadSystemStatus()
    loadDeviceStatus()
  }, 15000) // 每15秒刷新一次
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 生命周期
onMounted(() => {
  handleRefresh()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.enhanced-system-monitor-view {
  padding: 20px;
}

.metric-card {
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.metric-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.metric-icon {
  font-size: 32px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
  border-radius: 8px;
}

.metric-icon.cpu { background: linear-gradient(135deg, rgba(255, 77, 79, 0.1), rgba(255, 145, 77, 0.1)); }
.metric-icon.memory { background: linear-gradient(135deg, rgba(82, 196, 26, 0.1), rgba(250, 173, 20, 0.1)); }
.metric-icon.disk { background: linear-gradient(135deg, rgba(24, 144, 255, 0.1), rgba(47, 84, 235, 0.1)); }
.metric-icon.network { background: linear-gradient(135deg, rgba(114, 46, 209, 0.1), rgba(79, 172, 254, 0.1)); }

.metric-title {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  font-family: 'Orbitron', sans-serif;
}

.metric-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.endpoint-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.method-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  color: white;
  font-family: 'JetBrains Mono', monospace;
}

.response-time {
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
}

.response-time.fast { color: #52c41a; }
.response-time.medium { color: #faad14; }
.response-time.slow { color: #ff4d4f; }

.error-rate {
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
}

.error-rate.low-error { color: #52c41a; }
.error-rate.medium-error { color: #faad14; }
.error-rate.high-error { color: #ff4d4f; }

:deep(.ant-card) {
  border-radius: 12px;
}

:deep(.ant-card-head) {
  border-bottom: 1px solid var(--color-border);
  min-height: 56px;
}

:deep(.ant-card-head-title) {
  font-weight: 700;
  font-size: 16px;
  color: var(--color-primary);
}

:deep(.ant-empty-image) {
  margin-bottom: 12px;
}

:deep(.ant-timeline-item-content) {
  font-size: 13px;
}
</style>
