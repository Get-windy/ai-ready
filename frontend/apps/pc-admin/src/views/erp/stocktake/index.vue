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

        <!-- ═══ 工具栏右侧：操作按钮（列配置走数据表表头齿轮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true"><SettingOutlined /></a-button>
            </a-tooltip>
            <template v-if="activeTab === 'history'">
              <a-button type="primary" size="small" v-if="fnEnabled('add')" @click="handleAdd"><PlusOutlined /> 新增</a-button>
              <a-button size="small" v-if="fnEnabled('refresh')" @click="fetchData"><ReloadOutlined /> 刷新</a-button>
              <a-button size="small" v-if="fnEnabled('summary')" @click="handleSummary"><DashboardOutlined /> 汇总盘点</a-button>
              <a-button size="small" v-if="fnEnabled('printF8')" @click="handlePrintF8"><PrinterOutlined /> 打印(F8)</a-button>
              <a-button size="small" v-if="fnEnabled('export')" @click="handleExport"><ExportOutlined /> 导出</a-button>
              <a-button size="small" v-if="fnEnabled('config')" @click="showPageConfig = true"><SettingOutlined /> 配置</a-button>
            </template>
            <template v-else>
              <a-button size="small" v-if="fnEnabled('refresh')" @click="fetchData"><ReloadOutlined /> 刷新</a-button>
              <a-button type="primary" size="small" v-if="fnEnabled('allCheck')" @click="handleAllCheck"><AppstoreAddOutlined /> 全部盘点</a-button>
              <a-button size="small" v-if="fnEnabled('batchCheck')" @click="handleBatchCheck"><CheckSquareOutlined /> 批量盘点</a-button>
              <a-button size="small" v-if="fnEnabled('printF8')" @click="handlePrintF8"><PrinterOutlined /> 打印(F8)</a-button>
              <a-button size="small" v-if="fnEnabled('export')" @click="handleExport"><ExportOutlined /> 导出</a-button>
              <a-button size="small" v-if="fnEnabled('config')" @click="showPageConfig = true"><SettingOutlined /> 配置</a-button>
            </template>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <template v-if="activeTab === 'history'">
              <div class="search-container">
                <div class="search-grid" ref="docGridRef">
                  <div class="search-field-item"><a-range-picker v-model:value="dateRange" size="small" style="width:100%" @change="handleDateChange" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.stockTakeNo" placeholder="单据编号" allow-clear size="small" /></div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">单据状态</span>
                      <a-select v-model:value="searchParams.status" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="0">草稿</a-select-option>
                        <a-select-option :value="1">已保存</a-select-option>
                        <a-select-option :value="2">已盘点</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">盘点方式</span>
                      <a-select v-model:value="searchParams.checkMethod" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="1">手工录入</a-select-option>
                        <a-select-option :value="2">扫码盘点</a-select-option>
                        <a-select-option :value="3">快速盘点</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.regionName" placeholder="区域" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.warehouseName" placeholder="仓库" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.handlerName" placeholder="经手人" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.deptName" placeholder="部门" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.creatorName" placeholder="制单人" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.bookkeeperName" placeholder="记账人" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.linkedBillNo" placeholder="关联单据编号" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.remark" placeholder="单据备注" allow-clear size="small" /></div>
                  <div class="search-action-group" ref="docActionRef" :style="{ gridColumn: 'span ' + docActionSpan }">
                    <div class="search-field-item search-action-item">
                      <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <template v-else>
              <div class="search-container">
                <div class="search-grid" ref="uncheckedGridRef">
                  <div class="search-field-item"><a-range-picker v-model:value="dateRange" size="small" style="width:100%" @change="handleDateChange" /></div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">筛选条件</span>
                      <a-select v-model:value="searchParams.filterCondition" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option value="unchecked">未盘</a-select-option>
                        <a-select-option value="checked">已盘</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-field-item">
                    <a-select
                      v-model:value="searchParams.checkWarehouseId"
                      placeholder="仓库"
                      size="small"
                      allow-clear
                      show-search
                      :filter-option="(input, option:any) => option.label.toLowerCase().includes(input.toLowerCase())"
                      :options="warehouseOptions"
                    />
                  </div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.location" placeholder="货位" allow-clear size="small" /></div>
                  <div class="search-field-item"><a-input v-model:value="searchParams.productKeyword" placeholder="商品" allow-clear size="small" /></div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">可用库存</span>
                      <a-input-number v-model:value="searchParams.availableStockMin" :min="0" :precision="2" size="small" style="flex:1" placeholder="≥" />
                    </div>
                  </div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">账面库存</span>
                      <a-input-number v-model:value="searchParams.bookStockMin" :min="0" :precision="2" size="small" style="flex:1" placeholder="≥" />
                    </div>
                  </div>
                  <div class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">显示状态</span>
                      <a-select v-model:value="searchParams.showStatus" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option value="有货">有货</a-select-option>
                        <a-select-option value="无货">无货</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div class="search-field-item">
                    <a-checkbox v-model:checked="searchParams.includeHandled">包含未处理商品</a-checkbox>
                  </div>
                  <div class="search-action-group" ref="uncheckedActionRef" :style="{ gridColumn: 'span ' + uncheckedActionSpan }">
                    <div class="search-field-item search-action-item">
                      <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
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
              :storage-key="activeTab === 'history' ? 'stocktake-table-columns-history' : 'stocktake-table-columns-unchecked'"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="activeTab === 'unchecked'"
              :row-selection="rowSelection"
              :summary-columns="tableFooterColumns"
              row-key="id"
              @page-change="handlePageChange"
            >
              <!-- 单据编号 -->
              <template #stockTakeNoCell="{ record }">
                <a-button type="link" size="small" @click="handleView(record)">{{ record.stockTakeNo }}</a-button>
              </template>
              <!-- 单据状态 -->
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <!-- 盘点方式/类型 -->
              <template #checkMethodCell="{ record }">
                {{ getCheckMethodText(record.checkMethod) }}
              </template>
              <template #checkTypeCell="{ record }">
                {{ getCheckTypeText(record.checkType) }}
              </template>
              <!-- 盈亏 -->
              <template #totalDiffQuantityCell="{ record }">
                <span :class="record.totalDiffQuantity > 0 ? 'positive' : record.totalDiffQuantity < 0 ? 'negative' : ''" class="currency-value">
                  {{ formatQuantity(record.totalDiffQuantity) }}
                </span>
              </template>
              <template #totalDiffAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalDiffAmount) }}</span>
              </template>
              <!-- 操作列 -->
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button v-if="record.status !== 2" type="link" size="small" @click="handleView(record)">修改</a-button>
                  <a-button v-if="record.status !== 2" type="link" size="small" @click="handleProcess(record)">盘点处理</a-button>
                  <a-button v-if="record.status !== 2" type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
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
      :function-buttons-config="functionButtonConfig"
      :storage-key="activePageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, SettingOutlined,
  ExportOutlined, DashboardOutlined, AppstoreAddOutlined, CheckSquareOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { stockTakeApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useRouter } from 'vue-router'

const router = useRouter()

// ═══ Tab 配置 ═══
const tabs = [
  { key: 'history', label: '盘点单历史' },
  { key: 'unchecked', label: '未盘商品查询' },
]
const activeTab = ref('history')

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
const warehouseOptions = ref<{ label: string; value: number }[]>([])

const docGridRef = ref<HTMLElement | null>(null)
const docActionRef = ref<HTMLElement | null>(null)
const uncheckedGridRef = ref<HTMLElement | null>(null)
const uncheckedActionRef = ref<HTMLElement | null>(null)
const { span: docActionSpan } = useAutoGridSpan(docActionRef, docGridRef)
const { span: uncheckedActionSpan } = useAutoGridSpan(uncheckedActionRef, uncheckedGridRef)

const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

const searchParams = reactive({
  stockTakeNo: '', regionName: '', warehouseName: '', handlerName: '', deptName: '',
  creatorName: '', bookkeeperName: '', linkedBillNo: '', remark: '',
  status: undefined as string | number | undefined,
  checkMethod: undefined as string | number | undefined,
  // 未盘商品查询
  checkWarehouseId: undefined as number | undefined,
  location: '', productKeyword: '', filterCondition: '', showStatus: '',
  includeHandled: false, availableStockMin: undefined as number | undefined, bookStockMin: undefined as number | undefined,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const selectedRowKeys = ref<any[]>([])
const selectedRows = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[], rows: any[]) => { selectedRowKeys.value = keys; selectedRows.value = rows },
}))

// 列配置走数据表表头齿轮
const showPageConfig = ref(false)

// ═══ 页面配置 ═══
const PAGE_CONFIG_STORAGE_KEY = 'stock-take-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_HISTORY_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'stockTakeNo', label: '单据编号', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'checkMethod', label: '盘点方式', visible: true },
  { key: 'regionName', label: '区域', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'bookkeeperName', label: '记账人', visible: true },
  { key: 'linkedBillNo', label: '关联单据编号', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
]

const DEFAULT_UNCHECKED_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'filterCondition', label: '筛选条件', visible: true },
  { key: 'checkWarehouseId', label: '仓库', visible: true },
  { key: 'location', label: '货位', visible: true },
  { key: 'productKeyword', label: '商品', visible: true },
  { key: 'availableStockMin', label: '可用库存', visible: true },
  { key: 'bookStockMin', label: '账面库存', visible: true },
  { key: 'showStatus', label: '显示状态', visible: true },
  { key: 'includeHandled', label: '包含未处理商品', visible: true },
]

const DEFAULT_HISTORY_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'summary', label: '汇总盘点', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const DEFAULT_UNCHECKED_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'allCheck', label: '全部盘点', enabled: true },
  { key: 'batchCheck', label: '批量盘点', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const historyQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_HISTORY_QUERY_FIELDS.map(f => ({ ...f })))
const uncheckedQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_UNCHECKED_QUERY_FIELDS.map(f => ({ ...f })))
const historyFunctionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_HISTORY_FUNCTION_BUTTONS.map(f => ({ ...f })))
const uncheckedFunctionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_UNCHECKED_FUNCTION_BUTTONS.map(f => ({ ...f })))

const activeQueryFields = computed(() => activeTab.value === 'history' ? historyQueryConfig.value : uncheckedQueryConfig.value)
const activePageConfigStorageKey = computed(() => `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`)

function loadPageConfig() {
  try {
    const load = (tab: string, setter: (f: QueryFieldSetting[]) => void, defaultConfig: QueryFieldSetting[]) => {
      const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-' + tab)
      if (!raw) return
      const parsed = JSON.parse(raw) as PageConfigData
      if (parsed.queryFields) {
        setter(defaultConfig.map(df => {
          const saved = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        }))
      }
    }
    load('history', v => { historyQueryConfig.value = v }, DEFAULT_HISTORY_QUERY_FIELDS)
    load('unchecked', v => { uncheckedQueryConfig.value = v }, DEFAULT_UNCHECKED_QUERY_FIELDS)
    const hRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-history-buttons')
    if (hRaw) {
      const parsed = JSON.parse(hRaw) as PageConfigData
      if (parsed.functionButtons) historyFunctionButtonConfig.value = DEFAULT_HISTORY_FUNCTION_BUTTONS.map(bf => {
        const saved = parsed.functionButtons!.find((f: FunctionButtonSetting) => f.key === bf.key)
        return saved ? { ...bf, ...saved } : { ...bf }
      })
    }
    const uRaw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY + '-unchecked-buttons')
    if (uRaw) {
      const parsed = JSON.parse(uRaw) as PageConfigData
      if (parsed.functionButtons) uncheckedFunctionButtonConfig.value = DEFAULT_UNCHECKED_FUNCTION_BUTTONS.map(bf => {
        const saved = parsed.functionButtons!.find((f: FunctionButtonSetting) => f.key === bf.key)
        return saved ? { ...bf, ...saved } : { ...bf }
      })
    }
  } catch { /* ignore */ }
}

function handlePageConfigChange(config: any) {
  const tabKey = activeTab.value === 'history' ? '-history' : '-unchecked'
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + tabKey, JSON.stringify({ queryFields: config.queryFields || [] }))
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + '-history-buttons', JSON.stringify({ functionButtons: historyFunctionButtonConfig.value }))
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + '-unchecked-buttons', JSON.stringify({ functionButtons: uncheckedFunctionButtonConfig.value }))
  loadPageConfig()
}

function fnEnabled(key: string): boolean {
  const cfg = activeTab.value === 'history' ? historyFunctionButtonConfig.value : uncheckedFunctionButtonConfig.value
  const found = cfg.find(f => f.key === key)
  return found ? found.enabled : true
}

// ═══ 列定义 ═══
const historyColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 200, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'stockTakeDate', key: 'stockTakeDate', width: 110, sortable: true },
  { title: '单据编号', field: 'stockTakeNo', key: 'stockTakeNo', width: 170, type: 'slot', slotName: 'stockTakeNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '盘点方式', field: 'checkMethod', key: 'checkMethod', width: 90, align: 'center', type: 'slot', slotName: 'checkMethodCell' },
  { title: '盘点类型', field: 'checkType', key: 'checkType', width: 90, align: 'center', type: 'slot', slotName: 'checkTypeCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120, sortable: true },
  { title: '库区', field: 'regionName', key: 'regionName', width: 100 },
  { title: '盈亏数量', field: 'totalDiffQuantity', key: 'totalDiffQuantity', width: 110, align: 'right', type: 'slot', slotName: 'totalDiffQuantityCell', sortable: true },
  { title: '盈亏金额', field: 'totalDiffAmount', key: 'totalDiffAmount', width: 110, align: 'right', type: 'slot', slotName: 'totalDiffAmountCell', sortable: true },
  { title: '关联盘点单', field: 'linkedBillNo', key: 'linkedBillNo', width: 150 },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90, sortable: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90, sortable: true },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 140 },
]

const uncheckedColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '商品名称', field: 'productName', key: 'productName', width: 200, sortable: true },
  { title: '货号', field: 'productCode', key: 'productCode', width: 110 },
  { title: '单位', field: 'productUnit', key: 'productUnit', width: 80 },
  { title: '规格', field: 'productSpec', key: 'productSpec', width: 100 },
  { title: '型号', field: 'model', key: 'model', width: 90 },
  { title: '产地', field: 'origin', key: 'origin', width: 90 },
  { title: '品牌', field: 'brand', key: 'brand', width: 90 },
  { title: '可用库存', field: 'availableStock', key: 'availableStock', width: 100, align: 'right', sortable: true },
  { title: '账面库存', field: 'stockQuantity', key: 'stockQuantity', width: 100, align: 'right', sortable: true },
  { title: '重量（kg）', field: 'weight', key: 'weight', width: 90, align: 'right' },
  { title: '体积（m³）', field: 'volume', key: 'volume', width: 90, align: 'right' },
  { title: '仓库区域', field: 'region', key: 'region', width: 100 },
  { title: '货位', field: 'location', key: 'location', width: 100 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 120 },
]

const currentColumns = computed(() => activeTab.value === 'history' ? historyColumns : uncheckedColumns)

// ═══ 状态/字典映射 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已保存', color: 'blue' },
  2: { text: '已盘点', color: 'green' },
}
function getStatusText(status: number): string { return STATUS_MAP[status]?.text || '未知' }
function getStatusColor(status: number): string { return STATUS_MAP[status]?.color || 'default' }

const CHECK_METHOD_TEXT: Record<number, string> = { 1: '手工录入', 2: '扫码盘点', 3: '快速盘点' }
function getCheckMethodText(m: number): string { return CHECK_METHOD_TEXT[m] || '—' }
const CHECK_TYPE_TEXT: Record<number, string> = { 1: '全面盘点', 2: '抽盘', 3: '动态盘点' }
function getCheckTypeText(t: number): string { return CHECK_TYPE_TEXT[t] || '—' }

// ═══ 表格底部合计 ═══
const tableFooterColumns = computed(() => {
  if (activeTab.value !== 'history') return []
  const totalQty = tableData.value.reduce((s: number, r: any) => s + (r.totalDiffQuantity || 0), 0)
  const totalAmt = tableData.value.reduce((s: number, r: any) => s + (r.totalDiffAmount || 0), 0)
  return [
    { key: 'totalDiffQuantity', value: totalQty, highlight: true },
    { key: 'totalDiffAmount', value: totalAmt, highlight: true },
  ]
})

// ═══ 数据加载 ═══
const loadWarehouses = async () => {
  try {
    const list = await optionsApi.getWarehouses()
    warehouseOptions.value = (list || []).map((w: any) => ({ label: w.warehouseName || w.name, value: w.id }))
  } catch { warehouseOptions.value = [] }
}

async function fetchData() {
  loading.value = true
  try {
    if (activeTab.value === 'history') {
      const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
      if (searchParams.stockTakeNo) params.stockTakeNo = searchParams.stockTakeNo
      if (searchParams.regionName) params.regionName = searchParams.regionName
      if (searchParams.warehouseName) params.warehouseName = searchParams.warehouseName
      if (searchParams.handlerName) params.handlerName = searchParams.handlerName
      if (searchParams.deptName) params.deptName = searchParams.deptName
      if (searchParams.creatorName) params.creatorName = searchParams.creatorName
      if (searchParams.bookkeeperName) params.bookkeeperName = searchParams.bookkeeperName
      if (searchParams.linkedBillNo) params.linkedBillNo = searchParams.linkedBillNo
      if (searchParams.remark) params.remark = searchParams.remark
      if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
      if (searchParams.checkMethod !== undefined && searchParams.checkMethod !== '') params.checkMethod = searchParams.checkMethod
      if (searchParams.startDate) params.dateStart = searchParams.startDate
      if (searchParams.endDate) params.dateEnd = searchParams.endDate
      const res: any = await stockTakeApi.getPage(params)
      const body = (res as any)?.data ?? res
      tableData.value = body?.records || []
      pagination.total = Number(body?.total) || 0
    } else {
      const params: any = { pageSize: 9999 }
      if (searchParams.checkWarehouseId !== undefined) params.checkWarehouseId = searchParams.checkWarehouseId
      if (searchParams.location) params.location = searchParams.location
      if (searchParams.productKeyword) params.productKeyword = searchParams.productKeyword
      if (searchParams.filterCondition) params.filterCondition = searchParams.filterCondition
      if (searchParams.showStatus) params.showStatus = searchParams.showStatus
      if (searchParams.availableStockMin !== undefined && searchParams.availableStockMin !== null) params.availableStockMin = searchParams.availableStockMin
      if (searchParams.bookStockMin !== undefined && searchParams.bookStockMin !== null) params.bookStockMin = searchParams.bookStockMin
      if (searchParams.includeHandled) params.includeHandled = true
      const res: any = await stockTakeApi.uncheckedProducts(params)
      const body = (res as any)?.data ?? res
      const list = Array.isArray(body) ? body : []
      tableData.value = list
      pagination.total = list.length
    }
  } catch (error: any) {
    console.warn('[盘点单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  selectedRowKeys.value = []
  selectedRows.value = []
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
  router.push('/erp/stocktake/form')
}
function handleView(record: any) {
  router.push(`/erp/stocktake/form?id=${record.id}`)
}
function handleProcess(record: any) {
  Modal.confirm({
    title: '盘点处理确认',
    content: `确定对盘点单 ${record.stockTakeNo} 执行盘点处理吗？处理后将按盈亏自动生成报损单/报溢单。`,
    okText: '确认处理',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockTakeApi.process(record.id)
        message.success('盘点处理完成')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || error?.message || '盘点处理失败')
      }
    },
  })
}
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除盘点单 ${record.stockTakeNo} 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockTakeApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ── 未盘商品：创建盘点单并跳转 ──
function buildItemsFromUnchecked() {
  const rows = activeTab.value === 'unchecked' ? tableData.value : selectedRows.value
  return rows.map((p: any) => ({
    productId: p.productId,
    productCode: p.productCode || '',
    productName: p.productName || '',
    productSpec: p.productSpec || '',
    productUnit: p.productUnit || '',
    barcode: p.barcode || '',
    model: p.model || '',
    origin: p.origin || '',
    brand: p.brand || '',
    location: p.location || '',
    region: p.region || '',
    stockQuantity: p.stockQuantity ?? 0,
    checkQuantity: p.stockQuantity ?? 0,
    costPrice: 0,
    checkStatus: 1,
    remark: '',
  }))
}

async function handleAllCheck() {
  if (!searchParams.checkWarehouseId) {
    message.warning('请先选择仓库')
    return
  }
  const items = buildItemsFromUnchecked()
  if (items.length === 0) {
    message.warning('当前仓库没有未盘商品')
    return
  }
  await createAndJump(items, searchParams.checkWarehouseId)
}

async function handleBatchCheck() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先勾选要盘点的商品')
    return
  }
  const items = buildItemsFromUnchecked()
  if (items.length === 0) {
    message.warning('没有可盘点的商品')
    return
  }
  await createAndJump(items, searchParams.checkWarehouseId)
}

async function createAndJump(items: any[], warehouseId?: number) {
  try {
    const payload: any = {
      stockTakeDate: dayjs().format('YYYY-MM-DD'),
      checkMethod: 1,
      checkType: 1,
      warehouseId,
      status: 1,
      items,
    }
    const res: any = await stockTakeApi.create(payload)
    const created = (res as any)?.data ?? res
    message.success(`盘点单已创建：${created?.stockTakeNo || ''}，请录入实盘数量`)
    router.push(`/erp/stocktake/form?id=${created?.id}`)
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '创建盘点单失败')
  }
}

function handleSummary() {
  message.info('汇总盘点：按相同商品汇总盈亏数量')
}

function handlePrintF8() {
  if (activeTab.value === 'history') {
    if (selectedRowKeys.value.length === 0) {
      message.warning('请先选择要打印的盘点单')
      return
    }
    router.push(`/erp/stocktake/form?id=${selectedRowKeys.value[0]}`)
  } else {
    message.warning('请勾选商品后再打印')
  }
}

async function handleExport() {
  try {
    const params: any = { pageNum: 1, pageSize: 9999 }
    const res: any = activeTab.value === 'history'
      ? await stockTakeApi.getPage(params)
      : await stockTakeApi.uncheckedProducts(params)
    const body = (res as any)?.data ?? res
    const data = activeTab.value === 'history' ? (body?.records || []) : (Array.isArray(body) ? body : [])
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = activeTab.value === 'history'
      ? ['单据编号', '单据日期', '仓库', '盘点方式', '盈亏数量', '盈亏金额', '状态']
      : ['商品名称', '货号', '单位', '规格', '型号', '产地', '品牌', '可用库存', '账面库存', '条码']
    const rows = activeTab.value === 'history'
      ? data.map((r: any) => [r.stockTakeNo, r.stockTakeDate, r.warehouseName, getCheckMethodText(r.checkMethod), r.totalDiffQuantity, r.totalDiffAmount, getStatusText(r.status)])
      : data.map((r: any) => [r.productName, r.productCode, r.productUnit, r.productSpec, r.model, r.origin, r.brand, r.availableStock, r.stockQuantity, r.barcode])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${activeTab.value === 'history' ? '盘点单' : '未盘商品'}_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

const handleError = (error: Error) => {
  console.error('[盘点单] 页面错误', error)
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
  loadWarehouses()
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
.positive { color: #3f8600; font-weight: bold; }
.negative { color: #ff4d4f; font-weight: bold; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
