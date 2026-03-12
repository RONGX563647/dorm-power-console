<template>
  <div class="user-management-view">
    <div class="page-header">
      <h1>用户管理</h1>
      <a-button type="primary" @click="showCreateModal">
        <template #icon><PlusOutlined /></template>
        创建用户
      </a-button>
    </div>

    <a-card>
      <div class="filter-bar">
        <a-space>
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索用户名/邮箱"
            style="width: 280px"
            @search="handleSearch"
          />
          <a-select
            v-model:value="roleFilter"
            style="width: 150px"
            @change="handleFilterChange"
          >
            <a-select-option value="all">全部角色</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="user">普通用户</a-select-option>
          </a-select>
        </a-space>
      </div>

      <a-table
        :columns="userColumns"
        :data-source="filteredUsers"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, pageSizeOptions: ['5', '10', '15'] }"
        :row-key="(record: User) => record.username"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'username'">
            <span style="color: #1a1a1a; font-weight: 500">{{ record.username }}</span>
          </template>
          <template v-if="column.key === 'email'">
            <span style="color: #1a1a1a">{{ record.email }}</span>
          </template>
          <template v-if="column.key === 'role'">
            <a-tag :color="record.role === 'admin' ? 'blue' : 'default'">
              {{ record.role === 'admin' ? '管理员' : '普通用户' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="viewUser(record.username)">
                查看
              </a-button>
              <a-button type="link" size="small" @click="editUser(record)">
                编辑
              </a-button>
              <a-button type="link" size="small" @click="changePassword(record.username)">
                修改密码
              </a-button>
              <a-popconfirm
                v-if="record.username !== 'admin'"
                title="确定要删除这个用户吗？"
                @confirm="deleteUser(record.username)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 创建/编辑用户模态框 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingUser ? '编辑用户' : '创建用户'"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-form-item label="用户名" name="username">
          <a-input
            v-model:value="formData.username"
            placeholder="请输入用户名"
            :disabled="!!editingUser"
          />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formData.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="密码" name="password" v-if="!editingUser">
          <a-input-password v-model:value="formData.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="角色" name="role">
          <a-select v-model:value="formData.role" placeholder="请选择角色">
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="user">普通用户</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 修改密码模态框 -->
    <a-modal
      v-model:open="passwordModalVisible"
      title="修改密码"
      @ok="handlePasswordOk"
      @cancel="passwordModalVisible = false"
    >
      <a-form
        ref="passwordFormRef"
        :model="passwordData"
        :rules="passwordRules"
        layout="vertical"
      >
        <a-form-item label="旧密码" name="oldPassword">
          <a-input-password v-model:value="passwordData.oldPassword" placeholder="请输入旧密码" />
        </a-form-item>
        <a-form-item label="新密码" name="newPassword">
          <a-input-password v-model:value="passwordData.newPassword" placeholder="请输入新密码" />
        </a-form-item>
        <a-form-item label="确认密码" name="confirmPassword">
          <a-input-password v-model:value="passwordData.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 用户详情模态框 -->
    <a-modal
      v-model:open="detailVisible"
      title="用户详情"
      width="600px"
      :footer="null"
    >
      <div v-if="selectedUser">
        <a-descriptions bordered :column="2">
          <a-descriptions-item label="用户名">{{ selectedUser.username }}</a-descriptions-item>
          <a-descriptions-item label="邮箱">{{ selectedUser.email }}</a-descriptions-item>
          <a-descriptions-item label="角色">
            <a-tag :color="selectedUser.role === 'admin' ? 'blue' : 'default'">
              {{ selectedUser.role === 'admin' ? '管理员' : '普通用户' }}
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { userApi } from '@/api'
import type { User } from '@/types'

const loading = ref(false)
const users = ref<User[]>([])
const keyword = ref('')
const roleFilter = ref('all')
const modalVisible = ref(false)
const passwordModalVisible = ref(false)
const detailVisible = ref(false)
const editingUser = ref<User | null>(null)
const selectedUser = ref<User | null>(null)
const passwordUsername = ref('')

const formData = ref({
  username: '',
  email: '',
  password: '',
  role: 'user' as 'admin' | 'user'
})

const passwordData = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string) => {
        if (value !== passwordData.value.newPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ]
}

const userColumns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '角色', key: 'role' },
  { title: '操作', key: 'action', width: 250 }
]

const filteredUsers = computed(() => {
  return users.value
    .filter(user => {
      const k = keyword.value.trim().toLowerCase()
      if (k) {
        return user.username.toLowerCase().includes(k) || 
               user.email.toLowerCase().includes(k)
      }
      return true
    })
    .filter(user => {
      if (roleFilter.value === 'all') return true
      return user.role === roleFilter.value
    })
})

const loadUsers = async () => {
  try {
    loading.value = true
    users.value = await userApi.getUsers()
  } catch (error: any) {
    message.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  editingUser.value = null
  formData.value = { username: '', email: '', password: '', role: 'user' }
  modalVisible.value = true
}

const editUser = (user: User) => {
  editingUser.value = user
  formData.value = { 
    username: user.username, 
    email: user.email, 
    password: '', 
    role: user.role 
  }
  modalVisible.value = true
}

const handleModalOk = async () => {
  try {
    if (editingUser.value) {
      await userApi.updateUser(editingUser.value.username, formData.value)
      message.success('用户更新成功')
    } else {
      await userApi.createUser(formData.value)
      message.success('用户创建成功')
    }
    modalVisible.value = false
    await loadUsers()
  } catch (error: any) {
    message.error(editingUser.value ? '用户更新失败' : '用户创建失败')
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
}

const viewUser = async (username: string) => {
  try {
    selectedUser.value = await userApi.getUser(username)
    detailVisible.value = true
  } catch (error: any) {
    message.error('加载用户详情失败')
  }
}

const deleteUser = async (username: string) => {
  try {
    await userApi.deleteUser(username)
    message.success('用户删除成功')
    await loadUsers()
  } catch (error: any) {
    message.error('用户删除失败')
  }
}

const changePassword = (username: string) => {
  passwordUsername.value = username
  passwordData.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  passwordModalVisible.value = true
}

const handlePasswordOk = async () => {
  try {
    await userApi.changePassword(passwordUsername.value, {
      oldPassword: passwordData.value.oldPassword,
      newPassword: passwordData.value.newPassword
    })
    message.success('密码修改成功')
    passwordModalVisible.value = false
  } catch (error: any) {
    message.error('密码修改失败')
  }
}

const handleSearch = () => {
  // 搜索已在 computed 中处理
}

const handleFilterChange = () => {
  // 过滤已在 computed 中处理
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-management-view {
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
}

.filter-bar {
  margin-bottom: 16px;
}

:deep(.ant-card) {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.15);
}

:deep(.ant-card-head) {
  color: #1a1a1a;
  border-bottom-color: rgba(0, 212, 255, 0.15);
}

:deep(.ant-table) {
  color: #1a1a1a;
}

:deep(.ant-table-thead > tr > th) {
  color: #1a1a1a;
  background: rgba(0, 212, 255, 0.05);
}

:deep(.ant-table-tbody > tr) {
  border-bottom-color: rgba(0, 212, 255, 0.1);
}

:deep(.ant-table-tbody > tr:hover) {
  background: rgba(0, 212, 255, 0.05);
}

:deep(.ant-modal-content) {
  background: rgba(16, 24, 40, 0.95);
}

:deep(.ant-modal-header) {
  background: rgba(16, 24, 40, 0.95);
  border-bottom-color: rgba(0, 212, 255, 0.15);
}

:deep(.ant-modal-title) {
  color: #1a1a1a;
}

:deep(.ant-descriptions-item-label) {
  color: #1a1a1a;
}

:deep(.ant-descriptions-item-content) {
  color: #1a1a1a;
}
</style>