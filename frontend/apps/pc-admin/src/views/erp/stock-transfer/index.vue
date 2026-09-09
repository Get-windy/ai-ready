<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- ═══ Tab栏 + 工具栏 ═══ -->
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
            </a-select>
            <a-button
              type="link"
              size="small"
              style="padding: 0 4px"
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
        </template>

        <!-- ═══ 工具栏右侧：操作按钮（新增/刷新/批量打印/打印(F8)/导出/配置） ═══ -->
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
                      v-model:value="dateRange"
                      size="small"
                      style="width: 100%"
                      @change="handleDateChange"
                    />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.transferNo" placeholder="单据编号" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.applicantName" placeholder="经手人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.departmentName" placeholder="部门" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.createByName" placeholder="制单人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.posterName" placeholder="记账人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.fromWarehouseName" placeholder="出库仓库" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.toWarehouseName" placeholder="入库仓库" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">单据状态</span>
                      <a-select v-model:value="searchParams.status" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="0">草稿</a-select-option>
                        <a-select-option :value="1">待审批</a-select-option>
                        <a-select-option :value="2">已审批</a-select-option>
                        <a-select-option :value="3">已拒绝</a-select-option>
                        <a-select-option :value="4">调拨中</a-select-option>
                        <a-select-option :value="5">已完成</a-select-option>
                        <a-select-option :value="6">已取消</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">调拨方式</span>
                      <a-select v-model:value="searchParams.transferType" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="1">同价调拨</a-select-option>
                        <a-select-option :value="2">异价调拨</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.remark" placeholder="单据备注" allow-clear size="small" />
                  </div>
                  <div class="search-action-group" ref="docActionRef" :style="{ gridColumn: 'span ' + docActionSpan }">
                    <div class="search-field-item search-action-item">
                      <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                    </div>
                    <div class="search-field-item">
                      <a-checkbox v-model:checked="searchParams.showRed">显示红冲</a-checkbox>
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
                      v-model:value="dateRange"
                      size="small"
                      style="width: 100%"
                      @change="handleDateChange"
                    />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.transferNo" placeholder="单据编号" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.productName" placeholder="商品" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.applicantName" placeholder="经手人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.departmentName" placeholder="部门" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.createByName" placeholder="制单人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.posterName" placeholder="记账人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.fromWarehouseName" placeholder="出库仓库" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.toWarehouseName" placeholder="入库仓库" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">单据状态</span>
                      <a-select v-model:value="searchParams.status" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="0">草稿</a-select-option>
                        <a-select-option :value="1">待审批</a-select-option>
                        <a-select-option :value="2">已审批</a-select-option>
                        <a-select-option :value="3">已拒绝</a-select-option>
                        <a-select-option :value="4">调拨中</a-select-option>
                        <a-select-option :value="5">已完成</a-select-option>
                        <a-select-option :value="6">已取消</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.remark" placeholder="单据备注" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.itemRemark" placeholder="明细备注" allow-clear size="small" />
                  </div>
                  <div class="search-action-group" ref="detailActionRef" :style="{ gridColumn: 'span ' + detailActionSpan }">
                    <div class="search-field-item search-action-item">
                      <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                    </div>
                    <div class="search-field-item">
                      <a-checkbox v-model:checked="searchParams.showRed">显示红冲</a-checkbox>
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
              :selectable="true"
              :row-selection="rowSelection"
              :summary-columns="tableFooterColumns"
              row-key="id"
              @page-change="handlePageChange"
            >
              <!-- 单据编号 -->
              <template #transferNoCell="{ record }">
                <a-button type="link" size="small" @click="handleView(record)">
                  {{ record.transferNo }}
                </a-button>
              </template>
              <!-- 单据状态 -->
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <!-- 调拨方式 -->
              <template #transferTypeCell="{ record }">
                {{ getTransferTypeText(record.transferType) }}
              </template>
              <!-- 金额 -->
              <template #totalAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalAmount !== undefined ? record.totalAmount : record.transferAmount) }}</span>
              </template>
              <template #totalCostAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalCostAmount !== undefined ? record.totalCostAmount : record.costAmount) }}</span>
              </template>
              <template #totalTransferDiffCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalTransferDiff !== undefined ? record.totalTransferDiff : record.transferDiff) }}</span>
              </template>
              <!-- 数量 -->
              <template #totalQuantityCell="{ record }">
                <span class="currency-value">{{ formatQuantity(record.totalQuantity !== undefined ? record.totalQuantity : record.quantity) }}</span>
              </template>
              <template #quantityCell="{ record }">
                <span class="currency-value">{{ formatQuantity(record.quantity) }}</span>
              </template>
              <template #costAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.costAmount) }}</span>
              </template>
              <template #amountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.transferAmount) }}</span>
              </template>
              <template #diffCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.transferDiff) }}</span>
              </template>
              <!-- 操作列 -->
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" @click="handleEdit(record)">修改</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" @click="handleSubmit(record)">记帐</a-button>
                  <a-button v-if="record.status === 2" type="link" size="small" @click="handleExecute(record)">执行</a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >删除</a-button>
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
    <a-modal
      v-model:open="showPrintDialog"
      title="批量打印"
      :width="400"
      @ok="confirmPrint"
    >
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
import type { Dayjs } from 'dayjs'
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
import { stockTransferApi } from '@/api/erp'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

// ═══ Tab 配置 ═══
const tabs = [
  { key: 'doc', label: '按单据' },
  { key: 'detail', label: '按明细' },
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

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])

const docGridRef = ref<HTMLElement | null>(null)
const docActionRef = ref<HTMLElement | null>(null)
const detailGridRef = ref<HTMLElement | null>(null)
const detailActionRef = ref<HTMLElement | null>(null)
const { span: docActionSpan } = useAutoGridSpan(docActionRef, docGridRef)
const { span: detailActionSpan } = useAutoGridSpan(detailActionRef, detailGridRef)

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数 ═══
const searchParams = reactive({
  transferNo: '',
  sourceBillNo: '',
  applicantName: '',
  departmentName: '',
  createByName: '',
  posterName: '',
  fromWarehouseName: '',
  toWarehouseName: '',
  productName: '',
  remark: '',
  itemRemark: '',
  status: undefined as string | number | undefined,
  transferType: undefined as string | number | undefined,
  showRed: false,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

// ═══ 多选状态 ═══
const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => { selectedRowKeys.value = keys },
}))

// ═══ 列配置/页面配置弹窗 ═══
const showColumnConfig = ref(false)
const showPageConfig = ref(false)

// ═══ 页面配置（查询条件显隐、功能按钮） ═══
const PAGE_CONFIG_STORAGE_KEY = 'stock-transfer-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// 默认查询字段 - 按单据tab（对标文档12个查询条件）
const DEFAULT_DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'transferNo', label: '单据编号', visible: true },
  { key: 'applicantName', label: '经手人', visible: true },
  { key: 'departmentName', label: '部门', visible: true },
  { key: 'createByName', label: '制单人', visible: true },
  { key: 'posterName', label: '记账人', visible: true },
  { key: 'fromWarehouseName', label: '出库仓库', visible: true },
  { key: 'toWarehouseName', label: '入库仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'transferType', label: '调拨方式', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
]

// 默认查询字段 - 按明细tab（对标文档14个查询条件）
const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'transferNo', label: '单据编号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'applicantName', label: '经手人', visible: true },
  { key: 'departmentName', label: '部门', visible: true },
  { key: 'createByName', label: '制单人', visible: true },
  { key: 'posterName', label: '记账人', visible: true },
  { key: 'fromWarehouseName', label: '出库仓库', visible: true },
  { key: 'toWarehouseName', label: '入库仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'transferType', label: '调拨方式', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'itemRemark', label: '明细备注', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
]

// 默认功能按钮（按单据：新增/刷新/批量打印/打印(F8)/导出/配置）
const DEFAULT_DOC_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]
// 默认功能按钮（按明细：新增/刷新/打印(F8)/导出/配置，无批量打印）
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

const activeQueryFields = computed(() =>
  activeTab.value === 'doc' ? docQueryConfig.value : detailQueryConfig.value
)
const activeFunctionButtons = computed(() =>
  activeTab.value === 'doc' ? docFunctionButtonConfig.value : detailFunctionButtonConfig.value
)
const activePageConfigStorageKey = computed(() =>
  `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`
)

const DEFAULT_QUERY_FIELDS_BY_TAB = { doc: DEFAULT_DOC_QUERY_FIELDS, detail: DEFAULT_DETAIL_QUERY_FIELDS }
const QUERY_CONFIG_BY_TAB = { doc: docQueryConfig, detail: detailQueryConfig }
const FUNCTION_CONFIG_BY_TAB = { doc: docFunctionButtonConfig, detail: detailFunctionButtonConfig }
const DEFAULT_FUNCTION_BYTAB = { doc: DEFAULT_DOC_FUNCTION_BUTTONS, detail: DEFAULT_DETAIL_FUNCTION_BUTTONS }

function loadPageConfig() {
  try {
    const docRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-doc')
    if (docRaw) {
      const parsed = JSON.parse(docRaw) as PageConfigData
      if (parsed.queryFields) {
        docQueryConfig.value = DEFAULT_DOC_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
    }
    const detailRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-detail')
    if (detailRaw) {
      const parsed = JSON.parse(detailRaw) as PageConfigData
      if (parsed.queryFields) {
        detailQueryConfig.value = DEFAULT_DETAIL_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
    }
    const docBtnRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-buttons-doc')
    if (docBtnRaw) {
      const parsed = JSON.parse(docBtnRaw) as PageConfigData
      if (parsed.functionButtons) {
        docFunctionButtonConfig.value = DEFAULT_DOC_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons!.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
    }
    const detailBtnRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-buttons-detail')
    if (detailBtnRaw) {
      const parsed = JSON.parse(detailBtnRaw) as PageConfigData
      if (parsed.functionButtons) {
        detailFunctionButtonConfig.value = DEFAULT_DETAIL_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons!.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
    }
  } catch {
    // ignore
  }
}

function handlePageConfigChange(config: any) {
  const tabKey = activeTab.value === 'doc' ? '-doc' : '-detail'
  const defaultQuery = DEFAULT_QUERY_FIELDS_BY_TAB[activeTab.value]
  const queryCfg = QUERY_CONFIG_BY_TAB[activeTab.value]
  const funcCfg = FUNCTION_CONFIG_BY_TAB[activeTab.value]
  const defaultFunc = DEFAULT_FUNCTION_BYTAB[activeTab.value]
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + tabKey, JSON.stringify({
    queryFields: config.queryFields || queryCfg.value || defaultQuery,
  }))
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + '-buttons-' + activeTab.value, JSON.stringify({
    functionButtons: config.functionButtons || funcCfg.value || defaultFunc,
  }))
  loadPageConfig()
}

// ═══ 列定义（必须在 useColumnConfig 之前声明） ═══

/** 按单据 Tab 列（对标文档23列 + 序号/操作） */
const docColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 170, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'billDate', key: 'billDate', width: 110, sortable: true },
  { title: '单据编号', field: 'transferNo', key: 'transferNo', width: 170, type: 'slot', slotName: 'transferNoCell', sortable: true },
  { title: '来源订单', field: 'sourceBillNo', key: 'sourceBillNo', width: 150 },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '调拨方式', field: 'transferType', key: 'transferType', width: 100, align: 'center', type: 'slot', slotName: 'transferTypeCell' },
  { title: '出库仓库', field: 'fromWarehouseName', key: 'fromWarehouseName', width: 130, sortable: true },
  { title: '入库仓库', field: 'toWarehouseName', key: 'toWarehouseName', width: 130, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 100, sortable: true },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 100 },
  { title: '调拨数量', field: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right', type: 'slot', slotName: 'totalQuantityCell', sortable: true },
  { title: '成本金额', field: 'totalCostAmount', key: 'totalCostAmount', width: 110, align: 'right', type: 'slot', slotName: 'totalCostAmountCell' },
  { title: '调拨金额', field: 'totalAmount', key: 'totalAmount', width: 110, align: 'right', type: 'slot', slotName: 'totalAmountCell', sortable: true },
  { title: '调拨差额', field: 'totalTransferDiff', key: 'totalTransferDiff', width: 110, align: 'right', type: 'slot', slotName: 'totalTransferDiffCell' },
  { title: '重量（kg）', field: 'totalWeight', key: 'totalWeight', width: 90, align: 'right' },
  { title: '体积（m³）', field: 'totalVolume', key: 'totalVolume', width: 90, align: 'right' },
  { title: '单据备注', field: 'remark', key: 'remark', width: 130 },
  { title: '摘要', field: 'summary', key: 'summary', width: 150 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80 },
  { title: '记账人', field: 'posterName', key: 'posterName', width: 90, sortable: true },
  { title: '制单人', field: 'createByName', key: 'createByName', width: 90, sortable: true },
  { title: '记账时间', field: 'posterTime', key: 'posterTime', width: 140 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' },
]

/** 按明细 Tab 列（对标文档50列 + 序号/操作） */
const detailColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 170, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'billDate', key: 'billDate', width: 110, sortable: true },
  { title: '单据编号', field: 'transferNo', key: 'transferNo', width: 170, type: 'slot', slotName: 'transferNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '调拨方式', field: 'transferType', key: 'transferType', width: 100, align: 'center', type: 'slot', slotName: 'transferTypeCell' },
  { title: '出库仓库', field: 'fromWarehouseName', key: 'fromWarehouseName', width: 130, sortable: true },
  { title: '入库仓库', field: 'toWarehouseName', key: 'toWarehouseName', width: 130, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 100, sortable: true },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 100 },
  { title: '商品名称', field: 'productName', key: 'productName', width: 200, sortable: true },
  { title: '货号', field: 'productCode', key: 'productCode', width: 100 },
  { title: '口味', field: 'model', key: 'flavor', width: 80 },
  { title: '型号', field: 'model', key: 'model', width: 80 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 110 },
  { title: '规格', field: 'productSpec', key: 'productSpec', width: 100 },
  { title: '产地', field: 'origin', key: 'origin', width: 80 },
  { title: '品牌', field: 'brand', key: 'brand', width: 80 },
  { title: '表体自定义1(数字)', field: 'extNum1', key: 'extNum1', width: 120, align: 'right' },
  { title: '表体自定义2(数字)', field: 'extNum2', key: 'extNum2', width: 120, align: 'right' },
  { title: '表体自定义3(数字)', field: 'extNum3', key: 'extNum3', width: 120, align: 'right' },
  { title: '表体自定义4(文本)', field: 'extText1', key: 'extText1', width: 120 },
  { title: '表体自定义5(文本)', field: 'extText2', key: 'extText2', width: 120 },
  { title: '单位', field: 'productUnit', key: 'productUnit', width: 80 },
  { title: '调拨数量', field: 'quantity', key: 'quantity', width: 100, align: 'right', type: 'slot', slotName: 'quantityCell', sortable: true },
  { title: '批次条码', field: 'batchCode', key: 'batchCode', width: 120 },
  { title: '生产日期', field: 'productionDate', key: 'productionDate', width: 110 },
  { title: '到期日期', field: 'validityDate', key: 'validityDate', width: 110 },
  { title: '换算关系', field: 'conversionRelation', key: 'conversionRelation', width: 100 },
  { title: '换算结果', field: 'conversionResult', key: 'conversionResult', width: 100 },
  { title: '大包装', field: 'bigPack', key: 'bigPack', width: 70, align: 'right' },
  { title: '中包装', field: 'midPack', key: 'midPack', width: 70, align: 'right' },
  { title: '小包装', field: 'smallPack', key: 'smallPack', width: 70, align: 'right' },
  { title: '小单位', field: 'smallUnit', key: 'smallUnit', width: 70 },
  { title: '小单位数量', field: 'smallUnitQuantity', key: 'smallUnitQuantity', width: 90, align: 'right' },
  { title: '成本单价', field: 'unitCost', key: 'unitCost', width: 100, align: 'right' },
  { title: '成本金额', field: 'costAmount', key: 'costAmount', width: 110, align: 'right', type: 'slot', slotName: 'costAmountCell', sortable: true },
  { title: '调拨单价', field: 'transferPrice', key: 'transferPrice', width: 100, align: 'right' },
  { title: '调拨金额', field: 'transferAmount', key: 'transferAmount', width: 110, align: 'right', type: 'slot', slotName: 'amountCell', sortable: true },
  { title: '调拨差额', field: 'transferDiff', key: 'transferDiff', width: 100, align: 'right', type: 'slot', slotName: 'diffCell' },
  { title: '重量（kg）', field: 'weight', key: 'weight', width: 80, align: 'right' },
  { title: '体积（m³）', field: 'volume', key: 'volume', width: 80, align: 'right' },
  { title: '明细备注', field: 'remark', key: 'itemRemark', width: 120 },
  { title: '单据备注', field: 'docRemark', key: 'docRemark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 120 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80 },
  { title: '记账人', field: 'posterName', key: 'posterName', width: 90, sortable: true },
  { title: '制单人', field: 'createByName', key: 'createByName', width: 90, sortable: true },
  { title: '记账时间', field: 'posterTime', key: 'posterTime', width: 140 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' },
]

// ═══ 列配置 ═══
const docColumnDefs = computed(() => docColumns.map(col => ({ ...col })))
const detailColumnDefs = computed(() => detailColumns.map(col => ({ ...col })))
const {
  visibleColumns: docVisibleColumns,
  onSettingChange: onDocSettingChange,
  resetSettings: resetDocSettings,
  settingsColumns: docSettingsColumns,
} = useColumnConfig(docColumnDefs.value, 'stock-transfer-list-columns-doc')
const {
  visibleColumns: detailVisibleColumns,
  onSettingChange: onDetailSettingChange,
  resetSettings: resetDetailSettings,
  settingsColumns: detailSettingsColumns,
} = useColumnConfig(detailColumnDefs.value, 'stock-transfer-list-columns-detail')

const currentColumns = computed(() =>
  activeTab.value === 'doc' ? docVisibleColumns.value : detailVisibleColumns.value
)
const panelColumns = computed(() =>
  activeTab.value === 'doc' ? docSettingsColumns.value : detailSettingsColumns.value
)

function handleColumnConfigChange() {
  if (activeTab.value === 'doc') onDocSettingChange()
  else onDetailSettingChange()
}
function handleColumnConfigReset() {
  if (activeTab.value === 'doc') resetDocSettings()
  else resetDetailSettings()
}

// ═══ 状态映射（调拨单：草稿/待审批/已审批/已拒绝/调拨中/已完成/已取消） ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '已拒绝', color: 'red' },
  4: { text: '调拨中', color: 'processing' },
  5: { text: '已完成', color: 'success' },
  6: { text: '已取消', color: 'default' },
}
function getStatusText(status: number): string {
  return STATUS_MAP[status]?.text || '未知'
}
function getStatusColor(status: number): string {
  return STATUS_MAP[status]?.color || 'default'
}
function getTransferTypeText(t: number): string {
  return t === 2 ? '异价调拨' : '同价调拨'
}

// ═══ 表格底部合计 ═══
const tableFooterColumns = computed(() => {
  if (activeTab.value !== 'doc') return []
  const totalQty = tableData.value.reduce((s: number, r: any) => s + (r.totalQuantity || 0), 0)
  const totalAmt = tableData.value.reduce((s: number, r: any) => s + (r.totalAmount || 0), 0)
  return [
    { key: 'totalQuantity', value: totalQty, highlight: true },
    { key: 'totalAmount', value: totalAmt, highlight: true },
  ]
})

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchParams.transferNo) params.transferNo = searchParams.transferNo
    if (searchParams.applicantName) params.applicantName = searchParams.applicantName
    if (searchParams.departmentName) params.departmentName = searchParams.departmentName
    if (searchParams.createByName) params.createByName = searchParams.createByName
    if (searchParams.posterName) params.posterName = searchParams.posterName
    if (searchParams.fromWarehouseName) params.fromWarehouseName = searchParams.fromWarehouseName
    if (searchParams.toWarehouseName) params.toWarehouseName = searchParams.toWarehouseName
    if (searchParams.remark) params.remark = searchParams.remark
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.transferType !== undefined && searchParams.transferType !== '') params.transferType = searchParams.transferType
    if (searchParams.startDate) params.startDate = searchParams.startDate
    if (searchParams.endDate) params.endDate = searchParams.endDate

    let res: any
    if (activeTab.value === 'detail') {
      if (searchParams.productName) params.productName = searchParams.productName
      if (searchParams.itemRemark) params.itemRemark = searchParams.itemRemark
      res = await stockTransferApi.pageDetail(params)
    } else {
      res = await stockTransferApi.getPage(params)
    }
    if (res) {
      // 兼容 Promise<PageResult> 与 request 解包后的 { records, total }
      const body = (res as any)?.data ?? res
      tableData.value = body?.records || []
      pagination.total = Number(body?.total) || 0
    }
  } catch (error: any) {
    console.warn('[调拨单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
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
  let start: Dayjs, end: Dayjs
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
  dateRange.value = [start, end]
  searchParams.startDate = start.format('YYYY-MM-DD')
  searchParams.endDate = end.format('YYYY-MM-DD')
  handleSearch()
}

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
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
  router.push('/erp/stock-transfer/form')
}
function handleView(record: any) {
  router.push(`/erp/stock-transfer/form?id=${record.id}`)
}
function handleEdit(record: any) {
  router.push(`/erp/stock-transfer/form?id=${record.id}`)
}
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除调拨单 ${record.transferNo} 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockTransferApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}
function handleSubmit(record: any) {
  Modal.confirm({
    title: '记帐确认',
    content: `确认记帐调拨单 ${record.transferNo} 吗？记帐后两仓库存同步变动。`,
    okText: '确认记帐',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockTransferApi.submit(record.id)
        await stockTransferApi.approve(record.id)
        await stockTransferApi.execute(record.id)
        message.success('记帐成功，库存已更新')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '记帐失败')
      }
    },
  })
}
function handleExecute(record: any) {
  Modal.confirm({
    title: '执行调拨',
    content: `确定执行调拨单 ${record.transferNo} 吗？执行后两仓库存同步变动。`,
    okText: '确认执行',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockTransferApi.execute(record.id)
        message.success('调拨执行成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '执行失败')
      }
    },
  })
}
function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的调拨单')
    return
  }
  router.push(`/erp/stock-transfer/form?id=${selectedRowKeys.value[0]}`)
}

const showPrintDialog = ref(false)
const printTemplate = ref('default')
function handleBatchPrint() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要批量打印的调拨单')
    return
  }
  printTemplate.value = localStorage.getItem('stock-transfer-last-print-template') || 'default'
  showPrintDialog.value = true
}
async function confirmPrint() {
  try {
    await requestPostBatchPrint(printTemplate.value, selectedRowKeys.value)
    localStorage.setItem('stock-transfer-last-print-template', printTemplate.value)
    message.success(`已发送打印（模板: ${printTemplate.value}）`)
    showPrintDialog.value = false
  } catch {
    message.error('打印失败')
  }
}
async function requestPostBatchPrint(template: string, ids: any[]) {
  return request.post('/erp/stock/transfer/batch-print', { template, ids })
}

async function handleExport() {
  try {
    const params: any = {}
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.transferNo) params.transferNo = searchParams.transferNo
    const res: any = await stockTransferApi.getPage({ ...params, pageNum: 1, pageSize: 9999 })
    const body = (res as any)?.data ?? res
    const data = body?.records || []
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['单据编号', '单据日期', '出库仓库', '入库仓库', '经手人', '调拨方式', '调拨数量', '调拨金额', '状态']
    const rows = data.map((r: any) => [
      r.transferNo, r.billDate, r.fromWarehouseName, r.toWarehouseName, r.handlerName || r.applicantName,
      getTransferTypeText(r.transferType), r.totalQuantity, r.totalAmount, getStatusText(r.status),
    ])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `调拨单_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

const handleError = (error: Error) => {
  console.error('[调拨单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

function formatAmount(amount: number): string {
  if (amount === undefined || amount === null) return '0.00'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatQuantity(qty: number): string {
  if (qty === undefined || qty === null) return '0'
  return qty.toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}

// ═══ 初始化 ═══
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
.search-container > .search-grid { max-height: 80px; overflow: hidden; }
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
