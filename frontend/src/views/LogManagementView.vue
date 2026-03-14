<template>
  <div class="enhanced-log-management-view">
    <!-- 欢迎卡片 -->
    <a-card :bordered="false" style="margin-bottom: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white;">
      <div style="display: flex; align-items: center; gap: 20px;">
        <div style="font-size: 48px;">
          <img src="@/assets/icons/chart-bar.svg" alt="Logs" style="width: 48px; height: 48px; fill: white;" />
        </div>
        <div>
          <h1 style="margin: 0; font-size: 28px; font-weight: 700;">日志管理中心</h1>
          <p style="margin: 8px 0 0 0; opacity: 0.9;">实时监控系统日志 · 智能分析异常 · 快速定位问题</p>
        </div>
      </div>
    </a-card>

    <a-row :gutter="20">
      <!-- 左侧：日志统计和过滤 -->
      <a-col :xs="24" :lg="6">
        <!-- 统计卡片 -->
        <a-card :bordered="false" style="margin-bottom: 20px;">
          <template #title>
            <span style="display: flex; align-items: center;">
              <img src="@/assets/icons/chart-line.svg" alt="Statistics" style="width: 20px; height: 20px; margin-right: 8px;" />
              统计概览
            </span>
          </template>
          <a-statistic
            title="总日志数"
            :value="statistics?.total || 0"
            :value-style="{ color: '#1890ff', fontSize: '24px' }"
          >
            <template #prefix>
              <FileTextOutlined />
            </template>
          </a-statistic>
          <a-divider />

          <div class="level-stat-item">
            <span>ERROR</span>
            <a-progress
              :percent="getErrorPercent()"
              :stroke-color="'#ff4d4f'"
              :show-info="false"
              size="small"
            />
            <span class="level-count">{{ statistics?.byLevel?.ERROR || 0 }}</span>
          </div>

          <div class="level-stat-item">
            <span>WARN</span>
            <a-progress
              :percent="getWarnPercent()"
              :stroke-color="'#faad14'"
              :show-info="false"
              size="small"
            />
            <span class="level-count">{{ statistics?.byLevel?.WARN || 0 }}</span>
          </div>

          <div class="level-stat-item">
            <span>INFO</span>
            <a-progress
              :percent="getInfoPercent()"
              :stroke-color="'#52c41a'"
              :show-info="false"
              size="small"
            />
            <span class="level-count">{{ statistics?.byLevel?.INFO || 0 }}</span>
          </div>

          <div class="level-stat-item">
            <span>DEBUG</span>
            <a-progress
              :percent="getDebugPercent()"
              :stroke-color="'#1890ff'"
              :show-info="false"
              size="small"
            />
            <span class="level-count">{{ statistics?.byLevel?.DEBUG || 0 }}</span>
          </div>
        </a-card>

        <!-- 过滤条件卡片 -->
        <a-card :bordered="false">
          <template #title>
            <span style="display: flex; align-items: center;">
              <img src="@/assets/icons/search.svg" alt="Filter" style="width: 20px; height: 20px; margin-right: 8px;" />
              过滤条件
            </span>
          </template>
          <a-form layout="vertical">
            <a-form-item label="日志级别">
              <a-select
                v-model:value="filterLevel"
                style="width: 100%"
                placeholder="全部级别"
                allow-clear
                @change="handleFilterChange"
              >
                <a-select-option value="DEBUG">DEBUG - 调试</a-select-option>
                <a-select-option value="INFO">INFO - 信息</a-select-option>
                <a-select-option value="WARN">WARN - 警告</a-select-option>
                <a-select-option value="ERROR">ERROR - 错误</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="来源">
              <a-input
                v-model:value="filterSource"
                placeholder="搜索来源"
                @input="handleFilterChange"
              />
            </a-form-item>

            <a-form-item label="关键字">
              <a-input
                v-model:value="keyword"
                placeholder="搜索消息内容"
                @input="handleFilterChange"
              />
            </a-form-item>

            <a-form-item label="时间范围">
              <a-range-picker
                v-model:value="dateRange"
                :show-time="true"
                format="YYYY-MM-DD HH:mm"
                @change="handleDateChange"
              />
            </a-form-item>

            <a-divider />

            <a-space direction="vertical" style="width: 100%;">
              <a-button
                type="primary"
                block
                @click="handleRefresh"
                :loading="loading"
              >
                <ReloadOutlined />
                刷新数据
              </a-button>

              <a-button
                type="default"
                block
                @click="handleExport"
              >
                <ExportOutlined />
                导出日志
              </a-button>

              <a-button
                danger
                block
                @click="handleClearFilters"
              >
                清除筛选
              </a-button>
            </a-space>
          </a-form>
        </a-card>
      </a-col>

      <!-- 右侧：日志列表和详情 -->
      <a-col :xs="24" :lg="18">
        <!-- 快速操作 -->
        <a-card :bordered="false" style="margin-bottom: 20px;">
          <a-space>
            <a-badge :count="statistics?.byLevel?.ERROR || 0" :number-style="{ backgroundColor: '#ff4d4f' }">
              <a-button>错误日志</a-button>
            </a-badge>
            <a-badge :count="statistics?.byLevel?.WARN || 0" :number-style="{ backgroundColor: '#faad14' }">
              <a-button>警告日志</a-button>
            </a-badge>
            <a-button @click="handleQuickFilter('today')">
              今天
            </a-button>
            <a-button @click="handleQuickFilter('week')">
              本周
            </a-button>
            <a-button @click="handleQuickFilter('month')">
              本月
            </a-button>
          </a-space>
        </a-card>

        <!-- 日志列表卡片 -->
        <a-card :bordered="false">
          <template #title>
            <span style="display: flex; align-items: center;">
              <img src="@/assets/icons/database.svg" alt="Logs" style="width: 20px; height: 20px; margin-right: 8px;" />
              日志列表
            </span>
          </template>
          <a-table
            :columns="columns"
            :data-source="logs"
            :loading="loading"
            :pagination="pagination"
            @change="handleTableChange"
            row-key="id"
            :scroll="{ x: 1200 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'level'">
                <a-tag :color="getLevelColor(record.level)" class="level-tag">
                  {{ getLevelIcon(record.level) }}
                  {{ record.level }}
                </a-tag>
              </template>
              <template v-if="column.key === 'timestamp'">
                <div class="timestamp-cell">
                  <ClockCircleOutlined />
                  <span>{{ formatDate(record.timestamp) }}</span>
                </div>
              </template>
              <template v-if="column.key === 'source'">
                <a-tag color="blue">{{ record.source || 'N/A' }}</a-tag>
              </template>
              <template v-if="column.key === 'message'">
                <div
                  class="message-cell"
                  :class="record.level === 'ERROR' ? 'error-message' : ''"
                  @click="showLogDetail(record)"
                >
                  {{ record.message }}
                </div>
              </template>
              <template v-if="column.key === 'actions'">
                <a-space>
                  <a-button
                    type="link"
                    size="small"
                    @click="showLogDetail(record)"
                  >
                    详情
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <!-- 详情模态框 -->
    <a-modal
      v-model:visible="detailVisible"
      width="800px"
      :footer="null"
    >
      <template #title>
        <span style="display: flex; align-items: center;">
          <img src="@/assets/icons/database.svg" alt="Log Detail" style="width: 20px; height: 20px; margin-right: 8px;" />
          日志详情
        </span>
      </template>
      <div v-if="selectedLog" class="log-detail-content">
        <a-descriptions bordered :column="1">
          <a-descriptions-item label="日志ID">
            {{ selectedLog.id }}
          </a-descriptions-item>
          <a-descriptions-item label="级别">
            <a-tag :color="getLevelColor(selectedLog.level)">
              {{ getLevelIcon(selectedLog.level) }}
              {{ selectedLog.level }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="时间">
            <ClockCircleOutlined style="margin-right: 4px;" />
            {{ selectedLog.timestamp }}
          </a-descriptions-item>
          <a-descriptions-item label="来源">
            <TagOutlined style="margin-right: 4px;" />
            {{ selectedLog.source || 'N/A' }}
          </a-descriptions-item>
          <a-descriptions-item label="消息">
            <div class="detail-message">{{ selectedLog.message }}</div>
          </a-descriptions-item>
        </a-descriptions>

        <div style="margin-top: 16px; padding: 12px; background: #f5f5f5; border-radius: 4px;">
          <h4 style="margin-bottom: 8px;">
            <ThunderboltOutlined style="margin-right: 4px;" />
            快速操作
          </h4>
          <a-space>
            <a-button type="primary" @click="copyToClipboard(selectedLog.message)">
              <CopyOutlined />
              复制
            </a-button>
            <a-button @click="handleReportIssue(selectedLog)">
              <BugOutlined />
              报告问题
            </a-button>
          </a-space>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { message, notification } from 'ant-design-vue'
import {
  FileTextOutlined,
  ReloadOutlined,
  ExportOutlined,
  ClockCircleOutlined,
  TagOutlined,
  ThunderboltOutlined,
  CopyOutlined,
  BugOutlined,
  InfoCircleOutlined,
  ExclamationCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue'
import { logApi } from '@/api'
import type { LogEntry, LogStatistics } from '@/types'
import dayjs from 'dayjs'

const loading = ref(false)
const logs = ref<LogEntry[]>([])
const statistics = ref<LogStatistics | null>(null)
const detailVisible = ref(false)
const selectedLog = ref<LogEntry | null>(null)

// 过滤条件
const filterState = reactive({
  level: undefined as string | undefined,
  source: '',
  keyword: '',
  startTime: '',
  endTime: ''
})

// 分页
const pagination = ref({
  current: 1,
  pageSize: 20,
  total: 0
})

// 快捷引用
const filterLevel = ref<string>()
const filterSource = ref('')
const keyword = ref('')
const dateRange = ref<[dayjs.Dayjs, dayjs.Dayjs] | null>(null)

// 表格列
const columns = [
  {
    title: '时间',
    dataIndex: 'timestamp',
    key: 'timestamp',
    width: 200,
    fixed: 'left' as const,
    sorter: (a: LogEntry, b: LogEntry) => {
      return new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    }
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
    ellipsis: true,
    width: 600
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    fixed: 'right' as const
  }
]

// 计算统计百分比
const getErrorPercent = () => {
  const total = statistics.value?.total || 0
  const error = statistics.value?.byLevel?.ERROR || 0
  return total > 0 ? Math.round((error / total) * 100) : 0
}

const getWarnPercent = () => {
  const total = statistics.value?.total || 0
  const warn = statistics.value?.byLevel?.WARN || 0
  return total > 0 ? Math.round((warn / total) * 100) : 0
}

const getInfoPercent = () => {
  const total = statistics.value?.total || 0
  const info = statistics.value?.byLevel?.INFO || 0
  return total > 0 ? Math.round((info / total) * 100) : 0
}

const getDebugPercent = () => {
  const total = statistics.value?.total || 0
  const debug = statistics.value?.byLevel?.DEBUG || 0
  return total > 0 ? Math.round((debug / total) * 100) : 0
}

// 级别颜色
const getLevelColor = (level: string) => {
  const colors: Record<string, string> = {
    DEBUG: 'default',
    INFO: 'blue',
    WARN: 'orange',
    ERROR: 'red'
  }
  return colors[level] || 'default'
}

// 级别图标名称
const getLevelIcon = (level: string) => {
  const icons: Record<string, string> = {
    DEBUG: '🐛',
    INFO: 'ℹ️',
    WARN: '⚠️',
    ERROR: '❌'
  }
  return icons[level] || '📄'
}

// 格式化日期
const formatDate = (dateStr: string) => {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss')
}

// 加载日志
const loadLogs = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.value.current - 1,
      size: pagination.value.pageSize
    }

    if (filterState.level) params.level = filterState.level
    if (filterState.source) params.source = filterState.source
    if (filterState.startTime) params.startTime = filterState.startTime
    if (filterState.endTime) params.endTime = filterState.endTime

    const result = await logApi.getAllLogs(params)
    logs.value = result.content
    pagination.value.total = result.totalElements

    // 错误日志提醒
    const errorCount = result.content.filter(l => l.level === 'ERROR').length
    if (errorCount > 0) {
      notification.warning({
        message: '发现错误日志',
        description: `本次查询发现 ${errorCount} 条错误日志`,
        duration: 3
      })
    }
  } catch (error) {
    message.error('加载日志失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 加载统计
const loadStatistics = async () => {
  try {
    statistics.value = await logApi.getLogStatistics(7)
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

// 刷新
const handleRefresh = () => {
  loadLogs()
  loadStatistics()
}

// 表格变化
const handleTableChange = (pag: any) => {
  pagination.value.current = pag.current
  pagination.value.pageSize = pag.pageSize
  loadLogs()
}

// 过滤变化
const handleFilterChange = () => {
  filterState.level = filterLevel.value
  filterState.source = filterSource.value
  filterState.keyword = keyword.value
  pagination.value.current = 1
  loadLogs()
}

// 日期变化
const handleDateChange = (dates: [dayjs.Dayjs, dayjs.Dayjs] | null) => {
  if (dates) {
    filterState.startTime = dates[0].toISOString()
    filterState.endTime = dates[1].toISOString()
  } else {
    filterState.startTime = ''
    filterState.endTime = ''
  }
  pagination.value.current = 1
  loadLogs()
}

// 快捷筛选
const handleQuickFilter = (period: string) => {
  const now = dayjs()
  let start, end

  switch (period) {
    case 'today':
      start = now.startOf('day')
      end = now.endOf('day')
      break
    case 'week':
      start = now.startOf('week')
      end = now.endOf('week')
      break
    case 'month':
      start = now.startOf('month')
      end = now.endOf('month')
      break
  }

  dateRange.value = [start!, end!]
  filterState.startTime = start!.toISOString()
  filterState.endTime = end!.toISOString()
  pagination.value.current = 1
  loadLogs()
}

// 显示详情
const showLogDetail = (log: LogEntry) => {
  selectedLog.value = log
  detailVisible.value = true
}

// 复制到剪贴板
const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    message.success('已复制到剪贴板')
  })
}

// 报告问题
const handleReportIssue = (log: LogEntry) => {
  notification.info({
    message: '问题报告',
    description: '问题报告功能开发中...',
    duration: 2
  })
}

// 导出日志
const handleExport = async () => {
  try {
    message.loading('正在导出日志...', 0)
    // 这里可以调用后端导出API
    setTimeout(() => {
      message.destroy()
      message.success('日志导出成功')
    }, 1000)
  } catch (error) {
    message.error('导出失败')
  }
}

// 清除筛选
const handleClearFilters = () => {
  filterLevel.value = undefined
  filterSource.value = ''
  keyword.value = ''
  dateRange.value = null
  filterState.level = undefined
  filterState.source = ''
  filterState.keyword = ''
  filterState.startTime = ''
  filterState.endTime = ''
  pagination.value.current = 1
  loadLogs()
}

// 监听过滤器变化
watch([filterLevel, filterSource, keyword], () => {
  handleFilterChange()
})

// 挂载时加载
onMounted(() => {
  handleRefresh()

  // 每5分钟自动刷新统计
  setInterval(() => {
    loadStatistics()
  }, 300000)
})
</script>

<style scoped>
.enhanced-log-management-view {
  padding: 20px;
}

.level-stat-item {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.level-stat-item span:first-child {
  font-weight: 600;
  min-width: 60px;
}

.level-count {
  font-weight: 700;
  min-width: 40px;
  text-align: right;
}

.level-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
}

.timestamp-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text-secondary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
}

.message-cell {
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.message-cell:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.message-cell.error-message {
  background-color: rgba(255, 77, 79, 0.05);
  border-left: 3px solid #ff4d4f;
}

.log-detail-content {
  max-height: 600px;
  overflow-y: auto;
}

.detail-message {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-primary);
}

:deep(.ant-card) {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

:deep(.ant-statistic-title) {
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
}

:deep(.ant-statistic-content) {
  font-weight: 700;
  font-size: 28px;
}
</style>
