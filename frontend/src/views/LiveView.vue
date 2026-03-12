<template>
  <div class="live-view">
    <div class="page-header">
      <a-space>
        <h1>实时监控</h1>
        <a-space>
          <span style="color: #1a1a1a">实时刷新</span>
          <a-switch v-model:checked="autoRefresh" />
        </a-space>
        <span style="color: #1a1a1a">最后更新: {{ lastUpdateTime }}</span>
        <a-button @click="loadDevices">
          <template #icon><ReloadOutlined /></template>
          手动刷新
        </a-button>
      </a-space>
    </div>

    <a-row :gutter="16">
      <a-col :span="24">
        <a-card title="实时功率消耗">
          <div class="chart-header">
            <a-statistic
              title="总功率"
              :value="totalPower"
              :precision="1"
              suffix="W"
              :value-style="{ color: '#00d4ff', fontSize: '24px' }"
            />
            <a-statistic
              title="在线设备"
              :value="onlineCount"
              suffix="台"
              :value-style="{ color: '#52c41a', fontSize: '24px' }"
            />
          </div>
          <div ref="chartRef" class="chart-container"></div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="8" v-for="device in devices" :key="device.id">
        <a-card :title="device.name" :extra="device.room">
          <div class="device-info">
            <a-statistic
              title="功率"
              :value="getDevicePower(device.id)"
              :precision="1"
              suffix="W"
              :value-style="{ color: getPowerColor(getDevicePower(device.id)) }"
            />
            <div class="device-status">
              <a-badge :status="device.online ? 'success' : 'error'" :text="device.online ? '在线' : '离线'" />
            </div>
            <div class="device-details" v-if="statusMap[device.id]">
              <div class="detail-item">
                <span class="detail-label">电压:</span>
                <span class="detail-value">{{ (statusMap[device.id] as StripStatus)?.sockets?.[0]?.voltage_v?.toFixed(1) ?? '--' }} V</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">电流:</span>
                <span class="detail-value">{{ (statusMap[device.id] as StripStatus)?.sockets?.[0]?.current_a?.toFixed(2) ?? '--' }} A</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { deviceApi } from '@/api'
import { deviceStatusService, createPollingManager } from '@/services/deviceStatusService'
import type { Device, StripStatus } from '@/types'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'

const devices = ref<Device[]>([])
const statusMap = ref<Record<string, StripStatus | null>>({})
const chartRef = ref<HTMLElement>()
const autoRefresh = ref(true)
const lastUpdateTime = ref('')
let chartInstance: ECharts | null = null

// 计算属性
const totalPower = computed(() => {
  return devices.value.reduce((sum, device) => {
    return sum + (statusMap.value[device.id]?.total_power_w || 0)
  }, 0)
})

const onlineCount = computed(() => {
  return devices.value.filter(device => device.online).length
})

const getDevicePower = (deviceId: string) => {
  return statusMap.value[deviceId]?.total_power_w || 0
}

const getPowerColor = (power: number) => {
  if (power > 1000) return '#ff4d4f' // 高功率
  if (power > 500) return '#faad14' // 中功率
  return '#00d4ff' // 低功率
}

const loadDevices = async () => {
  try {
    devices.value = await deviceApi.getDevices()
    
    statusMap.value = await deviceStatusService.getBatchDeviceStatus(devices.value.map(d => d.id))
    lastUpdateTime.value = new Date().toLocaleString()
    updateChart()
  } catch (error: any) {
    message.error('Failed to load devices')
  }
}

const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: function(params: any) {
        let result = `${params[0].name}<br/>`
        params.forEach((param: any) => {
          result += `${param.marker}${param.seriesName}: ${param.value.toFixed(1)} W<br/>`
        })
        return result
      }
    },
    xAxis: {
      type: 'category',
      data: devices.value.map(d => d.name),
      axisLabel: {
        color: '#8ba3c7',
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '功率 (W)',
      nameTextStyle: {
        color: '#8ba3c7'
      },
      axisLabel: {
        color: '#8ba3c7'
      }
    },
    series: [
      {
        name: '功率',
        type: 'bar',
        data: devices.value.map(d => getDevicePower(d.id)),
        itemStyle: {
          color: function(params: any) {
            const power = params.value
            if (power > 1000) return '#ff4d4f'
            if (power > 500) return '#faad14'
            return '#00d4ff'
          }
        },
        barWidth: '60%'
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    }
  }
  
  chartInstance.setOption(option)
}

const updateChart = () => {
  if (!chartInstance) {
    initChart()
    return
  }
  
  const option = {
    xAxis: {
      data: devices.value.map(d => d.name)
    },
    series: [{
      data: devices.value.map(d => getDevicePower(d.id)),
      itemStyle: {
        color: function(params: any) {
          const power = params.value
          if (power > 1000) return '#ff4d4f'
          if (power > 500) return '#faad14'
          return '#00d4ff'
        }
      }
    }]
  }
  
  chartInstance.setOption(option)
}

const pollingManager = createPollingManager(loadDevices, 10000)

onMounted(() => {
  loadDevices()
  initChart()
  
  pollingManager.start()
  
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
})

onUnmounted(() => {
  pollingManager.stop()
  chartInstance?.dispose()
})
</script>

<style scoped>
.live-view {
  padding: 24px;
}

.page-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h1 {
  color: #1a1a1a;
  margin: 0;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.chart-header {
  display: flex;
  gap: 48px;
  margin-bottom: 24px;
}

.device-info {
  width: 100%;
}

.device-status {
  margin: 16px 0;
}

.device-details {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 212, 255, 0.15);
}

.detail-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.detail-label {
  color: #1a1a1a;
  font-size: 13px;
}

.detail-value {
  color: #1a1a1a;
  font-size: 13px;
  font-weight: 500;
}

:deep(.ant-card) {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
}

:deep(.ant-card-head) {
  color: #1a1a1a;
  border-bottom-color: rgba(0, 212, 255, 0.15);
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}

:deep(.ant-card-head-extra) {
  color: #1a1a1a;
  font-size: 13px;
}

:deep(.ant-statistic-title) {
  color: #1a1a1a;
}

:deep(.ant-statistic-content) {
  color: #1a1a1a;
}
</style>
