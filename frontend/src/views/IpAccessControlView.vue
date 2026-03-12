<template>
  <div class="ip-access-control-view">
    <a-page-header
      title="IP访问控制"
      sub-title="IP黑白名单管理"
      style="padding: 0 0 16px 0"
    >
      <template #extra>
        <a-space>
          <a-button @click="cleanup" :loading="cleaning">
            清理过期IP
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="whitelist" tab="白名单">
        <a-card>
          <template #extra>
            <a-button type="primary" @click="showIpModal('whitelist')">
              添加白名单
            </a-button>
          </template>

          <a-table
            :data-source="whitelist"
            :columns="ipColumns"
            :loading="loadingWhitelist"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag color="green">白名单</a-tag>
              </template>
              <template v-if="column.key === 'expireTime'">
                {{ record.expireTime || '永久有效' }}
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="showIpModal('whitelist', record)">
                    编辑
                  </a-button>
                  <a-popconfirm
                    title="确定删除此IP吗？"
                    @confirm="deleteIp(record.ipAddress)"
                  >
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="blacklist" tab="黑名单">
        <a-card>
          <template #extra>
            <a-button type="primary" @click="showIpModal('blacklist')">
              添加黑名单
            </a-button>
          </template>

          <a-table
            :data-source="blacklist"
            :columns="ipColumns"
            :loading="loadingBlacklist"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag color="red">黑名单</a-tag>
              </template>
              <template v-if="column.key === 'expireTime'">
                {{ record.expireTime || '永久有效' }}
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="showIpModal('blacklist', record)">
                    编辑
                  </a-button>
                  <a-popconfirm
                    title="确定删除此IP吗？"
                    @confirm="deleteIp(record.ipAddress)"
                  >
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="active" tab="活动IP">
        <a-card>
          <template #extra>
            <a-button @click="loadActiveIps">
              刷新
            </a-button>
          </template>

          <a-table
            :data-source="activeIps"
            :columns="activeIpColumns"
            :loading="loadingActive"
            :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button
                    type="link"
                    size="small"
                    @click="quickAddToWhitelist(record.ipAddress)"
                  >
                    加白名单
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="quickAddToBlacklist(record.ipAddress)"
                  >
                    加黑名单
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="check" tab="IP检查">
        <a-card>
          <a-form layout="inline" style="margin-bottom: 16px">
            <a-form-item label="IP地址">
              <a-input
                v-model:value="checkIpAddress"
                placeholder="请输入IP地址"
                style="width: 200px"
              />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="checkIp" :loading="checking">
                  检查
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>

          <div v-if="ipCheckResult" style="margin-top: 16px">
            <a-descriptions title="检查结果" bordered :column="2">
              <a-descriptions-item label="IP地址">
                {{ ipCheckResult.ipAddress }}
              </a-descriptions-item>
              <a-descriptions-item label="状态">
                <a-tag :color="ipCheckResult.status === 'ALLOWED' ? 'green' : 'red'">
                  {{ ipCheckResult.status === 'ALLOWED' ? '允许访问' : '禁止访问' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="是否在白名单">
                <a-tag :color="ipCheckResult.inWhitelist ? 'green' : 'default'">
                  {{ ipCheckResult.inWhitelist ? '是' : '否' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="是否在黑名单">
                <a-tag :color="ipCheckResult.inBlacklist ? 'red' : 'default'">
                  {{ ipCheckResult.inBlacklist ? '是' : '否' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="访问次数" v-if="ipCheckResult.accessCount">
                {{ ipCheckResult.accessCount }}
              </a-descriptions-item>
              <a-descriptions-item label="最后访问" v-if="ipCheckResult.lastAccess">
                {{ ipCheckResult.lastAccess }}
              </a-descriptions-item>
            </a-descriptions>
          </div>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="ipModalVisible"
      :title="editingIp ? '编辑IP' : `添加${currentListType === 'whitelist' ? '白名单' : '黑名单'}`"
      @ok="handleIpSubmit"
      :confirm-loading="submittingIp"
    >
      <a-form :model="ipForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="IP地址" required>
          <a-input
            v-model:value="ipForm.ipAddress"
            placeholder="请输入IP地址"
            :disabled="!!editingIp"
          />
        </a-form-item>
        <a-form-item label="类型">
          <a-radio-group v-model:value="currentListType" :disabled="!!editingIp">
            <a-radio value="whitelist">白名单</a-radio>
            <a-radio value="blacklist">黑名单</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="ipForm.note" placeholder="请输入备注" />
        </a-form-item>
        <a-form-item label="过期时间">
          <a-date-picker
            v-model:value="ipForm.expireTime"
            show-time
            placeholder="选择过期时间（留空表示永久）"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ipAccessControlApi } from '@/api'
import type { Dayjs } from 'dayjs'

const activeTab = ref('whitelist')
const loadingWhitelist = ref(false)
const loadingBlacklist = ref(false)
const loadingActive = ref(false)
const checking = ref(false)
const cleaning = ref(false)
const submittingIp = ref(false)

const whitelist = ref<any[]>([])
const blacklist = ref<any[]>([])
const activeIps = ref<any[]>([])
const checkIpAddress = ref('')
const ipCheckResult = ref<any>(null)

const ipModalVisible = ref(false)
const currentListType = ref<'whitelist' | 'blacklist'>('whitelist')
const editingIp = ref<any>(null)

const ipForm = ref({
  ipAddress: '',
  note: '',
  expireTime: null as Dayjs | null
})

const ipColumns = [
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress' },
  { title: '状态', key: 'status' },
  { title: '备注', dataIndex: 'note', key: 'note' },
  { title: '过期时间', key: 'expireTime' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 150 }
]

const activeIpColumns = [
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress' },
  { title: '访问次数', dataIndex: 'accessCount', key: 'accessCount' },
  { title: '最后访问', dataIndex: 'lastAccess', key: 'lastAccess' },
  { title: '位置', dataIndex: 'location', key: 'location' },
  { title: '操作', key: 'action', width: 150 }
]

onMounted(() => {
  loadWhitelist()
  loadBlacklist()
  loadActiveIps()
})

const loadWhitelist = async () => {
  loadingWhitelist.value = true
  try {
    const data = await ipAccessControlApi.getWhitelist()
    whitelist.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载白名单失败')
    console.error(error)
  } finally {
    loadingWhitelist.value = false
  }
}

const loadBlacklist = async () => {
  loadingBlacklist.value = true
  try {
    const data = await ipAccessControlApi.getBlacklist()
    blacklist.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载黑名单失败')
    console.error(error)
  } finally {
    loadingBlacklist.value = false
  }
}

const loadActiveIps = async () => {
  loadingActive.value = true
  try {
    const data = await ipAccessControlApi.getActiveIps()
    activeIps.value = Array.isArray(data) ? data : []
  } catch (error) {
    message.error('加载活动IP失败')
    console.error(error)
  } finally {
    loadingActive.value = false
  }
}

const showIpModal = (type: 'whitelist' | 'blacklist', ip?: any) => {
  currentListType.value = type
  editingIp.value = ip || null
  
  if (ip) {
    ipForm.value = {
      ipAddress: ip.ipAddress,
      note: ip.note || '',
      expireTime: ip.expireTime ? null : null
    }
  } else {
    ipForm.value = {
      ipAddress: '',
      note: '',
      expireTime: null
    }
  }
  
  ipModalVisible.value = true
}

const handleIpSubmit = async () => {
  if (!ipForm.value.ipAddress) {
    message.warning('请输入IP地址')
    return
  }

  submittingIp.value = true
  try {
    const data = {
      ipAddress: ipForm.value.ipAddress,
      note: ipForm.value.note,
      expireTime: ipForm.value.expireTime?.toISOString()
    }

    if (editingIp.value) {
      await ipAccessControlApi.updateIp(ipForm.value.ipAddress, data)
      message.success('IP更新成功')
    } else {
      if (currentListType.value === 'whitelist') {
        await ipAccessControlApi.addToWhitelist(data)
        message.success('添加白名单成功')
      } else {
        await ipAccessControlApi.addToBlacklist(data)
        message.success('添加黑名单成功')
      }
    }

    ipModalVisible.value = false
    await Promise.all([loadWhitelist(), loadBlacklist()])
  } catch (error) {
    message.error('操作失败')
    console.error(error)
  } finally {
    submittingIp.value = false
  }
}

const deleteIp = async (ipAddress: string) => {
  try {
    await ipAccessControlApi.deleteIp(ipAddress)
    message.success('IP已删除')
    await Promise.all([loadWhitelist(), loadBlacklist()])
  } catch (error) {
    message.error('删除失败')
    console.error(error)
  }
}

const checkIp = async () => {
  if (!checkIpAddress.value.trim()) {
    message.warning('请输入IP地址')
    return
  }

  checking.value = true
  try {
    const data = await ipAccessControlApi.checkIp(checkIpAddress.value)
    ipCheckResult.value = data
  } catch (error) {
    message.error('检查失败')
    console.error(error)
  } finally {
    checking.value = false
  }
}

const quickAddToWhitelist = async (ipAddress: string) => {
  try {
    await ipAccessControlApi.addToWhitelist({ ipAddress, note: '从活动IP添加' })
    message.success('添加白名单成功')
    await loadWhitelist()
  } catch (error) {
    message.error('添加失败')
    console.error(error)
  }
}

const quickAddToBlacklist = async (ipAddress: string) => {
  try {
    await ipAccessControlApi.addToBlacklist({ ipAddress, note: '从活动IP添加' })
    message.success('添加黑名单成功')
    await loadBlacklist()
  } catch (error) {
    message.error('添加失败')
    console.error(error)
  }
}

const cleanup = async () => {
  cleaning.value = true
  try {
    await ipAccessControlApi.cleanup()
    message.success('清理完成')
    await Promise.all([loadWhitelist(), loadBlacklist(), loadActiveIps()])
  } catch (error) {
    message.error('清理失败')
    console.error(error)
  } finally {
    cleaning.value = false
  }
}
</script>

<style scoped>
.ip-access-control-view {
  padding: 24px;
}

:deep(.ant-card) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>
