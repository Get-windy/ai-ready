import { onBeforeUnmount, onMounted, watch, type Ref } from 'vue'

/** 双击时应被忽略的交互控件（否则会与按钮点击、下拉展开等既有交互抢事件） */
const INTERACTIVE_SELECTOR = [
  'button',
  'a',
  'input',
  'textarea',
  'select',
  '.ant-dropdown-trigger',
  '.ant-select',
  '.ant-picker',
  '.ss-checkbox',
].join(', ')

/**
 * 页面侧「双击行」入口（事件委托）
 *
 * ## 为什么放在页面侧，而不是让 `BillTableList` / `BillDetailTable` 派发事件
 *
 * 这两个共享表格组件全站被 **211 处 / 170 个视图文件**引用。历史上曾有 39 个页面写了
 * `@cell-dblclick="handleView"`，但组件从未派发该事件 —— 即这批绑定**一直是死绑定，
 * 双击从来没有任何反应**。
 *
 * 若在组件上补一个全局派发，等于**一次性改变 211 处调用方的行为**（虽然大多是只读的
 * 「看详情」，但爆炸半径是全站，且无法逐页评估）。按《修缺陷优先组件级解决》的反面用法
 * 也不成立：这里缺的**不是**组件被删掉的能力，而是**各页自己要显式声明**的行级交互。
 *
 * 因此裁定：**共享表格组件的事件契约保持不变，谁要双击谁在页面侧自己接**。
 * 本 composable 就是那段"自己接"的公共实现（避免把同一段委托逻辑复制 39 份）。
 *
 * ## 依赖的 DOM 约定
 *
 * `BillDetailTable` 的 `<tr>` 上带有**惰性属性** `data-row-key`（值 = `record[rowKey] ?? record.id`，
 * 与它自身的 `:key` 同口径）—— 与既有的 `<td data-col-key>` 是同一范式，纯属性、零行为影响。
 * 占位空行不带该属性，因此天然不会被命中。
 *
 * ## 用法
 *
 * ```vue
 * <template>
 *   <div ref="tableWrap" class="table-area">
 *     <BillTableList :columns="columns" v-model:data-source="tableData" />
 *   </div>
 * </template>
 *
 * <script setup lang="ts">
 * const tableWrap = ref<HTMLElement | null>(null)
 * const tableData = ref<Row[]>([])
 * // 双击行 → 打开详情
 * useRowDblclick(tableWrap, () => tableData.value, handleView)
 * </script>
 * ```
 *
 * @param containerRef 包住表格的容器（表格自身的双击事件会冒泡到这里）
 * @param rows         取当前行数组的 getter（用于把 `data-row-key` 还原成记录对象）
 * @param onRow        命中真实行后的回调
 * @param keyField     行标识字段名，需与传给表格的 `row-key` 一致（默认 `id`）
 */
export function useRowDblclick<T = any>(
  containerRef: Ref<HTMLElement | null>,
  rows: () => T[],
  onRow: (record: T) => void,
  keyField = 'id',
) {
  let bound: HTMLElement | null = null

  function buildIndex(): Map<string, T> {
    const map = new Map<string, T>()
    for (const row of rows() || []) {
      if (!row || typeof row !== 'object') continue
      const anyRow = row as Record<string, any>
      // 与表格 `:key="record[rowKey] ?? record.id"` 同口径，否则查不到
      const key = anyRow[keyField] ?? anyRow.id
      if (key === undefined || key === null) continue
      map.set(String(key), row)
    }
    return map
  }

  function handler(e: MouseEvent) {
    const target = e.target as HTMLElement | null
    if (!target) return
    if (target.closest(INTERACTIVE_SELECTOR)) return
    const tr = target.closest('tr[data-row-key]') as HTMLElement | null
    if (!tr) return
    const key = tr.getAttribute('data-row-key')
    if (!key) return
    const record = buildIndex().get(key)
    if (record === undefined) return
    e.stopPropagation()
    onRow(record)
  }

  function bind() {
    const el = containerRef.value
    if (el === bound) return
    if (bound) bound.removeEventListener('dblclick', handler)
    bound = el
    if (bound) bound.addEventListener('dblclick', handler)
  }

  onMounted(bind)
  // 容器可能被 v-if 切换（切换 Tab、空态与表格互斥等），ref 变化后要重新挂
  watch(containerRef, bind)
  onBeforeUnmount(() => {
    if (bound) bound.removeEventListener('dblclick', handler)
    bound = null
  })

  /** 供页面在极端场景下手动解绑（一般不需要） */
  return {
    detach() {
      if (bound) bound.removeEventListener('dblclick', handler)
      bound = null
    },
  }
}
