<template>
  <div class="asig">
    <a-alert
      type="info"
      show-icon
      class="asig-alert"
      message="签署要回答的是「谁签的、凭什么代表这家公司」"
      description="签署记录会留下：谁（自然人）· 代表哪个主体 · 在哪个租户 · 凭什么代表（授权依据）· 签了哪一版 · 何时 · 签章哈希。所以「代表哪个主体」与「授权依据」两项必须填。"
    />

    <div class="asig-head">
      <div class="asig-head-info">
        <span v-if="versionId">
          当前要表态的版本：<b>第 {{ versionNo ?? '—' }} 版</b>
          <a-tag
            :color="versionStatus === 'DRAFT' ? 'blue' : 'default'"
            class="asig-tag"
          >
            {{ versionStatus === 'DRAFT' ? '待双方确认' : '已不是待确认状态' }}
          </a-tag>
        </span>
        <span
          v-else
          class="asig-muted"
        >
          这一版没有可签署的版本（协议还没有草稿版本）。
        </span>
      </div>
      <a-space :size="8">
        <a-button
          size="small"
          :loading="loading"
          @click="reload"
        >
          <ReloadOutlined /> 刷新
        </a-button>
        <a-button
          v-permission="'agreement:sign'"
          type="primary"
          size="small"
          :disabled="!canSign"
          @click="openSign"
        >
          <EditOutlined /> 签署本版
        </a-button>
      </a-space>
    </div>

    <div
      v-if="!canSign && signBlockReason"
      class="asig-block"
    >
      {{ signBlockReason }}
    </div>

    <!-- ═══ 签署记录（七要素） ═══ -->
    <a-table
      :columns="columns"
      :data-source="signatures"
      :pagination="false"
      :loading="loading"
      :expanded-row-keys="expandedKeys"
      row-key="id"
      size="small"
      bordered
      class="asig-table"
      @expand="handleExpand"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'signer'">
          <div>{{ record.signerName || ('用户 ' + (record.signerUserId ?? '—')) }}</div>
          <div class="asig-muted">
            自然人：{{ record.signerPersonId || '未登记' }}
          </div>
        </template>
        <template v-else-if="column.key === 'party'">
          <div>{{ record.partyName || '—' }}</div>
          <div class="asig-muted">
            {{ record.partySideLabel || PARTY_SIDE_TEXT[record.partySide || ''] || '—' }}
          </div>
        </template>
        <template v-else-if="column.key === 'tenant'">
          {{ record.signerPartyTenantName || record.signerPartyTenantId || '—' }}
        </template>
        <template v-else-if="column.key === 'authority'">
          <div>{{ record.authorityBasis || '—' }}</div>
          <div
            v-if="record.authorityEvidenceNo"
            class="asig-muted"
          >
            凭证号：{{ record.authorityEvidenceNo }}
          </div>
        </template>
        <template v-else-if="column.key === 'version'">
          第 {{ record.versionNo ?? '—' }} 版
        </template>
        <template v-else-if="column.key === 'signedAt'">
          {{ record.signedAt ? String(record.signedAt).replace('T', ' ') : '—' }}
        </template>
        <template v-else-if="column.key === 'effective'">
          <a-tag :color="record.effective === false ? 'red' : 'green'">
            {{ record.effective === false ? '已失效（内容被改过）' : '当前有效' }}
          </a-tag>
          <div
            v-if="record.effectiveNote"
            class="asig-muted"
          >
            {{ record.effectiveNote }}
          </div>
        </template>
        <template v-else-if="column.key === 'hash'">
          <a-button
            type="link"
            size="small"
            @click="toggleExpand(record)"
          >
            {{ isExpanded(record) ? '收起签章哈希' : '查看签章哈希' }}
          </a-button>
        </template>
      </template>
      <template #expandedRowRender="{ record }">
        <div class="asig-hash">
          <div class="asig-hash-label">签章哈希（SHA-256，证明这一版的确认痕迹没被改过）：</div>
          <div class="asig-hash-value">{{ record.signHash || '（无）' }}</div>
        </div>
      </template>
    </a-table>

    <div
      v-if="!signatures.length && !loading"
      class="asig-empty"
    >
      这一版还没有签署记录。双方都签署后，版本才允许置为生效 —— 缺一不可。
    </div>

    <!-- ═══ 签署弹窗（两步：填授权 → 意愿确认） ═══ -->
    <a-modal
      v-model:open="signVisible"
      :title="signStep === 'form' ? '签署本版' : '确认签署意愿'"
      :width="640"
      :mask-closable="false"
      :confirm-loading="submitting"
      :ok-text="signStep === 'form' ? '下一步：确认意愿' : '确认签署'"
      :ok-button-props="{ disabled: signStep === 'form' ? !formValid : !willingnessChecked }"
      cancel-text="取消"
      @ok="handleOk"
      @cancel="closeSign"
    >
      <!-- 第一步：谁签的、凭什么代表 -->
      <template v-if="signStep === 'form'">
        <a-alert
          type="warning"
          show-icon
          class="asig-alert"
          message="请确认你代表哪一个主体"
          description="签错主体等于替别人签了字 —— 这份记录将来会被用来回答「谁签的、凭什么代表这家公司」，选错主体无法悄悄改掉。"
        />
        <a-form
          layout="vertical"
          class="asig-form"
        >
          <a-form-item label="我代表哪个主体签署">
            <a-select
              v-model:value="form.representedPartyId"
              size="small"
              placeholder="请选择"
              :options="partyOptions"
            />
            <div class="asig-muted">
              只有这份协议的当事主体才签得下；系统会再校验一次你所在的租户是否有权代表它。
            </div>
          </a-form-item>
          <a-form-item label="凭什么代表这家公司（授权依据）">
            <a-auto-complete
              v-model:value="form.authorityBasis"
              size="small"
              :options="authorityOptions"
              placeholder="例如：法定代表人本人"
              :filter-option="filterAuthority"
            />
            <div class="asig-muted">
              必填。这一条是司法上「授权从哪来」的答案，请写具体（见下方常用示例，可直接填写后修改）。
            </div>
            <div class="asig-samples">
              <a-tag
                v-for="s in AUTHORITY_SAMPLES"
                :key="s"
                class="asig-sample"
                @click="form.authorityBasis = s"
              >
                {{ s }}
              </a-tag>
            </div>
          </a-form-item>
          <a-form-item label="授权凭证号（选填）">
            <a-input
              v-model:value="form.authorityEvidenceNo"
              size="small"
              placeholder="如：授权委托书编号 / 董事会决议编号"
              :maxlength="64"
            />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="签署人姓名（留痕，人读）">
                <a-input
                  v-model:value="form.signerName"
                  size="small"
                  placeholder="请填写真实姓名"
                  :maxlength="32"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="签署渠道">
                <a-input
                  v-model:value="form.channel"
                  size="small"
                  disabled
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="备注（选填）">
            <a-textarea
              v-model:value="form.remark"
              :rows="2"
              :maxlength="200"
              show-count
              placeholder="例如：经 2026 年第 3 次董事会授权签署"
            />
          </a-form-item>
        </a-form>
      </template>

      <!-- 第二步：意愿确认（把后果写到明面上） -->
      <template v-else>
        <a-alert
          type="error"
          show-icon
          class="asig-alert"
          message="签署即对该版本产生约束，请确认你已看清以下后果"
        />
        <ul class="asig-consequence">
          <li>签署后，本条记录即成为<b>你代表该主体对第 {{ versionNo ?? '—' }} 版的确认</b>，并与这一版的快照绑定。</li>
          <li>双方都签署后，这一版才可以置为生效；生效后<b>内容不可再改</b>，要改只能发起变更、重新签署。</li>
          <li>签署是不可撤销的留痕：记录会带上签章哈希，事后修改内容会让已签记录失效并被标出。</li>
          <li>本版内容包含「文字条款」时，系统<b>不会自动执行</b>这些文字条款，出事时需人工主张或走司法解决。</li>
        </ul>
        <a-descriptions
          :column="1"
          size="small"
          bordered
          class="asig-desc"
        >
          <a-descriptions-item label="代表主体">
            {{ selectedPartyLabel }}
          </a-descriptions-item>
          <a-descriptions-item label="授权依据">
            {{ form.authorityBasis || '—' }}
            <span v-if="form.authorityEvidenceNo">（凭证号：{{ form.authorityEvidenceNo }}）</span>
          </a-descriptions-item>
          <a-descriptions-item label="签署人">
            {{ form.signerName || '—' }}
          </a-descriptions-item>
          <a-descriptions-item label="签署版本">
            第 {{ versionNo ?? '—' }} 版
          </a-descriptions-item>
        </a-descriptions>
        <a-checkbox v-model:checked="willingnessChecked">
          我已看清上述后果，确认代表所填主体签署这一版，并愿意受其约束。
        </a-checkbox>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
/**
 * 签署面板 —— §13.6「自然人代表主体」
 *
 * 三条必须做对的事：
 *   ① 必须收集「凭什么代表这家公司」（authorityBasis 必填、authorityEvidenceNo 可选）——
 *      只记一个账号 ID 回答不了司法上的"谁签的、凭什么代表这家公司"；
 *   ② 签署前给**意愿确认**，并把后果写到明面上（签署即对版本产生约束、生效后不可改、
 *      文字条款系统不会自动执行）；
 *   ③ 已签署记录要显示七要素，签章哈希可折叠查看。
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, EditOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  agreementApi,
  PARTY_SIDE_TEXT,
  type AgreementSignature
} from '@/api/agreement'

const props = defineProps<{
  agreementId: number | string
  /** 要签署/查看的版本 */
  versionId?: number | string | null
  versionNo?: number | null
  versionStatus?: string
  /** 缔约两端（用于"我代表哪个主体"） */
  partyA?: { id?: number | string; name?: string; tenantId?: number | string; tenantName?: string }
  partyB?: { id?: number | string; name?: string; tenantId?: number | string; tenantName?: string }
  agreementType?: string
  /** 当前登录租户：用于判断"我是哪一方"与默认选中 */
  myTenantId?: number | string | null
  /** 已签署的甲方/乙方（双方都签了就不能再签） */
  signedSides?: string[]
}>()

/** 常用授权依据示例（点了就填进输入框，仍可修改） */
const AUTHORITY_SAMPLES = [
  '法定代表人本人',
  '授权委托书（含授权范围与限额）',
  '董事会 / 股东会决议授权',
  '公司盖章并由经办人办理',
  '平台入驻协议授权的系统操作人'
]
const authorityOptions = AUTHORITY_SAMPLES.map(s => ({ value: s }))
function filterAuthority(input: string, option: any) {
  return String(option?.value || '').includes(input)
}

const userStore = useUserStore()
const loading = ref(false)
const signatures = ref<AgreementSignature[]>([])
const expandedKeys = ref<any[]>([])

const columns = [
  { title: '谁签的', key: 'signer', width: 170 },
  { title: '代表哪个主体', key: 'party', width: 180 },
  { title: '在哪个租户', key: 'tenant', width: 150 },
  { title: '授权依据', key: 'authority', width: 220 },
  { title: '签了哪一版', key: 'version', width: 110 },
  { title: '何时签署', key: 'signedAt', width: 170 },
  { title: '是否有效', key: 'effective', width: 170 },
  { title: '签章哈希', key: 'hash', width: 140 }
]

// ── 我是哪一方（只用于给默认值与提示，最终以服务端判定为准） ──
const myTenant = computed(() => String(props.myTenantId ?? userStore.tenantId ?? ''))
const mySide = computed<'A' | 'B' | null>(() => {
  if (!myTenant.value) return null
  if (props.partyA?.tenantId && String(props.partyA.tenantId) === myTenant.value) return 'A'
  if (props.partyB?.tenantId && String(props.partyB.tenantId) === myTenant.value) return 'B'
  return null
})

const partyOptions = computed(() => {
  const out: Array<{ label: string; value: number | string }> = []
  if (props.partyA?.id) {
    out.push({
      label: `${props.partyA.name || '（未命名主体）'} · 甲方${props.partyA.tenantName ? '（' + props.partyA.tenantName + '）' : ''}`,
      value: props.partyA.id
    })
  }
  if (props.partyB?.id) {
    out.push({
      label: `${props.partyB.name || '（未命名主体）'} · 乙方${props.partyB.tenantName ? '（' + props.partyB.tenantName + '）' : ''}`,
      value: props.partyB.id
    })
  }
  return out
})

const canSign = computed(() => {
  if (!props.versionId) return false
  if (props.versionStatus !== 'DRAFT') return false
  if (!mySide.value) return false
  // 双方痕迹分开存：本方已经签过这一版就没必要再签
  return !(props.signedSides || []).includes(mySide.value)
})

const signBlockReason = computed(() => {
  if (!props.versionId) return '这一版不可签署：协议还没有草稿版本。'
  if (props.versionStatus !== 'DRAFT') return '这一版不可签署：只有「待双方确认」的版本才能签署，请先发起变更生成新草稿。'
  if (!mySide.value) return '当前登录租户不是这份协议的任一方，只能查看，不能代为签署。'
  if ((props.signedSides || []).includes(mySide.value)) return '本方已经签署过这一版了；另一方签署后即可置为生效。'
  return ''
})

// ── 签署弹窗 ──
const signVisible = ref(false)
const signStep = ref<'form' | 'confirm'>('form')
const submitting = ref(false)
const willingnessChecked = ref(false)
const form = reactive({
  representedPartyId: undefined as number | string | undefined,
  authorityBasis: '',
  authorityEvidenceNo: '',
  signerName: '',
  channel: 'PC',
  remark: ''
})

const selectedPartyLabel = computed(() => {
  const hit = partyOptions.value.find(o => String(o.value) === String(form.representedPartyId))
  return hit?.label || '未选择'
})

const formValid = computed(() =>
  !!form.representedPartyId && !!form.authorityBasis.trim() && !!form.signerName.trim()
)

function openSign() {
  if (!canSign.value) {
    message.warning(signBlockReason.value || '当前不能签署这一版')
    return
  }
  form.representedPartyId = mySide.value === 'A' ? props.partyA?.id : props.partyB?.id
  form.authorityBasis = ''
  form.authorityEvidenceNo = ''
  form.signerName = (userStore.userInfo as any)?.realName
    || (userStore.userInfo as any)?.nickname
    || (userStore.userInfo as any)?.username
    || ''
  form.channel = 'PC'
  form.remark = ''
  willingnessChecked.value = false
  signStep.value = 'form'
  signVisible.value = true
}

function closeSign() {
  signVisible.value = false
  signStep.value = 'form'
  willingnessChecked.value = false
}

function handleOk() {
  if (signStep.value === 'form') {
    if (!formValid.value) {
      message.warning('请填写「代表哪个主体」「授权依据」与「签署人姓名」')
      return
    }
    signStep.value = 'confirm'
    return
  }
  if (!willingnessChecked.value) {
    message.warning('请先勾选意愿确认')
    return
  }
  submitSign()
}

async function submitSign() {
  if (!props.versionId) return
  submitting.value = true
  try {
    await agreementApi.sign(props.versionId, {
      representedPartyId: form.representedPartyId as number | string,
      authorityBasis: form.authorityBasis.trim(),
      authorityEvidenceNo: form.authorityEvidenceNo.trim() || undefined,
      signerName: form.signerName.trim() || undefined,
      channel: form.channel,
      remark: form.remark.trim() || undefined
    })
    message.success('已签署，并完成了本方确认；等对方签署后即可置为生效')
    closeSign()
    await reload()
  } catch (error: any) {
    console.warn('[协议签署] 签署失败', error)
  } finally {
    submitting.value = false
  }
}

// ── 签署记录 ──
// ⚠️ expandedRowKeys 里放**行 key 的原值**（与 row-key 同类型）：
// 表格内部用 row-key 取值后与这个数组比对，放字符串化的 ID 在 ID 为数字时会永远对不上。
function toggleExpand(record: AgreementSignature) {
  const key: any = record.id
  const has = expandedKeys.value.some(k => String(k) === String(key))
  expandedKeys.value = has
    ? expandedKeys.value.filter(k => String(k) !== String(key))
    : [...expandedKeys.value, key]
}
function handleExpand(expanded: boolean, record: AgreementSignature) {
  const key: any = record.id
  const has = expandedKeys.value.some(k => String(k) === String(key))
  if (expanded && !has) expandedKeys.value = [...expandedKeys.value, key]
  if (!expanded) expandedKeys.value = expandedKeys.value.filter(k => String(k) !== String(key))
}
function isExpanded(record: AgreementSignature): boolean {
  return expandedKeys.value.some(k => String(k) === String(record.id))
}

async function reload() {
  if (!props.versionId) {
    signatures.value = []
    return
  }
  loading.value = true
  try {
    const res: any = await agreementApi.listSignatures(props.versionId)
    const list: any = res?.data ?? res ?? []
    signatures.value = Array.isArray(list) ? list : (list?.records || [])
  } catch (error: any) {
    console.warn('[协议签署] 签署记录加载失败', error)
    signatures.value = []
  } finally {
    loading.value = false
  }
}

onMounted(reload)
watch(() => props.versionId, reload)
</script>

<style scoped>
.asig-alert { margin-bottom: 10px; }
.asig-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; flex-wrap: wrap; }
.asig-head-info { font-size: 13px; color: #333; }
.asig-tag { margin-left: 6px; }
.asig-muted { font-size: 12px; color: #999; line-height: 1.6; }
.asig-block {
  font-size: 12px; color: #d46b08; background: #fff7e6; border: 1px solid #ffd591;
  border-radius: 4px; padding: 6px 8px; margin-bottom: 10px; line-height: 1.6;
}
.asig-table { margin-bottom: 8px; }
.asig-empty { font-size: 13px; color: #999; line-height: 1.7; }
.asig-hash { padding: 4px 8px; }
.asig-hash-label { font-size: 12px; color: #666; margin-bottom: 4px; }
.asig-hash-value {
  font-family: Consolas, Monaco, monospace; font-size: 12px; color: #333;
  word-break: break-all; background: #fafafa; border-radius: 4px; padding: 6px 8px;
}
.asig-form :deep(.ant-form-item) { margin-bottom: 12px; }
.asig-samples { margin-top: 6px; display: flex; flex-wrap: wrap; gap: 6px; }
.asig-sample { cursor: pointer; }
.asig-consequence { margin: 0 0 12px; padding-left: 20px; font-size: 13px; color: #333; line-height: 1.9; }
.asig-desc { margin-bottom: 12px; }
</style>
