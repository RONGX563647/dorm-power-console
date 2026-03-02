<template>
  <div class="log-management-view">
    <a-page-header
      title="日志管理"
      sub-title="查看系统日志和统计分析"
      :back-icon="false"
    />

    <div class="content-wrapper">
      <!-- 统计卡片 -->
      <a-row :gutter="16" class="stats-row">
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="总日志数"
              :value="statistics?.total || 0"
              :value-style="{ color: '#3f8600' }"
            >
              <template #prefix>
                <FileTextOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="错误日志"
              :value="statistics?.byLevel?.ERROR || 0"
              :value-style="{ color: '#cf1322' }"
            >
              <template #prefix>
                <CloseCircleOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="警告日志"
              :value="statistics?.byLevel?.WARN || 0"
              :value-style="{ color: '#faad14' }"
            >
              <template #prefix>
                <WarningOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="信息日志"
              :value="statistics?.byLevel?.INFO || 0"
              :value-style="{ color: '#1890ff' }"
            >
              <template #prefix>
                <InfoCircleOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>

      <!-- 日志列表 -->
      <a-card class="log-list-card" title="日志列表">
        <template #extra>
          <a-space>
            <a-select
              v-model:value="filterLevel"
              style="width: 120px"
              placeholder="日志级别"
              allow-clear
            >
              <a-select-option value="DEBUG">DEBUG</a-select-option>
              <a-select-option value="INFO">INFO</a-select-option>
              <a-select-option value="WARN">WARN</a-select-option>
              <a-select-option value="ERROR">ERROR</a-select-option>
            </a-select>
            <a-button @click="handleRefresh">
              <ReloadOutlined />
              刷新
            </a-button>
          </a-space>
        </template>

        <a-table
          :columns="columns"
          :data-source="logs"
          :loading="loading"
          :pagination="pagination"
          @change="handleTableChange"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'level'">
              <a-tag :color="getLevelColor(record.level)">
                {{ record.level }}
              </a-tag>
            </template>
            <template v-if="column.key === 'timestamp'">
              {{ formatDate(record.timestamp) }}
            </template>
            <template v-if="column.key === 'message'">
              <div class="message-cell">{{ record.message }}</div>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  FileTextOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  InfoCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import { logApi } from '@/api'
import type { LogEntry, LogStatistics } from '@/types'

const loading = ref(false)
const logs = ref<LogEntry[]>([])
const statistics = ref<LogStatistics | null>(null)
const filterLevel = ref<string>()
const pagination = ref({
  current: 1,
  pageSize: 20,
  total: 0
})

const columns = [
  {
    title: '时间',
    dataIndex: 'timestamp',
    key: 'timestamp',
    width: 180
  },
  {
    title: '级别',
    dataIndex: 'level',
    key: 'level',
    width: 100
  },
  {
    title: '来源',
    dataIndex: 'source',
    key: 'source',
    width: 150
  },
  {
    title: '消息',
    dataIndex: 'message',
    key: 'message',
    ellipsis: true
  }
]

const getLevelColor = (level: string) => {
  const colors: Record<string, string> = {
    DEBUG: 'default',
    INFO: 'blue',
    WARN: 'orange',
    ERROR: 'red'
  }
  return colors[level] || 'default'
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString()
}

const loadLogs = async () => {
  loading.value = true
  try {
    const result = await logApi.getAllLogs({
      page: pagination.value.current - 1,
      size: pagination.value.pageSize,
      level: filterLevel.value
    })
    logs.value = result.content
    pagination.value.total = result.totalElements
  } catch (error) {
    message.error('加载日志失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    statistics.value = await logApi.getLogStatistics(7)
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

const handleTableChange = (pag: any) => {
  pagination.value.current = pag.current
  pagination.value.pageSize = pag.pageSize
  loadLogs()
}

const handleRefresh = () => {
  loadLogs()
  loadStatistics()
}

watch(filterLevel, () => {
  pagination.value.current = 1
  loadLogs()
})

onMounted(() => {
  loadLogs()
  loadStatistics()
})
</script>

<style scoped lang="scss">
.log-management-view {
  padding: 24px;

  .stats-row {
    margin-bottom: 24px;
  }

  .log-list-card {
    .message-cell {
      max-width: 500px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}
</style>
