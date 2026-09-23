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
            <a-button
              size="small"
              type="primary"
              :disabled="selectedRows.length === 0"
              :loading="batchActing"
              @click="handleBatchPurchase('FINISHED')"
            >
              <ShoppingCartOutlined /> 采购成品({{ selectedRows.length }})
            </a-button>
            <a-button
              size="small"
              :disabled="selectedRows.length === 0"
              :loading="batchActing"
              @click="handleBatchPurchase('MATERIAL')"
            >
              <AppstoreOutlined /> 采购原料({{ selectedRows.length }})
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container" :data-expanded="showMoreConditions || null">
            <div class="search-grid" ref="gridRef">
              <div v-show="queryFieldVisible('dateRange')" class="search-field-item">
                <a-range-picker
                  v-model:value="documentDateRange"
                  size="small"
                  style="width: 100%"
                  @change="handleDocumentDateChange"
                />
              </div>
              <div v-show="queryFieldVisible('orderNo')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.orderNo"
                  placeholder="单据编号"
                  allow-clear
                  size="small"
                />
              </div>
              <div v-show="queryFieldVisible('saleType')" class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">销售类型</span>
                  <a-select
                    v-model:value="searchParams.saleType"
                    placeholder="全部"
                    allow-clear
                    size="small"
                  >
                    <a-select-option :value="1">正常销售</a-select-option>
                    <a-select-option :value="2">样品</a-select-option>
                    <a-select-option :value="3">促销</a-select-option>
                  </a-select>
                </div>
              </div>
              <div v-show="queryFieldVisible('generationMethod')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.generationMethod"
                  placeholder="录入方式"
                  allow-clear
                  size="small"
                />
              </div>
              <div v-show="queryFieldVisible('customerName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.customerName"
                  placeholder="客户"
                  allow-clear
                  size="small"
                  :suffix="h(SearchOutlined, { style: 'color:#bbb' })"
                />
              </div>
              <div v-show="queryFieldVisible('salesmanName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.salesmanName"
                  placeholder="经手人"
                  allow-clear
                  size="small"
                  :suffix="h(SearchOutlined, { style: 'color:#bbb' })"
                />
              </div>
              <div v-show="queryFieldVisible('deptName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.deptName"
                  placeholder="部门"
                  allow-clear
                  size="small"
                />
              </div>
              <div v-show="queryFieldVisible('creatorName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.creatorName"
                  placeholder="制单人"
                  allow-clear
                  size="small"
                />
              </div>
              <div v-show="queryFieldVisible('submitterName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.submitterName"
                  placeholder="提交人"
                  allow-clear
                  size="small"
                />
              </div>
              <div v-show="queryFieldVisible('auditorName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.auditorName"
                  placeholder="审核人"
                  allow-clear
                  size="small"
                />
              </div>
              <div v-show="queryFieldVisible('warehouseName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.warehouseName"
                  placeholder="仓库"
                  allow-clear
                  size="small"
                  :suffix="h(SearchOutlined, { style: 'color:#bbb' })"
                />
              </div>
              <div v-show="queryFieldVisible('productName')" class="search-field-item">
                <a-input
                  v-model:value="searchParams.productName"
                  placeholder="商品"
                  allow-clear
                  size="small"
                  :suffix="h(SearchOutlined, { style: 'color:#bbb' })"
                />
              </div>
              <div v-show="queryFieldVisible('status')" class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">单据状态</span>
                  <a-select
                    v-model:value="searchParams.status"
                    placeholder="全部"
                    allow-clear
                    size="small"
                  >
                    <a-select-option :value="0">草稿</a-select-option>
                    <a-select-option :value="1">待审核</a-select-option>
                    <a-select-option :value="2">待发货</a-select-option>
                    <a-select-option :value="3">部分发货</a-select-option>
                    <a-select-option :value="4">发货完成</a-select-option>
                    <a-select-option :value="5">交易完成</a-select-option>
                    <a-select-option :value="6">已取消</a-select-option>
                  </a-select>
                </div>
              </div>
              <div v-show="queryFieldVisible('bookkeepingStatus')" class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">记账状态</span>
                  <a-select
                    v-model:value="searchParams.bookkeepingStatus"
                    placeholder="全部"
                    allow-clear
                    size="small"
                  >
                    <a-select-option :value="0">未完成</a-select-option>
                    <a-select-option :value="1">已完成</a-select-option>
                  </a-select>
                </div>
              </div>
              <div v-show="queryFieldVisible('paymentStatus')" class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">支付状态</span>
                  <a-select
                    v-model:value="searchParams.paymentStatus"
                    placeholder="全部"
                    allow-clear
                    size="small"
                  >
                    <a-select-option :value="0">未支付</a-select-option>
                    <a-select-option :value="1">部分支付</a-select-option>
                    <a-select-option :value="2">已支付</a-select-option>
                  </a-select>
                </div>
              </div>

              <!-- 更多条件（展开后显示） -->
              <template v-if="showMoreConditions">
                <div v-show="queryFieldVisible('shipDate')" class="search-field-item">
                  <a-range-picker
                    v-model:value="shipDateRange"
                    size="small"
                    style="width: 100%"
                    @change="handleShipDateChange"
                  />
                </div>
                <div v-show="queryFieldVisible('receiverName')" class="search-field-item">
                  <a-input
                    v-model:value="searchParams.receiverName"
                    placeholder="收货人"
                    allow-clear
                    size="small"
                  />
                </div>
                <div v-show="queryFieldVisible('receiverPhone')" class="search-field-item">
                  <a-input
                    v-model:value="searchParams.receiverPhone"
                    placeholder="联系电话"
                    allow-clear
                    size="small"
                  />
                </div>
                <div v-show="queryFieldVisible('shippingAddress')" class="search-field-item">
                  <a-input
                    v-model:value="searchParams.shippingAddress"
                    placeholder="收货地址"
                    allow-clear
                    size="small"
                  />
                </div>
                <div v-show="queryFieldVisible('logisticsCompany')" class="search-field-item">
                  <a-input
                    v-model:value="searchParams.logisticsCompany"
                    placeholder="物流公司"
                    allow-clear
                    size="small"
                  />
                </div>
                <div v-show="queryFieldVisible('waybillNo')" class="search-field-item">
                  <a-input
                    v-model:value="searchParams.waybillNo"
                    placeholder="运单号"
                    allow-clear
                    size="small"
                  />
                </div>
                <div v-show="queryFieldVisible('depositAccount1')" class="search-field-item">
                  <a-input
                    v-model:value="searchParams.depositAccount1"
                    placeholder="订金账户1"
                    allow-clear
                    size="small"
                  />
                </div>
                <div v-show="queryFieldVisible('depositAccount2')" class="search-field-item">
                  <a-input
                    v-model:value="searchParams.depositAccount2"
                    placeholder="订金账户2"
                    allow-clear
                    size="small"
                  />
                </div>
                <div v-show="queryFieldVisible('sellerRemark')" class="search-field-item">
                  <a-input
                    v-model:value="searchParams.sellerRemark"
                    placeholder="卖家备注"
                    allow-clear
                    size="small"
                  />
                </div>
                <div v-show="queryFieldVisible('buyerRemark')" class="search-field-item">
                  <a-input
                    v-model:value="searchParams.buyerRemark"
                    placeholder="买家备注"
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
                <div v-show="queryFieldVisible('onlyCoupon')" class="search-field-item">
                  <a-checkbox v-model:checked="searchParams.onlyCoupon">
                    只查看用了优惠券的订单
                  </a-checkbox>
                </div>
                <div v-show="queryFieldVisible('showCancelled')" class="search-field-item">
                  <a-checkbox v-model:checked="searchParams.showCancelled">
                    显示已取消
                  </a-checkbox>
                </div>
                <div v-show="queryFieldVisible('onlySelected')" class="search-field-item">
                  <a-checkbox v-model:checked="searchParams.onlySelected">
                    仅显示已选中
                  </a-checkbox>
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
            :storage-key="'purchase-sales-driven-table-columns'"
            :data-source="tableData"
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
            <!-- 操作列：采购成品 / 采购原料 -->
            <template #actionCell="{ record }">
              <a-space :size="4">
                <a-button
                  type="link"
                  size="small"
                  :disabled="record.hasPurchased"
                  @click="handlePurchase('FINISHED', record)"
                >采购成品</a-button>
                <a-button
                  type="link"
                  size="small"
                  :disabled="record.hasPurchased"
                  @click="handlePurchase('MATERIAL', record)"
                >采购原料</a-button>
              </a-space>
            </template>
          </BillTableList>
        </template>

        <!-- ═══ 表格底部：合计 ═══ -->
        <template #table-footer>
          <div class="table-footer">
            <div class="footer-label">合计</div>
            <div class="footer-values">
              <span v-if="summaryData.count">共 {{ summaryData.count }} 单</span>
              <span v-if="summaryData.totalQuantity">发货商品数量: {{ summaryData.totalQuantity }}</span>
              <span v-if="summaryData.billAmount">订单金额: {{ summaryData.billAmount }}</span>
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

  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined, SearchOutlined, DownOutlined, UpOutlined,
  SettingOutlined, PlusOutlined, PrinterOutlined, ExportOutlined,
  ShoppingCartOutlined, AppstoreOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import request from '@/utils/request'

// ═══ 状态映射 ═══
const STATUS_MAP: Record<number, string> = {
  0: '草稿', 1: '待审核', 2: '待发货', 3: '部分发货',
  4: '发货完成', 5: '交易完成', 6: '已取消'
}
const SALE_TYPE_MAP: Record<number, string> = { 1: '正常销售', 2: '样品', 3: '促销' }
const BOOKKEEPING_MAP: Record<number, string> = { 0: '未完成', 1: '已完成' }
const YES_NO_MAP: Record<string, string> = { true: '是', false: '否' }

// ═══ 状态 ═══
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const selectedRows = ref<any[]>([])
const batchActing = ref(false)
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
const shipDateRange = ref<[Dayjs, Dayjs] | null>(null)

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

// ═══ 搜索参数 ═══
const searchParams = reactive({
  dateStart: '',
  dateEnd: '',
  orderNo: '',
  saleType: undefined as number | undefined,
  generationMethod: '',
  customerName: '',
  salesmanName: '',
  deptName: '',
  creatorName: '',
  submitterName: '',
  auditorName: '',
  warehouseName: '',
  productName: '',
  status: undefined as number | undefined,
  bookkeepingStatus: undefined as number | undefined,
  paymentStatus: undefined as number | undefined,
  shipDateStart: '',
  shipDateEnd: '',
  receiverName: '',
  receiverPhone: '',
  shippingAddress: '',
  logisticsCompany: '',
  waybillNo: '',
  depositAccount1: '',
  depositAccount2: '',
  sellerRemark: '',
  buyerRemark: '',
  onlyCoupon: false,
  showCancelled: false,
  onlySelected: false,
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total
}))

// ═══ 42个表格列（按文档顺序） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' as const },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right' as const, slotName: 'actionCell' },
  { title: '单据日期', field: 'orderDate', key: 'orderDate', width: 100, sortable: true },
  { title: '单据编号', field: 'orderNo', key: 'orderNo', width: 150, sortable: true },
  { title: '单据子类型', field: 'saleTypeText', key: 'saleType', width: 90 },
  { title: '录入方式', field: 'generationMethod', key: 'generationMethod', width: 100 },
  { title: '单据状态', field: 'statusText', key: 'status', width: 90 },
  { title: '是否使用优惠券', field: 'useCouponText', key: 'useCoupon', width: 100 },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 130 },
  { title: '客户', field: 'customerName', key: 'customerName', width: 130, sortable: true },
  { title: '记账状态', field: 'bookkeepingStatusText', key: 'bookkeepingStatus', width: 90 },
  { title: '强制终止', field: 'forcedTerminatedText', key: 'forcedTerminated', width: 80 },
  { title: '是否采购', field: 'hasPurchasedText', key: 'hasPurchased', width: 80 },
  { title: '发货商品数量', field: 'totalQuantity', key: 'totalQuantity', width: 110, align: 'right' as const },
  { title: '发货已发数量', field: 'shippedQuantity', key: 'shippedQuantity', width: 110, align: 'right' as const },
  { title: '发货未发数量', field: 'unshippedQuantity', key: 'unshippedQuantity', width: 110, align: 'right' as const },
  { title: '重量（kg）', field: 'totalWeight', key: 'totalWeight', width: 100, align: 'right' as const },
  { title: '体积（m³）', field: 'totalVolume', key: 'totalVolume', width: 100, align: 'right' as const },
  { title: '订单金额', field: 'billAmount', key: 'billAmount', width: 110, align: 'right' as const },
  { title: '订金账户1', field: 'depositAccount1', key: 'depositAccount1', width: 110 },
  { title: '订金金额1', field: 'depositAmount1', key: 'depositAmount1', width: 100, align: 'right' as const },
  { title: '订金账户2', field: 'depositAccount2', key: 'depositAccount2', width: 110 },
  { title: '订金金额2', field: 'depositAmount2', key: 'depositAmount2', width: 100, align: 'right' as const },
  { title: '物流公司', field: 'logisticsCompany', key: 'logisticsCompany', width: 110 },
  { title: '运单号', field: 'waybillNo', key: 'waybillNo', width: 130 },
  { title: '运费', field: 'shippingFee', key: 'shippingFee', width: 90, align: 'right' as const },
  { title: '优惠劵', field: 'couponAmount', key: 'couponAmount', width: 90, align: 'right' as const },
  { title: '优惠金额', field: 'discountAmount', key: 'discountAmount', width: 100, align: 'right' as const },
  { title: '收货人', field: 'receiverName', key: 'receiverName', width: 90 },
  { title: '联系电话', field: 'receiverPhone', key: 'receiverPhone', width: 120 },
  { title: '收货地址', field: 'shippingAddress', key: 'shippingAddress', width: 150 },
  { title: '预计发货', field: 'expectedShipTime', key: 'expectedShipTime', width: 120 },
  { title: '经手人', field: 'salesmanName', key: 'salesmanName', width: 90 },
  { title: '部门', field: 'deptName', key: 'deptName', width: 90 },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90 },
  { title: '提交人', field: 'submitterName', key: 'submitterName', width: 90 },
  { title: '审核人', field: 'auditorName', key: 'auditorName', width: 90 },
  { title: '卖家备注', field: 'orderRemark', key: 'orderRemark', width: 130 },
  { title: '买家备注', field: 'buyerRemark', key: 'buyerRemark', width: 130 },
  { title: '附件', field: 'attachment', key: 'attachment', width: 70 },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' as const },
  { title: '提交时间', field: 'submitTime', key: 'submitTime', width: 160 },
  { title: '制单时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 160 },
]

// ═══ 合计 ═══
const summaryData = computed(() => {
  const data = tableData.value
  if (!data.length) return {}
  const sum = (key: string) => {
    const val = data.reduce((acc, r) => acc + (Number(r[key]) || 0), 0)
    return val ? Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 2 }) : ''
  }
  return {
    count: data.length,
    totalQuantity: sum('totalQuantity'),
    billAmount: sum('billAmount'),
  }
})

// ═══ 页面配置 ═══
const queryFieldsConfig = ref([
  { key: 'dateRange', label: '日期', visible: true },
  { key: 'orderNo', label: '单据编号', visible: true },
  { key: 'saleType', label: '销售类型', visible: true },
  { key: 'generationMethod', label: '录入方式', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'salesmanName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'submitterName', label: '提交人', visible: true },
  { key: 'auditorName', label: '审核人', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'bookkeepingStatus', label: '记账状态', visible: true },
  { key: 'paymentStatus', label: '支付状态', visible: true },
  // ⚠️ 语义统一为「该查询条件是否启用」，**不是**「是否已折叠进更多条件」。
  // 这些字段本来就在「更多条件」折叠区里，展开/收起由 showMoreConditions 控制；
  // 若这里再给 false，用户展开「更多条件」后仍看不到它们（页面配置与折叠区互相打架）。
  { key: 'shipDate', label: '发货日期', visible: true },
  { key: 'receiverName', label: '收货人', visible: true },
  { key: 'receiverPhone', label: '联系电话', visible: true },
  { key: 'shippingAddress', label: '收货地址', visible: true },
  { key: 'logisticsCompany', label: '物流公司', visible: true },
  { key: 'waybillNo', label: '运单号', visible: true },
  { key: 'depositAccount1', label: '订金账户1', visible: true },
  { key: 'depositAccount2', label: '订金账户2', visible: true },
  { key: 'sellerRemark', label: '卖家备注', visible: true },
  { key: 'buyerRemark', label: '买家备注', visible: true },
  { key: 'onlyCoupon', label: '只查看用了优惠券的订单', visible: true },
  { key: 'showCancelled', label: '显示已取消', visible: true },
  { key: 'onlySelected', label: '仅显示已选中', visible: true },
])

const functionButtonConfig = ref([
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'purchaseFinished', label: '采购成品', enabled: true },
  { key: 'purchaseMaterial', label: '采购原料', enabled: true },
  { key: 'config', label: '配置', enabled: true },
])

/** 页面配置在 localStorage 的键，与 PageConfigPanel 的 storage-key 同源 */
const PAGE_CONFIG_STORAGE_KEY = 'purchase-sales-driven-page-config'

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

const handleShipDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) {
    searchParams.shipDateStart = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.shipDateEnd = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.shipDateStart = ''
    searchParams.shipDateEnd = ''
  }
}

const toggleMoreConditions = () => { showMoreConditions.value = !showMoreConditions.value }

// ═══ 数据请求 ═══
const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const apiParams: Record<string, any> = {
      current: pagination.current,
      size: pagination.pageSize,
      ...searchParams
    }
    Object.keys(apiParams).forEach(key => {
      if (apiParams[key] === '' || apiParams[key] === null || apiParams[key] === undefined) {
        delete apiParams[key]
      }
    })
    // 布尔参数保留 false（后端需区分未传/显式false）
    const res: any = await request.get('/erp/purchase/sales-driven/page', { params: apiParams })
    if (res) {
      const data = res.data || res
      const records = data.records || data.content || data.list || []
      records.forEach((r: any) => {
        r.statusText = STATUS_MAP[r.status] ?? r.status
        r.saleTypeText = SALE_TYPE_MAP[r.saleType] ?? r.saleType
        r.bookkeepingStatusText = r.bookkeepingStatus == null ? '-' : (BOOKKEEPING_MAP[r.bookkeepingStatus] ?? r.bookkeepingStatus)
        r.hasPurchasedText = YES_NO_MAP[String(r.hasPurchased)] ?? '否'
        r.useCouponText = YES_NO_MAP[String(r.useCoupon)] ?? '否'
        r.forcedTerminatedText = YES_NO_MAP[String(r.forcedTerminated)] ?? '否'
      })
      tableData.value = records
      pagination.total = data.total || 0
    }
  } catch (e: any) {
    hasError.value = true
    console.warn('[以销定购] 获取失败', e)
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

const handleSelectionChange = (rows: any[], _ids: (string | number)[]) => {
  selectedRows.value = rows
}

const reload = () => fetchData()
const clearSelection = () => { selectedRows.value = [] }

// ═══ 导出 ═══
function handleExportMenu({ key }: { key: string | number }) {
  if (tableData.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  const exportCols = columns.filter((c: any) => c.key !== 'rowNo' && c.key !== 'action')
  const headers = exportCols.map((c: any) => c.title)
  const rows = tableData.value.map(row =>
    exportCols.map((col: any) => {
      const val = row[col.field]
      if (val === null || val === undefined) return ''
      return String(val)
    })
  )
  if (key === 'csv') {
    const csv = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `以销定购_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } else {
    const tsv = [headers.join('\t'), ...rows.map(r => r.join('\t'))].join('\n')
    const blob = new Blob(['\uFEFF' + tsv], { type: 'application/vnd.ms-excel;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `以销定购_${dayjs().format('YYYYMMDD_HHmmss')}.xls`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  }
}

// ═══ 打印 ═══
function handlePrint() {
  window.print()
}

// ═══ 采购成品/采购原料（行级二次确认） ═══
function handlePurchase(mode: 'FINISHED' | 'MATERIAL', record: any) {
  Modal.confirm({
    title: '确认信息',
    content: '此操作将自动按商品供应商生成采购订单并提交，是否确认？',
    okText: '确定(Enter)',
    cancelText: '取消(Esc)',
    onOk: async () => {
      const url = mode === 'FINISHED'
        ? `/erp/purchase/sales-driven/${record.id}/purchase-finished`
        : `/erp/purchase/sales-driven/${record.id}/purchase-material`
      try {
        const res: any = await request.post(url)
        const orderNos = res?.purchaseOrderNos || []
        message.success(`已生成采购订单 ${orderNos.length} 张${orderNos.length ? '：' + orderNos.join(',') : ''}`)
        reload()
        clearSelection()
      } catch (e: any) {
        console.warn('[以销定购] 生成采购订单失败', e)
      }
    },
  })
}

// ═══ 批量采购成品/采购原料 ═══
function handleBatchPurchase(mode: 'FINISHED' | 'MATERIAL') {
  const pending = selectedRows.value.filter(r => !r.hasPurchased)
  if (pending.length === 0) {
    message.warning('请勾选未采购的销售订单')
    return
  }
  const label = mode === 'FINISHED' ? '采购成品' : '采购原料'
  Modal.confirm({
    title: `确认批量${label}`,
    content: `将按供应商自动生成采购订单并提交，共 ${pending.length} 张销售单，确认继续？`,
    okText: '确认生成',
    cancelText: '取消',
    onOk: async () => {
      batchActing.value = true
      let success = 0
      let fail = 0
      const orderNos: string[] = []
      for (const row of pending) {
        const url = mode === 'FINISHED'
          ? `/erp/purchase/sales-driven/${row.id}/purchase-finished`
          : `/erp/purchase/sales-driven/${row.id}/purchase-material`
        try {
          const res: any = await request.post(url)
          success++
          if (res?.purchaseOrderNos) orderNos.push(...res.purchaseOrderNos)
        } catch (e: any) {
          console.warn('[以销定购] 批量生成失败', e)
          fail++
        }
      }
      batchActing.value = false
      if (fail > 0) {
        message.warning(`已生成 ${success} 单，${fail} 单失败`)
      } else {
        message.success(`已生成 ${success} 张采购订单${orderNos.length ? '：' + orderNos.join(',') : ''}`)
      }
      clearSelection()
      reload()
    },
  })
}

// ═══ 错误处理 ═══
const handleError = (e: Error) => {
  hasError.value = true
  console.error(e)
}

onMounted(() => {
  restoreSavedPageConfig()
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
