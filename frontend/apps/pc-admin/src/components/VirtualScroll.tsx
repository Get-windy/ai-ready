/**
 * 虚拟滚动工具
 * 用于优化大量数据的渲染性能
 */

import { defineComponent, ref, computed, onMounted, onUnmounted, watch } from 'vue'

/**
 * 虚拟滚动配置
 */
export interface VirtualScrollOptions {
  /** 列表项高度（px），可以是固定值或函数 */
  itemHeight: number | ((index: number) => number)
  /** 可视区域高度（px） */
  containerHeight: number
  /** 缓冲区大小（额外渲染的行数） */
  bufferSize?: number
  /** 是否启用动态高度 */
  dynamic?: boolean
}

/**
 * 虚拟滚动状态
 */
interface VirtualScrollState {
  /** 可视区域起始索引 */
  startIndex: number
  /** 可视区域结束索引 */
  endIndex: number
  /** 滚动偏移量 */
  scrollTop: number
  /** 总高度 */
  totalHeight: number
  /** 偏移量 */
  offsetY: number
}

/**
 * 计算虚拟滚动状态
 */
export function useVirtualScroll(
  data: any[],
  options: VirtualScrollOptions
) {
  const {
    itemHeight,
    containerHeight,
    bufferSize = 5,
    dynamic = false
  } = options

  const scrollTop = ref(0)
  const containerRef = ref<HTMLElement | null>(null)

  // 计算总高度
  const totalHeight = computed(() => {
    if (dynamic) {
      // 动态高度需要在实际渲染后计算
      return data.length * 50 // 默认高度
    }
    return data.length * (itemHeight as number)
  })

  // 计算可视区域的起始索引
  const startIndex = computed(() => {
    const height = typeof itemHeight === 'function'
      ? itemHeight(0) // 使用第一个项的高度作为参考
      : itemHeight

    const start = Math.floor(scrollTop.value / height)
    return Math.max(0, start - bufferSize)
  })

  // 计算可视区域的结束索引
  const endIndex = computed(() => {
    const height = typeof itemHeight === 'function'
      ? itemHeight(0)
      : itemHeight

    const visibleCount = Math.ceil(containerHeight / height)
    const end = startIndex.value + visibleCount + bufferSize * 2
    return Math.min(data.length, end)
  })

  // 计算偏移量
  const offsetY = computed(() => {
    const height = typeof itemHeight === 'function'
      ? itemHeight(startIndex.value)
      : itemHeight

    return startIndex.value * height
  })

  // 处理滚动事件
  const handleScroll = (e: Event) => {
    const target = e.target as HTMLElement
    scrollTop.value = target.scrollTop
  }

  // 滚动到指定索引
  const scrollToIndex = (index: number) => {
    const height = typeof itemHeight === 'function'
      ? itemHeight(index)
      : itemHeight

    if (containerRef.value) {
      containerRef.value.scrollTop = index * height
    }
  }

  // 滚动到指定位置
  const scrollToPosition = (position: number) => {
    if (containerRef.value) {
      containerRef.value.scrollTop = position
    }
  }

  // 获取可视区域的数据
  const visibleData = computed(() => {
    return data.slice(startIndex.value, endIndex.value)
  })

  return {
    containerRef,
    scrollTop,
    totalHeight,
    startIndex,
    endIndex,
    offsetY,
    visibleData,
    handleScroll,
    scrollToIndex,
    scrollToPosition
  }
}

/**
 * 虚拟滚动组件
 */
export const VirtualScroll = defineComponent({
  name: 'VirtualScroll',
  props: {
    /** 数据列表 */
    data: {
      type: Array as () => any[],
      default: () => []
    },
    /** 列表项高度（px） */
    itemHeight: {
      type: [Number, Function] as unknown as () => number | ((index: number) => number),
      required: true
    },
    /** 容器高度（px） */
    height: {
      type: Number,
      required: true
    },
    /** 缓冲区大小 */
    bufferSize: {
      type: Number,
      default: 5
    },
    /** 是否启用动态高度 */
    dynamic: {
      type: Boolean,
      default: false
    }
  },
  emits: ['scroll', 'visible-change'],
  setup(props, { emit, slots }) {
    const containerRef = ref<HTMLElement | null>(null)

    const virtualScroll = useVirtualScroll(props.data, {
      itemHeight: props.itemHeight,
      containerHeight: props.height,
      bufferSize: props.bufferSize,
      dynamic: props.dynamic
    })

    // 监听数据变化
    watch(
      () => props.data,
      () => {
        virtualScroll.scrollTop.value = 0
      }
    )

    // 监听滚动事件
    const handleScroll = (e: Event) => {
      virtualScroll.handleScroll(e)
      emit('scroll', {
        scrollTop: virtualScroll.scrollTop.value,
        startIndex: virtualScroll.startIndex.value,
        endIndex: virtualScroll.endIndex.value
      })
    }

    return () => {
      const { itemHeight } = slots.default ? {} : props
      const heightFn = typeof itemHeight === 'function' ? itemHeight : () => itemHeight as number

      return (
        <div
          ref={containerRef}
          class="virtual-scroll-container"
          style={{
            height: `${props.height}px`,
            overflowY: 'auto',
            position: 'relative'
          }}
          onScroll={handleScroll}
        >
          {/* 总高度占位 */}
          <div
            class="virtual-scroll-phantom"
            style={{ height: `${virtualScroll.totalHeight.value}px` }}
          />

          {/* 可视区域内容 */}
          <div
            class="virtual-scroll-content"
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              transform: `translateY(${virtualScroll.offsetY.value}px)`
            }}
          >
            {virtualScroll.visibleData.value.map((item, index) => {
              const realIndex = virtualScroll.startIndex.value + index
              const itemHeight = heightFn(realIndex)

              if (slots.default) {
                return slots.default({
                  item,
                  index: realIndex,
                  style: {
                    height: `${itemHeight}px`
                  }
                })
              }

              return null
            })}
          </div>
        </div>
      )
    }
  }
})

/**
 * 虚拟列表组件（简化版）
 */
export const VirtualList = defineComponent({
  name: 'VirtualList',
  props: {
    /** 数据列表 */
    data: {
      type: Array as () => any[],
      default: () => []
    },
    /** 列表项高度（px） */
    itemSize: {
      type: Number,
      default: 50
    },
    /** 容器高度（px） */
    height: {
      type: Number,
      required: true
    },
    /** 额外渲染的行数 */
    extraItems: {
      type: Number,
      default: 5
    }
  },
  setup(props, { slots }) {
    const scrollTop = ref(0)
    const containerRef = ref<HTMLElement | null>(null)

    // 计算可视区域能显示多少项
    const visibleCount = computed(() => {
      return Math.ceil(props.height / props.itemSize)
    })

    // 计算起始索引
    const startIndex = computed(() => {
      const index = Math.floor(scrollTop.value / props.itemSize)
      return Math.max(0, index - props.extraItems)
    })

    // 计算结束索引
    const endIndex = computed(() => {
      return Math.min(
        props.data.length,
        startIndex.value + visibleCount.value + props.extraItems * 2
      )
    })

    // 计算可视区域数据
    const visibleData = computed(() => {
      return props.data.slice(startIndex.value, endIndex.value)
    })

    // 计算偏移量
    const offsetY = computed(() => {
      return startIndex.value * props.itemSize
    })

    // 计算总高度
    const totalHeight = computed(() => {
      return props.data.length * props.itemSize
    })

    // 处理滚动
    const handleScroll = (e: Event) => {
      const target = e.target as HTMLElement
      scrollTop.value = target.scrollTop
    }

    return () => (
      <div
        ref={containerRef}
        style={{
          height: `${props.height}px`,
          overflowY: 'auto',
          position: 'relative'
        }}
        onScroll={handleScroll}
      >
        {/* 占位元素 */}
        <div style={{ height: `${totalHeight.value}px` }} />

        {/* 可见区域 */}
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            transform: `translateY(${offsetY.value}px)`
          }}
        >
          {visibleData.value.map((item, index) => {
            const realIndex = startIndex.value + index
            return (
              <div
                key={realIndex}
                style={{
                  height: `${props.itemSize}px`,
                  overflow: 'hidden'
                }}
              >
                {slots.default?.({ item, index: realIndex })}
              </div>
            )
          })}
        </div>
      </div>
    )
  }
})

export default VirtualScroll