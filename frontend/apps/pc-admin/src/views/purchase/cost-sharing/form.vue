<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 非草稿状态锁定提示 -->
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已记账，内容不可修改`"
        type="warning"
        show-icon
        banner
        style="flex-shrink:0"
      />

      <BillFormPage
        v-model="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :summary="summaryConfig"
        :footer="footerConfig"
        @action="handleAction"
        @field-change="handleFieldChange"
        @draft="handleSaveDraft"
        @submit="handleSubmit"
      >
        <!-- ═══ 明细表格区：费用单明细 + 采购入库单分摊明细 ═══ -->
        <template #detail-table>
          <div class="detail-tables">
            <!-- ① 费用单明细表 -->
            <div class="table-block">
              <div class="block-header">
                <span class="block-title">费用单明细</span>
                <span class="block-subtitle">已选 {{ expenseItems.length }} 项，合计 ¥{{ fmtAmount(expenseTotal) }}</span>
                <a-space>
                  <a-button type="primary" ghost size="small" :disabled="isLocked" @click="showExpensePicker = true">
                    选费用单
                  </a-button>
                </a-space>
              </div>
              <BillDetailTable
                :columns="expenseCols"
                v-model:data-source="expenseItems"
                :max-height="220"
                :min-rows="4"
                :storage-key="'cost-sharing-expense-items-col'"
                :summary-columns="expenseSummary"
                @cell-change="handleExpenseCellChange"
              >
                <template #expenseActionCell="{ record, index, empty }">
                  <a-button
                    v-if="!empty"
                    type="link"
                    size="small"
                    danger
                    :disabled="isLocked"
                    @click="removeExpenseItem(index)"
                  >
                    删除
                  </a-button>
                </template>
              </BillDetailTable>
            </div>

            <!-- ② 采购入库单分摊明细表 -->
            <div class="table-block">
              <div class="block-header">
                <span class="block-title">采购入库单分摊明细</span>
                <a-space>
                  <a-select
                    v-model:value="formData.allocationMethod"
                    size="small"
                    style="width: 130px"
                    :disabled="isLocked"
                    :options="allocationMethodOptions"
                  />
                  <a-button size="small" :disabled="isLocked || items.length === 0" @click="doAllocate">自动分摊</a-button>
                  <a-button type="primary" ghost size="small" :disabled="isLocked" @click="showInboundPicker = true">
                    选采购入库单
                  </a-button>
                </a-space>
              </div>
              <BillDetailTable
                :columns="itemCols"
                v-model:data-source="items"
                :max-height="240"
                :min-rows="6"
                :storage-key="'cost-sharing-inbound-items-col'"
                :summary-columns="itemSummary"
                @cell-change="handleItemCellChange"
              >
                <template #itemActionCell="{ record, index, empty }">
                  <a-button
                    v-if="!empty"
                    type="link"
                    size="small"
                    danger
                    :disabled="isLocked"
                    @click="removeItem(index)"
                  >
                    删除
                  </a-button>
                </template>
              </BillDetailTable>
            </div>
          </div>
        </template>

        <!-- ═══ 底部备注 + 单据信息 ═══ -->
        <template #bottom-extra>
          <div class="doc-info-row">
            <span class="doc-info-item">
              单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">
              制单人 <a-tag color="blue">{{ formData.createByName || currentUserName || '系统' }}</a-tag>
            </span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span v-if="formData.accountTime" class="doc-info-item">记账时间 {{ formData.accountTime }}</span>
            <span v-if="formData.bookkeeperName" class="doc-info-item">
              记账人 <a-tag>{{ formData.bookkeeperName }}</a-tag>
            </span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 选费用单弹窗 ═══ -->
      <a-modal
        v-model:open="showExpensePicker"
        title="选择费用单"
        :width="860"
        :footer="null"
        destroy-on-close
      >
        <div class="picker-search">
          <a-input
            v-model:value="expenseSearch"
            placeholder="搜索费用单编号/申请人"
            allow-clear
            style="width: 280px"
            @press-enter="fetchExpenseDocs"
          />
          <a-button type="primary" @click="fetchExpenseDocs">查询</a-button>
        </div>
        <a-table
          :columns="expensePickerCols"
          :data-source="expenseDocList"
          :loading="expenseDocLoading"
          :pagination="{ pageSize: 8 }"
          row-key="id"
          size="small"
          :row-selection="expenseSelection"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'expenseAmount'">¥{{ fmtAmount(record.totalAmount) }}</template>
          </template>
        </a-table>
        <div class="picker-footer">
          <a-space>
            <a-button @click="showExpensePicker = false">取消</a-button>
            <a-button type="primary" :disabled="selectedExpenseIds.length === 0" @click="confirmExpenseDocs">
              确认选择（{{ selectedExpenseIds.length }} 项）
            </a-button>
          </a-space>
        </div>
      </a-modal>

      <!-- ═══ 选采购入库单弹窗 ═══ -->
      <a-modal
        v-model:open="showInboundPicker"
        title="选择采购入库单"
        :width="860"
        :footer="null"
        destroy-on-close
      >
        <div class="picker-search">
          <a-input
            v-model:value="inboundSearch"
            placeholder="搜索入库单编号/供应商"
            allow-clear
            style="width: 280px"
            @press-enter="fetchInbounds"
          />
          <a-button type="primary" @click="fetchInbounds">查询</a-button>
        </div>
        <a-table
          :columns="inboundPickerCols"
          :data-source="inboundList"
          :loading="inboundLoading"
          :pagination="{ pageSize: 8 }"
          row-key="id"
          size="small"
          :row-selection="inboundSelection"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'totalAmount'">¥{{ fmtAmount(record.totalAmount) }}</template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 9 ? 'green' : 'blue'">{{ inboundStatusText(record.status) }}</a-tag>
            </template>
          </template>
        </a-table>
        <div class="picker-footer">
          <a-space>
            <a-button @click="showInboundPicker = false">取消</a-button>
            <a-button type="primary" :disabled="selectedInboundIds.length === 0" @click="confirmInbounds">
              确认选择（{{ selectedInboundIds.length }} 单）
            </a-button>
          </a-space>
        </div>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { costSharingApi, inboundApi } from '@/api/erp'
import optionsApi from '@/api/options'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'PurchaseCostSharingForm' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore.userInfo?.username || userStore.userInfo?.nickname || '')

// ═══ 表单主数据 ═══
const formData = reactive<any>({
  id: undefined,
  sharingNo: '',
  handlerId: undefined,
  handlerName: '',
  departmentId: undefined,
  departmentName: '',
  sharingDate: dayjs().format('YYYY-MM-DD'),
  allocationMethod: 'amount',
  remark: '',
  totalAmount: 0,
  createByName: '',
  createTime: '',
  status: 0,
  accountTime: '',
  bookkeeperName: '',
})

// ═══ 两个明细表数据 ═══
const expenseItems = ref<any[]>([])
const items = ref<any[]>([])

const allocationMethodOptions = [
  { label: '按金额分摊', value: 'amount' },
  { label: '按数量分摊', value: 'quantity' },
  { label: '按重量分摊', value: 'weight' },
]

// ═══ 下拉选项 ═══
const optionRefs = reactive<any>({
  users: [],
  departments: [],
  suppliers: [],
  products: [],
})
const loadingOptions = ref(false)

async function loadOptions() {
  loadingOptions.value = true
  try {
    const [users, departments] = await Promise.all([
      optionsApi.getUsers(),
      optionsApi.getDepartments(),
    ])
    optionRefs.users = users || []
    optionRefs.departments = departments || []
  } catch { /* 静默 */ } finally { loadingOptions.value = false }
}

// ═══ 常量/复用 ═══
const isLocked = computed(() => Number(formData.status) !== 0)
const statusText = computed(() => [{ t: '草稿' }, { t: '已完成' }, { t: '已取消' }][Number(formData.status)]?.t || '草稿')
const statusColor = computed(() => (Number(formData.status) === 1 ? 'green' : Number(formData.status) === 2 ? 'red' : 'default'))
const saving = ref(false)

function fmtAmount(v: any) {
  return (Number(v) || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function fmtQty(v: any) {
  return (Number(v) || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}
function formatNow() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss')
}

// ═══ Header / Footer / Summary ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '采购费用分摊',
  orderNo: formData.sharingNo,
  showAttachment: true,
  actions: [
    { key: 'history', label: '历史', icon: undefined },
    { key: 'export', label: '导出', icon: undefined },
  ],
}))

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '合计金额', value: fmtAmount(expenseTotal.value), divider: true },
  { label: '本单金额', value: fmtAmount(formData.totalAmount) },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${fmtAmount(formData.totalAmount)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '记账',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 基本信息字段 ═══
const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'sharingNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true, placeholder: '保存后自动生成' },
  {
    key: 'handlerId', label: '经手人', type: 'select', inlineLabel: true, width: 210,
    options: optionRefs.users.map((u: any) => ({ label: u.name || u.username, value: u.id })),
  },
  {
    key: 'departmentId', label: '部门', type: 'select', inlineLabel: true, width: 210,
    options: optionRefs.departments.map((d: any) => ({ label: d.name, value: d.id })),
  },
  { key: 'sharingDate', label: '单据日期', type: 'date', format: 'YYYY-MM-DD', inlineLabel: true, width: 210 },
  {
    key: 'allocationMethod', label: '分摊方式', type: 'select', inlineLabel: true, width: 210,
    options: allocationMethodOptions,
  },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 300 },
])

function handleFieldChange(fieldKey: string, val: any) {
  if (fieldKey === 'handlerId') {
    const u = optionRefs.users.find((x: any) => x.id === val)
    formData.handlerName = u?.name || u?.username || ''
  } else if (fieldKey === 'departmentId') {
    const d = optionRefs.departments.find((x: any) => x.id === val)
    formData.departmentName = d?.name || ''
  }
}

// ═══ 合计金额 ═══
const expenseTotal = computed(() => expenseItems.value.reduce((s, i) => s + (Number(i.expenseAmount) || 0), 0))
// 合计金额变化时同步本单金额
watch(expenseTotal, (v) => { formData.totalAmount = v })

// ═══ 费用单明细列 ═══
const expenseCols: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'expenseNo', title: '费用单编号', width: 160, type: 'input', readonly: true },
  { key: 'partnerName', title: '往来单位', width: 140, type: 'input', readonly: true },
  { key: 'partnerCode', title: '往来单位编码', width: 110, type: 'input', readonly: true },
  { key: 'settleUnitId', title: '结算单位编号', width: 110, type: 'input', readonly: true },
  { key: 'settleUnit', title: '结算单位', width: 140, type: 'input', readonly: true },
  { key: 'expenseType', title: '费用项', width: 120, type: 'input', readonly: true },
  { key: 'expenseAmount', title: '费用金额', width: 110, type: 'number', readonly: true, precision: 2, align: 'right' },
  { key: 'remark', title: '备注', width: 160, type: 'input', readonly: true },
  { key: 'action', title: '操作', width: 70, type: 'action', fixed: 'right', slotName: 'expenseActionCell' },
]
const expenseSummary = computed(() => [
  { key: 'expenseAmount', value: Number(expenseTotal.value) || 0, highlight: true },
])

function handleExpenseCellChange() { /* 只读，无需处理 */ }
function removeExpenseItem(index: number) {
  expenseItems.value.splice(index, 1)
}

// ═══ 采购入库单明细列 ═══
const itemCols: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'inboundNo', title: '单据编号', width: 150, type: 'input', readonly: true },
  { key: 'supplierName', title: '供应商', width: 150, type: 'input', readonly: true },
  { key: 'supplierCode', title: '供应商编码', width: 100, type: 'input', readonly: true },
  { key: 'settleUnitId', title: '结算单位编号', width: 110, type: 'input', readonly: true },
  { key: 'settleUnit', title: '结算单位', width: 130, type: 'input', readonly: true },
  { key: 'productName', title: '商品名称', width: 180, type: 'input', readonly: true },
  { key: 'pricingUnit', title: '计价单位', width: 90, type: 'input', readonly: true },
  { key: 'quantity', title: '数量', width: 90, type: 'number', readonly: true, precision: 2, align: 'right' },
  { key: 'discountedUnitPrice', title: '优惠后单价', width: 100, type: 'number', readonly: true, precision: 2, align: 'right' },
  { key: 'discountedAmount', title: '优惠后金额', width: 110, type: 'number', readonly: true, precision: 2, align: 'right' },
  { key: 'allocatedCost', title: '分摊金额', width: 110, type: 'number', readonly: true, precision: 2, align: 'right' },
  { key: 'action', title: '操作', width: 70, type: 'action', fixed: 'right', slotName: 'itemActionCell' },
]
const allocatedTotal = computed(() => items.value.reduce((s, i) => s + (Number(i.allocatedCost) || 0), 0))
const itemSummary = computed(() => [
  { key: 'quantity', value: Number(items.value.reduce((s, i) => s + (Number(i.quantity) || 0), 0)) || 0 },
  { key: 'discountedAmount', value: Number(items.value.reduce((s, i) => s + (Number(i.discountedAmount) || 0), 0)) || 0 },
  { key: 'allocatedCost', value: Number(allocatedTotal.value) || 0, highlight: true },
])

function handleItemCellChange() { /* 只读，无需处理 */ }
function removeItem(index: number) {
  items.value.splice(index, 1)
}

// ═══ 选费用单 ═══
const showExpensePicker = ref(false)
const expenseSearch = ref('')
const expenseDocList = ref<any[]>([])
const expenseDocLoading = ref(false)
const selectedExpenseIds = ref<any[]>([])

const expenseSelection = computed(() => ({
  type: 'checkbox' as const,
  selectedRowKeys: selectedExpenseIds.value,
  onChange: (keys: any[]) => { selectedExpenseIds.value = keys },
}))

const expensePickerCols = [
  { title: '费用单编号', dataIndex: 'applicationCode', key: 'applicationCode', width: 160 },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 120 },
  { title: '结算单位', dataIndex: 'departmentName', key: 'departmentName', width: 130 },
  { title: '费用项', dataIndex: 'expenseTypeDesc', key: 'expenseTypeDesc', width: 130 },
  { title: '费用金额', dataIndex: 'totalAmount', key: 'expenseAmount', width: 120, align: 'right' },
  { title: '申请日期', dataIndex: 'applyDate', key: 'applyDate', width: 110 },
  { title: '备注', dataIndex: 'purpose', key: 'purpose', ellipsis: true },
]

async function fetchExpenseDocs() {
  expenseDocLoading.value = true
  try {
    const res: any = await request.get('/erp/expense/application/page', {
      pageNum: 1,
      pageSize: 50,
      keyword: expenseSearch.value || undefined,
    })
    const records = res?.records || res?.data?.records || res?.data?.list || []
    // 仅已审批/已报销的费用单可作为分摊来源
    expenseDocList.value = records.filter((d: any) => {
      const st = d.statusDesc || d.status || ''
      return st !== '待审批' && st !== '已拒绝' && st !== '草稿'
    })
  } catch { expenseDocList.value = [] } finally { expenseDocLoading.value = false }
}

function confirmExpenseDocs() {
  const selected = expenseDocList.value.filter((d: any) => selectedExpenseIds.value.includes(d.id))
  for (const doc of selected) {
    if (expenseItems.value.some((i) => i.expenseNo === doc.applicationCode)) continue
    expenseItems.value.push({
      expenseNo: doc.applicationCode,
      partnerId: doc.applicantId,
      partnerName: doc.applicantName,
      partnerCode: doc.applicantId,
      settleUnitId: doc.departmentId,
      settleUnit: doc.departmentName,
      expenseType: doc.expenseTypeDesc,
      expenseAmount: Number(doc.totalAmount) || 0,
      remark: doc.purpose || '',
    })
  }
  showExpensePicker.value = false
  selectedExpenseIds.value = []
}

// ═══ 选采购入库单 ═══
const showInboundPicker = ref(false)
const inboundSearch = ref('')
const inboundList = ref<any[]>([])
const inboundLoading = ref(false)
const selectedInboundIds = ref<any[]>([])

const inboundSelection = computed(() => ({
  type: 'checkbox' as const,
  selectedRowKeys: selectedInboundIds.value,
  onChange: (keys: any[]) => { selectedInboundIds.value = keys },
}))

const inboundPickerCols = [
  { title: '入库单号', dataIndex: 'inboundNo', key: 'inboundNo', width: 160 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 160 },
  { title: '入库日期', dataIndex: 'inboundDate', key: 'inboundDate', width: 110 },
  { title: '金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
]

async function fetchInbounds() {
  inboundLoading.value = true
  try {
    const res: any = await inboundApi.page({
      pageNum: 1,
      pageSize: 50,
      keyword: inboundSearch.value || undefined,
    })
    inboundList.value = res?.records || res?.data?.records || []
  } catch { inboundList.value = [] } finally { inboundLoading.value = false }
}

async function confirmInbounds() {
  const selected = inboundList.value.filter((d: any) => selectedInboundIds.value.includes(d.id))
  for (const doc of selected) {
    // 展开入库单明细行
    let detail: any[] = []
    try {
      const res: any = await inboundApi.getItems(doc.id)
      detail = Array.isArray(res) ? res : (res?.data || [])
    } catch { detail = [] }
    if (!detail.length) {
      message.warning(`入库单 ${doc.inboundNo} 无明细行，已跳过`)
      continue
    }
    for (const line of detail) {
      const qty = Number(line.inboundQuantity ?? line.orderQuantity ?? line.quantity) || 0
      const price = Number(line.unitPrice) || 0
      const lineAmount = Number(line.lineAmount ?? line.lineTotal) || qty * price
      items.value.push({
        inboundOrderId: doc.id,
        inboundNo: doc.inboundNo,
        supplierId: doc.supplierId,
        supplierName: doc.supplierName || '',
        supplierCode: doc.supplierCode || '',
        settleUnitId: doc.departmentId,
        settleUnit: doc.departmentName || '',
        productId: line.productId,
        productName: line.productName || '',
        pricingUnit: line.productUnit || '',
        quantity: qty,
        discountedUnitPrice: price,
        discountedAmount: lineAmount,
        allocatedCost: 0,
      })
    }
  }
  showInboundPicker.value = false
  selectedInboundIds.value = []
}

// ═══ 自动分摊 ═══
function doAllocate() {
  const total = expenseTotal.value
  if (total <= 0) { message.warning('请先选择费用单'); return }
  if (!items.value.length) { message.warning('请先选择采购入库单'); return }
  const method = formData.allocationMethod
  const base = items.value.map((i: any) => {
    if (method === 'amount') return Number(i.discountedAmount) || 0
    if (method === 'quantity') return Number(i.quantity) || 0
    return Number(i.weight) || Number(i.quantity) || 0
  })
  const totalBase = base.reduce((s, v) => s + v, 0)
  if (totalBase <= 0) { message.warning('分摊基数为0，无法分摊'); return }
  let allocated = 0
  items.value.forEach((i: any, idx: number) => {
    if (idx === items.value.length - 1) {
      i.allocatedCost = Math.round((total - allocated) * 100) / 100
    } else {
      const share = Math.round((total * base[idx] / totalBase) * 100) / 100
      i.allocatedCost = share
      allocated += share
    }
  })
  message.success('分摊完成')
}

// ═══ 保存草稿 / 记账 ═══
async function buildPayload() {
  if (!formData.handlerId) { message.warning('请选择经手人'); throw new Error('no-handler') }
  if (!expenseItems.value.length) { message.warning('请至少添加一项费用单明细'); throw new Error('no-expense') }
  if (!items.value.length) { message.warning('请至少添加一条采购入库单分摊明细'); throw new Error('no-item') }
  // 兜底：经手人/部门名称
  let handlerName = formData.handlerName
  if (!handlerName) {
    const u = optionRefs.users.find((x: any) => x.id === formData.handlerId)
    handlerName = u?.name || u?.username || ''
  }
  let departmentName = formData.departmentName
  if (!departmentName) {
    const d = optionRefs.departments.find((x: any) => x.id === formData.departmentId)
    departmentName = d?.name || ''
  }
  return {
    handlerId: formData.handlerId,
    handlerName,
    departmentId: formData.departmentId,
    departmentName,
    sharingDate: formData.sharingDate,
    sharingMethod: formData.allocationMethod,
    remark: formData.remark,
    createByName: formData.createByName || currentUserName.value,
    expenseItems: expenseItems.value.map((i) => ({
      expenseNo: i.expenseNo,
      partnerId: i.partnerId,
      partnerName: i.partnerName,
      partnerCode: i.partnerCode,
      settleUnitId: i.settleUnitId,
      settleUnit: i.settleUnit,
      expenseType: i.expenseType,
      expenseAmount: i.expenseAmount,
      remark: i.remark,
    })),
    details: items.value.map((i) => ({
      inboundId: i.inboundOrderId,
      inboundNo: i.inboundNo,
      supplierId: i.supplierId,
      supplierName: i.supplierName,
      supplierCode: i.supplierCode,
      settleUnitId: i.settleUnitId,
      settleUnit: i.settleUnit,
      productId: i.productId,
      productName: i.productName,
      pricingUnit: i.pricingUnit,
      quantity: i.quantity,
      discountedUnitPrice: i.discountedUnitPrice,
      discountedAmount: i.discountedAmount,
      allocatedCost: i.allocatedCost,
    })),
  }
}

async function saveBill(): Promise<number> {
  const payload = await buildPayload()
  if (formData.id) {
    // 编辑：更新
    await request.put(`/erp/purchase/cost-sharing/${formData.id}`, payload)
    return formData.id
  }
  const res: any = await costSharingApi.create(payload)
  return Number(res?.data ?? res) || formData.id
}

async function handleSaveDraft() {
  if (saving.value) return
  saving.value = true
  try {
    const id = await saveBill()
    formData.id = id
    message.success('草稿已保存')
    if (!formData.sharingNo) {
      // 回填编号
      try { const no: any = await costSharingApi.nextNo(); formData.sharingNo = no } catch { /* 静默 */ }
    }
  } catch (e: any) {
    if (!['no-expense', 'no-item', 'no-handler'].includes(e?.message)) message.error('保存失败')
  } finally { saving.value = false }
}

async function handleSubmit() {
  if (saving.value) return
  Modal.confirm({
    title: '记账确认',
    content: `确认记账？将把合计 ¥${fmtAmount(expenseTotal.value)} 分摊至 ${items.value.length} 条入库单明细，回写采购成本。`,
    okText: '记账',
    cancelText: '取消',
    onOk: async () => {
      saving.value = true
      try {
        const id = await saveBill()
        await costSharingApi.complete(id)
        message.success('已记账')
        router.push('/purchase/cost-sharing')
      } catch (e: any) {
        if (!['no-expense', 'no-item', 'no-handler'].includes(e?.message)) message.error('记账失败')
      } finally { saving.value = false }
    },
  })
}

// ═══ 头部操作 ═══
function handleAction(actionKey: string) {
  if (actionKey === 'history') router.push('/purchase/cost-sharing')
  if (actionKey === 'export') {
    // 导出当前明细为 CSV
    exportDetailCsv()
  }
}

function exportDetailCsv() {
  const header = ['费用单编号', '往来单位', '费用项', '费用金额', '入库单号', '商品名称', '数量', '优惠后单价', '优惠后金额', '分摊金额']
  const rows: any[][] = []
  const max = Math.max(expenseItems.value.length, items.value.length)
  for (let i = 0; i < max; i++) {
    const e = expenseItems.value[i] || {}
    const it = items.value[i] || {}
    rows.push([e.expenseNo, e.partnerName, e.expenseType, e.expenseAmount, it.inboundNo, it.productName, it.quantity, it.discountedUnitPrice, it.discountedAmount, it.allocatedCost])
  }
  const csv = [header, ...rows].map(r => r.map(v => `"${v ?? ''}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = `采购费用分摊_${formData.sharingNo || 'new'}.csv`; a.click()
  URL.revokeObjectURL(url)
}

// ═══ 键盘快捷键 ═══
function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); handleSaveDraft() }
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); handleSubmit() }
}

// ═══ 状态辅助 ═══
function inboundStatusText(status: number) {
  return ['草稿', '待审批', '已审批', '待收货', '已收货', '待质检', '已质检', '待入库', '已入库', '已完成', '已取消'][status] || '未知'
}

function handleError(err: any) { console.warn('[采购费用分摊] ErrorBoundary:', err) }
function goBack() { router.push('/purchase/cost-sharing') }

// ═══ 初始化 ═══
onMounted(async () => {
  await loadOptions()
  const editId = route.query.id ? Number(route.query.id) : undefined
  if (editId) {
    try {
      const res: any = await costSharingApi.getDetail(editId)
      const data = res?.data || res
      const sharing = data?.sharing || {}
      Object.assign(formData, {
        id: sharing.id,
        sharingNo: sharing.sharingNo,
        handlerId: sharing.handlerId,
        handlerName: sharing.handlerName,
        departmentId: sharing.departmentId,
        departmentName: sharing.departmentName,
        sharingDate: sharing.sharingDate || dayjs().format('YYYY-MM-DD'),
        allocationMethod: sharing.allocationMethod || 'amount',
        remark: sharing.remark,
        createByName: sharing.createByName,
        createTime: sharing.createTime,
        status: sharing.status,
        totalAmount: Number(sharing.totalAmount) || 0,
        accountTime: sharing.accountTime,
        bookkeeperName: sharing.bookkeeperName,
      })
      expenseItems.value = (data?.expenseItems || []).map((i: any) => ({
        expenseNo: i.expenseNo, partnerId: i.partnerId, partnerName: i.partnerName,
        partnerCode: i.partnerCode, settleUnitId: i.settleUnitId, settleUnit: i.settleUnit,
        expenseType: i.expenseType, expenseAmount: Number(i.expenseAmount) || 0, remark: i.remark,
      }))
      items.value = (data?.items || []).map((i: any) => ({
        inboundOrderId: i.inboundOrderId, inboundNo: i.inboundNo,
        supplierId: i.supplierId, supplierName: i.supplierName, supplierCode: i.supplierCode,
        settleUnitId: i.settleUnitId, settleUnit: i.settleUnit,
        productId: i.productId, productName: i.productName, pricingUnit: i.pricingUnit,
        quantity: Number(i.quantity) || 0, discountedUnitPrice: Number(i.discountedUnitPrice) || 0,
        discountedAmount: Number(i.discountedAmount) || 0, allocatedCost: Number(i.allocatedCost) || 0,
      }))
    } catch { message.error('加载分摊单失败') }
  } else {
    // 新建：获取编号
    try { const no: any = await costSharingApi.nextNo(); formData.sharingNo = no } catch { /* 静默 */ }
    formData.createByName = currentUserName.value
    formData.createTime = formatNow()
  }
  nextTick(() => {
    window.addEventListener('keydown', handleKeydown)
  })
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.detail-tables {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0 16px 8px;
}
.table-block {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
}
.block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
}
.block-title { font-size: 14px; font-weight: 600; }
.block-subtitle { font-size: 12px; color: #8c8c8c; margin-right: auto; }
.picker-search {
  display: flex; gap: 8px; align-items: center; margin-bottom: 12px;
}
.picker-footer {
  display: flex; justify-content: flex-end; margin-top: 12px;
}
.doc-info-row {
  display: flex; flex-wrap: wrap; gap: 16px; align-items: center;
  padding: 12px 0; font-size: 13px; color: #595959;
}
.doc-info-item { display: inline-flex; align-items: center; gap: 6px; }
</style>
