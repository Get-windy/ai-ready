<template>
  <div>
    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane
        key="receivable"
        tab="应收调整"
      >
        <ARReportPage
          ref="receivableRef"
          title="应收调整"
          :query-fields="receivableQueryFields"
          :columns="receivableColumns"
          :fetcher="receivableFetcher"
          export-file-name="应收调整"
          row-key="id"
        >
          <template #bodyCell="{ column, record, text }">
            <template v-if="column.dataIndex === 'status'">
              <a-tag :color="STATUS_MAP[text]?.color || 'default'">
                {{ STATUS_MAP[text]?.label || text }}
              </a-tag>
            </template>
            <template v-else-if="['totalAmount', 'paidAmount', 'remainingAmount'].includes(column.dataIndex as string)">
              {{ formatMoney(text) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space :size="4">
                <a-button
                  type="link"
                  size="small"
                  :disabled="Number(record.remainingAmount) <= 0"
                  @click="openWriteOff(record, 'receivable')"
                >
                  核销
                </a-button>
                <a-popconfirm
                  v-if="record.status !== 'baddebt'"
                  title="确认将该笔应收标记为坏账？"
                  @confirm="handleBadDebt(record)"
                >
                  <a-button
                    type="link"
                    size="small"
                    danger
                  >
                    坏账
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </ARReportPage>
      </a-tab-pane>
      <a-tab-pane
        key="payable"
        tab="应付调整"
      >
        <ARReportPage
          ref="payableRef"
          title="应付调整"
          :query-fields="payableQueryFields"
          :columns="payableColumns"
          :fetcher="payableFetcher"
          export-file-name="应付调整"
          row-key="id"
        >
          <template #bodyCell="{ column, record, text }">
            <template v-if="column.dataIndex === 'status'">
              <a-tag :color="STATUS_MAP[text]?.color || 'default'">
                {{ STATUS_MAP[text]?.label || text }}
              </a-tag>
            </template>
            <template v-else-if="['totalAmount', 'paidAmount', 'remainingAmount'].includes(column.dataIndex as string)">
              {{ formatMoney(text) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                :disabled="Number(record.remainingAmount) <= 0"
                @click="openWriteOff(record, 'payable')"
              >
                核销
              </a-button>
            </template>
          </template>
        </ARReportPage>
      </a-tab-pane>
    </a-tabs>

    <!-- ═══ 核销金额弹窗 ═══ -->
    <a-modal
      v-model:open="writeOffOpen"
      :title="writeOffType === 'receivable' ? '应收核销' : '应付核销'"
      :confirm-loading="submitting"
      @ok="handleWriteOff"
      @cancel="resetWriteOff"
    >
      <a-form layout="vertical">
        <a-form-item label="往来单位">
          <span>{{ currentRecord?.customerName || currentRecord?.supplierName }}（剩余 {{ formatMoney(currentRecord?.remainingAmount) }}）</span>
        </a-form-item>
        <a-form-item
          label="核销金额"
          required
        >
          <a-input-number
            v-model:value="writeOffAmount"
            :min="0.01"
            :max="Number(currentRecord?.remainingAmount) || undefined"
            :precision="2"
            style="width: 100%"
            placeholder="请输入核销金额"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { receivableApi, payableApi } from '@/api/finance'

// ═══ 应收/应付状态（与后端 Receivable.status / Payable.status 一致） ═══
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  draft: { label: '草稿', color: 'default' },
  partial: { label: '部分核销', color: 'orange' },
  paid: { label: '已结清', color: 'green' },
  baddebt: { label: '坏账', color: 'red' }
}

const STATUS_OPTIONS = Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value }))

// ═══ 查询区 ═══
const receivableQueryFields: ReportQueryField[] = [
  { key: 'customerId', type: 'input', label: '客户ID', placeholder: '客户ID', width: 160 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态', options: STATUS_OPTIONS, width: 140 }
]
const payableQueryFields: ReportQueryField[] = [
  { key: 'supplierId', type: 'input', label: '供应商ID', placeholder: '供应商ID', width: 160 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态', options: STATUS_OPTIONS, width: 140 }
]

// ═══ 表格列（与后端 ReceivableDTO / PayableDTO 字段一致） ═══
const baseColumns: any[] = [
  { title: '来源单号', dataIndex: 'sourceNo', key: 'sourceNo', width: 160 },
  { title: '发票号', dataIndex: 'invoiceNo', key: 'invoiceNo', width: 130 },
  { title: '总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, align: 'right' },
  { title: '已核销', dataIndex: 'paidAmount', key: 'paidAmount', width: 120, align: 'right' },
  { title: '剩余', dataIndex: 'remainingAmount', key: 'remainingAmount', width: 120, align: 'right' },
  { title: '到期日', dataIndex: 'dueDate', key: 'dueDate', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, align: 'center' },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true }
]
const receivableColumns: any[] = [
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 150, ellipsis: true },
  ...baseColumns,
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]
const payableColumns: any[] = [
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150, ellipsis: true },
  ...baseColumns,
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求（/erp/finance/receivable|payable/list，page/size 风格） ═══
function receivableFetcher(params: Record<string, any>) {
  return receivableApi.getPage(params)
}
function payableFetcher(params: Record<string, any>) {
  return payableApi.getPage(params)
}

// ═══ 核销 / 坏账操作 ═══
const activeTab = ref('receivable')
const receivableRef = ref<InstanceType<typeof ARReportPage>>()
const payableRef = ref<InstanceType<typeof ARReportPage>>()
const writeOffOpen = ref(false)
const submitting = ref(false)
const writeOffType = ref<'receivable' | 'payable'>('receivable')
const currentRecord = ref<any>(null)
const writeOffAmount = ref<number>()

function openWriteOff(record: any, type: 'receivable' | 'payable') {
  currentRecord.value = record
  writeOffType.value = type
  writeOffAmount.value = Number(record.remainingAmount) || undefined
  writeOffOpen.value = true
}

function resetWriteOff() {
  writeOffOpen.value = false
  currentRecord.value = null
  writeOffAmount.value = undefined
}

async function handleWriteOff() {
  if (!currentRecord.value || !writeOffAmount.value) {
    message.warning('请输入核销金额')
    return
  }
  submitting.value = true
  try {
    if (writeOffType.value === 'receivable') {
      await receivableApi.writeOff(currentRecord.value.id, writeOffAmount.value)
    } else {
      await payableApi.writeOff(currentRecord.value.id, writeOffAmount.value)
    }
    message.success('核销成功')
    resetWriteOff()
    reloadCurrent()
  } catch (e) {
    console.warn('[应收应付调整] 核销失败', e)
  } finally {
    submitting.value = false
  }
}

async function handleBadDebt(record: any) {
  try {
    await receivableApi.markBadDebt(record.id)
    message.success('已标记为坏账')
    reloadCurrent()
  } catch (e) {
    console.warn('[应收应付调整] 坏账标记失败', e)
  }
}

function reloadCurrent() {
  if (writeOffType.value === 'receivable') receivableRef.value?.reload()
  else payableRef.value?.reload()
}
</script>
