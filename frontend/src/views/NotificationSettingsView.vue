<template>
  <div class="notification-settings-view">
    <div class="page-header">
      <h1>通知设置</h1>
      <a-space>
        <a-button @click="handleReset">重置默认</a-button>
        <a-button type="primary" :loading="saving" @click="handleSave">保存设置</a-button>
      </a-space>
    </div>

    <a-row :gutter="24">
      <a-col :span="16">
        <a-card title="通知渠道" class="settings-card">
          <a-list>
            <a-list-item>
              <a-list-item-meta title="系统通知" description="在系统内接收通知消息">
                <template #avatar>
                  <a-avatar style="background-color: #1890ff">
                    <BellOutlined />
                  </a-avatar>
                </template>
              </a-list-item-meta>
              <a-switch v-model:checked="settings.inAppEnabled" />
            </a-list-item>

            <a-list-item>
              <a-list-item-meta title="邮件通知" description="通过邮件接收重要通知">
                <template #avatar>
                  <a-avatar style="background-color: #52c41a">
                    <MailOutlined />
                  </a-avatar>
                </template>
              </a-list-item-meta>
              <a-switch v-model:checked="settings.emailEnabled" />
            </a-list-item>

            <a-list-item>
              <a-list-item-meta title="短信通知" description="通过短信接收紧急通知">
                <template #avatar>
                  <a-avatar style="background-color: #722ed1">
                    <MessageOutlined />
                  </a-avatar>
                </template>
              </a-list-item-meta>
              <a-switch v-model:checked="settings.smsEnabled" />
            </a-list-item>
          </a-list>
        </a-card>

        <a-card title="通知类型" class="settings-card" style="margin-top: 24px">
          <a-list>
            <a-list-item>
              <a-list-item-meta title="告警通知" description="设备告警、异常状态等通知">
                <template #avatar>
                  <a-avatar style="background-color: #ff4d4f">
                    <WarningOutlined />
                  </a-avatar>
                </template>
              </a-list-item-meta>
              <a-switch v-model:checked="settings.alertNotifications" />
            </a-list-item>

            <a-list-item>
              <a-list-item-meta title="系统通知" description="系统更新、维护等通知">
                <template #avatar>
                  <a-avatar style="background-color: #1890ff">
                    <InfoCircleOutlined />
                  </a-avatar>
                </template>
              </a-list-item-meta>
              <a-switch v-model:checked="settings.systemNotifications" />
            </a-list-item>

            <a-list-item>
              <a-list-item-meta title="任务通知" description="定时任务执行结果通知">
                <template #avatar>
                  <a-avatar style="background-color: #faad14">
                    <ClockCircleOutlined />
                  </a-avatar>
                </template>
              </a-list-item-meta>
              <a-switch v-model:checked="settings.taskNotifications" />
            </a-list-item>

            <a-list-item>
              <a-list-item-meta title="计费通知" description="电费账单、余额不足等通知">
                <template #avatar>
                  <a-avatar style="background-color: #13c2c2">
                    <DollarOutlined />
                  </a-avatar>
                </template>
              </a-list-item-meta>
              <a-switch v-model:checked="settings.billingNotifications" />
            </a-list-item>
          </a-list>
        </a-card>

        <a-card title="告警级别设置" class="settings-card" style="margin-top: 24px">
          <a-form layout="vertical">
            <a-form-item label="接收的最低告警级别">
              <a-select v-model:value="settings.minAlertLevel" style="width: 200px">
                <a-select-option value="INFO">信息</a-select-option>
                <a-select-option value="WARNING">警告</a-select-option>
                <a-select-option value="ERROR">错误</a-select-option>
                <a-select-option value="CRITICAL">严重</a-select-option>
              </a-select>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card title="当前设置概览" class="settings-card">
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="系统通知">
              <a-tag :color="settings.inAppEnabled ? 'green' : 'red'">
                {{ settings.inAppEnabled ? '已启用' : '已禁用' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="邮件通知">
              <a-tag :color="settings.emailEnabled ? 'green' : 'red'">
                {{ settings.emailEnabled ? '已启用' : '已禁用' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="短信通知">
              <a-tag :color="settings.smsEnabled ? 'green' : 'red'">
                {{ settings.smsEnabled ? '已启用' : '已禁用' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="最低告警级别">
              <a-tag>{{ getLevelName(settings.minAlertLevel) }}</a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card title="通知统计" class="settings-card" style="margin-top: 24px">
          <a-statistic title="今日通知" :value="stats.todayCount" />
          <a-statistic title="本周通知" :value="stats.weekCount" style="margin-top: 16px" />
          <a-statistic title="未读通知" :value="stats.unreadCount" style="margin-top: 16px">
            <template #suffix>
              <a-button type="link" size="small" @click="goToNotifications">查看</a-button>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  BellOutlined,
  MailOutlined,
  MessageOutlined,
  WarningOutlined,
  InfoCircleOutlined,
  ClockCircleOutlined,
  DollarOutlined
} from '@ant-design/icons-vue'
import { notificationApi } from '@/api'

const router = useRouter()
const saving = ref(false)

const settings = ref({
  inAppEnabled: true,
  emailEnabled: true,
  smsEnabled: false,
  alertNotifications: true,
  systemNotifications: true,
  taskNotifications: true,
  billingNotifications: true,
  minAlertLevel: 'WARNING'
})

const stats = ref({
  todayCount: 0,
  weekCount: 0,
  unreadCount: 0
})

const defaultSettings = {
  inAppEnabled: true,
  emailEnabled: true,
  smsEnabled: false,
  alertNotifications: true,
  systemNotifications: true,
  taskNotifications: true,
  billingNotifications: true,
  minAlertLevel: 'WARNING'
}

const loadSettings = async () => {
  try {
    const savedSettings = localStorage.getItem('notificationSettings')
    if (savedSettings) {
      settings.value = JSON.parse(savedSettings)
    }
  } catch (error) {
    console.error('加载设置失败', error)
  }
}

const loadStats = async () => {
  try {
    const username = localStorage.getItem('username') || 'admin'
    const statsData = await notificationApi.getStatistics(username)
    stats.value = {
      todayCount: statsData.todayCount || 0,
      weekCount: statsData.weekCount || 0,
      unreadCount: statsData.unreadCount || 0
    }
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    localStorage.setItem('notificationSettings', JSON.stringify(settings.value))
    message.success('设置已保存')
  } catch (error) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleReset = () => {
  settings.value = { ...defaultSettings }
  message.info('已重置为默认设置')
}

const goToNotifications = () => {
  router.push('/notifications')
}

const getLevelName = (level: string) => {
  const map: Record<string, string> = {
    INFO: '信息',
    WARNING: '警告',
    ERROR: '错误',
    CRITICAL: '严重'
  }
  return map[level] || level
}

onMounted(() => {
  loadSettings()
  loadStats()
})
</script>

<style scoped>
.notification-settings-view {
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
  font-size: 24px;
}

.settings-card {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.1);
}

.settings-card :deep(.ant-card-head) {
  border-bottom: 1px solid rgba(0, 212, 255, 0.1);
}

.settings-card :deep(.ant-card-head-title) {
  color: #1a1a1a;
}

.settings-card :deep(.ant-list-item) {
  border-bottom: 1px solid rgba(0, 212, 255, 0.1);
}

.settings-card :deep(.ant-list-item-meta-title) {
  color: #1a1a1a;
}

.settings-card :deep(.ant-list-item-meta-description) {
  color: #1a1a1a;
}

.settings-card :deep(.ant-descriptions-item-label) {
  color: #1a1a1a;
}

.settings-card :deep(.ant-descriptions-item-content) {
  color: #1a1a1a;
}

.settings-card :deep(.ant-statistic-title) {
  color: #1a1a1a;
}

.settings-card :deep(.ant-statistic-content) {
  color: #1a1a1a;
}
</style>
