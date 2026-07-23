<template>
  <ErrorBoundary>
    <PageContainer title="其他收入单">
      <!-- ========== 单据头 ========== -->
      <div class="panel">
        <div class="panel-title">
          单据头
          <a-tag v-if="form.id" :color="statusColor(form.status)" class="panel-tag">{{ statusText(form.status) }}</a-tag>
        </div>
        <a-form layout="inline" class="header-form">
          <a-form-item label="单据编号">
            <a-input v-model:value="form.docNo" style="width: 180px" placeholder="自动生成" disabled />
          </a-form-item>
          <a-form-item label="收入类型" required>
            <a-select v-model:value="form.incomeType" style="width: 180px" placeholder="请选择收入类型"
              :options="incomeTypeOptions" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="收入日期">
            <a-date-picker v-model:value="form.incomeDate" value-format="YYYY-MM-DD" style="width: 150px" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="付款单位">
            <a-input v-model:value="form.partnerName" style="width: 200px" placeholder="付款单位名称" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="制单人">
            <a-input v-model:value="form.creatorName" style="width: 130px" placeholder="制单人" disabled />
          </a-form-item>
        </a-form>
      </div>

      <!-- ========== 收入金额区 ========== -->
      <div class="panel">
        <div class="panel-title">收入金额</div>
        <a-form layout="inline" class="header-form">
          <a-form-item label="收入金额" required>
            <a-input-number v-model:value="form.amount" :min="0" :precision="2"
              style="width: 180px" placeholder="输入收入金额" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="结算方式">
            <a-select v-model:value="form.settlementMethod" style="width: 160px" placeholder="选择结算方式"
              :options="methodOptions" :loading="methodLoading" :disabled="!editable" @change="handleMethodChange" />
          </a-form-item>
          <template v-if="form.settlementMethod === 'BANK_TRANSFER' || form.settlementMethod === 'BANK' || form.settlementMethod === 'CHECK'">
            <a-form-item label="开户银行">
              <a-input v-model:value="form.bankName" style="width: 150px" :disabled="!editable" />
            </a-form-item>
            <a-form-item label="银行账号">
              <a-input v-model:value="form.bankAccount" style="width: 170px" :disabled="!editable" />
            </a-form-item>
          </template>
          <template v-if="form.settlementMethod === 'CHECK'">
            <a-form-item label="支票号">
              <a-input v-model:value="form.checkNo" style="width: 150px" :disabled="!editable" />
            </a-form-item>
          </template>
          <template v-if="form.settlementMethod === 'BANK_TRANSFER' || form.settlementMethod === 'WECHAT' || form.settlementMethod === 'ALIPAY'">
            <a-form-item label="交易流水号">
              <a-input v-model:value="form.transactionNo" style="width: 200px" :disabled="!editable" />
            </a-form-item>
          </template>
        </a-form>
      </div>

      <!-- ========== 收入来源 ========== -->
      <div class="panel">
        <div class="panel-title">收入来源说明</div>
        <a-form layout="inline" class="header-form">
          <a-form-item label="来源说明" style="width: 100%;">
            <a-input v-model:value="form.source" placeholder="收入来源及说明" :disabled="!editable" style="max-width: 600px" />
          </a-form-item>
        </a-form>
      </div>

      <!-- ========== 操作按钮 ========== -->
      <div class="panel btn-row">
        <a-space wrap>
          <a-button v-if="editable" type="primary" :loading="saving" @click="handleSave">保存草稿</a-button>
          <a-button v-if="form.status === 0" type="primary" ghost :loading="submitting" @click="handleSubmit">提交审批</a-button>
          <template v-if="form.id">
            <a-button v-if="form.status === 1" type="primary" ghost :loading="acting" @click="handleApprove">审批通过</a-button>
            <a-button v-if="form.status === 1" :loading="acting" @click="handleReject">驳回</a-button>
            <a-button v-if="form.status === 2" type="primary" ghost :loading="acting" @click="handleComplete">完成收款</a-button>
            <a-button v-if="[1, 2].includes(form.status)" :loading="acting" @click="handleCancel">取消单据</a-button>
          </template>
          <a-button @click="router.push('/finance/other-income-doc')">返回列表</a-button>
        </a-space>
      </div>

      <!-- ========== 备注 ========== -->
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
import { request } from '@/utils/request'
import { paymentMethodApi } from '@/api/payment/md'

defineOptions({ name: 'OtherIncomeDocForm' })

const router = useRouter()
const route = useRoute()

// ========== 状态字典 ==========
const INCOME_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '已拒绝', color: 'red' },
  4: { text: '已完成', color: 'green' },
  8: { text: '已取消', color: 'red' },
}
function statusText(s: number) { return INCOME_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return INCOME_STATUS_MAP[s]?.color || 'default' }

const incomeTypeOptions = [
  { label: '利息收入', value: 'INTEREST' },
  { label: '租金收入', value: 'RENT' },
  { label: '罚款收入', value: 'PENALTY' },
  { label: '保险理赔', value: 'INSURANCE' },
  { label: '废品变卖', value: 'SCRAP' },
  { label: '政府补助', value: 'SUBSIDY' },
  { label: '其他', value: 'OTHER' },
]

// ========== 表单 ==========
const editable = computed(() => !form.id || form.status === 0)

const emptyForm = () => ({
  id: 0,
  docNo: '',
  incomeType: undefined as string | undefined,
  incomeDate: new Date().toISOString().slice(0, 10),
  amount: 0,
  partnerName: '',
  settlementMethod: undefined as string | undefined,
  bankName: '',
  bankAccount: '',
  checkNo: '',
  transactionNo: '',
  source: '',
  creatorName: '',
  remark: '',
  status: 0,
})

const form = reactive(emptyForm())
const saving = ref(false)
const submitting = ref(false)
const acting = ref(false)

// ========== 支付方式/结算方式选项（从 md_payment_method 档案读取） ==========
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
  form.bankName = ''
  form.bankAccount = ''
  form.checkNo = ''
  form.transactionNo = ''
}

// ========== API ==========
const incomeApi = {
  create: (data: any) => request.post('/erp/finance/other-income-doc', data),
  update: (id: number, data: any) => request.put(`/erp/finance/other-income-doc/${id}`, data),
  getById: (id: number) => request.get(`/erp/finance/other-income-doc/${id}`),
  submit: (id: number) => request.post(`/erp/finance/other-income-doc/${id}/submit`),
  approve: (id: number) => request.post(`/erp/finance/other-income-doc/${id}/approve`),
  reject: (id: number, reason: string) => request.post(`/erp/finance/other-income-doc/${id}/reject`, null, { params: { reason } }),
  complete: (id: number) => request.post(`/erp/finance/other-income-doc/${id}/complete`),
  cancel: (id: number, reason: string) => request.post(`/erp/finance/other-income-doc/${id}/cancel`, null, { params: { reason } }),
}

// ========== 操作 ==========
async function handleSave() {
  saving.value = true
  try {
    const payload = buildPayload()
    if (form.id) {
      await incomeApi.update(form.id, payload)
      message.success('保存成功')
    } else {
      await incomeApi.create(payload)
      message.success('创建成功')
    }
    router.push('/finance/other-income-doc')
  } catch (e: any) { message.error(e?.data?.message || '保存失败') }
  finally { saving.value = false }
}

async function handleSubmit() {
  if (!form.incomeType) { message.warning('请选择收入类型'); return }
  if (!form.amount || form.amount <= 0) { message.warning('请输入收入金额'); return }
  submitting.value = true
  try {
    if (form.id) {
      await incomeApi.submit(form.id)
    } else {
      const payload = buildPayload()
      const res: any = await incomeApi.create(payload)
      if (res?.id) await incomeApi.submit(res.id)
    }
    message.success('已提交审批')
    router.push('/finance/other-income-doc')
  } catch (e: any) { message.error(e?.data?.message || '提交失败') }
  finally { submitting.value = false }
}

async function handleApprove() {
  Modal.confirm({
    title: '审批通过', content: '确认审批通过该其他收入单？',
    onOk: async () => {
      acting.value = true
      try { await incomeApi.approve(form.id); message.success('已审批通过'); loadForm(form.id) }
      catch (e: any) { message.error(e?.data?.message || '审批失败') }
      finally { acting.value = false }
    }
  })
}

async function handleReject() {
  Modal.confirm({
    title: '驳回', content: '确认驳回该其他收入单？',
    onOk: async () => {
      acting.value = true
      try { await incomeApi.reject(form.id, '驳回'); message.success('已驳回'); loadForm(form.id) }
      catch (e: any) { message.error(e?.data?.message || '驳回失败') }
      finally { acting.value = false }
    }
  })
}

async function handleComplete() {
  Modal.confirm({
    title: '完成收款', content: '确认该笔收入已完成收款？',
    onOk: async () => {
      acting.value = true
      try { await incomeApi.complete(form.id); message.success('收款完成'); loadForm(form.id) }
      catch (e: any) { message.error(e?.data?.message || '操作失败') }
      finally { acting.value = false }
    }
  })
}

async function handleCancel() {
  Modal.confirm({
    title: '取消单据', content: '确认取消该其他收入单？',
    onOk: async () => {
      acting.value = true
      try { await incomeApi.cancel(form.id, '手动取消'); message.success('已取消'); loadForm(form.id) }
      catch (e: any) { message.error(e?.data?.message || '取消失败') }
      finally { acting.value = false }
    }
  })
}

function buildPayload() {
  return {
    incomeType: form.incomeType,
    incomeDate: form.incomeDate,
    amount: form.amount,
    partnerName: form.partnerName || undefined,
    settlementMethod: form.settlementMethod,
    bankName: form.bankName || undefined,
    bankAccount: form.bankAccount || undefined,
    checkNo: form.checkNo || undefined,
    transactionNo: form.transactionNo || undefined,
    source: form.source || undefined,
    remark: form.remark || undefined,
  }
}

async function loadForm(id: number) {
  try {
    const res: any = await incomeApi.getById(id)
    if (res) {
      Object.assign(form, {
        id: res.id,
        docNo: res.docNo || '',
        incomeType: res.incomeType,
        incomeDate: res.incomeDate || new Date().toISOString().slice(0, 10),
        amount: res.amount || 0,
        partnerName: res.partnerName || '',
        settlementMethod: res.settlementMethod,
        bankName: res.bankName || '',
        bankAccount: res.bankAccount || '',
        checkNo: res.checkNo || '',
        transactionNo: res.transactionNo || '',
        source: res.source || '',
        creatorName: res.creatorName || '',
        remark: res.remark || '',
        status: res.status ?? 0,
      })
    }
  } catch (e) { console.warn('[其他收入单] 加载失败', e) }
}

onMounted(async () => {
  await loadMethods()
  const editId = route.query.id ? Number(route.query.id) : undefined
  if (editId) await loadForm(editId)
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
