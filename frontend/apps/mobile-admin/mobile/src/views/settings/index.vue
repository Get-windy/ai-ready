<template>
  <div class="settings-page">
    <van-nav-bar
      title="系统设置"
      left-arrow
      @click-left="goBack"
    />

    <van-cell-group inset title="通知设置">
      <van-cell title="消息推送">
        <template #right-icon>
          <van-switch v-model="settings.pushEnabled" />
        </template>
      </van-cell>
      <van-cell title="审批通知">
        <template #right-icon>
          <van-switch v-model="settings.approvalNotify" />
        </template>
      </van-cell>
      <van-cell title="订单通知">
        <template #right-icon>
          <van-switch v-model="settings.orderNotify" />
        </template>
      </van-cell>
      <van-cell title="系统通知">
        <template #right-icon>
          <van-switch v-model="settings.systemNotify" />
        </template>
      </van-cell>
      <van-cell title="声音提醒">
        <template #right-icon>
          <van-switch v-model="settings.soundEnabled" />
        </template>
      </van-cell>
      <van-cell title="震动提醒">
        <template #right-icon>
          <van-switch v-model="settings.vibrationEnabled" />
        </template>
      </van-cell>
    </van-cell-group>

    <van-cell-group inset title="显示设置">
      <van-cell title="语言" is-link :value="settings.language" @click="showLanguagePicker = true" />
      <van-cell title="主题" is-link :value="settings.theme" @click="showThemePicker = true" />
      <van-cell title="字体大小" is-link :value="settings.fontSize" @click="showFontSizePicker = true" />
    </van-cell-group>

    <van-cell-group inset title="安全设置">
      <van-cell title="修改密码" is-link @click="changePassword" />
      <van-cell title="手势密码">
        <template #right-icon>
          <van-switch v-model="settings.gesturePassword" />
        </template>
      </van-cell>
      <van-cell title="自动锁定" is-link :value="settings.autoLock" @click="showAutoLockPicker = true" />
      <van-cell title="登录记录" is-link @click="viewLoginHistory" />
    </van-cell-group>

    <van-cell-group inset title="数据设置">
      <van-cell title="数据同步">
        <template #right-icon>
          <van-switch v-model="settings.dataSync" />
        </template>
      </van-cell>
      <van-cell title="离线模式">
        <template #right-icon>
          <van-switch v-model="settings.offlineMode" />
        </template>
      </van-cell>
      <van-cell title="清除缓存" is-link @click="clearCache" />
      <van-cell title="清除数据" is-link @click="clearData" />
    </van-cell-group>

    <van-cell-group inset title="其他设置">
      <van-cell title="检查更新" is-link @click="checkUpdate" />
      <van-cell title="关于我们" is-link @click="goAbout" />
      <van-cell title="隐私政策" is-link @click="goPrivacy" />
      <van-cell title="用户协议" is-link @click="goAgreement" />
    </van-cell-group>

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

    <van-popup v-model:show="showFontSizePicker" position="bottom" round>
      <van-picker
        title="字体大小"
        :columns="fontSizeOptions"
        @confirm="onFontSizeConfirm"
        @cancel="showFontSizePicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showAutoLockPicker" position="bottom" round>
      <van-picker
        title="自动锁定时间"
        :columns="autoLockOptions"
        @confirm="onAutoLockConfirm"
        @cancel="showAutoLockPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'

const router = useRouter()

const showLanguagePicker = ref(false)
const showThemePicker = ref(false)
const showFontSizePicker = ref(false)
const showAutoLockPicker = ref(false)

const settings = reactive({
  pushEnabled: true,
  approvalNotify: true,
  orderNotify: true,
  systemNotify: true,
  soundEnabled: true,
  vibrationEnabled: true,
  language: '简体中文',
  theme: '浅色模式',
  fontSize: '标准',
  gesturePassword: false,
  autoLock: '5分钟',
  dataSync: true,
  offlineMode: false
})

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

const fontSizeOptions = [
  { text: '小', value: 'small' },
  { text: '标准', value: 'normal' },
  { text: '大', value: 'large' },
  { text: '超大', value: 'extra-large' }
]

const autoLockOptions = [
  { text: '1分钟', value: '1' },
  { text: '5分钟', value: '5' },
  { text: '10分钟', value: '10' },
  { text: '30分钟', value: '30' },
  { text: '永不', value: 'never' }
]

onMounted(() => {
  loadSettings()
})

const loadSettings = () => {
  const savedSettings = localStorage.getItem('app_settings')
  if (savedSettings) {
    Object.assign(settings, JSON.parse(savedSettings))
  }
}

const saveSettings = () => {
  localStorage.setItem('app_settings', JSON.stringify(settings))
}

const goBack = () => router.back()

const changePassword = () => {
  router.push('/profile/change-password')
}

const viewLoginHistory = () => {
  router.push('/settings/login-history')
}

const clearCache = async () => {
  try {
    await showConfirmDialog({
      title: '清除缓存',
      message: '确定要清除缓存吗？'
    })
    localStorage.removeItem('cache_data')
    showSuccessToast('缓存已清除')
  } catch {
    // 用户取消
  }
}

const clearData = async () => {
  try {
    await showConfirmDialog({
      title: '清除数据',
      message: '确定要清除所有数据吗？此操作不可恢复！'
    })
    localStorage.clear()
    showSuccessToast('数据已清除')
    router.replace('/login')
  } catch {
    // 用户取消
  }
}

const checkUpdate = () => {
  showToast('已是最新版本')
}

const goAbout = () => {
  router.push('/about')
}

const goPrivacy = () => {
  router.push('/privacy')
}

const goAgreement = () => {
  router.push('/agreement')
}

const onLanguageConfirm = ({ selectedOptions }: any) => {
  settings.language = selectedOptions[0].text
  showLanguagePicker.value = false
  saveSettings()
}

const onThemeConfirm = ({ selectedOptions }: any) => {
  settings.theme = selectedOptions[0].text
  showThemePicker.value = false
  saveSettings()
}

const onFontSizeConfirm = ({ selectedOptions }: any) => {
  settings.fontSize = selectedOptions[0].text
  showFontSizePicker.value = false
  saveSettings()
}

const onAutoLockConfirm = ({ selectedOptions }: any) => {
  settings.autoLock = selectedOptions[0].text
  showAutoLockPicker.value = false
  saveSettings()
}
</script>

<style scoped lang="scss">
.settings-page {
  min-height: 100vh;
  background: #f5f5f5;
}
</style>