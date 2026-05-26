<template>
  <div class="contract-form">
    <van-cell-group inset>
      <van-field
        v-model="form.contractNo"
        label="合同编号"
        placeholder="自动生成"
        readonly
      />
      <van-field
        v-model="form.contractName"
        label="合同名称"
        placeholder="请输入合同名称"
        required
        :rules="[{ required: true, message: '请输入合同名称' }]"
      />
      <van-field
        v-model="form.contractType"
        label="合同类型"
        placeholder="请选择合同类型"
        readonly
        is-link
        required
        @click="showTypePicker = true"
        :rules="[{ required: true, message: '请选择合同类型' }]"
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
        v-model="form.startDate"
        label="开始日期"
        placeholder="请选择开始日期"
        readonly
        is-link
        required
        @click="showStartDatePicker = true"
        :rules="[{ required: true, message: '请选择开始日期' }]"
      />
      <van-field
        v-model="form.endDate"
        label="结束日期"
        placeholder="请选择结束日期"
        readonly
        is-link
        required
        @click="showEndDatePicker = true"
        :rules="[{ required: true, message: '请选择结束日期' }]"
      />
      <van-field
        v-model="form.contractAmount"
        label="合同金额"
        placeholder="请输入合同金额"
        type="number"
        required
        :rules="[{ required: true, message: '请输入合同金额' }]"
      />
      <van-field
        v-model="form.paymentMethod"
        label="付款方式"
        placeholder="请选择付款方式"
        readonly
        is-link
        @click="showPaymentPicker = true"
      />
      <van-field
        v-model="form.signDate"
        label="签订日期"
        placeholder="请选择签订日期"
        readonly
        is-link
        @click="showSignDatePicker = true"
      />
      <van-field
        v-model="form.signPerson"
        label="签订人"
        placeholder="请输入签订人"
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

    <van-cell-group inset title="合同条款">
      <van-field
        v-model="form.terms"
        type="textarea"
        rows="4"
        autosize
        placeholder="请输入合同主要条款"
      />
    </van-cell-group>

    <van-cell-group inset title="合同附件">
      <van-uploader
        v-model="form.attachments"
        multiple
        :max-count="10"
      />
    </van-cell-group>

    <van-cell-group inset title="审批流程">
      <van-steps direction="vertical" :active="0">
        <van-step>
          <div class="step-content">
            <div class="step-title">提交申请</div>
            <div class="step-user">当前用户</div>
          </div>
        </van-step>
        <van-step>
          <div class="step-content">
            <div class="step-title">部门主管审批</div>
            <div class="step-user">待审批</div>
          </div>
        </van-step>
        <van-step>
          <div class="step-content">
            <div class="step-title">财务审批</div>
            <div class="step-user">待审批</div>
          </div>
        </van-step>
        <van-step>
          <div class="step-content">
            <div class="step-title">总经理审批</div>
            <div class="step-user">待审批</div>
          </div>
        </van-step>
      </van-steps>
    </van-cell-group>

    <van-popup v-model:show="showTypePicker" position="bottom" round>
      <van-picker
        title="合同类型"
        :columns="typeOptions"
        @confirm="onTypeConfirm"
        @cancel="showTypePicker = false"
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

    <van-popup v-model:show="showStartDatePicker" position="bottom" round>
      <van-date-picker
        title="开始日期"
        @confirm="onStartDateConfirm"
        @cancel="showStartDatePicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showEndDatePicker" position="bottom" round>
      <van-date-picker
        title="结束日期"
        @confirm="onEndDateConfirm"
        @cancel="showEndDatePicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showPaymentPicker" position="bottom" round>
      <van-picker
        title="付款方式"
        :columns="paymentOptions"
        @confirm="onPaymentConfirm"
        @cancel="showPaymentPicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showSignDatePicker" position="bottom" round>
      <van-date-picker
        title="签订日期"
        @confirm="onSignDateConfirm"
        @cancel="showSignDatePicker = false"
      />
    </van-popup>

    <div class="form-actions">
      <van-button block type="default" @click="handleCancel">取消</van-button>
      <van-button block type="primary" @click="handleSubmit">提交审批</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { showToast, showSuccessToast } from 'vant'

const emit = defineEmits(['submit', 'cancel'])

const showTypePicker = ref(false)
const showCustomerPicker = ref(false)
const showStartDatePicker = ref(false)
const showEndDatePicker = ref(false)
const showPaymentPicker = ref(false)
const showSignDatePicker = ref(false)

const form = reactive({
  contractNo: '',
  contractName: '',
  contractType: '',
  customerName: '',
  customerId: 0,
  startDate: '',
  endDate: '',
  contractAmount: '',
  paymentMethod: '',
  signDate: '',
  signPerson: '',
  remark: '',
  terms: '',
  attachments: [] as any[]
})

const typeOptions = [
  { text: '销售合同', value: 'sales' },
  { text: '采购合同', value: 'purchase' },
  { text: '服务合同', value: 'service' },
  { text: '租赁合同', value: 'lease' }
]

const customerOptions = [
  { text: '北京科技有限公司', value: 1 },
  { text: '上海贸易公司', value: 2 },
  { text: '广州制造企业', value: 3 }
]

const paymentOptions = [
  { text: '一次性付款', value: 'once' },
  { text: '分期付款', value: 'installment' },
  { text: '预付款+尾款', value: 'prepaid' },
  { text: '月结', value: 'monthly' }
]

onMounted(() => {
  generateContractNo()
})

const generateContractNo = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0')
  form.contractNo = `CT${year}${month}${day}${random}`
}

const onTypeConfirm = ({ selectedOptions }: any) => {
  form.contractType = selectedOptions[0].text
  showTypePicker.value = false
}

const onCustomerConfirm = ({ selectedOptions }: any) => {
  form.customerName = selectedOptions[0].text
  form.customerId = selectedOptions[0].value
  showCustomerPicker.value = false
}

const onStartDateConfirm = ({ selectedValues }: any) => {
  form.startDate = selectedValues.join('-')
  showStartDatePicker.value = false
}

const onEndDateConfirm = ({ selectedValues }: any) => {
  form.endDate = selectedValues.join('-')
  showEndDatePicker.value = false
}

const onPaymentConfirm = ({ selectedOptions }: any) => {
  form.paymentMethod = selectedOptions[0].text
  showPaymentPicker.value = false
}

const onSignDateConfirm = ({ selectedValues }: any) => {
  form.signDate = selectedValues.join('-')
  showSignDatePicker.value = false
}

const handleCancel = () => {
  emit('cancel')
}

const handleSubmit = () => {
  if (!form.contractName || !form.contractType || !form.customerName || !form.startDate || !form.endDate || !form.contractAmount) {
    showToast('请填写必填项')
    return
  }
  emit('submit', form)
  showSuccessToast('合同已提交审批')
}
</script>

<style scoped lang="scss">
.contract-form {
  padding: 12px;
}

.step-content {
  .step-title {
    font-size: 14px;
    font-weight: 500;
    color: #333;
  }

  .step-user {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }
}

.form-actions {
  display: flex;
  gap: 12px;
  padding: 16px;
}
</style>