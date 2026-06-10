<template>
  <FullScreenDetail
    :visible="visible"
    :title="isEdit ? '编辑费用申请' : (isView ? '费用申请详情' : '新增费用申请')"
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
          <a-form-item label="申请标题" name="applicationTitle">
            <a-input v-model:value="form.applicationTitle" :disabled="isView" placeholder="请输入申请标题" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="费用类型" name="expenseType">
            <a-select v-model:value="form.expenseType" :disabled="isView" placeholder="请选择费用类型">
              <a-select-option v-for="t in expenseTypes" :key="t.value" :value="t.value">{{ t.label }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="申请日期" name="applyDate">
            <a-date-picker v-model:value="form.applyDate" :disabled="isView" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="部门">
            <a-input v-model:value="form.departmentName" :disabled="true" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="费用事由" name="purpose">
        <a-textarea v-model:value="form.purpose" :disabled="isView" :rows="2" placeholder="请输入费用事由" />
      </a-form-item>
      <a-form-item label="详细说明">
        <a-textarea v-model:value="form.description" :disabled="isView" :rows="3" placeholder="请输入详细说明" />
      </a-form-item>

      <a-divider>费用明细</a-divider>
      <a-button v-if="!isView" size="small" type="dashed" style="width: 100%; margin-bottom: 12px" @click="addItem">
        <PlusOutlined /> 添加明细
      </a-button>
      <a-table
        :data-source="form.items"
        :columns="itemColumns"
        :pagination="false"
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
        <a-descriptions-item label="当前审批人">{{ detailData.currentApproverName || '-' }}</a-descriptions-item>
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
defineOptions({ name: 'ExpenseApplicationDetail' })

import { ref, reactive, watch, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { FullScreenDetail } from '@/components'
import { feeApplicationApi, type FeeApplication } from '@/api/erp/expense'

const route = useRoute()
const router = useRouter()
const visible = ref(true)
const isEdit = ref(false)
const isView = ref(true)
const saveLoading = ref(false)
const isDirty = ref(false)
const formRef = ref()
const detailData = ref<FeeApplication | null>(null)

const id = computed(() => {
  const val = route.params.id
  return val && val !== 'create' ? Number(val) : NaN
})

const expenseTypes = [
  { value: 'TRAVEL', label: '差旅费' },
  { value: 'OFFICE_SUPPLIES', label: '办公用品' },
  { value: 'MEETING', label: '会议费' },
  { value: 'ENTERTAINMENT', label: '招待费' },
  { value: 'TRANSPORTATION', label: '交通费' },
  { value: 'COMMUNICATION', label: '通讯费' },
  { value: 'TRAINING', label: '培训费' },
  { value: 'CONSULTING', label: '咨询费' },
  { value: 'ADVERTISING', label: '广告费' },
  { value: 'R_D', label: '研发费' },
  { value: 'EQUIPMENT', label: '设备购置' },
  { value: 'MAINTENANCE', label: '维修费' },
  { value: 'RENTAL', label: '租赁费' },
  { value: 'INSURANCE', label: '保险费' },
  { value: 'TAX', label: '税费' },
  { value: 'OTHER', label: '其他' }
]

const form = reactive({
  applicationTitle: '',
  expenseType: undefined as string | undefined,
  applyDate: undefined as any,
  departmentName: '',
  departmentId: undefined as number | undefined,
  purpose: '',
  description: '',
  items: [] as any[]
})

const rules = {
  applicationTitle: [{ required: true, message: '请输入申请标题' }],
  expenseType: [{ required: true, message: '请选择费用类型' }],
  applyDate: [{ required: true, message: '请选择申请日期' }],
  purpose: [{ required: true, message: '请输入费用事由' }]
}

const itemColumns = [
  { dataIndex: 'itemName', title: '项目名称', width: 180 },
  { dataIndex: 'expenseDate', title: '日期', width: 130 },
  { dataIndex: 'amount', title: '金额', width: 120 },
  { dataIndex: 'vendorName', title: '收款方', width: 150 },
  { dataIndex: '_action', title: '操作', width: 60 }
]

function addItem() {
  form.items.push({
    key: Date.now(),
    itemName: '',
    amount: 0,
    expenseDate: undefined,
    vendorName: ''
  })
}

function statusColor(s: string) {
  const map: Record<string, string> = { DRAFT: 'default', SUBMITTED: 'blue', APPROVING: 'orange', APPROVED: 'green', REJECTED: 'red', CANCELLED: 'default', WITHDRAWN: 'default' }
  return map[s] || 'default'
}
function statusLabel(s: string) {
  const map: Record<string, string> = { DRAFT: '草稿', SUBMITTED: '待审批', APPROVING: '审批中', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消', WITHDRAWN: '已撤回' }
  return map[s] || s
}
function approvalActionLabel(a: string) {
  const map: Record<string, string> = { SUBMIT: '提交', APPROVE: '通过', REJECT: '拒绝', RETURN: '退回', TRANSFER: '转交', WITHDRAW: '撤回', CANCEL: '取消' }
  return map[a] || a
}

let originalFormJson = ''

function trackDirty() {
  isDirty.value = JSON.stringify(form) !== originalFormJson
}

watch(form, trackDirty, { deep: true })

async function loadData() {
  try {
    const res = await feeApplicationApi.getById(id.value) as any
    detailData.value = res.data || res
    const d = detailData.value!
    form.applicationTitle = d.applicationTitle
    form.expenseType = d.expenseType
    form.applyDate = d.applyDate ? dayjs(d.applyDate) : undefined
    form.departmentName = d.departmentName || ''
    form.departmentId = d.departmentId
    form.purpose = d.purpose
    form.description = d.description || ''
    form.items = (d.items || []).map((item: any) => ({ key: item.id, ...item }))
    originalFormJson = JSON.stringify(form)
  } catch (e) { message.error('加载详情失败') }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
    saveLoading.value = true
    const data = { ...form, applyDate: form.applyDate ? dayjs(form.applyDate).format('YYYY-MM-DD') : undefined }
    data.items = form.items.map((item: any) => ({
      itemName: item.itemName,
      amount: item.amount,
      expenseDate: item.expenseDate ? dayjs(item.expenseDate).format('YYYY-MM-DD') : undefined,
      vendorName: item.vendorName
    }))
    if (isEdit.value) {
      await feeApplicationApi.update(id.value, data)
      message.success('保存成功')
    } else {
      await feeApplicationApi.create(data)
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
  const editParam = route.query.edit
  isEdit.value = editParam === '1'
  isView.value = !isEdit.value && !!id.value
  if (id.value) loadData()
  document.addEventListener('keydown', handleKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>
