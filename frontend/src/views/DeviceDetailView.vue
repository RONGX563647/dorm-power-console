<template>
  <div class="device-detail">
    <div class="page-header">
      <a-space>
        <a-button @click="goBack">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
        <h1>{{ device?.name || '设备详情' }}</h1>
      </a-space>
      
      <div class="header-actions">
        <a-space>
          <a-space>
            <span style="color: #1a1a1a">实时刷新</span>
            <a-switch v-model:checked="autoRefresh" />
          </a-space>
          <span style="color: #1a1a1a">最后更新: {{ lastUpdateTime }}</span>
          <a-button @click="loadDevice">
            <template #icon><ReloadOutlined /></template>
            手动刷新
          </a-button>
        </a-space>
      </div>
    </div>

    <a-row :gutter="16">
      <a-col :span="16">
        <a-card title="设备信息">
          <a-descriptions bordered :column="2">
            <a-descriptions-item label="设备名称">{{ device?.name }}</a-descriptions-item>
            <a-descriptions-item label="房间">{{ device?.room }}</a-descriptions-item>
            <a-descriptions-item label="设备类型">{{ device?.type === 'strip' ? '智能排插' : '智能插座' }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-badge :status="device?.online ? 'success' : 'error'" :text="device?.online ? '在线' : '离线'" />
            </a-descriptions-item>
            <a-descriptions-item label="电压">{{ status?.sockets?.[0]?.voltage_v ? status.sockets[0].voltage_v.toFixed(1) : '--' }} V</a-descriptions-item>
            <a-descriptions-item label="电流">{{ status?.sockets?.[0]?.current_a ? status.sockets[0].current_a.toFixed(2) : '--' }} A</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card title="功率趋势" style="margin-top: 16px">
          <div class="chart-header">
            <a-button @click="loadDevice">
              <template #icon><ReloadOutlined /></template>
              手动刷新
            </a-button>
            <a-button @click="exportCsv">
              <template #icon><DownloadOutlined /></template>
              导出曲线 CSV
            </a-button>
          </div>
          <div ref="chartRef" class="chart-container"></div>
        </a-card>

        <a-card title="安全与策略" style="margin-top: 16px">
          <div class="security-section">
            <div class="security-item">
              <span>违规负载检测</span>
              <a-switch v-model:checked="securityRules.loadDetection" @change="updateSecurityRule('loadDetection')" />
            </div>
            <div class="security-item">
              <span>待机自动断电</span>
              <a-switch v-model:checked="securityRules.standbyPowerOff" @change="updateSecurityRule('standbyPowerOff')" />
            </div>
            
            <div class="rule-section">
              <h4>策略规则</h4>
              <div class="rule-item">
                <div>
                  <span>工作日 23:30 熄灯策略</span>
                  <span class="rule-scope">A 栋全体宿舍</span>
                </div>
                <a-switch v-model:checked="securityRules.lightsOut" @change="updateSecurityRule('lightsOut')" />
              </div>
              <div class="rule-item">
                <div>
                  <span>待机功率 &lt; 5W 自动断电</span>
                  <span class="rule-scope">非路由插孔</span>
                </div>
                <a-switch v-model:checked="securityRules.standbyPowerLimit" @change="updateSecurityRule('standbyPowerLimit')" />
              </div>
              <div class="rule-item">
                <div>
                  <span>违规负载瞬时切断</span>
                  <span class="rule-scope">高危设备指纹</span>
                </div>
                <a-switch v-model:checked="securityRules.instantCutoff" @change="updateSecurityRule('instantCutoff')" />
              </div>
            </div>
            
            <div class="simulation-buttons">
              <a-button type="primary" @click="simulateAlarm">模拟告警</a-button>
              <a-button @click="simulateOffline">模拟离线</a-button>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card title="实时数据">
          <a-statistic
            title="总功率"
            :value="status?.total_power_w || 0"
            :precision="1"
            suffix="W"
            :value-style="{ color: '#00d4ff' }"
          />
          <div style="margin-top: 16px">
            <a-statistic
              title="电压"
              :value="status?.sockets?.[0]?.voltage_v || 0"
              :precision="1"
              suffix="V"
              :value-style="{ color: '#e8f4ff' }"
            />
          </div>
          <div style="margin-top: 16px">
            <a-statistic
              title="电流"
              :value="status?.sockets?.[0]?.current_a || 0"
              :precision="2"
              suffix="A"
              :value-style="{ color: '#e8f4ff' }"
            />
          </div>
        </a-card>

        <a-card title="插座控制" style="margin-top: 16px">
          <a-list
            :data-source="status?.sockets || []"
            item-layout="vertical"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <div class="socket-item">
                  <div class="socket-header">
                    <span class="socket-name">{{ item.name }}</span>
                    <a-switch
                      :checked="item.on"
                      @change="toggleSocket(item.id)"
                      :loading="controlling"
                    />
                  </div>
                  <div class="socket-info">
                    <span>功率: {{ item.power_w.toFixed(1) }}W</span>
                    <span v-if="item.current_a">电流: {{ item.current_a.toFixed(2) }}A</span>
                  </div>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>

        <a-card title="快捷控制" style="margin-top: 16px">
          <div class="quick-control">
            <div class="control-item">
              <span>电源状态:</span>
              <span class="status-value">{{ powerStatus }}</span>
            </div>
            <div class="control-item">
              <span>定时关断:</span>
              <span class="status-value">{{ timerStatus }}</span>
            </div>
            <div class="control-item">
              <span>运行模式:</span>
              <span class="status-value mode">{{ modeStatus }}</span>
            </div>
            
            <div class="control-section">
              <h4>电源</h4>
              <a-button block @click="togglePower">开启电源</a-button>
            </div>
            
            <div class="control-section">
              <h4>定时关断</h4>
              <a-space wrap style="width: 100%">
                <a-button @click="setTimer(10)">10m</a-button>
                <a-button @click="setTimer(30)">30m</a-button>
                <a-button @click="setTimer(60)">1h</a-button>
              </a-space>
            </div>
            
            <div class="control-section">
              <h4>模式</h4>
              <a-space wrap style="width: 100%">
                <a-button @click="setMode('learning')">学习模式</a-button>
                <a-button @click="setMode('energy')">节能模式</a-button>
                <a-button @click="setMode('away')">离家模式</a-button>
              </a-space>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, ReloadOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { deviceApi, commandApi, alertApi } from '@/api'
import { deviceStatusService, createPollingManager } from '@/services/deviceStatusService'
import type { Device, StripStatus, TelemetryPoint } from '@/types'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'

const router = useRouter()
const route = useRoute()

const device = ref<Device | null>(null)
const status = ref<StripStatus | null>(null)
const telemetry = ref<TelemetryPoint[]>([])
const chartRef = ref<HTMLElement>()
const controlling = ref(false)
const autoRefresh = ref(true)
const lastUpdateTime = ref('')
let chartInstance: ECharts | null = null

// 安全规则状态
const securityRules = ref({
  loadDetection: true,
  standbyPowerOff: true,
  lightsOut: true,
  standbyPowerLimit: true,
  instantCutoff: true
})

// 快捷控制状态
const powerStatus = computed(() => status?.value?.sockets?.some(s => s.on) ? '开启' : '关闭')
const timerStatus = ref('未设置')
const modeStatus = ref('标准模式')

const loadDevice = async () => {
  try {
    const deviceId = route.params.id as string
    const [dev, telem] = await Promise.all([
      deviceApi.getDevices().then(devs => devs.find(d => d.id === deviceId) || null),
      deviceApi.getTelemetry(deviceId)
    ])
    
    device.value = dev
    status.value = await deviceStatusService.getDeviceStatus(deviceId)
    telemetry.value = telem
    lastUpdateTime.value = new Date().toLocaleString()
    
    initChart()
  } catch (error: any) {
    message.error('Failed to load device data')
  }
}

const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: telemetry.value.map(p => new Date(p.ts * 1000).toLocaleTimeString())
    },
    yAxis: {
      type: 'value',
      name: '功率 (W)'
    },
    series: [
      {
        name: '功率',
        type: 'line',
        smooth: true,
        data: telemetry.value.map(p => p.power_w),
        itemStyle: {
          color: '#00d4ff'
        }
      }
    ]
  }
  
  chartInstance.setOption(option)
}

const toggleSocket = async (socketId: number) => {
  try {
    controlling.value = true
    await commandApi.sendCommand(route.params.id as string, { action: 'toggle', socket: socketId })
    message.success('Command sent')
    await loadDevice()
  } catch (error: any) {
    message.error('Failed to control socket')
  } finally {
    controlling.value = false
  }
}

const goBack = () => {
  router.back()
}

const exportCsv = () => {
  // 简单的CSV导出实现
  const headers = ['时间', '功率 (W)']
  const rows = telemetry.value.map(p => [
    new Date(p.ts * 1000).toLocaleString(),
    p.power_w
  ])
  
  const csvContent = [
    headers.join(','),
    ...rows.map(row => row.join(','))
  ].join('\n')
  
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `power-trend-${device.value?.id}-${new Date().toISOString().split('T')[0]}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const updateSecurityRule = (rule: string) => {
  message.success(`规则 ${rule} 已更新`)
}

const simulateAlarm = async () => {
  try {
    await alertApi.simulateAlarm(route.params.id as string, 'power', 1500)
    message.success('模拟告警已触发，请查看告警列表')
  } catch (error: any) {
    message.error('模拟告警失败')
  }
}

const simulateOffline = async () => {
  try {
    await deviceApi.updateDevice(route.params.id as string, { online: false })
    message.success('设备已模拟离线')
    await loadDevice()
  } catch (error: any) {
    message.error('模拟离线失败')
  }
}

const togglePower = () => {
  message.success('电源状态已切换')
}

const setTimer = (minutes: number) => {
  timerStatus.value = `${minutes}分钟后`
  message.success(`已设置 ${minutes} 分钟后关断`)
}

const setMode = (mode: string) => {
  const modeMap: Record<string, string> = {
    learning: '学习模式',
    energy: '节能模式',
    away: '离家模式'
  }
  modeStatus.value = modeMap[mode]
  message.success(`已切换到 ${modeMap[mode]}`)
}

const pollingManager = createPollingManager(loadDevice, 10000)

onMounted(() => {
  loadDevice()
  pollingManager.start()
})

onUnmounted(() => {
  pollingManager.stop()
  chartInstance?.dispose()
})
</script>

<style scoped>
.device-detail {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h1 {
  color: #1a1a1a;
  margin: 0;
  display: inline-block;
}

.header-actions {
  display: flex;
  align-items: center;
}

.socket-item {
  width: 100%;
  padding: 8px 0;
}

.socket-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.socket-name {
  color: #1a1a1a;
  font-weight: 500;
}

.socket-info {
  color: #1a1a1a;
  font-size: 13px;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.security-section {
  padding: 16px 0;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.rule-section {
  margin: 24px 0;
}

.rule-section h4 {
  color: #1a1a1a;
  margin-bottom: 16px;
}

.rule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.rule-item div {
  display: flex;
  flex-direction: column;
}

.rule-scope {
  font-size: 12px;
  color: #1a1a1a;
  margin-top: 4px;
}

.simulation-buttons {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.quick-control {
  padding: 8px 0;
}

.control-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.status-value {
  color: #00d4ff;
  font-weight: 500;
}

.status-value.mode {
  color: #00e676;
}

.control-section {
  margin-top: 24px;
}

.control-section h4 {
  color: #1a1a1a;
  margin-bottom: 12px;
}

:deep(.ant-card) {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
}

:deep(.ant-card-head) {
  color: #1a1a1a;
  border-bottom-color: rgba(0, 212, 255, 0.15);
}

:deep(.ant-descriptions-view) {
  color: #1a1a1a;
}

:deep(.ant-descriptions-item-label) {
  color: #1a1a1a;
}
</style>
