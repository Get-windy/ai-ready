<template>
  <FullScreenDetail
    :visible="visible"
    :title="isEdit ? '编辑费用报销' : (isView ? '费用报销详情' : '新增费用报销')"
    :save-loading="saveLoading"
    :dirty="isDirty"
    :show-save-and-new="false"
    @close="handleClose"
    @save="handleSave"
  >
    <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }" size="small">
      <a-divider>基本信息</a-divider>
      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="报销标题" name="reimbursementTitle">
            <a-input v-model:value="form.reimbursementTitle" :disabled="isView" placeholder="请输入报销标题" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="报销日期" name="reimbursementDate">
            <a-date-picker v-model:value="form.reimbursementDate" :disabled="isView" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="关联申请单">
            <a-input v-model:value="form.applicationNo" :disabled="true" placeholder="可选关联费用申请" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="部门">
            <a-input v-model:value="form.departmentName" :disabled="true" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="报销事由">
        <a-textarea v-model:value="form.purpose" :disabled="isView" :rows="2" placeholder="请输入报销事由" />
      </a-form-item>
      <a-form-item label="详细说明">
        <a-textarea v-model:value="form.description" :disabled="isView" :rows="3" placeholder="请输入详细说明" />
      </a-form-item>

      <a-divider>报销明细</a-divider>
      <a-button v-if="!isView" size="small" type="dashed" style="width: 100%; margin-bottom: 12px" @click="addItem">
        <PlusOutlined /> 添加明细
      </a-button>
      <a-table
        :data-source="form.items"
        :columns="itemColumns"
        :pagination="false as any"
        size="small"
        row-key="key"
        :scroll="{ x: 800 }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'itemName'">
            <a-input v-model:value="record.itemName" :disabled="isView" size="small" placeholder="项目名称" />
          </template>
          <template v-else-if="column.dataIndex === 'amount'">
            <a-input-number v-model:value="record.amount" :disabled="isView" size="small" style="width: 100%" :min="0" :precision="2" />
          </template>
          <template v-else-if="column.dataIndex === 'expenseDate'">
            <a-date-picker v-model:value="record.expenseDate" :disabled="isView" size="small" style="width: 100%" />
          </template>
          <template v-else-if="column.dataIndex === 'vendorName'">
            <a-input v-model:value="record.vendorName" :disabled="isView" size="small" placeholder="收款方" />
          </template>
          <template v-else-if="column.dataIndex === 'invoiceNumber'">
            <a-input v-model:value="record.invoiceNumber" :disabled="isView" size="small" placeholder="发票号" />
          </template>
          <template v-else-if="column.dataIndex === '_action' && !isView">
            <a-button type="link" size="small" danger @click="form.items.splice(index, 1)">删除</a-button>
          </template>
        </template>
      </a-table>

      <a-divider v-if="!isView || (detailData?.approvalRecords?.length)">审批信息</a-divider>
      <a-descriptions v-if="detailData" :column="2" size="small" bordered>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColor(detailData.status)">{{ statusLabel(detailData.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="支付方式">{{ detailData.paymentMethod || '-' }}</a-descriptions-item>
        <a-descriptions-item v-if="detailData.approvalRecords?.length" label="审批记录" :span="2">
          <a-timeline>
            <a-timeline-item v-for="r in detailData.approvalRecords" :key="r.id">
              {{ r.approverName }} - {{ approvalActionLabel(r.approvalAction) }}: {{ r.approvalComment || '无意见' }}
              <br><small style="color: #999">{{ r.approvalTime }}</small>
            </a-timeline-item>
          </a-timeline>
        </a-descriptions-item>
      </a-descriptions>
    </a-form>
  </FullScreenDetail>
</template>

<script setup lang="ts">
defineOptions({ name: 'ExpenseReimbursementDetail' })

import { ref, reactive, watch, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { feeReimbursementApi, type FeeReimbursement } from '@/api/erp/expense'

const route = useRoute()
const router = useRouter()
const visible = ref(true)
const isEdit = ref(false)
const isView = ref(true)
const saveLoading = ref(false)
const isDirty = ref(false)
const formRef = ref()
const detailData = ref<FeeReimbursement | null>(null)
const id = computed(() => {
  const val = route.params.id
  return val && val !== 'create' ? Number(val) : NaN
})

const form = reactive({
  reimbursementTitle: '',
  reimbursementDate: undefined as any,
  applicationId: undefined as number | undefined,
  applicationNo: '',
  departmentName: '',
  departmentId: undefined as number | undefined,
  purpose: '',
  description: '',
  items: [] as any[]
})

const rules = {
  reimbursementTitle: [{ required: true, message: '请输入报销标题' }],
  reimbursementDate: [{ required: true, message: '请选择报销日期' }]
}

const itemColumns = [
  { dataIndex: 'itemName', title: '项目名称', width: 180 },
  { dataIndex: 'expenseDate', title: '日期', width: 130 },
  { dataIndex: 'amount', title: '金额', width: 120 },
  { dataIndex: 'vendorName', title: '收款方', width: 150 },
  { dataIndex: 'invoiceNumber', title: '发票号', width: 130 },
  { dataIndex: '_action', title: '操作', width: 60 }
]

function addItem() {
  form.items.push({ key: Date.now(), itemName: '', amount: 0, expenseDate: undefined, vendorName: '', invoiceNumber: '' })
}

function statusColor(s: string) { return ({ DRAFT: 'default', SUBMITTED: 'blue', APPROVING: 'orange', APPROVED: 'green', REJECTED: 'red' } as any)[s] || 'default' }
function statusLabel(s: string) { return ({ DRAFT: '草稿', SUBMITTED: '待审批', APPROVING: '审批中', APPROVED: '已通过', REJECTED: '已拒绝' } as any)[s] || s }
function approvalActionLabel(a: string) { return ({ SUBMIT: '提交', APPROVE: '通过', REJECT: '拒绝', RETURN: '退回' } as any)[a] || a }

let originalFormJson = ''
function trackDirty() { isDirty.value = JSON.stringify(form) !== originalFormJson }
watch(form, trackDirty, { deep: true })

async function loadData() {
  try {
    const res = await feeReimbursementApi.getById(id.value) as any
    detailData.value = res.data || res
    const d = detailData.value!
    form.reimbursementTitle = d.reimbursementTitle
    form.reimbursementDate = d.reimbursementDate ? dayjs(d.reimbursementDate) : undefined
    form.applicationId = d.applicationId
    form.applicationNo = d.applicationNo || ''
    form.departmentName = d.departmentName || ''
    form.departmentId = d.departmentId
    form.purpose = d.purpose || ''
    form.description = d.description || ''
    form.items = (d.items || []).map((item: any) => ({ key: item.id, ...item }))
    originalFormJson = JSON.stringify(form)
  } catch { message.error('加载详情失败') }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
    saveLoading.value = true
    const data = { ...form, reimbursementDate: form.reimbursementDate ? dayjs(form.reimbursementDate).format('YYYY-MM-DD') : undefined }
    data.items = form.items.map((item: any) => ({
      itemName: item.itemName, amount: item.amount,
      expenseDate: item.expenseDate ? dayjs(item.expenseDate).format('YYYY-MM-DD') : undefined,
      vendorName: item.vendorName, invoiceNumber: item.invoiceNumber
    }))
    if (isEdit.value) {
      await feeReimbursementApi.update(id.value, data)
      message.success('保存成功')
    } else {
      await feeReimbursementApi.create(data)
      message.success('创建成功')
    }
    visible.value = false
    router.back()
  } catch (e: any) {
    if (e?.errorFields) return
    message.error(e?.message || '操作失败')
  } finally { saveLoading.value = false }
}

function handleClose() {
  if (isDirty.value) {
    message.warning('有未保存的修改，请先保存')
    return
  }
  visible.value = false
  router.back()
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    if (!isView.value) handleSave()
  }
}

onMounted(() => {
  isEdit.value = route.query.edit === '1'
  isView.value = !isEdit.value && !!id.value
  if (id.value) loadData()
  document.addEventListener('keydown', handleKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>
