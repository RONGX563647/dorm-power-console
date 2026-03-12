<template>
  <div class="register-container">
    <div class="background-decoration"></div>
    <a-card class="register-card">
      <div class="top-decoration"></div>
      <a-space direction="vertical" size="large" class="register-content">
        <div class="logo-section">
          <div class="logo-icon">
            <ThunderboltOutlined />
          </div>
          <a-typography-title :level="3" class="title">
            注册账号
          </a-typography-title>
          <a-typography-text class="subtitle">
            创建您的 Dorm Power 账户
          </a-typography-text>
        </div>

        <a-form
          :model="formState"
          :rules="rules"
          @finish="handleRegister"
          layout="vertical"
        >
          <a-form-item name="username">
            <a-input
              v-model:value="formState.username"
              placeholder="用户名"
              size="large"
            >
              <template #prefix>
                <UserOutlined />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item name="email">
            <a-input
              v-model:value="formState.email"
              placeholder="邮箱"
              size="large"
            >
              <template #prefix>
                <MailOutlined />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item name="password">
            <a-input-password
              v-model:value="formState.password"
              placeholder="密码"
              size="large"
            >
              <template #prefix>
                <LockOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item name="confirmPassword">
            <a-input-password
              v-model:value="formState.confirmPassword"
              placeholder="确认密码"
              size="large"
            >
              <template #prefix>
                <SafetyOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              :loading="loading"
              block
              class="register-btn"
            >
              注册
            </a-button>
          </a-form-item>

          <div class="login-link">
            <span>已有账号？</span>
            <a @click="goToLogin">立即登录</a>
          </div>
        </a-form>
      </a-space>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ThunderboltOutlined,
  UserOutlined,
  LockOutlined,
  MailOutlined,
  SafetyOutlined
} from '@ant-design/icons-vue'
import { authApi } from '@/api'

const router = useRouter()
const loading = ref(false)

const formState = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = async (_rule: any, value: string) => {
  if (value !== formState.password) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, message: '用户名至少3个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  try {
    loading.value = true
    await authApi.register({
      username: formState.username,
      email: formState.email,
      password: formState.password
    })
    message.success('注册成功，请登录')
    router.push('/login')
  } catch (error: any) {
    message.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(180deg, #0a0f1a 0%, #0d1525 50%, #0a0f1a 100%);
  position: relative;
  overflow: hidden;
}

.background-decoration {
  position: absolute;
  top: 10%;
  left: 10%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  filter: blur(40px);
}

.register-card {
  width: 420px;
  border-radius: 16px;
  background: rgba(16, 24, 40, 0.9);
  border: 1px solid rgba(0, 212, 255, 0.2);
  box-shadow: 0 0 40px rgba(0, 212, 255, 0.15), 0 20px 50px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

.top-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #00d4ff, #0099ff, #00d4ff, transparent);
}

.register-content {
  padding: 32px;
  width: 100%;
}

.logo-section {
  text-align: center;
  margin-bottom: 8px;
}

.logo-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.2) 0%, rgba(0, 153, 255, 0.2) 100%);
  border: 1px solid rgba(0, 212, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 0 20px rgba(0, 212, 255, 0.3);
  font-size: 28px;
  color: #00d4ff;
}

.title {
  margin: 0 !important;
  color: #1a1a1a !important;
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.5);
}

.subtitle {
  color: #1a1a1a;
  font-size: 14px;
}

.register-btn {
  background: linear-gradient(90deg, #00d4ff, #0099ff);
  border: none;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
}

.register-btn:hover {
  background: linear-gradient(90deg, #00e5ff, #00aaff);
  box-shadow: 0 0 20px rgba(0, 212, 255, 0.4);
}

.login-link {
  text-align: center;
  color: #1a1a1a;
  font-size: 14px;
}

.login-link a {
  color: #00d4ff;
  margin-left: 8px;
}

.login-link a:hover {
  color: #00e5ff;
}
</style>
