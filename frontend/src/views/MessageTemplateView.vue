<template>
  <div class="message-template-view">
    <a-page-header
      title="消息模板"
      sub-title="消息通知模板管理"
      style="padding: 0 0 16px 0"
    >
      <template #extra>
        <a-space>
          <a-button @click="initTemplates" :loading="initializing">
            初始化默认模板
          </a-button>
          <a-button type="primary" @click="showTemplateModal()">
            新建模板
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-card>
      <a-space style="margin-bottom: 16px">
        <a-select
          v-model:value="filterType"
          placeholder="消息类型"
          style="width: 150px"
          allow-clear
          @change="loadTemplates"
        >
          <a-select-option value="ALERT">告警通知</a-select-option>
          <a-select-option value="BILLING">账单通知</a-select-option>
          <a-select-option value="SYSTEM">系统通知</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterChannel"
          placeholder="发送渠道"
          style="width: 150px"
          allow-clear
          @change="loadTemplates"
        >
          <a-select-option value="EMAIL">邮件</a-select-option>
          <a-select-option value="SMS">短信</a-select-option>
          <a-select-option value="PUSH">推送</a-select-option>
          <a-select-option value="WECHAT">微信</a-select-option>
        </a-select>
        <a-switch
          v-model:checked="showEnabledOnly"
          checked-children="仅启用"
          un-checked-children="全部"
          @change="loadTemplates"
        />
      </a-space>

      <a-table
        :data-source="templates"
        :columns="templateColumns"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="getTypeColor(record.type)">
              {{ getTypeText(record.type) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'channel'">
            <a-tag :color="getChannelColor(record.channel)">
              {{ getChannelText(record.channel) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'enabled'">
            <a-tag :color="record.enabled ? 'green' : 'red'">
              {{ record.enabled ? '已启用' : '已禁用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="previewTemplate(record)">
                预览
              </a-button>
              <a-button type="link" size="small" @click="showTemplateModal(record)">
                编辑
              </a-button>
              <a-popconfirm
                title="确定删除此模板吗？"
                @confirm="deleteTemplate(record.id)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="templateModalVisible"
      :title="editingTemplate ? '编辑模板' : '新建模板'"
      width="700px"
      @ok="handleTemplateSubmit"
      :confirm-loading="submitting"
    >
      <a-form :model="templateForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="模板代码" required>
          <a-input
            v-model:value="templateForm.templateCode"
            placeholder="请输入模板代码"
            :disabled="!!editingTemplate"
          />
        </a-form-item>
        <a-form-item label="模板名称" required>
          <a-input v-model:value="templateForm.name" placeholder="请输入模板名称" />
        </a-form-item>
        <a-form-item label="消息类型" required>
          <a-select v-model:value="templateForm.type" placeholder="请选择消息类型">
            <a-select-option value="ALERT">告警通知</a-select-option>
            <a-select-option value="BILLING">账单通知</a-select-option>
            <a-select-option value="SYSTEM">系统通知</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="发送渠道" required>
          <a-select v-model:value="templateForm.channel" placeholder="请选择发送渠道">
            <a-select-option value="EMAIL">邮件</a-select-option>
            <a-select-option value="SMS">短信</a-select-option>
            <a-select-option value="PUSH">推送</a-select-option>
            <a-select-option value="WECHAT">微信</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="模板标题" required>
          <a-input v-model:value="templateForm.subject" placeholder="请输入模板标题" />
        </a-form-item>
        <a-form-item label="模板内容" required>
          <a-textarea
            v-model:value="templateForm.content"
            placeholder="请输入模板内容，使用 ${变量名} 表示变量"
            :rows="6"
          />
          <div style="margin-top: 8px; color: #999; font-size: 12px">
            提示：使用 ${变量名} 格式定义变量，例如：${username}、${roomNumber}
          </div>
        </a-form-item>
        <a-form-item label="是否启用">
          <a-switch v-model:checked="templateForm.enabled" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="previewModalVisible"
      title="模板预览"
      width="600px"
      :footer="null"
    >
      <a-descriptions bordered :column="1">
        <a-descriptions-item label="模板名称">
          {{ previewData.name }}
        </a-descriptions-item>
        <a-descriptions-item label="模板标题">
          {{ previewData.subject }}
        </a-descriptions-item>
        <a-descriptions-item label="模板内容">
          <div style="white-space: pre-wrap">{{ previewData.content }}</div>
        </a-descriptions-item>
        <a-descriptions-item label="变量列表">
          <a-space wrap>
            <a-tag v-for="variable in previewData.variables" :key="variable" color="blue">
              {{ variable }}
            </a-tag>
            <span v-if="!previewData.variables || previewData.variables.length === 0">
              无变量
            </span>
          </a-space>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { messageTemplateApi } from '@/api'

const loading = ref(false)
const initializing = ref(false)
const submitting = ref(false)

const templates = ref<any[]>([])
const filterType = ref<string>()
const filterChannel = ref<string>()
const showEnabledOnly = ref(false)

const templateModalVisible = ref(false)
const previewModalVisible = ref(false)
const editingTemplate = ref<any>(null)
const previewData = ref<any>({})

const templateForm = ref({
  templateCode: '',
  name: '',
  type: 'SYSTEM',
  channel: 'EMAIL',
  subject: '',
  content: '',
  enabled: true
})

const templateColumns = [
  { title: '模板代码', dataIndex: 'templateCode', key: 'templateCode' },
  { title: '模板名称', dataIndex: 'name', key: 'name' },
  { title: '消息类型', dataIndex: 'type', key: 'type' },
  { title: '发送渠道', dataIndex: 'channel', key: 'channel' },
  { title: '状态', dataIndex: 'enabled', key: 'enabled' },
  { title: '操作', key: 'action', width: 200 }
]

onMounted(() => {
  loadTemplates()
})

const loadTemplates = async () => {
  loading.value = true
  try {
    let data
    if (showEnabledOnly.value) {
      data = await messageTemplateApi.getEnabledTemplates()
    } else if (filterType.value) {
      data = await messageTemplateApi.getTemplatesByType(filterType.value)
    } else if (filterChannel.value) {
      data = await messageTemplateApi.getTemplatesByChannel(filterChannel.value)
    } else {
      data = await messageTemplateApi.getEnabledTemplates()
    }
    templates.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载模板失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const showTemplateModal = (template?: any) => {
  editingTemplate.value = template || null
  if (template) {
    templateForm.value = {
      templateCode: template.templateCode,
      name: template.name,
      type: template.type,
      channel: template.channel,
      subject: template.subject,
      content: template.content,
      enabled: template.enabled
    }
  } else {
    templateForm.value = {
      templateCode: '',
      name: '',
      type: 'SYSTEM',
      channel: 'EMAIL',
      subject: '',
      content: '',
      enabled: true
    }
  }
  templateModalVisible.value = true
}

const handleTemplateSubmit = async () => {
  if (!templateForm.value.templateCode || !templateForm.value.name) {
    message.warning('请填写必填项')
    return
  }

  submitting.value = true
  try {
    if (editingTemplate.value) {
      await messageTemplateApi.updateTemplate(editingTemplate.value.id, templateForm.value)
      message.success('模板更新成功')
    } else {
      await messageTemplateApi.createTemplate(templateForm.value)
      message.success('模板创建成功')
    }
    templateModalVisible.value = false
    await loadTemplates()
  } catch (error) {
    message.error('操作失败')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const deleteTemplate = async (id: string) => {
  try {
    await messageTemplateApi.deleteTemplate(id)
    message.success('模板已删除')
    await loadTemplates()
  } catch (error) {
    message.error('删除失败')
    console.error(error)
  }
}

const previewTemplate = async (template: any) => {
  try {
    const variables = await messageTemplateApi.extractVariables(template.content)
    previewData.value = {
      name: template.name,
      subject: template.subject,
      content: template.content,
      variables: Array.isArray(variables) ? variables : []
    }
    previewModalVisible.value = true
  } catch (error) {
    console.error(error)
  }
}

const initTemplates = async () => {
  initializing.value = true
  try {
    await messageTemplateApi.initTemplates()
    message.success('初始化成功')
    await loadTemplates()
  } catch (error) {
    message.error('初始化失败')
    console.error(error)
  } finally {
    initializing.value = false
  }
}

const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    ALERT: 'red',
    BILLING: 'orange',
    SYSTEM: 'blue'
  }
  return colors[type] || 'default'
}

const getTypeText = (type: string) => {
  const texts: Record<string, string> = {
    ALERT: '告警通知',
    BILLING: '账单通知',
    SYSTEM: '系统通知'
  }
  return texts[type] || type
}

const getChannelColor = (channel: string) => {
  const colors: Record<string, string> = {
    EMAIL: 'green',
    SMS: 'blue',
    PUSH: 'orange',
    WECHAT: 'cyan'
  }
  return colors[channel] || 'default'
}

const getChannelText = (channel: string) => {
  const texts: Record<string, string> = {
    EMAIL: '邮件',
    SMS: '短信',
    PUSH: '推送',
    WECHAT: '微信'
  }
  return texts[channel] || channel
}
</script>

<style scoped>
.message-template-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>
