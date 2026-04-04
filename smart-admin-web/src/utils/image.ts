/**
 * 图片优化工具
 * 支持图片懒加载、压缩和预加载
 */

// 图片加载状态
type ImageStatus = 'loading' | 'loaded' | 'error'

// 图片缓存
const imageCache = new Map<string, { status: ImageStatus; data?: string }>()

/**
 * 图片配置
 */
export const imageConfig = {
  // 懒加载阈值（距离可视区域多少像素开始加载）
  lazyLoadThreshold: 100,
  
  // 默认图片质量（用于压缩）
  defaultQuality: 0.8,
  
  // 最大图片宽度
  maxWidth: 1920,
  
  // 最大图片高度
  maxHeight: 1080,
  
  // 支持的图片格式
  supportedFormats: ['image/jpeg', 'image/png', 'image/webp', 'image/gif'],
}

/**
 * 图片懒加载观察器
 */
let lazyLoadObserver: IntersectionObserver | null = null

/**
 * 初始化懒加载观察器
 */
export function initLazyLoadObserver(
  options: {
    threshold?: number
    rootMargin?: string
    onLoad?: (element: HTMLElement) => void
    onError?: (element: HTMLElement) => void
  } = {}
): IntersectionObserver {
  if (lazyLoadObserver) {
    return lazyLoadObserver
  }
  
  const {
    threshold = 0.1,
    rootMargin = `${imageConfig.lazyLoadThreshold}px`,
    onLoad,
    onError,
  } = options
  
  lazyLoadObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          const element = entry.target as HTMLElement
          const src = element.dataset.lazySrc || element.dataset.src
          
          if (src) {
            loadImage(src)
              .then(() => {
                if (element.tagName === 'IMG') {
                  (element as HTMLImageElement).src = src
                } else {
                  element.style.backgroundImage = `url(${src})`
                }
                element.removeAttribute('data-lazy-src')
                element.removeAttribute('data-src')
                onLoad?.(element)
              })
              .catch(() => {
                onError?.(element)
              })
            
            lazyLoadObserver?.unobserve(element)
          }
        }
      })
    },
    {
      threshold,
      rootMargin,
    }
  )
  
  return lazyLoadObserver
}

/**
 * 添加懒加载元素
 */
export function addLazyLoadElement(element: HTMLElement): void {
  const observer = initLazyLoadObserver()
  observer.observe(element)
}

/**
 * 移除懒加载元素
 */
export function removeLazyLoadElement(element: HTMLElement): void {
  lazyLoadObserver?.unobserve(element)
}

/**
 * 加载图片
 */
export function loadImage(src: string): Promise<string> {
  const cached = imageCache.get(src)
  
  if (cached) {
    if (cached.status === 'loaded') {
      return Promise.resolve(src)
    }
    if (cached.status === 'error') {
      return Promise.reject(new Error('Image load failed'))
    }
  }
  
  imageCache.set(src, { status: 'loading' })
  
  return new Promise((resolve, reject) => {
    const img = new Image()
    
    img.onload = () => {
      imageCache.set(src, { status: 'loaded', data: src })
      resolve(src)
    }
    
    img.onerror = () => {
      imageCache.set(src, { status: 'error' })
      reject(new Error(`Failed to load image: ${src}`))
    }
    
    img.src = src
  })
}

/**
 * 预加载图片列表
 */
export function preloadImages(
  urls: string[],
  options: {
    concurrency?: number
    onProgress?: (loaded: number, total: number) => void
  } = {}
): Promise<string[]> {
  const { concurrency = 3, onProgress } = options
  
  let loaded = 0
  const total = urls.length
  const results: string[] = []
  
  return new Promise((resolve, reject) => {
    const queue = [...urls]
    const loading = new Set<Promise<string>>()
    
    const loadNext = () => {
      while (queue.length > 0 && loading.size < concurrency) {
        const url = queue.shift()
        if (url) {
          const promise = loadImage(url)
            .then((src) => {
              results.push(src)
              loaded++
              onProgress?.(loaded, total)
              loading.delete(promise)
              loadNext()
            })
            .catch((error) => {
              loading.delete(promise)
              loadNext()
            })
          
          loading.add(promise)
        }
      }
      
      if (queue.length === 0 && loading.size === 0) {
        resolve(results)
      }
    }
    
    loadNext()
    
    // 失败处理
    setTimeout(() => {
      if (loading.size > 0) {
        reject(new Error('Preload timeout'))
      }
    }, 30000)
  })
}

/**
 * 压缩图片（客户端压缩）
 */
export function compressImage(
  file: File | Blob,
  options: {
    quality?: number
    maxWidth?: number
    maxHeight?: number
    type?: string
  } = {}
): Promise<Blob> {
  const {
    quality = imageConfig.defaultQuality,
    maxWidth = imageConfig.maxWidth,
    maxHeight = imageConfig.maxHeight,
    type = 'image/jpeg',
  } = options
  
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    
    reader.onload = (e) => {
      const img = new Image()
      
      img.onload = () => {
        // 计算缩放尺寸
        let width = img.width
        let height = img.height
        
        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height)
          width = width * ratio
          height = height * ratio
        }
        
        // 创建画布
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('Canvas context unavailable'))
          return
        }
        
        // 绘制图片
        ctx.drawImage(img, 0, 0, width, height)
        
        // 转换为 Blob
        canvas.toBlob(
          (blob) => {
            if (blob) {
              resolve(blob)
            } else {
              reject(new Error('Failed to create blob'))
            }
          },
          type,
          quality
        )
      }
      
      img.onerror = () => {
        reject(new Error('Failed to load image for compression'))
      }
      
      img.src = e.target?.result as string
    }
    
    reader.onerror = () => {
      reject(new Error('Failed to read file'))
    }
    
    reader.readAsDataURL(file)
  })
}

/**
 * 图片尺寸获取
 */
export function getImageSize(src: string): Promise<{ width: number; height: number }> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    
    img.onload = () => {
      resolve({
        width: img.width,
        height: img.height,
      })
    }
    
    img.onerror = () => {
      reject(new Error('Failed to get image size'))
    }
    
    img.src = src
  })
}

/**
 * 检查图片格式是否支持
 */
export function isImageFormatSupported(type: string): boolean {
  return imageConfig.supportedFormats.includes(type)
}

/**
 * 清理图片缓存
 */
export function clearImageCache(): void {
  imageCache.clear()
}

/**
 * 销毁懒加载观察器
 */
export function destroyLazyLoadObserver(): void {
  if (lazyLoadObserver) {
    lazyLoadObserver.disconnect()
    lazyLoadObserver = null
  }
}

// 默认导出
export default {
  initLazyLoadObserver,
  addLazyLoadElement,
  removeLazyLoadElement,
  loadImage,
  preloadImages,
  compressImage,
  getImageSize,
  isImageFormatSupported,
  clearImageCache,
  destroyLazyLoadObserver,
  imageConfig,
}