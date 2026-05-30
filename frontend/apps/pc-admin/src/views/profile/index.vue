<template>
  <div class="profile-page">
    <a-row :gutter="16">
      <!-- 左侧：用户信息卡片 -->
      <a-col :xs="24" :lg="8">
        <a-card :bordered="false" class="profile-card">
          <div class="profile-header">
            <a-avatar :src="userInfo.avatar" :size="80">
              {{ userInfo.nickname?.charAt(0) || userInfo.username?.charAt(0) }}
            </a-avatar>
            <h2 class="profile-name">{{ userInfo.nickname || userInfo.username }}</h2>
            <p class="profile-username">@{{ userInfo.username }}</p>
          </div>

          <a-divider />

          <a-descriptions :column="1" size="small" :colon="false" class="profile-desc">
            <a-descriptions-item label="角色">
              <a-tag v-for="role in userInfo.roleNames" :key="role" color="blue">
                {{ role }}
              </a-tag>
              <span v-if="!userInfo.roleNames?.length" style="color: #999">未分配</span>
            </a-descriptions-item>
            <a-descriptions-item label="部门">
              {{ userInfo.deptName || '未分配' }}
            </a-descriptions-item>
            <a-descriptions-item label="手机">
              {{ userInfo.phone || '未设置' }}
            </a-descriptions-item>
            <a-descriptions-item label="邮箱">
              {{ userInfo.email || '未设置' }}
            </a-descriptions-item>
          </a-descriptions>

          <a-divider />

          <a-button block @click="showEditProfile = true">
            <template #icon><EditOutlined /></template>
            编辑资料
          </a-button>
        </a-card>
      </a-col>

      <!-- 右侧：设置区域 -->
      <a-col :xs="24" :lg="16">
        <!-- 修改密码 -->
        <a-card :bordered="false" class="setting-card" title="修改密码">
          <a-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-form-item label="当前密码" name="oldPassword">
              <a-input-password
                v-model:value="passwordForm.oldPassword"
                placeholder="请输入当前密码"
              />
            </a-form-item>
            <a-form-item label="新密码" name="newPassword">
              <a-input-password
                v-model:value="passwordForm.newPassword"
                placeholder="请输入新密码"
              />
            </a-form-item>
            <a-form-item label="确认密码" name="confirmPassword">
              <a-input-password
                v-model:value="passwordForm.confirmPassword"
                placeholder="请再次输入新密码"
              />
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 6, span: 14 }">
              <a-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
                修改密码
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- 个人偏好 -->
        <a-card :bordered="false" class="setting-card" title="个人偏好">
          <a-form
            :model="preferenceForm"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-form-item label="语言">
              <a-select
                v-model:value="preferenceForm.language"
                style="width: 100%"
                @change="handlePreferenceChange"
              >
                <a-select-option value="zh-CN">简体中文</a-select-option>
                <a-select-option value="en-US">English</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="主题">
              <a-radio-group
                v-model:value="preferenceForm.theme"
                @change="handlePreferenceChange"
              >
                <a-radio-button value="light">
                  <SunOutlined /> 浅色
                </a-radio-button>
                <a-radio-button value="dark">
                  <MoonOutlined /> 深色
                </a-radio-button>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="布局模式">
              <a-radio-group
                v-model:value="preferenceForm.layoutMode"
                @change="handlePreferenceChange"
              >
                <a-radio-button value="side">侧边栏</a-radio-button>
                <a-radio-button value="top">顶部导航</a-radio-button>
                <a-radio-button value="mix">混合模式</a-radio-button>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="标签页">
              <a-switch
                v-model:checked="preferenceForm.tagsView"
                checked-children="开"
                un-checked-children="关"
                @change="handlePreferenceChange"
              />
              <span style="margin-left: 8px; color: #999; font-size: 12px">
                开启后显示标签导航
              </span>
            </a-form-item>
            <a-form-item label="固定头部">
              <a-switch
                v-model:checked="preferenceForm.fixedHeader"
                checked-children="开"
                un-checked-children="关"
                @change="handlePreferenceChange"
              />
              <span style="margin-left: 8px; color: #999; font-size: 12px">
                开启后页面头部将在滚动时固定
              </span>
            </a-form-item>
            <a-form-item label="侧边栏Logo">
              <a-switch
                v-model:checked="preferenceForm.sidebarLogo"
                checked-children="开"
                un-checked-children="关"
                @change="handlePreferenceChange"
              />
              <span style="margin-left: 8px; color: #999; font-size: 12px">
                关闭后隐藏侧边栏Logo
              </span>
            </a-form-item>
            <a-form-item label="主题色">
              <a-space>
                <div
                  v-for="color in presetColors"
                  :key="color"
                  class="color-block"
                  :class="{ active: preferenceForm.primaryColor === color }"
                  :style="{ backgroundColor: color }"
                  @click="selectPrimaryColor(color)"
                />
                <a-input
                  v-model:value="preferenceForm.primaryColor"
                  style="width: 100px"
                  size="small"
                  @change="handlePreferenceChange"
                />
              </a-space>
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 6, span: 14 }">
              <a-button type="primary" :loading="preferenceLoading" @click="handleSavePreferences">
                保存偏好设置
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
    </a-row>

    <!-- 编辑个人资料弹窗 -->
    <a-modal
      v-model:open="showEditProfile"
      title="编辑个人资料"
      :confirm-loading="profileLoading"
      width="500px"
      @ok="handleSaveProfile"
    >
      <a-form
        ref="profileFormRef"
        :model="profileForm"
        :rules="profileFormRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="头像">
          <a-upload
            v-model:file-list="avatarFileList"
            list-type="picture-card"
            :max-count="1"
            :before-upload="handleBeforeUpload"
            @remove="handleAvatarRemove"
          >
            <div v-if="avatarFileList.length < 1">
              <PlusOutlined />
              <div style="margin-top: 8px">上传</div>
            </div>
          </a-upload>
        </a-form-item>
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="profileForm.nickname" placeholder="请输入昵称" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="profileForm.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="profileForm.phone" placeholder="请输入手机号" />
        </a-form-item>
        <a-form-item label="性别" name="gender">
          <a-radio-group v-model:value="profileForm.gender">
            <a-radio :value="0">未知</a-radio>
            <a-radio :value="1">男</a-radio>
            <a-radio :value="2">女</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance, UploadProps } from 'ant-design-vue'
import {
  EditOutlined,
  PlusOutlined,
  SunOutlined,
  MoonOutlined
} from '@ant-design/icons-vue'
import { profileApi, type ProfileInfo, type PreferenceSettings } from '@/api/profile'

// 用户信息
const userInfo = reactive<ProfileInfo>({
  id: 0,
  username: '',
  nickname: '',
  avatar: '',
  email: '',
  phone: '',
  gender: 0,
  roleNames: [],
  deptName: ''
})

// ==================== 修改密码 ====================
const passwordFormRef = ref<FormInstance>()
const passwordLoading = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule: any, value: string) => {
  if (value !== passwordForm.newPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

const passwordRules = {
  oldPassword: { required: true, message: '请输入当前密码', trigger: 'blur' },
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleChangePassword = async () => {
  try {
    await passwordFormRef.value?.validate()
    passwordLoading.value = true
    await profileApi.changePassword(passwordForm)
    message.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error) {
    message.error('修改密码失败')
  } finally {
    passwordLoading.value = false
  }
}

// ==================== 偏好设置 ====================
const preferenceLoading = ref(false)

const preferenceForm = reactive<PreferenceSettings>({
  language: 'zh-CN',
  theme: 'light',
  layoutMode: 'side',
  primaryColor: '#1890ff',
  tagsView: true,
  fixedHeader: true,
  sidebarLogo: true
})

const presetColors = [
  '#1890ff', '#f5222d', '#fa541c', '#faad14',
  '#13c2c2', '#52c41a', '#2f54eb', '#722ed1'
]

const selectPrimaryColor = (color: string) => {
  preferenceForm.primaryColor = color
  handlePreferenceChange()
}

const handlePreferenceChange = () => {
  // 立即应用本地主题（颜色等）
  if (preferenceForm.theme === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark')
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
}

const handleSavePreferences = async () => {
  preferenceLoading.value = true
  try {
    await profileApi.updatePreferences({ ...preferenceForm })
    message.success('偏好设置保存成功')
  } catch {
    message.error('保存失败')
  } finally {
    preferenceLoading.value = false
  }
}

// ==================== 编辑个人资料 ====================
const showEditProfile = ref(false)
const profileLoading = ref(false)
const profileFormRef = ref<FormInstance>()
const avatarFileList = ref<any[]>([])

const profileForm = reactive({
  nickname: '',
  email: '',
  phone: '',
  gender: 0
})

const profileFormRules = {
  email: { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  phone: { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
}

const handleSaveProfile = async () => {
  try {
    await profileFormRef.value?.validate()
    profileLoading.value = true

    const data = { ...profileForm }

    await profileApi.updateProfile(data)
    message.success('个人资料更新成功')

    // 刷新用户信息
    await fetchProfile()

    showEditProfile.value = false
  } catch (error) {
    message.error('保存失败')
  } finally {
    profileLoading.value = false
  }
}

const handleBeforeUpload: UploadProps['beforeUpload'] = async (file) => {
  try {
    const res = await profileApi.uploadAvatar(file)
    if (res.data) {
      userInfo.avatar = res.data
      message.success('头像上传成功')
    }
  } catch {
    message.error('头像上传失败')
  }
  return false
}

const handleAvatarRemove = () => {
  userInfo.avatar = ''
}

// ==================== 数据加载 ====================
const fetchProfile = async () => {
  try {
    const res = await profileApi.getProfile()
    if (res.data) {
      Object.assign(userInfo, res.data)
      Object.assign(profileForm, {
        nickname: res.data.nickname,
        email: res.data.email,
        phone: res.data.phone,
        gender: res.data.gender
      })
    }
  } catch {
    message.error('加载个人信息失败')
  }
}

const fetchPreferences = async () => {
  try {
    const res = await profileApi.getPreferences()
    if (res.data) {
      Object.assign(preferenceForm, res.data)
      handlePreferenceChange()
    }
  } catch {
    // 使用默认设置
  }
}

onMounted(() => {
  fetchProfile()
  fetchPreferences()
})
</script>

<style scoped>
.profile-page {
  padding: 0;
}

.profile-card {
  text-align: center;
}

.profile-header {
  padding: 16px 0;
}

.profile-name {
  margin: 12px 0 4px;
  font-size: 20px;
  font-weight: 600;
}

.profile-username {
  color: #999;
  margin: 0;
  font-size: 13px;
}

.profile-desc {
  text-align: left;
}

.profile-desc :deep(.ant-descriptions-item-label) {
  color: #999;
}

.setting-card {
  margin-bottom: 16px;
}

.color-block {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.color-block.active {
  border-color: #000;
  transform: scale(1.2);
}

.color-block:hover {
  transform: scale(1.1);
}
</style>
