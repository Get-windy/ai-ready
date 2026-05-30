<template>
  <a-modal
    :open="open"
    :title="editData ? '编辑采购订单' : '新建采购订单'"
    width="800px"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 18 }"
    >
      <a-form-item label="订单编号" name="orderNo">
        <a-input v-model:value="formData.orderNo" placeholder="请输入订单编号" />
      </a-form-item>
      
      <a-form-item label="供应商" name="supplierId">
        <a-select
          v-model:value="formData.supplierId"
          placeholder="请选择供应商"
          show-search
          :filter-option="filterOption"
        >
          <a-select-option v-for="supplier in suppliers" :key="supplier.id" :value="supplier.id">
            {{ supplier.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      
      <a-form-item label="订单日期" name="orderDate">
        <a-date-picker v-model:value="formData.orderDate" style="width: 100%" />
      </a-form-item>
      
      <a-form-item label="交货日期" name="deliveryDate">
        <a-date-picker v-model:value="formData.deliveryDate" style="width: 100%" />
      </a-form-item>
      
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="4" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'

interface Props {
  open: boolean
  editData?: any
}

const props = defineProps<Props>()
const emit = defineEmits(['update:open', 'success'])

const formRef = ref<FormInstance>()
const formData = ref({
  orderNo: '',
  supplierId: undefined,
  orderDate: undefined,
  deliveryDate: undefined,
  remark: ''
})

const rules = {
  orderNo: [{ required: true, message: '请输入订单编号' }],
  supplierId: [{ required: true, message: '请选择供应商' }],
  orderDate: [{ required: true, message: '请选择订单日期' }]
}

const suppliers = ref([
  { id: 1, name: '供应商A' },
  { id: 2, name: '供应商B' },
  { id: 3, name: '供应商C' }
])

const filterOption = (input: string, option: any) => {
  return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

watch(() => props.editData, (newData) => {
  if (newData) {
    formData.value = {
      orderNo: newData.orderNo || '',
      supplierId: newData.supplierId,
      orderDate: newData.orderDate,
      deliveryDate: newData.deliveryDate,
      remark: newData.remark || ''
    }
  } else {
    formData.value = {
      orderNo: '',
      supplierId: undefined,
      orderDate: undefined,
      deliveryDate: undefined,
      remark: ''
    }
  }
})

const handleOk = async () => {
  try {
    await formRef.value?.validate()
    message.success(props.editData ? '编辑成功' : '创建成功')
    emit('success')
    emit('update:open', false)
  } catch (error) {
    console.error('验证失败:', error)
  }
}

const handleCancel = () => {
  emit('update:open', false)
}
</script>