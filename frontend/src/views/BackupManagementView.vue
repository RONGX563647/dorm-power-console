<template>
  <div class="backup-management-view">
    <a-page-header
      title="数据备份管理"
      sub-title="管理系统数据备份和恢复"
      :back-icon="false"
    />

    <div class="content-wrapper">
      <!-- 统计卡片 -->
      <a-row :gutter="16" class="stats-row">
        <a-col :span="8">
          <a-card>
            <a-statistic
              title="备份总数"
              :value="statistics?.totalBackups || 0"
              :value-style="{ color: '#3f8600' }"
            >
              <template #prefix>
                <DatabaseOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card>
            <a-statistic
              title="总大小"
              :value="formatSize(statistics?.totalSize || 0)"
              :value-style="{ color: '#1890ff' }"
            >
              <template #prefix>
                <HddOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card>
            <a-statistic
              title="上次备份"
              :value="statistics?.lastBackupTime ? formatDate(statistics.lastBackupTime) : '无'"
              :value-style="{ color: '#722ed1', fontSize: '16px' }"
            >
              <template #prefix>
                <ClockCircleOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>

      <!-- 备份列表 -->
      <a-card class="backup-list-card" title="备份列表">
        <template #extra>
          <a-space>
            <a-button type="primary" @click="handleCreateBackup">
              <PlusOutlined />
              创建备份
            </a-button>
            <a-button @click="handleRefresh">
              <ReloadOutlined />
              刷新
            </a-button>
          </a-space>
        </template>
        <a-table
          :columns="columns"
          :data-source="backups"
          :loading="loading"
          :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'">
              <a-tag :color="record.type === 'FULL' ? 'blue' : 'green'">
                {{ record.type === 'FULL' ? '全量备份' : '增量备份' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'size'">
              {{ formatSize(record.size) }}
            </template>
            <template v-if="column.key === 'createdAt'">
              {{ formatDate(record.createdAt) }}
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="handleRestore(record)">
                  <RollbackOutlined />
                  恢复
                </a-button>
                <a-button type="link" danger size="small" @click="handleDelete(record)">
                  <DeleteOutlined />
                  删除
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <!-- 创建备份弹窗 -->
    <a-modal
      v-model:open="createModalVisible"
      title="创建备份"
      @ok="confirmCreateBackup"
      @cancel="createModalVisible = false"
      :confirm-loading="createLoading"
    >
      <a-form :model="createForm" layout="vertical">
        <a-form-item label="备份名称" required>
          <a-input
            v-model:value="createForm.name"
            placeholder="请输入备份名称"
          />
        </a-form-item>
        <a-form-item label="备份类型">
          <a-radio-group v-model:value="createForm.type">
            <a-radio value="FULL">全量备份</a-radio>
            <a-radio value="INCREMENTAL">增量备份</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 恢复确认弹窗 -->
    <a-modal
      v-model:open="restoreModalVisible"
      title="恢复备份"
      @ok="confirmRestore"
      @cancel="restoreModalVisible = false"
      :confirm-loading="restoreLoading"
    >
      <a-alert
        message="警告"
        description="恢复备份将覆盖当前数据，请确保已备份重要数据。此操作不可撤销。"
        type="warning"
        show-icon
        style="margin-bottom: 16px"
      />
      <p>确定要恢复备份 <strong>{{ selectedBackup?.name }}</strong> 吗？</p>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  DatabaseOutlined,
  HddOutlined,
  ClockCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  RollbackOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { backupApi } from '@/api'
import type { BackupInfo, BackupStatistics } from '@/types'

const loading = ref(false)
const createLoading = ref(false)
const restoreLoading = ref(false)
const backups = ref<BackupInfo[]>([])
const statistics = ref<BackupStatistics | null>(null)
const createModalVisible = ref(false)
const restoreModalVisible = ref(false)
const selectedBackup = ref<BackupInfo | null>(null)
const createForm = ref({
  name: '',
  type: 'FULL' as 'FULL' | 'INCREMENTAL'
})

const columns = [
  {
    title: '备份名称',
    dataIndex: 'name',
    key: 'name'
  },
  {
    title: '类型',
    dataIndex: 'type',
    key: 'type',
    width: 120
  },
  {
    title: '大小',
    dataIndex: 'size',
    key: 'size',
    width: 120
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    fixed: 'right'
  }
]

const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString()
}

const loadBackups = async () => {
  loading.value = true
  try {
    backups.value = await backupApi.getAllBackups()
  } catch (error) {
    message.error('加载备份列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const backups = await backupApi.getAllBackups()
    statistics.value = {
      totalBackups: backups.length,
      totalSize: backups.reduce((sum, b) => sum + (b.size || 0), 0),
      lastBackupTime: backups.length > 0 ? backups[0].createdAt : undefined
    }
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

const handleRefresh = () => {
  loadBackups()
  loadStatistics()
}

const handleCreateBackup = () => {
  createForm.value = {
    name: `backup_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}`,
    type: 'FULL'
  }
  createModalVisible.value = true
}

const confirmCreateBackup = async () => {
  if (!createForm.value.name) {
    message.warning('请输入备份名称')
    return
  }

  createLoading.value = true
  try {
    await backupApi.createDatabaseBackup(createForm.value.name, 'admin')
    message.success('备份创建成功')
    createModalVisible.value = false
    handleRefresh()
  } catch (error) {
    message.error('备份创建失败')
    console.error(error)
  } finally {
    createLoading.value = false
  }
}

const handleRestore = (backup: BackupInfo) => {
  selectedBackup.value = backup
  restoreModalVisible.value = true
}

const confirmRestore = async () => {
  if (!selectedBackup.value) return

  restoreLoading.value = true
  try {
    message.info('备份恢复功能需要后端实现')
    restoreModalVisible.value = false
  } catch (error) {
    message.error('备份恢复失败')
    console.error(error)
  } finally {
    restoreLoading.value = false
  }
}

const handleDelete = (backup: BackupInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除备份 "${backup.name}" 吗？此操作不可撤销。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await backupApi.deleteBackup(Number(backup.id))
        message.success('备份删除成功')
        handleRefresh()
      } catch (error) {
        message.error('备份删除失败')
        console.error(error)
      }
    }
  })
}

onMounted(() => {
  loadBackups()
  loadStatistics()
})
</script>

<style scoped lang="scss">
.backup-management-view {
  padding: 24px;

  .stats-row {
    margin-bottom: 24px;
  }
}
</style>
