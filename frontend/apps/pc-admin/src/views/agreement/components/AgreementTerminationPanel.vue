<template>
  <div class="aterm">
    <!-- ═══ ⚠️ 终止 ≠ 免责：这是本模块最重要的一句提示 ═══ -->
    <a-alert
      type="error"
      show-icon
      class="aterm-alert"
      message="终止不等于免责"
      :description="exemptionNotice"
    />

    <div class="aterm-toolbar">
      <a-button
        v-permission="'agreement:terminate:request'"
        type="primary"
        size="small"
        danger
        :disabled="!canRequest"
        @click="openCreate"
      >
        <StopOutlined /> 发起终止
      </a-button>
      <a-button
        size="small"
        :loading="loading"
        @click="reload"
      >
        <ReloadOutlined /> 刷新
      </a-button>
      <span
        v-if="!canRequest"
        class="aterm-hint"
      >
        {{ requestBlockReason }}
      </span>
    </div>

    <!-- ═══ 终止记录 ═══ -->
    <a-table
      :columns="columns"
      :data-source="records"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="small"
      bordered
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'source'">
          <div>{{ sourceLabel(record.source) }}</div>
          <div class="aterm-muted">
            由{{ record.requestedSideLabel || '一方' }}发起
            {{ record.requestedAt ? ' · ' + String(record.requestedAt).replace('T', ' ') : '' }}
          </div>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="TERMINATION_STATUS_COLOR[record.status ?? -1] || 'default'">
            {{ record.statusLabel || TERMINATION_STATUS_TEXT[record.status ?? -1] || '未知' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'basis'">
          <div>{{ record.basisText || '—' }}</div>
          <div
            v-if="record.claimCounterpartyBreach"
            class="aterm-breach"
          >
            主张对方违约{{ record.breachNote ? '：' + record.breachNote : '' }}
            <div class="aterm-muted">
              ⚠️「违约」是当事人的主张，平台不认定违约、不判赔多少。
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'stop'">
          {{ record.stopPerformanceAt ? String(record.stopPerformanceAt).replace('T', ' ') : '立即' }}
        </template>
        <template v-else-if="column.key === 'counterparty'">
          <div v-if="record.counterpartyObjection">
            <a-tag color="volcano">
              对方已提异议
            </a-tag>
            <div class="aterm-muted">
              {{ record.counterpartyActionAt ? String(record.counterpartyActionAt).replace('T', ' ') : '' }}
              {{ record.objectionReason ? ' · ' + record.objectionReason : '' }}
            </div>
          </div>
          <div v-else-if="record.counterpartyActionAt">
            <a-tag color="green">
              对方已确认
            </a-tag>
            <div class="aterm-muted">
              {{ String(record.counterpartyActionAt).replace('T', ' ') }}
            </div>
          </div>
          <span
            v-else
            class="aterm-muted"
          >对方尚未表态</span>
          <div
            v-if="record.withdrawnAt"
            class="aterm-muted"
          >
            发起方已于 {{ String(record.withdrawnAt).replace('T', ' ') }} 撤回
            {{ record.withdrawReason ? '：' + record.withdrawReason : '' }}
          </div>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space :size="4">
            <a-button
              v-if="record.canConfirm"
              v-permission="'agreement:terminate:confirm'"
              type="link"
              size="small"
              @click="openAction(record, 'confirm')"
            >
              确认终止
            </a-button>
            <a-button
              v-if="record.canObject"
              v-permission="'agreement:terminate:confirm'"
              type="link"
              size="small"
              danger
              @click="openAction(record, 'object')"
            >
              提异议
            </a-button>
            <a-button
              v-if="record.canWithdraw"
              v-permission="'agreement:terminate:request'"
              type="link"
              size="small"
              @click="openAction(record, 'withdraw')"
            >
              撤回终止
            </a-button>
            <span
              v-if="!record.canConfirm && !record.canObject && !record.canWithdraw"
              class="aterm-muted"
            >
              无需你表态
            </span>
          </a-space>
        </template>
      </template>
    </a-table>

    <div
      v-if="!records.length && !loading"
      class="aterm-empty"
    >
      还没有终止记录。发起终止只会记录「从什么时候起停止履行」这个事实 ——
      系统不会自动结清、不会自动免责，是否违约与怎么赔偿要另行处理。
    </div>

    <!-- ═══ 发起终止 ═══ -->
    <a-modal
      v-model:open="createVisible"
      title="发起终止"
      :width="680"
      :mask-closable="false"
      :confirm-loading="submitting"
      ok-text="提交终止"
      ok-type="danger"
      cancel-text="取消"
      @ok="submitCreate"
    >
      <a-alert
        type="error"
        show-icon
        class="aterm-alert"
        message="提交前请看清：终止只停止履行，不代表责任了结"
        :description="exemptionNotice"
      />

      <a-form
        layout="vertical"
        class="aterm-form"
      >
        <a-form-item label="终止来源">
          <div class="aterm-sources">
            <div
              v-for="s in TERMINATION_SOURCE_OPTIONS"
              :key="s.value"
              :class="['aterm-source', { active: form.source === s.value }]"
              @click="form.source = s.value"
            >
              <div class="aterm-source-head">
                <span class="aterm-source-name">{{ s.label }}</span>
                <a-tag
                  v-if="s.requiresCounterpartyConfirm"
                  color="orange"
                >
                  需对方确认
                </a-tag>
              </div>
              <div class="aterm-source-desc">
                {{ s.desc }}
              </div>
            </div>
          </div>
        </a-form-item>

        <a-form-item label="终止依据（引用协议条款或法定情形）">
          <a-textarea
            v-model:value="form.basisText"
            :rows="3"
            :maxlength="500"
            show-count
            placeholder="例如：按第 2 版第 7 条，对方逾期付款已超过 15 日"
          />
        </a-form-item>

        <a-form-item>
          <a-checkbox v-model:checked="form.claimCounterpartyBreach">
            同时主张对方违约（⚠️ 这只是你的主张，平台不认定违约、不判赔多少）
          </a-checkbox>
        </a-form-item>

        <a-form-item
          v-if="form.claimCounterpartyBreach"
          label="主张违约的具体情形"
        >
          <a-textarea
            v-model:value="form.breachNote"
            :rows="2"
            :maxlength="300"
            show-count
            placeholder="例如：2026-08-01 应结货款 12 万元至今未付，已两次书面催告"
          />
        </a-form-item>

        <a-form-item label="从何时起停止履行">
          <a-date-picker
            v-model:value="form.stopPerformanceAt"
            show-time
            size="small"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空 = 立即（只是停止履行的时刻，不含结清与免责）"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 对方表态 / 撤回 ═══ -->
    <a-modal
      v-model:open="actionVisible"
      :title="actionTitle"
      :width="520"
      :confirm-loading="submitting"
      :ok-text="actionOkText"
      :ok-type="actionKind === 'confirm' ? 'primary' : 'danger'"
      cancel-text="取消"
      @ok="submitAction"
    >
      <a-alert
        v-if="actionKind === 'object'"
        type="warning"
        show-icon
        class="aterm-alert"
        message="提异议的后果"
        description="如果这条终止还在「待对方确认」，提异议则终止不成立、协议继续有效；如果协议已经终止，提异议只做留痕，不会回滚「已停止履行」这个事实（是否违约另走维权途径）。平台清退的异议即申诉。"
      />
      <a-alert
        v-else-if="actionKind === 'withdraw'"
        type="warning"
        show-icon
        class="aterm-alert"
        message="撤回后这条终止记录不再推进"
        description="只有「待对方确认」的终止可以撤回；已生效的终止无法通过撤回回到履行状态。"
      />
      <a-alert
        v-else
        type="info"
        show-icon
        class="aterm-alert"
        :message="exemptionNotice"
      />
      <a-textarea
        v-model:value="actionReason"
        :rows="3"
        :maxlength="300"
        show-count
        :placeholder="actionKind === 'confirm' ? '确认备注（选填）' : '原因（建议填写，会留痕）'"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
/**
 * 终止面板 —— §13.7
 *
 * 一条必须做对、而且是用户原话强调过的事：**终止 ≠ 免责**。
 *   · 界面必须显式写明「终止只表示停止履行，不代表责任了结；是否违约、如何赔偿需另行处理」；
 *   · 这句话**用服务端下发的 exemptionNotice**（正稿只有一处），不自己编；
 *   · 五种来源用业务白话展示（协商一致 / 自然到期 / 单方终止 / 因对方违约 / 平台清退），
 *     不把英文枚举甩给用户；
 *   · 谁能不能表态（确认 / 异议 / 撤回）由服务端下发 canConfirm / canObject / canWithdraw，
 *     前端不用自己猜"我是哪一方"。
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { StopOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import {
  agreementApi,
  TERMINATION_SOURCE_OPTIONS,
  TERMINATION_STATUS_TEXT,
  TERMINATION_STATUS_COLOR,
  NO_EXEMPTION_NOTICE_MIRROR,
  type AgreementTermination,
  type AgreementTerminationSourceCode
} from '@/api/agreement'

const props = defineProps<{
  agreementId: number | string
  /** 主档状态：草稿/已终止 都不能再发起终止（由服务端最终把关，前端提前说明） */
  agreementStatus?: string
}>()

const loading = ref(false)
const records = ref<AgreementTermination[]>([])

const columns = [
  { title: '终止来源', key: 'source', width: 190 },
  { title: '状态', key: 'status', width: 140 },
  { title: '终止依据', key: 'basis', width: 280 },
  { title: '停止履行自', key: 'stop', width: 170 },
  { title: '对方表态', key: 'counterparty', width: 220 },
  { title: '操作', key: 'action', width: 240 }
]

/**
 * 「终止 ≠ 免责」文案：优先用服务端下发的 exemptionNotice（正稿），
 * 只有在这份协议还没有任何终止记录、拿不到后端文案时，才用逐字一致的兜底常量。
 */
const exemptionNotice = computed(() => {
  const fromServer = records.value.find(r => !!r.exemptionNotice)?.exemptionNotice
  return fromServer || NO_EXEMPTION_NOTICE_MIRROR
})

function sourceLabel(source?: string): string {
  const hit = TERMINATION_SOURCE_OPTIONS.find(s => s.value === source)
  return hit ? hit.label : (source || '—')
}

// ── 能否发起 ──
const canRequest = computed(() => props.agreementStatus !== 'DRAFT' && props.agreementStatus !== 'TERMINATED')
const requestBlockReason = computed(() => {
  if (props.agreementStatus === 'DRAFT') return '这份协议还没有生效，谈不上终止；不想签了请直接删除草稿，或让对方否决当前版本。'
  if (props.agreementStatus === 'TERMINATED') return '这份协议已经是终止状态，不需要重复终止。'
  return ''
})

// ── 发起终止 ──
const createVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  source: 'MUTUAL_AGREEMENT' as AgreementTerminationSourceCode,
  basisText: '',
  claimCounterpartyBreach: false,
  breachNote: '',
  stopPerformanceAt: undefined as string | undefined
})

function openCreate() {
  if (!canRequest.value) {
    message.warning(requestBlockReason.value)
    return
  }
  form.source = 'MUTUAL_AGREEMENT'
  form.basisText = ''
  form.claimCounterpartyBreach = false
  form.breachNote = ''
  form.stopPerformanceAt = undefined
  createVisible.value = true
}

async function submitCreate() {
  if (!form.basisText.trim()) {
    message.warning('请填写终止依据：引用协议条款或法定情形')
    return
  }
  if (form.claimCounterpartyBreach && !form.breachNote.trim()) {
    message.warning('勾选了「主张对方违约」，请填写具体情形')
    return
  }
  submitting.value = true
  try {
    await agreementApi.requestTermination(props.agreementId, {
      source: form.source,
      basisText: form.basisText.trim(),
      claimCounterpartyBreach: form.claimCounterpartyBreach || undefined,
      breachNote: form.breachNote.trim() || undefined,
      stopPerformanceAt: form.stopPerformanceAt || undefined
    })
    const requiresConfirm = TERMINATION_SOURCE_OPTIONS.find(s => s.value === form.source)?.requiresCounterpartyConfirm
    message.success(requiresConfirm
      ? '终止已发起，等对方确认后协议才置为已终止'
      : '终止已记录：协议自此停止履行（⚠️ 不代表责任了结）')
    createVisible.value = false
    await reload()
  } catch (error: any) {
    console.warn('[协议终止] 发起失败', error)
  } finally {
    submitting.value = false
  }
}

// ── 对方表态 / 撤回 ──
const actionVisible = ref(false)
const actionKind = ref<'confirm' | 'object' | 'withdraw'>('confirm')
const actionTarget = ref<AgreementTermination | null>(null)
const actionReason = ref('')

const actionTitle = computed(() => {
  if (actionKind.value === 'object') return '提出异议'
  if (actionKind.value === 'withdraw') return '撤回终止'
  return '确认终止'
})
const actionOkText = computed(() => {
  if (actionKind.value === 'object') return '提交异议'
  if (actionKind.value === 'withdraw') return '确认撤回'
  return '确认终止'
})

function openAction(record: AgreementTermination, kind: 'confirm' | 'object' | 'withdraw') {
  actionTarget.value = record
  actionKind.value = kind
  actionReason.value = ''
  actionVisible.value = true
}

async function submitAction() {
  if (!actionTarget.value) return
  if (actionKind.value !== 'confirm' && !actionReason.value.trim()) {
    message.warning(actionKind.value === 'object' ? '请填写异议原因，会留痕' : '请填写撤回原因，会留痕')
    return
  }
  submitting.value = true
  try {
    const payload = { reason: actionReason.value.trim() || undefined }
    if (actionKind.value === 'confirm') {
      await agreementApi.confirmTermination(actionTarget.value.id, payload)
      message.success('已确认终止，协议自此停止履行（⚠️ 不代表责任了结）')
    } else if (actionKind.value === 'object') {
      await agreementApi.objectTermination(actionTarget.value.id, payload)
      message.success('异议已提交并留痕')
    } else {
      await agreementApi.withdrawTermination(actionTarget.value.id, payload)
      message.success('已撤回终止')
    }
    actionVisible.value = false
    await reload()
  } catch (error: any) {
    console.warn('[协议终止] 表态失败', error)
  } finally {
    submitting.value = false
  }
}

async function reload() {
  if (!props.agreementId) return
  loading.value = true
  try {
    const res: any = await agreementApi.listTerminations(props.agreementId)
    const list: any = res?.data ?? res ?? []
    records.value = Array.isArray(list) ? list : (list?.records || [])
  } catch (error: any) {
    console.warn('[协议终止] 记录加载失败', error)
    records.value = []
  } finally {
    loading.value = false
  }
}

onMounted(reload)
watch(() => props.agreementId, reload)
</script>

<style scoped>
.aterm-alert { margin-bottom: 10px; }
.aterm-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; flex-wrap: wrap; }
.aterm-hint { font-size: 12px; color: #d46b08; }
.aterm-muted { font-size: 12px; color: #999; line-height: 1.6; }
.aterm-breach { margin-top: 4px; font-size: 12px; color: #d4380d; line-height: 1.6; }
.aterm-empty { font-size: 13px; color: #999; line-height: 1.7; margin-top: 8px; }

/* 终止来源：业务白话卡片（不甩英文枚举） */
.aterm-sources { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 8px; }
.aterm-source {
  border: 1px solid #d9d9d9; border-radius: 6px; padding: 8px 10px; cursor: pointer;
  background: #fff; transition: all 0.2s;
}
.aterm-source:hover { border-color: #ff7875; }
.aterm-source.active { border-color: #ff4d4f; background: #fff1f0; box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.12); }
.aterm-source-head { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.aterm-source-name { font-size: 13px; font-weight: 600; color: #333; }
.aterm-source-desc { font-size: 12px; color: #666; line-height: 1.6; }
.aterm-form :deep(.ant-form-item) { margin-bottom: 12px; }
</style>
