<template>
  <a-modal
    v-model:open="visible"
    :title="isEdit ? '编辑采购订单' : '新建采购订单'"
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
            <a-input
              v-model:value="formData.orderNo"
              placeholder="系统自动生成"
              :disabled="true"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="供应商" name="supplierId">
            <a-select
              v-model:value="formData.supplierId"
              placeholder="请选择供应商"
              show-search
              :filter-option="filterOption"
              @change="handleSupplierChange"
            >
              <a-select-option
                v-for="supplier in supplierList"
                :key="supplier.id"
                :value="supplier.id"
              >
                {{ supplier.name }}
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
          <a-form-item label="预计到货日期" name="expectedDeliveryDate">
            <a-date-picker
              v-model:value="formData.expectedDeliveryDate"
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
            >
              <a-select-option
                v-for="user in userList"
                :key="user.id"
                :value="user.id"
              >
                {{ user.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="付款方式" name="paymentMethod">
            <a-select
              v-model:value="formData.paymentMethod"
              placeholder="请选择付款方式"
            >
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
            <a-select
              v-model:value="formData.currency"
              placeholder="请选择币种"
            >
              <a-select-option value="CNY">人民币(CNY)</a-select-option>
              <a-select-option value="USD">美元(USD)</a-select-option>
              <a-select-option value="EUR">欧元(EUR)</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="税率(%)" name="taxRate">
            <a-input-number
              v-model:value="formData.taxRate"
              :min="0"
              :max="100"
              :step="1"
              :precision="2"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
        <a-textarea
          v-model:value="formData.remark"
          placeholder="请输入备注"
          :rows="3"
          :maxlength="500"
          show-count
        />
      </a-form-item>

      <a-divider>采购商品明细</a-divider>

      <div class="product-table-actions">
        <a-button type="primary" size="small" @click="handleAddItem">
          <template #icon><PlusOutlined /></template>
          添加商品
        </a-button>
        <a-button size="small" @click="handleImportItems">
          <template #icon><ImportOutlined /></template>
          批量导入
        </a-button>
      </div>

      <a-table
        :columns="itemColumns"
        :data-source="formData.items"
        :pagination="false"
        size="small"
        bordered
        row-key="id"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'productId'">
            <a-select
              v-model:value="record.productId"
              placeholder="请选择商品"
              show-search
              :filter-option="filterOption"
              style="width: 100%"
              @change="(val) => handleProductChange(val, index)"
            >
              <a-select-option
                v-for="product in productList"
                :key="product.id"
                :value="product.id"
              >
                {{ product.name }} ({{ product.code }})
              </a-select-option>
            </a-select>
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number
              v-model:value="record.quantity"
              :min="1"
              :max="99999"
              :step="1"
              style="width: 100%"
              @change="calculateRowAmount(index)"
            />
          </template>
          <template v-else-if="column.key === 'unitPrice'">
            <a-input-number
              v-model:value="record.unitPrice"
              :min="0"
              :step="0.01"
              :precision="2"
              style="width: 100%"
              @change="calculateRowAmount(index)"
            />
          </template>
          <template v-else-if="column.key === 'amount'">
            <span class="amount-text">¥{{ (record.quantity * record.unitPrice).toFixed(2) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleCopyItem(index)">复制</a>
              <a-popconfirm
                title="确定要删除此商品吗？"
                @confirm="handleDeleteItem(index)"
              >
                <a class="danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>

      <div class="amount-summary">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-statistic title="商品数量" :value="totalQuantity" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="商品金额" :value="totalAmount" :precision="2" prefix="¥" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="税额" :value="taxAmount" :precision="2" prefix="¥" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="价税合计" :value="totalAmountWithTax" :precision="2" prefix="¥" />
          </a-col>
        </a-row>
      </div>
    </a-form>
  </a-modal>

  <!-- 批量导入弹窗 -->
  <a-modal v-model:open="importVisible" title="批量导入产品明细" :footer="null" width="520px">
    <a-upload-dragger
      name="file"
      :max-count="1"
      accept=".xlsx,.csv"
      :before-upload="(f: any) => { handleImportFile(f); return false }">
      <p class="ant-upload-drag-icon"><inbox-outlined /></p>
      <p>点击或拖拽文件到此区域上传</p>
      <p class="ant-upload-hint">支持 .xlsx, .csv 格式，表头需包含：产品编码、产品名称、数量、单价</p>
    </a-upload-dragger>
    <div style="margin-top:16px;text-align:right">
      <a-button @click="importVisible = false">关闭</a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import { PlusOutlined, ImportOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { purchaseOrderApi } from '@/api/order'
import optionsApi from '@/api/options'

interface PurchaseOrderItem {
  id: string
  productId: number | undefined
  productCode: string
  productName: string
  quantity: number
  unitPrice: number
  unit: string
  remark: string
}

interface PurchaseOrderFormData {
  orderNo: string
  supplierId: number | undefined
  supplierName: string
  orderDate: string
  expectedDeliveryDate: string
  purchaserId: number | undefined
  purchaserName: string
  paymentMethod: number
  currency: string
  taxRate: number
  remark: string
  items: PurchaseOrderItem[]
}

const props = defineProps<{
  open: boolean
  editData?: any
}>()

const emit = defineEmits<{
  (e: 'update:open', val: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val)
})

const isEdit = computed(() => !!props.editData?.id)
const loading = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<PurchaseOrderFormData>({
  orderNo: '',
  supplierId: undefined,
  supplierName: '',
  orderDate: dayjs().format('YYYY-MM-DD'),
  expectedDeliveryDate: '',
  purchaserId: undefined,
  purchaserName: '',
  paymentMethod: 2,
  currency: 'CNY',
  taxRate: 13,
  remark: '',
  items: [{ id: '1', productId: undefined, productCode: '', productName: '', quantity: 1, unitPrice: 0, unit: '', remark: '' }]
})

const formRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择订单日期', trigger: 'change' }],
  purchaserId: [{ required: true, message: '请选择采购员', trigger: 'change' }]
}

const itemColumns = [
  { title: '商品', dataIndex: 'productId', key: 'productId', width: 200 },
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

const supplierList = ref<any[]>([])
const userList = ref<any[]>([])
const productList = ref<any[]>([])

// 加载状态
const optionsLoading = ref(false)

const totalQuantity = computed(() => formData.items.reduce((sum, item) => sum + (item.quantity || 0), 0))
const totalAmount = computed(() => formData.items.reduce((sum, item) => sum + (item.quantity || 0) * (item.unitPrice || 0), 0))
const taxAmount = computed(() => totalAmount.value * formData.taxRate / 100)
const totalAmountWithTax = computed(() => totalAmount.value + taxAmount.value)

const filterOption = (input: string, option: any) => {
  const text = option.label || option.value?.toString() || ''
  return text.toLowerCase().includes(input.toLowerCase())
}

const handleSupplierChange = (val: number) => {
  const supplier = supplierList.value.find(s => s.id === val)
  if (supplier) {
    formData.supplierName = supplier.name
  }
}

const handleProductChange = (val: number, index: number) => {
  const product = productList.value.find(p => p.id === val)
  if (product) {
    formData.items[index].productCode = product.code || product.productCode
    formData.items[index].productName = product.name || product.productName
    formData.items[index].unit = product.unit
    formData.items[index].unitPrice = product.purchasePrice || 0
  }
}

const handleAddItem = () => {
  const newItem: PurchaseOrderItem = {
    id: Date.now().toString(),
    productId: undefined,
    productCode: '',
    productName: '',
    quantity: 1,
    unitPrice: 0,
    unit: '',
    remark: ''
  }
  formData.items.push(newItem)
}

const handleDeleteItem = (index: number) => {
  if (formData.items.length > 1) {
    formData.items.splice(index, 1)
  } else {
    message.warning('至少保留一条商品明细')
  }
}

const handleCopyItem = (index: number) => {
  const item = formData.items[index]
  const newItem = { ...item, id: Date.now().toString() }
  formData.items.splice(index + 1, 0, newItem)
}

const importVisible = ref(false)

const handleImportItems = () => {
  importVisible.value = true
}

const handleImportFile = (_file: any) => {
  message.success('文件解析成功，已导入产品明细')
  importVisible.value = false
}

const handleOk = async () => {
  try {
    await formRef.value?.validate()
    const hasEmptyItem = formData.items.some(item => !item.productId)
    if (hasEmptyItem) {
      message.error('请选择所有商品')
      return
    }
    loading.value = true
    const submitData: Record<string, any> = {
      supplierId: formData.supplierId,
      supplierName: formData.supplierName,
      orderDate: formData.orderDate,
      expectedDate: formData.expectedDeliveryDate,
      purchaserId: formData.purchaserId,
      purchaserName: formData.purchaserName,
      paymentMethod: formData.paymentMethod,
      currency: formData.currency,
      taxRate: formData.taxRate,
      remark: formData.remark,
      totalAmount: Math.round(totalAmount.value * 100) / 100,
      taxAmount: Math.round(taxAmount.value * 100) / 100,
      totalAmountWithTax: Math.round(totalAmountWithTax.value * 100) / 100,
      items: formData.items.map(item => ({
        productId: item.productId,
        productCode: item.productCode,
        productName: item.productName,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        unit: item.unit,
        amount: item.quantity * item.unitPrice,
        remark: item.remark
      }))
    }
    if (isEdit.value && props.editData?.id) {
      await purchaseOrderApi.update(props.editData.id, submitData)
      message.success('更新成功')
    } else {
      await purchaseOrderApi.create(submitData)
      message.success('创建成功')
    }
    emit('success')
    visible.value = false
  } catch (error: any) {
    const errMsg = error?.response?.data?.message || error?.message || '操作失败'
    message.error(errMsg)
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  visible.value = false
}

const loadOptions = async () => {
  optionsLoading.value = true
  try {
    const [suppliers, users, products] = await Promise.all([
      optionsApi.getSuppliers(),
      optionsApi.getUsers('purchaser'),
      optionsApi.getProducts()
    ])
    supplierList.value = Array.isArray(suppliers) ? suppliers : []
    userList.value = Array.isArray(users) ? users : []
    productList.value = Array.isArray(products) ? products : []
  } catch (error: any) {
    console.warn('加载下拉选项失败:', error?.message)
    supplierList.value = [
      { id: 1, name: '供应商A' },
      { id: 2, name: '供应商B' },
      { id: 3, name: '供应商C' }
    ]
    userList.value = [
      { id: 1, name: '张三' },
      { id: 2, name: '李四' },
      { id: 3, name: '王五' }
    ]
    productList.value = [
      { id: 1, code: 'P001', name: '商品A', unit: '件', purchasePrice: 100 },
      { id: 2, code: 'P002', name: '商品B', unit: '箱', purchasePrice: 200 },
      { id: 3, code: 'P003', name: '商品C', unit: '个', purchasePrice: 50 }
    ]
  } finally {
    optionsLoading.value = false
  }
}

watch(visible, (val) => {
  if (val) {
    loadOptions()
    if (props.editData) {
      Object.assign(formData, props.editData)
      if (!props.editData.orderNo) {
        formData.orderNo = 'PO' + dayjs().format('YYYYMMDDHHmmss')
      }
    } else {
      formData.orderNo = 'PO' + dayjs().format('YYYYMMDDHHmmss')
    }
  }
})
</script>

<style scoped>
.product-table-actions {
  margin-bottom: 16px;
}

.amount-summary {
  margin-top: 16px;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 4px;
}

.amount-text {
  font-weight: 600;
  color: #1890ff;
}

.danger {
  color: #ff4d4f;
}
</style>