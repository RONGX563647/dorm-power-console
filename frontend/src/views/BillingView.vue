<template>
  <div class="billing-view">
    <div class="page-header">
      <h1>计费管理</h1>
    </div>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="price-rules" tab="电价规则">
        <a-card>
          <div class="filter-bar">
            <a-button type="primary" @click="showPriceRuleModal()">
              <template #icon><PlusOutlined /></template>
              新增规则
            </a-button>
          </div>

          <a-table
            :columns="priceRuleColumns"
            :data-source="priceRules"
            :loading="loading"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
            :row-key="(record: ElectricityPriceRule) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'type'">
                <a-tag :color="getPriceTypeColor(record.type)">
                  {{ getPriceTypeName(record.type) }}
                </a-tag>
              </template>
              <template v-if="column.key === 'enabled'">
                <a-switch :checked="record.enabled" @change="(checked: boolean) => togglePriceRule(record, checked)" />
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="showPriceRuleModal(record)">编辑</a-button>
                  <a-popconfirm title="确定删除此规则？" @confirm="deletePriceRule(record.id)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="bills" tab="账单管理">
        <a-card>
          <div class="filter-bar">
            <a-space>
              <a-input-search
                v-model:value="billRoomId"
                placeholder="输入房间ID"
                style="width: 200px"
                @search="loadBills"
              />
              <a-button @click="loadPendingBills">查看待缴费账单</a-button>
              <a-button type="primary" @click="showGenerateBillModal">
                生成账单
              </a-button>
            </a-space>
          </div>

          <a-table
            :columns="billColumns"
            :data-source="bills"
            :loading="billsLoading"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
            :row-key="(record: ElectricityBill) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="getBillStatusColor(record.status)">
                  {{ getBillStatusName(record.status) }}
                </a-tag>
              </template>
              <template v-if="column.key === 'amount'">
                <span style="color: #52c41a; font-weight: 500">¥{{ record.amount.toFixed(2) }}</span>
              </template>
              <template v-if="column.key === 'action'">
                <a-space v-if="record.status === 'PENDING'">
                  <a-button type="link" size="small" @click="showPayBillModal(record)">缴费</a-button>
                </a-space>
                <span v-else style="color: #1a1a1a">-</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="recharge" tab="充值管理">
        <a-card>
          <div class="filter-bar">
            <a-space>
              <a-input-search
                v-model:value="rechargeRoomId"
                placeholder="输入房间ID"
                style="width: 200px"
                @search="loadRechargeRecords"
              />
              <a-button type="primary" @click="showRechargeModal">
                充值
              </a-button>
            </a-space>
          </div>

          <a-row :gutter="16" style="margin-bottom: 16px" v-if="roomBalance">
            <a-col :span="8">
              <a-card size="small">
                <a-statistic title="当前余额" :value="roomBalance.balance" :precision="2" prefix="¥" />
              </a-card>
            </a-col>
          </a-row>

          <a-table
            :columns="rechargeColumns"
            :data-source="rechargeRecords"
            :loading="rechargeLoading"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
            :row-key="(record: RechargeRecord) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">
                <span style="color: #52c41a; font-weight: 500">+¥{{ record.amount.toFixed(2) }}</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="priceRuleModalVisible"
      :title="editingPriceRule ? '编辑电价规则' : '新增电价规则'"
      @ok="handlePriceRuleOk"
    >
      <a-form ref="priceRuleFormRef" :model="priceRuleForm" :rules="priceRuleRules" layout="vertical">
        <a-form-item label="规则名称" name="name">
          <a-input v-model:value="priceRuleForm.name" placeholder="请输入规则名称" />
        </a-form-item>
        <a-form-item label="电价(元/kWh)" name="pricePerKwh">
          <a-input-number v-model:value="priceRuleForm.pricePerKwh" :min="0" :step="0.01" style="width: 100%" />
        </a-form-item>
        <a-form-item label="类型" name="type">
          <a-select v-model:value="priceRuleForm.type">
            <a-select-option value="NORMAL">普通</a-select-option>
            <a-select-option value="PEAK">峰时</a-select-option>
            <a-select-option value="VALLEY">谷时</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="启用">
          <a-switch v-model:checked="priceRuleForm.enabled" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="generateBillModalVisible"
      title="生成月度账单"
      @ok="handleGenerateBill"
    >
      <a-form :model="generateBillForm" layout="vertical">
        <a-form-item label="房间ID" required>
          <a-input v-model:value="generateBillForm.roomId" placeholder="请输入房间ID" />
        </a-form-item>
        <a-form-item label="账单周期(YYYY-MM)" required>
          <a-input v-model:value="generateBillForm.period" placeholder="例如: 2024-01" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="payBillModalVisible"
      title="缴纳电费"
      @ok="handlePayBill"
    >
      <a-form :model="payBillForm" layout="vertical">
        <a-form-item label="支付方式" required>
          <a-select v-model:value="payBillForm.paymentMethod">
            <a-select-option value="CASH">现金</a-select-option>
            <a-select-option value="WECHAT">微信</a-select-option>
            <a-select-option value="ALIPAY">支付宝</a-select-option>
            <a-select-option value="CARD">银行卡</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="rechargeModalVisible"
      title="余额充值"
      @ok="handleRecharge"
    >
      <a-form :model="rechargeForm" layout="vertical">
        <a-form-item label="房间ID" required>
          <a-input v-model:value="rechargeForm.roomId" placeholder="请输入房间ID" />
        </a-form-item>
        <a-form-item label="充值金额" required>
          <a-input-number v-model:value="rechargeForm.amount" :min="0" :step="10" style="width: 100%" />
        </a-form-item>
        <a-form-item label="支付方式" required>
          <a-select v-model:value="rechargeForm.paymentMethod">
            <a-select-option value="CASH">现金</a-select-option>
            <a-select-option value="WECHAT">微信</a-select-option>
            <a-select-option value="ALIPAY">支付宝</a-select-option>
            <a-select-option value="CARD">银行卡</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { billingApi } from '@/api'
import type { ElectricityPriceRule, ElectricityBill, RechargeRecord, RoomBalance } from '@/types'

const activeTab = ref('price-rules')
const loading = ref(false)
const billsLoading = ref(false)
const rechargeLoading = ref(false)

const priceRules = ref<ElectricityPriceRule[]>([])
const bills = ref<ElectricityBill[]>([])
const rechargeRecords = ref<RechargeRecord[]>([])
const roomBalance = ref<RoomBalance | null>(null)

const billRoomId = ref('')
const rechargeRoomId = ref('')

const priceRuleColumns = [
  { title: '规则名称', dataIndex: 'name', key: 'name' },
  { title: '电价(元/kWh)', dataIndex: 'pricePerKwh', key: 'pricePerKwh' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '启用', dataIndex: 'enabled', key: 'enabled' },
  { title: '操作', key: 'action', width: 150 }
]

const billColumns = [
  { title: '账单ID', dataIndex: 'id', key: 'id' },
  { title: '房间ID', dataIndex: 'roomId', key: 'roomId' },
  { title: '账单周期', dataIndex: 'period', key: 'period' },
  { title: '用电量(kWh)', dataIndex: 'consumption', key: 'consumption' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', width: 100 }
]

const rechargeColumns = [
  { title: '记录ID', dataIndex: 'id', key: 'id' },
  { title: '房间ID', dataIndex: 'roomId', key: 'roomId' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '支付方式', dataIndex: 'paymentMethod', key: 'paymentMethod' },
  { title: '操作员', dataIndex: 'operator', key: 'operator' },
  { title: '时间', dataIndex: 'createdAt', key: 'createdAt' }
]

const priceRuleModalVisible = ref(false)
const editingPriceRule = ref<ElectricityPriceRule | null>(null)
const priceRuleFormRef = ref()
const priceRuleForm = ref<Partial<ElectricityPriceRule>>({
  name: '',
  pricePerKwh: 0.5,
  type: 'NORMAL',
  enabled: true
})
const priceRuleRules = {
  name: [{ required: true, message: '请输入规则名称' }],
  pricePerKwh: [{ required: true, message: '请输入电价' }]
}

const generateBillModalVisible = ref(false)
const generateBillForm = ref({ roomId: '', period: '' })

const payBillModalVisible = ref(false)
const payBillForm = ref({ billId: '', paymentMethod: 'WECHAT' })

const rechargeModalVisible = ref(false)
const rechargeForm = ref({ roomId: '', amount: 100, paymentMethod: 'WECHAT' })

const loadPriceRules = async () => {
  loading.value = true
  try {
    priceRules.value = await billingApi.getPriceRules()
  } catch (error: any) {
    message.error('加载电价规则失败')
  } finally {
    loading.value = false
  }
}

const loadBills = async () => {
  if (!billRoomId.value) {
    message.warning('请输入房间ID')
    return
  }
  billsLoading.value = true
  try {
    const result = await billingApi.getRoomBills(billRoomId.value)
    bills.value = result.content || []
  } catch (error: any) {
    message.error('加载账单失败')
  } finally {
    billsLoading.value = false
  }
}

const loadPendingBills = async () => {
  billsLoading.value = true
  try {
    bills.value = await billingApi.getPendingBills()
  } catch (error: any) {
    message.error('加载待缴费账单失败')
  } finally {
    billsLoading.value = false
  }
}

const loadRechargeRecords = async () => {
  if (!rechargeRoomId.value) {
    message.warning('请输入房间ID')
    return
  }
  rechargeLoading.value = true
  try {
    const [recordsResult, balanceResult] = await Promise.all([
      billingApi.getRechargeRecords(rechargeRoomId.value),
      billingApi.getRoomBalance(rechargeRoomId.value)
    ])
    rechargeRecords.value = recordsResult.content || []
    roomBalance.value = balanceResult
  } catch (error: any) {
    message.error('加载充值记录失败')
  } finally {
    rechargeLoading.value = false
  }
}

const showPriceRuleModal = (rule?: ElectricityPriceRule) => {
  editingPriceRule.value = rule || null
  priceRuleForm.value = rule ? { ...rule } : { name: '', pricePerKwh: 0.5, type: 'NORMAL', enabled: true }
  priceRuleModalVisible.value = true
}

const handlePriceRuleOk = async () => {
  try {
    await priceRuleFormRef.value?.validate()
    if (editingPriceRule.value) {
      await billingApi.updatePriceRule(editingPriceRule.value.id, priceRuleForm.value)
      message.success('更新成功')
    } else {
      await billingApi.createPriceRule(priceRuleForm.value)
      message.success('创建成功')
    }
    priceRuleModalVisible.value = false
    loadPriceRules()
  } catch (error: any) {
    message.error('操作失败')
  }
}

const togglePriceRule = async (rule: ElectricityPriceRule, enabled: boolean) => {
  try {
    await billingApi.updatePriceRule(rule.id, { enabled })
    message.success('更新成功')
    loadPriceRules()
  } catch (error: any) {
    message.error('更新失败')
  }
}

const deletePriceRule = async (id: string) => {
  try {
    await billingApi.deletePriceRule(id)
    message.success('删除成功')
    loadPriceRules()
  } catch (error: any) {
    message.error('删除失败')
  }
}

const showGenerateBillModal = () => {
  generateBillForm.value = { roomId: '', period: '' }
  generateBillModalVisible.value = true
}

const handleGenerateBill = async () => {
  if (!generateBillForm.value.roomId || !generateBillForm.value.period) {
    message.warning('请填写完整信息')
    return
  }
  try {
    await billingApi.generateBill(generateBillForm.value.roomId, generateBillForm.value.period)
    message.success('账单生成成功')
    generateBillModalVisible.value = false
    billRoomId.value = generateBillForm.value.roomId
    loadBills()
  } catch (error: any) {
    message.error('生成账单失败')
  }
}

const showPayBillModal = (bill: ElectricityBill) => {
  payBillForm.value = { billId: bill.id, paymentMethod: 'WECHAT' }
  payBillModalVisible.value = true
}

const handlePayBill = async () => {
  try {
    await billingApi.payBill(payBillForm.value.billId, payBillForm.value.paymentMethod)
    message.success('缴费成功')
    payBillModalVisible.value = false
    loadBills()
  } catch (error: any) {
    message.error('缴费失败')
  }
}

const showRechargeModal = () => {
  rechargeForm.value = { roomId: rechargeRoomId.value, amount: 100, paymentMethod: 'WECHAT' }
  rechargeModalVisible.value = true
}

const handleRecharge = async () => {
  if (!rechargeForm.value.roomId || !rechargeForm.value.amount) {
    message.warning('请填写完整信息')
    return
  }
  try {
    await billingApi.recharge(rechargeForm.value.roomId, rechargeForm.value.amount, rechargeForm.value.paymentMethod)
    message.success('充值成功')
    rechargeModalVisible.value = false
    loadRechargeRecords()
  } catch (error: any) {
    message.error('充值失败')
  }
}

const getPriceTypeName = (type: string) => {
  const map: Record<string, string> = { NORMAL: '普通', PEAK: '峰时', VALLEY: '谷时' }
  return map[type] || type
}

const getPriceTypeColor = (type: string) => {
  const map: Record<string, string> = { NORMAL: 'blue', PEAK: 'red', VALLEY: 'green' }
  return map[type] || 'default'
}

const getBillStatusName = (status: string) => {
  const map: Record<string, string> = { PENDING: '待缴费', PAID: '已缴费', OVERDUE: '已逾期' }
  return map[status] || status
}

const getBillStatusColor = (status: string) => {
  const map: Record<string, string> = { PENDING: 'orange', PAID: 'green', OVERDUE: 'red' }
  return map[status] || 'default'
}

onMounted(() => {
  loadPriceRules()
})
</script>

<style scoped>
.billing-view {
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

.filter-bar {
  margin-bottom: 16px;
}
</style>
