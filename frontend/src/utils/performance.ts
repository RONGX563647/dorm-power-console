import { ref, computed, watch, type Ref, type ComputedRef } from 'vue'

interface CacheOptions {
  ttl?: number
  maxSize?: number
}

interface CacheEntry<T> {
  data: T
  timestamp: number
  key: string
}

export function createDataCache<T>(options: CacheOptions = {}) {
  const { ttl = 60000, maxSize = 100 } = options
  const cache = new Map<string, CacheEntry<T>>()
  const keys = new Set<string>()

  const get = (key: string): T | null => {
    const entry = cache.get(key)
    if (!entry) return null

    const now = Date.now()
    if (now - entry.timestamp > ttl) {
      cache.delete(key)
      keys.delete(key)
      return null
    }

    return entry.data
  }

  const set = (key: string, data: T) => {
    if (cache.size >= maxSize && !cache.has(key)) {
      const oldestKey = keys.values().next().value
      if (oldestKey) {
        cache.delete(oldestKey)
        keys.delete(oldestKey)
      }
    }

    cache.set(key, { data, timestamp: Date.now(), key })
    keys.add(key)
  }

  const has = (key: string): boolean => {
    const entry = cache.get(key)
    if (!entry) return false

    const now = Date.now()
    if (now - entry.timestamp > ttl) {
      cache.delete(key)
      keys.delete(key)
      return false
    }

    return true
  }

  const clear = () => {
    cache.clear()
    keys.clear()
  }

  const size = () => cache.size

  return { get, set, has, clear, size }
}

export function useOptimizedComputed<T>(
  getter: () => T,
  deps?: Ref[]
): ComputedRef<T> {
  let cachedValue: T
  let isInitialized = false

  return computed(() => {
    if (!isInitialized) {
      cachedValue = getter()
      isInitialized = true
    }
    return cachedValue
  })
}

export function useDebounce<T>(value: Ref<T>, delay: number = 300): Ref<T> {
  const debouncedValue = ref(value.value) as Ref<T>
  let timer: ReturnType<typeof setTimeout> | null = null

  watch(value, (newValue) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      debouncedValue.value = newValue
    }, delay)
  })

  return debouncedValue
}

export function useThrottle<T>(value: Ref<T>, delay: number = 300): Ref<T> {
  const throttledValue = ref(value.value) as Ref<T>
  let lastTime = 0
  let timer: ReturnType<typeof setTimeout> | null = null

  watch(value, (newValue) => {
    const now = Date.now()
    const remaining = delay - (now - lastTime)

    if (remaining <= 0) {
      throttledValue.value = newValue
      lastTime = now
    } else if (!timer) {
      timer = setTimeout(() => {
        throttledValue.value = newValue
        lastTime = Date.now()
        timer = null
      }, remaining)
    }
  })

  return throttledValue
}

export function useLazyLoad(
  callback: () => Promise<void>,
  options: {
    immediate?: boolean
    delay?: number
  } = {}
) {
  const { immediate = false, delay = 100 } = options
  const loading = ref(false)
  const loaded = ref(false)
  const error = ref<Error | null>(null)

  const load = async () => {
    if (loaded.value || loading.value) return

    loading.value = true
    error.value = null

    try {
      await new Promise(resolve => setTimeout(resolve, delay))
      await callback()
      loaded.value = true
    } catch (e) {
      error.value = e as Error
    } finally {
      loading.value = false
    }
  }

  if (immediate) {
    load()
  }

  return { loading, loaded, error, load }
}

export function useRequestCancellation() {
  const abortControllers = new Map<string, AbortController>()

  const createController = (key: string): AbortController => {
    if (abortControllers.has(key)) {
      abortControllers.get(key)?.abort()
    }

    const controller = new AbortController()
    abortControllers.set(key, controller)
    return controller
  }

  const cancelRequest = (key: string) => {
    const controller = abortControllers.get(key)
    if (controller) {
      controller.abort()
      abortControllers.delete(key)
    }
  }

  const cancelAll = () => {
    abortControllers.forEach(controller => controller.abort())
    abortControllers.clear()
  }

  return { createController, cancelRequest, cancelAll }
}

export function useIntersectionObserver(
  options: IntersectionObserverInit = {}
) {
  const observedElements = new Map<Element, (entry: IntersectionObserverEntry) => void>()
  let observer: IntersectionObserver | null = null

  const observe = (element: Element, callback: (entry: IntersectionObserverEntry) => void) => {
    if (!observer) {
      observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          const callback = observedElements.get(entry.target)
          if (callback) callback(entry)
        })
      }, options)
    }

    observedElements.set(element, callback)
    observer.observe(element)
  }

  const unobserve = (element: Element) => {
    if (observer) {
      observer.unobserve(element)
      observedElements.delete(element)
    }
  }

  const disconnect = () => {
    if (observer) {
      observer.disconnect()
      observedElements.clear()
    }
  }

  return { observe, unobserve, disconnect }
}

export function useVirtualList<T>(
  items: Ref<T[]>,
  options: {
    itemHeight: number
    containerHeight: number
    overscan?: number
  } = { itemHeight: 50, containerHeight: 500, overscan: 3 }
) {
  const scrollTop = ref(0)
  const { itemHeight, containerHeight, overscan = 3 } = options

  const visibleCount = Math.ceil(containerHeight / itemHeight)
  const startIndex = computed(() => Math.max(0, Math.floor(scrollTop.value / itemHeight) - overscan))
  const endIndex = computed(() => Math.min(items.value.length, startIndex.value + visibleCount + overscan * 2))

  const visibleItems = computed(() => 
    items.value.slice(startIndex.value, endIndex.value).map((item, index) => ({
      item,
      index: startIndex.value + index,
      style: {
        position: 'absolute' as const,
        top: `${(startIndex.value + index) * itemHeight}px`,
        height: `${itemHeight}px`,
        width: '100%'
      }
    }))
  )

  const totalHeight = computed(() => items.value.length * itemHeight)

  const onScroll = (e: Event) => {
    const target = e.target as HTMLElement
    scrollTop.value = target.scrollTop
  }

  return {
    visibleItems,
    totalHeight,
    onScroll
  }
}
