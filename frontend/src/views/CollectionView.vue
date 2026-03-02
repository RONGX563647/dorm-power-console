<template>
  <div class="collection-view">
    <a-page-header
      title="收款管理"
      sub-title="电费收款记录管理"
      style="padding: 0 0 16px 0"
    >
      <template #extra>
        <a-space>
          <a-button @click="cleanup" :loading="cleaning">
            清理过期记录
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-card>
      <a-space style="margin-bottom: 16px">
        <a-input
          v-model:value="searchRoomId"
          placeholder="房间ID"
          style="width: 200px"
          allow-clear
        />
        <a-button type="primary" @click="searchCollections" :loading="loading">
          查询
        </a-button>
      </a-space>

      <a-table
        :data-source="collections"
        :columns="collectionColumns"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            <span style="color: #52c41a; font-weight: 600">
              ¥{{ record.amount.toFixed(2) }}
            </span>
          </template>
          <template v-if="column.key === 'paymentMethod'">
            <a-tag :color="getPaymentMethodColor(record.paymentMethod)">
              {{ getPaymentMethodText(record.paymentMethod) }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { collectionApi } from '@/api'

const loading = ref(false)
const cleaning = ref(false)

const collections = ref<any[]>([])
const searchRoomId = ref('')

const collectionColumns = [
  { title: '收款编号', dataIndex: 'id', key: 'id' },
  { title: '房间号', dataIndex: 'roomNumber', key: 'roomNumber' },
  { title: '收款金额', dataIndex: 'amount', key: 'amount' },
  { title: '支付方式', dataIndex: 'paymentMethod', key: 'paymentMethod' },
  { title: '收款人', dataIndex: 'operator', key: 'operator' },
  { title: '收款时间', dataIndex: 'collectedAt', key: 'collectedAt' },
  { title: '备注', dataIndex: 'note', key: 'note' }
]

onMounted(() => {
  loadCollections()
})

const loadCollections = async () => {
  loading.value = true
  try {
    const data = await collectionApi.getList()
    collections.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载收款记录失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const searchCollections = async () => {
  if (!searchRoomId.value.trim()) {
    await loadCollections()
    return
  }

  loading.value = true
  try {
    const data = await collectionApi.getByRoom(searchRoomId.value)
    collections.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('查询失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const cleanup = async () => {
  cleaning.value = true
  try {
    await collectionApi.cleanup()
    message.success('清理完成')
    await loadCollections()
  } catch (error) {
    message.error('清理失败')
    console.error(error)
  } finally {
    cleaning.value = false
  }
}

const getPaymentMethodColor = (method: string) => {
  const colors: Record<string, string> = {
    CASH: 'green',
    WECHAT: 'blue',
    ALIPAY: 'cyan',
    CARD: 'orange'
  }
  return colors[method] || 'default'
}

const getPaymentMethodText = (method: string) => {
  const texts: Record<string, string> = {
    CASH: '现金',
    WECHAT: '微信',
    ALIPAY: '支付宝',
    CARD: '银行卡'
  }
  return texts[method] || method
}
</script>

<style scoped>
.collection-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>
