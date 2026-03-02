<template>
  <div class="system-config-view">
    <a-page-header
      title="系统配置管理"
      sub-title="管理系统配置参数和初始化设置"
      :back-icon="false"
    />

    <div class="content-wrapper">
      <!-- 配置分类标签页 -->
      <a-card class="config-card">
        <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
          <a-tab-pane key="all" tab="全部配置">
            <ConfigTable
              :configs="filteredConfigs"
              :loading="loading"
              @edit="handleEdit"
              @refresh="loadConfigs"
            />
          </a-tab-pane>
          <a-tab-pane key="system" tab="系统配置">
            <ConfigTable
              :configs="filteredConfigs"
              :loading="loading"
              @edit="handleEdit"
              @refresh="loadConfigs"
            />
          </a-tab-pane>
          <a-tab-pane key="device" tab="设备配置">
            <ConfigTable
              :configs="filteredConfigs"
              :loading="loading"
              @edit="handleEdit"
              @refresh="loadConfigs"
            />
          </a-tab-pane>
          <a-tab-pane key="alert" tab="告警配置">
            <ConfigTable
              :configs="filteredConfigs"
              :loading="loading"
              @edit="handleEdit"
              @refresh="loadConfigs"
            />
          </a-tab-pane>
        </a-tabs>

        <!-- 操作按钮 -->
        <div class="action-bar">
          <a-space>
            <a-button type="primary" @click="handleInitDefault">
              <ReloadOutlined />
              初始化默认配置
            </a-button>
            <a-button @click="loadConfigs">
              <ReloadOutlined />
              刷新
            </a-button>
          </a-space>
        </div>
      </a-card>
    </div>

    <!-- 编辑配置弹窗 -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑配置"
      @ok="handleSave"
      @cancel="editModalVisible = false"
      :confirm-loading="saveLoading"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="配置键">
          <a-input v-model:value="editForm.key" disabled />
        </a-form-item>
        <a-form-item label="配置值">
          <a-input v-model:value="editForm.value" placeholder="请输入配置值" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="editForm.description" :rows="3" disabled />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { systemConfigApi } from '@/api'
import type { SystemConfig } from '@/types'
import ConfigTable from '@/components/ConfigTable.vue'

const loading = ref(false)
const saveLoading = ref(false)
const configs = ref<SystemConfig[]>([])
const activeTab = ref('all')
const editModalVisible = ref(false)
const editForm = ref<Partial<SystemConfig>>({})

// 根据当前标签页过滤配置
const filteredConfigs = computed(() => {
  if (activeTab.value === 'all') {
    return configs.value
  }
  return configs.value.filter(config => config.category === activeTab.value)
})

// 加载配置列表
const loadConfigs = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'all') {
      configs.value = await systemConfigApi.getAllConfigs()
    } else {
      configs.value = await systemConfigApi.getConfigsByCategory(activeTab.value)
    }
  } catch (error) {
    message.error('加载配置失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 标签页切换
const handleTabChange = () => {
  loadConfigs()
}

// 编辑配置
const handleEdit = (config: SystemConfig) => {
  editForm.value = { ...config }
  editModalVisible.value = true
}

// 保存配置
const handleSave = async () => {
  if (!editForm.value.key || !editForm.value.value) {
    message.warning('请填写完整配置信息')
    return
  }

  saveLoading.value = true
  try {
    await systemConfigApi.updateConfig(editForm.value.key, editForm.value.value)
    message.success('配置更新成功')
    editModalVisible.value = false
    loadConfigs()
  } catch (error) {
    message.error('配置更新失败')
    console.error(error)
  } finally {
    saveLoading.value = false
  }
}

// 初始化默认配置
const handleInitDefault = async () => {
  try {
    await systemConfigApi.initDefaultConfigs()
    message.success('默认配置初始化成功')
    loadConfigs()
  } catch (error) {
    message.error('初始化失败')
    console.error(error)
  }
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped lang="scss">
.system-config-view {
  padding: 24px;

  .content-wrapper {
    margin-top: 16px;
  }

  .config-card {
    .action-bar {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #f0f0f0;
    }
  }
}
</style>
