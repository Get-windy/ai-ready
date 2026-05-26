/**
 * 预加载工具
 * 用于资源预加载、预获取
 */

// 预加载状态
type PreloadStatus = 'pending' | 'loading' | 'loaded' | 'failed'

// 预加载资源类型
type ResourceType = 'script' | 'style' | 'image' | 'font' | 'fetch'

// 预加载资源配置
interface PreloadResource {
  url: string
  type: ResourceType
  as?: string
  crossOrigin?: boolean
  integrity?: string
  priority?: 'high' | 'low' | 'auto'
}

// 预加载资源状态映射
const resourceStatus = new Map<string, PreloadStatus>()

/**
 * 资源预加载器
 */
export class ResourcePreloader {
  private loadedResources = new Set<string>()
  private loadingResources = new Map<string, Promise<any>>()
  
  /**
   * 预加载单个资源
   */
  async preload(resource: PreloadResource): Promise<any> {
    const { url, type, as, crossOrigin, integrity, priority = 'auto' } = resource
    
    // 已加载
    if (this.loadedResources.has(url)) {
      return Promise.resolve()
    }
    
    // 正在加载
    if (this.loadingResources.has(url)) {
      return this.loadingResources.get(url)
    }
    
    // 创建加载 Promise
    const loadPromise = this.loadResource(resource)
    this.loadingResources.set(url, loadPromise)
    
    try {
      await loadPromise
      this.loadedResources.add(url)
      resourceStatus.set(url, 'loaded')
    } catch (error) {
      resourceStatus.set(url, 'failed')
      throw error
    } finally {
      this.loadingResources.delete(url)
    }
  }
  
  /**
   * 批量预加载资源
   */
  async preloadAll(resources: PreloadResource[]): Promise<void[]> {
    return Promise.all(resources.map(r => this.preload(r)))
  }
  
  /**
   * 加载单个资源
   */
  private loadResource(resource: PreloadResource): Promise<any> {
    const { url, type, as, crossOrigin, integrity } = resource
    
    resourceStatus.set(url, 'loading')
    
    switch (type) {
      case 'script':
        return this.loadScript(url, crossOrigin, integrity)
      case 'style':
        return this.loadStyle(url, crossOrigin, integrity)
      case 'image':
        return this.loadImage(url)
      case 'font':
        return this.loadFont(url, crossOrigin, integrity)
      case 'fetch':
        return fetch(url).then(() => {})
      default:
        return Promise.resolve()
    }
  }
  
  /**
   * 加载脚本
   */
  private loadScript(url: string, crossOrigin?: boolean, integrity?: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = url
      script.async = true
      
      if (crossOrigin) script.crossOrigin = 'anonymous'
      if (integrity) script.integrity = integrity
      
      script.onload = () => resolve()
      script.onerror = () => reject(new Error(`Failed to load script: ${url}`))
      
      document.head.appendChild(script)
    })
  }
  
  /**
   * 加载样式
   */
  private loadStyle(url: string, crossOrigin?: boolean, integrity?: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const link = document.createElement('link')
      link.rel = 'stylesheet'
      link.href = url
      
      if (crossOrigin) link.crossOrigin = 'anonymous'
      if (integrity) link.integrity = integrity
      
      link.onload = () => resolve()
      link.onerror = () => reject(new Error(`Failed to load style: ${url}`))
      
      document.head.appendChild(link)
    })
  }
  
  /**
   * 加载图片
   */
  private loadImage(url: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const img = new Image()
      img.onload = () => resolve()
      img.onerror = () => reject(new Error(`Failed to load image: ${url}`))
      img.src = url
    })
  }
  
  /**
   * 加载字体
   */
  private loadFont(url: string, crossOrigin?: boolean, integrity?: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const link = document.createElement('link')
      link.rel = 'preload'
      link.as = 'font'
      link.type = 'font/woff2'
      link.href = url
      
      if (crossOrigin) link.crossOrigin = 'anonymous'
      if (integrity) link.integrity = integrity
      
      link.onload = () => resolve()
      link.onerror = () => reject(new Error(`Failed to load font: ${url}`))
      
      document.head.appendChild(link)
    })
  }
  
  /**
   * 获取资源加载状态
   */
  getStatus(url: string): PreloadStatus {
    return resourceStatus.get(url) || 'pending'
  }
  
  /**
   * 清除已加载缓存
   */
  clear() {
    this.loadedResources.clear()
    this.loadingResources.clear()
    resourceStatus.clear()
  }
}

// 全局预加载器实例
export const resourcePreloader = new ResourcePreloader()

/**
 * DNS 预解析
 */
export function dnsPrefetch(domain: string) {
  const link = document.createElement('link')
  link.rel = 'dns-prefetch'
  link.href = domain
  document.head.appendChild(link)
}

/**
 * 预连接
 */
export function preconnect(origin: string, crossOrigin = true) {
  const link = document.createElement('link')
  link.rel = 'preconnect'
  link.href = origin
  if (crossOrigin) link.crossOrigin = 'anonymous'
  document.head.appendChild(link)
}

/**
 * 预获取资源
 */
export function prefetch(url: string, as?: string) {
  const link = document.createElement('link')
  link.rel = 'prefetch'
  link.href = url
  if (as) link.as = as
  document.head.appendChild(link)
}

/**
 * 页面预加载
 * 用于预加载用户可能访问的页面
 */
export function prefetchPage(path: string) {
  // 使用动态 import 预加载页面组件
  const pageModule = import(/* webpackPrefetch: true */ `@/views${path}.vue`)
  return pageModule
}

/**
 * 路由预加载钩子
 * 在路由切换时预加载下一个可能访问的路由
 */
export function setupRoutePreload(router: any) {
  // 鼠标悬停在链接上时预加载
  document.addEventListener('mouseover', (e) => {
    const target = e.target as HTMLElement
    const link = target.closest('a[href]')
    
    if (link) {
      const href = link.getAttribute('href')
      if (href && href.startsWith('/') && !isRouteLoaded(href)) {
        prefetchPage(href)
      }
    }
  }, { passive: true })
  
  // 触摸开始时预加载（移动端）
  document.addEventListener('touchstart', (e) => {
    const target = e.target as HTMLElement
    const link = target.closest('a[href]')
    
    if (link) {
      const href = link.getAttribute('href')
      if (href && href.startsWith('/') && !isRouteLoaded(href)) {
        prefetchPage(href)
      }
    }
  }, { passive: true })
}

/**
 * 检查路由是否已加载
 */
function isRouteLoaded(path: string): boolean {
  return resourceStatus.has(`route:${path}`)
}

/**
 * 视口内预加载
 * 用于预加载视口内的资源
 */
export function setupViewportPreload() {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        const el = entry.target as HTMLElement
        
        // 预加载图片
        const imgSrc = el.getAttribute('data-preload-image')
        if (imgSrc) {
          resourcePreloader.preload({ url: imgSrc, type: 'image' })
        }
        
        // 预加载链接
        const linkHref = el.getAttribute('data-preload-link')
        if (linkHref) {
          prefetchPage(linkHref)
        }
        
        observer.unobserve(el)
      }
    })
  }, {
    rootMargin: '100px'
  })
  
  // 观察所有带预加载属性的元素
  document.querySelectorAll('[data-preload-image], [data-preload-link]').forEach((el) => {
    observer.observe(el)
  })
}

/**
 * 初始化预加载
 */
export function initPreload() {
  // DNS 预解析
  dnsPrefetch('//cdn.jsdelivr.net')
  dnsPrefetch('//fonts.googleapis.com')
  
  // 预连接 API
  preconnect(window.location.origin)
  
  // 设置视口预加载
  setupViewportPreload()
}

export default {
  ResourcePreloader,
  resourcePreloader,
  dnsPrefetch,
  preconnect,
  prefetch,
  prefetchPage,
  setupRoutePreload,
  setupViewportPreload,
  initPreload
}