<template>
  <ErrorBoundary>
    <PageContainer title="短信发送">
      <a-alert
        type="warning"
        show-icon
        message="营销短信群发与发送记录后端端点待补全"
        description="当前页对接短信服务配置（/api/sms）真实端点，可维护短信通道配置并测试连通性；群发任务与发送明细将在后端提供端点后接入。"
        style="margin-bottom: 16px"
      />
      <div class="config-area">
        <a-card
          title="短信通道配置"
          :bordered="false"
        >
          <a-form
            :label-col="{ span: 5 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-form-item label="服务商">
              <a-select
                v-model:value="form.provider"
                :options="providerOptions"
                placeholder="请选择短信服务商"
              />
            </a-form-item>
            <a-form-item label="AccessKey">
              <a-input
                v-model:value="form.accessKey"
                placeholder="服务商 AccessKey"
              />
            </a-form-item>
            <a-form-item label="AccessSecret">
              <a-input-password
                v-model:value="form.accessSecret"
                placeholder="服务商 AccessSecret"
              />
            </a-form-item>
            <a-form-item label="短信签名">
              <a-input
                v-model:value="form.signName"
                placeholder="如：企智连"
              />
            </a-form-item>
            <a-form-item label="启用状态">
              <a-switch
                v-model:checked="form.enabled"
                checked-children="启用"
                un-checked-children="停用"
              />
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 5, span: 14 }">
              <a-space>
                <a-button
                  type="primary"
                  :loading="saving"
                  @click="handleSave"
                >
                  保存配置
                </a-button>
                <a-button
                  :loading="testing"
                  @click="handleTest"
                >
                  测试连通性
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { smsApi, type SmsConfig } from '@/api/marketing'

// ═══ 服务商选项（后端 provider 为自由字符串） ═══
const providerOptions = [
  { label: '阿里云短信', value: 'aliyun' },
  { label: '腾讯云短信', value: 'tencent' },
  { label: '华为云短信', value: 'huawei' }
]

const form = reactive<SmsConfig>({
  provider: undefined,
  accessKey: undefined,
  accessSecret: undefined,
  signName: undefined,
  enabled: false
})

const saving = ref(false)
const testing = ref(false)

async function handleSave() {
  saving.value = true
  try {
    const ok = await smsApi.saveConfig({ ...form })
    if (ok) {
      message.success('配置已保存')
    } else {
      message.error('配置保存失败')
    }
  } catch (e) {
    console.warn('[短信发送] 配置保存失败', e)
  } finally {
    saving.value = false
  }
}

async function handleTest() {
  if (!form.provider || !form.accessKey) {
    message.warning('请先填写服务商与 AccessKey')
    return
  }
  testing.value = true
  try {
    const result = await smsApi.test({ ...form })
    if (result.success) {
      message.success(result.message)
    } else {
      message.error(result.message)
    }
  } catch (e) {
    console.warn('[短信发送] 连通性测试失败', e)
  } finally {
    testing.value = false
  }
}

onMounted(async () => {
  try {
    const config = await smsApi.getConfig()
    if (config) {
      Object.assign(form, {
        provider: config.provider,
        accessKey: config.accessKey,
        accessSecret: config.accessSecret,
        signName: config.signName,
        enabled: !!config.enabled
      })
    }
  } catch (e) {
    console.warn('[短信发送] 配置获取失败', e)
  }
})
</script>

<style scoped>
.config-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  max-width: 760px;
}
</style>
