<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="erp-dashboard-header">
        <div class="erp-dashboard-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>ERP仪表盘</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="erp-dashboard-title">ERP仪表盘</h2>
        </div>
        <div class="erp-dashboard-header-right">
          <span class="data-status">
            <a-badge :status="loading ? 'processing' : 'success'" />
            <span v-if="lastUpdateTime" class="update-time">数据更新: {{ lastUpdateTime }}</span>
          </span>
          <span class="auto-refresh-badge" v-if="autoRefreshCountdown > 0">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="loading" @click="debounceClick('refresh', loadData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </div>
    </template>

    <!-- KPI 统计卡片 -->
    <a-row :gutter="12" style="margin-bottom: 16px;">
      <a-col :span="4" v-for="kpi in kpiCards" :key="kpi.key">
        <div class="kpi-card" :style="{ borderTop: `3px solid ${kpi.color}` }">
          <div class="kpi-value" :style="{ color: kpi.color }">{{ loading ? '-' : kpi.value }}</div>
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-link" @click="navigateTo(kpi.path)">查看详情 →</div>
        </div>
      </a-col>
    </a-row>

    <a-row :gutter="12" style="flex: 1;">
      <!-- 快捷入口 -->
      <a-col :span="6">
        <a-card title="快捷入口" :bordered="false" class="section-card">
          <a-row :gutter="[8, 8]">
            <a-col :span="12" v-for="entry in quickEntries" :key="entry.key">
              <a-button class="quick-entry-btn" @click="navigateTo(entry.path)">
                <component :is="entry.icon" /> {{ entry.label }}
              </a-button>
            </a-col>
          </a-row>
        </a-card>

        <!-- 操作指引 -->
        <a-card title="常用操作" :bordered="false" class="section-card" style="margin-top: 12px;">
          <a-timeline>
            <a-timeline-item v-for="tip in quickTips" :key="tip.text" :color="tip.color">
              {{ tip.text }}
            </a-timeline-item>
          </a-timeline>
        </a-card>
      </a-col>

      <!-- 待处理事项 -->
      <a-col :span="18">
        <a-card title="待处理事项" :bordered="false" class="section-card">
          <a-table
            :data-source="pendingItems"
            :columns="pendingColumns"
            :loading="loading"
            :pagination="{ pageSize: 10, size: 'small', showTotal: (t) => `共 ${t} 条` }"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'type'">
                <a-tag :color="record.typeColor">{{ record.typeLabel }}</a-tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <StatusTag :status="record.status" :map="record.statusMap" />
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" @click="navigateTo(record.path)">查看</a-button>
              </template>
            </template>
            <template #emptyText>
              <a-empty description="暂无待处理事项" />
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { ColumnsType } from 'ant-design-vue/es/table'
import {
  ReloadOutlined,
  SyncOutlined,
  AppstoreOutlined,
  ShoppingOutlined,
  SwapOutlined,
  InboxOutlined,
  SendOutlined,
  CheckSquareOutlined,
  RollbackOutlined,
  TeamOutlined,
  ContainerOutlined,
  NumberOutlined,
  BarcodeOutlined,
  BarChartOutlined,
  LineChartOutlined,
  DollarOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import request from '@/utils/request'
import { INBOUND_STATUS, SHIPMENT_STATUS, STOCKTAKE_STATUS_ORDER, RETURN_STATUS } from '@/utils/statusConfig'

const router = useRouter()
const loading = ref(false)
const lastUpdateTime = ref('')

interface KpiItem {
  key: string
  label: string
  value: number
  color: string
  path: string
}

const kpiCards = ref<KpiItem[]>([
  { key: 'products', label: '产品总数', value: 0, color: '#1890ff', path: '/erp/product' },
  { key: 'partners', label: '往来单位', value: 0, color: '#722ed1', path: '/erp/partner' },
  { key: 'pendingInbound', label: '待入库', value: 0, color: '#faad14', path: '/erp/stock-in' },
  { key: 'pendingShipment', label: '待发货', value: 0, color: '#fa8c16', path: '/erp/shipment' },
  { key: 'pendingStocktake', label: '待盘点', value: 0, color: '#13c2c2', path: '/erp/stocktake' },
  { key: 'pendingReturn', label: '待退货', value: 0, color: '#f5222d', path: '/erp/return' },
])

const quickEntries = [
  { key: 'product', label: '产品管理', icon: AppstoreOutlined, path: '/erp/product' },
  { key: 'partner', label: '往来单位', icon: TeamOutlined, path: '/erp/partner' },
  { key: 'sale', label: '销售管理', icon: ShoppingOutlined, path: '/erp/sale' },
  { key: 'stock', label: '库存管理', icon: ContainerOutlined, path: '/erp/stock' },
  { key: 'stock-in', label: '入库管理', icon: InboxOutlined, path: '/erp/stock-in' },
  { key: 'shipment', label: '发货管理', icon: SendOutlined, path: '/erp/shipment' },
  { key: 'stocktake', label: '库存盘点', icon: CheckSquareOutlined, path: '/erp/stocktake' },
  { key: 'return', label: '退货管理', icon: RollbackOutlined, path: '/erp/return' },
  { key: 'purchase-exchange', label: '采购换货', icon: SwapOutlined, path: '/erp/purchase-exchange' },
  { key: 'batch', label: '批次管理', icon: BarcodeOutlined, path: '/erp/batch' },
  { key: 'serial', label: '序列号管理', icon: NumberOutlined, path: '/erp/serial' },
  { key: 'sales-analysis', label: '销售分析', icon: BarChartOutlined, path: '/erp/sales-analysis' },
]

const quickTips = [
  { text: '新建产品 → 产品管理右上角「新增产品」', color: 'blue' },
  { text: '新建入库单 → 入库管理右上角「新建入库单」', color: 'green' },
  { text: '审核单据 → 在列表中点击「审核」按钮', color: 'orange' },
  { text: 'Ctrl+N 快捷键快速新建单据', color: 'purple' },
  { text: 'F5 刷新当前列表数据', color: 'cyan' },
]

const pendingColumns: ColumnsType<any> = [
  { title: '类型', key: 'type', dataIndex: 'type', width: 100 },
  { title: '单号', dataIndex: 'code', key: 'code', width: 160 },
  { title: '摘要', dataIndex: 'summary', key: 'summary', width: 150 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '日期', dataIndex: 'date', key: 'date', width: 120 },
  { title: '操作', key: 'action', dataIndex: 'action', width: 80, fixed: 'right' },
]

interface PendingItem {
  id: number | string
  type: string
  typeLabel: string
  typeColor: string
  code: string
  summary: string
  status: number
  statusMap: Record<number, { text: string; color: string }>
  date: string
  path: string
}

const pendingItems = ref<PendingItem[]>([])

function navigateTo(path: string) {
  router.push(path)
}

async function loadData() {
  loading.value = true
  try {
    // 并行加载各模块数据
    const [
      productRes,
      partnerRes,
      inboundRes,
      shipmentRes,
      stocktakeRes,
      returnRes
    ] = await Promise.allSettled([
      request.get('/erp/product/list', { pageSize: 1 }),
      request.get('/erp/partner/list', { pageSize: 1 }),
      request.get('/erp/purchase/inbound/page', { pageSize: 999, status: 0 }),
      request.get('/erp/sale/outbound/page', { pageSize: 999, status: 0 }),
      request.get('/erp/stock/check/page', { pageSize: 999, status: 0 }),
      request.get('/erp/sale/return/page', { pageSize: 999, status: 0 }),
    ])

    // 更新 KPI
    if (productRes.status === 'fulfilled') {
      kpiCards.value[0].value = productRes.value.data?.total || productRes.value.data?.records?.length || 0
    }
    if (partnerRes.status === 'fulfilled') {
      kpiCards.value[1].value = partnerRes.value.data?.total || partnerRes.value.data?.records?.length || 0
    }
    if (inboundRes.status === 'fulfilled') {
      const data = inboundRes.value.data
      kpiCards.value[2].value = data?.total || data?.records?.length || 0
    }
    if (shipmentRes.status === 'fulfilled') {
      const data = shipmentRes.value.data
      kpiCards.value[3].value = data?.total || data?.records?.length || 0
    }
    if (stocktakeRes.status === 'fulfilled') {
      const data = stocktakeRes.value.data
      kpiCards.value[4].value = data?.total || data?.records?.length || 0
    }
    if (returnRes.status === 'fulfilled') {
      const data = returnRes.value.data
      kpiCards.value[5].value = data?.total || data?.records?.length || 0
    }

    // 汇总待处理事项
    const items: PendingItem[] = []

    if (inboundRes.status === 'fulfilled') {
      const records = inboundRes.value.data?.records || []
      records.slice(0, 5).forEach((r: any) => {
        items.push({
          id: `inbound-${r.id}`,
          type: 'inbound',
          typeLabel: '入库',
          typeColor: 'orange',
          code: r.inboundNo || '-',
          summary: `${r.supplierName || ''} ${r.warehouseName || ''}`,
          status: r.status,
          statusMap: INBOUND_STATUS,
          date: r.inboundDate || '',
          path: '/erp/stock-in',
        })
      })
    }

    if (shipmentRes.status === 'fulfilled') {
      const records = shipmentRes.value.data?.records || []
      records.slice(0, 5).forEach((r: any) => {
        items.push({
          id: `shipment-${r.id}`,
          type: 'shipment',
          typeLabel: '发货',
          typeColor: 'blue',
          code: r.shipmentNo || '-',
          summary: `${r.customerName || ''} → ${r.warehouseName || ''}`,
          status: r.status,
          statusMap: SHIPMENT_STATUS,
          date: r.shipmentDate || '',
          path: '/erp/shipment',
        })
      })
    }

    if (stocktakeRes.status === 'fulfilled') {
      const records = stocktakeRes.value.data?.records || []
      records.slice(0, 5).forEach((r: any) => {
        items.push({
          id: `stocktake-${r.id}`,
          type: 'stocktake',
          typeLabel: '盘点',
          typeColor: 'cyan',
          code: r.checkNo || r.stocktakeNo || '-',
          summary: r.warehouseName || '',
          status: r.status,
          statusMap: STOCKTAKE_STATUS_ORDER,
          date: r.checkDate || r.stocktakeDate || '',
          path: '/erp/stocktake',
        })
      })
    }

    if (returnRes.status === 'fulfilled') {
      const records = returnRes.value.data?.records || []
      records.slice(0, 5).forEach((r: any) => {
        items.push({
          id: `return-${r.id}`,
          type: 'return',
          typeLabel: '退货',
          typeColor: 'red',
          code: r.returnNo || '-',
          summary: r.customerName || r.returnReason || '',
          status: r.status,
          statusMap: RETURN_STATUS,
          date: r.returnDate || '',
          path: '/erp/return',
        })
      })
    }

    // 按日期排序
    items.sort((a, b) => (b.date || '').localeCompare(a.date || ''))
    pendingItems.value = items

    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  } catch (error) {
    console.warn('[ERP仪表盘] 加载数据失败', error)
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// ── 防抖 ──────────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 自动刷新 ──────────────────────────────────────────
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 键盘快捷键 ────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault(); debounceClick('refresh', loadData); return
  }
}

function handleError(err: any) {
  console.warn('[ERP仪表盘] ErrorBoundary 捕获异常:', err)
}

onMounted(() => {
  loadData()
  autoRefreshCountdown.value = 300
  refreshTimer = setInterval(() => { loadData(); autoRefreshCountdown.value = 300 }, 300000)
  countdownTimer = setInterval(() => { if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value-- }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.erp-dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.erp-dashboard-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.erp-dashboard-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.erp-dashboard-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.data-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

/* KPI 卡片 */
.kpi-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s;
  text-align: center;
}

.kpi-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.kpi-value {
  font-size: 28px;
  font-weight: 700;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  line-height: 1.2;
}

.kpi-label {
  font-size: 13px;
  color: #666;
  margin: 4px 0 8px;
}

.kpi-link {
  font-size: 12px;
  color: #1890ff;
  opacity: 0;
  transition: opacity 0.2s;
}

.kpi-card:hover .kpi-link {
  opacity: 1;
}

/* 快捷入口 */
.quick-entry-btn {
  width: 100%;
  text-align: left;
  padding: 8px 12px;
  height: auto;
  border-color: #f0f0f0;
  font-size: 13px;
}

.quick-entry-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
}

/* 区块卡片 */
.section-card {
  height: 100%;
}

.section-card :deep(.ant-card-head) {
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 0;
  font-weight: 600;
  font-size: 14px;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>
