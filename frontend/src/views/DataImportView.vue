<template>
  <div class="data-import-view">
    <a-page-header
      title="数据导入"
      sub-title="批量导入学生、房间、设备数据"
      style="padding: 0 0 16px 0"
    />

    <a-row :gutter="16">
      <a-col :span="8">
        <a-card title="学生数据导入" :hoverable="true">
          <a-space direction="vertical" style="width: 100%">
            <a-upload-dragger
              :before-upload="beforeUpload"
              :show-upload-list="false"
              accept=".xlsx,.xls,.csv"
            >
              <p class="ant-upload-drag-icon">
                <inbox-outlined />
              </p>
              <p class="ant-upload-text">点击或拖拽文件到此区域</p>
              <p class="ant-upload-hint">支持 .xlsx, .xls, .csv 格式</p>
            </a-upload-dragger>
            <a-button
              type="primary"
              block
              @click="importStudents"
              :loading="importingStudents"
              :disabled="!studentFile"
            >
              导入学生数据
            </a-button>
            <a-button block @click="downloadTemplate('students')">
              下载模板
            </a-button>
          </a-space>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card title="房间数据导入" :hoverable="true">
          <a-space direction="vertical" style="width: 100%">
            <a-upload-dragger
              :before-upload="beforeUploadRoom"
              :show-upload-list="false"
              accept=".xlsx,.xls,.csv"
            >
              <p class="ant-upload-drag-icon">
                <inbox-outlined />
              </p>
              <p class="ant-upload-text">点击或拖拽文件到此区域</p>
              <p class="ant-upload-hint">支持 .xlsx, .xls, .csv 格式</p>
            </a-upload-dragger>
            <a-button
              type="primary"
              block
              @click="importRooms"
              :loading="importingRooms"
              :disabled="!roomFile"
            >
              导入房间数据
            </a-button>
            <a-button block @click="downloadTemplate('rooms')">
              下载模板
            </a-button>
          </a-space>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card title="设备数据导入" :hoverable="true">
          <a-space direction="vertical" style="width: 100%">
            <a-upload-dragger
              :before-upload="beforeUploadDevice"
              :show-upload-list="false"
              accept=".xlsx,.xls,.csv"
            >
              <p class="ant-upload-drag-icon">
                <inbox-outlined />
              </p>
              <p class="ant-upload-text">点击或拖拽文件到此区域</p>
              <p class="ant-upload-hint">支持 .xlsx, .xls, .csv 格式</p>
            </a-upload-dragger>
            <a-button
              type="primary"
              block
              @click="importDevices"
              :loading="importingDevices"
              :disabled="!deviceFile"
            >
              导入设备数据
            </a-button>
            <a-button block @click="downloadTemplate('devices')">
              下载模板
            </a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>

    <a-card title="JSON数据导入" style="margin-top: 16px">
      <a-textarea
        v-model:value="jsonData"
        placeholder="请输入JSON格式的数据"
        :rows="10"
        style="margin-bottom: 16px"
      />
      <a-button type="primary" @click="importJson" :loading="importingJson">
        导入JSON数据
      </a-button>
    </a-card>

    <a-card title="导入说明" style="margin-top: 16px">
      <a-descriptions bordered :column="1">
        <a-descriptions-item label="学生数据格式">
          学号、姓名、性别、学院、专业、班级、联系电话、邮箱
        </a-descriptions-item>
        <a-descriptions-item label="房间数据格式">
          楼栋号、房间号、楼层、房间类型、床位数
        </a-descriptions-item>
        <a-descriptions-item label="设备数据格式">
          设备ID、设备名称、设备类型、所属房间、安装位置
        </a-descriptions-item>
        <a-descriptions-item label="注意事项">
          <ul style="margin: 0; padding-left: 20px">
            <li>请确保数据格式正确，避免导入失败</li>
            <li>导入前建议先下载数据模板</li>
            <li>重复数据将被跳过或更新</li>
            <li>大文件导入可能需要较长时间，请耐心等待</li>
          </ul>
        </a-descriptions-item>
      </a-descriptions>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import { importApi } from '@/api'

const studentFile = ref<File | null>(null)
const roomFile = ref<File | null>(null)
const deviceFile = ref<File | null>(null)
const jsonData = ref('')

const importingStudents = ref(false)
const importingRooms = ref(false)
const importingDevices = ref(false)
const importingJson = ref(false)

const beforeUpload = (file: File) => {
  studentFile.value = file
  return false
}

const beforeUploadRoom = (file: File) => {
  roomFile.value = file
  return false
}

const beforeUploadDevice = (file: File) => {
  deviceFile.value = file
  return false
}

const importStudents = async () => {
  if (!studentFile.value) {
    message.warning('请先选择文件')
    return
  }

  importingStudents.value = true
  try {
    const result = await importApi.importStudents(studentFile.value)
    message.success(`成功导入 ${(result as any).successCount || 0} 条数据`)
    studentFile.value = null
  } catch (error) {
    message.error('导入失败')
    console.error(error)
  } finally {
    importingStudents.value = false
  }
}

const importRooms = async () => {
  if (!roomFile.value) {
    message.warning('请先选择文件')
    return
  }

  importingRooms.value = true
  try {
    const result = await importApi.importRooms(roomFile.value)
    message.success(`成功导入 ${(result as any).successCount || 0} 条数据`)
    roomFile.value = null
  } catch (error) {
    message.error('导入失败')
    console.error(error)
  } finally {
    importingRooms.value = false
  }
}

const importDevices = async () => {
  if (!deviceFile.value) {
    message.warning('请先选择文件')
    return
  }

  importingDevices.value = true
  try {
    const result = await importApi.importDevices(deviceFile.value)
    message.success(`成功导入 ${(result as any).successCount || 0} 条数据`)
    deviceFile.value = null
  } catch (error) {
    message.error('导入失败')
    console.error(error)
  } finally {
    importingDevices.value = false
  }
}

const importJson = async () => {
  if (!jsonData.value.trim()) {
    message.warning('请输入JSON数据')
    return
  }

  importingJson.value = true
  try {
    const data = JSON.parse(jsonData.value)
    await importApi.importJson(data)
    message.success('JSON数据导入成功')
    jsonData.value = ''
  } catch (error) {
    if (error instanceof SyntaxError) {
      message.error('JSON格式错误')
    } else {
      message.error('导入失败')
    }
    console.error(error)
  } finally {
    importingJson.value = false
  }
}

const downloadTemplate = async (type: string) => {
  try {
    const blob = await importApi.downloadTemplate(type)
    const url = window.URL.createObjectURL(blob as any)
    const link = document.createElement('a')
    link.href = url
    link.download = `${type}_template.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    message.error('下载模板失败')
    console.error(error)
  }
}
</script>

<style scoped>
.data-import-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>
