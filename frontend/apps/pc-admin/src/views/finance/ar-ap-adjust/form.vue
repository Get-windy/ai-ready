<template>
  <ErrorBoundary>
    <PageContainer title="应收应付调整">
      <!-- ═══ 单据头 ═══ -->
      <div class="panel">
        <div class="panel-title">
          单据头
          <a-tag v-if="form.id" :color="statusColor(form.status)" class="panel-tag">{{ statusText(form.status) }}</a-tag>
        </div>
        <a-form layout="inline" class="header-form">
          <a-form-item label="调整单号">
            <a-input v-model:value="form.adjustNo" style="width: 180px" placeholder="自动生成" disabled />
          </a-form-item>
          <a-form-item label="单位类型" required>
            <a-select v-model:value="form.partyType" style="width: 120px" placeholder="选择类型"
              :options="partyTypeOptions" :disabled="!editable" @change="handlePartyTypeChange" />
          </a-form-item>
          <a-form-item label="往来单位" required>
            <a-select v-model:value="form.partyId" style="width: 220px"
              :placeholder="form.partyType === 'CUSTOMER' ? '选择客户（可搜索）' : '选择供应商（可搜索）'"
              show-search :filter-option="(i: any, o: any) => o.label?.includes(i)"
              :options="partyOptions" :loading="partyLoading" :disabled="!editable || !form.partyType"
              @change="handlePartyChange" />
          </a-form-item>
          <a-form-item label="调整日期">
            <a-date-picker v-model:value="form.adjustDate" value-format="YYYY-MM-DD" style="width: 150px" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="来源单号">
            <a-input v-model:value="form.sourceNo" style="width: 170px" placeholder="关联单据号" :disabled="!editable" />
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 调整明细 ═══ -->
      <div class="panel">
        <div class="panel-title">调整明细</div>
        <a-form layout="inline" class="header-form">
          <a-form-item label="调整类型" required>
            <a-select v-model:value="form.adjustType" style="width: 140px" placeholder="选择调整类型"
              :options="adjustTypeOptions" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="调整方向" required>
            <a-select v-model:value="form.direction" style="width: 120px" placeholder="选择方向"
              :options="directionOptions" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="调整金额" required>
            <a-input-number v-model:value="form.adjustAmount" :min="0" :precision="2"
              style="width: 180px" placeholder="输入调整金额" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="关联单据">
            <a-select v-model:value="form.refId" style="width: 220px" placeholder="选择关联单据（可选）"
              show-search :filter-option="(i: any, o: any) => o.label?.includes(i)"
              :options="refOrderOptions" :loading="refOrderLoading" :disabled="!editable || !form.partyId"
              @change="handleRefOrderChange" />
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 备注 ═══ -->
      <div class="panel">
        <div class="panel-title">备注</div>
        <a-textarea v-model:value="form.remark" :rows="3" placeholder="调整原因说明" :disabled="!editable" style="max-width: 600px" />
      </div>

      <!-- ═══ 操作按钮 ═══ -->
      <div class="panel btn-row">
        <a-space wrap>
          <a-button v-if="editable" type="primary" :loading="saving" @click="handleSave">保存草稿</a-button>
          <a-button v-if="editable" :loading="saving" @click="handleSaveAndSubmit">保存并提交</a-button>
          <template v-if="form.id">
            <a-button v-if="form.status === 1" type="primary" ghost :loading="acting" @click="handleApprove">审批通过</a-button>
            <a-button v-if="form.status === 1" :loading="acting" @click="handleReject">驳回</a-button>
            <a-button v-if="form.status === 2" type="primary" ghost :loading="acting" @click="handleComplete">完成</a-button>
            <a-button v-if="[0, 1, 2].includes(form.status)" :loading="acting" @click="handleCancel">取消单据</a-button>
          </template>
          <a-button @click="router.push('/finance/ar-ap-adjust')">返回列表</a-button>
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
import { receivableApi, payableApi } from '@/api/finance'
import { customerApi } from '@/api/customer'
import { supplierApi } from '@/api/supplier'
import request from '@/utils/request'

defineOptions({ name: 'ArApAdjustForm' })

const router = useRouter()
const route = useRoute()

// ═══ 状态字典 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '已完成', color: 'green' },
  4: { text: '已取消', color: 'red' },
  5: { text: '已驳回', color: 'red' },
}
function statusText(s: number) { return STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return STATUS_MAP[s]?.color || 'default' }

// ═══ 选项字典 ═══
const partyTypeOptions = [
  { label: '客户', value: 'CUSTOMER' },
  { label: '供应商', value: 'SUPPLIER' },
]
const adjustTypeOptions = [
  { label: '坏账', value: 'BAD_DEBT' },
  { label: '折让', value: 'ALLOWANCE' },
  { label: '汇兑差异', value: 'EXCHANGE_DIFF' },
  { label: '其他', value: 'OTHER' },
]
const directionOptions = [
  { label: '增（调增）', value: 'INCREASE' },
  { label: '减（调减）', value: 'DECREASE' },
]

function formatMoney(v: any) {
  if (v === null || v === undefined || isNaN(Number(v))) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 表单 ═══
const editable = computed(() => !form.id || form.status === 0 || form.status === 5)

const emptyForm = () => ({
  id: 0,
  adjustNo: '',
  partyType: undefined as string | undefined,
  partyId: undefined as number | undefined,
  partyName: '',
  adjustDate: new Date().toISOString().slice(0, 10),
  adjustType: undefined as string | undefined,
  direction: undefined as string | undefined,
  adjustAmount: 0,
  refId: undefined as number | undefined,
  refNo: '',
  sourceNo: '',
  remark: '',
  status: 0,
})
const form = reactive(emptyForm())
const saving = ref(false)
const acting = ref(false)

// ═══ 往来单位选项 ═══
const partyOptions = ref<{ label: string; value: number }[]>([])
const partyLoading = ref(false)

async function loadCustomers() {
  partyLoading.value = true
  try {
    const res: any = await customerApi.getPage({ pageSize: 200 })
    partyOptions.value = (res?.records || []).map((c: any) => ({
      label: `${c.name || ''}${c.code ? '(' + c.code + ')' : ''}`,
      value: c.id,
    }))
  } catch { partyOptions.value = [] }
  finally { partyLoading.value = false }
}

async function loadSuppliers() {
  partyLoading.value = true
  try {
    const res: any = await supplierApi.page({ pageSize: 200 })
    const list: any[] = res?.records || []
    partyOptions.value = list.map((s: any) => ({
      label: `${s.supplierName || s.name || ''}${s.supplierCode || s.code ? '(' + (s.supplierCode || s.code) + ')' : ''}`,
      value: s.id,
    }))
  } catch { partyOptions.value = [] }
  finally { partyLoading.value = false }
}

function handlePartyTypeChange(_val: string) {
  form.partyId = undefined
  form.partyName = ''
  form.refId = undefined
  form.refNo = ''
  refOrderOptions.value = []
  if (form.partyType === 'CUSTOMER') {
    loadCustomers()
  } else if (form.partyType === 'SUPPLIER') {
    loadSuppliers()
  } else {
    partyOptions.value = []
  }
}

function handlePartyChange(val: number) {
  const opt = partyOptions.value.find(o => o.value === val)
  form.partyName = opt?.label?.split('(')[0] || ''
  loadRefOrders(val)
}

// ═══ 关联单据选项 ═══
const refOrderOptions = ref<{ label: string; value: number }[]>([])
const refOrderLoading = ref(false)

async function loadRefOrders(partyId: number) {
  if (!partyId || !form.partyType) return
  refOrderLoading.value = true
  try {
    if (form.partyType === 'CUSTOMER') {
      const res: any = await receivableApi.getPage({ customerId: partyId, pageSize: 200 })
      refOrderOptions.value = (res?.records || []).map((r: any) => ({
        label: `${r.sourceNo || r.invoiceNo || '无单号'} ¥${formatMoney(r.remainingAmount || r.totalAmount)}`,
        value: r.id,
      }))
    } else {
      const res: any = await payableApi.getPage({ supplierId: partyId, pageSize: 200 })
      refOrderOptions.value = (res?.records || []).map((r: any) => ({
        label: `${r.sourceNo || r.invoiceNo || '无单号'} ¥${formatMoney(r.remainingAmount || r.totalAmount)}`,
        value: r.id,
      }))
    }
  } catch { refOrderOptions.value = [] }
  finally { refOrderLoading.value = false }
}

function handleRefOrderChange(val: number) {
  const opt = refOrderOptions.value.find(o => o.value === val)
  form.refNo = opt?.label?.split(' ¥')[0] || ''
}

// ═══ 调整API（自定义封装，调用后端调整接口） ═══
const adjustApi = {
  create: (data: any) => request.post('/erp/finance/ar-ap-adjust', data),
  update: (id: number, data: any) => request.put(`/erp/finance/ar-ap-adjust/${id}`, data),
  getById: (id: number) => request.get(`/erp/finance/ar-ap-adjust/${id}`),
  submit: (id: number) => request.post(`/erp/finance/ar-ap-adjust/${id}/submit`),
  approve: (id: number, note?: string) => request.post(`/erp/finance/ar-ap-adjust/${id}/approve`, null, { params: { note } }),
  reject: (id: number, reason: string) => request.post(`/erp/finance/ar-ap-adjust/${id}/reject`, null, { params: { reason } }),
  complete: (id: number) => request.post(`/erp/finance/ar-ap-adjust/${id}/complete`),
  cancel: (id: number, reason: string) => request.post(`/erp/finance/ar-ap-adjust/${id}/cancel`, null, { params: { reason } }),
}

// ═══ 操作 ═══
function buildPayload() {
  return {
    partyType: form.partyType,
    partyId: form.partyId,
    partyName: form.partyName,
    adjustDate: form.adjustDate,
    adjustType: form.adjustType,
    direction: form.direction,
    adjustAmount: form.adjustAmount,
    refId: form.refId || undefined,
    refNo: form.refNo || undefined,
    sourceNo: form.sourceNo || undefined,
    remark: form.remark || undefined,
  }
}

function validate(): boolean {
  if (!form.partyType) { message.warning('请选择单位类型'); return false }
  if (!form.partyId) { message.warning('请选择往来单位'); return false }
  if (!form.adjustType) { message.warning('请选择调整类型'); return false }
  if (!form.direction) { message.warning('请选择调整方向'); return false }
  if (!form.adjustAmount || form.adjustAmount <= 0) { message.warning('请输入调整金额'); return false }
  return true
}

async function handleSave() {
  if (!validate()) return
  saving.value = true
  try {
    if (form.id) {
      await adjustApi.update(form.id, buildPayload())
      message.success('保存成功')
    } else {
      const res: any = await adjustApi.create(buildPayload())
      if (res?.id) form.id = res.id
      message.success('创建成功')
    }
    router.push('/finance/ar-ap-adjust')
  } catch (e: any) {
    message.error(e?.data?.message || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSaveAndSubmit() {
  if (!validate()) return
  saving.value = true
  try {
    let id = form.id
    if (!id) {
      const res: any = await adjustApi.create(buildPayload())
      id = res?.id
    } else {
      await adjustApi.update(id, buildPayload())
    }
    if (id) {
      await adjustApi.submit(id)
      message.success('已提交审批')
    }
    router.push('/finance/ar-ap-adjust')
  } catch (e: any) {
    message.error(e?.data?.message || e?.message || '提交失败')
  } finally {
    saving.value = false
  }
}

async function handleApprove() {
  Modal.confirm({
    title: '审批通过',
    content: '确认审批通过该调整单？',
    onOk: async () => {
      acting.value = true
      try {
        await adjustApi.approve(form.id)
        message.success('已审批通过')
        await loadForm(form.id)
      } catch (e: any) {
        message.error(e?.data?.message || '审批失败')
      } finally {
        acting.value = false
      }
    },
  })
}

async function handleReject() {
  Modal.confirm({
    title: '驳回',
    content: '确认驳回该调整单？',
    onOk: async () => {
      acting.value = true
      try {
        await adjustApi.reject(form.id, '驳回')
        message.success('已驳回')
        await loadForm(form.id)
      } catch (e: any) {
        message.error(e?.data?.message || '驳回失败')
      } finally {
        acting.value = false
      }
    },
  })
}

async function handleComplete() {
  Modal.confirm({
    title: '完成',
    content: '确认完成该调整单？完成后将更新应收/应付余额。',
    onOk: async () => {
      acting.value = true
      try {
        await adjustApi.complete(form.id)
        message.success('已完成')
        await loadForm(form.id)
      } catch (e: any) {
        message.error(e?.data?.message || '完成失败')
      } finally {
        acting.value = false
      }
    },
  })
}

async function handleCancel() {
  Modal.confirm({
    title: '取消单据',
    content: '确认取消该调整单？',
    onOk: async () => {
      acting.value = true
      try {
        await adjustApi.cancel(form.id, '手动取消')
        message.success('已取消')
        await loadForm(form.id)
      } catch (e: any) {
        message.error(e?.data?.message || '取消失败')
      } finally {
        acting.value = false
      }
    },
  })
}

async function loadForm(id: number) {
  try {
    const res: any = await adjustApi.getById(id)
    if (res) {
      Object.assign(form, {
        id: res.id,
        adjustNo: res.adjustNo || '',
        partyType: res.partyType,
        partyId: res.partyId,
        partyName: res.partyName || '',
        adjustDate: res.adjustDate || new Date().toISOString().slice(0, 10),
        adjustType: res.adjustType,
        direction: res.direction,
        adjustAmount: res.adjustAmount || 0,
        refId: res.refId,
        refNo: res.refNo || '',
        sourceNo: res.sourceNo || '',
        remark: res.remark || '',
        status: res.status ?? 0,
      })
      // 加载关联的往来单位选项和关联单据
      if (form.partyType === 'CUSTOMER') {
        await loadCustomers()
      } else if (form.partyType === 'SUPPLIER') {
        await loadSuppliers()
      }
      if (form.partyId) {
        await loadRefOrders(form.partyId)
      }
    }
  } catch (e) {
    console.warn('[应收应付调整] 加载失败', e)
  }
}

onMounted(async () => {
  const editId = route.query.id ? Number(route.query.id) : undefined
  if (editId) {
    await loadForm(editId)
  }
})
</script>

<style scoped>
.panel { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.panel-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
.panel-tag { margin-left: auto; }
.header-form { display: flex; flex-wrap: wrap; gap: 0; }
.header-form .ant-form-item { margin-bottom: 12px; }
.btn-row { display: flex; justify-content: flex-start; }
</style>
