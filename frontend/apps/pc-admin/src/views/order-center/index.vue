<template>
  <PageContainer full-height>
    <template #header>
      <div class="order-page-header">
        <div class="order-page-header-left">
          <a-breadcrumb class="order-page-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>订单中心</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="order-page-header-title">订单中心</h2>
        </div>
        <div class="order-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="() => fetchData()">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>
    <ModuleLayout
    :breadcrumb-items="breadcrumbItems"
    :selected-count="selectedRowKeys.length"
    :current-page="pagination.current"
    :page-size="pagination.pageSize"
    :total-items="filteredTotal"
    :loading="loading"
    :error="error"
    :empty="!loading && !error && filteredTotal === 0"
    :empty-text="emptyContextText"
    search-placeholder="搜索订单号 / 客户 / 供应商..."
    :search-value="searchKeyword"
    :show-pagination="false"
    @search-input="handleSearchInput"
    @search-submit="handleSearchSubmit"
    @clear-selection="handleClearSelection"
    @retry="handleRetry"
  >
    <template #breadcrumb>
      <div class="order-breadcrumb">
        <span class="order-breadcrumb-subtitle">统一管理采购与销售订单</span>
      </div>
    </template>

    <!-- ── 顶栏操作区 ─────────────────────────── -->
    <template #actions>
      <a-space wrap class="action-bar">
        <!-- 快速日期筛选（带计数） -->
        <a-radio-group
          v-model:value="quickDateFilter"
          size="small"
          button-style="outline"
          @change="handleQuickFilterChange"
        >
          <a-radio-button value="today">
            今天
            <small v-if="dateCountMap.today > 0" class="filter-count">{{ dateCountMap.today }}</small>
          </a-radio-button>
          <a-radio-button value="week">
            本周
            <small v-if="dateCountMap.week > 0" class="filter-count">{{ dateCountMap.week }}</small>
          </a-radio-button>
          <a-radio-button value="month">
            本月
            <small v-if="dateCountMap.month > 0" class="filter-count">{{ dateCountMap.month }}</small>
          </a-radio-button>
          <a-radio-button value="all">
            全部
            <small v-if="typeAndStatusFiltered.length > 0" class="filter-count">{{ typeAndStatusFiltered.length }}</small>
          </a-radio-button>
        </a-radio-group>

        <a-divider type="vertical" />

        <!-- 类型筛选 -->
        <a-select
          v-model:value="filterTab"
          placeholder="订单类型"
          allow-clear
          style="width: 120px"
          @change="handleFilterTabChange"
          @clear="handleFilterTabClear"
        >
          <a-select-option value="all">全部</a-select-option>
          <a-select-option value="purchase">采购订单</a-select-option>
          <a-select-option value="sales">销售订单</a-select-option>
        </a-select>

        <!-- 状态筛选 -->
        <a-select
          v-model:value="filterStatus"
          placeholder="订单状态"
          allow-clear
          style="width: 120px"
          @change="handleFilterStatusChange"
          @clear="handleFilterStatusChange"
        >
          <a-select-option :value="0">草稿</a-select-option>
          <a-select-option :value="1">待审批</a-select-option>
          <a-select-option :value="2">已审批</a-select-option>
          <a-select-option :value="3">已拒绝</a-select-option>
          <a-select-option :value="4">执行中</a-select-option>
          <a-select-option :value="5">已完成</a-select-option>
          <a-select-option :value="6">已取消</a-select-option>
        </a-select>

        <a-divider type="vertical" />

        <!-- 列自定义 -->
        <a-dropdown trigger="click">
          <a-button size="small" class="column-config-btn">
            <template #icon><SettingOutlined /></template>
            列
          </a-button>
          <template #overlay>
            <a-menu class="column-menu" @click="handleColumnMenuClick">
              <template v-for="col in columnDefs" :key="col.key">
                <a-menu-item
                  v-if="col.key !== 'action'"
                  :disabled="col.key === 'orderNo'"
                  class="column-menu-item"
                  :key="col.key"
                >
                  <a-checkbox
                    :checked="visibleColumnKeys.includes(col.key)"
                    @change="(e: any) => handleColumnCheckChange(col.key, e)"
                  >
                    {{ col.title }}
                  </a-checkbox>
                </a-menu-item>
              </template>
            </a-menu>
          </template>
        </a-dropdown>

        <span class="update-time-text">
          数据更新：{{ lastUpdateTime || '--' }}
        </span>
      </a-space>
    </template>

    <!-- ── 批量操作条 ─────────────────────────── -->
    <template #batch-actions>
      <a-space>
        <span class="selected-count-label">
          已选 <strong>{{ selectedRowKeys.length }}</strong> 项
        </span>
        <span v-if="selectedTotalAmount > 0" class="selected-total-label">
          金额合计 <strong class="selected-total-amount">¥{{ selectedTotalAmount.toFixed(2) }}</strong>
        </span>
        <a-divider type="vertical" />
        <a-button
          size="small"
          type="primary"
          danger
          :loading="batchDeleting"
          @click="handleBatchDelete"
        >
          <template #icon><DeleteOutlined /></template>
          批量删除
        </a-button>
        <a-button
          size="small"
          :loading="batchExporting"
          @click="handleBatchExport"
        >
          <template #icon><DownloadOutlined /></template>
          批量导出
        </a-button>
      </a-space>
    </template>

    <!-- ── 空数据引导 ─────────────────────────── -->
    <template #empty-actions>
      <a-space direction="vertical" align="center" size="middle">
        <span style="color: #999; font-size: 14px;">
          {{ isEmptyDueToFilter ? '没有匹配的订单，请调整筛选条件' : '还没有订单，创建第一笔订单开始使用吧' }}
        </span>
        <a-button v-if="!isEmptyDueToFilter" type="primary" size="large" @click="handleCreateOrder">
          <template #icon><PlusOutlined /></template>
          新建订单
        </a-button>
        <a-button v-else size="large" @click="clearAllFilters">
          <template #icon><ReloadOutlined /></template>
          清除筛选
        </a-button>
      </a-space>
    </template>

    <!-- ── 列表内容 ───────────────────────────── -->
    <template #list-view>
      <!-- 统计卡片（加载中显示骨架，避免旧数据闪烁） -->
      <a-row :gutter="[16, 16]" class="stat-row">
        <template v-if="loading && dataSource.length > 0">
          <a-col :xs="12" :sm="12" :md="6" v-for="n in 4" :key="n">
            <a-card size="small" class="stat-card">
              <div class="stat-skeleton-inner">
                <div class="stat-skeleton-title" />
                <div class="stat-skeleton-value" />
              </div>
            </a-card>
          </a-col>
        </template>
        <template v-else>
          <a-col :xs="12" :sm="12" :md="6">
            <div class="stat-card stat-card--primary">
              <div class="stat-card-icon">
                <FileTextOutlined />
              </div>
              <div class="stat-card-content">
                <div class="stat-card-title">订单总数</div>
                <div class="stat-card-value">{{ filteredTotal }}</div>
              </div>
            </div>
          </a-col>
          <a-col :xs="12" :sm="12" :md="6">
            <div class="stat-card stat-card--blue">
              <div class="stat-card-icon">
                <ShoppingCartOutlined />
              </div>
              <div class="stat-card-content">
                <div class="stat-card-title">采购订单</div>
                <div class="stat-card-value">{{ purchaseCount }}</div>
              </div>
            </div>
          </a-col>
          <a-col :xs="12" :sm="12" :md="6">
            <div class="stat-card stat-card--green">
              <div class="stat-card-icon">
                <RocketOutlined />
              </div>
              <div class="stat-card-content">
                <div class="stat-card-title">销售订单</div>
                <div class="stat-card-value">{{ salesCount }}</div>
              </div>
            </div>
          </a-col>
          <a-col :xs="12" :sm="12" :md="6">
            <div class="stat-card stat-card--orange">
              <div class="stat-card-icon">
                <ClockCircleOutlined />
              </div>
              <div class="stat-card-content">
                <div class="stat-card-title">待审批</div>
                <div class="stat-card-value">{{ pendingCount }}</div>
              </div>
            </div>
          </a-col>
        </template>
      </a-row>

      <!-- 骨架屏（首次加载） -->
      <div v-if="loading && dataSource.length === 0" class="skeleton-container">
        <div class="skeleton-table">
          <div v-for="n in 6" :key="n" class="skeleton-table-row">
            <div class="skeleton-cell" style="width: 18%">&nbsp;</div>
            <div class="skeleton-cell" style="width: 8%">&nbsp;</div>
            <div class="skeleton-cell" style="width: 16%">&nbsp;</div>
            <div class="skeleton-cell" style="width: 12%">&nbsp;</div>
            <div class="skeleton-cell" style="width: 10%">&nbsp;</div>
            <div class="skeleton-cell" style="width: 16%">&nbsp;</div>
            <div class="skeleton-cell" style="width: 14%">&nbsp;</div>
          </div>
        </div>
      </div>

      <!-- 数据表格 -->
      <div
        v-show="!loading || dataSource.length > 0"
        ref="tableContainerRef"
        class="table-container"
      >
        <VxeTableList
          :columns="displayColumns"
          :data-source="displayData"
          row-key="_rowKey"
          :pagination="false"
          :loading="loading"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        >
          <template #orderTypeCell="{ record }">
            <a-tag :color="record.orderType === 'purchase' ? 'blue' : 'green'" class="type-tag">
              {{ record.orderType === 'purchase' ? '采购' : '销售' }}
            </a-tag>
          </template>

          <template #orderStatusCell="{ record }">
            <span class="status-badge">
              <span
                class="status-dot"
                :style="{ backgroundColor: STATUS_COLORS[record.orderStatus] || '#999' }"
              />
              <span>{{ getStatusText(record.orderStatus) }}</span>
            </span>
          </template>

          <template #totalAmountCell="{ record }">
            <span class="currency-value">
              ¥{{ (record.totalAmount || 0).toFixed(2) }}
            </span>
          </template>

          <template #partyNameCell="{ record }">
            <span class="party-name" :title="record.customerName || record.supplierName || '-'">
              {{ record.customerName || record.supplierName || '-' }}
            </span>
          </template>

          <template #action="{ record }">
            <a-space :size="0" class="action-cell">
              <a-tooltip title="查看详情">
                <a-button type="link" size="small" class="action-btn" @click="handleView(record)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>

              <a-divider type="vertical" class="action-divider" />

              <a-tooltip title="复制订单号">
                <a-button type="link" size="small" class="action-btn" @click="handleCopyOrderNo(record)">
                  <template #icon><CopyOutlined /></template>
                </a-button>
              </a-tooltip>

              <template v-if="record.orderStatus === 0">
                <a-divider type="vertical" class="action-divider" />
                <a-tooltip title="提交审批">
                  <a-button type="link" size="small" class="action-btn action-btn--submit" @click="handleQuickSubmit(record)">
                    提交
                  </a-button>
                </a-tooltip>
              </template>

              <template v-else-if="record.orderStatus === 1">
                <a-divider type="vertical" class="action-divider" />
                <a-tooltip title="审批通过">
                  <a-button type="link" size="small" class="action-btn action-btn--approve" @click="handleQuickApprove(record)">
                    审批
                  </a-button>
                </a-tooltip>
              </template>

              <template v-else-if="record.orderStatus === 2 || record.orderStatus === 4">
                <a-divider type="vertical" class="action-divider" />
                <a-tooltip title="取消订单">
                  <a-button type="link" size="small" class="action-btn action-btn--cancel" @click="handleQuickCancel(record)">
                    取消
                  </a-button>
                </a-tooltip>
              </template>
            </a-space>
          </template>
        </VxeTableList>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <a-pagination
            v-model:current="pagination.current"
            :page-size="pagination.pageSize"
            :total="filteredTotal"
            :page-size-options="['10', '20', '50', '100']"
            show-size-changer
            show-quick-jumper
            :show-total="(total: number) => `共 ${total} 条`"
            @change="handlePageChange"
          />
        </div>
      </div>
    </template>
  </ModuleLayout>

  <!-- 新建订单类型选择 -->
  <a-modal
    v-model:open="createTypeModalVisible"
    title="选择订单类型"
    :footer="null"
    :closable="true"
    width="400px"
    centered
  >
    <div class="create-order-picker">
      <a-card
        hoverable
        class="create-type-card"
        @click="navigateToCreate('purchase')"
      >
        <template #cover>
          <div class="create-type-icon create-type-icon--purchase">
            <ImportOutlined />
          </div>
        </template>
        <a-card-meta title="采购订单">
          <template #description>向供应商采购货物</template>
        </a-card-meta>
      </a-card>
      <a-card
        hoverable
        class="create-type-card"
        @click="navigateToCreate('sales')"
      >
        <template #cover>
          <div class="create-type-icon create-type-icon--sales">
            <ExportOutlined />
          </div>
        </template>
        <a-card-meta title="销售订单">
          <template #description>向客户销售货物</template>
        </a-card-meta>
      </a-card>
    </div>
  </a-modal>

  <!-- 回到顶部 -->
  <a-back-top :visibility-height="400" />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { PageContainer } from '@/components'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import {
  EyeOutlined,
  CopyOutlined,
  DeleteOutlined,
  DownloadOutlined,
  PlusOutlined,
  ReloadOutlined,
  SettingOutlined,
  ImportOutlined,
  ExportOutlined,
  FileTextOutlined,
  ShoppingCartOutlined,
  RocketOutlined,
  ClockCircleOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'
import { ModuleLayout } from '@ai-ready/components'
import { purchaseOrderApi } from '@/api/purchase'
import { salesOrderApi } from '@/api/order'
import { useUserStore } from '@/stores/user'

// ── 常量 ──────────────────────────────────────────────────

const STATUS_COLORS: Record<number, string> = {
  0: '#999999',
  1: '#fa8c16',
  2: '#1890ff',
  3: '#f5222d',
  4: '#13c2c2',
  5: '#52c41a',
  6: '#d9d9d9'
}

const STATUS_TEXTS: Record<number, string> = {
  0: '草稿',
  1: '待审批',
  2: '已审批',
  3: '已拒绝',
  4: '执行中',
  5: '已完成',
  6: '已取消'
}

const COLUMN_CONFIG_KEY = 'order-center-columns-v2'

// ── 类型 ──────────────────────────────────────────────────

interface UnifiedOrder {
  id: number
  _rowKey: string
  orderNo: string
  orderType: 'purchase' | 'sales'
  orderStatus: number
  customerName?: string
  supplierName?: string
  totalAmount: number
  createTime: string
}

// ── Store / Router ────────────────────────────────────────

const userStore = useUserStore()
const router = useRouter()

// ── 响应式状态 ─────────────────────────────────────────────

const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<UnifiedOrder[]>([])
const selectedRowKeys = ref<(string | number)[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const searchKeyword = ref('')
const batchDeleting = ref(false)
const batchExporting = ref(false)

const filterTab = ref<string | undefined>('all')
const filterStatus = ref<number | undefined>(undefined)
const quickDateFilter = ref<string>('all')

const pagination = reactive({
  current: 1,
  pageSize: 20
})

const sortState = reactive({
  field: '',
  order: '' as '' | 'ascend' | 'descend'
})

// 列配置从 localStorage 加载，实现持久化
const savedColumns = localStorage.getItem(COLUMN_CONFIG_KEY)
const visibleColumnKeys = ref<string[]>(
  savedColumns
    ? JSON.parse(savedColumns)
    : ['orderNo', 'orderType', 'partyName', 'totalAmount', 'orderStatus', 'createTime', 'action']
)

// 列配置变更时自动持久化
watch(visibleColumnKeys, (val) => {
  localStorage.setItem(COLUMN_CONFIG_KEY, JSON.stringify(val))
}, { deep: true })

const scrollY = ref(480)
const tableContainerRef = ref<HTMLElement | null>(null)
const createTypeModalVisible = ref(false)

  // ── 日期工具 ──────────────────────────────────────────────

  function getTodayRange(): [string, string] {
    const today = dayjs().format('YYYY-MM-DD')
    return [today, today]
  }

  function getWeekRange(): [string, string] {
    const start = dayjs().startOf('week').add(1, 'day').format('YYYY-MM-DD')
    const end = dayjs().format('YYYY-MM-DD')
    return [start, end]
  }

  function getMonthRange(): [string, string] {
    const start = dayjs().startOf('month').format('YYYY-MM-DD')
    const end = dayjs().format('YYYY-MM-DD')
    return [start, end]
  }

  function formatTimestamp(date: Date): string {
    return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
  }

  const requestDateRange = computed<[string, string] | null>(() => {
    switch (quickDateFilter.value) {
      case 'today': return getTodayRange()
      case 'week':  return getWeekRange()
      case 'month': return getMonthRange()
      default:      return null
    }
  })

  // 保留客户端日期检测函数（用于 dateCountMap 角标）
  function isToday(dateStr: string): boolean {
    const d = new Date(dateStr)
    const now = new Date()
    return d.getFullYear() === now.getFullYear()
      && d.getMonth() === now.getMonth()
      && d.getDate() === now.getDate()
  }

  function isThisWeek(dateStr: string): boolean {
    const d = new Date(dateStr)
    const now = new Date()
    const weekStart = new Date(now)
    weekStart.setDate(now.getDate() - ((now.getDay() + 6) % 7))
    weekStart.setHours(0, 0, 0, 0)
    return d >= weekStart && d <= now
  }

  function isThisMonth(dateStr: string): boolean {
    const d = new Date(dateStr)
    const now = new Date()
    return d.getFullYear() === now.getFullYear()
      && d.getMonth() === now.getMonth()
  }


// ── 列定义 ────────────────────────────────────────────────

interface ColumnDef {
  title: string
  field?: string
  key: string
  width?: number
  align?: 'left' | 'right' | 'center'
  ellipsis?: boolean
  fixed?: 'left' | 'right'
  slotName?: string
}

const columnDefs: ColumnDef[] = [
  { title: '订单号', field: 'orderNo', key: 'orderNo', width: 180, fixed: 'left', ellipsis: true },
  { title: '类型', field: 'orderType', key: 'orderType', width: 80, slotName: 'orderTypeCell' },
  { title: '往来单位', field: 'partyName', key: 'partyName', width: 160, ellipsis: true, slotName: 'partyNameCell' },
  { title: '金额', field: 'totalAmount', key: 'totalAmount', width: 130, align: 'right', slotName: 'totalAmountCell' },
  { title: '状态', field: 'orderStatus', key: 'orderStatus', width: 110, slotName: 'orderStatusCell' },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 170 },
  { type: 'action', title: '操作', width: 220, fixed: 'right' }
]

const displayColumns = computed(() =>
  columnDefs.filter(col => visibleColumnKeys.value.includes(col.key))
)

// ── 计算属性 ──────────────────────────────────────────────

const breadcrumbItems = computed(() => [{ text: '订单中心' }])

/**
 * ── 数据管道（重组后） ──
 *
 *  dataSource (全量 API 原始数据)
 *    → keywordFiltered (关键字搜索)
 *      → typeAndStatusFiltered (类型 + 状态，不含日期)
 *        → 用于 dateCountMap (日期角标联动当前筛选条件)
 *        → dateFiltered (日期筛选)
 *          → sortedOrders (排序)
 *            → displayData (分页)
 */

/** 关键字搜索 */
const keywordFiltered = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return dataSource.value
  return dataSource.value.filter(o =>
    o.orderNo.toLowerCase().includes(kw)
    || (o.customerName || '').toLowerCase().includes(kw)
    || (o.supplierName || '').toLowerCase().includes(kw)
  )
})

/** 类型 + 状态 筛选（不含日期） */
const typeAndStatusFiltered = computed(() => {
  let data = keywordFiltered.value
  if (filterTab.value && filterTab.value !== 'all') {
    data = data.filter(o => o.orderType === filterTab.value)
  }
  if (filterStatus.value !== undefined) {
    data = data.filter(o => o.orderStatus === filterStatus.value)
  }
  return data
})

/**
 * 日期筛选 badge 计数
 * 基于 typeAndStatusFiltered（已应用类型/状态/关键字），
 * 确保角标与当前筛选条件联动。
 */
const dateCountMap = computed(() => {
  const base = typeAndStatusFiltered.value
  return {
    today: base.filter(o => isToday(o.createTime)).length,
    week: base.filter(o => isThisWeek(o.createTime)).length,
    month: base.filter(o => isThisMonth(o.createTime)).length
  }
})

/** 日期筛选 */
const dateFiltered = computed(() => {
  if (quickDateFilter.value === 'all') return typeAndStatusFiltered.value
  return typeAndStatusFiltered.value.filter(o => {
    switch (quickDateFilter.value) {
      case 'today': return isToday(o.createTime)
      case 'week':  return isThisWeek(o.createTime)
      case 'month': return isThisMonth(o.createTime)
      default:      return true
    }
  })
})

/** 排序 */
const sortedOrders = computed(() => {
  const data = [...dateFiltered.value]
  if (sortState.field && sortState.order) {
    data.sort((a, b) => {
      const field = sortState.field as keyof UnifiedOrder
      const aVal = a[field]
      const bVal = b[field]
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sortState.order === 'ascend' ? aVal - bVal : bVal - aVal
      }
      if (typeof aVal === 'string' && typeof bVal === 'string') {
        return sortState.order === 'ascend'
          ? aVal.localeCompare(bVal)
          : bVal.localeCompare(aVal)
      }
      return 0
    })
  }
  return data
})

/** 分页数据 */
const displayData = computed(() => {
  const start = (pagination.current - 1) * pagination.pageSize
  return sortedOrders.value.slice(start, start + pagination.pageSize)
})

const filteredTotal = computed(() => sortedOrders.value.length)

/** 统计卡片（基于当前全量筛选后的数据） */
const purchaseCount = computed(() =>
  dateFiltered.value.filter(o => o.orderType === 'purchase').length
)
const salesCount = computed(() =>
  dateFiltered.value.filter(o => o.orderType === 'sales').length
)
const pendingCount = computed(() =>
  dateFiltered.value.filter(o => o.orderStatus === 1).length
)

/** 选中项金额合计 */
const selectedTotalAmount = computed(() => {
  return selectedRowKeys.value.reduce<number>((sum, key) => {
    const order = dataSource.value.find(o => o.id === key)
    return sum + (order?.totalAmount || 0)
  }, 0)
})

/** 判定：空数据是筛选导致还是真无数据 */
const isEmptyDueToFilter = computed(() => {
  if (dataSource.value.length === 0) return false
  return filteredTotal.value === 0
})

/** 空状态文本 */
const emptyContextText = computed(() =>
  isEmptyDueToFilter.value
    ? '当前筛选条件下无匹配订单'
    : '暂无订单数据'
)

// ── 行选择（VxeTableList 暂不支持自定义行选择，保留状态供批量操作使用）

// ── 自适应滚动高度 ──────────────────────────────────────

let resizeObserver: ResizeObserver | null = null

function updateScrollY() {
  nextTick(() => {
    if (tableContainerRef.value) {
      const rect = tableContainerRef.value.getBoundingClientRect()
      scrollY.value = Math.max(300, window.innerHeight - rect.top - 200)
    }
  })
}

// ── 状态工具 ──────────────────────────────────────────────

function getStatusText(status: number): string {
  return STATUS_TEXTS[status] || '未知'
}

// ── 数据获取 ──────────────────────────────────────────────

async function fetchData(append = false) {
  loading.value = true
  error.value = null
  if (!append) selectedRowKeys.value = []
  try {
    // 构建请求参数 - 使用真实分页，不再全量拉取
    const params: any = {
      current: pagination.current,
      pageSize: pagination.pageSize,
      tenantId: userStore.tenantId,
      orderType: filterTab.value === 'all' ? undefined : filterTab.value,
      orderStatus: filterStatus.value,
      keyword: searchKeyword.value || undefined,
      startDate: requestDateRange.value?.[0],
      endDate: requestDateRange.value?.[1]
    }

    const allOrders: UnifiedOrder[] = []
    let totalCount = 0

    // 采购订单
    if (filterTab.value === 'all' || filterTab.value === 'purchase') {
      try {
        const res = await purchaseOrderApi.page({
          current: params.current,
          size: params.pageSize,
          tenantId: params.tenantId,
          status: params.orderStatus,
          keyword: params.keyword,
          startDate: params.startDate,
          endDate: params.endDate
        })
        const records = res.data?.records || (res as any).records || []
        records.forEach((o: any) => {
          allOrders.push({
            id: o.id,
            _rowKey: `purchase_${o.id}`,
            orderNo: o.orderNo,
            orderType: 'purchase' as const,
            orderStatus: o.status ?? o.orderStatus ?? 0,
            supplierName: o.supplierName,
            totalAmount: o.totalAmountWithTax ?? o.totalAmount ?? 0,
            createTime: o.createTime
          })
        })
        totalCount = res.data?.total || (res as any).total || 0
      } catch (e: any) {
        console.warn('[订单中心] 采购订单接口异常:', e?.message)
      }
    }

    // 销售订单
    if (filterTab.value === 'all' || filterTab.value === 'sales') {
      try {
        const res = await salesOrderApi.getPage({
          pageNum: params.current,
          pageSize: params.pageSize,
          tenantId: params.tenantId,
          status: params.orderStatus,
          keyword: params.keyword,
          startDate: params.startDate,
          endDate: params.endDate
        } as any)
        const records = res.data?.records || (res as any).records || []
        records.forEach((o: any) => {
          allOrders.push({
            id: o.id,
            _rowKey: `sales_${o.id}`,
            orderNo: o.orderNo,
            orderType: 'sales' as const,
            orderStatus: o.status ?? o.orderStatus ?? 0,
            customerName: o.customerName,
            totalAmount: o.finalAmount ?? o.totalAmount ?? 0,
            createTime: o.createTime
          })
        })
        totalCount += res.data?.total || (res as any).total || 0
      } catch (e: any) {
        console.warn('[订单中心] 销售订单接口异常:', e?.message)
      }
    }

    dataSource.value = allOrders
    pagination.current = params.current
    lastUpdateTime.value = formatTimestamp(new Date())
  } catch (err: any) {
    error.value = err?.message || '获取数据失败，请稍后重试'
    dataSource.value = []
  } finally {
    loading.value = false
  }
}

// ── 事件处理 ──────────────────────────────────────────────

function handleRetry() {
  fetchData()
}

function handleSearchSubmit(value: string) {
  searchKeyword.value = value || ''
  pagination.current = 1
}

/** 实时同步 ModuleLayout 搜索输入框的值，确保 clearAllFilters 能清空搜索框 */
function handleSearchInput(value: string) {
  searchKeyword.value = value || ''
}

function handleClearSelection() {
  selectedRowKeys.value = []
}

/**
 * 统一分页变更处理
 * AntDV 4.x Pagination @change 事件签名: (page, pageSize)
 * 当 pageSize 改变时自动重置到第 1 页
 */
function handlePageChange(page: number, pageSize?: number) {
  if (pageSize && pageSize !== pagination.pageSize) {
    pagination.pageSize = pageSize
    pagination.current = 1
  } else {
    pagination.current = page
  }
}

// handlePageSizeChange 已合并到 handlePageChange（AntDV 4.x 已废弃 showSizeChange）

function handleFilterTabChange() {
  pagination.current = 1
  selectedRowKeys.value = []
  fetchData()
}

/** 类型筛选 allow-clear 清空时，回退到 'all' 防止 fetchData 不命中任何分支 */
function handleFilterTabClear() {
  filterTab.value = 'all'
  pagination.current = 1
  selectedRowKeys.value = []
  fetchData()
}

function handleFilterStatusChange() {
  pagination.current = 1
  selectedRowKeys.value = []
}

function handleQuickFilterChange() {
  pagination.current = 1
  selectedRowKeys.value = []
}

function clearAllFilters() {
  quickDateFilter.value = 'all'
  filterTab.value = 'all'
  filterStatus.value = undefined
  searchKeyword.value = ''
  pagination.current = 1
  fetchData()
}

function handleTableChange(_pag: any, _filters: any, sorter: any) {
  if (sorter && sorter.field) {
    sortState.field = sorter.field
    sortState.order = sorter.order || ''
  } else {
    sortState.field = ''
    sortState.order = ''
  }
  pagination.current = 1
}

function handleView(record: any) {
  const path = record.orderType === 'purchase'
    ? `/purchase/order/${record.id}`
    : `/sale/order/${record.id}`
  router.push(path)
}

async function handleCopyOrderNo(record: any) {
  try {
    await navigator.clipboard.writeText(record.orderNo)
    message.success(`已复制订单号: ${record.orderNo}`)
  } catch (err) {
    console.warn('[订单中心] 复制订单号失败', err)
    const ta = document.createElement('textarea')
    ta.value = record.orderNo
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    message.success(`已复制订单号: ${record.orderNo}`)
  }
}

/** 新建订单：弹出类型选择 */
function handleCreateOrder() {
  createTypeModalVisible.value = true
}

function navigateToCreate(type: 'purchase' | 'sales') {
  createTypeModalVisible.value = false
  // 跳转到对应模块的订单标签页，用户可直接点击「新建订单」按钮
  if (type === 'purchase') {
    router.push('/purchase?tab=orders')
  } else {
    router.push('/sale?tab=orders')
  }
}

/** 快捷提交审批（草稿→待审批） */
function handleQuickSubmit(record: any) {
  Modal.confirm({
    title: '提交审批',
    content: `确定提交订单 ${record.orderNo} 进入审批流程？`,
    okText: '确认提交',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        if (record.orderType === 'purchase') {
          await purchaseOrderApi.submit(record.id)
        } else {
          await salesOrderApi.submit(record.id)
        }
        message.success(`订单 ${record.orderNo} 已提交审批`)
        fetchData()
      } catch (err) {
        console.warn('[订单中心] 提交审批失败', err)
        message.error('提交失败')
      }
    }
  })
}

/** 快捷审批通过（待审批→已审批） */
function handleQuickApprove(record: any) {
  Modal.confirm({
    title: '审批通过',
    content: `确定审批通过订单 ${record.orderNo}？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        if (record.orderType === 'purchase') {
          await purchaseOrderApi.approve(record.id)
        } else {
          await salesOrderApi.approve(record.id)
        }
        message.success(`订单 ${record.orderNo} 已审批通过`)
        fetchData()
      } catch (err) {
        console.warn('[订单中心] 审批失败', err)
        message.error('审批失败')
      }
    }
  })
}

/** 快捷取消 */
function handleQuickCancel(record: any) {
  Modal.confirm({
    title: '取消订单',
    content: `确定取消订单 ${record.orderNo}？`,
    okText: '确认取消',
    okType: 'danger',
    cancelText: '保留',
    centered: true,
    async onOk() {
      try {
        if (record.orderType === 'purchase') {
          await purchaseOrderApi.cancel(record.id, '运营取消')
        } else {
          await salesOrderApi.cancel(record.id)
        }
        message.success(`订单 ${record.orderNo} 已取消`)
        fetchData()
      } catch (err) {
        console.warn('[订单中心] 取消失败', err)
        message.error('取消失败')
      }
    }
  })
}

/** 批量删除（带进度反馈） */
async function handleBatchDelete() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要删除的订单')
    return
  }
  Modal.confirm({
    title: '确认批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 条订单？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      batchDeleting.value = true
      const allKeys = [...selectedRowKeys.value]
      const purchaseIds: number[] = []
      const salesIds: number[] = []

      allKeys.forEach(id => {
        const order = dataSource.value.find(o => o.id === id)
        if (order?.orderType === 'purchase') purchaseIds.push(Number(id))
        else if (order?.orderType === 'sales') salesIds.push(Number(id))
      })

      const total = allKeys.length
      let succeeded = 0
      let failed = 0
      const msgKey = 'batch-delete-progress'

      message.loading({ content: `正在删除 0/${total}...`, key: msgKey, duration: 0 })

      // 采购订单（逐个删除以跟踪进度）
      for (const id of purchaseIds) {
        try {
          await purchaseOrderApi.delete(id)
          succeeded++
        } catch (err) {
          console.warn('[订单中心] 批量删除采购订单失败', err)
          failed++
        }
        message.open({ content: `正在删除 ${succeeded + failed}/${total}...`, key: msgKey })
      }

      // 销售订单（批量删除）
      if (salesIds.length > 0) {
        try {
          await salesOrderApi.batchDelete(salesIds)
          succeeded += salesIds.length
        } catch (err) {
          console.warn('[订单中心] 批量删除销售订单失败', err)
          failed += salesIds.length
        }
        message.open({ content: `正在删除 ${succeeded + failed}/${total}...`, key: msgKey })
      }

      message.destroy(msgKey)
      if (failed > 0) {
        message.warning(`删除完成：成功 ${succeeded} 条，失败 ${failed} 条`)
      } else {
        message.success(`成功删除 ${succeeded} 条订单`)
      }

      batchDeleting.value = false
      selectedRowKeys.value = []
      fetchData()
    }
  })
}

/** 批量导出 */
async function handleBatchExport() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要导出的订单')
    return
  }
  Modal.confirm({
    title: '确认批量导出',
    content: `确定要导出选中的 ${selectedRowKeys.value.length} 条订单？`,
    okText: '确认导出',
    cancelText: '取消',
    centered: true,
    async onOk() {
      batchExporting.value = true
      const allKeys = [...selectedRowKeys.value]
      const purchaseIds = allKeys.filter(id =>
        dataSource.value.find(o => o.id === id)?.orderType === 'purchase'
      )
      const salesIds = allKeys.filter(id =>
        dataSource.value.find(o => o.id === id)?.orderType === 'sales'
      )

      message.loading({ content: '正在导出...', key: 'batch-export', duration: 0 })
      try {
        if (purchaseIds.length > 0) {
          await purchaseOrderApi.exportData({ ids: purchaseIds } as any)
        }
        if (salesIds.length > 0) {
          await salesOrderApi.export({ ids: salesIds } as any)
        }
        message.success({ content: '导出任务已提交', key: 'batch-export' })
      } catch (err) {
        console.warn('[订单中心] 批量导出失败', err)
        message.error({ content: '导出失败', key: 'batch-export' })
      } finally {
        batchExporting.value = false
        message.destroy('batch-export')
      }
    }
  })
}

/** 列自定义：菜单点击阻止冒泡 */
function handleColumnMenuClick(info: any) {
  info.domEvent.stopPropagation()
}

function handleColumnCheckChange(key: string, e: any) {
  if (key === 'orderNo') return
  if (e.target.checked) {
    if (!visibleColumnKeys.value.includes(key)) {
      visibleColumnKeys.value.push(key)
    }
  } else {
    if (visibleColumnKeys.value.filter(k => k !== 'action').length <= 2) return
    const idx = visibleColumnKeys.value.indexOf(key)
    if (idx >= 0) visibleColumnKeys.value.splice(idx, 1)
  }
}

// ── 自动刷新 ──────────────────────────────────────────────
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 生命周期 ──────────────────────────────────────────────

onMounted(() => {
  fetchData()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  nextTick(() => {
    updateScrollY()
    resizeObserver = new ResizeObserver(() => updateScrollY())
    if (tableContainerRef.value) {
      resizeObserver.observe(tableContainerRef.value)
    }
  })
})

onUnmounted(() => {
  if (resizeObserver) resizeObserver.disconnect()
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
/* ── 页面头 ─────────────────────────────── */
.order-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.order-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.order-page-breadcrumb {
  font-size: 13px;
}
.order-page-breadcrumb :deep(li) {
  font-size: 13px;
}
.order-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.order-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

/* ── 面包屑 ─────────────────────────────── */
.order-breadcrumb {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.order-breadcrumb-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.order-breadcrumb-subtitle {
  font-size: 12px;
  color: #999;
}

/* ── 顶栏操作区 ─────────────────────────── */
.action-bar {
  width: 100%;
}
.filter-count {
  display: inline-block;
  margin-left: 3px;
  font-size: 11px;
  color: #999;
  font-weight: 400;
}
.column-config-btn {
  font-size: 13px;
}
.column-menu-item {
  padding: 4px 12px !important;
  cursor: default !important;
}
.column-menu-item:hover {
  background-color: transparent !important;
}
.column-menu-item .ant-checkbox-wrapper {
  width: 100%;
}
/* 数据更新时间 */
.update-time-bar {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}
.refresh-btn {
  cursor: pointer;
  font-size: 14px;
  transition: color 0.2s;
}
.refresh-btn:hover {
  color: #1890ff;
}
.refresh-btn.spinning {
  animation: spin 1s linear infinite;
  pointer-events: none;
  color: #1890ff;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.update-time-text {
  user-select: none;
}

/* ── 批量操作 ───────────────────────────── */
.selected-count-label {
  font-size: 13px;
  color: #666;
}
.selected-count-label strong {
  color: #1890ff;
}
.selected-total-label {
  font-size: 12px;
  color: #999;
}
.selected-total-amount {
  font-family: 'SF Mono', 'Fira Code', 'Monaco', 'Menlo', 'Consolas', monospace;
  color: #f5222d;
  font-weight: 600;
}

/* ── 统计卡片 ───────────────────────────── */
.stat-row {
  margin-bottom: 16px;
}
.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  transition: all 0.3s ease;
  cursor: default;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.stat-card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  margin-right: 16px;
}
.stat-card-content {
  flex: 1;
}
.stat-card-title {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 4px;
}
.stat-card-value {
  font-family: 'SFMono-Regular', 'SF Mono', 'Fira Code', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 28px;
  font-weight: 600;
  color: #fff;
  line-height: 1.2;
}
/* 渐变背景主题 */
.stat-card--primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.stat-card--blue {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
}
.stat-card--green {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
}
.stat-card--orange {
  background: linear-gradient(135deg, #fa8c16 0%, #d46b08 100%);
}

/* ── 统计卡片骨架屏 ─────────────────────── */
.stat-skeleton-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 4px 0;
}
.stat-skeleton-title {
  width: 50%;
  height: 12px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: 4px;
}
.stat-skeleton-value {
  width: 35%;
  height: 22px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: 4px;
}

/* ── 骨架屏（表格）──────────────────────── */
.skeleton-container {
  padding: 0;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
.skeleton-table {
  padding: 0;
}
.skeleton-table-row {
  display: flex;
  align-items: center;
  padding: 14px 12px;
  border-bottom: 1px solid #f0f0f0;
  gap: 12px;
}
.skeleton-table-row:last-child {
  border-bottom: none;
}
.skeleton-cell {
  height: 14px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: 4px;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── 表格容器 ───────────────────────────── */
.table-container {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

/* ── 订单表格 ───────────────────────────── */

/* 行悬浮高亮 + 阴影 */
.order-data-row {
  transition: box-shadow 0.2s, background-color 0.15s;
}
.order-data-row:hover td {
  background-color: #fafcff !important;
}
.order-data-row:hover {
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  position: relative;
  z-index: 1;
}

/* ── 类型标签 ───────────────────────────── */
.type-tag {
  font-size: 12px;
  border-radius: 4px;
}

/* ── 状态徽章（圆点 + 文字）─────────────── */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* ── 金额 ───────────────────────────────── */
.currency-value {
  font-family: 'SF Mono', 'Fira Code', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-variant-numeric: tabular-nums;
  font-size: 13px;
  font-weight: 500;
  text-align: right;
  display: block;
}

/* ── 往来单位 ───────────────────────────── */
.party-name {
  color: #555;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

/* ── 操作列 ─────────────────────────────── */
.action-cell {
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
}
.action-btn {
  font-size: 13px;
  padding: 0 6px;
  height: 24px;
}
.action-btn--submit {
  color: #52c41a;
}
.action-btn--submit:hover {
  color: #73d13d !important;
}
.action-btn--approve {
  color: #1890ff;
}
.action-btn--approve:hover {
  color: #40a9ff !important;
}
.action-btn--cancel {
  color: #fa8c16;
}
.action-btn--cancel:hover {
  color: #ffa940 !important;
}
.action-divider {
  height: 14px;
  border-color: #e0e0e0;
}

/* ── 分页 ───────────────────────────────── */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

/* ── 新建订单类型选择 ───────────────────── */
.create-order-picker {
  display: flex;
  gap: 16px;
  justify-content: center;
  padding: 8px 0;
}
.create-type-card {
  width: 160px;
  text-align: center;
  border-radius: 8px;
  cursor: pointer;
}
.create-type-card:hover {
  border-color: #1890ff;
}
.create-type-card :deep(.ant-card-cover) {
  padding: 24px 0 0;
}
.create-type-icon {
  font-size: 36px;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 64px;
}
.create-type-icon--purchase {
  color: #1890ff;
}
.create-type-icon--sales {
  color: #52c41a;
}
</style>
