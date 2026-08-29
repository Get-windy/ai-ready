<template>
  <ErrorBoundary>
    <PageContainer title="会计凭证">
      <!-- ═══ 表头操作按钮（对标：打印F8/导出） ═══ -->
      <div class="panel header-actions">
        <a-space :size="8">
          <a-button size="small" @click="handlePrint">打印(F8)</a-button>
          <a-button size="small" @click="handleExport">导出</a-button>
        </a-space>
      </div>
      <div class="panel">
        <div class="panel-title">
          凭证头
          <a-tag v-if="form.id" :color="statusColor" class="panel-tag">{{ statusText }}</a-tag>
        </div>
        <a-form layout="inline" class="header-form">
          <a-form-item label="凭证号">
            <a-input v-model:value="form.voucherNo" style="width: 180px" placeholder="自动生成" disabled />
          </a-form-item>
          <a-form-item label="凭证日期">
            <a-date-picker v-model:value="form.voucherDate" value-format="YYYY-MM-DD" style="width: 150px" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="凭证类型">
            <a-select v-model:value="form.voucherType" style="width: 130px" :options="voucherTypeOptions" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="附件数">
            <a-input-number v-model:value="form.attachmentCount" :min="0" style="width: 80px" :disabled="!editable" />
          </a-form-item>
          <a-form-item label="摘要" :label-col="{ style: { width: '50px' } }">
            <a-input v-model:value="form.summary" style="width: 300px" placeholder="凭证摘要" :disabled="!editable" />
          </a-form-item>
        </a-form>
      </div>

      <div class="panel">
        <div class="panel-title">
          会计分录
          <span class="panel-subtitle">借：¥{{ formatMoney(debitTotal) }} &nbsp; 贷：¥{{ formatMoney(creditTotal) }}</span>
          <a-tag v-if="Math.abs(debitTotal - creditTotal) > 0.001" color="red" style="margin-left:8px">借贷不平衡</a-tag>
          <a-tag v-else color="green" style="margin-left:8px">借贷平衡</a-tag>
          <a-button v-if="editable" size="small" type="link" @click="addEntry">+ 添加分录</a-button>
        </div>
        <a-table :columns="entryColumns" :data-source="entries" row-key="_key"
          size="small" :pagination="false" :scroll="{ x: 1100 }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.dataIndex === 'lineNo'">{{ index + 1 }}</template>
            <template v-else-if="column.dataIndex === 'subjectId'">
              <a-select v-model:value="record.subjectId" style="width: 100%" placeholder="选择科目"
                show-search :filter-option="(i: any, o: any) => o.label?.includes(i)"
                :options="subjectOptions" :loading="subjectLoading" :disabled="!editable"
                @change="(v: number) => syncSubjectInfo(record, v)" />
            </template>
            <template v-else-if="column.dataIndex === 'subjectCode'">
              <span>{{ getSubjectField(record.subjectId, 'subjectCode') }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'subjectName'">
              <span>{{ getSubjectField(record.subjectId, 'subjectName') }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'summary'">
              <a-input v-model:value="record.summary" placeholder="摘要" :disabled="!editable" />
            </template>
            <template v-else-if="column.dataIndex === 'debitAmount'">
              <a-input-number v-model:value="record.debitAmount" :min="0" :precision="2" style="width: 100%"
                :disabled="!editable" @change="recalcTotals" />
            </template>
            <template v-else-if="column.dataIndex === 'creditAmount'">
              <a-input-number v-model:value="record.creditAmount" :min="0" :precision="2" style="width: 100%"
                :disabled="!editable" @change="recalcTotals" />
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-popconfirm title="删除该分录？" @confirm="removeEntry(index)">
                <a-button size="small" type="link" danger :disabled="!editable">删除</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
        <div v-if="entries.length === 0" class="table-empty-hint">暂无分录，点击「添加分录」录入</div>
      </div>

      <div class="panel btn-row">
        <a-space wrap>
          <a-button v-if="editable" type="primary" :loading="saving" @click="handleSave" :disabled="!balanceOk">保存</a-button>
          <template v-if="form.id">
            <a-button v-if="form.status === 0" :loading="acting" @click="handleAudit">审核</a-button>
            <a-button v-if="form.status === 1" :loading="acting" @click="handlePost">过账</a-button>
            <a-button v-if="form.status === 2" :loading="acting" @click="handleReverse">红冲</a-button>
          </template>
          <a-button @click="router.push('/finance/voucher')">返回列表</a-button>
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
import { voucherApi, accountSubjectApi } from '@/api/finance'
import request from '@/utils/request'

defineOptions({ name: 'VoucherForm' })
const router = useRouter()

// ═══ 表头操作（对标：打印F8/导出） ═══
function handlePrint() {
  message.info('打印(F8)待对接打印模板')
}

async function handleExport() {
  try {
    const blob = await request.get('/erp/finance/voucher/export', { params: { id: form.id, voucherNo: form.voucherNo }, responseType: 'blob' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `会计凭证_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}
const route = useRoute()

const voucherTypeOptions = [
  { label: '记账凭证', value: '记' },
  { label: '收款凭证', value: '收' },
  { label: '付款凭证', value: '付' },
  { label: '转账凭证', value: '转' },
]

function formatMoney(v: any) { return v ? Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '0.00' }

const editable = computed(() => !form.id || form.status === 0)
const statusText = computed(() => {
  const map: Record<number, string> = { 0: '草稿', 1: '已审核', 2: '已过账', 3: '已红冲' }
  return map[form.status] || '未知'
})
const statusColor = computed(() => {
  const map: Record<number, string> = { 0: 'default', 1: 'blue', 2: 'green', 3: 'red' }
  return map[form.status] || 'default'
})

const emptyForm = () => ({
  id: 0, voucherNo: '', voucherDate: new Date().toISOString().slice(0, 10),
  voucherType: '记', attachmentCount: 0, summary: '', status: 0,
})
const form = reactive(emptyForm())
const saving = ref(false)
const acting = ref(false)

// ═══ 分录网格 ═══
let _keySeq = 0
const entries = ref<any[]>([])
// 对标 7 列：明细摘要/科目编号/科目全名/明细科目/借方金额/贷方金额
const entryColumns = [
  { title: '行号', dataIndex: 'lineNo', key: 'lineNo', width: 50 },
  { title: '科目编号', dataIndex: 'subjectCode', key: 'subjectCode', width: 100 },
  { title: '科目全名', dataIndex: 'subjectName', key: 'subjectName', width: 160 },
  { title: '明细科目', dataIndex: 'subjectId', key: 'subjectId', width: 200 },
  { title: '摘要', dataIndex: 'summary', key: 'summary', width: 180 },
  { title: '借方金额', dataIndex: 'debitAmount', key: 'debitAmount', width: 130, align: 'right' },
  { title: '贷方金额', dataIndex: 'creditAmount', key: 'creditAmount', width: 130, align: 'right' },
  { title: '操作', dataIndex: 'action', key: 'action', width: 70 },
]

const debitTotal = computed(() => entries.value.reduce((s, e) => s + Number(e.debitAmount || 0), 0))
const creditTotal = computed(() => entries.value.reduce((s, e) => s + Number(e.creditAmount || 0), 0))
const balanceOk = computed(() => Math.abs(debitTotal.value - creditTotal.value) < 0.001 && debitTotal.value > 0)

function addEntry() {
  entries.value.push({ _key: ++_keySeq, subjectId: undefined, summary: '', debitAmount: 0, creditAmount: 0 })
}
function removeEntry(index: number) { entries.value.splice(index, 1) }
function recalcTotals() { /* computed handles this */ }

// ═══ 科目选项 ═══
const subjectOptions = ref<{ label: string; value: number }[]>([])
const subjectLoading = ref(false)
const subjectMap = ref<Record<number, any>>({})
async function loadSubjects() {
  subjectLoading.value = true
  try {
    const res: any = await accountSubjectApi.getList()
    const list: any[] = Array.isArray(res) ? res : res?.data || []
    subjectMap.value = {}
    subjectOptions.value = list.map((s: any) => {
      subjectMap.value[s.id] = s
      return { label: `${s.subjectCode} ${s.subjectName}`, value: s.id }
    })
  } catch { subjectOptions.value = [] }
  finally { subjectLoading.value = false }
}

function getSubjectField(subjectId: number | undefined, field: string): string {
  if (!subjectId) return ''
  const s = subjectMap.value[subjectId]
  return s ? (s[field] ?? '') : ''
}
function syncSubjectInfo(record: any, val: number) {
  const s = subjectMap.value[val]
  if (s) {
    record.subjectCode = s.subjectCode
    record.subjectName = s.subjectName
  }
}

// ═══ 操作 ═══
async function handleSave() {
  if (!balanceOk.value) { message.warning('借贷不平衡，无法保存'); return }
  if (entries.value.length === 0) { message.warning('请至少添加一条分录'); return }
  saving.value = true
  try {
    const payload = {
      voucherDate: form.voucherDate, voucherType: form.voucherType,
      attachmentCount: form.attachmentCount, summary: form.summary,
      items: entries.value.map((e, i) => ({
        lineNo: i + 1, subjectId: e.subjectId, summary: e.summary,
        debitAmount: e.debitAmount || 0, creditAmount: e.creditAmount || 0,
      })),
    }
    if (form.id) {
      await voucherApi.create({ ...payload, id: form.id })
    } else {
      await voucherApi.create(payload)
    }
    message.success('保存成功')
    router.push('/finance/voucher')
  } catch (e: any) { message.error(e?.data?.message || '保存失败') }
  finally { saving.value = false }
}

async function handleAudit() {
  Modal.confirm({ title: '审核凭证', content: '确认审核该凭证？',
    onOk: async () => {
      acting.value = true
      try { await voucherApi.audit(form.id); message.success('已审核'); loadForm(form.id) }
      catch (e: any) { message.error(e?.data?.message || '审核失败') }
      finally { acting.value = false }
    }
  })
}

async function handlePost() {
  Modal.confirm({ title: '过账', content: '确认过账？过账后不可修改分录。',
    onOk: async () => {
      acting.value = true
      try { await voucherApi.post(form.id); message.success('已过账'); loadForm(form.id) }
      catch (e: any) { message.error(e?.data?.message || '过账失败') }
      finally { acting.value = false }
    }
  })
}

async function handleReverse() {
  Modal.confirm({ title: '红冲', content: '确认红冲该凭证？将生成红字凭证。',
    onOk: async () => {
      acting.value = true
      try { await voucherApi.reverse(form.id, '红冲'); message.success('已红冲'); router.push('/finance/voucher') }
      catch (e: any) { message.error(e?.data?.message || '红冲失败') }
      finally { acting.value = false }
    }
  })
}

async function loadForm(id: number) {
  try {
    const res: any = await voucherApi.getById(id)
    if (res) {
      Object.assign(form, {
        id: res.id, voucherNo: res.voucherNo || '', voucherDate: res.voucherDate || new Date().toISOString().slice(0, 10),
        voucherType: res.voucherType || '记', attachmentCount: res.attachmentCount || 0,
        summary: res.summary || '', status: res.status ?? 0,
      })
      entries.value = (res.items || []).map((item: any) => ({
        _key: ++_keySeq, subjectId: item.subjectId, summary: item.summary || '',
        debitAmount: item.debitAmount || 0, creditAmount: item.creditAmount || 0,
      }))
    }
  } catch (e) { console.warn('[凭证] 加载失败', e) }
}

onMounted(async () => {
  await loadSubjects()
  const editId = route.query.id ? Number(route.query.id) : undefined
  if (editId) { await loadForm(editId) } else { addEntry(); addEntry() }
})
</script>

<style scoped>
.panel { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.panel-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.panel-subtitle { font-size: 13px; font-weight: 400; color: #666; }
.panel-tag { margin-left: auto; }
.header-form .ant-form-item { margin-bottom: 12px; }
.header-actions { padding: 12px 16px; margin-bottom: 0; }
.btn-row { display: flex; justify-content: flex-start; }
.table-empty-hint { text-align: center; padding: 24px; color: #999; }
</style>
