<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="订单处理"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      :stat-cards="statCards"
      page-param-style="pageNum"
      export-file-name="商城订单"
      row-key="id"
      empty-text="暂无商城订单"
    >
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'mallStatus'">
          <a-tag :color="ORDER_STATUS_MAP[mallStatusOf(record)]?.color">
            {{ ORDER_STATUS_MAP[mallStatusOf(record)]?.label || mallStatusOf(record) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'paymentStatus'">
          <a-tag :color="PAYMENT_STATUS_MAP[text]?.color">
            {{ PAYMENT_STATUS_MAP[text]?.label || '-' }}
          </a-tag>
        </template>
        <template v-else-if="['totalAmount', 'receivedAmount'].includes(column.dataIndex as string)">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="openDetail(record as MallOrderAdmin)"
            >
              详情
            </a-button>
            <a-popconfirm
              v-if="mallStatusOf(record) === 'PAID'"
              title="确认审核通过该订单？"
              ok-text="通过"
              cancel-text="取消"
              @confirm="handleApprove(record as MallOrderAdmin)"
            >
              <a-button
                type="link"
                size="small"
                style="color: #52c41a"
              >
                通过
              </a-button>
            </a-popconfirm>
            <a-button
              v-if="['PAID', 'PENDING_PAYMENT'].includes(mallStatusOf(record))"
              type="link"
              size="small"
              danger
              @click="openReject(record as MallOrderAdmin)"
            >
              驳回
            </a-button>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <!-- 订单详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      :title="'订单详情 - ' + (detailData?.orderNo || '')"
      :footer="null"
      width="720px"
    >
      <a-descriptions
        v-if="detailData"
        bordered
        :column="2"
        size="small"
      >
        <a-descriptions-item
          label="订单编号"
          :span="2"
        >
          {{ detailData.orderNo }}
        </a-descriptions-item>
        <a-descriptions-item label="客户名称">
          {{ detailData.customerName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="订单来源">
          {{ detailData.orderSource === 3 ? '个人会员商城' : '企业客户商城' }}
        </a-descriptions-item>
        <a-descriptions-item label="订单状态">
          <a-tag :color="ORDER_STATUS_MAP[mallStatusOf(detailData)]?.color">
            {{ ORDER_STATUS_MAP[mallStatusOf(detailData)]?.label || '-' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="支付状态">
          {{ PAYMENT_STATUS_MAP[detailData.paymentStatus ?? -1]?.label || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="订单总额">
          ¥{{ formatMoney(detailData.totalAmount) }}
        </a-descriptions-item>
        <a-descriptions-item label="已收款">
          ¥{{ formatMoney(detailData.receivedAmount) }}
        </a-descriptions-item>
        <a-descriptions-item label="支付方式">
          {{ detailData.paymentMethod || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="下单时间">
          {{ detailData.orderDate || detailData.createTime || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="收货人">
          {{ detailData.consignee || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="联系电话">
          {{ detailData.consigneePhone || '-' }}
        </a-descriptions-item>
        <a-descriptions-item
          label="收货地址"
          :span="2"
        >
          {{ detailData.consigneeAddress || detailData.shippingAddress || '-' }}
        </a-descriptions-item>
        <a-descriptions-item
          label="买家备注"
          :span="2"
        >
          {{ detailData.buyerRemark || '-' }}
        </a-descriptions-item>
        <a-descriptions-item
          label="单据备注"
          :span="2"
        >
          {{ detailData.remark || detailData.orderRemark || '-' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <!-- 驳回原因弹窗 -->
    <a-modal
      v-model:open="rejectVisible"
      title="驳回订单"
      :confirm-loading="rejectLoading"
      @ok="handleRejectConfirm"
    >
      <a-textarea
        v-model:value="rejectReason"
        placeholder="请输入驳回原因（必填）"
        :rows="3"
        :maxlength="500"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, StatCardItem } from '@/components/ARReportPage/types'
import { mallAdminOrderApi, mallTradeApi, type MallOrderAdmin } from '@/api/erp/mall'

defineOptions({ name: 'MallOrderProcess' })

// ═══ 订单状态（与后端 MallAdminServiceImpl.toErpStatus / extInfo.originalMallStatus 一致） ═══
const ORDER_STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING_PAYMENT: { label: '待付款', color: 'orange' },
  PAID: { label: '待审核', color: 'orange' },
  APPROVED: { label: '已审核', color: 'blue' },
  SHIPPED: { label: '已发货', color: 'cyan' },
  COMPLETED: { label: '已完成', color: 'green' },
  CANCELLED: { label: '已取消', color: 'red' },
  REJECTED: { label: '已驳回', color: 'red' }
}

// ═══ 支付状态（与后端 ErpSaleOrderMall.paymentStatus 注释一致） ═══
const PAYMENT_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待支付', color: 'orange' },
  1: { label: '支付中', color: 'blue' },
  2: { label: '已支付', color: 'green' },
  3: { label: '部分支付', color: 'gold' },
  4: { label: '已退款', color: 'red' }
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '订单号/客户/收货人' },
  {
    key: 'orderStatus',
    type: 'select',
    label: '订单状态',
    placeholder: '全部状态',
    options: [
      { label: '待付款', value: 'PENDING_PAYMENT' },
      { label: '待审核', value: 'PAID' },
      { label: '已审核', value: 'APPROVED' },
      { label: '已发货', value: 'SHIPPED' },
      { label: '已完成', value: 'COMPLETED' },
      { label: '已取消/驳回', value: 'CANCELLED' }
    ]
  }
]

const columns: any[] = [
  { title: '订单编号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 150, ellipsis: true },
  { title: '订单总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' },
  { title: '已收款', dataIndex: 'receivedAmount', key: 'receivedAmount', width: 110, align: 'right' },
  { title: '订单状态', dataIndex: 'mallStatus', key: 'mallStatus', width: 100 },
  { title: '支付状态', dataIndex: 'paymentStatus', key: 'paymentStatus', width: 100 },
  { title: '收货人', dataIndex: 'consignee', key: 'consignee', width: 90 },
  { title: '下单时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 150, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 还原商城原始状态：优先 extInfo.originalMallStatus，回退 ERP status 映射（与后端逻辑一致） */
function mallStatusOf(record: Partial<MallOrderAdmin>): string {
  const ext = record.extInfo
  if (ext && ext.includes('originalMallStatus')) {
    const m = ext.match(/"originalMallStatus"\s*:\s*"([^"]+)"/)
    if (m) return m[1]
  }
  const fallback: Record<number, string> = {
    0: 'PENDING_PAYMENT', 1: 'PAID', 2: 'APPROVED', 3: 'SHIPPED', 4: 'COMPLETED', 5: 'CANCELLED'
  }
  return fallback[record.status ?? 0] || 'PENDING_PAYMENT'
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return mallAdminOrderApi.page(params)
}

// ═══ 交易汇总卡片（独立请求 /trade-analysis） ═══
const tradeSummary = ref<{ totalOrderCount?: number; totalGmv?: number; avgOrderAmount?: number; refundRate?: number }>({})

const statCards = computed<StatCardItem[]>(() => [
  { label: '总订单数', value: Number(tradeSummary.value.totalOrderCount) || 0, suffix: '单' },
  { label: '商城GMV', value: Number(tradeSummary.value.totalGmv) || 0, precision: 2, prefix: '¥' },
  { label: '客单价', value: Number(tradeSummary.value.avgOrderAmount) || 0, precision: 2, prefix: '¥' },
  { label: '退款率', value: Number(tradeSummary.value.refundRate) || 0, precision: 2, suffix: '%' }
])

// ═══ 详情 / 审核 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const detailVisible = ref(false)
const detailData = ref<MallOrderAdmin | null>(null)

async function openDetail(record: MallOrderAdmin) {
  try {
    detailData.value = await mallAdminOrderApi.detail(record.id)
    detailVisible.value = true
  } catch (e) {
    console.warn('[订单处理] 订单详情获取失败', e)
  }
}

async function handleApprove(record: MallOrderAdmin) {
  try {
    await mallAdminOrderApi.approve(record.id)
    message.success(`订单 ${record.orderNo} 已审核通过`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[订单处理] 审核通过失败', e)
  }
}

const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')
const rejectTarget = ref<MallOrderAdmin | null>(null)

function openReject(record: MallOrderAdmin) {
  rejectTarget.value = record
  rejectReason.value = ''
  rejectVisible.value = true
}

async function handleRejectConfirm() {
  if (!rejectReason.value.trim()) {
    message.warning('请输入驳回原因')
    return
  }
  if (!rejectTarget.value) return
  rejectLoading.value = true
  try {
    await mallAdminOrderApi.reject(rejectTarget.value.id, rejectReason.value.trim())
    message.success(`订单 ${rejectTarget.value.orderNo} 已驳回`)
    rejectVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[订单处理] 驳回失败', e)
  } finally {
    rejectLoading.value = false
  }
}

onMounted(async () => {
  try {
    const res = await mallTradeApi.analysis()
    tradeSummary.value = res?.summary || {}
  } catch (e) {
    console.warn('[订单处理] 交易汇总获取失败', e)
  }
})
</script>
