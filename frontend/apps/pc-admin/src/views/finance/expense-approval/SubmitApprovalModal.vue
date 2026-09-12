<template>
  <a-modal
    :open="open"
    title="提交审批"
    :confirm-loading="submitting"
    ok-text="提交"
    @update:open="emit('update:open', $event)"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-form layout="vertical">
      <a-form-item label="费用单">
        <span>{{ record?.docNo }}（{{ formatAmount(record?.totalAmount) }}）</span>
      </a-form-item>
      <a-form-item label="审批级数">
        <a-select
          v-model:value="totalLevel"
          style="width:100%"
        >
          <a-select-option :value="1">
            1 级（部门审批）
          </a-select-option>
          <a-select-option :value="2">
            2 级（部门 → 财务）
          </a-select-option>
          <a-select-option :value="3">
            3 级（部门 → 财务 → 总经理）
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        label="第一级审批人"
        :required="!autoAssigned"
      >
        <a-select
          v-model:value="approverId"
          show-search
          allow-clear
          :options="approverOptions"
          :loading="loadingApprovers"
          placeholder="请选择第一级审批人"
          style="width:100%"
          @change="autoAssigned = false"
        />
        <div
          v-if="autoAssigned"
          class="auto-assign-hint"
        >
          <CheckCircleOutlined /> 已按「{{ suggestSourceText }}」自动指派，可手动改选
        </div>
        <div
          v-else-if="suggestLoaded && !approverId"
          class="auto-assign-hint auto-assign-hint--warn"
        >
          <ExclamationCircleOutlined /> 未配置默认审批人，可先在《费用审批》页「审批人配置」中预设
        </div>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { expenseApprovalApi } from '@/api/finance'
import optionsApi from '@/api/options'

/**
 * 提交审批弹窗（费用审批工作台 / 费用单页共用）
 * 提交后费用单进入第一级审批队列，current_approver_id 指向所选审批人（审批人隔离口径）。
 * 第一级审批人支持自动指派：部门负责人 → 审批人配置，均可手动改选。
 */
defineOptions({ name: 'SubmitExpenseApprovalModal' })

const props = defineProps<{
  open: boolean
  record: any
}>()

const emit = defineEmits<{
  'update:open': [v: boolean]
  'success': []
}>()

const submitting = ref(false)
const totalLevel = ref(3)
const approverId = ref<number | undefined>(undefined)
const approverOptions = ref<any[]>([])
const loadingApprovers = ref(false)
const autoAssigned = ref(false)
const suggestLoaded = ref(false)
const suggestSourceText = ref('')
const suggestApproverId = ref<number | undefined>(undefined)

function formatAmount(amount: any): string {
  if (amount === undefined || amount === null || amount === '') return '0.00'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadApprovers() {
  loadingApprovers.value = true
  try {
    const users = await optionsApi.getUsers()
    approverOptions.value = (users || []).map((u: any) => ({
      label: u.nickname || u.name || u.realName || u.username || String(u.id),
      value: u.id,
    }))
  } catch {
    approverOptions.value = []
  } finally {
    loadingApprovers.value = false
  }
}

/** 自动指派：拉取建议审批人（一级取部门负责人，未设置回退审批人配置） */
async function loadSuggest() {
  suggestLoaded.value = false
  suggestApproverId.value = undefined
  suggestSourceText.value = ''
  autoAssigned.value = false
  if (!props.record?.id) {
    suggestLoaded.value = true
    return
  }
  try {
    const res: any = await expenseApprovalApi.getSuggest(props.record.id, totalLevel.value)
    const list: any[] = res?.data || res || []
    const first = list.find((s: any) => Number(s.level) === 1)
    if (first?.approverId) {
      suggestApproverId.value = first.approverId
      suggestSourceText.value = first.source === 'DEPT_LEADER' ? '部门负责人' : '审批人配置'
      approverId.value = first.approverId
      autoAssigned.value = true
    }
  } catch {
    /* 建议失败不阻断手选 */
  } finally {
    suggestLoaded.value = true
  }
}

watch(() => props.open, (val) => {
  if (!val) return
  totalLevel.value = Number(props.record?.totalApprovalLevel) > 0 ? Number(props.record.totalApprovalLevel) : 3
  approverId.value = undefined
  loadApprovers()
  loadSuggest()
})

// 选择审批级数变化时重新解析一级建议（如从 1 级改为 3 级）
watch(totalLevel, () => {
  if (props.open) loadSuggest()
})

async function handleOk() {
  if (!props.record?.id) {
    message.warning('请先保存费用单')
    return
  }
  if (!approverId.value) {
    message.warning('请选择第一级审批人')
    return
  }
  submitting.value = true
  try {
    const approver = approverOptions.value.find(o => o.value === approverId.value)
    await expenseApprovalApi.submit({
      docId: props.record.id,
      totalLevel: totalLevel.value,
      approverId: approverId.value,
      approverName: approver?.label,
    })
    message.success('已提交审批')
    emit('update:open', false)
    emit('success')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '提交审批失败')
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  emit('update:open', false)
}
</script>

<style scoped>
.auto-assign-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #52c41a;
  line-height: 18px;
}
.auto-assign-hint--warn {
  color: #fa8c16;
}
</style>
