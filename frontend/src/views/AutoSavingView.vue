<template>
  <div class="auto-saving-view">
    <a-page-header
      title="智能节能管理"
      sub-title="用电预测、节能策略与自动节能控制"
      style="padding: 0 0 16px 0"
    >
      <template #extra>
        <a-space>
          <a-select
            v-model:value="selectedRoom"
            placeholder="选择房间"
            style="width: 200px"
            @change="handleRoomChange"
          >
            <a-select-option v-for="room in rooms" :key="room.id" :value="room.id">
              {{ room.building }} - {{ room.roomNumber }}
            </a-select-option>
          </a-select>
        </a-space>
      </template>
    </a-page-header>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="prediction" tab="用电预测">
        <a-card title="用电量预测">
          <a-space style="margin-bottom: 16px">
            <a-input-number
              v-model:value="predictionDays"
              :min="1"
              :max="30"
              placeholder="预测天数"
              style="width: 120px"
            />
            <a-button type="primary" @click="predictPower" :loading="predicting">
              开始预测
            </a-button>
          </a-space>

          <div v-if="powerPrediction" style="margin-top: 16px">
            <a-card size="small" title="预测结果">
              <a-descriptions :column="2" size="small">
                <a-descriptions-item label="预测日用电量">
                  {{ powerPrediction.predictedDailyKwh.toFixed(2) }} kWh
                </a-descriptions-item>
                <a-descriptions-item label="节能潜力">
                  {{ (powerPrediction.savingsPotential * 100).toFixed(1) }}%
                </a-descriptions-item>
                <a-descriptions-item label="预测开始时间">
                  {{ powerPrediction.startTime }}
                </a-descriptions-item>
                <a-descriptions-item label="预测结束时间">
                  {{ powerPrediction.endTime }}
                </a-descriptions-item>
              </a-descriptions>
            </a-card>

            <a-card size="small" title="24小时预测" style="margin-top: 16px">
              <a-button @click="getHourlyPrediction" :loading="loadingHourly" style="margin-bottom: 16px">
                获取小时预测
              </a-button>
              <div v-if="hourlyPredictions && hourlyPredictions.length > 0">
                <a-table
                  :data-source="hourlyPredictions"
                  :columns="hourlyColumns"
                  :pagination="false"
                  size="small"
                />
              </div>
            </a-card>
          </div>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="strategies" tab="节能策略">
        <a-card title="节能策略">
          <a-button type="primary" @click="getStrategies" :loading="loadingStrategies" style="margin-bottom: 16px">
            生成策略
          </a-button>

          <a-table
            v-if="strategies && strategies.length > 0"
            :data-source="strategies"
            :columns="strategyColumns"
            :pagination="false"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'">
                <a-button type="link" size="small" @click="executeStrategy(record.id)" :loading="executingStrategy === record.id">
                  执行
                </a-button>
              </template>
              <template v-if="column.key === 'priority'">
                <a-tag :color="getPriorityColor(record.priority)">
                  {{ getPriorityText(record.priority) }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="auto" tab="自动节能">
        <a-card title="自动节能控制">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card size="small" title="自动节能状态">
                <a-button type="primary" @click="getAutoSavingStatus" :loading="loadingStatus" style="margin-bottom: 16px">
                  刷新状态
                </a-button>
                <div v-if="autoSavingStatus">
                  <a-descriptions :column="1" size="small">
                    <a-descriptions-item label="状态">
                      <a-tag :color="autoSavingStatus.enabled ? 'green' : 'red'">
                        {{ autoSavingStatus.enabled ? '已开启' : '已关闭' }}
                      </a-tag>
                    </a-descriptions-item>
                    <a-descriptions-item label="说明">
                      {{ autoSavingStatus.message }}
                    </a-descriptions-item>
                  </a-descriptions>
                  <a-button 
                    type="primary" 
                    :loading="toggling" 
                    style="margin-top: 16px"
                    @click="toggleAutoSaving(!autoSavingStatus.enabled)"
                  >
                    {{ autoSavingStatus.enabled ? '关闭' : '开启' }}自动节能
                  </a-button>
                </div>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card size="small" title="用电阈值设置">
                <a-input-number
                  v-model:value="threshold"
                  :min="0.1"
                  :max="10"
                  :step="0.1"
                  placeholder="日用电量阈值(kWh)"
                  style="width: 100%; margin-bottom: 16px"
                />
                <a-button type="primary" @click="setThreshold" :loading="settingThreshold">
                  设置阈值
                </a-button>
              </a-card>
            </a-col>
          </a-row>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="stats" tab="节能统计">
        <a-card title="节能效果统计">
          <a-space style="margin-bottom: 16px">
            <a-button type="primary" @click="getSavingStats" :loading="loadingStats">
              获取房间统计
            </a-button>
            <a-button @click="getAllSavingStats" :loading="loadingAllStats">
              获取全局统计
            </a-button>
          </a-space>

          <div v-if="savingStats" style="margin-top: 16px">
            <a-card size="small" title="房间节能统计">
              <a-descriptions :column="2" size="small">
                <a-descriptions-item label="总节省电量">
                  {{ savingStats.totalSavedKwh.sum().toFixed(2) }} kWh
                </a-descriptions-item>
                <a-descriptions-item label="执行节能操作次数">
                  {{ savingStats.executedActions.get() }} 次
                </a-descriptions-item>
                <a-descriptions-item label="最近一次操作">
                  {{ savingStats.lastActionTime || '暂无' }}
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </div>

          <div v-if="allSavingStats" style="margin-top: 16px">
            <a-card size="small" title="全局节能统计">
              <a-descriptions :column="2" size="small">
                <a-descriptions-item label="统计房间数">
                  {{ allSavingStats.summary.totalRooms }} 间
                </a-descriptions-item>
                <a-descriptions-item label="累计节省电量">
                  {{ allSavingStats.summary.totalSavedKwh.toFixed(2) }} kWh
                </a-descriptions-item>
                <a-descriptions-item label="累计执行操作">
                  {{ allSavingStats.summary.totalActions }} 次
                </a-descriptions-item>
                <a-descriptions-item label="说明">
                  {{ allSavingStats.summary.message }}
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </div>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="report" tab="节能报告">
        <a-card title="节能分析报告">
          <a-space style="margin-bottom: 16px">
            <a-input-number
              v-model:value="reportDays"
              :min="1"
              :max="30"
              placeholder="分析天数"
              style="width: 120px"
            />
            <a-button type="primary" @click="generateReport" :loading="generatingReport">
              生成报告
            </a-button>
          </a-space>

          <div v-if="report" style="margin-top: 16px">
            <a-card size="small" title="节能分析报告">
              <a-descriptions :column="1" size="small">
                <a-descriptions-item label="房间ID">
                  {{ report.roomId }}
                </a-descriptions-item>
                <a-descriptions-item label="预测日用电量">
                  {{ report.prediction.predictedDailyKwh.toFixed(2) }} kWh
                </a-descriptions-item>
                <a-descriptions-item label="节能策略数量">
                  {{ report.strategies.length }} 个
                </a-descriptions-item>
                <a-descriptions-item label="节能建议">
                  <a-list size="small">
                    <a-list-item v-for="(recommendation, index) in report.recommendations" :key="index">
                      {{ recommendation }}
                    </a-list-item>
                  </a-list>
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </div>
        </a-card>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { autoSavingApi, dormApi } from '@/api'

const activeTab = ref('prediction')
const selectedRoom = ref<string>()
const rooms = ref<any[]>([])

const predictionDays = ref(7)
const reportDays = ref(7)
const threshold = ref(2.0)

const predicting = ref(false)
const loadingHourly = ref(false)
const loadingStrategies = ref(false)
const loadingStatus = ref(false)
const toggling = ref(false)
const settingThreshold = ref(false)
const loadingStats = ref(false)
const loadingAllStats = ref(false)
const generatingReport = ref(false)
const executingStrategy = ref<string | null>(null)

const powerPrediction = ref<any>(null)
const hourlyPredictions = ref<any[]>([])
const strategies = ref<any[]>([])
const autoSavingStatus = ref<any>(null)
const savingStats = ref<any>(null)
const allSavingStats = ref<any>(null)
const report = ref<any>(null)

const hourlyColumns = [
  { title: '小时', dataIndex: 'hour', key: 'hour' },
  { title: '预测功率(W)', dataIndex: 'predictedPower', key: 'predictedPower' },
  { title: '置信度(%)', dataIndex: 'confidence', key: 'confidence' }
]

const strategyColumns = [
  { title: '策略名称', dataIndex: 'name', key: 'name' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '优先级', dataIndex: 'priority', key: 'priority' },
  { title: '节能效果', dataIndex: 'savingPercentage', key: 'savingPercentage' },
  { title: '操作', key: 'action', width: 100 }
]

onMounted(async () => {
  await loadRooms()
  await getAutoSavingStatus()
})

const loadRooms = async () => {
  try {
    const data = await dormApi.getRooms()
    rooms.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载房间失败:', error)
  }
}

const handleRoomChange = () => {
  powerPrediction.value = null
  hourlyPredictions.value = []
  strategies.value = []
  savingStats.value = null
  report.value = null
}

const predictPower = async () => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }

  predicting.value = true
  try {
    const result = await autoSavingApi.predictPower(selectedRoom.value, predictionDays.value)
    powerPrediction.value = result
  } catch (error) {
    console.error('预测用电量失败:', error)
    message.error('预测失败，请稍后再试')
  } finally {
    predicting.value = false
  }
}

const getHourlyPrediction = async () => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }

  loadingHourly.value = true
  try {
    const result = await autoSavingApi.getHourlyPrediction(selectedRoom.value)
    hourlyPredictions.value = Array.isArray(result) ? result : []
  } catch (error) {
    console.error('获取小时预测失败:', error)
    message.error('获取失败，请稍后再试')
  } finally {
    loadingHourly.value = false
  }
}

const getStrategies = async () => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }

  loadingStrategies.value = true
  try {
    const result = await autoSavingApi.getStrategies(selectedRoom.value)
    strategies.value = Array.isArray(result) ? result : []
  } catch (error) {
    console.error('生成节能策略失败:', error)
    message.error('生成失败，请稍后再试')
  } finally {
    loadingStrategies.value = false
  }
}

const executeStrategy = async (strategyId: string) => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }

  executingStrategy.value = strategyId
  try {
    await autoSavingApi.executeStrategy(selectedRoom.value, strategyId)
    message.success('策略执行成功')
  } catch (error) {
    console.error('执行策略失败:', error)
    message.error('执行失败，请稍后再试')
  } finally {
    executingStrategy.value = null
  }
}

const getAutoSavingStatus = async () => {
  loadingStatus.value = true
  try {
    const result = await autoSavingApi.getAutoSavingStatus()
    autoSavingStatus.value = result
  } catch (error) {
    console.error('获取自动节能状态失败:', error)
    message.error('获取失败，请稍后再试')
  } finally {
    loadingStatus.value = false
  }
}

const toggleAutoSaving = async (enabled: boolean) => {
  toggling.value = true
  try {
    const result = await autoSavingApi.toggleAutoSaving(enabled)
    autoSavingStatus.value = result
    message.success(result.message)
  } catch (error) {
    console.error('切换自动节能状态失败:', error)
    message.error('切换失败，请稍后再试')
  } finally {
    toggling.value = false
  }
}

const setThreshold = async () => {
  settingThreshold.value = true
  try {
    const result = await autoSavingApi.setThreshold(threshold.value)
    message.success(result.message)
  } catch (error) {
    console.error('设置阈值失败:', error)
    message.error('设置失败，请稍后再试')
  } finally {
    settingThreshold.value = false
  }
}

const getSavingStats = async () => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }

  loadingStats.value = true
  try {
    const result = await autoSavingApi.getSavingStats(selectedRoom.value)
    savingStats.value = result
  } catch (error) {
    console.error('获取节能统计失败:', error)
    message.error('获取失败，请稍后再试')
  } finally {
    loadingStats.value = false
  }
}

const getAllSavingStats = async () => {
  loadingAllStats.value = true
  try {
    const result = await autoSavingApi.getAllSavingStats()
    allSavingStats.value = result
  } catch (error) {
    console.error('获取全局节能统计失败:', error)
    message.error('获取失败，请稍后再试')
  } finally {
    loadingAllStats.value = false
  }
}

const generateReport = async () => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }

  generatingReport.value = true
  try {
    const result = await autoSavingApi.generateReport(selectedRoom.value, reportDays.value)
    report.value = result
  } catch (error) {
    console.error('生成节能报告失败:', error)
    message.error('生成失败，请稍后再试')
  } finally {
    generatingReport.value = false
  }
}

const getPriorityColor = (priority: number) => {
  switch (priority) {
    case 1:
      return 'red'
    case 2:
      return 'orange'
    case 3:
      return 'blue'
    default:
      return 'default'
  }
}

const getPriorityText = (priority: number) => {
  switch (priority) {
    case 1:
      return '高'
    case 2:
      return '中'
    case 3:
      return '低'
    default:
      return '未知'
  }
}
</script>

<style scoped>
.auto-saving-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>