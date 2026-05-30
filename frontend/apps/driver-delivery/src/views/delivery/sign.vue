<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Field, Button, Uploader, showToast, showLoadingToast, closeToast, Dialog } from 'vant'
import { api } from '@/api'
import SignaturePad from '@/components/common/SignaturePad.vue'

const router = useRouter()
const route = useRoute()

const orderId = route.params.id as string

const receiverName = ref('')
const receiverPhone = ref('')
const remark = ref('')
const signatureImage = ref('')
const photos = ref<any[]>([])
const currentLocation = ref<{ lat: number; lng: number } | null>(null)

onMounted(() => {
  getCurrentLocation()
})

const getCurrentLocation = () => {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        currentLocation.value = {
          lat: position.coords.latitude,
          lng: position.coords.longitude
        }
      },
      (error) => {
        console.error('获取位置失败:', error)
      }
    )
  }
}

const handleSignatureComplete = (image: string) => {
  signatureImage.value = image
}

const handlePhotoUpload = (file: any) => {
  photos.value.push(file)
  return false
}

const handlePhotoDelete = (index: number) => {
  photos.value.splice(index, 1)
}

const handleSubmit = async () => {
  if (!receiverName.value) {
    showToast({ type: 'fail', message: '请输入签收人姓名' })
    return
  }
  
  if (!signatureImage.value) {
    showToast({ type: 'fail', message: '请完成签名' })
    return
  }
  
  Dialog.confirm({
    title: '签收确认',
    message: '确定提交签收信息吗？'
  }).then(async () => {
    showLoadingToast({ message: '提交中...', forbidClick: true })
    
    try {
      const photoUrls = photos.value.map(p => p.url || p.content)
      
      await api.sign.submit(orderId, {
        signatureImage: signatureImage.value,
        photos: photoUrls,
        receiverName: receiverName.value,
        receiverPhone: receiverPhone.value,
        remark: remark.value,
        location: currentLocation.value || { lat: 0, lng: 0 }
      })
      
      showToast({ type: 'success', message: '签收成功' })
      
      if (photos.value.length > 0) {
        router.push(`/delivery/${orderId}/collect`)
      } else {
        router.push('/order')
      }
    } finally {
      closeToast()
    }
  }).catch((err) => { console.error('签收操作失败:', err) })
}
</script>

<template>
  <div class="sign-page">
    <NavBar 
      title="签收" 
      left-arrow
      @click-left="router.back()"
    />
    
    <div class="sign-content">
      <div class="section-title">签收人信息</div>
      <Field
        v-model="receiverName"
        label="签收人"
        placeholder="请输入签收人姓名"
        required
      />
      <Field
        v-model="receiverPhone"
        label="联系电话"
        placeholder="请输入联系电话"
        type="tel"
      />
      
      <div class="section-title">签名</div>
      <div class="signature-container">
        <SignaturePad @complete="handleSignatureComplete" />
      </div>
      
      <div class="section-title">拍照凭证</div>
      <Uploader
        v-model="photos"
        :max-count="5"
        :after-read="handlePhotoUpload"
        @delete="handlePhotoDelete"
      />
      
      <div class="section-title">备注</div>
      <Field
        v-model="remark"
        label="备注"
        placeholder="请输入备注信息"
        type="textarea"
        rows="3"
      />
      
      <div class="submit-section">
        <Button 
          type="primary" 
          size="large"
          block
          @click="handleSubmit"
        >
          提交签收
        </Button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.sign-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.sign-content {
  padding: 12px;
}

.section-title {
  font-size: 14px;
  color: #969799;
  padding: 12px 0 8px;
}

.signature-container {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.submit-section {
  padding: 24px 12px;
}
</style>