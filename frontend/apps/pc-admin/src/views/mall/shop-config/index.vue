<template>
  <ErrorBoundary>
    <PageContainer title="店铺设置">
      <a-card
        :bordered="false"
        class="config-card"
      >
        <a-spin :spinning="loading">
          <a-form
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-form-item
              label="商城名称"
              required
            >
              <a-input
                v-model:value="form.shopName"
                :maxlength="50"
                placeholder="展示在买家端顶部的商城名称"
              />
            </a-form-item>
            <a-form-item label="商城LOGO">
              <a-input
                v-model:value="form.shopLogo"
                placeholder="LOGO图片URL"
              />
              <div
                v-if="form.shopLogo"
                class="logo-preview"
              >
                <a-image
                  :src="form.shopLogo"
                  :height="48"
                  style="border-radius: 4px"
                />
              </div>
            </a-form-item>
            <a-form-item label="商城描述">
              <a-textarea
                v-model:value="form.shopDesc"
                :rows="3"
                :maxlength="200"
                placeholder="商城简介，展示在买家端"
              />
            </a-form-item>
            <a-form-item label="主题色">
              <div class="theme-color-row">
                <input
                  v-model="form.themeColor"
                  type="color"
                  class="color-picker"
                >
                <a-input
                  v-model:value="form.themeColor"
                  style="width: 140px"
                  placeholder="#1890ff"
                />
              </div>
            </a-form-item>
            <a-form-item label="页面模板">
              <a-select
                v-model:value="form.templateId"
                allow-clear
                placeholder="选择买家端页面模板"
                :options="templateOptions"
                :loading="templateLoading"
              />
              <div class="form-tip">
                模板列表来自后端「页面模板」配置
              </div>
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
import { shopConfigApi, shopTemplateApi, type ShopConfig, type ShopTemplate } from '@/api/erp/mall'

defineOptions({ name: 'MallShopConfig' })

const loading = ref(false)
const saving = ref(false)
const templateLoading = ref(false)
const configId = ref<number | undefined>(undefined)
const templates = ref<ShopTemplate[]>([])

const form = reactive({
  shopName: '',
  shopLogo: '',
  shopDesc: '',
  themeColor: '#1890ff',
  templateId: undefined as number | undefined
})

// 完整配置缓存：保存时合并，避免覆盖其他设置页维护的字段
let fullConfig: ShopConfig | null = null

const templateOptions = computed(() =>
  templates.value.map(t => ({
    label: t.isDefault === 1 ? `${t.templateName}（默认）` : t.templateName,
    value: t.id
  }))
)

async function loadConfig() {
  loading.value = true
  try {
    const res: any = await shopConfigApi.get()
    const cfg: ShopConfig | null = res?.data ?? res ?? null
    fullConfig = cfg
    configId.value = cfg?.id
    form.shopName = cfg?.shopName || ''
    form.shopLogo = cfg?.shopLogo || ''
    form.shopDesc = cfg?.shopDesc || ''
    form.themeColor = cfg?.themeColor || '#1890ff'
    form.templateId = cfg?.templateId
  } catch (e) {
    console.warn('[店铺设置] 商城配置获取失败', e)
  } finally {
    loading.value = false
  }
}

async function loadTemplates() {
  templateLoading.value = true
  try {
    const res: any = await shopTemplateApi.list()
    templates.value = Array.isArray(res) ? res : (res?.data ?? [])
  } catch (e) {
    console.warn('[店铺设置] 模板列表获取失败', e)
  } finally {
    templateLoading.value = false
  }
}

async function handleSave() {
  if (!form.shopName.trim()) {
    message.warning('请输入商城名称')
    return
  }
  saving.value = true
  try {
    const payload: ShopConfig = {
      ...(fullConfig || {}),
      id: configId.value,
      shopName: form.shopName.trim(),
      shopLogo: form.shopLogo,
      shopDesc: form.shopDesc,
      themeColor: form.themeColor,
      templateId: form.templateId
    }
    await shopConfigApi.update(payload)
    message.success('店铺设置已保存')
    loadConfig()
  } catch (e) {
    console.warn('[店铺设置] 保存失败', e)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
  loadTemplates()
})
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
.theme-color-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.color-picker {
  width: 40px;
  height: 32px;
  padding: 2px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  background: #fff;
}
.logo-preview {
  margin-top: 8px;
}
</style>
