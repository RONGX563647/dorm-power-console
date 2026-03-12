<template>
  <div class="login-container">
    <div class="background-decoration"></div>
    <div class="background-decoration-2"></div>
    <a-card class="login-card">
      <div class="top-decoration"></div>
      <a-space direction="vertical" size="large" class="login-content">
        <div class="logo-section">
          <div class="logo-icon">
            <ThunderboltOutlined />
          </div>
          <a-typography-title :level="3" class="title">
            Dorm Power
          </a-typography-title>
          <a-typography-text class="subtitle">
            智能能耗管理系统
          </a-typography-text>
        </div>

        <div class="login-tip">
          <SafetyOutlined />
          <span>系统仅启用一个管理员账号</span>
        </div>

        <a-form
          :model="formState"
          :rules="rules"
          @finish="handleLogin"
          layout="vertical"
        >
          <a-form-item name="account">
            <a-input
              v-model:value="formState.account"
              placeholder="用户名或邮箱"
              size="large"
            >
              <template #prefix>
                <UserOutlined />
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

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              :loading="loading"
              block
              class="login-btn"
            >
              登录
            </a-button>
          </a-form-item>

          <div class="register-link">
            <span>还没有账号？</span>
            <a @click="goToRegister">立即注册</a>
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
  SafetyOutlined,
  UserOutlined,
  LockOutlined
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)

const formState = reactive({
  account: '',
  password: ''
})

const rules = {
  account: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  try {
    loading.value = true
    await authStore.login(formState.account, formState.password)
    message.success('登录成功')
    router.push('/app/dashboard')
  } catch (error: any) {
    message.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: var(--gradient-bg);
  position: relative;
  overflow: hidden;
}

.background-decoration {
  position: absolute;
  top: 10%;
  left: 10%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(42, 121, 101, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  filter: blur(60px);
  animation: float 20s ease-in-out infinite;
}

.background-decoration-2 {
  position: absolute;
  bottom: 10%;
  right: 10%;
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(51, 126, 204, 0.12) 0%, transparent 70%);
  border-radius: 50%;
  filter: blur(60px);
  animation: float 25s ease-in-out infinite reverse;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(30px, -30px);
  }
}

.login-card {
  width: 420px;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xl);
  backdrop-filter: blur(20px);
  position: relative;
  overflow: hidden;
  animation: slideInUp 0.6s var(--ease-out);
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.top-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--gradient-primary);
}

.login-content {
  padding: 40px 32px;
  width: 100%;
}

.logo-section {
  text-align: center;
  margin-bottom: 8px;
}

.logo-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--gradient-primary-light);
  border: 2px solid var(--color-primary-border);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  box-shadow: 0 4px 20px rgba(42, 121, 101, 0.2);
  font-size: 32px;
  color: var(--color-primary);
  transition: all var(--duration-normal) var(--ease-in-out);
}

.logo-icon:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 30px rgba(42, 121, 101, 0.3);
}

.title {
  margin: 0 !important;
  color: var(--color-text-primary) !important;
  font-weight: var(--font-weight-bold) !important;
  font-size: 28px !important;
}

.subtitle {
  color: var(--color-text-secondary);
  font-size: 14px;
  display: block;
  margin-top: 4px;
}

.login-tip {
  padding: 12px 16px;
  background: var(--color-primary-bg);
  border: 1px solid var(--color-primary-border);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-primary);
  font-size: 13px;
}

.login-tip :deep(.anticon) {
  font-size: 16px;
}

.login-btn {
  background: var(--gradient-primary);
  border: none;
  height: 48px;
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
  border-radius: var(--radius-md);
  box-shadow: 0 4px 16px rgba(42, 121, 101, 0.3);
  transition: all var(--duration-normal) var(--ease-in-out);
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(42, 121, 101, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.register-link {
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
  margin-top: 16px;
}

.register-link a {
  color: var(--color-primary);
  margin-left: 8px;
  font-weight: var(--font-weight-medium);
  transition: color var(--duration-fast) var(--ease-in-out);
}

.register-link a:hover {
  color: var(--color-primary-light);
}

.login-card :deep(.ant-input-affix-wrapper) {
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  transition: all var(--duration-fast) var(--ease-in-out);
}

.login-card :deep(.ant-input-affix-wrapper:hover) {
  border-color: var(--color-primary-light);
}

.login-card :deep(.ant-input-affix-wrapper-focused) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary-bg);
}

.login-card :deep(.ant-input-prefix) {
  color: var(--color-text-tertiary);
  margin-right: 8px;
}

.login-card :deep(.ant-form-item-label > label) {
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

@media (max-width: 480px) {
  .login-card {
    width: 90%;
    max-width: 380px;
  }
  
  .login-content {
    padding: 32px 24px;
  }
  
  .logo-icon {
    width: 64px;
    height: 64px;
    font-size: 28px;
  }
  
  .title {
    font-size: 24px !important;
  }
}
</style>
