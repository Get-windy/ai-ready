<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="优惠券"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="优惠券"
      row-key="id"
    >
      <template #header-extra>
        <a-button type="primary" size="small" @click="openCreate">
          <template #icon><PlusOutlined /></template>发放优惠券
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'programId'">
          {{ programName(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'partnerId'">
          {{ text ?? '未绑定' }}
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="STATUS_MAP[text]?.color">{{ STATUS_MAP[text]?.label || text || '-' }}</a-tag>
        </template>
        <template v-else-if="['faceValue', 'balance'].includes(column.dataIndex as string)">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="['expirationDate', 'usedTime', 'createTime'].includes(column.dataIndex as string)">
          {{ fmtTime(text) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a @click="openEdit(record)">编辑</a>
            <a-divider type="vertical" />
            <a-popconfirm v-if="record.status === 'UNUSED'" title="确认核销该优惠券？" @confirm="handleUse(record)">
              <a>核销</a>
            </a-popconfirm>
            <a-popconfirm title="确认删除该优惠券？" @confirm="handleDelete(record)">
              <a class="text-danger">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <!-- 发放/编辑优惠券弹窗 -->
    <a-modal v-model:open="modalVisible" :title="editingCoupon ? '编辑优惠券' : '发放优惠券'" :confirm-loading="modalLoading" :width="520" @ok="handleModalOk" @cancel="modalVisible = false">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 16px">
        <a-form-item label="优惠方案" required>
          <a-select v-model:value="modalForm.programId" placeholder="请选择优惠方案" :options="programOptions" show-search :filter-option="filterOption" />
        </a-form-item>
        <a-form-item label="优惠码">
          <a-input v-model:value="modalForm.code" placeholder="留空自动生成" :disabled="!!editingCoupon" />
        </a-form-item>
        <a-form-item label="面额">
          <a-input-number v-model:value="modalForm.faceValue" :min="0" :precision="2" style="width: 100%" />
        </a-form-item>
        <a-form-item label="绑定会员">
          <a-select v-model:value="modalForm.partnerId" placeholder="不绑表示公开券" allow-clear :options="memberOptions" show-search :filter-option="filterOption" />
        </a-form-item>
        <a-form-item label="有效期">
          <a-date-picker v-model:value="modalForm.expirationDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="modalForm.remark" :rows="2" placeholder="选填" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { couponApi, loyaltyProgramApi, type LoyaltyCoupon, type LoyaltyProgram } from '@/api/marketing'
import { memberApi } from '@/api/erp'

const STATUS_MAP: Record<string, { label: string; color: string }> = {
  UNUSED: { label: '未使用', color: 'blue' },
  USED: { label: '已使用', color: 'green' },
  EXPIRED: { label: '已过期', color: 'default' },
  CANCELLED: { label: '已取消', color: 'red' }
}

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'partnerId', type: 'input', label: '会员ID', placeholder: '会员(联系人)ID', width: 160 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value })) }
]

const columns: any[] = [
  { title: '优惠码', dataIndex: 'code', key: 'code', width: 150 },
  { title: '所属程序', dataIndex: 'programId', key: 'programId', width: 140, ellipsis: true },
  { title: '使用会员', dataIndex: 'partnerId', key: 'partnerId', width: 100 },
  { title: '面额', dataIndex: 'faceValue', key: 'faceValue', width: 100, align: 'right' },
  { title: '余额', dataIndex: 'balance', key: 'balance', width: 100, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '有效期至', dataIndex: 'expirationDate', key: 'expirationDate', width: 150 },
  { title: '使用时间', dataIndex: 'usedTime', key: 'usedTime', width: 150 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

const programs = ref<LoyaltyProgram[]>([])
const programOptions = computed(() => programs.value.map(p => ({ label: p.name, value: p.id })))
const members = ref<any[]>([])
const memberOptions = computed(() => members.value.map(m => ({ label: `${m.name} (${m.phone || m.cardCode})`, value: m.id })))

function programName(programId: number | undefined): string {
  if (!programId) return '-'
  const p = programs.value.find(item => item.id === programId)
  return p?.name || `#${programId}`
}

function fetcher(params: Record<string, any>) {
  return couponApi.page(params)
}

// 表单弹窗
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingCoupon = ref<LoyaltyCoupon | null>(null)
const modalForm = reactive<Record<string, any>>({})

function filterOption(input: string, option: any) {
  return (option?.label || '').toLowerCase().includes(input.toLowerCase())
}

function openCreate() {
  editingCoupon.value = null
  Object.assign(modalForm, { programId: undefined, code: '', faceValue: 0, partnerId: undefined, expirationDate: undefined, remark: '' })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingCoupon.value = record
  Object.assign(modalForm, {
    programId: record.programId,
    code: record.code,
    faceValue: record.faceValue,
    partnerId: record.partnerId,
    expirationDate: record.expirationDate ? dayjs(record.expirationDate) : undefined,
    remark: record.remark
  })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.programId) { message.warning('请选择优惠方案'); return }
  modalLoading.value = true
  try {
    const payload = {
      ...modalForm,
      expirationDate: modalForm.expirationDate ? dayjs(modalForm.expirationDate).format('YYYY-MM-DD') : undefined
    }
    if (editingCoupon.value) {
      await couponApi.update(editingCoupon.value.id, payload)
      message.success('更新成功')
    } else {
      await couponApi.create(payload)
      message.success('发放成功')
    }
    modalVisible.value = false
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    modalLoading.value = false
  }
}

async function handleUse(record: any) {
  try { await couponApi.use(record.id); message.success('核销成功'); reportRef.value?.reload() }
  catch (e) { console.warn('[优惠券] 核销失败', e) }
}

async function handleDelete(record: any) {
  try { await couponApi.remove(record.id); message.success('删除成功'); reportRef.value?.reload() }
  catch (e) { console.warn('[优惠券] 删除失败', e) }
}

onMounted(async () => {
  try { programs.value = await loyaltyProgramApi.list() } catch (e) { console.warn('[优惠券] 程序列表获取失败', e) }
  try { const res = await memberApi.list({ pageNum: 1, pageSize: 200 }); members.value = (res as any).records || res || [] } catch (e) { /* ignore */ }
})
</script>
