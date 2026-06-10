<template>
  <FullScreenDetail
    :visible="open"
    title="换货单审批"
    :save-loading="submitting"
    @close="handleCancel"
    @save="handleSubmit"
  >
    <div v-if="record" class="approve-info">
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="换货单号">{{ record.exchangeNo }}</a-descriptions-item>
        <a-descriptions-item label="原采购订单">{{ record.originalOrderNo }}</a-descriptions-item>
        <a-descriptions-item label="供应商">{{ record.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="换货日期">{{ record.exchangeDate }}</a-descriptions-item>
        <a-descriptions-item label="换货类型">{{ getExchangeTypeText(record.exchangeType) }}</a-descriptions-item>
        <a-descriptions-item label="换货金额">¥{{ record.totalAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="换货原因" :span="2">{{ record.exchangeReason }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ record.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
    </div>

    <a-divider />

    <a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
      <a-form-item label="审批结果" name="approved">
        <a-radio-group v-model:value="formData.approved">
          <a-radio :value="true">通过</a-radio>
          <a-radio :value="false">拒绝</a-radio>
        </a-radio-group>
      </a-form-item>

      <a-form-item label="审批意见" name="remark">
        <a-textarea
          v-model:value="formData.remark"
          :rows="4"
          placeholder="请输入审批意见"
        />
      </a-form-item>
    </a-form>
  </FullScreenDetail>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { FullScreenDetail } from '@/components'
import {
  purchaseExchangeApi,
  type PurchaseExchange
} from '@/api/purchase-exchange'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

interface Props {
  open: boolean
  record: PurchaseExchange | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const formData = reactive({
  approved: true,
  remark: ''
})

const formRules = {
  approved: [{ required: true, message: '请选择审批结果' }],
  remark: [{ required: true, message: '请输入审批意见' }]
}

const getExchangeTypeText = (type: number): string => {
  const texts: Record<number, string> = {
    1: '质量问题',
    2: '规格不符',
    3: '数量错误',
    4: '其他'
  }
  return texts[type] || '未知'
}

const handleSubmit = async () => {
  if (!props.record) return

  try {
    await formRef.value?.validate()
    submitting.value = true

    if (formData.approved) {
      await purchaseExchangeApi.approve(props.record.id, {
        approved: true,
        remark: formData.remark
      })
      message.success('审批通过')
    } else {
      await purchaseExchangeApi.reject(props.record.id, formData.remark)
      message.success('已拒绝')
    }

    emit('success')
    resetForm()
  } catch (error) {
    console.warn('[采购换货] 审批失败', error)
    message.error('审批失败')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  resetForm()
  emit('update:open', false)
}

const resetForm = () => {
  formData.approved = true
  formData.remark = ''
  formRef.value?.resetFields()
}
</script>

<style scoped>
.approve-info {
  margin-bottom: 16px;
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
