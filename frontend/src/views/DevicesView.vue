<template>
  <div class="devices-view">
    <div class="page-header">
      <h1>设备管理</h1>
      <a-button type="primary" @click="loadDevices">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <a-card class="stats-card">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-statistic
            title="总设备"
            :value="devices.length"
            :value-style="{ color: 'var(--color-text-primary)', fontWeight: 700, fontSize: '28px' }"
          >
            <template #title>
              <span style="color: var(--color-text-secondary)">总设备</span>
            </template>
          </a-statistic>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-statistic
            title="在线"
            :value="onlineCount"
            :value-style="{ color: 'var(--color-success)', fontWeight: 700, fontSize: '28px' }"
          >
            <template #title>
              <span style="color: var(--color-text-secondary)">在线</span>
            </template>
          </a-statistic>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-statistic
            title="离线"
            :value="offlineCount"
            :value-style="{ color: 'var(--color-text-tertiary)', fontWeight: 700, fontSize: '28px' }"
          >
            <template #title>
              <span style="color: var(--color-text-secondary)">离线</span>
            </template>
          </a-statistic>
        </a-col>
      </a-row>
    </a-card>

    <div class="cyber-table-container">
      <a-table
        class="cyber-table"
        :columns="columns"
        :data-source="filteredDevices"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
        :row-key="(record: Device) => record.id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <router-link :to="`/devices/${record.id}`" class="device-link">
              <DesktopOutlined style="margin-right: 8px; color: var(--color-primary)" />
              {{ record.name }}
            </router-link>
          </template>
          <template v-if="column.key === 'id'">
            <span class="device-id">{{ record.id }}</span>
          </template>
          <template v-if="column.key === 'room'">
            <a-tag color="blue">{{ record.room }}</a-tag>
          </template>
          <template v-if="column.key === 'online'">
            <a-tag :color="record.online ? 'success' : 'default'" class="status-tag">
              <template #icon>
                <WifiOutlined v-if="record.online" />
                <DisconnectOutlined v-else />
              </template>
              {{ record.online ? '在线' : '离线' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'power'">
            <span :class="['power-value', { online: record.online }]">
              <ThunderboltOutlined v-if="record.online" style="margin-right: 4px" />
              {{ record.online ? (powerMap[record.id] ?? 0).toFixed(1) : '--' }} W
            </span>
          </template>
          <template v-if="column.key === 'firmware'">
            <span class="firmware-version">v1.0.3</span>
          </template>
          <template v-if="column.key === 'lastSeen'">
            <span class="last-seen">{{ formatLastSeen(record.lastSeen) }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <router-link :to="`/devices/${record.id}`">
              <a-button type="link" size="small">查看详情</a-button>
            </router-link>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import {
  ReloadOutlined,
  DesktopOutlined,
  WifiOutlined,
  DisconnectOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import { deviceApi } from '@/api'
import { deviceStatusService, createPollingManager } from '@/services/deviceStatusService'
import type { Device, StripStatus } from '@/types'

const loading = ref(false)
const devices = ref<Device[]>([])
const powerMap = ref<Record<string, number>>({})
const keyword = ref('')
const statusFilter = ref<'all' | 'online' | 'offline'>('all')
const roomFilter = ref('all')

const onlineCount = computed(() => devices.value.filter(d => d.online).length)
const offlineCount = computed(() => devices.value.filter(d => !d.online).length)

const roomOptions = computed(() => {
  const rooms = Array.from(new Set(devices.value.map(d => d.room)))
  return [{ value: 'all', label: '全部房间' }, ...rooms.map(r => ({ value: r, label: r }))]
})

const filteredDevices = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return devices.value
    .filter(d => {
      if (k) {
        return d.name.toLowerCase().includes(k) || 
               d.room.toLowerCase().includes(k) || 
               d.id.toLowerCase().includes(k)
      }
      return true
    })
    .filter(d => {
      if (statusFilter.value === 'all') return true
      if (statusFilter.value === 'online') return d.online
      return !d.online
    })
    .filter(d => {
      if (roomFilter.value === 'all') return true
      return d.room === roomFilter.value
    })
    .sort((a, b) => new Date(b.lastSeen).getTime() - new Date(a.lastSeen).getTime())
})

const columns: TableProps['columns'] = [
  { title: '设备名称', dataIndex: 'name', key: 'name' },
  { title: '设备 ID', dataIndex: 'id', key: 'id' },
  { title: '房间', dataIndex: 'room', key: 'room' },
  { title: '在线状态', key: 'online' },
  { title: '当前功率', key: 'power' },
  { title: '固件版本', key: 'firmware' },
  { 
    title: '最近上报', 
    dataIndex: 'lastSeen', 
    key: 'lastSeen',
    sorter: (a: Device, b: Device) => new Date(a.lastSeen).getTime() - new Date(b.lastSeen).getTime()
  },
  { title: '操作', key: 'action' }
]

const loadDevices = async () => {
  try {
    loading.value = true
    const devs = await deviceApi.getDevices()
    devices.value = devs
    
    const statusMap = await deviceStatusService.getBatchDeviceStatus(devs.map(d => d.id))
    
    const powerPairs = Object.entries(statusMap).map(([id, status]) => {
      return [id, status?.total_power_w ?? 0] as const
    })
    
    powerMap.value = Object.fromEntries(powerPairs)
  } catch (error: any) {
    message.error('加载设备失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
}

const handleFilterChange = () => {
}

const formatLastSeen = (dateString: string) => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (minutes < 1440) return `${Math.floor(minutes / 60)}小时前`
  return date.toLocaleDateString()
}

const pollingManager = createPollingManager(loadDevices, 10000)

onMounted(() => {
  loadDevices()
  pollingManager.start()
})

onUnmounted(() => {
  pollingManager.stop()
})
</script>

<style scoped>
.devices-view {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.page-header h1 {
  color: var(--color-primary);
  margin: 0;
  font-size: 28px;
  font-weight: 900;
  font-family: 'Orbitron', sans-serif;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.stats-card {
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  border: var(--card-border);
  border-radius: var(--card-radius);
  box-shadow: var(--card-shadow);
  margin-bottom: var(--spacing-lg);
  transition: all 0.4s ease;
}

.stats-card:hover {
  transform: translateY(-4px);
  border-color: var(--color-primary);
  box-shadow: var(--shadow-lg);
}

.stats-card :deep(.ant-statistic-title) {
  color: var(--color-text-secondary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.stats-card :deep(.ant-statistic-content) {
  color: var(--color-text-primary);
  font-family: 'Orbitron', sans-serif;
  font-weight: 900;
}

.device-link {
  color: var(--color-primary);
  font-weight: 600;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  transition: all 0.3s ease;
  font-family: 'JetBrains Mono', monospace;
}

.device-link:hover {
  color: var(--color-secondary);
  text-decoration: underline;
  text-shadow: 0 0 10px var(--color-secondary-bg);
}

.device-id {
  color: var(--color-text-secondary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  background: var(--color-primary-bg);
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid var(--color-primary-border);
}

.status-tag {
  border-radius: 6px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.power-value {
  color: var(--color-text-secondary);
  font-family: 'JetBrains Mono', monospace;
  font-weight: 500;
}

.power-value.online {
  color: var(--color-primary);
  font-weight: 700;
  text-shadow: 0 0 10px var(--color-primary-bg);
}

.firmware-version {
  color: var(--color-text-secondary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  background: var(--color-primary-bg);
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid var(--color-primary-border);
}

.last-seen {
  color: var(--color-text-secondary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
}

:deep(.ant-card-head) {
  color: var(--color-primary);
  border-bottom: 1px solid var(--color-border);
  font-family: 'Orbitron', sans-serif;
  font-weight: 700;
}

:deep(.ant-table) {
  color: var(--color-text-primary);
}

:deep(.ant-tag) {
  border-radius: 6px;
  font-family: 'JetBrains Mono', monospace;
}

:deep(.ant-input-search) {
  border-radius: var(--input-radius);
}

:deep(.ant-input) {
  background: var(--color-bg-container);
  border: var(--input-border);
  color: var(--color-text-primary);
  font-family: 'JetBrains Mono', monospace;
}

:deep(.ant-input::placeholder) {
  color: var(--color-text-tertiary);
}

:deep(.ant-select-selector) {
  background: var(--color-bg-container) !important;
  border: var(--input-border) !important;
  border-radius: var(--input-radius) !important;
  color: var(--color-text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

:deep(.ant-select-selection-item) {
  color: var(--color-text-primary);
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  border: none;
  box-shadow: 0 4px 15px var(--color-primary-bg);
  font-family: 'Orbitron', sans-serif;
  font-weight: 600;
  letter-spacing: 1px;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, var(--color-primary-light) 0%, var(--color-primary) 100%);
  box-shadow: 0 8px 25px var(--color-primary-bg);
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-sm);
  }
  
  .page-header h1 {
    font-size: 20px;
  }
}
</style>
