<template>
  <div class="crm-contract">
    <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="tableData"
    :loading="loading"
    :pagination="pagination"
    :table-key="'crm-contract-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    :show-export="true"
    add-text="新建合同"
    @add="handleAdd"
    @refresh="fetchData"
    @search="handleSearch"
    @page-change="handlePageChange"
    @sort-change="handleSortChange"
    @filter-change="handleFilterChange"
    @export="handleExport"
  >
    <template #toolbar-actions>
    </template>

    <template #contractNo="{ record }">
      <a @click="handleView(record)">{{ record.contractNo }}</a>
    </template>
    <template #contractAmount="{ record }">
      <span class="amount">¥{{ formatAmount(record.contractAmount) }}</span>
    </template>
    <template #status="{ record }">
      <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
    </template>
    <template #action="{ record }">
      <a-space :size="4">
        <a-tooltip title="查看"><a-button type="link" size="small" @click="handleView(record)"><template #icon><EyeOutlined /></template></a-button></a-tooltip>
        <a-tooltip v-if="record.status === 0" title="编辑"><a-button type="link" size="small" @click="handleEdit(record)"><template #icon><EditOutlined /></template></a-button></a-tooltip>
        <a-tooltip v-if="record.status === 1" title="审批"><a-button type="link" size="small" @click="handleApprove(record)"><template #icon><CheckCircleOutlined /></template></a-button></a-tooltip>
        <a-tooltip v-if="record.status === 2" title="签订"><a-button type="link" size="small" @click="handleSign(record)"><template #icon><FileDoneOutlined /></template></a-button></a-tooltip>
        <a-tooltip v-if="record.status === 0" title="删除"><a-button type="link" danger size="small" @click="handleDeleteConfirm(record)"><template #icon><DeleteOutlined /></template></a-button></a-tooltip>
      </a-space>
    </template>
  </TableList>

  <a-modal v-model:open="modalVisible" :title="modalTitle" width="800px" :confirm-loading="submitLoading" @ok="handleSubmit" @cancel="handleModalCancel">
    <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="合同编号" name="contractNo"><a-input v-model:value="formData.contractNo" placeholder="自动生成" disabled /></a-form-item>
      <a-form-item label="合同名称" name="contractName"><a-input v-model:value="formData.contractName" placeholder="请输入合同名称" /></a-form-item>
      <a-form-item label="合同类型" name="contractType">
        <a-select v-model:value="formData.contractType" placeholder="请选择合同类型">
          <a-select-option value="sales">销售合同</a-select-option>
          <a-select-option value="purchase">采购合同</a-select-option>
          <a-select-option value="service">服务合同</a-select-option>
          <a-select-option value="lease">租赁合同</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="客户名称" name="customerId">
        <a-select v-model:value="formData.customerId" placeholder="请选择客户" show-search :filter-option="filterOption">
          <a-select-option v-for="c in customerList" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-row :gutter="16">
        <a-col :span="12"><a-form-item label="开始日期" name="startDate" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }"><a-date-picker v-model:value="formData.startDate" style="width:100%" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="结束日期" name="endDate" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }"><a-date-picker v-model:value="formData.endDate" style="width:100%" /></a-form-item></a-col>
      </a-row>
      <a-form-item label="合同金额" name="contractAmount"><a-input-number v-model:value="formData.contractAmount" :min="0" :precision="2" style="width:100%" /></a-form-item>
      <a-form-item label="付款方式" name="paymentMethod">
        <a-select v-model:value="formData.paymentMethod" placeholder="请选择付款方式">
          <a-select-option value="once">一次性付款</a-select-option>
          <a-select-option value="installment">分期付款</a-select-option>
          <a-select-option value="prepaid">预付款+尾款</a-select-option>
          <a-select-option value="monthly">月结</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="签订日期" name="signDate"><a-date-picker v-model:value="formData.signDate" style="width:100%" /></a-form-item>
      <a-form-item label="签订人" name="signPerson"><a-input v-model:value="formData.signPerson" placeholder="请输入签订人" /></a-form-item>
      <a-form-item label="合同条款" name="terms"><a-textarea v-model:value="formData.terms" placeholder="请输入合同主要条款" :rows="3" /></a-form-item>
      <a-form-item label="备注" name="remark"><a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" /></a-form-item>
    </a-form>
  </a-modal>

  <a-modal v-model:open="detailVisible" title="合同详情" width="800px" :footer="null">
    <a-descriptions :column="2" bordered>
      <a-descriptions-item label="合同编号">{{ contractDetail.contractNo }}</a-descriptions-item>
      <a-descriptions-item label="合同名称">{{ contractDetail.contractName }}</a-descriptions-item>
      <a-descriptions-item label="合同类型">{{ contractDetail.contractTypeLabel }}</a-descriptions-item>
      <a-descriptions-item label="客户名称">{{ contractDetail.customerName }}</a-descriptions-item>
      <a-descriptions-item label="开始日期">{{ contractDetail.startDate }}</a-descriptions-item>
      <a-descriptions-item label="结束日期">{{ contractDetail.endDate }}</a-descriptions-item>
      <a-descriptions-item label="合同金额"><span class="amount">¥{{ formatAmount(contractDetail.contractAmount) }}</span></a-descriptions-item>
      <a-descriptions-item label="付款方式">{{ contractDetail.paymentMethodLabel }}</a-descriptions-item>
      <a-descriptions-item label="签订日期">{{ contractDetail.signDate }}</a-descriptions-item>
      <a-descriptions-item label="签订人">{{ contractDetail.signPerson }}</a-descriptions-item>
      <a-descriptions-item label="合同状态"><a-tag :color="getStatusColor(contractDetail.status)">{{ getStatusText(contractDetail.status) }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ contractDetail.createTime }}</a-descriptions-item>
      <a-descriptions-item label="合同条款" :span="2">{{ contractDetail.terms }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ contractDetail.remark }}</a-descriptions-item>
    </a-descriptions>
    <a-divider>审批流程</a-divider>
    <a-steps :current="contractDetail.currentStep" status="process">
      <a-step title="提交申请" :description="contractDetail.creator" />
      <a-step title="部门主管审批" :description="contractDetail.departmentApprover" />
      <a-step title="财务审批" :description="contractDetail.financeApprover" />
      <a-step title="总经理审批" :description="contractDetail.generalApprover" />
    </a-steps>
  </a-modal>

  <a-modal v-model:open="signModalVisible" title="合同签订" width="500px" @ok="handleSignSubmit" @cancel="signModalVisible = false">
    <a-form :model="signForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="合同名称"><a-input :value="signForm.contractName" disabled /></a-form-item>
      <a-form-item label="签订日期" required><a-date-picker v-model:value="signForm.signDate" style="width:100%" placeholder="请选择签订日期" /></a-form-item>
      <a-form-item label="签订人" required><a-input v-model:value="signForm.signPerson" placeholder="请输入签订人姓名" /></a-form-item>
    </a-form>
  </a-modal>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined, FileDoneOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import type { FormInstance } from 'ant-design-vue'
import { contractApi } from '@/api/crm'
import { exportCsv } from '@/utils/exportCsv'

const tableRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const signModalVisible = ref(false)
const modalTitle = ref('新建合同')
const formRef = ref<FormInstance>()
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const tableData = ref<any[]>([])

const columns = [
  { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo', width: 150, sortable: true, slotName: 'contractNo' },
  { title: '合同名称', dataIndex: 'contractName', key: 'contractName', width: 200 },
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 150 },
  { title: '合同类型', dataIndex: 'contractTypeLabel', key: 'contractTypeLabel', width: 100 },
  { title: '合同金额', dataIndex: 'contractAmount', key: 'contractAmount', width: 120, slotName: 'contractAmount' },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 100, type: 'date' as const },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 100, type: 'date' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150, type: 'date' as const },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'contractNo', label: '合同编号', type: 'input' as const, placeholder: '输入合同编号' },
  { key: 'contractName', label: '合同名称', type: 'input' as const, placeholder: '输入合同名称' },
  { key: 'customerName', label: '客户名称', type: 'input' as const, placeholder: '输入客户名称' },
  { key: 'contractType', label: '合同类型', type: 'select' as const, options: [
    { label: '销售合同', value: 'sales' }, { label: '采购合同', value: 'purchase' },
    { label: '服务合同', value: 'service' }, { label: '租赁合同', value: 'lease' }
  ]},
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已审批', value: 2 },
    { label: '待签署', value: 3 }, { label: '已签署', value: 4 }, { label: '生效中', value: 5 },
    { label: '执行中', value: 6 }, { label: '已完成', value: 7 }, { label: '已终止', value: 8 },
    { label: '已过期', value: 9 }, { label: '已取消', value: 10 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'blue', 3: 'geekblue', 4: 'purple', 5: 'green', 6: 'green', 7: 'green', 8: 'red', 9: 'red', 10: 'red' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已审批', 3: '待签署', 4: '已签署', 5: '生效中', 6: '执行中', 7: '已完成', 8: '已终止', 9: '已过期', 10: '已取消' }

// 将前端字符串状态映射为后端数字状态
function mapStatusToBackend(status: string): number | undefined {
  const map: Record<string, number> = { draft: 0, pending: 1, approved: 2, signing: 3, signed: 4, effective: 5, executing: 6, completed: 7, terminated: 8, expired: 9, cancelled: 10 }
  return map[status]
}
// 将后端数字状态映射为前端字符串状态（用于筛选条件）
function mapStatusToFrontend(status: number): string {
  const map: Record<number, string> = { 0: 'draft', 1: 'pending', 2: 'approved', 3: 'signing', 4: 'signed', 5: 'effective', 6: 'executing', 7: 'completed', 8: 'terminated', 9: 'expired', 10: 'cancelled' }
  return map[status] || 'draft'
}

const summaryData = computed(() => {
  if (tableData.value.length === 0) return undefined
  const totalAmount = tableData.value.reduce((s, r) => s + (r.contractAmount || 0), 0)
  return [
    { label: '本页数量', value: tableData.value.length, type: 'default' as const },
    { label: '本页金额', value: `¥${totalAmount.toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`, type: 'primary' as const }
  ]
})

function getStatusColor(status: string): string { return statusColorMap[status] || 'default' }
function getStatusText(status: string): string { return statusTextMap[status] || status }
function formatAmount(amount: number): string { return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }

const formData = reactive({ id: undefined, contractNo: '', contractName: '', contractType: undefined, customerId: undefined, startDate: undefined, endDate: undefined, contractAmount: undefined, paymentMethod: undefined, signDate: undefined, signPerson: '', terms: '', remark: '', attachments: [] })
const formRules = { contractName: [{ required: true, message: '请输入合同名称' }], contractType: [{ required: true, message: '请选择合同类型' }], customerId: [{ required: true, message: '请选择客户' }], startDate: [{ required: true, message: '请选择开始日期' }], endDate: [{ required: true, message: '请选择结束日期' }], contractAmount: [{ required: true, message: '请输入合同金额' }] }
const customerList = ref([{ id: 1, name: '北京科技有限公司' }, { id: 2, name: '上海贸易公司' }, { id: 3, name: '广州制造企业' }])
const contractDetail = ref<any>({})
const signForm = reactive({ contractId: undefined, contractName: '', signDate: undefined as any, signPerson: '' })

const filterOption = (input: string, option: any) => option.name?.toLowerCase().includes(input.toLowerCase())

onMounted(() => fetchData())

function generateContractNo() {
  const now = new Date()
  const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0')
  formData.contractNo = `CT${now.getFullYear()}${String(now.getMonth()+1).padStart(2,'0')}${String(now.getDate()).padStart(2,'0')}${random}`
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchFilters.keyword) params.keyword = searchFilters.keyword
    if (searchFilters.status !== undefined && searchFilters.status !== null) params.status = Number(searchFilters.status)
    const res = await contractApi.page(params)
    const result = res as any
    const records = result.records || result.data?.records || []
    tableData.value = records.map((r: any) => ({
      ...r,
      contractTypeLabel: r.contractTypeLabel || r.contractType || ''
    }))
    pagination.total = result.total ?? result.data?.total ?? 0
  } catch { message.error('获取合同数据失败') }
  finally { loading.value = false }
}

function handleView(record: any) {
  contractDetail.value = { ...record, currentStep: 2, creator: '张三', departmentApprover: '李四', financeApprover: '王五', generalApprover: '赵六' }
  detailVisible.value = true
}
function handleEdit(record: any) { modalTitle.value = '编辑合同'; Object.assign(formData, record); modalVisible.value = true }
function handleAdd() { modalTitle.value = '新建合同'; generateContractNo(); modalVisible.value = true }

function handleApprove(record: any) {
  Modal.confirm({ title: '确认审批', content: `确定要审批合同 "${record.contractName}" 吗？`, okText: '确认审批', cancelText: '取消', centered: true, async onOk() {
    try { await contractApi.approve(record.id); message.success('审批成功'); fetchData() }
    catch { message.error('审批失败') }
  }})
}
function handleSign(record: any) { signForm.contractId = record.id; signForm.contractName = record.contractName; signForm.signDate = undefined; signForm.signPerson = record.signPerson || ''; signModalVisible.value = true }
async function handleSignSubmit() {
  if (!signForm.signDate) { message.warning('请选择签订日期'); return }
  if (!signForm.signPerson.trim()) { message.warning('请输入签订人'); return }
  try { await contractApi.sign(signForm.contractId!, 'manual'); message.success(`合同"${signForm.contractName}"签订成功`); signModalVisible.value = false; fetchData() }
  catch { message.error('签订失败') }
}
function handleDeleteConfirm(record: any) {
  Modal.confirm({ title: '确认删除', content: `确定要删除合同 "${record.contractName}" 吗？`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true, async onOk() {
    try { await contractApi.terminate(record.id, '删除'); message.success('删除成功'); fetchData() }
    catch { message.error('删除失败') }
  }})
}

async function handleSubmit() {
  try { await formRef.value?.validate() } catch { return }
  submitLoading.value = true
  try {
    const data = { ...formData }
    if (data.id) {
      await contractApi.update(data.id, data)
    } else {
      await contractApi.create(data)
    }
    message.success('保存成功'); modalVisible.value = false; fetchData()
  } catch { message.error('保存失败') }
  finally { submitLoading.value = false }
}
function handleModalCancel() { formRef.value?.resetFields(); modalVisible.value = false }

function handleExport() {
  const headers = ['合同编号', '合同名称', '客户名称', '合同类型', '合同金额', '开始日期', '结束日期', '状态', '创建时间']
  const rows = tableData.value.map((row: any) => [row.contractNo, row.contractName, row.customerName, row.contractTypeLabel, row.contractAmount, row.startDate, row.endDate, getStatusText(row.status), row.createTime])
  exportCsv(headers, rows, '合同')
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }
</script>

<style scoped lang="scss">
.amount { color: #f5222d; font-weight: 500; }
</style>
