<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="退货申请处理"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="退货申请单"
      row-key="id"
      empty-text="暂无退货申请"
    >
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="STATUS_MAP[text]?.color">
            {{ STATUS_MAP[text]?.label || '未知' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'totalAmount'">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'totalQuantity'">
          {{ formatQty(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="openDetail(record)"
            >
              详情
            </a-button>
            <template v-if="record.status === 1">
              <a-popconfirm
                title="确认审批通过该退货申请？"
                ok-text="通过"
                cancel-text="取消"
                @confirm="handleApprove(record)"
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
                type="link"
                size="small"
                danger
                @click="openReject(record)"
              >
                拒绝
              </a-button>
            </template>
            <a-popconfirm
              v-if="record.status === 2"
              title="确认完成该退货申请单？"
              ok-text="完成"
              cancel-text="取消"
              @confirm="handleComplete(record)"
            >
              <a-button
                type="link"
                size="small"
                style="color: #52c41a"
              >
                完成
              </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <!-- 退货详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      :title="'退货申请详情 - ' + (detailData?.returnNo || '')"
      :footer="null"
      width="860px"
    >
      <a-descriptions
        v-if="detailData"
        bordered
        :column="2"
        size="small"
      >
        <a-descriptions-item label="退货单号">
          {{ detailData.returnNo }}
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="STATUS_MAP[detailData.status ?? -1]?.color">
            {{ STATUS_MAP[detailData.status ?? -1]?.label || '-' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="客户名称">
          {{ detailData.customerName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="退货仓库">
          {{ detailData.warehouseName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="退货数量">
          {{ formatQty(detailData.totalQuantity) }}
        </a-descriptions-item>
        <a-descriptions-item label="退货金额">
          ¥{{ formatMoney(detailData.totalAmount) }}
        </a-descriptions-item>
        <a-descriptions-item label="制单人">
          {{ detailData.creatorName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="审核人">
          {{ detailData.auditorName || detailData.auditor || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="申请时间">
          {{ detailData.createTime || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="审核时间">
          {{ detailData.auditTime || detailData.approvedTime || '-' }}
        </a-descriptions-item>
        <a-descriptions-item
          label="退货原因"
          :span="2"
        >
          {{ detailData.reason || '-' }}
        </a-descriptions-item>
        <a-descriptions-item
          label="备注"
          :span="2"
        >
          {{ detailData.remark || detailData.approvedNote || '-' }}
        </a-descriptions-item>
      </a-descriptions>

      <a-divider>退货明细</a-divider>
      <a-table
        :data-source="detailData?.items || []"
        :columns="itemColumns"
        :pagination="false"
        size="small"
        row-key="id"
        :locale="{ emptyText: '暂无明细' }"
      >
        <template #bodyCell="{ column, text }">
          <template v-if="['unitPrice', 'lineAmount'].includes(column.dataIndex as string)">
            {{ formatMoney(text) }}
          </template>
          <template v-else-if="column.dataIndex === 'returnQuantity'">
            {{ formatQty(text) }}
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 拒绝原因弹窗 -->
    <a-modal
      v-model:open="rejectVisible"
      title="拒绝退货申请"
      :confirm-loading="rejectLoading"
      @ok="handleRejectConfirm"
    >
      <a-textarea
        v-model:value="rejectReason"
        placeholder="请输入拒绝原因（必填）"
        :rows="3"
        :maxlength="500"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { saleReturnApi } from '@/api/erp'

defineOptions({ name: 'MallReturnProcess' })

// ═══ 退货单状态（与后端 SaleReturnServiceImpl 状态流转一致：0草稿 1待审批 2已审批 3已完成 4已取消） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '已完成', color: 'green' },
  4: { label: '已取消', color: 'red' }
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '退货单号' },
  { key: 'customerName', type: 'input', label: '客户', placeholder: '客户名称', width: 160 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
  },
  { key: 'dateRange', type: 'date-range', label: '申请日期' }
]

const columns: any[] = [
  { title: '退货单号', dataIndex: 'returnNo', key: 'returnNo', width: 170 },
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 150, ellipsis: true },
  { title: '退货仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '退货数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right' },
  { title: '退货金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' },
  { title: '退货原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '申请时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 170, fixed: 'right' }
]

const itemColumns: any[] = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 110 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', ellipsis: true },
  { title: '规格', dataIndex: 'specification', key: 'specification', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 70 },
  { title: '退货数量', dataIndex: 'returnQuantity', key: 'returnQuantity', width: 90, align: 'right' },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' },
  { title: '金额', dataIndex: 'lineAmount', key: 'lineAmount', width: 110, align: 'right' },
  { title: '退货原因', dataIndex: 'reason', key: 'reason', width: 120, ellipsis: true }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

// ═══ 数据请求（erp-sales /erp/sale/return/page） ═══
function fetcher(params: Record<string, any>) {
  return saleReturnApi.page(params)
}

// ═══ 详情 / 审批 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const detailVisible = ref(false)
const detailData = ref<any>(null)

async function openDetail(record: any) {
  try {
    const res: any = await saleReturnApi.getById(record.id)
    const data = res?.data ?? res
    if (!data?.items) {
      try {
        const items: any = await saleReturnApi.getItems(record.id)
        data.items = items?.data ?? items ?? []
      } catch {
        data.items = []
      }
    }
    detailData.value = data
    detailVisible.value = true
  } catch (e) {
    console.warn('[退货申请处理] 详情获取失败', e)
  }
}

async function handleApprove(record: any) {
  try {
    await saleReturnApi.approve(record.id)
    message.success(`退货单 ${record.returnNo} 已审批通过`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[退货申请处理] 审批通过失败', e)
  }
}

async function handleComplete(record: any) {
  try {
    await saleReturnApi.complete(record.id)
    message.success(`退货单 ${record.returnNo} 已完成`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[退货申请处理] 完成失败', e)
  }
}

const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')
const rejectTarget = ref<any>(null)

function openReject(record: any) {
  rejectTarget.value = record
  rejectReason.value = ''
  rejectVisible.value = true
}

async function handleRejectConfirm() {
  if (!rejectReason.value.trim()) {
    message.warning('请输入拒绝原因')
    return
  }
  if (!rejectTarget.value) return
  rejectLoading.value = true
  try {
    await saleReturnApi.reject(rejectTarget.value.id, rejectReason.value.trim())
    message.success(`退货单 ${rejectTarget.value.returnNo} 已拒绝并退回草稿`)
    rejectVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[退货申请处理] 拒绝失败', e)
  } finally {
    rejectLoading.value = false
  }
}
</script>
