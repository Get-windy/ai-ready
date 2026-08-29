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
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
              allow-clear
            >
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
            <a-button type="link" size="small" style="padding: 0 4px" @click="handleSaveScheme">
              <PlusOutlined />
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
          </BillDetailTable>
        </template>
      </DocCenterLayout>

      <!-- 页面配置弹窗（对标顶部齿轮） -->
      <PageConfigPanel
        :open="showPageConfig"
        :storage-key="'order-center-page-config'"
        :query-fields-config="pageConfigQueryFields"
        :function-buttons-config="pageConfigButtons"
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
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import { PlusOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import DocCenterLayout from '@/components/DocCenterLayout/DocCenterLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { userPageConfigApi } from '@/api/erp'
import { saleOrderApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type {
  SearchConfigMap, SearchCheckboxConfigMap,
  StatCardConfigMap, ToolbarConfigMap,
} from '@/components/DocCenterLayout/types'

defineOptions({ name: 'OrderCenter' })
const router = useRouter()
const userStore = useUserStore()
const tenantId = computed(() => userStore.tenantId || 1)

// ═══ 阶段 Tab + 维度子 Tab ═══
const mainTabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待审核' },
  { key: 'picking', label: '拣货/发货' },
]
const subTabs = computed(() => {
  if (mainTab.value === 'picking') return [{ key: 'byDoc', label: '按单据' }]
  return [
    { key: 'byDoc', label: '按单据' },
    { key: 'byDate', label: '按时间' },
    { key: 'byRoute', label: '按线路' },
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

const byDocSearchFields: SearchConfigMap = {
  'all.byDoc': [
    { key: 'orderNo', label: '单据编号', type: 'input' },
    { key: 'deliveryRouteName', label: '配送线路', type: 'input' },
    { key: 'customerName', label: '客户', type: 'input' },
    { key: 'salesmanName', label: '经手人', type: 'input' },
    { key: 'status', label: '单据状态', type: 'select', options: [
      { label: '草稿', value: 0 }, { label: '待审核', value: 1 },
      { label: '待发货', value: 2 }, { label: '部分发货', value: 3 },
      { label: '已发货', value: 4 }, { label: '已完成', value: 5 },
      { label: '已取消', value: 6 },
    ]},
    { key: 'settlementMethod', label: '结款方式', type: 'select', options: [] },
    { key: 'supplementType', label: '补单类型', type: 'select', options: [] },
    { key: 'productName', label: '商品', type: 'input' },
    { key: 'generationMethod', label: '产生方式', type: 'select', options: [
      { label: '手工新增', value: 1 }, { label: '订单转', value: 2 }, { label: '补单', value: 3 },
    ]},
    { key: 'remark', label: '单据备注', type: 'input' },
    { key: 'receiverName', label: '收货人', type: 'input' },
    { key: 'receiverPhone', label: '联系电话', type: 'input' },
  ],
  'all.fulfillment': [
    { key: 'orderNo', label: '单据编号', type: 'input' },
    { key: 'customerName', label: '客户', type: 'input' },
    { key: 'salesmanName', label: '经手人', type: 'input' },
    { key: 'status', label: '单据状态', type: 'select', options: [
      { label: '待发货', value: 2 }, { label: '部分发货', value: 3 },
      { label: '已发货', value: 4 }, { label: '已完成', value: 5 },
    ]},
  ],
}
const searchConfig: SearchConfigMap = {
  'all.byDoc': byDocSearchFields['all.byDoc'],
  'all.byDate': byDocSearchFields['all.byDoc'],
  'all.byRoute': byDocSearchFields['all.byDoc'],
  'all.byCustomer': byDocSearchFields['all.byDoc'],
  'all.fulfillment': byDocSearchFields['all.fulfillment'],
  'pending.byDoc': byDocSearchFields['all.byDoc'],
  'pending.byDate': byDocSearchFields['all.byDoc'],
  'pending.byRoute': byDocSearchFields['all.byDoc'],
  'pending.byCustomer': byDocSearchFields['all.byDoc'],
  'picking.byDoc': [
    { key: 'orderNo', label: '单据编号', type: 'input' },
    { key: 'customerName', label: '客户', type: 'input' },
    { key: 'warehouseName', label: '拣货仓库', type: 'input' },
    { key: 'receiverName', label: '收货人', type: 'input' },
  ],
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
}

// ═══ 工具栏 ═══
const toolbarConfig: ToolbarConfigMap = {
  'all.byDoc': [
    { key: 'add', label: '新增', type: 'primary' },
    { key: 'refresh', label: '刷新' },
    { key: 'batchPrint', label: '批量打印' },
    { key: 'productSummary', label: '商品汇总' },
    { key: 'more', label: '更多', dropdownItems: [
      { key: 'batchImport', label: '批量导入' },
      { key: 'logisticsRemark', label: '物流备注' },
      { key: 'printF8', label: '打印(F8)' },
      { key: 'export', label: '导出' },
      { key: 'pageConfig', label: '配置' },
    ]},
  ],
  'all.byDate': [ { key: 'add', label: '新增', type: 'primary' }, { key: 'refresh', label: '刷新' }, { key: 'export', label: '导出' } ],
  'all.byRoute': [ { key: 'add', label: '新增', type: 'primary' }, { key: 'refresh', label: '刷新' }, { key: 'export', label: '导出' } ],
  'all.byCustomer': [ { key: 'add', label: '新增', type: 'primary' }, { key: 'refresh', label: '刷新' }, { key: 'export', label: '导出' } ],
  'all.fulfillment': [ { key: 'refresh', label: '刷新' }, { key: 'export', label: '导出' } ],
  'pending.byDoc': [ { key: 'refresh', label: '刷新' }, { key: 'batchApprove', label: '批量审核' }, { key: 'export', label: '导出' } ],
  'pending.byDate': [ { key: 'refresh', label: '刷新' } ],
  'pending.byRoute': [ { key: 'refresh', label: '刷新' } ],
  'pending.byCustomer': [ { key: 'refresh', label: '刷新' } ],
  'picking.byDoc': [ { key: 'refresh', label: '刷新' }, { key: 'batchShip', label: '批量发货' }, { key: 'export', label: '导出' } ],
}

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

// ═══ 查询方案 ═══
const queryScheme = ref('')
function handleSaveScheme() {
  const name = window.prompt('请输入方案名称：')
  if (!name) return
  queryScheme.value = name
  message.success(`查询方案「${name}」已保存`)
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
  { key: 'customerRemark', title: '客户备注', width: 140, ellipsis: true },
  { key: 'salesmanName', title: '经手人', width: 90 },
  { key: 'deptName', title: '部门', width: 90 },
  { key: 'productAmount', title: '金额', width: 110, align: 'right' },
  { key: 'discountAmount', title: '折后金额', width: 110, align: 'right' },
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
  { key: 'buyerRemark', title: '买家备注', width: 140, ellipsis: true },
  { key: 'orderRemark', title: '卖家备注', width: 140, ellipsis: true },
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

const fulfillmentColumns: DetailColumnConfig[] = [
  { key: 'orderNo', title: '单据编号', width: 150, slotName: 'orderNoCell' },
  { key: 'status', title: '单据状态', width: 90, slotName: 'statusCell' },
  { key: 'originalOrderNo', title: '原始订单', width: 150 },
  { key: 'supplementOrderNo', title: '已发订单', width: 150 },
  { key: 'supplementStatus', title: '补单状态', width: 90 },
  { key: 'originalAmount', title: '原单金额', width: 110, align: 'right' },
  { key: 'remainingUnshippedAmount', title: '剩余未发金额', width: 120, align: 'right' },
  { key: 'originalDiscount', title: '原单优惠', width: 100, align: 'right' },
  { key: 'originalItems', title: '原单商品项', width: 100 },
  { key: 'unshippedItems', title: '未发商品项', width: 100 },
  { key: 'originalQuantity', title: '原单商品数量', width: 120, align: 'right' },
  { key: 'unshippedQuantity', title: '未发商品数量', width: 120, align: 'right' },
  { key: 'customerName', title: '客户', width: 160 },
  { key: 'expectedShipTime', title: '预计发货时间', width: 130 },
  { key: 'action', title: '操作', width: 150, fixed: 'right', slotName: 'actionCell' },
]

const pickingColumns: DetailColumnConfig[] = [
  { key: 'checkbox', title: '', type: 'checkbox', width: 40 },
  { key: 'orderDate', title: '单据日期', width: 110 },
  { key: 'orderNo', title: '单据编号', width: 150, slotName: 'orderNoCell' },
  { key: 'status', title: '单据状态', width: 90, slotName: 'statusCell' },
  { key: 'printCount', title: '打印次数', width: 80 },
  { key: 'settlementStatus', title: '结算状态', width: 90 },
  { key: 'pickingWarehouse', title: '拣货仓库', width: 110 },
  { key: 'customerName', title: '客户', width: 160 },
  { key: 'billAmount', title: '销售金额', width: 110, align: 'right' },
  { key: 'volume', title: '体积(m³)', width: 90, align: 'right' },
  { key: 'weight', title: '重量(kg)', width: 90, align: 'right' },
  { key: 'lineCount', title: '商品行数', width: 90 },
  { key: 'totalQuantity', title: '商品数量', width: 90, align: 'right' },
  { key: 'pickedQuantity', title: '已拣货数量', width: 100, align: 'right' },
  { key: 'unpickedQuantity', title: '未拣货数量', width: 100, align: 'right' },
  { key: 'shippedQuantity', title: '已发货数量', width: 100, align: 'right' },
  { key: 'unshippedQuantity', title: '未发货数量', width: 100, align: 'right' },
  { key: 'receiverName', title: '收货人', width: 90 },
  { key: 'deliveryMethod', title: '配送方式', width: 90 },
  { key: 'logisticsCompany', title: '物流公司', width: 100 },
  { key: 'driverName', title: '配送司机', width: 90 },
  { key: 'action', title: '操作', width: 150, fixed: 'right', slotName: 'actionCell' },
]

// 按时间/线路/客户 维度用精简分组列
const groupColumns: DetailColumnConfig[] = [
  { key: 'period', title: '期间/线路/客户', width: 200 },
  { key: 'orderCount', title: '订单数', width: 100, align: 'right' },
  { key: 'billAmount', title: '本单金额', width: 120, align: 'right' },
  { key: 'settledAmount', title: '已结金额', width: 120, align: 'right' },
  { key: 'totalQuantity', title: '订货数量', width: 100, align: 'right' },
]

const activeColumns = computed(() => {
  if (mainTab.value === 'picking') return pickingColumns
  if (subTab.value === 'fulfillment') return fulfillmentColumns
  if (subTab.value === 'byDate' || subTab.value === 'byRoute' || subTab.value === 'byCustomer') return groupColumns
  return byDocColumns
})
const storageKey = computed(() => `order-center-${mainTab.value}-${subTab.value}`)

// ═══ 数据加载 ═══
const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = { ...searchForm, current: paginationConfig.current, size: paginationConfig.pageSize, tenantId: tenantId.value }
    if (dateRange.value) {
      params.dateStart = dateRange.value[0].format('YYYY-MM-DD')
      params.dateEnd = dateRange.value[1].format('YYYY-MM-DD')
    }
    // 阶段过滤
    if (mainTab.value === 'pending') params.status = 1
    if (mainTab.value === 'picking') params.status = 2
    Object.keys(params).forEach(k => { if (params[k] === undefined || params[k] === null || params[k] === '') delete params[k] })

    let res: any
    const isDoc = subTab.value === 'byDoc'
    if (mainTab.value === 'pending' && isDoc) {
      res = await saleOrderApi.getPendingReviewPage({ ...params, pageNum: params.current, pageSize: params.size })
    } else if (mainTab.value === 'picking' && isDoc) {
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
    tableData.value = data?.records || data?.list || []
    paginationConfig.total = Number(data?.total) || 0
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

// Tab 切换
watch([mainTab, subTab], () => {
  paginationConfig.current = 1
  fetchData()
})

// ═══ 工具栏操作 ═══
const tableRef = ref<InstanceType<typeof BillDetailTable>>()
const getSelectedIds = (): number[] => {
  const records = tableRef.value?.getCheckedRecords?.() || []
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
    case 'printF8': await handleBatchPrint(); break
    case 'export': handleExport(); break
    case 'pageConfig': showPageConfig.value = true; break
    case 'batchApprove': await handleBatchApprove(); break
    case 'batchShip': await handleBatchShip(); break
  }
}

// 批量打印（增加打印次数）
const handleBatchPrint = async () => {
  const ids = getSelectedIds()
  if (!ids.length) { message.warning('请先勾选要打印的单据'); return }
  try {
    await saleOrderApi.batchPrint(ids)
    message.success('打印成功（已登记打印次数）'); fetchData()
  } catch { message.error('打印失败') }
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

const handleExport = async () => {
  try {
    const blob = await request.get('/erp/sale/order/export', { params: { ...searchForm }, responseType: 'blob' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `订单处理中心_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
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
const handlePrint = (record: any) => { message.info(`打印订单 ${record.orderNo}`) }

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

const handleError = (err: any) => { console.warn('[订单处理中心] ErrorBoundary:', err) }

// ═══ 页面配置（对标：查询条件显隐 + 功能按钮） ═══
const showPageConfig = ref(false)
const pageConfigQueryFields = ref([
  { key: 'orderNo', label: '单据编号', visible: true },
  { key: 'deliveryRouteName', label: '配送线路', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'salesmanName', label: '经手人', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'settlementMethod', label: '结款方式', visible: false },
  { key: 'supplementType', label: '补单类型', visible: false },
  { key: 'productName', label: '商品', visible: false },
  { key: 'generationMethod', label: '产生方式', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'receiverName', label: '收货人', visible: false },
  { key: 'receiverPhone', label: '联系电话', visible: false },
])
const pageConfigButtons = ref([
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'productSummary', label: '商品汇总', enabled: true },
  { key: 'pageConfig', label: '配置', enabled: true },
])

async function handlePageConfigChange(config: any) {
  try {
    await userPageConfigApi.save('order-center', 'page-config', JSON.stringify({
      queryFields: config.queryFields || pageConfigQueryFields.value,
      functionButtons: config.functionButtons || pageConfigButtons.value,
    }))
  } catch { /* 静默失败 */ }
}

onMounted(() => {
  fetchData()
})
onUnmounted(() => {})
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
</style>
