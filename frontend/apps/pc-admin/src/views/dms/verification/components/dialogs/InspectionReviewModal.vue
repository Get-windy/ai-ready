<template>
  <a-modal
    :open="open"
    title="审核巡检"
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
      <a-form-item label="车牌号">
        <span>{{ record?.plateNo }}</span>
      </a-form-item>
      <a-form-item label="巡检类型">
        <span>{{ INSPECTION_TYPE_MAP[record?.inspectionType]?.text }}</span>
      </a-form-item>
      <a-form-item label="系统结论">
        <a-tag :color="record?.result === 1 ? 'green' : 'red'">
          {{ record?.result === 1 ? '通过' : '不通过' }}
        </a-tag>
      </a-form-item>
      <a-form-item
        label="审核结果"
        name="result"
      >
        <a-select
          v-model:value="form.result"
          :options="[{ value: 1, label: '通过' }, { value: 2, label: '不通过' }]"
        />
      </a-form-item>
      <a-form-item label="审核意见">
        <a-textarea
          v-model:value="form.remark"
          :rows="2"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import { INSPECTION_TYPE_MAP } from '../../config/verification-config'

const props = defineProps<{
  open: boolean
  saving?: boolean
  record?: any
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  submit: [values: { result: number; remark?: string }]
}>()

const formRef = ref<FormInstance>()
const form = reactive<any>({ result: 1, remark: '' })
const rules: Record<string, any[]> = {
  result: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
}

// 打开时按当前记录回填（等价于原 openInspectionReview 的初始化）
watch(() => props.open, (v) => {
  if (v) {
    form.result = props.record?.result === 1 ? 1 : 2
    form.remark = ''
  }
})

async function onOk() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  emit('submit', { result: form.result, remark: form.remark })
}
</script>
