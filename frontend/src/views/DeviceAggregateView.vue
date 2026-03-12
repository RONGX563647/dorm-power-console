<template>
  <div class="device-aggregate">
    <!-- 顶部工具栏 -->
    <div class="top-toolbar">
      <div class="toolbar-left">
        <a-select 
          v-model:value="currentDevice" 
          placeholder="选择设备" 
          style="width: 200px"
          @change="refreshData"
        >
          <a-select-option v-for="device in devices" :key="device.id" :value="device.id">
            {{ device.name }} ({{ device.room }})
          </a-select-option>
        </a-select>
        <a-space>
          <a-button :type="timeRange === '60s' ? 'primary' : 'default'" @click="setTimeRange('60s')">60s</a-button>
          <a-button :type="timeRange === '24h' ? 'primary' : 'default'" @click="setTimeRange('24h')">24h</a-button>
          <a-button :type="timeRange === '7d' ? 'primary' : 'default'" @click="setTimeRange('7d')">7d</a-button>
        </a-space>
      </div>
      <div class="toolbar-right">
        <span style="color: #1a1a1a; margin-right: 12px">实时刷新</span>
        <a-switch v-model:checked="autoRefresh" />
        <span style="color: #1a1a1a; margin: 0 12px">最后更新: {{ lastUpdateTime }}</span>
        <a-button @click="refreshData">
          <template #icon><ReloadOutlined /></template>
          手动刷新
        </a-button>
      </div>
    </div>

    <!-- 状态提示 -->
    <div class="status-alert">
      <CheckCircleOutlined style="color: #52c41a; margin-right: 8px" />
      暂无高风险异常，系统处于稳定状态
    </div>

    <!-- 基本信息 -->
    <div class="basic-info">
      <div class="info-item">
        <span class="info-label">宿舍总功率</span>
        <span class="info-value">{{ totalPower.toFixed(1) }} W</span>
      </div>
      <div class="info-item">
        <span class="info-label">在线设备</span>
        <span class="info-value">{{ onlineCount }}/{{ totalDevices }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">当前设备</span>
        <span class="info-value">{{ currentDevice ? devices.find(d => d.id === currentDevice)?.name : '-' }}</span>
      </div>
    </div>

    <!-- 主要内容 -->
    <a-row :gutter="16">
      <!-- 左侧内容 -->
      <a-col :span="16">
        <!-- 宿舍负载分布 -->
        <a-card title="宿舍负载分布">
          <div class="load-distribution">
            <div class="load-item" v-for="device in devices" :key="device.id">
              <span class="load-name">{{ device.name }}</span>
              <div class="load-bar">
                <div class="load-fill" :style="{ width: device.id === currentDevice ? loadPercentage + '%' : '0%' }"></div>
              </div>
              <span class="load-value">{{ device.id === currentDevice ? currentPower.toFixed(1) : '0.0' }} W</span>
            </div>
          </div>
        </a-card>

        <!-- 统计卡片 -->
        <a-card style="margin-top: 16px">
          <a-row :gutter="16">
            <a-col :span="6">
              <div class="stat-card">
                <div class="stat-icon">
                  <ThunderboltOutlined />
                </div>
                <div class="stat-content">
                  <div class="stat-value">{{ totalPower.toFixed(1) }} W</div>
                  <div class="stat-label">当前总功率</div>
                  <div class="stat-desc">对比近 60s 均值</div>
                </div>
              </div>
            </a-col>
            <a-col :span="6">
              <div class="stat-card">
                <div class="stat-icon">
                  <ClockCircleOutlined />
                </div>
                <div class="stat-content">
                  <div class="stat-value">{{ todayEnergy.toFixed(3) }} kWh</div>
                  <div class="stat-label">今日电量</div>
                  <div class="stat-desc">演示估算</div>
                </div>
              </div>
            </a-col>
            <a-col :span="6">
              <div class="stat-card">
                <div class="stat-icon">
                  <WifiOutlined />
                </div>
                <div class="stat-content">
                  <div class="stat-value">{{ onlineCount }}/{{ totalDevices }}</div>
                  <div class="stat-label">在线设备</div>
                  <div class="stat-desc">A-302 设备在线率</div>
                </div>
              </div>
            </a-col>
            <a-col :span="6">
              <div class="stat-card">
                <div class="stat-icon">
                  <BellOutlined />
                </div>
                <div class="stat-content">
                  <div class="stat-value">{{ alertCount }}</div>
                  <div class="stat-label">告警(24h)</div>
                  <div class="stat-desc">高功率阈值告警</div>
                </div>
              </div>
            </a-col>
          </a-row>
        </a-card>
      </a-col>

      <!-- 右侧内容 -->
      <a-col :span="8">
        <!-- 安全与策略 -->
        <a-card title="安全与策略">
          <div class="security-section">
            <div class="security-item">
              <span>违规负载检测</span>
              <a-switch v-model:checked="securityRules.loadDetection" />
            </div>
            <div class="security-item">
              <span>待机自动断电</span>
              <a-switch v-model:checked="securityRules.standbyPowerOff" />
            </div>

            <div class="rule-section">
              <h4>策略规则</h4>
              <div class="rule-item">
                <div>
                  <span>工作日 23:30 熄灯策略</span>
                  <span class="rule-scope">A 栋全体宿舍</span>
                </div>
                <a-switch v-model:checked="securityRules.lightsOut" />
              </div>
              <div class="rule-item">
                <div>
                  <span>待机功率 &lt; 5W 自动断电</span>
                  <span class="rule-scope">非路由插孔</span>
                </div>
                <a-switch v-model:checked="securityRules.standbyPowerLimit" />
              </div>
              <div class="rule-item">
                <div>
                  <span>违规负载瞬时切断</span>
                  <span class="rule-scope">高危设备指纹</span>
                </div>
                <a-switch v-model:checked="securityRules.instantCutoff" />
              </div>
            </div>

            <div class="simulation-buttons">
              <a-button type="primary" @click="simulateAlarm">模拟告警</a-button>
              <a-button @click="simulateOffline">模拟离线</a-button>
            </div>
          </div>
        </a-card>

        <!-- 高功率插孔和离线时长 -->
        <a-card style="margin-top: 16px">
          <a-row :gutter="16">
            <a-col :span="12">
              <div class="info-card">
                <div class="info-icon">
                  <FireOutlined />
                </div>
                <div class="info-content">
                  <div class="info-value">{{ highPowerSockets }}</div>
                  <div class="info-label">高功率插孔</div>
                  <div class="info-desc">阈值 &gt; 80W</div>
                </div>
              </div>
            </a-col>
            <a-col :span="12">
              <div class="info-card">
                <div class="info-icon">
                  <DisconnectOutlined />
                </div>
                <div class="info-content">
                  <div class="info-value">{{ offlineDuration }}</div>
                  <div class="info-label">离线时长</div>
                  <div class="info-desc">当前设备</div>
                </div>
              </div>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  CheckCircleOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  WifiOutlined,
  BellOutlined,
  FireOutlined,
  DisconnectOutlined
} from '@ant-design/icons-vue'
import { deviceApi, alertApi } from '@/api'
import type { Device } from '@/types'

// 响应式数据
const timeRange = ref('60s')
const autoRefresh = ref(true)
const lastUpdateTime = ref('')
const totalPower = ref(0.0)
const currentPower = ref(0.0)
const onlineCount = ref(0)
const totalDevices = ref(0)
const currentDevice = ref('')
const loadPercentage = ref(0)
const todayEnergy = ref(0.0)
const alertCount = ref(0)
const highPowerSockets = ref(0)
const offlineDuration = ref('0 min')

// 设备数据
const devices = ref<Device[]>([])
const deviceOptions = ref<any[]>([])

// 安全规则
const securityRules = ref({
  loadDetection: true,
  standbyPowerOff: true,
  lightsOut: true,
  standbyPowerLimit: true,
  instantCutoff: false
})

// 方法
const setTimeRange = (range: string) => {
  timeRange.value = range
  refreshData()
}

const loadDevices = async () => {
  try {
    const data = await deviceApi.getDevices()
    devices.value = Array.isArray(data) ? data : []
    deviceOptions.value = devices.value.map(d => ({
      label: `${d.name} (${d.room})`,
      value: d.id
    }))
    
    if (devices.value.length > 0) {
      currentDevice.value = devices.value[0].id
      totalDevices.value = devices.value.length
      await refreshData()
    }
  } catch (error: any) {
    message.error('加载设备失败')
    console.error(error)
  }
}

const refreshData = async () => {
  if (!currentDevice.value) return
  
  try {
    // 获取设备状态
    const status = await deviceApi.getDeviceStatus(currentDevice.value)
    currentPower.value = status.total_power_w || 0
    onlineCount.value = devices.value.filter(d => d.online).length
    
    // 计算负载百分比
    loadPercentage.value = Math.min(100, (currentPower.value / 2000) * 100)
    
    // 计算总功率
    totalPower.value = devices.value.reduce((sum, d) => {
      const deviceStatus = d.id === currentDevice.value ? status : null
      return sum + (deviceStatus?.total_power_w || 0)
    }, 0)
    
    // 更新时间
    lastUpdateTime.value = new Date().toLocaleTimeString()
  } catch (error: any) {
    console.error('刷新数据失败:', error)
  }
}

const simulateAlarm = async () => {
  try {
    await alertApi.simulateAlarm(currentDevice.value, 'power', 1500)
    message.success('模拟告警已触发')
    alertCount.value++
  } catch (error: any) {
    message.error('模拟告警失败')
    console.error(error)
  }
}

const simulateOffline = () => {
  onlineCount.value = Math.max(0, onlineCount.value - 1)
  message.warning('模拟设备离线')
}

// 生命周期
onMounted(() => {
  loadDevices()
  
  const interval = setInterval(() => {
    if (autoRefresh.value) {
      refreshData()
    }
  }, 5000)
  
  onUnmounted(() => {
    clearInterval(interval)
  })
})
</script>

<style scoped>
.device-aggregate {
  padding: 24px;
}

.top-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px;
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
  border-radius: 8px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-alert {
  background: rgba(82, 196, 26, 0.1);
  border: 1px solid rgba(82, 196, 26, 0.3);
  border-radius: 4px;
  padding: 12px 16px;
  margin-bottom: 16px;
  color: #52c41a;
  display: flex;
  align-items: center;
}

.basic-info {
  display: flex;
  gap: 32px;
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  color: #1a1a1a;
  font-size: 14px;
}

.info-value {
  color: #1a1a1a;
  font-size: 24px;
  font-weight: 600;
}

.load-distribution {
  padding: 16px 0;
}

.load-item {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.load-name {
  color: #1a1a1a;
  width: 150px;
}

.load-bar {
  flex: 1;
  height: 8px;
  background: rgba(0, 212, 255, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

.load-fill {
  height: 100%;
  background: linear-gradient(90deg, #00d4ff, #0080ff);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.load-value {
  color: #00d4ff;
  font-weight: 500;
  width: 80px;
  text-align: right;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: rgba(16, 24, 40, 0.4);
  border-radius: 8px;
  border: 1px solid rgba(0, 212, 255, 0.15);
}

.stat-icon {
  font-size: 24px;
  color: #00d4ff;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 212, 255, 0.1);
  border-radius: 8px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  color: #1a1a1a;
  font-size: 20px;
  font-weight: 600;
}

.stat-label {
  color: #1a1a1a;
  font-size: 14px;
  margin-top: 4px;
}

.stat-desc {
  color: #5a6a7a;
  font-size: 12px;
  margin-top: 2px;
}

.security-section {
  padding: 8px 0;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.rule-section {
  margin: 24px 0;
}

.rule-section h4 {
  color: #1a1a1a;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 600;
}

.rule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.rule-item div {
  display: flex;
  flex-direction: column;
}

.rule-scope {
  font-size: 12px;
  color: #1a1a1a;
  margin-top: 4px;
}

.simulation-buttons {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.info-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: rgba(16, 24, 40, 0.4);
  border-radius: 8px;
  border: 1px solid rgba(0, 212, 255, 0.15);
  text-align: center;
}

.info-icon {
  font-size: 20px;
  color: #faad14;
  margin-bottom: 8px;
}

.info-content {
  flex: 1;
}

:deep(.ant-card) {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
}

:deep(.ant-card-head) {
  color: #1a1a1a;
  border-bottom-color: rgba(0, 212, 255, 0.15);
}
</style>