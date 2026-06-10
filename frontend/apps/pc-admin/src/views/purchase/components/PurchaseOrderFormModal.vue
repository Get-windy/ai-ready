<template>
  <FullScreenDetail
    :visible="open"
    :title="editData ? '编辑采购订单' : '新建采购订单'"
    :save-loading="loading"
    :show-save-and-new="!isEdit"
    @close="handleFormClose"
    @save="handleOk"
    @save-and-new="handleFormSaveAndNew"
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
            <a-input v-model:value="formData.orderNo" placeholder="系统自动生成" :disabled="true" size="small" />
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
              size="small"
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
              size="small"
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
              size="small"
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
              size="small"
            >
              <a-select-option v-for="item in userOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="付款方式" name="paymentMethod">
            <a-select v-model:value="formData.paymentMethod" placeholder="请选择付款方式" size="small">
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
            <a-select v-model:value="formData.currency" placeholder="请选择币种" size="small">
              <a-select-option value="CNY">人民币(CNY)</a-select-option>
              <a-select-option value="USD">美元(USD)</a-select-option>
              <a-select-option value="EUR">欧元(EUR)</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="税率(%)" name="taxRate">
            <a-input-number v-model:value="formData.taxRate" :min="0" :max="100" :step="1" :precision="2" style="width: 100%" size="small" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" :maxlength="500" show-count size="small" />
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
            size="small"
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
          <a-input-number v-model:value="record.quantity" :min="1" :max="99999" :step="1" style="width: 100%" size="small" />
        </template>
        <template #unitPriceCell="{ record }">
          <a-input-number v-model:value="record.unitPrice" :min="0" :step="0.01" :precision="2" style="width: 100%" size="small" />
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
  </FullScreenDetail>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseOrderFormModal' })

import { ref, reactive, computed, watch, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { FullScreenDetail } from '@/components'
import { PlusOutlined, ImportOutlined, InboxOutlined } from '@ant-design/icons-vue'
import * as XLSX from 'xlsx'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { purchaseOrderApi } from '@/api/erp'
import optionsApi from '@/api/options'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

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

// ── 表单脏状态跟踪 ──────────────────────────────────────
const initialFormSnapshot = ref<string>('')
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify({ ...formData, items: [...formData.items] })
}
const formDirty = computed(() => {
  if (!props.open) return false
  return JSON.stringify({ ...formData, items: [...formData.items] }) !== initialFormSnapshot.value
})

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
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const data = new Uint8Array(e.target?.result as ArrayBuffer)
      const workbook = XLSX.read(data, { type: 'array' })
      const firstSheetName = workbook.SheetNames[0]
      if (!firstSheetName) {
        message.warning('文件中没有可读取的工作表')
        return
      }
      const sheet = workbook.Sheets[firstSheetName]
      const rows: Record<string, any>[] = XLSX.utils.sheet_to_json(sheet, { defval: '' })

      if (rows.length === 0) {
        message.warning('文件中未读取到有效数据行')
        return
      }

      const newItems = rows.map((row: any) => ({
        id: Date.now().toString() + Math.random(),
        productId: undefined,
        productCode: (row['产品编码'] || row['productCode'] || row['编码'] || '').toString(),
        productName: (row['产品名称'] || row['productName'] || row['名称'] || '').toString(),
        quantity: Number(row['数量'] || row['quantity'] || 1),
        unitPrice: Number(row['单价'] || row['unitPrice'] || 0),
        unit: (row['单位'] || row['unit'] || '').toString(),
        remark: (row['备注'] || row['remark'] || '').toString()
      }))

      formData.items = newItems
      message.success(`成功导入 ${newItems.length} 条商品明细`)
    } catch (err) {
      console.warn('[导入] 解析文件失败', err)
      message.error('文件解析失败，请检查文件格式（支持 .xlsx / .csv）')
    }
    importVisible.value = false
  }
  reader.onerror = () => {
    message.error('文件读取失败')
    importVisible.value = false
  }
  reader.readAsArrayBuffer(_file)
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

function handleFormClose() {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '当前表单有未保存的内容，确定关闭吗？',
      onOk: () => { updateOpen(false) }
    })
  } else {
    updateOpen(false)
  }
}

async function handleFormSaveAndNew() {
  try {
    await formRef.value?.validate()
    if (formData.items.length === 0) { message.error('请至少添加一条商品明细'); return }
    if (formData.items.some(item => !item.productId)) { message.error('请选择所有商品'); return }
    loading.value = true
    const submitData: Record<string, any> = {
      supplierId: formData.supplierId, supplierName: formData.supplierName,
      orderDate: formData.orderDate, expectedDate: formData.expectedDate,
      purchaserId: formData.purchaserId, purchaserName: formData.purchaserName,
      paymentMethod: formData.paymentMethod, currency: formData.currency,
      taxRate: formData.taxRate, remark: formData.remark,
      totalAmount: Math.round(totalAmount.value * 100) / 100,
      taxAmount: Math.round(taxAmount.value * 100) / 100,
      totalAmountWithTax: Math.round(totalAmountWithTax.value * 100) / 100,
      items: formData.items.map(i => ({
        productId: i.productId, productCode: i.productCode,
        productName: i.productName, quantity: i.quantity,
        unitPrice: i.unitPrice, unit: i.unit, remark: i.remark
      }))
    }
    if (props.editData?.id) {
      await purchaseOrderApi.update(props.editData.id, submitData)
      message.success('更新成功')
    } else {
      await purchaseOrderApi.create(submitData)
      message.success('创建成功')
    }
    // 重置表单，保持打开
    Object.assign(formData, {
      orderNo: 'PO' + dayjs().format('YYYYMMDDHHmmss'),
      supplierId: undefined, supplierName: '',
      orderDate: dayjs().format('YYYY-MM-DD'),
      expectedDate: '', purchaserId: undefined, purchaserName: '',
      paymentMethod: 2, currency: 'CNY', taxRate: 13, remark: '',
      items: [{ id: '1', productId: undefined, productCode: '', productName: '', quantity: 1, unitPrice: 0, unit: '', remark: '' }]
    })
    emit('success')
    nextTick(() => saveFormSnapshot())
  } catch (error: any) {
    const errMsg = error?.response?.data?.message || error?.message || '操作失败'
    message.error(errMsg)
  } finally {
    loading.value = false
  }
}

function handleCancel() { handleFormClose() }

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
    nextTick(() => saveFormSnapshot())
  }
})
</script>

<style scoped>
.product-table-actions { margin-bottom: 16px; }
.amount-summary { margin-top: 16px; padding: 16px; background: var(--color-bg-layout); border-radius: var(--border-radius-lg); }
.amount-text { font-weight: 600; color: var(--color-primary); }
.danger { color: var(--color-danger); }

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}
</style>
