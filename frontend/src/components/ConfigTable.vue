<template>
  <div class="config-table">
    <a-table
      :columns="columns"
      :data-source="configs"
      :loading="loading"
      :pagination="{ pageSize: 10 }"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="$emit('edit', record)">
              <EditOutlined />
              编辑
            </a-button>
          </a-space>
        </template>
        <template v-if="column.key === 'category'">
          <a-tag :color="getCategoryColor(record.category)">
            {{ getCategoryLabel(record.category) }}
          </a-tag>
        </template>
        <template v-if="column.key === 'updatedAt'">
          {{ formatDate(record.updatedAt) }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { EditOutlined } from '@ant-design/icons-vue'
import type { SystemConfig } from '@/types'

interface Props {
  configs: SystemConfig[]
  loading: boolean
}

defineProps<Props>()
defineEmits<{
  edit: [config: SystemConfig]
  refresh: []
}>()

const columns = [
  {
    title: '配置键',
    dataIndex: 'key',
    key: 'key',
    width: 200
  },
  {
    title: '配置值',
    dataIndex: 'value',
    key: 'value',
    ellipsis: true
  },
  {
    title: '分类',
    dataIndex: 'category',
    key: 'category',
    width: 120
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    ellipsis: true
  },
  {
    title: '更新时间',
    dataIndex: 'updatedAt',
    key: 'updatedAt',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    fixed: 'right'
  }
]

const getCategoryColor = (category: string) => {
  const colors: Record<string, string> = {
    system: 'blue',
    device: 'green',
    alert: 'orange',
    security: 'red'
  }
  return colors[category] || 'default'
}

const getCategoryLabel = (category: string) => {
  const labels: Record<string, string> = {
    system: '系统',
    device: '设备',
    alert: '告警',
    security: '安全'
  }
  return labels[category] || category
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}
</script>

<style scoped lang="scss">
.config-table {
  width: 100%;
}
</style>
