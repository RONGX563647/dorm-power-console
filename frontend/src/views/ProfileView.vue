<template>
  <div class="profile-view">
    <div class="page-header">
      <h1>个人信息</h1>
    </div>

    <a-row :gutter="24">
      <a-col :span="8">
        <a-card class="profile-card">
          <div class="profile-avatar-section">
            <a-avatar :size="100" class="profile-avatar">
              <template #icon>
                <UserOutlined />
              </template>
            </a-avatar>
            <h2 class="profile-name">{{ userInfo.username }}</h2>
            <p class="profile-role">
              <a-tag :color="userInfo.role === 'admin' ? 'blue' : 'default'">
                {{ userInfo.role === 'admin' ? '管理员' : '普通用户' }}
              </a-tag>
            </p>
          </div>

          <a-divider />

          <div class="profile-stats">
            <div class="stat-item">
              <div class="stat-value">{{ userStats.notificationCount }}</div>
              <div class="stat-label">未读通知</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ userStats.loginCount }}</div>
              <div class="stat-label">登录次数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ userStats.deviceCount }}</div>
              <div class="stat-label">管理设备</div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :span="16">
        <a-card title="基本信息" class="profile-card">
          <a-form :model="profileForm" layout="vertical">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="用户名">
                  <a-input v-model:value="profileForm.username" disabled />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="邮箱">
                  <a-input v-model:value="profileForm.email" placeholder="请输入邮箱" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="手机号">
                  <a-input v-model:value="profileForm.phone" placeholder="请输入手机号" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="部门">
                  <a-input v-model:value="profileForm.department" placeholder="请输入部门" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item>
              <a-button type="primary" :loading="saving" @click="handleSaveProfile">
                保存修改
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <a-card title="修改密码" class="profile-card" style="margin-top: 24px">
          <a-form :model="passwordForm" layout="vertical">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="当前密码">
                  <a-input-password
                    v-model:value="passwordForm.oldPassword"
                    placeholder="请输入当前密码"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="新密码">
                  <a-input-password
                    v-model:value="passwordForm.newPassword"
                    placeholder="请输入新密码"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="确认新密码">
                  <a-input-password
                    v-model:value="passwordForm.confirmPassword"
                    placeholder="请确认新密码"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item>
              <a-button type="primary" :loading="changingPassword" @click="handleChangePassword">
                修改密码
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <a-card title="账户信息" class="profile-card" style="margin-top: 24px">
          <a-descriptions :column="2">
            <a-descriptions-item label="注册时间">
              {{ formatDate(userInfo.createdAt) }}
            </a-descriptions-item>
            <a-descriptions-item label="最后登录">
              {{ formatDate(userInfo.lastLoginAt) }}
            </a-descriptions-item>
            <a-descriptions-item label="账户状态">
              <a-badge status="success" text="正常" />
            </a-descriptions-item>
            <a-descriptions-item label="用户ID">
              {{ userInfo.id }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { UserOutlined } from '@ant-design/icons-vue'
import { userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const saving = ref(false)
const changingPassword = ref(false)

const userInfo = ref({
  id: '',
  username: '',
  email: '',
  role: 'user',
  createdAt: '',
  lastLoginAt: ''
})

const userStats = ref({
  notificationCount: 0,
  loginCount: 0,
  deviceCount: 0
})

const profileForm = ref({
  username: '',
  email: '',
  phone: '',
  department: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const loadUserInfo = async () => {
  try {
    const username = authStore.user?.username || localStorage.getItem('username') || 'admin'
    const user = await userApi.getUser(username)
    userInfo.value = {
      id: user.id || '',
      username: user.username || '',
      email: user.email || '',
      role: user.role || 'user',
      createdAt: user.createdAt || '',
      lastLoginAt: user.lastLoginAt || ''
    }
    profileForm.value = {
      username: user.username || '',
      email: user.email || '',
      phone: user.phone || '',
      department: user.department || ''
    }
  } catch (error) {
    message.error('加载用户信息失败')
  }
}

const handleSaveProfile = async () => {
  saving.value = true
  try {
    const username = userInfo.value.username
    await userApi.updateProfile(username, { email: profileForm.value.email })
    message.success('个人信息已更新')
    loadUserInfo()
  } catch (error) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleChangePassword = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    message.error('两次输入的新密码不一致')
    return
  }
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    message.error('请填写完整密码信息')
    return
  }

  changingPassword.value = true
  try {
    const username = userInfo.value.username
    await userApi.changePassword(username, {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    message.success('密码修改成功')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (error) {
    message.error('密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-view {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  color: #1a1a1a;
  margin: 0;
  font-size: 24px;
}

.profile-card {
  background: rgba(16, 24, 40, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.1);
}

.profile-card :deep(.ant-card-head) {
  border-bottom: 1px solid rgba(0, 212, 255, 0.1);
}

.profile-card :deep(.ant-card-head-title) {
  color: #1a1a1a;
}

.profile-avatar-section {
  text-align: center;
  padding: 24px 0;
}

.profile-avatar {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.2) 0%, rgba(0, 153, 255, 0.2) 100%);
  border: 2px solid rgba(0, 212, 255, 0.3);
  box-shadow: 0 0 20px rgba(0, 212, 255, 0.3);
  color: #00d4ff;
  font-size: 40px;
}

.profile-name {
  color: #1a1a1a;
  margin: 16px 0 8px;
  font-size: 20px;
}

.profile-role {
  margin: 0;
}

.profile-stats {
  display: flex;
  justify-content: space-around;
  padding: 16px 0;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #00d4ff;
}

.stat-label {
  font-size: 12px;
  color: #1a1a1a;
  margin-top: 4px;
}

.profile-card :deep(.ant-descriptions-item-label) {
  color: #1a1a1a;
}

.profile-card :deep(.ant-descriptions-item-content) {
  color: #1a1a1a;
}

.profile-card :deep(.ant-divider) {
  border-color: rgba(0, 212, 255, 0.1);
}
</style>
