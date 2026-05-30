<template>
  <div class="profile-page">
    <van-nav-bar title="个人中心" />

    <div class="profile-header">
      <van-image
        round
        width="80"
        height="80"
        :src="userInfo.avatar || defaultAvatar"
        @click="changeAvatar"
      />
      <div class="user-name">{{ userInfo.name }}</div>
      <div class="user-role">{{ userInfo.roleName }}</div>
      <div class="user-department">{{ userInfo.department }}</div>
    </div>

    <van-cell-group inset title="个人信息">
      <van-cell title="用户名" :value="userInfo.username" />
      <van-cell title="姓名" :value="userInfo.name" />
      <van-cell title="手机号" :value="userInfo.phone" is-link @click="editPhone" />
      <van-cell title="邮箱" :value="userInfo.email" is-link @click="editEmail" />
      <van-cell title="部门" :value="userInfo.department" />
      <van-cell title="职位" :value="userInfo.position" />
    </van-cell-group>

    <van-cell-group inset title="工作统计">
      <van-grid :column-num="4" :border="false">
        <van-grid-item>
          <div class="stat-value">{{ userInfo.orderCount }}</div>
          <div class="stat-label">订单数</div>
        </van-grid-item>
        <van-grid-item>
          <div class="stat-value">{{ userInfo.customerCount }}</div>
          <div class="stat-label">客户数</div>
        </van-grid-item>
        <van-grid-item>
          <div class="stat-value">{{ userInfo.salesAmount }}</div>
          <div class="stat-label">销售额</div>
        </van-grid-item>
        <van-grid-item>
          <div class="stat-value">{{ userInfo.approvalCount }}</div>
          <div class="stat-label">审批数</div>
        </van-grid-item>
      </van-grid>
    </van-cell-group>

    <van-cell-group inset title="常用功能">
      <van-cell title="修改密码" is-link @click="changePassword" />
      <van-cell title="消息通知" is-link :value="notificationCount" @click="goNotification" />
      <van-cell title="我的审批" is-link @click="goMyApproval" />
      <van-cell title="我的订单" is-link @click="goMyOrder" />
      <van-cell title="我的客户" is-link @click="goMyCustomer" />
    </van-cell-group>

    <van-cell-group inset title="系统设置">
      <van-cell title="语言设置" is-link :value="language" @click="showLanguagePicker = true" />
      <van-cell title="主题设置" is-link :value="theme" @click="showThemePicker = true" />
      <van-cell title="通知设置" is-link @click="goNotificationSettings" />
      <van-cell title="清除缓存" is-link @click="clearCache" />
    </van-cell-group>

    <van-cell-group inset title="其他">
      <van-cell title="帮助中心" is-link @click="goHelp" />
      <van-cell title="关于我们" is-link @click="goAbout" />
      <van-cell title="意见反馈" is-link @click="goFeedback" />
    </van-cell-group>

    <div class="logout-btn">
      <van-button block type="danger" plain @click="handleLogout">退出登录</van-button>
    </div>

    <van-popup v-model:show="showLanguagePicker" position="bottom" round>
      <van-picker
        title="选择语言"
        :columns="languageOptions"
        @confirm="onLanguageConfirm"
        @cancel="showLanguagePicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showThemePicker" position="bottom" round>
      <van-picker
        title="选择主题"
        :columns="themeOptions"
        @confirm="onThemeConfirm"
        @cancel="showThemePicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast, showToast, showDialog } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
const notificationCount = ref(5)
const language = ref('简体中文')
const theme = ref('浅色模式')
const showLanguagePicker = ref(false)
const showThemePicker = ref(false)

const languageOptions = [
  { text: '简体中文', value: 'zh-CN' },
  { text: '繁体中文', value: 'zh-TW' },
  { text: 'English', value: 'en-US' }
]

const themeOptions = [
  { text: '浅色模式', value: 'light' },
  { text: '深色模式', value: 'dark' },
  { text: '跟随系统', value: 'auto' }
]

const userInfo = ref({
  id: 1,
  name: '张三',
  username: 'zhangsan',
  phone: '138****1234',
  email: 'zhangsan@example.com',
  department: '销售部',
  position: '销售经理',
  roleName: '管理员',
  avatar: '',
  orderCount: 28,
  customerCount: 15,
  salesAmount: '¥258万',
  approvalCount: 12
})

onMounted(() => {
  loadUserInfo()
})

const loadUserInfo = () => {
  if (userStore.userInfo) {
    userInfo.value = { ...userInfo.value, ...userStore.userInfo }
  }
}

const changeAvatar = () => {
  const input = document.createElement('input'); input.type = 'file'; input.accept = 'image/*'; input.onchange = (e) => { const f = e.target?.files?.[0]; if (f) { showToast('头像更新成功') } }; input.click()
}

const editPhone = () => {
  router.push('/profile/edit-phone')
}

const editEmail = () => {
  router.push('/profile/edit-email')
}

const changePassword = () => {
  router.push('/profile/change-password')
}

const goNotification = () => {
  router.push('/notification')
}

const goMyApproval = () => {
  router.push('/approval?tab=mine')
}

const goMyOrder = () => {
  router.push('/order')
}

const goMyCustomer = () => {
  router.push('/customer')
}

const goNotificationSettings = () => {
  router.push('/settings/notification')
}

const clearCache = async () => {
  try {
    await showConfirmDialog({
      title: '清除缓存',
      message: '确定要清除缓存吗？'
    })
    localStorage.clear()
    showSuccessToast('缓存已清除')
  } catch {
    // 用户取消
  }
}

const goHelp = () => {
  router.push('/help')
}

const goAbout = () => {
  router.push('/about')
}

const goFeedback = () => {
  router.push('/feedback')
}

const handleLogout = async () => {
  try {
    await showConfirmDialog({
      title: '退出登录',
      message: '确定要退出登录吗？'
    })
    userStore.logout()
    router.replace('/login')
  } catch {
    // 用户取消
  }
}

const onLanguageConfirm = ({ selectedOptions }: any) => {
  language.value = selectedOptions[0].text
  showLanguagePicker.value = false
}

const onThemeConfirm = ({ selectedOptions }: any) => {
  theme.value = selectedOptions[0].text
  showThemePicker.value = false
}
</script>

<style scoped lang="scss">
.profile-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.profile-header {
  padding: 32px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  text-align: center;
  color: #fff;

  .user-name {
    font-size: 20px;
    font-weight: 500;
    margin-top: 12px;
  }

  .user-role {
    font-size: 14px;
    opacity: 0.8;
    margin-top: 4px;
  }

  .user-department {
    font-size: 12px;
    opacity: 0.6;
    margin-top: 4px;
  }
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.logout-btn {
  padding: 16px;
  margin-top: 16px;
}
</style>