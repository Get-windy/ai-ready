<template>
  <ErrorBoundary>
    <PageContainer title="基础设置">
      <a-card
        :bordered="false"
        class="config-card"
      >
        <a-spin :spinning="loading">
          <a-form
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-form-item label="商城状态">
              <a-switch
                :checked="form.status === 1"
                checked-children="启用"
                un-checked-children="停用"
                @change="(v: any) => (form.status = v ? 1 : 0)"
              />
              <div class="form-tip">
                停用后买家端将无法访问商城
              </div>
            </a-form-item>
            <a-form-item label="开放注册">
              <a-switch
                :checked="form.enableRegister === 1"
                checked-children="开放"
                un-checked-children="关闭"
                @change="(v: any) => (form.enableRegister = v ? 1 : 0)"
              />
              <div class="form-tip">
                关闭后新买家无法自助注册账号
              </div>
            </a-form-item>
            <a-form-item label="注册自动审核">
              <a-switch
                :checked="form.enableAutoAudit === 1"
                checked-children="自动"
                un-checked-children="人工"
                @change="(v: any) => (form.enableAutoAudit = v ? 1 : 0)"
              />
              <div class="form-tip">
                关闭后注册申请需在「买家申请管理」人工审核
              </div>
            </a-form-item>
            <a-form-item label="最小起订金额">
              <a-input-number
                v-model:value="form.minOrderAmount"
                :min="0"
                :precision="2"
                style="width: 240px"
                placeholder="0 表示不限制"
              />
              <span class="form-unit">元</span>
            </a-form-item>
            <a-form-item label="支付方式">
              <a-checkbox-group
                v-model:value="paymentMethodList"
                :options="PAYMENT_METHOD_OPTIONS"
              />
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 6, span: 14 }">
              <a-space>
                <a-button
                  type="primary"
                  :loading="saving"
                  @click="handleSave"
                >
                  保存设置
                </a-button>
                <a-button @click="loadConfig">
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-spin>
      </a-card>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { shopConfigApi, type ShopConfig } from '@/api/erp/mall'

defineOptions({ name: 'MallBasicConfig' })

// ═══ 支付方式（与后端 ShopConfig.paymentMethods 逗号分隔存储一致） ═══
const PAYMENT_METHOD_OPTIONS = [
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信支付', value: 'WECHAT' },
  { label: '银联支付', value: 'UNIONPAY' },
  { label: '银行转账', value: 'BANK' },
  { label: '货到付款', value: 'CASH' }
]

const loading = ref(false)
const saving = ref(false)
const configId = ref<number | undefined>(undefined)
const paymentMethodList = ref<string[]>([])

const form = reactive({
  status: 1 as number,
  enableRegister: 1 as number,
  enableAutoAudit: 0 as number,
  minOrderAmount: 0 as number
})

// 完整配置缓存：保存时合并，避免覆盖其他设置页维护的字段
let fullConfig: ShopConfig | null = null

async function loadConfig() {
  loading.value = true
  try {
    const res: any = await shopConfigApi.get()
    const cfg: ShopConfig | null = res?.data ?? res ?? null
    fullConfig = cfg
    configId.value = cfg?.id
    form.status = cfg?.status ?? 1
    form.enableRegister = cfg?.enableRegister ?? 1
    form.enableAutoAudit = cfg?.enableAutoAudit ?? 0
    form.minOrderAmount = Number(cfg?.minOrderAmount) || 0
    paymentMethodList.value = cfg?.paymentMethods
      ? String(cfg.paymentMethods).split(',').filter(Boolean)
      : ['ALIPAY', 'WECHAT']
  } catch (e) {
    console.warn('[基础设置] 商城配置获取失败', e)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!paymentMethodList.value.length) {
    message.warning('请至少选择一种支付方式')
    return
  }
  saving.value = true
  try {
    const payload: ShopConfig = {
      ...(fullConfig || {}),
      id: configId.value,
      shopName: fullConfig?.shopName || '订货商城',
      status: form.status,
      enableRegister: form.enableRegister,
      enableAutoAudit: form.enableAutoAudit,
      minOrderAmount: form.minOrderAmount,
      paymentMethods: paymentMethodList.value.join(',')
    }
    await shopConfigApi.update(payload)
    message.success('基础设置已保存')
    loadConfig()
  } catch (e) {
    console.warn('[基础设置] 保存失败', e)
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<style scoped>
.config-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
.form-unit {
  margin-left: 8px;
  color: #606266;
}
</style>
