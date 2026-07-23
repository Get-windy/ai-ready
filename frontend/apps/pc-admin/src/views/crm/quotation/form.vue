<template>
  <PageContainer full-height>
    <template #header>
      <div class="quotation-form-header">
        <div class="quotation-form-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/">首页</router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>
              <router-link to="/crm/quotation">报价管理</router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>{{ isEdit ? '编辑报价' : '新建报价' }}</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="quotation-form-header-title">
            {{ isEdit ? '编辑报价' : '新建报价' }}
          </h2>
        </div>
        <div class="quotation-form-header-right">
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
                  <a-form-item label="报价单号" name="quotationNo">
                    <a-input v-model:value="formData.quotationNo" placeholder="自动生成" disabled size="small" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="报价名称" name="quotationName">
                    <a-input v-model:value="formData.quotationName" placeholder="请输入报价名称" size="small" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="报价日期" name="quotationDate">
                    <a-date-picker v-model:value="formData.quotationDate" style="width: 100%" size="small" />
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
                  <a-form-item label="联系人" name="contactPerson">
                    <a-input v-model:value="formData.contactPerson" placeholder="请输入联系人" size="small" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="联系电话" name="contactPhone">
                    <a-input v-model:value="formData.contactPhone" placeholder="请输入联系电话" size="small" />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="24">
                <a-col :span="8">
                  <a-form-item label="有效期(天)" name="validDays">
                    <a-input-number v-model:value="formData.validDays" :min="1" style="width: 100%" size="small" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="币种" name="currency">
                    <a-select v-model:value="formData.currency" placeholder="请选择币种" size="small">
                      <a-select-option value="CNY">人民币(CNY)</a-select-option>
                      <a-select-option value="USD">美元(USD)</a-select-option>
                      <a-select-option value="EUR">欧元(EUR)</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="关联商机">
                    <a-select v-model:value="formData.opportunityId" placeholder="请选择商机" allow-clear show-search :filter-option="filterOption" size="small">
                      <a-select-option v-for="o in opportunityList" :key="o.id" :value="o.id">{{ o.name }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>
            </a-form>
          </a-card>

          <!-- 报价明细 -->
          <a-card title="报价明细" size="small" class="form-section">
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
              <template #productSelector="{ record, index }">
                <a-select v-model:value="record.productId" placeholder="选择产品" show-search :filter-option="filterOption" size="small" style="width: 100%" @change="(val: number) => onProductSelect(index, val)">
                  <a-select-option v-for="p in productList" :key="p.id" :value="p.id">{{ p.name }} ({{ p.spec || '-' }})</a-select-option>
                </a-select>
              </template>
              <template #quantityCell="{ record, index }">
                <a-input-number v-model:value="record.quantity" :min="1" style="width: 80px" size="small" @change="recalcItem(index)" />
              </template>
              <template #unitCell="{ record }">
                <a-input v-model:value="record.unit" placeholder="单位" style="width: 60px" size="small" />
              </template>
              <template #priceCell="{ record, index }">
                <a-input-number v-model:value="record.price" :min="0" :precision="2" style="width: 100px" size="small" @change="recalcItem(index)" />
              </template>
              <template #discountCell="{ record, index }">
                <a-input-number v-model:value="record.discount" :min="0" :max="100" :precision="2" style="width: 80px" size="small" @change="recalcItem(index)" />
              </template>
              <template #subtotalCell="{ record }">
                <span class="amount-cell">¥{{ formatAmount(record.subtotal) }}</span>
              </template>
              <template #action="{ index }">
                <a-button v-if="formData.items.length > 1" type="link" danger size="small" @click="removeItem(index)">删除</a-button>
              </template>
            </BillTableList>
            <a-button type="dashed" block style="margin-top: 12px" @click="addItem">
              <template #icon><PlusOutlined /></template>添加产品
            </a-button>
          </a-card>

          <!-- 费用汇总 -->
          <a-card title="费用汇总" size="small" class="form-section">
            <a-row :gutter="24" class="summary-row">
              <a-col :span="6">
                <div class="summary-item">
                  <span class="summary-label">产品金额</span>
                  <span class="summary-value">¥{{ formatAmount(calcTotalAmount()) }}</span>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="summary-item">
                  <span class="summary-label">折扣金额</span>
                  <span class="summary-value discount">¥{{ formatAmount(calcDiscountAmount()) }}</span>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="summary-item total">
                  <span class="summary-label">报价总额</span>
                  <span class="summary-value">¥{{ formatAmount(calcGrandTotal()) }}</span>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="summary-item">
                  <span class="summary-label">产品数量</span>
                  <span class="summary-value">{{ formData.items.reduce((s, i) => s + (i.quantity || 0), 0) }}</span>
                </div>
              </a-col>
            </a-row>
          </a-card>

          <!-- 附加信息 -->
          <a-card title="附加信息" size="small" class="form-section">
            <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
              <a-form-item label="报价条款">
                <a-textarea v-model:value="formData.terms" placeholder="请输入报价条款" :rows="3" size="small" />
              </a-form-item>
              <a-form-item label="备注">
                <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" size="small" />
              </a-form-item>
            </a-form>
          </a-card>

          <!-- 审批流程 -->
          <a-card title="审批流程" size="small" class="form-section">
            <a-timeline>
              <a-timeline-item v-for="(step, idx) in approvalSteps" :key="idx" :color="step.color">
                <template #dot v-if="step.icon">
                  <component :is="step.icon" />
                </template>
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
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { quotationApi } from '@/api/erp'
import { crmCustomerApi, opportunityApi } from '@/api/crm'
import { productApi } from '@/api/erp/product'
import dayjs from 'dayjs'
import { PlusOutlined, CheckCircleOutlined, SyncOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'

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
  quotationNo: '',
  quotationName: '',
  customerId: undefined as number | undefined,
  contactPerson: '',
  contactPhone: '',
  quotationDate: undefined as any,
  validDays: 30,
  currency: 'CNY',
  opportunityId: undefined as number | undefined,
  terms: '',
  remark: '',
  status: 'draft' as string,
  items: [] as any[]
})

const formRules: Record<string, any> = {
  quotationName: [{ required: true, message: '请输入报价名称' }],
  customerId: [{ required: true, message: '请选择客户', type: 'number' }],
  quotationDate: [{ required: true, message: '请选择报价日期' }]
}

const customerList = ref<any[]>([])
const productList = ref<any[]>([])
const opportunityList = ref<any[]>([])
const approvalSteps = ref<any[]>([])

const itemColumns = [
  { field: 'productName', title: '产品', width: 200, slotName: 'productSelector' },
  { field: 'spec', title: '规格', width: 100 },
  { field: 'quantity', title: '数量', width: 80, align: 'right', slotName: 'quantityCell' },
  { field: 'unit', title: '单位', width: 60, align: 'center', slotName: 'unitCell' },
  { field: 'price', title: '单价', width: 100, align: 'right', slotName: 'priceCell' },
  { field: 'discount', title: '折扣%', width: 80, align: 'right', slotName: 'discountCell' },
  { field: 'subtotal', title: '小计', width: 100, align: 'right', slotName: 'subtotalCell' },
  { field: 'action', title: '操作', width: 60, align: 'center', type: 'action' }
]

const filterOption = (input: string, option: any) => {
  return option?.children?.toLowerCase()?.includes(input.toLowerCase()) ?? false
}

function formatAmount(val: number | undefined | null): string {
  return (val ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function calcItemSubtotal(item: any): number {
  const qty = item.quantity || 0
  const price = item.price || 0
  const d = item.discount || 0
  return qty * price * (1 - d / 100)
}

function recalcItem(index: number) {
  const item = formData.items[index]
  item.subtotal = calcItemSubtotal(item)
}

function calcTotalAmount(): number {
  return formData.items.reduce((s, item) => s + calcItemSubtotal(item), 0)
}

function calcDiscountAmount(): number {
  return formData.items.reduce((s, item) => {
    const qty = item.quantity || 0
    const price = item.price || 0
    const d = item.discount || 0
    return s + qty * price * (d / 100)
  }, 0)
}

function calcGrandTotal(): number {
  return calcTotalAmount()
}

function addItem() {
  formData.items.push({
    productId: undefined,
    productName: '',
    spec: '',
    quantity: 1,
    unit: '',
    price: 0,
    discount: 0,
    subtotal: 0
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
    item.price = product.salePrice || 0
    recalcItem(index)
  }
}

function onCustomerChange(customerId: number) {
  const customer = customerList.value.find(c => c.id === customerId)
  if (customer) {
    formData.contactPerson = formData.contactPerson || customer.contactPerson || ''
    formData.contactPhone = formData.contactPhone || customer.phone || ''
  }
}

function generateQuotationNo() {
  const now = new Date()
  formData.quotationNo = `QT${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`
}

async function loadCustomers() {
  try {
    const res = await crmCustomerApi.page({ pageNum: 1, pageSize: 200 })
    customerList.value = (res as any).records || (res as any).data?.records || []
  } catch (e) {
    console.warn('[报价表单] 加载客户列表失败', e)
    customerList.value = []
  }
}

async function loadProducts() {
  try {
    const res = await productApi.page({ pageNum: 1, pageSize: 500 })
    productList.value = (res as any).records || (res as any).data?.records || []
  } catch (e) {
    console.warn('[报价表单] 加载产品列表失败', e)
    productList.value = []
  }
}

async function loadOpportunities() {
  try {
    const res = await opportunityApi.page({ pageNum: 1, pageSize: 200 })
    opportunityList.value = (res as any).records || []
  } catch (e) {
    console.warn('[报价表单] 加载商机列表失败', e)
    opportunityList.value = []
  }
}

async function loadDetail(id: number) {
  pageLoading.value = true
  try {
    const res = await quotationApi.getById(id)
    const data = res as any
    isEdit.value = true
    Object.assign(formData, {
      id: data.id,
      quotationNo: data.quotationNo,
      quotationName: data.quotationName,
      customerId: data.customerId,
      contactPerson: data.contactPerson,
      contactPhone: data.contactPhone,
      quotationDate: data.quotationDate ? dayjs(data.quotationDate) : undefined,
      validDays: data.validDays ?? 30,
      currency: data.currency || 'CNY',
      opportunityId: data.opportunityId,
      terms: data.terms || '',
      remark: data.remark || '',
      status: data.status || 'draft',
      items: data.items?.map((i: any) => ({ ...i, subtotal: calcItemSubtotal(i) })) || []
    })
    // 加载审批步骤
    if (data.approvalRecords) {
      approvalSteps.value = data.approvalRecords.map((r: any) => ({
        title: r.stepName,
        desc: r.comment || '',
        time: r.createTime,
        color: r.status === 'approved' ? 'green' : r.status === 'rejected' ? 'red' : 'blue',
        icon: r.status === 'approved' ? CheckCircleOutlined : r.status === 'rejected' ? CloseCircleOutlined : SyncOutlined
      }))
    }
  } catch (e) {
    console.warn('[报价表单] 加载详情失败', e)
    message.error('加载报价信息失败')
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
      quotationDate: formData.quotationDate ? dayjs(formData.quotationDate).format('YYYY-MM-DD') : undefined,
      items: formData.items.map((item: any) => ({
        ...item,
        subtotal: calcItemSubtotal(item)
      }))
    }
    if (isEdit.value) {
      await quotationApi.update(formData.id!, payload)
    } else {
      await quotationApi.create(payload)
    }
    message.success(submit ? '提交成功' : '保存成功')
    router.push('/crm/quotation')
  } catch (e: any) {
    console.warn('[报价表单] 保存失败', e)
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  router.push('/crm/quotation')
}

onMounted(async () => {
  await Promise.all([loadCustomers(), loadProducts(), loadOpportunities()])
  const id = route.params.id || route.query.id
  if (id) {
    await loadDetail(Number(id))
  } else {
    generateQuotationNo()
    if (formData.items.length === 0) addItem()
  }
})
</script>

<style scoped>
.quotation-form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.quotation-form-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.quotation-form-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.quotation-form-header-right {
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
.summary-value.discount {
  color: #faad14;
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
