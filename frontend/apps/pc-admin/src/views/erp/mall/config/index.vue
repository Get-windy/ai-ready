<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <a-breadcrumb>
          <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
          <a-breadcrumb-item><router-link to="/erp/mall">商城管理</router-link></a-breadcrumb-item>
          <a-breadcrumb-item>商城配置</a-breadcrumb-item>
        </a-breadcrumb>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" style="font-size: 12px; color: #999;">更新于: {{ lastUpdateTime }}</span>
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
                  <a-input v-model:value="formState.shopName" placeholder="请输入商城名称" maxlength="100" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="主题色" name="themeColor">
                  <a-input v-model:value="formState.themeColor" placeholder="#1890ff">
                    <template #prefix>
                      <div class="color-preview" :style="{ backgroundColor: formState.themeColor || '#1890ff' }" />
                    </template>
                  </a-input>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="商城描述" name="shopDesc">
              <a-textarea v-model:value="formState.shopDesc" placeholder="请输入商城描述" :rows="3" maxlength="500" />
            </a-form-item>
            <a-form-item label="商城LOGO" name="shopLogo">
              <a-input v-model:value="formState.shopLogo" placeholder="请输入LOGO图片URL" />
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
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="支付方式" name="paymentMethods">
                  <a-select v-model:value="formState.paymentMethods" mode="multiple" placeholder="选择支付方式">
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
            <a-button v-permission="'erp:mall:config:save'" type="primary" :loading="saveLoading" @click="handleSave">保存配置</a-button>
            <a-button @click="handleReset" :style="{ marginLeft: '12px' }">重置</a-button>
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
import type { FormInstance } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { shopConfigApi, type ShopConfig } from '@/api/erp/mall'

const loading = ref(false)
const saveLoading = ref(false)
const formRef = ref<FormInstance>()

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

const formRules = {
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
        data.paymentMethods = data.paymentMethods ? data.paymentMethods.split(',') : ['offline']
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
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
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
</style>
