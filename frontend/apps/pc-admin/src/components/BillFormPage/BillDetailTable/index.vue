<template>
  <div
    class="bill-detail-table"
    :class="{ 'table-expanded': expanded }"
  >
    <!-- Loading 遮罩 -->
    <div
      v-if="loading"
      class="table-loading-mask"
    >
      <span class="loading-spinner" />
      <span>加载中...</span>
    </div>
    <!-- 表格容器 -->
    <div
      ref="tableContainerRef"
      class="spreadsheet-table"
      :style="spreadsheetTableStyle"
    >
      <!-- 空数据提示（没有 minRows 时才显示） -->
      <div
        v-if="!loading && dataSource.length === 0 && !minRows"
        class="table-empty-text"
      >
        暂无数据
      </div>
      <table
        v-else
        class="ss-grid"
        :style="gridTableStyle"
      >
        <thead>
          <tr>
            <th
              v-for="(col, ci) in visibleColumns"
              :key="col.key"
              :style="getColStyle(col)"
              :class="getColClass(col)"
              :data-filler="col.key === '__filler__' || undefined"
            >
              <!-- ═══ rowNo 列：内嵌齿轮设置图标 ═══ -->
              <template v-if="col.type === 'rowNo'">
                <span
                  class="th-settings-btn"
                  title="配置"
                  @click.stop="showColPanel = true"
                >
                  <SettingOutlined />
                </span>
              </template>
              <!-- ═══ checkbox 列：全选复选框 ═══ -->
              <template v-else-if="col.type === 'checkbox'">
                <input
                  type="checkbox"
                  class="ss-checkbox ss-checkbox-header"
                  :checked="checkedRows.size === realDataCount && realDataCount > 0"
                  @change="(e) => checkAll((e.target as HTMLInputElement).checked)"
                >
              </template>
              <!-- ═══ 商品名称列：内嵌扫描枪开关 ═══ -->
              <template v-else-if="col.showScanToggle">
                <span class="th-title">{{ col.title }}</span>
                <a-tooltip :title="scanEnabled ? '已开启扫描枪录入' : '扫描枪录入'">
                  <span class="th-scan-label">扫描枪录入</span>
                </a-tooltip>
                <a-switch
                  v-model:checked="scanEnabled"
                  size="small"
                  class="th-scan-switch"
                />
                <span
                  v-if="col.sortable"
                  class="th-sort-icon"
                  :class="getSortIconClass(col)"
                  :title="col.tooltip || '点击排序'"
                  @click="toggleSort(col)"
                >
                  <CaretUpOutlined v-if="sortState.key === col.key && sortState.order === 'asc'" />
                  <CaretDownOutlined v-else-if="sortState.key === col.key && sortState.order === 'desc'" />
                  <span
                    v-else
                    class="sort-neutral"
                  ><CaretUpOutlined /><CaretDownOutlined /></span>
                </span>
              </template>
              <!-- ═══ 普通列标题 ═══ -->
              <template v-else>
                <span class="th-title">{{ col.title }}</span>
                <span
                  v-if="col.sortable"
                  class="th-sort-icon"
                  :class="getSortIconClass(col)"
                  :title="col.tooltip || '点击排序'"
                  @click="toggleSort(col)"
                >
                  <CaretUpOutlined v-if="sortState.key === col.key && sortState.order === 'asc'" />
                  <CaretDownOutlined v-else-if="sortState.key === col.key && sortState.order === 'desc'" />
                  <span
                    v-else
                    class="sort-neutral"
                  ><CaretUpOutlined /><CaretDownOutlined /></span>
                </span>
              </template>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(record, rowIndex) in displayRows"
            :key="record.id || rowIndex"
            class="ss-row"
            :class="{ 'ss-empty-row': record._isEmptyRow }"
          >
            <td
              v-for="col in visibleColumns"
              :key="col.key"
              :class="getCellClass(col)"
              :style="{ width: col.width ? col.width + 'px' : 'auto' }"
              :data-col-key="col.key"
            >
              <!-- 填充列：空白 -->
              <template v-if="col.key === '__filler__'">
                <span class="ss-empty-cell" />
              </template>
              <!-- 空行（与填充列互斥） -->
              <template v-else-if="record._isEmptyRow">
                <span class="ss-empty-cell" />
              </template>
              <!-- 行号 -->
              <template v-else-if="col.type === 'rowNo'">
                <span class="ss-row-no">{{ rowIndex + 1 }}</span>
              </template>
              <!-- 复选框列 -->
              <template v-else-if="col.type === 'checkbox'">
                <input
                  type="checkbox"
                  class="ss-checkbox"
                  :checked="isChecked(record, rowIndex)"
                  @change="(e) => handleCheckboxChange(record, rowIndex, (e.target as HTMLInputElement).checked)"
                >
              </template>
              <!-- 操作列 -->
              <template v-else-if="col.type === 'action'">
                <slot
                  :name="col.slotName || 'actionCell'"
                  :record="record"
                  :index="rowIndex"
                  :empty="false"
                />
              </template>
              <!-- 按钮列 -->
              <template v-else-if="col.type === 'button'">
                <div class="ss-button-cell">
                  <template
                    v-for="(btn, bi) in col.buttons"
                    :key="bi"
                  >
                    <a-button
                      :type="btn.type || 'link'"
                      :danger="btn.danger"
                      size="small"
                      @click="btn.onClick?.(record, rowIndex)"
                    >
                      {{ btn.label }}
                    </a-button>
                  </template>
                </div>
              </template>
              <!-- 自定义插槽列 -->
              <template v-else-if="col.type === 'slot'">
                <slot
                  :name="col.slotName || col.key + 'Cell'"
                  :record="record"
                  :index="rowIndex"
                  :empty="false"
                />
              </template>
              <!-- boolean 类型：查看模式 -->
              <template v-else-if="isViewMode && col.type === 'boolean'">
                <span class="ss-cell-text ss-bool-display">{{ record[col.key] ? '✓' : '-' }}</span>
              </template>
              <!-- 查看模式 -->
              <template v-else-if="isViewMode">
                <span class="ss-cell-text">{{ getCellDisplayValue(col, record) }}</span>
              </template>
              <!-- ══ 编辑模式：原生输入 ═══ -->
              <template v-else>
                <!-- select 类型 -->
                <select
                  v-if="col.type === 'select'"
                  :value="record[col.key] ?? ''"
                  class="ss-native-input ss-native-select"
                  @change="(e: Event) => updateCell(record, col.key, (e.target as HTMLSelectElement).value)"
                  @keydown.enter.prevent="handleCellKeydown($event, record, col.key, rowIndex)"
                >
                  <option
                    value=""
                    disabled
                  >
                    {{ col.placeholder || '请选择' }}
                  </option>
                  <option
                    v-for="opt in col.options"
                    :key="opt.value"
                    :value="opt.value"
                  >
                    {{ opt.label }}
                  </option>
                </select>
                <!-- date 类型：无数据时渲染空白，不显示原生日期控件占位 -->
                <template v-else-if="col.type === 'date'">
                  <input
                    v-if="record[col.key]"
                    type="date"
                    :value="record[col.key]"
                    class="ss-native-input ss-native-date"
                    @change="(e: Event) => updateCell(record, col.key, (e.target as HTMLInputElement).value)"
                    @keydown.enter.prevent="handleCellKeydown($event, record, col.key, rowIndex)"
                  >
                  <span v-else class="ss-date-empty"></span>
                </template>
                <!-- number 类型 -->
                <input
                  v-else-if="col.type === 'number'"
                  type="number"
                  :value="record[col.key] ?? 0"
                  :step="getNumberStep(col)"
                  class="ss-native-input ss-native-number"
                  @input="(e: Event) => updateCell(record, col.key, parseNumber((e.target as HTMLInputElement).value, col))"
                  @keydown.enter.prevent="handleCellKeydown($event, record, col.key, rowIndex)"
                >
                <!-- searchable input 类型（输入搜索 + 下拉） -->
                <template v-else-if="col.searchable">
                  <SearchSelect
                    :model-value="record[col.key]"
                    :options="col.options"
                    :placeholder="col.placeholder || '搜索'"
                    @update:model-value="(val: any) => updateCell(record, col.key, val)"
                    @open-select-modal="handleOpenSelectModal(record, rowIndex, col.key)"
                  />
                </template>
                <!-- boolean 类型：复选框 -->
                <label v-else-if="col.type === 'boolean'" class="ss-bool-cell">
                  <input
                    type="checkbox"
                    :checked="!!record[col.key]"
                    class="ss-bool-checkbox"
                    @change="(e: Event) => updateCell(record, col.key, (e.target as HTMLInputElement).checked)"
                  >
                </label>
                <!-- input 类型（默认） -->
                <input
                  v-else
                  type="text"
                  :value="record[col.key] ?? ''"
                  :placeholder="col.placeholder || ''"
                  class="ss-native-input ss-native-text"
                  @input="(e: Event) => updateCell(record, col.key, (e.target as HTMLInputElement).value)"
                  @keydown.enter.prevent="handleCellKeydown($event, record, col.key, rowIndex)"
                >
              </template>
            </td>
          </tr>
        </tbody>
        <!-- ═══ 合计行整合进 tfoot ═══ -->
        <tfoot v-if="summaryValues.length">
          <tr class="ss-summary-tr">
            <td
              v-for="col in visibleColumns"
              :key="'sum-' + col.key"
              :class="getSummaryCellClass(col)"
              :style="{ width: col.width ? col.width + 'px' : 'auto' }"
            >
              <template v-if="col.key === '__filler__'">
                <span />
              </template>
              <template v-else-if="col.type === 'rowNo'">
                <span class="summary-total-label">合计</span>
              </template>
              <template v-else>
                {{ getSummaryValue(col.key) }}
              </template>
            </td>
          </tr>
        </tfoot>
      </table>

      <!-- ═══ 底部展开/收起（在滚动容器内，sticky 到底部） ═══ -->
      <div class="detail-expand">
        <a-button
          type="link"
          size="small"
          @click="toggleExpand"
        >
          <FullscreenOutlined />
          {{ expanded ? '表格收起显示' : '表格展开显示' }}
        </a-button>
      </div>
    </div>

    <!-- ═══ 列设置面板（Modal 形式，含个人/全局标签页） ═══ -->
    <a-modal
      v-model:open="showColPanel"
      title="配置"
      :footer="null"
      :mask-closable="true"
      :closable="true"
      width="680px"
      centered
    >
      <a-tabs v-model:active-key="colConfigTab" class="col-config-tabs">
        <!-- ── 个人配置 Tab ── -->
        <a-tab-pane key="personal" tab="个人配置">
          <div class="col-tab-tip">只对当前操作员有效，在全局配置内配置字段显示、排序</div>
          <div class="col-settings-panel-modal">
            <div class="col-panel-body">
              <div
                v-for="(setting, si) in columnSettings"
                :key="setting.key"
                class="col-setting-row"
                :class="{ 'col-setting-ghost': !setting.visible }"
              >
                <span class="col-seq">{{ si + 1 }}</span>
                <a-checkbox
                  v-model:checked="setting.visible"
                  :disabled="isLockedColumn(setting.key)"
                  @change="onColSettingChange"
                />
                <span
                  class="col-setting-title"
                  draggable="true"
                  @dragstart="onDragStart(si)"
                  @dragover.prevent="onDragOver(si)"
                  @drop="onDrop(si)"
                >
                  <span class="drag-handle">⠿</span>
                  {{ setting.title }}
                </span>
                <span class="col-display-name">{{ setting.displayName || setting.title }}</span>
                <a-input-number
                  v-model:value="setting.width"
                  class="col-width-input"
                  :min="40"
                  :max="500"
                  size="small"
                  @change="onColSettingChange"
                />
              </div>
            </div>
            <div class="col-panel-footer">
              <a-button size="middle" @click="resetColumnSettings">恢复默认</a-button>
            </div>
          </div>
        </a-tab-pane>

        <!-- ── 全局配置 Tab ── -->
        <a-tab-pane key="global" tab="全局配置">
          <div class="col-tab-tip">系统级配置，无单据配置权限的操作员只能在已配置范围内进行操作</div>
          <div class="col-settings-panel-modal">
            <div class="col-panel-body">
              <div
                v-for="(setting, si) in globalSettings"
                :key="setting.key"
                class="col-setting-row"
                :class="{ 'col-setting-ghost': !setting.visible }"
              >
                <span class="col-seq">{{ si + 1 }}</span>
                <a-checkbox
                  v-model:checked="setting.visible"
                  :disabled="isLockedColumn(setting.key)"
                  @change="onGlobalSettingChange"
                />
                <span
                  class="col-setting-title"
                  draggable="true"
                  @dragstart="onDragStart(si)"
                  @dragover.prevent="onDragOver(si)"
                  @drop="onDrop(si)"
                >
                  <span class="drag-handle">⠿</span>
                  {{ setting.title }}
                </span>
                <a-input
                  v-model:value="setting.displayName"
                  class="col-display-name-input"
                  size="small"
                  :placeholder="setting.title"
                  @change="onGlobalSettingChange"
                />
                <a-input-number
                  v-model:value="setting.width"
                  class="col-width-input"
                  :min="40"
                  :max="500"
                  size="small"
                  @change="onGlobalSettingChange"
                />
              </div>
            </div>
            <div class="col-panel-footer">
              <a-button size="middle" @click="resetColumnSettings">恢复默认</a-button>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>


    <!-- 分页器 -->
    <StandardPagination
      v-if="showPagination"
      v-model:current="currentPage"
      v-model:page-size="currentPageSize"
      :total="total"
      :page-size-options="pageSizeOptions"
      :hide-on-single-page="false"
      @change="onPageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive, nextTick } from 'vue'
import { SettingOutlined, FullscreenOutlined, CaretUpOutlined, CaretDownOutlined } from '@ant-design/icons-vue'
import { Modal, Button, Checkbox, Select, InputNumber, Input, Tabs } from 'ant-design-vue'
import type { DetailColumnConfig, ColumnSetting } from './types'
import SearchSelect from '@/components/SearchSelect/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'

defineOptions({ name: 'BillDetailTable' })

const props = withDefaults(defineProps<{
  /** 列配置 */
  columns: DetailColumnConfig[]
  /** 数据源 */
  dataSource: any[]
  /** 是否查看模式 */
  viewMode?: boolean
  /** 表格最大高度（0=不限制，由 flex 父容器驱动高度；>0 时用 inline style 限制） */
  maxHeight?: number
  /** 合计列定义 */
  summaryColumns?: { key: string; value: number; highlight?: boolean }[]
  /** 是否加载中 */
  loading?: boolean
  /** 最小显示行数（不足时用空行填充；默认 20，业务页确需改变才传入并在传参处注释原因） */
  minRows?: number
  /** 列配置存储键名（不同表格使用不同键，避免冲突） */
  storageKey?: string
  /** 填充模式：右侧生成空白列占满剩余宽度 */
  fillMode?: boolean
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
  /** 回车跳转列（按回车跳转到下一行同列） */
  enterJumpColumns?: string[]
  /** 公式配置（{ 列key: 表达式 }，支持 {fieldName} 占位符） */
  formulas?: Record<string, string>
}>(), {
  viewMode: false,
  maxHeight: 0,
  summaryColumns: () => [],
  loading: false,
  // 默认 20 行（金标准）：与「空数据时显示空提示（dataSource 为空且未传 minRows）」配合；业务页确需改变才在页面传 :min-rows 并注释原因
  minRows: 20,
  storageKey: 'product-unit-columns-config',
  fillMode: false,
  showPagination: false,
  current: 1,
  pageSize: 20,
  total: 0,
  pageSizeOptions: () => [10, 20, 50, 100],
  enterJumpColumns: () => [],
  formulas: () => ({}),
})

const emit = defineEmits<{
  'cellChange': [record: any, fieldKey: string, value: any]
  /** 展开/收起状态变化 */
  'expand-change': [expanded: boolean]
  /** 复选框变化 */
  'checkbox-change': [record: any, rowIndex: number, checked: boolean]
  /** 全选/取消全选 */
  'checkbox-all': [checked: boolean, records: any[]]
  /** 排序变化 */
  'sort-change': [key: string | null, order: 'asc' | 'desc' | null]
  /** 打开选择弹窗（空关键字回车时触发） */
  'openSelectModal': [record: any, rowIndex: number, fieldKey: string]
  /** 分页变化 */
  'update:current': [number]
  'update:pageSize': [number]
  'page-change': [{ page: number; pageSize: number }]
}>()

// ═══ 状态 ═══
const expanded = ref(false)
const showColPanel = ref(false)
const scanEnabled = ref(false)
const tableContainerRef = ref<HTMLElement>()
const colPanelRef = ref<HTMLElement>()

// 排序状态
const sortState = reactive<{
  key: string | null
  order: 'asc' | 'desc' | null
}>({
  key: null,
  order: null,
})

// 复选框状态
const checkedRows = ref<Set<number>>(new Set())
const checkedRecords = ref<any[]>([])

// 分页相关状态
const currentPage = ref(props.current)
const currentPageSize = ref(props.pageSize)

// 同步 props 变化
watch(() => props.current, (val) => {
  currentPage.value = val
})

watch(() => props.pageSize, (val) => {
  currentPageSize.value = val
})

// 分页事件处理
function onPageChange(page: number, pageSize: number) {
  // 触发父组件更新
  emit('update:current', page)
  emit('update:pageSize', pageSize)
  emit('page-change', { page, pageSize })
}

const isViewMode = computed(() => props.viewMode)

/** 表格容器样式
 * ⚠️ maxHeight 默认不应用（maxHeight prop 默认值 = 0），
 *    高度完全由 flex 链驱动。如果外部传入 maxHeight > 0，
 *    会用 inline style 限制高度，此时 flex 和 maxHeight 取小值，
 *    可能导致 .spreadsheet-table 下方出现空白区域。
 *    推荐：在 flex 布局中不要传 maxHeight，让组件自适应。
 */
const spreadsheetTableStyle = computed(() => {
  // 不再使用 maxHeight 约束，让 flex 布局自然处理高度
  // maxHeight 会创建独立滚动容器导致 sticky 表头失效
  if (!expanded.value && props.maxHeight > 0) return { maxHeight: props.maxHeight + 'px' }
  return {}
})

// ═══ 数据行（带空行填充和排序） ═══
const displayRows = computed(() => {
  const data = [...props.dataSource]

  // 应用排序
  if (sortState.key && sortState.order) {
    const col = props.columns.find(c => c.key === sortState.key)
    if (col?.sorter) {
      data.sort(col.sorter)
    } else {
      // 默认排序逻辑
      data.sort((a, b) => {
        const valA = a[sortState.key!]
        const valB = b[sortState.key!]
        if (valA == null && valB == null) return 0
        if (valA == null) return 1
        if (valB == null) return -1
        if (typeof valA === 'number' && typeof valB === 'number') {
          return valA - valB
        }
        return String(valA).localeCompare(String(valB))
      })
    }
    if (sortState.order === 'desc') {
      data.reverse()
    }
  }

  // 空行填充
  const minRows = props.minRows || 0
  if (data.length < minRows) {
    for (let i = data.length; i < minRows; i++) {
      data.push({ _isEmptyRow: true, id: `empty-${i}` })
    }
  }
  return data
})

// 真实数据行数（排除空行）
const realDataCount = computed(() => props.dataSource.length)

// ═══ 排序功能 ═══
function toggleSort(col: DetailColumnConfig) {
  if (!col.sortable) return

  if (sortState.key === col.key) {
    // 循环切换排序状态：null -> asc -> desc -> null
    if (sortState.order === 'asc') {
      sortState.order = 'desc'
    } else if (sortState.order === 'desc') {
      sortState.key = null
      sortState.order = null
    } else {
      sortState.order = 'asc'
    }
  } else {
    sortState.key = col.key
    sortState.order = 'asc'
  }

  emit('sort-change', sortState.key, sortState.order)
}

function getSortIconClass(col: DetailColumnConfig) {
  if (sortState.key !== col.key) return 'sort-none'
  return sortState.order === 'asc' ? 'sort-asc' : 'sort-desc'
}

// ═══ 复选框方法 ═══
function isChecked(record: any, rowIndex: number): boolean {
  return checkedRows.value.has(rowIndex)
}

function handleCheckboxChange(record: any, rowIndex: number, checked: boolean) {
  if (checked) {
    checkedRows.value.add(rowIndex)
    checkedRecords.value.push(record)
  } else {
    checkedRows.value.delete(rowIndex)
    const idx = checkedRecords.value.indexOf(record)
    if (idx > -1) {
      checkedRecords.value.splice(idx, 1)
    }
  }
  emit('checkbox-change', record, rowIndex, checked)
}

function checkAll(checked: boolean) {
  checkedRows.value.clear()
  checkedRecords.value = []
  if (checked) {
    // 只选择真实数据行，排除空行
    props.dataSource.forEach((record, index) => {
      checkedRows.value.add(index)
      checkedRecords.value.push(record)
    })
  }
  emit('checkbox-all', checked, [...checkedRecords.value])
}

function getCheckedRecords(): any[] {
  return [...checkedRecords.value]
}

// 暴露方法给父组件
defineExpose({
  getCheckedRecords,
  openColumnConfig: () => { showColPanel.value = true },
})

// ═══ 列设置（运行时） ═══
const defaultSettings = computed<ColumnSetting[]>(() =>
  props.columns.map(col => ({
    key: col.key,
    title: col.title,
    displayName: col.title,
    visible: !col.defaultHidden,
    width: col.width || 100,
    fixed: col.fixed || '',
  }))
)

const columnSettings = reactive<ColumnSetting[]>([...defaultSettings.value])

// 列配置面板活动标签：个人配置 / 全局配置
const colConfigTab = ref<'personal' | 'global'>('personal')

// 全局配置（独立存储，显示名可编辑）
const globalSettings = reactive<ColumnSetting[]>([...defaultSettings.value])

// 从本地存储加载列配置
const STORAGE_KEY = computed(() => props.storageKey || 'product-unit-columns-config')
const GLOBAL_STORAGE_KEY = computed(() => (props.storageKey || 'product-unit-columns-config') + '-global')

function loadStoredSettings(storageKey: string, target: ColumnSetting[]) {
  try {
    const stored = localStorage.getItem(storageKey)
    if (stored) {
      const parsed = JSON.parse(stored)
      const mergedConfig = props.columns.map(col => {
        const storedCol = parsed.find((sc: any) => sc.key === col.key)
        return storedCol
          ? { key: col.key, title: col.title, displayName: storedCol.displayName || col.title, visible: storedCol.visible ?? !col.defaultHidden, width: storedCol.width || col.width || 100, fixed: storedCol.fixed || col.fixed || '' }
          : { key: col.key, title: col.title, displayName: col.title, visible: !col.defaultHidden, width: col.width || 100, fixed: col.fixed || '' }
      })
      target.splice(0, target.length, ...mergedConfig)
    }
  } catch (error) {
    console.warn('加载列配置失败:', error)
  }
}

function saveStoredSettings(storageKey: string, settings: ColumnSetting[]) {
  try {
    localStorage.setItem(storageKey, JSON.stringify(settings))
  } catch (error) {
    console.warn('保存列配置失败:', error)
  }
}

// 加载个人配置和全局配置
loadStoredSettings(STORAGE_KEY.value, columnSettings)
loadStoredSettings(GLOBAL_STORAGE_KEY.value, globalSettings)

// 同步 columns 变化
watch(() => props.columns, (newCols) => {
  // 同步个人配置
  const existingMap = new Map(columnSettings.map(s => [s.key, s]))
  const newSettings = newCols.map(col => {
    const existing = existingMap.get(col.key)
    return {
      key: col.key,
      title: col.title,
      displayName: existing?.displayName || col.title,
      visible: existing ? existing.visible : !col.defaultHidden,
      width: existing ? existing.width : (col.width || 100),
      fixed: existing ? existing.fixed : (col.fixed || ''),
    }
  })
  columnSettings.splice(0, columnSettings.length, ...newSettings)

  // 同步全局配置
  const globalMap = new Map(globalSettings.map(s => [s.key, s]))
  const newGlobalSettings = newCols.map(col => {
    const existing = globalMap.get(col.key)
    return {
      key: col.key,
      title: col.title,
      displayName: existing?.displayName || col.title,
      visible: existing ? existing.visible : !col.defaultHidden,
      width: existing ? existing.width : (col.width || 100),
      fixed: existing ? existing.fixed : (col.fixed || ''),
    }
  })
  globalSettings.splice(0, globalSettings.length, ...newGlobalSettings)
}, { deep: true })

/** 可见列（过滤隐藏 + 按设置顺序 + 应用冻结 + 可选填充列） */
const visibleColumns = computed<DetailColumnConfig[]>(() => {
  const colMap = new Map(props.columns.map(c => [c.key, c]))
  const cols = columnSettings
    .filter(s => s.visible && s.key !== '__filler__')
    .map(s => {
      const col = colMap.get(s.key)!
      return {
        ...col,
        // 使用个人配置的 displayName（优先）或全局配置的 displayName
        title: s.displayName || col.title,
        width: s.width,
        fixed: s.fixed as 'left' | 'right' | undefined,
      }
    })
  // 填充模式：在尾部追加空白列占满剩余空间
  if (props.fillMode) {
    cols.push({ key: '__filler__', title: '', type: '__filler__', width: undefined } as any)
  }
  return cols
})

// 计算所有可见列的总宽度（不含填充列）
const totalContentWidth = computed(() => {
  return visibleColumns.value.reduce((sum, col) => {
    if (col.key === '__filler__') return sum
    return sum + (col.width || 80)
  }, 0)
})

// 表格动态样式：宽度 100%，最小宽度为列宽总和
// — 列宽不足容器宽时，表格 100% 填充（配合填充列占满剩余空间）
// — 列宽超出容器时，表格按列宽撑开，外层 overflow:auto 产生横向滚动
const gridTableStyle = computed(() => {
  if (totalContentWidth.value === 0) return { width: '100%' }
  return { width: '100%', minWidth: totalContentWidth.value + 'px' }
})

// ═══ 合计值 ═══
const summaryValues = computed(() => props.summaryColumns)

function getSummaryValue(key: string): string {
  const found = props.summaryColumns.find(c => c.key === key)
  return found ? String(found.value) : ''
}

// ═══ 样式辅助 ═══
function getColStyle(col: DetailColumnConfig) {
  if (col.key === '__filler__') {
    return { minWidth: '0' }
  }
  return {
    width: col.width ? col.width + 'px' : 'auto',
    minWidth: col.width ? col.width + 'px' : '80px',
  }
}

function getColClass(col: DetailColumnConfig) {
  if (col.key === '__filler__') {
    return { 'ss-filler-col': true }
  }
  return {
    'ss-fixed-left': col.fixed === 'left',
    'ss-fixed-right': col.fixed === 'right',
    'ss-header-settings': col.type === 'rowNo',
  }
}

function getCellClass(col: DetailColumnConfig) {
  return {
    'ss-fixed-left': col.fixed === 'left',
    'ss-fixed-right': col.fixed === 'right',
    'ss-cell-number': col.type === 'number',
    'ss-cell-action': col.type === 'action' || col.type === 'button',
    'ss-cell-checkbox': col.type === 'checkbox',
    'ss-cell-center': col.align === 'center',
    'ss-cell-right': col.align === 'right',
  }
}

function getSummaryCellClass(col: DetailColumnConfig) {
  const found = props.summaryColumns.find(c => c.key === col.key)
  return {
    'ss-fixed-left': col.fixed === 'left',
    'ss-fixed-right': col.fixed === 'right',
    'ss-cell-number': col.type === 'number',
    'summary-highlight': found?.highlight,
  }
}

function getNumberStep(col: DetailColumnConfig): string {
  if (col.precision === 0) return '1'
  if (col.precision === 1) return '0.1'
  return '0.01'
}

function parseNumber(val: string, col: DetailColumnConfig): number {
  const num = parseFloat(val)
  if (isNaN(num)) return 0
  const precision = col.precision ?? 2
  return Number(num.toFixed(precision))
}

// ══ 单元格更新 ═══
function updateCell(record: any, fieldKey: string, value: any) {
  record[fieldKey] = value
  // 公式计算：检查是否有公式引用了此字段
  for (const [targetKey, formula] of Object.entries(props.formulas)) {
    if (!formula) continue
    const fieldRef = `{${fieldKey}}`
    if (formula.includes(fieldRef)) {
      const result = evaluateFormula(formula, record)
      if (result !== null && result !== undefined) {
        record[targetKey] = result
      }
    }
  }
  emit('cellChange', record, fieldKey, value)
}

/** 简单表达式求值（支持数学运算和常用函数） */
function evaluateFormula(formula: string, row: any): any {
  try {
    const expr = formula.replace(/\{(\w+)\}/g, (_, key) => {
      const val = row[key]
      return val === null || val === undefined || val === '' ? '0' : String(val)
    })
    // 安全求值：仅允许数字、运算符、括号和安全的数学函数
    const safeFunctions = ['Math.round', 'Math.floor', 'Math.ceil', 'Math.abs', 'Math.max', 'Math.min', 'Math.pow', 'Math.sqrt']
    let safeExpr = expr
    safeFunctions.forEach(fn => {
      const shortName = fn.split('.')[1]
      safeExpr = safeExpr.replace(new RegExp(`\\b${shortName}\\b`, 'g'), fn)
    })
    if (/^[\d\s+\-*/.(),%<>=!&|?:a-zA-Z]+$/.test(safeExpr)) {
      return new Function(`return (${safeExpr})`)()
    }
    return null
  } catch {
    return null
  }
}

/** 回车跳转处理 */
function handleCellKeydown(e: KeyboardEvent, record: any, colKey: string, rowIndex: number) {
  if (e.key === 'Enter' && props.enterJumpColumns.includes(colKey)) {
    e.preventDefault()
    const tableEl = tableContainerRef.value
    if (!tableEl) return
    const rows = tableEl.querySelectorAll('.ss-row')
    // 从当前行之后开始查找下一个非空行
    for (let i = rowIndex + 1; i < rows.length; i++) {
      const nextRow = rows[i]
      // 跳过空行
      if (nextRow.classList.contains('ss-empty-row')) continue
      const cell = nextRow.querySelector<HTMLElement>(`[data-col-key="${colKey}"]`)
      if (cell) {
        const input = cell.querySelector<HTMLInputElement>('input, select')
        input?.focus()
        input?.select?.()
        break
      }
    }
  }
}

// ═══ 打开选择弹窗 ═══
function handleOpenSelectModal(record: any, rowIndex: number, fieldKey: string) {
  emit('openSelectModal', record, rowIndex, fieldKey)
}

/** 获取查看模式的显示值 */
function getCellDisplayValue(col: DetailColumnConfig, record: any): string {
  const raw = record[col.key]
  if (raw === undefined || raw === null || raw === '') {
    // 即使值为空，如果有 formatter 也调用它
    if (col.formatter) {
      return col.formatter(raw, record)
    }
    return ''
  }

  // 使用 formatter
  if (col.formatter) {
    return col.formatter(raw, record)
  }

  if (col.type === 'select' && col.options) {
    const opt = col.options.find(o => String(o.value) === String(raw))
    return opt?.label || String(raw)
  }
  if (col.type === 'number') {
    return typeof raw === 'number' ? raw.toFixed(col.precision ?? 2) : String(raw)
  }
  return String(raw)
}

// ═══ 展开/收起 ═══
function toggleExpand() {
  expanded.value = !expanded.value
  emit('expand-change', expanded.value)
}

/** 锁定列：行号和操作列不允许隐藏 */
const LOCKED_COLUMNS = ['rowNo', 'action']
function isLockedColumn(key: string): boolean {
  return LOCKED_COLUMNS.includes(key)
}

// ═══ 列设置 ═══
function onColSettingChange() {
  columnSettings.splice(0, 0) // force reactivity
  saveStoredSettings(STORAGE_KEY.value, columnSettings)
}

function onGlobalSettingChange() {
  globalSettings.splice(0, 0) // force reactivity
  saveStoredSettings(GLOBAL_STORAGE_KEY.value, globalSettings)
}

function resetColumnSettings() {
  if (colConfigTab.value === 'personal') {
    columnSettings.splice(0, columnSettings.length, ...defaultSettings.value)
    saveStoredSettings(STORAGE_KEY.value, columnSettings)
  } else {
    globalSettings.splice(0, globalSettings.length, ...defaultSettings.value)
    saveStoredSettings(GLOBAL_STORAGE_KEY.value, globalSettings)
  }
}

// ═══ 拖拽排序 ═══
let dragIndex = -1

function onDragStart(index: number) {
  dragIndex = index
}

function onDragOver(index: number) {
  if (dragIndex === -1 || dragIndex === index) return
  const target = colConfigTab.value === 'personal' ? columnSettings : globalSettings
  const item = target.splice(dragIndex, 1)[0]
  target.splice(index, 0, item)
  dragIndex = index
}

function onDrop(_index: number) {
  dragIndex = -1
  // 保存拖拽后的顺序
  if (colConfigTab.value === 'personal') {
    saveStoredSettings(STORAGE_KEY.value, columnSettings)
  } else {
    saveStoredSettings(GLOBAL_STORAGE_KEY.value, globalSettings)
  }
}
</script>

<style scoped>
.bill-detail-table {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  position: relative;
}

/* ═══ Loading 遮罩 ═══ */
.table-loading-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  z-index: 10;
  font-size: 13px;
  color: #666;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #1890ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* ═══ 空数据提示 ═══ */
.table-empty-text {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #999;
  font-size: 13px;
}

/* ═ 表格容器 ═══
 * ⚠️ 父容器链必须满足以下条件，sticky 表头才能正常工作：
 *   1. 每层都需要 overflow: hidden（ containment ）
 *   2. 每层都需要 display: flex; flex-direction: column（让 flex: 1 生效）
 *   3. 不要给 .spreadsheet-table 设置固定 maxHeight 内联样式，
 *      否则会破坏 flex 高度分配，导致滚动极值时表头 1px 跳动。
 *      maxHeight 只在展开模式（70vh）或外部显式传入 > 0 时才应用。
 */
.spreadsheet-table {
  /* ⚠️ 必须用 height:0 + flex-grow:1，不能用 flex:1。
   * flex:1 的 flex-basis:0% 在某些浏览器中不能可靠地创建
   * 有界高度，导致 overflow:auto 无法触发滚动（展开时尤其明显）。
   * height:0 强制元素从零高度开始，flex-grow:1 增长填满可用空间，
   * 保证高度始终等于 flex 分配的空间 → overflow:auto 产生滚动。
   */
  height: 0;
  flex-grow: 1;
  overflow: auto;          /* 唯一的滚动容器，sticky th 相对它定位 */
  border: 1px solid #e8e8e8;
  border-bottom: none;
}

/* ⚠️ 必须用 separate，不能用 collapse！
 * border-collapse: collapse + position: sticky 有两个严重问题：
 *   1. 折叠边框在 thead/tbody 之间共享，吸顶时边框跟着 tbody 滚走，
 *      表头和表体之间出现"分隔线消失"的视觉 bug。
 *   2. 折叠边框居中于单元格边缘，导致 sticky top:0 的定位基准
 *      与初始位置有 0.5px 偏移，滚动时表头出现 1px 跳动。
 * separate + border-spacing: 0 完美避开这两个问题。
 */
.ss-grid {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
  font-size: 13px;
}

/* ─── 表头 ── */
.ss-grid thead {
  background: #fafafa;
}
/* ⚠️ 边框策略：只画 right + bottom，不画 top + left。
 * 原因：separate 模式下相邻单元格边框不折叠，如果四边都画会出现 2px 双线。
 * 只画 right + bottom 可保证任意两格之间恰好 1px 线条：
 *   - 左边的格子画 right，右边的格子不画 left → 合计 1px
 *   - 上面的格子画 bottom，下面的格子不画 top → 合计 1px
 * 外框由 .spreadsheet-table 容器的 border 提供（top/left/right），
 * 底部由最后一行的 border-bottom 提供。
 */
.ss-grid th {
  background: #fafafa;
  border-right: 1px solid #e8e8e8;
  border-bottom: 2px solid #b0b0b0;
  padding: 0 6px;
  text-align: center;
  font-weight: 600;
  color: #262626;
  font-size: 13px;
  height: 32px;
  box-sizing: border-box;
  position: sticky;
  top: 0;
  z-index: 10;
  white-space: nowrap;
  vertical-align: middle;
  user-select: none;
}

.ss-grid td {
  border-right: 1px solid #e8e8e8;
  border-bottom: 1px solid #e8e8e8;
  padding: 0;
  height: 28px;
  vertical-align: middle;
}

/* 最后一列不画右边线，容器提供右边框 */
.ss-grid th:last-child:not(.ss-filler-col),
.ss-grid td:last-child:not(.ss-filler-col) {
  border-right: none;
}

/* 填充列：th 保留底边框和背景色，仅移除右边框 */
.ss-grid th.ss-filler-col {
  border-right: none !important;
  background: #fafafa;
}
/* 填充列：td 完全无边框 */
.ss-grid td.ss-filler-col {
  border: none !important;
  padding: 0 !important;
  cursor: default;
  min-width: 0;
}

/* 左对齐列：左侧 5px 间距 */
.ss-grid td:not(.ss-cell-center):not(.ss-cell-right):not(.ss-cell-number):not(.ss-cell-action):not(.ss-cell-checkbox) {
  padding-left: 5px;
}

/* 右对齐列（数字列 + 显式右对齐）：右侧 5px 间距 */
.ss-grid td.ss-cell-number,
.ss-grid td.ss-cell-right {
  padding-right: 5px;
}

.ss-row:hover td {
  background: #f5f7fa;
}

/* 空行样式 */
.ss-empty-row td {
  background: #fafafa;
}

.ss-empty-cell {
  display: block;
  height: 28px;
}

.ss-row-no {
  display: block;
  text-align: center;
  color: #8c8c8c;
  font-size: 11px;
  line-height: 28px;
}

/* ─── rowNo 列：齿轮设置按钮 ─── */
.ss-header-settings {
  cursor: pointer;
  text-align: center;
  padding: 0 !important;
}

.th-settings-btn {
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

.th-settings-btn:hover {
  background: rgba(255, 77, 79, 0.08);
}

/* ─── 列标题文字 ─── */
.th-title {
  display: inline;
  margin-right: 4px;
}

/* ─── 扫描枪标签 + 开关 ─── */
.th-scan-label {
  font-size: 11px;
  color: #8c8c8c;
  margin-right: 4px;
  font-weight: 400;
}

.th-scan-switch {
  margin-right: 2px;
}

/* ─── 排序图标 ─── */
.th-sort-icon {
  font-size: 10px;
  color: #bfbfbf;
  cursor: pointer;
  margin-left: 2px;
  position: relative;
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
}

.th-sort-icon:hover {
  color: #1890ff;
}



.sort-asc {
  color: #1890ff;
}

.sort-desc {
  color: #1890ff;
}

.sort-neutral {
  display: inline-flex;
  flex-direction: column;
  line-height: 0.8;
  font-size: 8px;
  color: #bfbfbf;
}
.sort-neutral .anticon {
  font-size: 9px;
}

.sort-order-badge {
  font-size: 10px;
  margin-left: 1px;
  font-weight: bold;
}

/* ═══ 原生输入框样式（无嵌套感） ═══ */
.ss-native-input {
  width: 100%;
  height: 100%;
  border: none !important;
  outline: none !important;
  background: transparent !important;
  font-size: 13px !important;
  color: #262626;
  padding: 0 6px !important;
  box-sizing: border-box;
  font-family: inherit;
  line-height: 28px;
}

.ss-native-input:focus {
  background: #e6f7ff !important;
}

.ss-native-input::placeholder {
  color: #bfbfbf;
}

.ss-native-select {
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
}

.ss-native-number {
  text-align: right;
}

.ss-native-date {
  cursor: pointer;
}

/* 无数据日期单元格：纯空白占位（无内容、无占位符、无日期图标） */
.ss-date-empty {
  display: inline-block;
  min-width: 100px;
  min-height: 22px;
}

/* ─── 数字单元格右对齐 ─── */
.ss-cell-number .ss-native-input {
  text-align: right;
}

/* ─── 固定列 ─── */
.ss-fixed-left {
  position: sticky;
  left: 0;
  z-index: 1;
  background: inherit;
}

.ss-fixed-right {
  position: sticky;
  right: 0;
  z-index: 1;
  background: inherit;
}

.ss-grid th.ss-fixed-left,
.ss-grid th.ss-fixed-right {
  z-index: 3;
}

.ss-row:hover .ss-fixed-left,
.ss-row:hover .ss-fixed-right {
  background: #f5f7fa !important;
}

/* 查看模式文本 */
.ss-cell-text {
  display: block;
  padding: 0 6px;
  font-size: 13px;
  color: #262626;
  line-height: 28px;
}

/* boolean 类型：复选框单元格 */
.ss-bool-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  cursor: pointer;
}
.ss-bool-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #1890ff;
}
.ss-bool-display {
  text-align: center;
  font-weight: 600;
  color: #52c41a;
}

/* ═══ 复选框样式 ═══ */
.ss-checkbox {
  width: 14px;
  height: 14px;
  cursor: pointer;
  margin: 0 auto;
  display: block;
}

.ss-checkbox-header {
  margin: 0 auto;
}

/* ═══ 按钮列样式 ═══ */
.ss-button-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 100%;
}

/* ─── 操作列内容居中 ── */
.ss-grid td.ss-cell-action {
  text-align: center;
}

/* ─── 统一 Ant Design 按钮字体为 13px ─── */
:deep(.ant-btn) {
  font-size: 13px !important;
}
:deep(.ant-btn.ant-btn-sm) {
  height: 24px;
  padding: 0 6px;
}
:deep(.ant-btn.ant-btn-link) {
  height: auto;
  line-height: 1.4;
  padding: 0 4px;
}
.ss-grid td.ss-cell-action :deep(.ant-space) {
  display: inline-flex;
  justify-content: center;
  width: 100%;
}

/* ═══ 合计行（tfoot） ═══ */
.ss-summary-tr td {
  background: #fafafa;
  border-right: 1px solid #e8e8e8;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 6px;
  height: 30px;
  font-weight: 600;
  font-size: 13px;
  vertical-align: middle;
}

.summary-total-label {
  font-weight: 700;
  color: #262626;
  display: block;
  line-height: 30px;
}

.summary-highlight {
  color: #ff4d4f;
  font-weight: 700;
  text-align: right;
}

/* ═══ 展开/收起 ═══ */
.detail-expand {
  text-align: center;
  padding: 4px;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-top: none;
  flex-shrink: 0;
  position: sticky;
  bottom: 0;
  z-index: 10;
}

.detail-expand :deep(.ant-btn-link) {
  color: #1890ff;
  font-size: 12px;
}

/* ═══ 展开模式：隐藏边框，可滚动 ═══ */
.table-expanded {
  /* 不添加 overflow-y: auto，保持 .spreadsheet-table 为唯一滚动容器
     这样 sticky thead 才能正常工作 */
  min-height: auto;
}

.table-expanded .spreadsheet-table {
  border: none;
}

.table-expanded .detail-expand {
  border: none;
  background: transparent;
}

/* ═══ 列设置面板 ═══ */
.col-settings-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 60px 20px 0;
}

.col-settings-panel {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  width: 360px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  z-index: 1001;
}

.col-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.col-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 6px 0;
}

.col-setting-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  font-size: 12px;
  cursor: default;
  transition: background 0.15s;
}

.col-setting-row:hover {
  background: #f5f7fa;
}

.col-setting-ghost {
  opacity: 0.45;
}

.col-setting-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: grab;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #262626;
}

.drag-handle {
  color: #bfbfbf;
  cursor: grab;
  font-size: 13px;
  user-select: none;
}

.col-freeze-select {
  width: 80px;
  height: 24px;
  font-size: 11px;
  border: 1px solid #d9d9d9;
  border-radius: 3px;
  padding: 0 4px;
  color: #595959;
  background: #fff;
  outline: none;
  flex-shrink: 0;
}

.col-freeze-select:focus {
  border-color: #1890ff;
}

.col-width-input {
  width: 55px;
  height: 24px;
  font-size: 11px;
  border: 1px solid #d9d9d9;
  border-radius: 3px;
  padding: 0 6px;
  text-align: right;
  outline: none;
  flex-shrink: 0;
}

.col-width-input:focus {
  border-color: #1890ff;
}

.col-panel-footer {
  padding: 8px 14px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
}

/* 为Modal样式的列设置面板新增样式 */
.col-settings-panel-modal {
  max-height: 55vh;
  overflow-y: auto;
}

/* 列配置标签页样式 */
.col-config-tabs :deep(.ant-tabs-nav) {
  padding: 0 14px;
  margin-bottom: 0;
}
.col-config-tabs :deep(.ant-tabs-content-holder) {
  border: 1px solid #d9d9d9;
  border-top: none;
  border-radius: 0 0 4px 4px;
}
.col-tab-tip {
  font-size: 12px;
  color: #fa8c16;
  padding: 8px 14px 4px;
}
.col-seq {
  width: 24px;
  text-align: center;
  color: #8c8c8c;
  font-size: 11px;
  flex-shrink: 0;
}
.col-display-name {
  flex: 0 0 120px;
  padding: 2px 6px;
  font-size: 12px;
  color: #595959;
  background: #fafafa;
  border-radius: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.col-display-name-input {
  flex: 0 0 120px;
  height: 24px;
  font-size: 12px;
}

/* 分页器样式（继承自 ColumnConfigTable） */
.standard-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px 0;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  margin-top: -1px;
  z-index: 1;
  flex-shrink: 0; /* 防止在 flex 布局中被压缩 */
}

.standard-pagination :deep(.ant-pagination) {
  margin-right: 16px;
}

.standard-pagination .pagination-info {
  color: #999;
  font-size: 13px;
  margin-left: 16px;
}
</style>