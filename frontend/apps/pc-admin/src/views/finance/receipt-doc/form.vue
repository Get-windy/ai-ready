<template>
  <ErrorBoundary>
    <PageContainer title="收款单">
      <!-- ═══ 单据头 ═══ -->
      <div class="panel">
        <div class="panel-title">
          单据头
          <a-tag v-if="form.id" :color="statusColor(form.status)" class="panel-tag">{{ statusText(form.status) }}</a-tag>
        </div>
        <a-form layout="inline" class="header-form">
          <a-form-item label="收款单号">
            <a-input v-model:value="form.receiptNo" style="width: 180px" placeholder="自动生成" disabled />
          </a-form-item>
          <a-form-item label="客户" required>
            <a-select v-model:value="form.customerId" style="width: 220px" placeholder="请选择客户（可搜索）"
              show-search :filter-option="(input: any, option: any) => option.label?.includes(input)"
              :options="customerOptions" :loading="customerLoading" :disabled="!editable"
              @change="handleCustomerChange" />
          </a-form-item>
          <a-form-item label="收款日期">
            <a-date-picker v-model:value="form.receiptDate" value-format="YYYY-MM-DD" style="width: 150px" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="收款类型">
            <a-select v-model:value="form.receiptType" style="width: 130px" :options="receiptTypeOptions" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="来源单号">
            <a-input v-model:value="form.sourceNo" style="width: 170px" placeholder="关联订单号" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="经手人">
            <a-input v-model:value="form.salesPersonName" style="width: 130px" placeholder="经手人" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="部门">
            <a-input v-model:value="form.departmentName" style="width: 130px" placeholder="部门" :disabled="!editable" />
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 收款金额区 ═══ -->
      <div class="panel">
        <div class="panel-title">收款金额</div>
        <a-form layout="inline" class="header-form">
          <a-form-item label="收款金额" required>
            <a-input-number v-model:value="form.receiptAmount" :min="0" :precision="2"
              style="width: 180px" placeholder="输入收款金额" :disabled="!editable" @change="handleAmountChange" />
          </a-form-item>
          <a-form-item label="支付方式">
            <a-select v-model:value="form.paymentMethod" style="width: 160px" placeholder="选择支付方式"
              :options="methodOptions" :loading="methodLoading" :disabled="!editable" @change="handleMethodChange" />
          </a-form-item>
          <template v-if="form.paymentMethod === 'BANK' || form.paymentMethod === 'CHECK'">
            <a-form-item label="开户银行">
              <a-input v-model:value="form.bankName" style="width: 150px" :disabled="!editable" />
            </a-form-item>
            <a-form-item label="银行账号">
              <a-input v-model:value="form.bankAccount" style="width: 170px" :disabled="!editable" />
            </a-form-item>
          </template>
          <template v-if="form.paymentMethod === 'CHECK'">
            <a-form-item label="支票号">
              <a-input v-model:value="form.checkNo" style="width: 150px" :disabled="!editable" />
            </a-form-item>
          </template>
          <template v-if="form.paymentMethod === 'WECHAT' || form.paymentMethod === 'ALIPAY'">
            <a-form-item label="交易号">
              <a-input v-model:value="form.transactionNo" style="width: 200px" :disabled="!editable" />
            </a-form-item>
          </template>
        </a-form>
      </div>

      <!-- ═══ 核销明细 ═══ -->
      <div class="panel">
        <div class="panel-title">
          核销明细
          <span v-if="form.customerId && customerReceivables.length > 0" class="panel-subtitle">
            已选 {{ selectedReceivables.size }} 笔，核销金额 ¥{{ formatMoney(writeOffTotal) }}
          </span>
        </div>
        <a-table :columns="receivableColumns" :data-source="customerReceivables" :loading="receivableLoading"
          row-key="id" size="small" :pagination="{ pageSize: 10 }" :scroll="{ x: 900 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'selection'">
              <a-checkbox :checked="isSelected(record)" :disabled="!editable" @change="toggleSelect(record)" />
            </template>
            <template v-else-if="column.dataIndex === 'remainingAmount'">
              ¥{{ formatMoney(record.remainingAmount) }}
            </template>
            <template v-else-if="column.dataIndex === 'verifyAmount'">
              <a-input-number v-if="isSelected(record)" v-model:value="record._verifyAmount"
                :min="0" :max="record.remainingAmount" :precision="2" style="width: 120px" :disabled="!editable"
                @change="recalcWriteOff" />
              <span v-else>-</span>
            </template>
            <template v-else-if="column.dataIndex === 'dueDate'">
              {{ record.dueDate || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <a-tag :color="record.status === 'normal' ? 'blue' : record.status === 'overdue' ? 'red' : 'default'">
                {{ record.status === 'normal' ? '正常' : record.status === 'overdue' ? '逾期' : record.status }}
              </a-tag>
            </template>
          </template>
        </a-table>
        <div v-if="!form.customerId" class="table-empty-hint">请先选择客户，自动加载未核销应收单</div>
      </div>

      <!-- ═══ 金额面板 ═══ -->
      <div class="panel panel-summary">
        <div class="summary-row">
          <span class="summary-label">收款金额</span>
          <span class="summary-value">¥ {{ formatMoney(form.receiptAmount) }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">已核销金额</span>
          <span class="summary-value">¥ {{ formatMoney(form.verifiedAmount) }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">待核销金额</span>
          <span class="summary-value" :class="{ 'text-danger': form.pendingAmount > 0 }">¥ {{ formatMoney(form.pendingAmount) }}</span>
        </div>
      </div>

      <!-- ═══ 操作按钮 ═══ -->
      <div class="panel btn-row">
        <a-space wrap>
          <a-button v-if="editable" type="primary" :loading="saving" @click="handleSave">保存草稿</a-button>
          <a-button v-if="form.status === 0" type="primary" ghost :loading="submitting" @click="handleSubmit">提交审批</a-button>
          <template v-if="form.id">
            <a-button v-if="form.status === 1" type="primary" ghost :loading="acting" @click="handleApprove">审批通过</a-button>
            <a-button v-if="form.status === 1" :loading="acting" @click="handleReject">驳回</a-button>
            <a-button v-if="form.status === 2 || form.status === 5" type="primary" ghost :loading="acting" @click="handleCompleteVerify">完成核销</a-button>
            <a-button v-if="[1, 2, 5].includes(form.status)" :loading="acting" @click="handleCancel">取消单据</a-button>
            <a-button v-if="form.status === 6" type="primary" ghost :loading="acting" @click="handleComplete">完成收款</a-button>
          </template>
          <a-button @click="router.push('/finance/receipt-doc')">返回列表</a-button>
        </a-space>
      </div>

      <!-- ═══ 备注 ═══ -->
      <div class="panel">
        <div class="panel-title">备注</div>
        <a-textarea v-model:value="form.remark" :rows="2" placeholder="单据备注" :disabled="!editable" style="max-width: 600px" />
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { receiptApi } from '@/api/finance'
import { paymentMethodApi } from '@/api/payment/md'
import { customerApi } from '@/api/customer'

defineOptions({ name: 'ReceiptDocForm' })

const router = useRouter()
const route = useRoute()

// ═══ 状态字典 ═══
const RECEIPT_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '已拒绝', color: 'red' },
  4: { text: '待核销', color: 'gold' },
  5: { text: '核销中', color: 'purple' },
  6: { text: '已核销', color: 'green' },
  7: { text: '已完成', color: 'green' },
  8: { text: '已取消', color: 'red' },
}
function statusText(s: number) { return RECEIPT_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return RECEIPT_STATUS_MAP[s]?.color || 'default' }

const receiptTypeOptions = [
  { label: '销售收款', value: 1 },
  { label: '预收', value: 2 },
  { label: '其他', value: 3 },
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 表单 ═══
const editable = computed(() => !form.id || form.status === 0)

const emptyForm = () => ({
  id: 0,
  receiptNo: '',
  receiptType: 1,
  customerId: undefined as number | undefined,
  customerName: '',
  orderId: undefined as number | undefined,
  orderNo: '',
  sourceNo: '',
  sourceType: '',
  receiptDate: new Date().toISOString().slice(0, 10),
  receiptAmount: 0,
  verifiedAmount: 0,
  pendingAmount: 0,
  paymentMethod: undefined as string | undefined,
  bankName: '',
  bankAccount: '',
  checkNo: '',
  transactionNo: '',
  salesPersonName: '',
  departmentName: '',
  remark: '',
  status: 0,
})

const form = reactive(emptyForm())
const saving = ref(false)
const submitting = ref(false)
const acting = ref(false)

// ═══ 客户选项 ═══
const customerOptions = ref<{ label: string; value: number }[]>([])
const customerLoading = ref(false)
async function loadCustomers() {
  customerLoading.value = true
  try {
    const res: any = await customerApi.getPage({ pageSize: 200 })
    const list: any[] = res?.records || []
    customerOptions.value = list.map((c: any) => ({ label: `${c.name || ''}${c.code ? '(' + c.code + ')' : ''}`, value: c.id }))
  } catch { customerOptions.value = [] }
  finally { customerLoading.value = false }
}
function handleCustomerChange(val: number) {
  const opt = customerOptions.value.find(o => o.value === val)
  form.customerName = opt?.label?.split('(')[0] || ''
  loadReceivables(val)
}

// ═══ 支付方式选项（从 md_payment_method 档案读取） ═══
const methodOptions = ref<{ label: string; value: string }[]>([])
const methodLoading = ref(false)
async function loadMethods() {
  methodLoading.value = true
  try {
    const res: any = await paymentMethodApi.list()
    const list: any[] = Array.isArray(res) ? res : res?.data || []
    methodOptions.value = list.map((m: any) => ({ label: `[${m.methodCode}] ${m.methodName}`, value: m.methodCode }))
  } catch { methodOptions.value = [] }
  finally { methodLoading.value = false }
}
function handleMethodChange(_val: string) {
  // Reset conditional fields when method changes
  form.bankName = ''
  form.bankAccount = ''
  form.checkNo = ''
  form.transactionNo = ''
}

// ═══ 金额联动 ═══
function handleAmountChange() {
  form.pendingAmount = (form.receiptAmount || 0) - (form.verifiedAmount || 0)
}

// ═══ 核销明细 ═══
const receivableColumns = [
  { title: '选择', dataIndex: 'selection', key: 'selection', width: 60 },
  { title: '来源单号', dataIndex: 'sourceNo', key: 'sourceNo', width: 160 },
  { title: '应收金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, align: 'right' },
  { title: '已核销', dataIndex: 'paidAmount', key: 'paidAmount', width: 100, align: 'right' },
  { title: '未核销金额', dataIndex: 'remainingAmount', key: 'remainingAmount', width: 120, align: 'right' },
  { title: '到期日', dataIndex: 'dueDate', key: 'dueDate', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '本次核销', dataIndex: 'verifyAmount', key: 'verifyAmount', width: 150 },
]

const customerReceivables = ref<any[]>([])
const receivableLoading = ref(false)
const selectedReceivables = ref<Set<number>>(new Set())
const writeOffTotal = computed(() => {
  return customerReceivables.value
    .filter(r => selectedReceivables.value.has(r.id))
    .reduce((sum, r) => sum + Number(r._verifyAmount || 0), 0)
})

async function loadReceivables(customerId: number) {
  receivableLoading.value = true
  selectedReceivables.value = new Set()
  try {
    const res: any = await receiptApi.getPage({ customerId, pageNum: 1, pageSize: 500 })
    const list = res?.records || []
    customerReceivables.value = list
      .filter((r: any) => r.status < 7 && (r.pendingAmount || r.remainingAmount) > 0)
      .map((r: any) => ({ ...r, _verifyAmount: 0 }))
  } catch {
    customerReceivables.value = []
  } finally {
    receivableLoading.value = false
  }
}

function isSelected(record: any) { return selectedReceivables.value.has(record.id) }
function toggleSelect(record: any) {
  if (selectedReceivables.value.has(record.id)) {
    selectedReceivables.value.delete(record.id)
    record._verifyAmount = 0
  } else {
    selectedReceivables.value.add(record.id)
    record._verifyAmount = Math.min(record.remainingAmount, form.receiptAmount || 0)
  }
  recalcWriteOff()
}
function recalcWriteOff() {
  const total = writeOffTotal.value
  form.verifiedAmount = total
  form.pendingAmount = (form.receiptAmount || 0) - total
}

// ═══ 操作 ═══
async function handleSave() {
  saving.value = true
  try {
    const payload = buildPayload()
    if (form.id) {
      await receiptApi.update(form.id, payload)
      message.success('保存成功')
    } else {
      await receiptApi.create(payload)
      message.success('创建成功')
    }
    router.push('/finance/receipt-doc')
  } catch (e: any) {
    message.error(e?.data?.message || '保存失败')
  } finally { saving.value = false }
}

async function handleSubmit() {
  if (!form.customerId) { message.warning('请选择客户'); return }
  if (!form.receiptAmount || form.receiptAmount <= 0) { message.warning('请输入收款金额'); return }
  submitting.value = true
  try {
    if (form.id) {
      await receiptApi.submit(form.id)
    } else {
      const payload = buildPayload()
      const res: any = await receiptApi.create(payload)
      if (res?.id) {
        form.id = res.id
        form.status = 0
        await receiptApi.submit(res.id)
      }
    }
    message.success('已提交审批')
    router.push('/finance/receipt-doc')
  } catch (e: any) {
    message.error(e?.data?.message || '提交失败')
  } finally { submitting.value = false }
}

async function handleApprove() {
  Modal.confirm({
    title: '审批通过',
    content: '确认审批通过该收款单？',
    onOk: async () => {
      acting.value = true
      try {
        await receiptApi.approve(form.id)
        message.success('已审批通过')
        loadForm(form.id)
      } catch (e: any) { message.error(e?.data?.message || '审批失败') }
      finally { acting.value = false }
    }
  })
}

async function handleReject() {
  Modal.confirm({
    title: '驳回单据',
    content: '确认驳回该收款单？',
    onOk: async () => {
      acting.value = true
      try {
        await receiptApi.reject(form.id, '驳回')
        message.success('已驳回')
        loadForm(form.id)
      } catch (e: any) { message.error(e?.data?.message || '驳回失败') }
      finally { acting.value = false }
    }
  })
}

async function handleCompleteVerify() {
  acting.value = true
  try {
    await receiptApi.completeVerify(form.id)
    message.success('核销完成')
    loadForm(form.id)
  } catch (e: any) { message.error(e?.data?.message || '核销失败') }
  finally { acting.value = false }
}

async function handleComplete() {
  acting.value = true
  try {
    await receiptApi.complete(form.id)
    message.success('已完成')
    loadForm(form.id)
  } catch (e: any) { message.error(e?.data?.message || '完成失败') }
  finally { acting.value = false }
}

async function handleCancel() {
  Modal.confirm({
    title: '取消单据',
    content: '确认取消该收款单？',
    onOk: async () => {
      acting.value = true
      try {
        await receiptApi.cancel(form.id, '手动取消')
        message.success('已取消')
        loadForm(form.id)
      } catch (e: any) { message.error(e?.data?.message || '取消失败') }
      finally { acting.value = false }
    }
  })
}

function buildPayload() {
  return {
    receiptType: form.receiptType,
    customerId: form.customerId,
    customerName: form.customerName,
    receiptDate: form.receiptDate,
    receiptAmount: form.receiptAmount,
    paymentMethod: form.paymentMethod,
    bankName: form.bankName || undefined,
    bankAccount: form.bankAccount || undefined,
    checkNo: form.checkNo || undefined,
    transactionNo: form.transactionNo || undefined,
    salesPersonName: form.salesPersonName || undefined,
    departmentName: form.departmentName || undefined,
    sourceNo: form.sourceNo || undefined,
    remark: form.remark || undefined,
  }
}

async function loadForm(id: number) {
  try {
    const res: any = await receiptApi.getById(id)
    if (res) {
      Object.assign(form, {
        id: res.id,
        receiptNo: res.receiptNo || '',
        receiptType: res.receiptType ?? 1,
        customerId: res.customerId,
        customerName: res.customerName || '',
        orderId: res.orderId,
        orderNo: res.orderNo || '',
        sourceNo: res.sourceNo || '',
        sourceType: res.sourceType || '',
        receiptDate: res.receiptDate || new Date().toISOString().slice(0, 10),
        receiptAmount: res.receiptAmount || 0,
        verifiedAmount: res.verifiedAmount || 0,
        pendingAmount: res.pendingAmount || 0,
        paymentMethod: res.paymentMethod,
        bankName: res.bankName || '',
        bankAccount: res.bankAccount || '',
        checkNo: res.checkNo || '',
        transactionNo: res.transactionNo || '',
        salesPersonName: res.salesPersonName || '',
        departmentName: res.departmentName || '',
        remark: res.remark || '',
        status: res.status ?? 0,
      })
    }
  } catch (e) {
    console.warn('[收款单] 加载失败', e)
  }
}

onMounted(async () => {
  await Promise.all([loadCustomers(), loadMethods()])
  const editId = route.query.id ? Number(route.query.id) : undefined
  if (editId) {
    await loadForm(editId)
  }
})
</script>

<style scoped>
.panel { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.panel-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
.panel-subtitle { font-size: 13px; font-weight: 400; color: #666; }
.panel-tag { margin-left: auto; }
.header-form { display: flex; flex-wrap: wrap; gap: 0; }
.header-form .ant-form-item { margin-bottom: 12px; }
.btn-row { display: flex; justify-content: flex-start; }
.panel-summary { display: flex; gap: 40px; padding: 16px 24px; }
.summary-row { display: flex; flex-direction: column; gap: 4px; }
.summary-label { font-size: 13px; color: #999; }
.summary-value { font-size: 20px; font-weight: 700; color: #333; }
.text-danger { color: #f5222d; }
.table-empty-hint { text-align: center; padding: 24px; color: #999; font-size: 14px; }
</style>
