<template>
  <a-modal
    :open="open"
    title="处理预警"
    :width="520"
    :confirm-loading="saving"
    @ok="onOk"
    @cancel="emit('update:open', false)"
  >
    <a-form
      ref="formRef"
      :model="form"
      :rules="rules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      size="small"
    >
      <a-form-item label="预警类型">
        <a-tag :color="ALERT_TYPE_MAP[record?.alertType]?.color">
          {{ ALERT_TYPE_MAP[record?.alertType]?.text }}
        </a-tag>
      </a-form-item>
      <a-form-item label="预警内容">
        <span>{{ record?.alertContent }}</span>
      </a-form-item>
      <a-form-item
        label="处理结果"
        name="handleStatus"
      >
        <a-select
          v-model:value="form.handleStatus"
          :options="[
            { value: 1, label: '已确认' },
            { value: 2, label: '已忽略' },
            { value: 3, label: '已处理' },
          ]"
        />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea
          v-model:value="form.remark"
          :rows="2"
          placeholder="处理备注"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import { ALERT_TYPE_MAP } from '../../config/verification-config'

const props = defineProps<{
  open: boolean
  saving?: boolean
  record?: any
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  submit: [values: { handleStatus: number; remark?: string }]
}>()

const formRef = ref<FormInstance>()
const form = reactive<any>({ handleStatus: 3, remark: '' })
const rules: Record<string, any[]> = {
  handleStatus: [{ required: true, message: '请选择处理结果', trigger: 'change' }],
}

// 打开时重置表单（等价于原 openAlertHandle 里的重置）
watch(() => props.open, (v) => {
  if (v) {
    form.handleStatus = 3
    form.remark = ''
  }
})

async function onOk() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  emit('submit', { handleStatus: form.handleStatus, remark: form.remark })
}
</script>
