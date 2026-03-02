<template>
  <div class="history-view">
    <div class="page-header">
      <h1>历史数据</h1>
    </div>

    <a-card>
      <div class="filter-bar">
        <a-space wrap>
          <a-select
            v-model:value="formState.device"
            style="width: 200px"
            :options="deviceOptions"
            placeholder="选择设备"
          />
          <a-select
            v-model:value="formState.range"
            style="width: 150px"
            :options="rangeOptions"
            placeholder="选择时间范围"
          />
          <a-button type="primary" @click="loadHistory">
            查询
          </a-button>
          <a-button @click="exportCsv">
            <template #icon><DownloadOutlined /></template>
            导出数据
          </a-button>
        </a-space>
      </div>

      <div class="stats-card" v-if="telemetry.length > 0">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-statistic
              title="平均功率"
              :value="averagePower"
              :precision="1"
              suffix="W"
              :value-style="{ color: '#00d4ff' }"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic
              title="最大功率"
              :value="maxPower"
              :precision="1"
              suffix="W"
              :value-style="{ color: '#ff4d4f' }"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic
              title="最小功率"
              :value="minPower"
              :precision="1"
              suffix="W"
              :value-style="{ color: '#52c41a' }"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic
              title="总能耗"
              :value="totalEnergy"
              :precision="2"
              suffix="kWh"
              :value-style="{ color: '#faad14' }"
            />
          </a-col>
        </a-row>
      </div>

      <div ref="chartRef" class="chart-container" style="margin-top: 24px"></div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import { deviceApi } from '@/api'
import type { Device, TelemetryPoint } from '@/types'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'

const formState = reactive({
  device: '',
  range: '60s'
})

const devices = ref<Device[]>([])
const telemetry = ref<TelemetryPoint[]>([])
const chartRef = ref<HTMLElement>()
let chartInstance: ECharts | null = null

const deviceOptions = ref<any[]>([])
const rangeOptions = [
  { label: '1分钟', value: '60s' },
  { label: '5分钟', value: '5m' },
  { label: '30分钟', value: '30m' },
  { label: '1小时', value: '1h' },
  { label: '24小时', value: '24h' },
  { label: '7天', value: '7d' },
  { label: '30天', value: '30d' }
]

// 统计数据计算
const averagePower = computed(() => {
  if (telemetry.value.length === 0) return 0
  const sum = telemetry.value.reduce((acc, p) => acc + p.power_w, 0)
  return sum / telemetry.value.length
})

const maxPower = computed(() => {
  if (telemetry.value.length === 0) return 0
  return Math.max(...telemetry.value.map(p => p.power_w))
})

const minPower = computed(() => {
  if (telemetry.value.length === 0) return 0
  return Math.min(...telemetry.value.map(p => p.power_w))
})

const totalEnergy = computed(() => {
  if (telemetry.value.length === 0) return 0
  // 简化计算：假设数据点均匀分布，计算总能耗（kWh）
  const sum = telemetry.value.reduce((acc, p) => acc + p.power_w, 0)
  const hours = getHoursFromRange(formState.range)
  return (sum / 1000) * hours
})

const getHoursFromRange = (range: string): number => {
  const match = range.match(/(\d+)([smhd])/)
  if (!match) return 1
  const [, value, unit] = match
  const numValue = parseInt(value)
  
  switch (unit) {
    case 's': return numValue / 3600
    case 'm': return numValue / 60
    case 'h': return numValue
    case 'd': return numValue * 24
    default: return 1
  }
}

const loadDevices = async () => {
  try {
    devices.value = await deviceApi.getDevices()
    deviceOptions.value = devices.value.map(d => ({
      label: `${d.name} (${d.room})`,
      value: d.id
    }))
    
    if (devices.value.length > 0) {
      formState.device = devices.value[0].id
      loadHistory()
    }
  } catch (error: any) {
    message.error('Failed to load devices')
  }
}

const loadHistory = async () => {
  if (!formState.device) return
  
  try {
    telemetry.value = await deviceApi.getTelemetry(formState.device, formState.range)
    initChart()
  } catch (error: any) {
    message.error('Failed to load history data')
  }
}

const initChart = () => {
  if (!chartRef.value) return
  
  if (chartInstance) {
    chartInstance.dispose()
  }
  
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
      data: telemetry.value.map(p => new Date(p.ts * 1000).toLocaleTimeString()),
      axisLabel: {
        color: '#8ba3c7'
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
        type: 'line',
        smooth: true,
        data: telemetry.value.map(p => p.power_w),
        itemStyle: {
          color: '#00d4ff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 212, 255, 0.3)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0)' }
          ])
        },
        markLine: {
          data: [
            {
              type: 'average',
              name: '平均值',
              label: {
                formatter: '平均值: {c} W'
              }
            }
          ]
        }
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    }
  }
  
  chartInstance.setOption(option)
}

const exportCsv = () => {
  if (telemetry.value.length === 0) {
    message.warning('没有数据可导出')
    return
  }
  
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
  link.setAttribute('download', `history-data-${formState.device}-${new Date().toISOString().split('T')[0]}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  message.success('数据导出成功')
}

onMounted(() => {
  loadDevices()
  
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
})
</script>

<style scoped>
.history-view {
  padding: 24px;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h1 {
  color: #1a1a1a;
  margin: 0;
}

.filter-bar {
  margin-bottom: 24px;
}

.stats-card {
  margin: 24px 0;
  padding: 16px;
  background: rgba(16, 24, 40, 0.4);
  border-radius: 8px;
  border: 1px solid rgba(0, 212, 255, 0.15);
}

.chart-container {
  height: 400px;
  width: 100%;
}

:deep(.ant-card) {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
}

:deep(.ant-card-head) {
  color: #1a1a1a;
  border-bottom-color: rgba(0, 212, 255, 0.15);
}

:deep(.ant-form-item-label > label) {
  color: #1a1a1a;
}

:deep(.ant-statistic-title) {
  color: #1a1a1a !important;
}
</style>
