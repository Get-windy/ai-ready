<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <DocCenterLayout
        v-model:active-main-tab="mainTab"
        v-model:active-sub-tab="subTab"
        v-model:date-shortcut="dateShortcut"
        v-model:date-range="dateRange"
        v-model:search-values="searchForm"
        v-model:page-current="paginationConfig.current"
        v-model:page-size="paginationConfig.pageSize"
        :main-tabs="mainTabs"
        :sub-tabs="subTabs"
        :search-config="searchConfig"
        :search-checkbox-config="searchCheckboxConfig"
        :hidden-field-keys="hiddenSearchFieldKeys"
        :stat-card-config="statCardConfig"
        :fulfillment-config="fulfillmentConfig"
        :toolbar-config="toolbarConfig"
        :chart-config="chartConfig"
        :stats-data="stats"
        :fulfillment-data="fulfillmentStats"
        :fulfillment-rate="fulfillmentRate"
        :fulfillment-revenue="fulfillmentRevenue"
        :show-pagination="showPagination"
        :page-total="paginationConfig.total"
        @search="handleSearch"
        @refresh="fetchData"
        @toolbar-action="handleToolbarAction"
        @page-change="handlePageChange"
        @time-mode-change="timeChartMode = $event"
      >
        <!-- 图表区域 -->
        <template #chart>
          <div ref="chartContainerRef" class="chart-container" :style="{ height: (chartConfig.height || 200) + 'px' }"></div>
        </template>

        <!-- 表格区域 -->
        <template #table>
          <BillDetailTable
            :columns="activeColumns"
            :data-source="activeTableData"
            :loading="loading"
            :view-mode="true"
            :summary-columns="timeSummaryColumns"
            :storage-key="'sale-order-columns'"
            style="height: 100%"
            @checkbox-change="handleCheckboxChange"
            @checkbox-all="handleCheckboxAll"
          >
            <!-- 操作列 (byDoc / fulfillment) -->
            <template #actionCell="{ record }">
              <template v-if="mainTab === 'all' && subTab === 'byDoc'">
                <a-space :size="4">
                  <a-button v-if="record.status >= 4" type="link" size="small" @click="handlePayment(record)">收款</a-button>
                  <a-button type="link" size="small" @click="handleCancel(record)">取消</a-button>
                  <a-button type="link" size="small" @click="goEdit(record.id)">修改</a-button>
                  <a-dropdown>
                    <a-button type="link" size="small">更多<DownOutlined /></a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item @click="goEdit(record.id)">详情</a-menu-item>
                        <a-menu-item @click="handlePrint(record)">打印</a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
              <template v-else-if="subTab === 'fulfillment'">
                <a-button type="link" size="small" @click="goEdit(record.id)">详情</a-button>
              </template>
            </template>
            <!-- 单据编号列 -->
            <template #orderNoCell="{ record }">
              <a-button type="link" size="small" @click="goEdit(record.id)">{{ record.orderNo }}</a-button>
            </template>
            <!-- 状态列 -->
            <template #statusCell="{ record }">
              <template v-if="mainTab === 'all' && subTab === 'byDoc'">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <template v-else-if="mainTab === 'pendingReview'">
                <a-tag color="orange">待审核</a-tag>
              </template>
              <template v-else>
                <a-tag color="orange">待发货</a-tag>
              </template>
            </template>
            <!-- 金额列 (byDoc) -->
            <template #billAmountCell="{ record }">
              <span class="text-red">{{ formatMoney(record.billAmount) }}</span>
            </template>
            <template #productAmountCell="{ record }">
              {{ formatMoney(record.productAmount) }}
            </template>
            <template #settledAmountCell="{ record }">
              {{ formatMoney(record.settledAmount) }}
            </template>
            <template #returnAmountCell="{ record }">
              {{ formatMoney(record.returnAmount) }}
            </template>
            <!-- 待审核操作列 -->
            <template #pendingReviewActionCell="{ record }">
              <a-space :size="4">
                <a-button type="link" size="small" @click="handleApprove(record)">审核</a-button>
                <a-button type="link" size="small" danger @click="handleReject(record)">拒绝</a-button>
                <a-button type="link" size="small" @click="goEdit(record.id)">详情</a-button>
              </a-space>
            </template>
            <!-- 拣货/发货操作列 -->
            <template #pickingShippingActionCell="{ record }">
              <a-space :size="4">
                <a-button type="link" size="small" @click="handleCancel(record)">取消</a-button>
                <a-button type="link" size="small" @click="handlePickShip(record)">拣完发货</a-button>
                <a-dropdown>
                  <a-button type="link" size="small">更多<DownOutlined /></a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item @click="goEdit(record.id)">详情</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </a-space>
            </template>
            <!-- 线路钻取 (byRoute) -->
            <template #deliveryRouteCell="{ record }">
              <a-button type="link" size="small" @click="drillDownByRoute(record.deliveryRoute)">
                {{ record.deliveryRoute }}
              </a-button>
            </template>
            <!-- 客户钻取 (byCustomer) -->
            <template #customerNameCell="{ record }">
              <a-button type="link" size="small" @click="drillDownByCustomer(record.customerName)">
                {{ record.customerName }}
              </a-button>
            </template>
          </BillDetailTable>
        </template>
      </DocCenterLayout>

      <!-- ═══ 页面配置弹窗 ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
      <!-- ═══ 批量打印弹窗 ═══ -->
      <a-modal
        v-model:open="showPrintDialog"
        title="批量打印"
        width="400px"
        :mask-closable="false"
        @ok="confirmPrint"
      >
        <div style="margin-bottom:12px;color:#888;font-size:12px">
          已选择 {{ selectedRows.length }} 条订单
        </div>
        <a-form layout="vertical">
          <a-form-item label="打印模板">
            <a-select v-model:value="printTemplate">
              <a-select-option value="default">默认模板</a-select-option>
              <a-select-option value="detail">详细模板</a-select-option>
              <a-select-option value="compact">紧凑模板</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-modal>
      <!-- ═══ 物流备注弹窗 ═══ -->
      <a-modal
        v-model:open="showLogisticsRemarkModal"
        title="物流备注"
        width="480px"
        :mask-closable="false"
        @ok="confirmLogisticsRemark"
      >
        <div style="margin-bottom:8px;color:#888;font-size:12px">
          已选择 {{ selectedRows.length }} 条订单
        </div>
        <a-textarea
          v-model:value="logisticsRemarkText"
          placeholder="请输入物流备注内容..."
          :rows="4"
          :maxlength="500"
          show-count
        />
      </a-modal>
      <!-- ═══ 收款弹窗 ═══ -->
      <a-modal
        v-model:open="paymentRecordVisible"
        title="收款"
        width="400px"
        :mask-closable="false"
        @ok="confirmPayment"
      >
        <a-form layout="vertical">
          <a-form-item label="收款金额">
            <a-input-number
              v-model:value="paymentAmount"
              :precision="2"
              :min="0.01"
              style="width: 100%"
              placeholder="请输入收款金额"
            />
          </a-form-item>
        </a-form>
      </a-modal>
      <!-- ═══ 隐藏文件上传(批量导入) ═══ -->
      <input
        ref="fileInputRef"
        type="file"
        accept=".xlsx,.xls"
        style="display: none"
        @change="handleFileChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, DownOutlined, SearchOutlined, ReloadOutlined,
  PrinterOutlined, BarChartOutlined, CheckOutlined,
} from '@ant-design/icons-vue'
import dayjs, { type Dayjs } from 'dayjs'
import * as echarts from 'echarts'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { DocCenterLayout } from '@/components/DocCenterLayout'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type {
  DocMainTab, DocSubTab, SearchConfigMap, SearchCheckboxConfigMap,
  StatCardConfigMap, FulfillmentConfigMap, ToolbarConfigMap,
  ChartConfig,
} from '@/components/DocCenterLayout'
import { saleOrderApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'

// ── 状态管理 ──
const router = useRouter()
const userStore = useUserStore()
const tenantId = computed(() => userStore?.tenantId || 1)

// ═══ Tab状态 ═══
const mainTab = ref('all')
const subTab = ref('byDoc')
const dateShortcut = ref('thisWeek2')
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])
const timeChartMode = ref('day')

// ═══ 搜索表单 ═══
const searchForm = reactive({
  orderNo: '', customerName: '', salesmanName: '', deliveryRoute: '',
  supplementType: '', productName: '', status: '' as any, settlementMethod: '',
  warehouseName: '', region: '', promoterName: '', deptName: '',
  // 高级搜索字段（byDoc/pendingReview/fillfillment 使用）
  brand: '', industryCategory: '', generationMethod: '', orderRemark: '',
  detailRemark: '', isGift: '' as any,
  creatorName: '', auditorName: '', submitterName: '',
  extNum1: '' as any, extNum2: '' as any,
  extText1: '', extText2: '', extText3: '',
  footerExtText1: '', footerExtText2: '',
  receiverName: '', receiverPhone: '',
  shippingAddress: '', logisticsCompany: '', waybillNo: '',
  saleType: '' as any, itemProperty: '',
  deliveryMethod: '', deliveryDriver: '', deliveryVehicle: '',
  buyerRemark: '', sellerRemark: '', summary: '',
  printCount: '' as any,
  minAmount: '' as any, maxAmount: '' as any,
  productAmount: '' as any, couponAmount: '' as any, directDiscount: '' as any,
  orderDate: '', customerTicket: '', customerRemark: '',
  source: '', submitTime: '', auditTime: '',
  shipDateStart: '', shipDateEnd: '',
  supplementStatus: '', remainingUnshippedAmount: '',
  thirdPartyOrderNo: '',
  // 复选框字段
  showDetail: false, hideReturnRelated: false, showSelected: false,
})

// 搜索表单字段 → 后端 API 参数名映射
const SEARCH_FIELD_MAP: Record<string, string> = {
  orderNo: 'orderNo', customerName: 'customerName', salesmanName: 'salesmanName',
  deliveryRoute: 'deliveryRoute', supplementType: 'supplementType',
  productName: 'productName', status: 'status', settlementMethod: 'settlementMethod',
  warehouseName: 'warehouseName', region: 'region', promoterName: 'promoterName',
  deptName: 'deptName', brand: 'productBrand', industryCategory: 'industryCategory',
  generationMethod: 'generationMethod', orderRemark: 'orderRemark',
  detailRemark: 'detailRemark', isGift: 'isGift',
  creatorName: 'creatorName', auditorName: 'auditorName', submitterName: 'submitterName',
  extNum1: 'extNum1', extNum2: 'extNum2',
  extText1: 'extText1', extText2: 'extText2', extText3: 'extText3',
  footerExtText1: 'footerExtText1', footerExtText2: 'footerExtText2',
  receiverName: 'receiverName', receiverPhone: 'receiverPhone',
  shippingAddress: 'shippingAddress', logisticsCompany: 'logisticsCompany',
  waybillNo: 'waybillNo', saleType: 'saleType', itemProperty: 'itemProperty',
  deliveryMethod: 'deliveryMethod', deliveryDriver: 'driverName',
  deliveryVehicle: 'deliveryVehicle', buyerRemark: 'buyerRemark',
  sellerRemark: 'orderRemark', summary: 'summary',
  printCount: 'printCount',
  minAmount: 'minAmount', maxAmount: 'maxAmount',
  productAmount: 'productAmount', couponAmount: 'couponAmount', directDiscount: 'directDiscount',
  orderDate: 'orderDate', customerTicket: 'customerTicket', customerRemark: 'customerRemark',
  source: 'orderSource', submitTime: 'submitTime', auditTime: 'auditTime',
  shipDateStart: 'shipDateStart', shipDateEnd: 'shipDateEnd',
  supplementStatus: 'supplementStatus', remainingUnshippedAmount: 'remainingUnshippedAmount',
  thirdPartyOrderNo: 'thirdPartyOrderNo',
}

// 需要转为数字的字段
const NUMERIC_SEARCH_FIELDS = new Set(['status', 'extNum1', 'extNum2', 'printCount', 'minAmount', 'maxAmount', 'saleType'])

/**
 * 从 searchForm 构建 API 请求参数
 * 自动过滤隐藏字段和空值，并按 SEARCH_FIELD_MAP 映射参数名
 */
function buildSearchParams(): Record<string, any> {
  const params: Record<string, any> = {}
  const hiddenSet = new Set(hiddenSearchFieldKeys.value)
  for (const [formKey, apiKey] of Object.entries(SEARCH_FIELD_MAP)) {
    if (hiddenSet.has(formKey)) continue
    const raw = (searchForm as any)[formKey]
    if (raw === undefined || raw === null || raw === '') continue
    params[apiKey] = NUMERIC_SEARCH_FIELDS.has(formKey) ? Number(raw) : String(raw)
  }
  return params
}

// 复选框字段不参与 SEARCH_FIELD_MAP 映射，需单独处理
function buildCheckboxParams(): Record<string, any> {
  const params: Record<string, any> = {}
  if (searchForm.showDetail) params.showDetail = true
  if (searchForm.hideReturnRelated) params.hideReturnRelated = true
  if (searchForm.showSelected) {
    params.showSelected = true
    if (selectedRowKeys.value.length > 0) {
      params.selectedIds = selectedRowKeys.value.join(',')
    }
  }
  return params
}

// ═══ 搜索字段隐藏配置（由 PageConfigPanel 控制） ═══
const hiddenSearchFieldKeys = ref<string[]>([])

// ═══ 加载 & 数据 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const timeGroupData = ref<any[]>([])
const routeGroupData = ref<any[]>([])
const customerGroupData = ref<any[]>([])

// ═══ 选中行(批量操作) ═══
const selectedRowKeys = ref<string[]>([])
const selectedRows = ref<any[]>([])

// ═══ ECharts ═══
const chartContainerRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// ═══ 页面配置弹窗 ═══
const showPageConfig = ref(false)

function handlePageConfigChange(config: any) {
  // 页面配置变更：根据查询条件 tab 的 visible 字段控制搜索字段显隐
  if (config?.queryFields && Array.isArray(config.queryFields)) {
    hiddenSearchFieldKeys.value = config.queryFields
      .filter((f: any) => f.visible === false)
      .map((f: any) => f.key)
  }
}

// ═══ 分页 ═══
const paginationConfig = reactive({
  current: 1, pageSize: 20, total: 0,
  showSizeChanger: true, showQuickJumper: true,
  pageSizeOptions: ['20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条记录`,
})

// ═══ 统计卡片 ═══
const stats = reactive({
  totalOrders: 0, pendingOutbound: 0, pendingShip: 0,
  outboundCount: 0, shippedCount: 0, completedCount: 0,
})

// ═══ 履约统计 ═══
const fulfillmentStats = reactive({
  unfulfilledOrders: 0,
  unsupplementedOrders: 0,
  supplementingOrders: 0,
  terminatedOrders: 0,
  completedFulfillment: 0,
})
const fulfillmentRate = ref(0)
const fulfillmentRevenue = ref(0)

// ═══ 状态映射 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审核', color: 'orange' },
  2: { text: '待发货', color: 'processing' },
  3: { text: '部分发货', color: 'warning' },
  4: { text: '发货完成', color: 'success' },
  5: { text: '交易完成', color: 'green' },
  6: { text: '已取消', color: 'default' },
}

function getStatusText(s: number) { return STATUS_MAP[s]?.text || '未知' }
function getStatusColor(s: number) { return STATUS_MAP[s]?.color || 'default' }
function formatMoney(v: any) {
  if (!v) return '0'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

// ════════════════════════════════════════════════
// DocCenterLayout 配置
// ════════════════════════════════════════════════

const mainTabs: DocMainTab[] = [
  { key: 'all', label: '全部' },
  { key: 'pendingReview', label: '1.待审核' },
  { key: 'pickingShipping', label: '2.拣货/发货' },
]

const subTabs: DocSubTab[] = [
  { key: 'byDoc', label: '按单据' },
  { key: 'byTime', label: '按时间' },
  { key: 'byRoute', label: '按线路' },
  { key: 'byCustomer', label: '按客户' },
  { key: 'fulfillment', label: '订单履约' },
]

// 搜索字段配置
const searchConfig: SearchConfigMap = {
  // 全部 > 按单据 / 按线路 / 按客户
  'all.byDoc': [
    // ── 基础搜索字段（第一排，始终显示） ──
    { key: 'orderDate', label: '单据日期', type: 'input', span: 3 },
    { key: 'orderNo', label: '单据编号', type: 'input', span: 3, suffix: 'search' },
    { key: 'deliveryRoute', label: '配送线路', type: 'input', span: 3, suffix: 'search' },
    { key: 'customerName', label: '客户', type: 'input', span: 3, suffix: 'search' },
    { key: 'salesmanName', label: '经手人', type: 'input', span: 3, suffix: 'search' },
    { key: 'status', label: '单据状态', type: 'select', span: 3, options: [
      { label: '全部', value: '' },
      { label: '草稿', value: 0 },
      { label: '待审核', value: 1 },
      { label: '待发货', value: 2 },
      { label: '部分发货', value: 3 },
      { label: '发货完成', value: 4 },
      { label: '交易完成', value: 5 },
    ]},
    { key: 'settlementMethod', label: '结款方式', type: 'select', span: 3, options: [{ label: '全部', value: '' }] },
    { key: 'supplementType', label: '补单类型', type: 'select', span: 3, options: [{ label: '全部', value: '' }] },
    { key: 'submitTime', label: '提交时间', type: 'input', span: 3, suffix: 'search' },
    { key: 'productName', label: '商品', type: 'input', span: 3, suffix: 'search' },

    // ── 高级搜索字段（展开后显示） ──
    { key: 'brand', label: '品牌', type: 'input', span: 3, suffix: 'search' },
    { key: 'industryCategory', label: '所属行业类别', type: 'input', span: 3, suffix: 'search' },
    { key: 'deptName', label: '部门', type: 'input', span: 3, suffix: 'search' },
    { key: 'warehouseName', label: '仓库', type: 'input', span: 3, suffix: 'search' },
    { key: 'generationMethod', label: '产生方式', type: 'input', span: 3, suffix: 'search' },
    { key: 'orderRemark', label: '单据备注', type: 'input', span: 3, suffix: 'search' },
    { key: 'detailRemark', label: '明细备注', type: 'input', span: 3, suffix: 'search' },
    { key: 'isGift', label: '是否赠品', type: 'input', span: 3, suffix: 'search' },
    { key: 'creatorName', label: '制单人', type: 'input', span: 3, suffix: 'search' },
    { key: 'auditorName', label: '审核人', type: 'input', span: 3, suffix: 'search' },
    { key: 'submitterName', label: '提交人', type: 'input', span: 3, suffix: 'search' },
    { key: 'extNum1', label: '表头自定义字段1(数字)', type: 'input', span: 3, suffix: 'search' },
    { key: 'extNum2', label: '表头自定义字段2(数字)', type: 'input', span: 3, suffix: 'search' },
    { key: 'extText1', label: '表头自定义字段3(文本)', type: 'input', span: 3, suffix: 'search' },
    { key: 'extText2', label: '表头自定义字段4(文本)', type: 'input', span: 3, suffix: 'search' },
    { key: 'extText3', label: '表头自定义字段5(文本)', type: 'input', span: 3, suffix: 'search' },
    { key: 'footerExtText1', label: '表尾自定义1(文本)', type: 'input', span: 3, suffix: 'search' },
    { key: 'footerExtText2', label: '表尾自定义2(文本)', type: 'input', span: 3, suffix: 'search' },
    { key: 'shipDateStart', label: '发货日期(起)', type: 'input', span: 3, suffix: 'search' },
    { key: 'shipDateEnd', label: '发货日期(止)', type: 'input', span: 3, suffix: 'search' },
    { key: 'receiverName', label: '收货人', type: 'input', span: 3, suffix: 'search' },
    { key: 'receiverPhone', label: '联系电话', type: 'input', span: 3, suffix: 'search' },
    { key: 'shippingAddress', label: '收货地址', type: 'input', span: 3, suffix: 'search' },
    { key: 'logisticsCompany', label: '物流公司', type: 'input', span: 3, suffix: 'search' },
    { key: 'waybillNo', label: '运单号', type: 'input', span: 3, suffix: 'search' },
    { key: 'region', label: '区域', type: 'input', span: 3, suffix: 'search' },
    { key: 'saleType', label: '销售类型', type: 'input', span: 3, suffix: 'search' },
    { key: 'itemProperty', label: '商品行属性', type: 'input', span: 3, suffix: 'search' },
    { key: 'deliveryMethod', label: '配送方式', type: 'input', span: 3, suffix: 'search' },
    { key: 'deliveryDriver', label: '配送司机', type: 'input', span: 3, suffix: 'search' },
    { key: 'deliveryVehicle', label: '配送车辆', type: 'input', span: 3, suffix: 'search' },
    { key: 'sellerRemark', label: '卖家备注', type: 'input', span: 3, suffix: 'search' },
    { key: 'buyerRemark', label: '买家备注', type: 'input', span: 3, suffix: 'search' },
    { key: 'summary', label: '摘要', type: 'input', span: 3, suffix: 'search' },
    { key: 'printCount', label: '打印次数', type: 'input', span: 3, suffix: 'search' },
    { key: 'minAmount', label: '本单金额最小', type: 'input', span: 3, suffix: 'search' },
    { key: 'maxAmount', label: '本单金额最大', type: 'input', span: 3, suffix: 'search' },
    { key: 'auditTime', label: '审核时间', type: 'input', span: 3, suffix: 'search' },
    { key: 'source', label: '来源', type: 'input', span: 3, suffix: 'search' },
  ],
  // 全部 > 按时间（简化搜索，无页面配置按钮）
  'all.byTime': [
    { key: 'customerName', label: '客户', type: 'input', span: 4, suffix: 'search' },
    { key: 'salesmanName', label: '经手人', type: 'input', span: 4, suffix: 'search' },
    { key: 'warehouseName', label: '仓库', type: 'input', span: 4, suffix: 'search' },
    { key: 'region', label: '区域', type: 'input', span: 4, suffix: 'search' },
    { key: 'promoterName', label: '推广人', type: 'input', span: 4, suffix: 'search' },
    { key: 'deptName', label: '部门', type: 'input', span: 4, suffix: 'search' },
  ],
  // 全部 > 按线路（简化搜索，无页面配置按钮）
  'all.byRoute': [
    { key: 'customerName', label: '客户', type: 'input', span: 4, suffix: 'search' },
    { key: 'salesmanName', label: '经手人', type: 'input', span: 4, suffix: 'search' },
    { key: 'warehouseName', label: '仓库', type: 'input', span: 4, suffix: 'search' },
    { key: 'region', label: '区域', type: 'input', span: 4, suffix: 'search' },
    { key: 'promoterName', label: '推广人', type: 'input', span: 4, suffix: 'search' },
    { key: 'deptName', label: '部门', type: 'input', span: 4, suffix: 'search' },
  ],
  // 全部 > 按客户（简化搜索，无页面配置按钮）
  'all.byCustomer': [
    { key: 'customerName', label: '客户', type: 'input', span: 4, suffix: 'search' },
    { key: 'salesmanName', label: '经手人', type: 'input', span: 4, suffix: 'search' },
    { key: 'warehouseName', label: '仓库', type: 'input', span: 4, suffix: 'search' },
    { key: 'region', label: '区域', type: 'input', span: 4, suffix: 'search' },
    { key: 'promoterName', label: '推广人', type: 'input', span: 4, suffix: 'search' },
    { key: 'deptName', label: '部门', type: 'input', span: 4, suffix: 'search' },
  ],
  // 全部 > 订单履约 — 严格按文档第62行字段列表
  'all.fulfillment': [
    { key: 'orderDate', label: '单据日期', type: 'input', span: 3 },
    { key: 'orderNo', label: '单据编号', type: 'input', span: 3, suffix: 'search' },
    { key: 'deliveryRoute', label: '配送线路', type: 'input', span: 3, suffix: 'search' },
    { key: 'customerName', label: '客户', type: 'input', span: 3, suffix: 'search' },
    { key: 'salesmanName', label: '经手人', type: 'input', span: 3, suffix: 'search' },
    { key: 'status', label: '单据状态', type: 'select', span: 3, options: [
      { label: '全部', value: '' }, { label: '待审核', value: 1 }, { label: '待发货', value: 2 },
      { label: '部分发货', value: 3 }, { label: '发货完成', value: 4 }, { label: '交易完成', value: 5 },
    ]},
    { key: 'submitTime', label: '提交时间', type: 'input', span: 3, suffix: 'search' },
    { key: 'productName', label: '商品', type: 'input', span: 3, suffix: 'search' },
    { key: 'deptName', label: '部门', type: 'input', span: 3, suffix: 'search' },
    { key: 'warehouseName', label: '仓库', type: 'input', span: 3, suffix: 'search' },
    { key: 'generationMethod', label: '产生方式', type: 'input', span: 3, suffix: 'search' },
    { key: 'orderRemark', label: '单据备注', type: 'input', span: 3, suffix: 'search' },
    { key: 'detailRemark', label: '明细备注', type: 'input', span: 3, suffix: 'search' },
    { key: 'isGift', label: '是否赠品', type: 'input', span: 3, suffix: 'search' },
    { key: 'creatorName', label: '制单人', type: 'input', span: 3, suffix: 'search' },
    { key: 'auditorName', label: '审核人', type: 'input', span: 3, suffix: 'search' },
    { key: 'submitterName', label: '提交人', type: 'input', span: 3, suffix: 'search' },
    { key: 'extNum1', label: '表头自定义字段1(数字)', type: 'input', span: 3, suffix: 'search' },
    { key: 'extNum2', label: '表头自定义字段2(数字)', type: 'input', span: 3, suffix: 'search' },
    { key: 'extText1', label: '表头自定义字段3(文本)', type: 'input', span: 3, suffix: 'search' },
    { key: 'extText2', label: '表头自定义字段4(文本)', type: 'input', span: 3, suffix: 'search' },
    { key: 'extText3', label: '表头自定义字段5(文本)', type: 'input', span: 3, suffix: 'search' },
    { key: 'footerExtText1', label: '表尾自定义1(文本)', type: 'input', span: 3, suffix: 'search' },
    { key: 'footerExtText2', label: '表尾自定义2(文本)', type: 'input', span: 3, suffix: 'search' },
    { key: 'shipDateStart', label: '发货日期(起)', type: 'input', span: 3, suffix: 'search' },
    { key: 'shipDateEnd', label: '发货日期(止)', type: 'input', span: 3, suffix: 'search' },
    { key: 'receiverName', label: '收货人', type: 'input', span: 3, suffix: 'search' },
    { key: 'receiverPhone', label: '联系电话', type: 'input', span: 3, suffix: 'search' },
    { key: 'shippingAddress', label: '收货地址', type: 'input', span: 3, suffix: 'search' },
    { key: 'logisticsCompany', label: '物流公司', type: 'input', span: 3, suffix: 'search' },
    { key: 'waybillNo', label: '运单号', type: 'input', span: 3, suffix: 'search' },
    { key: 'region', label: '区域', type: 'input', span: 3, suffix: 'search' },
    { key: 'saleType', label: '销售类型', type: 'input', span: 3, suffix: 'search' },
    { key: 'itemProperty', label: '商品行属性', type: 'input', span: 3, suffix: 'search' },
    { key: 'deliveryMethod', label: '配送方式', type: 'input', span: 3, suffix: 'search' },
    { key: 'deliveryDriver', label: '配送司机', type: 'input', span: 3, suffix: 'search' },
    { key: 'deliveryVehicle', label: '配送车辆', type: 'input', span: 3, suffix: 'search' },
    { key: 'sellerRemark', label: '卖家备注', type: 'input', span: 3, suffix: 'search' },
    { key: 'buyerRemark', label: '买家备注', type: 'input', span: 3, suffix: 'search' },
    { key: 'printCount', label: '打印次数', type: 'input', span: 3, suffix: 'search' },
    { key: 'minAmount', label: '本单金额最小', type: 'input', span: 3, suffix: 'search' },
    { key: 'maxAmount', label: '本单金额最大', type: 'input', span: 3, suffix: 'search' },
    { key: 'supplementStatus', label: '补单状态', type: 'input', span: 3, suffix: 'search' },
    { key: 'remainingUnshippedAmount', label: '订单未补金额', type: 'input', span: 3, suffix: 'search' },
    { key: 'auditTime', label: '审核时间', type: 'input', span: 3, suffix: 'search' },
  ],
  // 待审核 — 严格按文档第66行字段列表
  pendingReview: [
    { key: 'orderDate', label: '单据日期', type: 'input', span: 4 },
    { key: 'orderNo', label: '单据编号', type: 'input', span: 4, suffix: 'search' },
    { key: 'deliveryRoute', label: '配送线路', type: 'input', span: 4, suffix: 'search' },
    { key: 'customerName', label: '客户', type: 'input', span: 4, suffix: 'search' },
    { key: 'salesmanName', label: '经手人', type: 'input', span: 4, suffix: 'search' },
    { key: 'settlementMethod', label: '结款方式', type: 'select', span: 4, options: [{ label: '全部', value: '' }] },
    { key: 'submitTime', label: '提交时间', type: 'input', span: 4, suffix: 'search' },
    { key: 'productName', label: '商品', type: 'input', span: 4, suffix: 'search' },
    { key: 'brand', label: '品牌', type: 'input', span: 4, suffix: 'search' },
    { key: 'industryCategory', label: '所属行业类别', type: 'input', span: 4, suffix: 'search' },
    { key: 'deptName', label: '部门', type: 'input', span: 4, suffix: 'search' },
    { key: 'warehouseName', label: '仓库', type: 'input', span: 4, suffix: 'search' },
    { key: 'generationMethod', label: '产生方式', type: 'input', span: 4, suffix: 'search' },
    { key: 'orderRemark', label: '单据备注', type: 'input', span: 4, suffix: 'search' },
    { key: 'detailRemark', label: '明细备注', type: 'input', span: 4, suffix: 'search' },
    { key: 'isGift', label: '是否赠品', type: 'input', span: 4, suffix: 'search' },
    { key: 'creatorName', label: '制单人', type: 'input', span: 4, suffix: 'search' },
    { key: 'auditorName', label: '审核人', type: 'input', span: 4, suffix: 'search' },
    { key: 'submitterName', label: '提交人', type: 'input', span: 4, suffix: 'search' },
    { key: 'extNum1', label: '表头自定义字段1(数字)', type: 'input', span: 4, suffix: 'search' },
    { key: 'extNum2', label: '表头自定义字段2(数字)', type: 'input', span: 4, suffix: 'search' },
    { key: 'extText1', label: '表头自定义字段3(文本)', type: 'input', span: 4, suffix: 'search' },
    { key: 'extText2', label: '表头自定义字段4(文本)', type: 'input', span: 4, suffix: 'search' },
    { key: 'extText3', label: '表头自定义字段5(文本)', type: 'input', span: 4, suffix: 'search' },
    { key: 'footerExtText1', label: '表尾自定义1(文本)', type: 'input', span: 4, suffix: 'search' },
    { key: 'footerExtText2', label: '表尾自定义2(文本)', type: 'input', span: 4, suffix: 'search' },
    { key: 'shipDateStart', label: '发货日期(起)', type: 'input', span: 4, suffix: 'search' },
    { key: 'shipDateEnd', label: '发货日期(止)', type: 'input', span: 4, suffix: 'search' },
    { key: 'receiverName', label: '收货人', type: 'input', span: 4, suffix: 'search' },
    { key: 'receiverPhone', label: '联系电话', type: 'input', span: 4, suffix: 'search' },
    { key: 'shippingAddress', label: '收货地址', type: 'input', span: 4, suffix: 'search' },
    { key: 'logisticsCompany', label: '物流公司', type: 'input', span: 4, suffix: 'search' },
    { key: 'waybillNo', label: '运单号', type: 'input', span: 4, suffix: 'search' },
    { key: 'region', label: '区域', type: 'input', span: 4, suffix: 'search' },
    { key: 'saleType', label: '销售类型', type: 'input', span: 4, suffix: 'search' },
    { key: 'itemProperty', label: '商品行属性', type: 'input', span: 4, suffix: 'search' },
    { key: 'deliveryMethod', label: '配送方式', type: 'input', span: 4, suffix: 'search' },
    { key: 'deliveryDriver', label: '配送司机', type: 'input', span: 4, suffix: 'search' },
    { key: 'deliveryVehicle', label: '配送车辆', type: 'input', span: 4, suffix: 'search' },
    { key: 'sellerRemark', label: '卖家备注', type: 'input', span: 4, suffix: 'search' },
    { key: 'buyerRemark', label: '买家备注', type: 'input', span: 4, suffix: 'search' },
    { key: 'summary', label: '摘要', type: 'input', span: 4, suffix: 'search' },
    { key: 'printCount', label: '打印次数', type: 'input', span: 4, suffix: 'search' },
    { key: 'minAmount', label: '本单金额最小', type: 'input', span: 4, suffix: 'search' },
    { key: 'maxAmount', label: '本单金额最大', type: 'input', span: 4, suffix: 'search' },
    { key: 'auditTime', label: '审核时间', type: 'input', span: 4, suffix: 'search' },
    { key: 'source', label: '来源', type: 'input', span: 4, suffix: 'search' },
  ],
  // 拣货/发货（文档指定 13 个搜索字段）
  pickingShipping: [
    { key: 'orderDate', label: '单据日期', type: 'input', span: 4 },
    { key: 'orderNo', label: '单据编号', type: 'input', span: 4 },
    { key: 'customerName', label: '客户', type: 'input', span: 4, suffix: 'search' },
    { key: 'salesmanName', label: '经手人', type: 'input', span: 4, suffix: 'search' },
    { key: 'submitTime', label: '提交时间', type: 'input', span: 4, suffix: 'search' },
    { key: 'warehouseName', label: '仓库', type: 'input', span: 4, suffix: 'search' },
    { key: 'extNum1', label: '表头自定义字段1(数字)', type: 'input', span: 4, suffix: 'search' },
    { key: 'extNum2', label: '表头自定义字段2(数字)', type: 'input', span: 4, suffix: 'search' },
    { key: 'extText1', label: '表头自定义字段3(文本)', type: 'input', span: 4, suffix: 'search' },
    { key: 'extText2', label: '表头自定义字段4(文本)', type: 'input', span: 4, suffix: 'search' },
    { key: 'extText3', label: '表头自定义字段5(文本)', type: 'input', span: 4, suffix: 'search' },
    { key: 'logisticsCompany', label: '物流公司', type: 'input', span: 4, suffix: 'search' },
    { key: 'deliveryMethod', label: '配送方式', type: 'input', span: 4, suffix: 'search' },
    { key: 'deliveryDriver', label: '配送司机', type: 'input', span: 4, suffix: 'search' },
    { key: 'printCount', label: '打印次数', type: 'input', span: 4, suffix: 'search' },
  ],
}

// 搜索复选框配置（byTime/byRoute/byCustomer 无页面配置，不配复选框）
const searchCheckboxConfig: SearchCheckboxConfigMap = {
  'all.byDoc': [
    { key: 'hideReturnRelated', label: '不显示有关联审核中退货申请单的单据' },
    { key: 'showSelected', label: '仅显示已选中' },
  ],
  'all.fulfillment': [
    { key: 'showDetail', label: '按明细显示' },
  ],
  'pendingReview': [
    { key: 'showSelected', label: '仅显示已选中' },
    { key: 'hideReturnRelated', label: '不显示有关联审核中退货申请单的单据' },
  ],
  'pickingShipping': [
    { key: 'showSelected', label: '仅显示已选中' },
  ],
}

// 统计卡片配置（按子Tab分组）
const statCardConfig: StatCardConfigMap = {
  'all.byDoc': [
    { label: '订单总数', valueKey: 'totalOrders', color: '#1890ff' },
    { label: '待出库', valueKey: 'pendingOutbound', color: '#fa8c16' },
    { label: '待发货', valueKey: 'pendingShip', color: '#faad14' },
    { label: '已出库', valueKey: 'outboundCount', color: '#52c41a' },
    { label: '发货完成', valueKey: 'shippedCount', color: '#13c2c2' },
    { label: '交易完成', valueKey: 'completedCount', color: '#722ed1' },
  ],
  'all.byTime': [
    { label: '订单总数', valueKey: 'totalOrders', color: '#1890ff' },
    { label: '待出库', valueKey: 'pendingOutbound', color: '#fa8c16' },
    { label: '待发货', valueKey: 'pendingShip', color: '#faad14' },
    { label: '已出库', valueKey: 'outboundCount', color: '#52c41a' },
    { label: '发货完成', valueKey: 'shippedCount', color: '#13c2c2' },
    { label: '交易完成', valueKey: 'completedCount', color: '#722ed1' },
  ],
  'all.byRoute': [
    { label: '订单总数', valueKey: 'totalOrders', color: '#1890ff' },
    { label: '待出库', valueKey: 'pendingOutbound', color: '#fa8c16' },
    { label: '待发货', valueKey: 'pendingShip', color: '#faad14' },
    { label: '已出库', valueKey: 'outboundCount', color: '#52c41a' },
    { label: '发货完成', valueKey: 'shippedCount', color: '#13c2c2' },
    { label: '交易完成', valueKey: 'completedCount', color: '#722ed1' },
  ],
  'all.byCustomer': [
    { label: '订单总数', valueKey: 'totalOrders', color: '#1890ff' },
    { label: '待出库', valueKey: 'pendingOutbound', color: '#fa8c16' },
    { label: '待发货', valueKey: 'pendingShip', color: '#faad14' },
    { label: '已出库', valueKey: 'outboundCount', color: '#52c41a' },
    { label: '发货完成', valueKey: 'shippedCount', color: '#13c2c2' },
    { label: '交易完成', valueKey: 'completedCount', color: '#722ed1' },
  ],
  'all.fulfillment': [
    { label: '订单总数', valueKey: 'totalOrders', color: '#1890ff' },
    { label: '待出库', valueKey: 'pendingOutbound', color: '#fa8c16' },
    { label: '待发货', valueKey: 'pendingShip', color: '#faad14' },
    { label: '已出库', valueKey: 'outboundCount', color: '#52c41a' },
    { label: '发货完成', valueKey: 'shippedCount', color: '#13c2c2' },
    { label: '交易完成', valueKey: 'completedCount', color: '#722ed1' },
  ],
}

// 履约统计卡片配置
const fulfillmentConfig: FulfillmentConfigMap = {
  'all.fulfillment': [
    { label: '未履约订单总数', valueKey: 'unfulfilledOrders' },
    { label: '未补订单总数', valueKey: 'unsupplementedOrders' },
    { label: '补单中的订单总数', valueKey: 'supplementingOrders' },
    { label: '终止履约订单总数', valueKey: 'terminatedOrders' },
    { label: '已完成履约订单总数', valueKey: 'completedFulfillment' },
  ],
}

// 工具栏按钮配置（按开发文档各 tab 规范对齐）
const toolbarConfig: ToolbarConfigMap = {
  // 全部 > 按单据：新增/刷新/批量打印/商品汇总/批量导入/物流备注/打印(F8)/导出/配置
  'all.byDoc': [
    { key: 'add', label: '新增', type: 'primary', icon: 'PlusOutlined', visibleFor: () => true },
    { key: 'refresh', label: '刷新', icon: 'ReloadOutlined', visibleFor: () => true },
    { key: 'batchPrint', label: '批量打印', icon: 'PrinterOutlined', visibleFor: () => true },
    { key: 'productSummary', label: '商品汇总', icon: 'BarChartOutlined', visibleFor: () => true },
    { key: 'import', label: '批量导入', visibleFor: () => true },
    { key: 'logisticsRemark', label: '物流备注', visibleFor: () => true },
    { key: 'print', label: '打印(F8)', visibleFor: () => true },
    { key: 'export', label: '导出', visibleFor: () => true },
    { key: 'pageConfig', label: '配置', visibleFor: () => true },
  ],
  // 全部 > 按时间/按路线/按客户：刷新/批量打印/导出/配置
  'all.byTime': [
    { key: 'batchPrint', label: '批量打印', icon: 'PrinterOutlined', visibleFor: () => true },
    { key: 'export', label: '导出', visibleFor: () => true },
    { key: 'pageConfig', label: '配置', visibleFor: () => true },
  ],
  'all.byRoute': [
    { key: 'batchPrint', label: '批量打印', icon: 'PrinterOutlined', visibleFor: () => true },
    { key: 'export', label: '导出', visibleFor: () => true },
    { key: 'pageConfig', label: '配置', visibleFor: () => true },
  ],
  'all.byCustomer': [
    { key: 'batchPrint', label: '批量打印', icon: 'PrinterOutlined', visibleFor: () => true },
    { key: 'export', label: '导出', visibleFor: () => true },
    { key: 'pageConfig', label: '配置', visibleFor: () => true },
  ],
  // 全部 > 订单履约：刷新/导出/配置
  'all.fulfillment': [
    { key: 'refresh', label: '刷新', icon: 'ReloadOutlined', visibleFor: () => true },
    { key: 'export', label: '导出', visibleFor: () => true },
    { key: 'pageConfig', label: '配置', visibleFor: () => true },
  ],
  // 待审核：新增/刷新/批量打印/商品汇总/批量导入/物流备注/打印(F8)/导出/配置
  pendingReview: [
    { key: 'add', label: '新增', type: 'primary', icon: 'PlusOutlined', visibleFor: () => true },
    { key: 'refresh', label: '刷新', icon: 'ReloadOutlined', visibleFor: () => true },
    { key: 'batchPrint', label: '批量打印', icon: 'PrinterOutlined', visibleFor: () => true },
    { key: 'productSummary', label: '商品汇总', icon: 'BarChartOutlined', visibleFor: () => true },
    { key: 'import', label: '批量导入', visibleFor: () => true },
    { key: 'logisticsRemark', label: '物流备注', visibleFor: () => true },
    { key: 'print', label: '打印(F8)', visibleFor: () => true },
    { key: 'export', label: '导出', visibleFor: () => true },
    { key: 'pageConfig', label: '配置', visibleFor: () => true },
  ],
  // 拣货/发货：刷新/批量打印/商品汇总/拣完批量发货/物流备注/配置
  pickingShipping: [
    { key: 'refresh', label: '刷新', icon: 'ReloadOutlined', visibleFor: () => true },
    { key: 'batchPrint', label: '批量打印', icon: 'PrinterOutlined', visibleFor: () => true },
    { key: 'productSummary', label: '商品汇总', icon: 'BarChartOutlined', visibleFor: () => true },
    { key: 'pickComplete', label: '拣完批量发货', icon: 'CheckOutlined', visibleFor: () => true },
    { key: 'logisticsRemark', label: '物流备注', visibleFor: () => true },
    { key: 'pageConfig', label: '配置', visibleFor: () => true },
  ],
}

// 图表配置
const chartConfig = computed<ChartConfig>(() => ({
  visible: mainTab.value === 'all' && (subTab.value === 'byTime' || subTab.value === 'byRoute'),
  title: '履约概览',
  height: 200,
  showTimeToggle: subTab.value === 'byTime',
  legend: subTab.value === 'byRoute'
    ? [
        { label: '待处理单数', color: '#ff4d4f' },
        { label: '待出库单数', color: '#fa8c16' },
        { label: '已出库单数', color: '#52c41a' },
      ]
    : undefined,
}))

const chartPlaceholderText = computed(() => {
  if (subTab.value === 'byTime') return `图表区域（按${timeChartMode.value === 'day' ? '天' : timeChartMode.value === 'week' ? '周' : '月'}）`
  return '图表区域（ECharts）'
})

// 分页显示：byTime/byRoute 为分组数据不翻页
const showPagination = computed(() => {
  if (mainTab.value !== 'all') return true
  return subTab.value === 'byDoc' || subTab.value === 'byCustomer' || subTab.value === 'fulfillment'
})

// 当前激活的列配置（根据 mainTab/subTab 切换）
// 列配置（显隐/排序）由 BillDetailTable 内部管理
const activeColumns = computed(() => {
  if (mainTab.value === 'pendingReview') return pendingReviewColumns
  if (mainTab.value === 'pickingShipping') return pickingShippingColumns
  switch (subTab.value) {
    case 'byTime': return byTimeColumns
    case 'byRoute': return byRouteColumns
    case 'byCustomer': return byCustomerColumns
    case 'fulfillment': return fulfillmentColumns
    default: return byDocColumns
  }
})

// 当前激活的表格数据
const activeTableData = computed(() => {
  if (mainTab.value === 'pendingReview' || mainTab.value === 'pickingShipping') {
    let data = [...tableData.value];

    // 为拣货/发货页面计算已拣货数量
    if (mainTab.value === 'pickingShipping') {
      data = data.map((item: any) => {
        // 已拣货数量 = 原始商品项数 - 未发货商品项数
        const originalCount = item.originalItemCount || 0;
        const unshippedCount = item.unshippedItemCount || 0;
        const pickedCount = Math.max(0, originalCount - unshippedCount);

        return {
          ...item,
          calculatedPickedQuantity: pickedCount
        };
      });
    }

    return data;
  }

  switch (subTab.value) {
    case 'byTime': return timeGroupData.value
    case 'byRoute': return routeGroupData.value
    case 'byCustomer': return customerGroupData.value
    default: return tableData.value
  }
})

// 按时间分组的合计行数据
const timeSummaryColumns = computed(() => {
  if (!(mainTab.value === 'all' && subTab.value === 'byTime')) return []
  return [
    { key: 'totalOrders', value: stats.totalOrders || 0, highlight: true },
    { key: 'pendingReviewCount', value: 0 },
    { key: 'pendingOutboundCount', value: stats.pendingOutbound || 0, highlight: true },
    { key: 'outboundCount', value: stats.outboundCount || 0, highlight: true },
    { key: 'shippedCount', value: stats.shippedCount || 0, highlight: true },
    { key: 'completedCount', value: stats.completedCount || 0, highlight: true },
  ]
})

// ════════════════════════════════════════════════
// 列定义（DetailColumnConfig 格式）
// ════════════════════════════════════════════════

const byDocColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', width: 50, type: 'rowNo' },
  { key: 'selection', title: '', width: 40, type: 'checkbox' },
  { key: 'action', title: '操作', width: 140, fixed: 'left', type: 'action', slotName: 'actionCell' },
  { key: 'orderDate', title: '单据日期', width: 110, sortable: true },
  { key: 'orderNo', title: '单据编号', width: 150, type: 'slot', slotName: 'orderNoCell', sortable: true },
  { key: 'status', title: '单据状态', width: 90, type: 'slot', slotName: 'statusCell' },
  { key: 'warehouseName', title: '仓库', width: 100 },
  { key: 'customerName', title: '客户', width: 150 },
  { key: 'settlementMethod', title: '结款方式', width: 80 },
  { key: 'customerCode', title: '客户编号', width: 120 },
  { key: 'customerLevel', title: '客户级别', width: 100 },
  { key: 'customerTicket', title: '客户一票通', width: 100 },
  { key: 'receiverName', title: '收货人', width: 80 },
  { key: 'receiverPhone', title: '联系电话', width: 110 },
  { key: 'shippingAddress', title: '收货地址', width: 150 },
  { key: 'promoterName', title: '推广人', width: 80 },
  { key: 'customerRemark', title: '客户备注', width: 120 },
  { key: 'salesmanName', title: '经手人', width: 80 },
  { key: 'deptName', title: '部门', width: 80 },
  { key: 'billAmount', title: '本单金额', width: 100, align: 'right', type: 'slot', slotName: 'billAmountCell' },
  { key: 'productAmount', title: '金额', width: 100, align: 'right', type: 'slot', slotName: 'productAmountCell' },
  { key: 'promoDiscount', title: '促销优惠', width: 80, align: 'right' },
  { key: 'discountAmount', title: '优惠金额', width: 80, align: 'right' },
  { key: 'couponAmount', title: '优惠券', width: 80, align: 'right' },
  { key: 'directDiscount', title: '直接优惠', width: 80, align: 'right' },
  { key: 'freightPayer', title: '运费承担方', width: 90 },
  { key: 'shippingFee', title: '运费', width: 80, align: 'right' },
  { key: 'otherFee', title: '其他费用', width: 80, align: 'right' },
  { key: 'settledAmount', title: '已结金额', width: 80, align: 'right', type: 'slot', slotName: 'settledAmountCell' },
  { key: 'expectedShipTime', title: '预计发货时间', width: 110 },
  { key: 'totalQuantity', title: '订货数量', width: 80, align: 'right' },
  { key: 'shippedQuantity', title: '已发数量', width: 80, align: 'right' },
  { key: 'unshippedQuantity', title: '未发数量', width: 80, align: 'right' },
  { key: 'returnQuantity', title: '退货数量', width: 80, align: 'right' },
  { key: 'returnAmount', title: '退货金额', width: 80, align: 'right', type: 'slot', slotName: 'returnAmountCell' },
  { key: 'totalWeight', title: '重量(kg)', width: 80, align: 'right' },
  { key: 'totalVolume', title: '体积(m³)', width: 80, align: 'right' },
  { key: 'logisticsCompany', title: '物流公司', width: 100 },
  { key: 'waybillNo', title: '运单号', width: 100 },
  { key: 'depositAccount1', title: '订金账户1', width: 90 },
  { key: 'depositAccount2', title: '订金账户2', width: 90 },
  { key: 'depositAccount3', title: '订金账户3', width: 90 },
  { key: 'depositAccount4', title: '订金账户4', width: 90 },
  { key: 'region', title: '区域', width: 80 },
  { key: 'saleType', title: '销售类型', width: 80 },
  { key: 'deliveryMethod', title: '配送方式', width: 80 },
  { key: 'buyerRemark', title: '买家备注', width: 120 },
  { key: 'orderRemark', title: '卖家备注', width: 120 },
  { key: 'summary', title: '摘要', width: 120 },
  { key: 'attachment', title: '附件', width: 60 },
  { key: 'extNum1', title: '表头自定义字段1(数字)', width: 120 },
  { key: 'extNum2', title: '表头自定义字段2(数字)', width: 120 },
  { key: 'extText1', title: '表头自定义字段3(文本)', width: 120 },
  { key: 'extText2', title: '表头自定义字段4(文本)', width: 120 },
  { key: 'extText3', title: '表头自定义字段5(文本)', width: 120 },
  { key: 'footerExtText1', title: '表尾自定义字段1', width: 120 },
  { key: 'footerExtText2', title: '表尾自定义字段2', width: 120 },
  { key: 'submitTime', title: '提交时间', width: 110 },
  { key: 'generationMethod', title: '产生方式', width: 80 },
  { key: 'bookkeepingTime', title: '制单时间', width: 110 },
  { key: 'creatorName', title: '制单人', width: 80 },
  { key: 'submitterName', title: '提交人', width: 80 },
  { key: 'auditorName', title: '审核人', width: 80 },
  { key: 'printCount', title: '打印次数', width: 70, align: 'right' },
  { key: 'thirdPartyOrderNo', title: '第三方单号', width: 120 },
  { key: 'resendMessage', title: '重推元气订单完成消息', width: 140 },
  { key: 'auditTime', title: '审核时间', width: 110 },
]

const byTimeColumns: DetailColumnConfig[] = [
  { key: 'seq', title: '', width: 40, type: 'rowNo' },
  { key: 'orderDay', title: '日期', width: 110 },
  { key: 'totalOrders', title: '订单总数', width: 80, align: 'right' },
  { key: 'pendingReviewCount', title: '审核中单数', width: 90, align: 'right' },
  { key: 'pendingOutboundCount', title: '待出库单数', width: 90, align: 'right' },
  { key: 'pendingShipCount', title: '待发货单数', width: 90, align: 'right' },
  { key: 'outboundCount', title: '已出库单数', width: 90, align: 'right' },
  { key: 'shippedCount', title: '发货完成单数', width: 100, align: 'right' },
  { key: 'completedCount', title: '交易完成单数', width: 100, align: 'right' },
]

const byRouteColumns: DetailColumnConfig[] = [
  { key: 'seq', title: '', width: 40, type: 'rowNo' },
  { key: 'deliveryRoute', title: '线路', width: 120, type: 'slot', slotName: 'deliveryRouteCell' },
  { key: 'totalOrders', title: '订单总数', width: 80, align: 'right' },
  { key: 'totalAmount', title: '订单总额', width: 100, align: 'right' },
  { key: 'pendingReviewCount', title: '审核中单数', width: 90, align: 'right' },
  { key: 'pendingOutboundCount', title: '待出库单数', width: 90, align: 'right' },
  { key: 'pendingShipCount', title: '待发货单数', width: 90, align: 'right' },
  { key: 'outboundCount', title: '已出库单数', width: 90, align: 'right' },
  { key: 'shippedCount', title: '发货完成单数', width: 100, align: 'right' },
  { key: 'completedCount', title: '交易完成单数', width: 100, align: 'right' },
]

const byCustomerColumns: DetailColumnConfig[] = [
  { key: 'seq', title: '', width: 40, type: 'rowNo' },
  { key: 'customerName', title: '客户', width: 200, type: 'slot', slotName: 'customerNameCell' },
  { key: 'totalOrders', title: '订单总数', width: 80, align: 'right' },
  { key: 'totalAmount', title: '订单总额', width: 100, align: 'right' },
  { key: 'pendingReviewCount', title: '审核中单数', width: 90, align: 'right' },
  { key: 'pendingOutboundCount', title: '待出库单数', width: 90, align: 'right' },
  { key: 'pendingShipCount', title: '待发货单数', width: 90, align: 'right' },
  { key: 'outboundCount', title: '已出库单数', width: 90, align: 'right' },
  { key: 'shippedCount', title: '发货完成单数', width: 100, align: 'right' },
  { key: 'completedCount', title: '交易完成单数', width: 100, align: 'right' },
]

const fulfillmentColumns: DetailColumnConfig[] = [
  { key: 'seq', title: '', width: 50, type: 'rowNo' },
  { key: 'selection', title: '', width: 40, type: 'checkbox' },
  { key: 'action', title: '操作', width: 80, fixed: 'left', type: 'action', slotName: 'actionCell' },
  { key: 'orderDate', title: '单据日期', width: 110 },
  { key: 'originalOrderNo', title: '原始订单', width: 150 },
  { key: 'status', title: '单据状态', width: 90, type: 'slot', slotName: 'statusCell' },
  { key: 'warehouseName', title: '仓库', width: 100 },
  { key: 'shippedOrderNo', title: '已发订单', width: 150 },
  { key: 'supplementStatus', title: '补单状态', width: 90 },
  { key: 'originalAmount', title: '原单金额', width: 100, align: 'right' },
  { key: 'remainingUnshippedAmount', title: '剩余未发金额', width: 110, align: 'right' },
  { key: 'originalDiscount', title: '原单优惠', width: 90, align: 'right' },
  { key: 'originalItemCount', title: '原单商品项', width: 90, align: 'right' },
  { key: 'unshippedItemCount', title: '未发商品项', width: 90, align: 'right' },
  { key: 'originalQuantity', title: '原单商品数量', width: 100, align: 'right' },
  { key: 'unshippedQuantityItems', title: '未发商品数量', width: 100, align: 'right' },
  { key: 'salesmanName', title: '经手人', width: 80 },
  { key: 'customerName', title: '客户', width: 150 },
  { key: 'receiverPhone', title: '联系电话', width: 110 },
  { key: 'customerCode', title: '客户编号', width: 120 },
  { key: 'customerLevel', title: '客户级别', width: 100 },
  { key: 'receiverName', title: '收货人', width: 80 },
  { key: 'shippingAddress', title: '收货地址', width: 150 },
  { key: 'promoterName', title: '推广人', width: 80 },
  { key: 'customerTicket', title: '客户一票通', width: 100 },
  { key: 'customerRemark', title: '客户备注', width: 120 },
  { key: 'deptName', title: '部门', width: 80 },
  { key: 'billAmount', title: '优惠后金额', width: 100, align: 'right' },
  { key: 'freightPayer', title: '运费承担方', width: 90 },
  { key: 'shippingFee', title: '运费', width: 80, align: 'right' },
  { key: 'otherFee', title: '其他费用', width: 80, align: 'right' },
  { key: 'actualAmount', title: '本单金额', width: 100, align: 'right' },
  { key: 'promoDiscount', title: '促销优惠', width: 80, align: 'right' },
  { key: 'couponAmount', title: '优惠券', width: 80, align: 'right' },
  { key: 'directDiscount', title: '直接优惠', width: 80, align: 'right' },
  { key: 'settledAmount', title: '已结金额', width: 80, align: 'right' },
  { key: 'expectedShipTime', title: '预计发货时间', width: 110 },
  { key: 'totalQuantity', title: '订货数量', width: 80, align: 'right' },
  { key: 'shippedQuantity', title: '已发数量', width: 80, align: 'right' },
  { key: 'unshippedQuantity', title: '未发数量', width: 80, align: 'right' },
  { key: 'returnQuantity', title: '退货数量', width: 80, align: 'right' },
  { key: 'returnAmount', title: '退货金额', width: 80, align: 'right' },
  { key: 'totalWeight', title: '重量', width: 80, align: 'right' },
  { key: 'totalVolume', title: '体积', width: 80, align: 'right' },
  { key: 'logisticsCompany', title: '物流公司', width: 100 },
  { key: 'waybillNo', title: '运单号', width: 100 },
  { key: 'depositAccount1', title: '订金账户1', width: 90 },
  { key: 'depositAccount2', title: '订金账户2', width: 90 },
  { key: 'depositAccount3', title: '订金账户3', width: 90 },
  { key: 'depositAccount4', title: '订金账户4', width: 90 },
  { key: 'region', title: '区域', width: 80 },
  { key: 'saleType', title: '销售类型', width: 80 },
  { key: 'deliveryMethod', title: '配送方式', width: 80 },
  { key: 'buyerRemark', title: '买家备注', width: 120 },
  { key: 'orderRemark', title: '卖家备注', width: 120 },
  { key: 'summary', title: '摘要', width: 120 },
  { key: 'attachment', title: '附件', width: 60 },
  { key: 'extNum1', title: '表头自定义字段1(数字)', width: 120 },
  { key: 'extNum2', title: '表头自定义字段2(数字)', width: 120 },
  { key: 'extText1', title: '表头自定义字段3(文本)', width: 120 },
  { key: 'extText2', title: '表头自定义字段4(文本)', width: 120 },
  { key: 'extText3', title: '表头自定义字段5(文本)', width: 120 },
  { key: 'footerExtText1', title: '表尾自定义字段1', width: 120 },
  { key: 'footerExtText2', title: '表尾自定义字段2', width: 120 },
  { key: 'submitTime', title: '提交时间', width: 110 },
  { key: 'generationMethod', title: '产生方式', width: 80 },
  { key: 'bookkeepingTime', title: '制单时间', width: 110 },
  { key: 'creatorName', title: '制单人', width: 80 },
  { key: 'submitterName', title: '提交人', width: 80 },
  { key: 'auditorName', title: '审核人', width: 80 },
  { key: 'printCount', title: '打印次数', width: 70 },
  { key: 'thirdPartyOrderNo', title: '第三方单号', width: 120 },
  { key: 'pushQywxMsgComplete', title: '重推元气订单完成消息', width: 150 },
  { key: 'auditTime', title: '审核时间', width: 110 },
]

const pendingReviewColumns: DetailColumnConfig[] = [
  { key: 'seq', title: '', width: 50, type: 'rowNo' },
  { key: 'selection', title: '', width: 40, type: 'checkbox' },
  { key: 'action', title: '操作', width: 140, fixed: 'left', type: 'action', slotName: 'pendingReviewActionCell' },
  { key: 'orderDate', title: '单据日期', width: 110, sortable: true },
  { key: 'orderNo', title: '单据编号', width: 150, type: 'slot', slotName: 'orderNoCell', sortable: true },
  { key: 'warehouseName', title: '仓库', width: 100 },
  { key: 'customerName', title: '客户', width: 150 },
  { key: 'settlementMethod', title: '结款方式', width: 80 },
  { key: 'customerCode', title: '客户编号', width: 120 },
  { key: 'customerLevel', title: '客户级别', width: 100 },
  { key: 'receiverName', title: '收货人', width: 80 },
  { key: 'receiverPhone', title: '联系电话', width: 110 },
  { key: 'shippingAddress', title: '收货地址', width: 150 },
  { key: 'promoterName', title: '推广人', width: 80 },
  { key: 'customerTicket', title: '客户一票通', width: 100 },
  { key: 'customerRemark', title: '客户备注', width: 120 },
  { key: 'salesmanName', title: '经手人', width: 80 },
  { key: 'deptName', title: '部门', width: 80 },
  { key: 'productAmount', title: '商品金额', width: 100, align: 'right' },
  { key: 'discountAmount', title: '折后金额', width: 100, align: 'right' },
  { key: 'billAmount', title: '优惠后金额', width: 100, align: 'right' },
  { key: 'freightPayer', title: '运费承担方', width: 90 },
  { key: 'shippingFee', title: '运费', width: 80, align: 'right' },
  { key: 'otherFee', title: '其他费用', width: 80, align: 'right' },
  { key: 'actualAmount', title: '本单金额', width: 100, align: 'right' },
  { key: 'promoDiscount', title: '促销优惠', width: 80, align: 'right' },
  { key: 'couponAmount', title: '优惠券', width: 80, align: 'right' },
  { key: 'directDiscount', title: '直接优惠', width: 80, align: 'right' },
  { key: 'settledAmount', title: '已结金额', width: 80, align: 'right' },
  { key: 'expectedShipTime', title: '预计发货时间', width: 110 },
  { key: 'totalQuantity', title: '订货数量', width: 80, align: 'right' },
  { key: 'shippedQuantity', title: '已发数量', width: 80, align: 'right' },
  { key: 'unshippedQuantity', title: '未发数量', width: 80, align: 'right' },
  { key: 'returnQuantity', title: '退货数量', width: 80, align: 'right' },
  { key: 'returnAmount', title: '退货金额', width: 80, align: 'right' },
  { key: 'totalWeight', title: '重量', width: 80, align: 'right' },
  { key: 'totalVolume', title: '体积', width: 80, align: 'right' },
  { key: 'logisticsCompany', title: '物流公司', width: 100 },
  { key: 'waybillNo', title: '运单号', width: 100 },
  { key: 'depositAccount1', title: '订金账户1', width: 90 },
  { key: 'depositAccount2', title: '订金账户2', width: 90 },
  { key: 'depositAccount3', title: '订金账户3', width: 90 },
  { key: 'depositAccount4', title: '订金账户4', width: 90 },
  { key: 'region', title: '区域', width: 80 },
  { key: 'saleType', title: '销售类型', width: 80 },
  { key: 'deliveryMethod', title: '配送方式', width: 80 },
  { key: 'buyerRemark', title: '买家备注', width: 120 },
  { key: 'orderRemark', title: '卖家备注', width: 120 },
  { key: 'summary', title: '摘要', width: 120 },
  { key: 'attachment', title: '附件', width: 60 },
  { key: 'extNum1', title: '表头自定义字段1(数字)', width: 120 },
  { key: 'extNum2', title: '表头自定义字段2(数字)', width: 120 },
  { key: 'extText1', title: '表头自定义字段3(文本)', width: 120 },
  { key: 'extText2', title: '表头自定义字段4(文本)', width: 120 },
  { key: 'extText3', title: '表头自定义字段5(文本)', width: 120 },
  { key: 'footerExtText1', title: '表尾自定义字段1', width: 120 },
  { key: 'footerExtText2', title: '表尾自定义字段2', width: 120 },
  { key: 'submitTime', title: '提交时间', width: 110 },
  { key: 'generationMethod', title: '产生方式', width: 80 },
  { key: 'bookkeepingTime', title: '制单时间', width: 110 },
  { key: 'creatorName', title: '制单人', width: 80 },
  { key: 'submitterName', title: '提交人', width: 80 },
  { key: 'auditorName', title: '审核人', width: 80 },
  { key: 'printCount', title: '打印次数', width: 70, align: 'right' },
  { key: 'thirdPartyOrderNo', title: '第三方单号', width: 120 },
  { key: 'pushQywxMsgComplete', title: '重推元气订单完成消息', width: 150 },
  { key: 'auditTime', title: '审核时间', width: 110 },
]

const pickingShippingColumns: DetailColumnConfig[] = [
  { key: 'seq', title: '', width: 50, type: 'rowNo' },
  { key: 'selection', title: '', width: 40, type: 'checkbox' },
  { key: 'action', title: '操作', width: 130, fixed: 'left', type: 'action', slotName: 'pickingShippingActionCell' },
  { key: 'orderDate', title: '单据日期', width: 110 },
  { key: 'orderNo', title: '单据编号', width: 150, type: 'slot', slotName: 'orderNoCell' },
  { key: 'status', title: '单据状态', width: 90, type: 'slot', slotName: 'statusCell' },
  { key: 'printCount', title: '打印次数', width: 70, align: 'right' },
  { key: 'settlementStatus', title: '结算状态', width: 90 },
  { key: 'pickingWarehouse', title: '拣货仓库', width: 100 },
  { key: 'collectionLocation', title: '集货位', width: 80 },
  { key: 'customerName', title: '客户', width: 150 },
  { key: 'productAmount', title: '销售金额', width: 100, align: 'right' },
  { key: 'totalVolume', title: '体积', width: 80, align: 'right' },
  { key: 'totalWeight', title: '重量', width: 80, align: 'right' },
  { key: 'originalItemCount', title: '商品行数', width: 80, align: 'right' },
  { key: 'totalQuantity', title: '商品数量', width: 80, align: 'right' },
  { key: 'calculatedPickedQuantity', title: '已拣货数量', width: 80, align: 'right' },
  { key: 'unshippedItemCount', title: '未拣货数量', width: 80, align: 'right' },
  { key: 'shippedQuantity', title: '已发货数量', width: 80, align: 'right' },
  { key: 'unshippedQuantity', title: '未发货数量', width: 80, align: 'right' },
  { key: 'receiverName', title: '收货人', width: 80 },
  { key: 'receiverPhone', title: '联系电话', width: 110 },
  { key: 'shippingAddress', title: '收货地址', width: 150 },
  { key: 'expectedShipTime', title: '预计发货时间', width: 110 },
  { key: 'salesmanName', title: '经手人', width: 80 },
  { key: 'summary', title: '摘要', width: 200 },
  { key: 'pickupAddress', title: '提货地址', width: 150 },
  { key: 'deliveryMethod', title: '配送方式', width: 80 },
  { key: 'logisticsCompany', title: '物流公司', width: 100 },
  { key: 'deliveryDriver', title: '配送司机', width: 80 },
  { key: 'orderSource', title: '单据来源', width: 80 },
  { key: 'bookkeepingTime', title: '制单时间', width: 110 },
  { key: 'extNum1', title: '表头自定义字段1(数字)', width: 120 },
  { key: 'extNum2', title: '表头自定义字段2(数字)', width: 120 },
  { key: 'extText1', title: '表头自定义字段3(文本)', width: 120 },
  { key: 'extText2', title: '表头自定义字段4(文本)', width: 120 },
  { key: 'extText3', title: '表头自定义字段5(文本)', width: 120 },
  { key: 'orderRemark', title: '单据备注', width: 120 },
  { key: 'sortField', title: '排序', width: 80 },
  { key: 'sortValue', title: '排序值', width: 80 },
]

// ════════════════════════════════════════════════
// 数据加载 & 事件处理
// ════════════════════════════════════════════════

function handleSearch() {
  paginationConfig.current = 1
  fetchData()
}

function handleCheckboxChange(record: any, _rowIndex: number, checked: boolean) {
  if (checked) {
    if (!selectedRowKeys.value.includes(record.id)) {
      selectedRowKeys.value.push(record.id)
      selectedRows.value.push(record)
    }
  } else {
    selectedRowKeys.value = selectedRowKeys.value.filter(k => k !== record.id)
    selectedRows.value = selectedRows.value.filter(r => r.id !== record.id)
  }
}

function handleCheckboxAll(checked: boolean, records: any[]) {
  if (checked) {
    const newKeys = records.map(r => r.id).filter(k => !selectedRowKeys.value.includes(k))
    selectedRowKeys.value.push(...newKeys)
    const newRows = records.filter(r => !selectedRowKeys.value.includes(r.id) || newKeys.includes(r.id))
    selectedRows.value = records
  } else {
    // 清除当前页的选中
    const currentIds = new Set(activeTableData.value.map(r => r.id))
    selectedRowKeys.value = selectedRowKeys.value.filter(k => !currentIds.has(k))
    selectedRows.value = selectedRows.value.filter(r => !currentIds.has(r.id))
  }
}

function handlePageChange(page: number, pageSize: number) {
  paginationConfig.current = page
  paginationConfig.pageSize = pageSize
  fetchData()
}

function handleToolbarAction(key: string) {
  switch (key) {
    case 'add': goCreate(); break
    case 'refresh': fetchData(); break
    case 'batchPrint': handleBatchPrint(); break
    case 'productSummary': handleProductSummary(); break
    case 'print': handleBatchPrint(); break
    case 'auditPass': handleBatchAuditPass(); break
    case 'pickComplete': handleBatchPickComplete(); break
    case 'logisticsRemark': handleLogisticsRemark(); break
    case 'pageConfig': showPageConfig.value = true; break
    case 'export': handleExport(); break
    case 'import': openImportDialog(); break
    default: break
  }
}

const printTemplate = ref('default')
const showPrintDialog = ref(false)

/** 获取打印配置 */
function getPrintConfig(): { alwaysLastTemplate: boolean, printAfterSubmit: boolean } {
  try {
    const raw = localStorage.getItem('sale-order-form-print-config')
    if (raw) {
      const parsed = JSON.parse(raw)
      return {
        alwaysLastTemplate: parsed.alwaysLastTemplate ?? false,
        printAfterSubmit: parsed.printAfterSubmit ?? false,
      }
    }
  } catch {}
  return { alwaysLastTemplate: false, printAfterSubmit: false }
}

/** 保存最后一次使用的模板 */
function saveLastPrintTemplate(template: string) {
  try {
    localStorage.setItem('sale-order-last-print-template', template)
  } catch {}
}

/** 恢复最后一次使用的模板 */
function loadLastPrintTemplate(): string {
  try {
    return localStorage.getItem('sale-order-last-print-template') || 'default'
  } catch {
    return 'default'
  }
}

function handleBatchPrint() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择要打印的订单')
    return
  }
  const printConfig = getPrintConfig()
  printTemplate.value = loadLastPrintTemplate()
  // 如果开启"始终使用最后打印模板"，跳过选择弹窗直接打印
  if (printConfig.alwaysLastTemplate) {
    doPrint(selectedRows.value.map(r => Number(r.id)), printTemplate.value)
    return
  }
  showPrintDialog.value = true
}

async function confirmPrint() {
  const ids = selectedRows.value.map(r => Number(r.id))
  await doPrint(ids, printTemplate.value)
  showPrintDialog.value = false
}

async function doPrint(ids: number[], template: string) {
  try {
    await request.post('/erp/sale/order/batch-print', ids)
    saveLastPrintTemplate(template)
    message.success(`已打印 ${ids.length} 条订单（模板: ${template}）`)
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '打印失败')
  }
}

function handleProductSummary() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择要汇总的订单')
    return
  }
  const productMap = new Map<string, { name: string; quantity: number; amount: number }>()
  for (const order of selectedRows.value) {
    const items = (order as any).items || []
    for (const item of items) {
      const key = item.productName || item.productId
      if (!key) continue
      const existing = productMap.get(key) || { name: item.productName || `商品#${item.productId}`, quantity: 0, amount: 0 }
      existing.quantity += Number(item.quantity || item.totalQuantity || 0)
      existing.amount += Number(item.amount || item.productAmount || 0)
      productMap.set(key, existing)
    }
  }
  if (productMap.size === 0) {
    message.info('选中订单暂无商品明细数据')
    return
  }
  let html = '<table style="width:100%;border-collapse:collapse;font-size:13px">'
  html += '<thead><tr style="background:#fafafa"><th style="padding:8px 12px;border:1px solid #e8e8e8;text-align:left">商品</th><th style="padding:8px 12px;border:1px solid #e8e8e8;text-align:right">数量</th><th style="padding:8px 12px;border:1px solid #e8e8e8;text-align:right">金额</th></tr></thead><tbody>'
  for (const p of productMap.values()) {
    html += `<tr><td style="padding:6px 12px;border:1px solid #f0f0f0">${p.name}</td><td style="padding:6px 12px;border:1px solid #f0f0f0;text-align:right">${p.quantity}</td><td style="padding:6px 12px;border:1px solid #f0f0f0;text-align:right">${formatMoney(p.amount)}</td></tr>`
  }
  html += '</tbody></table>'
  Modal.info({
    title: '商品汇总',
    width: 600,
    content: html,
  })
}

function handleBatchAuditPass() {
  const pendingRows = selectedRows.value.filter(r => r.status === 1)
  if (pendingRows.length === 0) {
    message.warning('请选择待审核状态的订单')
    return
  }
  Modal.confirm({
    title: '批量审核通过',
    content: `确定要审核通过 ${pendingRows.length} 条订单吗？`,
    onOk: async () => {
      try {
        await saleOrderApi.batchApprove(pendingRows.map(r => Number(r.id)))
        message.success(`已审核通过 ${pendingRows.length} 条订单`)
        fetchData()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '批量审核失败')
      }
    },
  })
}

function handleBatchPickComplete() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择要发货的订单')
    return
  }
  Modal.confirm({
    title: '批量拣完发货',
    content: `确定要对 ${selectedRows.value.length} 条订单执行拣完发货吗？`,
    onOk: async () => {
      try {
        for (const row of selectedRows.value) {
          await saleOrderApi.ship(Number(row.id), 0)
        }
        message.success(`已完成 ${selectedRows.value.length} 条订单发货`)
        fetchData()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '批量发货失败')
      }
    },
  })
}

const logisticsRemarkText = ref('')
const showLogisticsRemarkModal = ref(false)

function handleLogisticsRemark() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择要添加物流备注的订单')
    return
  }
  logisticsRemarkText.value = ''
  showLogisticsRemarkModal.value = true
}

async function confirmLogisticsRemark() {
  if (!logisticsRemarkText.value.trim()) {
    message.warning('请输入物流备注内容')
    return
  }
  try {
    // 逐条订单添加物流备注
    const remark = logisticsRemarkText.value.trim()
    for (const row of selectedRows.value) {
      await saleOrderApi.update(Number(row.id), { orderRemark: remark } as any)
    }
    message.success(`已为 ${selectedRows.value.length} 条订单添加物流备注`)
    showLogisticsRemarkModal.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '添加物流备注失败')
  }
}

// ═══ 批量导入 ═══
const fileInputRef = ref<HTMLInputElement | null>(null)
const importLoading = ref(false)

function openImportDialog() {
  fileInputRef.value?.click()
}

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)
  importLoading.value = true

  try {
    const res: any = await saleOrderApi.import?.(formData) || await importSaleOrders(formData)
    message.success(`导入成功: ${res?.data?.count || 0} 条`)
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
    // 重置input，允许再次选择同一文件
    input.value = ''
  }
}

async function importSaleOrders(formData: FormData): Promise<any> {
  // 若saleOrderApi没有import方法，使用通用上传
  return await fetch('/api/erp/sale/order/import', {
    method: 'POST',
    headers: { Authorization: `Bearer ${userStore.token}` },
    body: formData,
  }).then(r => r.json())
}

async function fetchData() {
  loading.value = true
  const tid = tenantId.value
  const start = dateRange.value?.[0]?.format('YYYY-MM-DD') || ''
  const end = dateRange.value?.[1]?.format('YYYY-MM-DD') || ''
  const searchParams = buildSearchParams()
  const checkboxParams = buildCheckboxParams()

  try {
    if (mainTab.value === 'all') {
      const statsRes: any = await saleOrderApi.getCenterStats({
        tenantId: tid, startDate: start, endDate: end,
      })
      const statsData = statsRes?.data || statsRes || {}
      Object.assign(stats, statsData)

      if (subTab.value === 'byDoc') {
        const res: any = await saleOrderApi.getCenterPageByDoc({
          pageNum: paginationConfig.current, pageSize: paginationConfig.pageSize,
          tenantId: tid, startDate: start, endDate: end,
          ...searchParams, ...checkboxParams,
        })
        const pageData = res?.data || res || {}
        tableData.value = pageData.records || []
        paginationConfig.total = Number(pageData.total) || 0
      } else if (subTab.value === 'byTime') {
        const res: any = await saleOrderApi.getCenterGroupByDate({
          tenantId: tid, startDate: start, endDate: end,
          ...searchParams,
        })
        timeGroupData.value = res?.data || res || []
      } else if (subTab.value === 'byRoute') {
        const res: any = await saleOrderApi.getCenterGroupByRoute({
          tenantId: tid, startDate: start, endDate: end,
          ...searchParams,
        })
        routeGroupData.value = res?.data || res || []
      } else if (subTab.value === 'byCustomer') {
        const res: any = await saleOrderApi.getCenterGroupByCustomer({
          tenantId: tid, startDate: start, endDate: end,
          ...searchParams,
        })
        customerGroupData.value = res?.data || res || []
      } else if (subTab.value === 'fulfillment') {
        const res: any = await saleOrderApi.getCenterFulfillmentPage({
          pageNum: paginationConfig.current, pageSize: paginationConfig.pageSize,
          tenantId: tid, startDate: start, endDate: end,
          ...searchParams, ...checkboxParams,
        })
        const pageData = res?.data || res || {}
        tableData.value = pageData.records || []
        paginationConfig.total = Number(pageData.total) || 0
      }
    } else if (mainTab.value === 'pendingReview') {
      const res: any = await saleOrderApi.getPendingReviewPage({
        pageNum: paginationConfig.current, pageSize: paginationConfig.pageSize,
        tenantId: tid, startDate: start, endDate: end,
        ...searchParams, ...checkboxParams,
      })
      const pageData = res?.data || res || {}
      tableData.value = pageData.records || []
      paginationConfig.total = Number(pageData.total) || 0
    } else if (mainTab.value === 'pickingShipping') {
      const res: any = await saleOrderApi.getPickingShippingPage({
        pageNum: paginationConfig.current, pageSize: paginationConfig.pageSize,
        tenantId: tid, startDate: start, endDate: end,
        ...searchParams, ...checkboxParams,
      })
      const pageData = res?.data || res || {}
      tableData.value = pageData.records || []
      paginationConfig.total = Number(pageData.total) || 0
    }
  } catch (err: any) {
    console.error('[订单处理中心] 数据加载失败', err)
    message.error(err?.response?.data?.message || '数据加载失败')
  } finally {
    loading.value = false
    nextTick(() => renderChart())
    loadFulfillmentOverview()
    // 数据变化时清空选中状态
    selectedRowKeys.value = []
    selectedRows.value = []
  }
}

// ═══ 操作 ═══
function goCreate() { router.push('/erp/sale/form') }
function goEdit(id: string) { router.push(`/sale/order/${id}`) }

function handleApprove(record: any) {
  Modal.confirm({
    title: '确认审核通过',
    content: `确定要审核通过订单 ${record.orderNo} 吗？`,
    onOk: async () => {
      try { await saleOrderApi.approve(record.id); message.success('审核通过'); fetchData() }
      catch (e: any) { message.error(e?.response?.data?.message || '审核失败') }
    },
  })
}

function handleReject(record: any) {
  Modal.confirm({
    title: '确认拒绝',
    content: `确定要拒绝订单 ${record.orderNo} 吗？`,
    onOk: async () => {
      try { await saleOrderApi.reject(record.id, '审核不通过'); message.success('已拒绝'); fetchData() }
      catch (e: any) { message.error(e?.response?.data?.message || '操作失败') }
    },
  })
}

function handleCancel(record: any) {
  Modal.confirm({
    title: '确认取消',
    content: `确定要取消订单 ${record.orderNo} 吗？`,
    onOk: async () => {
      try { await saleOrderApi.cancel(record.id); message.success('已取消'); fetchData() }
      catch (e: any) { message.error(e?.response?.data?.message || '取消失败') }
    },
  })
}

const paymentRecordVisible = ref(false)
const paymentRecordId = ref<string>('')
const paymentAmount = ref<number>(0)

function handlePayment(record: any) {
  paymentRecordId.value = record.id
  paymentAmount.value = Number(record.billAmount || 0) - Number(record.settledAmount || 0)
  paymentRecordVisible.value = true
}

async function confirmPayment() {
  if (paymentAmount.value <= 0) {
    message.warning('收款金额必须大于0')
    return
  }
  try {
    await saleOrderApi.recordPayment(paymentRecordId.value, paymentAmount.value)
    message.success('收款成功')
    paymentRecordVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '收款失败')
  }
}

async function handlePrint(record: any) {
  try {
    await request.post('/erp/sale/order/batch-print', [Number(record.id)])
    message.success(`订单 ${record.orderNo} 打印成功`)
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '打印失败')
  }
}

function handlePickShip(record: any) {
  Modal.confirm({
    title: '确认拣完发货',
    content: `确定要对订单 ${record.orderNo} 执行拣完发货吗？`,
    onOk: async () => {
      try { await saleOrderApi.ship(record.id, 0); message.success('发货成功'); fetchData() }
      catch (e: any) { message.error(e?.response?.data?.message || '发货失败') }
    },
  })
}

function handleError(err: Error) { message.error(`页面错误: ${err.message}`) }

// ═══ 钻取导航 ═══
function drillDownByRoute(route: string) {
  mainTab.value = 'all'
  subTab.value = 'byDoc'
  searchForm.deliveryRoute = route
  paginationConfig.current = 1
  fetchData()
}

function drillDownByCustomer(customerName: string) {
  mainTab.value = 'all'
  subTab.value = 'byDoc'
  searchForm.customerName = customerName
  paginationConfig.current = 1
  fetchData()
}

// ═══ 键盘快捷操作 ═══
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    fetchData()
  } else if (e.key === 'F8') {
    e.preventDefault()
    handleToolbarAction('batchPrint')
  }
}

// ═══ 导出 ═══
function handleExport() {
  const start = dateRange.value?.[0]?.format('YYYY-MM-DD') || ''
  const end = dateRange.value?.[1]?.format('YYYY-MM-DD') || ''
  saleOrderApi.export({
    tenantId: tenantId.value,
    startDate: start,
    endDate: end,
    orderNo: searchForm.orderNo || undefined,
    status: searchForm.status !== '' ? searchForm.status : undefined,
  }).then((res: any) => {
    const data = res?.data || res || []
    if (!data.length) { message.info('没有可导出的数据'); return }
    // 简单CSV导出
    const headers = activeColumns.value.filter(c => c.key !== 'action' && c.key !== 'selection' && c.key !== 'rowNo').map(c => c.title)
    const keys = activeColumns.value.filter(c => c.key !== 'action' && c.key !== 'selection' && c.key !== 'rowNo').map(c => c.key)
    const rows = data.map((row: any) => keys.map(k => row[k] ?? ''))
    const csv = '\uFEFF' + [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `销售订单_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  }).catch(() => message.error('导出失败'))
}

// ═══ ECharts 渲染 ═══
function renderChart() {
  if (!chartContainerRef.value) return
  const visible = mainTab.value === 'all' && (subTab.value === 'byTime' || subTab.value === 'byRoute')
  if (!visible) {
    if (chartInstance) { chartInstance.dispose(); chartInstance = null }
    return
  }

  if (!chartInstance) {
    chartInstance = echarts.init(chartContainerRef.value)
  }

  let option: echarts.EChartsOption
  if (subTab.value === 'byTime' && timeGroupData.value.length > 0) {
    const data = [...timeGroupData.value].sort((a, b) => (a.orderDay || '').localeCompare(b.orderDay || ''))
    option = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['待审核', '待出库', '待发货', '已出库', '发货完成', '交易完成'], top: 0, textStyle: { fontSize: 10 } },
      grid: { left: 40, right: 20, top: 30, bottom: 20 },
      xAxis: { type: 'category', data: data.map(d => d.orderDay), axisLabel: { fontSize: 10 } },
      yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
      series: [
        { name: '待审核', type: 'bar', stack: 'total', data: data.map(d => Number(d.pendingReviewCount || 0)), itemStyle: { color: '#fa8c16' } },
        { name: '待出库', type: 'bar', stack: 'total', data: data.map(d => Number(d.pendingOutboundCount || 0)), itemStyle: { color: '#faad14' } },
        { name: '待发货', type: 'bar', stack: 'total', data: data.map(d => Math.max(0, Number(d.pendingShipCount || 0) - Number(d.pendingOutboundCount || 0))), itemStyle: { color: '#ffc53d' } },
        { name: '已出库', type: 'bar', stack: 'total', data: data.map(d => Number(d.outboundCount || 0)), itemStyle: { color: '#52c41a' } },
        { name: '发货完成', type: 'bar', stack: 'total', data: data.map(d => Number(d.shippedCount || 0)), itemStyle: { color: '#13c2c2' } },
        { name: '交易完成', type: 'bar', stack: 'total', data: data.map(d => Number(d.completedCount || 0)), itemStyle: { color: '#722ed1' } },
      ],
    }
  } else if (subTab.value === 'byRoute' && routeGroupData.value.length > 0) {
    const data = routeGroupData.value.slice(0, 20)
    option = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['待处理', '待出库', '已出库'], top: 0, textStyle: { fontSize: 10 } },
      grid: { left: 80, right: 20, top: 30, bottom: 20 },
      yAxis: { type: 'category', data: data.map(d => d.deliveryRoute), axisLabel: { fontSize: 10 } },
      xAxis: { type: 'value', axisLabel: { fontSize: 10 } },
      series: [
        { name: '待处理', type: 'bar', stack: 'total', data: data.map(d => Number(d.pendingReviewCount || 0) + Number(d.pendingOutboundCount || 0)), itemStyle: { color: '#ff4d4f' } },
        { name: '待出库', type: 'bar', stack: 'total', data: data.map(d => Number(d.pendingShipCount || 0)), itemStyle: { color: '#fa8c16' } },
        { name: '已出库', type: 'bar', stack: 'total', data: data.map(d => Number(d.outboundCount || 0)), itemStyle: { color: '#52c41a' } },
      ],
    }
  } else {
    option = { title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } } }
  }

  chartInstance.setOption(option, true)
}

// 监听数据变化重新渲染图表
watch([timeGroupData, routeGroupData, subTab, mainTab], () => {
  nextTick(() => renderChart())
}, { deep: true })

// 窗口resize重绘
function handleResize() { chartInstance?.resize() }

// ═══ 加载履约概览统计 ═══
async function loadFulfillmentOverview() {
  if (!(mainTab.value === 'all' && subTab.value === 'fulfillment')) return
  try {
    const tid = tenantId.value
    const start = dateRange.value?.[0]?.format('YYYY-MM-DD') || ''
    const end = dateRange.value?.[1]?.format('YYYY-MM-DD') || ''
    const res: any = await saleOrderApi.getCenterFulfillmentOverview({ tenantId: tid, startDate: start, endDate: end })
    const data = res?.data || res || {}
    // 后端返回 totalOrders / pendingOutboundCount / outboundCount / fulfilledRevenue
    const pendingOut = Number(data.pendingOutboundCount || 0)
    const outbound = Number(data.outboundCount || 0)
    const totalOrders = Number(data.totalOrders || 0)
    Object.assign(fulfillmentStats, {
      unfulfilledOrders: pendingOut,
      unsupplementedOrders: pendingOut,
      supplementingOrders: 0,
      terminatedOrders: 0,
      completedFulfillment: outbound,
    })
    // 履约率 = 已完成 / 总数 × 100
    fulfillmentRate.value = totalOrders > 0 ? Math.round((outbound / totalOrders) * 100) : 0
    // 预计营收（保留原始数值）
    fulfillmentRevenue.value = Number(data.fulfilledRevenue || 0)
  } catch (e) {
    console.warn('加载履约概览失败', e)
  }
}

// ═══ 初始化 ═══
onMounted(() => {
  // 默认选中近一周
  dateShortcut.value = 'thisWeek2'
  const now = dayjs()
  dateRange.value = [now.subtract(7, 'day'), now]
  fetchData()
  window.addEventListener('resize', handleResize)
  window.addEventListener('keydown', handleKeydown)

  // 初始化搜索字段隐藏配置（从localStorage加载）
  try {
    const raw = localStorage.getItem('sale-order-page-config')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed?.queryFields && Array.isArray(parsed.queryFields)) {
        hiddenSearchFieldKeys.value = parsed.queryFields
          .filter((f: any) => f.visible === false)
          .map((f: any) => f.key)
      }
    }
  } catch { /* ignore */ }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('keydown', handleKeydown)
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
})
</script>

<style scoped>
.chart-container {
  width: 100%;
  min-height: 150px;
}
.text-red {
  color: #ff4d4f;
  font-weight: 500;
}
:deep(.bill-detail-table) {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
</style>
