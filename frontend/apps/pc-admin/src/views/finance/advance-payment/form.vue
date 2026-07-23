<template>
  <ErrorBoundary>
    <PageContainer title="预付款单">
      <div class="panel">
        <div class="panel-title">
          单据头
          <a-tag v-if="form.id" :color="statusColor(form.status)" class="panel-tag">{{ statusText(form.status) }}</a-tag>
        </div>
        <a-form layout="inline" class="header-form">
          <a-form-item label="预付款单号">
            <a-input v-model:value="form.prePaymentNo" style="width: 180px" placeholder="自动生成" disabled />
          </a-form-item>
          <a-form-item label="供应商" required>
            <a-select v-model:value="form.supplierId" style="width: 220px" placeholder="选择供应商（可搜索）"
              show-search :filter-option="(i: any, o: any) => o.label?.includes(i)"
              :options="supplierOptions" :loading="supplierLoading" :disabled="!editable"
              @change="handleSupplierChange" />
          </a-form-item>
          <a-form-item label="预付日期">
            <a-date-picker v-model:value="form.paymentDate" value-format="YYYY-MM-DD" style="width: 150px" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="来源">
            <a-input v-model:value="form.sourceNo" style="width: 170px" placeholder="来源单号" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="定金类型">
            <a-select v-model:value="form.depositType" style="width: 130px" :options="depositTypeOptions" :disabled="!editable" />
          </a-form-item>
        </a-form>
      </div>

      <div class="panel">
        <div class="panel-title">预付金额</div>
        <a-form layout="inline" class="header-form">
          <a-form-item label="预付金额" required>
            <a-input-number v-model:value="form.amount" :min="0" :precision="2" style="width: 180px" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="支付方式">
            <a-select v-model:value="form.paymentMethod" style="width: 160px" placeholder="选择支付方式"
              :options="methodOptions" :loading="methodLoading" :disabled="!editable" />
          </a-form-item>
          <template v-if="form.paymentMethod === 'BANK'">
            <a-form-item label="银行">
              <a-input v-model:value="form.bankName" style="width: 150px" :disabled="!editable" />
            </a-form-item>
            <a-form-item label="账号">
              <a-input v-model:value="form.bankAccount" style="width: 170px" :disabled="!editable" />
            </a-form-item>
          </template>
          <a-form-item label="交易号">
            <a-input v-model:value="form.transactionNo" style="width: 200px" :disabled="!editable" />
          </a-form-item>
        </a-form>
      </div>

      <div class="panel panel-summary">
        <div class="summary-row">
          <span class="summary-label">预付金额</span>
          <span class="summary-value">¥{{ formatMoney(form.amount) }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">已使用</span>
          <span class="summary-value">¥{{ formatMoney(form.usedAmount) }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">剩余金额</span>
          <span class="summary-value">¥{{ formatMoney(form.remainingAmount) }}</span>
        </div>
      </div>

      <div class="panel btn-row">
        <a-space wrap>
          <a-button v-if="editable" type="primary" :loading="saving" @click="handleSave">保存</a-button>
          <template v-if="form.id">
            <a-button v-if="['paid'].includes(form.status)" type="primary" ghost :loading="acting" @click="handleOffset">冲抵到付款单</a-button>
            <a-button v-if="['paid'].includes(form.status)" :loading="acting" @click="handleRecover">收回</a-button>
            <a-button v-if="['paid'].includes(form.status)" :loading="acting" @click="handleRefund">退还</a-button>
          </template>
          <a-button @click="router.push('/finance/advance-payment')">返回列表</a-button>
        </a-space>
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
import { prePaymentApi } from '@/api/finance'
import { paymentMethodApi } from '@/api/payment/md'
import { supplierApi } from '@/api/supplier'

defineOptions({ name: 'AdvancePaymentForm' })
const router = useRouter()
const route = useRoute()

const STATUS_MAP: Record<string, { text: string; color: string }> = {
  paid: { text: '已付款', color: 'blue' },
  offset: { text: '已核销', color: 'green' },
  recovered: { text: '已收回', color: 'orange' },
  refunded: { text: '已退还', color: 'orange' },
}
function statusText(s: string) { return STATUS_MAP[s]?.text || s || '-' }
function statusColor(s: string) { return STATUS_MAP[s]?.color || 'default' }
const depositTypeOptions = [{ label: '普通预付', value: 0 }, { label: '定金', value: 1 }]
function formatMoney(v: any) { return v ? Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '0.00' }
const editable = computed(() => !form.id)

const emptyForm = () => ({
  id: 0, prePaymentNo: '', supplierId: undefined as number | undefined, supplierName: '',
  paymentDate: new Date().toISOString().slice(0, 10), amount: 0, usedAmount: 0, remainingAmount: 0,
  paymentMethod: undefined as string | undefined, bankName: '', bankAccount: '', transactionNo: '',
  depositType: 0, sourceNo: '', status: 'paid', remark: '',
})
const form = reactive(emptyForm())
const saving = ref(false); const acting = ref(false)

const supplierOptions = ref<{ label: string; value: number }[]>([])
const supplierLoading = ref(false)
async function loadSuppliers() {
  supplierLoading.value = true
  try {
    const res: any = await supplierApi.page({ pageSize: 200 })
    supplierOptions.value = (res?.records || []).map((s: any) => ({ label: `${s.name || s.supplierName || ''}${s.code ? '(' + s.code + ')' : ''}`, value: s.id }))
  } catch { supplierOptions.value = [] }
  finally { supplierLoading.value = false }
}
function handleSupplierChange(val: number) {
  const opt = supplierOptions.value.find(o => o.value === val)
  form.supplierName = opt?.label?.split('(')[0] || ''
}

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

async function handleSave() {
  if (!form.supplierId) { message.warning('请选择供应商'); return }
  saving.value = true
  try {
    await prePaymentApi.create({
      supplierId: form.supplierId, supplierName: form.supplierName,
      paymentDate: form.paymentDate, amount: form.amount,
      paymentMethod: form.paymentMethod, bankName: form.bankName || undefined,
      bankAccount: form.bankAccount || undefined, transactionNo: form.transactionNo || undefined,
      depositType: form.depositType, sourceNo: form.sourceNo || undefined, remark: form.remark || undefined,
    })
    message.success('保存成功'); router.push('/finance/advance-payment')
  } catch (e: any) { message.error(e?.data?.message || '保存失败') }
  finally { saving.value = false }
}

async function handleOffset() {
  Modal.confirm({ title: '冲抵到付款单', content: '将该预付款冲抵到付款单？',
    onOk: async () => {
      acting.value = true
      try { await prePaymentApi.offsetToPayment(form.id, 0, form.remainingAmount); message.success('冲抵成功'); router.push('/finance/payment-doc/form') }
      catch (e: any) { message.error(e?.data?.message || '冲抵失败') }
      finally { acting.value = false }
    }
  })
}

async function handleRecover() {
  Modal.confirm({ title: '收回预付款', content: '确认收回该预付款？',
    onOk: async () => {
      acting.value = true
      try { await prePaymentApi.recover(form.id, '收回'); message.success('已收回'); loadForm(form.id) }
      catch (e: any) { message.error(e?.data?.message || '失败') }
      finally { acting.value = false }
    }
  })
}

async function handleRefund() {
  Modal.confirm({ title: '退还预付款', content: '确认退还该预付款？',
    onOk: async () => {
      acting.value = true
      try { await prePaymentApi.refund(form.id, '退还'); message.success('已退还'); loadForm(form.id) }
      catch (e: any) { message.error(e?.data?.message || '失败') }
      finally { acting.value = false }
    }
  })
}

async function loadForm(id: number) {
  try {
    const res: any = await prePaymentApi.getById(id)
    if (res) Object.assign(form, res)
  } catch (e) { console.warn(e) }
}

onMounted(async () => {
  await Promise.all([loadSuppliers(), loadMethods()])
  const editId = route.query.id ? Number(route.query.id) : undefined
  if (editId) await loadForm(editId)
})
</script>

<style scoped>
.panel { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.panel-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
.panel-tag { margin-left: auto; }
.header-form .ant-form-item { margin-bottom: 12px; }
.btn-row { display: flex; justify-content: flex-start; }
.panel-summary { display: flex; gap: 40px; padding: 16px 24px; }
.summary-row { display: flex; flex-direction: column; gap: 4px; }
.summary-label { font-size: 13px; color: #999; }
.summary-value { font-size: 20px; font-weight: 700; color: #333; }
</style>
