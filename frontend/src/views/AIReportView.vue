<template>
  <div class="ai-report-view">
    <div class="page-header">
      <h1>AI 能耗报告</h1>
    </div>

    <a-row :gutter="16">
      <a-col :span="24">
        <a-card title="生成报告">
          <div class="filter-bar">
            <a-space wrap>
              <a-select
                v-model:value="formState.roomId"
                style="width: 200px"
                :options="roomOptions"
                placeholder="选择房间"
              />
              <a-select
                v-model:value="formState.range"
                style="width: 150px"
                :options="rangeOptions"
                placeholder="选择时间范围"
              />
              <a-button type="primary" @click="generateReport" :loading="loading">
                生成报告
              </a-button>
              <a-button v-if="report" @click="exportReport">
                <template #icon><DownloadOutlined /></template>
                导出报告
              </a-button>
            </a-space>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px" v-if="report">
      <a-col :span="24">
        <a-card title="分析报告">
          <div class="report-header">
            <div class="report-info">
              <span class="report-date">{{ generateDate }}</span>
              <span class="report-room">{{ formState.roomId }} 房间</span>
            </div>
            <a-tag color="blue">AI 生成</a-tag>
          </div>
          
          <a-typography class="report-content">
            <a-typography-title :level="4" class="section-title">摘要</a-typography-title>
            <a-typography-paragraph class="report-paragraph">
              {{ report.summary }}
            </a-typography-paragraph>
            
            <a-typography-title :level="4" class="section-title">建议</a-typography-title>
            <a-typography-paragraph class="report-paragraph">
              {{ report.recommendations }}
            </a-typography-paragraph>
            
            <a-typography-title :level="4" class="section-title">节能小贴士</a-typography-title>
            <a-list class="tips-list">
              <a-list-item v-for="(tip, index) in report.tips" :key="index">
                <a-list-item-meta>
                  <template #avatar>
                    <a-badge :count="index + 1" :style="{ backgroundColor: '#00d4ff' }" />
                  </template>
                  <template #title>
                    <span>{{ tip }}</span>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </a-list>
          </a-typography>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px" v-if="report">
      <a-col :span="12">
        <a-card title="能耗分析">
          <div ref="chartRef" class="chart-container"></div>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="节能潜力">
          <div class="potential-card">
            <div class="potential-item">
              <a-statistic
                title="预计节能"
                :value="energySaving"
                :precision="1"
                suffix="kWh"
                :value-style="{ color: '#00d4ff' }"
              />
            </div>
            <div class="potential-item">
              <a-statistic
                title="预计节省"
                :value="costSaving"
                :precision="2"
                suffix="元"
                :value-style="{ color: '#52c41a' }"
              />
            </div>
            <div class="potential-item">
              <a-statistic
                title="碳减排"
                :value="carbonReduction"
                :precision="2"
                suffix="kg"
                :value-style="{ color: '#faad14' }"
              />
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import { deviceApi } from '@/api'
import type { Device } from '@/types'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'

const formState = reactive({
  roomId: '',
  range: '7d'
})

const devices = ref<Device[]>([])
const roomOptions = ref<any[]>([])
const loading = ref(false)
const report = ref<any>(null)
const chartRef = ref<HTMLElement>()
let chartInstance: ECharts | null = null

const rangeOptions = [
  { label: '7天', value: '7d' },
  { label: '30天', value: '30d' },
  { label: '90天', value: '90d' }
]

const generateDate = computed(() => {
  return new Date().toLocaleDateString()
})

// 模拟节能数据
const energySaving = ref(12.5)
const costSaving = ref(8.75)
const carbonReduction = ref(9.3)

const loadDevices = async () => {
  try {
    devices.value = await deviceApi.getDevices()
    const rooms = [...new Set(devices.value.map(d => d.room))]
    roomOptions.value = rooms.map(room => ({
      label: room,
      value: room
    }))
    
    if (rooms.length > 0) {
      formState.roomId = rooms[0]
    }
  } catch (error: any) {
    message.error('Failed to load devices')
  }
}

const generateReport = async () => {
  if (!formState.roomId) {
    message.warning('请选择房间')
    return
  }
  
  try {
    loading.value = true
    // 模拟API调用
    // const response = await fetch(`/api/rooms/${formState.roomId}/ai_report`)
    // const data = await response.json()
    
    // 模拟数据
    report.value = {
      summary: `基于 ${formState.range === '7d' ? '7天' : formState.range === '30d' ? '30天' : '90天'} 的数据分析，${formState.roomId} 房间的能耗表现整体良好，但仍有优化空间。平均功率为 120W，高峰期主要集中在晚上 8-10 点。`,
      recommendations: '建议在非使用时间关闭不必要的设备，特别是在晚上 11 点后。考虑更换为节能型电器，如 LED 灯和节能空调。建议在用电高峰期（晚上 8-10 点）减少高功率设备的同时使用。',
      tips: [
        '离开房间时随手关闭灯光和电器',
        '使用节能型电器，如 LED 灯和节能空调',
        '避免在用电高峰期同时使用多个高功率设备',
        '定期检查设备待机功耗，拔掉不使用设备的插头',
        '合理设置空调温度，夏季不低于 26℃，冬季不高于 20℃'
      ]
    }
    
    message.success('报告生成成功')
    initChart()
  } catch (error: any) {
    message.error('报告生成失败')
    report.value = {
      summary: `AI 分析报告 - ${formState.roomId} 房间`,
      recommendations: '基于历史数据，建议在用电高峰期优化功率使用。',
      tips: [
        '关闭未使用的设备',
        '使用节能电器',
        '避免同时使用高功率设备'
      ]
    }
    initChart()
  } finally {
    loading.value = false
  }
}

const exportReport = () => {
  if (!report.value) return
  
  // 简单的文本导出实现
  const content = `
AI 能耗报告
日期: ${generateDate.value}
房间: ${formState.roomId}
时间范围: ${formState.range === '7d' ? '7天' : formState.range === '30d' ? '30天' : '90天'}

摘要:
${report.value.summary}

建议:
${report.value.recommendations}

节能小贴士:
${report.value.tips.map((tip: string, index: number) => `${index + 1}. ${tip}`).join('\n')}

预计节能: ${energySaving.value} kWh
预计节省: ${costSaving.value} 元
碳减排: ${carbonReduction.value} kg
`
  
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `ai-report-${formState.roomId}-${new Date().toISOString().split('T')[0]}.txt`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  message.success('报告导出成功')
}

const initChart = () => {
  if (!chartRef.value) return
  
  if (chartInstance) {
    chartInstance.dispose()
  }
  
  chartInstance = echarts.init(chartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLabel: {
        color: '#8ba3c7'
      }
    },
    yAxis: {
      type: 'value',
      name: '能耗 (kWh)',
      nameTextStyle: {
        color: '#8ba3c7'
      },
      axisLabel: {
        color: '#8ba3c7'
      }
    },
    series: [
      {
        name: '实际能耗',
        type: 'bar',
        data: [5.2, 4.8, 5.5, 6.1, 7.2, 3.5, 3.8],
        itemStyle: {
          color: '#00d4ff'
        }
      },
      {
        name: '预测能耗',
        type: 'line',
        data: [4.5, 4.2, 4.8, 5.3, 6.0, 3.0, 3.2],
        itemStyle: {
          color: '#52c41a'
        },
        smooth: true
      }
    ],
    legend: {
      data: ['实际能耗', '预测能耗'],
      textStyle: {
        color: '#8ba3c7'
      }
    }
  }
  
  chartInstance.setOption(option)
}

onMounted(() => {
  loadDevices()
  
  window.addEventListener('resize', () => {
    chartInstance?.resize()
  })
})
</script>

<style scoped>
.ai-report-view {
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
  margin-bottom: 16px;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 212, 255, 0.15);
}

.report-info {
  display: flex;
  gap: 24px;
}

.report-date,
.report-room {
  color: #1a1a1a;
  font-size: 14px;
}

.report-content {
  line-height: 1.6;
}

.section-title {
  margin-bottom: 16px;
  color: #00d4ff !important;
}

.report-paragraph {
  margin-bottom: 24px;
  color: #1a1a1a;
}

.tips-list {
  margin-top: 16px;
}

:deep(.ant-list-item) {
  background: rgba(16, 24, 40, 0.4);
  border-radius: 8px;
  margin-bottom: 8px;
  padding: 12px;
}

:deep(.ant-list-item-meta-title) {
  color: #1a1a1a;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.potential-card {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 16px 0;
}

.potential-item {
  text-align: center;
}

:deep(.ant-card) {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
}

:deep(.ant-card-head) {
  color: #1a1a1a;
  border-bottom-color: rgba(0, 212, 255, 0.15);
}

:deep(.ant-typography) {
  color: #1a1a1a;
}

:deep(.ant-typography-title) {
  color: #1a1a1a !important;
}

:deep(.ant-statistic-title) {
  color: #1a1a1a !important;
}
</style>
