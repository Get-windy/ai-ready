<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
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
              <a-select-option value="">
                --查询方案--
              </a-select-option>
              <a-select-option value="draft">草稿</a-select-option>
              <a-select-option value="pending">待审批</a-select-option>
              <a-select-option value="approved">已审核</a-select-option>
              <a-select-option value="completed">已完成</a-select-option>
            </a-select>
            <a-button type="link" size="small" style="padding: 0 4px">
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

        <!-- ═══ 工具栏右侧：列配置/页面配置/新增/刷新/批量打印/打印/导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="列配置">
              <a-button size="small" @click="showColumnConfig = true">
                <TableOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button type="primary" size="small" @click="handleAdd">
              <PlusOutlined /> 新增
            </a-button>
            <a-button size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="activeTab === 'doc'" size="small" @click="handleBatchPrint">
              <PrinterOutlined /> 批量打印
            </a-button>
            <a-button size="small" @click="handlePrintF8">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <!-- 按单据 Tab 搜索行 -->
            <template v-if="activeTab === 'doc'">
              <div class="search-container">
                <div class="search-grid" ref="docGridRef">
                  <div class="search-field-item">
                    <a-range-picker
                      v-model:value="docDateRange"
                      size="small"
                      style="width: 100%"
                      @change="handleDocDateChange"
                    />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="docSearch.receiptNo" placeholder="单据编号" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="docSearch.customerName" placeholder="结算单位" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="docSearch.receiptAccount" placeholder="收款账户" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="docSearch.handlerName" placeholder="经手人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="docSearch.departmentName" placeholder="部门" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="docSearch.creatorName" placeholder="制单人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="docSearch.bookkeeperName" placeholder="记账人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="docSearch.auditorName" placeholder="审核人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">单据状态</span>
                      <a-select v-model:value="docSearch.status" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="0">草稿</a-select-option>
                        <a-select-option :value="1">待审批</a-select-option>
                        <a-select-option :value="2">已审批</a-select-option>
                        <a-select-option :value="3">已拒绝</a-select-option>
                        <a-select-option :value="4">待核销</a-select-option>
                        <a-select-option :value="5">核销中</a-select-option>
                        <a-select-option :value="6">已核销</a-select-option>
                        <a-select-option :value="7">已完成</a-select-option>
                        <a-select-option :value="8">已取消</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="docSearch.remark" placeholder="单据备注" allow-clear size="small" />
                  </div>
                  <div class="search-action-group" ref="docActionRef" :style="{ gridColumn: 'span ' + docActionSpan }">
                    <div class="search-field-item search-action-item">
                      <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                    </div>
                    <div class="search-field-item">
                      <a-checkbox v-model:checked="docSearch.showRed">显示红冲</a-checkbox>
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <!-- 按明细 Tab 搜索行 -->
            <template v-else>
              <div class="search-container">
                <div class="search-grid" ref="detailGridRef">
                  <div class="search-field-item">
                    <a-range-picker
                      v-model:value="detailDateRange"
                      size="small"
                      style="width: 100%"
                      @change="handleDetailDateChange"
                    />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="detailSearch.receiptNo" placeholder="单据编号" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="detailSearch.customerName" placeholder="结算单位" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="detailSearch.handlerName" placeholder="经手人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="detailSearch.departmentName" placeholder="部门" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="detailSearch.bookkeeperName" placeholder="记账人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="detailSearch.auditorName" placeholder="审核人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">单据状态</span>
                      <a-select v-model:value="detailSearch.status" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="0">草稿</a-select-option>
                        <a-select-option :value="1">待审批</a-select-option>
                        <a-select-option :value="2">已审批</a-select-option>
                        <a-select-option :value="3">已拒绝</a-select-option>
                        <a-select-option :value="4">待核销</a-select-option>
                        <a-select-option :value="5">核销中</a-select-option>
                        <a-select-option :value="6">已核销</a-select-option>
                        <a-select-option :value="7">已完成</a-select-option>
                        <a-select-option :value="8">已取消</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="detailSearch.tradeUnit" placeholder="往来单位" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="detailSearch.sourceHandler" placeholder="源单经手人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="detailSearch.settlementNo" placeholder="结算单据编号" allow-clear size="small" />
                  </div>
                  <div class="search-action-group" ref="detailActionRef" :style="{ gridColumn: 'span ' + detailActionSpan }">
                    <div class="search-field-item search-action-item">
                      <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                    </div>
                    <div class="search-field-item">
                      <a-checkbox v-model:checked="detailSearch.showRed">显示红冲</a-checkbox>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="currentColumns"
              :data-source="tableData"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :show-summary="true"
              :summary-data="tableFooterColumns"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #receiptNoCell="{ record }">
                <a-button type="link" size="small" @click="handleView(record)">
                  {{ record.receiptNo || '-' }}
                </a-button>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status !== undefined ? record.status : record.receiptStatus)">
                  {{ getStatusText(record.status !== undefined ? record.status : record.receiptStatus) }}
                </a-tag>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" @click="handleEdit(record)">修改</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" @click="handleConfirm(record)">记账</a-button>
                  <a-button
                    v-if="[1, 2, 4, 5].includes(record.status)"
                    type="link"
                    size="small"
                    danger
                    @click="handleCancel(record)"
                  >取消</a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 列配置弹窗 ═══ -->
    <ColumnConfigPanel
      :open="showColumnConfig"
      :settings-columns="panelColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showColumnConfig = $event"
      @change="handleColumnConfigChange"
      @reset="handleColumnConfigReset"
      @drag-end="handleColumnConfigChange"
    />

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryFields"
      :function-buttons-config="activeFunctionButtons"
      :storage-key="activePageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 批量打印弹窗 ═══ -->
    <a-modal v-model:open="showPrintDialog" title="批量打印" :width="400" @ok="confirmPrint">
      <div style="padding: 16px 0;">
        <div style="margin-bottom: 8px;">打印模板：</div>
        <a-select v-model:value="printTemplate" style="width: 100%;">
          <a-select-option value="default">标准模板</a-select-option>
          <a-select-option value="simple">简化模板</a-select-option>
          <a-select-option value="detailed">详细模板</a-select-option>
        </a-select>
      </div>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, SettingOutlined,
  TableOutlined, ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { receiptApi } from '@/api/finance'
import { useRouter } from 'vue-router'

defineOptions({ name: 'FinanceReceiptDocList' })

const router = useRouter()

// ═══ Tab 配置 ═══
const tabs = [
  { key: 'doc', label: '按单据' },
  { key: 'detail', label: '收款明细' },
]
const activeTab = ref('doc')

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
const queryScheme = ref('')

const loading = ref(false)
const tableData = ref<any[]>([])

const docGridRef = ref<HTMLElement | null>(null)
const docActionRef = ref<HTMLElement | null>(null)
const detailGridRef = ref<HTMLElement | null>(null)
const detailActionRef = ref<HTMLElement | null>(null)
const { span: docActionSpan } = useAutoGridSpan(docActionRef, docGridRef)
const { span: detailActionSpan } = useAutoGridSpan(detailActionRef, detailGridRef)

const docDateRange = ref<[any, any] | null>([dayjs().subtract(7, 'day'), dayjs()])
const detailDateRange = ref<[any, any] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数（按单据） ═══
const docSearch = reactive<any>({
  receiptNo: '',
  customerName: '',
  receiptAccount: '',
  handlerName: '',
  departmentName: '',
  creatorName: '',
  bookkeeperName: '',
  auditorName: '',
  status: undefined,
  remark: '',
  showRed: false,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

// ═══ 搜索参数（按明细） ═══
const detailSearch = reactive<any>({
  receiptNo: '',
  customerName: '',
  handlerName: '',
  departmentName: '',
  bookkeeperName: '',
  auditorName: '',
  status: undefined,
  tradeUnit: '',
  sourceHandler: '',
  settlementNo: '',
  showRed: false,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => { selectedRowKeys.value = keys },
}))

const showColumnConfig = ref(false)
const showPageConfig = ref(false)

// ═══ 页面配置 ═══
const PAGE_CONFIG_STORAGE_KEY = 'receipt-doc-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'receiptNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '结算单位', visible: true },
  { key: 'receiptAccount', label: '收款账户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'departmentName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'bookkeeperName', label: '记账人', visible: true },
  { key: 'auditorName', label: '审核人', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
]
const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'receiptNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '结算单位', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'departmentName', label: '部门', visible: true },
  { key: 'bookkeeperName', label: '记账人', visible: true },
  { key: 'auditorName', label: '审核人', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'tradeUnit', label: '往来单位', visible: true },
  { key: 'sourceHandler', label: '源单经手人', visible: true },
  { key: 'settlementNo', label: '结算单据编号', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
]
const DEFAULT_DOC_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]
const DEFAULT_DETAIL_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const docQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DOC_QUERY_FIELDS.map(f => ({ ...f })))
const detailQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DETAIL_QUERY_FIELDS.map(f => ({ ...f })))
const docFunctionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_DOC_FUNCTION_BUTTONS.map(f => ({ ...f })))
const detailFunctionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_DETAIL_FUNCTION_BUTTONS.map(f => ({ ...f })))

const activeQueryFields = computed(() => activeTab.value === 'doc' ? docQueryConfig.value : detailQueryConfig.value)
const activeFunctionButtons = computed(() => activeTab.value === 'doc' ? docFunctionButtonConfig.value : detailFunctionButtonConfig.value)
const activePageConfigStorageKey = computed(() => `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`)

const DEFAULT_QUERY_FIELDS_BY_TAB = { doc: DEFAULT_DOC_QUERY_FIELDS, detail: DEFAULT_DETAIL_QUERY_FIELDS }
const QUERY_CONFIG_BY_TAB = { doc: docQueryConfig, detail: detailQueryConfig }
const FUNCTION_CONFIG_BY_TAB = { doc: docFunctionButtonConfig, detail: detailFunctionButtonConfig }
const DEFAULT_FUNCTION_BY_TAB = { doc: DEFAULT_DOC_FUNCTION_BUTTONS, detail: DEFAULT_DETAIL_FUNCTION_BUTTONS }

function loadPageConfig() {
  try {
    ;['doc', 'detail'].forEach((tabKey) => {
      const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-' + tabKey)
      if (raw) {
        const parsed = JSON.parse(raw)
        const qCfg = tabKey === 'doc' ? docQueryConfig.value : detailQueryConfig.value
        const defaultQ = tabKey === 'doc' ? DEFAULT_DOC_QUERY_FIELDS : DEFAULT_DETAIL_QUERY_FIELDS
        if (parsed.queryFields) {
          const merged = defaultQ.map((df) => {
            const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
            return saved ? { ...df, ...saved } : { ...df }
          })
          qCfg.splice(0, qCfg.length, ...merged)
        }
      }
      const btnRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-buttons-' + tabKey)
      if (btnRaw) {
        const parsed = JSON.parse(btnRaw)
        const fCfg = tabKey === 'doc' ? docFunctionButtonConfig.value : detailFunctionButtonConfig.value
        const defaultF = tabKey === 'doc' ? DEFAULT_DOC_FUNCTION_BUTTONS : DEFAULT_DETAIL_FUNCTION_BUTTONS
        if (parsed.functionButtons) {
          const merged = defaultF.map((bf) => {
            const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
            return saved ? { ...bf, ...saved } : { ...bf }
          })
          fCfg.splice(0, fCfg.length, ...merged)
        }
      }
    })
  } catch { /* ignore */ }
}

function handlePageConfigChange(config: any) {
  const tabKey = activeTab.value === 'doc' ? '-doc' : '-detail'
  const defaultQuery = DEFAULT_QUERY_FIELDS_BY_TAB[activeTab.value]
  const queryCfg = QUERY_CONFIG_BY_TAB[activeTab.value]
  const funcCfg = FUNCTION_CONFIG_BY_TAB[activeTab.value]
  const defaultFunc = DEFAULT_FUNCTION_BY_TAB[activeTab.value]
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + tabKey, JSON.stringify({
    queryFields: config.queryFields || queryCfg.value || defaultQuery,
  }))
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + '-buttons-' + activeTab.value, JSON.stringify({
    functionButtons: config.functionButtons || funcCfg.value || defaultFunc,
  }))
  loadPageConfig()
}

// ═══ 列定义（按单据 26 列 / 收款明细 24 列） ═══
const docColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 150, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'receiptDate', key: 'receiptDate', width: 110, sortable: true },
  { title: '单据编号', field: 'receiptNo', key: 'receiptNo', width: 170, type: 'slot', slotName: 'receiptNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '结算单位编号', field: 'customerId', key: 'customerCode', width: 110, defaultHidden: true },
  { title: '结算单位', field: 'customerName', key: 'customerName', width: 150, sortable: true },
  { title: '优惠金额', field: 'discountAmount', key: 'discountAmount', width: 100, align: 'right' },
  { title: '金额', field: 'receiptAmount', key: 'receiptAmount', width: 120, align: 'right', sortable: true },
  { title: '收款账户1', field: 'receiptAccount1', key: 'receiptAccount1', width: 120 },
  { title: '收款金额1', field: 'receiptAmount1', key: 'receiptAmount1', width: 110, align: 'right' },
  { title: '收款账户2', field: 'receiptAccount2', key: 'receiptAccount2', width: 120 },
  { title: '收款金额2', field: 'receiptAmount2', key: 'receiptAmount2', width: 110, align: 'right' },
  { title: '收款账户3', field: 'receiptAccount3', key: 'receiptAccount3', width: 120, defaultHidden: true },
  { title: '收款金额3', field: 'receiptAmount3', key: 'receiptAmount3', width: 110, align: 'right', defaultHidden: true },
  { title: '收款账户4', field: 'receiptAccount4', key: 'receiptAccount4', width: 120, defaultHidden: true },
  { title: '收款金额4', field: 'receiptAmount4', key: 'receiptAmount4', width: 110, align: 'right', defaultHidden: true },
  { title: '使用预收款', field: 'usePrepaidAmount', key: 'usePrepaidAmount', width: 100, align: 'right' },
  { title: '经手人', field: 'salesPersonName', key: 'salesPersonName', width: 100, sortable: true },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 100, defaultHidden: true },
  { title: '记账人', field: 'verifiedBy', key: 'bookkeeper', width: 90, defaultHidden: true },
  { title: '制单人', field: 'createBy', key: 'creator', width: 90, defaultHidden: true },
  { title: '摘要', field: 'summary', key: 'summary', width: 140, defaultHidden: true },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80, defaultHidden: true },
  { title: '单据备注', field: 'remark', key: 'remark', width: 130, defaultHidden: true },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140, defaultHidden: true },
  { title: '记账时间', field: 'verifiedTime', key: 'verifiedTime', width: 140, defaultHidden: true },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' },
]

const detailColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 150, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'receiptDate', key: 'receiptDate', width: 110, sortable: true },
  { title: '单据编号', field: 'receiptNo', key: 'receiptNo', width: 170, type: 'slot', slotName: 'receiptNoCell', sortable: true },
  { title: '单据状态', field: 'receiptStatus', key: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '结算单位编号', field: 'customerCode', key: 'customerCode', width: 110, defaultHidden: true },
  { title: '结算单位', field: 'customerName', key: 'customerName', width: 150, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 100, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100, defaultHidden: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90, defaultHidden: true },
  { title: '审核人', field: 'auditorName', key: 'auditorName', width: 90, defaultHidden: true },
  { title: '结算单据', field: 'settlementNo', key: 'settlementNo', width: 150, sortable: true },
  { title: '往来单位', field: 'tradeUnit', key: 'tradeUnit', width: 150 },
  { title: '商品金额', field: 'productAmount', key: 'productAmount', width: 110, align: 'right', defaultHidden: true },
  { title: '优惠金额', field: 'discountAmount', key: 'discountAmount', width: 100, align: 'right', defaultHidden: true },
  { title: '其他费用', field: 'otherFee', key: 'otherFee', width: 100, align: 'right', defaultHidden: true },
  { title: '运费', field: 'freight', key: 'freight', width: 90, align: 'right', defaultHidden: true },
  { title: '本单金额', field: 'billAmount', key: 'billAmount', width: 120, align: 'right', sortable: true },
  { title: '已结金额', field: 'settledAmount', key: 'settledAmount', width: 110, align: 'right', defaultHidden: true },
  { title: '未结金额', field: 'unsettledAmount', key: 'unsettledAmount', width: 110, align: 'right', defaultHidden: true },
  { title: '本次优惠', field: 'currentDiscount', key: 'currentDiscount', width: 100, align: 'right', defaultHidden: true },
  { title: '本次结算', field: 'currentSettle', key: 'currentSettle', width: 100, align: 'right', defaultHidden: true },
  { title: '源单经手人', field: 'sourceHandlerName', key: 'sourceHandlerName', width: 110, defaultHidden: true },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80, defaultHidden: true },
  { title: '源单备注', field: 'remark', key: 'remark', width: 130, defaultHidden: true },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right', defaultHidden: true },
]

const docColumnDefs = computed(() => docColumns.map(c => ({ ...c })))
const detailColumnDefs = computed(() => detailColumns.map(c => ({ ...c })))

const {
  visibleColumns: docVisible,
  onSettingChange: onDocSettingChange,
  resetSettings: resetDocSettings,
  settingsColumns: docSettings,
} = useColumnConfig(docColumnDefs.value, 'receipt-doc-list-columns-doc')
const {
  visibleColumns: detailVisible,
  onSettingChange: onDetailSettingChange,
  resetSettings: resetDetailSettings,
  settingsColumns: detailSettings,
} = useColumnConfig(detailColumnDefs.value, 'receipt-doc-list-columns-detail')

const currentColumns = computed(() => activeTab.value === 'doc' ? docVisible.value : detailVisible.value)
const panelColumns = computed(() => activeTab.value === 'doc' ? docSettings.value : detailSettings.value)

function handleColumnConfigChange() {
  if (activeTab.value === 'doc') onDocSettingChange()
  else onDetailSettingChange()
}
function handleColumnConfigReset() {
  if (activeTab.value === 'doc') resetDocSettings()
  else resetDetailSettings()
}

// ═══ 状态映射 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '已拒绝', color: 'red' },
  4: { text: '待核销', color: 'purple' },
  5: { text: '核销中', color: 'cyan' },
  6: { text: '已核销', color: 'geekblue' },
  7: { text: '已完成', color: 'green' },
  8: { text: '已取消', color: 'default' },
}
function getStatusText(status: number): string {
  return STATUS_MAP[status]?.text || '未知'
}
function getStatusColor(status: number): string {
  return STATUS_MAP[status]?.color || 'default'
}

// ═══ 表格底部合计 ═══
const tableFooterColumns = computed(() => {
  if (activeTab.value !== 'doc') return []
  const totalAmt = tableData.value.reduce((s: number, r: any) => s + (r.receiptAmount || 0), 0)
  return [
    { label: '金额', value: totalAmt, type: 'currency' },
  ]
})

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    if (activeTab.value === 'detail') {
      await fetchDetailData()
    } else {
      await fetchDocData()
    }
  } catch (error: any) {
    console.warn('[收款单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

async function fetchDocData() {
  const params: any = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (docSearch.receiptNo) params.keyword = docSearch.receiptNo
  if (docSearch.customerName) params.customerName = docSearch.customerName
  if (docSearch.handlerName) params.handlerName = docSearch.handlerName
  if (docSearch.departmentName) params.departmentName = docSearch.departmentName
  if (docSearch.creatorName) params.creatorName = docSearch.creatorName
  if (docSearch.bookkeeperName) params.bookkeeperName = docSearch.bookkeeperName
  if (docSearch.remark) params.remark = docSearch.remark
  if (docSearch.status !== undefined && docSearch.status !== '') params.status = docSearch.status
  if (docSearch.startDate) params.startDate = docSearch.startDate
  if (docSearch.endDate) params.endDate = docSearch.endDate
  const res: any = await receiptApi.getPage(params)
  const body = (res as any)?.data ?? res
  const records = body?.records || []
  records.forEach((r: any) => {
    r.printCount = r.printCount ?? 0
    r.summary = r.summary || r.internalNote || ''
  })
  tableData.value = records
  pagination.total = Number(body?.total) || 0
}

async function fetchDetailData() {
  const params: any = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (detailSearch.receiptNo) params.keyword = detailSearch.receiptNo
  if (detailSearch.customerName) params.customerName = detailSearch.customerName
  if (detailSearch.tradeUnit) params.tradeUnit = detailSearch.tradeUnit
  if (detailSearch.sourceHandler) params.sourceHandler = detailSearch.sourceHandler
  if (detailSearch.settlementNo) params.settlementNo = detailSearch.settlementNo
  if (detailSearch.bookkeeperName) params.bookkeeperName = detailSearch.bookkeeperName
  if (detailSearch.status !== undefined && detailSearch.status !== '') params.status = detailSearch.status
  if (detailSearch.startDate) params.startDate = detailSearch.startDate
  if (detailSearch.endDate) params.endDate = detailSearch.endDate
  const res: any = await receiptApi.getPageDetail(params)
  const body = (res as any)?.data ?? res
  const records = body?.records || []
  records.forEach((r: any) => {
    r.status = r.receiptStatus
    r.printCount = r.printCount ?? 0
  })
  tableData.value = records
  pagination.total = Number(body?.total) || 0
}

// ═══ 事件处理 ═══
function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchData()
}

function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: any, end: any
  switch (key) {
    case 'yesterday': start = now.subtract(1, 'day'); end = now.subtract(1, 'day'); break
    case 'today': start = now; end = now; break
    case 'week': start = now.startOf('week'); end = now; break
    case 'lastWeek': start = now.subtract(7, 'day'); end = now; break
    case 'month': start = now.startOf('month'); end = now; break
    case 'lastMonth': start = now.subtract(1, 'month').startOf('month'); end = now.subtract(1, 'month').endOf('month'); break
    case 'last3Month': start = now.subtract(3, 'month'); end = now; break
    case 'year': start = now.startOf('year'); end = now; break
    default: start = now.subtract(7, 'day'); end = now
  }
  const startStr = start.format('YYYY-MM-DD')
  const endStr = end.format('YYYY-MM-DD')
  if (activeTab.value === 'doc') {
    docDateRange.value = [start, end]
    docSearch.startDate = startStr
    docSearch.endDate = endStr
  } else {
    detailDateRange.value = [start, end]
    detailSearch.startDate = startStr
    detailSearch.endDate = endStr
  }
  handleSearch()
}

function handleDocDateChange(dates: any) {
  if (dates && dates.length === 2) {
    docSearch.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    docSearch.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    docSearch.startDate = ''
    docSearch.endDate = ''
  }
}

function handleDetailDateChange(dates: any) {
  if (dates && dates.length === 2) {
    detailSearch.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    detailSearch.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    detailSearch.startDate = ''
    detailSearch.endDate = ''
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 操作 ═══
function handleAdd() {
  router.push('/finance/receipt-doc/form')
}
function handleView(record: any) {
  router.push(`/finance/receipt-doc/form?id=${record.id}`)
}
function handleEdit(record: any) {
  router.push(`/finance/receipt-doc/form?id=${record.id}`)
}

function handleConfirm(record: any) {
  Modal.confirm({
    title: '记账确认',
    content: `确认记账收款单 ${record.receiptNo} 吗？`,
    okText: '确认记账',
    cancelText: '取消',
    onOk: async () => {
      try {
        await receiptApi.submit(record.id)
        await receiptApi.approve(record.id)
        message.success('记账成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '记账失败')
      }
    },
  })
}

function handleCancel(record: any) {
  Modal.confirm({
    title: '取消单据',
    content: `确认取消收款单 ${record.receiptNo || ''} 吗？`,
    okText: '确认取消',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await receiptApi.cancel(record.id, '手动取消')
        message.success('已取消')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '取消失败')
      }
    },
  })
}

function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的收款单')
    return
  }
  router.push(`/finance/receipt-doc/form?id=${selectedRowKeys.value[0]}`)
}

const showPrintDialog = ref(false)
const printTemplate = ref('default')
function handleBatchPrint() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要批量打印的收款单')
    return
  }
  showPrintDialog.value = true
}
async function confirmPrint() {
  try {
    await receiptApi.batchPrint({ template: printTemplate.value, ids: selectedRowKeys.value })
    message.success(`已发送打印（模板: ${printTemplate.value}）`)
    showPrintDialog.value = false
  } catch {
    message.error('打印失败')
  }
}

async function handleExport() {
  try {
    const params: any = {}
    if (docSearch.receiptNo) params.keyword = docSearch.receiptNo
    if (docSearch.status !== undefined && docSearch.status !== '') params.status = docSearch.status
    const res: any = await receiptApi.getPage({ ...params, pageNum: 1, pageSize: 9999 })
    const body = (res as any)?.data ?? res
    const data = body?.records || []
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['单据编号', '单据日期', '结算单位', '单方类型', '金额', '优惠金额', '经手人', '状态']
    const rows = data.map((r: any) => [
      r.receiptNo, r.receiptDate, r.customerName, receiptTypeText(r.receiptType),
      formatAmount(r.receiptAmount), formatAmount(r.discountAmount), r.salesPersonName, getStatusText(r.status),
    ])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `收款单_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

function receiptTypeText(t: number): string {
  return t === 1 ? '销售收款' : t === 2 ? '预收' : t === 3 ? '其他' : String(t ?? '')
}

const handleError = (error: Error) => {
  console.error('[收款单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

function formatAmount(amount: number): string {
  if (amount === undefined || amount === null) return '0.00'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  loadPageConfig()
  setQuickDate('lastWeek')
  fetchData()
})
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
.quick-dates :deep(.ant-btn) { font-size: 13px; padding: 2px 8px; }
.quick-dates :deep(.ant-btn-primary) { color: #fff; background: #ff7a45; border-color: #ff7a45; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container > .search-grid { max-height: 90px; overflow: hidden; }
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
.search-action-group { display: flex; flex-wrap: nowrap; align-items: center; }
.search-action-group .search-field-item { width: auto; flex: 0 0 auto; margin-right: 4px; }
.search-action-group .search-field-item:last-child { margin-right: 0; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
