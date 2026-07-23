<template>
  <ErrorBoundary>
    <PageContainer title="推广创建">
      <div class="form-area">
        <a-alert
          type="info"
          show-icon
          message="创建的营销活动默认为草稿状态，可在「推广记录」中提交审批并跟踪执行"
          style="margin-bottom: 16px"
        />
        <a-form
          :model="form"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 14 }"
          @finish="handleSubmit"
        >
          <a-form-item
            label="活动名称"
            name="campaignName"
            :rules="[{ required: true, message: '请输入活动名称' }]"
          >
            <a-input
              v-model:value="form.campaignName"
              placeholder="请输入推广活动名称"
              :maxlength="60"
              show-count
            />
          </a-form-item>
          <a-form-item
            label="活动类型"
            name="campaignType"
            :rules="[{ required: true, message: '请选择活动类型' }]"
          >
            <a-select
              v-model:value="form.campaignType"
              :options="campaignTypeOptions"
              placeholder="请选择"
            />
          </a-form-item>
          <a-form-item label="活动时间">
            <a-range-picker
              v-model:value="form.dateRange"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="活动预算">
            <a-input-number
              v-model:value="form.budget"
              :min="0"
              :precision="2"
              placeholder="预算金额（元）"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="预期营收">
            <a-input-number
              v-model:value="form.expectedRevenue"
              :min="0"
              :precision="2"
              placeholder="预期带来的营收（元）"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="预期线索数">
            <a-input-number
              v-model:value="form.expectedLeads"
              :min="0"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="预期订单数">
            <a-input-number
              v-model:value="form.expectedOrders"
              :min="0"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="目标人群">
            <a-input
              v-model:value="form.targetAudience"
              placeholder="如：华东地区 VIP 客户"
            />
          </a-form-item>
          <a-form-item label="目标区域">
            <a-input
              v-model:value="form.targetRegion"
              placeholder="如：华东 / 全国"
            />
          </a-form-item>
          <a-form-item label="活动目标">
            <a-textarea
              v-model:value="form.objective"
              :rows="2"
              placeholder="本次推广希望达成的目标"
            />
          </a-form-item>
          <a-form-item label="活动描述">
            <a-textarea
              v-model:value="form.description"
              :rows="3"
              placeholder="活动内容、渠道、节奏等说明"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="form.remark"
              :rows="2"
              placeholder="选填"
            />
          </a-form-item>
          <a-form-item :wrapper-col="{ offset: 5, span: 14 }">
            <a-space>
              <a-button
                type="primary"
                html-type="submit"
                :loading="submitting"
              >
                创建活动
              </a-button>
              <a-button @click="handleReset">
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { campaignApi, type CampaignCreatePayload } from '@/api/marketing'

// ═══ 活动类型（与后端 CampaignType 枚举一致） ═══
const CAMPAIGN_TYPE_MAP: Record<number, string> = {
  1: '邮件营销',
  2: '短信营销',
  3: '微信营销',
  4: '电话营销',
  5: '活动营销',
  6: '线上推广',
  7: '线下推广',
  8: '内容营销',
  9: '社交媒体',
  10: '综合营销'
}
const campaignTypeOptions = Object.entries(CAMPAIGN_TYPE_MAP).map(([value, label]) => ({ label, value: Number(value) }))

// ═══ 表单 ═══
const submitting = ref(false)
const form = reactive<{
  campaignName?: string
  campaignType?: number
  dateRange?: [Dayjs, Dayjs]
  budget?: number
  expectedRevenue?: number
  expectedLeads?: number
  expectedOrders?: number
  targetAudience?: string
  targetRegion?: string
  objective?: string
  description?: string
  remark?: string
}>({})

function handleReset() {
  Object.keys(form).forEach(key => {
    ;(form as Record<string, any>)[key] = undefined
  })
}

async function handleSubmit() {
  submitting.value = true
  try {
    const payload: CampaignCreatePayload = {
      campaignName: form.campaignName!,
      campaignType: form.campaignType,
      budget: form.budget,
      expectedRevenue: form.expectedRevenue,
      expectedLeads: form.expectedLeads,
      expectedOrders: form.expectedOrders,
      targetAudience: form.targetAudience,
      targetRegion: form.targetRegion,
      objective: form.objective,
      description: form.description,
      remark: form.remark,
      startDate: form.dateRange?.[0] ? dayjs(form.dateRange[0]).format('YYYY-MM-DD') : undefined,
      endDate: form.dateRange?.[1] ? dayjs(form.dateRange[1]).format('YYYY-MM-DD') : undefined
    }
    await campaignApi.create(payload)
    message.success('推广活动创建成功，可前往「推广记录」查看并提交审批')
    handleReset()
  } catch (e) {
    console.warn('[推广创建] 创建失败', e)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.form-area {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  max-width: 860px;
}
</style>
