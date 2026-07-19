import { ref, watch, onMounted, onBeforeUnmount, nextTick, type Ref } from 'vue'

/**
 * 自动计算 grid-column span 值
 * 根据元素内容实际宽度动态决定占几列（向上取整）
 *
 * @param actionRef action-group 元素的 ref
 * @param gridRef   所属 search-grid 容器的 ref
 */
export function useAutoGridSpan(
  actionRef: Ref<HTMLElement | null>,
  gridRef: Ref<HTMLElement | null>
) {
  const span = ref(1)
  let resizeObserver: ResizeObserver | null = null

  function calc() {
    const el = actionRef.value
    const grid = gridRef.value
    if (!el || !grid) return

    // scrollWidth 反映内容实际宽度（不受 grid 列宽约束）
    const contentWidth = el.scrollWidth
    if (contentWidth <= 0) return

    // 计算单列宽度
    const cs = getComputedStyle(grid)
    const gap = parseFloat(cs.columnGap || cs.gap || '12') || 12
    const gridWidth = grid.clientWidth
    const colCount = Math.max(1, Math.floor((gridWidth + gap) / (160 + gap)))
    const colWidth = (gridWidth - gap * (colCount - 1)) / colCount

    if (colWidth > 0) {
      span.value = Math.max(1, Math.ceil(contentWidth / colWidth))
    }
  }

  onMounted(async () => {
    await nextTick()
    calc()
    if (gridRef.value) {
      resizeObserver = new ResizeObserver(calc)
      resizeObserver.observe(gridRef.value)
    }
  })

  // 当 action 元素出现/消失时重新计算（v-if 切换场景）
  watch(actionRef, async () => {
    await nextTick()
    calc()
  })

  onBeforeUnmount(() => {
    resizeObserver?.disconnect()
  })

  return { span }
}
