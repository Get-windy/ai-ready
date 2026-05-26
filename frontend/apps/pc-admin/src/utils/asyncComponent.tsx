/**
 * 组件懒加载工具
 * 用于封装 Vue 的 defineAsyncComponent，提供加载状态处理
 */

import { defineAsyncComponent, type AsyncComponentLoader } from 'vue'
import { Spin } from 'ant-design-vue'
import { LoadingOutlined } from '@ant-design/icons-vue'

/**
 * 组件加载状态配置
 */
export interface AsyncComponentOptions {
  /** 加载延迟（ms），避免闪烁 */
  delay?: number
  /** 超时时间（ms） */
  timeout?: number
  /** 加载失败的组件 */
  errorComponent?: any
  /** 是否可重试 */
  retryable?: boolean
  /** 最大重试次数 */
  maxRetries?: number
  /** 自定义加载状态 */
  loadingComponent?: any
}

/**
 * 默认加载组件
 */
const DefaultLoadingComponent = {
  template: `
    <div class="async-component-loading">
      <a-spin :indicator="indicator" />
    </div>
  `,
  components: { Spin },
  setup() {
    return {
      indicator: () => {
        return <LoadingOutlined spin style="font-size: 24px" />
      }
    }
  }
}

/**
 * 默认错误组件
 */
const DefaultErrorComponent = {
  template: `
    <div class="async-component-error">
      <p>组件加载失败，请刷新页面重试</p>
      <a-button type="primary" @click="retry" v-if="retryable">
        重试
      </a-button>
    </div>
  `,
  emits: ['retry'],
  props: {
    retryable: {
      type: Boolean,
      default: true
    }
  },
  setup(props: any, { emit }: any) {
    const retry = () => {
      emit('retry')
    }
    return { retry }
  }
}

/**
 * 创建懒加载组件
 * @param loader 组件加载函数
 * @param options 配置选项
 */
export function createAsyncComponent(
  loader: AsyncComponentLoader,
  options: AsyncComponentOptions = {}
) {
  const {
    delay = 200,
    timeout = 10000,
    errorComponent = DefaultErrorComponent,
    loadingComponent = DefaultLoadingComponent,
    retryable = true,
    maxRetries = 3
  } = options

  return defineAsyncComponent({
    loader,
    loadingComponent,
    errorComponent,
    delay,
    timeout,
    suspensible: false,
    onError: (error, retry, fail) => {
      if (error.message.includes('fetch') && retryable && maxRetries > 0) {
        // 网络错误，可重试
        setTimeout(() => retry(), 1000)
      } else {
        // 其他错误或超过重试次数，直接失败
        fail()
      }
    }
  })
}

/**
 * 批量创建懒加载组件
 * @param components 组件映射对象
 * @param options 配置选项
 */
export function createAsyncComponents<T extends Record<string, AsyncComponentLoader>>(
  components: T,
  options?: AsyncComponentOptions
): Record<keyof T, ReturnType<typeof createAsyncComponent>> {
  const result: any = {}
  for (const key in components) {
    result[key] = createAsyncComponent(components[key], options)
  }
  return result
}

/**
 * 预加载组件
 * 用于预加载即将使用的组件
 * @param loader 组件加载函数
 */
export function preloadComponent(loader: AsyncComponentLoader): Promise<void> {
  return loader()
}

/**
 * 预加载路由组件
 * @param routes 路由配置
 */
export function preloadRouteComponents(routes: any[]) {
  routes.forEach((route) => {
    if (route.component && typeof route.component === 'function') {
      // 预加载组件，但不实际渲染
      route.component().catch(() => {})
    }
    if (route.children) {
      preloadRouteComponents(route.children)
    }
  })
}