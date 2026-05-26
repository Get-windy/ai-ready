import { defineComponent, ref, onErrorCaptured, provide, type ComponentPublicInstance, type PropType } from 'vue'
import ErrorBoundaryComponent from './ErrorBoundary.vue'

/**
 * 创建一个带自定义降级组件的错误边界
 * @param fallbackComponent 自定义降级组件
 */
export function createErrorBoundary(fallbackComponent?: any) {
  return defineComponent({
    name: 'DynamicErrorBoundary',
    
    props: {
      fallback: {
        type: Object as PropType<any>,
        default: null
      },
      onError: {
        type: Function as PropType<(error: Error, instance: ComponentPublicInstance, info: string) => void>,
        default: undefined
      },
      retryable: {
        type: Boolean,
        default: true
      }
    },
    
    setup(props, { slots }) {
      const hasError = ref(false)
      const capturedError = ref<Error | null>(null)
      const errorInfo = ref('')
      
      // 提供错误恢复方法给子组件
      provide('errorBoundary', {
        resetError: () => {
          hasError.value = false
          capturedError.value = null
          errorInfo.value = ''
        },
        hasError
      })
      
      onErrorCaptured((error: Error, instance: ComponentPublicInstance, info: string) => {
        hasError.value = true
        capturedError.value = error
        errorInfo.value = info
        
        props.onError?.(error, instance, info)
        
        return false // 阻止错误继续传播
      })
      
      return () => {
        if (hasError.value) {
          // 使用自定义降级组件或默认降级
          if (props.fallback) {
            return h(props.fallback, {
              error: capturedError.value,
              errorInfo: errorInfo.value,
              onRetry: () => {
                hasError.value = false
                capturedError.value = null
                errorInfo.value = ''
              }
            })
          }
          
          if (fallbackComponent) {
            return h(fallbackComponent, {
              error: capturedError.value,
              errorInfo: errorInfo.value
            })
          }
          
          // 使用默认的 ErrorBoundary 组件
          return h(ErrorBoundaryComponent, {
            onError: props.onError,
            retryable: props.retryable
          })
        }
        
        return slots.default?.()
      }
    }
  })
}

/**
 * 错误边界高阶组件（HOC）
 * 用于包装单个组件
 */
export function withErrorBoundary(
  WrappedComponent: any,
  options?: {
    fallback?: any
    onError?: (error: Error, instance: ComponentPublicInstance, info: string) => void
  }
) {
  return defineComponent({
    name: `WithErrorBoundary(${WrappedComponent.name || 'Component'})`,
    
    setup(_, { slots }) {
      const hasError = ref(false)
      
      onErrorCaptured((error: Error, instance: ComponentPublicInstance, info: string) => {
        hasError.value = true
        
        options?.onError?.(error, instance, info)
        
        return false
      })
      
      return () => {
        if (hasError.value) {
          if (options?.fallback) {
            return h(options.fallback, {
              onRetry: () => { hasError.value = false }
            })
          }
          
          return h(ErrorBoundaryComponent, {
            onRetry: () => { hasError.value = false }
          })
        }
        
        return h(WrappedComponent)
      }
    }
  })
}

/**
 * 异步组件错误处理包装器
 * 用于处理异步加载的组件错误
 */
export function createAsyncComponentWithErrorBoundary(
  asyncComponentLoader: () => Promise<any>,
  options?: {
    loadingComponent?: any
    errorComponent?: any
    onError?: (error: Error) => void
    delay?: number
    timeout?: number
  }
) {
  return defineComponent({
    name: 'AsyncErrorBoundary',
    
    setup() {
      const loading = ref(true)
      const hasError = ref(false)
      const loadedComponent = ref<any>(null)
      
      const loadComponent = async () => {
        loading.value = true
        hasError.value = false
        
        try {
          const component = await asyncComponentLoader()
          loadedComponent.value = component
        } catch (err) {
          hasError.value = true
          options?.onError?.(err as Error)
        } finally {
          loading.value = false
        }
      }
      
      loadComponent()
      
      return () => {
        if (loading.value && options?.loadingComponent) {
          return h(options.loadingComponent)
        }
        
        if (hasError.value) {
          if (options?.errorComponent) {
            return h(options.errorComponent, {
              onRetry: loadComponent
            })
          }
          
          return h(ErrorBoundaryComponent, {
            onRetry: loadComponent
          })
        }
        
        if (loadedComponent.value) {
          return h(loadedComponent.value)
        }
        
        return null
      }
    }
  })
}

// 导入 h 函数
import { h } from 'vue'

export default {
  createErrorBoundary,
  withErrorBoundary,
  createAsyncComponentWithErrorBoundary
}