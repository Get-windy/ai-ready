<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      full-height
      :title="detail?.title || '协议详情'"
    >
      <!-- ═══ 页头右侧：返回 / 刷新 / 发起变更 ═══ -->
      <template #headerExtra>
        <a-space :size="8">
          <a-button
            size="small"
            @click="goBack"
          >
            <RollbackOutlined /> 返回列表
          </a-button>
          <a-button
            size="small"
            :loading="loading"
            @click="reload"
          >
            <ReloadOutlined /> 刷新
          </a-button>
          <a-button
            v-permission="'agreement:version:create'"
            size="small"
            :disabled="!canChangeVersion"
            @click="changeVisible = true"
          >
            <SwapOutlined /> 发起变更
          </a-button>
        </a-space>
      </template>

      <a-spin
        :spinning="loading"
        class="detail-spin"
      >
        <div class="detail-body">
          <!-- ═══ 变更洽谈中提示：现行版本继续有效 ═══ -->
          <a-alert
            v-if="draftVersion && activeVersion"
            type="warning"
            show-icon
            class="detail-alert"
            message="有一版变更正在洽谈中，现行版本继续有效"
            description="双方就新版本达成一致并都确认后，新版本才生效；在此之前，交易与结算仍按现行生效版本执行。"
          />

          <!-- ═══ 一、基本信息 ═══ -->
          <a-card
            size="small"
            class="detail-card"
            title="基本信息"
          >
            <template #extra>
              <a-tag :color="statusColor(detail?.status)">
                {{ statusText(detail?.status) }}
              </a-tag>
            </template>
            <a-descriptions
              :column="3"
              size="small"
              bordered
            >
              <a-descriptions-item label="协议编号">
                {{ detail?.agreementNo || '—' }}
              </a-descriptions-item>
              <a-descriptions-item label="协议类型">
                <a-tag :color="typeMeta(detail?.agreementType).color">
                  {{ typeMeta(detail?.agreementType).label }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="当前生效版本">
                <span v-if="activeVersion">第 {{ activeVersion.versionNo }} 版</span>
                <span
                  v-else
                  class="muted"
                >尚未生效</span>
              </a-descriptions-item>
              <a-descriptions-item label="甲方（主体 / 租户）">
                {{ formatParty(detail?.partyAName, detail?.partyATenantName) }}
              </a-descriptions-item>
              <a-descriptions-item label="乙方（主体 / 租户）">
                {{ formatParty(detail?.partyBName, detail?.partyBTenantName, detail?.agreementType) }}
              </a-descriptions-item>
              <a-descriptions-item label="生效期">
                {{ formatPeriod(detail?.effectiveFrom, detail?.effectiveTo) }}
              </a-descriptions-item>
            </a-descriptions>
          </a-card>

          <!-- ═══ 二、条款约定 ═══ -->
          <a-card
            size="small"
            class="detail-card"
          >
            <template #title>
              <span>条款约定</span>
              <span
                v-if="termsVersion"
                class="card-title-sub"
              >
                （第 {{ termsVersion.versionNo }} 版 · {{ versionStatusText(termsVersion.status) }}）
              </span>
            </template>
            <template #extra>
              <a-space :size="8">
                <a-tag
                  v-if="termsEditable"
                  color="blue"
                >
                  草稿可改
                </a-tag>
                <a-tag
                  v-else
                  color="default"
                >
                  已生效，只读
                </a-tag>
                <a-button
                  v-if="termsEditable"
                  v-permission="'agreement:update'"
                  type="primary"
                  size="small"
                  :loading="savingTerms"
                  @click="handleSaveTerms"
                >
                  保存条款
                </a-button>
              </a-space>
            </template>

            <a-alert
              v-if="!termsEditable && termsVersion"
              type="info"
              show-icon
              class="detail-alert"
              message="已生效的版本不能再改"
              description="要调整约定，请点右上角「发起变更」：系统会复制出一份新的草稿版本，双方都确认后才生效，现行版本在此之前继续有效。"
            />

            <div
              v-if="!termGroups.length"
              class="empty-hint"
            >
              条款字典暂不可用（平台还没定义可选项）。条款必须从平台字典里选，不接受自由文本，这样才能被系统自动执行、也能作为司法举证依据。
            </div>

            <div
              v-else
              class="term-list"
            >
              <div
                v-for="group in termGroups"
                :key="group.termCode"
                class="term-item"
              >
                <div class="term-head">
                  <span class="term-name">
                    <span
                      v-if="isRequiredGroup(group)"
                      class="required-star"
                    >*</span>
                    {{ group.termName || group.termCode }}
                  </span>
                  <a-tag
                    v-if="isRequiredGroup(group)"
                    color="red"
                  >
                    必填
                  </a-tag>
                  <span
                    v-if="isRequiredGroup(group) && !selectedOf(group.termCode)?.optionCode"
                    class="term-warn"
                  >
                    必填条款未约定，协议不能生效
                  </span>
                </div>

                <div class="term-control">
                  <a-select
                    :value="selectedOf(group.termCode)?.optionCode"
                    size="small"
                    class="term-select"
                    allow-clear
                    :disabled="!termsEditable"
                    placeholder="请选择一项（平台只提供选项，不给默认值）"
                    @change="handleTermChange(group.termCode, $event)"
                  >
                    <a-select-option
                      v-for="opt in group.options"
                      :key="opt.optionCode"
                      :value="opt.optionCode"
                    >
                      <div class="opt-row">
                        <span class="opt-label">{{ opt.optionLabel }}</span>
                        <span class="opt-sem">{{ opt.semantics }}</span>
                      </div>
                    </a-select-option>
                  </a-select>

                  <a-input
                    v-if="paramNameOf(group)"
                    v-model:value="selected[group.termCode].paramValue"
                    size="small"
                    class="term-param"
                    :disabled="!termsEditable"
                    :placeholder="`请填写${paramNameOf(group)}`"
                  />
                </div>

                <div
                  v-if="semanticsOf(group)"
                  class="term-semantics"
                >
                  <InfoCircleOutlined class="term-semantics-icon" />
                  <span>选这一项后系统会这样执行：{{ semanticsOf(group) }}</span>
                </div>
              </div>
            </div>

            <div
              v-if="termsEditable && missingRequiredNames.length"
              class="block-reason"
            >
              还有必填条款没约定：{{ missingRequiredNames.join('、') }}。全部约定完才能置为生效。
            </div>
          </a-card>

          <!-- ═══ 三、双签状态与置生效 ═══ -->
          <a-card
            size="small"
            class="detail-card"
          >
            <template #title>
              <span>双方签署</span>
              <span
                v-if="confirmVersion"
                class="card-title-sub"
              >
                （第 {{ confirmVersion.versionNo }} 版 · {{ versionStatusText(confirmVersion.status) }}）
              </span>
            </template>

            <a-row :gutter="16">
              <a-col :span="12">
                <div class="sign-card">
                  <div class="sign-side">甲方 · {{ formatParty(detail?.partyAName, detail?.partyATenantName) }}</div>
                  <template v-if="confirmVersion?.partyAConfirmedAt">
                    <div class="sign-ok">
                      <CheckCircleFilled /> 已确认
                    </div>
                    <div class="sign-meta">
                      确认人：{{ confirmVersion?.partyAConfirmedBy || '—' }}
                    </div>
                    <div class="sign-meta">
                      确认时间：{{ confirmVersion?.partyAConfirmedAt }}
                    </div>
                  </template>
                  <div
                    v-else
                    class="sign-wait"
                  >
                    待确认
                  </div>
                </div>
              </a-col>
              <a-col :span="12">
                <div class="sign-card">
                  <div class="sign-side">
                    乙方 · {{ detail?.agreementType === 'CONSUMER_PROMISE' ? '不特定消费者' : formatParty(detail?.partyBName, detail?.partyBTenantName) }}
                  </div>
                  <template v-if="confirmVersion?.partyBConfirmedAt">
                    <div class="sign-ok">
                      <CheckCircleFilled /> 已确认
                    </div>
                    <div class="sign-meta">
                      确认人：{{ confirmVersion?.partyBConfirmedBy || '—' }}
                    </div>
                    <div class="sign-meta">
                      确认时间：{{ confirmVersion?.partyBConfirmedAt }}
                    </div>
                  </template>
                  <div
                    v-else
                    class="sign-wait"
                  >
                    待确认
                  </div>
                </div>
              </a-col>
            </a-row>

            <div class="sign-actions">
              <a-button
                v-if="mySide && !myConfirmed && canConfirm"
                v-permission="'agreement:version:confirm'"
                type="primary"
                size="small"
                :loading="confirming"
                @click="handleConfirm"
              >
                确认本方签署
              </a-button>
              <a-button
                v-if="canActivate"
                v-permission="'agreement:version:activate'"
                type="primary"
                size="small"
                :disabled="activateBlockers.length > 0"
                :loading="activating"
                @click="handleActivate"
              >
                置为生效
              </a-button>
              <span
                v-if="canActivate && activateBlockers.length"
                class="block-reason"
              >
                还不能生效，缺少：{{ activateBlockers.join('；') }}
              </span>
              <span
                v-else-if="canActivate"
                class="ready-hint"
              >
                必填条款已约定、双方已确认，可以置为生效。
              </span>
              <span
                v-if="!mySide"
                class="muted"
              >
                （当前登录租户不是这份协议的任一方，只能查看，不能代为签署）
              </span>
              <span class="muted">
                「确认」只表示同意这一版；要留下「谁签的、凭什么代表这家公司」的授权痕迹，
                请在下方「协议生命周期 → 签署」页完成正式签署。
              </span>
            </div>
          </a-card>

          <!-- ═══ 四、协议生命周期（内容设定 / 唯一送达 / 多轮协商 / 签署 / 终止） ═══ -->
          <a-card
            size="small"
            class="detail-card"
          >
            <template #title>
              <span>协议生命周期</span>
              <span class="card-title-sub">内容设定 → 唯一送达 → 多轮协商 → 签署 → 终止</span>
            </template>

            <a-tabs
              v-model:activeKey="lifecycleTab"
              size="small"
              class="detail-tabs"
              @change="onLifecycleTabChange"
            >
              <!-- ── 字段设定版 + 文字版 + 履约方式（§13.1 / §13.2 / §13.10） ── -->
              <a-tab-pane
                key="content"
                tab="内容约定"
              >
                <AgreementContentEditor
                  v-if="lifecycleTab === 'content'"
                  :version-id="contentVersionId"
                  @saved="handleContentSaved"
                />
              </a-tab-pane>

              <!-- ── 唯一送达（§13.5） ── -->
              <a-tab-pane
                key="invites"
                tab="送达与邀请"
              >
                <AgreementInvitePanel
                  v-if="lifecycleTab === 'invites'"
                  :agreement-id="agreementId"
                  :agreement-status="detail?.status"
                  :versions="versions"
                />
              </a-tab-pane>

              <!-- ── 多轮协商：时间线 + 逐条 diff + 同意/提出修改/拒绝（§13.4） ── -->
              <a-tab-pane
                key="negotiation"
                tab="多轮协商"
              >
                <AgreementNegotiationPanel
                  v-if="lifecycleTab === 'negotiation'"
                  :agreement-id="agreementId"
                  :versions="versions"
                  :active-version-id="activeVersion?.id"
                  @changed="reload"
                />
              </a-tab-pane>

              <!-- ── 签署（§13.6 自然人代表主体） ── -->
              <a-tab-pane
                key="sign"
                tab="签署"
              >
                <AgreementSignaturePanel
                  v-if="lifecycleTab === 'sign'"
                  :agreement-id="agreementId"
                  :version-id="confirmVersion?.id"
                  :version-no="confirmVersion?.versionNo"
                  :version-status="confirmVersion?.status"
                  :party-a="{
                    id: detail?.partyAId,
                    name: detail?.partyAName,
                    tenantId: detail?.partyATenantId,
                    tenantName: detail?.partyATenantName
                  }"
                  :party-b="{
                    id: detail?.partyBId,
                    name: detail?.partyBName,
                    tenantId: detail?.partyBTenantId,
                    tenantName: detail?.partyBTenantName
                  }"
                  :agreement-type="detail?.agreementType"
                  :my-tenant-id="userStore.tenantId"
                  :signed-sides="signedSides"
                />
              </a-tab-pane>

              <!-- ── 终止（§13.7 ⚠️ 终止 ≠ 免责） ── -->
              <a-tab-pane
                key="termination"
                tab="终止"
              >
                <AgreementTerminationPanel
                  v-if="lifecycleTab === 'termination'"
                  :agreement-id="agreementId"
                  :agreement-status="detail?.status"
                />
              </a-tab-pane>
            </a-tabs>
          </a-card>

          <!-- ═══ 五、版本历史 ═══ -->
          <a-card
            size="small"
            class="detail-card"
            title="版本历史"
          >
            <a-table
              :columns="versionColumns"
              :data-source="versions"
              :pagination="false"
              row-key="id"
              size="small"
              bordered
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'versionNo'">
                  第 {{ record.versionNo }} 版
                </template>
                <template v-else-if="column.key === 'status'">
                  <a-tag :color="versionStatusColor(record.status)">
                    {{ versionStatusText(record.status) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'changeReason'">
                  {{ record.changeReason || '首次签订' }}
                </template>
                <template v-else-if="column.key === 'partyAConfirmedAt'">
                  {{ record.partyAConfirmedAt || '待确认' }}
                </template>
                <template v-else-if="column.key === 'partyBConfirmedAt'">
                  {{ record.partyBConfirmedAt || '待确认' }}
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button
                    type="link"
                    size="small"
                    :loading="snapshotLoadingId === record.id"
                    @click="openSnapshot(record)"
                  >
                    查看条款快照
                  </a-button>
                </template>
              </template>
            </a-table>
          </a-card>
        </div>
      </a-spin>
    </PageContainer>

    <!-- ═══ 条款快照弹窗（只读，任何版本都可取证查看） ═══ -->
    <a-modal
      v-model:open="snapshotVisible"
      :title="`第 ${snapshotVersion?.versionNo ?? ''} 版条款快照`"
      :width="720"
      :footer="null"
    >
      <div class="snapshot-meta">
        <div>版本状态：{{ versionStatusText(snapshotVersion?.status) }}</div>
        <div>变更原因：{{ snapshotVersion?.changeReason || '首次签订' }}</div>
        <div>甲方确认时间：{{ snapshotVersion?.partyAConfirmedAt || '待确认' }}</div>
        <div>乙方确认时间：{{ snapshotVersion?.partyBConfirmedAt || '待确认' }}</div>
      </div>
      <div
        v-if="snapshotTerms.length"
        class="snapshot-terms"
      >
        <div
          v-for="t in snapshotTerms"
          :key="t.termCode"
          class="snapshot-term"
        >
          <div class="snapshot-term-name">{{ t.termName || t.termCode }}</div>
          <div class="snapshot-term-value">
            {{ t.optionLabel || t.optionCode || '未约定' }}
            <span v-if="t.paramValue">（参数：{{ t.paramValue }}）</span>
          </div>
          <div
            v-if="t.semantics"
            class="term-semantics"
          >
            {{ t.semantics }}
          </div>
        </div>
      </div>
      <div
        v-else
        class="empty-hint"
      >
        这一版没有可展示的条款明细（可能只保存了完整快照文件）。
      </div>
      <a-collapse
        v-if="snapshotRaw"
        ghost
        class="snapshot-raw"
      >
        <a-collapse-panel
          key="raw"
          header="查看原始快照内容"
        >
          <pre class="snapshot-pre">{{ snapshotRaw }}</pre>
        </a-collapse-panel>
      </a-collapse>
    </a-modal>

    <!-- ═══ 发起变更弹窗 ═══ -->
    <a-modal
      v-model:open="changeVisible"
      title="发起变更"
      :width="560"
      :confirm-loading="submittingChange"
      ok-text="发起变更"
      cancel-text="取消"
      @ok="handleSubmitChange"
    >
      <a-alert
        type="warning"
        show-icon
        message="变更谈成之前，现行版本继续有效"
        description="系统会复制当前生效版本生成一份新的待确认版本；双方都确认后新版本才生效，已发生的单据与结算不受影响。"
      />
      <a-form
        layout="vertical"
        style="margin-top: 12px"
      >
        <a-form-item
          label="变更原因"
          required
        >
          <a-textarea
            v-model:value="changeReason"
            :rows="3"
            :maxlength="255"
            show-count
            placeholder="例如：佣金比例由 8% 调整为 10%，自下月起执行"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 协议详情页（分区卡片：基本信息 / 条款约定 / 双方签署 / 版本历史）
 *
 * 设计依据：docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md §十二 与 §3.4.4b~d3
 *   · 条款只能从平台字典里选「一个枚举选项 + 可选参数」，不是自由文本 —— 选的时候就要看到
 *     该选项的 semantics（选了这个系统会怎么执行），这是给双方看的条款说明书；
 *   · 平台不给默认值：必填条款没选完，不允许置为生效（后端会拒，前端提前禁用并说明缺什么）；
 *   · 双签缺一不可：双方确认痕迹都在，版本才允许生效；
 *   · 已生效版本只读：ACTIVE 版本没有任何编辑入口，改协议只能「发起变更」建新草稿版本；
 *   · 变更谈成之前，现行版本继续显示为生效中、交易照常按它执行。
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  RollbackOutlined, ReloadOutlined, SwapOutlined, InfoCircleOutlined, CheckCircleFilled
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import AgreementContentEditor from './components/AgreementContentEditor.vue'
import AgreementInvitePanel from './components/AgreementInvitePanel.vue'
import AgreementNegotiationPanel from './components/AgreementNegotiationPanel.vue'
import AgreementSignaturePanel from './components/AgreementSignaturePanel.vue'
import AgreementTerminationPanel from './components/AgreementTerminationPanel.vue'
import { usePermission } from '@/composables/usePermission'
import { useUserStore } from '@/stores/user'
import {
  agreementApi,
  AGREEMENT_TYPE_META,
  AGREEMENT_STATUS_TEXT,
  AGREEMENT_STATUS_COLOR,
  AGREEMENT_VERSION_STATUS_TEXT,
  AGREEMENT_VERSION_STATUS_COLOR,
  type Agreement,
  type AgreementType,
  type AgreementStatus,
  type AgreementTerm,
  type AgreementTermOptionGroup,
  type AgreementVersion,
  type AgreementVersionStatus
} from '@/api/agreement'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { checkPermission } = usePermission()

const agreementId = computed(() => String(route.params.id || ''))

const loading = ref(false)
const detail = ref<Agreement | null>(null)
const versions = ref<AgreementVersion[]>([])
const termGroups = ref<AgreementTermOptionGroup[]>([])
/** 从列表页带过来的版本 ID（刚发起完变更时直接落在新草稿版本上） */
const focusVersionId = ref<string>(String(route.query.versionId || ''))

// ── 当前正在看的三类版本 ──────────────────────────────
const activeVersion = computed(() => versions.value.find(v => v.status === 'ACTIVE'))
const draftVersion = computed(() => versions.value.find(v => v.status === 'DRAFT'))
/** 条款区展示的版本：优先看指定版本 → 草稿版本 → 现行生效版本 */
const termsVersion = computed(() => {
  if (focusVersionId.value) {
    const hit = versions.value.find(v => String(v.id) === focusVersionId.value)
    if (hit) return hit
  }
  return draftVersion.value || activeVersion.value
})
/** 需要双方签署的版本：待确认的草稿优先，其次现行版本 */
const confirmVersion = computed(() => draftVersion.value || activeVersion.value)

// ── 协议生命周期分组 Tab（内容设定 / 唯一送达 / 多轮协商 / 签署 / 终止） ──
const lifecycleTab = ref('content')
/**
 * 内容约定编辑的版本：优先当前查看的版本（草稿或现行版本）。
 * 已生效版本只读（后端也会拒），界面据此显示只读原因 —— 改内容必须先发起变更。
 */
const contentVersionId = computed(() => termsVersion.value?.id ?? null)

/**
 * 已经表态过的哪几方（用版本上的确认痕迹判定 —— 签署会同时完成本方确认，
 * 双签痕迹仍是"能否置为生效"的唯一依据）。
 */
const signedSides = computed<string[]>(() => {
  const out: string[] = []
  if (confirmVersion.value?.partyAConfirmedAt) out.push('A')
  if (confirmVersion.value?.partyBConfirmedAt) out.push('B')
  return out
})

/**
 * 各面板用 v-if 在页签激活时才挂载：不然一进详情页就会同时打 5 个接口，
 * 而用户通常只关心其中一个页签。
 */
function onLifecycleTabChange() {
  // 面板自行在挂载时取数，这里不需要额外动作
}

/** 内容保存后会清空双方确认痕迹、必填缺项也会变 —— 重新拉一遍详情与版本 */
function handleContentSaved() {
  reload()
}

// ── 条款选择状态 ──────────────────────────────────────
const selected = reactive<Record<string, { optionCode?: string; paramValue?: string }>>({})
const savingTerms = ref(false)

/** 从版本里取条款明细：优先版本自带的 terms，其次解析快照 */
function extractTerms(version?: AgreementVersion | null): AgreementTerm[] {
  if (!version) return []
  if (Array.isArray(version.terms) && version.terms.length) return version.terms
  let snap: any = version.snapshot ?? version.snapshotJson
  if (typeof snap === 'string') {
    try { snap = JSON.parse(snap) } catch { return [] }
  }
  if (snap && Array.isArray(snap.terms)) return snap.terms as AgreementTerm[]
  return []
}

function syncSelection() {
  for (const key of Object.keys(selected)) delete selected[key]
  const terms = extractTerms(termsVersion.value)
  for (const t of terms) {
    selected[t.termCode] = {
      optionCode: t.optionCode,
      paramValue: t.paramValue === null || t.paramValue === undefined ? '' : String(t.paramValue)
    }
  }
}

function selectedOf(termCode: string) {
  return selected[termCode]
}

function isRequiredGroup(group: AgreementTermOptionGroup): boolean {
  return !!(group.required || group.options?.some(o => o.required))
}

function semanticsOf(group: AgreementTermOptionGroup): string {
  const code = selected[group.termCode]?.optionCode
  if (!code) return ''
  return group.options.find(o => o.optionCode === code)?.semantics || ''
}

/** 选中项需要附带参数时，返回参数名（如「分账比例」） */
function paramNameOf(group: AgreementTermOptionGroup): string {
  const code = selected[group.termCode]?.optionCode
  if (!code) return ''
  return group.options.find(o => o.optionCode === code)?.needsParam || ''
}

function handleTermChange(termCode: string, value: any) {
  selected[termCode] = { optionCode: value || undefined, paramValue: '' }
}

/** 必填但还没约定的条款名（置生效的硬性前提之一） */
const missingRequiredNames = computed(() =>
  termGroups.value
    .filter(g => isRequiredGroup(g) && !selected[g.termCode]?.optionCode)
    .map(g => g.termName || g.termCode)
)

/** 选了需要参数的选项、但参数没填 */
const missingParamNames = computed(() =>
  termGroups.value
    .filter(g => {
      const st = selected[g.termCode]
      if (!st?.optionCode) return false
      const need = paramNameOf(g)
      return !!need && !st.paramValue
    })
    .map(g => `${g.termName || g.termCode}（还差${paramNameOf(g)}）`)
)

/** 只有「洽谈中」的版本可以改条款；已生效版本一律只读 */
const termsEditable = computed(() =>
  termsVersion.value?.status === 'DRAFT' && checkPermission('agreement:update')
)

// ── 本方身份与双签 ────────────────────────────────────
/** 当前登录租户是协议的哪一方；都不是则为空（只能看，不能代签） */
const mySide = computed<'A' | 'B' | null>(() => {
  const myTenant = String(userStore.tenantId ?? '')
  if (!myTenant) return null
  if (detail.value?.partyATenantId && String(detail.value.partyATenantId) === myTenant) return 'A'
  if (detail.value?.partyBTenantId && String(detail.value.partyBTenantId) === myTenant) return 'B'
  return null
})

const myConfirmed = computed(() => {
  if (!confirmVersion.value || !mySide.value) return false
  return mySide.value === 'A'
    ? !!confirmVersion.value.partyAConfirmedAt
    : !!confirmVersion.value.partyBConfirmedAt
})

const canConfirm = computed(() => confirmVersion.value?.status === 'DRAFT')
const canActivate = computed(() => confirmVersion.value?.status === 'DRAFT')

/** 置生效还缺什么（逐条写清楚，不用「条件不满足」这种含糊话） */
const activateBlockers = computed(() => {
  const blockers: string[] = []
  if (missingRequiredNames.value.length) {
    blockers.push(`必填条款未约定（${missingRequiredNames.value.join('、')}）`)
  }
  if (missingParamNames.value.length) {
    blockers.push(`已选条款的附带参数未填（${missingParamNames.value.join('、')}）`)
  }
  if (!confirmVersion.value?.partyAConfirmedAt) blockers.push('甲方尚未确认签署')
  if (!confirmVersion.value?.partyBConfirmedAt) blockers.push('乙方尚未确认签署')
  return blockers
})

// ── 版本历史 ──────────────────────────────────────────
const versionColumns = [
  { title: '版本', key: 'versionNo', width: 100 },
  { title: '状态', key: 'status', width: 130 },
  { title: '变更原因', key: 'changeReason', width: 240 },
  { title: '甲方确认时间', key: 'partyAConfirmedAt', width: 170 },
  { title: '乙方确认时间', key: 'partyBConfirmedAt', width: 170 },
  { title: '操作', key: 'action', width: 140 }
]

// ── 快照弹窗（任何版本都可取证查看，包括已被取代的历史版本） ──
const snapshotVisible = ref(false)
const snapshotVersion = ref<AgreementVersion | null>(null)
const snapshotTerms = ref<AgreementTerm[]>([])
const snapshotRaw = ref('')
const snapshotLoadingId = ref<number | string | null>(null)

async function openSnapshot(record: AgreementVersion) {
  snapshotLoadingId.value = record.id
  try {
    const res: any = await agreementApi.getVersion(record.id)
    const v: AgreementVersion = (res?.data ?? res) || record
    snapshotVersion.value = v
    snapshotTerms.value = extractTerms(v)
    const raw = v.snapshot ?? v.snapshotJson
    snapshotRaw.value = raw
      ? (typeof raw === 'string' ? raw : JSON.stringify(raw, null, 2))
      : ''
    snapshotVisible.value = true
  } catch (error: any) {
    console.warn('[协议详情] 版本快照加载失败', error)
  } finally {
    snapshotLoadingId.value = null
  }
}

// ── 展示辅助 ──────────────────────────────────────────
function typeMeta(type?: string) {
  return AGREEMENT_TYPE_META[type as AgreementType] || { label: type || '未知', color: 'default', shortDesc: '', detail: '' }
}
function statusText(status?: string) {
  return AGREEMENT_STATUS_TEXT[status as AgreementStatus] || status || '未知'
}
function statusColor(status?: string) {
  return AGREEMENT_STATUS_COLOR[status as AgreementStatus] || 'default'
}
function versionStatusText(status?: string) {
  return AGREEMENT_VERSION_STATUS_TEXT[status as AgreementVersionStatus] || status || '未知'
}
function versionStatusColor(status?: string) {
  return AGREEMENT_VERSION_STATUS_COLOR[status as AgreementVersionStatus] || 'default'
}
function formatParty(partyName?: string, tenantName?: string, type?: string): string {
  if (type === 'CONSUMER_PROMISE' && !partyName && !tenantName) return '不特定消费者'
  const name = partyName || '（未指定主体）'
  return tenantName ? `${name}（${tenantName}）` : name
}
function formatPeriod(from?: string, to?: string): string {
  if (!from && !to) return '长期有效'
  return `${from || '—'} 至 ${to || '长期'}`
}

// ── 数据加载 ──────────────────────────────────────────
/** 字典分组兼容两种下发形态：数组 [{termCode, options}] 或映射 { termCode: options[] } */
function normalizeGroups(raw: any): AgreementTermOptionGroup[] {
  if (!raw) return []
  if (Array.isArray(raw)) {
    return raw.map((g: any) => ({
      termCode: g.termCode,
      termName: g.termName,
      required: g.required,
      options: g.options || g.children || []
    })).filter((g: any) => !!g.termCode)
  }
  if (typeof raw === 'object') {
    return Object.keys(raw).map(code => {
      const opts: any[] = raw[code] || []
      return {
        termCode: code,
        termName: opts[0]?.termName,
        required: opts.some((o: any) => o.required),
        options: opts
      }
    })
  }
  return []
}

async function reload() {
  const id = agreementId.value
  if (!id) return
  loading.value = true
  try {
    const [dRes, vRes, gRes] = await Promise.all([
      agreementApi.getDetail(id),
      agreementApi.listVersions(id).catch(() => [] as AgreementVersion[]),
      agreementApi.listTermOptionGroups().catch(() => [] as AgreementTermOptionGroup[])
    ])
    const d: any = dRes?.data ?? dRes
    detail.value = d || null

    const vList: any = vRes?.data ?? vRes
    let list: AgreementVersion[] = Array.isArray(vList) ? vList : (vList?.records || [])
    // 后端未单独下发版本列表时，用详情带出的当前生效版本兜底
    if (!list.length && d?.currentVersion) list = [d.currentVersion]
    versions.value = list

    const g: any = gRes?.data ?? gRes
    termGroups.value = normalizeGroups(g)

    syncSelection()
  } catch (error: any) {
    console.warn('[协议详情] 加载失败', error)
  } finally {
    loading.value = false
  }
}

/** 切换查看的版本后重新回填该版本的条款选择 */
watch(termsVersion, () => syncSelection())

// ── 操作 ──────────────────────────────────────────────
async function handleSaveTerms() {
  if (!termsVersion.value) return
  const terms = Object.keys(selected)
    .filter(code => !!selected[code]?.optionCode)
    .map(code => ({
      termCode: code,
      optionCode: selected[code].optionCode,
      paramValue: selected[code].paramValue || undefined
    }))
  savingTerms.value = true
  try {
    await agreementApi.update(agreementId.value, { versionId: termsVersion.value.id, terms })
    message.success('草稿条款已保存')
    await reload()
  } catch (error: any) {
    console.warn('[协议详情] 保存条款失败', error)
  } finally {
    savingTerms.value = false
  }
}

const confirming = ref(false)
async function handleConfirm() {
  if (!confirmVersion.value) return
  confirming.value = true
  try {
    await agreementApi.confirmVersion(confirmVersion.value.id)
    message.success('本方已确认签署，等对方确认后即可置为生效')
    await reload()
  } catch (error: any) {
    console.warn('[协议详情] 确认签署失败', error)
  } finally {
    confirming.value = false
  }
}

const activating = ref(false)
async function handleActivate() {
  if (!confirmVersion.value) return
  activating.value = true
  try {
    await agreementApi.activateVersion(confirmVersion.value.id)
    message.success('协议已生效')
    await reload()
  } catch (error: any) {
    console.warn('[协议详情] 置生效失败', error)
  } finally {
    activating.value = false
  }
}

// ── 发起变更 ──────────────────────────────────────────
const changeVisible = ref(false)
const changeReason = ref('')
const submittingChange = ref(false)
/** 没有生效版本时无可复制的底版，先置生效后再谈变更 */
const canChangeVersion = computed(() => !!activeVersion.value)

async function handleSubmitChange() {
  if (!changeReason.value.trim()) {
    message.warning('请填写变更原因，双方确认时要看这一条')
    return
  }
  submittingChange.value = true
  try {
    const newVersionId: any = await agreementApi.createVersion(agreementId.value, {
      changeReason: changeReason.value.trim()
    })
    message.success('已发起变更，现行版本继续有效，等对方确认')
    changeVisible.value = false
    changeReason.value = ''
    focusVersionId.value = String(newVersionId?.data ?? newVersionId ?? '')
    await reload()
  } catch (error: any) {
    console.warn('[协议详情] 发起变更失败', error)
  } finally {
    submittingChange.value = false
  }
}

function goBack() {
  router.push('/agreement')
}

const handleError = (error: Error) => {
  console.error('[协议详情] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(reload)
</script>

<style scoped>
/* 详情内容较长：PageContainer 的内容区是 overflow:hidden，必须由这一层自己滚动，否则会被裁掉 */
.detail-spin { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.detail-spin :deep(.ant-spin-container) { flex: 1; min-height: 0; overflow-y: auto; }
.detail-body { padding: 12px 16px 24px; display: flex; flex-direction: column; gap: 12px; }
.detail-card :deep(.ant-card-body) { padding: 12px 16px; }
.card-title-sub { font-size: 12px; color: #999; margin-left: 4px; }
.detail-alert { margin-bottom: 12px; }
.muted { color: #999; }
.empty-hint { font-size: 13px; color: #999; line-height: 1.7; }

/* 条款区 */
.term-list { display: flex; flex-direction: column; gap: 12px; }
.term-item { border-bottom: 1px dashed #f0f0f0; padding-bottom: 10px; }
.term-item:last-child { border-bottom: none; padding-bottom: 0; }
.term-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.term-name { font-size: 13px; font-weight: 600; color: #333; }
.required-star { color: #f5222d; margin-right: 2px; }
.term-warn { font-size: 12px; color: #f5222d; }
.term-control { display: flex; align-items: center; gap: 8px; }
.term-select { flex: 1; min-width: 0; max-width: 520px; }
.term-param { width: 200px; flex-shrink: 0; }
.opt-row { display: flex; flex-direction: column; line-height: 1.4; }
.opt-label { font-size: 13px; }
.opt-sem { font-size: 12px; color: #999; white-space: normal; }
.term-semantics {
  display: flex; align-items: flex-start; gap: 6px; margin-top: 6px;
  font-size: 12px; color: #595959; background: #f6ffed;
  border: 1px solid #b7eb8f; border-radius: 4px; padding: 6px 8px; line-height: 1.6;
}
.term-semantics-icon { color: #52c41a; margin-top: 2px; }
.block-reason {
  margin-top: 10px; font-size: 12px; color: #d4380d;
  background: #fff2e8; border: 1px solid #ffbb96; border-radius: 4px; padding: 6px 8px; line-height: 1.6;
}
.ready-hint { font-size: 12px; color: #389e0d; }

/* 双签区 */
.sign-card { border: 1px solid #f0f0f0; border-radius: 6px; padding: 10px 12px; background: #fafafa; }
.sign-side { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 6px; }
.sign-ok { color: #389e0d; font-size: 13px; display: flex; align-items: center; gap: 4px; }
.sign-wait { color: #fa8c16; font-size: 13px; }
.sign-meta { font-size: 12px; color: #666; line-height: 1.7; }
.sign-actions { display: flex; align-items: center; gap: 10px; margin-top: 12px; flex-wrap: wrap; }

/* 快照弹窗 */
.snapshot-meta { font-size: 12px; color: #666; line-height: 1.9; margin-bottom: 10px; }
.snapshot-terms { display: flex; flex-direction: column; gap: 8px; }
.snapshot-term { border: 1px solid #f0f0f0; border-radius: 4px; padding: 8px 10px; }
.snapshot-term-name { font-size: 13px; font-weight: 600; color: #333; }
.snapshot-term-value { font-size: 13px; color: #1890ff; margin-top: 2px; }
.snapshot-raw { margin-top: 10px; }
.snapshot-pre {
  max-height: 260px; overflow: auto; font-size: 12px; line-height: 1.6;
  background: #fafafa; border-radius: 4px; padding: 8px; margin: 0;
}

/* 协议生命周期分组 Tab */
.detail-tabs { margin-top: -4px; }
.detail-tabs :deep(.ant-tabs-nav) { margin-bottom: 10px; }
</style>
