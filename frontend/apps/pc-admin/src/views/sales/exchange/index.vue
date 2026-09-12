<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- ═══ 工具栏 + 搜索区域 ═══ -->
      <div class="page-header">
        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <div class="query-scheme-wrap">
              <a-select
                v-model:value="queryScheme"
                style="width: 140px"
                size="small"
                placeholder="--查询方案--"
                @change="handleSchemeChange"
              >
                <a-select-option value="">
                  --查询方案--
                </a-select-option>
                <a-select-option
                  v-for="s in querySchemes"
                  :key="s.name"
                  :value="s.name"
                >
                  {{ s.name }}
                </a-select-option>
              </a-select>
              <a-button
                type="link"
                size="small"
                style="padding: 0 4px"
                @click="handleSaveScheme"
              >
                <PlusOutlined />
              </a-button>
            </div>
            <a-space
              :size="4"
              class="quick-dates"
            >
              <a-button
                v-for="d in quickDates"
                :key="d.key"
                :type="quickDate === d.key ? 'primary' : 'link'"
                size="small"
                @click="setQuickDate(d.key)"
              >
                {{ d.label }}
              </a-button>
            </a-space>
          </div>
          <div class="toolbar-right">
            <a-space :size="8">
              <!-- 页面配置入口（对标：顶部按钮区域最左侧的配置按钮） -->
              <a-tooltip
                v-if="btnEnabled('config')"
                title="配置"
                placement="bottom"
              >
                <a-button size="small" @click="showPageConfig = true">
                  <SettingOutlined />
                </a-button>
              </a-tooltip>
              <a-tooltip
                v-if="btnEnabled('add')"
                title="新增"
                placement="bottom"
              >
                <a-button type="primary" size="small" @click="handleAdd">
                  <PlusOutlined /> 新增
                </a-button>
              </a-tooltip>
              <a-tooltip
                v-if="btnEnabled('refresh')"
                title="刷新"
                placement="bottom"
              >
                <a-button size="small" @click="fetchData">
                  <ReloadOutlined /> 刷新
                </a-button>
              </a-tooltip>
              <a-tooltip
                v-if="btnEnabled('batchPrint')"
                title="批量打印"
                placement="bottom"
              >
                <a-button size="small" @click="handleBatchPrint">
                  <PrinterOutlined /> 批量打印
                </a-button>
              </a-tooltip>
              <a-tooltip
                v-if="btnEnabled('print')"
                title="打印(F8)"
                placement="bottom"
              >
                <a-button size="small" @click="handlePrintSelected">
                  <PrinterOutlined /> 打印(F8)
                </a-button>
              </a-tooltip>
              <a-tooltip
                v-if="btnEnabled('export')"
                title="导出"
                placement="bottom"
              >
                <a-button size="small" :disabled="exporting" @click="handleExport">
                  <ExportOutlined /> 导出
                </a-button>
              </a-tooltip>
            </a-space>
          </div>
        </div>

        <!-- 搜索区域：字段显隐与排序由页面配置「查询条件」驱动 -->
        <div class="search-area">
          <div class="search-container" :data-expanded="showMore || null">
            <div ref="gridRef" class="search-grid">
              <!-- 查询条件：顺序与显隐由页面配置驱动 -->
              <div
                v-for="f in searchInputFields"
                :key="f.key"
                class="search-field-item"
              >
                <SearchFieldControl
                  :field="f"
                  :search-params="searchParams"
                  :date-range="dateRange"
                  @date-change="handleDateChange"
                />
              </div>

              <!-- 查询 / 重置 + 勾选类条件（对标：查询按钮及其后的勾选选项位于末尾） -->
              <div
                ref="actionRef"
                class="search-action-group"
                :style="{ gridColumn: 'span ' + actionSpan }"
              >
                <div class="search-field-item search-action-item">
                  <a-space :size="4">
                    <a-button type="primary" size="small" @click="handleSearch">
                      查询
                    </a-button>
                    <a-button size="small" @click="handleResetSearch">
                      重置
                    </a-button>
                  </a-space>
                </div>
                <div
                  v-for="f in searchCheckboxFields"
                  :key="f.key"
                  class="search-field-item"
                >
                  <label>
                    <a-checkbox v-model:checked="searchParams[f.key]">
                      {{ f.label }}
                    </a-checkbox>
                  </label>
                </div>
              </div>
            </div>
            <div class="search-more-toggle">
              <a-button type="link" size="small" @click="showMore = !showMore">
                {{ showMore ? '收起' : '更多条件' }}
                <UpOutlined v-if="showMore" />
                <DownOutlined v-else />
              </a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- ═══ 表格区域 ═══ -->
      <div class="table-area">
        <BillTableList
          :columns="exchangeColumns"
          :data-source="tableData"
          :storage-key="COLUMN_STORAGE_KEY"
          :global-config-key="COLUMN_STORAGE_KEY"
          :loading="loading"
          :pagination="billPagination"
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :show-export="false"
          :show-batch-delete="false"
          :selectable="true"
          row-key="id"
          @page-change="handlePageChange"
          @selection-change="handleSelectionChange"
        >
          <!-- 单据编号 -->
          <template #exchangeNoCell="{ record }">
            <a-button
              type="link"
              size="small"
              @click="handleView(record)"
            >
              {{ record.exchangeNo }}
            </a-button>
          </template>
          <!-- 单据状态 -->
          <template #statusCell="{ record }">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <!-- 结算状态 -->
          <template #settleStatusCell="{ record }">
            <a-tag :color="getSettleStatusColor(record.settleStatus)">
              {{ getSettleStatusText(record.settleStatus) }}
            </a-tag>
          </template>
        </BillTableList>
      </div>

      <!-- ═══ 页面配置弹窗（查询条件 20 项 / 功能按钮 6 个） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonsConfig"
        :storage-key="PAGE_CONFIG_STORAGE_KEY"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 打印弹窗（真实模板渲染 + 打印次数回写） ═══ -->
      <PrintDialog
        ref="printDialogRef"
        page-code="sale-exchange"
        :print-data="printData"
        @print-success="handlePrintSuccess"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, h, defineComponent, type PropType } from 'vue'
import { message, Modal, Input, InputNumber, Select, DatePicker } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  ExportOutlined,
  SettingOutlined,
  DownOutlined,
  UpOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { saleExchangeApi } from '@/api/erp'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { useRouter } from 'vue-router'

const router = useRouter()

// ═══ 搜索条件控件（按配置顺序渲染，避免 20 段重复模板） ═══
const SearchFieldControl = defineComponent({
  name: 'SearchFieldControl',
  props: {
    field: { type: Object as PropType<SearchFieldDef>, required: true },
    searchParams: { type: Object as PropType<Record<string, any>>, required: true },
    dateRange: { type: Array as unknown as PropType<[Dayjs, Dayjs] | null>, default: null },
  },
  emits: ['date-change'],
  setup(props, { emit }) {
    return () => {
      const f = props.field
      const value = props.searchParams[f.key]
      if (f.type === 'range') {
        return h(DatePicker.RangePicker, {
          value: props.dateRange,
          size: 'small',
          style: 'width:100%',
          'onUpdate:value': (v: any) => emit('date-change', v),
        })
      }
      if (f.type === 'number') {
        return h(InputNumber, {
          value,
          size: 'small',
          style: 'width:100%',
          placeholder: f.placeholder || f.label,
          'onUpdate:value': (v: any) => { props.searchParams[f.key] = v },
        })
      }
      if (f.type === 'select') {
        return h('div', { class: 'search-select-wrap' }, [
          h('span', { class: 'search-select-label' }, f.label),
          h(Select, {
            value,
            size: 'small',
            allowClear: true,
            placeholder: '全部',
            'onUpdate:value': (v: any) => { props.searchParams[f.key] = v },
          }, () => (f.options || []).map(o =>
            h(Select.Option, { key: String(o.value), value: o.value }, () => o.label)
          )),
        ])
      }
      return h(Input, {
        value,
        size: 'small',
        allowClear: true,
        placeholder: f.placeholder || f.label,
        'onUpdate:value': (v: any) => { props.searchParams[f.key] = v },
      })
    }
  },
})

// ═══ 快捷日期 ═══
const quickDates = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'month', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' },
  { key: 'year', label: '本年' },
]
const quickDate = ref('week')

// ═══ 查询方案 ═══
const SCHEME_STORAGE_KEY = 'sale-exchange-query-schemes'
const queryScheme = ref('')
const querySchemes = ref<{ name: string; params: Record<string, any> }[]>([])

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const showMore = ref(false)

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数 ═══
const searchParams = reactive<Record<string, any>>({
  exchangeNo: '',
  customerName: '',
  handlerName: '',
  deptName: '',
  creatorName: '',
  bookkeeperName: '',
  outWarehouseName: '',
  inWarehouseName: '',
  status: undefined,
  settleStatus: undefined,
  salesType: '',
  productLineAttr: undefined,
  remark: '',
  extNum1: undefined,
  extNum2: undefined,
  extText1: '',
  extText2: '',
  extText3: '',
  showRedFlush: false,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

// ═══ 多选状态（BillTableList 通过 selection-change 事件回传） ═══
const selectedRowKeys = ref<any[]>([])
const selectedRows = ref<any[]>([])
function handleSelectionChange(rows: any[], ids: any[]) {
  selectedRows.value = rows
  selectedRowKeys.value = ids
}

// ═══ 页面配置（查询条件 20 项 / 功能按钮 6 个） ═══
const PAGE_CONFIG_STORAGE_KEY = 'sale-exchange-page-config'
const COLUMN_STORAGE_KEY = 'sale-exchange-table-columns'
const showPageConfig = ref(false)

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
type SearchFieldType = 'range' | 'input' | 'select' | 'number' | 'checkbox'
interface SearchFieldDef {
  key: string
  label: string
  type: SearchFieldType
  placeholder?: string
  options?: { label: string; value: any }[]
}

/** 查询条件默认字段（与开发文档「页面配置 → 查询条件」20 项一一对应） */
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'exchangeNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'bookkeeperName', label: '记账人', visible: false },
  { key: 'outWarehouseName', label: '出库仓库', visible: false },
  { key: 'inWarehouseName', label: '入库仓库', visible: false },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'settleStatus', label: '结算状态', visible: true },
  { key: 'salesType', label: '销售类型', visible: false },
  { key: 'productLineAttr', label: '商品行属性', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'extNum1', label: '自定义字段1(数字)', visible: false },
  { key: 'extNum2', label: '自定义字段2(数字)', visible: false },
  { key: 'extText1', label: '自定义字段3(文本)', visible: false },
  { key: 'extText2', label: '自定义字段4(文本)', visible: false },
  { key: 'extText3', label: '自定义字段5(文本)', visible: false },
  { key: 'showRedFlush', label: '显示红冲', visible: true },
]

/** 功能按钮默认（与开发文档一致：新增/刷新/批量打印/打印(F8)/导出/配置） */
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

/** 查询字段控件定义（类型/占位/选项），渲染顺序由页面配置决定 */
const SEARCH_FIELD_DEFS: Record<string, SearchFieldDef> = {
  date: { key: 'date', label: '日期', type: 'range' },
  exchangeNo: { key: 'exchangeNo', label: '单据编号', type: 'input', placeholder: '单据编号' },
  customerName: { key: 'customerName', label: '客户', type: 'input', placeholder: '客户' },
  handlerName: { key: 'handlerName', label: '经手人', type: 'input', placeholder: '经手人' },
  deptName: { key: 'deptName', label: '部门', type: 'input', placeholder: '部门' },
  creatorName: { key: 'creatorName', label: '制单人', type: 'input', placeholder: '制单人' },
  bookkeeperName: { key: 'bookkeeperName', label: '记账人', type: 'input', placeholder: '记账人' },
  outWarehouseName: { key: 'outWarehouseName', label: '出库仓库', type: 'input', placeholder: '出库仓库' },
  inWarehouseName: { key: 'inWarehouseName', label: '入库仓库', type: 'input', placeholder: '入库仓库' },
  status: {
    key: 'status', label: '单据状态', type: 'select',
    options: [
      { label: '草稿', value: 0 },
      { label: '待审核', value: 1 },
      { label: '已审核', value: 2 },
      { label: '已完成', value: 4 },
      { label: '已拒绝', value: 5 },
      { label: '已取消', value: 6 },
    ],
  },
  settleStatus: {
    key: 'settleStatus', label: '结算状态', type: 'select',
    options: [
      { label: '未结算', value: 'unsettled' },
      { label: '部分结算', value: 'partial' },
      { label: '已结算', value: 'settled' },
    ],
  },
  salesType: { key: 'salesType', label: '销售类型', type: 'input', placeholder: '销售类型' },
  productLineAttr: {
    key: 'productLineAttr', label: '商品行属性', type: 'select',
    options: [
      { label: '普通', value: 'normal' },
      { label: '赠品', value: 'gift' },
      { label: '促销品', value: 'promo' },
    ],
  },
  remark: { key: 'remark', label: '单据备注', type: 'input', placeholder: '单据备注' },
  extNum1: { key: 'extNum1', label: '自定义字段1(数字)', type: 'number' },
  extNum2: { key: 'extNum2', label: '自定义字段2(数字)', type: 'number' },
  extText1: { key: 'extText1', label: '自定义字段3(文本)', type: 'input' },
  extText2: { key: 'extText2', label: '自定义字段4(文本)', type: 'input' },
  extText3: { key: 'extText3', label: '自定义字段5(文本)', type: 'input' },
  showRedFlush: { key: 'showRedFlush', label: '显示红冲', type: 'checkbox' },
}

const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonsConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

/** 按配置顺序 + 显隐，算出实际渲染的搜索控件 */
const orderedVisibleFields = computed<SearchFieldDef[]>(() =>
  queryFieldsConfig.value
    .filter(f => f.visible !== false)
    .map(f => SEARCH_FIELD_DEFS[f.key])
    .filter((f): f is SearchFieldDef => !!f)
)

/** 输入/选择类条件（顺序 = 页面配置拖拽顺序） */
const searchInputFields = computed(() => orderedVisibleFields.value.filter(f => f.type !== 'checkbox'))
/** 勾选类条件：与「查询/重置」同处动作区，位于整个搜索区末尾 */
const searchCheckboxFields = computed(() => orderedVisibleFields.value.filter(f => f.type === 'checkbox'))

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    // 保留「查询条件」拖拽排序：按已保存顺序还原，新增字段追加到末尾
    if (parsed.queryFields) {
      const savedFields: QueryFieldSetting[] = parsed.queryFields
      const ordered: QueryFieldSetting[] = []
      savedFields.forEach((s) => {
        const def = DEFAULT_QUERY_FIELDS.find(d => d.key === s.key)
        if (def) ordered.push({ ...def, ...s })
      })
      DEFAULT_QUERY_FIELDS.forEach((d) => {
        if (!savedFields.some(s => s.key === d.key)) ordered.push({ ...d })
      })
      queryFieldsConfig.value = ordered
    }
    if (parsed.functionButtons) {
      functionButtonsConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
        const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
        return saved ? { ...bf, ...saved } : { ...bf }
      })
    }
  } catch { /* 配置损坏时回落默认 */ }
}

function handlePageConfigChange(config: any) {
  if (config?.queryFields) queryFieldsConfig.value = config.queryFields
  if (config?.functionButtons) functionButtonsConfig.value = config.functionButtons
  loadPageConfig()
}

function btnEnabled(key: string): boolean {
  return functionButtonsConfig.value.find(b => b.key === key)?.enabled !== false
}

// ═══ 状态映射（与后端 0/1/2/4/5/6 完全一致） ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审核', color: 'processing' },
  2: { text: '已审核', color: 'blue' },
  4: { text: '已完成', color: 'success' },
  5: { text: '已拒绝', color: 'error' },
  6: { text: '已取消', color: 'default' },
}

const SETTLE_STATUS_MAP: Record<string, { text: string; color: string }> = {
  unsettled: { text: '未结算', color: 'default' },
  partial: { text: '部分结算', color: 'processing' },
  settled: { text: '已结算', color: 'success' },
}

function getStatusText(status: number): string {
  return STATUS_MAP[status]?.text || '-'
}
function getStatusColor(status: number): string {
  return STATUS_MAP[status]?.color || 'default'
}
function getSettleStatusText(settleStatus: string): string {
  return SETTLE_STATUS_MAP[settleStatus]?.text || '-'
}
function getSettleStatusColor(settleStatus: string): string {
  return SETTLE_STATUS_MAP[settleStatus]?.color || 'default'
}

// ═══ 表格列（与开发文档「表格列字段」32 列一致，序号列为表格内置） ═══
const exchangeColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '单据日期', field: 'exchangeDate', key: 'exchangeDate', width: 110, sortable: true },
  { title: '单据编号', field: 'exchangeNo', key: 'exchangeNo', width: 180, type: 'slot', slotName: 'exchangeNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '入库仓库', field: 'inWarehouseName', key: 'inWarehouseName', width: 120 },
  { title: '出库仓库', field: 'outWarehouseName', key: 'outWarehouseName', width: 120 },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 120 },
  { title: '客户', field: 'customerName', key: 'customerName', width: 160, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90 },
  { title: '部门', field: 'deptName', key: 'deptName', width: 110 },
  { title: '入库数量', field: 'inQuantityTotal', key: 'inQuantityTotal', width: 100, align: 'right' },
  { title: '出库数量', field: 'outQuantityTotal', key: 'outQuantityTotal', width: 100, align: 'right' },
  { title: '本单金额', field: 'totalAmount', key: 'totalAmount', width: 110, align: 'right', sortable: true },
  { title: '金额', field: 'productAmount', key: 'productAmount', width: 110, align: 'right' },
  { title: '折后金额', field: 'discountAmount', key: 'discountAmount', width: 110, align: 'right' },
  { title: '已结金额', field: 'settledAmount', key: 'settledAmount', width: 110, align: 'right' },
  { title: '结算状态', field: 'settleStatus', key: 'settleStatus', width: 100, align: 'center', type: 'slot', slotName: 'settleStatusCell' },
  { title: '重量(kg)', field: 'totalWeight', key: 'totalWeight', width: 100, align: 'right' },
  { title: '体积(m³)', field: 'totalVolume', key: 'totalVolume', width: 100, align: 'right' },
  { title: '销售类型', field: 'salesType', key: 'salesType', width: 100 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 150 },
  { title: '表头自定义字段1(数字)', field: 'extNum1', key: 'extNum1', width: 150, align: 'right' },
  { title: '表头自定义字段2(数字)', field: 'extNum2', key: 'extNum2', width: 150, align: 'right' },
  { title: '表头自定义字段3(文本)', field: 'extText1', key: 'extText1', width: 150 },
  { title: '表头自定义字段4(文本)', field: 'extText2', key: 'extText2', width: 150 },
  { title: '表头自定义字段5(文本)', field: 'extText3', key: 'extText3', width: 150 },
  { title: '摘要', field: 'summary', key: 'summary', width: 150 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90 },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 150 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 150, sortable: true },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 90, align: 'center' },
]

// ═══ 数据加载 ═══
function buildQueryParams(): Record<string, any> {
  const params: Record<string, any> = {}
  if (searchParams.exchangeNo) params.exchangeNo = searchParams.exchangeNo
  if (searchParams.customerName) params.customerName = searchParams.customerName
  if (searchParams.handlerName) params.handlerName = searchParams.handlerName
  if (searchParams.deptName) params.deptName = searchParams.deptName
  if (searchParams.creatorName) params.creatorName = searchParams.creatorName
  if (searchParams.bookkeeperName) params.bookkeeperName = searchParams.bookkeeperName
  if (searchParams.outWarehouseName) params.outWarehouseName = searchParams.outWarehouseName
  if (searchParams.inWarehouseName) params.inWarehouseName = searchParams.inWarehouseName
  if (searchParams.status !== undefined && searchParams.status !== null && searchParams.status !== '') {
    params.status = searchParams.status
  }
  if (searchParams.settleStatus) params.settleStatus = searchParams.settleStatus
  if (searchParams.salesType) params.salesType = searchParams.salesType
  if (searchParams.productLineAttr) params.productLineAttr = searchParams.productLineAttr
  if (searchParams.remark) params.remark = searchParams.remark
  if (searchParams.extNum1 !== undefined && searchParams.extNum1 !== null) params.extNum1 = searchParams.extNum1
  if (searchParams.extNum2 !== undefined && searchParams.extNum2 !== null) params.extNum2 = searchParams.extNum2
  if (searchParams.extText1) params.extText1 = searchParams.extText1
  if (searchParams.extText2) params.extText2 = searchParams.extText2
  if (searchParams.extText3) params.extText3 = searchParams.extText3
  if (searchParams.showRedFlush) params.showRedFlush = true
  if (searchParams.startDate) params.startDate = searchParams.startDate
  if (searchParams.endDate) params.endDate = searchParams.endDate
  return params
}

async function fetchData() {
  loading.value = true
  hasError.value = false
  try {
    const res: any = await saleExchangeApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ...buildQueryParams(),
    } as any)
    if (res) {
      tableData.value = res.records || []
      pagination.total = res.total || 0
    }
  } catch (error: any) {
    hasError.value = true
    console.warn('[销售换货单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件处理 ═══
function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: Dayjs, end: Dayjs
  switch (key) {
    case 'yesterday':
      start = now.subtract(1, 'day'); end = now.subtract(1, 'day'); break
    case 'today':
      start = now; end = now; break
    case 'week':
      start = now.startOf('week'); end = now; break
    case 'lastWeek':
      start = now.subtract(7, 'day'); end = now; break
    case 'month':
      start = now.startOf('month'); end = now; break
    case 'lastMonth':
      start = now.subtract(1, 'month').startOf('month'); end = now.subtract(1, 'month').endOf('month'); break
    case 'last3Month':
      start = now.subtract(3, 'month'); end = now; break
    case 'year':
      start = now.startOf('year'); end = now; break
    default:
      start = now.subtract(7, 'day'); end = now
  }
  dateRange.value = [start, end]
  searchParams.startDate = start.format('YYYY-MM-DD')
  searchParams.endDate = end.format('YYYY-MM-DD')
  handleSearch()
}

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2 && dates[0] && dates[1]) {
    dateRange.value = [dates[0], dates[1]]
    searchParams.startDate = dates[0].format('YYYY-MM-DD')
    searchParams.endDate = dates[1].format('YYYY-MM-DD')
  } else {
    dateRange.value = null
    searchParams.startDate = ''
    searchParams.endDate = ''
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleResetSearch() {
  Object.assign(searchParams, {
    exchangeNo: '', customerName: '', handlerName: '', deptName: '',
    creatorName: '', bookkeeperName: '', outWarehouseName: '', inWarehouseName: '',
    status: undefined, settleStatus: undefined, salesType: '', productLineAttr: undefined,
    remark: '', extNum1: undefined, extNum2: undefined,
    extText1: '', extText2: '', extText3: '', showRedFlush: false,
  })
  setQuickDate('week')
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 查询方案 ═══
function loadSchemes() {
  try {
    querySchemes.value = JSON.parse(localStorage.getItem(SCHEME_STORAGE_KEY) || '[]')
  } catch {
    querySchemes.value = []
  }
}

function handleSaveScheme() {
  let name = ''
  Modal.confirm({
    title: '保存查询方案',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;font-size:13px;color:#595959' }, '将当前查询条件保存为本机查询方案'),
      h('input', {
        class: 'ant-input ant-input-sm',
        placeholder: '方案名称',
        onInput: (e: any) => { name = e.target.value },
      }),
    ]),
    onOk: () => {
      const schemeName = (name || '').trim()
      if (!schemeName) {
        message.warning('请输入方案名称')
        return Promise.reject()
      }
      const list = querySchemes.value.filter(s => s.name !== schemeName)
      list.push({ name: schemeName, params: { ...buildQueryParams() } })
      querySchemes.value = list.slice(-20)
      localStorage.setItem(SCHEME_STORAGE_KEY, JSON.stringify(querySchemes.value))
      queryScheme.value = schemeName
      message.success(`查询方案「${schemeName}」已保存`)
      return undefined
    },
  })
}

function handleSchemeChange(name: string) {
  if (!name) return
  const scheme = querySchemes.value.find(s => s.name === name)
  if (!scheme) return
  const p = scheme.params || {}
  Object.assign(searchParams, {
    exchangeNo: p.exchangeNo || '', customerName: p.customerName || '',
    handlerName: p.handlerName || '', deptName: p.deptName || '',
    creatorName: p.creatorName || '', bookkeeperName: p.bookkeeperName || '',
    outWarehouseName: p.outWarehouseName || '', inWarehouseName: p.inWarehouseName || '',
    status: p.status, settleStatus: p.settleStatus, salesType: p.salesType || '',
    productLineAttr: p.productLineAttr, remark: p.remark || '',
    extNum1: p.extNum1, extNum2: p.extNum2,
    extText1: p.extText1 || '', extText2: p.extText2 || '', extText3: p.extText3 || '',
    showRedFlush: !!p.showRedFlush,
    startDate: p.startDate || '', endDate: p.endDate || '',
  })
  if (p.startDate && p.endDate) {
    dateRange.value = [dayjs(p.startDate), dayjs(p.endDate)]
  }
  handleSearch()
}

// ═══ 操作 ═══
function handleAdd() {
  router.push('/sales/exchange/form')
}

function handleView(record: any) {
  router.push(`/sales/exchange/form?id=${record.id}`)
}

// ═══ 打印（真实模板渲染 + 打印次数回写） ═══
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({})
const pendingPrintIds = ref<any[]>([])
const pendingPrintIsBatch = ref(false)

function handlePrintSelected() {
  if (selectedRowKeys.value.length !== 1) {
    message.warning('请选择一张换货单进行打印（多张请使用批量打印）')
    return
  }
  const id = selectedRowKeys.value[0]
  saleExchangeApi.getById(id)
    .then((res: any) => {
      printData.value = { doc: res, items: res?.items || [] }
      pendingPrintIds.value = [id]
      pendingPrintIsBatch.value = false
      printDialogRef.value?.open()
    })
    .catch(() => message.error('加载打印数据失败'))
}

function handleBatchPrint() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的换货单')
    return
  }
  printData.value = { docs: selectedRows.value }
  pendingPrintIds.value = [...selectedRowKeys.value]
  pendingPrintIsBatch.value = true
  printDialogRef.value?.open()
}

async function handlePrintSuccess() {
  try {
    if (pendingPrintIsBatch.value) {
      await saleExchangeApi.batchPrint(pendingPrintIds.value)
    } else if (pendingPrintIds.value.length === 1) {
      await saleExchangeApi.print(pendingPrintIds.value[0])
    }
    fetchData()
  } catch { /* 打印次数回写失败不影响打印结果 */ }
}

// ═══ 导出（后端真实 xlsx） ═══
async function handleExport() {
  exporting.value = true
  try {
    const blob: any = await saleExchangeApi.export(buildQueryParams())
    const url = URL.createObjectURL(blob as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `销售换货单_${dayjs().format('YYYY-MM-DD')}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e: any) {
    message.error(e?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

const handleError = (error: Error) => {
  hasError.value = true
  console.error('[销售换货单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ F8 打印快捷键 ═══
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrintSelected()
  }
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  loadSchemes()
  setQuickDate('week')
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.page-header {
  background: #fff;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

/* ═══ 查询方案 ═══ */
.query-scheme-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* ═══ 快捷日期 ═══ */
.quick-dates :deep(.ant-btn) {
  font-size: 13px;
  padding: 2px 8px;
}
.quick-dates :deep(.ant-btn-primary) {
  color: #fff;
  background: #ff7a45;
  border-color: #ff7a45;
}

/* ═══ 搜索区域 ═══ */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; margin-top: 0; }
.search-container:not(:has([data-expanded])) > .search-grid { max-height: 80px; overflow: hidden; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-select .ant-select-selector) { font-size: 13px; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 0; }
.search-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; border-radius: 0 !important; box-shadow: none !important; padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center; }
.search-action-item { flex-shrink: 0; }
.search-action-group {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}
.search-action-group .search-field-item {
  flex: 0 0 auto;
  width: auto;
  margin-right: 4px;
}
.search-action-group .search-field-item:last-child {
  margin-right: 0;
}
.search-more-toggle {
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.search-more-toggle::before,
.search-more-toggle::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e8e8e8;
}
.search-more-toggle :deep(.ant-btn) {
  font-size: 13px;
}

/* ═══ 表格区域 ═══ */
.table-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  padding: 12px 16px;
}

/* ═══ 紧凑尺寸 ═══ */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
