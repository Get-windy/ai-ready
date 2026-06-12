/**
 * 图片懒加载组件
 * 使用 Intersection Observer API 实现图片懒加载
 */

import { defineComponent, onMounted, ref, onUnmounted } from 'vue'
import { Image } from 'ant-design-vue'
import { PictureOutlined } from '@ant-design/icons-vue'

/**
 * 图片懒加载选项
 */
interface LazyImageOptions {
  /** 图片占位符 */
  placeholder?: string
  /** 是否使用模糊占位图 */
  blurPlaceholder?: boolean
  /** 根边距（px） */
  rootMargin?: string
  /** 交叉阈值 */
  threshold?: number
  /** 失败重试次数 */
  retryCount?: number
}

/**
 * 默认占位图（Base64）
 */
const DEFAULT_PLACEHOLDER = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyMDAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMjAwIDIwMCI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iI2Y1ZjVmNSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkeT0iLjNlbSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzk5OSI+5aSx6L+H5aSN5rWL5bm76ZyyPC90ZXh0Pjwvc3ZnPg=='

export const LazyImage = defineComponent({
  name: 'LazyImage',
  props: {
    /** 图片地址 */
    src: {
      type: String,
      required: true
    },
    /** 占位符 */
    placeholder: {
      type: String,
      default: DEFAULT_PLACEHOLDER
    },
    /** 图片宽度 */
    width: {
      type: [String, Number],
      default: '100%'
    },
    /** 图片高度 */
    height: {
      type: [String, Number],
      default: 'auto'
    },
    /** 加载失败重试次数 */
    retryCount: {
      type: Number,
      default: 2
    },
    /** 是否预加载 */
    preload: {
      type: Boolean,
      default: false
    },
    /** 根边距 */
    rootMargin: {
      type: String,
      default: '50px'
    },
    /** 交叉阈值 */
    threshold: {
      type: Number,
      default: 0.1
    },
    /** Alt 文本 */
    alt: {
      type: String,
      default: ''
    },
    /** 图片描述 */
    preview: {
      type: Boolean,
      default: true
    },
    /** 是否支持图片预览 */
    fit: {
      type: String as () => 'fill' | 'contain' | 'cover' | 'none' | 'scale-down',
      default: 'contain'
    }
  },
  emits: ['load', 'error', 'click'],
  setup(props, { emit }) {
    const visible = ref(false)
    const loaded = ref(false)
    const error = ref(false)
    const retryTimes = ref(0)
    const imageRef = ref<HTMLElement | null>(null)
    let observer: IntersectionObserver | null = null

    // 加载图片
    const loadImage = () => {
      if (visible.value && !loaded.value && !error.value) {
        const img = new window.Image()

        img.onload = () => {
          loaded.value = true
          error.value = false
          emit('load', img)
        }

        img.onerror = () => {
          retryTimes.value++
          if (retryTimes.value <= props.retryCount) {
            // 重试加载
            setTimeout(() => {
              loadImage()
            }, 1000)
          } else {
            error.value = true
            loaded.value = false
            emit('error', new Error(`Failed to load image after ${props.retryCount} retries`))
          }
        }

        img.src = props.src
      }
    }

    // 处理图片点击
    const handleClick = () => {
      emit('click')
    }

    // 初始化 Intersection Observer
    const initObserver = () => {
      if (!imageRef.value) return

      observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              visible.value = true
              loadImage()
              // 图片进入视图后停止观察
              if (observer) {
                observer.unobserve(entry.target)
              }
            }
          })
        },
        {
          rootMargin: props.rootMargin,
          threshold: props.threshold
        }
      )

      observer.observe(imageRef.value)
    }

    onMounted(() => {
      if (props.preload) {
        // 如果需要预加载，直接加载图片
        visible.value = true
        loadImage()
      } else {
        // 否则使用懒加载
        initObserver()
      }
    })

    onUnmounted(() => {
      if (observer) {
        observer.disconnect()
        observer = null
      }
    })

    return () => {
      const style = {
        width: typeof props.width === 'number' ? `${props.width}px` : props.width,
        height: typeof props.height === 'number' ? `${props.height}px` : props.height,
        objectFit: props.fit,
        display: 'block'
      }

      // 占位图样式
      const placeholderStyle = {
        ...style,
        backgroundColor: 'var(--ar-fill-color-light, #f5f7fa)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }

      return (
        <div ref={imageRef} class="lazy-image-container" style={style}>
          {loaded.value ? (
            <Image
              src={props.src}
              alt={props.alt}
              preview={props.preview}
              style={style}
              onClick={handleClick}
            />
          ) : (
            <div class="lazy-image-placeholder" style={placeholderStyle}>
              {error.value ? (
                <div class="lazy-image-error">
                  <PictureOutlined style={{ fontSize: '32px', color: 'var(--ar-border-color-dark, #d4d7de)' }} />
                  <p style={{ marginTop: '8px', color: 'var(--ar-text-color-secondary, #909399)' }}>图片加载失败</p>
                </div>
              ) : (
                <div class="lazy-image-loading">
                  <div class="loading-spinner" />
                  <p style={{ marginTop: '8px', color: 'var(--ar-text-color-secondary, #909399)' }}>加载中...</p>
                </div>
              )}
            </div>
          )}
        </div>
      )
    }
  }
})

/**
 * 批量预加载图片
 * @param images 图片地址数组
 */
export function preloadImages(images: string[]): Promise<void[]> {
  const promises = images.map((src) => {
    return new Promise<void>((resolve, reject) => {
      const img = new window.Image()
      img.onload = () => resolve()
      img.onerror = () => reject(new Error(`Failed to preload image: ${src}`))
      img.src = src
    })
  })

  return Promise.all(promises)
}

export default LazyImage