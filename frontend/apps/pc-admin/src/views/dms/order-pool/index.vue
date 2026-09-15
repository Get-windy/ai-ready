<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        订单池（配送 → 调度管理 → 订单池，菜单 80860）
        · 定位：待分配配送任务的公开池——配送员抢单，或企业设定竞价（价低者得）
        · 对标状态：ql361 无此页面（本系统新增），按《订单池开发文档》金标准实现
        · 状态机：待抢单(0) →启用竞价→ 竞价中(1) →结算→ 已接单(2)；超时→已过期(3)；下架→已下架(4)
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- 工具栏左侧：池生命周期批量操作 -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="btnEnabled('publish')"
              type="primary"
              size="small"
              @click="openPublishModal"
            >
              <PlusOutlined /> 批量发布到池
            </a-button>
            <a-button
              v-if="btnEnabled('offline')"
              size="small"
              :disabled="selectedRowKeys.length === 0"
              @click="handleBatchOffline"
            >
              <StopOutlined /> 批量下架
            </a-button>
            <a-button
              v-if="btnEnabled('expireScan')"
              size="small"
              @click="handleExpireScan"
            >
              <ClockCircleOutlined /> 过期处理
            </a-button>
            <a-button
              v-if="btnEnabled('enableBid')"
              size="small"
              :disabled="!canToggleBid"
              @click="handleBidToggle"
            >
              <ThunderboltOutlined /> {{ bidToggleLabel }}
            </a-button>
          </a-space>
        </template>

        <!-- 工具栏右侧：配置 / 刷新 / 导出 -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              v-if="btnEnabled('config')"
              title="页面配置"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="btnEnabled('export')"
              size="small"
              @click="handleExport"
            >
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- 查询区：横向自适应网格（系统统一版式） -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div
                ref="searchGridRef"
                class="search-grid"
              >
                <div
                  v-if="fieldVisible('taskNo')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchParams.taskNo"
                    size="small"
                    placeholder="任务编号"
                    allow-clear
                  />
                </div>
                <div
                  v-if="fieldVisible('orderNo')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchParams.orderNo"
                    size="small"
                    placeholder="关联订单号"
                    allow-clear
                  />
                </div>
                <div
                  v-if="fieldVisible('customerName')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchParams.customerName"
                    size="small"
                    placeholder="客户"
                    allow-clear
                  />
                </div>
                <div
                  v-if="fieldVisible('orderType')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="searchParams.orderType"
                    size="small"
                    style="width: 100%"
                    placeholder="订单类型"
                    allow-clear
                    :options="ORDER_TYPE_OPTIONS"
                  />
                </div>
                <div
                  v-if="fieldVisible('poolStatus')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="searchParams.poolStatus"
                    size="small"
                    style="width: 100%"
                    placeholder="池状态"
                    allow-clear
                    :options="POOL_STATUS_OPTIONS"
                  />
                </div>
                <div
                  v-if="fieldVisible('bidEnabled')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="searchParams.bidEnabled"
                    size="small"
                    style="width: 100%"
                    placeholder="竞价模式"
                    allow-clear
                    :options="BID_ENABLED_OPTIONS"
                  />
                </div>
                <div
                  v-if="fieldVisible('riderId')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="searchParams.riderId"
                    size="small"
                    style="width: 100%"
                    placeholder="接单配送员"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="riderOptions"
                    :loading="riderLoading"
                  />
                </div>
                <div
                  v-if="fieldVisible('routeId')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="searchParams.routeId"
                    size="small"
                    style="width: 100%"
                    placeholder="配送线路"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="routeOptions"
                    :loading="routeLoading"
                  />
                </div>
                <div
                  v-if="fieldVisible('publishTime')"
                  class="search-field-item"
                >
                  <a-range-picker
                    v-model:value="publishRange"
                    size="small"
                    style="width: 100%"
                    :placeholder="['发布起', '发布止']"
                    @change="handlePublishRangeChange"
                  />
                </div>
                <div
                  v-if="fieldVisible('feeRange')"
                  class="search-field-item fee-range"
                >
                  <a-input-number
                    v-model:value="searchParams.feeMin"
                    size="small"
                    style="width: 100%"
                    placeholder="配送费≥"
                  />
                  <span class="fee-sep">-</span>
                  <a-input-number
                    v-model:value="searchParams.feeMax"
                    size="small"
                    style="width: 100%"
                    placeholder="配送费≤"
                  />
                </div>
                <div
                  ref="searchActionRef"
                  class="search-action-group"
                  :style="{ gridColumn: 'span ' + actionSpan }"
                >
                  <div class="search-field-item search-action-item">
                    <a-button
                      type="primary"
                      size="small"
                      @click="handleSearch"
                    >
                      查询
                    </a-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- 数据表 -->
        <template #table>
          <div class="table-area">
            <BillTableList
              ref="tableRef"
              :columns="columns"
              :data-source="tableData"
              storage-key="dms-order-pool-columns"
              global-config-key="dms-order-pool-columns"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              :summary-data="tableFooterColumns"
              :min-empty-rows="0"
              row-key="id"
              @page-change="handlePageChange"
              @selection-change="handleSelectionChange"
            >
              <template #taskNoCell="{ record }">
                <a-button
                  type="link"
                  size="small"
                  @click="goBidDetail(record)"
                >
                  {{ record.taskNo }}
                </a-button>
              </template>
              <template #poolStatusCell="{ record }">
                <a-tag :color="POOL_STATUS_MAP[record.poolStatus]?.color || 'default'">
                  {{ record.poolStatusText || POOL_STATUS_MAP[record.poolStatus]?.label || '-' }}
                </a-tag>
              </template>
              <template #bidEnabledCell="{ record }">
                <a-tag :color="record.bidEnabled === 1 ? 'purple' : 'default'">
                  {{ record.bidEnabled === 1 ? '竞价' : '抢单' }}
                </a-tag>
              </template>
              <template #remainCell="{ record }">
                <span :class="{ 'remain-warn': record.remainSeconds != null && record.remainSeconds <= 300 }">
                  {{ formatRemain(record) }}
                </span>
              </template>
              <template #moneyCell="{ record, column }">
                <span class="currency-value">{{ formatMoney(record[colKey(column)]) }}</span>
              </template>
              <template #qtyCell="{ record, column }">
                <span class="currency-value">{{ formatQty(record[colKey(column)]) }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button
                    type="link"
                    size="small"
                    @click="goBidDetail(record)"
                  >
                    查看竞标
                  </a-button>
                  <a-button
                    v-if="[0, 1].includes(record.poolStatus)"
                    type="link"
                    size="small"
                    @click="openAssignModal(record)"
                  >
                    强制分配
                  </a-button>
                  <a-button
                    v-if="record.poolStatus === 0"
                    type="link"
                    size="small"
                    @click="openEnableBidModal(record)"
                  >
                    开启竞价
                  </a-button>
                  <a-button
                    v-if="record.poolStatus === 1"
                    type="link"
                    size="small"
                    @click="handleSettle(record)"
                  >
                    结算中标
                  </a-button>
                  <a-button
                    v-if="record.poolStatus === 1"
                    type="link"
                    size="small"
                    @click="handleDisableBid(record)"
                  >
                    关闭竞价
                  </a-button>
                  <a-button
                    v-if="[0, 1].includes(record.poolStatus)"
                    type="link"
                    size="small"
                    @click="openBidModal(record)"
                  >
                    代客出价
                  </a-button>
                  <a-button
                    v-if="[0, 1].includes(record.poolStatus)"
                    type="link"
                    size="small"
                    danger
                    @click="handleOffline(record)"
                  >
                    下架
                  </a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- 页面配置 -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryConfig"
      :function-buttons-config="functionButtonConfig"
      :storage-key="PAGE_CONFIG_STORAGE_KEY"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- 批量发布到池 -->
    <a-modal
      v-model:open="publishModal.open"
      title="发布任务到订单池"
      :width="760"
      :confirm-loading="publishModal.saving"
      @ok="handlePublishConfirm"
    >
      <a-form
        layout="horizontal"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 17 }"
      >
        <a-form-item
          label="待分配任务"
          required
        >
          <a-select
            v-model:value="publishModal.taskIds"
            mode="multiple"
            size="small"
            style="width: 100%"
            placeholder="选择要发布到池的配送任务（状态：待分配）"
            show-search
            option-filter-prop="label"
            :max-tag-count="4"
            :options="publishModal.taskOptions"
            :loading="publishModal.taskLoading"
          />
        </a-form-item>
        <a-form-item label="配送费">
          <a-input-number
            v-model:value="publishModal.deliveryFee"
            size="small"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="不填则沿用任务上的配送费"
          />
        </a-form-item>
        <a-form-item label="同时开启竞价">
          <a-switch
            v-model:checked="publishModal.enableBid"
            size="small"
          />
        </a-form-item>
        <template v-if="publishModal.enableBid">
          <a-form-item label="起拍价">
            <a-input-number
              v-model:value="publishModal.bidStartPrice"
              size="small"
              :min="0"
              :precision="2"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="竞价时长(分钟)">
            <a-input-number
              v-model:value="publishModal.durationMinutes"
              size="small"
              :min="1"
              :max="1440"
              :precision="0"
              style="width: 100%"
            />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>

    <!-- 开启竞价（单条） -->
    <a-modal
      v-model:open="enableBidModal.open"
      title="开启竞价"
      :width="480"
      :confirm-loading="enableBidModal.saving"
      @ok="handleEnableBidConfirm"
    >
      <a-form
        layout="horizontal"
        :label-col="{ span: 7 }"
        :wrapper-col="{ span: 15 }"
      >
        <a-form-item label="任务编号">
          <span>{{ enableBidModal.record?.taskNo }}</span>
        </a-form-item>
        <a-form-item label="起拍价">
          <a-input-number
            v-model:value="enableBidModal.startPrice"
            size="small"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="竞价时长(分钟)">
          <a-input-number
            v-model:value="enableBidModal.durationMinutes"
            size="small"
            :min="1"
            :max="1440"
            :precision="0"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 强制分配 -->
    <a-modal
      v-model:open="assignModal.open"
      title="强制分配（定向指派）"
      :width="480"
      :confirm-loading="assignModal.saving"
      @ok="handleAssignConfirm"
    >
      <a-form
        layout="horizontal"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="任务编号">
          <span>{{ assignModal.record?.taskNo }}</span>
        </a-form-item>
        <a-form-item
          label="配送员"
          required
        >
          <a-select
            v-model:value="assignModal.riderId"
            size="small"
            style="width: 100%"
            placeholder="选择配送员"
            show-search
            option-filter-prop="label"
            :options="riderOptions"
            :loading="riderLoading"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 代客出价 -->
    <a-modal
      v-model:open="bidModal.open"
      title="代客出价（配送员报价）"
      :width="480"
      :confirm-loading="bidModal.saving"
      @ok="handleBidConfirm"
    >
      <a-form
        layout="horizontal"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="任务编号">
          <span>{{ bidModal.record?.taskNo }}</span>
        </a-form-item>
        <a-form-item
          label="配送员"
          required
        >
          <a-select
            v-model:value="bidModal.riderId"
            size="small"
            style="width: 100%"
            placeholder="选择配送员"
            show-search
            option-filter-prop="label"
            :options="riderOptions"
            :loading="riderLoading"
          />
        </a-form-item>
        <a-form-item
          label="报价"
          required
        >
          <a-input-number
            v-model:value="bidModal.price"
            size="small"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined, StopOutlined, ClockCircleOutlined, ThunderboltOutlined,
  SettingOutlined, ReloadOutlined, ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { orderPoolApi, POOL_STATUS_MAP, type DmsOrderPool } from '@/api/dms/order-pool'
import { taskApi } from '@/api/dms/task'
import { mdRouteApi } from '@/api/md'

defineOptions({ name: 'DmsOrderPool' })

const router = useRouter()

const ORDER_TYPE_OPTIONS = [
  { label: '销售配送', value: 1 },
  { label: '调拨', value: 2 },
  { label: '退货', value: 3 },
]
const POOL_STATUS_OPTIONS = Object.entries(POOL_STATUS_MAP).map(([v, o]) => ({ label: o.label, value: Number(v) }))
const BID_ENABLED_OPTIONS = [
  { label: '竞价', value: 1 },
  { label: '抢单', value: 0 },
]

// ═══ 数据与分页 ═══
const loading = ref(false)
const tableData = ref<DmsOrderPool[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

// ═══ 查询区 ═══
const searchGridRef = ref<HTMLElement | null>(null)
const searchActionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(searchActionRef, searchGridRef)

const publishRange = ref<[Dayjs, Dayjs] | null>(null)
const searchParams = reactive({
  taskNo: '',
  orderNo: '',
  customerName: '',
  orderType: undefined as number | undefined,
  poolStatus: undefined as number | undefined,
  bidEnabled: undefined as number | undefined,
  routeId: undefined as number | undefined,
  riderId: undefined as number | undefined,
  publishTimeStart: '',
  publishTimeEnd: '',
  feeMin: undefined as number | undefined,
  feeMax: undefined as number | undefined,
})

// ═══ 页面配置 ═══
const PAGE_CONFIG_STORAGE_KEY = 'dms-order-pool-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'taskNo', label: '任务编号', visible: true },
  { key: 'orderNo', label: '关联订单号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'orderType', label: '订单类型', visible: true },
  { key: 'poolStatus', label: '池状态', visible: true },
  { key: 'bidEnabled', label: '竞价模式', visible: true },
  { key: 'routeId', label: '配送线路', visible: true },
  { key: 'riderId', label: '接单配送员', visible: true },
  { key: 'publishTime', label: '发布时间', visible: true },
  { key: 'feeRange', label: '配送费区间', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'publish', label: '批量发布到池', enabled: true },
  { key: 'offline', label: '批量下架', enabled: true },
  { key: 'expireScan', label: '过期处理', enabled: true },
  { key: 'enableBid', label: '开启/关闭竞价', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const showPageConfig = ref(false)
const queryConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw) as PageConfigData
    if (parsed.queryFields) {
      queryConfig.value = DEFAULT_QUERY_FIELDS.map(df => {
        const saved = parsed.queryFields!.find(f => f.key === df.key)
        return saved ? { ...df, visible: saved.visible !== false } : { ...df }
      })
    }
    if (parsed.functionButtons) {
      functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
        const saved = parsed.functionButtons!.find(f => f.key === bf.key)
        return saved ? { ...bf, enabled: saved.enabled !== false } : { ...bf }
      })
    }
  } catch { /* ignore */ }
}
function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || [],
  }))
  loadPageConfig()
}
function fieldVisible(key: string): boolean {
  return queryConfig.value.find(f => f.key === key)?.visible !== false
}
function btnEnabled(key: string): boolean {
  return functionButtonConfig.value.find(b => b.key === key)?.enabled !== false
}

// ═══ 选择 ═══
const tableRef = ref<any>(null)
const selectedRowKeys = ref<any[]>([])
const selectedRows = ref<DmsOrderPool[]>([])
function handleSelectionChange(rows: any[], ids: any[]) {
  selectedRows.value = rows
  selectedRowKeys.value = ids
}

// ═══ 列定义 ═══
const columns = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', width: 300, fixed: 'right', type: 'action', slotName: 'actionCell' },
  { key: 'taskNo', title: '任务编号', width: 170, type: 'slot', slotName: 'taskNoCell', sortable: true },
  { key: 'orderNo', title: '关联订单号', width: 160 },
  { key: 'orderType', title: '订单类型', width: 100, formatter: (_v: any, r: any) => r.orderTypeText || '-' },
  { key: 'customerName', title: '客户', width: 180 },
  { key: 'sourceAddress', title: '取货地址', width: 200 },
  { key: 'customerAddress', title: '收货地址', width: 200 },
  // 线路/区域与下架留痕：文档 §3.1 默认列之外的可配置列（默认隐藏，表头齿轮可开启）
  {
    key: 'routeArea',
    title: '配送线路/区域',
    width: 180,
    defaultHidden: true,
    formatter: (_v: any, r: any) => {
      const routeName = r.routeId ? routeNameMap.value[r.routeId] : ''
      return [routeName, r.routeArea].filter(Boolean).join(' / ') || '-'
    },
  },
  { key: 'offlineReason', title: '下架原因', width: 160, defaultHidden: true },
  { key: 'offlineTime', title: '下架时间', width: 150, defaultHidden: true, formatter: (v: any) => formatDateTime(v) },
  { key: 'estimatedDistance', title: '距离(km)', width: 100, align: 'right', type: 'slot', slotName: 'qtyCell' },
  { key: 'goodsAmount', title: '货品金额', width: 110, align: 'right', type: 'slot', slotName: 'moneyCell' },
  { key: 'deliveryFee', title: '配送费', width: 100, align: 'right', type: 'slot', slotName: 'moneyCell' },
  { key: 'bidEnabled', title: '竞价模式', width: 100, align: 'center', type: 'slot', slotName: 'bidEnabledCell' },
  { key: 'bidCurrentPrice', title: '当前价/中标价', width: 130, align: 'right', type: 'slot', slotName: 'moneyCell' },
  { key: 'bidCount', title: '竞价数', width: 90, align: 'right' },
  { key: 'poolStatus', title: '池状态', width: 100, align: 'center', type: 'slot', slotName: 'poolStatusCell' },
  { key: 'riderName', title: '接单配送员', width: 120 },
  { key: 'publishedTime', title: '发布时间', width: 150, formatter: (v: any) => formatDateTime(v) },
  { key: 'expireTime', title: '过期时间', width: 150, formatter: (v: any) => formatDateTime(v) },
  { key: 'remainSeconds', title: '剩余时间', width: 110, type: 'slot', slotName: 'remainCell' },
  { key: 'remark', title: '备注', width: 160 },
]

function colKey(column: any): string {
  return column?.key ?? column?.dataIndex ?? ''
}

const tableFooterColumns = computed(() => {
  const sum = (key: string) => tableData.value.reduce((s, r: any) => s + (Number(r[key]) || 0), 0)
  return [
    { key: 'goodsAmount', value: round2(sum('goodsAmount')), highlight: true },
    { key: 'deliveryFee', value: round2(sum('deliveryFee')), highlight: true },
    { key: 'bidCurrentPrice', value: round2(sum('bidCurrentPrice')), highlight: true },
  ]
})

// ═══ 配送员候选（复用 DMS 配送查询下拉口径）═══
const riderOptions = ref<{ label: string; value: number; name: string }[]>([])
const riderLoading = ref(false)
async function loadRiders() {
  riderLoading.value = true
  try {
    const res: any = await taskApi.filterOptions()
    const data = res?.data ?? res ?? {}
    riderOptions.value = (data.drivers || []).map((r: any) => ({ label: r.name, value: r.id, name: r.name }))
  } catch (e) {
    console.warn('[订单池] 配送员下拉加载失败', e)
  } finally {
    riderLoading.value = false
  }
}

// ═══ 配送线路候选（《线路开发文档》主数据，仅启用线路）═══
const routeOptions = ref<{ label: string; value: number }[]>([])
const routeLoading = ref(false)
const routeNameMap = computed<Record<number, string>>(() =>
  Object.fromEntries(routeOptions.value.map(o => [o.value, o.label])))
async function loadRoutes() {
  routeLoading.value = true
  try {
    const res: any = await mdRouteApi.options()
    const list: any[] = res?.data ?? res ?? []
    routeOptions.value = list.map((r: any) => ({
      label: r.routeName ? `${r.routeCode || ''} ${r.routeName}`.trim() : String(r.id),
      value: r.id,
    }))
  } catch (e) {
    console.warn('[订单池] 配送线路下拉加载失败', e)
  } finally {
    routeLoading.value = false
  }
}

// ═══ 数据加载 ═══
function buildParams(extra: Record<string, any> = {}): any {
  const params: any = { ...extra }
  for (const k of ['taskNo', 'orderNo', 'customerName'] as const) {
    const v = (searchParams as any)[k]
    if (typeof v === 'string' && v.trim()) params[k] = v.trim()
  }
  if (searchParams.orderType !== undefined) params.orderType = searchParams.orderType
  if (searchParams.poolStatus !== undefined) params.poolStatus = searchParams.poolStatus
  if (searchParams.bidEnabled !== undefined) params.bidEnabled = searchParams.bidEnabled
  if (searchParams.routeId) params.routeId = searchParams.routeId
  if (searchParams.riderId) params.riderId = searchParams.riderId
  if (searchParams.feeMin !== undefined && searchParams.feeMin !== null) params.feeMin = searchParams.feeMin
  if (searchParams.feeMax !== undefined && searchParams.feeMax !== null) params.feeMax = searchParams.feeMax
  if (searchParams.publishTimeStart) params.publishTimeStart = searchParams.publishTimeStart
  if (searchParams.publishTimeEnd) params.publishTimeEnd = searchParams.publishTimeEnd
  return params
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await orderPoolApi.page({
      ...buildParams(),
      current: pagination.current,
      size: pagination.pageSize,
    })
    const body = res?.data ?? res ?? {}
    tableData.value = body.records || []
    pagination.total = Number(body.total) || 0
    tableRef.value?.clearSelection?.()
    selectedRowKeys.value = []
    selectedRows.value = []
  } catch (e: any) {
    console.warn('[订单池] 列表加载失败', e)
    message.error(e?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

function handlePublishRangeChange(dates: any) {
  if (dates && dates.length === 2) {
    searchParams.publishTimeStart = dates[0].format('YYYY-MM-DD')
    searchParams.publishTimeEnd = dates[1].format('YYYY-MM-DD')
  } else {
    searchParams.publishTimeStart = ''
    searchParams.publishTimeEnd = ''
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
function goBidDetail(record: DmsOrderPool) {
  router.push(`/dms/order-pool/bid-detail?poolId=${record.id}&taskId=${record.taskId}&taskNo=${record.taskNo || ''}`)
}

// ═══ 发布到池 ═══
const publishModal = reactive({
  open: false,
  saving: false,
  taskIds: [] as number[],
  taskOptions: [] as { label: string; value: number }[],
  taskLoading: false,
  deliveryFee: undefined as number | undefined,
  enableBid: false,
  bidStartPrice: 0,
  durationMinutes: 60,
})
async function openPublishModal() {
  publishModal.open = true
  publishModal.taskIds = []
  publishModal.taskLoading = true
  try {
    // 候选只取「待分配(0)」：后端 publish 也只接受待分配任务（已分配/在途任务入池会覆盖他人骑手）
    const res: any = await taskApi.page({ current: 1, size: 200, statusList: [0] } as any)
    const body = res?.data ?? res ?? {}
    const list = body.records || []
    publishModal.taskOptions = list
      .filter((t: any) => t.id)
      .map((t: any) => ({
        label: `${t.taskNo}｜${t.customerName || ''}｜${t.orderNo || ''}`,
        value: t.id,
      }))
  } catch (e) {
    console.warn('[订单池] 待分配任务加载失败', e)
  } finally {
    publishModal.taskLoading = false
  }
}
async function handlePublishConfirm() {
  if (publishModal.taskIds.length === 0) {
    message.warning('请选择要发布到池的任务')
    return
  }
  if (publishModal.enableBid && (!publishModal.bidStartPrice || !publishModal.durationMinutes)) {
    message.warning('开启竞价需填写起拍价与竞价时长')
    return
  }
  publishModal.saving = true
  try {
    await orderPoolApi.publish({
      taskIds: publishModal.taskIds,
      deliveryFee: publishModal.deliveryFee,
      bidStartPrice: publishModal.enableBid ? publishModal.bidStartPrice : undefined,
      durationMinutes: publishModal.enableBid ? publishModal.durationMinutes : undefined,
    })
    message.success(`已发布 ${publishModal.taskIds.length} 条到订单池`)
    publishModal.open = false
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '发布失败')
  } finally {
    publishModal.saving = false
  }
}

// ═══ 开启 / 关闭竞价（同一入口按选中行状态切换）═══
const bidToggleTarget = computed(() => (selectedRows.value.length === 1 ? selectedRows.value[0] : null))
const bidToggleLabel = computed(() => (bidToggleTarget.value?.poolStatus === 1 ? '关闭竞价' : '开启竞价'))
const canToggleBid = computed(() =>
  !!bidToggleTarget.value && [0, 1].includes(bidToggleTarget.value.poolStatus))
function handleBidToggle() {
  const target = bidToggleTarget.value
  if (!target) {
    message.warning('请选择 1 条待抢单/竞价中的池记录')
    return
  }
  if (target.poolStatus === 1) {
    handleDisableBid(target)
  } else {
    openEnableBidModal(target)
  }
}

function handleDisableBid(record: DmsOrderPool) {
  Modal.confirm({
    title: '关闭竞价',
    content: `确定撤回任务 ${record.taskNo} 的竞价？本次 ${record.bidCount || 0} 条报价将全部作废，池回到「待抢单」。`,
    okText: '确认关闭',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res: any = await orderPoolApi.disableBid(record.id)
        const n = res?.data ?? res ?? 0
        message.success(`已关闭竞价，作废报价 ${n} 条`)
        fetchData()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '关闭竞价失败')
      }
    },
  })
}

// ═══ 开启竞价 ═══
const enableBidModal = reactive({
  open: false, saving: false, record: null as DmsOrderPool | null,
  startPrice: 0, durationMinutes: 60,
})
function openEnableBidModal(record?: DmsOrderPool) {
  const target = record || selectedRows.value[0]
  if (!target) {
    message.warning('请选择 1 条待抢单的池记录')
    return
  }
  enableBidModal.record = target
  enableBidModal.startPrice = Number(target.deliveryFee) || 0
  enableBidModal.durationMinutes = 60
  enableBidModal.open = true
}
async function handleEnableBidConfirm() {
  if (!enableBidModal.record) return
  if (!enableBidModal.startPrice || enableBidModal.startPrice <= 0) {
    message.warning('请填写起拍价')
    return
  }
  enableBidModal.saving = true
  try {
    await orderPoolApi.enableBid(enableBidModal.record.id, enableBidModal.startPrice, enableBidModal.durationMinutes)
    message.success('已开启竞价')
    enableBidModal.open = false
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '开启竞价失败')
  } finally {
    enableBidModal.saving = false
  }
}

// ═══ 强制分配 ═══
const assignModal = reactive({
  open: false, saving: false, record: null as DmsOrderPool | null,
  riderId: undefined as number | undefined,
})
function openAssignModal(record: DmsOrderPool) {
  assignModal.record = record
  assignModal.riderId = undefined
  assignModal.open = true
}
async function handleAssignConfirm() {
  if (!assignModal.record) return
  if (!assignModal.riderId) {
    message.warning('请选择配送员')
    return
  }
  assignModal.saving = true
  try {
    const riderName = riderOptions.value.find(r => r.value === assignModal.riderId)?.name
    await orderPoolApi.forceAssign(assignModal.record.id, { riderId: assignModal.riderId, riderName })
    message.success('已强制分配')
    assignModal.open = false
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '强制分配失败')
  } finally {
    assignModal.saving = false
  }
}

// ═══ 代客出价 ═══
const bidModal = reactive({
  open: false, saving: false, record: null as DmsOrderPool | null,
  riderId: undefined as number | undefined, price: 0,
})
function openBidModal(record: DmsOrderPool) {
  bidModal.record = record
  bidModal.riderId = undefined
  bidModal.price = Number(record.bidCurrentPrice) || Number(record.deliveryFee) || 0
  bidModal.open = true
}
async function handleBidConfirm() {
  if (!bidModal.record) return
  if (!bidModal.riderId) {
    message.warning('请选择配送员')
    return
  }
  if (!bidModal.price || bidModal.price <= 0) {
    message.warning('请填写报价')
    return
  }
  bidModal.saving = true
  try {
    const riderName = riderOptions.value.find(r => r.value === bidModal.riderId)?.name
    await orderPoolApi.bid(bidModal.record.id, { riderId: bidModal.riderId, riderName, price: bidModal.price })
    message.success('已出价')
    bidModal.open = false
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '出价失败')
  } finally {
    bidModal.saving = false
  }
}

// ═══ 结算 / 下架 / 过期 ═══
function handleSettle(record: DmsOrderPool) {
  Modal.confirm({
    title: '结算中标',
    content: `按「价低优先」结算任务 ${record.taskNo} 的竞价？中标配送员将同步指派到配送任务。`,
    okText: '确认结算',
    cancelText: '取消',
    onOk: async () => {
      try {
        await orderPoolApi.settle(record.id)
        message.success('结算完成')
        fetchData()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '结算失败')
      }
    },
  })
}

function handleOffline(record: DmsOrderPool) {
  Modal.confirm({
    title: '下架订单池条目',
    content: `确定下架任务 ${record.taskNo}？下架后不可再抢单/竞价。`,
    okText: '确认下架',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await orderPoolApi.offline(record.id, '手工下架')
        message.success('已下架')
        fetchData()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '下架失败')
      }
    },
  })
}

function handleBatchOffline() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要下架的池记录')
    return
  }
  Modal.confirm({
    title: '批量下架',
    content: `确定下架选中的 ${selectedRowKeys.value.length} 条池记录？`,
    okText: '确认下架',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      let ok = 0
      let fail = 0
      for (const id of [...selectedRowKeys.value]) {
        try {
          await orderPoolApi.offline(id, '批量下架')
          ok++
        } catch {
          fail++
        }
      }
      if (fail === 0) {
        message.success(`已下架 ${ok} 条`)
      } else {
        message.warning(`下架成功 ${ok} 条，失败 ${fail} 条（已接单/已过期的不可下架）`)
      }
      fetchData()
    },
  })
}

async function handleExpireScan() {
  try {
    const res: any = await orderPoolApi.expireScan()
    const n = res?.data ?? res ?? 0
    message.success(`过期处理完成，本次过期 ${n} 条（竞价截止未中标）`)
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '过期处理失败')
  }
}

// ═══ 导出 ═══
async function handleExport() {
  try {
    const blob: any = await orderPoolApi.exportExcel(buildParams())
    if (!blob || blob.size === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `订单池_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '导出失败')
  }
}

// ═══ 工具 ═══
function formatMoney(val: any): string {
  if (val === undefined || val === null || val === '' || isNaN(Number(val))) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatQty(val: any): string {
  if (val === undefined || val === null || val === '') return '0'
  return String(Number(val))
}
function round2(v: number): number {
  return Math.round((Number(v) || 0) * 100) / 100
}
function formatDateTime(val: any): string {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm')
}
function formatRemain(record: DmsOrderPool): string {
  const s = record?.remainSeconds
  if (s === undefined || s === null) return '-'
  if (s <= 0) return '已到时'
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (h > 0) return `${h}小时${m}分`
  return `${m}分`
}
function handleError(err: any) {
  console.warn('[订单池] ErrorBoundary:', err)
}

onMounted(() => {
  loadPageConfig()
  loadRiders()
  loadRoutes()
  fetchData()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select),
.search-field-item :deep(.ant-picker),
.search-field-item :deep(.ant-input-number) { width: 100%; font-size: 13px; }
.fee-range { gap: 4px; align-items: center; }
.fee-sep { color: #bfbfbf; }
.search-action-group { display: flex; flex-wrap: nowrap; align-items: center; }
.search-action-group .search-field-item { width: auto; flex: 0 0 auto; margin-right: 8px; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
.remain-warn { color: #fa8c16; font-weight: 600; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
