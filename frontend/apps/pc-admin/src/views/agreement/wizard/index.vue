<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      full-height
      title="发起契约"
    >
      <template #headerExtra>
        <a-button
          size="small"
          @click="goList"
        >
          <RollbackOutlined /> 返回列表
        </a-button>
      </template>

      <div class="awz">
        <a-steps
          :current="step"
          size="small"
          class="awz-steps"
        >
          <a-step title="起点与两端" />
          <a-step title="逐项约定" />
          <a-step title="预览并完成" />
        </a-steps>

        <!-- ═══ 第一步：起点（模板 / 从零）与两端 ═══ -->
        <template v-if="step === 0">
          <a-card
            size="small"
            class="awz-card"
            title="一、从哪里开始"
          >
            <a-alert
              type="warning"
              show-icon
              class="awz-alert"
              message="模板是显式选择的起点，不是自动套用的默认值"
              description="用模板只是替你预填一遍；模板里有的内容不等于已经约定 —— 是否算约定，只以双方在本次这一版上确认并签署的内容为准。"
            />
            <a-radio-group
              v-model:value="origin"
              class="awz-origin"
            >
              <a-radio
                value="template"
                :disabled="!canApplyTemplate"
              >
                从模板起草（可选平台模板或本租户模板）
              </a-radio>
              <a-radio value="blank">
                从零起草（不套用任何模板）
              </a-radio>
            </a-radio-group>
            <div
              v-if="!canApplyTemplate"
              class="awz-muted"
            >
              当前账号没有「用模板发起契约」的权限，只能从零起草（后端对模板发起同时要求建协议权与用模板权）。
            </div>

            <div
              v-if="origin === 'template'"
              class="awz-templates"
            >
              <a-spin :spinning="templateLoading">
                <div
                  v-if="!templates.length"
                  class="awz-empty"
                >
                  没有可用的模板（平台模板与本租户模板都为空，或当前账号没有模板查看权限）。
                  可以改为「从零起草」。
                </div>
                <div
                  v-else
                  class="awz-template-grid"
                >
                  <div
                    v-for="t in templates"
                    :key="t.id"
                    :class="['awz-template', { active: String(selectedTemplateId) === String(t.id) }]"
                    @click="selectTemplate(t)"
                  >
                    <div class="awz-template-head">
                      <span class="awz-template-name">{{ t.templateName }}</span>
                      <a-tag :color="TEMPLATE_SCOPE_COLOR[t.scope || ''] || 'default'">
                        {{ t.scopeLabel || TEMPLATE_SCOPE_TEXT[t.scope || ''] || '' }}
                      </a-tag>
                    </div>
                    <div class="awz-template-line">
                      适用：{{ typeLabel(t.agreementType) }}
                      <span class="awz-muted">
                        （预填 字段 {{ t.settingCount ?? 0 }} / 条款 {{ t.termCount ?? 0 }} / 文字 {{ t.narrativeCount ?? 0 }}）
                      </span>
                    </div>
                    <div
                      v-if="t.description"
                      class="awz-template-desc"
                    >
                      {{ t.description }}
                    </div>
                    <div
                      v-if="t.legalReviewStatus !== 'APPROVED'"
                      class="awz-template-flag"
                    >
                      法务审核：{{ t.legalReviewStatusLabel || LEGAL_REVIEW_TEXT[t.legalReviewStatus || ''] || '—' }}
                    </div>
                  </div>
                </div>
              </a-spin>
            </div>
          </a-card>

          <a-card
            size="small"
            class="awz-card"
            title="二、和谁签（协议类型与两端）"
          >
            <div class="awz-type-grid">
              <div
                v-for="t in ALL_TYPES"
                :key="t"
                :class="['awz-type', { active: form.agreementType === t }]"
                @click="handleTypeChange(t)"
              >
                <div class="awz-type-head">
                  <span class="awz-type-name">{{ AGREEMENT_TYPE_META[t].label }}</span>
                  <span class="awz-type-side">{{ AGREEMENT_TYPE_META[t].shortDesc }}</span>
                </div>
                <div class="awz-type-desc">
                  {{ AGREEMENT_TYPE_META[t].detail }}
                </div>
              </div>
            </div>

            <a-form
              layout="vertical"
              class="awz-form"
            >
              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="协议标题">
                    <a-input
                      v-model:value="form.title"
                      size="small"
                      :maxlength="100"
                      placeholder="例如：2026 年度华东区代销合作框架"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="生效期">
                    <a-range-picker
                      v-model:value="period"
                      size="small"
                      style="width: 100%"
                      @change="handlePeriodChange"
                    />
                    <div class="awz-muted">
                      留空表示长期有效；到期前可以发起变更续签。
                    </div>
                  </a-form-item>
                </a-col>

                <a-col :span="12">
                  <a-form-item :label="form.agreementType === 'CONSUMER_PROMISE' ? '承诺方主体（甲方）' : '甲方主体'">
                    <a-input
                      :value="partyALabel"
                      readonly
                      size="small"
                      placeholder="点击右侧「选择」从往来单位里挑"
                    >
                      <template #suffix>
                        <a-button
                          type="link"
                          size="small"
                          @click="pickerSide = 'A'"
                        >
                          选择
                        </a-button>
                      </template>
                    </a-input>
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="甲方所属租户">
                    <a-select
                      v-model:value="form.partyATenantId"
                      size="small"
                      show-search
                      allow-clear
                      option-filter-prop="label"
                      placeholder="请选择租户"
                      :options="tenantOptions"
                      :disabled="form.agreementType === 'PLATFORM_SERVICE'"
                    />
                    <div
                      v-if="form.agreementType === 'PLATFORM_SERVICE'"
                      class="awz-muted"
                    >
                      平台服务协议的甲方固定为平台方（平台主体 + 系统租户），不用改。
                    </div>
                  </a-form-item>
                </a-col>

                <template v-if="form.agreementType !== 'CONSUMER_PROMISE'">
                  <a-col :span="12">
                    <a-form-item label="乙方主体">
                      <a-input
                        :value="partyBLabel"
                        readonly
                        size="small"
                        placeholder="点击右侧「选择」从往来单位里挑"
                      >
                        <template #suffix>
                          <a-button
                            type="link"
                            size="small"
                            @click="pickerSide = 'B'"
                          >
                            选择
                          </a-button>
                        </template>
                      </a-input>
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="乙方所属租户">
                      <a-select
                        v-model:value="form.partyBTenantId"
                        size="small"
                        show-search
                        allow-clear
                        option-filter-prop="label"
                        placeholder="请选择租户"
                        :options="tenantOptions"
                      />
                    </a-form-item>
                  </a-col>
                </template>
                <a-col
                  v-else
                  :span="24"
                >
                  <a-alert
                    type="info"
                    show-icon
                    message="这项承诺面向不特定消费者，不需要指定乙方"
                    description="承诺只能高于法定与平台底线，低于底线的内容不允许保存。"
                  />
                </a-col>
              </a-row>
            </a-form>

            <div class="awz-actions">
              <a-button
                type="primary"
                size="small"
                :loading="creating"
                @click="handleCreate"
              >
                下一步：逐项约定
              </a-button>
              <span class="awz-muted">
                这一步会先建出协议主档与首个草稿版本，接下来的内容都填在这一版上。
              </span>
            </div>
          </a-card>
        </template>

        <!-- ═══ 第二步：逐项约定（字段设定版 / 文字版 / 履约方式） ═══ -->
        <template v-else-if="step === 1">
          <a-card
            size="small"
            class="awz-card"
            title="三、约定本版内容"
          >
            <template #extra>
              <span class="awz-muted">
                协议已建好：{{ created?.agreementNo || '' }} 第 {{ draftVersionNo ?? '—' }} 版（草稿）
              </span>
            </template>

            <!-- 模板预填：必须让人看到哪些项是模板带来的、仍需双方确认 -->
            <a-alert
              v-if="templateSourceText"
              type="warning"
              show-icon
              class="awz-alert"
              :message="`下面带「模板预填，仍需双方确认」标记的，是模板「${selectedTemplateName}」带过来的`"
              :description="`${templateSourceText}。模板里有 ≠ 已经约定：这些内容仍需双方在本次这一版上逐项确认并签署，才是约定。`"
            />
            <a-checkbox
              v-if="templateSourceText"
              v-model:checked="templateChecked"
              class="awz-checkbox"
            >
              我已逐项核对模板预填的内容，并确认这些内容仍需双方在本次这一版上确认（模板不是默认值）。
            </a-checkbox>

            <AgreementContentEditor
              ref="contentEditorRef"
              :version-id="draftVersionId"
              :template-setting-keys="templateSettingKeys"
              :template-narrative-keys="templateNarrativeKeys"
              :template-source-text="templateSourceText"
              @saved="onContentSaved"
            />

            <div
              v-if="pendingMissing.length"
              class="awz-block"
            >
              还不能进入下一步：以下必填项没有约定 —— {{ pendingMissing.join('、') }}。
              <div class="awz-muted">
                未约定的项系统不会按默认值执行（签得下来，但用不了）；要让下游能跑，请先把它们约定清楚。
              </div>
            </div>

            <div class="awz-actions">
              <a-button
                size="small"
                @click="step = 0"
              >
                上一步
              </a-button>
              <a-button
                type="primary"
                size="small"
                :disabled="!canGoPreview"
                @click="goPreview"
              >
                下一步：预览
              </a-button>
            </div>
          </a-card>
        </template>

        <!-- ═══ 第三步：预览并完成 ═══ -->
        <template v-else>
          <a-card
            size="small"
            class="awz-card"
            title="四、预览并完成"
          >
            <a-descriptions
              :column="2"
              size="small"
              bordered
            >
              <a-descriptions-item label="协议标题">
                {{ form.title }}
              </a-descriptions-item>
              <a-descriptions-item label="协议类型">
                {{ AGREEMENT_TYPE_META[form.agreementType].label }}
              </a-descriptions-item>
              <a-descriptions-item label="甲方（主体 / 租户）">
                {{ partyALabel || '—' }} / {{ tenantNameOf(form.partyATenantId) }}
              </a-descriptions-item>
              <a-descriptions-item label="乙方（主体 / 租户）">
                <template v-if="form.agreementType === 'CONSUMER_PROMISE'">
                  不特定消费者
                </template>
                <template v-else>
                  {{ partyBLabel || '—' }} / {{ tenantNameOf(form.partyBTenantId) }}
                </template>
              </a-descriptions-item>
              <a-descriptions-item label="生效期">
                {{ formatPeriod(form.effectiveFrom, form.effectiveTo) }}
              </a-descriptions-item>
              <a-descriptions-item label="起草起点">
                {{ templateSourceText || '从零起草（未使用模板）' }}
              </a-descriptions-item>
              <a-descriptions-item label="已约定的字段">
                {{ agreedSettingNames.length ? agreedSettingNames.join('、') : '暂无' }}
              </a-descriptions-item>
              <a-descriptions-item label="履约方式集合">
                {{ modeNames.length ? modeNames.join('、') : '未约定（不表示随便用哪种）' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="文字条款"
                :span="2"
              >
                {{ narrativeTitles.length ? narrativeTitles.join('、') : '暂未填写' }}
                <span class="awz-muted">
                  （此类条款系统不会自动执行，需人工处理）
                </span>
              </a-descriptions-item>
            </a-descriptions>

            <a-alert
              type="warning"
              show-icon
              class="awz-alert awz-alert-top"
              message="接下来要做的是：送达 → 协商 → 签署"
              description="这一版现在是草稿，双方都签署后才可以置为生效。请到协议详情页发起「唯一送达」，把这一版发给对方；对方确认或提出修改会形成新一轮协商，协商期间现行生效版本继续有效。"
            />

            <div class="awz-actions">
              <a-button
                size="small"
                @click="step = 1"
              >
                上一步
              </a-button>
              <a-button
                type="primary"
                size="small"
                @click="goDetail"
              >
                完成，进入协议详情
              </a-button>
            </div>
          </a-card>
        </template>
      </div>
    </PageContainer>

    <PartnerSelectModal
      :open="pickerVisible"
      :default-tab="'customer'"
      @update:open="pickerVisible = $event"
      @select="handlePartySelected"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 发起契约向导 —— §13.4「模板 / 从零起草 → 首版草稿」
 *
 * 三步：起点与两端 → 逐项约定（设定版 / 文字版 / 履约方式）→ 预览并完成。
 *
 * 三条必须做对的事：
 *   ① 用**仓里已有的往来单位选择弹窗**选主体，不另造选择器；
 *   ② 模板不是默认值：用了模板之后，界面必须让人看到"哪些项是模板带来的、仍需双方确认"
 *      （模板来源由后端记进版本快照 templateSourceText，界面据此如实展示）；
 *   ③ 必填项没约齐时，明确列出"还缺哪几项"并拦下"下一步"，而不是只说"条件不满足"。
 *
 * ⚠️ 口径说明：必填**设定**不阻断签署，阻断的是「使用」（§13.3 补充口径 2）。
 * 这里拦住"下一步"只是发起向导的引导 —— 避免用户发一份自己都知道没法执行的协议出去；
 * 到了协商阶段（已有草稿）编辑内容时不再拦保存，那正是"洽谈中"的含义。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import { RollbackOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import PartnerSelectModal from '@/components/PartnerSelectModal/index.vue'
import AgreementContentEditor from '../components/AgreementContentEditor.vue'
import { tenantApi } from '@/api/tenant'
import { usePermission } from '@/composables/usePermission'
import { useUserStore } from '@/stores/user'
import {
  agreementApi,
  AGREEMENT_TYPE_META,
  TEMPLATE_SCOPE_TEXT,
  TEMPLATE_SCOPE_COLOR,
  LEGAL_REVIEW_TEXT,
  FULFILLMENT_MODE_OPTIONS,
  type AgreementTemplate,
  type AgreementTemplateDetail,
  type AgreementType,
  type Agreement
} from '@/api/agreement'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { checkPermission } = usePermission()

/**
 * 「从模板发起」需要同时具备建协议权与用模板权（后端是 AND）：
 * 少了任何一个都会被拒，因此这里先把入口关掉并说明，而不是让人填完再失败。
 */
const canApplyTemplate = computed(() => checkPermission('agreement:template:apply'))

const ALL_TYPES: AgreementType[] = ['PLATFORM_SERVICE', 'DISTRIBUTION', 'GOODS_FRAMEWORK', 'CONSUMER_PROMISE']
function typeLabel(type?: string): string {
  return AGREEMENT_TYPE_META[type as AgreementType]?.label || type || '—'
}

const step = ref(0)
const creating = ref(false)

// ═══ 第一步：起点 ═══
const origin = ref<'template' | 'blank'>('blank')
const templates = ref<AgreementTemplate[]>([])
const templateLoading = ref(false)
const selectedTemplateId = ref<number | string | undefined>(undefined)
const selectedTemplateName = ref('')

// 模板预填内容的编码（用于给内容编辑器打「模板预填，仍需双方确认」标）
const templateSettingKeys = ref<string[]>([])
const templateNarrativeKeys = ref<string[]>([])

async function loadTemplates() {
  templateLoading.value = true
  try {
    const res: any = await agreementApi.templatePage({ current: 1, size: 200, status: 1 })
    const body = res?.data ?? res ?? {}
    templates.value = (body.records || body.list || []).filter((t: AgreementTemplate) => t.legalReviewStatus !== 'REJECTED')
  } catch (error: any) {
    console.warn('[发起契约] 模板列表加载失败', error)
    templates.value = []
  } finally {
    templateLoading.value = false
  }
}

async function selectTemplate(t: AgreementTemplate) {
  selectedTemplateId.value = t.id
  selectedTemplateName.value = t.templateName || ''
  // 模板登记的类型即适用类型：预选上，用户仍可改（改了会在 apply 时以用户选的为准）
  if (t.agreementType) form.agreementType = t.agreementType as AgreementType
  // 记住模板预填了哪些项，界面上要能看出来"这些是模板带来的"
  try {
    const res: any = await agreementApi.templateDetail(t.id)
    const detail: AgreementTemplateDetail = (res?.data ?? res) || {}
    templateSettingKeys.value = (detail.settings || []).map(s => s.settingKey)
    templateNarrativeKeys.value = (detail.narratives || []).map(n => n.sectionCode)
  } catch (error: any) {
    console.warn('[发起契约] 模板详情加载失败', error)
    templateSettingKeys.value = []
    templateNarrativeKeys.value = []
  }
}

// ═══ 两端信息 ═══
const form = reactive({
  agreementType: 'DISTRIBUTION' as AgreementType,
  title: '',
  partyAId: undefined as number | string | undefined,
  partyATenantId: undefined as number | string | undefined,
  partyBId: undefined as number | string | undefined,
  partyBTenantId: undefined as number | string | undefined,
  effectiveFrom: undefined as string | undefined,
  effectiveTo: undefined as string | undefined
})
const period = ref<[Dayjs, Dayjs] | null>(null)
const partyALabel = ref('')
const partyBLabel = ref('')
const pickerSide = ref<'A' | 'B' | null>(null)
const pickerVisible = computed({
  get: () => pickerSide.value !== null,
  set: (v: boolean) => { if (!v) pickerSide.value = null }
})

const tenantOptions = ref<Array<{ label: string; value: number | string }>>([])
function tenantNameOf(id?: number | string): string {
  const hit = tenantOptions.value.find(o => String(o.value) === String(id))
  return hit?.label || '—'
}

async function loadTenantOptions() {
  try {
    const res: any = await tenantApi.getPage({ pageNum: 1, pageSize: 200 })
    const body = res?.data ?? res ?? {}
    const list = body.records || body.list || []
    tenantOptions.value = list.map((t: any) => ({
      label: t.tenantName || t.tenantCode || String(t.id),
      value: t.id
    }))
  } catch {
    // 租户侧无平台租户查询权限属正常：保留空列表，不阻断录入
    tenantOptions.value = []
  }
}

function handleTypeChange(type: AgreementType) {
  form.agreementType = type
  if (type === 'PLATFORM_SERVICE') form.partyATenantId = 1
  if (type === 'CONSUMER_PROMISE') {
    form.partyBId = undefined
    form.partyBTenantId = undefined
    partyBLabel.value = ''
  }
}

function handlePartySelected(record: any) {
  const side = pickerSide.value
  const name = record?.partnerName || record?.partnerShortName || ''
  if (side === 'A') {
    form.partyAId = record?.id
    partyALabel.value = name
  } else if (side === 'B') {
    form.partyBId = record?.id
    partyBLabel.value = name
  }
  pickerSide.value = null
}

function handlePeriodChange(dates: any) {
  form.effectiveFrom = dates?.[0] ? String(dates[0].format('YYYY-MM-DD')) : undefined
  form.effectiveTo = dates?.[1] ? String(dates[1].format('YYYY-MM-DD')) : undefined
}
function formatPeriod(from?: string, to?: string): string {
  if (!from && !to) return '长期有效'
  return `${from || '—'} 至 ${to || '长期'}`
}

function validateBasic(): string | null {
  if (origin.value === 'template' && !selectedTemplateId.value) return '请选择一个模板，或改为「从零起草」'
  if (!form.title.trim()) return '请填写协议标题'
  if (!form.partyAId) return form.agreementType === 'CONSUMER_PROMISE' ? '请选择承诺方主体（甲方）' : '请选择甲方主体'
  if (!form.partyATenantId) return '请选择甲方所属租户'
  if (form.agreementType !== 'CONSUMER_PROMISE') {
    if (!form.partyBId) return '请选择乙方主体'
    if (!form.partyBTenantId) return '请选择乙方所属租户'
  }
  return null
}

// ═══ 创建（从零 / 从模板） ═══
const created = ref<Agreement | null>(null)
const draftVersionId = ref<number | string | null>(null)
const draftVersionNo = ref<number | null>(null)
const templateSourceText = ref('')
const templateChecked = ref(false)

async function loadDraft(agreementId: number | string) {
  const [dRes, vRes] = await Promise.all([
    agreementApi.getDetail(agreementId),
    agreementApi.listVersions(agreementId).catch(() => [] as any)
  ])
  created.value = (dRes?.data ?? dRes) || null
  const list: any = vRes?.data ?? vRes ?? []
  const versions: any[] = Array.isArray(list) ? list : (list?.records || [])
  const draft = versions.find(v => v.status === 'DRAFT')
  draftVersionId.value = draft?.id ?? null
  draftVersionNo.value = draft?.versionNo ?? null
  // 模板来源由后端写进版本快照，内容接口会带出来
  if (draft?.id) {
    try {
      const cRes: any = await agreementApi.getContent(draft.id)
      const c = cRes?.data ?? cRes
      templateSourceText.value = c?.templateSourceText || ''
    } catch {
      templateSourceText.value = ''
    }
  }
}

async function handleCreate() {
  const invalid = validateBasic()
  if (invalid) {
    message.warning(invalid)
    return
  }
  creating.value = true
  try {
    let agreementId: number | string | undefined
    if (origin.value === 'template' && selectedTemplateId.value) {
      const res: any = await agreementApi.applyTemplate(selectedTemplateId.value, {
        partyAId: form.partyAId as number | string,
        partyATenantId: form.partyATenantId as number | string,
        partyBId: form.agreementType === 'CONSUMER_PROMISE' ? undefined : form.partyBId,
        partyBTenantId: form.agreementType === 'CONSUMER_PROMISE' ? undefined : form.partyBTenantId,
        title: form.title.trim(),
        agreementType: form.agreementType,
        effectiveFrom: form.effectiveFrom,
        effectiveTo: form.effectiveTo
      })
      agreementId = res?.data ?? res
    } else {
      const res: any = await agreementApi.create({
        agreementType: form.agreementType,
        partyAId: form.partyAId as number | string,
        partyATenantId: form.partyATenantId as number | string,
        partyBId: form.agreementType === 'CONSUMER_PROMISE' ? undefined : form.partyBId,
        partyBTenantId: form.agreementType === 'CONSUMER_PROMISE' ? undefined : form.partyBTenantId,
        title: form.title.trim(),
        effectiveFrom: form.effectiveFrom,
        effectiveTo: form.effectiveTo
      })
      agreementId = res?.data ?? res
    }
    if (!agreementId) {
      message.warning('协议未能建立：服务没有返回协议编号，请重试')
      return
    }
    await loadDraft(agreementId)
    // 先取一次内容快照：这样一进第二步就能看出"还缺哪几项"，而不是点了下一步才被动报错
    await refreshSnapshot()
    message.success(origin.value === 'template'
      ? '协议已建立，模板内容已预填 —— 请逐项核对后确认'
      : '协议草稿已建立，请逐项约定本版内容')
    step.value = 1
  } catch (error: any) {
    console.warn('[发起契约] 建立协议失败', error)
  } finally {
    creating.value = false
  }
}

// ═══ 第二步：内容 ═══
const contentEditorRef = ref<InstanceType<typeof AgreementContentEditor> | null>(null)
const snapshot = ref<{ missing: string[]; agreed: string[]; modes: string[]; narratives: string[] }>({
  missing: [], agreed: [], modes: [], narratives: []
})

/**
 * 还缺哪几项必填 —— **优先读编辑器里的实时值**（用户改了还没保存时也拦得住），
 * 编辑器还没挂载时退回最近一次接口返回的缺项清单。
 */
const pendingMissing = computed<string[]>(() => {
  const live = (contentEditorRef.value as any)?.missingNames
  return Array.isArray(live) ? live : snapshot.value.missing
})
const canGoPreview = computed(() =>
  pendingMissing.value.length === 0 && (!templateSourceText.value || templateChecked.value)
)

function onContentSaved(result: any) {
  void result
  refreshSnapshot()
}

/** 读一次内容接口，拿到"还缺什么 / 已约定什么"，用于拦下一步与做预览 */
async function refreshSnapshot() {
  if (!draftVersionId.value) return
  try {
    const res: any = await agreementApi.getContent(draftVersionId.value)
    const c = res?.data ?? res
    if (!c) return
    const defs = c.settingDefs || []
    const items = c.settings || []
    const valueOf = (key: string) => items.find((i: any) => i.settingKey === key)
    const isAgreed = (key: string) => {
      const it = valueOf(key)
      return !!it && String(it.value ?? '').trim() !== ''
    }
    // ⚠️ 履约方式集合由专门的多选承担（集合语义落在履约方式表），不从设定行判断
    const missing = defs
      .filter((d: any) => d.required && d.settingKey !== 'FULFILLMENT_MODES')
      .filter((d: any) => !isAgreed(d.settingKey))
      .map((d: any) => d.label || d.settingKey)
    const modes = (c.fulfillmentModes || []).map((m: any) => m.mode)
    if (!modes.length) {
      const modeDef = defs.find((d: any) => d.settingKey === 'FULFILLMENT_MODES')
      missing.push(modeDef?.label || '履约方式集合')
    }
    snapshot.value = {
      missing,
      agreed: defs
        .filter((d: any) => d.settingKey !== 'FULFILLMENT_MODES' && isAgreed(d.settingKey))
        .map((d: any) => d.label || d.settingKey),
      modes,
      narratives: (c.narratives || []).map((n: any) => n.sectionTitle || n.sectionCode)
    }
    if (c.templateSourceText) templateSourceText.value = c.templateSourceText
  } catch (error: any) {
    console.warn('[发起契约] 内容快照读取失败', error)
  }
}

const agreedSettingNames = computed(() => snapshot.value.agreed)
const modeNames = computed(() =>
  snapshot.value.modes.map(m => FULFILLMENT_MODE_OPTIONS.find(o => o.value === m)?.label || m)
)
const narrativeTitles = computed(() => snapshot.value.narratives)

function goPreview() {
  if (pendingMissing.value.length) {
    message.warning(`还有必填项没有约定：${pendingMissing.value.join('、')}`)
    return
  }
  if (templateSourceText.value && !templateChecked.value) {
    message.warning('请先确认你已逐项核对模板预填的内容（模板不是默认值）')
    return
  }
  refreshSnapshot()
  step.value = 2
}

// ═══ 收尾 ═══
function goDetail() {
  const id = created.value?.id
  if (!id) {
    goList()
    return
  }
  router.push(`/agreement/detail/${id}`)
}
function goList() {
  router.push('/agreement')
}

const handleError = (error: Error) => {
  console.error('[发起契约] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(async () => {
  form.partyATenantId = userStore.tenantId || undefined
  await loadTenantOptions()
  await loadTemplates()
  const tplId = route.query.templateId
  if (tplId) {
    if (!canApplyTemplate.value) {
      // 没权限时不能默认走模板分支，否则会在提交时才被拒
      origin.value = 'blank'
      message.warning('当前账号没有「用模板发起契约」的权限，已改为从零起草')
      return
    }
    const hit = templates.value.find(t => String(t.id) === String(tplId))
    origin.value = 'template'
    if (hit) await selectTemplate(hit)
    else selectedTemplateId.value = tplId
  }
})
</script>

<style scoped>
.awz { padding: 12px 16px 24px; display: flex; flex-direction: column; gap: 12px; max-width: 1200px; }
.awz-steps { background: #fff; border: 1px solid #f0f0f0; border-radius: 6px; padding: 12px 16px; }
.awz-card :deep(.ant-card-body) { padding: 12px 16px; }
.awz-alert { margin-bottom: 10px; }
.awz-alert-top { margin-top: 12px; }
.awz-muted { font-size: 12px; color: #999; line-height: 1.6; }
.awz-empty { font-size: 13px; color: #999; line-height: 1.7; }
.awz-origin { margin-bottom: 10px; }
.awz-templates { margin-bottom: 4px; }
.awz-template-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 10px; }
.awz-template {
  border: 1px solid #d9d9d9; border-radius: 6px; padding: 8px 10px; cursor: pointer;
  background: #fff; transition: all 0.2s;
}
.awz-template:hover { border-color: #4096ff; }
.awz-template.active { border-color: #1890ff; background: #e6f7ff; box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.12); }
.awz-template-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 4px; }
.awz-template-name { font-size: 13px; font-weight: 600; color: #333; }
.awz-template-line { font-size: 12px; color: #595959; line-height: 1.6; }
.awz-template-desc { font-size: 12px; color: #8c8c8c; line-height: 1.6; margin-top: 2px; }
.awz-template-flag { font-size: 12px; color: #d46b08; margin-top: 4px; }

.awz-type-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 10px; margin-bottom: 12px; }
.awz-type { border: 1px solid #d9d9d9; border-radius: 6px; padding: 8px 10px; cursor: pointer; background: #fff; transition: all 0.2s; }
.awz-type:hover { border-color: #4096ff; }
.awz-type.active { border-color: #1890ff; background: #e6f7ff; box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.12); }
.awz-type-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 4px; }
.awz-type-name { font-size: 13px; font-weight: 600; color: #333; }
.awz-type-side { font-size: 12px; color: #1890ff; }
.awz-type-desc { font-size: 12px; color: #666; line-height: 1.6; }

.awz-form :deep(.ant-form-item) { margin-bottom: 12px; }
.awz-actions { display: flex; align-items: center; gap: 10px; margin-top: 12px; flex-wrap: wrap; }
.awz-checkbox { margin-bottom: 10px; font-size: 13px; }
.awz-block {
  font-size: 12px; color: #d4380d; background: #fff2e8; border: 1px solid #ffbb96;
  border-radius: 4px; padding: 8px 10px; line-height: 1.7; margin-bottom: 4px;
}
</style>
