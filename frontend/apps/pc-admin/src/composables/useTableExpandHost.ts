/**
 * 表格展开联动的「宿主」组合式函数
 *
 * 背景：`BillDetailTable` 底部的「表格展开显示 / 收起显示」按钮，除了 `emit('expand-change')`，
 * 还会冒泡一个 DOM 事件 `table-expand-change`（便于跨插槽/跨组件层级传递）。标准列表页用的是
 * `CategoryListLayout` / `DocCenterLayout` / `BillTableList`，这些布局组件**已内置**监听并自动
 * 收起表格下方的分页区 / 页脚区，业务页不需要写任何代码。
 *
 * 但**自建页面骨架**（没有用上述布局组件、直接放 `BillDetailTable` + 自己写分页栏）的页面，
 * 需要自己当「宿主」：在根元素挂 `@table-expand-change`，并用 `tableExpanded` 控制下方区域的显隐，
 * 否则点「表格展开显示」时下方区域仍占位、表格长不高（按钮形同摆设）。
 *
 * 用法：
 * ```vue
 * <template>
 *   <div class="my-page" @table-expand-change="onTableExpandChange">
 *     <BillDetailTable ... />
 *     <div v-if="!tableExpanded" class="table-pagination">
 *       <StandardPagination ... />
 *     </div>
 *   </div>
 * </template>
 *
 * <script setup lang="ts">
 * const { tableExpanded, onTableExpandChange } = useTableExpandHost()
 * </script>
 * ```
 */
import { ref } from 'vue'

export function useTableExpandHost() {
  /** 表格是否处于「展开显示」态（true 时宿主应收起表格下方的分页/页脚区，把高度让给表格） */
  const tableExpanded = ref(false)

  /** 挂到宿主根元素：@table-expand-change="onTableExpandChange" */
  function onTableExpandChange(e: Event) {
    tableExpanded.value = !!(e as CustomEvent).detail
  }

  return { tableExpanded, onTableExpandChange }
}
