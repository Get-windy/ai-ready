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

        <!-- ═══ 工具栏右侧：操作按钮 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="列配置">
              <a-button
                size="small"
                @click="showColumnConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              type="primary"
              size="small"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增
            </a-button>
            <a-button
              size="small"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="message.info('打印功能待启用')">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-dropdown>
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="export" @click="message.info('导出功能待启用')">
                    导出
                  </a-menu-item>
                  <a-menu-item key="page-config" @click="showPageConfig = true">
                    页面配置
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <!-- 按单据 Tab 搜索 -->
            <template v-if="activeTab === 'doc'">
              <div class="search-container" :data-expanded="showMoreCond || null">
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
                  <a-input
                    v-model:value="searchParams.retailNo"
                    placeholder="单据编号"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.customerName"
                    placeholder="客户"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.handlerName"
                    placeholder="经手人"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.departmentName"
                    placeholder="部门"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.warehouseName"
                    placeholder="仓库"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">单据状态</span>
                    <a-select
                      v-model:value="searchParams.status"
                      size="small"
                      allow-clear
                    >
                      <a-select-option :value="undefined">全部</a-select-option>
                      <a-select-option :value="0">草稿</a-select-option>
                      <a-select-option :value="1">已完成</a-select-option>
                      <a-select-option :value="2">挂单</a-select-option>
                      <a-select-option :value="3">已作废</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">销售类型</span>
                    <a-select
                      v-model:value="searchParams.saleType"
                      size="small"
                      allow-clear
                    >
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option value="NORMAL">正常销售</a-select-option>
                      <a-select-option value="RETURN">退货</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.creatorName"
                    placeholder="制单人"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.memberCardNo"
                    placeholder="会员卡号"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.remark"
                    placeholder="单据备注"
                    allow-clear
                    size="small"
                  />
                </div>
                <!-- 更多条件 -->
                <template v-if="showMoreCond">
                  <div class="search-field-item">
                    <a-input v-model:value="searchParams.extNum1" placeholder="表头自定义1" allow-clear size="small" />
                  </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.extNum2" placeholder="表头自定义2" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.extText1" placeholder="表头自定义3" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.extText2" placeholder="表头自定义4" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.extText3" placeholder="表头自定义5" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.printCount" placeholder="打印次数" allow-clear size="small" />
                </div>
                </template>
                <div class="search-action-group" ref="docActionRef" :style="{ gridColumn: 'span ' + docActionSpan }">
                <div class="search-field-item search-action-item">
                  <a-space :size="8">
                    <a-button
                      type="primary"
                      size="small"
                      @click="handleSearch"
                    >
                      查询
                    </a-button>
                    <a-button
                      size="small"
                      @click="handleReset"
                    >
                      重置
                    </a-button>
                  </a-space>
                </div>
                <div class="search-field-item">
                  <a-checkbox v-model:checked="searchParams.showRedFlush">
                    显示红冲
                  </a-checkbox>
                </div>
                </div>
              </div>
                <div class="search-more-toggle">
                  <a-button
                    type="link"
                    size="small"
                    @click="showMoreCond = !showMoreCond"
                  >
                    {{ showMoreCond ? '收起条件' : '更多条件' }}
                  </a-button>
                </div>
              </div>
            </template>

            <!-- 按明细 Tab 搜索 -->
            <template v-else>
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
                  <a-input
                    v-model:value="searchParams.retailNo"
                    placeholder="单据编号"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.productName"
                    placeholder="商品"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.customerName"
                    placeholder="客户"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.handlerName"
                    placeholder="经手人"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.warehouseName"
                    placeholder="仓库"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">单据状态</span>
                    <a-select
                      v-model:value="searchParams.status"
                      size="small"
                      allow-clear
                    >
                      <a-select-option :value="undefined">全部</a-select-option>
                      <a-select-option :value="0">草稿</a-select-option>
                      <a-select-option :value="1">已完成</a-select-option>
                      <a-select-option :value="2">挂单</a-select-option>
                      <a-select-option :value="3">已作废</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.itemRemark"
                    placeholder="明细备注"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.remark"
                    placeholder="单据备注"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-action-group" ref="detailActionRef" :style="{ gridColumn: 'span ' + detailActionSpan }">
                <div class="search-field-item search-action-item">
                  <a-space :size="8">
                    <a-button
                      type="primary"
                      size="small"
                      @click="handleSearch"
                    >
                      查询
                    </a-button>
                    <a-button
                      size="small"
                      @click="handleReset"
                    >
                      重置
                    </a-button>
                  </a-space>
                </div>
                <div class="search-field-item">
                  <a-checkbox v-model:checked="searchParams.showRedFlush">
                    显示红冲
                  </a-checkbox>
                </div>
                </div>
              </div>
            </template>
          </div>
        </template>

        <!-- ═══ 数据表格 ═══ -->
        <template #table>
          <div class="table-wrap">
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
              <template #retailNoCell="{ record }">
                <a-button
                  type="link"
                  size="small"
                  @click="handleView(record)"
                >
                  {{ record.retailNo }}
                </a-button>
              </template>
              <!-- 单据状态 -->
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>
              <!-- 金额 -->
              <template #amountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.amount) }}</span>
              </template>
              <!-- 应付金额 -->
              <template #payableAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.payableAmount) }}</span>
              </template>
              <!-- 操作列 -->
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button
                    type="link"
                    size="small"
                    @click="handleView(record)"
                  >
                    查看
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                  <a-dropdown v-if="record.status > 0">
                    <a-button type="link" size="small">
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item @click="handleCopy(record)">
                          复制
                        </a-menu-item>
                        <a-menu-item v-if="record.status !== 3" @click="handleVoid(record)">
                          作废
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
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
      :global-config-key="activeTab === 'doc' ? 'retail-list-columns-doc' : 'retail-list-columns-detail'"
      @update:open="showColumnConfig = $event"
      @change="handleColumnConfigChange"
      @reset="handleColumnConfigReset"
      @drag-end="handleColumnConfigChange"
      @global-config-change="handleGlobalColumnConfigChange"
    />

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeTab === 'detail' ? detailQueryConfig : docQueryConfig"
      :storage-key="'retail-page-config'"
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
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  SettingOutlined,
  SearchOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { retailOrderApi, userPageConfigApi } from '@/api/erp'
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

// ═══ 查询方案 ═══
const queryScheme = ref('')

// ═══ 状态 ═══
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数 ═══
const searchParams = reactive({
  retailNo: '',
  customerName: '',
  handlerName: '',
  departmentName: '',
  warehouseName: '',
  productName: '',
  itemRemark: '',
  status: undefined as number | undefined,
  saleType: '',
  remark: '',
  creatorName: '',
  memberCardNo: '',
  extNum1: '',
  extNum2: '',
  extText1: '',
  extText2: '',
  extText3: '',
  printCount: '',
  showRedFlush: false,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

const showMoreCond = ref(false)

const docGridRef = ref<HTMLElement | null>(null)
const docActionRef = ref<HTMLElement | null>(null)
const detailGridRef = ref<HTMLElement | null>(null)
const detailActionRef = ref<HTMLElement | null>(null)
const { span: docActionSpan } = useAutoGridSpan(docActionRef, docGridRef)
const { span: detailActionSpan } = useAutoGridSpan(detailActionRef, detailGridRef)

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

// ═══ 多选 ═══
const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => { selectedRowKeys.value = keys }
}))

// ═══ 配置弹窗 ═══
const showColumnConfig = ref(false)
const showPageConfig = ref(false)

// ═══ 页面配置 ═══
const PAGE_CONFIG_MODULE = 'retail'
const PAGE_CONFIG_PAGE = 'order'

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'retailNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'departmentName', label: '部门', visible: false },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'saleType', label: '销售类型', visible: true },
  { key: 'productAttribute', label: '商品行属性', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'extNum1', label: '表头自定义字段1(数字)', visible: false },
  { key: 'extNum2', label: '表头自定义字段2(数字)', visible: false },
  { key: 'extText1', label: '表头自定义字段3(文本)', visible: false },
  { key: 'extText2', label: '表头自定义字段4(文本)', visible: false },
  { key: 'extText3', label: '表头自定义字段5(文本)', visible: false },
  { key: 'printCount', label: '打印次数', visible: false },
  { key: 'showRedFlush', label: '显示红冲', visible: true },
]

const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'retailNo', label: '单据编号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'departmentName', label: '部门', visible: false },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'itemRemark', label: '明细备注', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'showRedFlush', label: '显示红冲', visible: true },
]

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

async function loadPageConfig() {
  // 先尝试从 API 加载
  try {
    const raw = await userPageConfigApi.get(PAGE_CONFIG_MODULE, PAGE_CONFIG_PAGE)
    if (raw) {
      const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
      if (parsed.queryFields) {
        docQueryConfig.value = DEFAULT_DOC_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
      if (parsed.detailQueryFields) {
        detailQueryConfig.value = DEFAULT_DETAIL_QUERY_FIELDS.map(df => {
          const saved = parsed.detailQueryFields.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
      if (parsed.functionButtons) {
        functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
      return
    }
  } catch { /* API 不可用时切换到 localStorage */ }

  // 切换：从 localStorage 加载
  try {
    const raw = localStorage.getItem('retail-order-page-config')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed.queryFields) {
        docQueryConfig.value = DEFAULT_DOC_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
      if (parsed.functionButtons) {
        functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
    }
  } catch { /* ignore */ }
}

async function handlePageConfigChange(config: any) {
  const payload = {
    queryFields: config.queryFields || docQueryConfig.value,
    detailQueryFields: config.detailQueryFields || detailQueryConfig.value,
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }
  // 保存到后端 API
  try {
    await userPageConfigApi.save(PAGE_CONFIG_MODULE, PAGE_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默失败 */ }
  // 同时写入 localStorage 作为切换
  try {
    localStorage.setItem('retail-order-page-config', JSON.stringify(payload))
  } catch { /* ignore */ }
  await loadPageConfig()
}

// ═══ 列定义 ═══

/** 按单据 Tab 列 (42列) */
const docColumns = [
  { title: '操作', key: 'action', type: 'action', width: 160, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'orderDate', key: 'orderDate', width: 110, sortable: true },
  { title: '单据编号', field: 'retailNo', key: 'retailNo', width: 170, type: 'slot', slotName: 'retailNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120, sortable: true },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 100 },
  { title: '客户', field: 'customerName', key: 'customerName', width: 200, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80, sortable: true },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 100 },
  { title: '销售数量', field: 'totalQuantity', key: 'totalQuantity', width: 80, align: 'right', sortable: true },
  { title: '金额', field: 'amount', key: 'amount', width: 120, align: 'right', type: 'slot', slotName: 'amountCell', sortable: true },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 120, align: 'right' },
  { title: '优惠后金额', field: 'favorableAmount', key: 'favorableAmount', width: 120, align: 'right' },
  { title: '本单金额', field: 'payableAmount', key: 'payableAmount', width: 120, align: 'right', type: 'slot', slotName: 'payableAmountCell', sortable: true },
  { title: '促销优惠', field: 'promoDiscount', key: 'promoDiscount', width: 100, align: 'right' },
  { title: '优惠券', field: 'couponDiscount', key: 'couponDiscount', width: 100, align: 'right' },
  { title: '直接优惠', field: 'directDiscount', key: 'directDiscount', width: 100, align: 'right' },
  { title: '优惠金额', field: 'totalDiscount', key: 'totalDiscount', width: 100, align: 'right' },
  { title: '收款现金', field: 'cashAmount', key: 'cashAmount', width: 100, align: 'right' },
  { title: '收款银行卡', field: 'cardAmount', key: 'cardAmount', width: 100, align: 'right' },
  { title: '收款支付宝', field: 'alipayAmount', key: 'alipayAmount', width: 100, align: 'right' },
  { title: '收款微信', field: 'wechatAmount', key: 'wechatAmount', width: 100, align: 'right' },
  { title: '收款聚合支付', field: 'aggregateAmount', key: 'aggregateAmount', width: 110, align: 'right' },
  { title: '收款预收款', field: 'prepaidAmount', key: 'prepaidAmount', width: 100, align: 'right' },
  { title: '收款共享平台支付账户', field: 'paymentAccount1', key: 'paymentAccount1', width: 150 },
  { title: '收款中国农业银行', field: 'abcAmount', key: 'abcAmount', width: 120, align: 'right' },
  { title: '收款中国建设银行', field: 'ccbAmount', key: 'ccbAmount', width: 120, align: 'right' },
  { title: '收款京东支付', field: 'jdAmount', key: 'jdAmount', width: 100, align: 'right' },
  { title: '收款合计', field: 'totalReceived', key: 'totalReceived', width: 100, align: 'right' },
  { title: '销售类型', field: 'saleType', key: 'saleType', width: 80 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '表头自定义字段1(数字)', field: 'extNum1', key: 'extNum1', width: 130 },
  { title: '表头自定义字段2(数字)', field: 'extNum2', key: 'extNum2', width: 130 },
  { title: '表头自定义字段3(文本)', field: 'extText1', key: 'extText1Doc', width: 130 },
  { title: '表头自定义字段4(文本)', field: 'extText2', key: 'extText2Doc', width: 130 },
  { title: '表头自定义字段5(文本)', field: 'extText3', key: 'extText3Doc', width: 130 },
  { title: '摘要', field: 'summary', key: 'summary', width: 150 },
  { title: '附件', field: 'attachment', key: 'attachmentDoc', width: 60 },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 80 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80, sortable: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' },
]

/** 按明细 Tab 列 (52列) */
const detailColumns = [
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'orderDate', key: 'orderDate', width: 110, sortable: true },
  { title: '单据编号', field: 'retailNo', key: 'retailNo', width: 170, type: 'slot', slotName: 'retailNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120, sortable: true },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 100 },
  { title: '客户', field: 'customerName', key: 'customerName', width: 160, sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80, sortable: true },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 100 },
  { title: '商品名称', field: 'productName', key: 'productName', width: 200, sortable: true },
  { title: '货号', field: 'itemCode', key: 'itemCode', width: 100 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 110 },
  { title: '规格', field: 'specification', key: 'specification', width: 100 },
  { title: '型号', field: 'model', key: 'model', width: 80 },
  { title: '产地', field: 'origin', key: 'origin', width: 80 },
  { title: '品牌', field: 'brand', key: 'brand', width: 80 },
  { title: '单据自定义1(数字字段)', field: 'extNum1', key: 'extNum1', width: 120 },
  { title: '单据自定义2(数字字段)', field: 'extNum2', key: 'extNum2', width: 120 },
  { title: '单据自定义3(数字字段)', field: 'extNum3', key: 'extNum3', width: 120 },
  { title: '单据自定义4(文本字段)', field: 'extText1', key: 'extText1Detail', width: 120 },
  { title: '单据自定义5(文本字段)', field: 'extText2', key: 'extText2Detail', width: 120 },
  { title: '单据自定义6(数字字段)', field: 'extNum4', key: 'extNum4', width: 120 },
  { title: '单据自定义7(数字字段)', field: 'extNum5', key: 'extNum5', width: 120 },
  { title: '单据自定义8(往来单位)', field: 'extPartner', key: 'extPartner', width: 120 },
  { title: '单据自定义9(职员)', field: 'extStaff', key: 'extStaff', width: 100 },
  { title: '单据自定义10(部门)', field: 'extDept', key: 'extDept', width: 100 },
  { title: '图片', field: 'imageUrl', key: 'imageUrl', width: 80, type: 'image' },
  { title: '区域', field: 'region', key: 'region', width: 90 },
  { title: '货位', field: 'location', key: 'location', width: 90 },
  { title: '计价单位', field: 'productUnit', key: 'productUnitDetail', width: 90 },
  { title: '商品行属性', field: 'productAttribute', key: 'productAttribute', width: 100 },
  { title: '件散数量', field: 'pieceQuantity', key: 'pieceQuantity', width: 90, align: 'right' },
  { title: '可用库存', field: 'availableStock', key: 'availableStock', width: 100, align: 'right' },
  { title: '可用库存换算结果', field: 'availableStockConverted', key: 'availableStockConverted', width: 130, align: 'right' },
  { title: '账面库存', field: 'bookStock', key: 'bookStock', width: 100, align: 'right' },
  { title: '批次条码', field: 'batchNo', key: 'batchNo', width: 120 },
  { title: '生产日期', field: 'productionDate', key: 'productionDate', width: 110 },
  { title: '保质期', field: 'shelfLife', key: 'shelfLife', width: 90 },
  { title: '到期日期', field: 'validityDate', key: 'validityDate', width: 110 },
  { title: '最近销售日期', field: 'lastSaleDate', key: 'lastSaleDate', width: 110 },
  { title: '最近售价', field: 'lastSalePrice', key: 'lastSalePrice', width: 100, align: 'right' },
  { title: '零售价', field: 'retailPrice', key: 'retailPrice', width: 100, align: 'right' },
  { title: '批发价', field: 'wholesalePrice', key: 'wholesalePrice', width: 100, align: 'right' },
  { title: '最低售价', field: 'minSalePrice', key: 'minSalePrice', width: 100, align: 'right' },
  { title: '参考成本单价', field: 'costPrice', key: 'costPrice', width: 110, align: 'right' },
  { title: '参考成本金额', field: 'costAmount', key: 'costAmount', width: 110, align: 'right' },
  { title: '兑换礼品', field: 'giftItem', key: 'giftItem', width: 100 },
  { title: '兑换积分', field: 'exchangePoints', key: 'exchangePoints', width: 90, align: 'right' },
  { title: '产生积分', field: 'generatedPoints', key: 'generatedPoints', width: 90, align: 'right' },
  { title: '使用积分', field: 'usedPoints', key: 'usedPoints', width: 90, align: 'right' },
  { title: '赠品', field: 'gift', key: 'gift', width: 70 },
  { title: '价格等级1', field: 'priceLevel1', key: 'priceLevel1', width: 100, align: 'right' },
  { title: '价格等级2', field: 'priceLevel2', key: 'priceLevel2', width: 100, align: 'right' },
  { title: '价格等级3', field: 'priceLevel3', key: 'priceLevel3', width: 100, align: 'right' },
  { title: '价格等级4', field: 'priceLevel4', key: 'priceLevel4', width: 100, align: 'right' },
  { title: '价格等级5', field: 'priceLevel5', key: 'priceLevel5', width: 100, align: 'right' },
  { title: '价格等级6', field: 'priceLevel6', key: 'priceLevel6', width: 100, align: 'right' },
  { title: '价格等级7', field: 'priceLevel7', key: 'priceLevel7', width: 100, align: 'right' },
  { title: '价格等级8', field: 'priceLevel8', key: 'priceLevel8', width: 100, align: 'right' },
  { title: '单位', field: 'unit', key: 'unit', width: 80 },
  { title: '销售数量', field: 'quantity', key: 'quantity', width: 80, align: 'right', sortable: true },
  { title: '换算关系', field: 'conversionRelation', key: 'conversionRelation', width: 100 },
  { title: '换算结果', field: 'conversionResult', key: 'conversionResult', width: 100 },
  { title: '大包装', field: 'bigPack', key: 'bigPack', width: 70, align: 'right' },
  { title: '中包装', field: 'midPack', key: 'midPack', width: 70, align: 'right' },
  { title: '小包装', field: 'smallPack', key: 'smallPack', width: 70, align: 'right' },
  { title: '小单位', field: 'smallUnit', key: 'smallUnit', width: 70 },
  { title: '小单位数量', field: 'smallUnitQuantity', key: 'smallUnitQuantity', width: 90, align: 'right' },
  { title: '单价', field: 'unitPrice', key: 'unitPrice', width: 80, align: 'right' },
  { title: '小单位单价', field: 'smallUnitPrice', key: 'smallUnitPrice', width: 90, align: 'right' },
  { title: '金额', field: 'amount', key: 'itemAmount', width: 100, align: 'right', sortable: true },
  { title: '折扣(%)', field: 'discountRate', key: 'discountRate', width: 70, align: 'right' },
  { title: '折后单价', field: 'discountedPrice', key: 'discountedPrice', width: 80, align: 'right' },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 100, align: 'right' },
  { title: '优惠折扣', field: 'favorableDiscountRate', key: 'favorableDiscountRate', width: 80, align: 'right' },
  { title: '优惠后单价', field: 'favorableUnitPrice', key: 'favorableUnitPrice', width: 90, align: 'right' },
  { title: '优惠后金额', field: 'favorableAmount', key: 'favorableAmount', width: 100, align: 'right' },
  { title: '明细备注', field: 'itemRemark', key: 'itemRemark', width: 120 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '摘要', field: 'summary', key: 'summary', width: 120 },
  { title: '附件', field: 'attachment', key: 'attachmentDetail', width: 60 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80, sortable: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 80 },
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
} = useColumnConfig(docColumnDefs.value, 'retail-list-columns-doc')
const {
  visibleColumns: detailVisibleColumns,
  onSettingChange: onDetailSettingChange,
  resetSettings: resetDetailSettings,
  settingsColumns: detailSettingsColumns,
} = useColumnConfig(detailColumnDefs.value, 'retail-list-columns-detail')

const currentColumns = computed(() => activeTab.value === 'doc' ? docVisibleColumns.value : detailVisibleColumns.value)
const panelColumns = computed(() => activeTab.value === 'doc' ? docSettingsColumns.value : detailSettingsColumns.value)

function handleColumnConfigChange() {
  if (activeTab.value === 'doc') onDocSettingChange()
  else onDetailSettingChange()
}
function handleColumnConfigReset() {
  if (activeTab.value === 'doc') resetDocSettings()
  else resetDetailSettings()
}
function handleGlobalColumnConfigChange(settings: any[]) {
  const key = activeTab.value === 'doc' ? 'retail-list-columns-doc' : 'retail-list-columns-detail'
  userPageConfigApi.save('col-config', key, JSON.stringify(settings)).catch(() => {
    // 静默失败，已切换到 localStorage
  })
}

// ═══ 状态映射 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已完成', color: 'green' },
  2: { text: '挂单', color: 'orange' },
  3: { text: '已作废', color: 'red' },
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
  const totalAmount = tableData.value.reduce((s: number, r: any) => s + (r.amount || 0), 0)
  const payableAmount = tableData.value.reduce((s: number, r: any) => s + (r.payableAmount || 0), 0)
  const totalReceived = tableData.value.reduce((s: number, r: any) => s + (r.totalReceived || 0), 0)
  return [
    { key: 'amount', value: totalAmount, highlight: true },
    { key: 'payableAmount', value: payableAmount, highlight: true },
    { key: 'totalReceived', value: totalReceived, highlight: true },
  ]
})

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  hasError.value = false
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchParams.retailNo) params.retailNo = searchParams.retailNo
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.departmentName) params.departmentName = searchParams.departmentName
    if (searchParams.warehouseName) params.warehouseName = searchParams.warehouseName
    if (searchParams.status !== undefined) params.status = searchParams.status
    if (searchParams.saleType) params.saleType = searchParams.saleType
    if (searchParams.remark) params.remark = searchParams.remark
    if (searchParams.creatorName) params.creatorName = searchParams.creatorName
    if (searchParams.memberCardNo) params.memberCardNo = searchParams.memberCardNo
    if (searchParams.startDate) params.dateStart = searchParams.startDate
    if (searchParams.endDate) params.dateEnd = searchParams.endDate
    if (searchParams.showRedFlush) params.showRedFlush = true

    if (activeTab.value === 'detail') {
      if (searchParams.productName) params.productName = searchParams.productName
      if (searchParams.itemRemark) params.remark = searchParams.itemRemark
      const res = await retailOrderApi.pageByDetail(params)
      if (res) {
        tableData.value = res.records || []
        pagination.total = Number(res.total) || 0
      }
    } else {
      const res = await retailOrderApi.pageByDoc(params)
      if (res) {
        tableData.value = res.records || []
        pagination.total = Number(res.total) || 0
      }
    }
  } catch (error: any) {
    hasError.value = true
    console.warn('[零售单] 获取列表失败', error)
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

function handleSearch() { pagination.current = 1; fetchData() }
function handleReset() {
  Object.assign(searchParams, {
    retailNo: '', customerName: '', handlerName: '', departmentName: '', warehouseName: '',
    productName: '', itemRemark: '', status: undefined, saleType: '', remark: '',
    creatorName: '', memberCardNo: '', showRedFlush: false,
  })
  dateRange.value = [dayjs().subtract(7, 'day'), dayjs()]
  searchParams.startDate = dayjs().subtract(7, 'day').format('YYYY-MM-DD')
  searchParams.endDate = dayjs().format('YYYY-MM-DD')
  pagination.current = 1
  fetchData()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page; pagination.pageSize = pageSize; fetchData()
}

// ═══ 操作 ═══
function handleAdd() { router.push('/sales/retail/create') }
function handleView(record: any) { router.push(`/sales/retail/form/${record.id}`) }
function handleEdit(record: any) { router.push(`/sales/retail/form/${record.id}`) }
function handleCopy(record: any) {
  Modal.confirm({
    title: '确认复制', content: `确定要复制零售单 ${record.retailNo} 吗？`,
    onOk: async () => {
      try {
        await retailOrderApi.copy(record.id)
        message.success('复制成功')
        fetchData()
      } catch (e: any) { message.error(e?.response?.data?.message || '复制失败') }
    },
  })
}
function handleVoid(record: any) {
  Modal.confirm({
    title: '确认作废', content: `确定要作废零售单 ${record.retailNo} 吗？`,
    okType: 'danger', okText: '确认作废',
    onOk: async () => {
      try {
        await retailOrderApi.voidOrder(record.id)
        message.success('作废成功')
        fetchData()
      } catch (e: any) { message.error(e?.response?.data?.message || '作废失败') }
    },
  })
}
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除', content: `确定要删除零售单 ${record.retailNo} 吗？`,
    okType: 'danger', okText: '确认删除',
    onOk: async () => {
      try {
        await retailOrderApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (e: any) { message.error(e?.response?.data?.message || '删除失败') }
    },
  })
}

const handleError = (error: Error) => {
  hasError.value = true
  console.error('[零售单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

function formatAmount(amount: number): string {
  if (!amount) return '0.00'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 初始化 ═══
onMounted(async () => {
  await loadPageConfig()
  setQuickDate('lastWeek')
  fetchData()
})
</script>
<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
.quick-dates :deep(.ant-btn) { font-size: 13px; padding: 2px 8px; }
.quick-dates :deep(.ant-btn-primary) { font-weight: 600; }
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
.table-wrap { flex: 1; overflow: hidden; }
.currency-value { font-family: 'Menlo', 'Monaco', 'Consolas', monospace; font-size: 13px; }
</style>
