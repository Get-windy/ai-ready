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

        <!-- ═══ 工具栏右侧：操作按钮（新增/刷新/打印/导出/配置） ═══ -->
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
                    <a-input v-model:value="searchParams.stockOutNo" placeholder="单据编号" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.partnerName" placeholder="往来单位" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.handlerName" placeholder="经手人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.deptName" placeholder="部门" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.creatorName" placeholder="制单人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.bookkeeperName" placeholder="记账人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.warehouseName" placeholder="仓库" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">单据状态</span>
                      <a-select v-model:value="searchParams.status" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="0">草稿</a-select-option>
                        <a-select-option :value="1">待审批</a-select-option>
                        <a-select-option :value="2">已审核</a-select-option>
                        <a-select-option :value="3">已出库</a-select-option>
                        <a-select-option :value="4">已拒绝</a-select-option>
                        <a-select-option :value="5">已取消</a-select-option>
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
                    <a-input v-model:value="searchParams.stockOutNo" placeholder="单据编号" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.productName" placeholder="商品" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.partnerName" placeholder="往来单位" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.handlerName" placeholder="经手人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.deptName" placeholder="部门" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.creatorName" placeholder="制单人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.bookkeeperName" placeholder="记账人" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.warehouseName" placeholder="仓库" allow-clear size="small" />
                  </div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">单据状态</span>
                      <a-select v-model:value="searchParams.status" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="0">草稿</a-select-option>
                        <a-select-option :value="1">待审批</a-select-option>
                        <a-select-option :value="2">已审核</a-select-option>
                        <a-select-option :value="3">已出库</a-select-option>
                        <a-select-option :value="4">已拒绝</a-select-option>
                        <a-select-option :value="5">已取消</a-select-option>
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
              <template #stockOutNoCell="{ record }">
                <a-button type="link" size="small" @click="handleView(record)">
                  {{ record.stockOutNo }}
                </a-button>
              </template>
              <!-- 单据状态 -->
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <!-- 金额 -->
              <template #totalAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalAmount) }}</span>
              </template>
              <!-- 出库数量 -->
              <template #totalQuantityCell="{ record }">
                <span class="currency-value">{{ formatQuantity(record.totalQuantity) }}</span>
              </template>
              <!-- 数量 -->
              <template #quantityCell="{ record }">
                <span class="currency-value">{{ formatQuantity(record.quantity) }}</span>
              </template>
              <!-- 明细金额 -->
              <template #amountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.amount) }}</span>
              </template>
              <!-- 操作列 -->
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" @click="handleEdit(record)">修改</a-button>
                  <a-button
                    v-if="record.status === 1"
                    type="link"
                    size="small"
                    @click="handleApprove(record)"
                  >审批</a-button>
                  <a-button
                    v-if="record.status === 2"
                    type="link"
                    size="small"
                    @click="handleComplete(record)"
                  >出库</a-button>
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
      :function-buttons-config="functionButtonConfig"
      :storage-key="activePageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
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
import { stockOutApi } from '@/api/erp'
import { useRouter } from 'vue-router'

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
  stockOutNo: '',
  partnerName: '',
  handlerName: '',
  deptName: '',
  creatorName: '',
  bookkeeperName: '',
  warehouseName: '',
  productName: '',
  remark: '',
  itemRemark: '',
  status: undefined as string | number | undefined,
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
const PAGE_CONFIG_STORAGE_KEY = 'stock-out-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// 默认查询字段 - 按单据tab（对标文档11个查询条件）
const DEFAULT_DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'stockOutNo', label: '单据编号', visible: true },
  { key: 'partnerName', label: '往来单位', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'bookkeeperName', label: '记账人', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
]

// 默认查询字段 - 按明细tab（对标文档13个查询条件）
const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'stockOutNo', label: '单据编号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'partnerName', label: '往来单位', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'bookkeeperName', label: '记账人', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'itemRemark', label: '明细备注', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
]

// 默认功能按钮（对标文档：新增/刷新/打印(F8)/导出/配置）
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const docQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DOC_QUERY_FIELDS.map(f => ({ ...f })))
const detailQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DETAIL_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

const activeQueryFields = computed(() =>
  activeTab.value === 'doc' ? docQueryConfig.value : detailQueryConfig.value
)
const activePageConfigStorageKey = computed(() =>
  `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`
)

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
    const btnRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-buttons')
    if (btnRaw) {
      const parsed = JSON.parse(btnRaw) as PageConfigData
      if (parsed.functionButtons) {
        functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
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
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + tabKey, JSON.stringify({
    queryFields: config.queryFields || [],
  }))
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + '-buttons', JSON.stringify({
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }))
  loadPageConfig()
}

// ═══ 列定义（必须在 useColumnConfig 之前声明） ═══

/** 按单据 Tab 列（对标文档20列） */
const docColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 180, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'stockOutDate', key: 'stockOutDate', width: 110, sortable: true },
  { title: '单据编号', field: 'stockOutNo', key: 'stockOutNo', width: 170, type: 'slot', slotName: 'stockOutNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120, sortable: true },
  { title: '往来单位编号', field: 'partnerCode', key: 'partnerCode', width: 110 },
  { title: '往来单位', field: 'partnerName', key: 'partnerName', width: 160, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100 },
  { title: '出库数量', field: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'right', type: 'slot', slotName: 'totalQuantityCell', sortable: true },
  { title: '本单金额', field: 'totalAmount', key: 'totalAmount', width: 120, align: 'right', type: 'slot', slotName: 'totalAmountCell', sortable: true },
  { title: '重量（kg）', field: 'totalWeight', key: 'totalWeight', width: 90, align: 'right' },
  { title: '体积（m³）', field: 'totalVolume', key: 'totalVolume', width: 90, align: 'right' },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 140 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90, sortable: true },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90, sortable: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' },
]

/** 按明细 Tab 列（对标文档45列） */
const detailColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 180, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'stockOutDate', key: 'stockOutDate', width: 110, sortable: true },
  { title: '单据编号', field: 'stockOutNo', key: 'stockOutNo', width: 170, type: 'slot', slotName: 'stockOutNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120, sortable: true },
  { title: '往来单位编号', field: 'partnerCode', key: 'partnerCode', width: 110 },
  { title: '往来单位', field: 'partnerName', key: 'partnerName', width: 160, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100 },
  { title: '商品名称', field: 'productName', key: 'productName', width: 200, sortable: true },
  { title: '货号', field: 'productCode', key: 'productCode', width: 100 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 110 },
  { title: '规格', field: 'productSpec', key: 'productSpec', width: 100 },
  { title: '型号', field: 'model', key: 'model', width: 80 },
  { title: '产地', field: 'origin', key: 'origin', width: 80 },
  { title: '品牌', field: 'brand', key: 'brand', width: 80 },
  { title: '表体自定义1(数字)', field: 'itemExtNum1', key: 'itemExtNum1', width: 120, align: 'right' },
  { title: '表体自定义2(数字)', field: 'itemExtNum2', key: 'itemExtNum2', width: 120, align: 'right' },
  { title: '表体自定义3(数字)', field: 'itemExtNum3', key: 'itemExtNum3', width: 120, align: 'right' },
  { title: '表体自定义4(文本)', field: 'itemExtText1', key: 'itemExtText1', width: 120 },
  { title: '表体自定义5(文本)', field: 'itemExtText2', key: 'itemExtText2', width: 120 },
  { title: '单位', field: 'productUnit', key: 'productUnit', width: 80 },
  { title: '出库单价', field: 'unitPrice', key: 'unitPrice', width: 90, align: 'right' },
  { title: '出库数量', field: 'quantity', key: 'quantity', width: 90, align: 'right', type: 'slot', slotName: 'quantityCell', sortable: true },
  { title: '批次条码', field: 'batchCode', key: 'batchCode', width: 120 },
  { title: '生产日期', field: 'productionDate', key: 'productionDate', width: 110 },
  { title: '到期日期', field: 'expiryDate', key: 'expiryDate', width: 110 },
  { title: '换算关系', field: 'conversionRelation', key: 'conversionRelation', width: 100 },
  { title: '换算结果', field: 'conversionResult', key: 'conversionResult', width: 100 },
  { title: '大包装', field: 'bigPack', key: 'bigPack', width: 70, align: 'right' },
  { title: '中包装', field: 'midPack', key: 'midPack', width: 70, align: 'right' },
  { title: '小包装', field: 'smallPack', key: 'smallPack', width: 70, align: 'right' },
  { title: '小单位', field: 'smallUnit', key: 'smallUnit', width: 70 },
  { title: '小单位数量', field: 'smallUnitQuantity', key: 'smallUnitQuantity', width: 90, align: 'right' },
  { title: '出库金额', field: 'amount', key: 'amount', width: 100, align: 'right', type: 'slot', slotName: 'amountCell', sortable: true },
  { title: '重量（kg）', field: 'weight', key: 'weight', width: 80, align: 'right' },
  { title: '体积（m³）', field: 'volume', key: 'volume', width: 80, align: 'right' },
  { title: '明细备注', field: 'remark', key: 'itemRemark', width: 120 },
  { title: '单据备注', field: 'docRemark', key: 'docRemark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 120 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90, sortable: true },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90, sortable: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140 },
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
} = useColumnConfig(docColumnDefs.value, 'stock-out-list-columns-doc')
const {
  visibleColumns: detailVisibleColumns,
  onSettingChange: onDetailSettingChange,
  resetSettings: resetDetailSettings,
  settingsColumns: detailSettingsColumns,
} = useColumnConfig(detailColumnDefs.value, 'stock-out-list-columns-detail')

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

// ═══ 状态映射（其他出库单：草稿/待审批/已审核/已出库/已拒绝/已取消） ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审核', color: 'blue' },
  3: { text: '已出库', color: 'green' },
  4: { text: '已拒绝', color: 'red' },
  5: { text: '已取消', color: 'default' },
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
    if (searchParams.stockOutNo) params.stockOutNo = searchParams.stockOutNo
    if (searchParams.partnerName) params.partnerName = searchParams.partnerName
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.deptName) params.deptName = searchParams.deptName
    if (searchParams.creatorName) params.creatorName = searchParams.creatorName
    if (searchParams.bookkeeperName) params.bookkeeperName = searchParams.bookkeeperName
    if (searchParams.warehouseName) params.warehouseName = searchParams.warehouseName
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.remark) params.remark = searchParams.remark
    if (searchParams.startDate) params.dateStart = searchParams.startDate
    if (searchParams.endDate) params.dateEnd = searchParams.endDate

    let res: any
    if (activeTab.value === 'detail') {
      if (searchParams.productName) params.productName = searchParams.productName
      if (searchParams.itemRemark) params.itemRemark = searchParams.itemRemark
      res = await stockOutApi.pageDetail(params)
    } else {
      res = await stockOutApi.getPage(params)
    }
    if (res) {
      // 兼容 Promise<PageResult> 与 request 解包后的 { records, total }
      const body = (res as any)?.data ?? res
      tableData.value = body?.records || []
      pagination.total = Number(body?.total) || 0
    }
  } catch (error: any) {
    console.warn('[其他出库单] 获取列表失败', error)
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
  router.push('/erp/stock-out/form')
}
function handleView(record: any) {
  router.push(`/erp/stock-out/form?id=${record.id}`)
}
function handleEdit(record: any) {
  router.push(`/erp/stock-out/form?id=${record.id}`)
}
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除出库单 ${record.stockOutNo} 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockOutApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}
function handleApprove(record: any) {
  Modal.confirm({
    title: '审批确认',
    content: `确定审批通过出库单 ${record.stockOutNo} 吗？`,
    okText: '审批通过',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockOutApi.approve(record.id)
        message.success('审批成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '审批失败')
      }
    },
  })
}
function handleComplete(record: any) {
  Modal.confirm({
    title: '出库确认',
    content: `确定完成出库单 ${record.stockOutNo} 的库存出库操作吗？`,
    okText: '确认出库',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockOutApi.complete(record.id)
        message.success('出库完成')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '出库失败')
      }
    },
  })
}
function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的出库单')
    return
  }
  router.push(`/erp/stock-out/form?id=${selectedRowKeys.value[0]}`)
}
async function handleExport() {
  try {
    const params: any = {}
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.stockOutNo) params.stockOutNo = searchParams.stockOutNo
    const res: any = await stockOutApi.getPage({ ...params, pageNum: 1, pageSize: 9999 })
    const body = (res as any)?.data ?? res
    const data = body?.records || []
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['单据编号', '单据日期', '仓库', '往来单位', '经手人', '出库数量', '本单金额', '状态']
    const rows = data.map((r: any) => [
      r.stockOutNo, r.stockOutDate, r.warehouseName, r.partnerName, r.handlerName,
      r.totalQuantity, r.totalAmount, getStatusText(r.status),
    ])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `其他出库单_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

const handleError = (error: Error) => {
  console.error('[其他出库单] 页面错误', error)
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
