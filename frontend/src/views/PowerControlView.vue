<template>
  <div class="power-control-view">
    <a-page-header
      title="电源控制"
      sub-title="宿舍电源远程控制与管理"
      style="padding: 0 0 16px 0"
    />

    <a-row :gutter="16">
      <a-col :span="12">
        <a-card title="断电房间列表" :loading="loadingCutoff">
          <template #extra>
            <a-button @click="loadCutoffRooms">
              刷新
            </a-button>
          </template>

          <a-table
            :data-source="cutoffRooms"
            :columns="cutoffColumns"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag color="red">已断电</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-button
                  type="link"
                  size="small"
                  @click="restorePower(record.id)"
                  :loading="record.restoring"
                >
                  恢复供电
                </a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="12">
        <a-card title="欠费房间列表" :loading="loadingOverdue">
          <template #extra>
            <a-button @click="loadOverdueRooms">
              刷新
            </a-button>
          </template>

          <a-table
            :data-source="overdueRooms"
            :columns="overdueColumns"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'balance'">
                <span :style="{ color: record.balance < 0 ? 'red' : 'green' }">
                  ¥{{ record.balance.toFixed(2) }}
                </span>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button
                    type="link"
                    size="small"
                    @click="cutoffPower(record.id, '欠费断电')"
                    :loading="record.cutting"
                  >
                    断电
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="showRechargeModal(record)"
                  >
                    充值
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <a-card title="手动控制" style="margin-top: 16px">
      <a-form layout="inline">
        <a-form-item label="房间号">
          <a-select
            v-model:value="selectedRoom"
            placeholder="选择房间"
            style="width: 200px"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option v-for="room in allRooms" :key="room.id" :value="room.id">
              {{ room.building }} - {{ room.roomNumber }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="断电原因">
          <a-input
            v-model:value="cutoffReason"
            placeholder="请输入断电原因"
            style="width: 200px"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button
              type="primary"
              danger
              @click="cutoffPowerManual"
              :loading="cuttingOff"
            >
              断电
            </a-button>
            <a-button
              type="primary"
              @click="restorePowerManual"
              :loading="restoring"
            >
              恢复供电
            </a-button>
            <a-button @click="checkStatus" :loading="checkingStatus">
              查询状态
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-divider />

      <div v-if="roomStatus" style="margin-top: 16px">
        <a-descriptions title="房间电源状态" bordered :column="2">
          <a-descriptions-item label="房间号">
            {{ roomStatus.roomNumber }}
          </a-descriptions-item>
          <a-descriptions-item label="电源状态">
            <a-tag :color="roomStatus.powerOn ? 'green' : 'red'">
              {{ roomStatus.powerOn ? '供电中' : '已断电' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="当前功率">
            {{ roomStatus.currentPower }} W
          </a-descriptions-item>
          <a-descriptions-item label="今日用电">
            {{ roomStatus.todayUsage }} kWh
          </a-descriptions-item>
          <a-descriptions-item label="余额" v-if="roomStatus.balance !== undefined">
            <span :style="{ color: roomStatus.balance < 0 ? 'red' : 'green' }">
              ¥{{ roomStatus.balance.toFixed(2) }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="最后更新">
            {{ roomStatus.lastUpdate }}
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-card>

    <a-card title="批量操作" style="margin-top: 16px">
      <a-space direction="vertical" style="width: 100%">
        <a-alert
          message="批量操作将影响多个房间，请谨慎操作"
          type="warning"
          show-icon
        />
        <a-space>
          <a-button type="primary" danger @click="batchCutoff" :loading="batchCutting">
            批量断电（欠费房间）
          </a-button>
          <a-button type="primary" @click="batchRestore" :loading="batchRestoring">
            批量恢复供电（已缴费房间）
          </a-button>
        </a-space>
      </a-space>
    </a-card>

    <a-modal
      v-model:open="rechargeModalVisible"
      title="房间充值"
      @ok="handleRecharge"
      :confirm-loading="recharging"
    >
      <a-form :model="rechargeForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="房间号">
          <a-input :value="rechargeForm.roomNumber" disabled />
        </a-form-item>
        <a-form-item label="当前余额">
          <a-input :value="`¥${rechargeForm.balance.toFixed(2)}`" disabled />
        </a-form-item>
        <a-form-item label="充值金额" required>
          <a-input-number
            v-model:value="rechargeForm.amount"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入充值金额"
          />
        </a-form-item>
        <a-form-item label="支付方式">
          <a-select v-model:value="rechargeForm.paymentMethod">
            <a-select-option value="CASH">现金</a-select-option>
            <a-select-option value="WECHAT">微信</a-select-option>
            <a-select-option value="ALIPAY">支付宝</a-select-option>
            <a-select-option value="CARD">银行卡</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="rechargeForm.note" :rows="2" placeholder="备注信息" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { powerControlApi, dormApi, billingApi } from '@/api'

const loadingCutoff = ref(false)
const loadingOverdue = ref(false)
const cuttingOff = ref(false)
const restoring = ref(false)
const checkingStatus = ref(false)
const batchCutting = ref(false)
const batchRestoring = ref(false)
const recharging = ref(false)

const cutoffRooms = ref<any[]>([])
const overdueRooms = ref<any[]>([])
const allRooms = ref<any[]>([])
const selectedRoom = ref<string>()
const cutoffReason = ref('')
const roomStatus = ref<any>(null)

const rechargeModalVisible = ref(false)
const rechargeForm = ref({
  roomId: '',
  roomNumber: '',
  balance: 0,
  amount: 0,
  paymentMethod: 'WECHAT',
  note: ''
})

const cutoffColumns = [
  { title: '房间号', dataIndex: 'roomNumber', key: 'roomNumber' },
  { title: '楼栋', dataIndex: 'building', key: 'building' },
  { title: '断电时间', dataIndex: 'cutoffTime', key: 'cutoffTime' },
  { title: '断电原因', dataIndex: 'reason', key: 'reason' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action', width: 100 }
]

const overdueColumns = [
  { title: '房间号', dataIndex: 'roomNumber', key: 'roomNumber' },
  { title: '楼栋', dataIndex: 'building', key: 'building' },
  { title: '余额', dataIndex: 'balance', key: 'balance' },
  { title: '欠费天数', dataIndex: 'overdueDays', key: 'overdueDays' },
  { title: '操作', key: 'action', width: 150 }
]

onMounted(async () => {
  await loadAllRooms()
  await Promise.all([
    loadCutoffRooms(),
    loadOverdueRooms()
  ])
})

const loadAllRooms = async () => {
  try {
    const data = await dormApi.getRooms()
    allRooms.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载房间失败:', error)
  }
}

const loadCutoffRooms = async () => {
  loadingCutoff.value = true
  try {
    const data = await powerControlApi.getCutoffRooms()
    cutoffRooms.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载断电房间失败')
    console.error(error)
  } finally {
    loadingCutoff.value = false
  }
}

const loadOverdueRooms = async () => {
  loadingOverdue.value = true
  try {
    const data = await powerControlApi.getOverdueRooms()
    overdueRooms.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载欠费房间失败')
    console.error(error)
  } finally {
    loadingOverdue.value = false
  }
}

const filterOption = (input: string, option: any) => {
  const text = option.children?.[0]?.children || ''
  return text.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

const cutoffPower = async (roomId: string, reason: string) => {
  const room = overdueRooms.value.find(r => r.id === roomId)
  if (room) {
    room.cutting = true
  }

  try {
    await powerControlApi.cutoff(roomId, reason)
    message.success('断电成功')
    await Promise.all([loadCutoffRooms(), loadOverdueRooms()])
  } catch (error) {
    message.error('断电失败')
    console.error(error)
  } finally {
    if (room) {
      room.cutting = false
    }
  }
}

const restorePower = async (roomId: string) => {
  const room = cutoffRooms.value.find(r => r.id === roomId)
  if (room) {
    room.restoring = true
  }

  try {
    await powerControlApi.restore(roomId)
    message.success('恢复供电成功')
    await Promise.all([loadCutoffRooms(), loadOverdueRooms()])
  } catch (error) {
    message.error('恢复供电失败')
    console.error(error)
  } finally {
    if (room) {
      room.restoring = false
    }
  }
}

const cutoffPowerManual = async () => {
  if (!selectedRoom.value) {
    message.warning('请选择房间')
    return
  }

  cuttingOff.value = true
  try {
    await powerControlApi.cutoff(selectedRoom.value, cutoffReason.value || '手动断电')
    message.success('断电成功')
    await Promise.all([loadCutoffRooms(), loadOverdueRooms()])
    cutoffReason.value = ''
  } catch (error) {
    message.error('断电失败')
    console.error(error)
  } finally {
    cuttingOff.value = false
  }
}

const restorePowerManual = async () => {
  if (!selectedRoom.value) {
    message.warning('请选择房间')
    return
  }

  restoring.value = true
  try {
    await powerControlApi.restore(selectedRoom.value)
    message.success('恢复供电成功')
    await Promise.all([loadCutoffRooms(), loadOverdueRooms()])
  } catch (error) {
    message.error('恢复供电失败')
    console.error(error)
  } finally {
    restoring.value = false
  }
}

const checkStatus = async () => {
  if (!selectedRoom.value) {
    message.warning('请选择房间')
    return
  }

  checkingStatus.value = true
  try {
    const data = await powerControlApi.getStatus(selectedRoom.value)
    roomStatus.value = data
  } catch (error) {
    message.error('查询状态失败')
    console.error(error)
  } finally {
    checkingStatus.value = false
  }
}

const batchCutoff = () => {
  Modal.confirm({
    title: '确认批量断电',
    content: '确定要对所有欠费房间进行断电操作吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      batchCutting.value = true
      try {
        const promises = overdueRooms.value
          .filter(room => room.balance < 0)
          .map(room => powerControlApi.cutoff(room.id, '欠费断电'))
        
        await Promise.all(promises)
        message.success('批量断电成功')
        await Promise.all([loadCutoffRooms(), loadOverdueRooms()])
      } catch (error) {
        message.error('批量断电失败')
        console.error(error)
      } finally {
        batchCutting.value = false
      }
    }
  })
}

const batchRestore = () => {
  Modal.confirm({
    title: '确认批量恢复供电',
    content: '确定要对所有已缴费房间恢复供电吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      batchRestoring.value = true
      try {
        const promises = cutoffRooms.value
          .filter(room => room.balance >= 0)
          .map(room => powerControlApi.restore(room.id))
        
        await Promise.all(promises)
        message.success('批量恢复供电成功')
        await Promise.all([loadCutoffRooms(), loadOverdueRooms()])
      } catch (error) {
        message.error('批量恢复供电失败')
        console.error(error)
      } finally {
        batchRestoring.value = false
      }
    }
  })
}

const showRechargeModal = (room: any) => {
  rechargeForm.value = {
    roomId: room.id,
    roomNumber: room.roomNumber,
    balance: room.balance,
    amount: 0,
    paymentMethod: 'WECHAT',
    note: ''
  }
  rechargeModalVisible.value = true
}

const handleRecharge = async () => {
  if (rechargeForm.value.amount <= 0) {
    message.warning('请输入充值金额')
    return
  }

  recharging.value = true
  try {
    await billingApi.recharge(
      rechargeForm.value.roomId,
      rechargeForm.value.amount,
      rechargeForm.value.paymentMethod,
      rechargeForm.value.note
    )
    message.success('充值成功')
    rechargeModalVisible.value = false
    await Promise.all([loadCutoffRooms(), loadOverdueRooms()])
  } catch (error) {
    message.error('充值失败')
    console.error(error)
  } finally {
    recharging.value = false
  }
}
</script>

<style scoped>
.power-control-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>
