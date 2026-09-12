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
            <a-select v-model:value="queryScheme" style="width: 140px" size="small" placeholder="--查询方案--">
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
            <a-button type="link" size="small" style="padding: 0 4px"><PlusOutlined /></a-button>
          </div>
          <a-space :size="4" class="quick-dates">
            <a-button
              v-for="d in quickDates"
              :key="d.key"
              :type="quickDate === d.key ? 'primary' : 'link'"
              size="small"
              @click="setQuickDate(d.key)"
            >{{ d.label }}</a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置/新增/刷新/打印(F8)/导出（列配置走数据表表头齿轮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true"><SettingOutlined /></a-button>
            </a-tooltip>
            <a-button type="primary" size="small" @click="handleAdd"><PlusOutlined /> 新增</a-button>
            <a-button size="small" @click="fetchData"><ReloadOutlined /> 刷新</a-button>
            <a-button size="small" :disabled="selectedRowKeys.length === 0" @click="handlePrintF8"><PrinterOutlined /> 打印(F8)</a-button>
            <a-button size="small" @click="handleExport"><ExportOutlined /> 导出</a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域（按单据 tab） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div class="search-grid" ref="searchGridRef">
                <template v-if="activeTab === 'doc'">
                  <div class="search-field-item">
                    <a-range-picker v-model:value="docDateRange" size="small" style="width:100%" @change="handleDocDateChange" />
                  </div>
                  <div class="search-field-item"><a-input v-model:value="docSearch.docNo" placeholder="单据编号" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="docSearch.partnerName" placeholder="往来单位" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="docSearch.handlerName" placeholder="经手人" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="docSearch.deptName" placeholder="部门" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="docSearch.creatorName" placeholder="制单人" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="docSearch.bookkeeperName" placeholder="记账人" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="docSearch.auditorName" placeholder="审核人" allow-clear size="small" /></div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">单据状态</span>
                      <a-select v-model:value="docSearch.status" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option value="0">草稿</a-select-option>
                        <a-select-option value="1">已记账</a-select-option>
                        <a-select-option value="2">已取消</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-field-item"><a-input v-model:value="docSearch.payAccountName" placeholder="付款账户" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="docSearch.remark" placeholder="单据备注" allow-clear size="small" /></div>
                  <div class="search-action-group" ref="searchActionRef" :style="{ gridColumn: 'span ' + actionSpan }">
                    <div class="search-field-item search-action-item"><a-button type="primary" size="small" @click="handleSearch">查询</a-button></div>
                    <div class="search-field-item"><a-checkbox v-model:checked="docSearch.showRed">显示红冲</a-checkbox></div>
                  </div>
                </template>
                <template v-else>
                  <div class="search-field-item">
                    <a-range-picker v-model:value="detailDateRange" size="small" style="width:100%" @change="handleDetailDateChange" />
                  </div>
                  <div class="search-field-item"><a-input v-model:value="detailSearch.docNo" placeholder="单据编号" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="detailSearch.partnerName" placeholder="往来单位" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="detailSearch.handlerName" placeholder="经手人" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="detailSearch.deptName" placeholder="部门" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="detailSearch.creatorName" placeholder="制单人" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="detailSearch.bookkeeperName" placeholder="记账人" allow-clear size="small" /></div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">单据状态</span>
                      <a-select v-model:value="detailSearch.status" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option value="0">草稿</a-select-option>
                        <a-select-option value="1">已记账</a-select-option>
                        <a-select-option value="2">已取消</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-field-item"><a-input v-model:value="detailSearch.expenseName" placeholder="费用科目/名称" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="detailSearch.remark" placeholder="备注" allow-clear size="small" /></div>
                  <div class="search-action-group" ref="searchActionRef" :style="{ gridColumn: 'span ' + actionSpan }">
                    <div class="search-field-item search-action-item"><a-button type="primary" size="small" @click="handleSearch">查询</a-button></div>
                    <div class="search-field-item"><a-checkbox v-model:checked="detailSearch.showRed">显示红冲</a-checkbox></div>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="currentColumns"
              :data-source="tableData"
              :storage-key="activeTab === 'doc' ? 'expense-doc-table-columns-doc' : 'expense-doc-table-columns-detail'"
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
              <template #docNoCell="{ record }">
                <a-button type="link" size="small" @click="handleView(record)">{{ record.docNo }}</a-button>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <template #approvalStatusCell="{ record }">
                <a-tag :color="getApprovalColor(record.approvalStatus)">
                  {{ getApprovalText(record.approvalStatus, record.approvalLevel) }}
                </a-tag>
              </template>
              <template #settleStatusCell="{ record }">
                <a-tag :color="getSettleColor(record.status)">{{ getSettleText(record.status) }}</a-tag>
              </template>
              <template #totalAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalAmount) }}</span>
              </template>
              <template #amountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.amount ?? record.totalAmount) }}</span>
              </template>
              <template #payAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.payAmount) }}</span>
              </template>
              <template #payAmount2Cell="{ record }">
                <span class="currency-value">{{ formatAmount(record.payAmount2) }}</span>
              </template>
              <template #payAmount3Cell="{ record }">
                <span class="currency-value">{{ formatAmount(record.payAmount3) }}</span>
              </template>
              <template #payAmount4Cell="{ record }">
                <span class="currency-value">{{ formatAmount(record.payAmount4) }}</span>
              </template>
              <template #printCountCell="{ record }">
                <span class="currency-value">{{ record.printCount ?? 0 }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button v-if="String(record.status) === '0'" type="link" size="small" @click="handleEdit(record)">修改</a-button>
                  <a-button v-if="String(record.status) === '0'" type="link" size="small" @click="handleConfirm(record)">记账</a-button>
                  <a-button v-if="String(record.status) === '0'" type="link" size="small" @click="handleCancel(record)">取消</a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryFields"
      :function-buttons-config="activeFunctionButtons"
      :storage-key="activePageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, SettingOutlined,
  ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { expenseDocApi } from '@/api/finance'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'FinanceExpenseDocPage' })

const router = useRouter()
const userStore = useUserStore()

// ═══ Tab 配置（双 Tab：按单据 / 按明细） ═══
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
const searchGridRef = ref<HTMLElement | null>(null)
const searchActionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(searchActionRef, searchGridRef)

// ═══ 按单据搜索参数（12 查询条件） ═══
const docDateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])
const docSearch = reactive({
  docNo: '',
  partnerName: '',
  handlerName: '',
  deptName: '',
  creatorName: '',
  bookkeeperName: '',
  auditorName: '',
  payAccountName: '',
  remark: '',
  status: undefined as string | undefined,
  showRed: false,
})

// ═══ 按明细搜索参数（11 查询条件） ═══
const detailDateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])
const detailSearch = reactive({
  docNo: '',
  partnerName: '',
  handlerName: '',
  deptName: '',
  creatorName: '',
  bookkeeperName: '',
  expenseName: '',
  remark: '',
  status: undefined as string | undefined,
  showRed: false,
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

// ═══ 页面配置弹窗（列配置走数据表表头齿轮） ═══
const showPageConfig = ref(false)

// ═══ 页面配置（每 Tab 独立） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'docNo', label: '单据编号', visible: true },
  { key: 'partnerName', label: '往来单位', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'bookkeeperName', label: '记账人', visible: true },
  { key: 'auditorName', label: '审核人', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'payAccountName', label: '付款账户', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
]
const DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'docNo', label: '单据编号', visible: true },
  { key: 'partnerName', label: '往来单位', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'bookkeeperName', label: '记账人', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'expenseName', label: '费用科目', visible: true },
  { key: 'remark', label: '备注', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const docQueryConfig = ref<QueryFieldSetting[]>(DOC_QUERY_FIELDS.map(f => ({ ...f })))
const detailQueryConfig = ref<QueryFieldSetting[]>(DETAIL_QUERY_FIELDS.map(f => ({ ...f })))
const docFunctionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const detailFunctionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

const activeQueryFields = computed(() =>
  activeTab.value === 'doc' ? docQueryConfig.value : detailQueryConfig.value)
const activeFunctionButtons = computed(() =>
  activeTab.value === 'doc' ? docFunctionButtonConfig.value : detailFunctionButtonConfig.value)

function pageConfigStorageKeyFor(tab: string): string {
  return `expense-doc-page-config-${tab}`
}
const activePageConfigStorageKey = computed(() => pageConfigStorageKeyFor(activeTab.value))

function loadPageConfig() {
  ;['doc', 'detail'].forEach((tab) => {
    try {
      const raw = localStorage.getItem(pageConfigStorageKeyFor(tab))
      if (!raw) return
      const parsed = JSON.parse(raw) as PageConfigData
      const targetFields = tab === 'doc' ? docQueryConfig : detailQueryConfig
      const sourceFields = tab === 'doc' ? DOC_QUERY_FIELDS : DETAIL_QUERY_FIELDS
      if (parsed.queryFields) {
        targetFields.value = sourceFields.map(df => {
          const saved = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
      if (parsed.functionButtons) {
        const targetBtns = tab === 'doc' ? docFunctionButtonConfig : detailFunctionButtonConfig
        targetBtns.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons!.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
    } catch { /* ignore */ }
  })
}

function handlePageConfigChange(config: any) {
  const tab = activeTab.value
  const targetFields = tab === 'doc' ? docQueryConfig : detailQueryConfig
  const targetBtns = tab === 'doc' ? docFunctionButtonConfig : detailFunctionButtonConfig
  localStorage.setItem(pageConfigStorageKeyFor(tab), JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || targetBtns.value,
  }))
  if (config.queryFields) targetFields.value = config.queryFields
  if (config.functionButtons) targetBtns.value = config.functionButtons
  loadPageConfig()
}

// ═══ 按单据列（27 配置列，默认显示 13） ═══
const docAllColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 170, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'docDate', key: 'docDate', width: 110, sortable: true },
  { title: '单据编号', field: 'docNo', key: 'docNo', width: 170, type: 'slot', slotName: 'docNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '审批状态', field: 'approvalStatus', key: 'approvalStatus', width: 110, align: 'center', type: 'slot', slotName: 'approvalStatusCell' },
  { title: '结算状态', field: 'settleStatus', key: 'settleStatus', width: 100, align: 'center', type: 'slot', slotName: 'settleStatusCell' },
  { title: '往来单位', field: 'partnerName', key: 'partnerName', width: 180, sortable: true },
  { title: '往来单位编号', field: 'partnerCode', key: 'partnerCode', width: 120, defaultHidden: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100, defaultHidden: true },
  { title: '源单编号', field: 'sourceNo', key: 'sourceNo', width: 150, defaultHidden: false },
  { title: '金额', field: 'totalAmount', key: 'totalAmount', width: 120, align: 'right', type: 'slot', slotName: 'totalAmountCell', sortable: true },
  { title: '付款账户1', field: 'payAccountName', key: 'payAccountName', width: 150, sortable: true },
  { title: '付款金额1', field: 'payAmount', key: 'payAmount', width: 110, align: 'right', type: 'slot', slotName: 'payAmountCell' },
  { title: '付款账户2', field: 'payAccount2Name', key: 'payAccount2Name', width: 150 },
  { title: '付款金额2', field: 'payAmount2', key: 'payAmount2', width: 110, align: 'right', type: 'slot', slotName: 'payAmount2Cell' },
  { title: '付款账户3', field: 'payAccount3Name', key: 'payAccount3Name', width: 150, defaultHidden: true },
  { title: '付款金额3', field: 'payAmount3', key: 'payAmount3', width: 110, align: 'right', type: 'slot', slotName: 'payAmount3Cell', defaultHidden: true },
  { title: '付款账户4', field: 'payAccount4Name', key: 'payAccount4Name', width: 150, defaultHidden: true },
  { title: '付款金额4', field: 'payAmount4', key: 'payAmount4', width: 110, align: 'right', type: 'slot', slotName: 'payAmount4Cell', defaultHidden: true },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90, sortable: true, defaultHidden: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90, sortable: true, defaultHidden: true },
  { title: '审核人', field: 'auditorName', key: 'auditorName', width: 90, defaultHidden: true },
  { title: '摘要', field: 'summary', key: 'summary', width: 140, defaultHidden: true },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120, defaultHidden: true },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80, defaultHidden: true },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140, defaultHidden: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140, defaultHidden: true },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right', type: 'slot', slotName: 'printCountCell' },
]

// ═══ 按明细列（19 配置列，默认显示 8） ═══
const detailAllColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 170, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'docDate', key: 'docDate', width: 110, sortable: true },
  { title: '单据编号', field: 'docNo', key: 'docNo', width: 170, type: 'slot', slotName: 'docNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '结算状态', field: 'settleStatus', key: 'settleStatus', width: 100, align: 'center', type: 'slot', slotName: 'settleStatusCell', defaultHidden: true },
  { title: '往来单位', field: 'partnerName', key: 'partnerName', width: 180, sortable: true },
  { title: '往来单位编号', field: 'partnerCode', key: 'partnerCode', width: 120, defaultHidden: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100, defaultHidden: true },
  { title: '费用编号', field: 'expenseCode', key: 'expenseCode', width: 120, defaultHidden: true },
  { title: '费用名称', field: 'expenseName', key: 'expenseName', width: 160, sortable: true },
  { title: '金额', field: 'amount', key: 'amount', width: 120, align: 'right', type: 'slot', slotName: 'amountCell', sortable: true },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90, sortable: true, defaultHidden: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90, sortable: true, defaultHidden: true },
  { title: '摘要', field: 'summary', key: 'summary', width: 140, defaultHidden: true },
  { title: '明细备注', field: 'itemRemark', key: 'itemRemark', width: 140, defaultHidden: true },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120, defaultHidden: true },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140, defaultHidden: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140, defaultHidden: true },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right', type: 'slot', slotName: 'printCountCell' },
]

const currentColumns = computed(() =>
  activeTab.value === 'doc' ? docAllColumns : detailAllColumns)

// ═══ 状态映射 ═══
const STATUS_MAP: Record<string, { text: string; color: string }> = {
  '0': { text: '草稿', color: 'default' },
  '1': { text: '已记账', color: 'blue' },
  '2': { text: '已取消', color: 'red' },
}
function getStatusText(status: any): string {
  return STATUS_MAP[String(status)]?.text || status || '未知'
}
function getStatusColor(status: any): string {
  return STATUS_MAP[String(status)]?.color || 'default'
}
function getSettleText(status: any): string {
  return String(status) === '1' ? '已结算' : String(status) === '0' ? '未结算' : '-'
}
function getSettleColor(status: any): string {
  return String(status) === '1' ? 'green' : String(status) === '0' ? 'default' : 'default'
}

// ═══ 审批状态（0未提交 1审批中 2审批通过 3审批驳回） ═══
const APPROVAL_STATUS_MAP: Record<string, { text: string; color: string }> = {
  '0': { text: '未提交', color: 'default' },
  '1': { text: '审批中', color: 'orange' },
  '2': { text: '审批通过', color: 'green' },
  '3': { text: '审批驳回', color: 'red' },
}
function getApprovalText(status: any, level: any): string {
  const key = String(status ?? '0')
  if (key === '1') {
    const n = Number(level)
    const name = n === 1 ? '部门' : n === 2 ? '财务' : n === 3 ? '总经理' : `第${n || 0}级`
    return `${name}审批中`
  }
  return APPROVAL_STATUS_MAP[key]?.text || '未提交'
}
function getApprovalColor(status: any): string {
  return APPROVAL_STATUS_MAP[String(status ?? '0')]?.color || 'default'
}

// ═══ 表格底部合计（仅按单据 tab） ═══
const tableFooterColumns = computed(() => {
  if (activeTab.value !== 'doc') return []
  const totalAmount = tableData.value.reduce((s: number, r: any) => s + (Number(r.totalAmount) || 0), 0)
  const payAmount = tableData.value.reduce((s: number, r: any) => s + (Number(r.payAmount) || 0), 0)
  return [
    { key: 'totalAmount', value: totalAmount, highlight: true },
    { key: 'payAmount', value: payAmount, highlight: true },
  ]
})

// ═══ 数据加载 ═══
async function fetchData() {
  if (activeTab.value === 'detail') await fetchDetailData()
  else await fetchDocData()
}

async function fetchDocData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (docSearch.docNo) params.docNo = docSearch.docNo
    if (docSearch.partnerName) params.partnerName = docSearch.partnerName
    if (docSearch.handlerName) params.handlerName = docSearch.handlerName
    if (docSearch.deptName) params.deptName = docSearch.deptName
    if (docSearch.creatorName) params.creatorName = docSearch.creatorName
    if (docSearch.bookkeeperName) params.bookkeeperName = docSearch.bookkeeperName
    if (docSearch.payAccountName) params.payAccountName = docSearch.payAccountName
    if (docSearch.remark) params.remark = docSearch.remark
    if (docSearch.status !== undefined && docSearch.status !== '') params.status = docSearch.status
    if (docDateStart.value) params.dateStart = docDateStart.value
    if (docDateEnd.value) params.dateEnd = docDateEnd.value
    const res: any = await expenseDocApi.getPage(params)
    const body = res ?? {}
    tableData.value = body?.records || []
    pagination.total = Number(body?.total) || 0
  } catch (error: any) {
    console.warn('[费用单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

async function fetchDetailData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (detailSearch.docNo) params.docNo = detailSearch.docNo
    if (detailSearch.partnerName) params.partnerName = detailSearch.partnerName
    if (detailSearch.handlerName) params.handlerName = detailSearch.handlerName
    if (detailSearch.deptName) params.deptName = detailSearch.deptName
    if (detailSearch.creatorName) params.creatorName = detailSearch.creatorName
    if (detailSearch.bookkeeperName) params.bookkeeperName = detailSearch.bookkeeperName
    if (detailSearch.expenseName) params.expenseName = detailSearch.expenseName
    if (detailSearch.remark) params.remark = detailSearch.remark
    if (detailSearch.status !== undefined && detailSearch.status !== '') params.status = detailSearch.status
    if (detailDateStart.value) params.dateStart = detailDateStart.value
    if (detailDateEnd.value) params.dateEnd = detailDateEnd.value
    const res: any = await expenseDocApi.getPageDetail(params)
    const body = res ?? {}
    tableData.value = body?.records || []
    pagination.total = Number(body?.total) || 0
  } catch (error: any) {
    console.warn('[费用单] 获取明细失败', error)
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
  const s = start.format('YYYY-MM-DD')
  const e = end.format('YYYY-MM-DD')
  if (activeTab.value === 'doc') {
    docDateRange.value = [start, end]
    docDateStart.value = s
    docDateEnd.value = e
  } else {
    detailDateRange.value = [start, end]
    detailDateStart.value = s
    detailDateEnd.value = e
  }
  handleSearch()
}

// ═══ 每 Tab 独立日期范围 ═══
const docDateStart = ref(docDateRange.value?.[0]?.format('YYYY-MM-DD') || '')
const docDateEnd = ref(docDateRange.value?.[1]?.format('YYYY-MM-DD') || '')
const detailDateStart = ref(detailDateRange.value?.[0]?.format('YYYY-MM-DD') || '')
const detailDateEnd = ref(detailDateRange.value?.[1]?.format('YYYY-MM-DD') || '')

function handleDocDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    docDateStart.value = dates[0]?.format('YYYY-MM-DD') || ''
    docDateEnd.value = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    docDateStart.value = ''
    docDateEnd.value = ''
  }
}
function handleDetailDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    detailDateStart.value = dates[0]?.format('YYYY-MM-DD') || ''
    detailDateEnd.value = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    detailDateStart.value = ''
    detailDateEnd.value = ''
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
function currentOperator() {
  return { id: userStore?.userId || 0, name: userStore?.userInfo?.nickname || userStore?.userInfo?.username || '系统' }
}

function handleAdd() {
  router.push(`/finance/expense-doc/form`)
}
function handleView(record: any) {
  router.push(`/finance/expense-doc/form?id=${record.id}`)
}
function handleEdit(record: any) {
  router.push(`/finance/expense-doc/form?id=${record.id}`)
}
function handleConfirm(record: any) {
  Modal.confirm({
    title: '记账确认',
    content: `确定对费用单 ${record.docNo} 执行记账吗？记账后生成会计凭证。`,
    okText: '确认记账',
    cancelText: '取消',
    onOk: async () => {
      try {
        const u = currentOperator()
        await expenseDocApi.confirm(record.id, u.id, u.name)
        message.success('记账完成')
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
    content: `确定取消费用单 ${record.docNo} 吗？`,
    okText: '确认取消',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await expenseDocApi.cancel(record.id)
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
    message.warning('请先选择要打印的费用单')
    return
  }
  router.push(`/finance/expense-doc/form?id=${selectedRowKeys.value[0]}`)
}
async function handleExport() {
  try {
    const params: any = { pageNum: 1, pageSize: 9999 }
    if (activeTab.value === 'doc') {
      if (docSearch.docNo) params.docNo = docSearch.docNo
      if (docSearch.partnerName) params.partnerName = docSearch.partnerName
      const res: any = await expenseDocApi.getPage(params)
      const data = Array.isArray(res) ? res : (res?.records || [])
      if (data.length === 0) { message.warning('没有可导出的数据'); return }
      const headers = ['单据编号', '单据日期', '单据状态', '往来单位', '金额', '经手人', '付款账户1', '付款金额1']
      const rows = data.map((r: any) => [
        r.docNo, r.docDate, getStatusText(r.status), r.partnerName, r.totalAmount, r.handlerName, r.payAccountName, r.payAmount,
      ])
      const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
      downloadCsv(csv, `费用单_${dayjs().format('YYYYMMDD_HHmmss')}.csv`)
    } else {
      paramFillDetail(params)
      const res: any = await expenseDocApi.getPageDetail(params)
      const data = Array.isArray(res) ? res : (res?.records || [])
      if (data.length === 0) { message.warning('没有可导出的数据'); return }
      const headers = ['单据编号', '单据日期', '单据状态', '往来单位', '费用名称', '金额']
      const rows = data.map((r: any) => [
        r.docNo, r.docDate, getStatusText(r.status), r.partnerName, r.expenseName, r.amount,
      ])
      const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
      downloadCsv(csv, `费用明细_${dayjs().format('YYYYMMDD_HHmmss')}.csv`)
    }
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}
function paramFillDetail(params: any) {
  if (detailSearch.docNo) params.docNo = detailSearch.docNo
  if (detailSearch.partnerName) params.partnerName = detailSearch.partnerName
  if (detailSearch.expenseName) params.expenseName = detailSearch.expenseName
}
function downloadCsv(csv: string, filename: string) {
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

const handleError = (error: Error) => {
  console.error('[费用单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

function formatAmount(amount: any): string {
  if (amount === undefined || amount === null) return '0.00'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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
