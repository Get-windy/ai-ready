<template>
  <div class="crm-quotation-page">
    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :table-key="'crm-quotation-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :show-export="true"
      add-text="新建报价"
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

      <template #quotationNo="{ record }">
        <a @click="handleView(record)">{{ record.quotationNo }}</a>
      </template>
      <template #totalAmount="{ record }">
        <span class="amount">¥{{ formatAmount(record.totalAmount) }}</span>
      </template>
      <template #status="{ record }">
        <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
      </template>
      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看"><a-button type="link" size="small" @click="handleView(record)"><template #icon><EyeOutlined /></template></a-button></a-tooltip>
          <a-tooltip v-if="record.status === 'draft'" title="编辑"><a-button type="link" size="small" @click="handleEdit(record)"><template #icon><EditOutlined /></template></a-button></a-tooltip>
          <a-tooltip v-if="record.status === 'draft'" title="发送"><a-button type="link" size="small" @click="handleSend(record)"><template #icon><SendOutlined /></template></a-button></a-tooltip>
          <a-tooltip v-if="record.status === 'accepted'" title="转订单"><a-button type="link" size="small" @click="handleConvert(record)"><template #icon><FileProtectOutlined /></template></a-button></a-tooltip>
          <a-tooltip title="复制"><a-button type="link" size="small" @click="handleCopy(record)"><template #icon><CopyOutlined /></template></a-button></a-tooltip>
          <a-tooltip v-if="record.status === 'draft'" title="删除"><a-button type="link" danger size="small" @click="handleDeleteConfirm(record)"><template #icon><DeleteOutlined /></template></a-button></a-tooltip>
        </a-space>
      </template>
    </TableList>

    <a-modal v-model:open="modalVisible" :title="modalTitle" width="900px" :confirm-loading="submitLoading" @ok="handleSubmit" @cancel="handleModalCancel">
      <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="报价单号" name="quotationNo" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }"><a-input v-model:value="formData.quotationNo" placeholder="自动生成" disabled /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="报价日期" name="quotationDate" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }"><a-date-picker v-model:value="formData.quotationDate" style="width:100%" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="报价名称" name="quotationName" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }"><a-input v-model:value="formData.quotationName" placeholder="请输入报价名称" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="有效期(天)" name="validDays" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }"><a-input-number v-model:value="formData.validDays" :min="1" style="width:100%" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="客户名称" name="customerId" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-select v-model:value="formData.customerId" placeholder="请选择客户" show-search :filter-option="filterOption">
              <a-select-option v-for="c in customerList" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
            </a-select>
          </a-form-item></a-col>
          <a-col :span="12"><a-form-item label="币种" name="currency" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-select v-model:value="formData.currency" placeholder="请选择币种">
              <a-select-option value="CNY">人民币(CNY)</a-select-option>
              <a-select-option value="USD">美元(USD)</a-select-option>
              <a-select-option value="EUR">欧元(EUR)</a-select-option>
            </a-select>
          </a-form-item></a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="联系人" name="contactPerson" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }"><a-input v-model:value="formData.contactPerson" placeholder="请输入联系人" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="联系电话" name="contactPhone" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }"><a-input v-model:value="formData.contactPhone" placeholder="请输入联系电话" /></a-form-item></a-col>
        </a-row>

        <a-divider>报价明细</a-divider>
        <a-table :columns="itemColumns" :data-source="formData.items" :pagination="false" size="small">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'productName'"><a-input v-model:value="record.productName" placeholder="产品名称" /></template>
            <template v-if="column.key === 'spec'"><a-input v-model:value="record.spec" placeholder="规格型号" /></template>
            <template v-if="column.key === 'quantity'"><a-input-number v-model:value="record.quantity" :min="1" style="width:80px" /></template>
            <template v-if="column.key === 'unit'"><a-input v-model:value="record.unit" placeholder="单位" style="width:60px" /></template>
            <template v-if="column.key === 'price'"><a-input-number v-model:value="record.price" :min="0" :precision="2" style="width:100px" /></template>
            <template v-if="column.key === 'discount'"><a-input-number v-model:value="record.discount" :min="0" :max="100" style="width:80px" /></template>
            <template v-if="column.key === 'subtotal'"><span class="amount">¥{{ calcItemSubtotal(record) }}</span></template>
            <template v-if="column.key === 'action'"><a @click="removeItem(index)" v-if="formData.items.length > 1">删除</a></template>
          </template>
        </a-table>
        <a-button type="dashed" block @click="addItem" style="margin-top:16px"><template #icon><PlusOutlined /></template>添加产品</a-button>

        <a-divider>费用汇总</a-divider>
        <a-row :gutter="24">
          <a-col :span="8"><a-statistic title="产品金额" :value="calcTotalAmount()" :precision="2" prefix="¥" /></a-col>
          <a-col :span="8"><a-statistic title="折扣金额" :value="calcDiscountAmount()" :precision="2" prefix="¥" /></a-col>
          <a-col :span="8"><a-statistic title="报价总额" :value="calcGrandTotal()" :precision="2" prefix="¥" :value-style="{ color: '#f5222d' }" /></a-col>
        </a-row>
        <a-form-item label="报价条款" name="terms" style="margin-top:16px"><a-textarea v-model:value="formData.terms" placeholder="请输入报价条款" :rows="3" /></a-form-item>
        <a-form-item label="备注" name="remark"><a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="detailVisible" title="报价详情" width="900px" :footer="null">
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="报价单号">{{ quotationDetail.quotationNo }}</a-descriptions-item>
        <a-descriptions-item label="报价名称">{{ quotationDetail.quotationName }}</a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ quotationDetail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="联系人">{{ quotationDetail.contactPerson }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{ quotationDetail.contactPhone }}</a-descriptions-item>
        <a-descriptions-item label="报价日期">{{ quotationDetail.quotationDate }}</a-descriptions-item>
        <a-descriptions-item label="有效期">{{ quotationDetail.validDays }}天</a-descriptions-item>
        <a-descriptions-item label="币种">{{ quotationDetail.currency }}</a-descriptions-item>
        <a-descriptions-item label="报价总额"><span class="amount">¥{{ formatAmount(quotationDetail.totalAmount) }}</span></a-descriptions-item>
        <a-descriptions-item label="报价状态"><a-tag :color="getStatusColor(quotationDetail.status)">{{ getStatusText(quotationDetail.status) }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="报价条款" :span="2">{{ quotationDetail.terms }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ quotationDetail.remark }}</a-descriptions-item>
      </a-descriptions>
      <a-divider>报价明细</a-divider>
      <a-table :columns="detailItemColumns" :data-source="quotationDetail.items" :pagination="false" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'subtotal'"><span class="amount">¥{{ formatAmount(record.subtotal) }}</span></template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SendOutlined, CopyOutlined, FileProtectOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { quotationApi } from '@/api/erp'
import { exportCsv } from '@/utils/exportCsv'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

const tableRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const modalTitle = ref('新建报价')
const formRef = ref<FormInstance>()
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const tableData = ref<any[]>([])

const columns = [
  { title: '报价单号', dataIndex: 'quotationNo', key: 'quotationNo', width: 150, sortable: true, slotName: 'quotationNo' },
  { title: '报价名称', dataIndex: 'quotationName', key: 'quotationName', width: 200 },
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 150 },
  { title: '报价日期', dataIndex: 'quotationDate', key: 'quotationDate', width: 100, type: 'date' as const },
  { title: '有效期', dataIndex: 'validDays', key: 'validDays', width: 80 },
  { title: '报价总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, slotName: 'totalAmount' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150, type: 'date' as const },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'quotationNo', label: '报价单号', type: 'input' as const, placeholder: '输入报价单号' },
  { key: 'quotationName', label: '报价名称', type: 'input' as const, placeholder: '输入报价名称' },
  { key: 'customerName', label: '客户名称', type: 'input' as const, placeholder: '输入客户名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' }, { label: '已发送', value: 'sent' }, { label: '已接受', value: 'accepted' }, { label: '已拒绝', value: 'rejected' }, { label: '已过期', value: 'expired' }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<string, string> = { draft: 'default', sent: 'blue', accepted: 'green', rejected: 'red', expired: 'orange' }
const statusTextMap: Record<string, string> = { draft: '草稿', sent: '已发送', accepted: '已接受', rejected: '已拒绝', expired: '已过期' }

function getStatusColor(status: string): string { return statusColorMap[status] || 'default' }
function getStatusText(status: string): string { return statusTextMap[status] || status }
function formatAmount(amount: number): string { return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }

const summaryData = computed(() => {
  if (tableData.value.length === 0) return undefined
  const total = tableData.value.reduce((s, r) => s + (r.totalAmount || 0), 0)
  return [
    { label: '本页数量', value: tableData.value.length, type: 'default' as const },
    { label: '报价总额', value: `¥${total.toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`, type: 'primary' as const }
  ]
})

const filterOption = (input: string, option: any) => option.name?.toLowerCase().includes(input.toLowerCase())

const formData = reactive({
  id: undefined, quotationNo: '', quotationName: '', customerId: undefined, contactPerson: '', contactPhone: '',
  quotationDate: undefined, validDays: 30, currency: 'CNY', terms: '', remark: '',
  items: [{ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0 }]
})
const formRules = { quotationName: [{ required: true, message: '请输入报价名称' }], customerId: [{ required: true, message: '请选择客户' }], quotationDate: [{ required: true, message: '请选择报价日期' }] }
const customerList = ref([{ id: 1, name: '北京科技有限公司' }, { id: 2, name: '上海贸易公司' }, { id: 3, name: '广州制造企业' }])
const quotationDetail = ref<any>({})

const itemColumns = [
  { title: '产品名称', key: 'productName', width: 150 }, { title: '规格型号', key: 'spec', width: 100 },
  { title: '数量', key: 'quantity', width: 80 }, { title: '单位', key: 'unit', width: 60 },
  { title: '单价', key: 'price', width: 100 }, { title: '折扣%', key: 'discount', width: 80 },
  { title: '小计', key: 'subtotal', width: 100 }, { title: '操作', key: 'action', width: 60 }
]
const detailItemColumns = [
  { title: '产品名称', dataIndex: 'productName', width: 150 }, { title: '规格型号', dataIndex: 'spec', width: 100 },
  { title: '数量', dataIndex: 'quantity', width: 80 }, { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '单价', dataIndex: 'price', width: 100 }, { title: '折扣%', dataIndex: 'discount', width: 80 },
  { title: '小计', key: 'subtotal', dataIndex: 'subtotal', width: 100 }
]

function calcItemSubtotal(item: any) { const qty = item.quantity || 0; const price = item.price || 0; const d = item.discount || 0; return (qty * price * (1 - d / 100)).toFixed(2) }
function calcTotalAmount() { return formData.items.reduce((s, item) => s + parseFloat(calcItemSubtotal(item)), 0) }
function calcDiscountAmount() { return formData.items.reduce((s, item) => { const qty = item.quantity || 0; const price = item.price || 0; const d = item.discount || 0; return s + qty * price * (d / 100) }, 0) }
function calcGrandTotal() { return calcTotalAmount() }
function addItem() { formData.items.push({ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0 }) }
function removeItem(index: number) { formData.items.splice(index, 1) }

function generateQuotationNo() {
  const now = new Date()
  formData.quotationNo = `QT${now.getFullYear()}${String(now.getMonth()+1).padStart(2,'0')}${String(now.getDate()).padStart(2,'0')}${Math.floor(Math.random()*1000).toString().padStart(3,'0')}`
}

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await quotationApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    tableData.value = (res as any).records || []
    pagination.total = (res as any).total || 0
  } catch {
    message.error('获取数据失败')
  }
  finally { loading.value = false }
}

function handleView(record: any) {
  quotationDetail.value = {
    ...record,
    items: [
      { productName: '笔记本电脑', spec: '银色/16GB/512GB', quantity: 5, unit: '台', price: 8999, discount: 0, subtotal: 44995 },
      { productName: '无线鼠标', spec: '黑色', quantity: 10, unit: '个', price: 199, discount: 5, subtotal: 1890.5 },
      { productName: '机械键盘', spec: '青轴/白色', quantity: 5, unit: '个', price: 399, discount: 0, subtotal: 1995 }
    ],
    terms: '付款方式：预付30%，发货前付清余款\n交货期：下单后15个工作日内\n质保期：产品保修1年', remark: ''
  }
  detailVisible.value = true
}
function handleEdit(record: any) { modalTitle.value = '编辑报价'; Object.assign(formData, record); modalVisible.value = true }
function handleAdd() { modalTitle.value = '新建报价'; generateQuotationNo(); formData.items = [{ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0 }]; modalVisible.value = true }

async function handleSend(record: any) { try { await quotationApi.send(record.id); message.success('报价已发送'); fetchData() } catch { message.error('发送失败') } }
function handleConvert(record: any) {
  Modal.confirm({ title: '确认转订单', content: `确定要将报价 "${record.quotationName}" 转为销售订单吗？`, okText: '确认转换', cancelText: '取消', centered: true, async onOk() { try { await quotationApi.convertToOrder(record.id); message.success('报价已成功转为订单'); fetchData() } catch { message.error('转换失败') } } })
}
function handleCopy(record: any) {
  Modal.confirm({ title: '复制报价', content: `确定要复制报价 "${record.quotationName}" 吗？`, okText: '确认复制', cancelText: '取消', centered: true, onOk() { modalTitle.value = '新建报价（复制）'; generateQuotationNo(); Object.assign(formData, { ...record, id: undefined, quotationNo: formData.quotationNo, items: (record.items || [{ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0 }]).map((i: any) => ({ ...i })) }); modalVisible.value = true } })
}
function handleDeleteConfirm(record: any) {
  Modal.confirm({ title: '确认删除', content: `确定要删除报价 "${record.quotationName}" 吗？`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true, async onOk() { try { await quotationApi.delete(record.id); message.success('删除成功'); fetchData() } catch { message.error('删除失败') } } })
}

async function handleSubmit() {
  try { await formRef.value?.validate() } catch { return }
  submitLoading.value = true
  try {
    const payload = { ...formData, quotationDate: formData.quotationDate ? dayjs(formData.quotationDate).format('YYYY-MM-DD') : undefined }
    if (formData.id) { await quotationApi.update(formData.id, payload) } else { await quotationApi.create(payload) }
    message.success('保存成功'); modalVisible.value = false; fetchData()
  } catch (err: any) { message.error(err?.message || '保存失败') }
  finally { submitLoading.value = false }
}
function handleModalCancel() { formRef.value?.resetFields(); modalVisible.value = false }

function handleExport() {
  const headers = ['报价单号', '报价名称', '客户名称', '报价日期', '有效期', '报价总额', '状态', '创建时间']
  const rows = tableData.value.map((row: any) => [row.quotationNo, row.quotationName, row.customerName, row.quotationDate, row.validDays, row.totalAmount, getStatusText(row.status), row.createTime])
  exportCsv(headers, rows, '报价')
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }
</script>

<style scoped lang="scss">
.amount { color: #f5222d; font-weight: 500; }
</style>
