<template>
  <a-modal
    :open="open"
    title="实名认证审核"
    :width="580"
    :confirm-loading="saving"
    @ok="onOk"
    @cancel="emit('update:open', false)"
  >
    <a-descriptions
      :column="2"
      bordered
      size="small"
      style="margin-bottom: 12px"
    >
      <a-descriptions-item label="配送员">
        {{ record?.riderName }}
      </a-descriptions-item>
      <a-descriptions-item label="身份类型">
        {{ record?.riderTypeText }}
      </a-descriptions-item>
      <a-descriptions-item label="证件姓名">
        {{ record?.realName || '-' }}
      </a-descriptions-item>
      <a-descriptions-item label="身份证号">
        {{ record?.idCardNo || '-' }}
      </a-descriptions-item>
      <a-descriptions-item label="证照数">
        {{ record?.certCount || 0 }}
      </a-descriptions-item>
      <a-descriptions-item label="到期预警">
        {{ record?.expiringCertCount || 0 }}
      </a-descriptions-item>
    </a-descriptions>
    <a-form
      ref="formRef"
      :model="form"
      :rules="rules"
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 18 }"
      size="small"
    >
      <a-form-item
        label="审核结果"
        name="approved"
      >
        <a-radio-group v-model:value="form.approved">
          <a-radio :value="true">
            通过
          </a-radio>
          <a-radio :value="false">
            驳回
          </a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="资质有效期">
        <a-date-picker
          v-model:value="form.endorseExpireDate"
          style="width: 100%"
          value-format="YYYY-MM-DD"
          placeholder="外部平台背书 / 资质有效期（可核定）"
        />
      </a-form-item>
      <a-form-item
        label="审核意见"
        name="auditRemark"
      >
        <a-textarea
          v-model:value="form.auditRemark"
          :rows="3"
          placeholder="驳回时必填"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  saving?: boolean
  record?: any
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  submit: [values: { approved: boolean; auditRemark?: string; endorseExpireDate?: string }]
}>()

const formRef = ref<FormInstance>()
const form = reactive<any>({ approved: true, auditRemark: '', endorseExpireDate: undefined })
const rules: Record<string, any[]> = {
  auditRemark: [{
    validator: async (_rule: any, value: string) => {
      if (form.approved === false && !value) {
        throw new Error('驳回时必须填写审核意见')
      }
      return Promise.resolve()
    },
    trigger: 'change',
  }],
}

// 打开时初始化（等价于原 openKycAudit）
watch(() => props.open, (v) => {
  if (v) {
    form.approved = true
    form.auditRemark = ''
    form.endorseExpireDate = props.record?.endorseExpireDate || undefined
  }
})

async function onOk() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  emit('submit', {
    approved: form.approved,
    auditRemark: form.auditRemark,
    endorseExpireDate: form.endorseExpireDate,
  })
}
</script>
