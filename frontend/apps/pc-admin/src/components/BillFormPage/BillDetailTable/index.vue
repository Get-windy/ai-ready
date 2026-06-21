<template>
  <div class="bill-detail-table" :class="{ 'table-expanded': expanded }">
    <!-- 表格容器 -->
    <div class="spreadsheet-table" ref="tableContainerRef" :style="{ maxHeight: expanded ? 'none' : maxHeight + 'px' }">
      <table class="ss-grid">
        <thead>
          <tr>
            <th
              v-for="(col, ci) in visibleColumns"
              :key="col.key"
              :style="getColStyle(col)"
              :class="getColClass(col)"
            >
              <!-- ═══ rowNo 列：内嵌齿轮设置图标 ═══ -->
              <template v-if="col.type === 'rowNo'">
                <span class="th-settings-btn" @click.stop="showColPanel = !showColPanel" title="列设置">
                  <SettingOutlined />
                </span>
              </template>
              <!-- ═══ 商品名称列：内嵌扫描枪开关 ═══ -->
              <template v-else-if="col.showScanToggle">
                <span class="th-title">{{ col.title }}</span>
                <a-tooltip :title="scanEnabled ? '已开启扫描枪录入' : '扫描枪录入'">
                  <span class="th-scan-label">扫描枪录入</span>
                </a-tooltip>
                <a-switch v-model:checked="scanEnabled" size="small" class="th-scan-switch" />
                <span class="th-sort-icon" title="排序">⇅</span>
              </template>
              <!-- ═══ 普通列标题 ═══ -->
              <template v-else>
                <span class="th-title">{{ col.title }}</span>
                <span v-if="col.type !== 'action'" class="th-sort-icon" title="排序">⇅</span>
              </template>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(record, rowIndex) in props.dataSource" :key="record.id || rowIndex" class="ss-row">
            <td
              v-for="col in visibleColumns"
              :key="col.key"
              :class="getCellClass(col)"
              :style="{ width: col.width ? col.width + 'px' : 'auto' }"
            >
              <!-- 行号 -->
              <template v-if="col.type === 'rowNo'">
                <span class="ss-row-no">{{ rowIndex + 1 }}</span>
              </template>
              <!-- 操作列 -->
              <template v-else-if="col.type === 'action'">
                <slot :name="col.slotName || 'actionCell'" :record="record" :index="rowIndex" :empty="false" />
              </template>
              <!-- 自定义插槽列 -->
              <template v-else-if="col.type === 'slot'">
                <slot :name="col.slotName || col.key + 'Cell'" :record="record" :index="rowIndex" :empty="false" />
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
                >
                  <option value="" disabled>{{ col.placeholder || '请选择' }}</option>
                  <option v-for="opt in col.options" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </option>
                </select>
                <!-- date 类型 -->
                <input
                  v-else-if="col.type === 'date'"
                  type="date"
                  :value="record[col.key] || ''"
                  class="ss-native-input ss-native-date"
                  @change="(e: Event) => updateCell(record, col.key, (e.target as HTMLInputElement).value)"
                />
                <!-- number 类型 -->
                <input
                  v-else-if="col.type === 'number'"
                  type="number"
                  :value="record[col.key] ?? 0"
                  :step="getNumberStep(col)"
                  class="ss-native-input ss-native-number"
                  @input="(e: Event) => updateCell(record, col.key, parseNumber((e.target as HTMLInputElement).value, col))"
                />
                <!-- searchable input 类型（输入搜索 + 下拉） -->
                <template v-else-if="col.searchable">
                  <SearchSelect
                    :model-value="record[col.key]"
                    :options="col.options"
                    :placeholder="col.placeholder || '搜索'"
                    @update:model-value="(val: any) => updateCell(record, col.key, val)"
                  />
                </template>
                <!-- input 类型（默认） -->
                <input
                  v-else
                  type="text"
                  :value="record[col.key] ?? ''"
                  :placeholder="col.placeholder || ''"
                  class="ss-native-input ss-native-text"
                  @input="(e: Event) => updateCell(record, col.key, (e.target as HTMLInputElement).value)"
                />
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
              <template v-if="col.type === 'rowNo'">
                <span class="summary-total-label">合计</span>
              </template>
              <template v-else>
                {{ getSummaryValue(col.key) }}
              </template>
            </td>
          </tr>
        </tfoot>
      </table>
    </div>

    <!-- ═══ 列设置面板（Popover） ═══ -->
    <Teleport to="body">
      <div v-if="showColPanel" class="col-settings-overlay" @click.self="showColPanel = false">
        <div class="col-settings-panel" ref="colPanelRef">
          <div class="col-panel-header">
            <span>列设置</span>
            <a-button type="text" size="small" @click="showColPanel = false">✕</a-button>
          </div>
          <div class="col-panel-body">
            <div
              v-for="(setting, si) in columnSettings"
              :key="setting.key"
              class="col-setting-row"
              :class="{ 'col-setting-ghost': !setting.visible }"
            >
              <!-- 可见性复选框 -->
              <a-checkbox v-model:checked="setting.visible" :disabled="isLockedColumn(setting.key)" @change="onColSettingChange" />
              <!-- 列标题（可拖拽排序） -->
              <span class="col-setting-title" draggable="true"
                @dragstart="onDragStart(si)"
                @dragover.prevent="onDragOver(si)"
                @drop="onDrop(si)"
              >
                <span class="drag-handle">⠿</span>
                {{ setting.title }}
              </span>
              <!-- 冻结选择 -->
              <select v-model="setting.fixed" class="col-freeze-select" @change="onColSettingChange">
                <option value="">不冻结</option>
                <option value="left">冻结左侧</option>
                <option value="right">冻结右侧</option>
              </select>
              <!-- 宽度调整 -->
              <input
                type="number"
                v-model.number="setting.width"
                class="col-width-input"
                min="40"
                max="500"
                @change="onColSettingChange"
              />
            </div>
          </div>
          <div class="col-panel-footer">
            <a-button size="small" @click="resetColumnSettings">恢复默认</a-button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ═══ 底部展开/收起 ══ -->
    <div class="detail-expand">
      <a-button type="link" size="small" @click="toggleExpand">
        <FullscreenOutlined />
        {{ expanded ? '表格收起显示' : '表格展开显示' }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive, nextTick } from 'vue'
import { SettingOutlined, FullscreenOutlined } from '@ant-design/icons-vue'
import type { DetailColumnConfig, ColumnSetting } from './types'
import SearchSelect from '@/components/SearchSelect/index.vue'

defineOptions({ name: 'BillDetailTable' })

const props = withDefaults(defineProps<{
  /** 列配置 */
  columns: DetailColumnConfig[]
  /** 数据源 */
  dataSource: any[]
  /** 是否查看模式 */
  viewMode?: boolean
  /** 表格最大高度 */
  maxHeight?: number
  /** 合计列定义 */
  summaryColumns?: { key: string; value: number; highlight?: boolean }[]
}>(), {
  viewMode: false,
  maxHeight: 400,
  summaryColumns: () => [],
})

const emit = defineEmits<{
  'cellChange': [record: any, fieldKey: string, value: any]
  /** 展开/收起状态变化 */
  'expand-change': [expanded: boolean]
}>()

// ═══ 状态 ═══
const expanded = ref(false)
const showColPanel = ref(false)
const scanEnabled = ref(false)
const tableContainerRef = ref<HTMLElement>()
const colPanelRef = ref<HTMLElement>()

const isViewMode = computed(() => props.viewMode)

// ═══ 列设置（运行时） ═══
const defaultSettings = computed<ColumnSetting[]>(() =>
  props.columns.map(col => ({
    key: col.key,
    title: col.title,
    visible: true, // 默认全部可见；rowNo/action 在面板中禁用手动隐藏
    width: col.width || 100,
    fixed: col.fixed || '',
  }))
)

const columnSettings = reactive<ColumnSetting[]>([...defaultSettings.value])

// 同步 columns 变化
watch(() => props.columns, (newCols) => {
  const existingMap = new Map(columnSettings.map(s => [s.key, s]))
  const newSettings = newCols.map(col => {
    const existing = existingMap.get(col.key)
    return {
      key: col.key,
      title: col.title,
      visible: existing ? existing.visible : true, // 保留已有可见性，默认可见
      width: existing ? existing.width : (col.width || 100),
      fixed: existing ? existing.fixed : (col.fixed || ''),
    }
  })
  columnSettings.splice(0, columnSettings.length, ...newSettings)
}, { deep: true })

/** 可见列（过滤隐藏 + 按设置顺序 + 应用冻结） */
const visibleColumns = computed<DetailColumnConfig[]>(() => {
  const colMap = new Map(props.columns.map(c => [c.key, c]))
  return columnSettings
    .filter(s => s.visible)
    .map(s => {
      const col = colMap.get(s.key)!
      return {
        ...col,
        width: s.width,
        fixed: s.fixed as 'left' | 'right' | undefined,
      }
    })
})

// ═══ 合计值 ═══
const summaryValues = computed(() => props.summaryColumns)

function getSummaryValue(key: string): string {
  const found = props.summaryColumns.find(c => c.key === key)
  return found ? String(found.value) : ''
}

// ═══ 样式辅助 ═══
function getColStyle(col: DetailColumnConfig) {
  return {
    width: col.width ? col.width + 'px' : 'auto',
    minWidth: col.width ? col.width + 'px' : '80px',
  }
}

function getColClass(col: DetailColumnConfig) {
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
    'ss-cell-action': col.type === 'action',
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
  emit('cellChange', record, fieldKey, value)
}

/** 获取查看模式的显示值 */
function getCellDisplayValue(col: DetailColumnConfig, record: any): string {
  const raw = record[col.key]
  if (raw === undefined || raw === null || raw === '') return ''
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
  // 触发响应式更新
  columnSettings.splice(0, 0) // force reactivity
}

function resetColumnSettings() {
  columnSettings.splice(0, columnSettings.length, ...defaultSettings.value)
}

// ═══ 拖拽排序 ═══
let dragIndex = -1

function onDragStart(index: number) {
  dragIndex = index
}

function onDragOver(index: number) {
  if (dragIndex === -1 || dragIndex === index) return
  const item = columnSettings.splice(dragIndex, 1)[0]
  columnSettings.splice(index, 0, item)
  dragIndex = index
}

function onDrop(_index: number) {
  dragIndex = -1
}
</script>

<style scoped>
.bill-detail-table {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

/* ═ 表格容器 ═══ */
.spreadsheet-table {
  flex: 1;
  overflow: auto;
  border: 1px solid #d9d9d9;
  border-bottom: none;
}

.ss-grid {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 12px;
}

/* ─── 表头 ── */
.ss-grid th {
  background: #fafafa;
  border: 1px solid #e8e8e8;
  padding: 0 6px;
  text-align: center;
  font-weight: 600;
  color: #262626;
  font-size: 12px;
  height: 32px;
  position: sticky;
  top: 0;
  z-index: 2;
  white-space: nowrap;
  vertical-align: middle;
  user-select: none;
}

.ss-grid td {
  border: 1px solid #e8e8e8;
  padding: 0;
  height: 28px;
  vertical-align: middle;
}

.ss-row:hover td {
  background: #f5f7fa;
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
}

.th-sort-icon:hover {
  color: #1890ff;
}

/* ═══ 原生输入框样式（无嵌套感） ═══ */
.ss-native-input {
  width: 100%;
  height: 100%;
  border: none !important;
  outline: none !important;
  background: transparent !important;
  font-size: 12px !important;
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
  font-size: 12px;
  color: #262626;
  line-height: 28px;
}

/* ═══ 合计行（tfoot） ═══ */
/* ─── 操作列内容居中 ─── */
.ss-grid td.ss-cell-action {
  text-align: center;
}
.ss-grid td.ss-cell-action :deep(.ant-space) {
  display: inline-flex;
  justify-content: center;
  width: 100%;
}

.ss-summary-tr td {
  background: #fafafa;
  border: 1px solid #e8e8e8;
  padding: 0 6px;
  height: 30px;
  font-weight: 600;
  font-size: 12px;
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
}

.detail-expand :deep(.ant-btn-link) {
  color: #1890ff;
  font-size: 12px;
}

/* ═══ 展开模式：隐藏边框，全屏 ═══ */
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
</style>
