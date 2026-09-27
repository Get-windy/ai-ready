<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        采购订货收货（配发收 → 收货业务 → 采购订货收货，菜单 70161；对标 ql361 menuId 40001 / billType 230101）
        · 页面 = 采购收货处理工作台：跟踪「应收/已收/未收数量」（按单据）与「订货/已收货/待收货数量」（按明细）
        · 双 Tab：按单据（对标 28 列/默认 11）、按明细（对标 51 列/默认 16），各自独立列配置与页面配置
        · 数据来源复用既有接口：/erp/purchase/order/doc-query/page（采购订单）与 /detail-query/page（订单明细）
        · 批量收货：按选中单据生成《采购入库单》（inboundApi.createFromOrder → 采购入库单页确认收货/记账）
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：批量收货 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('batchReceive')"
              type="primary"
              size="small"
              class="btn-receive"
              :disabled="selectedRows.length === 0"
              @click="handleBatchReceive"
            >
              <InboxOutlined /> 批量收货<template v-if="selectedRows.length">({{ selectedRows.length }})</template>
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：配置 + 刷新 + 打印(F8) + 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleSearch"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('printF8')"
              size="small"
              @click="handlePrintF8"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（按 Tab 配置驱动：按单据 13 项 / 按明细 11 项） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template
                v-for="field in renderQueryFields"
                :key="field.key"
              >
                <div class="search-item">
                  <span class="search-label">{{ field.label }}</span>
                  <!-- 日期：区间 -->
                  <a-range-picker
                    v-if="field.key === 'date'"
                    v-model:value="searchForm.dateRange"
                    size="small"
                    style="width: 230px"
                    value-format="YYYY-MM-DD"
                    @change="handleSearch"
                  />
                  <a-select
                    v-else-if="field.type === 'select'"
                    v-model:value="searchForm[field.key]"
                    size="small"
                    style="width: 130px"
                    allow-clear
                    :placeholder="field.placeholder || '全部'"
                    :options="field.options || []"
                    @change="handleSearch"
                  />
                  <a-input-number
                    v-else-if="field.type === 'number'"
                    v-model:value="searchForm[field.key]"
                    size="small"
                    style="width: 130px"
                    :placeholder="field.placeholder || '请输入'"
                  />
                  <!-- 自定义数字字段：最小 / 最大区间 -->
                  <span
                    v-else-if="field.type === 'numberRange'"
                    class="search-range"
                  >
                    <a-input-number
                      v-model:value="searchForm[field.key + 'Min']"
                      size="small"
                      style="width: 74px"
                      placeholder="最小"
                    />
                    <span class="range-sep">~</span>
                    <a-input-number
                      v-model:value="searchForm[field.key + 'Max']"
                      size="small"
                      style="width: 74px"
                      placeholder="最大"
                    />
                  </span>
                  <a-input
                    v-else
                    v-model:value="searchForm[field.key]"
                    size="small"
                    style="width: 150px"
                    allow-clear
                    :placeholder="field.placeholder || '请输入'"
                    @press-enter="handleSearch"
                  />
                </div>
              </template>
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
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              ref="tableListRef"
              :columns="currentColumns"
              :data-source="tableData"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :storage-key="tableStorageKey"
              :global-config-key="tableStorageKey"
              @selection-change="handleSelectionChange"
            >
              <template #orderNoCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleOpenDetail(record)"
                >{{ record.orderNo }}</a>
              </template>

              <template #receiptStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="RECEIPT_STATUS_MAP[receiptStatusOf(record)]?.color || 'default'"
                >
                  {{ RECEIPT_STATUS_MAP[receiptStatusOf(record)]?.label }}
                </a-tag>
              </template>

              <template #orderStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="ORDER_STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ ORDER_STATUS_MAP[record.status]?.label || record.status }}
                </a-tag>
              </template>

              <template #settleStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="SETTLE_STATUS_MAP[settleStatusOf(record)]?.color || 'default'"
                >
                  {{ SETTLE_STATUS_MAP[settleStatusOf(record)]?.label }}
                </a-tag>
              </template>

              <template #attachmentCell="{ record }">
                <span v-if="!record.__ghost">{{ record.attachment ? '有' : '-' }}</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="handleOpenDetail(record)"
                  >
                    查看
                  </a-button>
                  <a-button
                    v-if="activeTab === 'doc'"
                    type="link"
                    size="small"
                    @click="handleReceiveOne(record)"
                  >
                    收货
                  </a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏 ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 打印（模板渲染） ═══ -->
      <PrintDialog
        ref="printDialogRef"
        page-code="purchase-receive"
        :document-id="printData.id ?? printData.orderId"
        :print-data="printData"
        @print-success="handlePrintSuccess"
      />

      <!-- ═══ 采购订单明细（查看） ═══ -->
      <a-modal
        v-model:open="showDetail"
        title="采购订单明细"
        :width="920"
        :footer="null"
      >
        <a-spin :spinning="detailLoading">
          <a-descriptions
            bordered
            size="small"
            :column="3"
          >
            <a-descriptions-item label="单据编号">
              {{ detail.orderNo || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="单据日期">
              {{ formatDate(detail.orderDate) }}
            </a-descriptions-item>
            <a-descriptions-item label="仓库">
              {{ detail.warehouseName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="往来单位">
              {{ detail.supplierName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="经手人">
              {{ detail.purchaserName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="预计收货日期">
              {{ formatDate(detail.expectedReceiveTime) }}
            </a-descriptions-item>
            <a-descriptions-item label="应收数量">
              {{ num(detail.totalQuantity) }}
            </a-descriptions-item>
            <a-descriptions-item label="已收数量">
              {{ num(detail.receivedQuantity) }}
            </a-descriptions-item>
            <a-descriptions-item label="未收数量">
              {{ num(detail.unreceiveQuantity) }}
            </a-descriptions-item>
          </a-descriptions>
          <a-table
            class="detail-items"
            :data-source="detailItems"
            :columns="detailItemColumns"
            size="small"
            row-key="rowKey"
            :pagination="false"
            :scroll="{ y: 320 }"
          />
        </a-spin>
      </a-modal>

      <!-- ═══ 页面配置（当前 Tab 的查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="activeQueryConfig"
        :function-buttons-config="activeFunctionButtons"
        :default-query-fields-config="activeDefaultQueryFields"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="activePageStorageKey"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  InboxOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import * as XLSX from 'xlsx'
import dayjs from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { purchaseDocQueryApi } from '@/api/purchase'
import { inboundApi, purchaseOrderApi } from '@/api/erp'

defineOptions({ name: 'DispatchPurchaseReceive' })

const TABS = [
  { key: 'doc', label: '按单据' },
  { key: 'detail', label: '按明细' },
]
const activeTab = ref('doc')

// ═══ 状态口径 ═══
/** 采购订单原始状态（0草稿/1待审批/2已审批/3已下达/4已取消/5履行中/6已完成） */
const ORDER_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '已下达', color: 'geekblue' },
  4: { label: '已取消', color: 'red' },
  5: { label: '履行中', color: 'processing' },
  6: { label: '已完成', color: 'green' },
}
/** 收货进度（本页「单据状态」列口径，见开发文档 §4：待收货/部分收货/已收货） */
const RECEIPT_STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待收货', color: 'orange' },
  PARTIAL: { label: '部分收货', color: 'processing' },
  RECEIVED: { label: '已收货', color: 'green' },
}
/** 结算状态（按「已结金额 / 本单金额」推导，见开发文档 §6 口径说明） */
const SETTLE_STATUS_MAP: Record<string, { label: string; color: string }> = {
  unsettled: { label: '未结算', color: 'default' },
  partial: { label: '部分结算', color: 'orange' },
  settled: { label: '已结算', color: 'green' },
}

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
const selectedRows = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 查询条件（按 Tab 各自维护；字段与后端 doc-query / detail-query DTO 一一对应） ═══
interface QueryFieldDef {
  label: string
  /** numberRange：自定义数字字段用「最小 / 最大」区间（与《采购单据查询》同口径） */
  type: 'input' | 'select' | 'number' | 'numberRange'
  options?: Array<{ label: string; value: any }>
}
const STATUS_OPTIONS = Object.keys(ORDER_STATUS_MAP).map(k => ({ label: ORDER_STATUS_MAP[Number(k)].label, value: Number(k) }))

const DOC_QUERY_DEFS: Record<string, QueryFieldDef> = {
  orderNo: { label: '单据编号', type: 'input' },
  supplierName: { label: '往来单位', type: 'input' },
  purchaserName: { label: '经手人', type: 'input' },
  createByName: { label: '制单人', type: 'input' },
  warehouseName: { label: '仓库', type: 'input' },
  status: { label: '单据状态', type: 'select', options: STATUS_OPTIONS },
  remark: { label: '单据备注', type: 'input' },
  extNum1: { label: '自定义字段1(数字)', type: 'numberRange' },
  extNum2: { label: '自定义字段2(数字)', type: 'numberRange' },
  extText1: { label: '自定义字段3(文本)', type: 'input' },
  extText2: { label: '自定义字段4(文本)', type: 'input' },
  extText3: { label: '自定义字段5(文本)', type: 'input' },
}
const DETAIL_QUERY_DEFS: Record<string, QueryFieldDef> = {
  orderNo: { label: '单据编号', type: 'input' },
  productName: { label: '商品', type: 'input' },
  supplierName: { label: '往来单位', type: 'input' },
  purchaserName: { label: '经手人', type: 'input' },
  deptName: { label: '部门', type: 'input' },
  createByName: { label: '制单人', type: 'input' },
  warehouseName: { label: '仓库', type: 'input' },
  status: { label: '单据状态', type: 'select', options: STATUS_OPTIONS },
  itemRemark: { label: '明细备注', type: 'input' },
  remark: { label: '单据备注', type: 'input' },
}

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

/** 对标实测：按单据 13 项查询条件（含「日期」） */
const DEFAULT_DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'orderNo', label: '单据编号', visible: true },
  { key: 'supplierName', label: '往来单位', visible: true },
  { key: 'purchaserName', label: '经手人', visible: true },
  { key: 'createByName', label: '制单人', visible: false },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'remark', label: '单据备注', visible: false },
  { key: 'extNum1', label: '自定义字段1(数字)', visible: false },
  { key: 'extNum2', label: '自定义字段2(数字)', visible: false },
  { key: 'extText1', label: '自定义字段3(文本)', visible: false },
  { key: 'extText2', label: '自定义字段4(文本)', visible: false },
  { key: 'extText3', label: '自定义字段5(文本)', visible: false },
]
/** 对标实测：按明细 11 项查询条件（含「日期」） */
const DEFAULT_DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'orderNo', label: '单据编号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'supplierName', label: '往来单位', visible: true },
  { key: 'purchaserName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: false },
  { key: 'createByName', label: '制单人', visible: false },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'itemRemark', label: '明细备注', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
]
/** 对标实测：两个 Tab 均为 刷新 / 批量收货 / 打印(F8) / 导出（「配置」即齿轮，恒显） */
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'batchReceive', label: '批量收货', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const docQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DOC_QUERY_FIELDS.map(f => ({ ...f })))
const detailQueryConfig = ref<QueryFieldSetting[]>(DEFAULT_DETAIL_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const showPageConfig = ref(false)

const activeQueryConfig = computed({
  get: () => (activeTab.value === 'doc' ? docQueryConfig.value : detailQueryConfig.value),
  set: (v: QueryFieldSetting[]) => {
    if (activeTab.value === 'doc') docQueryConfig.value = v
    else detailQueryConfig.value = v
  },
})
const activeDefaultQueryFields = computed(() =>
  activeTab.value === 'doc' ? DEFAULT_DOC_QUERY_FIELDS : DEFAULT_DETAIL_QUERY_FIELDS)
/**
 * 功能按钮「当前配置」透传给 PageConfigPanel。
 * 必须显式定义：若模板引用未定义变量，PageConfigPanel 的 activeBaseFunctionButtons 会回落到
 * 其内置默认（销售订单按钮集），一旦有本地存储配置就会把本页按钮替换成「新增/审核通过/批量发货」等。
 */
const activeFunctionButtons = computed({
  get: () => functionButtons.value,
  set: (v: FunctionButtonSetting[]) => { functionButtons.value = v },
})
const activePageStorageKey = computed(() =>
  activeTab.value === 'doc' ? 'dispatch-purchase-receive-page-config' : 'dispatch-purchase-receive-page-config-detail')
const tableStorageKey = computed(() =>
  activeTab.value === 'doc' ? 'dispatch-purchase-receive-columns-doc' : 'dispatch-purchase-receive-columns-detail')

const activeDefs = computed(() => (activeTab.value === 'doc' ? DOC_QUERY_DEFS : DETAIL_QUERY_DEFS))

const renderQueryFields = computed(() =>
  activeQueryConfig.value
    .filter(f => f.visible && (f.key === 'date' || activeDefs.value[f.key]))
    .map(f => ({
      key: f.key,
      label: f.label,
      ...(activeDefs.value[f.key] || { type: 'input' as const }),
    })))

function isButtonEnabled(key: string): boolean {
  const hit = functionButtons.value.find(b => b.key === key)
  return hit ? hit.enabled : false
}

function handlePageConfigChange(config: any) {
  if (config?.queryFields) activeQueryConfig.value = config.queryFields.map((f: any) => ({ ...f }))
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map((b: any) => ({ ...b }))
}

// ═══ 查询表单 ═══
const searchForm = reactive<Record<string, any>>({ dateRange: [] })
for (const key of new Set([...Object.keys(DOC_QUERY_DEFS), ...Object.keys(DETAIL_QUERY_DEFS)])) {
  searchForm[key] = undefined
  searchForm[`${key}Min`] = undefined
  searchForm[`${key}Max`] = undefined
}

function buildQueryParams(): Record<string, any> {
  const params: Record<string, any> = {}
  if (searchForm.dateRange?.length === 2) {
    params.dateStart = searchForm.dateRange[0]
    params.dateEnd = searchForm.dateRange[1]
  }
  for (const key of Object.keys(activeDefs.value)) {
    // 自定义数字字段：后端口径为区间（extNum1Start / extNum1End）
    if (activeDefs.value[key].type === 'numberRange') {
      const min = searchForm[`${key}Min`]
      const max = searchForm[`${key}Max`]
      if (min !== undefined && min !== null && min !== '') params[`${key}Start`] = min
      if (max !== undefined && max !== null && max !== '') params[`${key}End`] = max
      continue
    }
    const val = searchForm[key]
    if (val !== undefined && val !== null && val !== '') params[key] = val
  }
  return params
}

// ═══ 列定义（对标实测） ═══
const num = (v: any) => (v === null || v === undefined || v === '' ? '-' : Number(v).toLocaleString('zh-CN'))
const money = (v: any) => (v === null || v === undefined || v === '' || isNaN(Number(v)) ? '-' : Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }))
const formatDate = (v: any) => (v ? String(v).replace('T', ' ').slice(0, 10) : '-')
const formatDateTime = (v: any) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')

/** 按单据 Tab（对标 28 列；「结算单位/结算单位编号」本系统采购订单无该口径，见文档 §6.3） */
const docColumns: any[] = [
  { title: '', key: 'checkbox', type: 'checkbox', width: 40, fixed: 'left' },
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  // ── 默认显示（对标 11；本系统 10，因无「结算单位」） ──
  { title: '单据日期', key: 'orderDate', width: 110, formatter: (v: any) => formatDate(v) },
  { title: '单据编号', key: 'orderNo', type: 'slot', slotName: 'orderNoCell', width: 165 },
  { title: '单据状态', key: 'receiptStatus', type: 'slot', slotName: 'receiptStatusCell', width: 100 },
  { title: '结算状态', key: 'settleStatus', type: 'slot', slotName: 'settleStatusCell', width: 100 },
  { title: '仓库', key: 'warehouseName', width: 120 },
  { title: '往来单位', key: 'supplierName', width: 180 },
  { title: '应收数量', key: 'totalQuantity', width: 100, align: 'right', formatter: (v: any) => num(v) },
  { title: '未收数量', key: 'unreceiveQuantity', width: 100, align: 'right', formatter: (v: any) => num(v) },
  { title: '预计收货日期', key: 'expectedReceiveTime', width: 120, formatter: (v: any) => formatDate(v) },
  { title: '打印次数', key: 'printCount', width: 90, align: 'right' },
  // ── 默认隐藏 ──
  { title: '往来编号', key: 'supplierCode', width: 110, defaultHidden: true },
  { title: '经手人', key: 'purchaserName', width: 100, defaultHidden: true },
  { title: '部门', key: 'deptName', width: 110, defaultHidden: true },
  { title: '已收数量', key: 'receivedQuantity', width: 100, align: 'right', defaultHidden: true, formatter: (v: any) => num(v) },
  { title: '订单金额', key: 'billAmount', width: 120, align: 'right', defaultHidden: true, formatter: (v: any) => money(v) },
  { title: '重量（kg）', key: 'weight', width: 100, align: 'right', defaultHidden: true, formatter: (v: any) => num(v) },
  { title: '体积（m³）', key: 'volume', width: 100, align: 'right', defaultHidden: true, formatter: (v: any) => num(v) },
  { title: '单据备注', key: 'remark', width: 150, defaultHidden: true },
  { title: '表头自定义字段1(数字)', key: 'extNum1', width: 150, defaultHidden: true },
  { title: '表头自定义字段2(数字)', key: 'extNum2', width: 150, defaultHidden: true },
  { title: '表头自定义字段3(文本)', key: 'extText1', width: 150, defaultHidden: true },
  { title: '表头自定义字段4(文本)', key: 'extText2', width: 150, defaultHidden: true },
  { title: '表头自定义字段5(文本)', key: 'extText3', width: 150, defaultHidden: true },
  { title: '摘要', key: 'summary', width: 150, defaultHidden: true },
  { title: '附件', key: 'attachment', type: 'slot', slotName: 'attachmentCell', width: 80, defaultHidden: true },
  { title: '制单人', key: 'createByName', width: 90, defaultHidden: true },
  // 本系统补充（隐藏）：采购订单原始状态便于与《采购订单》页对照
  { title: '订单状态', key: 'status', type: 'slot', slotName: 'orderStatusCell', width: 100, defaultHidden: true },
]

/** 按明细 Tab（对标 51 列） */
const detailColumns: any[] = [
  { title: '', key: 'checkbox', type: 'checkbox', width: 40, fixed: 'left' },
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 90, fixed: 'left' },
  // ── 默认显示（对标 16） ──
  { title: '单据日期', key: 'orderDate', width: 110, formatter: (v: any) => formatDate(v) },
  { title: '单据编号', key: 'orderNo', type: 'slot', slotName: 'orderNoCell', width: 165 },
  { title: '单据状态', key: 'receiptStatus', type: 'slot', slotName: 'receiptStatusCell', width: 100 },
  { title: '结算状态', key: 'settleStatus', type: 'slot', slotName: 'settleStatusCell', width: 100 },
  { title: '仓库', key: 'warehouseName', width: 110 },
  { title: '往来单位', key: 'supplierName', width: 160 },
  { title: '经手人', key: 'purchaserName', width: 100 },
  { title: '商品名称', key: 'productName', width: 190 },
  { title: '单位', key: 'unit', width: 70 },
  { title: '订货数量', key: 'quantity', width: 100, align: 'right', formatter: (v: any) => num(v) },
  { title: '已收货数量', key: 'receivedQuantity', width: 110, align: 'right', formatter: (v: any) => num(v) },
  { title: '待收货数量', key: 'unreceiveQuantity', width: 110, align: 'right', formatter: (v: any) => num(v) },
  { title: '重量（kg）', key: 'weight', width: 100, align: 'right', formatter: (v: any) => num(v) },
  { title: '体积（m³）', key: 'volume', width: 100, align: 'right', formatter: (v: any) => num(v) },
  { title: '明细备注', key: 'itemRemark', width: 140 },
  { title: '附件', key: 'attachment', type: 'slot', slotName: 'attachmentCell', width: 80 },
  // ── 默认隐藏（对标其余 35 列） ──
  { title: '往来编号', key: 'supplierCode', width: 110, defaultHidden: true },
  { title: '部门', key: 'deptName', width: 110, defaultHidden: true },
  { title: '货号', key: 'itemCode', width: 110, defaultHidden: true },
  { title: '条码', key: 'barcode', width: 120, defaultHidden: true },
  { title: '规格', key: 'specification', width: 100, defaultHidden: true },
  { title: '型号', key: 'model', width: 90, defaultHidden: true },
  { title: '产地', key: 'origin', width: 90, defaultHidden: true },
  { title: '品牌', key: 'brand', width: 90, defaultHidden: true },
  { title: '单据自定义1(数字字段)', key: 'customField1', width: 150, defaultHidden: true },
  { title: '单据自定义2(数字字段)', key: 'customField2', width: 150, defaultHidden: true },
  { title: '单据自定义3(数字字段)', key: 'customField3', width: 150, defaultHidden: true },
  { title: '单据自定义4(文本字段)', key: 'customField4', width: 150, defaultHidden: true },
  { title: '单据自定义5(文本字段)', key: 'customField5', width: 150, defaultHidden: true },
  { title: '单据自定义6(数字字段)', key: 'customField6', width: 150, defaultHidden: true },
  { title: '单据自定义7(数字字段)', key: 'customField7', width: 150, defaultHidden: true },
  { title: '单据自定义8(往来单位)', key: 'customField8', width: 150, defaultHidden: true },
  { title: '单据自定义9(职员)', key: 'customField9', width: 150, defaultHidden: true },
  { title: '单据自定义10(部门)', key: 'customField10', width: 150, defaultHidden: true },
  { title: '小单位', key: 'smallUnit', width: 80, defaultHidden: true },
  { title: '小单位数量', key: 'smallUnitQuantity', width: 110, align: 'right', defaultHidden: true, formatter: (v: any) => num(v) },
  { title: '换算关系', key: 'conversionRelation', width: 110, defaultHidden: true },
  { title: '换算结果', key: 'convertedQuantity', width: 100, align: 'right', defaultHidden: true, formatter: (v: any) => num(v) },
  { title: '大包装', key: 'bigPack', width: 80, align: 'right', defaultHidden: true, formatter: (v: any) => num(v) },
  { title: '中包装', key: 'midPack', width: 80, align: 'right', defaultHidden: true, formatter: (v: any) => num(v) },
  { title: '小包装', key: 'smallPack', width: 80, align: 'right', defaultHidden: true, formatter: (v: any) => num(v) },
  { title: '单价', key: 'unitPrice', width: 100, align: 'right', defaultHidden: true, formatter: (v: any) => money(v) },
  { title: '小单位单价', key: 'smallUnitPrice', width: 110, align: 'right', defaultHidden: true, formatter: (v: any) => money(v) },
  { title: '金额', key: 'amount', width: 110, align: 'right', defaultHidden: true, formatter: (v: any) => money(v) },
  { title: '优惠折扣(%)', key: 'discountRate', width: 110, align: 'right', defaultHidden: true },
  { title: '优惠后单价', key: 'discountedUnitPrice', width: 110, align: 'right', defaultHidden: true, formatter: (v: any) => money(v) },
  { title: '优惠后金额', key: 'discountedAmount', width: 110, align: 'right', defaultHidden: true, formatter: (v: any) => money(v) },
  { title: '单据备注', key: 'remark', width: 140, defaultHidden: true },
  { title: '摘要', key: 'summary', width: 140, defaultHidden: true },
  { title: '制单人', key: 'createByName', width: 90, defaultHidden: true },
  { title: '打印次数', key: 'printCount', width: 90, align: 'right', defaultHidden: true },
  { title: '订单状态', key: 'status', type: 'slot', slotName: 'orderStatusCell', width: 100, defaultHidden: true },
]

const currentColumns = computed(() => (activeTab.value === 'doc' ? docColumns : detailColumns))

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const params = { ...buildQueryParams(), current: pagination.current, size: pagination.pageSize }
    const res: any = activeTab.value === 'doc'
      ? await purchaseDocQueryApi.docPage(params)
      : await purchaseDocQueryApi.detailPage(params)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[采购订货收货] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

function handleTabChange(key: string) {
  if (key === activeTab.value) return
  activeTab.value = key
  clearSelection()
  pagination.current = 1
  fetchList()
}

function handleSearch() {
  // 查询条件变化后旧选中行已不在当前结果集中，必须清除，避免误对旧单据执行批量收货
  clearSelection()
  pagination.current = 1
  fetchList()
}

function handleReset() {
  searchForm.dateRange = []
  for (const key of Object.keys(activeDefs.value)) {
    searchForm[key] = undefined
    searchForm[`${key}Min`] = undefined
    searchForm[`${key}Max`] = undefined
  }
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

/**
 * 选中行：按「行对象」记录（BillTableList 回传的 ids 取 record.id，
 * 而按明细行的主键是 itemId/orderId，无 id 字段，故不以 ids 为准）
 */
function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows || []
}

/** 表实例：清除表格内部勾选态（页面只维护 selectedRows，不同步清会导致勾选残留） */
const tableListRef = ref<any>(null)
function clearSelection() {
  selectedRows.value = []
  tableListRef.value?.clearSelection?.()
}

/**
 * 雪花ID 一律按字符串透传（JS Number 超 MAX_SAFE_INTEGER 会丢精度：
 * ...000702 被舍入成 ...000800，后端会报「采购订单不存在」）
 */
function toId(v: any): string | undefined {
  if (v === null || v === undefined || v === '') return undefined
  return String(v)
}

// ═══ 收货进度 / 结算状态（推导口径，见开发文档 §6.2） ═══
function receiptStatusOf(record: any): string {
  const total = Number(record?.totalQuantity ?? record?.quantity ?? 0)
  const received = Number(record?.receivedQuantity ?? 0)
  if (received <= 0) return 'PENDING'
  if (total > 0 && received >= total) return 'RECEIVED'
  return 'PARTIAL'
}
function settleStatusOf(record: any): string {
  const amount = Number(record?.billAmount ?? record?.amount ?? 0)
  const settled = Number(record?.settledAmount ?? 0)
  if (settled <= 0) return 'unsettled'
  if (amount > 0 && settled >= amount) return 'settled'
  return 'partial'
}

// ═══ 批量收货 / 单条收货 → 生成《采购入库单》 ═══
async function receiveOrders(orderIds: string[]) {
  const succeeded: string[] = []
  const failed: string[] = []
  for (const id of orderIds) {
    try {
      const res: any = await inboundApi.createFromOrder(id)
      succeeded.push(res?.inboundNo || res?.billNo || String(id))
    } catch (error: any) {
      failed.push(`${id}: ${error?.response?.data?.message || error?.message || '失败'}`)
    }
  }
  return { succeeded, failed }
}

function handleBatchReceive() {
  // 已全部收完的订单不再生成空入库单（后端也只带出「待收数量 > 0」的明细）
  const receivable = selectedRows.value.filter((r: any) => Number(r.unreceiveQuantity ?? 0) > 0)
  const skipped = selectedRows.value.length - receivable.length
  const orderIds = Array.from(new Set(
    receivable.map((r: any) => toId(r.id ?? r.orderId)).filter(Boolean),
  )) as string[]
  if (skipped > 0) {
    message.info(`已跳过 ${skipped} 张已收满的订单`)
  }
  if (!orderIds.length) {
    message.warning(selectedRows.value.length ? '勾选的订单均已收满，无需收货' : '请先勾选要收货的采购订单')
    return
  }
  Modal.confirm({
    title: '批量收货',
    content: `将按 ${orderIds.length} 张采购订单生成《采购入库单》，确认继续？`,
    okText: '确认收货',
    onOk: async () => {
      loading.value = true
      try {
        const { succeeded, failed } = await receiveOrders(orderIds)
        if (failed.length) {
          Modal.warning({
            title: '收货结果',
            content: `成功 ${succeeded.length} 张，失败 ${failed.length} 张：\n${failed.join('\n')}`,
          })
        } else {
          message.success(`已生成 ${succeeded.length} 张采购入库单，请到《采购入库单》确认收货/记账`)
        }
        clearSelection()
        fetchList()
      } finally {
        loading.value = false
      }
    },
  })
}

function handleReceiveOne(record: any) {
  const id = toId(record?.id ?? record?.orderId)
  if (!id) {
    message.warning('该行缺少采购订单ID，无法收货')
    return
  }
  Modal.confirm({
    title: '收货',
    content: `将按采购订单「${record.orderNo}」生成《采购入库单》，确认继续？`,
    okText: '确认收货',
    onOk: async () => {
      const { succeeded, failed } = await receiveOrders([id as string])
      if (failed.length) message.error(`收货失败：${failed[0]}`)
      else message.success('已生成采购入库单，请到《采购入库单》确认收货/记账')
      fetchList()
    },
  })
}

// ═══ 明细查看 ═══
const showDetail = ref(false)
const detailLoading = ref(false)
const detail = ref<any>({})
const detailItems = ref<any[]>([])
const detailItemColumns: any[] = [
  { title: '商品名称', dataIndex: 'productName', width: 200 },
  { title: '货号', dataIndex: 'itemCode', width: 110 },
  { title: '规格', dataIndex: 'specification', width: 100 },
  { title: '单位', dataIndex: 'unit', width: 70 },
  { title: '订货数量', dataIndex: 'quantity', width: 100, align: 'right' },
  { title: '已收货数量', dataIndex: 'receivedQuantity', width: 110, align: 'right' },
  { title: '待收货数量', dataIndex: 'unreceiveQuantity', width: 110, align: 'right' },
]

async function handleOpenDetail(record: any) {
  showDetail.value = true
  detailLoading.value = true
  detail.value = record || {}
  detailItems.value = []
  const orderId = toId(record?.id ?? record?.orderId)
  if (!orderId) {
    detailLoading.value = false
    return
  }
  try {
    const res: any = await purchaseOrderApi.getById(orderId)
    const head = res?.data || res || {}
    detail.value = { ...record, ...head }
    const items = head.items || head.details || head.orderItems || []
    detailItems.value = Array.isArray(items) ? items.map((it: any, i: number) => ({ ...it, rowKey: it.id ?? i })) : []
  } catch (error) {
    console.warn('[采购订货收货] 订单明细加载失败', error)
  } finally {
    detailLoading.value = false
  }
}

// ═══ 打印(F8) ═══
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({})
/** 本次打印的订单 ID（打印成功后回写打印次数） */
const printingIds = ref<string[]>([])

async function handlePrintF8() {
  if (!selectedRows.value.length) {
    message.warning('请先勾选要打印的采购订单')
    return
  }
  const row = selectedRows.value[0]
  printData.value = { ...row, receiptStatusText: RECEIPT_STATUS_MAP[receiptStatusOf(row)]?.label }
  printingIds.value = selectedRows.value
    .map((r: any) => toId(r.id ?? r.orderId))
    .filter(Boolean) as string[]
  await nextTick()
  printDialogRef.value?.open()
}

/** 打印成功 → 累加订单「打印次数」（后端 batch-print）后刷新列表 */
async function handlePrintSuccess() {
  const ids = printingIds.value
  printingIds.value = []
  if (ids.length) {
    try {
      await purchaseOrderApi.batchPrint(ids)
    } catch (error) {
      console.warn('[采购订货收货] 打印次数回写失败', error)
    }
  }
  fetchList()
}

// ═══ 导出（真实 xlsx：按当前查询条件全量拉取 + 当前 Tab 全部业务列） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params = { ...buildQueryParams(), current: 1, size: 10000 }
    const res: any = activeTab.value === 'doc'
      ? await purchaseDocQueryApi.docPage(params)
      : await purchaseDocQueryApi.detailPage(params)
    const records: any[] = res?.records || []
    if (!records.length) {
      message.warning('没有可导出的数据')
      return
    }
    const visible = currentColumns.value.filter((c: any) => c.type !== 'checkbox' && c.type !== 'rowNo' && c.type !== 'action')
    const rows = records.map((r: any) => {
      const row: Record<string, any> = {}
      for (const col of visible) {
        const raw = r[col.key]
        row[col.title] = col.key === 'receiptStatus' ? RECEIPT_STATUS_MAP[receiptStatusOf(r)]?.label
          : col.key === 'settleStatus' ? SETTLE_STATUS_MAP[settleStatusOf(r)]?.label
            : col.key === 'status' ? (ORDER_STATUS_MAP[Number(r.status)]?.label || r.status)
              : raw
      }
      return row
    })
    const ws = XLSX.utils.json_to_sheet(rows)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, activeTab.value === 'doc' ? '按单据' : '按明细')
    XLSX.writeFile(wb, `采购订货收货_${activeTab.value === 'doc' ? '按单据' : '按明细'}_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

function handleError(error: Error) {
  console.error('[采购订货收货] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

/** F8：打印当前勾选单据（与《订单处理中心》《销售换货单》同口径） */
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrintF8()
  }
}

onMounted(() => {
  fetchList()
  window.addEventListener('keydown', handleKeydown)
})
onUnmounted(() => window.removeEventListener('keydown', handleKeydown))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.search-range { display: inline-flex; align-items: center; gap: 4px; }
.range-sep { color: #999; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.detail-items { margin-top: 12px; }

.btn-receive {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-receive:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
