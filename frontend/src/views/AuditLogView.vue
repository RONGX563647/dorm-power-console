<template>
  <div class="audit-log-view">
    <a-page-header
      title="审计日志"
      sub-title="系统操作审计记录"
      style="padding: 0 0 16px 0"
    >
      <template #extra>
        <a-space>
          <a-button @click="cleanup" :loading="cleaning">
            清理过期日志
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-card>
      <a-space style="margin-bottom: 16px">
        <a-input
          v-model:value="searchUsername"
          placeholder="用户名"
          style="width: 150px"
          allow-clear
        />
        <a-select
          v-model:value="searchModule"
          placeholder="操作模块"
          style="width: 150px"
          allow-clear
        >
          <a-select-option value="DEVICE">设备管理</a-select-option>
          <a-select-option value="USER">用户管理</a-select-option>
          <a-select-option value="BILLING">计费管理</a-select-option>
          <a-select-option value="SYSTEM">系统管理</a-select-option>
          <a-select-option value="AUTH">认证授权</a-select-option>
        </a-select>
        <a-range-picker
          v-model:value="dateRange"
          :placeholder="['开始时间', '结束时间']"
        />
        <a-button type="primary" @click="searchLogs" :loading="loading">
          查询
        </a-button>
        <a-button @click="resetSearch">
          重置
        </a-button>
      </a-space>

      <a-table
        :data-source="logs"
        :columns="logColumns"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'module'">
            <a-tag :color="getModuleColor(record.module)">
              {{ getModuleText(record.module) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-tag :color="getActionColor(record.action)">
              {{ record.action }}
            </a-tag>
          </template>
          <template v-if="column.key === 'success'">
            <a-tag :color="record.success ? 'green' : 'red'">
              {{ record.success ? '成功' : '失败' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { auditLogApi } from '@/api'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'

const loading = ref(false)
const cleaning = ref(false)

const logs = ref<any[]>([])
const searchUsername = ref('')
const searchModule = ref<string>()
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['5', '10', '15'],
  showTotal: (total: number) => `共 ${total} 条`
})

const logColumns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '操作模块', dataIndex: 'module', key: 'module' },
  { title: '操作类型', dataIndex: 'action', key: 'action' },
  { title: '操作描述', dataIndex: 'description', key: 'description' },
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress' },
  { title: '状态', key: 'success' },
  { title: '操作时间', dataIndex: 'operationTime', key: 'operationTime' }
]

onMounted(() => {
  loadLogs()
})

const loadLogs = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current - 1,
      size: pagination.pageSize
    }

    let data
    if (searchUsername.value && dateRange.value && dateRange.value.length === 2) {
      data = await auditLogApi.getUserLogsByRange(
        searchUsername.value,
        dateRange.value[0].toISOString(),
        dateRange.value[1].toISOString()
      )
    } else if (searchUsername.value) {
      data = await auditLogApi.getUserLogs(searchUsername.value)
    } else if (searchModule.value) {
      data = await auditLogApi.getModuleLogs(searchModule.value)
    } else if (dateRange.value && dateRange.value.length === 2) {
      data = await auditLogApi.getLogsByRange(
        dateRange.value[0].toISOString(),
        dateRange.value[1].toISOString()
      )
    } else {
      data = await auditLogApi.getLogs(params)
    }

    if (Array.isArray(data)) {
      logs.value = data
      pagination.total = data.length
    } else if ((data as any).content) {
      logs.value = (data as any).content
      pagination.total = (data as any).totalElements
    }
  } catch (error) {
    message.error('加载日志失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const searchLogs = () => {
  pagination.current = 1
  loadLogs()
}

const resetSearch = () => {
  searchUsername.value = ''
  searchModule.value = undefined
  dateRange.value = null
  pagination.current = 1
  loadLogs()
}

const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadLogs()
}

const getModuleColor = (module: string) => {
  const colors: Record<string, string> = {
    DEVICE: 'blue',
    USER: 'green',
    BILLING: 'orange',
    SYSTEM: 'purple',
    AUTH: 'cyan'
  }
  return colors[module] || 'default'
}

const getModuleText = (module: string) => {
  const texts: Record<string, string> = {
    DEVICE: '设备管理',
    USER: '用户管理',
    BILLING: '计费管理',
    SYSTEM: '系统管理',
    AUTH: '认证授权'
  }
  return texts[module] || module
}

const getActionColor = (action: string) => {
  if (action.includes('CREATE') || action.includes('ADD')) return 'green'
  if (action.includes('UPDATE') || action.includes('EDIT')) return 'blue'
  if (action.includes('DELETE') || action.includes('REMOVE')) return 'red'
  return 'default'
}

const cleanup = async () => {
  cleaning.value = true
  try {
    await auditLogApi.cleanup()
    message.success('清理完成')
    await loadLogs()
  } catch (error) {
    message.error('清理失败')
    console.error(error)
  } finally {
    cleaning.value = false
  }
}
</script>

<style scoped>
.audit-log-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>
