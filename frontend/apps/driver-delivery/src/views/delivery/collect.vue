<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Field, RadioGroup, Radio, Button, Uploader, showToast, showLoadingToast, closeToast, Dialog } from 'vant'
import { api } from '@/api'

const router = useRouter()
const route = useRoute()

const orderId = route.params.id as string

const amount = ref(0)
const paymentMethod = ref('cash')
const remark = ref('')
const photos = ref<any[]>([])
const paymentMethods = ref<any[]>([])

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.collect.getPaymentMethods()
    paymentMethods.value = res.data || [
      { value: 'cash', label: '现金' },
      { value: 'wechat', label: '微信' },
      { value: 'alipay', label: '支付宝' },
      { value: 'bank', label: '银行转账' }
    ]
    
    const orderRes = await api.order.getDetail(orderId)
    amount.value = orderRes.data?.collectAmount || 0
  } finally {
    closeToast()
  }
})

const handlePhotoUpload = (file: any) => {
  photos.value.push(file)
  return false
}

const handleSubmit = async () => {
  if (amount.value <= 0) {
    showToast({ type: 'fail', message: '请输入收款金额' })
    return
  }
  
  Dialog.confirm({
    title: '收款确认',
    message: `确定收款 ¥${amount.value} 吗？`
  }).then(async () => {
    showLoadingToast({ message: '提交中...', forbidClick: true })
    
    try {
      const photoUrls = photos.value.map(p => p.url || p.content)
      
      await api.collect.submit(orderId, {
        amount: amount.value,
        paymentMethod: paymentMethod.value,
        remark: remark.value,
        photos: photoUrls
      })
      
      showToast({ type: 'success', message: '收款成功' })
      router.push('/order')
    } finally {
      closeToast()
    }
  }).catch(() => {})
}
</script>

<template>
  <div class="collect-page">
    <NavBar 
      title="收款" 
      left-arrow
      @click-left="router.back()"
    />
    
    <div class="collect-content">
      <div class="amount-display">
        <div class="amount-label">待收金额</div>
        <div class="amount-value">¥{{ amount }}</div>
      </div>
      
      <div class="section-title">收款金额</div>
      <Field
        v-model="amount"
        label="金额"
        placeholder="请输入收款金额"
        type="number"
        required
      />
      
      <div class="section-title">支付方式</div>
      <RadioGroup v-model="paymentMethod" class="payment-methods">
        <Radio 
          v-for="method in paymentMethods"
          :key="method.value"
          :name="method.value"
        >
          {{ method.label }}
        </Radio>
      </RadioGroup>
      
      <div class="section-title">拍照凭证</div>
      <Uploader
        v-model="photos"
        :max-count="3"
        :after-read="handlePhotoUpload"
      />
      
      <div class="section-title">备注</div>
      <Field
        v-model="remark"
        label="备注"
        placeholder="请输入备注信息"
        type="textarea"
        rows="2"
      />
      
      <div class="submit-section">
        <Button 
          type="primary" 
          size="large"
          block
          @click="handleSubmit"
        >
          确认收款
        </Button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.collect-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.collect-content {
  padding: 12px;
}

.amount-display {
  background: linear-gradient(135deg, #ff976a, #ffb88c);
  padding: 24px;
  border-radius: 8px;
  text-align: center;
  color: #fff;
  margin-bottom: 16px;
  
  .amount-label {
    font-size: 14px;
  }
  
  .amount-value {
    font-size: 32px;
    font-weight: 600;
    margin-top: 8px;
  }
}

.section-title {
  font-size: 14px;
  color: #969799;
  padding: 12px 0 8px;
}

.payment-methods {
  background: #fff;
  padding: 12px;
  border-radius: 8px;
}

.submit-section {
  padding: 24px 12px;
}
</style>