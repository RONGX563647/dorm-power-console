import { deviceApi } from '@/api'
import type { StripStatus } from '@/types'

interface CacheEntry<T> {
  data: T
  timestamp: number
}

const CACHE_TTL = 10000
const statusCache = new Map<string, CacheEntry<StripStatus | null>>()
const pendingRequests = new Map<string, Promise<StripStatus | null>>()

export const deviceStatusService = {
  async getDeviceStatus(deviceId: string, forceRefresh = false): Promise<StripStatus | null> {
    const now = Date.now()
    const cached = statusCache.get(deviceId)
    
    if (!forceRefresh && cached && (now - cached.timestamp) < CACHE_TTL) {
      return cached.data
    }
    
    if (pendingRequests.has(deviceId)) {
      return pendingRequests.get(deviceId)!
    }
    
    const request = (async () => {
      try {
        const status = await deviceApi.getDeviceStatus(deviceId)
        statusCache.set(deviceId, { data: status, timestamp: Date.now() })
        return status
      } catch (error) {
        console.warn(`获取设备 ${deviceId} 状态失败:`, error)
        if (cached) {
          return cached.data
        }
        return null
      } finally {
        pendingRequests.delete(deviceId)
      }
    })()
    
    pendingRequests.set(deviceId, request)
    return request
  },

  async getBatchDeviceStatus(deviceIds: string[], forceRefresh = false): Promise<Record<string, StripStatus | null>> {
    const results: Record<string, StripStatus | null> = {}
    const now = Date.now()
    const idsToFetch: string[] = []
    
    for (const id of deviceIds) {
      const cached = statusCache.get(id)
      if (!forceRefresh && cached && (now - cached.timestamp) < CACHE_TTL) {
        results[id] = cached.data
      } else if (!pendingRequests.has(id)) {
        idsToFetch.push(id)
      } else {
        results[id] = await pendingRequests.get(id)!
      }
    }
    
    if (idsToFetch.length > 0) {
      const BATCH_SIZE = 5
      const batches: string[][] = []
      for (let i = 0; i < idsToFetch.length; i += BATCH_SIZE) {
        batches.push(idsToFetch.slice(i, i + BATCH_SIZE))
      }
      
      for (const batch of batches) {
        await Promise.all(
          batch.map(async (id) => {
            results[id] = await this.getDeviceStatus(id, forceRefresh)
          })
        )
      }
    }
    
    return results
  },

  invalidateCache(deviceId?: string) {
    if (deviceId) {
      statusCache.delete(deviceId)
    } else {
      statusCache.clear()
    }
  },

  getCachedStatus(deviceId: string): StripStatus | null | undefined {
    const cached = statusCache.get(deviceId)
    if (cached && (Date.now() - cached.timestamp) < CACHE_TTL) {
      return cached.data
    }
    return undefined
  }
}

export function createPollingManager(
  callback: () => Promise<void>,
  interval: number = 10000
) {
  let timer: ReturnType<typeof setInterval> | null = null
  let isRunning = false
  
  const execute = async () => {
    if (isRunning) return
    isRunning = true
    try {
      await callback()
    } catch (error) {
      console.error('轮询执行失败:', error)
    } finally {
      isRunning = false
    }
  }
  
  return {
    start() {
      if (timer) return
      execute()
      timer = setInterval(execute, interval)
    },
    
    stop() {
      if (timer) {
        clearInterval(timer)
        timer = null
      }
    },
    
    isPolling() {
      return timer !== null
    }
  }
}
