<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Form, Field, CellGroup, Button, Uploader, Dialog, showLoadingToast, closeToast } from 'vant'
import type { UploaderFileListItem } from 'vant'
import { useUserStore } from '@/stores/user'
import { api } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  nickname: '',
  phone: '',
  email: '',
  gender: '',
  birthday: '',
  avatar: ''
})

const avatarFile = ref<UploaderFileListItem[]>([])

onMounted(() => {
  if (userStore.user) {
    form.value = {
      nickname: userStore.user.nickname || '',
      phone: userStore.user.phone || '',
      email: userStore.user.email || '',
      gender: userStore.user.gender || '',
      birthday: userStore.user.birthday || '',
      avatar: userStore.user.avatar || ''
    }
    
    if (form.value.avatar) {
      avatarFile.value = [{ url: form.value.avatar }]
    }
  }
})

const handleAvatarChange = (file: any) => {
  if (file && file.url) {
    form.value.avatar = file.url
  }
}

const handleSave = async () => {
  if (!form.value.nickname) {
    Dialog.alert({ message: '请输入昵称' })
    return
  }
  
  showLoadingToast({ message: '保存中...', forbidClick: true, duration: 0 })
  
  try {
    await api.user.updateProfile(form.value)
    userStore.setUser({ ...userStore.user, ...form.value } as any)
    Dialog.alert({ message: '保存成功' }).then(() => {
      router.back()
    })
  } catch (err) {
    console.warn('[个人信息] 保存失败', err)
    Dialog.alert({ message: '保存失败，请检查网络连接' })
  } finally {
    closeToast()
  }
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="profile-page">
    <NavBar 
      title="个人信息"
      left-arrow
      @click-left="goBack"
    />
    
    <div class="profile-content">
      <CellGroup inset>
        <div class="avatar-section">
          <Uploader 
            v-model="avatarFile"
            :max-count="1"
            :deletable="true"
            :preview-size="80"
            @after-read="handleAvatarChange"
          />
          <div class="avatar-tip">点击更换头像</div>
        </div>
        
        <Form @submit="handleSave">
          <Field
            v-model="form.nickname"
            label="昵称"
            placeholder="请输入昵称"
            required
            :rules="[{ required: true, message: '请输入昵称' }]"
          />
          
          <Field
            v-model="form.phone"
            label="手机号"
            placeholder="请输入手机号"
            type="tel"
            :rules="[{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' }]"
          />
          
          <Field
            v-model="form.email"
            label="邮箱"
            placeholder="请输入邮箱"
            type="email"
          />
          
          <Field
            v-model="form.gender"
            label="性别"
            placeholder="请选择性别"
            readonly
            is-link
            @click="() => {}"
          />
          
          <Field
            v-model="form.birthday"
            label="生日"
            placeholder="请选择生日"
            readonly
            is-link
            @click="() => {}"
          />
          
          <div class="submit-section">
            <Button 
              block 
              type="primary" 
              native-type="submit"
            >
              保存
            </Button>
          </div>
        </Form>
      </CellGroup>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.profile-content {
  padding: 12px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: #fff;
  
  .avatar-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #969799;
  }
}

.submit-section {
  margin: 16px;
}
</style>