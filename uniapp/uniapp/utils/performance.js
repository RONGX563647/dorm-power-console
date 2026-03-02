/**
 * 性能优化工具函数库
 */

/**
 * 防抖函数
 * @param {Function} fn 需要防抖的函数
 * @param {Number} delay 延迟时间（毫秒）
 * @returns {Function} 防抖后的函数
 */
export function debounce(fn, delay = 300) {
    let timer = null
    return function(...args) {
        if (timer) clearTimeout(timer)
        timer = setTimeout(() => {
            fn.apply(this, args)
        }, delay)
    }
}

/**
 * 节流函数
 * @param {Function} fn 需要节流的函数
 * @param {Number} delay 延迟时间（毫秒）
 * @returns {Function} 节流后的函数
 */
export function throttle(fn, delay = 300) {
    let lastTime = 0
    return function(...args) {
        const now = Date.now()
        if (now - lastTime >= delay) {
            lastTime = now
            fn.apply(this, args)
        }
    }
}

/**
 * 深拷贝
 * @param {Object} obj 需要拷贝的对象
 * @returns {Object} 拷贝后的对象
 */
export function deepClone(obj) {
    if (obj === null || typeof obj !== 'object') return obj
    if (obj instanceof Date) return new Date(obj)
    if (obj instanceof RegExp) return new RegExp(obj)
    
    const clone = Array.isArray(obj) ? [] : {}
    for (let key in obj) {
        if (obj.hasOwnProperty(key)) {
            clone[key] = deepClone(obj[key])
        }
    }
    return clone
}

/**
 * 内存缓存
 */
export class MemoryCache {
    constructor(maxSize = 50) {
        this.cache = new Map()
        this.maxSize = maxSize
    }
    
    get(key) {
        if (!this.cache.has(key)) return null
        const value = this.cache.get(key)
        this.cache.delete(key)
        this.cache.set(key, value)
        return value
    }
    
    set(key, value) {
        if (this.cache.has(key)) {
            this.cache.delete(key)
        } else if (this.cache.size >= this.maxSize) {
            const firstKey = this.cache.keys().next().value
            this.cache.delete(firstKey)
        }
        this.cache.set(key, value)
    }
    
    has(key) {
        return this.cache.has(key)
    }
    
    delete(key) {
        return this.cache.delete(key)
    }
    
    clear() {
        this.cache.clear()
    }
}

/**
 * 本地存储封装
 */
export const storage = {
    set(key, value, expire = 0) {
        const data = {
            value,
            expire: expire > 0 ? Date.now() + expire * 1000 : 0
        }
        try {
            uni.setStorageSync(key, JSON.stringify(data))
        } catch (e) {
            console.error('Storage set error:', e)
        }
    },
    
    get(key) {
        try {
            const data = uni.getStorageSync(key)
            if (!data) return null
            
            const parsed = JSON.parse(data)
            if (parsed.expire > 0 && Date.now() > parsed.expire) {
                uni.removeStorageSync(key)
                return null
            }
            return parsed.value
        } catch (e) {
            console.error('Storage get error:', e)
            return null
        }
    },
    
    remove(key) {
        try {
            uni.removeStorageSync(key)
        } catch (e) {
            console.error('Storage remove error:', e)
        }
    },
    
    clear() {
        try {
            uni.clearStorageSync()
        } catch (e) {
            console.error('Storage clear error:', e)
        }
    }
}

/**
 * 图片懒加载
 */
export class ImageLazyLoader {
    constructor(options = {}) {
        this.options = {
            threshold: options.threshold || 0.1,
            rootMargin: options.rootMargin || '50px'
        }
        this.observer = null
    }
    
    observe(selector, callback) {
        if (typeof IntersectionObserver === 'undefined') {
            callback()
            return
        }
        
        this.observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    callback(entry.target)
                    this.observer.unobserve(entry.target)
                }
            })
        }, {
            threshold: this.options.threshold,
            rootMargin: this.options.rootMargin
        })
        
        const elements = document.querySelectorAll(selector)
        elements.forEach(el => this.observer.observe(el))
    }
    
    disconnect() {
        if (this.observer) {
            this.observer.disconnect()
        }
    }
}

/**
 * 请求队列管理
 */
export class RequestQueue {
    constructor(maxConcurrent = 5) {
        this.queue = []
        this.running = 0
        this.maxConcurrent = maxConcurrent
    }
    
    add(requestFn) {
        return new Promise((resolve, reject) => {
            this.queue.push({
                requestFn,
                resolve,
                reject
            })
            this.run()
        })
    }
    
    run() {
        while (this.running < this.maxConcurrent && this.queue.length > 0) {
            const { requestFn, resolve, reject } = this.queue.shift()
            this.running++
            
            requestFn()
                .then(resolve)
                .catch(reject)
                .finally(() => {
                    this.running--
                    this.run()
                })
        }
    }
}

/**
 * 性能监控
 */
export class PerformanceMonitor {
    constructor() {
        this.metrics = {}
    }
    
    start(name) {
        this.metrics[name] = {
            start: Date.now()
        }
    }
    
    end(name) {
        if (!this.metrics[name]) return
        this.metrics[name].end = Date.now()
        this.metrics[name].duration = this.metrics[name].end - this.metrics[name].start
        return this.metrics[name].duration
    }
    
    getMetrics() {
        return this.metrics
    }
    
    clear() {
        this.metrics = {}
    }
}

/**
 * 数据分页加载
 */
export class PaginationLoader {
    constructor(options = {}) {
        this.pageSize = options.pageSize || 20
        this.currentPage = 1
        this.hasMore = true
        this.loading = false
        this.data = []
    }
    
    async load(requestFn, reset = false) {
        if (this.loading || (!this.hasMore && !reset)) return
        
        if (reset) {
            this.currentPage = 1
            this.hasMore = true
            this.data = []
        }
        
        this.loading = true
        try {
            const result = await requestFn({
                page: this.currentPage,
                pageSize: this.pageSize
            })
            
            if (result.length < this.pageSize) {
                this.hasMore = false
            }
            
            this.data = reset ? result : [...this.data, ...result]
            this.currentPage++
            
            return {
                data: this.data,
                hasMore: this.hasMore
            }
        } finally {
            this.loading = false
        }
    }
    
    reset() {
        this.currentPage = 1
        this.hasMore = true
        this.data = []
        this.loading = false
    }
}

/**
 * 函数缓存装饰器
 */
export function memoize(fn) {
    const cache = new Map()
    return function(...args) {
        const key = JSON.stringify(args)
        if (cache.has(key)) {
            return cache.get(key)
        }
        const result = fn.apply(this, args)
        cache.set(key, result)
        return result
    }
}

/**
 * 批量处理
 */
export function batchProcess(items, processor, batchSize = 10) {
    return new Promise((resolve) => {
        const results = []
        let index = 0
        
        function processBatch() {
            const batch = items.slice(index, index + batchSize)
            if (batch.length === 0) {
                resolve(results)
                return
            }
            
            Promise.all(batch.map(processor))
                .then(batchResults => {
                    results.push(...batchResults)
                    index += batchSize
                    setTimeout(processBatch, 0)
                })
        }
        
        processBatch()
    })
}

export default {
    debounce,
    throttle,
    deepClone,
    MemoryCache,
    storage,
    ImageLazyLoader,
    RequestQueue,
    PerformanceMonitor,
    PaginationLoader,
    memoize,
    batchProcess
}
