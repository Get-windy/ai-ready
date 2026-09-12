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
        :toolbar-config="toolbarConfig"
        :show-pagination="true"
        :page-total="paginationConfig.total"
        :stats-data="stats"
        @search="handleSearch"
        @refresh="fetchData"
        @toolbar-action="handleToolbarAction"
        @page-change="handlePageChange"
      >
        <!-- 工具栏左侧：查询方案 + 快捷日期（DocCenterLayout 内置日期快捷） -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <a-select
              v-model:value="queryScheme"
              style="width: 160px"
              size="small"
              placeholder="--查询方案--"
              allow-clear
              @change="applyQueryScheme"
            >
              <a-select-option value="">--查询方案--</a-select-option>
              <a-select-option v-for="(_, name) in querySchemes" :key="name" :value="name">
                {{ name }}
              </a-select-option>
            </a-select>
            <a-button type="link" size="small" style="padding: 0 4px" title="保存当前查询条件为方案" @click="handleSaveScheme">
              <PlusOutlined />
            </a-button>
            <a-button
              v-if="queryScheme"
              type="link"
              size="small"
              danger
              style="padding: 0 4px"
              title="删除当前查询方案"
              @click="handleDeleteScheme"
            >
              <DeleteOutlined />
            </a-button>
          </div>
        </template>

        <template #table>
          <BillDetailTable
            ref="tableRef"
            :columns="activeColumns"
            :data-source="activeTableData"
            :loading="loading"
            :view-mode="true"
            :storage-key="'order-center-columns-' + mainTab + '-' + subTab"
            style="height: 100%"
          >
            <template #actionCell="{ record }">
              <a-space :size="4">
                <a-button type="link" size="small" @click="goDetail(record)">查看</a-button>
                <a-button v-if="record.status === 1" type="link" size="small" @click="handleApprove(record)">审核</a-button>
                <a-button v-if="record.status === 2" type="link" size="small" @click="handleShip(record)">发货</a-button>
                <a-button v-if="record.status === 1" type="link" size="small" @click="handleReject(record)">取消</a-button>
                <a-dropdown>
                  <a-button type="link" size="small">更多</a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item @click="handleCopy(record)">复制</a-menu-item>
                      <a-menu-item @click="handlePrint(record)">打印</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </a-space>
            </template>
            <template #orderNoCell="{ record }">
              <a-button type="link" size="small" @click="goDetail(record)">{{ record.orderNo }}</a-button>
            </template>
            <template #saleTypeCell="{ record }">
              <span>{{ saleTypeText(record.saleType) }}</span>
            </template>
            <template #statusCell="{ record }">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
            <!-- 单据来源（拣货/发货） -->
            <template #orderSourceCell="{ record }">
              <span>{{ orderSourceText(record.orderSource) }}</span>
            </template>
            <!-- 重推元气订单完成消息：未接入该外部系统时置灰 -->
            <template #resendMessageCell>
              <a-button
                type="link"
                size="small"
                :disabled="!RESEND_MESSAGE_ENABLED"
                :title="RESEND_MESSAGE_ENABLED ? '' : '本系统未接入「元气」外部系统，暂不支持重推'"
              >
                重推
              </a-button>
            </template>
          </BillDetailTable>
        </template>
      </DocCenterLayout>

      <!-- 页面配置弹窗（对标顶部齿轮） -->
      <PageConfigPanel
        :open="showPageConfig"
        :storage-key="'order-center-page-config'"
        :query-fields-config="pageConfigQueryFields"
        :function-buttons-config="pageConfigButtons"
        :hide-print-config="mainTab === 'picking'"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- 批量导入：隐藏文件输入 -->
      <input
        ref="importFileRef"
        type="file"
        accept=".xlsx,.xls,.csv"
        style="display: none"
        @change="handleImportChange"
      >

      <!-- 商品汇总弹窗 -->
      <a-modal
        v-model:open="productSummaryVisible"
        title="商品汇总"
        :footer="null"
        width="720px"
      >
        <a-table
          :columns="productSummaryColumns"
          :data-source="productSummaryRows"
          :loading="productSummaryLoading"
          size="small"
          row-key="productId"
          :pagination="{ pageSize: 20, showSizeChanger: true }"
        />
      </a-modal>

      <!-- 物流备注弹窗 -->
      <a-modal
        v-model:open="logisticsRemarkVisible"
        title="批量物流备注"
        :ok-text="'确定'"
        @ok="submitLogisticsRemark"
      >
        <a-textarea
          v-model:value="logisticsRemarkText"
          :rows="4"
          placeholder="请输入物流备注内容，将应用到已勾选单据"
          show-count
        />
      </a-modal>

      <!-- 打印弹窗（真实打印：模板渲染 → 浏览器打印 / 远程打印链） -->
      <PrintDialog
        ref="printDialogRef"
        page-code="sale"
        :print-data="printData"
        :always-last-template="pagePrintConfig.alwaysLastTemplate"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import DocCenterLayout from '@/components/DocCenterLayout/DocCenterLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { userPageConfigApi } from '@/api/erp'
import { saleOrderApi } from '@/api/erp'
import { printingApi } from '@/api/printing'
import { useUserStore } from '@/stores/user'
import { exportCsvWithLoading } from '@/utils/exportCsv'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import PrintDialog from '@/components/PrintDialog/index.vue'
import type {
  SearchConfigMap, SearchCheckboxConfigMap, SearchFieldItem,
  StatCardConfigMap, ToolbarConfigMap, ToolbarButtonItem,
} from '@/components/DocCenterLayout/types'

defineOptions({ name: 'OrderCenter' })
const router = useRouter()
const userStore = useUserStore()
const tenantId = computed(() => userStore.tenantId || 1)

// ═══ 阶段 Tab + 维度子 Tab ═══
const mainTabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待审核' },
  { key: 'picking', label: '拣货发货' },
]
const subTabs = computed(() => {
  // 只有「全部」阶段才有维度子 Tab；待审核 / 拣货发货为单一处理列表
  if (mainTab.value !== 'all') return [{ key: 'byDoc', label: '按单据' }]
  return [
    { key: 'byDoc', label: '按单据' },
    { key: 'byDate', label: '按时间' },
    { key: 'byRoute', label: '按路线' },
    { key: 'byCustomer', label: '按客户' },
    { key: 'fulfillment', label: '订单履约' },
  ]
})
const mainTab = ref('all')
const subTab = ref('byDoc')
const dateShortcut = ref('thisWeek')
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索 ═══
const searchForm = reactive<Record<string, any>>({})
const hiddenSearchFieldKeys = ref<string[]>([])

// ═══ 查询条件（对齐文档：按单据 49 项，key 即后端查询参数名） ═══
const ORDER_STATUS_OPTIONS = [
  { label: '全部', value: '' },
  { label: '草稿', value: 0 },
  { label: '待审核', value: 1 },
  { label: '待发货', value: 2 },
  { label: '部分发货', value: 3 },
  { label: '发货完成', value: 4 },
  { label: '交易完成', value: 5 },
  { label: '已取消', value: 6 },
]
// 表头自定义字段 1~5（数字1~2、文本3~5），按单据与订单履约共用
const HEAD_EXT_SEARCH_FIELDS: SearchFieldItem[] = [
  { key: 'extNum1', label: '表头自定义字段1(数字)', type: 'input', span: 3 },
  { key: 'extNum2', label: '表头自定义字段2(数字)', type: 'input', span: 3 },
  { key: 'extText1', label: '表头自定义字段3(文本)', type: 'input', span: 3 },
  { key: 'extText2', label: '表头自定义字段4(文本)', type: 'input', span: 3 },
  { key: 'extText3', label: '表头自定义字段5(文本)', type: 'input', span: 3 },
]
// 表尾自定义 1~2、发货日期起止、收货/物流/备注区（按单据与订单履约共用）
const TAIL_SEARCH_FIELDS: SearchFieldItem[] = [
  { key: 'footerExtText1', label: '表尾自定义1(文本)', type: 'input', span: 3 },
  { key: 'footerExtText2', label: '表尾自定义2(文本)', type: 'input', span: 3 },
  { key: 'shipDateStart', label: '发货日期(起)', type: 'input', span: 3 },
  { key: 'shipDateEnd', label: '发货日期(止)', type: 'input', span: 3 },
  { key: 'receiverName', label: '收货人', type: 'input', span: 3 },
  { key: 'receiverPhone', label: '联系电话', type: 'input', span: 3 },
  { key: 'shippingAddress', label: '收货地址', type: 'input', span: 3 },
  { key: 'logisticsCompany', label: '物流公司', type: 'input', span: 3 },
  { key: 'waybillNo', label: '运单号', type: 'input', span: 3 },
  { key: 'region', label: '区域', type: 'input', span: 3 },
  { key: 'saleType', label: '销售类型', type: 'input', span: 3 },
  { key: 'itemProperty', label: '商品行属性', type: 'input', span: 3 },
  { key: 'deliveryMethod', label: '配送方式', type: 'input', span: 3 },
]
// 卖家/买家备注、摘要、打印次数、金额区间、时间区间、来源（按单据与订单履约共用）
const REMARK_SEARCH_FIELDS: SearchFieldItem[] = [
  { key: 'orderRemark', label: '卖家备注', type: 'input', span: 3 },
  { key: 'buyerRemark', label: '买家备注', type: 'input', span: 3 },
  { key: 'summary', label: '摘要', type: 'input', span: 3 },
  { key: 'printCount', label: '打印次数', type: 'input', span: 3 },
  { key: 'billAmountMin', label: '本单金额最小', type: 'input', span: 3 },
  { key: 'billAmountMax', label: '本单金额最大', type: 'input', span: 3 },
  { key: 'auditTime', label: '审核时间', type: 'input', span: 3 },
]

const byDocSearchFields: SearchFieldItem[] = [
  { key: 'orderDate', label: '单据日期', type: 'input', span: 3 },
  { key: 'orderNo', label: '单据编号', type: 'input', span: 3, suffix: 'search' },
  { key: 'deliveryRoute', label: '配送线路', type: 'input', span: 3, suffix: 'search' },
  { key: 'customerName', label: '客户', type: 'input', span: 3, suffix: 'search' },
  { key: 'salesmanName', label: '经手人', type: 'input', span: 3, suffix: 'search' },
  { key: 'status', label: '单据状态', type: 'select', span: 3, options: ORDER_STATUS_OPTIONS },
  { key: 'settlementMethod', label: '结款方式', type: 'select', span: 3, options: [
    { label: '全部', value: '' }, { label: '现结', value: '现结' },
    { label: '月结', value: '月结' }, { label: '预收', value: '预收' },
    { label: '货到付款', value: '货到付款' },
  ]},
  { key: 'supplementType', label: '补单类型', type: 'select', span: 3, options: [
    { label: '全部', value: '' }, { label: '正常单', value: '正常单' }, { label: '补单', value: '补单' },
  ]},
  { key: 'submitTime', label: '提交时间', type: 'input', span: 3, suffix: 'search' },
  { key: 'productName', label: '商品', type: 'input', span: 3, suffix: 'search' },
  { key: 'productBrand', label: '品牌', type: 'input', span: 3, suffix: 'search' },
  { key: 'industryCategory', label: '所属行业类别', type: 'input', span: 3, suffix: 'search' },
  { key: 'deptName', label: '部门', type: 'input', span: 3, suffix: 'search' },
  { key: 'warehouseName', label: '仓库', type: 'input', span: 3, suffix: 'search' },
  { key: 'generationMethod', label: '产生方式', type: 'input', span: 3, suffix: 'search' },
  { key: 'remark', label: '单据备注', type: 'input', span: 3, suffix: 'search' },
  { key: 'detailRemark', label: '明细备注', type: 'input', span: 3, suffix: 'search' },
  { key: 'isGift', label: '是否赠品', type: 'select', span: 3, options: [
    { label: '全部', value: '' }, { label: '是', value: '1' }, { label: '否', value: '0' },
  ]},
  { key: 'creatorName', label: '制单人', type: 'input', span: 3, suffix: 'search' },
  { key: 'auditorName', label: '审核人', type: 'input', span: 3, suffix: 'search' },
  { key: 'submitterName', label: '提交人', type: 'input', span: 3, suffix: 'search' },
  ...HEAD_EXT_SEARCH_FIELDS,
  ...TAIL_SEARCH_FIELDS,
  { key: 'driverName', label: '配送司机', type: 'input', span: 3, suffix: 'search' },
  { key: 'deliveryVehicle', label: '配送车辆', type: 'input', span: 3, suffix: 'search' },
  ...REMARK_SEARCH_FIELDS,
  { key: 'source', label: '来源', type: 'input', span: 3, suffix: 'search' },
]

// 订单履约查询条件（比按单据少「补单类型」，多「补单状态 / 订单未补金额」）
const fulfillmentSearchFields: SearchFieldItem[] = [
  ...byDocSearchFields.filter(f => f.key !== 'supplementType'),
  { key: 'supplementStatus', label: '补单状态', type: 'input', span: 3, suffix: 'search' },
  { key: 'remainingUnshippedAmount', label: '订单未补金额', type: 'input', span: 3, suffix: 'search' },
]
// 待审核查询条件：同按单据，去掉「补单类型」
const pendingSearchFields: SearchFieldItem[] = byDocSearchFields.filter(f => f.key !== 'supplementType')
// 按时间 / 按路线 / 按客户：维度汇总视图，精简查询项
const groupSearchFields: SearchFieldItem[] = [
  { key: 'customerName', label: '客户', type: 'input', span: 4, suffix: 'search' },
  { key: 'salesmanName', label: '经手人', type: 'input', span: 4, suffix: 'search' },
  { key: 'warehouseName', label: '仓库', type: 'input', span: 4, suffix: 'search' },
  { key: 'region', label: '区域', type: 'input', span: 4, suffix: 'search' },
  { key: 'promoterName', label: '推广人', type: 'input', span: 4, suffix: 'search' },
  { key: 'deptName', label: '部门', type: 'input', span: 4, suffix: 'search' },
]
// 拣货/发货查询条件（文档指定 15 项）
const pickingSearchFields: SearchFieldItem[] = [
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
  { key: 'driverName', label: '配送司机', type: 'input', span: 4, suffix: 'search' },
  { key: 'printCount', label: '打印次数', type: 'input', span: 4, suffix: 'search' },
]

const searchConfig: SearchConfigMap = {
  'all.byDoc': byDocSearchFields,
  'all.byDate': groupSearchFields,
  'all.byRoute': groupSearchFields,
  'all.byCustomer': groupSearchFields,
  'all.fulfillment': fulfillmentSearchFields,
  'pending.byDoc': pendingSearchFields,
  'picking.byDoc': pickingSearchFields,
}

const searchCheckboxConfig: SearchCheckboxConfigMap = {
  'all.byDoc': [
    { key: 'byDetail', label: '按明细显示' },
    { key: 'hideReturnApply', label: '不显示有关联审核中退货申请单的单据' },
    { key: 'showSelected', label: '仅显示已选中' },
  ],
  'all.fulfillment': [
    { key: 'showSelected', label: '仅显示已选中' },
  ],
  'pending.byDoc': [
    { key: 'showSelected', label: '仅显示已选中' },
    { key: 'hideReturnApply', label: '不显示有关联审核中退货申请单的单据' },
  ],
  'picking.byDoc': [
    { key: 'showSelected', label: '仅显示已选中' },
  ],
}

// ═══ 工具栏（按单据 / 待审核为文档指定 9 个功能按钮） ═══
const docToolbarButtons: ToolbarButtonItem[] = [
  { key: 'add', label: '新增', type: 'primary' },
  { key: 'refresh', label: '刷新' },
  { key: 'batchPrint', label: '批量打印' },
  { key: 'productSummary', label: '商品汇总' },
  { key: 'batchImport', label: '批量导入' },
  { key: 'logisticsRemark', label: '物流备注' },
  { key: 'printF8', label: '打印(F8)' },
  { key: 'export', label: '导出' },
  { key: 'pageConfig', label: '配置' },
]
const baseToolbarConfig: ToolbarConfigMap = {
  'all.byDoc': docToolbarButtons,
  'all.byDate': [ { key: 'batchPrint', label: '批量打印' }, { key: 'export', label: '导出' }, { key: 'pageConfig', label: '配置' } ],
  'all.byRoute': [ { key: 'batchPrint', label: '批量打印' }, { key: 'export', label: '导出' }, { key: 'pageConfig', label: '配置' } ],
  'all.byCustomer': [ { key: 'batchPrint', label: '批量打印' }, { key: 'export', label: '导出' }, { key: 'pageConfig', label: '配置' } ],
  'all.fulfillment': [ { key: 'refresh', label: '刷新' }, { key: 'export', label: '导出' }, { key: 'pageConfig', label: '配置' } ],
  'pending.byDoc': docToolbarButtons,
  'picking.byDoc': [
    { key: 'refresh', label: '刷新' },
    { key: 'batchPrint', label: '批量打印' },
    { key: 'productSummary', label: '商品汇总' },
    { key: 'pickComplete', label: '拣完批量发货' },
    { key: 'logisticsRemark', label: '物流备注' },
    { key: 'pageConfig', label: '配置' },
  ],
}
/** 页面配置中被关闭的功能按钮（由 PageConfigPanel 写入，工具栏据此真实隐藏） */
const hiddenToolbarKeys = ref<string[]>([])
/** 页面配置-打印配置（始终使用最后一次打印模板 / 批量打印关联退货申请单） */
const pagePrintConfig = reactive({ alwaysLastTemplate: false, linkReturnApply: false })
/** 当前 Tab 的工具栏按钮（已应用页面配置开关） */
const toolbarConfig = computed<ToolbarConfigMap>(() => {
  const out: ToolbarConfigMap = {}
  Object.keys(baseToolbarConfig).forEach(tabKey => {
    out[tabKey] = baseToolbarConfig[tabKey].filter(b => !hiddenToolbarKeys.value.includes(b.key))
  })
  return out
})

// ═══ 统计卡片（六档：订单总数/待出库/待发货/已出库/发货完成/交易完成） ═══
const stats = ref<Record<string, number>>({
  totalOrders: 0, pendingOutbound: 0, pendingShip: 0,
  outboundCount: 0, shippedCount: 0, completedCount: 0,
})
const statCardConfig: StatCardConfigMap = {
  'all.byDoc': [
    { label: '订单总数', valueKey: 'totalOrders', color: '#1890ff' },
    { label: '待出库', valueKey: 'pendingOutbound', color: '#fa8c16' },
    { label: '待发货', valueKey: 'pendingShip', color: '#fa8c16' },
    { label: '已出库', valueKey: 'outboundCount', color: '#722ed1' },
    { label: '发货完成', valueKey: 'shippedCount', color: '#13c2c2' },
    { label: '交易完成', valueKey: 'completedCount', color: '#52c41a' },
  ],
  'all.byDate': [ { label: '订单总数', valueKey: 'totalOrders', color: '#1890ff' } ],
  'all.byRoute': [ { label: '订单总数', valueKey: 'totalOrders', color: '#1890ff' } ],
  'all.byCustomer': [ { label: '订单总数', valueKey: 'totalOrders', color: '#1890ff' } ],
  'all.fulfillment': [
    { label: '订单总数', valueKey: 'totalOrders', color: '#1890ff' },
    { label: '发货完成', valueKey: 'shippedCount', color: '#13c2c2' },
    { label: '交易完成', valueKey: 'completedCount', color: '#52c41a' },
  ],
  'pending.byDoc': [ { label: '待审核', valueKey: 'pendingReview', color: '#fa8c16' } ],
  'picking.byDoc': [
    { label: '待出库', valueKey: 'pendingOutbound', color: '#fa8c16' },
    { label: '待发货', valueKey: 'pendingShip', color: '#fa8c16' },
    { label: '已出库', valueKey: 'outboundCount', color: '#722ed1' },
  ],
}

// ═══ 查询方案（按操作员持久化，整套条件可复用） ═══
const queryScheme = ref('')
const querySchemes = ref<Record<string, any>>({})

async function loadQuerySchemes() {
  try {
    const raw = await userPageConfigApi.get('order-center', 'query-schemes')
    querySchemes.value = raw ? (typeof raw === 'string' ? JSON.parse(raw) : raw) : {}
  } catch { querySchemes.value = {} }
}

async function persistQuerySchemes() {
  try {
    await userPageConfigApi.save('order-center', 'query-schemes', JSON.stringify(querySchemes.value))
    return true
  } catch { return false }
}

async function handleSaveScheme() {
  const name = window.prompt('请输入方案名称：')
  if (!name || !name.trim()) return
  const schemeName = name.trim()
  querySchemes.value[schemeName] = {
    tab: currentTabKey.value,
    search: { ...searchForm },
    dateRange: dateRange.value
      ? [dateRange.value[0].format('YYYY-MM-DD'), dateRange.value[1].format('YYYY-MM-DD')]
      : null,
  }
  const ok = await persistQuerySchemes()
  if (ok) {
    queryScheme.value = schemeName
    message.success(`查询方案「${schemeName}」已保存`)
  } else {
    message.error('查询方案保存失败')
  }
}

/** 应用查询方案：恢复条件并立即检索 */
function applyQueryScheme(name: string) {
  if (!name) return
  const scheme = querySchemes.value[name]
  if (!scheme) return
  Object.keys(searchForm).forEach(k => { delete searchForm[k] })
  Object.assign(searchForm, scheme.search || {})
  if (Array.isArray(scheme.dateRange) && scheme.dateRange.length === 2) {
    dateRange.value = [dayjs(scheme.dateRange[0]), dayjs(scheme.dateRange[1])]
  }
  handleSearch()
}

/** 删除查询方案 */
async function handleDeleteScheme() {
  const name = queryScheme.value
  if (!name) return
  delete querySchemes.value[name]
  queryScheme.value = ''
  const ok = await persistQuerySchemes()
  ok ? message.success(`查询方案「${name}」已删除`) : message.error('删除失败')
}

// ═══ 分页 ═══
const paginationConfig = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 数据 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const activeTableData = computed(() => tableData.value)

// ═══ 列定义（按维度） ═══
const byDocColumns: DetailColumnConfig[] = [
  { key: 'checkbox', title: '', type: 'checkbox', width: 40 },
  { key: 'orderDate', title: '单据日期', width: 110 },
  { key: 'orderNo', title: '单据编号', width: 150, slotName: 'orderNoCell' },
  { key: 'status', title: '单据状态', width: 90, slotName: 'statusCell' },
  { key: 'warehouseName', title: '仓库', width: 120 },
  { key: 'customerName', title: '客户', width: 160 },
  { key: 'settlementMethod', title: '结款方式', width: 90 },
  { key: 'customerCode', title: '客户编号', width: 100 },
  { key: 'customerLevel', title: '客户级别', width: 80 },
  { key: 'receiverName', title: '收货人', width: 90 },
  { key: 'receiverPhone', title: '联系电话', width: 110 },
  { key: 'shippingAddress', title: '收货地址', width: 180 },
  { key: 'promoterName', title: '推广人', width: 90 },
  { key: 'customerTicket', title: '客户一票通', width: 90 },
  { key: 'customerRemark', title: '客户备注', width: 140 },
  { key: 'salesmanName', title: '经手人', width: 90 },
  { key: 'deptName', title: '部门', width: 90 },
  { key: 'productAmount', title: '金额', width: 110, align: 'right' },
  { key: 'discountAmount', title: '折后金额', width: 110, align: 'right' },
  { key: 'favorableAmount', title: '优惠后金额', width: 110, align: 'right' },
  { key: 'freightPayer', title: '运费承担方', width: 100 },
  { key: 'shippingFee', title: '运费', width: 90, align: 'right' },
  { key: 'otherFee', title: '其他费用', width: 90, align: 'right' },
  { key: 'billAmount', title: '本单金额', width: 110, align: 'right' },
  { key: 'promoDiscount', title: '促销优惠', width: 90, align: 'right' },
  { key: 'couponAmount', title: '优惠劵', width: 90, align: 'right' },
  { key: 'directDiscount', title: '直接优惠', width: 90, align: 'right' },
  { key: 'settledAmount', title: '已结金额', width: 110, align: 'right' },
  { key: 'expectedShipTime', title: '预计发货时间', width: 130 },
  { key: 'totalQuantity', title: '订货数量', width: 90, align: 'right' },
  { key: 'shippedQuantity', title: '已发数量', width: 90, align: 'right' },
  { key: 'unshippedQuantity', title: '未发数量', width: 90, align: 'right' },
  { key: 'returnQuantity', title: '退货数量', width: 90, align: 'right' },
  { key: 'returnAmount', title: '退货金额', width: 100, align: 'right' },
  { key: 'totalWeight', title: '重量(kg)', width: 90, align: 'right' },
  { key: 'totalVolume', title: '体积(m³)', width: 90, align: 'right' },
  { key: 'logisticsCompany', title: '物流公司', width: 110 },
  { key: 'waybillNo', title: '运单号', width: 130 },
  { key: 'depositAccount1', title: '订金账户1', width: 110 },
  { key: 'depositAccount2', title: '订金账户2', width: 110 },
  { key: 'depositAccount3', title: '订金账户3', width: 110 },
  { key: 'depositAccount4', title: '订金账户4', width: 110 },
  { key: 'region', title: '区域', width: 80 },
  { key: 'saleType', title: '销售类型', width: 90, slotName: 'saleTypeCell' },
  { key: 'deliveryMethod', title: '配送方式', width: 100 },
  { key: 'buyerRemark', title: '买家备注', width: 140 },
  { key: 'orderRemark', title: '卖家备注', width: 140 },
  { key: 'summary', title: '摘要', width: 120 },
  { key: 'attachment', title: '附件', width: 70 },
  { key: 'extNum1', title: '表头自定义字段1', width: 100, align: 'right' },
  { key: 'extNum2', title: '表头自定义字段2', width: 100, align: 'right' },
  { key: 'extText1', title: '表头自定义字段3', width: 110 },
  { key: 'extText2', title: '表头自定义字段4', width: 110 },
  { key: 'extText3', title: '表头自定义字段5', width: 110 },
  { key: 'footerExtText1', title: '表尾自定义字段1', width: 110 },
  { key: 'footerExtText2', title: '表尾自定义字段2', width: 110 },
  { key: 'submitTime', title: '提交时间', width: 130 },
  { key: 'generationMethod', title: '产生方式', width: 90 },
  { key: 'bookkeepingTime', title: '制单时间', width: 130 },
  { key: 'creatorName', title: '制单人', width: 90 },
  { key: 'submitterName', title: '提交人', width: 90 },
  { key: 'auditorName', title: '审核人', width: 90 },
  { key: 'printCount', title: '打印次数', width: 80 },
  { key: 'thirdPartyOrderNo', title: '第三方单号', width: 130 },
  { key: 'resendMessage', title: '重推元气订单完成消息', width: 150, slotName: 'resendMessageCell' },
  { key: 'auditTime', title: '审核时间', width: 130 },
  { key: 'action', title: '操作', width: 170, fixed: 'right', slotName: 'actionCell' },
]

// 商品汇总弹窗列
const productSummaryColumns = [
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 220, ellipsis: true },
  { title: '货号', dataIndex: 'productCode', key: 'productCode', width: 110 },
  { title: '规格', dataIndex: 'spec', key: 'spec', width: 100 },
  { title: '计价单位', dataIndex: 'unitName', key: 'unitName', width: 90 },
  { title: '数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'right' },
  { title: '金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, align: 'right' },
]

// 订单履约列（71 列，对齐文档「按单据基础上增加 原始订单/已发订单/补单状态…」）
const fulfillmentColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40 },
  { key: 'orderDate', title: '单据日期', width: 110 },
  { key: 'originalOrderNo', title: '原始订单', width: 150 },
  { key: 'status', title: '单据状态', width: 90, slotName: 'statusCell' },
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
  { key: 'favorableAmount', title: '优惠后金额', width: 100, align: 'right' },
  { key: 'freightPayer', title: '运费承担方', width: 90 },
  { key: 'shippingFee', title: '运费', width: 80, align: 'right' },
  { key: 'otherFee', title: '其他费用', width: 80, align: 'right' },
  { key: 'billAmount', title: '本单金额', width: 100, align: 'right' },
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
  { key: 'resendMessage', title: '重推元气订单完成消息', width: 150, slotName: 'resendMessageCell' },
  { key: 'auditTime', title: '审核时间', width: 110 },
  { key: 'action', title: '操作', width: 150, fixed: 'right', slotName: 'actionCell' },
]

// 拣货/发货列（37 列，对齐文档；已拣货/未拣货取拣货作业真实回写值）
const pickingColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40 },
  { key: 'orderDate', title: '单据日期', width: 110 },
  { key: 'orderNo', title: '单据编号', width: 150, slotName: 'orderNoCell' },
  { key: 'status', title: '单据状态', width: 90, slotName: 'statusCell' },
  { key: 'printCount', title: '打印次数', width: 70, align: 'right' },
  { key: 'settlementStatus', title: '结算状态', width: 90 },
  { key: 'pickingWarehouse', title: '拣货仓库', width: 100 },
  { key: 'collectionLocation', title: '集货位', width: 80 },
  { key: 'customerName', title: '客户', width: 150 },
  { key: 'productAmount', title: '销售金额', width: 100, align: 'right' },
  { key: 'totalVolume', title: '体积(m³)', width: 80, align: 'right' },
  { key: 'totalWeight', title: '重量(kg)', width: 80, align: 'right' },
  { key: 'lineCount', title: '商品行数', width: 80, align: 'right' },
  { key: 'totalQuantity', title: '商品数量', width: 80, align: 'right' },
  { key: 'pickedQuantity', title: '已拣货数量', width: 90, align: 'right' },
  { key: 'unpickedQuantity', title: '未拣货数量', width: 90, align: 'right' },
  { key: 'shippedQuantity', title: '已发货数量', width: 90, align: 'right' },
  { key: 'unshippedQuantity', title: '未发货数量', width: 90, align: 'right' },
  { key: 'receiverName', title: '收货人', width: 80 },
  { key: 'receiverPhone', title: '联系电话', width: 110 },
  { key: 'shippingAddress', title: '收货地址', width: 150 },
  { key: 'expectedShipTime', title: '预计发货时间', width: 110 },
  { key: 'salesmanName', title: '经手人', width: 80 },
  { key: 'summary', title: '摘要', width: 200 },
  { key: 'pickupAddress', title: '提货地址', width: 150 },
  { key: 'deliveryMethod', title: '配送方式', width: 80 },
  { key: 'logisticsCompany', title: '物流公司', width: 100 },
  { key: 'driverName', title: '配送司机', width: 80 },
  { key: 'orderSource', title: '单据来源', width: 80, slotName: 'orderSourceCell' },
  { key: 'bookkeepingTime', title: '制单时间', width: 110 },
  { key: 'extNum1', title: '表头自定义字段1(数字)', width: 120 },
  { key: 'extNum2', title: '表头自定义字段2(数字)', width: 120 },
  { key: 'extText1', title: '表头自定义字段3(文本)', width: 120 },
  { key: 'extText2', title: '表头自定义字段4(文本)', width: 120 },
  { key: 'extText3', title: '表头自定义字段5(文本)', width: 120 },
  { key: 'orderRemark', title: '单据备注', width: 120 },
  { key: 'sortOrder', title: '排序', width: 80, align: 'right' },
  { key: 'sortValue', title: '排序值', width: 80, align: 'right' },
  { key: 'action', title: '操作', width: 150, fixed: 'right', slotName: 'actionCell' },
]

// 按时间维度：按天/周/月聚合
const byDateColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40 },
  { key: 'orderDay', title: '日期', width: 110 },
  { key: 'totalOrders', title: '订单总数', width: 90, align: 'right' },
  { key: 'pendingReviewCount', title: '审核中单数', width: 100, align: 'right' },
  { key: 'pendingOutboundCount', title: '待出库单数', width: 100, align: 'right' },
  { key: 'pendingShipCount', title: '待发货单数', width: 100, align: 'right' },
  { key: 'outboundCount', title: '已出库单数', width: 100, align: 'right' },
  { key: 'shippedCount', title: '发货完成单数', width: 110, align: 'right' },
  { key: 'completedCount', title: '交易完成单数', width: 110, align: 'right' },
]
// 按路线维度：按配送线路聚合
const byRouteColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40 },
  { key: 'deliveryRoute', title: '线路', width: 160 },
  { key: 'totalOrders', title: '订单总数', width: 90, align: 'right' },
  { key: 'totalAmount', title: '订单总额', width: 110, align: 'right' },
  { key: 'pendingReviewCount', title: '审核中单数', width: 100, align: 'right' },
  { key: 'pendingOutboundCount', title: '待出库单数', width: 100, align: 'right' },
  { key: 'pendingShipCount', title: '待发货单数', width: 100, align: 'right' },
  { key: 'outboundCount', title: '已出库单数', width: 100, align: 'right' },
  { key: 'shippedCount', title: '发货完成单数', width: 110, align: 'right' },
  { key: 'completedCount', title: '交易完成单数', width: 110, align: 'right' },
]
// 按客户维度：按客户聚合
const byCustomerColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40 },
  { key: 'customerName', title: '客户', width: 200 },
  { key: 'totalOrders', title: '订单总数', width: 90, align: 'right' },
  { key: 'totalAmount', title: '订单总额', width: 110, align: 'right' },
  { key: 'pendingReviewCount', title: '审核中单数', width: 100, align: 'right' },
  { key: 'pendingOutboundCount', title: '待出库单数', width: 100, align: 'right' },
  { key: 'pendingShipCount', title: '待发货单数', width: 100, align: 'right' },
  { key: 'outboundCount', title: '已出库单数', width: 100, align: 'right' },
  { key: 'shippedCount', title: '发货完成单数', width: 110, align: 'right' },
  { key: 'completedCount', title: '交易完成单数', width: 110, align: 'right' },
]

// 待审核列＝按单据列（文档：同按单据子标签页，65 字段），直接复用同一份定义
const pendingColumns = byDocColumns

const activeColumns = computed(() => {
  if (mainTab.value === 'picking') return pickingColumns
  if (mainTab.value === 'pending') return pendingColumns
  if (subTab.value === 'fulfillment') return fulfillmentColumns
  if (subTab.value === 'byDate') return byDateColumns
  if (subTab.value === 'byRoute') return byRouteColumns
  if (subTab.value === 'byCustomer') return byCustomerColumns
  return byDocColumns
})
const storageKey = computed(() => `order-center-${mainTab.value}-${subTab.value}`)

// ═══ 数据加载 ═══
const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      ...searchForm,
      current: paginationConfig.current,
      size: paginationConfig.pageSize,
      pageNum: paginationConfig.current,
      pageSize: paginationConfig.pageSize,
      tenantId: tenantId.value,
    }
    if (dateRange.value) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }
    // 阶段过滤：仅在用户未显式筛选单据状态时生效（拣货/发货由后端固定 status in (2,3)）
    if (params.status === undefined || params.status === null || params.status === '') {
      if (mainTab.value === 'pending') params.status = 1
    }
    if (mainTab.value === 'picking') delete params.status
    Object.keys(params).forEach(k => { if (params[k] === undefined || params[k] === null || params[k] === '') delete params[k] })

    let res: any
    // 阶段优先：待审核 / 拣货发货各自只有一套列表，不受维度子 Tab 影响
    if (mainTab.value === 'pending') {
      res = await saleOrderApi.getPendingReviewPage({ ...params, pageNum: params.current, pageSize: params.size })
    } else if (mainTab.value === 'picking') {
      res = await saleOrderApi.getPickingShippingPage({ ...params, pageNum: params.current, pageSize: params.size })
    } else if (subTab.value === 'fulfillment') {
      res = await saleOrderApi.getCenterFulfillmentPage({ ...params, pageNum: params.current, pageSize: params.size })
    } else if (subTab.value === 'byDate') {
      res = await saleOrderApi.getCenterGroupByDate(params)
    } else if (subTab.value === 'byRoute') {
      res = await saleOrderApi.getCenterGroupByRoute(params)
    } else if (subTab.value === 'byCustomer') {
      res = await saleOrderApi.getCenterGroupByCustomer(params)
    } else {
      res = await saleOrderApi.getCenterPageByDoc({ ...params, pageNum: params.current, pageSize: params.size })
    }
    const data = res?.data || res || {}
    if (Array.isArray(data)) {
      // 按时间 / 按路线 / 按客户：后端返回分组数组（非分页）
      tableData.value = data
      paginationConfig.total = data.length
    } else {
      tableData.value = data?.records || data?.list || []
      paginationConfig.total = Number(data?.total) || 0
    }
    // 统计卡片
    if (Number(paginationConfig.current) === 1 || mainTab.value === 'all') {
      const st: any = await saleOrderApi.getCenterStats({ tenantId: tenantId.value, ...params })
      stats.value = st?.data || st || {}
    }
  } catch (error) {
    console.warn('[订单处理中心] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { paginationConfig.current = 1; fetchData() }
const handlePageChange = (page: number, size: number) => { paginationConfig.current = page; paginationConfig.pageSize = size; fetchData() }

// 主 Tab 切换：若当前子 Tab 在新阶段不存在（如履约 → 拣货发货），重置为第一个子 Tab
watch(mainTab, () => {
  const keys = subTabs.value.map(t => t.key)
  if (!keys.includes(subTab.value)) subTab.value = keys[0] || 'byDoc'
})

// Tab 切换：重置分页、加载该 Tab 的页面配置、拉取数据
watch([mainTab, subTab], () => {
  paginationConfig.current = 1
  loadPageConfig()
  fetchData()
})

/** 加载当前 Tab 已保存的页面配置（查询条件显隐 / 功能按钮开关 / 打印配置） */
async function loadPageConfig() {
  try {
    const raw = await userPageConfigApi.get('order-center', `page-config-${currentTabKey.value}`)
    if (!raw) return
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (Array.isArray(parsed?.queryFields)) {
      hiddenSearchFieldKeys.value = parsed.queryFields
        .filter((f: any) => f.visible === false).map((f: any) => f.key)
    }
    if (Array.isArray(parsed?.functionButtons)) {
      hiddenToolbarKeys.value = parsed.functionButtons
        .filter((b: any) => b.enabled === false).map((b: any) => b.key)
    }
    if (parsed?.printConfig) {
      pagePrintConfig.alwaysLastTemplate = parsed.printConfig.alwaysLastTemplate === true
      pagePrintConfig.linkReturnApply = parsed.printConfig.linkReturnApply === true
    }
  } catch { /* 静默失败：保持默认全显 */ }
}

// ═══ 工具栏操作 ═══
const tableRef = ref<InstanceType<typeof BillDetailTable>>()
const getSelectedIds = (): any[] => {
  const records = tableRef.value?.getCheckedRecords?.() || []
  // 雪花 ID 在前端为字符串，保持原样透传避免精度丢失
  return records.map((r: any) => r.orderId || r.id).filter(Boolean)
}

const handleToolbarAction = async (action: string) => {
  switch (action) {
    case 'add': router.push('/sales/order/form'); break
    case 'refresh': fetchData(); break
    case 'batchPrint': await handleBatchPrint(); break
    case 'productSummary': await handleProductSummary(); break
    case 'batchImport': handleBatchImport(); break
    case 'logisticsRemark': await handleLogisticsRemark(); break
    case 'printF8': await handlePrintF8(); break
    case 'export': handleExport(); break
    case 'pageConfig': showPageConfig.value = true; break
    case 'batchApprove': await handleBatchApprove(); break
    case 'batchShip': await handleBatchShip(); break
    case 'pickComplete': await handlePickComplete(); break
  }
}

/** 拣完批量发货：对已勾选且已拣完的单据批量确认出库 */
const handlePickComplete = async () => {
  const ids = getSelectedIds()
  if (!ids.length) { message.warning('请先勾选要发货的单据'); return }
  Modal.confirm({
    title: '拣完批量发货确认', content: `确认对已勾选的 ${ids.length} 张单据执行拣货完成并发货吗？`,
    okText: '确认发货', cancelText: '取消',
    onOk: async () => {
      try {
        const records = tableRef.value?.getCheckedRecords?.() || []
        let done = 0
        for (const r of records) {
          const warehouseId = r.warehouseId
          if (!warehouseId) continue
          await saleOrderApi.ship(r.orderId || r.id, warehouseId)
          done++
        }
        message.success(`已完成 ${done} 张单据的发货`); fetchData()
      } catch { message.error('批量发货失败') }
    },
  })
}

// ═══ 打印（真实打印：拉取单据详情 → 渲染已发布模板 → 浏览器打印） ═══
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({})

/** 单张打印：打开打印弹窗（可选模板、预览、份数） */
const openPrintDialog = async (record: any) => {
  const id = record.orderId || record.id
  if (!id) { message.warning('未找到可打印的单据'); return }
  try {
    const res: any = await saleOrderApi.getById(id)
    printData.value = res?.data || res || {}
    printDialogRef.value?.open()
  } catch { message.error('加载单据详情失败') }
}

/** 打印(F8)：优先打印勾选单据，未勾选则打印当前页首行 */
const handlePrintF8 = async () => {
  const records = tableRef.value?.getCheckedRecords?.() || []
  if (records.length) { await openPrintDialog(records[0]); return }
  const first: any = activeTableData.value[0]
  if (!first) { message.warning('当前页无可打印单据'); return }
  await openPrintDialog(first)
}

/** 批量打印：登记打印次数 + 按同一模板连续打印已勾选单据 */
const handleBatchPrint = async () => {
  const ids = getSelectedIds()
  if (!ids.length) { message.warning('请先勾选要打印的单据'); return }
  try {
    await saleOrderApi.batchPrint(ids)
    const done = await printContinuous(ids)
    if (done) fetchData()
  } catch { message.error('打印失败') }
}

/** 连续打印：逐张渲染已发布模板，合并为一次打印任务输出 */
async function printContinuous(ids: any[]): Promise<boolean> {
  const tplRes: any = await printingApi.getTemplates({ page: 1, size: 50, pageCode: 'sale', status: 1 })
  const templates = tplRes?.data?.records || tplRes?.records || []
  if (!templates.length) {
    message.warning('未找到已发布的销售订单打印模板，请先在「打印模板」中配置并发布')
    return false
  }
  const tpl = templates.find((t: any) => t.isDefault) || templates[0]
  const htmls: string[] = []
  for (const id of ids) {
    const detail: any = await saleOrderApi.getById(id)
    const res: any = await printingApi.renderTemplate({
      templateJson: tpl.templateJson,
      dataJson: JSON.stringify(detail?.data || detail || {}),
    })
    htmls.push(res?.data?.html || res?.html || '')
  }
  const iframe = document.createElement('iframe')
  iframe.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0'
  document.body.appendChild(iframe)
  const doc = iframe.contentDocument || iframe.contentWindow?.document
  if (!doc) { message.error('无法创建打印窗口'); return false }
  doc.open()
  doc.write(`<!DOCTYPE html><html><head><title>销售订单打印</title>
    <style>
      body{margin:0;padding:8mm;font-family:SimSun,serif;font-size:12px}
      table{border-collapse:collapse;width:100%}
      td,th{border:1px solid #333;padding:4px 6px}
      .print-page{page-break-after:always}
      .print-page:last-child{page-break-after:auto}
      @media print{@page{size:auto;margin:8mm}}
    </style></head><body>${htmls.map(h => `<div class="print-page">${h}</div>`).join('')}</body></html>`)
  doc.close()
  await new Promise(r => setTimeout(r, 300))
  iframe.contentWindow?.focus()
  iframe.contentWindow?.print()
  setTimeout(() => document.body.removeChild(iframe), 1000)
  message.success(`已发送 ${ids.length} 张单据到打印机`)
  return true
}

// 商品汇总（打开汇总弹窗）
const productSummaryVisible = ref(false)
const productSummaryLoading = ref(false)
const productSummaryRows = ref<any[]>([])
const handleProductSummary = async () => {
  productSummaryLoading.value = true
  try {
    const params: Record<string, any> = { tenantId: tenantId.value }
    if (dateRange.value) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }
    const res: any = await saleOrderApi.productSummary(params)
    productSummaryRows.value = res?.data || []
    productSummaryVisible.value = true
  } catch { message.error('商品汇总获取失败') } finally {
    productSummaryLoading.value = false
  }
}

// 批量导入（文件选择）
const importFileRef = ref<HTMLInputElement>()
const handleBatchImport = () => { importFileRef.value?.click() }
const handleImportChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const res: any = await saleOrderApi.batchImport(file)
    message.success(res?.data || '导入成功'); fetchData()
  } catch { message.error('导入失败') } finally {
    input.value = ''
  }
}

// 物流备注（批量更新）
const logisticsRemarkVisible = ref(false)
const logisticsRemarkText = ref('')
const handleLogisticsRemark = () => {
  const ids = getSelectedIds()
  if (!ids.length) { message.warning('请先勾选要备注的单据'); return }
  logisticsRemarkText.value = ''
  logisticsRemarkVisible.value = true
}
const submitLogisticsRemark = async () => {
  const ids = getSelectedIds()
  if (!logisticsRemarkText.value.trim()) { message.warning('请输入物流备注'); return }
  try {
    await saleOrderApi.batchLogisticsRemark(ids, logisticsRemarkText.value.trim())
    message.success('物流备注已更新'); logisticsRemarkVisible.value = false; fetchData()
  } catch { message.error('更新失败') }
}

// 批量审核
const handleBatchApprove = () => {
  const ids = getSelectedIds()
  if (!ids.length) { message.warning('请先勾选要审核的单据'); return }
  Modal.confirm({
    title: '批量审核确认', content: `确认审核通过已勾选的 ${ids.length} 张单据吗？`,
    okText: '确认审核', cancelText: '取消',
    onOk: async () => {
      try {
        await saleOrderApi.batchApprove(ids)
        message.success('批量审核成功'); fetchData()
      } catch { message.error('批量审核失败') }
    },
  })
}

// 批量发货（逐单确认出库）
const handleBatchShip = () => {
  const ids = getSelectedIds()
  if (!ids.length) { message.warning('请先勾选要发货的单据'); return }
  Modal.confirm({
    title: '批量发货确认', content: `确认对已勾选的 ${ids.length} 张单据发货吗？`,
    okText: '确认发货', cancelText: '取消',
    onOk: async () => {
      try {
        const records = tableRef.value?.getCheckedRecords?.() || []
        for (const r of records) {
          const warehouseId = r.warehouseId
          if (!warehouseId) continue
          await saleOrderApi.ship(r.orderId || r.id, warehouseId)
        }
        message.success('批量发货成功'); fetchData()
      } catch { message.error('批量发货失败') }
    },
  })
}

/** 导出：按当前查询条件导出真实数据为 CSV（复用通用导出工具） */
const handleExport = async () => {
  try {
    const params: Record<string, any> = {
      ...searchForm,
      pageNum: 1,
      pageSize: 5000,
      tenantId: tenantId.value,
    }
    if (dateRange.value) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }
    Object.keys(params).forEach(k => {
      if (params[k] === undefined || params[k] === null || params[k] === '') delete params[k]
    })
    const res: any = await saleOrderApi.getCenterPageByDoc(params)
    const data = res?.data || res || {}
    const records: any[] = data?.records || data?.list || []
    if (!records.length) { message.warning('当前查询条件下没有可导出的数据'); return }
    const headers = [
      '单据日期', '单据编号', '单据状态', '仓库', '客户', '经手人', '部门',
      '金额', '优惠后金额', '本单金额', '已结金额',
      '订货数量', '已发数量', '未发数量',
      '物流公司', '运单号', '制单人', '审核人', '审核时间',
    ]
    const rows = records.map((r: any) => [
      r.orderDate ?? '', r.orderNo ?? '', getStatusText(r.status), r.warehouseName ?? '',
      r.customerName ?? '', r.salesmanName ?? '', r.deptName ?? '',
      r.productAmount ?? 0, r.favorableAmount ?? 0, r.billAmount ?? 0, r.settledAmount ?? 0,
      r.totalQuantity ?? 0, r.shippedQuantity ?? 0, r.unshippedQuantity ?? 0,
      r.logisticsCompany ?? '', r.waybillNo ?? '', r.creatorName ?? '',
      r.auditorName ?? '', r.auditTime ?? '',
    ])
    await exportCsvWithLoading(headers, rows, '订单处理中心')
  } catch { message.error('导出失败') }
}

// ═══ 行操作 ═══
const goDetail = (record: any) => { router.push(`/sales/order/form?id=${record.orderId || record.id}`) }
const handleApprove = (record: any) => {
  Modal.confirm({
    title: '审核确认', content: `确认审核通过订单 ${record.orderNo} 吗？`,
    okText: '确认审核', cancelText: '取消',
    onOk: async () => {
      try {
        await saleOrderApi.approve(record.orderId || record.id)
        message.success('审核通过'); fetchData()
      } catch { message.error('审核失败') }
    },
  })
}
const handleShip = (record: any) => {
  Modal.confirm({
    title: '发货确认', content: `确认对订单 ${record.orderNo} 发货吗？`,
    okText: '确认发货', cancelText: '取消',
    onOk: async () => {
      try {
        await saleOrderApi.ship(record.orderId || record.id, record.warehouseId)
        message.success('已发货'); fetchData()
      } catch { message.error('发货失败') }
    },
  })
}
const handleReject = (record: any) => {
  Modal.confirm({
    title: '取消订单', content: `确认取消订单 ${record.orderNo} 吗？`,
    okType: 'danger', okText: '确认取消',
    onOk: async () => {
      try {
        await saleOrderApi.cancel(record.orderId || record.id)
        message.success('已取消'); fetchData()
      } catch { message.error('取消失败') }
    },
  })
}
const handleCopy = (record: any) => { router.push(`/sales/order/form?copy=${record.orderId || record.id}`) }
const handlePrint = (record: any) => { openPrintDialog(record) }
/** 重推元气订单完成消息：本系统未接入「元气」外部系统，如实置灰并说明，不伪装成功 */
const RESEND_MESSAGE_ENABLED = false

// ═══ 状态辅助 ═══
const getStatusColor = (status: number) => {
  const map: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'orange', 3: 'processing', 4: 'blue', 5: 'green', 6: 'red' }
  return map[status] || 'default'
}
const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '草稿', 1: '待审核', 2: '待发货', 3: '部分发货', 4: '已发货', 5: '已完成', 6: '已取消' }
  return map[status] || '未知'
}

const SALE_TYPE_MAP: Record<number, string> = { 0: '普通销售', 1: '预订货', 2: '零售', 3: '换货' }
const saleTypeText = (t: number) => SALE_TYPE_MAP[t] ?? '-'

// 单据来源（拣货/发货 Tab）
const ORDER_SOURCE_MAP: Record<number, string> = {
  0: '手工录入', 1: '销售订单', 2: '零售单', 3: '接口导入', 4: '预订货单',
}
const orderSourceText = (s: any) => (s === null || s === undefined ? '-' : (ORDER_SOURCE_MAP[Number(s)] ?? String(s)))

const handleError = (err: any) => { console.warn('[订单处理中心] ErrorBoundary:', err) }

// ═══ 页面配置（查询条件显隐 + 功能按钮开关，均与当前 Tab 的搜索/工具栏同源） ═══
const showPageConfig = ref(false)
const currentTabKey = computed(() => `${mainTab.value}.${subTab.value}`)
const currentSearchFields = computed<SearchFieldItem[]>(
  () => searchConfig[currentTabKey.value] || byDocSearchFields
)
const currentToolbarButtons = computed<ToolbarButtonItem[]>(
  () => baseToolbarConfig[currentTabKey.value] || docToolbarButtons
)
/** 页面配置-查询条件：key 与 searchConfig 完全一致，关闭即从搜索区移除 */
const pageConfigQueryFields = computed(() =>
  currentSearchFields.value.map(f => ({
    key: f.key,
    label: f.label,
    visible: !hiddenSearchFieldKeys.value.includes(f.key),
  }))
)
/** 页面配置-功能按钮：key 与 toolbarConfig 完全一致，关闭即从工具栏移除 */
const pageConfigButtons = computed(() =>
  currentToolbarButtons.value.map(b => ({
    key: b.key,
    label: b.label,
    enabled: !hiddenToolbarKeys.value.includes(b.key),
  }))
)

async function handlePageConfigChange(config: any) {
  // 1) 查询条件显隐 → 搜索区真实隐藏
  if (Array.isArray(config?.queryFields)) {
    hiddenSearchFieldKeys.value = config.queryFields
      .filter((f: any) => f.visible === false)
      .map((f: any) => f.key)
  }
  // 2) 功能按钮开关 → 工具栏真实隐藏
  if (Array.isArray(config?.functionButtons)) {
    hiddenToolbarKeys.value = config.functionButtons
      .filter((b: any) => b.enabled === false)
      .map((b: any) => b.key)
  }
  // 3) 打印配置 → 传给打印弹窗真实生效
  if (config?.printConfig) {
    pagePrintConfig.alwaysLastTemplate = config.printConfig.alwaysLastTemplate === true
    pagePrintConfig.linkReturnApply = config.printConfig.linkReturnApply === true
  }
  // 4) 落库（按 Tab 维度保存，避免不同视图互相覆盖）
  try {
    await userPageConfigApi.save('order-center', `page-config-${currentTabKey.value}`, JSON.stringify({
      queryFields: config?.queryFields || pageConfigQueryFields.value,
      functionButtons: config?.functionButtons || pageConfigButtons.value,
      printConfig: config?.printConfig || pagePrintConfig,
    }))
  } catch { /* 静默失败 */ }
}

// ═══ 打印(F8) 快捷键 ═══
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrintF8()
  }
}

onMounted(() => {
  loadPageConfig()
  loadQuerySchemes()
  fetchData()
  window.addEventListener('keydown', handleKeydown)
})
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
</style>
