<template>
  <PageContainer full-height>
    <template #header>
      <div class="invoice-form-header">
        <div class="invoice-form-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/">首页</router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>
              <router-link to="/crm/invoice">发票管理</router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>{{ isEdit ? '编辑发票' : '新建发票' }}</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="invoice-form-header-title">
            {{ isEdit ? '编辑发票' : '新建发票' }}
          </h2>
        </div>
        <div class="invoice-form-header-right">
          <a-space>
            <a-button @click="handleCancel">取消</a-button>
            <a-button :loading="saving" @click="handleSave">保存<span class="shortcut-hint">Ctrl+S</span></a-button>
            <a-button type="primary" :loading="saving" @click="handleSubmit">提交<span class="shortcut-hint">Ctrl+Enter</span></a-button>
          </a-space>
        </div>
      </div>
    </template>

    <ErrorBoundary @reset="fetchData">
      <div class="form-body">
        <a-spin :spinning="pageLoading">
          <!-- 基本信息 -->
          <a-card title="基本信息" size="small" class="form-section">
            <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-row :gutter="24">
                <a-col :span="8">
                  <a-form-item label="发票号码" name="invoiceNo">
                    <a-input v-model:value="formData.invoiceNo" placeholder="请输入发票号码" size="small" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="发票类型" name="invoiceType">
                    <a-select v-model:value="formData.invoiceType" placeholder="请选择发票类型" size="small">
                      <a-select-option value="special">增值税专用发票</a-select-option>
                      <a-select-option value="normal">增值税普通发票</a-select-option>
                      <a-select-option value="electronic">电子发票</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="开票日期" name="invoiceDate">
                    <a-date-picker v-model:value="formData.invoiceDate" style="width: 100%" size="small" />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="24">
                <a-col :span="8">
                  <a-form-item label="客户名称" name="customerId">
                    <a-select v-model:value="formData.customerId" placeholder="请选择客户" show-search :filter-option="filterOption" size="small" @change="onCustomerChange">
                      <a-select-option v-for="c in customerList" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="发票抬头" name="invoiceTitle">
                    <a-input v-model:value="formData.invoiceTitle" placeholder="请输入发票抬头" size="small" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="纳税人识别号" name="taxId">
                    <a-input v-model:value="formData.taxId" placeholder="请输入纳税人识别号" size="small" />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="24">
                <a-col :span="8">
                  <a-form-item label="关联订单" name="relatedOrders">
                    <a-select v-model:value="formData.relatedOrders" mode="multiple" placeholder="请选择关联订单" size="small">
                      <a-select-option v-for="o in orderList" :key="o.id" :value="o.id">{{ o.orderNo }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="开票人" name="issuer">
                    <a-input v-model:value="formData.issuer" placeholder="请输入开票人" size="small" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="发票方向">
                    <a-radio-group v-model:value="formData.direction" size="small">
                      <a-radio value="sales">销售发票</a-radio>
                      <a-radio value="purchase">采购发票</a-radio>
                    </a-radio-group>
                  </a-form-item>
                </a-col>
              </a-row>
            </a-form>
          </a-card>

          <!-- 发票明细 -->
          <a-card title="发票明细" size="small" class="form-section">
            <BillTableList
              :columns="itemColumns"
              :data-source="formData.items"
              :pagination="false as any"
              :show-toolbar="false"
              :selectable="false"
              :show-add="false"
              :show-search="false"
              :show-export="false"
              :show-batch-delete="false"
            >
              <template #productNameCell="{ record, index }">
                <a-select v-model:value="record.productId" placeholder="选择商品" show-search :filter-option="filterOption" size="small" style="width: 100%" @change="(val: number) => onProductSelect(index, val)">
                  <a-select-option v-for="p in productList" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
                </a-select>
              </template>
              <template #quantityCell="{ record, index }">
                <a-input-number v-model:value="record.quantity" :min="1" style="width: 80px" size="small" @change="recalcItem(index)" />
              </template>
              <template #unitPriceCell="{ record, index }">
                <a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" style="width: 100px" size="small" @change="recalcItem(index)" />
              </template>
              <template #taxRateCell="{ record }">
                <a-select v-model:value="record.taxRate" size="small" style="width: 80px">
                  <a-select-option value="13">13%</a-select-option>
                  <a-select-option value="9">9%</a-select-option>
                  <a-select-option value="6">6%</a-select-option>
                  <a-select-option value="3">3%</a-select-option>
                  <a-select-option value="0">0%</a-select-option>
                </a-select>
              </template>
              <template #amountCell="{ record }">
                <span class="amount-cell">¥{{ formatAmount(record.amount) }}</span>
              </template>
              <template #taxAmountCell="{ record }">
                <span class="amount-cell">¥{{ formatAmount(record.taxAmount) }}</span>
              </template>
              <template #action="{ index }">
                <a-button v-if="formData.items.length > 1" type="link" danger size="small" @click="removeItem(index)">删除</a-button>
              </template>
            </BillTableList>
            <a-button type="dashed" block style="margin-top: 12px" @click="addItem">
              <template #icon><PlusOutlined /></template>添加明细
            </a-button>
          </a-card>

          <!-- 金额汇总 -->
          <a-card title="金额汇总" size="small" class="form-section">
            <a-row :gutter="24" class="summary-row">
              <a-col :span="6">
                <div class="summary-item">
                  <span class="summary-label">发票金额(不含税)</span>
                  <span class="summary-value">¥{{ formatAmount(calcTotalAmount()) }}</span>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="summary-item">
                  <span class="summary-label">税额</span>
                  <span class="summary-value">¥{{ formatAmount(calcTotalTax()) }}</span>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="summary-item total">
                  <span class="summary-label">价税合计</span>
                  <span class="summary-value">¥{{ formatAmount(calcGrandTotal()) }}</span>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="summary-item">
                  <span class="summary-label">明细数量</span>
                  <span class="summary-value">{{ formData.items.length }}</span>
                </div>
              </a-col>
            </a-row>
          </a-card>

          <!-- 备注 -->
          <a-card title="备注" size="small" class="form-section">
            <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="3" size="small" />
          </a-card>

          <!-- 审批流程 -->
          <a-card title="审批流程" size="small" class="form-section">
            <a-timeline>
              <a-timeline-item v-for="(step, idx) in approvalSteps" :key="idx" :color="step.color">
                <div class="approval-step">
                  <div class="approval-step-title">{{ step.title }}</div>
                  <div class="approval-step-desc">{{ step.desc }}</div>
                  <div class="approval-step-time">{{ step.time }}</div>
                </div>
              </a-timeline-item>
              <a-timeline-item color="gray">
                <div class="approval-step">
                  <div class="approval-step-title">提交审批</div>
                  <div class="approval-step-desc">等待提交审批流程</div>
                </div>
              </a-timeline-item>
            </a-timeline>
          </a-card>
        </a-spin>
      </div>
    </ErrorBoundary>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { invoiceApi, crmCustomerApi } from '@/api/crm'
import { productApi } from '@/api/erp/product'
import dayjs from 'dayjs'
import { PlusOutlined } from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)
const pageLoading = ref(false)
const isEdit = ref(false)

async function fetchData() {
  const id = route.params.id || route.query.id
  if (id) {
    await loadDetail(Number(id))
  }
}

const formData = reactive({
  id: undefined as number | undefined,
  invoiceNo: '',
  invoiceType: undefined as string | undefined,
  invoiceDate: undefined as any,
  customerId: undefined as number | undefined,
  invoiceTitle: '',
  taxId: '',
  relatedOrders: [] as number[],
  issuer: '',
  direction: 'sales' as string,
  remark: '',
  status: 'draft' as string,
  items: [] as any[]
})

const formRules: Record<string, any> = {
  invoiceNo: [{ required: true, message: '请输入发票号码' }],
  invoiceType: [{ required: true, message: '请选择发票类型' }],
  invoiceDate: [{ required: true, message: '请选择开票日期' }],
  customerId: [{ required: true, message: '请选择客户', type: 'number' }]
}

const customerList = ref<any[]>([])
const productList = ref<any[]>([])
const orderList = ref<any[]>([])
const approvalSteps = ref<any[]>([])

const itemColumns = [
  { field: 'productName', title: '商品名称', width: 180, slotName: 'productNameCell' },
  { field: 'spec', title: '规格型号', width: 100 },
  { field: 'unit', title: '单位', width: 60, align: 'center' },
  { field: 'quantity', title: '数量', width: 80, align: 'right', slotName: 'quantityCell' },
  { field: 'unitPrice', title: '单价', width: 100, align: 'right', slotName: 'unitPriceCell' },
  { field: 'amount', title: '金额', width: 100, align: 'right', slotName: 'amountCell' },
  { field: 'taxRate', title: '税率', width: 80, align: 'center', slotName: 'taxRateCell' },
  { field: 'taxAmount', title: '税额', width: 100, align: 'right', slotName: 'taxAmountCell' },
  { field: 'action', title: '操作', width: 60, align: 'center', type: 'action' }
]

const filterOption = (input: string, option: any) => {
  return option?.children?.toLowerCase()?.includes(input.toLowerCase()) ?? false
}

function formatAmount(val: number | undefined | null): string {
  return (val ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function calcItemAmount(item: any): number {
  return (item.quantity || 0) * (item.unitPrice || 0)
}

function calcItemTax(item: any): number {
  const amount = calcItemAmount(item)
  const rate = parseFloat(item.taxRate || '0') / 100
  return amount * rate
}

function recalcItem(index: number) {
  const item = formData.items[index]
  item.amount = calcItemAmount(item)
  item.taxAmount = calcItemTax(item)
}

function calcTotalAmount(): number {
  return formData.items.reduce((s, item) => s + calcItemAmount(item), 0)
}

function calcTotalTax(): number {
  return formData.items.reduce((s, item) => s + calcItemTax(item), 0)
}

function calcGrandTotal(): number {
  return calcTotalAmount() + calcTotalTax()
}

function addItem() {
  formData.items.push({
    productId: undefined,
    productName: '',
    spec: '',
    unit: '',
    quantity: 1,
    unitPrice: 0,
    amount: 0,
    taxRate: '13',
    taxAmount: 0
  })
}

function removeItem(index: number) {
  formData.items.splice(index, 1)
}

function onProductSelect(index: number, productId: number) {
  const product = productList.value.find(p => p.id === productId)
  if (product) {
    const item = formData.items[index]
    item.productName = product.name
    item.spec = product.spec || ''
    item.unit = product.unit || ''
    item.unitPrice = product.salePrice || 0
    recalcItem(index)
  }
}

function onCustomerChange(customerId: number) {
  const customer = customerList.value.find(c => c.id === customerId)
  if (customer) {
    formData.invoiceTitle = formData.invoiceTitle || customer.name || ''
    formData.taxId = formData.taxId || customer.taxId || ''
  }
}

async function loadCustomers() {
  try {
    const res = await crmCustomerApi.page({ pageNum: 1, pageSize: 200 })
    customerList.value = (res as any).records || (res as any).data?.records || []
  } catch (e) {
    console.warn('[发票表单] 加载客户列表失败', e)
    customerList.value = []
  }
}

async function loadProducts() {
  try {
    const res = await productApi.page({ pageNum: 1, pageSize: 500 })
    productList.value = (res as any).records || (res as any).data?.records || []
  } catch (e) {
    console.warn('[发票表单] 加载产品列表失败', e)
    productList.value = []
  }
}

async function loadOrders() {
  orderList.value = [
    { id: 1, orderNo: 'SO20240115001', amount: 58000 },
    { id: 2, orderNo: 'SO20240115002', amount: 32500 },
    { id: 3, orderNo: 'SO20240114003', amount: 128000 }
  ]
}

async function loadDetail(id: number) {
  pageLoading.value = true
  try {
    const res = await invoiceApi.getById(id)
    const data = res as any
    isEdit.value = true
    Object.assign(formData, {
      id: data.id,
      invoiceNo: data.invoiceNo,
      invoiceType: data.invoiceType,
      invoiceDate: data.invoiceDate ? dayjs(data.invoiceDate) : undefined,
      customerId: data.customerId,
      invoiceTitle: data.invoiceTitle || '',
      taxId: data.taxId || '',
      relatedOrders: data.relatedOrders || [],
      issuer: data.issuer || '',
      direction: data.direction || 'sales',
      remark: data.remark || '',
      status: data.status || 'draft',
      items: data.items?.map((i: any) => ({ ...i, amount: calcItemAmount(i), taxAmount: calcItemTax(i) })) || []
    })
    if (data.approvalRecords) {
      approvalSteps.value = data.approvalRecords.map((r: any) => ({
        title: r.stepName,
        desc: r.comment || '',
        time: r.createTime,
        color: r.status === 'approved' ? 'green' : r.status === 'rejected' ? 'red' : 'blue'
      }))
    }
  } catch (e) {
    console.warn('[发票表单] 加载详情失败', e)
    message.error('加载发票信息失败')
  } finally {
    pageLoading.value = false
  }
}

async function handleSave() {
  await doSubmit(false)
}

async function handleSubmit() {
  await doSubmit(true)
}

async function doSubmit(submit: boolean) {
  try {
    await formRef.value?.validate()
  } catch {
    message.warning('请完善表单信息')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...formData,
      amount: calcTotalAmount(),
      taxAmount: calcTotalTax(),
      totalAmount: calcGrandTotal(),
      invoiceDate: formData.invoiceDate ? dayjs(formData.invoiceDate).format('YYYY-MM-DD') : undefined,
      items: formData.items.map((item: any) => ({
        ...item,
        amount: calcItemAmount(item),
        taxAmount: calcItemTax(item)
      }))
    }
    if (isEdit.value) {
      await invoiceApi.update(formData.id!, payload)
    } else {
      await invoiceApi.create(payload)
    }
    message.success(submit ? '提交成功' : '保存成功')
    router.push('/crm/invoice')
  } catch (e: any) {
    console.warn('[发票表单] 保存失败', e)
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  router.push('/crm/invoice')
}

onMounted(async () => {
  await Promise.all([loadCustomers(), loadProducts(), loadOrders()])
  const id = route.params.id || route.query.id
  if (id) {
    await loadDetail(Number(id))
  } else {
    if (formData.items.length === 0) addItem()
  }
})
</script>

<style scoped>
.invoice-form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.invoice-form-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.invoice-form-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.invoice-form-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.form-body {
  padding: 16px;
  overflow-y: auto;
}
.form-section {
  margin-bottom: 16px;
}
.summary-row {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}
.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.summary-label {
  font-size: 14px;
  color: #666;
}
.summary-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}
.summary-item.total .summary-value {
  color: #f5222d;
}
.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}
.approval-step-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.approval-step-desc {
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
}
.approval-step-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
  font-size: 11px;
  margin-left: 4px;
}
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px; line-height: 28px;
}
</style>
