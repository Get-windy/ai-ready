<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      title="采购费用分摊"
      full-height
    >
      <template #headerExtra>
        <a-button @click="goBack">返回列表</a-button>
      </template>

      <!-- ═══ 1. 费用项 ═══ -->
      <div class="panel">
        <div class="panel-title">
          费用项
          <span class="panel-subtitle">已选 {{ expenseItems.length }} 项，合计 ¥{{ fmtAmount(expenseTotal) }}</span>
          <a-button size="small" type="link" @click="addExpenseItem">+ 手动添加</a-button>
          <a-button size="small" type="link" @click="showExpenseDocPicker = true">从费用单选择</a-button>
        </div>
        <a-table
          :columns="expenseColumns"
          :data-source="expenseItems"
          row-key="rowKey"
          size="small"
          :pagination="false"
          bordered
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.dataIndex === 'expenseType'">
              <a-select
                v-model:value="record.expenseType"
                style="width: 120px"
                :options="expenseTypeOptions"
                :disabled="record.source === 'doc'"
              />
            </template>
            <template v-else-if="column.dataIndex === 'amount'">
              <a-input-number
                v-model:value="record.amount"
                :min="0"
                :precision="2"
                style="width: 120px"
                :disabled="record.source === 'doc'"
                @change="recalcExpenseTotal"
              />
            </template>
            <template v-else-if="column.dataIndex === 'source'">
              <a-tag :color="record.source === 'doc' ? 'blue' : 'default'">
                {{ record.source === 'doc' ? '费用单' : '手动' }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-popconfirm title="移除该费用项？" @confirm="removeExpenseItem(index)">
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </template>
          </template>
          <template #emptyText>
            <a-empty description="请添加费用项（手动或从费用单选择）" />
          </template>
        </a-table>
      </div>

      <!-- ═══ 2. 分摊目标 - 采购入库单 ═══ -->
      <div class="panel">
        <div class="panel-title">
          待分摊入库单
          <span class="panel-subtitle">已选 {{ selectedInboundIds.length }} 单</span>
          <a-button size="small" type="link" :loading="inboundLoading" @click="fetchInbounds">刷新</a-button>
        </div>
        <a-form layout="inline" style="margin-bottom: 12px;">
          <a-form-item label="供应商">
            <a-select
              v-model:value="inboundFilter.supplierId"
              placeholder="全部供应商"
              show-search
              :filter-option="filterOption"
              :options="supplierOptions"
              allow-clear
              style="width: 200px"
              @change="fetchInbounds"
            />
          </a-form-item>
          <a-form-item label="入库日期">
            <a-range-picker
              v-model:value="inboundDateRange"
              style="width: 220px"
              @change="handleInboundDateChange"
            />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" ghost size="small" @click="fetchInbounds">查询</a-button>
          </a-form-item>
        </a-form>
        <a-table
          :columns="inboundColumns"
          :data-source="inboundOptions"
          :loading="inboundLoading"
          row-key="id"
          size="small"
          :pagination="{ pageSize: 5 }"
          :scroll="{ x: 800 }"
          :row-selection="inboundSelection"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'inboundNo'">
              <a>{{ record.inboundNo }}</a>
            </template>
            <template v-else-if="column.dataIndex === 'totalAmount'">
              ¥{{ fmtAmount(record.totalAmount) }}
            </template>
            <template v-else-if="column.dataIndex === 'totalQuantity'">
              {{ fmtQty(record.totalQuantity) }}
            </template>
            <template v-else-if="column.dataIndex === 'totalWeight'">
              {{ record.totalWeight ? fmtQty(record.totalWeight) : '-' }}
            </template>
          </template>
          <template #emptyText>
            <a-empty description="暂无采购入库单数据" />
          </template>
        </a-table>
      </div>

      <!-- ═══ 3. 分摊方式 ═══ -->
      <div class="panel">
        <div class="panel-title">分摊方式</div>
        <a-radio-group v-model:value="sharingMethod" button-style="solid">
          <a-radio-button value="amount">按金额分摊</a-radio-button>
          <a-radio-button value="quantity">按数量分摊</a-radio-button>
          <a-radio-button value="weight">按重量分摊</a-radio-button>
        </a-radio-group>
        <span style="margin-left: 24px; color: #8c8c8c; font-size: 13px;">
          分摊基数：
          <strong>{{ fmtQty(allocationBase) }}</strong>
          （{{ sharingMethod === 'amount' ? '金额' : sharingMethod === 'quantity' ? '数量' : '重量' }}）
        </span>
      </div>

      <!-- ═══ 4. 分摊预览 ═══ -->
      <div class="panel">
        <div class="panel-title">
          分摊预览
          <span class="panel-subtitle">
            费用合计 ¥{{ fmtAmount(expenseTotal) }}
            ｜ 已分摊 ¥{{ fmtAmount(totalAllocated) }}
            <a-tag v-if="Math.abs(expenseTotal - totalAllocated) < 0.01" color="green">分摊平衡</a-tag>
            <a-tag v-else color="orange">尾差 {{ fmtAmount(Math.abs(expenseTotal - totalAllocated)) }}</a-tag>
          </span>
        </div>
        <a-table
          :columns="previewColumns"
          :data-source="allocationPreview"
          :loading="previewLoading"
          row-key="rowKey"
          size="small"
          :pagination="false"
          :scroll="{ x: 1000 }"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'amount'">
              ¥{{ fmtAmount(record.amount) }}
            </template>
            <template v-else-if="column.dataIndex === 'baseValue'">
              {{ sharingMethod === 'amount' ? '¥' + fmtAmount(record.baseValue) : fmtQty(record.baseValue) }}
            </template>
            <template v-else-if="column.dataIndex === 'shareRatio'">
              {{ record.shareRatio }}%
            </template>
            <template v-else-if="column.dataIndex === 'sharedAmount'">
              <span :style="{ color: record.sharedAmount > 0 ? '#ff4d4f' : '#8c8c8c' }">
                ¥{{ fmtAmount(record.sharedAmount) }}
              </span>
            </template>
          </template>
          <template #summary>
            <a-table-summary-row>
              <a-table-summary-cell
                v-for="(col, idx) in previewColumns"
                :key="col.dataIndex"
                :index="idx"
              >
                <template v-if="idx === 0">
                  <strong>合计</strong>
                </template>
                <template v-else-if="col.dataIndex === 'quantity'">
                  <strong>{{ fmtQty(totalPreviewQty) }}</strong>
                </template>
                <template v-else-if="col.dataIndex === 'amount'">
                  <strong>¥{{ fmtAmount(totalPreviewAmount) }}</strong>
                </template>
                <template v-else-if="col.dataIndex === 'sharedAmount'">
                  <strong style="color: #ff4d4f;">¥{{ fmtAmount(totalAllocated) }}</strong>
                </template>
              </a-table-summary-cell>
            </a-table-summary-row>
          </template>
          <template #emptyText>
            <a-empty description="请先选择费用项和入库单，然后点击预览" />
          </template>
        </a-table>
      </div>

      <!-- ═══ 5. 操作按钮 ═══ -->
      <div class="panel btn-row">
        <a-space wrap>
          <a-button type="primary" ghost :loading="previewLoading" @click="handlePreview">
            预览分摊
          </a-button>
          <a-button
            type="primary"
            :loading="confirming"
            :disabled="allocationPreview.length === 0"
            @click="handleConfirm"
          >
            确认分摊
          </a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button @click="goBack">返回列表</a-button>
        </a-space>
      </div>

      <!-- ═══ 费用单选择弹窗 ═══ -->
      <a-modal
        v-model:open="showExpenseDocPicker"
        title="选择费用单"
        width="760px"
        :footer="null"
        destroy-on-close
      >
        <div style="margin-bottom: 12px;">
          <a-input-search
            v-model:value="expenseDocSearch"
            placeholder="搜索费用单号"
            allow-clear
            style="width: 220px"
            @search="fetchExpenseDocs"
          />
        </div>
        <a-table
          :columns="expenseDocColumns"
          :data-source="expenseDocList"
          :loading="expenseDocLoading"
          row-key="id"
          size="small"
          :pagination="{ pageSize: 5 }"
          :row-selection="expenseDocSelection"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'amount'">
              ¥{{ fmtAmount(record.amount) }}
            </template>
          </template>
        </a-table>
        <div style="text-align: right; margin-top: 12px;">
          <a-space>
            <a-button @click="showExpenseDocPicker = false">取消</a-button>
            <a-button type="primary" :disabled="selectedExpenseDocIds.length === 0" @click="confirmExpenseDocs">
              确认选择（{{ selectedExpenseDocIds.length }} 项）
            </a-button>
          </a-space>
        </div>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'
import optionsApi from '@/api/options'
import { inboundApi } from '@/api/erp'

defineOptions({ name: 'PurchaseCostSharingForm' })

const router = useRouter()
const route = useRoute()

// ═══ 后端就绪标识 ═══
const backendReady = true

// ═══ 静态选项 ═══
const expenseTypeOptions = [
  { label: '运费', value: '运费' },
  { label: '装卸费', value: '装卸费' },
  { label: '保险费', value: '保险费' },
  { label: '关税', value: '关税' },
  { label: '仓储费', value: '仓储费' },
  { label: '检验费', value: '检验费' },
  { label: '包装费', value: '包装费' },
  { label: '其他费用', value: '其他费用' },
]

// ═══ 费用项 ═══
interface ExpenseItem {
  rowKey: number
  expenseType: string
  amount: number
  source: 'manual' | 'doc'
  sourceDocId?: number
  sourceDocNo?: string
  remark: string
}
let expenseRowKeySeq = 0
const expenseItems = ref<ExpenseItem[]>([])
const expenseColumns = [
  { title: '费用类型', dataIndex: 'expenseType', width: 140 },
  { title: '金额', dataIndex: 'amount', width: 160, align: 'right' },
  { title: '来源', dataIndex: 'source', width: 100 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', dataIndex: 'action', width: 80 },
]
const expenseTotal = computed(() =>
  expenseItems.value.reduce((s, i) => s + (Number(i.amount) || 0), 0)
)

function addExpenseItem(doc?: { expenseType?: string; amount?: number; id?: number; expenseNo?: string }) {
  expenseItems.value.push({
    rowKey: ++expenseRowKeySeq,
    expenseType: doc?.expenseType || '运费',
    amount: doc?.amount || 0,
    source: doc ? 'doc' : 'manual',
    sourceDocId: doc?.id,
    sourceDocNo: doc?.expenseNo,
    remark: '',
  })
  recalcExpenseTotal()
}
function removeExpenseItem(index: number) {
  expenseItems.value.splice(index, 1)
  recalcExpenseTotal()
}
function recalcExpenseTotal() {
  // computed handles this; trigger reactivity
}

// ═══ 费用单选择（弹窗） ═══
const showExpenseDocPicker = ref(false)
const expenseDocSearch = ref('')
const expenseDocLoading = ref(false)
const expenseDocList = ref<any[]>([])
const selectedExpenseDocIds = ref<number[]>([])

const expenseDocSelection = {
  type: 'checkbox' as const,
  selectedRowKeys: selectedExpenseDocIds,
  onChange: (keys: any[]) => { selectedExpenseDocIds.value = keys },
}
const expenseDocColumns = [
  { title: '费用单号', dataIndex: 'expenseNo', width: 160 },
  { title: '费用类型', dataIndex: 'expenseType', width: 100 },
  { title: '金额', dataIndex: 'amount', width: 120, align: 'right' },
  { title: '费用日期', dataIndex: 'expenseDate', width: 110 },
  { title: '申请人', dataIndex: 'applicantName', width: 100 },
]

async function fetchExpenseDocs() {
  expenseDocLoading.value = true
  try {
    const res: any = await request.get('/finance/expense-doc/page', {
      pageNum: 1,
      pageSize: 50,
      keyword: expenseDocSearch.value || undefined,
      status: 2, // 已审批的费用单
    })
    expenseDocList.value = res?.records || res?.data?.records || []
  } catch {
    expenseDocList.value = []
  } finally {
    expenseDocLoading.value = false
  }
}

function confirmExpenseDocs() {
  const selected = expenseDocList.value.filter(
    (d: any) => selectedExpenseDocIds.value.includes(d.id)
  )
  for (const doc of selected) {
    addExpenseItem({
      expenseType: doc.expenseType,
      amount: doc.amount,
      id: doc.id,
      expenseNo: doc.expenseNo,
    })
  }
  showExpenseDocPicker.value = false
  selectedExpenseDocIds.value = []
}

// ═══ 供应商选项 ═══
const supplierOptions = ref<{ label: string; value: number }[]>([])
const supplierLoading = ref(false)

async function loadSuppliers() {
  supplierLoading.value = true
  try {
    const list = await optionsApi.getSuppliers()
    supplierOptions.value = (list || []).map((s: any) => ({ label: s.name || s.supplierName || '', value: s.id }))
  } catch {
    supplierOptions.value = []
  } finally {
    supplierLoading.value = false
  }
}

// ═══ 采购入库单（分摊目标） ═══
const inboundLoading = ref(false)
const inboundOptions = ref<any[]>([])
const selectedInboundIds = ref<number[]>([])
const inboundDateRange = ref<[Dayjs, Dayjs] | null>(null)

const inboundFilter = reactive({
  supplierId: undefined as number | undefined,
  startDate: '',
  endDate: '',
})

const inboundSelection = {
  type: 'checkbox' as const,
  selectedRowKeys: selectedInboundIds,
  onChange: (keys: any[]) => { selectedInboundIds.value = keys },
}

const inboundColumns = [
  { title: '入库单号', dataIndex: 'inboundNo', width: 160 },
  { title: '供应商', dataIndex: 'supplierName', width: 140 },
  { title: '入库日期', dataIndex: 'inboundDate', width: 110 },
  { title: '金额', dataIndex: 'totalAmount', width: 120, align: 'right' },
  { title: '数量', dataIndex: 'totalQuantity', width: 100, align: 'right' },
  { title: '重量', dataIndex: 'totalWeight', width: 100, align: 'right' },
  { title: '状态', dataIndex: 'statusName', width: 90 },
]

function handleInboundDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates?.length === 2) {
    inboundFilter.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    inboundFilter.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    inboundFilter.startDate = ''
    inboundFilter.endDate = ''
  }
}

async function fetchInbounds() {
  inboundLoading.value = true
  try {
    const res: any = await inboundApi.page({
      pageNum: 1,
      pageSize: 100,
      supplierId: inboundFilter.supplierId || undefined,
      startDate: inboundFilter.startDate || undefined,
      endDate: inboundFilter.endDate || undefined,
    })
    inboundOptions.value = res?.records || res?.data?.records || []
  } catch {
    inboundOptions.value = []
  } finally {
    inboundLoading.value = false
  }
}

// ═══ 分摊方式 ═══
const sharingMethod = ref<'amount' | 'quantity' | 'weight'>('amount')

const selectedInbounds = computed(() =>
  inboundOptions.value.filter((b: any) => selectedInboundIds.value.includes(b.id))
)

const allocationBase = computed(() => {
  const bases = selectedInbounds.value.map((b: any) => {
    if (sharingMethod.value === 'amount') return Number(b.totalAmount) || 0
    if (sharingMethod.value === 'quantity') return Number(b.totalQuantity) || 0
    return Number(b.totalWeight) || 0
  })
  return bases.reduce((s, v) => s + v, 0)
})

// ═══ 分摊预览 ═══
interface AllocationRow {
  rowKey: string
  inboundId: number
  inboundNo: string
  productCode?: string
  productName?: string
  productSpec?: string
  productUnit?: string
  quantity: number
  amount: number
  baseValue: number
  shareRatio: string
  sharedAmount: number
}

const previewLoading = ref(false)
const allocationPreview = ref<AllocationRow[]>([])

const previewColumns = [
  { title: '入库单号', dataIndex: 'inboundNo', width: 140 },
  { title: '商品名称', dataIndex: 'productName', width: 160 },
  { title: '规格', dataIndex: 'productSpec', width: 90 },
  { title: '单位', dataIndex: 'productUnit', width: 70 },
  { title: '数量', dataIndex: 'quantity', width: 90, align: 'right' },
  { title: '金额', dataIndex: 'amount', width: 110, align: 'right' },
  { title: '分摊基数', dataIndex: 'baseValue', width: 110, align: 'right' },
  { title: '分摊比例', dataIndex: 'shareRatio', width: 90, align: 'right' },
  { title: '分摊金额', dataIndex: 'sharedAmount', width: 130, align: 'right' },
]

const totalAllocated = computed(() =>
  allocationPreview.value.reduce((s, r) => s + r.sharedAmount, 0)
)
const totalPreviewQty = computed(() =>
  allocationPreview.value.reduce((s, r) => s + r.quantity, 0)
)
const totalPreviewAmount = computed(() =>
  allocationPreview.value.reduce((s, r) => s + r.amount, 0)
)

async function handlePreview() {
  if (expenseItems.value.length === 0) {
    message.warning('请至少添加一项费用')
    return
  }
  if (selectedInboundIds.value.length === 0) {
    message.warning('请至少选择一张入库单')
    return
  }

  previewLoading.value = true
  try {
    // 加载选中入库单的明细
    const allRows: AllocationRow[] = []
    let rowKeySeq = 0
    const expense = expenseTotal.value

    for (const inboundId of selectedInboundIds.value) {
      const inbound = inboundOptions.value.find((b: any) => b.id === inboundId)
      let items: any[] = []
      try {
        const res: any = await inboundApi.getItems(inboundId)
        items = Array.isArray(res) ? res : (res?.data || [])
      } catch {
        items = []
      }

      for (const item of items) {
        const qty = Number((item.orderQuantity ?? item.inboundQuantity ?? item.quantity) || 0)
        const price = Number(item.unitPrice) || 0
        const lineAmount = qty * price
        let baseValue = 0
        if (sharingMethod.value === 'amount') baseValue = lineAmount
        else if (sharingMethod.value === 'quantity') baseValue = qty
        else baseValue = Number(item.weight || item.totalWeight || 0) * qty

        allRows.push({
          rowKey: `preview-${++rowKeySeq}`,
          inboundId,
          inboundNo: inbound?.inboundNo || '',
          productCode: item.productCode,
          productName: item.productName,
          productSpec: item.productSpec,
          productUnit: item.productUnit,
          quantity: qty,
          amount: lineAmount,
          baseValue,
          shareRatio: '0.00',
          sharedAmount: 0,
        })
      }
    }

    // 计算分摊
    const totalBase = allRows.reduce((s, r) => s + r.baseValue, 0)
    if (totalBase > 0 && expense > 0) {
      for (const row of allRows) {
        const ratio = row.baseValue / totalBase
        row.shareRatio = (ratio * 100).toFixed(4)
        row.sharedAmount = Math.round(expense * ratio * 100) / 100
      }
      // 尾差调整
      const allocated = allRows.slice(0, -1).reduce((s, r) => s + r.sharedAmount, 0)
      if (allRows.length > 0) {
        allRows[allRows.length - 1].sharedAmount = Math.round((expense - allocated) * 100) / 100
      }
    }

    allocationPreview.value = allRows
    message.success(`分摊预览完成，共 ${allRows.length} 条明细`)
  } catch (e: any) {
    message.error('分摊计算失败')
    console.error('[分摊预览]', e)
  } finally {
    previewLoading.value = false
  }
}

// ═══ 确认分摊 ═══
const confirming = ref(false)

async function handleConfirm() {
  if (allocationPreview.value.length === 0) {
    message.warning('请先执行预览分摊')
    return
  }

  Modal.confirm({
    title: '确认分摊',
    content: `确认将费用 ¥${fmtAmount(expenseTotal)} 分摊至 ${selectedInboundIds.value.length} 张入库单（共 ${allocationPreview.value.length} 条明细）？分摊后将更新库存成本。`,
    okText: '确认分摊',
    cancelText: '取消',
    onOk: async () => {
      confirming.value = true
      try {
        const payload = {
          expenseItems: expenseItems.value.map(i => ({
            expenseType: i.expenseType,
            amount: i.amount,
            source: i.source,
            sourceDocId: i.sourceDocId,
            remark: i.remark,
          })),
          inboundIds: selectedInboundIds.value,
          sharingMethod: sharingMethod.value,
          details: allocationPreview.value.map(r => ({
            inboundId: r.inboundId,
            inboundNo: r.inboundNo,
            productName: r.productName,
            productCode: r.productCode,
            quantity: r.quantity,
            amount: r.amount,
            baseValue: r.baseValue,
            shareRatio: r.shareRatio,
            sharedAmount: r.sharedAmount,
          })),
        }

        await request.post("/erp/purchase/cost-sharing", payload)
        message.success("分摊成功")
        router.push("/erp/purchase/cost-sharing")
      } catch (e: any) {
        message.error(e?.data?.message || '分摊失败')
      } finally {
        confirming.value = false
      }
    },
  })
}

// ═══ 重置 ═══
function handleReset() {
  expenseItems.value = []
  selectedInboundIds.value = []
  allocationPreview.value = []
  sharingMethod.value = 'amount'
  inboundFilter.supplierId = undefined
  inboundFilter.startDate = ''
  inboundFilter.endDate = ''
  inboundDateRange.value = null
  message.success('已重置')
}

// ═══ 工具函数 ═══
function filterOption(input: string, option: any) {
  return String(option?.label || '').toLowerCase().includes(input.toLowerCase())
}

function fmtAmount(v: any): string {
  return (Number(v) || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function fmtQty(v: any): string {
  return (Number(v) || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function handleError(e: Error) {
  console.error('[采购费用分摊]', e)
}
function goBack() {
  router.push('/erp/purchase/cost-sharing')
}

// ═══ 初始化 ═══
onMounted(async () => {
  await loadSuppliers()
  await fetchInbounds()

  // 编辑模式：通过 query.id 加载已有分摊单（等待后端实现）
  const editId = route.query.id ? Number(route.query.id) : undefined
  if (editId) {
    try {
      const res: any = await request.get(`/erp/purchase/cost-sharing/${editId}`)
      if (res) {
        // NOTE: 后端就绪后实现加载逻辑
      }
    } catch {
      // ignore
    }
  }
})
</script>

<style scoped>
.panel {
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.panel-subtitle {
  font-size: 13px;
  font-weight: 400;
  color: #8c8c8c;
}
.btn-row {
  display: flex;
  justify-content: flex-start;
}
</style>
