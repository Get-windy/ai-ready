<template>
  <a-modal
    :open="open"
    :title="editData ? '编辑销售订单' : '新建销售订单'"
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
        <a-input v-model:value="formData.orderNo" placeholder="系统自动生成" :disabled="true" />
      </a-form-item>

      <a-form-item label="客户" name="customerId">
        <a-select
          v-model:value="formData.customerId"
          placeholder="请选择客户"
          show-search
          :filter-option="filterOption"
        >
          <a-select-option v-for="customer in customers" :key="customer.id" :value="customer.id">
            {{ customer.name }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="订单日期" name="orderDate">
        <a-date-picker v-model:value="formData.orderDate" style="width: 100%" />
      </a-form-item>

      <a-form-item label="预计发货日期" name="deliveryDate">
        <a-date-picker v-model:value="formData.deliveryDate" style="width: 100%" />
      </a-form-item>

      <a-form-item label="销售员" name="salespersonId">
        <a-select
          v-model:value="formData.salespersonId"
          placeholder="请选择销售员"
          show-search
          :filter-option="filterOption"
        >
          <a-select-option v-for="user in salespersons" :key="user.id" :value="user.id">
            {{ user.name }}
          </a-select-option>
        </a-select>
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
  customerId: undefined as number | undefined,
  orderDate: undefined as string | undefined,
  deliveryDate: undefined as string | undefined,
  salespersonId: undefined as number | undefined,
  remark: ''
})

const rules = {
  customerId: [{ required: true, message: '请选择客户' }],
  orderDate: [{ required: true, message: '请选择订单日期' }]
}

const customers = ref([
  { id: 1, name: '客户A' },
  { id: 2, name: '客户B' },
  { id: 3, name: '客户C' }
])

const salespersons = ref([
  { id: 1, name: '张三' },
  { id: 2, name: '李四' }
])

const filterOption = (input: string, option: any) => {
  return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

watch(() => props.editData, (newData) => {
  if (newData) {
    formData.value = {
      orderNo: newData.orderNo || '',
      customerId: newData.customerId,
      orderDate: newData.orderDate,
      deliveryDate: newData.deliveryDate,
      salespersonId: newData.salespersonId,
      remark: newData.remark || ''
    }
  } else {
    formData.value = {
      orderNo: '',
      customerId: undefined,
      orderDate: undefined,
      deliveryDate: undefined,
      salespersonId: undefined,
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
