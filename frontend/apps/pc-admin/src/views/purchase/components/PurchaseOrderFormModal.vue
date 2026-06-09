<template>
  <a-modal
    :open="open"
    :title="editData ? '编辑采购订单' : '新建采购订单'"
    :width="900"
    :confirm-loading="loading"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 18 }"
    >
      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="订单号" name="orderNo">
            <a-input v-model:value="formData.orderNo" placeholder="系统自动生成" :disabled="true" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="供应商" name="supplierId">
            <a-select
              v-model:value="formData.supplierId"
              placeholder="请选择供应商"
              show-search
              :filter-option="filterOption"
              :loading="loadingOptions"
              @change="handleSupplierChange"
            >
              <a-select-option v-for="item in supplierOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="订单日期" name="orderDate">
            <a-date-picker
              v-model:value="formData.orderDate"
              placeholder="请选择订单日期"
              style="width: 100%"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="预计到货日期" name="expectedDate">
            <a-date-picker
              v-model:value="formData.expectedDate"
              placeholder="请选择预计到货日期"
              style="width: 100%"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="采购员" name="purchaserId">
            <a-select
              v-model:value="formData.purchaserId"
              placeholder="请选择采购员"
              show-search
              :filter-option="filterOption"
              :loading="loadingOptions"
            >
              <a-select-option v-for="item in userOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="付款方式" name="paymentMethod">
            <a-select v-model:value="formData.paymentMethod" placeholder="请选择付款方式">
              <a-select-option :value="1">预付全款</a-select-option>
              <a-select-option :value="2">货到付款</a-select-option>
              <a-select-option :value="3">分期付款</a-select-option>
              <a-select-option :value="4">月结</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="结算币种" name="currency">
            <a-select v-model:value="formData.currency" placeholder="请选择币种">
              <a-select-option value="CNY">人民币(CNY)</a-select-option>
              <a-select-option value="USD">美元(USD)</a-select-option>
              <a-select-option value="EUR">欧元(EUR)</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="税率(%)" name="taxRate">
            <a-input-number v-model:value="formData.taxRate" :min="0" :max="100" :step="1" :precision="2" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" :maxlength="500" show-count />
      </a-form-item>

      <a-divider>采购商品明细</a-divider>

      <div class="product-table-actions">
        <a-button type="primary" size="small" @click="handleAddItem">
          <template #icon><PlusOutlined /></template>添加商品
        </a-button>
        <a-button size="small" @click="handleImportItems">
          <template #icon><ImportOutlined /></template>批量导入
        </a-button>
      </div>

      <VxeTableList
        :columns="itemVxeColumns"
        :data-source="formData.items"
        :pagination="false"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #productIdCell="{ record, index }">
          <a-select
            v-model:value="record.productId"
            placeholder="请选择商品"
            show-search
            :filter-option="filterOption"
            style="width: 100%"
            :loading="loadingOptions"
            @change="(val: number) => handleProductChange(val, index)"
          >
            <a-select-option v-for="p in productOptions" :key="p.id" :value="p.id">
              {{ p.name }} ({{ p.code || p.productCode }})
            </a-select-option>
          </a-select>
        </template>
        <template #productCodeCell="{ record }">
          {{ record.productCode }}
        </template>
        <template #quantityCell="{ record }">
          <a-input-number v-model:value="record.quantity" :min="1" :max="99999" :step="1" style="width: 100%" />
        </template>
        <template #unitPriceCell="{ record }">
          <a-input-number v-model:value="record.unitPrice" :min="0" :step="0.01" :precision="2" style="width: 100%" />
        </template>
        <template #amountCell="{ record }">
          <span class="amount-text">¥{{ ((record.quantity || 0) * (record.unitPrice || 0)).toFixed(2) }}</span>
        </template>
        <template #unitCell="{ record }">
          {{ record.unit }}
        </template>
        <template #action="{ index }">
          <a-space>
            <a @click="handleCopyItem(index)">复制</a>
            <a-popconfirm title="确定删除？" @confirm="handleDeleteItem(index)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </VxeTableList>

      <div class="amount-summary">
        <a-row :gutter="16">
          <a-col :span="6"><a-statistic title="商品数量" :value="totalQuantity" /></a-col>
          <a-col :span="6"><a-statistic title="商品金额" :value="totalAmount" :precision="2" prefix="¥" /></a-col>
          <a-col :span="6"><a-statistic title="税额" :value="taxAmount" :precision="2" prefix="¥" /></a-col>
          <a-col :span="6"><a-statistic title="价税合计" :value="totalAmountWithTax" :precision="2" prefix="¥" /></a-col>
        </a-row>
      </div>
    </a-form>

    <!-- 批量导入弹窗 -->
    <a-modal v-model:open="importVisible" title="批量导入产品明细" :footer="null" width="520px">
      <a-upload-dragger name="file" :max-count="1" accept=".xlsx,.csv" :before-upload="(f: any) => { handleImportFile(f); return false }">
        <p class="ant-upload-drag-icon"><InboxOutlined /></p>
        <p>点击或拖拽文件上传</p>
        <p class="ant-upload-hint">支持 .xlsx, .csv 格式，表头需包含：产品编码、产品名称、数量、单价</p>
      </a-upload-dragger>
      <div style="margin-top:16px;text-align:right">
        <a-button @click="importVisible = false">关闭</a-button>
      </div>
    </a-modal>
  </a-modal>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseOrderFormModal' })

import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PlusOutlined, ImportOutlined, InboxOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { purchaseOrderApi } from '@/api/erp'
import optionsApi from '@/api/options'

interface OrderItem {
  id: string
  productId: number | undefined
  productCode: string
  productName: string
  quantity: number
  unitPrice: number
  unit: string
  remark: string
}

interface Props {
  open: boolean
  editData?: any
  isEdit?: boolean
  /** @deprecated 使用 editData 替代 */
  record?: any
}

const props = defineProps<Props>()
const emit = defineEmits<{ (e: 'update:open', val: boolean): void; (e: 'success'): void }>()

const loading = ref(false)
const loadingOptions = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  orderNo: '',
  supplierId: undefined as number | undefined,
  supplierName: '',
  orderDate: dayjs().format('YYYY-MM-DD'),
  expectedDate: '',
  purchaserId: undefined as number | undefined,
  purchaserName: '',
  paymentMethod: 2,
  currency: 'CNY',
  taxRate: 13,
  remark: '',
  items: [] as OrderItem[]
})

const formRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择订单日期', trigger: 'change' }],
  purchaserId: [{ required: true, message: '请选择采购员', trigger: 'change' }]
}

const itemColumns = [
  { title: '商品', dataIndex: 'productId', key: 'productId', width: 200 },
  { title: '编码', dataIndex: 'productCode', key: 'productCode', width: 100 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
  { title: '操作', type: 'action', width: 100, fixed: 'right' }
]

const supplierOptions = ref<any[]>([])
const userOptions = ref<any[]>([])
const productOptions = ref<any[]>([])
const importVisible = ref(false)

const totalQuantity = computed(() => formData.items.reduce((s, i) => s + (i.quantity || 0), 0))
const totalAmount = computed(() => formData.items.reduce((s, i) => s + (i.quantity || 0) * (i.unitPrice || 0), 0))
const taxAmount = computed(() => totalAmount.value * (formData.taxRate || 0) / 100)
const totalAmountWithTax = computed(() => totalAmount.value + taxAmount.value)

const filterOption = (input: string, option: any) => {
  const text = option?.label || option?.value?.toString() || ''
  return text.toLowerCase().includes(input.toLowerCase())
}

function handleSupplierChange(val: number) {
  const supplier = supplierOptions.value.find(s => s.id === val)
  if (supplier) formData.supplierName = supplier.name
}

function handleProductChange(val: number, index: number) {
  const product = productOptions.value.find(p => p.id === val)
  if (product && formData.items[index]) {
    formData.items[index].productCode = product.code || product.productCode || ''
    formData.items[index].productName = product.name || product.productName || ''
    formData.items[index].unit = product.unit || ''
    formData.items[index].unitPrice = product.purchasePrice || 0
  }
}

function handleAddItem() {
  formData.items.push({
    id: Date.now().toString(),
    productId: undefined,
    productCode: '',
    productName: '',
    quantity: 1,
    unitPrice: 0,
    unit: '',
    remark: ''
  })
}

function handleDeleteItem(index: number) {
  if (formData.items.length > 1) {
    formData.items.splice(index, 1)
  } else {
    message.warning('至少保留一条商品明细')
  }
}

function handleCopyItem(index: number) {
  const item = formData.items[index]
  if (item) {
    formData.items.splice(index + 1, 0, { ...item, id: Date.now().toString() })
  }
}

function handleImportItems() { importVisible.value = true }
function handleImportFile(_file: File) {
  // TODO: 实际解析 Excel/CSV 文件内容并导入产品明细
  const fileName = _file.name
  message.success(`已接收文件 "${fileName}"，请使用单品添加方式录入明细（批量导入功能尚在完善）`)
  importVisible.value = false
}

async function handleOk() {
  try {
    await formRef.value?.validate()
    if (formData.items.length === 0) {
      message.error('请至少添加一条商品明细')
      return
    }
    if (formData.items.some(item => !item.productId)) {
      message.error('请选择所有商品')
      return
    }
    loading.value = true
    const submitData: Record<string, any> = {
      supplierId: formData.supplierId,
      supplierName: formData.supplierName,
      orderDate: formData.orderDate,
      expectedDate: formData.expectedDate,
      purchaserId: formData.purchaserId,
      purchaserName: formData.purchaserName,
      paymentMethod: formData.paymentMethod,
      currency: formData.currency,
      taxRate: formData.taxRate,
      remark: formData.remark,
      totalAmount: Math.round(totalAmount.value * 100) / 100,
      taxAmount: Math.round(taxAmount.value * 100) / 100,
      totalAmountWithTax: Math.round(totalAmountWithTax.value * 100) / 100,
      items: formData.items.map(i => ({
        productId: i.productId,
        productCode: i.productCode,
        productName: i.productName,
        quantity: i.quantity,
        unitPrice: i.unitPrice,
        unit: i.unit,
        remark: i.remark
      }))
    }
    if (props.editData?.id) {
      await purchaseOrderApi.update(props.editData.id, submitData)
      message.success('更新成功')
    } else {
      await purchaseOrderApi.create(submitData)
      message.success('创建成功')
    }
    emit('success')
    updateOpen(false)
  } catch (error: any) {
    console.warn('[采购订单] 保存失败', error?.response?.data || error)
    const errMsg = error?.response?.data?.message || error?.message || '操作失败'
    message.error(errMsg)
  } finally {
    loading.value = false
  }
}

function handleCancel() { updateOpen(false) }

function updateOpen(val: boolean) {
  emit('update:open', val)
}

/** 从响应中安全提取数组数据（兼容标准包装和无包装响应） */
function extractArray(res: any): any[] {
  if (Array.isArray(res)) return res
  if (res?.data && Array.isArray(res.data)) return res.data
  return []
}

async function loadOptions() {
  loadingOptions.value = true
  try {
    const [suppliers, users, products] = await Promise.all([
      optionsApi.getSuppliers(),
      optionsApi.getUsers('purchaser'),
      optionsApi.getProducts()
    ])
    supplierOptions.value = extractArray(suppliers)
    userOptions.value = extractArray(users)
    productOptions.value = extractArray(products)
    if (supplierOptions.value.length === 0 || userOptions.value.length === 0 || productOptions.value.length === 0) {
      console.warn('[采购订单] 部分下拉数据为空，请检查后端接口')
    }
  } catch (error: any) {
    console.warn('[采购订单] 加载下拉选项失败，请检查后端接口:', error?.message)
    // 使用空数组，让用户自行判断是否后端故障
    if (supplierOptions.value.length === 0) supplierOptions.value = []
    if (userOptions.value.length === 0) userOptions.value = []
    if (productOptions.value.length === 0) productOptions.value = []
  } finally {
    loadingOptions.value = false
  }
}

watch(() => props.open, (val) => {
  if (val) {
    loadOptions()
    const data = props.editData || props.record
    if (data) {
      formData.orderNo = data.orderNo || 'PO' + dayjs().format('YYYYMMDDHHmmss')
      formData.supplierId = data.supplierId
      formData.supplierName = data.supplierName || ''
      formData.orderDate = data.orderDate || dayjs().format('YYYY-MM-DD')
      formData.expectedDate = data.expectedDate || data.expectedDeliveryDate || ''
      formData.purchaserId = data.purchaserId || data.buyerId
      formData.purchaserName = data.purchaserName || data.buyerName || ''
      formData.paymentMethod = data.paymentMethod ?? (props.isEdit ? undefined : 2)
      formData.currency = data.currency || 'CNY'
      formData.taxRate = data.taxRate ?? 13
      formData.remark = data.remark || ''
      formData.items = data.items?.length
        ? data.items.map((item: any) => ({
            id: Date.now().toString() + Math.random(),
            productId: item.productId,
            productCode: item.productCode || '',
            productName: item.productName || '',
            quantity: item.quantity || 1,
            unitPrice: item.unitPrice || 0,
            unit: item.unit || '',
            remark: item.remark || ''
          }))
        : [{ id: '1', productId: undefined, productCode: '', productName: '', quantity: 1, unitPrice: 0, unit: '', remark: '' }]
    } else {
      formData.orderNo = 'PO' + dayjs().format('YYYYMMDDHHmmss')
      formData.orderDate = dayjs().format('YYYY-MM-DD')
      formData.paymentMethod = 2
      formData.currency = 'CNY'
      formData.taxRate = 13
      formData.remark = ''
      formData.supplierId = undefined
      formData.supplierName = ''
      formData.expectedDate = ''
      formData.purchaserId = undefined
      formData.purchaserName = ''
      formData.items = [{ id: '1', productId: undefined, productCode: '', productName: '', quantity: 1, unitPrice: 0, unit: '', remark: '' }]
    }
  }
})
</script>

<style scoped>
.product-table-actions { margin-bottom: 16px; }
.amount-summary { margin-top: 16px; padding: 16px; background: var(--color-bg-layout); border-radius: var(--border-radius-lg); }
.amount-text { font-weight: 600; color: var(--color-primary); }
.danger { color: var(--color-danger); }
</style>
