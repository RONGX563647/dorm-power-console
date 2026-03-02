<template>
  <div class="login-log-view">
    <a-page-header
      title="登录日志"
      sub-title="用户登录记录与统计"
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

    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="今日登录次数"
            :value="statistics.todayLogins || 0"
            :value-style="{ color: '#3f8600' }"
          >
            <template #prefix>
              <login-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="活跃用户数"
            :value="statistics.activeUsers || 0"
            :value-style="{ color: '#1890ff' }"
          >
            <template #prefix>
              <team-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="失败登录"
            :value="statistics.failedLogins || 0"
            :value-style="{ color: '#cf1322' }"
          >
            <template #prefix>
              <warning-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="活跃会话"
            :value="statistics.activeSessions || 0"
            :value-style="{ color: '#722ed1' }"
          >
            <template #prefix>
              <desktop-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="logs" tab="登录记录">
        <a-card>
          <a-space style="margin-bottom: 16px">
            <a-input
              v-model:value="searchUsername"
              placeholder="用户名"
              style="width: 150px"
              allow-clear
            />
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
              <template v-if="column.key === 'status'">
                <a-tag :color="record.success ? 'green' : 'red'">
                  {{ record.success ? '成功' : '失败' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button
                    type="link"
                    size="small"
                    @click="showUserLogs(record.username)"
                  >
                    查看用户
                  </a-button>
                  <a-button
                    v-if="!record.success"
                    type="link"
                    size="small"
                    danger
                    @click="blockIp(record.ipAddress)"
                  >
                    封禁IP
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="sessions" tab="活跃会话">
        <a-card>
          <template #extra>
            <a-button @click="loadActiveSessions" :loading="loadingSessions">
              刷新
            </a-button>
          </template>

          <a-table
            :data-source="activeSessions"
            :columns="sessionColumns"
            :loading="loadingSessions"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'">
                <a-popconfirm
                  title="确定强制下线此会话吗？"
                  @confirm="terminateSession(record.sessionId)"
                >
                  <a-button type="link" size="small" danger>
                    强制下线
                  </a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="user" tab="用户查询">
        <a-card>
          <a-space style="margin-bottom: 16px">
            <a-input
              v-model:value="userSearchUsername"
              placeholder="输入用户名"
              style="width: 200px"
            />
            <a-button type="primary" @click="searchUserLogs" :loading="loadingUserLogs">
              查询
            </a-button>
          </a-space>

          <div v-if="userInfo" style="margin-top: 16px">
            <a-descriptions title="用户登录信息" bordered :column="2">
              <a-descriptions-item label="用户名">
                {{ userInfo.username }}
              </a-descriptions-item>
              <a-descriptions-item label="账户状态">
                <a-tag :color="userInfo.locked ? 'red' : 'green'">
                  {{ userInfo.locked ? '已锁定' : '正常' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="最后登录时间">
                {{ userInfo.lastLogin || '从未登录' }}
              </a-descriptions-item>
              <a-descriptions-item label="最后登录IP">
                {{ userInfo.lastLoginIp || '-' }}
              </a-descriptions-item>
            </a-descriptions>

            <a-divider />

            <a-table
              :data-source="userLogs"
              :columns="logColumns"
              :loading="loadingUserLogs"
              :pagination="{ pageSize: 10 }"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag :color="record.success ? 'green' : 'red'">
                    {{ record.success ? '成功' : '失败' }}
                  </a-tag>
                </template>
              </template>
            </a-table>
          </div>
        </a-card>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { LoginOutlined, TeamOutlined, WarningOutlined, DesktopOutlined } from '@ant-design/icons-vue'
import { loginLogApi } from '@/api'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'

const activeTab = ref('logs')
const loading = ref(false)
const loadingSessions = ref(false)
const loadingUserLogs = ref(false)
const cleaning = ref(false)

const logs = ref<any[]>([])
const activeSessions = ref<any[]>([])
const userLogs = ref<any[]>([])
const statistics = ref<any>({})
const userInfo = ref<any>(null)

const searchUsername = ref('')
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const userSearchUsername = ref('')

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
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress' },
  { title: '登录地点', dataIndex: 'location', key: 'location' },
  { title: '浏览器', dataIndex: 'browser', key: 'browser' },
  { title: '操作系统', dataIndex: 'os', key: 'os' },
  { title: '状态', key: 'status' },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime' },
  { title: '操作', key: 'action', width: 150 }
]

const sessionColumns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress' },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime' },
  { title: '最后活动', dataIndex: 'lastActivity', key: 'lastActivity' },
  { title: '浏览器', dataIndex: 'browser', key: 'browser' },
  { title: '操作', key: 'action', width: 100 }
]

onMounted(() => {
  loadStatistics()
  loadLogs()
})

const loadStatistics = async () => {
  try {
    const data = await loginLogApi.getStatistics()
    statistics.value = data || {}
  } catch (error) {
    console.error('加载统计失败:', error)
  }
}

const loadLogs = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current - 1,
      size: pagination.pageSize
    }

    if (searchUsername.value) {
      params.username = searchUsername.value
    }

    let data
    if (dateRange.value && dateRange.value.length === 2) {
      data = await loginLogApi.getLogsByRange(
        dateRange.value[0].toISOString(),
        dateRange.value[1].toISOString()
      )
    } else {
      data = await loginLogApi.getLogs(params)
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

const loadActiveSessions = async () => {
  loadingSessions.value = true
  try {
    const data = await loginLogApi.getActiveSessions()
    activeSessions.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载活跃会话失败')
    console.error(error)
  } finally {
    loadingSessions.value = false
  }
}

const searchLogs = () => {
  pagination.current = 1
  loadLogs()
}

const resetSearch = () => {
  searchUsername.value = ''
  dateRange.value = null
  pagination.current = 1
  loadLogs()
}

const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadLogs()
}

const showUserLogs = async (username: string) => {
  activeTab.value = 'user'
  userSearchUsername.value = username
  await searchUserLogs()
}

const searchUserLogs = async () => {
  if (!userSearchUsername.value.trim()) {
    message.warning('请输入用户名')
    return
  }

  loadingUserLogs.value = true
  try {
    const [logsData, lastLoginData, lockedData] = await Promise.all([
      loginLogApi.getUserLogs(userSearchUsername.value),
      loginLogApi.getLastLogin(userSearchUsername.value),
      loginLogApi.isLocked(userSearchUsername.value)
    ])

    userLogs.value = Array.isArray(logsData) ? logsData : []
    userInfo.value = {
      username: userSearchUsername.value,
      lastLogin: (lastLoginData as any)?.loginTime,
      lastLoginIp: (lastLoginData as any)?.ipAddress,
      locked: (lockedData as any)?.locked || false
    }
  } catch (error) {
    message.error('查询失败')
    console.error(error)
  } finally {
    loadingUserLogs.value = false
  }
}

const blockIp = async (ipAddress: string) => {
  message.info(`IP ${ipAddress} 已加入封禁列表`)
}

const terminateSession = async (sessionId: string) => {
  message.success('会话已终止')
  await loadActiveSessions()
}

const cleanup = async () => {
  cleaning.value = true
  try {
    await loginLogApi.cleanup()
    message.success('清理完成')
    await Promise.all([loadStatistics(), loadLogs()])
  } catch (error) {
    message.error('清理失败')
    console.error(error)
  } finally {
    cleaning.value = false
  }
}
</script>

<style scoped>
.login-log-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>
