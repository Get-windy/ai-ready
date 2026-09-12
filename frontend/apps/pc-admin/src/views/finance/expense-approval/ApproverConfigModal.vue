<template>
  <a-modal
    :open="open"
    title="审批人配置"
    :width="560"
    :confirm-loading="saving"
    ok-text="保存"
    @update:open="emit('update:open', $event)"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-alert
      type="info"
      show-icon
      banner
      style="margin-bottom:12px"
    >
      <template #message>
        配置后「提交审批 / 审批通过」将自动指派各级审批人，无需每次手选；第一级优先取费用单所属部门的负责人，部门未设负责人时使用下面的配置。
      </template>
    </a-alert>
    <a-form layout="vertical">
      <a-form-item label="第一级（部门审批）">
        <a-select
          v-model:value="level1ApproverId"
          show-search
          allow-clear
          :options="approverOptions"
          :loading="loadingApprovers"
          placeholder="未配置（提交时需手选）"
          style="width:100%"
        />
      </a-form-item>
      <a-form-item label="第二级（财务审批）">
        <a-select
          v-model:value="level2ApproverId"
          show-search
          allow-clear
          :options="approverOptions"
          :loading="loadingApprovers"
          placeholder="未配置（通过时需手选）"
          style="width:100%"
        />
      </a-form-item>
      <a-form-item label="第三级（总经理审批）">
        <a-select
          v-model:value="level3ApproverId"
          show-search
          allow-clear
          :options="approverOptions"
          :loading="loadingApprovers"
          placeholder="未配置（通过时需手选）"
          style="width:100%"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { expenseApprovalApi } from '@/api/finance'
import optionsApi from '@/api/options'

/**
 * 费用审批人配置弹窗：按级别配置默认审批人（系统参数 expense.approval.levelN.approverId），
 * 用于提交/通过环节的审批人自动指派。
 */
defineOptions({ name: 'ExpenseApproverConfigModal' })

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ 'update:open': [v: boolean]; 'saved': [] }>()

const saving = ref(false)
const loadingApprovers = ref(false)
const approverOptions = ref<any[]>([])
const level1ApproverId = ref<number | undefined>(undefined)
const level2ApproverId = ref<number | undefined>(undefined)
const level3ApproverId = ref<number | undefined>(undefined)

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

async function loadConfig() {
  try {
    const res: any = await expenseApprovalApi.getApproverConfig()
    const cfg = res?.data || res || {}
    level1ApproverId.value = cfg.level1ApproverId ?? undefined
    level2ApproverId.value = cfg.level2ApproverId ?? undefined
    level3ApproverId.value = cfg.level3ApproverId ?? undefined
  } catch (error: any) {
    message.error(error?.response?.data?.message || '读取审批人配置失败')
  }
}

watch(() => props.open, (val) => {
  if (!val) return
  level1ApproverId.value = undefined
  level2ApproverId.value = undefined
  level3ApproverId.value = undefined
  loadApprovers()
  loadConfig()
})

async function handleOk() {
  saving.value = true
  try {
    await expenseApprovalApi.saveApproverConfig({
      level1ApproverId: level1ApproverId.value ?? null,
      level2ApproverId: level2ApproverId.value ?? null,
      level3ApproverId: level3ApproverId.value ?? null,
    })
    message.success('审批人配置已保存')
    emit('update:open', false)
    emit('saved')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  emit('update:open', false)
}
</script>
