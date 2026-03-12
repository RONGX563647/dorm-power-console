<template>
  <div class="mqtt-simulator-view">
    <!-- 欢迎卡片 -->
    <a-card
      :bordered="false"
      style="
        margin-bottom: 20px;
        background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
        color: white;
      "
    >
      <div style="display: flex; align-items: center; gap: 20px">
        <div style="font-size: 48px">
          <img
            src="@/assets/icons/antenna.svg"
            alt="MQTT"
            style="width: 48px; height: 48px; fill: white"
          />
        </div>
        <div>
          <h1 style="margin: 0; font-size: 28px; font-weight: 700">
            MQTT设备模拟器
          </h1>
          <p style="margin: 8px 0 0 0; opacity: 0.9">
            模拟物联网设备，测试系统性能与稳定性
          </p>
        </div>
      </div>
    </a-card>

    <a-row :gutter="20">
      <!-- 左侧：配置和控制 -->
      <a-col :xs="24" :lg="8">
        <!-- 配置卡片 -->
        <a-card :bordered="false" style="margin-bottom: 20px">
          <template #title>
            <span style="display: flex; align-items: center">
              <img
                src="@/assets/icons/chart-bar.svg"
                alt="Config"
                style="width: 20px; height: 20px; margin-right: 8px"
              />
              模拟配置
            </span>
          </template>
          <a-form :model="simulatorForm" layout="vertical">
            <a-form-item label="设备数量" required>
              <a-slider
                v-model:value="simulatorForm.devices"
                :min="1"
                :max="MAX_SIMULATOR_DEVICES"
                :marks="DEVICE_COUNT_MARKS"
                tooltip-placement="bottom"
              />
              <a-input-number
                v-model:value="simulatorForm.devices"
                :min="1"
                :max="MAX_SIMULATOR_DEVICES"
                style="width: 100%; margin-top: 8px"
              />
            </a-form-item>

            <a-form-item label="持续时间(秒)" required>
              <a-slider
                v-model:value="simulatorForm.duration"
                :min="1"
                :max="3600"
                :marks="{ 60: '1m', 300: '5m', 600: '10m', 3600: '1h' }"
                tooltip-placement="bottom"
              />
              <a-input-number
                v-model:value="simulatorForm.duration"
                :min="1"
                :max="3600"
                style="width: 100%; margin-top: 8px"
              />
            </a-form-item>

            <a-form-item label="发送间隔(秒)" required>
              <a-slider
                v-model:value="simulatorForm.interval"
                :min="0.01"
                :max="60"
                :step="0.01"
                :marks="{ 0.1: '0.1s', 1: '1s', 5: '5s', 60: '60s' }"
                tooltip-placement="bottom"
              />
              <a-input-number
                v-model:value="simulatorForm.interval"
                :min="0.01"
                :max="60"
                :step="0.01"
                style="width: 100%; margin-top: 8px"
              />
            </a-form-item>

            <a-form-item label="消息类型">
              <a-select v-model:value="simulatorForm.messageType">
                <a-select-option value="MIXED">
                  <span style="display: flex; align-items: center">
                    <img
                      src="@/assets/icons/chart-bar.svg"
                      alt="Mixed"
                      style="width: 16px; height: 16px; margin-right: 8px"
                    />
                    混合消息
                  </span>
                </a-select-option>
                <a-select-option value="STATUS">
                  <span style="display: flex; align-items: center">
                    <img
                      src="@/assets/icons/antenna.svg"
                      alt="Status"
                      style="width: 16px; height: 16px; margin-right: 8px"
                    />
                    状态消息
                  </span>
                </a-select-option>
                <a-select-option value="TELEMETRY">
                  <span style="display: flex; align-items: center">
                    <img
                      src="@/assets/icons/chart-line.svg"
                      alt="Telemetry"
                      style="width: 16px; height: 16px; margin-right: 8px"
                    />
                    遥测数据
                  </span>
                </a-select-option>
              </a-select>
            </a-form-item>

            <a-divider orientation="left">高级配置</a-divider>

            <a-form-item label="在线率">
              <a-slider
                v-model:value="simulatorForm.onlineRate"
                :min="0"
                :max="1"
                :step="0.01"
                tooltip-placement="bottom"
              />
              <span style="display: block; text-align: center; margin-top: 8px">
                {{ (simulatorForm.onlineRate * 100).toFixed(1) }}%
              </span>
            </a-form-item>

            <a-form-item label="功率范围(瓦)">
              <div style="margin-bottom: 8px">
                <span>最小: {{ simulatorForm.minPower }}W</span>
                <span style="float: right"
                  >最大: {{ simulatorForm.maxPower }}W</span
                >
              </div>
              <a-slider
                range
                v-model="powerRange"
                :min="0"
                :max="10000"
                :step="10"
                :tooltip-placement="'bottom'"
              />
              <div style="display: flex; gap: 8px; margin-top: 8px">
                <a-input-number
                  v-model:value="simulatorForm.minPower"
                  :min="0"
                  :max="simulatorForm.maxPower - 10"
                  placeholder="最小功率"
                />
                <a-input-number
                  v-model:value="simulatorForm.maxPower"
                  :min="simulatorForm.minPower + 10"
                  :max="10000"
                  placeholder="最大功率"
                />
              </div>
            </a-form-item>

            <a-form-item label="详细监控">
              <a-switch
                v-model:checked="simulatorForm.enableDetailedMonitoring"
                :disabled="shouldUseSummaryMonitoring(simulatorForm.devices)"
              />
              <span
                style="margin-left: 8px; color: var(--color-text-secondary)"
                >{{
                  shouldUseSummaryMonitoring(simulatorForm.devices)
                    ? "设备规模过大时自动切换为摘要监控"
                    : "启用详细性能监控"
                }}</span
              >
            </a-form-item>

            <a-alert
              v-if="shouldUseSummaryMonitoring(simulatorForm.devices)"
              type="warning"
              show-icon
              style="margin-bottom: 16px"
              message="当前配置会启用摘要监控"
              :description="predictedSummaryMonitoringDescription"
            />

            <a-form-item>
              <a-button
                type="primary"
                block
                size="large"
                @click="startSimulator"
                :loading="loading"
                style="height: 48px; font-size: 16px"
              >
                <template #icon><PlayCircleOutlined /></template>
                启动模拟
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <!-- 右侧：监控和统计 -->
      <a-col :xs="24" :lg="16">
        <!-- 实时监控卡片 -->
        <a-card
          :bordered="false"
          style="margin-bottom: 20px"
          v-if="simulatorStatus.taskId"
        >
          <template #title>
            <span style="display: flex; align-items: center">
              <img
                src="@/assets/icons/chart-line.svg"
                alt="Monitoring"
                style="width: 20px; height: 20px; margin-right: 8px"
              />
              实时监控
            </span>
          </template>
          <a-alert
            v-if="simulatorStatus.summaryOnly"
            type="warning"
            show-icon
            style="margin-bottom: 16px"
            message="大规模任务已自动切换为摘要监控模式"
            :description="summaryMonitoringDescription"
          />
          <a-row :gutter="16">
            <a-col :span="8">
              <a-statistic
                title="总消息数"
                :value="simulatorStatus.totalMessages"
                :value-style="{ color: '#1890ff', fontSize: '28px' }"
              />
            </a-col>
            <a-col :span="8">
              <a-statistic
                title="成功率"
                :value="simulatorStatus.successRate.toFixed(2)"
                suffix="%"
                :value-style="{
                  color:
                    simulatorStatus.successRate > 95 ? '#52c41a' : '#faad14',
                  fontSize: '28px',
                }"
              />
            </a-col>
            <a-col :span="8">
              <a-statistic
                title="错误数"
                :value="simulatorStatus.errorMessages"
                :value-style="{ color: '#ff4d4f', fontSize: '28px' }"
              />
            </a-col>
          </a-row>

          <a-divider />

          <a-row :gutter="16">
            <a-col :span="12">
              <a-statistic
                title="平均间隔"
                :value="simulatorStatus.avgSendInterval.toFixed(2)"
                suffix="ms"
                :value-style="{ fontSize: '20px' }"
              />
            </a-col>
            <a-col :span="12">
              <a-statistic
                :title="
                  simulatorStatus.summaryOnly ? '在线率(当前批次)' : '在线率'
                "
                :value="simulatorStatus.onlineRate.toFixed(2)"
                suffix="%"
                :value-style="{ fontSize: '20px' }"
              />
            </a-col>
          </a-row>

          <a-divider />

          <div style="margin-bottom: 16px">
            <div
              style="
                display: flex;
                justify-content: space-between;
                margin-bottom: 8px;
              "
            >
              <span>CPU使用率</span>
              <span>{{ simulatorStatus.cpuUsage.toFixed(2) }}%</span>
            </div>
            <a-progress
              :percent="simulatorStatus.cpuUsage"
              :stroke-color="getProgressColor(simulatorStatus.cpuUsage)"
              status="active"
            />
          </div>

          <div>
            <div
              style="
                display: flex;
                justify-content: space-between;
                margin-bottom: 8px;
              "
            >
              <span>内存使用</span>
              <span>{{ formatBytes(simulatorStatus.memoryUsage) }}</span>
            </div>
            <a-progress
              :percent="
                Math.min(
                  100,
                  (simulatorStatus.memoryUsage / (512 * 1024 * 1024)) * 100,
                )
              "
              :stroke-color="
                getProgressColor(
                  (simulatorStatus.memoryUsage / (512 * 1024 * 1024)) * 100,
                )
              "
              status="active"
            />
          </div>

          <a-divider />

          <div style="text-align: center; margin-top: 16px">
            <a-button
              type="danger"
              @click="stopSimulator"
              v-if="simulatorStatus.status === 'RUNNING'"
              size="large"
            >
              <template #icon><StopOutlined /></template>
              停止模拟
            </a-button>
          </div>
        </a-card>

        <!-- 任务列表卡片 -->
        <a-card :bordered="false">
          <template #title>
            <span style="display: flex; align-items: center">
              <img
                src="@/assets/icons/database.svg"
                alt="Tasks"
                style="width: 20px; height: 20px; margin-right: 8px"
              />
              任务列表
            </span>
          </template>
          <a-table
            :columns="columns"
            :data-source="simulatorTasks"
            row-key="taskId"
            :pagination="{ pageSize: 10, showSizeChanger: true }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-space>
                  <a-tag :color="getStatusColor(record.status)">
                    {{ record.status }}
                  </a-tag>
                  <a-tag v-if="record.summaryOnly" color="gold">
                    摘要模式
                  </a-tag>
                </a-space>
              </template>
              <template v-else-if="column.key === 'successRate'">
                <a-progress
                  :percent="record.successRate"
                  :stroke-color="getProgressColor(record.successRate)"
                  :show-info="false"
                  size="small"
                />
                <span
                  style="font-size: 12px; color: var(--color-text-secondary)"
                >
                  {{ record.successRate.toFixed(1) }}%
                </span>
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-space>
                  <a-button
                    type="link"
                    size="small"
                    @click="viewDetails(record.taskId)"
                  >
                    详情
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="exportData(record.taskId)"
                  >
                    导出
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <!-- 详情模态框 -->
    <a-modal v-model:open="detailModalVisible" width="800px" :footer="null">
      <template #title>
        <span style="display: flex; align-items: center">
          <img
            src="@/assets/icons/chart-bar.svg"
            alt="Task Detail"
            style="width: 20px; height: 20px; margin-right: 8px"
          />
          任务详情
        </span>
      </template>
      <div v-if="selectedTask">
        <a-descriptions bordered :column="2">
          <a-descriptions-item label="任务ID">{{
            selectedTask.taskId
          }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="getStatusColor(selectedTask.status)">{{
              selectedTask.status
            }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="设备数">{{
            selectedTask.devices
          }}</a-descriptions-item>
          <a-descriptions-item label="持续时间"
            >{{ selectedTask.duration }}秒</a-descriptions-item
          >
          <a-descriptions-item label="消息类型">{{
            selectedTask.messageType
          }}</a-descriptions-item>
          <a-descriptions-item label="平均间隔"
            >{{
              selectedTask.avgSendInterval.toFixed(2)
            }}ms</a-descriptions-item
          >
          <a-descriptions-item label="最大间隔"
            >{{ selectedTask.maxSendInterval }}ms</a-descriptions-item
          >
          <a-descriptions-item label="最小间隔"
            >{{ selectedTask.minSendInterval }}ms</a-descriptions-item
          >
          <a-descriptions-item label="总消息数">{{
            selectedTask.totalMessages
          }}</a-descriptions-item>
          <a-descriptions-item label="成功数">{{
            selectedTask.successMessages
          }}</a-descriptions-item>
          <a-descriptions-item label="成功率"
            >{{ selectedTask.successRate.toFixed(2) }}%</a-descriptions-item
          >
          <a-descriptions-item label="错误数">{{
            selectedTask.errorMessages
          }}</a-descriptions-item>
          <a-descriptions-item label="平均功率"
            >{{ selectedTask.avgPower.toFixed(2) }}W</a-descriptions-item
          >
          <a-descriptions-item label="最大功率"
            >{{ selectedTask.maxPower }}W</a-descriptions-item
          >
          <a-descriptions-item label="最小功率"
            >{{ selectedTask.minPower }}W</a-descriptions-item
          >
          <a-descriptions-item label="运行时间">{{
            formatDuration(selectedTask.runtime)
          }}</a-descriptions-item>
          <a-descriptions-item label="CPU使用率"
            >{{ selectedTask.cpuUsage.toFixed(2) }}%</a-descriptions-item
          >
          <a-descriptions-item label="内存使用">{{
            formatBytes(selectedTask.memoryUsage)
          }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, onUnmounted, watch } from "vue";
import { message, notification } from "ant-design-vue";
import { PlayCircleOutlined, StopOutlined } from "@ant-design/icons-vue";
import { simulatorApi } from "@/api";

const MAX_SIMULATOR_DEVICES = 1_000_000;
const SUMMARY_MONITORING_THRESHOLD = 50_000;
const FAST_POLLING_MS = 2_000;
const MEDIUM_POLLING_MS = 5_000;
const SLOW_POLLING_MS = 10_000;
const DEVICE_COUNT_MARKS: Record<number, string> = {
  1: "1",
  1000: "1千",
  100000: "10万",
  1000000: "100万",
};

// 模拟器配置表单
const simulatorForm = reactive({
  devices: 50,
  duration: 300,
  interval: 1.0,
  messageType: "MIXED",
  enableDetailedMonitoring: false,
  minPower: 0,
  maxPower: 200,
  minVoltage: 210,
  maxVoltage: 240,
  onlineRate: 0.95,
  deviceNamePrefix: "模拟设备",
  roomStart: 101,
  roomEnd: 1000,
  enableHeartbeat: true,
  heartbeatInterval: 60,
});

// 功率范围数组，用于滑块
const powerRange = ref([simulatorForm.minPower, simulatorForm.maxPower]);

// 加载状态
const loading = ref(false);

// 模拟器状态
const simulatorStatus = ref({
  taskId: "",
  status: "",
  devices: 0,
  duration: 0,
  interval: 0,
  totalMessages: 0,
  successMessages: 0,
  errorMessages: 0,
  successRate: 0,
  avgSendInterval: 0,
  maxSendInterval: 0,
  minSendInterval: 0,
  onlineRate: 0,
  avgPower: 0,
  maxPower: 0,
  minPower: 0,
  cpuUsage: 0,
  memoryUsage: 0,
  devicesPerCycle: 0,
  monitoringMode: "REALTIME",
  recommendedPollingIntervalMs: FAST_POLLING_MS,
  summaryOnly: false,
  message: "",
});

const shouldUseSummaryMonitoring = (devices: number) =>
  devices > SUMMARY_MONITORING_THRESHOLD;

const getRecommendedPollingInterval = (devices: number) => {
  if (devices > 200_000) return SLOW_POLLING_MS;
  if (devices > SUMMARY_MONITORING_THRESHOLD) return MEDIUM_POLLING_MS;
  return FAST_POLLING_MS;
};

const summaryMonitoringDescription = computed(() => {
  const devicesPerCycle = simulatorStatus.value.devicesPerCycle || 0;
  const pollingInterval =
    simulatorStatus.value.recommendedPollingIntervalMs ||
    getRecommendedPollingInterval(
      simulatorStatus.value.devices || simulatorForm.devices,
    );
  return `当前仅保留摘要指标展示，每轮最多处理 ${devicesPerCycle.toLocaleString()} 台设备，前端轮询已调整为 ${Math.round(
    pollingInterval / 1000,
  )} 秒一次，以避免设备数量过多时实时监控页面和状态接口同时被压垮。`;
});

const predictedSummaryMonitoringDescription = computed(() => {
  const devicesPerCycle = Math.min(
    simulatorForm.devices,
    Math.max(1, SUMMARY_MONITORING_THRESHOLD / 5),
    10_000,
  );
  const pollingInterval = getRecommendedPollingInterval(simulatorForm.devices);
  return `启动后会自动关闭详细监控，并以摘要模式展示当前批次指标。后端每轮最多处理 ${devicesPerCycle.toLocaleString()} 台设备，前端将按 ${Math.round(
    pollingInterval / 1000,
  )} 秒频率轮询。`;
});

// 模拟器任务列表
const simulatorTasks = ref<any[]>([]);

// 详情模态框
const detailModalVisible = ref(false);
const selectedTask = ref<any>(null);

// 表格列定义
const columns = [
  {
    title: "任务ID",
    dataIndex: "taskId",
    key: "taskId",
    ellipsis: true,
    width: 200,
  },
  {
    title: "状态",
    dataIndex: "status",
    key: "status",
    width: 100,
  },
  {
    title: "设备数",
    dataIndex: "devices",
    key: "devices",
    width: 80,
  },
  {
    title: "消息数",
    dataIndex: "totalMessages",
    key: "totalMessages",
    width: 100,
  },
  {
    title: "成功率",
    dataIndex: "successRate",
    key: "successRate",
    width: 120,
  },
  {
    title: "运行时间",
    dataIndex: "runtime",
    key: "runtime",
    width: 100,
    customRender: (text: number) => formatDuration(text),
  },
  {
    title: "操作",
    key: "actions",
    width: 150,
  },
];

// 确保 minPower 和 maxPower 的有效性
watch(
  () => simulatorForm.minPower,
  (newVal) => {
    if (newVal >= simulatorForm.maxPower) {
      simulatorForm.maxPower = newVal + 10;
    }
    powerRange.value = [simulatorForm.minPower, simulatorForm.maxPower];
  },
);

watch(
  () => simulatorForm.maxPower,
  (newVal) => {
    if (newVal <= simulatorForm.minPower) {
      simulatorForm.minPower = newVal - 10;
      if (simulatorForm.minPower < 0) {
        simulatorForm.minPower = 0;
      }
    }
    powerRange.value = [simulatorForm.minPower, simulatorForm.maxPower];
  },
);

// 监听功率范围变化，同步到表单
watch(
  powerRange,
  (newRange) => {
    simulatorForm.minPower = newRange[0];
    simulatorForm.maxPower = newRange[1];
  },
  { deep: true },
);

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    RUNNING: "blue",
    COMPLETED: "green",
    STOPPED: "orange",
    ERROR: "red",
    NOT_FOUND: "default",
  };
  return colors[status] || "default";
};

// 获取进度条颜色
const getProgressColor = (value: number) => {
  if (value < 50) return "#52c41a";
  if (value < 80) return "#faad14";
  return "#ff4d4f";
};

// 格式化字节数
const formatBytes = (bytes: number) => {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i];
};

// 格式化持续时间
const formatDuration = (ms: number) => {
  if (isNaN(ms) || ms < 0) return "0s";
  if (ms < 1000) return ms + "ms";
  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return seconds + "s";
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return minutes + "m " + remainingSeconds + "s";
};

// 启动模拟器
const startSimulator = async () => {
  try {
    loading.value = true;
    if (shouldUseSummaryMonitoring(simulatorForm.devices)) {
      simulatorForm.enableDetailedMonitoring = false;
    }
    const response = await simulatorApi.startSimulator(simulatorForm);
    if (response.status === "STARTED") {
      message.success(response.message);
      simulatorStatus.value.taskId = response.taskId;
      simulatorStatus.value.summaryOnly = shouldUseSummaryMonitoring(
        simulatorForm.devices,
      );
      simulatorStatus.value.recommendedPollingIntervalMs =
        getRecommendedPollingInterval(simulatorForm.devices);
      // 开始轮询状态
      startPolling();
    } else {
      message.error(response.message || "模拟器启动失败");
    }
  } catch (error: any) {
    message.error("模拟器启动失败: " + (error.message || "未知错误"));
  } finally {
    loading.value = false;
  }
};

// 停止模拟器
const stopSimulator = async () => {
  if (!simulatorStatus.value.taskId) {
    message.error("没有运行中的模拟器任务");
    return;
  }

  try {
    loading.value = true;
    const response = await simulatorApi.stopSimulator(
      simulatorStatus.value.taskId,
    );
    if (response.status === "STOPPED" || response.status === "COMPLETED") {
      message.success("模拟器已停止");
      stopPolling();
      await refreshTasks();
    } else {
      message.error(response.message || "停止失败");
    }
  } catch (error: any) {
    message.error("停止失败: " + (error.message || "未知错误"));
  } finally {
    loading.value = false;
  }
};

// 查看详情
const viewDetails = async (taskId: string) => {
  try {
    const status = await simulatorApi.getSimulatorStatus(taskId);
    selectedTask.value = status;
    detailModalVisible.value = true;
  } catch (error: any) {
    message.error("获取详情失败: " + (error.message || "未知错误"));
  }
};

// 导出数据
const exportData = async (taskId: string) => {
  try {
    const status = await simulatorApi.getSimulatorStatus(taskId);
    const dataStr = JSON.stringify(status, null, 2);
    const dataUri =
      "data:application/json;charset=utf-8," + encodeURIComponent(dataStr);

    const exportFileDefaultName = `mqtt_simulator_${taskId}_${Date.now()}.json`;

    const linkElement = document.createElement("a");
    linkElement.setAttribute("href", dataUri);
    linkElement.setAttribute("download", exportFileDefaultName);
    linkElement.click();

    message.success("数据已导出");
  } catch (error: any) {
    message.error("导出失败: " + (error.message || "未知错误"));
  }
};

// 获取模拟器状态
const refreshStatus = async () => {
  if (!simulatorStatus.value.taskId) return;

  try {
    const status = await simulatorApi.getSimulatorStatus(
      simulatorStatus.value.taskId,
    );
    if (status.status === "NOT_FOUND") {
      // 任务可能已完成
      stopPolling();
      await refreshTasks();
      return;
    }

    Object.assign(simulatorStatus.value, status);

    const recommendedPollingInterval =
      status.recommendedPollingIntervalMs ||
      getRecommendedPollingInterval(status.devices || simulatorForm.devices);
    if (pollingIntervalMs.value !== recommendedPollingInterval) {
      pollingIntervalMs.value = recommendedPollingInterval;
      startPolling();
      return;
    }

    // 成功率达到阈值时显示通知
    if (
      !status.summaryOnly &&
      status.successRate > 99 &&
      status.successRate !== 100
    ) {
      notification.success({
        message: "高成功率",
        description: `当前成功率: ${status.successRate.toFixed(2)}%`,
        duration: 3,
      });
    }
  } catch (error) {
    console.error("刷新状态失败:", error);
  }
};

// 刷新任务列表
const refreshTasks = async () => {
  try {
    const tasks = await simulatorApi.getAllSimulatorTasks();
    simulatorTasks.value = tasks;
  } catch (error) {
    console.error("刷新任务列表失败:", error);
  }
};

// 轮询管理
let pollingInterval: number | null = null;
const pollingIntervalMs = ref(FAST_POLLING_MS);
const taskRefreshDivider = computed(() =>
  simulatorStatus.value.summaryOnly ? 3 : 1,
);
let pollingTickCount = 0;

const startPolling = () => {
  stopPolling();
  pollingTickCount = 0;
  pollingInterval = window.setInterval(() => {
    refreshStatus();
    pollingTickCount += 1;
    if (pollingTickCount % taskRefreshDivider.value === 0) {
      refreshTasks();
    }
  }, pollingIntervalMs.value);
};

const stopPolling = () => {
  if (pollingInterval) {
    clearInterval(pollingInterval);
    pollingInterval = null;
  }
};

// 组件挂载
onMounted(() => {
  pollingIntervalMs.value = getRecommendedPollingInterval(
    simulatorForm.devices,
  );
  refreshTasks();
});

// 组件卸载
onUnmounted(() => {
  stopPolling();
});
</script>

<style scoped>
.mqtt-simulator-view {
  padding: 20px;
}

:deep(.ant-card) {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.ant-statistic-title) {
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
}

:deep(.ant-statistic-content) {
  font-weight: 700;
}

:deep(.ant-progress-inner) {
  border-radius: 4px;
}
</style>
