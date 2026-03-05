<template>
  <div class="api-test-view">
    <div class="page-header">
      <h1>API 测试</h1>
      <a-space>
        <a-button @click="runAllTests" :loading="running" type="primary">
          <template #icon><PlayCircleOutlined /></template>
          运行所有测试
        </a-button>
        <a-button @click="clearResults">
          <template #icon><ClearOutlined /></template>
          清空结果
        </a-button>
      </a-space>
    </div>

    <a-row :gutter="16">
      <a-col :span="6">
        <a-card title="测试统计">
          <a-statistic
            title="总测试数"
            :value="totalTests"
            :value-style="{ color: '#00d4ff' }"
          />
          <div style="margin-top: 16px">
            <a-statistic
              title="通过"
              :value="passedTests"
              :value-style="{ color: '#52c41a' }"
            >
              <template #prefix><CheckCircleOutlined /></template>
            </a-statistic>
          </div>
          <div style="margin-top: 16px">
            <a-statistic
              title="失败"
              :value="failedTests"
              :value-style="{ color: '#ff4d4f' }"
            >
              <template #prefix><CloseCircleOutlined /></template>
            </a-statistic>
          </div>
        </a-card>
      </a-col>

      <a-col :span="18">
        <a-card title="测试结果">
          <a-collapse v-model:activeKey="activeKeys">
            <a-collapse-panel v-for="(category, index) in testCategories" :key="index" :header="category.name">
              <a-list :data-source="category.tests" size="small">
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a-list-item-meta>
                      <template #title>
                        <span :style="{ color: item.status === 'success' ? '#52c41a' : item.status === 'error' ? '#ff4d4f' : '#faad14' }">
                          {{ item.status === 'success' ? '✓' : item.status === 'error' ? '✗' : '⚠' }} {{ item.name }}
                        </span>
                      </template>
                      <template #description>
                        <div>
                          <div style="color: #1a1a1a">{{ item.endpoint }}</div>
                          <div v-if="item.message" style="color: #ff4d4f">{{ item.message }}</div>
                          <div v-if="item.duration" style="color: #52c41a">耗时: {{ item.duration }}ms</div>
                        </div>
                      </template>
                    </a-list-item-meta>
                  </a-list-item>
                </template>
              </a-list>
            </a-collapse-panel>
          </a-collapse>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlayCircleOutlined, ClearOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import { authApi, deviceApi, commandApi, aiReportApi, deviceGroupApi, userApi, alertApi, taskApi, dormApi, monitorApi } from '@/api'

interface TestResult {
  name: string
  endpoint: string
  status: 'success' | 'error' | 'warning'
  message?: string
  duration?: number
}

interface TestCategory {
  name: string
  tests: TestResult[]
}

const running = ref(false)
const activeKeys = ref<number[]>([])
const results = ref<TestResult[]>([])

const testCategories = computed(() => {
  const categories: TestCategory[] = [
    { name: '认证模块 (Auth)', tests: [] },
    { name: '设备模块 (Devices)', tests: [] },
    { name: '命令模块 (Commands)', tests: [] },
    { name: '遥测模块 (Telemetry)', tests: [] },
    { name: 'AI报告模块 (AI Reports)', tests: [] },
    { name: '设备分组模块 (Device Groups)', tests: [] },
    { name: '用户模块 (Users)', tests: [] },
    { name: '告警模块 (Alerts)', tests: [] },
    { name: '定时任务模块 (Tasks)', tests: [] },
    { name: '健康检查 (Health)', tests: [] }
  ]

  results.value.forEach(result => {
    if (result.endpoint.includes('/auth')) {
      categories[0].tests.push(result)
    } else if (result.endpoint.includes('/devices') || result.endpoint.includes('/strips')) {
      categories[1].tests.push(result)
    } else if (result.endpoint.includes('/cmd')) {
      categories[2].tests.push(result)
    } else if (result.endpoint.includes('/telemetry')) {
      categories[3].tests.push(result)
    } else if (result.endpoint.includes('/ai')) {
      categories[4].tests.push(result)
    } else if (result.endpoint.includes('/groups')) {
      categories[5].tests.push(result)
    } else if (result.endpoint.includes('/users')) {
      categories[6].tests.push(result)
    } else if (result.endpoint.includes('/alerts')) {
      categories[7].tests.push(result)
    } else if (result.endpoint.includes('/tasks')) {
      categories[8].tests.push(result)
    } else if (result.endpoint.includes('/health')) {
      categories[9].tests.push(result)
    }
  })

  return categories.filter(c => c.tests.length > 0)
})

const totalTests = computed(() => results.value.length)
const passedTests = computed(() => results.value.filter(r => r.status === 'success').length)
const failedTests = computed(() => results.value.filter(r => r.status === 'error').length)

const addResult = (result: TestResult) => {
  results.value.push(result)
}

const runTest = async (name: string, endpoint: string, testFn: () => Promise<any>) => {
  const startTime = Date.now()
  try {
    await testFn()
    const duration = Date.now() - startTime
    addResult({ name, endpoint, status: 'success', duration })
  } catch (error: any) {
    const duration = Date.now() - startTime
    addResult({ name, endpoint, status: 'error', message: error.message || 'Unknown error', duration })
  }
}

const runAllTests = async () => {
  running.value = true
  results.value = []
  activeKeys.value = Array.from({ length: 10 }, (_, i) => i)

  try {
    // 认证模块测试
    await runTest('登录', '/api/auth/login', async () => {
      const result = await authApi.login({ account: 'admin', password: 'admin123' })
      if (!result.token) throw new Error('Missing token')
    })

    await runTest('获取当前用户', '/api/auth/me', async () => {
      const result = await authApi.getCurrentUser()
      if (!result.username) throw new Error('Missing username')
    })

    await runTest('刷新令牌', '/api/auth/refresh', async () => {
      const result = await authApi.refreshToken()
      if (!result.token) throw new Error('Missing token')
    })

    // 设备模块测试
    await runTest('获取设备列表', '/api/devices', async () => {
      const result = await deviceApi.getDevices()
      if (!Array.isArray(result)) throw new Error('Invalid response')
    })

    await runTest('获取房间设备', '/api/devices/room/101', async () => {
      const result = await deviceApi.getRoomDevices('101')
      if (!Array.isArray(result)) throw new Error('Invalid response')
    })

    await runTest('获取设备历史', '/api/devices/{id}/history', async () => {
      const devices = await deviceApi.getDevices()
      if (devices.length > 0) {
        const result = await deviceApi.getDeviceHistory(devices[0].id)
        if (!Array.isArray(result)) throw new Error('Invalid response')
      } else {
        throw new Error('No devices available')
      }
    })

    // 命令模块测试
    await runTest('发送命令', '/api/strips/{id}/cmd', async () => {
      const devices = await deviceApi.getDevices()
      if (devices.length > 0) {
        const result = await commandApi.sendCommand(devices[0].id, { action: 'QUERY' })
        if (!(result as any).cmdId && !(result as any).cmd_id) throw new Error('Missing cmdId')
      } else {
        throw new Error('No devices available')
      }
    })

    // 遥测模块测试
    await runTest('获取遥测数据', '/api/telemetry', async () => {
      const devices = await deviceApi.getDevices()
      if (devices.length > 0) {
        const result = await deviceApi.getTelemetry(devices[0].id, '60s')
        if (!Array.isArray(result)) throw new Error('Invalid response')
      } else {
        throw new Error('No devices available')
      }
    })

    await runTest('获取遥测统计', '/api/telemetry/statistics', async () => {
      const devices = await deviceApi.getDevices()
      if (devices.length > 0) {
        const result = await deviceApi.getTelemetryStatistics({ device: devices[0].id })
        if (!result) throw new Error('Invalid response')
      } else {
        throw new Error('No devices available')
      }
    })

    // 宿舍管理模块测试
    await runTest('获取房间列表', '/api/dorm/rooms', async () => {
      const result = await dormApi.getAllRooms()
      if (!Array.isArray(result)) throw new Error('Invalid response')
    })

    // AI报告模块测试
    await runTest('生成AI报告', '/api/rooms/{roomId}/ai_report', async () => {
      const rooms = await dormApi.getAllRooms()
      if (rooms.length > 0) {
        const result = await aiReportApi.getReport(rooms[0].id)
        if (!(result as any).summary) throw new Error('Missing summary')
      } else {
        throw new Error('No rooms available')
      }
    })

    // 设备分组模块测试
    await runTest('获取分组列表', '/api/groups', async () => {
      const result = await deviceGroupApi.getGroups()
      if (!Array.isArray(result)) throw new Error('Invalid response')
    })

    // 用户模块测试
    await runTest('获取用户列表', '/api/users', async () => {
      const result = await userApi.getUsers()
      if (!Array.isArray(result)) throw new Error('Invalid response')
    })

    // 告警模块测试
    await runTest('获取未解决告警', '/api/alerts/unresolved', async () => {
      const result = await alertApi.getUnresolvedAlerts()
      if (!Array.isArray(result)) throw new Error('Invalid response')
    })

    // 定时任务模块测试
    await runTest('获取所有任务', '/api/tasks', async () => {
      const result = await taskApi.getAllTasks()
      if (!Array.isArray(result)) throw new Error('Invalid response')
    })

    // 系统监控模块测试
    await runTest('获取系统状态', '/api/admin/monitor/system', async () => {
      const result = await monitorApi.getSystemStatus()
      if (!result) throw new Error('Invalid response')
    })

    message.success('所有测试完成')
  } catch (error: any) {
    message.error('测试过程中发生错误')
  } finally {
    running.value = false
  }
}

const clearResults = () => {
  results.value = []
  activeKeys.value = []
}
</script>

<style scoped>
.api-test-view {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  color: #1a1a1a;
  margin: 0;
}

:deep(.ant-card) {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
}

:deep(.ant-card-head) {
  color: #1a1a1a;
  border-bottom-color: rgba(0, 212, 255, 0.15);
}

:deep(.ant-statistic-title) {
  color: #1a1a1a;
}

:deep(.ant-statistic-content) {
  color: #1a1a1a;
}

:deep(.ant-collapse) {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
}

:deep(.ant-collapse-item) {
  border-bottom-color: rgba(0, 212, 255, 0.1);
}

:deep(.ant-collapse-header) {
  color: #1a1a1a;
}

:deep(.ant-collapse-content) {
  background: rgba(16, 24, 40, 0.6);
}

:deep(.ant-list-item) {
  border-bottom-color: rgba(0, 212, 255, 0.1);
}
</style>