<template>
  <ARForm
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-width="120px"
    class="purchase-order-form"
  >
    <ARFormItem label="采购订单号" name="orderNumber" required>
      <ARInput
        v-model="formData.orderNumber"
        placeholder="请输入采购订单号"
        clearable
        :maxlength="50"
      />
    </ARFormItem>

    <ARFormItem label="供应商" name="supplierId" required>
      <a-select
        v-model:value="formData.supplierId"
        placeholder="请选择供应商"
        show-search
        allow-clear
      >
        <a-select-option
          v-for="supplier in supplierOptions"
          :key="supplier.id"
          :value="supplier.id"
        >
          {{ supplier.name }}
        </a-select-option>
      </a-select>
    </ARFormItem>

    <ARFormItem label="订单日期" name="orderDate" required>
      <a-date-picker
        v-model:value="formData.orderDate"
        placeholder="请选择订单日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        style="width: 100%"
      />
    </ARFormItem>

    <ARFormItem label="预计到货日期" name="expectedDeliveryDate">
      <a-date-picker
        v-model:value="formData.expectedDeliveryDate"
        placeholder="请选择预计到货日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        style="width: 100%"
      />
    </ARFormItem>

    <ARFormItem label="采购员" name="purchaser">
      <ARInput
        v-model="formData.purchaser"
        placeholder="请输入采购员姓名"
        clearable
      />
    </ARFormItem>

    <ARFormItem label="订单状态" name="status" required>
      <a-select
        v-model:value="formData.status"
        placeholder="请选择订单状态"
      >
        <a-select-option
          v-for="status in statusOptions"
          :key="status.value"
          :value="status.value"
        >
          {{ status.label }}
        </a-select-option>
      </a-select>
    </ARFormItem>

    <ARFormItem label="订单备注" name="remark">
      <a-textarea
        v-model:value="formData.remark"
        :rows="3"
        placeholder="请输入订单备注信息"
        :maxlength="500"
        show-count
      />
    </ARFormItem>

    <!-- 采购商品表格 -->
    <ARFormItem label="采购商品" class="product-table-item">
      <div class="product-table-header">
        <h4>商品列表</h4>
        <ARButton
          type="primary"
          size="small"
          @click="addProductItem"
        >
          添加商品
        </ARButton>
      </div>

      <a-table
        :columns="productColumns"
        :data-source="formData.products"
        :pagination="false"
        bordered
        style="margin-top: 10px"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'productName'">
            <ARInput v-model="record.productName" placeholder="请输入商品名称" clearable />
          </template>
          <template v-else-if="column.key === 'sku'">
            <ARInput v-model="record.sku" placeholder="请输入SKU编码" clearable />
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number
              v-model:value="record.quantity"
              :min="1"
              :max="99999"
              :step="1"
              style="width: 100%"
            />
          </template>
          <template v-else-if="column.key === 'unitPrice'">
            <a-input-number
              v-model:value="record.unitPrice"
              :min="0"
              :step="0.01"
              :precision="2"
              style="width: 100%"
            />
          </template>
          <template v-else-if="column.key === 'amount'">
            <span>{{ (record.quantity * record.unitPrice).toFixed(2) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <ARButton type="danger" size="small" @click="removeProductItem(index)">删除</ARButton>
          </template>
        </template>
      </a-table>

      <div class="product-table-footer">
        <span>总计: {{ totalAmount.toFixed(2) }} 元</span>
      </div>
    </ARFormItem>

    <!-- 表单操作按钮 -->
    <div class="form-actions">
      <ARButton type="primary" :loading="submitting" @click="submitForm">
        提交订单
      </ARButton>
      <ARButton @click="resetForm" style="margin-left: 12px">
        重置
      </ARButton>
      <ARButton @click="cancelForm" style="margin-left: 12px">
        取消
      </ARButton>
    </div>
  </ARForm>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue'

// 采购订单表单数据
interface ProductItem {
  productName: string
  sku: string
  quantity: number
  unitPrice: number
}

interface PurchaseOrderFormData {
  orderNumber: string
  supplierId: string
  orderDate: string
  expectedDeliveryDate: string
  purchaser: string
  status: string
  remark: string
  products: ProductItem[]
}

const props = defineProps<{
  initialData?: Partial<PurchaseOrderFormData>
}>()

const emit = defineEmits<{
  (e: 'submit', data: PurchaseOrderFormData): void
  (e: 'cancel'): void
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

// 表格列配置
const productColumns = [
  { title: '商品名称', dataIndex: 'productName', key: 'productName' },
  { title: 'SKU编码', dataIndex: 'sku', key: 'sku' },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 120 },
  { title: '单价(元)', dataIndex: 'unitPrice', key: 'unitPrice', width: 120 },
  { title: '金额(元)', key: 'amount', width: 120 },
  { title: '操作', key: 'action', width: 80 }
]

// 表单数据
const formData = reactive<PurchaseOrderFormData>({
  orderNumber: props.initialData?.orderNumber || '',
  supplierId: props.initialData?.supplierId || '',
  orderDate: props.initialData?.orderDate || '',
  expectedDeliveryDate: props.initialData?.expectedDeliveryDate || '',
  purchaser: props.initialData?.purchaser || '',
  status: props.initialData?.status || 'draft',
  remark: props.initialData?.remark || '',
  products: props.initialData?.products || [
    { productName: '', sku: '', quantity: 1, unitPrice: 0 }
  ]
})

// 供应商选项
const supplierOptions = ref([
  { id: '1', name: '供应商A' },
  { id: '2', name: '供应商B' },
  { id: '3', name: '供应商C' },
  { id: '4', name: '供应商D' }
])

// 状态选项
const statusOptions = ref([
  { value: 'draft', label: '草稿' },
  { value: 'pending', label: '待审核' },
  { value: 'approved', label: '已审核' },
  { value: 'processing', label: '处理中' },
  { value: 'shipped', label: '已发货' },
  { value: 'received', label: '已收货' },
  { value: 'completed', label: '已完成' },
  { value: 'cancelled', label: '已取消' }
])

// 表单验证规则
const formRules: Record<string, Rule[]> = {
  orderNumber: [
    { required: true, message: '请输入采购订单号', trigger: 'blur' },
    { min: 5, max: 50, message: '长度在 5 到 50 个字符', trigger: 'blur' }
  ],
  supplierId: [
    { required: true, message: '请选择供应商', trigger: 'change' }
  ],
  orderDate: [
    { required: true, message: '请选择订单日期', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择订单状态', trigger: 'change' }
  ]
}

// 计算总金额
const totalAmount = computed(() => {
  return formData.products.reduce((sum, product) => {
    return sum + (product.quantity * product.unitPrice)
  }, 0)
})

// 添加商品项
const addProductItem = () => {
  formData.products.push({
    productName: '',
    sku: '',
    quantity: 1,
    unitPrice: 0
  })
}

// 删除商品项
const removeProductItem = (index: number) => {
  if (formData.products.length > 1) {
    formData.products.splice(index, 1)
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    submitting.value = true

    const submitData: PurchaseOrderFormData = {
      ...formData,
      products: formData.products.map(product => ({
        ...product,
        amount: product.quantity * product.unitPrice
      }))
    }

    emit('submit', submitData)
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
    formData.products = [{ productName: '', sku: '', quantity: 1, unitPrice: 0 }]
  }
}

// 取消表单
const cancelForm = () => {
  emit('cancel')
}

// 暴露方法
defineExpose({
  submitForm,
  resetForm,
  cancelForm,
  formData,
  totalAmount
})
</script>

<style lang="scss" scoped>
.purchase-order-form {
  .product-table-item {
    :deep(.ar-form-item__content) {
      flex-direction: column;
      align-items: stretch;
    }
  }

  .product-table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    h4 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--ar-text-color-primary, #303133);
    }
  }

  .product-table-footer {
    margin-top: 12px;
    padding: 8px 12px;
    background-color: var(--ar-bg-color, #f0f2f5);
    border-radius: var(--ar-border-radius-base, 4px);
    font-weight: 600;
    text-align: right;
  }

  .form-actions {
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid var(--ar-border-color-base, #dcdfe6);
    text-align: right;
  }
}
</style>
