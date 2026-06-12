<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <a-breadcrumb>
          <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
          <a-breadcrumb-item><router-link to="/mall">商城管理</router-link></a-breadcrumb-item>
          <a-breadcrumb-item>商城配置</a-breadcrumb-item>
        </a-breadcrumb>
        <div class="page-header__right">
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
          <span v-if="autoRefreshCountdown > 0" class="page-header__countdown"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <span v-if="lastUpdateTime" class="page-header__update-time">更新于: {{ lastUpdateTime }}</span>
        </div>
      </div>
    </template>

    <div class="config-page">
      <a-spin :spinning="loading">
        <a-form
          ref="formRef"
          :model="formState"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 16 }"
          :rules="formRules"
          label-align="right"
        >
          <a-card title="基础信息" :bordered="false" class="config-card">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="商城名称" name="shopName">
                  <a-input v-model:value="formState.shopName" placeholder="请输入商城名称" :maxlength="100" size="small" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="主题色" name="themeColor">
                  <a-input v-model:value="formState.themeColor" placeholder="#1890ff" size="small">
                    <template #prefix>
                      <div class="color-preview" :style="{ backgroundColor: formState.themeColor || '#1890ff' }" />
                    </template>
                  </a-input>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="商城描述" name="shopDesc">
              <a-textarea v-model:value="formState.shopDesc" placeholder="请输入商城描述" :rows="3" :maxlength="500" size="small" />
            </a-form-item>
            <a-form-item label="商城LOGO" name="shopLogo">
              <a-input v-model:value="formState.shopLogo" placeholder="请输入LOGO图片URL" size="small" />
            </a-form-item>
          </a-card>

          <a-card title="交易设置" :bordered="false" class="config-card">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="最小起订金额" name="minOrderAmount">
                  <a-input-number
                    v-model:value="formState.minOrderAmount"
                    :min="0"
                    :precision="2"
                    :style="{ width: '100%' }"
                    placeholder="0=不限制"
                    size="small"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="免运费金额" name="freeShippingAmount">
                  <a-input-number
                    v-model:value="formState.freeShippingAmount"
                    :min="0"
                    :precision="2"
                    :style="{ width: '100%' }"
                    placeholder="0=不免运费"
                    size="small"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="固定运费" name="freightAmount">
                  <a-input-number
                    v-model:value="formState.freightAmount"
                    :min="0"
                    :precision="2"
                    :style="{ width: '100%' }"
                    size="small"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="支付方式" name="paymentMethods">
                  <a-select v-model:value="formState.paymentMethods" mode="multiple" placeholder="选择支付方式" size="small">
                    <a-select-option value="offline">线下转账</a-select-option>
                    <a-select-option value="wechat">微信支付</a-select-option>
                    <a-select-option value="alipay">支付宝</a-select-option>
                  </a-select>
                  <div class="form-tip">逗号分隔的支付方式</div>
                </a-form-item>
              </a-col>
            </a-row>
          </a-card>

          <a-card title="注册设置" :bordered="false" class="config-card">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="开放注册" name="enableRegister">
                  <a-switch v-model:checked="formState.enableRegister" :checked-value="1" :un-checked-value="0" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="自动审核" name="enableAutoAudit">
                  <a-switch v-model:checked="formState.enableAutoAudit" :checked-value="1" :un-checked-value="0" />
                  <div class="form-tip">开启后，新注册用户自动通过审核</div>
                </a-form-item>
              </a-col>
            </a-row>
          </a-card>

          <div class="form-actions">
            <a-button v-permission="'erp:mall:config:save'" type="primary" size="small" :loading="saveLoading" @click="debounceClick('save', handleSave)">保存配置</a-button>
            <a-button size="small" @click="debounceClick('reset', handleReset)" :style="{ marginLeft: '12px' }">重置</a-button>
          </div>
        </a-form>
      </a-spin>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'MallConfig' })

import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { SyncOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { shopConfigApi, type ShopConfig } from '@/api/erp/mall'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const loading = ref(false)
const saveLoading = ref(false)
const formRef = ref<FormInstance>()
const autoRefreshCountdown = ref(0)

const formState = reactive<ShopConfig>({
  shopName: '',
  shopLogo: '',
  shopDesc: '',
  themeColor: '#1890ff',
  paymentMethods: 'offline',
  enableRegister: 1,
  enableAutoAudit: 0,
  minOrderAmount: 0,
  freeShippingAmount: 0,
  freightAmount: 0
})

const formRules: Record<string, any> = {
  shopName: { required: true, message: '请输入商城名称', trigger: 'blur' }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await shopConfigApi.get()
    if (res?.data) {
      const data = { ...res.data }
      // 处理支付方式：后端存逗号分隔，前端是数组
      if (typeof data.paymentMethods === 'string') {
        (data as any).paymentMethods = data.paymentMethods ? data.paymentMethods.split(',') : ['offline']
      }
      Object.assign(formState, data)
    }
  } catch (err) {
    console.warn('[商城配置] 加载配置失败', err)
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  try {
    await formRef.value?.validate()
    saveLoading.value = true

    const data = { ...formState }
    // 支付方式数组转逗号分隔
    if (Array.isArray(data.paymentMethods)) {
      data.paymentMethods = (data.paymentMethods as any).join(',')
    }

    await shopConfigApi.update(data as ShopConfig)
    message.success('保存成功')
  } catch (err) {
    console.warn('[商城配置] 保存失败', err)
  } finally {
    saveLoading.value = false
  }
}

const handleReset = () => {
  fetchData()
  message.info('已重置')
}

// ── 键盘快捷键 ──
const lastUpdateTime = ref('')
function handleKeydown(e: KeyboardEvent) {
  if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return
  if (e.key === 'F5') {
    e.preventDefault()
    fetchData().then(() => message.info('已刷新'))
  }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => { fetchData(); autoRefreshCountdown.value = 30 }, 30000)
  countdownTimer = setInterval(() => { if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value-- }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

function handleError(err: any) { console.warn('[MallConfig]', err) }

defineExpose({ fetchData })
</script>

<style scoped>
.config-page {
  padding: 16px;
  max-width: 960px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.page-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header__update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.page-header__countdown {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.config-card {
  margin-bottom: 16px;
}

.config-card :deep(.ant-card-head-title) {
  font-size: 14px;
  font-weight: 600;
}

.color-preview {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  vertical-align: middle;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  line-height: 1.4;
}

.form-actions {
  padding: 16px 0;
  text-align: center;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>
