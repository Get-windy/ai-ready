import { defineAsyncComponent, type Component, type App } from 'vue'

/**
 * 懒加载工具类
 * 用于组件和路由的懒加载优化
 */

// 加载状态缓存
const loadingComponents = new Map<string, Promise<Component>>()

// 组件加载错误处理
const errorComponents = new Map<string, Component>()

// 默认加载中组件
const DefaultLoadingComponent = {
  name: 'AsyncLoading',
  template: `
    <div class="async-loading">
      <a-spin />
      <span>加载中...</span>
    </div>
  `
}

// 默认错误组件
const DefaultErrorComponent = {
  name: 'AsyncError',
  template: `
    <div class="async-error">
      <a-result
        status="error"
        title="加载失败"
        sub-title="组件加载失败，请刷新页面重试"
      >
        <template #extra>
          <a-button type="primary" @click="handleRetry">
            重新加载
          </a-button>
        </template>
      </a-result>
    </div>
  `,
  methods: {
    handleRetry() {
      window.location.reload()
    }
  }
}

/**
 * 创建懒加载组件
 * @param loader 组件加载函数
 * @param options 配置选项
 */
export function lazyLoad(
  loader: () => Promise<{ default: Component }>,
  options?: {
    loadingComponent?: Component
    errorComponent?: Component
    delay?: number
    timeout?: number
    retryCount?: number
  }
) {
  const {
    loadingComponent = DefaultLoadingComponent,
    errorComponent = DefaultErrorComponent,
    delay = 200,
    timeout = 10000,
    retryCount = 3
  } = options || {}

  return defineAsyncComponent({
    loader: () => withRetry(loader, retryCount),
    loadingComponent,
    errorComponent,
    delay,
    timeout
  })
}

/**
 * 带重试的加载函数
 */
async function withRetry(
  loader: () => Promise<{ default: Component }>,
  retryCount: number
): Promise<Component> {
  let lastError: Error | null = null
  
  for (let i = 0; i < retryCount; i++) {
    try {
      const module = await loader()
      return module.default
    } catch (error) {
      lastError = error as Error
      console.warn(`加载失败，第 ${i + 1} 次重试...`, error)
      await delayMs(1000 * (i + 1)) // 指数退避
    }
  }
  
  throw lastError
}

/**
 * 延迟函数
 */
function delayMs(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 路由懒加载
 * 用于路由配置中的组件懒加载
 */
export function lazyRoute(
  importFn: () => Promise<any>,
  options?: {
    loadingComponent?: Component
    preload?: boolean
  }
) {
  const { preload = false } = options || {}
  
  if (preload) {
    // 预加载组件
    importFn().catch(err => {
      console.warn('预加载失败:', err)
    })
  }
  
  return lazyLoad(importFn, options)
}

/**
 * 组件预加载器
 * 用于在用户可能访问某个页面前提前加载组件
 */
export class ComponentPreloader {
  private preloadedComponents = new Map<string, Promise<Component>>()
  private preloadQueue: Array<() => Promise<any>> = []
  private isProcessing = false
  
  /**
   * 添加预加载任务
   */
  add(key: string, loader: () => Promise<{ default: Component }>) {
    if (this.preloadedComponents.has(key)) {
      return
    }
    
    this.preloadQueue.push(async () => {
      const component = await lazyLoad(loader)()
      this.preloadedComponents.set(key, Promise.resolve(component))
    })
    
    this.processQueue()
  }
  
  /**
   * 批量添加预加载任务
   */
  addAll(tasks: Array<{ key: string; loader: () => Promise<{ default: Component }> }>) {
    tasks.forEach(({ key, loader }) => {
      this.add(key, loader)
    })
  }
  
  /**
   * 处理预加载队列
   */
  private async processQueue() {
    if (this.isProcessing || this.preloadQueue.length === 0) {
      return
    }
    
    this.isProcessing = true
    
    while (this.preloadQueue.length > 0) {
      const task = this.preloadQueue.shift()
      if (task) {
        try {
          await task()
        } catch (error) {
          console.warn('预加载任务失败:', error)
        }
        // 每个任务之间间隔 100ms，避免阻塞主线程
        await delayMs(100)
      }
    }
    
    this.isProcessing = false
  }
  
  /**
   * 获取已预加载的组件
   */
  get(key: string): Promise<Component> | undefined {
    return this.preloadedComponents.get(key)
  }
  
  /**
   * 清除预加载缓存
   */
  clear() {
    this.preloadedComponents.clear()
    this.preloadQueue = []
  }
}

// 全局预加载器实例
export const preloader = new ComponentPreloader()

/**
 * 图片懒加载指令
 */
export const lazyImageDirective = {
  mounted(el: HTMLImageElement, binding: { value: string }) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          el.src = binding.value
          el.classList.add('loaded')
          observer.unobserve(el)
        }
      })
    }, {
      rootMargin: '50px'
    })
    
    observer.observe(el)
    
    // 存储观察器实例以便清理
    ;(el as any)._observer = observer
  },
  
  unmounted(el: HTMLImageElement) {
    const observer = (el as any)._observer
    if (observer) {
      observer.disconnect()
    }
  }
}

/**
 * 安装懒加载插件
 */
export function setupLazyLoading(app: App) {
  // 注册图片懒加载指令
  app.directive('lazy-image', lazyImageDirective)
  
  // 全局属性
  app.config.globalProperties.$preloader = preloader
}

export default {
  lazyLoad,
  lazyRoute,
  ComponentPreloader,
  preloader,
  lazyImageDirective,
  setupLazyLoading
}