import axios from 'axios'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'

const instance = axios.create({
  baseURL: '',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

instance.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const data = error.response.data
      
      switch (status) {
        case 401:
          const authStore = useAuthStore()
          authStore.logout()
          message.error('登录已过期，请重新登录')
          window.location.href = '/login'
          break
        case 403:
          message.error('没有权限访问该资源')
          break
        case 404:
          message.error('请求的资源不存在')
          break
        case 500:
          message.error(data?.message || '服务器错误')
          break
        default:
          message.error(data?.message || `请求失败 (${status})`)
      }
    } else if (error.request) {
      message.error('网络错误，请检查网络连接')
    } else {
      message.error('请求配置错误')
    }
    
    return Promise.reject(error)
  }
)

export async function get<T = any>(url: string, config?: any): Promise<T> {
  return instance.get(url, config)
}

export async function post<T = any>(url: string, data?: any, config?: any): Promise<T> {
  return instance.post(url, data, config)
}

export async function put<T = any>(url: string, data?: any, config?: any): Promise<T> {
  return instance.put(url, data, config)
}

export async function del<T = any>(url: string, config?: any): Promise<T> {
  return instance.delete(url, config)
}

export async function patch<T = any>(url: string, data?: any, config?: any): Promise<T> {
  return instance.patch(url, data, config)
}

export default instance
