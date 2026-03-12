<template>
  <div class="agent-view">
    <a-page-header
      title="AI智能代理"
      sub-title="行为学习、异常检测与智能场景管理"
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
          <a-button type="primary" @click="analyzeRoom" :loading="analyzing">
            综合分析
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="behavior" tab="行为学习">
        <a-card title="用电行为分析">
          <a-space style="margin-bottom: 16px">
            <a-input-number
              v-model:value="behaviorDays"
              :min="1"
              :max="30"
              placeholder="学习天数"
              style="width: 120px"
            />
            <a-button type="primary" @click="learnBehavior" :loading="learning">
              开始学习
            </a-button>
            <a-button @click="getBehaviorSuggestions" :loading="loadingSuggestions">
              获取建议
            </a-button>
          </a-space>

          <a-row :gutter="16" style="margin-top: 16px">
            <a-col :span="12">
              <a-card size="small" title="行为预测">
                <a-input-number
                  v-model:value="predictHours"
                  :min="1"
                  :max="168"
                  placeholder="预测小时数"
                  style="width: 120px; margin-bottom: 8px"
                />
                <a-button @click="predictBehavior" :loading="predicting" style="margin-left: 8px">
                  预测
                </a-button>
                <div v-if="behaviorPrediction" style="margin-top: 16px">
                  <a-descriptions :column="1" size="small">
                    <a-descriptions-item label="预测功率">
                      {{ behaviorPrediction.predictedPower }} W
                    </a-descriptions-item>
                    <a-descriptions-item label="置信度">
                      {{ (behaviorPrediction.confidence * 100).toFixed(1) }}%
                    </a-descriptions-item>
                    <a-descriptions-item label="预测时段">
                      {{ behaviorPrediction.timeRange }}
                    </a-descriptions-item>
                  </a-descriptions>
                </div>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card size="small" title="行为建议">
                <a-list
                  v-if="behaviorSuggestions && behaviorSuggestions.length > 0"
                  :data-source="behaviorSuggestions"
                  size="small"
                >
                  <template #renderItem="{ item }">
                    <a-list-item>
                      <a-list-item-meta
                        :title="item.title"
                        :description="item.description"
                      />
                    </a-list-item>
                  </template>
                </a-list>
                <a-empty v-else description="暂无建议" />
              </a-card>
            </a-col>
          </a-row>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="anomaly" tab="异常检测">
        <a-card title="设备异常检测">
          <a-space style="margin-bottom: 16px">
            <a-select
              v-model:value="selectedDevice"
              placeholder="选择设备"
              style="width: 200px"
            >
              <a-select-option v-for="device in devices" :key="device.id" :value="device.id">
                {{ device.name }}
              </a-select-option>
            </a-select>
            <a-button type="primary" @click="detectAnomalyRealtime" :loading="detecting">
              实时检测
            </a-button>
            <a-button @click="detectAnomalyBatch" :loading="detectingBatch">
              批量检测
            </a-button>
          </a-space>

          <a-row :gutter="16" style="margin-top: 16px">
            <a-col :span="8">
              <a-card size="small" title="异常预测">
                <a-input-number
                  v-model:value="anomalyPredictHours"
                  :min="1"
                  :max="168"
                  placeholder="预测小时数"
                  style="width: 120px; margin-bottom: 8px"
                />
                <a-button @click="predictAnomaly" :loading="predictingAnomaly" style="margin-left: 8px">
                  预测
                </a-button>
                <div v-if="anomalyPrediction" style="margin-top: 16px">
                  <a-statistic
                    title="异常概率"
                    :value="anomalyPrediction.probability * 100"
                    suffix="%"
                    :value-style="{ color: anomalyPrediction.probability > 0.7 ? '#cf1322' : '#3f8600' }"
                  />
                </div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card size="small" title="设备健康度">
                <a-button @click="getDeviceHealth" :loading="loadingHealth" style="margin-bottom: 8px">
                  检查健康度
                </a-button>
                <div v-if="deviceHealth" style="margin-top: 16px">
                  <a-progress
                    type="circle"
                    :percent="deviceHealth.score * 100"
                    :status="deviceHealth.score > 0.7 ? 'success' : 'exception'"
                  />
                  <p style="margin-top: 8px">{{ deviceHealth.status }}</p>
                </div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card size="small" title="故障预测">
                <a-button @click="predictFailure" :loading="predictingFailure" style="margin-bottom: 8px">
                  预测故障
                </a-button>
                <div v-if="failurePrediction" style="margin-top: 16px">
                  <a-alert
                    :type="failurePrediction.willFail ? 'error' : 'success'"
                    :message="failurePrediction.message"
                    show-icon
                  />
                </div>
              </a-card>
            </a-col>
          </a-row>

          <a-card size="small" title="房间异常统计" style="margin-top: 16px">
            <a-button @click="getRoomAnomalies" :loading="loadingAnomalies" style="margin-bottom: 8px">
              获取统计
            </a-button>
            <a-table
              v-if="roomAnomalies"
              :data-source="roomAnomalies"
              :columns="anomalyColumns"
              :pagination="false"
              size="small"
            />
          </a-card>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="scene" tab="场景管理">
        <a-card title="智能场景">
          <a-space style="margin-bottom: 16px">
            <a-button type="primary" @click="autoGenerateScene" :loading="generatingScene">
              自动生成场景
            </a-button>
            <a-button @click="getRoomScenes" :loading="loadingScenes">
              刷新场景列表
            </a-button>
          </a-space>

          <a-table
            :data-source="scenes"
            :columns="sceneColumns"
            :loading="loadingScenes"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="executeScene(record.id)" :loading="record.executing">
                    执行
                  </a-button>
                  <a-button type="link" size="small" @click="toggleScene(record.id)">
                    {{ record.enabled ? '禁用' : '启用' }}
                  </a-button>
                  <a-button type="link" size="small" @click="optimizeScene(record.id)" :loading="record.optimizing">
                    优化
                  </a-button>
                  <a-button type="link" size="small" @click="showSceneHistory(record.id)">
                    历史
                  </a-button>
                  <a-popconfirm
                    title="确定删除此场景吗？"
                    @confirm="deleteScene(record.id)"
                  >
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
              <template v-if="column.key === 'enabled'">
                <a-tag :color="record.enabled ? 'green' : 'red'">
                  {{ record.enabled ? '已启用' : '已禁用' }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="sceneHistoryVisible"
      title="场景执行历史"
      width="800px"
      :footer="null"
    >
      <a-table
        :data-source="sceneHistory"
        :columns="historyColumns"
        :loading="loadingHistory"
        :pagination="{ pageSize: 10 }"
        size="small"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { agentApi, dormApi, deviceApi } from '@/api'

const activeTab = ref('behavior')
const selectedRoom = ref<string>()
const selectedDevice = ref<string>()
const rooms = ref<any[]>([])
const devices = ref<any[]>([])

const behaviorDays = ref(7)
const predictHours = ref(24)
const anomalyPredictHours = ref(24)

const learning = ref(false)
const loadingSuggestions = ref(false)
const predicting = ref(false)
const detecting = ref(false)
const detectingBatch = ref(false)
const predictingAnomaly = ref(false)
const loadingHealth = ref(false)
const predictingFailure = ref(false)
const loadingAnomalies = ref(false)
const generatingScene = ref(false)
const loadingScenes = ref(false)
const analyzing = ref(false)
const loadingHistory = ref(false)

const behaviorPrediction = ref<any>(null)
const behaviorSuggestions = ref<any[]>([])
const anomalyPrediction = ref<any>(null)
const deviceHealth = ref<any>(null)
const failurePrediction = ref<any>(null)
const roomAnomalies = ref<any[]>([])
const scenes = ref<any[]>([])
const sceneHistory = ref<any[]>([])
const sceneHistoryVisible = ref(false)

const anomalyColumns = [
  { title: '设备ID', dataIndex: 'deviceId', key: 'deviceId' },
  { title: '异常类型', dataIndex: 'type', key: 'type' },
  { title: '严重程度', dataIndex: 'severity', key: 'severity' },
  { title: '检测时间', dataIndex: 'detectedAt', key: 'detectedAt' }
]

const sceneColumns = [
  { title: '场景名称', dataIndex: 'name', key: 'name' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '状态', dataIndex: 'enabled', key: 'enabled' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 300 }
]

const historyColumns = [
  { title: '执行时间', dataIndex: 'executedAt', key: 'executedAt' },
  { title: '执行结果', dataIndex: 'result', key: 'result' },
  { title: '耗时(ms)', dataIndex: 'duration', key: 'duration' },
  { title: '备注', dataIndex: 'note', key: 'note' }
]

onMounted(async () => {
  await loadRooms()
  await loadDevices()
})

const loadRooms = async () => {
  try {
    const data = await dormApi.getRooms()
    rooms.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载房间失败:', error)
  }
}

const loadDevices = async () => {
  try {
    const data = await deviceApi.getDevices()
    devices.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载设备失败:', error)
  }
}

const handleRoomChange = () => {
  behaviorPrediction.value = null
  behaviorSuggestions.value = []
  anomalyPrediction.value = null
  deviceHealth.value = null
  failurePrediction.value = null
  roomAnomalies.value = []
  scenes.value = []
}

const learnBehavior = async () => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }
  
  learning.value = true
  try {
    const result = await agentApi.learnBehavior(selectedRoom.value, behaviorDays.value)
    message.success('行为学习完成')
    console.log('学习结果:', result)
  } catch (error) {
    message.error('行为学习失败')
    console.error(error)
  } finally {
    learning.value = false
  }
}

const getBehaviorSuggestions = async () => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }
  
  loadingSuggestions.value = true
  try {
    const result = await agentApi.getBehaviorSuggestions(selectedRoom.value)
    behaviorSuggestions.value = Array.isArray(result) ? result : []
  } catch (error) {
    message.error('获取建议失败')
    console.error(error)
  } finally {
    loadingSuggestions.value = false
  }
}

const predictBehavior = async () => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }
  
  predicting.value = true
  try {
    const result = await agentApi.predictBehavior(selectedRoom.value, predictHours.value)
    behaviorPrediction.value = result
  } catch (error) {
    message.error('行为预测失败')
    console.error(error)
  } finally {
    predicting.value = false
  }
}

const detectAnomalyRealtime = async () => {
  if (!selectedDevice.value) {
    message.warning('请先选择设备')
    return
  }
  
  detecting.value = true
  try {
    const result = await agentApi.detectAnomalyRealtime(selectedDevice.value, {})
    message.success('实时检测完成')
    console.log('检测结果:', result)
  } catch (error) {
    message.error('实时检测失败')
    console.error(error)
  } finally {
    detecting.value = false
  }
}

const detectAnomalyBatch = async () => {
  if (!selectedDevice.value) {
    message.warning('请先选择设备')
    return
  }
  
  detectingBatch.value = true
  try {
    const result = await agentApi.detectAnomalyBatch(selectedDevice.value, 24)
    message.success('批量检测完成')
    console.log('批量检测结果:', result)
  } catch (error) {
    message.error('批量检测失败')
    console.error(error)
  } finally {
    detectingBatch.value = false
  }
}

const predictAnomaly = async () => {
  if (!selectedDevice.value) {
    message.warning('请先选择设备')
    return
  }
  
  predictingAnomaly.value = true
  try {
    const result = await agentApi.predictAnomaly(selectedDevice.value, anomalyPredictHours.value)
    anomalyPrediction.value = result
  } catch (error) {
    message.error('异常预测失败')
    console.error(error)
  } finally {
    predictingAnomaly.value = false
  }
}

const getDeviceHealth = async () => {
  if (!selectedDevice.value) {
    message.warning('请先选择设备')
    return
  }
  
  loadingHealth.value = true
  try {
    const result = await agentApi.getDeviceHealth(selectedDevice.value)
    deviceHealth.value = result
  } catch (error) {
    message.error('获取健康度失败')
    console.error(error)
  } finally {
    loadingHealth.value = false
  }
}

const predictFailure = async () => {
  if (!selectedDevice.value) {
    message.warning('请先选择设备')
    return
  }
  
  predictingFailure.value = true
  try {
    const result = await agentApi.predictFailure(selectedDevice.value)
    failurePrediction.value = result
  } catch (error) {
    message.error('故障预测失败')
    console.error(error)
  } finally {
    predictingFailure.value = false
  }
}

const getRoomAnomalies = async () => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }
  
  loadingAnomalies.value = true
  try {
    const result = await agentApi.getRoomAnomalies(selectedRoom.value)
    roomAnomalies.value = Array.isArray(result) ? result : []
  } catch (error) {
    message.error('获取异常统计失败')
    console.error(error)
  } finally {
    loadingAnomalies.value = false
  }
}

const autoGenerateScene = async () => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }
  
  generatingScene.value = true
  try {
    await agentApi.autoGenerateScene(selectedRoom.value)
    message.success('场景生成成功')
    await getRoomScenes()
  } catch (error) {
    message.error('场景生成失败')
    console.error(error)
  } finally {
    generatingScene.value = false
  }
}

const getRoomScenes = async () => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }
  
  loadingScenes.value = true
  try {
    const result = await agentApi.getRoomScenes(selectedRoom.value)
    scenes.value = Array.isArray(result) ? result : []
  } catch (error) {
    message.error('获取场景列表失败')
    console.error(error)
  } finally {
    loadingScenes.value = false
  }
}

const executeScene = async (sceneId: string) => {
  const scene = scenes.value.find(s => s.id === sceneId)
  if (scene) {
    scene.executing = true
  }
  
  try {
    await agentApi.executeScene(sceneId)
    message.success('场景执行成功')
  } catch (error) {
    message.error('场景执行失败')
    console.error(error)
  } finally {
    if (scene) {
      scene.executing = false
    }
  }
}

const toggleScene = async (sceneId: string) => {
  try {
    await agentApi.toggleScene(sceneId)
    message.success('场景状态已更新')
    await getRoomScenes()
  } catch (error) {
    message.error('更新场景状态失败')
    console.error(error)
  }
}

const optimizeScene = async (sceneId: string) => {
  const scene = scenes.value.find(s => s.id === sceneId)
  if (scene) {
    scene.optimizing = true
  }
  
  try {
    await agentApi.optimizeScene(sceneId)
    message.success('场景优化成功')
  } catch (error) {
    message.error('场景优化失败')
    console.error(error)
  } finally {
    if (scene) {
      scene.optimizing = false
    }
  }
}

const showSceneHistory = async (sceneId: string) => {
  sceneHistoryVisible.value = true
  loadingHistory.value = true
  
  try {
    const result = await agentApi.getSceneHistory(sceneId, 30)
    sceneHistory.value = Array.isArray(result) ? result : []
  } catch (error) {
    message.error('获取场景历史失败')
    console.error(error)
  } finally {
    loadingHistory.value = false
  }
}

const deleteScene = async (sceneId: string) => {
  try {
    await agentApi.deleteScene(sceneId)
    message.success('场景已删除')
    await getRoomScenes()
  } catch (error) {
    message.error('删除场景失败')
    console.error(error)
  }
}

const analyzeRoom = async () => {
  if (!selectedRoom.value) {
    message.warning('请先选择房间')
    return
  }
  
  analyzing.value = true
  try {
    const result = await agentApi.analyzeRoom(selectedRoom.value)
    message.success('综合分析完成')
    console.log('分析结果:', result)
  } catch (error) {
    message.error('综合分析失败')
    console.error(error)
  } finally {
    analyzing.value = false
  }
}
</script>

<style scoped>
.agent-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>
