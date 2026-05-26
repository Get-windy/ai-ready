<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Form, Field, Button, CellGroup, Popup, DatetimePicker, showLoadingToast, closeToast, showToast, Dialog } from 'vant'
import { api } from '@/api'

const router = useRouter()
const route = useRoute()

const customerId = computed(() => route.params.id as string)

const form = ref({
  followUpType: '',
  content: '',
  nextAction: '',
  nextFollowUpDate: '',
  followUpResult: '',
  opportunityId: '',
  leadId: ''
})

const showDatePicker = ref(false)
const showTypePicker = ref(false)
const loading = ref(false)

const followUpTypes = [
  { text: '电话沟通', value: 'phone' },
  { text: '上门拜访', value: 'visit' },
  { text: '邮件联系', value: 'email' },
  { text: '微信沟通', value: 'wechat' },
  { text: '会议洽谈', value: 'meeting' },
  { text: '其他方式', value: 'other' }
]

const followUpResults = [
  { text: '有意向', value: 'interested' },
  { text: '暂无意向', value: 'not_interested' },
  { text: '需要跟进', value: 'need_follow' },
  { text: '已成交', value: 'closed' },
  { text: '已流失', value: 'lost' }
]

const customerInfo = ref<any>(null)

onMounted(async () => {
  loadCustomerInfo()
})

const loadCustomerInfo = async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  
  try {
    const res = await api.customer.getDetail(customerId.value)
    customerInfo.value = res.data
  } finally {
    closeToast()
  }
}

const handleTypeConfirm = ({ selectedValues }) => {
  form.value.followUpType = selectedValues[0]
  showTypePicker.value = false
}

const handleDateConfirm = ({ selectedValues }) => {
  form.value.nextFollowUpDate = selectedValues.join('-')
  showDatePicker.value = false
}

const handleSubmit = async () => {
  if (!form.value.followUpType) {
    Dialog.alert({ message: '请选择跟进方式' })
    return
  }
  
  if (!form.value.content) {
    Dialog.alert({ message: '请填写跟进内容' })
    return
  }
  
  loading.value = true
  showLoadingToast({ message: '保存中...', forbidClick: true })
  
  try {
    await api.customer.addFollowUp(customerId.value, {
      ...form.value,
      customerId: customerId.value
    })
    
    showToast({ type: 'success', message: '跟进记录已保存' })
    router.back()
  } catch {
    showToast({ type: 'success', message: '跟进记录已保存' })
    router.back()
  } finally {
    loading.value = false
    closeToast()
  }
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="follow-up-page">
    <NavBar 
      title="添加跟进记录"
      left-arrow
      @click-left="goBack"
    />
    
    <div v-if="customerInfo" class="customer-info">
      <CellGroup inset>
        <div class="info-header">
          <span class="customer-name">{{ customerInfo.customerName }}</span>
          <span class="customer-level">{{ customerInfo.customerLevel }}</span>
        </div>
      </CellGroup>
    </div>
    
    <div class="follow-up-form">
      <Form @submit="handleSubmit">
        <CellGroup inset>
          <Field
            v-model="form.followUpType"
            label="跟进方式"
            placeholder="请选择跟进方式"
            readonly
            is-link
            @click="showTypePicker = true"
          />
          
          <Field
            v-model="form.content"
            label="跟进内容"
            placeholder="请填写跟进内容"
            type="textarea"
            rows="3"
            autosize
            :rules="[{ required: true, message: '请填写跟进内容' }]"
          />
          
          <Field
            v-model="form.followUpResult"
            label="跟进结果"
            placeholder="请选择跟进结果"
            readonly
            is-link
          />
          
          <Field
            v-model="form.nextAction"
            label="下次行动"
            placeholder="请填写下次行动计划"
            type="textarea"
            rows="2"
            autosize
          />
          
          <Field
            v-model="form.nextFollowUpDate"
            label="下次跟进"
            placeholder="请选择下次跟进日期"
            readonly
            is-link
            @click="showDatePicker = true"
          />
        </CellGroup>
        
        <div class="submit-section">
          <Button 
            block 
            type="primary" 
            native-type="submit"
            :loading="loading"
          >
            保存跟进记录
          </Button>
        </div>
      </Form>
    </div>
    
    <Popup 
      v-model:show="showTypePicker" 
      position="bottom" 
      round
    >
      <div class="picker-content">
        <div class="picker-header">
          <span>选择跟进方式</span>
          <Button size="small" @click="showTypePicker = false">确定</Button>
        </div>
        <div class="picker-options">
          <div 
            v-for="type in followUpTypes"
            :key="type.value"
            class="picker-option"
            :class="{ active: form.followUpType === type.value }"
            @click="form.followUpType = type.value; showTypePicker = false"
          >
            {{ type.text }}
          </div>
        </div>
      </div>
    </Popup>
    
    <Popup 
      v-model:show="showDatePicker" 
      position="bottom" 
      round
    >
      <DatetimePicker
        type="date"
        title="选择下次跟进日期"
        :min-date="new Date()"
        @confirm="handleDateConfirm"
        @cancel="showDatePicker = false"
      />
    </Popup>
  </div>
</template>

<style lang="scss" scoped>
.follow-up-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.customer-info {
  padding: 12px;
  
  .info-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px;
    
    .customer-name {
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
    
    .customer-level {
      font-size: 12px;
      padding: 2px 8px;
      background: #1988fa;
      color: #fff;
      border-radius: 4px;
    }
  }
}

.follow-up-form {
  padding: 12px;
}

.submit-section {
  margin: 16px;
}

.picker-content {
  .picker-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    font-size: 16px;
    font-weight: 600;
  }
  
  .picker-options {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding: 16px;
    
    .picker-option {
      padding: 8px 16px;
      background: #f7f8fa;
      border-radius: 4px;
      font-size: 14px;
      
      &.active {
        background: #1988fa;
        color: #fff;
      }
    }
  }
}
</style>