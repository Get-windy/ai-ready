<template>
  <ErrorBoundary>
    <PageContainer title="运费设置">
      <a-card
        :bordered="false"
        class="config-card"
      >
        <a-alert
          type="info"
          show-icon
          message="运费规则"
          description="订单金额达到「免运费金额」时免收运费，否则按「固定运费」收取。免运费金额设为 0 表示全部订单免运费。"
          style="margin-bottom: 24px"
        />
        <a-spin :spinning="loading">
          <a-form
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-form-item label="免运费金额">
              <a-input-number
                v-model:value="form.freeShippingAmount"
                :min="0"
                :precision="2"
                style="width: 240px"
              />
              <span class="form-unit">元（订单满此金额免运费，0 为全部免运费）</span>
            </a-form-item>
            <a-form-item label="固定运费">
              <a-input-number
                v-model:value="form.freightAmount"
                :min="0"
                :precision="2"
                style="width: 240px"
              />
              <span class="form-unit">元（未达免运费金额时收取）</span>
            </a-form-item>
            <a-form-item label="当前规则预览">
              <span class="rule-preview">{{ rulePreview }}</span>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { shopConfigApi, type ShopConfig } from '@/api/erp/mall'

defineOptions({ name: 'MallFreightConfig' })

const loading = ref(false)
const saving = ref(false)
const configId = ref<number | undefined>(undefined)

const form = reactive({
  freeShippingAmount: 0 as number,
  freightAmount: 0 as number
})

// 完整配置缓存：保存时合并，避免覆盖其他设置页维护的字段
let fullConfig: ShopConfig | null = null

const rulePreview = computed(() => {
  const free = Number(form.freeShippingAmount) || 0
  const freight = Number(form.freightAmount) || 0
  if (free <= 0) return '全部订单免运费'
  return `订单满 ¥${free.toLocaleString('zh-CN', { minimumFractionDigits: 2 })} 免运费，未满收运费 ¥${freight.toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
})

async function loadConfig() {
  loading.value = true
  try {
    const res: any = await shopConfigApi.get()
    const cfg: ShopConfig | null = res?.data ?? res ?? null
    fullConfig = cfg
    configId.value = cfg?.id
    form.freeShippingAmount = Number(cfg?.freeShippingAmount) || 0
    form.freightAmount = Number(cfg?.freightAmount) || 0
  } catch (e) {
    console.warn('[运费设置] 商城配置获取失败', e)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const payload: ShopConfig = {
      ...(fullConfig || {}),
      id: configId.value,
      shopName: fullConfig?.shopName || '订货商城',
      freeShippingAmount: form.freeShippingAmount,
      freightAmount: form.freightAmount
    }
    await shopConfigApi.update(payload)
    message.success('运费设置已保存')
    loadConfig()
  } catch (e) {
    console.warn('[运费设置] 保存失败', e)
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
.form-unit {
  margin-left: 8px;
  color: #606266;
  font-size: 12px;
}
.rule-preview {
  color: #1890ff;
  font-weight: 500;
}
</style>
