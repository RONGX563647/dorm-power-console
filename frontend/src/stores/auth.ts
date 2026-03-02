import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import { authApi } from '@/api'

/**
 * 认证状态管理
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('dorm_auth_token'))
  const getUserFromStorage = () => {
    const cached = localStorage.getItem('dorm_auth_user')
    return cached ? JSON.parse(cached) : null
  }
  const user = ref<User | null>(getUserFromStorage())
  
  const isAuthenticated = computed(() => !!token.value)
  const ready = ref(true)

  /**
   * 用户登录
   */
  async function login(account: string, password: string) {
    const data = await authApi.login({ account, password })
    token.value = data.token
    user.value = data.user
    localStorage.setItem('dorm_auth_token', data.token)
    localStorage.setItem('dorm_auth_user', JSON.stringify(data.user))
  }

  /**
   * 用户登出
   */
  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('dorm_auth_token')
    localStorage.removeItem('dorm_auth_user')
  }

  return {
    token,
    user,
    isAuthenticated,
    ready,
    login,
    logout
  }
})
