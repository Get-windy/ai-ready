<template>
  <ErrorBoundary @error="handleError">
  <div class="detail-page">
    <!-- 固定顶栏 -->
    <div class="detail-header">
      <div class="detail-header__left">
        <a-button type="text" @click="goBack"><LeftOutlined /> 返回</a-button>
        <span class="detail-title">盘点单详情</span>
        <span v-if="detailData?.checkNo" class="detail-code">({{ detailData.checkNo }})</span>
      </div>
      <div class="detail-header__right">
        <a-space>
          <span v-if="lastUpdateTime" class="update-time">数据更新: {{ lastUpdateTime }}</span>
          <PrintButton v-if="detailData && detailData.status >= 6" template-type="stocktake" :business-id="detailData.id" business-type="stocktake" button-text="打印" button-size="small" />
          <a-button @click="goBack">返回</a-button>
        </a-space>
      </div>
    </div>

    <div class="detail-body">
      <a-spin :spinning="loading">
        <!-- 状态操作栏 -->
        <div class="detail-action-bar" v-if="detailData">
          <StatusTag :status="detailData.status" :map="STOCKTAKE_STATUS_ORDER" size="large" />
          <span class="action-bar-divider" />

          <!-- 草稿 -> 提交审批 -->
          <a-button v-if="detailData.status === 0" type="primary" @click="handleSubmitApproval">
            <SendOutlined /> 提交审批
          </a-button>
          <!-- 待审批 -> 审批/拒绝 -->
          <a-button v-if="detailData.status === 1" type="primary" @click="handleApprove">
            <CheckOutlined /> 审批通过
          </a-button>
          <a-button v-if="detailData.status === 1" danger @click="handleReject">
            <CloseOutlined /> 拒绝
          </a-button>
          <!-- 已审批 -> 开始盘点 -->
          <a-button v-if="detailData.status === 2" type="primary" @click="handleStart">
            <PlayCircleOutlined /> 开始盘点
          </a-button>
          <!-- 进行中 -> 完成盘点 -->
          <a-button v-if="detailData.status === 5" type="primary" @click="handleComplete">
            <CheckCircleOutlined /> 完成盘点
          </a-button>
          <!-- 已完成/有差异 -> 库存调整 -->
          <a-button v-if="detailData.status === 6 && detailData.diffItems > 0" type="primary" @click="handleAdjust">
            <AuditOutlined /> 库存调整
          </a-button>
        </div>

        <!-- 基本信息 -->
        <a-card title="基本信息" class="detail-card" v-if="detailData">
          <a-descriptions bordered :column="3" size="small">
            <a-descriptions-item label="盘点单号">{{ detailData.checkNo }}</a-descriptions-item>
            <a-descriptions-item label="仓库">{{ detailData.warehouseName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="盘点类型">{{ checkTypeLabel(detailData.checkType) }}</a-descriptions-item>
            <a-descriptions-item label="盘点日期">{{ detailData.checkDate ? formatDate(detailData.checkDate) : '-' }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <StatusTag :status="detailData.status" :map="STOCKTAKE_STATUS_ORDER" />
            </a-descriptions-item>
            <a-descriptions-item label="操作人">{{ detailData.creatorName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="盘点人">{{ detailData.checkerName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="监盘人">{{ detailData.supervisorName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="审批人">{{ detailData.approvedByName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="3">{{ detailData.remark || '-' }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 差异分析卡片（状态 >= 已完成） -->
        <a-card v-if="detailData && detailData.status >= 6" title="盘点差异分析" class="detail-card">
          <a-row :gutter="16" style="margin-bottom: 16px;">
            <a-col :span="8">
              <div class="diff-stat-card">
                <div class="diff-stat-label">系统数量</div>
                <div class="diff-stat-value">{{ detailData.totalBookQuantity ?? '-' }}</div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="diff-stat-card">
                <div class="diff-stat-label">实际数量</div>
                <div class="diff-stat-value">{{ detailData.totalActualQuantity ?? '-' }}</div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="diff-stat-card highlight-diff">
                <div class="diff-stat-label">差异数量</div>
                <div class="diff-stat-value" :class="diffTotalClass">
                  {{ diffTotalPrefix }}{{ detailData.totalDiffQuantity ?? 0 }}
                </div>
              </div>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="8">
              <div class="diff-stat-card">
                <div class="diff-stat-label">系统金额</div>
                <div class="diff-stat-value">¥{{ formatAmount(detailData.totalBookAmount) }}</div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="diff-stat-card">
                <div class="diff-stat-label">实际金额</div>
                <div class="diff-stat-value">¥{{ formatAmount(detailData.totalActualAmount) }}</div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="diff-stat-card highlight-diff">
                <div class="diff-stat-label">差异金额</div>
                <div class="diff-stat-value" :class="diffTotalClass">
                  {{ diffAmountPrefix }}¥{{ formatAmount(detailData.totalDiffAmount) }}
                </div>
              </div>
            </a-col>
          </a-row>

          <a-divider />
          <div class="diff-summary">
            <span>盘点总件数: <b>{{ detailData.totalItems || 0 }}</b></span>
            <span class="diff-summary-sep">|</span>
            <span>已盘点: <b>{{ detailData.checkedItems || 0 }}</b></span>
            <span class="diff-summary-sep">|</span>
            <span>差异项: <b :style="{ color: (detailData.diffItems || 0) > 0 ? '#ff4d4f' : '#52c41a' }">{{ detailData.diffItems || 0 }}</b></span>
            <span class="diff-summary-sep">|</span>
            <span>差异率: <b :style="{ color: diffRateColor }">{{ diffRateText }}</b></span>
          </div>
        </a-card>

        <!-- 盘点明细 -->
        <a-card title="盘点明细" class="detail-card" v-if="detailItems.length > 0">
          <div class="detail-card-toolbar">
            <a-space>
              <a-radio-group v-model:value="filterDiff" size="small" button-style="solid">
                <a-radio-button :value="0">全部 ({{ detailData?.totalItems || 0 }})</a-radio-button>
                <a-radio-button :value="1">有差异 ({{ detailData?.diffItems || 0 }})</a-radio-button>
              </a-radio-group>
            </a-space>
          </div>
          <a-table
            :data-source="filteredItems"
            :columns="detailColumns"
            :pagination="{ pageSize: 20, showSizeChanger: true, showTotal: t => `共 ${t} 条` }"
            size="small"
            bordered
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'diffQuantity'">
                <span :class="diffItemClass(record.diffQuantity)">
                  {{ record.diffQuantity > 0 ? '+' : '' }}{{ record.diffQuantity }}
                </span>
              </template>
              <template v-else-if="column.dataIndex === 'diffType'">
                <a-tag v-if="record.diffType === 1" color="green">盘盈</a-tag>
                <a-tag v-else-if="record.diffType === 2" color="red">盘亏</a-tag>
                <span v-else>-</span>
              </template>
              <template v-else-if="column.dataIndex === 'checkStatus'">
                <a-tag :color="record.checkStatus === 1 ? 'blue' : 'default'">
                  {{ record.checkStatus === 1 ? '已盘点' : '未盘点' }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-card>

        <!-- 空状态 -->
        <div v-if="!loading && !detailData" class="detail-empty">
          <WarningOutlined style="font-size: 48px; color: #d9d9d9;" />
          <p>未找到盘点单信息</p>
        </div>
      </a-spin>
    </div>

    <!-- 拒绝原因弹窗 -->
    <a-modal v-model:open="rejectModalVisible" title="拒绝盘点单" @ok="handleRejectConfirm" :confirm-loading="rejectLoading" ok-text="确认拒绝" ok-button-props="{ danger: true }">
      <a-form layout="vertical">
        <a-form-item label="拒绝原因" required>
          <a-textarea v-model:value="rejectReason" :rows="3" placeholder="请输入拒绝原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  LeftOutlined, CheckCircleOutlined, PlayCircleOutlined, WarningOutlined,
  SendOutlined, CheckOutlined, CloseOutlined, AuditOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import { STOCKTAKE_STATUS_ORDER } from '@/utils/statusConfig'
import request from '@/utils/request'

interface StockCheckItemData {
  id: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  bookQuantity: number
  actualQuantity: number
  diffQuantity: number
  diffType: number
  checkStatus: number
  unitCost: number
  bookAmount: number
  actualAmount: number
  diffAmount: number
  checkedTime: string
  checkNote: string
}

function handleError(err: any) { console.warn('[盘点详情]', err) }

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const lastUpdateTime = ref('')
const detailData = ref<any>(null)
const detailItems = ref<StockCheckItemData[]>([])
const filterDiff = ref(0)
const rejectModalVisible = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')

const filteredItems = computed(() => {
  if (filterDiff.value === 0) return detailItems.value
  return detailItems.value.filter(i => i.diffQuantity !== 0)
})

const diffTotalPrefix = computed(() => {
  const d = detailData.value?.totalDiffQuantity
  if (d == null) return ''
  return d > 0 ? '+' : ''
})

const diffAmountPrefix = computed(() => {
  const d = detailData.value?.totalDiffAmount
  if (d == null) return ''
  return d > 0 ? '+' : ''
})

const diffTotalClass = computed(() => {
  const d = detailData.value?.totalDiffQuantity
  if (!d) return ''
  return d > 0 ? 'diff-positive' : 'diff-negative'
})

const diffRateText = computed(() => {
  const total = detailData.value?.totalBookQuantity
  const diff = detailData.value?.totalDiffQuantity
  if (!total || !diff) return '0.00%'
  const rate = Math.abs(diff) / Math.abs(total) * 100
  return `${rate.toFixed(2)}%`
})

const diffRateColor = computed(() => {
  const d = detailData.value?.totalDiffQuantity
  if (!d) return '#303133'
  return Math.abs(d) > 0 ? '#ff4d4f' : '#52c41a'
})

const detailColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 130 },
  { title: '产品名称', dataIndex: 'productName', width: 160 },
  { title: '规格', dataIndex: 'productSpec', width: 100 },
  { title: '单位', dataIndex: 'productUnit', width: 60 },
  { title: '系统数量', dataIndex: 'bookQuantity', width: 90, align: 'right' },
  { title: '实际数量', dataIndex: 'actualQuantity', width: 90, align: 'right' },
  { title: '差异', dataIndex: 'diffQuantity', width: 90, align: 'right' },
  { title: '差异类型', dataIndex: 'diffType', width: 80, align: 'center' },
  { title: '盘点状态', dataIndex: 'checkStatus', width: 80, align: 'center' },
  { title: '单位成本', dataIndex: 'unitCost', width: 100, align: 'right' },
  { title: '差异金额', dataIndex: 'diffAmount', width: 110, align: 'right' },
  { title: '备注', dataIndex: 'checkNote', minWidth: 120, ellipsis: true },
]

function checkTypeLabel(type: number | undefined) {
  const map: Record<number, string> = { 1: '全盘', 2: '抽盘', 3: '动态盘点' }
  return type != null ? (map[type] || `类型${type}`) : '-'
}

function formatDate(d: string | undefined) {
  if (!d) return '-'
  try { return d.slice(0, 10) } catch { return d }
}

function formatAmount(v: number | undefined | null) {
  if (v == null) return '0.00'
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function diffItemClass(v: number) {
  if (v > 0) return 'diff-positive'
  if (v < 0) return 'diff-negative'
  return ''
}

function goBack() { router.back() }

async function fetchDetail() {
  const id = Number(route.params.id)
  if (!id) { message.error('参数错误'); return }
  loading.value = true
  try {
    const [headerRes, itemsRes] = await Promise.all([
      request.get(`/erp/stock/check/${id}`),
      request.get(`/erp/stock/check/${id}/items`).catch(() => ({ data: [] })),
    ])
    detailData.value = headerRes.data || null
    const rawItems: any[] = itemsRes.data || []
    detailItems.value = rawItems.map(i => ({
      id: i.id,
      productCode: i.productCode || '',
      productName: i.productName || '',
      productSpec: i.productSpec || '',
      productUnit: i.productUnit || '',
      bookQuantity: i.bookQuantity ?? 0,
      actualQuantity: i.actualQuantity ?? 0,
      diffQuantity: i.diffQuantity ?? 0,
      diffType: i.diffType ?? 0,
      checkStatus: i.checkStatus ?? 0,
      unitCost: i.unitCost ?? 0,
      bookAmount: i.bookAmount ?? 0,
      actualAmount: i.actualAmount ?? 0,
      diffAmount: i.diffAmount ?? 0,
      checkedTime: i.checkedTime || '',
      checkNote: i.checkNote || '',
    }))
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  } catch (err) {
    console.warn('[盘点详情] 获取数据失败', err)
    message.error('获取盘点单详情失败')
  } finally {
    loading.value = false
  }
}

const handleSubmitApproval = () => {
  const record = detailData.value
  if (!record) return
  Modal.confirm({
    title: '提交审批',
    content: `确认提交盘点单 ${record.checkNo} 进行审批？`,
    okText: '确认提交',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/check/${record.id}/submit`)
        message.success('已提交审批')
        fetchDetail()
      } catch (error) {
        console.warn('[盘点详情] 提交审批失败', error)
        message.error('提交审批失败')
      }
    }
  })
}

const handleApprove = () => {
  const record = detailData.value
  if (!record) return
  Modal.confirm({
    title: '审批通过',
    content: `确认审批通过盘点单 ${record.checkNo}？`,
    okText: '确认通过',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/check/${record.id}/approve`)
        message.success('已审批通过')
        fetchDetail()
      } catch (error) {
        console.warn('[盘点详情] 审批失败', error)
        message.error('审批失败')
      }
    }
  })
}

const handleReject = () => {
  rejectReason.value = ''
  rejectModalVisible.value = true
}

const handleRejectConfirm = async () => {
  if (!rejectReason.value.trim()) {
    message.warning('请输入拒绝原因')
    return
  }
  const record = detailData.value
  if (!record) return
  rejectLoading.value = true
  try {
    await request.post(`/erp/stock/check/${record.id}/reject?reason=${encodeURIComponent(rejectReason.value)}`)
    message.success('已拒绝')
    rejectModalVisible.value = false
    fetchDetail()
  } catch (error) {
    console.warn('[盘点详情] 拒绝失败', error)
    message.error('拒绝失败')
  } finally {
    rejectLoading.value = false
  }
}

const handleStart = () => {
  const record = detailData.value
  if (!record) return
  Modal.confirm({
    title: '开始盘点',
    content: `确认开始盘点单 ${record.checkNo} 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/check/${record.id}/start`)
        message.success('已开始盘点')
        fetchDetail()
      } catch (error) {
        console.warn('[盘点详情] 开始盘点失败', error)
        message.error('开始盘点失败')
      }
    }
  })
}

const handleComplete = () => {
  const record = detailData.value
  if (!record) return
  Modal.confirm({
    title: '完成盘点',
    content: `确认完成盘点单 ${record.checkNo} 吗？完成后可进行库存调整。`,
    okText: '确认完成',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/check/${record.id}/complete`)
        message.success('盘点完成')
        fetchDetail()
      } catch (error) {
        console.warn('[盘点详情] 完成盘点失败', error)
        message.error('完成盘点失败')
      }
    }
  })
}

const handleAdjust = () => {
  const record = detailData.value
  if (!record) return
  Modal.confirm({
    title: '库存调整确认',
    content: `盘点单 ${record.checkNo} 存在 ${record.diffItems} 项差异（差异金额 ¥${formatAmount(record.totalDiffAmount)}）。确认按差异调整库存？`,
    okText: '确认调整',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/check/${record.id}/adjust`)
        message.success('库存调整完成')
        fetchDetail()
      } catch (error) {
        console.warn('[盘点详情] 库存调整失败', error)
        message.error('库存调整失败')
      }
    }
  })
}

onMounted(() => { fetchDetail() })
onUnmounted(() => {})
</script>

<style scoped>
.detail-page { height: 100%; display: flex; flex-direction: column; background: #f5f7fa; }
.detail-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 24px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; z-index: 10; }
.detail-header__left { display: flex; align-items: center; gap: 8px; }
.detail-title { font-size: 16px; font-weight: 600; color: #303133; }
.detail-code { font-size: 13px; color: #999; }
.detail-header__right { display: flex; align-items: center; gap: 8px; }
.update-time { font-size: 12px; color: #999; white-space: nowrap; }
.detail-body { flex: 1; overflow: auto; padding: 16px 24px; }
.detail-action-bar { display: flex; align-items: center; gap: 12px; padding: 12px 16px; background: #fff; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.action-bar-divider { flex: 1; }
.detail-card { margin-bottom: 16px; }
.detail-card-toolbar { margin-bottom: 12px; }
.detail-empty { display: flex; flex-direction: column; align-items: center; padding: 80px 0; gap: 12px; color: #999; }

.diff-stat-card { padding: 16px; background: #fafafa; border-radius: 6px; border: 1px solid #f0f0f0; text-align: center; }
.diff-stat-card.highlight-diff { background: #fffbe6; border-color: #ffe58f; }
.diff-stat-label { font-size: 13px; color: #666; margin-bottom: 8px; }
.diff-stat-value { font-size: 24px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, monospace; font-variant-numeric: tabular-nums; }
.diff-positive { color: #3f8600; font-weight: bold; }
.diff-negative { color: #ff4d4f; font-weight: bold; }

.diff-summary { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #666; }
.diff-summary-sep { color: #d9d9d9; }

:deep(.ant-input-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
