<template>
  <!--
    审核设置 - 单据审核规则配置弹窗（审核设置页 80622 的子组件）
    · 形态对标 ql361：列表每行一个「设置」按钮 → 打开该单据的「审核条件 → 审批人集合」配置
    · 保存走 PUT /api/workflow/audit-config/{docType}（真实落库 sys_audit_rule），保存成功由父页回读

    ⚠️ 本文件此前是「审批流程编辑器」（214 行、无菜单无路由入口、编辑态保存只 message.info 不提交），
       本轮按《审核设置开发文档》§4 的裁定**收敛**：流程定义/节点的编辑归「流程定义 / 流程设计」页
       （views/workflow/designer）唯一持有，本页不再提供第二套流程定义编辑器；
       本文件因此被改写为**本页自己的**规则配置弹窗，由 index.vue 真实引用（不再是孤儿实现）。
       文件路径保持不变，是为了不牵动 router/dynamicRoutes.ts 里既有的组件懒加载映射。
  -->
  <a-modal
    :open="open"
    :title="`审核设置 - ${doc?.docName || ''}`"
    :width="720"
    :confirm-loading="saving"
    ok-text="保存"
    @update:open="(value: boolean) => emit('update:open', value)"
    @ok="handleSave"
  >
    <a-alert
      type="info"
      show-icon
      class="modal-tip"
      message="勾选需要审核的条件，并为每个条件选择审批人；未勾选的条件表示该类单据不因该条件触发审核。"
    />

    <div class="rule-list">
      <div
        v-for="row in editorRows"
        :key="row.condition"
        class="rule-row"
      >
        <a-checkbox v-model:checked="row.enabled">
          {{ row.conditionLabel }}
        </a-checkbox>
        <a-select
          v-model:value="row.userIds"
          mode="multiple"
          size="small"
          class="rule-approvers"
          :disabled="!row.enabled"
          :options="approverOptions"
          :max-tag-count="3"
          option-filter-prop="label"
          :placeholder="row.enabled ? '请选择审批人（可多选）' : '未启用该条件'"
          allow-clear
        />
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { auditConfigApi, type AuditDocRule, type AuditRuleApprover } from '@/api/workflow'
import { optionsApi } from '@/api/options'

defineOptions({ name: 'SetAuditConfigRuleForm' })

const props = defineProps<{
  /** 弹窗显隐（父页 v-model:open） */
  open: boolean
  /** 正在配置的单据行（含已保存的规则，用于回填） */
  doc: AuditDocRule | null
  /** 审核条件目录（后端逐字取自 ql361，父页从列表接口一次性取得） */
  conditions: { value: string; label: string }[]
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  /** 保存成功（父页据此回读列表 —— 摘要由后端拼装，必须以服务端口径为准） */
  (e: 'saved'): void
}>()

interface EditorRow {
  condition: string
  conditionLabel: string
  enabled: boolean
  /** 选中的审批人ID（雪花 ID 字符串，禁止 Number()） */
  userIds: string[]
}

const saving = ref(false)
const editorRows = ref<EditorRow[]>([])

// ═══ 审批人下拉（复用通用选项 API：/user/list） ═══
// ⚠️ 该端点需要 system:user:list 权限；无权限时降级为空列表并提示，不阻塞其它功能
const approverOptions = ref<{ label: string; value: string }[]>([])
const approverLoaded = ref(false)

async function loadApprovers() {
  if (approverLoaded.value) return
  try {
    const users: any = await optionsApi.getUsers()
    const list = Array.isArray(users) ? users : (users?.records || [])
    approverOptions.value = list.map((u: any) => ({
      // 后端 JacksonConfig 已全局把 Long 序列化为 String，这里再兜一层 String()
      value: String(u.id),
      label: u.realName || u.nickname || u.username || u.name || String(u.id),
    }))
    approverLoaded.value = true
  } catch (error) {
    console.warn('[审核设置] 审批人列表加载失败', error)
    approverOptions.value = []
    message.warning('审批人列表加载失败，请确认当前账号拥有「用户列表」权限')
  }
}

/** 提交时作为姓名快照一并上送（后端在账号不可解析时用它兜底展示） */
function userNameOf(userId: string): string {
  return approverOptions.value.find(o => o.value === userId)?.label || ''
}

/** 打开弹窗时按「条件目录 × 已保存规则」求并集构建编辑行 */
function buildRows() {
  const savedMap = new Map<string, AuditRuleApprover[]>(
    (props.doc?.rules || []).map(r => [r.condition, r.approvers || []]),
  )
  editorRows.value = props.conditions.map((c) => {
    const saved = savedMap.get(c.value)
    return {
      condition: c.value,
      conditionLabel: c.label,
      enabled: !!saved && saved.length > 0,
      userIds: saved ? saved.map(a => String(a.userId)) : [],
    }
  })
}

watch(
  () => [props.open, props.doc?.docType, props.conditions] as const,
  ([open]) => {
    if (open) {
      buildRows()
      loadApprovers()
    }
  },
  { immediate: true },
)

async function handleSave() {
  const doc = props.doc
  if (!doc) return

  // 条件目录没加载出来（列表接口失败）时保存会把该单据的规则**清空**（全量覆盖语义），必须拦下
  if (editorRows.value.length === 0) {
    message.warning('审核条件目录未加载，请关闭弹窗并刷新页面后重试')
    return
  }

  // 已勾选却没选审批人 = 配了一条不会生效的规则，直接拦下比静默丢弃友好
  const invalid = editorRows.value.filter(r => r.enabled && r.userIds.length === 0)
  if (invalid.length) {
    message.warning(`请为「${invalid[0].conditionLabel}」选择审批人`)
    return
  }

  const rules = editorRows.value
    .filter(r => r.enabled && r.userIds.length > 0)
    .map(r => ({
      condition: r.condition,
      approvers: r.userIds.map(id => ({ userId: String(id), userName: userNameOf(String(id)) })),
    }))

  saving.value = true
  try {
    await auditConfigApi.save(doc.docType, rules)
    message.success('保存成功')
    emit('update:open', false)
    emit('saved')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.modal-tip { margin-bottom: 12px; }
.rule-list { display: flex; flex-direction: column; gap: 10px; }
.rule-row { display: flex; align-items: center; gap: 12px; }
.rule-approvers { flex: 1; min-width: 0; }
</style>
