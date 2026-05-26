<template>
  <div class="quotation-form">
    <van-cell-group inset>
      <van-field
        v-model="form.quotationNo"
        label="报价单号"
        placeholder="自动生成"
        readonly
      />
      <van-field
        v-model="form.quotationName"
        label="报价名称"
        placeholder="请输入报价名称"
        required
        :rules="[{ required: true, message: '请输入报价名称' }]"
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
        v-model="form.contactPerson"
        label="联系人"
        placeholder="请输入联系人"
      />
      <van-field
        v-model="form.contactPhone"
        label="联系电话"
        placeholder="请输入联系电话"
        type="tel"
      />
      <van-field
        v-model="form.quotationDate"
        label="报价日期"
        placeholder="请选择报价日期"
        readonly
        is-link
        required
        @click="showDatePicker = true"
        :rules="[{ required: true, message: '请选择报价日期' }]"
      />
      <van-field
        v-model="form.validDays"
        label="有效期(天)"
        placeholder="请输入有效期"
        type="number"
      />
      <van-field
        v-model="form.currency"
        label="币种"
        placeholder="请选择币种"
        readonly
        is-link
        @click="showCurrencyPicker = true"
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

    <van-cell-group inset title="报价明细">
      <div class="quotation-items">
        <div v-for="(item, index) in form.items" :key="index" class="quotation-item">
          <van-cell-group inset>
            <van-field
              v-model="item.productName"
              label="产品名称"
              placeholder="请输入产品名称"
              required
            />
            <van-field
              v-model="item.spec"
              label="规格型号"
              placeholder="请输入规格型号"
            />
            <van-field
              v-model="item.quantity"
              label="数量"
              placeholder="请输入数量"
              type="number"
              required
            />
            <van-field
              v-model="item.unit"
              label="单位"
              placeholder="请输入单位"
            />
            <van-field
              v-model="item.price"
              label="单价"
              placeholder="请输入单价"
              type="number"
              required
            />
            <van-field
              v-model="item.discount"
              label="折扣"
              placeholder="请输入折扣"
              type="number"
            />
            <van-field
              :model-value="calculateItemTotal(item)"
              label="小计"
              readonly
            />
            <van-field
              v-model="item.remark"
              label="备注"
              placeholder="请输入备注"
            />
          </van-cell-group>
          <van-button size="small" type="danger" plain @click="removeItem(index)">删除</van-button>
        </div>
      </div>
      <van-button block type="primary" plain @click="addItem">添加产品</van-button>
    </van-cell-group>

    <van-cell-group inset title="费用汇总">
      <van-cell title="产品金额" :value="calculateTotalAmount()" />
      <van-cell title="折扣金额" :value="calculateDiscountAmount()" />
      <van-cell title="税费" :value="calculateTaxAmount()" />
      <van-cell title="报价总额" :value="calculateGrandTotal()" value-class="grand-total" />
    </van-cell-group>

    <van-cell-group inset title="报价条款">
      <van-field
        v-model="form.terms"
        type="textarea"
        rows="4"
        autosize
        placeholder="请输入报价条款（如付款方式、交货期等）"
      />
    </van-cell-group>

    <van-cell-group inset title="附件">
      <van-uploader
        v-model="form.attachments"
        multiple
        :max-count="5"
      />
    </van-cell-group>

    <van-popup v-model:show="showCustomerPicker" position="bottom" round>
      <van-picker
        title="选择客户"
        :columns="customerOptions"
        @confirm="onCustomerConfirm"
        @cancel="showCustomerPicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showDatePicker" position="bottom" round>
      <van-date-picker
        title="报价日期"
        @confirm="onDateConfirm"
        @cancel="showDatePicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showCurrencyPicker" position="bottom" round>
      <van-picker
        title="选择币种"
        :columns="currencyOptions"
        @confirm="onCurrencyConfirm"
        @cancel="showCurrencyPicker = false"
      />
    </van-popup>

    <div class="form-actions">
      <van-button block type="default" @click="handleCancel">取消</van-button>
      <van-button block type="primary" @click="handleSubmit">提交报价</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { showToast, showSuccessToast } from 'vant'

const emit = defineEmits(['submit', 'cancel'])

const showCustomerPicker = ref(false)
const showDatePicker = ref(false)
const showCurrencyPicker = ref(false)

const form = reactive({
  quotationNo: '',
  quotationName: '',
  customerName: '',
  customerId: 0,
  contactPerson: '',
  contactPhone: '',
  quotationDate: '',
  validDays: '30',
  currency: 'CNY',
  remark: '',
  terms: '',
  items: [] as any[],
  attachments: [] as any[]
})

const customerOptions = [
  { text: '北京科技有限公司', value: 1 },
  { text: '上海贸易公司', value: 2 },
  { text: '广州制造企业', value: 3 }
]

const currencyOptions = [
  { text: '人民币(CNY)', value: 'CNY' },
  { text: '美元(USD)', value: 'USD' },
  { text: '欧元(EUR)', value: 'EUR' }
]

onMounted(() => {
  generateQuotationNo()
  addItem()
})

const generateQuotationNo = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0')
  form.quotationNo = `QT${year}${month}${day}${random}`
}

const addItem = () => {
  form.items.push({
    productName: '',
    spec: '',
    quantity: '',
    unit: '',
    price: '',
    discount: '0',
    remark: ''
  })
}

const removeItem = (index: number) => {
  if (form.items.length > 1) {
    form.items.splice(index, 1)
  } else {
    showToast('至少保留一条报价明细')
  }
}

const calculateItemTotal = (item: any) => {
  const quantity = parseFloat(item.quantity) || 0
  const price = parseFloat(item.price) || 0
  const discount = parseFloat(item.discount) || 0
  const total = quantity * price * (1 - discount / 100)
  return total.toFixed(2)
}

const calculateTotalAmount = () => {
  const total = form.items.reduce((sum, item) => {
    return sum + parseFloat(calculateItemTotal(item))
  }, 0)
  return `¥${total.toFixed(2)}`
}

const calculateDiscountAmount = () => {
  const discount = form.items.reduce((sum, item) => {
    const quantity = parseFloat(item.quantity) || 0
    const price = parseFloat(item.price) || 0
    const discountRate = parseFloat(item.discount) || 0
    return sum + quantity * price * (discountRate / 100)
  }, 0)
  return `¥${discount.toFixed(2)}`
}

const calculateTaxAmount = () => {
  const total = form.items.reduce((sum, item) => {
    return sum + parseFloat(calculateItemTotal(item))
  }, 0)
  const tax = total * 0.13
  return `¥${tax.toFixed(2)}`
}

const calculateGrandTotal = () => {
  const total = form.items.reduce((sum, item) => {
    return sum + parseFloat(calculateItemTotal(item))
  }, 0)
  const tax = total * 0.13
  return `¥${(total + tax).toFixed(2)}`
}

const onCustomerConfirm = ({ selectedOptions }: any) => {
  form.customerName = selectedOptions[0].text
  form.customerId = selectedOptions[0].value
  showCustomerPicker.value = false
}

const onDateConfirm = ({ selectedValues }: any) => {
  form.quotationDate = selectedValues.join('-')
  showDatePicker.value = false
}

const onCurrencyConfirm = ({ selectedOptions }: any) => {
  form.currency = selectedOptions[0].value
  showCurrencyPicker.value = false
}

const handleCancel = () => {
  emit('cancel')
}

const handleSubmit = () => {
  if (!form.quotationName || !form.customerName || !form.quotationDate) {
    showToast('请填写必填项')
    return
  }
  if (form.items.length === 0 || !form.items[0].productName || !form.items[0].quantity || !form.items[0].price) {
    showToast('请添加报价明细')
    return
  }
  emit('submit', form)
  showSuccessToast('报价单已提交')
}
</script>

<style scoped lang="scss">
.quotation-form {
  padding: 12px;
}

.quotation-items {
  .quotation-item {
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }
  }
}

.grand-total {
  color: #ee0a24 !important;
  font-weight: 600 !important;
  font-size: 16px !important;
}

.form-actions {
  display: flex;
  gap: 12px;
  padding: 16px;
}
</style>