<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 快捷日期 ═══ -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <a-select
              v-model:value="queryScheme"
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
            >
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
            <a-button
              type="link"
              size="small"
              style="padding: 0 4px"
            >
              <PlusOutlined />
            </a-button>
          </div>
          <a-space :size="4" class="quick-dates">
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
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-dropdown>
              <a-button size="small">
                <ExportOutlined /> 导出 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="handleExportMenu">
                  <a-menu-item key="csv">导出CSV</a-menu-item>
                  <a-menu-item key="excel">导出Excel</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- ═══ 搜索区域（18个查询条件 + 更多条件） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container" :data-expanded="showMoreConditions || null">
            <div class="search-grid" ref="gridRef">
              <!-- 固定：日期范围 -->
              <div v-show="queryFieldVisible('dateRange')" class="search-field-item">
                <a-range-picker
                  v-model:value="documentDateRange"
                  size="small"
                  style="width: 100%"
                  @change="handleDocumentDateChange"
                />
              </div>
              <!-- 单据编号 -->
              <div v-show="queryFieldVisible('documentNo')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.documentNo"
                  placeholder="单据编号"
                  allow-clear
                  size="small"
                />
              </div>
              <!-- 供应商 -->
              <div v-show="queryFieldVisible('supplierName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.supplierName"
                  placeholder="供应商"
                  allow-clear
                  size="small"
                  :suffix="h(SearchOutlined, { style: 'color:#bbb' })"
                />
              </div>
              <!-- 经手人 -->
              <div v-show="queryFieldVisible('handlerName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.handlerName"
                  placeholder="经手人"
                  allow-clear
                  size="small"
                  :suffix="h(SearchOutlined, { style: 'color:#bbb' })"
                />
              </div>
              <!-- 部门 -->
              <div v-show="queryFieldVisible('departmentName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.departmentName"
                  placeholder="部门"
                  allow-clear
                  size="small"
                />
              </div>
              <!-- 制单人 -->
              <div v-show="queryFieldVisible('creatorName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.creatorName"
                  placeholder="制单人"
                  allow-clear
                  size="small"
                />
              </div>
              <!-- 记账人 -->
              <div v-show="queryFieldVisible('bookkeeperName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.bookkeeperName"
                  placeholder="记账人"
                  allow-clear
                  size="small"
                />
              </div>
              <!-- 仓库 -->
              <div v-show="queryFieldVisible('warehouseName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.warehouseName"
                  placeholder="仓库"
                  allow-clear
                  size="small"
                  :suffix="h(SearchOutlined, { style: 'color:#bbb' })"
                />
              </div>
              <!-- 结算状态 -->
              <div v-show="queryFieldVisible('settlementStatus')" class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">结算状态</span>
                  <a-select
                    v-model:value="searchParams.settlementStatus"
                    placeholder="全部"
                    allow-clear
                    size="small"
                  >
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option value="已结算">已结算</a-select-option>
                    <a-select-option value="未结算">未结算</a-select-option>
                  </a-select>
                </div>
              </div>
              <!-- 来源订单 -->
              <div v-show="queryFieldVisible('sourceOrder')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.sourceOrder"
                  placeholder="来源订单"
                  allow-clear
                  size="small"
                />
              </div>
              <!-- 单据类型 -->
              <div v-show="queryFieldVisible('documentType')" class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">单据类型</span>
                  <a-select
                    v-model:value="searchParams.documentType"
                    placeholder="全部单据"
                    allow-clear
                    size="small"
                  >
                    <a-select-option value="">全部单据</a-select-option>
                    <a-select-option value="INBOUND">采购入库单</a-select-option>
                    <a-select-option value="RETURN">采购退货单</a-select-option>
                    <a-select-option value="EXCHANGE">采购换货单</a-select-option>
                  </a-select>
                </div>
              </div>
              <!-- 单据备注 -->
              <div v-show="queryFieldVisible('remark')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.remark"
                  placeholder="单据备注"
                  allow-clear
                  size="small"
                />
              </div>
              <!-- 更多条件（展开后显示） -->
              <template v-if="showMoreConditions">
                <!-- 自定义字段1(数字) 范围 -->
                <div v-show="queryFieldVisible('extNum1')" class="search-field-item">
                  <a-input-number
                    v-model:value="searchParams.extNum1Min"
                    placeholder="自定义1最小值"
                    size="small"
                    style="width: 100%"
                  />
                  <span style="margin: 0 4px; flex-shrink: 0">-</span>
                  <a-input-number
                    v-model:value="searchParams.extNum1Max"
                    placeholder="自定义1最大值"
                    size="small"
                    style="width: 100%"
                  />
                </div>
                <!-- 自定义字段2(数字) 范围 -->
                <div v-show="queryFieldVisible('extNum2')" class="search-field-item">
                  <a-input-number
                    v-model:value="searchParams.extNum2Min"
                    placeholder="自定义2最小值"
                    size="small"
                    style="width: 100%"
                  />
                  <span style="margin: 0 4px; flex-shrink: 0">-</span>
                  <a-input-number
                    v-model:value="searchParams.extNum2Max"
                    placeholder="自定义2最大值"
                    size="small"
                    style="width: 100%"
                  />
                </div>
                <!-- 自定义字段3(文本) -->
                <div v-show="queryFieldVisible('extText1')" class="search-field-item">
                  <a-input
                    v-model:value="searchParams.extText1"
                    placeholder="自定义字段3"
                    allow-clear
                    size="small"
                  />
                </div>
                <!-- 自定义字段4(文本) -->
                <div v-show="queryFieldVisible('extText2')" class="search-field-item">
                  <a-input
                    v-model:value="searchParams.extText2"
                    placeholder="自定义字段4"
                    allow-clear
                    size="small"
                  />
                </div>
                <!-- 自定义字段5(文本) -->
                <div v-show="queryFieldVisible('extText3')" class="search-field-item">
                  <a-input
                    v-model:value="searchParams.extText3"
                    placeholder="自定义字段5"
                    allow-clear
                    size="small"
                  />
                </div>
              </template>
              <!-- 查询按钮 + 勾选框 -->
              <div class="search-action-group" ref="actionRef" :style="{ gridColumn: 'span ' + actionSpan }">
                <div class="search-field-item search-action-item">
                  <a-button type="primary" size="small" @click="handleSearch">
                    <SearchOutlined /> 查询
                  </a-button>
                </div>
                <div v-show="queryFieldVisible('showRed')" class="search-field-item">
                  <a-checkbox v-model:checked="searchParams.showRed">显示红冲单据</a-checkbox>
                </div>
              </div>
            </div>
            <div class="search-more-toggle">
              <a-button size="small" @click="toggleMoreConditions">
                <DownOutlined v-if="!showMoreConditions" />
                <UpOutlined v-else />
                {{ showMoreConditions ? '收起' : '更多条件' }}
              </a-button>
            </div>
          </div>
          </div>
        </template>

        <!-- ═══ 数据表格 ═══ -->
        <template #table>
          <BillTableList
            :columns="columns"
            :storage-key="'purchase-doc-query-table-columns'"
            :data-source="tableData"
            :loading="loading"
            :pagination="billPagination"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            :selectable="false"
            row-key="id"
            @page-change="handlePageChange"
          >
            <!-- 操作列 -->
            <template #actionCell="{ record }">
              <a-space :size="4">
                <a-button type="link" size="small" @click="handleCopy(record)">复制</a-button>
                <a-button type="link" size="small" @click="handleNote(record)">整单备注</a-button>
              </a-space>
            </template>
            <!-- 单据类型中文显示 -->
            <template #documentTypeCell="{ record }">
              {{ DOC_TYPE_LABELS[record.documentType] || record.documentType }}
            </template>
          </BillTableList>
        </template>

        <!-- ═══ 表格底部：合计 ═══ -->
        <template #table-footer>
          <div class="table-footer">
            <div class="footer-label">合计</div>
            <div class="footer-values">
              <span v-if="summaryData.purchaseQuantity">采购数量: {{ summaryData.purchaseQuantity }}</span>
              <span v-if="summaryData.amount">金额: {{ summaryData.amount }}</span>
              <span v-if="summaryData.totalAmount">本单金额: {{ summaryData.totalAmount }}</span>
              <span v-if="summaryData.fee">其他费用: {{ summaryData.fee }}</span>
            </div>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonConfig"
      :storage-key="PAGE_CONFIG_STORAGE_KEY"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 整单备注弹窗 ═══ -->
    <a-modal
      v-model:open="showNoteModal"
      title="整单备注"
      :width="480"
      @ok="saveNote"
      @cancel="showNoteModal = false"
    >
      <div style="margin-bottom: 8px; color: #666; font-size: 12px">
        单据编号: {{ noteRecord.documentNo }}
      </div>
      <a-textarea
        v-model:value="noteContent"
        :rows="4"
        placeholder="请输入整单备注内容"
      />
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, SearchOutlined, DownOutlined, UpOutlined,
  SettingOutlined, PlusOutlined, PrinterOutlined, ExportOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { purchaseDocUnifiedApi } from '@/api/purchase'

const router = useRouter()

// ═══ 状态 ═══
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const showMoreConditions = ref(false)
const showPageConfig = ref(false)

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 查询方案 ═══
const queryScheme = ref('')

// ═══ 快捷日期 ═══
const quickDate = ref('thisWeek')
const quickDates = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'thisWeek', label: '本周' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'thisMonth', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Months', label: '近三月' },
  { key: 'thisYear', label: '本年' },
]

const documentDateRange = ref<[Dayjs, Dayjs] | null>(null)

function setQuickDate(key: string) {
  quickDate.value = key
  const today = dayjs()
  let start: Dayjs
  let end: Dayjs

  switch (key) {
    case 'yesterday': start = end = today.subtract(1, 'day'); break
    case 'today': start = end = today; break
    case 'thisWeek': start = today.startOf('week'); end = today; break
    case 'lastWeek': start = today.subtract(7, 'day'); end = today; break
    case 'thisMonth': start = today.startOf('month'); end = today; break
    case 'lastMonth': start = today.subtract(1, 'month').startOf('month'); end = today.subtract(1, 'month').endOf('month'); break
    case 'last3Months': start = today.subtract(3, 'month'); end = today; break
    case 'thisYear': start = today.startOf('year'); end = today; break
    default: return
  }

  documentDateRange.value = [start, end]
  searchParams.dateStart = start.format('YYYY-MM-DD')
  searchParams.dateEnd = end.format('YYYY-MM-DD')
  handleSearch()
}

// ═══ 搜索参数（18个查询条件） ═══
const searchParams = reactive({
  documentNo: '',
  documentType: '',
  dateStart: '',
  dateEnd: '',
  supplierName: '',
  supplierCode: '',
  handlerName: '',
  departmentName: '',
  creatorName: '',
  bookkeeperName: '',
  warehouseName: '',
  settlementStatus: '',
  sourceOrder: '',
  remark: '',
  extNum1Min: null as number | null,
  extNum1Max: null as number | null,
  extNum2Min: null as number | null,
  extNum2Max: null as number | null,
  extText1: '',
  extText2: '',
  extText3: '',
  status: undefined as number | undefined,
  showRed: false,
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total
}))

// ═══ 单据类型中文 ═══
const DOC_TYPE_LABELS: Record<string, string> = {
  INBOUND: '采购入库单',
  RETURN: '采购退货单',
  EXCHANGE: '采购换货单',
}

// ═══ 表格列（对标 37 列 + 序号/操作） ═══
const columns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' as const },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right' as const, slotName: 'actionCell' },
  { title: '单据日期', field: 'documentDate', key: 'documentDate', width: 100, sortable: true },
  { title: '单据编号', field: 'documentNo', key: 'documentNo', width: 140, sortable: true },
  { title: '单据类型', field: 'documentType', key: 'documentType', width: 100, slots: { default: 'documentTypeCell' } },
  { title: '入库仓库', field: 'inboundWarehouse', key: 'inboundWarehouse', width: 100 },
  { title: '出库仓库', field: 'outboundWarehouse', key: 'outboundWarehouse', width: 100, sortable: true },
  { title: '供应商名称', field: 'supplierName', key: 'supplierName', width: 140, sortable: true },
  { title: '供应商编号', field: 'supplierCode', key: 'supplierCode', width: 110 },
  { title: '联系人', field: 'contactName', key: 'contactName', width: 80 },
  { title: '联系电话', field: 'contactPhone', key: 'contactPhone', width: 110 },
  { title: '联系地址', field: 'contactAddress', key: 'contactAddress', width: 140 },
  { title: '供应商备注', field: 'supplierRemark', key: 'supplierRemark', width: 120 },
  { title: '来源订单', field: 'sourceOrder', key: 'sourceOrder', width: 140 },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80 },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 80 },
  { title: '结算状态', field: 'settlementStatus', key: 'settlementStatus', width: 80 },
  { title: '采购数量', field: 'purchaseQuantity', key: 'purchaseQuantity', width: 80, align: 'right' as const },
  { title: '金额', field: 'amount', key: 'amount', width: 90, align: 'right' as const },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 90, align: 'right' as const },
  { title: '优惠后金额', field: 'favorableAmount', key: 'favorableAmount', width: 90, align: 'right' as const },
  { title: '税额', field: 'taxAmount', key: 'taxAmount', width: 80, align: 'right' as const },
  { title: '价税合计', field: 'totalAmountWithTax', key: 'totalAmountWithTax', width: 90, align: 'right' as const },
  { title: '本单金额', field: 'totalAmount', key: 'totalAmount', width: 90, align: 'right' as const },
  { title: '其他费用', field: 'fee', key: 'fee', width: 80, align: 'right' as const },
  { title: '优惠金额', field: 'discountAmount', key: 'discountAmount', width: 80, align: 'right' as const },
  { title: '表头自定义字段1(数字)', field: 'extNum1', key: 'extNum1', width: 80, align: 'right' as const },
  { title: '表头自定义字段2(数字)', field: 'extNum2', key: 'extNum2', width: 80, align: 'right' as const },
  { title: '表头自定义字段3(文本)', field: 'extText1', key: 'extText1', width: 100 },
  { title: '表头自定义字段4(文本)', field: 'extText2', key: 'extText2', width: 100 },
  { title: '表头自定义字段5(文本)', field: 'extText3', key: 'extText3', width: 100 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 120 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 60 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 80 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80 },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 100 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 100 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 60, align: 'right' as const },
]

// ═══ 合计汇总 ═══
const summaryData = computed(() => {
  const data = tableData.value
  if (!data.length) return {}
  const sum = (key: string) => {
    const val = data.reduce((acc, r) => acc + (Number(r[key]) || 0), 0)
    return val ? val.toFixed(2) : ''
  }
  return {
    purchaseQuantity: sum('purchaseQuantity'),
    amount: sum('amount'),
    totalAmount: sum('totalAmount'),
    fee: sum('fee'),
  }
})

// ═══ 页面配置（18个查询条件 + 功能按钮） ═══
const queryFieldsConfig = ref([
  { key: 'dateRange', label: '日期', visible: true },
  { key: 'documentNo', label: '单据编号', visible: true },
  { key: 'supplierName', label: '供应商', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'departmentName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'bookkeeperName', label: '记账人', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'settlementStatus', label: '结算状态', visible: true },
  { key: 'sourceOrder', label: '来源订单', visible: true },
  { key: 'documentType', label: '单据类型', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  // ⚠️ 语义统一为「该查询条件是否启用」，**不是**「是否已折叠进更多条件」。
  // 这几个默认 true：它们本来就在「更多条件」折叠区里，展开/收起由 showMoreConditions 控制；
  // 若这里再给 false，用户展开「更多条件」后仍看不到它们（页面配置与折叠区互相打架）。
  { key: 'extNum1', label: '自定义字段1(数字)', visible: true },
  { key: 'extNum2', label: '自定义字段2(数字)', visible: true },
  { key: 'extText1', label: '自定义字段3(文本)', visible: true },
  { key: 'extText2', label: '自定义字段4(文本)', visible: true },
  { key: 'extText3', label: '自定义字段5(文本)', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
])

const functionButtonConfig = ref([
  { key: 'search', label: '查询', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'more', label: '更多条件', enabled: true },
])

/** 页面配置在 localStorage 的键，与 PageConfigPanel 的 storage-key 同源 */
const PAGE_CONFIG_STORAGE_KEY = 'purchase-doc-query-page-config'

/** 查询条件是否启用（由页面配置弹窗的「查询条件」页签控制） */
function queryFieldVisible(key: string): boolean {
  const field = queryFieldsConfig.value.find(f => f.key === key)
  return field ? field.visible : true
}

/** 应用一份页面配置（弹窗 change 时传入；挂载时从 localStorage 还原） */
function applyPageConfig(config: { queryFields?: any[] }) {
  if (Array.isArray(config?.queryFields)) {
    queryFieldsConfig.value = queryFieldsConfig.value.map(df => {
      const saved = config.queryFields!.find((f: any) => f.key === df.key)
      return saved ? { ...df, visible: saved.visible !== false } : df
    })
  }
}

function handlePageConfigChange(config: any) {
  applyPageConfig(config || {})
}

/** 挂载时还原已保存配置：PageConfigPanel 只在被打开时才读存档，不会主动同步给页面 */
function restoreSavedPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (raw) {
      applyPageConfig(JSON.parse(raw))
    }
  } catch { /* 配置损坏时按默认展示 */ }
}

// ═══ 日期处理 ═══
const handleDocumentDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) {
    searchParams.dateStart = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.dateEnd = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.dateStart = ''
    searchParams.dateEnd = ''
  }
}

const toggleMoreConditions = () => {
  showMoreConditions.value = !showMoreConditions.value
}

// ═══ 数据请求 ═══
const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const apiParams: Record<string, any> = {
      current: pagination.current,
      size: pagination.pageSize,
      documentNo: searchParams.documentNo,
      documentType: searchParams.documentType,
      dateStart: searchParams.dateStart,
      dateEnd: searchParams.dateEnd,
      supplierName: searchParams.supplierName,
      supplierCode: searchParams.supplierCode,
      handlerName: searchParams.handlerName,
      departmentName: searchParams.departmentName,
      creatorName: searchParams.creatorName,
      bookkeeperName: searchParams.bookkeeperName,
      warehouseName: searchParams.warehouseName,
      settlementStatus: searchParams.settlementStatus,
      sourceOrder: searchParams.sourceOrder,
      remark: searchParams.remark,
      extNum1Start: searchParams.extNum1Min,
      extNum1End: searchParams.extNum1Max,
      extNum2Start: searchParams.extNum2Min,
      extNum2End: searchParams.extNum2Max,
      extText1: searchParams.extText1,
      extText2: searchParams.extText2,
      extText3: searchParams.extText3,
      showRed: searchParams.showRed || undefined,
    }
    // 清理空字符串/空值参数
    Object.keys(apiParams).forEach(key => {
      if (apiParams[key] === '' || apiParams[key] === null || apiParams[key] === undefined) {
        delete apiParams[key]
      }
    })

    const res: any = await purchaseDocUnifiedApi.page(apiParams)
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || []
      pagination.total = data.total || 0
    }
  } catch (e: any) {
    hasError.value = true
    message.error('查询失败，请检查网络后重试')
    console.warn('[采购单据查询] 获取失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 导出 ═══
function handleExportMenu({ key }: { key: string | number }) {
  if (tableData.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }

  // 导出列
  const exportCols = columns.filter((c: any) => c.key !== 'rowNo' && c.key !== 'action')
  const headers = exportCols.map((c: any) => c.title)
  const rows = tableData.value.map(row =>
    exportCols.map((col: any) => {
      const val = row[col.field]
      if (val === null || val === undefined) return ''
      if (col.key === 'documentType') return DOC_TYPE_LABELS[val] || val
      return String(val)
    })
  )

  if (key === 'csv') {
    const csv = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `采购单据查询_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } else {
    const tsv = [headers.join('\t'), ...rows.map(r => r.join('\t'))].join('\n')
    const blob = new Blob(['\uFEFF' + tsv], { type: 'application/vnd.ms-excel;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `采购单据查询_${dayjs().format('YYYYMMDD_HHmmss')}.xls`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  }
}

// ═══ 打印 ═══
function handlePrint() {
  window.print()
}

// ═══ 复制 ═══
const COPY_ROUTE_MAP: Record<string, string> = {
  INBOUND: '/purchase/inbound/form',
  RETURN: '/purchase/return/form',
  EXCHANGE: '/purchase/exchange/form',
}

function handleCopy(record: any) {
  const route = COPY_ROUTE_MAP[record.documentType]
  if (route) {
    router.push({ path: route, query: { copyFrom: record.id } })
  } else {
    message.warning('暂不可用该单据类型的复制')
  }
}

// ═══ 整单备注 ═══
const showNoteModal = ref(false)
const noteRecord = reactive<any>({ documentNo: '', id: null, documentType: '' })
const noteContent = ref('')

function handleNote(record: any) {
  noteRecord.documentNo = record.documentNo
  noteRecord.id = record.id
  noteRecord.documentType = record.documentType
  noteContent.value = record.remark || ''
  showNoteModal.value = true
}

async function saveNote() {
  try {
    await purchaseDocUnifiedApi.updateRemark(noteRecord.documentType, noteRecord.id, noteContent.value)
    message.success('备注保存成功')
    showNoteModal.value = false
    fetchData()
  } catch {
    message.error('备注保存失败')
  }
}

// ═══ 错误处理 ═══
const handleError = (e: Error) => {
  hasError.value = true
  console.error(e)
}

onMounted(() => {
  restoreSavedPageConfig()
  // 初始化默认快捷日期（近一周）
  setQuickDate('thisWeek')
})
</script>

<style scoped>
/* ═══ 查询方案 ═══ */
.query-scheme-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* ═══ 快捷日期 ═══ */
.quick-dates :deep(.ant-btn-link) {
  color: #555;
  padding: 0 8px;
  height: 24px;
  line-height: 24px;
}
.quick-dates :deep(.ant-btn-primary) {
  color: #fff;
  background: #fa8c16;
  border-color: #fa8c16;
}
.quick-dates :deep(.ant-btn-primary:hover) {
  background: #fa8c16;
  border-color: #fa8c16;
}

/* ═══ 搜索区域 ═══ */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
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
  width: auto;
  flex: 0 0 auto;
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

/* ═══ 表格底部合计 ═══ */
.table-footer {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-top: none;
  font-size: 12px;
  color: #333;
}
.footer-label {
  font-weight: 600;
  min-width: 170px;
}
.footer-values {
  flex: 1;
  display: flex;
  gap: 16px;
}
</style>
