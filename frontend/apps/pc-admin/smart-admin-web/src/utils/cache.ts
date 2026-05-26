/**
 * 前端缓存工具
 * 支持内存缓存、LocalStorage、SessionStorage、IndexedDB
 */

// 缓存类型
type CacheType = 'memory' | 'local' | 'session' | 'indexedDB'

// 缓存配置
interface CacheConfig {
  type: CacheType
  prefix?: string
  maxAge?: number // 过期时间（毫秒）
  maxSize?: number // 最大缓存数量（仅内存缓存）
}

// 缓存项
interface CacheItem<T> {
  data: T
  timestamp: number
  maxAge?: number
}

/**
 * 内存缓存类
 */
export class MemoryCache<T = any> {
  private cache = new Map<string, CacheItem<T>>()
  private maxSize: number
  
  constructor(maxSize = 100) {
    this.maxSize = maxSize
  }
  
  set(key: string, data: T, maxAge?: number): void {
    // LRU 淘汰策略
    if (this.cache.size >= this.maxSize) {
      const oldestKey = this.cache.keys().next().value
      if (oldestKey) {
        this.cache.delete(oldestKey)
      }
    }
    
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      maxAge
    })
  }
  
  get(key: string): T | null {
    const item = this.cache.get(key)
    
    if (!item) return null
    
    // 检查过期
    if (item.maxAge && Date.now() - item.timestamp > item.maxAge) {
      this.cache.delete(key)
      return null
    }
    
    // 更新访问顺序（LRU）
    this.cache.delete(key)
    this.cache.set(key, item)
    
    return item.data
  }
  
  has(key: string): boolean {
    return this.get(key) !== null
  }
  
  delete(key: string): boolean {
    return this.cache.delete(key)
  }
  
  clear(): void {
    this.cache.clear()
  }
  
  get size(): number {
    return this.cache.size
  }
}

/**
 * Storage 缓存类
 */
export class StorageCache<T = any> {
  private storage: Storage
  private prefix: string
  
  constructor(type: 'local' | 'session', prefix = 'cache_') {
    this.storage = type === 'local' ? localStorage : sessionStorage
    this.prefix = prefix
  }
  
  private getKey(key: string): string {
    return `${this.prefix}${key}`
  }
  
  set(key: string, data: T, maxAge?: number): void {
    const item: CacheItem<T> = {
      data,
      timestamp: Date.now(),
      maxAge
    }
    
    try {
      this.storage.setItem(this.getKey(key), JSON.stringify(item))
    } catch (error) {
      console.warn('Storage quota exceeded:', error)
      this.clearExpired()
    }
  }
  
  get(key: string): T | null {
    try {
      const raw = this.storage.getItem(this.getKey(key))
      if (!raw) return null
      
      const item: CacheItem<T> = JSON.parse(raw)
      
      // 检查过期
      if (item.maxAge && Date.now() - item.timestamp > item.maxAge) {
        this.delete(key)
        return null
      }
      
      return item.data
    } catch {
      return null
    }
  }
  
  has(key: string): boolean {
    return this.get(key) !== null
  }
  
  delete(key: string): void {
    this.storage.removeItem(this.getKey(key))
  }
  
  clear(): void {
    const keysToRemove: string[] = []
    
    for (let i = 0; i < this.storage.length; i++) {
      const key = this.storage.key(i)
      if (key?.startsWith(this.prefix)) {
        keysToRemove.push(key)
      }
    }
    
    keysToRemove.forEach(key => this.storage.removeItem(key))
  }
  
  clearExpired(): void {
    const keysToRemove: string[] = []
    
    for (let i = 0; i < this.storage.length; i++) {
      const key = this.storage.key(i)
      if (key?.startsWith(this.prefix)) {
        try {
          const raw = this.storage.getItem(key)
          if (raw) {
            const item = JSON.parse(raw)
            if (item.maxAge && Date.now() - item.timestamp > item.maxAge) {
              keysToRemove.push(key)
            }
          }
        } catch {
          keysToRemove.push(key)
        }
      }
    }
    
    keysToRemove.forEach(key => this.storage.removeItem(key))
  }
}

/**
 * 统一缓存管理器
 */
export class CacheManager {
  private memoryCache: MemoryCache
  private localCache: StorageCache
  private sessionCache: StorageCache
  
  constructor() {
    this.memoryCache = new MemoryCache(200)
    this.localCache = new StorageCache('local')
    this.sessionCache = new StorageCache('session')
  }
  
  /**
   * 设置缓存
   */
  set<T>(key: string, data: T, options?: CacheConfig): void {
    const { type = 'memory', maxAge } = options || {}
    
    switch (type) {
      case 'memory':
        this.memoryCache.set(key, data, maxAge)
        break
      case 'local':
        this.localCache.set(key, data, maxAge)
        break
      case 'session':
        this.sessionCache.set(key, data, maxAge)
        break
    }
  }
  
  /**
   * 获取缓存
   */
  get<T>(key: string, type: CacheType = 'memory'): T | null {
    switch (type) {
      case 'memory':
        return this.memoryCache.get(key) as T
      case 'local':
        return this.localCache.get(key) as T
      case 'session':
        return this.sessionCache.get(key) as T
      default:
        return null
    }
  }
  
  /**
   * 多级缓存获取（内存 -> Session -> Local）
   */
  getMultiLevel<T>(key: string): T | null {
    // 1. 内存缓存
    let data = this.memoryCache.get(key) as T
    if (data !== null) return data
    
    // 2. Session 缓存
    data = this.sessionCache.get(key) as T
    if (data !== null) {
      this.memoryCache.set(key, data)
      return data
    }
    
    // 3. Local 缓存
    data = this.localCache.get(key) as T
    if (data !== null) {
      this.memoryCache.set(key, data)
      this.sessionCache.set(key, data)
      return data
    }
    
    return null
  }
  
  /**
   * 删除缓存
   */
  delete(key: string, type?: CacheType): void {
    if (!type) {
      this.memoryCache.delete(key)
      this.localCache.delete(key)
      this.sessionCache.delete(key)
    } else {
      switch (type) {
        case 'memory':
          this.memoryCache.delete(key)
          break
        case 'local':
          this.localCache.delete(key)
          break
        case 'session':
          this.sessionCache.delete(key)
          break
      }
    }
  }
  
  /**
   * 清空所有缓存
   */
  clear(): void {
    this.memoryCache.clear()
    this.localCache.clear()
    this.sessionCache.clear()
  }
  
  /**
   * 清理过期缓存
   */
  cleanup(): void {
    this.localCache.clearExpired()
    this.sessionCache.clearExpired()
  }
}

// 全局缓存实例
export const cache = new CacheManager()

/**
 * API 缓存装饰器
 */
export function cacheAPI(maxAge = 5 * 60 * 1000) {
  return function (
    target: any,
    propertyKey: string,
    descriptor: PropertyDescriptor
  ) {
    const originalMethod = descriptor.value
    
    descriptor.value = async function (...args: any[]) {
      const cacheKey = `${propertyKey}_${JSON.stringify(args)}`
      
      // 尝试从缓存获取
      const cached = cache.get(cacheKey, 'memory')
      if (cached !== null) {
        return cached
      }
      
      // 调用原方法
      const result = await originalMethod.apply(this, args)
      
      // 存入缓存
      cache.set(cacheKey, result, { type: 'memory', maxAge })
      
      return result
    }
    
    return descriptor
  }
}

export default {
  MemoryCache,
  StorageCache,
  CacheManager,
  cache,
  cacheAPI
}