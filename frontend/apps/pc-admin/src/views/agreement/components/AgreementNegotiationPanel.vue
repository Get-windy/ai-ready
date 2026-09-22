<template>
  <div class="aneg">
    <!-- ═══ 协商期间现行版本继续有效（这一条必须让人看清） ═══ -->
    <a-alert
      type="warning"
      show-icon
      class="aneg-alert"
      message="协商期间，现行生效版本继续有效"
      description="下面这些「待双方确认」的版本就是一轮轮协商。在双方都同意并签署、新版本置为生效之前，交易与结算仍按现行生效版本执行 —— 谈成之前交易照常。"
    />

    <a-row :gutter="12">
      <!-- ═══ 左：协商时间线 ═══ -->
      <a-col :span="10">
        <a-card
          size="small"
          class="aneg-card"
          title="协商时间线"
        >
          <template #extra>
            <span class="aneg-sub">谁提的 · 改了什么 · 留言</span>
          </template>

          <div
            v-if="!orderedVersions.length"
            class="aneg-empty"
          >
            还没有版本记录。
          </div>

          <a-timeline v-else>
            <a-timeline-item
              v-for="v in orderedVersions"
              :key="v.id"
              :color="timelineColor(v)"
            >
              <div
                :class="['aneg-node', { active: String(v.id) === String(activeVersionId) }]"
                @click="selectVersion(v)"
              >
                <div class="aneg-node-head">
                  <span class="aneg-node-title">第 {{ v.versionNo }} 版</span>
                  <a-tag :color="versionColor(v.status)">
                    {{ versionStatusLabel(v.status) }}
                  </a-tag>
                  <a-tag :color="v.proposedBySide === 'B' ? 'cyan' : 'blue'">
                    {{ v.proposedBySideLabel || PARTY_SIDE_TEXT[v.proposedBySide || ''] || '发起方未知' }}提出
                  </a-tag>
                </div>
                <div class="aneg-node-line">
                  变更原因：{{ v.changeReason || '首次签订' }}
                </div>
                <div
                  v-if="v.proposalNote"
                  class="aneg-node-note"
                >
                  协商留言：{{ v.proposalNote }}
                </div>
                <div class="aneg-node-meta">
                  <span
                    :class="v.partyAConfirmedAt ? 'aneg-ok' : 'aneg-wait'"
                  >
                    甲方{{ v.partyAConfirmedAt ? '已确认' : '待确认' }}
                  </span>
                  <span
                    :class="v.partyBConfirmedAt ? 'aneg-ok' : 'aneg-wait'"
                  >
                    乙方{{ v.partyBConfirmedAt ? '已确认' : '待确认' }}
                  </span>
                  <span
                    v-if="v.proposedByPerson"
                    class="aneg-muted"
                  >提出人：{{ v.proposedByPerson }}</span>
                </div>
                <div
                  v-if="v.noRetroactiveNote"
                  class="aneg-no-retro"
                >
                  {{ v.noRetroactiveNote }}
                </div>
              </div>
            </a-timeline-item>
          </a-timeline>
        </a-card>
      </a-col>

      <!-- ═══ 右：逐条 diff + 三个动作 ═══ -->
      <a-col :span="14">
        <a-card
          size="small"
          class="aneg-card"
        >
          <template #title>
            <span>逐条对比</span>
            <span
              v-if="diff"
              class="aneg-sub"
            >
              （第 {{ diff.versionNo }} 版 vs {{ diff.againstLabel || '上一版' }}）
            </span>
          </template>
          <template #extra>
            <a-button
              size="small"
              :loading="diffLoading"
              @click="loadDiff"
            >
              <ReloadOutlined /> 刷新对比
            </a-button>
          </template>

          <div
            v-if="!activeVersionId"
            class="aneg-empty"
          >
            请在左边选一个版本，查看它相对上一版改了哪些地方。
          </div>

          <template v-else>
            <div
              v-if="diff?.summary?.length"
              class="aneg-summary"
            >
              <a-tag
                v-for="(s, i) in diff.summary"
                :key="i"
                color="blue"
              >
                {{ s }}
              </a-tag>
              <a-tag :color="diff.snapshotChanged ? 'orange' : 'default'">
                {{ diff.snapshotChanged ? '条款正文/期限有改动' : '快照正文逐字未变' }}
              </a-tag>
            </div>

            <a-table
              :columns="diffColumns"
              :data-source="diffRows"
              :pagination="false"
              :loading="diffLoading"
              row-key="rowKey"
              size="small"
              bordered
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'category'">
                  {{ record.categoryLabel }}
                </template>
                <template v-else-if="column.key === 'changeType'">
                  <a-tag :color="changeColor(record.changeType)">
                    {{ record.changeTypeLabel || changeLabel(record.changeType) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'label'">
                  {{ record.label || record.code || '—' }}
                </template>
                <template v-else-if="column.key === 'before'">
                  {{ renderSide(record, 'before') }}
                </template>
                <template v-else-if="column.key === 'after'">
                  {{ renderSide(record, 'after') }}
                </template>
              </template>
            </a-table>

            <div
              v-if="!diffRows.length && !diffLoading"
              class="aneg-empty"
            >
              这一版与基准版逐条对比没有差异（可能只是期限或正文有改动，见上方标签）。
            </div>

            <!-- ═══ 三个动作：同意 / 提出修改 / 拒绝 ═══ -->
            <div class="aneg-actions">
              <a-space :size="8">
                <a-button
                  v-permission="'agreement:version:confirm'"
                  type="primary"
                  size="small"
                  :disabled="!canAct"
                  :loading="acting"
                  @click="handleAgree"
                >
                  <CheckOutlined /> 同意这一版
                </a-button>
                <a-button
                  v-permission="'agreement:version:create'"
                  size="small"
                  :disabled="!canPropose"
                  @click="openPropose"
                >
                  <EditOutlined /> 提出修改
                </a-button>
                <a-button
                  v-permission="'agreement:version:confirm'"
                  size="small"
                  danger
                  :disabled="!canAct"
                  @click="openReject"
                >
                  <CloseOutlined /> 拒绝
                </a-button>
              </a-space>
              <div
                v-if="actionBlockReason"
                class="aneg-block"
              >
                {{ actionBlockReason }}
              </div>
              <div
                v-else
                class="aneg-muted"
              >
                「同意」表示本方确认这一版（正式签署还会额外留下"凭什么代表"的授权痕迹，见「签署」页）；
                「提出修改」等于反要约，会新建一个草稿版本并留下你的协商留言；「拒绝」会让这一版作废，
                现行生效版本继续有效。
              </div>
            </div>
          </template>
        </a-card>
      </a-col>
    </a-row>

    <!-- ═══ 提出修改（反要约） ═══ -->
    <a-modal
      v-model:open="proposeVisible"
      title="提出修改（反要约）"
      :width="600"
      :confirm-loading="acting"
      ok-text="提交并新建一版草稿"
      cancel-text="取消"
      @ok="submitPropose"
    >
      <a-alert
        type="warning"
        show-icon
        class="aneg-alert"
        message="修改 = 反要约 = 新建一个待确认版本"
        description="系统会以你选的那一版为基准复制出新草稿；谈成之前，现行生效版本继续有效、交易照常。"
      />
      <a-form
        layout="vertical"
        class="aneg-form"
      >
        <a-form-item label="以哪一版为基准改">
          <a-select
            v-model:value="proposeForm.baseVersionId"
            size="small"
            :options="baseVersionOptions"
          />
        </a-form-item>
        <a-form-item label="变更原因（正式留痕，必填）">
          <a-input
            v-model:value="proposeForm.changeReason"
            size="small"
            :maxlength="255"
            placeholder="例如：账期由 30 天改为 45 天"
          />
        </a-form-item>
        <a-form-item label="协商留言（对话式留痕）">
          <a-textarea
            v-model:value="proposeForm.proposalNote"
            :rows="3"
            :maxlength="500"
            show-count
            placeholder="例如：旺季资金周转压力大，账期希望放宽到 45 天；其余条款不变。"
          />
          <div class="aneg-muted">
            这句话会留在协商时间线上，方便对方看懂你为什么改。
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 拒绝 ═══ -->
    <a-modal
      v-model:open="rejectVisible"
      title="拒绝这一版"
      :width="520"
      :confirm-loading="acting"
      ok-text="确认拒绝"
      ok-type="danger"
      cancel-text="取消"
      @ok="submitReject"
    >
      <a-alert
        type="warning"
        show-icon
        class="aneg-alert"
        message="拒绝的后果"
        description="这一版会被置为「已否决」；现行生效版本继续有效，交易照常。如果只是有地方要改，用「提出修改」比拒绝更合适（拒绝等于这一轮谈崩）。"
      />
      <a-textarea
        v-model:value="rejectReason"
        :rows="3"
        :maxlength="300"
        show-count
        placeholder="拒绝原因（建议填写，会留痕）"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
/**
 * 多轮协商面板 —— §13.4「要约 → 反要约 → 承诺」
 *
 * 要做对的三件事：
 *   ① 逐条 diff 要能看清四类差异（条款 / 字段设定 / 文字条款 / 履约方式），四类共用一个 Item 形状；
 *   ② 协商时间线要标出**谁提的**（proposedBySide / proposedByPerson）、留言与双方表态；
 *   ③ 必须让人看清一条：**协商期间现行生效版本继续有效**（㉛：谈成之前交易照常）。
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, CheckOutlined, EditOutlined, CloseOutlined } from '@ant-design/icons-vue'
import {
  agreementApi,
  AGREEMENT_VERSION_STATUS_TEXT,
  AGREEMENT_VERSION_STATUS_COLOR,
  PARTY_SIDE_TEXT,
  type AgreementVersion,
  type AgreementVersionStatus,
  type AgreementVersionDiff,
  type AgreementDiffItem
} from '@/api/agreement'

const props = defineProps<{
  agreementId: number | string
  /** 协议的全部版本（详情页已加载，避免重复请求） */
  versions?: AgreementVersion[]
  /** 现行生效版本（协商期间照常执行的那一版） */
  activeVersionId?: number | string | null
}>()

const emit = defineEmits<{ 'changed': [] }>()

function versionStatusLabel(status?: string): string {
  return AGREEMENT_VERSION_STATUS_TEXT[status as AgreementVersionStatus] || status || '未知'
}
function versionColor(status?: string): string {
  return AGREEMENT_VERSION_STATUS_COLOR[status as AgreementVersionStatus] || 'default'
}
function timelineColor(v: AgreementVersion): string {
  if (v.status === 'ACTIVE') return 'green'
  if (v.status === 'REJECTED') return 'red'
  if (v.status === 'SUPERSEDED') return 'gray'
  return 'blue'
}

/** 时间线按版本号从小到大（读起来就是一轮轮谈下来的过程） */
const orderedVersions = computed(() =>
  [...(props.versions || [])].sort((a, b) => (a.versionNo ?? 0) - (b.versionNo ?? 0))
)

// ── 选中的版本 + diff ──
const activeVersionId = ref<string>('')
const diff = ref<AgreementVersionDiff | null>(null)
const diffLoading = ref(false)

function selectVersion(v: AgreementVersion) {
  activeVersionId.value = String(v.id)
  loadDiff()
}

const diffColumns = [
  { title: '类别', key: 'category', width: 110 },
  { title: '变化', key: 'changeType', width: 80 },
  { title: '条目', key: 'label', width: 160 },
  { title: '原来', key: 'before', width: 200 },
  { title: '改成', key: 'after', width: 200 }
]

interface DiffRow extends AgreementDiffItem {
  rowKey: string
  categoryLabel: string
}

/** 四类差异合成一张表（后端刻意让四类共用一个 Item 形状，前端就一套渲染逻辑吃下） */
const diffRows = computed<DiffRow[]>(() => {
  const d = diff.value
  if (!d) return []
  const groups: Array<[string, AgreementDiffItem[] | undefined]> = [
    ['条款', d.terms],
    ['字段设定', d.settings],
    ['文字条款', d.narratives],
    ['履约方式', d.fulfillmentModes]
  ]
  const out: DiffRow[] = []
  for (const [categoryLabel, items] of groups) {
    for (const item of items || []) {
      out.push({ ...item, categoryLabel, rowKey: `${categoryLabel}-${item.code || item.label || out.length}` })
    }
  }
  return out
})

function changeLabel(type?: string): string {
  if (type === 'ADDED') return '新增'
  if (type === 'REMOVED') return '删除'
  if (type === 'CHANGED') return '修改'
  return '变化'
}
function changeColor(type?: string): string {
  if (type === 'ADDED') return 'green'
  if (type === 'REMOVED') return 'red'
  if (type === 'CHANGED') return 'orange'
  return 'default'
}
function renderSide(record: AgreementDiffItem, side: 'before' | 'after'): string {
  const text = side === 'before' ? record.beforeText : record.afterText
  const code = side === 'before' ? record.beforeCode : record.afterCode
  if (text && code) return `${text}（${code}）`
  if (text) return text
  if (code) return code
  return '—'
}

async function loadDiff() {
  if (!activeVersionId.value) return
  diffLoading.value = true
  try {
    const res: any = await agreementApi.diffVersion(activeVersionId.value)
    diff.value = res?.data ?? res ?? null
  } catch (error: any) {
    console.warn('[协议协商] 版本差异加载失败', error)
    diff.value = null
  } finally {
    diffLoading.value = false
  }
}

// ── 选中的版本本身（判断能不能表态） ──
const activeVersion = computed(() =>
  orderedVersions.value.find(v => String(v.id) === activeVersionId.value) || null
)
const canAct = computed(() => activeVersion.value?.status === 'DRAFT')
const canPropose = computed(() => {
  // 反要约的基准可以是草稿或现行生效版本（没有草稿时以生效版本为基准改）
  const s = activeVersion.value?.status
  return s === 'DRAFT' || s === 'ACTIVE'
})
const actionBlockReason = computed(() => {
  if (!activeVersion.value) return ''
  const s = activeVersion.value.status
  if (s === 'DRAFT') return ''
  if (s === 'ACTIVE') return '这是现行生效版本，不能同意/拒绝；要改它请点「提出修改」新建一版。'
  if (s === 'SUPERSEDED') return '这一版已经被新版取代，不能再表态。'
  if (s === 'REJECTED') return '这一版已被否决，不能再表态；如需继续谈，请以现行生效版本为基准「提出修改」。'
  return ''
})

// ── 三个动作 ──
const acting = ref(false)

async function handleAgree() {
  if (!activeVersion.value) return
  acting.value = true
  try {
    await agreementApi.confirmVersion(activeVersion.value.id)
    message.success('已表示同意这一版；等对方也同意并签署后即可置为生效')
    emit('changed')
  } catch (error: any) {
    console.warn('[协议协商] 同意失败', error)
  } finally {
    acting.value = false
  }
}

const proposeVisible = ref(false)
const proposeForm = reactive<{ baseVersionId?: number | string; changeReason: string; proposalNote: string }>({
  baseVersionId: undefined,
  changeReason: '',
  proposalNote: ''
})

const baseVersionOptions = computed(() =>
  orderedVersions.value
    .filter(v => v.status === 'DRAFT' || v.status === 'ACTIVE')
    .map(v => ({
      label: `第 ${v.versionNo} 版 · ${versionStatusLabel(v.status)}`,
      value: v.id
    }))
)

function openPropose() {
  proposeForm.baseVersionId = activeVersion.value?.id
  proposeForm.changeReason = ''
  proposeForm.proposalNote = ''
  proposeVisible.value = true
}

async function submitPropose() {
  if (!proposeForm.changeReason.trim()) {
    message.warning('请填写变更原因：你改了什么、为什么改（这是正式留痕）')
    return
  }
  acting.value = true
  try {
    const newVersionId: any = await agreementApi.propose(props.agreementId, {
      changeReason: proposeForm.changeReason.trim(),
      proposalNote: proposeForm.proposalNote.trim() || undefined,
      baseVersionId: proposeForm.baseVersionId
    })
    message.success('已提出修改：系统已为你新建一版草稿，现行版本继续有效')
    proposeVisible.value = false
    const vid = newVersionId?.data ?? newVersionId
    if (vid) {
      activeVersionId.value = String(vid)
      diff.value = null
    }
    emit('changed')
  } catch (error: any) {
    console.warn('[协议协商] 提出修改失败', error)
  } finally {
    acting.value = false
  }
}

const rejectVisible = ref(false)
const rejectReason = ref('')

function openReject() {
  rejectReason.value = ''
  rejectVisible.value = true
}

async function submitReject() {
  if (!activeVersion.value) return
  acting.value = true
  try {
    await agreementApi.rejectVersion(activeVersion.value.id, rejectReason.value.trim() || undefined)
    message.success('已拒绝这一版；现行生效版本继续有效，交易照常')
    rejectVisible.value = false
    emit('changed')
  } catch (error: any) {
    console.warn('[协议协商] 拒绝失败', error)
  } finally {
    acting.value = false
  }
}

// ── 初始化：默认选中待确认的草稿版本（没有则选现行生效版本） ──
function pickDefault() {
  const list = orderedVersions.value
  const draft = [...list].reverse().find(v => v.status === 'DRAFT')
  const active = list.find(v => v.status === 'ACTIVE')
  const chosen = draft || active || list[list.length - 1]
  if (chosen && !activeVersionId.value) {
    activeVersionId.value = String(chosen.id)
    loadDiff()
  }
}

onMounted(pickDefault)
watch(() => props.versions, () => {
  if (!activeVersionId.value) pickDefault()
}, { deep: false })
</script>

<style scoped>
.aneg-alert { margin-bottom: 10px; }
.aneg-card { margin-bottom: 12px; }
.aneg-card :deep(.ant-card-body) { padding: 12px 16px; max-height: 620px; overflow-y: auto; }
.aneg-sub { font-size: 12px; color: #999; margin-left: 4px; }
.aneg-empty { font-size: 13px; color: #999; line-height: 1.7; }
.aneg-muted { font-size: 12px; color: #999; line-height: 1.6; }

/* 时间线节点 */
.aneg-node { border: 1px solid #f0f0f0; border-radius: 6px; padding: 8px 10px; cursor: pointer; transition: all 0.2s; }
.aneg-node:hover { border-color: #91caff; }
.aneg-node.active { border-color: #1890ff; background: #f0f7ff; box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.12); }
.aneg-node-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 4px; }
.aneg-node-title { font-size: 13px; font-weight: 600; color: #333; }
.aneg-node-line { font-size: 12px; color: #595959; line-height: 1.6; }
.aneg-node-note { font-size: 12px; color: #0958d9; line-height: 1.6; margin-top: 2px; }
.aneg-node-meta { font-size: 12px; color: #666; line-height: 1.7; display: flex; gap: 10px; flex-wrap: wrap; margin-top: 2px; }
.aneg-ok { color: #389e0d; }
.aneg-wait { color: #fa8c16; }
.aneg-no-retro { font-size: 12px; color: #8c4a1f; background: #fff7e6; border-radius: 4px; padding: 4px 6px; margin-top: 4px; }

.aneg-summary { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 8px; }
.aneg-actions { margin-top: 12px; display: flex; flex-direction: column; gap: 6px; }
.aneg-block {
  font-size: 12px; color: #d46b08; background: #fff7e6; border: 1px solid #ffd591;
  border-radius: 4px; padding: 6px 8px; line-height: 1.6;
}
.aneg-form :deep(.ant-form-item) { margin-bottom: 12px; }
</style>
