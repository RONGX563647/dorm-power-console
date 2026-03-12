<template>
  <div class="telemetry-view">
    <a-page-header
      title="遥测数据"
      sub-title="设备遥测数据查询与导出"
      style="padding: 0 0 16px 0"
    />

    <a-card>
      <a-space style="margin-bottom: 16px">
        <a-select
          v-model:value="selectedDevice"
          placeholder="选择设备"
          style="width: 200px"
        >
          <a-select-option v-for="device in devices" :key="device.id" :value="device.id">
            {{ device.name }}
          </a-select-option>
        </a-select>
        <a-select v-model:value="timeRange" placeholder="时间范围" style="width: 150px">
          <a-select-option value="60s">最近1分钟</a-select-option>
          <a-select-option value="24h">最近24小时</a-select-option>
          <a-select-option value="7d">最近7天</a-select-option>
          <a-select-option value="30d">最近30天</a-select-option>
        </a-select>
        <a-button type="primary" @click="loadTelemetry" :loading="loading">
          查询
        </a-button>
        <a-button @click="showExportModal">
          导出数据
        </a-button>
      </a-space>

      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="8">
          <a-card size="small">
            <a-statistic
              title="平均功率"
              :value="statistics.averagePowerW || 0"
              suffix="W"
              :precision="2"
            />
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card size="small">
            <a-statistic
              title="最大功率"
              :value="statistics.maxPowerW || 0"
              suffix="W"
              :precision="2"
            />
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card size="small">
            <a-statistic
              title="最小功率"
              :value="statistics.minPowerW || 0"
              suffix="W"
              :precision="2"
            />
          </a-card>
        </a-col>
      </a-row>

      <a-table
        :data-source="telemetryData"
        :columns="telemetryColumns"
        :loading="loading"
        :pagination="{ pageSize: 20, showSizeChanger: true, pageSizeOptions: ['10', '20', '50'] }"
        :scroll="{ x: 1000 }"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ts'">
            {{ new Date(record.ts * 1000).toLocaleString() }}
          </template>
          <template v-if="column.key === 'power_w'">
            <span :style="{ color: record.power_w > 1000 ? 'red' : 'green' }">
              {{ record.power_w.toFixed(2) }}
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="exportModalVisible"
      title="导出遥测数据"
      @ok="handleExport"
      :confirm-loading="exporting"
    >
      <a-form :model="exportForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="设备">
          <a-input :value="selectedDeviceName" disabled />
        </a-form-item>
        <a-form-item label="开始时间" required>
          <a-date-picker
            v-model:value="exportForm.startTime"
            show-time
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="结束时间" required>
          <a-date-picker
            v-model:value="exportForm.endTime"
            show-time
            placeholder="选择结束时间"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="导出格式">
          <a-select v-model:value="exportForm.format">
            <a-select-option value="csv">CSV</a-select-option>
            <a-select-option value="xlsx">Excel</a-select-option>
            <a-select-option value="json">JSON</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { telemetryApi, deviceApi } from '@/api'
import type { Dayjs } from 'dayjs'

const loading = ref(false)
const exporting = ref(false)

const devices = ref<any[]>([])
const selectedDevice = ref<string>()
const timeRange = ref('24h')
const telemetryData = ref<any[]>([])
const statistics = ref<any>({})

const exportModalVisible = ref(false)
const exportForm = ref({
  startTime: null as Dayjs | null,
  endTime: null as Dayjs | null,
  format: 'csv'
})

const telemetryColumns = [
  { title: '时间戳', dataIndex: 'ts', key: 'ts', width: 180 },
  { title: '功率(W)', dataIndex: 'power_w', key: 'power_w', width: 120 },
  { title: '电压(V)', dataIndex: 'voltage_v', key: 'voltage_v', width: 120 },
  { title: '电流(A)', dataIndex: 'current_a', key: 'current_a', width: 120 },
  { title: '频率(Hz)', dataIndex: 'frequency', key: 'frequency', width: 120 },
  { title: '功率因数', dataIndex: 'power_factor', key: 'power_factor', width: 120 },
  { title: '温度(℃)', dataIndex: 'temperature', key: 'temperature', width: 120 }
]

const selectedDeviceName = computed(() => {
  const device = devices.value.find(d => d.id === selectedDevice.value)
  return device?.name || ''
})

onMounted(async () => {
  await loadDevices()
})

const loadDevices = async () => {
  try {
    const data = await deviceApi.getDevices()
    devices.value = Array.isArray(data) ? data : []
    
    if (devices.value.length > 0) {
      selectedDevice.value = devices.value[0].id
      await loadTelemetry()
    }
  } catch (error) {
    message.error('加载设备失败')
    console.error(error)
  }
}

const loadTelemetry = async () => {
  if (!selectedDevice.value) {
    message.warning('请选择设备')
    return
  }

  loading.value = true
  try {
    const [telemetryDataResult, statsData] = await Promise.all([
      telemetryApi.getTelemetry(selectedDevice.value, timeRange.value),
      telemetryApi.getStatistics(selectedDevice.value)
    ])
    
    telemetryData.value = Array.isArray(telemetryDataResult) ? telemetryDataResult : []
    statistics.value = statsData || {}
  } catch (error) {
    message.error('加载遥测数据失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const showExportModal = () => {
  if (!selectedDevice.value) {
    message.warning('请先选择设备')
    return
  }

  exportForm.value = {
    startTime: null,
    endTime: null,
    format: 'csv'
  }
  exportModalVisible.value = true
}

const handleExport = async () => {
  if (!exportForm.value.startTime || !exportForm.value.endTime) {
    message.warning('请选择时间范围')
    return
  }

  exporting.value = true
  try {
    const blob = await telemetryApi.exportTelemetry(
      selectedDevice.value!,
      exportForm.value.startTime.toISOString(),
      exportForm.value.endTime.toISOString(),
      exportForm.value.format
    )
    
    const url = window.URL.createObjectURL(blob as any)
    const link = document.createElement('a')
    link.href = url
    link.download = `telemetry_${selectedDeviceName.value}_${Date.now()}.${exportForm.value.format}`
    link.click()
    window.URL.revokeObjectURL(url)
    
    message.success('导出成功')
    exportModalVisible.value = false
  } catch (error) {
    message.error('导出失败')
    console.error(error)
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.telemetry-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>