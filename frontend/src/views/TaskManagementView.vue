<template>
  <div class="task-management-view">
    <div class="page-header">
      <h1>定时任务管理</h1>
      <a-button type="primary" @click="showCreateModal">
        <template #icon><PlusOutlined /></template>
        创建任务
      </a-button>
    </div>

    <a-row :gutter="16">
      <a-col :span="16">
        <a-card title="任务列表">
          <div class="filter-bar">
            <a-space>
              <a-select
                v-model:value="deviceFilter"
                style="width: 200px"
                @change="handleFilterChange"
              >
                <a-select-option value="all">全部设备</a-select-option>
                <a-select-option
                  v-for="device in devices"
                  :key="device.id"
                  :value="device.id"
                >
                  {{ device.name }}
                </a-select-option>
              </a-select>
              <a-select
                v-model:value="statusFilter"
                style="width: 150px"
                @change="handleFilterChange"
              >
                <a-select-option value="all">全部状态</a-select-option>
                <a-select-option value="enabled">已启用</a-select-option>
                <a-select-option value="disabled">已禁用</a-select-option>
              </a-select>
            </a-space>
          </div>

          <a-table
            :columns="taskColumns"
            :data-source="filteredTasks"
            :loading="loading"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
            :row-key="(record: ScheduledTask) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <span style="color: #1a1a1a; font-weight: 500">{{ record.name }}</span>
              </template>
              <template v-if="column.key === 'deviceName'">
                <span style="color: #1a1a1a">{{ getDeviceName(record.deviceId) }}</span>
              </template>
              <template v-if="column.key === 'action'">
                <a-tag :color="getActionColor(record.action)">{{ getActionText(record.action) }}</a-tag>
              </template>
              <template v-if="column.key === 'cronExpression'">
                <code style="color: #00d4ff">{{ record.cronExpression }}</code>
              </template>
              <template v-if="column.key === 'enabled'">
                <a-switch
                  :checked="record.enabled"
                  @change="toggleTask(record.id, record.enabled)"
                  :loading="toggling"
                />
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="viewTask(record)">
                    查看
                  </a-button>
                  <a-button type="link" size="small" @click="editTask(record)">
                    编辑
                  </a-button>
                  <a-popconfirm
                    title="确定要删除这个任务吗？"
                    @confirm="deleteTask(record.id)"
                  >
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card title="任务统计">
          <a-statistic
            title="总任务数"
            :value="tasks.length"
            :value-style="{ color: '#00d4ff' }"
          >
            <template #prefix><ClockCircleOutlined /></template>
          </a-statistic>
          <div style="margin-top: 24px">
            <a-statistic
              title="已启用任务"
              :value="enabledCount"
              :value-style="{ color: '#52c41a' }"
            >
              <template #prefix><CheckCircleOutlined /></template>
            </a-statistic>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 创建/编辑任务模态框 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingTask ? '编辑任务' : '创建任务'"
      width="700px"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-form-item label="任务名称" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入任务名称" />
        </a-form-item>
        <a-form-item label="设备" name="deviceId">
          <a-select
            v-model:value="formData.deviceId"
            placeholder="请选择设备"
            style="width: 100%"
            :options="devices.map(d => ({ label: d.name, value: d.id }))"
          />
        </a-form-item>
        <a-form-item label="执行动作" name="action">
          <a-select v-model:value="formData.action" placeholder="请选择执行动作">
            <a-select-option value="ON">开启</a-select-option>
            <a-select-option value="OFF">关闭</a-select-option>
            <a-select-option value="TOGGLE">切换</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="Cron表达式" name="cronExpression">
          <a-input-group compact>
            <a-select
              v-model:value="cronPreset"
              style="width: 200px"
              placeholder="选择预设时间"
              @change="handleCronPresetChange"
            >
              <a-select-option value="0 0 23 * * ?">每天 23:00</a-select-option>
              <a-select-option value="0 0 7 * * ?">每天 07:00</a-select-option>
              <a-select-option value="0 0 23 * * MON-FRI">工作日 23:00</a-select-option>
              <a-select-option value="0 0 23 * * SAT,SUN">周末 23:00</a-select-option>
              <a-select-option value="0 0 */2 * * ?">每2小时</a-select-option>
              <a-select-option value="0 30 12 * * ?">每天 12:30</a-select-option>
              <a-select-option value="custom">自定义</a-select-option>
            </a-select>
            <a-input
              v-model:value="formData.cronExpression"
              placeholder="例如: 0 0 23 * * ?"
              style="width: calc(100% - 200px)"
              :disabled="cronPreset !== 'custom' && cronPreset !== ''"
            />
          </a-input-group>
          <div style="margin-top: 8px; color: #1a1a1a; font-size: 12px">
            <div>格式: 秒 分 时 日 月 周 (例如: 0 0 23 * * ? 表示每天23:00执行)</div>
            <div style="margin-top: 4px">
              <a-tag color="blue" style="cursor: pointer" @click="setCronExpression('0 0 23 * * ?')">每天23:00</a-tag>
              <a-tag color="green" style="cursor: pointer" @click="setCronExpression('0 0 7 * * ?')">每天07:00</a-tag>
              <a-tag color="orange" style="cursor: pointer" @click="setCronExpression('0 0 */2 * * ?')">每2小时</a-tag>
            </div>
          </div>
        </a-form-item>
        <a-form-item label="任务描述" name="description">
          <a-textarea
            v-model:value="formData.description"
            placeholder="请输入任务描述"
            :rows="3"
          />
        </a-form-item>
        <a-form-item label="启用任务">
          <a-switch v-model:checked="formData.enabled" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 任务详情模态框 -->
    <a-modal
      v-model:open="detailVisible"
      title="任务详情"
      width="600px"
      :footer="null"
    >
      <div v-if="selectedTask">
        <a-descriptions bordered :column="2">
          <a-descriptions-item label="任务名称">{{ selectedTask.name }}</a-descriptions-item>
          <a-descriptions-item label="设备">{{ getDeviceName(selectedTask.deviceId) }}</a-descriptions-item>
          <a-descriptions-item label="执行动作">
            <a-tag :color="getActionColor(selectedTask.action)">{{ getActionText(selectedTask.action) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="任务状态">
            <a-badge :status="selectedTask.enabled ? 'success' : 'default'" :text="selectedTask.enabled ? '已启用' : '已禁用'" />
          </a-descriptions-item>
          <a-descriptions-item label="Cron表达式" :span="2">
            <code>{{ selectedTask.cronExpression }}</code>
          </a-descriptions-item>
          <a-descriptions-item label="任务描述" :span="2">
            {{ selectedTask.description || '--' }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDate(selectedTask.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatDate(selectedTask.updatedAt) }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ClockCircleOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import { taskApi, deviceApi } from '@/api'
import type { ScheduledTask, Device } from '@/types'

const loading = ref(false)
const toggling = ref(false)
const tasks = ref<ScheduledTask[]>([])
const devices = ref<Device[]>([])
const deviceFilter = ref('all')
const statusFilter = ref('all')
const modalVisible = ref(false)
const detailVisible = ref(false)
const editingTask = ref<ScheduledTask | null>(null)
const selectedTask = ref<ScheduledTask | null>(null)
const cronPreset = ref('')

const formData = ref({
  name: '',
  deviceId: '',
  action: 'ON' as 'ON' | 'OFF' | 'TOGGLE',
  cronExpression: '',
  description: '',
  enabled: true
})

const handleCronPresetChange = (value: string) => {
  if (value !== 'custom') {
    formData.value.cronExpression = value
  }
}

const setCronExpression = (expression: string) => {
  formData.value.cronExpression = expression
  cronPreset.value = expression
}

const formRules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  action: [{ required: true, message: '请选择执行动作', trigger: 'change' }],
  cronExpression: [{ required: true, message: '请输入Cron表达式', trigger: 'blur' }]
}

const taskColumns = [
  { title: '任务名称', dataIndex: 'name', key: 'name' },
  { title: '设备', key: 'deviceName' },
  { title: '执行动作', key: 'action', width: 100 },
  { title: 'Cron表达式', key: 'cronExpression' },
  { title: '启用状态', key: 'enabled', width: 100 },
  { title: '操作', key: 'action', width: 180 }
]

const enabledCount = computed(() => {
  return tasks.value.filter(t => t.enabled).length
})

const filteredTasks = computed(() => {
  return tasks.value
    .filter(task => {
      if (deviceFilter.value === 'all') return true
      return task.deviceId === deviceFilter.value
    })
    .filter(task => {
      if (statusFilter.value === 'all') return true
      if (statusFilter.value === 'enabled') return task.enabled
      return !task.enabled
    })
})

const loadTasks = async () => {
  try {
    loading.value = true
    tasks.value = await taskApi.getAllTasks()
  } catch (error: any) {
    message.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

const loadDevices = async () => {
  try {
    devices.value = await deviceApi.getDevices()
  } catch (error: any) {
    message.error('加载设备列表失败')
  }
}

const showCreateModal = () => {
  editingTask.value = null
  formData.value = {
    name: '',
    deviceId: devices.value.length > 0 ? devices.value[0].id : '',
    action: 'ON',
    cronExpression: '',
    description: '',
    enabled: true
  }
  modalVisible.value = true
}

const editTask = (task: ScheduledTask) => {
  editingTask.value = task
  formData.value = {
    name: task.name,
    deviceId: task.deviceId,
    action: task.action,
    cronExpression: task.cronExpression,
    description: task.description || '',
    enabled: task.enabled
  }
  modalVisible.value = true
}

const handleModalOk = async () => {
  try {
    if (editingTask.value) {
      await taskApi.updateTask(editingTask.value.id, formData.value)
      message.success('任务更新成功')
    } else {
      await taskApi.createTask(formData.value)
      message.success('任务创建成功')
    }
    modalVisible.value = false
    await loadTasks()
  } catch (error: any) {
    message.error(editingTask.value ? '任务更新失败' : '任务创建失败')
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
}

const viewTask = (task: ScheduledTask) => {
  selectedTask.value = task
  detailVisible.value = true
}

const deleteTask = async (taskId: string) => {
  try {
    await taskApi.deleteTask(taskId)
    message.success('任务删除成功')
    await loadTasks()
  } catch (error: any) {
    message.error('任务删除失败')
  }
}

const toggleTask = async (taskId: string, currentEnabled: boolean) => {
  try {
    toggling.value = true
    await taskApi.toggleTask(taskId, !currentEnabled)
    message.success('任务状态已切换')
    await loadTasks()
  } catch (error: any) {
    message.error('任务状态切换失败')
  } finally {
    toggling.value = false
  }
}

const handleFilterChange = () => {
  // 过滤已在 computed 中处理
}

const getDeviceName = (deviceId: string) => {
  const device = devices.value.find(d => d.id === deviceId)
  return device ? device.name : deviceId
}

const getActionColor = (action: string) => {
  const colors: Record<string, string> = {
    'ON': 'green',
    'OFF': 'red',
    'TOGGLE': 'blue'
  }
  return colors[action] || 'default'
}

const getActionText = (action: string) => {
  const texts: Record<string, string> = {
    'ON': '开启',
    'OFF': '关闭',
    'TOGGLE': '切换'
  }
  return texts[action] || action
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '--'
  return new Date(dateString).toLocaleString()
}

onMounted(() => {
  loadTasks()
  loadDevices()
})
</script>

<style scoped>
.task-management-view {
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

.filter-bar {
  margin-bottom: 16px;
}

:deep(.ant-card) {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
}

:deep(.ant-card-head) {
  color: #1a1a1a;
  border-bottom-color: rgba(0, 212, 255, 0.15);
}

:deep(.ant-table) {
  color: #1a1a1a;
}

:deep(.ant-table-thead > tr > th) {
  color: #1a1a1a;
  background: rgba(0, 212, 255, 0.05);
}

:deep(.ant-table-tbody > tr) {
  border-bottom-color: rgba(0, 212, 255, 0.1);
}

:deep(.ant-table-tbody > tr:hover) {
  background: rgba(0, 212, 255, 0.05);
}

:deep(.ant-statistic-title) {
  color: #1a1a1a;
}

:deep(.ant-statistic-content) {
  color: #1a1a1a;
}

:deep(.ant-modal-content) {
  background: rgba(16, 24, 40, 0.95);
}

:deep(.ant-modal-header) {
  background: rgba(16, 24, 40, 0.95);
  border-bottom-color: rgba(0, 212, 255, 0.15);
}

:deep(.ant-modal-title) {
  color: #1a1a1a;
}

:deep(.ant-descriptions-item-label) {
  color: #1a1a1a;
}

:deep(.ant-descriptions-item-content) {
  color: #1a1a1a;
}

code {
  background: rgba(0, 212, 255, 0.1);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}
</style>