<template>
  <div class="invoice-form">
    <van-cell-group inset>
      <van-field
        v-model="form.invoiceNo"
        label="发票号码"
        placeholder="请输入发票号码"
        required
        :rules="[{ required: true, message: '请输入发票号码' }]"
      />
      <van-field
        v-model="form.invoiceType"
        label="发票类型"
        placeholder="请选择发票类型"
        readonly
        is-link
        required
        @click="showTypePicker = true"
        :rules="[{ required: true, message: '请选择发票类型' }]"
      />
      <van-field
        v-model="form.invoiceDate"
        label="开票日期"
        placeholder="请选择开票日期"
        readonly
        is-link
        required
        @click="showDatePicker = true"
        :rules="[{ required: true, message: '请选择开票日期' }]"
      />
      <van-field
        v-model="form.customerName"
        label="客户名称"
        placeholder="请选择客户"
        readonly
        is-link
        required
        @click="showCustomerPicker = true"
        :rules="[{ required: true, message: '请选择客户' }]"
      />
      <van-field
        v-model="form.amount"
        label="发票金额"
        placeholder="请输入发票金额"
        type="number"
        required
        :rules="[{ required: true, message: '请输入发票金额' }]"
      >
        <template #button>
          <van-button size="small" type="primary">计算税额</van-button>
        </template>
      </van-field>
      <van-field
        v-model="form.taxAmount"
        label="税额"
        placeholder="自动计算"
        readonly
      />
      <van-field
        v-model="form.totalAmount"
        label="价税合计"
        placeholder="自动计算"
        readonly
      />
      <van-field
        v-model="form.taxRate"
        label="税率"
        placeholder="请选择税率"
        readonly
        is-link
        @click="showTaxRatePicker = true"
      />
      <van-field
        v-model="form.remark"
        label="备注"
        placeholder="请输入备注"
        type="textarea"
        rows="2"
        autosize
      />
    </van-cell-group>

    <van-cell-group inset title="关联订单">
      <van-cell
        v-for="order in form.relatedOrders"
        :key="order.id"
        :title="order.orderNo"
        :value="order.amount"
        is-link
        @click="viewOrder(order)"
      >
        <template #icon>
          <van-checkbox v-model="order.selected" />
        </template>
      </van-cell>
      <van-cell title="添加关联订单" is-link @click="addRelatedOrder">
        <template #icon>
          <van-icon name="plus" />
        </template>
      </van-cell>
    </van-cell-group>

    <van-cell-group inset title="附件">
      <van-uploader
        v-model="form.attachments"
        multiple
        :max-count="5"
      />
    </van-cell-group>

    <van-popup v-model:show="showTypePicker" position="bottom" round>
      <van-picker
        title="发票类型"
        :columns="typeOptions"
        @confirm="onTypeConfirm"
        @cancel="showTypePicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showDatePicker" position="bottom" round>
      <van-date-picker
        title="开票日期"
        @confirm="onDateConfirm"
        @cancel="showDatePicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showCustomerPicker" position="bottom" round>
      <van-picker
        title="选择客户"
        :columns="customerOptions"
        @confirm="onCustomerConfirm"
        @cancel="showCustomerPicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showTaxRatePicker" position="bottom" round>
      <van-picker
        title="选择税率"
        :columns="taxRateOptions"
        @confirm="onTaxRateConfirm"
        @cancel="showTaxRatePicker = false"
      />
    </van-popup>

    <div class="form-actions">
      <van-button block type="default" @click="handleCancel">取消</van-button>
      <van-button block type="primary" @click="handleSubmit">提交</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { showToast, showSuccessToast, showDialog } from 'vant'

const emit = defineEmits(['submit', 'cancel'])

const showTypePicker = ref(false)
const showDatePicker = ref(false)
const showCustomerPicker = ref(false)
const showTaxRatePicker = ref(false)

const form = reactive({
  invoiceNo: '',
  invoiceType: '',
  invoiceDate: '',
  customerName: '',
  customerId: 0,
  amount: '',
  taxAmount: '',
  totalAmount: '',
  taxRate: '13%',
  remark: '',
  relatedOrders: [] as any[],
  attachments: [] as any[]
})

const typeOptions = [
  { text: '增值税专用发票', value: 'special' },
  { text: '增值税普通发票', value: 'normal' },
  { text: '电子发票', value: 'electronic' }
]

const customerOptions = [
  { text: '北京科技有限公司', value: 1 },
  { text: '上海贸易公司', value: 2 },
  { text: '广州制造企业', value: 3 }
]

const taxRateOptions = [
  { text: '13%', value: '13' },
  { text: '9%', value: '9' },
  { text: '6%', value: '6' },
  { text: '3%', value: '3' },
  { text: '0%', value: '0' }
]

watch(() => form.amount, (val) => {
  if (val && form.taxRate) {
    const rate = parseFloat(form.taxRate.replace('%', '')) / 100
    const amount = parseFloat(val)
    form.taxAmount = (amount * rate).toFixed(2)
    form.totalAmount = (amount + amount * rate).toFixed(2)
  }
})

watch(() => form.taxRate, (val) => {
  if (form.amount && val) {
    const rate = parseFloat(val.replace('%', '')) / 100
    const amount = parseFloat(form.amount)
    form.taxAmount = (amount * rate).toFixed(2)
    form.totalAmount = (amount + amount * rate).toFixed(2)
  }
})

const onTypeConfirm = ({ selectedOptions }: any) => {
  form.invoiceType = selectedOptions[0].text
  showTypePicker.value = false
}

const onDateConfirm = ({ selectedValues }: any) => {
  form.invoiceDate = selectedValues.join('-')
  showDatePicker.value = false
}

const onCustomerConfirm = ({ selectedOptions }: any) => {
  form.customerName = selectedOptions[0].text
  form.customerId = selectedOptions[0].value
  showCustomerPicker.value = false
}

const onTaxRateConfirm = ({ selectedOptions }: any) => {
  form.taxRate = selectedOptions[0].text
  showTaxRatePicker.value = false
}

const viewOrder = (order: any) => {
  showToast(`查看订单: ${order.orderNo}`)
}

const addRelatedOrder = () => {
  showDialog({
    title: '关联订单',
    message: '请选择需要关联到此发票的销售订单',
    confirmButtonText: '选择订单',
    showCancelButton: true
  }).then(() => {
    showSuccessToast('订单已关联')
  }).catch(() => {})
}

const handleCancel = () => {
  emit('cancel')
}

const handleSubmit = () => {
  if (!form.invoiceNo || !form.invoiceType || !form.invoiceDate || !form.customerName || !form.amount) {
    showToast('请填写必填项')
    return
  }
  emit('submit', form)
  showSuccessToast('发票已提交')
}
</script>

<style scoped lang="scss">
.invoice-form {
  padding: 12px;
}

.form-actions {
  display: flex;
  gap: 12px;
  padding: 16px;
}
</style>