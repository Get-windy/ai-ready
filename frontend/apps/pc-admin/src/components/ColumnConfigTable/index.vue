<template>
  <div class="column-config-table">
    <a-table
      ref="tableRef"
      v-bind="forwardAttrs"
      :columns="visibleColumns"
      :data-source="paddedDataSource"
    >
      <template #headerCell="{ column }">
        <!-- rowNo 列：齿轮触发列配置 -->
        <template v-if="column.key === 'rowNo'">
          <span
            class="cc-table-gear"
            title="列配置"
            @click="showPanel = true"
          >
            <SettingOutlined />
          </span>
          <slot
            name="headerCell"
            :column="column"
          />
        </template>
        <!-- 其他列：透传父组件的 headerCell -->
        <template v-else>
          <slot
            name="headerCell"
            :column="column"
          />
        </template>
      </template>
      <!-- 透传其他具名插槽（空行 bodyCell 渲染空白） -->
      <template
        v-for="slotName in passthroughSlots"
        :key="slotName"
        #[slotName]="slotProps"
      >
        <template v-if="slotName === 'bodyCell' && slotProps.record?._isEmptyRow" />
        <template v-else>
          <slot
            :name="slotName"
            v-bind="slotProps"
          />
        </template>
      </template>
    </a-table>

    <!-- 列配置面板 -->
    <ColumnConfigPanel
      :open="showPanel"
      :settings-columns="settingsColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showPanel = $event"
      @change="onSettingChange"
      @reset="resetSettings"
      @drag-end="handleColumnDrag"
    />

    <!-- 分页器 -->
    <StandardPagination
      v-if="showPagination"
      v-model:current="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-size-options="pageSizeOptions"
      :hide-on-single-page="false"
      @change="onPageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, useAttrs, useSlots, watch } from 'vue'
import { SettingOutlined } from '@ant-design/icons-vue'
import { useColumnConfig } from '@/composables/useColumnConfig'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'

defineOptions({ name: 'ColumnConfigTable' })

const props = withDefaults(defineProps<{
  /** 基础列定义（传给 useColumnConfig） */
  columnDefs: any[]
  /** localStorage 存储键 */
  storageKey: string
  /** 是否启用填充列 */
  fillMode?: boolean
  /** 最小显示行数（数据不足时填充空行，0=禁用） */
  minRows?: number
  /** 是否显示分页器 */
  showPagination?: boolean
  /** 当前页码 */
  current?: number
  /** 每页大小 */
  pageSize?: number
  /** 总条数 */
  total?: number
  /** 每页显示条数选项 */
  pageSizeOptions?: number[]
}>(), {
  fillMode: false,
  minRows: 20,
  showPagination: false,
  current: 1,
  pageSize: 20,
  total: 0,
  pageSizeOptions: () => [10, 20, 50, 100]
})

const emit = defineEmits<{
  'update:current': [number]
  'update:pageSize': [number]
  'page-change': [{ page: number; pageSize: number }]
}>()

const $attrs = useAttrs()
const $slots = useSlots()

// 需要透传的插槽（排除 headerCell，我们自己处理）
const passthroughSlots = computed(() => {
  return Object.keys($slots).filter(k => k !== 'headerCell')
})

// 从 attrs 中摘除 data-source，由 paddedDataSource 接管
const forwardAttrs = computed(() => {
  const attrs = { ...$attrs }
  delete (attrs as any)['data-source']
  delete (attrs as any)['dataSource']
  return attrs
})

// 最小行数填充（空行）
const paddedDataSource = computed(() => {
  const rawData = ($attrs as any)['dataSource'] ?? ($attrs as any)['data-source']
  const data = Array.isArray(rawData) ? rawData : []
  if (props.minRows <= 0 || data.length >= props.minRows) return data
  return [
    ...data,
    ...Array.from({ length: props.minRows - data.length }, (_, i) => ({
      _isEmptyRow: true,
      id: `__empty__${i}`,
    })),
  ]
})

// 列配置
const {
  showPanel,
  visibleColumns,
  settingsColumns,
  isLockedColumn,
  onSettingChange,
  resetSettings,
  handleColumnDrag,
} = useColumnConfig(props.columnDefs, props.storageKey, props.fillMode)

// 分页相关状态
const currentPage = ref(props.current)
const pageSize = ref(props.pageSize)

// 同步 props 变化
watch(() => props.current, (val) => {
  currentPage.value = val
})

watch(() => props.pageSize, (val) => {
  pageSize.value = val
})

// 分页事件处理
function onPageChange(page: number, pageSize: number) {
  // 触发父组件更新
  emit('update:current', page)
  emit('update:pageSize', pageSize)
  emit('page-change', { page, pageSize })
}

const tableRef = ref()
</script>

<style scoped>
.column-config-table {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.cc-table-gear {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ff4d4f;
  font-size: 15px;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 3px;
  transition: background 0.2s;
}
.cc-table-gear:hover {
  background: rgba(255, 77, 79, 0.08);
}

/* 分页器样式 */
.pagination-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px 0;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  margin-top: -1px;
  z-index: 1;
}

.pagination-container :deep(.ant-pagination) {
  margin-right: 16px;
}

.pagination-info {
  color: #999;
  font-size: 13px;
  margin-left: 16px;
}
</style>
