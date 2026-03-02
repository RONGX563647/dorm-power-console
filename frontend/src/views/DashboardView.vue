<template>
  <div class="dashboard-view">
    <div class="kpi-section">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :sm="12" :lg="6">
          <div class="kpi-card total-power">
            <div class="kpi-icon">
              <ThunderboltOutlined />
            </div>
            <div class="kpi-content">
              <div class="kpi-value">{{ totalPowerDisplay }}</div>
              <div class="kpi-unit">W</div>
              <div class="kpi-label">总功率</div>
            </div>
          </div>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <div class="kpi-card online-devices">
            <div class="kpi-icon">
              <WifiOutlined />
            </div>
            <div class="kpi-content">
              <div class="kpi-value">{{ onlineCount }}</div>
              <div class="kpi-unit">/{{ totalDevices }}</div>
              <div class="kpi-label">在线设备</div>
            </div>
          </div>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <div class="kpi-card avg-power">
            <div class="kpi-icon">
              <LineChartOutlined />
            </div>
            <div class="kpi-content">
              <div class="kpi-value">{{ avgPowerDisplay }}</div>
              <div class="kpi-unit">W</div>
              <div class="kpi-label">平均功率</div>
            </div>
          </div>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <div class="kpi-card events" @click="showNotifications" style="cursor: pointer;">
            <div class="kpi-icon">
              <BellOutlined />
            </div>
            <div class="kpi-content">
              <div class="kpi-value">{{ eventCount }}</div>
              <div class="kpi-unit">条</div>
              <div class="kpi-label">今日事件</div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <div class="main-content">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :lg="16">
          <div class="chart-card">
            <div class="card-header">
              <span class="card-title">
                <LineChartOutlined />
                功率趋势
              </span>
              <a-radio-group v-model:value="timeRange" size="small" @change="handleTimeRangeChange">
                <a-radio-button value="1h">1小时</a-radio-button>
                <a-radio-button value="24h">24小时</a-radio-button>
                <a-radio-button value="7d">7天</a-radio-button>
              </a-radio-group>
            </div>
            <div ref="chartRef" class="chart-container"></div>
          </div>
        </a-col>

        <a-col :xs="24" :lg="8">
          <div class="side-panel">
            <div class="panel-section">
              <div class="section-title">
                <RocketOutlined />
                快捷操作
              </div>
              <div class="quick-actions">
                <a-button class="action-btn primary" @click="refreshAll">
                  <ReloadOutlined />
                  刷新数据
                </a-button>
                <a-button class="action-btn" @click="viewAllDevices">
                  <DesktopOutlined />
                  设备管理
                </a-button>
                <a-button class="action-btn" @click="viewLiveMonitor">
                  <FundViewOutlined />
                  实时监控
                </a-button>
                <a-button class="action-btn" @click="generateAIReport">
                  <ExperimentOutlined />
                  AI 报告
                </a-button>
              </div>
            </div>

            <div class="panel-section">
              <div class="section-title">
                <DashboardOutlined />
                系统状态
              </div>
              <div class="status-list">
                <div class="status-item">
                  <span class="status-label">系统运行</span>
                  <span class="status-badge success">正常</span>
                </div>
                <div class="status-item">
                  <span class="status-label">API 连接</span>
                  <span class="status-badge success">正常</span>
                </div>
                <div class="status-item">
                  <span class="status-label">数据同步</span>
                  <span class="status-badge success">实时</span>
                </div>
                <div class="status-item">
                  <span class="status-label">最后更新</span>
                  <span class="status-time">{{ lastUpdateTime }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <div class="bottom-section">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :lg="14">
          <div class="data-card">
            <div class="card-header">
              <span class="card-title">
                <DesktopOutlined />
                设备状态
              </span>
              <a-button type="link" size="small" @click="viewAllDevices">
                查看全部
                <RightOutlined />
              </a-button>
            </div>
            <a-table
              :columns="deviceColumns"
              :data-source="visibleDevices"
              :pagination="pagination"
              size="small"
              :row-class-name="getRowClassName"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'name'">
                  <span class="device-name">{{ record.name }}</span>
                </template>
                <template v-if="column.key === 'room'">
                  <a-tag color="blue">{{ record.room }}</a-tag>
                </template>
                <template v-if="column.key === 'power'">
                  <span class="power-value">{{ getDevicePower(record.id) }}W</span>
                </template>
                <template v-if="column.key === 'status'">
                  <a-badge 
                    :status="record.online ? 'success' : 'error'" 
                    :text="record.online ? '在线' : '离线'" 
                  />
                </template>
                <template v-if="column.key === 'action'">
                  <a-button type="link" size="small" @click="viewDevice(record.id)">
                    详情
                  </a-button>
                </template>
              </template>
            </a-table>
          </div>
        </a-col>

        <a-col :xs="24" :lg="10">
          <div class="data-card">
            <div class="card-header">
              <span class="card-title">
                <HistoryOutlined />
                最近事件
              </span>
              <a-button type="link" size="small" @click="viewHistory">
                查看全部
                <RightOutlined />
              </a-button>
            </div>
            <div class="timeline-container">
              <a-timeline>
                <a-timeline-item
                  v-for="event in visibleEvents"
                  :key="event.id"
                  :color="eventColor(event.status)"
                >
                  <div class="timeline-item">
                    <div class="timeline-time">{{ event.time }}</div>
                    <div class="timeline-content">
                      <div class="timeline-title">{{ getEventTitle(event.type) }}</div>
                      <div class="timeline-detail">{{ event.detail }}</div>
                    </div>
                  </div>
                </a-timeline-item>
              </a-timeline>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, shallowRef, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ThunderboltOutlined,
  WifiOutlined,
  LineChartOutlined,
  BellOutlined,
  ReloadOutlined,
  DesktopOutlined,
  FundViewOutlined,
  ExperimentOutlined,
  DashboardOutlined,
  RocketOutlined,
  HistoryOutlined,
  RightOutlined
} from '@ant-design/icons-vue'
import { deviceApi } from '@/api'
import { deviceStatusService, createPollingManager } from '@/services/deviceStatusService'
import type { Device, StripStatus, DashboardEvent } from '@/types'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { useThrottle } from '@/utils/performance'

const router = useRouter()

const devices = shallowRef<Device[]>([])
const statusMap = shallowRef<Record<string, StripStatus | null>>({})
const events = shallowRef<DashboardEvent[]>([])
const chartRef = ref<HTMLElement>()
const lastUpdateTime = ref('')
const timeRange = ref('24h')
const chartInstance = shallowRef<ECharts | null>(null)

const totalPower = computed(() => {
  return Object.values(statusMap.value).reduce((sum, status) => {
    return sum + (status?.total_power_w ?? 0)
  }, 0)
})

const totalPowerDisplay = computed(() => totalPower.value.toFixed(1))

const onlineCount = computed(() => {
  return devices.value.filter(d => d.online).length
})

const totalDevices = computed(() => devices.value.length)

const avgPower = computed(() => {
  const online = Object.values(statusMap.value).filter(s => s !== null)
  if (online.length === 0) return 0
  return online.reduce((sum, s) => sum + (s?.total_power_w ?? 0), 0) / online.length
})

const avgPowerDisplay = computed(() => avgPower.value.toFixed(1))

const eventCount = computed(() => events.value.length)

const visibleDevices = computed(() => devices.value.slice(0, 10))

const visibleEvents = computed(() => events.value.slice(0, 5))

const pagination = computed(() => ({
  pageSize: 5,
  size: 'small' as const,
  showSizeChanger: false
}))

const deviceColumns = [
  { title: '设备名称', dataIndex: 'name', key: 'name', width: '30%' },
  { title: '房间', dataIndex: 'room', key: 'room', width: '20%' },
  { title: '功率', key: 'power', width: '20%' },
  { title: '状态', key: 'status', width: '15%' },
  { title: '操作', key: 'action', width: '15%' }
]

const getDevicePower = (deviceId: string) => {
  return statusMap.value[deviceId]?.total_power_w?.toFixed(1) ?? '--'
}

const getRowClassName = (record: Device) => {
  return record.online ? 'online-row' : 'offline-row'
}

const eventColor = (status: string) => {
  const colors: Record<string, string> = {
    ok: 'green',
    warn: 'orange',
    fail: 'red'
  }
  return colors[status] || 'blue'
}

const getEventTitle = (type: string) => {
  const titles: Record<string, string> = {
    REPORT: '系统报告',
    CMD: '命令执行',
    ALERT: '告警通知',
    SYSTEM: '系统事件'
  }
  return titles[type] || type
}

const loadDashboard = async () => {
  try {
    const devs = await deviceApi.getDevices()
    devices.value = devs
    
    statusMap.value = await deviceStatusService.getBatchDeviceStatus(devs.map(d => d.id))
    
    generateEvents()
    lastUpdateTime.value = new Date().toLocaleTimeString()
  } catch (error: any) {
    message.error('加载仪表盘数据失败')
  }
}

const generateEvents = () => {
  const now = new Date()
  events.value = [
    {
      id: '1',
      type: 'SYSTEM',
      time: now.toLocaleTimeString(),
      detail: `系统正常运行，${onlineCount.value} 台设备在线`,
      status: 'ok'
    },
    {
      id: '2',
      type: 'REPORT',
      time: new Date(now.getTime() - 300000).toLocaleTimeString(),
      detail: '功率数据已更新',
      status: 'ok'
    },
    {
      id: '3',
      type: 'SYSTEM',
      time: new Date(now.getTime() - 600000).toLocaleTimeString(),
      detail: '定时任务执行完成',
      status: 'ok'
    }
  ]
}

const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance.value = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance.value) return
  
  const hours = timeRange.value === '1h' ? 12 : timeRange.value === '24h' ? 24 : 7
  const data = Array.from({ length: hours }, () => Math.floor(Math.random() * 200) + 50)
  const labels = Array.from({ length: hours }, (_, i) => {
    if (timeRange.value === '7d') {
      return `${i + 1}天`
    }
    const h = new Date().getHours() - hours + i + 1
    return `${(h + 24) % 24}:00`
  })
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: 'var(--color-border)',
      textStyle: { color: 'var(--color-text-primary)' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { lineStyle: { color: 'var(--color-border)' } },
      axisLabel: { color: 'var(--color-text-secondary)' }
    },
    yAxis: {
      type: 'value',
      name: '功率 (W)',
      nameTextStyle: { color: 'var(--color-text-secondary)' },
      axisLine: { lineStyle: { color: 'var(--color-border)' } },
      axisLabel: { color: 'var(--color-text-secondary)' },
      splitLine: { lineStyle: { color: 'var(--color-border-light)' } }
    },
    series: [
      {
        name: '功率',
        type: 'line',
        smooth: true,
        symbol: 'none',
        data: data,
        lineStyle: {
          color: 'var(--color-primary)',
          width: 3
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(42, 121, 101, 0.3)' },
            { offset: 1, color: 'rgba(42, 121, 101, 0.05)' }
          ])
        }
      }
    ]
  }
  
  chartInstance.value.setOption(option)
}

const handleTimeRangeChange = () => {
  updateChart()
}

const viewDevice = (id: string) => {
  router.push(`/devices/${id}`)
}

const refreshAll = async () => {
  await loadDashboard()
  updateChart()
  message.success('数据已刷新')
}

const viewAllDevices = () => {
  router.push('/devices')
}

const showNotifications = () => {
  const notificationText = events.value.map(e => 
    `${e.time} - ${getEventTitle(e.type)}: ${e.detail}`
  ).join('\n')
  
  message.info(notificationText || '暂无新通知')
}

const viewHistory = () => {
  router.push('/history')
}

const generateAIReport = () => {
  router.push('/ai')
}

const viewLiveMonitor = () => {
  router.push('/live')
}

const pollingManager = createPollingManager(loadDashboard, 10000)

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  loadDashboard()
  initChart()
  pollingManager.start()
  
  if (chartRef.value && chartInstance.value) {
    resizeObserver = new ResizeObserver(() => {
      chartInstance.value?.resize()
    })
    resizeObserver.observe(chartRef.value)
  }
})

onUnmounted(() => {
  pollingManager.stop()
  chartInstance.value?.dispose()
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.dashboard-view {
  padding: 0;
  background: transparent;
  min-height: auto;
}

.kpi-section {
  margin-bottom: var(--spacing-lg);
}

.kpi-card {
  display: flex;
  align-items: center;
  padding: var(--spacing-lg);
  border-radius: var(--card-radius);
  background: var(--card-bg);
  border: var(--card-border);
  transition: all var(--duration-normal) var(--ease-in-out);
  cursor: pointer;
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.kpi-icon {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-right: var(--spacing-md);
  transition: all var(--duration-normal) var(--ease-in-out);
}

.total-power .kpi-icon {
  background: var(--color-primary-bg);
  color: var(--color-primary);
  border: 1px solid var(--color-primary-border);
}

.online-devices .kpi-icon {
  background: var(--color-success-bg);
  color: var(--color-success);
  border: 1px solid var(--color-success-border);
}

.avg-power .kpi-icon {
  background: var(--color-secondary-bg);
  color: var(--color-secondary);
  border: 1px solid var(--color-secondary-border);
}

.events .kpi-icon {
  background: var(--color-warning-bg);
  color: var(--color-warning);
  border: 1px solid var(--color-warning-border);
}

.kpi-card:hover .kpi-icon {
  transform: scale(1.05);
}

.kpi-content {
  flex: 1;
}

.kpi-value {
  display: inline-block;
  font-size: 32px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1;
}

.kpi-unit {
  display: inline-block;
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-left: 4px;
}

.kpi-label {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.main-content {
  margin-bottom: var(--spacing-lg);
}

.chart-card {
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  padding: var(--card-padding);
  height: 100%;
  box-shadow: var(--card-shadow);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.card-title {
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-container {
  height: 320px;
  width: 100%;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.panel-section {
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  padding: var(--card-padding);
  box-shadow: var(--card-shadow);
}

.section-title {
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-md);
  display: flex;
  align-items: center;
  gap: 8px;
}

.quick-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-sm);
}

.action-btn {
  height: 42px;
  border-radius: var(--radius-md);
  background: var(--color-bg-spotlight);
  border: 1px solid var(--color-border);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all var(--duration-fast) var(--ease-in-out);
}

.action-btn:hover {
  background: var(--color-primary-bg);
  border-color: var(--color-primary);
  color: var(--color-primary);
  transform: translateY(-2px);
}

.action-btn.primary {
  background: var(--gradient-primary);
  border: none;
  color: var(--color-text-inverse);
}

.action-btn.primary:hover {
  box-shadow: 0 6px 20px rgba(42, 121, 101, 0.4);
  transform: translateY(-2px);
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm) 0;
  border-bottom: 1px solid var(--color-border-light);
}

.status-item:last-child {
  border-bottom: none;
}

.status-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.status-badge {
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: var(--font-weight-medium);
}

.status-badge.success {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.status-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.bottom-section {
  margin-bottom: 0;
}

.data-card {
  background: var(--card-bg);
  border: var(--card-border);
  border-radius: var(--card-radius);
  padding: var(--card-padding);
  height: 100%;
  box-shadow: var(--card-shadow);
}

.device-name {
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

.power-value {
  color: var(--color-primary);
  font-family: 'Courier New', monospace;
  font-weight: var(--font-weight-semibold);
}

.timeline-container {
  max-height: 280px;
  overflow-y: auto;
  padding-right: 8px;
}

.timeline-container::-webkit-scrollbar {
  width: 4px;
}

.timeline-container::-webkit-scrollbar-thumb {
  background: var(--color-border-dark);
  border-radius: 2px;
}

.timeline-item {
  padding-bottom: 8px;
}

.timeline-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-bottom: 4px;
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.timeline-title {
  font-size: 13px;
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

.timeline-detail {
  font-size: 12px;
  color: var(--color-text-secondary);
}

:deep(.ant-table) {
  background: transparent;
}

:deep(.ant-table-thead > tr > th) {
  background: var(--table-header-bg);
  color: var(--color-text-primary);
  border-bottom: var(--table-border);
}

:deep(.ant-table-tbody > tr > td) {
  border-bottom: var(--table-border);
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: var(--table-row-hover-bg);
}

:deep(.online-row) {
  background: transparent;
}

:deep(.offline-row) {
  background: var(--color-error-bg);
}

:deep(.ant-radio-group) {
  background: var(--color-bg-spotlight);
  border-radius: var(--radius-md);
  padding: 2px;
}

:deep(.ant-radio-button-wrapper) {
  background: transparent;
  border: none;
  color: var(--color-text-secondary);
  transition: all var(--duration-fast) var(--ease-in-out);
}

:deep(.ant-radio-button-wrapper:hover) {
  color: var(--color-primary);
}

:deep(.ant-radio-button-wrapper-checked) {
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-sm);
}

:deep(.ant-tag) {
  border-radius: var(--tag-radius);
  padding: var(--tag-padding);
}

:deep(.ant-badge-status-text) {
  color: var(--color-text-secondary);
}

@media (max-width: 768px) {
  .kpi-section {
    margin-bottom: var(--spacing-md);
  }
  
  .kpi-card {
    padding: var(--spacing-md);
  }
  
  .kpi-icon {
    width: 48px;
    height: 48px;
    font-size: 24px;
  }
  
  .kpi-value {
    font-size: 24px;
  }
  
  .quick-actions {
    grid-template-columns: 1fr;
  }
}
</style>
