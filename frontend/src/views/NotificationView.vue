<template>
  <div class="notification-view">
    <a-page-header
      title="通知中心"
      sub-title="查看和管理系统通知"
      :back-icon="false"
    />

    <div class="content-wrapper">
      <!-- 统计卡片 -->
      <a-row :gutter="16" class="stats-row">
        <a-col :span="8">
          <a-card>
            <a-statistic
              title="未读通知"
              :value="unreadCount"
              :value-style="{ color: '#cf1322' }"
            >
              <template #prefix>
                <BellOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card>
            <a-statistic
              title="总通知数"
              :value="pagination.total"
              :value-style="{ color: '#1890ff' }"
            >
              <template #prefix>
                <MailOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card>
            <div class="action-buttons">
              <a-button type="primary" @click="handleMarkAllRead" :disabled="unreadCount === 0">
                <CheckCircleOutlined />
                全部标记已读
              </a-button>
            </div>
          </a-card>
        </a-col>
      </a-row>

      <!-- 通知列表 -->
      <a-card class="notification-list-card" title="通知列表">
        <template #extra>
          <a-space>
            <a-radio-group v-model:value="filterType" @change="handleFilterChange">
              <a-radio-button value="all">全部</a-radio-button>
              <a-radio-button value="unread">未读</a-radio-button>
              <a-radio-button value="read">已读</a-radio-button>
            </a-radio-group>
            <a-button @click="handleRefresh">
              <ReloadOutlined />
              刷新
            </a-button>
          </a-space>
        </template>

        <a-list
          :data-source="filteredNotifications"
          :loading="loading"
          :pagination="pagination"
        >
          <template #renderItem="{ item }">
            <a-list-item
              :class="['notification-item', { unread: !item.read }]"
              @click="handleNotificationClick(item)"
            >
              <a-list-item-meta>
                <template #avatar>
                  <a-avatar :style="{ backgroundColor: getTypeColor(item.type) }">
                    <template v-if="item.type === 'INFO'">
                      <InfoCircleOutlined />
                    </template>
                    <template v-else-if="item.type === 'WARNING'">
                      <WarningOutlined />
                    </template>
                    <template v-else-if="item.type === 'ERROR'">
                      <CloseCircleOutlined />
                    </template>
                    <template v-else>
                      <CheckCircleOutlined />
                    </template>
                  </a-avatar>
                </template>
                <template #title>
                  <span :class="{ 'unread-text': !item.read }">{{ item.title }}</span>
                </template>
                <template #description>
                  <div class="notification-content">
                    <p>{{ item.message }}</p>
                    <span class="notification-time">{{ formatDate(item.createdAt) }}</span>
                  </div>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-button
                  v-if="!item.read"
                  type="link"
                  size="small"
                  @click.stop="handleMarkRead(item)"
                >
                  <CheckOutlined />
                  标记已读
                </a-button>
                <a-button type="link" danger size="small" @click.stop="handleDelete(item)">
                  <DeleteOutlined />
                  删除
                </a-button>
              </template>
            </a-list-item>
          </template>
        </a-list>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  BellOutlined,
  MailOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined,
  WarningOutlined,
  CloseCircleOutlined,
  ReloadOutlined,
  CheckOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { notificationApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { Notification } from '@/types'

const authStore = useAuthStore()
const loading = ref(false)
const notifications = ref<Notification[]>([])
const unreadCount = ref(0)
const filterType = ref<'all' | 'unread' | 'read'>('all')
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0
})

const username = computed(() => authStore.user?.username || 'admin')

const filteredNotifications = computed(() => {
  if (filterType.value === 'unread') {
    return notifications.value.filter(n => !n.read)
  } else if (filterType.value === 'read') {
    return notifications.value.filter(n => n.read)
  }
  return notifications.value
})

const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    INFO: '#1890ff',
    WARNING: '#faad14',
    ERROR: '#ff4d4f',
    SUCCESS: '#52c41a'
  }
  return colors[type] || '#1890ff'
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString()
}

const loadNotifications = async () => {
  loading.value = true
  try {
    const result = await notificationApi.getUserNotifications(username.value, {
      page: pagination.value.current - 1,
      size: pagination.value.pageSize
    })
    notifications.value = result.content
    pagination.value.total = result.totalElements
  } catch (error) {
    message.error('加载通知失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadUnreadCount = async () => {
  try {
    const result = await notificationApi.getUnreadCount(username.value)
    unreadCount.value = result.count
  } catch (error) {
    console.error('加载未读数量失败', error)
  }
}

const handleRefresh = () => {
  loadNotifications()
  loadUnreadCount()
}

const handleFilterChange = () => {
  pagination.value.current = 1
}

const handleMarkRead = async (notification: Notification) => {
  try {
    await notificationApi.markAsRead(notification.id)
    notification.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    message.success('已标记为已读')
  } catch (error) {
    message.error('操作失败')
    console.error(error)
  }
}

const handleMarkAllRead = async () => {
  try {
    await notificationApi.markAllAsRead(username.value)
    notifications.value.forEach(n => n.read = true)
    unreadCount.value = 0
    message.success('全部标记为已读')
  } catch (error) {
    message.error('操作失败')
    console.error(error)
  }
}

const handleDelete = (notification: Notification) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这条通知吗？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await notificationApi.deleteNotification(notification.id)
        message.success('删除成功')
        handleRefresh()
      } catch (error) {
        message.error('删除失败')
        console.error(error)
      }
    }
  })
}

const handleNotificationClick = (notification: Notification) => {
  if (!notification.read) {
    handleMarkRead(notification)
  }
}

watch(() => pagination.value.current, loadNotifications)

onMounted(() => {
  loadNotifications()
  loadUnreadCount()
})
</script>

<style scoped lang="scss">
.notification-view {
  padding: 24px;

  .stats-row {
    margin-bottom: 24px;

    .action-buttons {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
    }
  }

  .notification-list-card {
    .notification-item {
      cursor: pointer;
      transition: background-color 0.3s;

      &:hover {
        background-color: #f5f5f5;
      }

      &.unread {
        background-color: #e6f7ff;

        &:hover {
          background-color: #bae7ff;
        }
      }

      .unread-text {
        font-weight: bold;
      }

      .notification-content {
        p {
          margin: 0 0 4px 0;
        }

        .notification-time {
          color: #999;
          font-size: 12px;
        }
      }
    }
  }
}
</style>
