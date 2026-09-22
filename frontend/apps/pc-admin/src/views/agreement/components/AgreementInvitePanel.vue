<template>
  <div class="ainv">
    <!-- ═══ 说明：唯一送达是什么意思 ═══ -->
    <a-alert
      type="info"
      show-icon
      class="ainv-alert"
      message="唯一送达：这份契约只会送到指定的那一方手里"
      description="邀请链接与邀请码绑定「目标主体 + 目标租户 + 这一版文稿 + 有效期 + 一次性」。对方必须先登录，且会话租户与所代表主体都对得上才能打开 —— 转发给别人是打不开的。"
    />

    <div class="ainv-toolbar">
      <a-button
        v-permission="'agreement:invite:create'"
        type="primary"
        size="small"
        :disabled="!canCreate"
        @click="openCreate"
      >
        <SendOutlined /> 发起唯一送达
      </a-button>
      <a-button
        size="small"
        :loading="loading"
        @click="reload"
      >
        <ReloadOutlined /> 刷新
      </a-button>
      <span
        v-if="!canCreate"
        class="ainv-hint"
      >
        {{ createBlockReason }}
      </span>
    </div>

    <!-- ═══ 刚下发的邀请：明文 token 只此一次 ═══ -->
    <a-card
      v-if="issued"
      size="small"
      class="ainv-card"
      title="邀请已发出 —— 请立即复制，之后无法再次查看"
    >
      <template #extra>
        <a-button
          type="text"
          size="small"
          @click="issued = null"
        >
          收起
        </a-button>
      </template>

      <a-alert
        type="warning"
        show-icon
        class="ainv-alert"
        message="短链与邀请码只在这里显示这一次"
        description="出于安全，系统只保存它们的摘要：关闭后任何页面都查不回明文，只能重新发起一份新的邀请。请现在复制并通过可靠渠道发给对方。"
      />

      <a-descriptions
        :column="1"
        size="small"
        bordered
      >
        <a-descriptions-item label="发给谁">
          {{ issued.targetPartyName || '—' }}
          <span class="ainv-muted">（{{ issued.targetSideLabel || '' }}
            {{ issued.targetTenantName ? ' · ' + issued.targetTenantName : '' }}）</span>
        </a-descriptions-item>
        <a-descriptions-item label="对应文稿">
          第 {{ issued.versionNo ?? '—' }} 版
        </a-descriptions-item>
        <a-descriptions-item label="有效期至">
          {{ issued.expiresAt ? String(issued.expiresAt).replace('T', ' ') : '—' }}
        </a-descriptions-item>
        <a-descriptions-item label="邀请链接">
          <div class="ainv-copy">
            <a-input
              :value="fullLink"
              size="small"
              readonly
              class="ainv-copy-input"
            />
            <a-button
              size="small"
              type="primary"
              @click="copy(fullLink, '邀请链接')"
            >
              复制链接
            </a-button>
          </div>
        </a-descriptions-item>
        <a-descriptions-item label="邀请码">
          <div class="ainv-copy">
            <a-input
              :value="issued.inviteCode || ''"
              size="small"
              readonly
              class="ainv-copy-input ainv-code"
            />
            <a-button
              size="small"
              @click="copy(issued.inviteCode || '', '邀请码')"
            >
              复制邀请码
            </a-button>
          </div>
          <div class="ainv-muted">
            邀请码可以口述或手工输入（已去掉 0/O、1/I 这类念出来会听错的字符）。
          </div>
        </a-descriptions-item>
      </a-descriptions>

      <div class="ainv-qr-note">
        二维码：本仓前端没有引入二维码生成库，因此这里只给出短链与邀请码。
        如需二维码，请把上面的链接贴到你们惯用的二维码工具里生成，或在后端补二维码依赖后再启用本页渲染。
      </div>

      <div
        v-if="issued.usageNote"
        class="ainv-usage"
      >
        {{ issued.usageNote }}
      </div>
    </a-card>

    <!-- ═══ 邀请记录 ═══ -->
    <a-table
      :columns="columns"
      :data-source="invites"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="small"
      bordered
      class="ainv-table"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'target'">
          <div>{{ record.targetPartyName || '—' }}</div>
          <div class="ainv-muted">
            {{ record.targetSideLabel || '' }}
            {{ record.targetTenantName ? ' · ' + record.targetTenantName : '' }}
          </div>
        </template>
        <template v-else-if="column.key === 'version'">
          第 {{ record.versionNo ?? '—' }} 版
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="INVITE_STATUS_COLOR[record.status ?? -1] || 'default'">
            {{ record.statusLabel || INVITE_STATUS_TEXT[record.status ?? -1] || '未知' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'view'">
          <div>浏览次数：{{ record.viewCount ?? 0 }}</div>
          <div class="ainv-muted">
            首次查看：{{ record.firstViewedAt ? String(record.firstViewedAt).replace('T', ' ') : '尚未查看' }}
          </div>
        </template>
        <template v-else-if="column.key === 'accepted'">
          <div v-if="record.acceptedAt">
            <a-tag color="green">
              已被领取
            </a-tag>
            <div class="ainv-muted">
              {{ String(record.acceptedAt).replace('T', ' ') }}
              {{ record.acceptChannel === 'QRCODE' ? ' · 扫码打开' : ' · 链接打开' }}
            </div>
          </div>
          <span
            v-else
            class="ainv-muted"
          >尚未领取</span>
        </template>
        <template v-else-if="column.key === 'expiresAt'">
          {{ record.expiresAt ? String(record.expiresAt).replace('T', ' ') : '—' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-tooltip
            v-if="canRevoke(record)"
            title="撤回后链接立即失效（与「已过期」「已领取」是三种不同的失效）"
            placement="bottom"
          >
            <a-button
              v-permission="'agreement:invite:revoke'"
              type="link"
              size="small"
              danger
              @click="handleRevoke(record)"
            >
              撤回
            </a-button>
          </a-tooltip>
          <span
            v-else
            class="ainv-muted"
          >
            {{ revokeBlockReason(record) }}
          </span>
        </template>
      </template>
    </a-table>

    <!-- ═══ 发起送达弹窗 ═══ -->
    <a-modal
      v-model:open="createVisible"
      title="发起唯一送达"
      :width="560"
      :confirm-loading="creating"
      ok-text="生成邀请"
      cancel-text="取消"
      @ok="handleCreate"
    >
      <a-alert
        type="info"
        show-icon
        class="ainv-alert"
        message="收件方由系统判定为「另一端」，不需要也不允许你来指定"
        description="能指定发给谁，就等于把「这份契约不是发给你的」这条判定交给了客户端，唯一送达就形同虚设。"
      />
      <a-form
        layout="vertical"
        class="ainv-form"
      >
        <a-form-item label="送给哪一版文稿">
          <a-select
            v-model:value="createForm.versionId"
            size="small"
            allow-clear
            placeholder="默认：当前待对方确认的草稿版本"
            :options="versionOptions"
          />
          <div class="ainv-muted">
            送出去的是这一版的快照；对方确认或提出修改后，再来一版就是新一轮协商。
          </div>
        </a-form-item>
        <a-form-item label="送达渠道（只用于留痕）">
          <a-radio-group v-model:value="createForm.channel">
            <a-radio value="LINK">
              链接
            </a-radio>
            <a-radio value="QRCODE">
              二维码
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="有效期">
          <a-select
            v-model:value="createForm.expiresInHours"
            size="small"
            :options="expiresOptions"
          />
          <div class="ainv-muted">
            到点即失效，对方需要你重新发起一份（1 ~ 720 小时）。
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 撤回邀请 ═══ -->
    <a-modal
      v-model:open="revokeVisible"
      title="撤回这份邀请"
      :width="480"
      :confirm-loading="revoking"
      ok-text="确认撤回"
      ok-type="danger"
      cancel-text="取消"
      @ok="submitRevoke"
    >
      <a-alert
        type="warning"
        show-icon
        class="ainv-alert"
        message="撤回后链接立即失效"
        description="对方再打开会看到「这份邀请已被发起方撤回」，这与「已过期」「已领取」是三种不同的失效，各自可查。"
      />
      <a-textarea
        v-model:value="revokeReason"
        :rows="3"
        :maxlength="200"
        show-count
        placeholder="撤回原因（选填，会留痕）"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
/**
 * 唯一送达（邀请）面板 —— §13.5
 *
 * 三条必须做对的事：
 *   ① 明文 token / 短链**只在创建那一次**由后端返回，之后查不回 ——
 *      界面必须提示"请立即复制，之后无法再次查看"，不能假装还能再查；
 *   ② 邀请列表要显示"发出去了、对方看没看、领没领"（浏览次数 / 首次查看时间 / 领取留痕），
 *      而不是只显示一个状态码；
 *   ③ 二维码：本仓前端**没有**二维码生成依赖（实测 package.json 与 pnpm-lock 均无），
 *      因此这里只展示短链与邀请码，不为生成图片新装依赖。
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { SendOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import {
  agreementApi,
  INVITE_STATUS_TEXT,
  INVITE_STATUS_COLOR,
  type AgreementInvite,
  type AgreementVersion
} from '@/api/agreement'

const props = defineProps<{
  agreementId: number | string
  /** 主档状态：已终止的协议不能再发起送达 */
  agreementStatus?: string
  /** 可送达的版本（草稿优先，用于下拉选择） */
  versions?: AgreementVersion[]
}>()

const loading = ref(false)
const invites = ref<AgreementInvite[]>([])
/** 刚下发的那一份（含明文 token / 短链 / 邀请码，仅内存中短暂保留） */
const issued = ref<AgreementInvite | null>(null)

const columns = [
  { title: '发给谁', key: 'target', width: 200 },
  { title: '文稿', key: 'version', width: 90 },
  { title: '状态', key: 'status', width: 150 },
  { title: '查看情况', key: 'view', width: 230 },
  { title: '领取情况', key: 'accepted', width: 190 },
  { title: '有效期至', key: 'expiresAt', width: 170 },
  { title: '操作', key: 'action', width: 160 }
]

// ── 能否发起 ──
const TERMINATED = 'TERMINATED'
const canCreate = computed(() => props.agreementStatus !== TERMINATED)
const createBlockReason = computed(() => (canCreate.value
  ? ''
  : '这份协议已终止，不能再发起送达；如需继续合作请重新签订一份协议。'))

/** 短链是站内相对路径（/agreement/invite?token=xxx），拼上站点域名才是能发出去的完整链接 */
const fullLink = computed(() => {
  const link = issued.value?.shortLink || ''
  if (!link) return ''
  if (/^https?:\/\//i.test(link)) return link
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return origin ? origin + link : link
})

// ── 复制 ──
async function copy(text: string, what: string) {
  if (!text) {
    message.warning(`没有可复制的${what}`)
    return
  }
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      const el = document.createElement('textarea')
      el.value = text
      el.style.position = 'fixed'
      el.style.opacity = '0'
      document.body.appendChild(el)
      el.select()
      document.execCommand('copy')
      document.body.removeChild(el)
    }
    message.success(`${what}已复制`)
  } catch (error: any) {
    console.warn('[唯一送达] 复制失败', error)
    message.warning(`复制失败，请手动选中${what}复制`)
  }
}

// ── 邀请记录 ──
function canRevoke(record: AgreementInvite): boolean {
  return record.status === 0
}
function revokeBlockReason(record: AgreementInvite): string {
  if (record.status === 1) return '已被领取，不能再撤回'
  if (record.status === 2) return '已撤回'
  if (record.status === 3) return '已过期，无需撤回'
  return ''
}

async function reload() {
  if (!props.agreementId) return
  loading.value = true
  try {
    const res: any = await agreementApi.listInvites(props.agreementId)
    const list: any = res?.data ?? res ?? []
    invites.value = Array.isArray(list) ? list : (list?.records || [])
  } catch (error: any) {
    console.warn('[唯一送达] 邀请记录加载失败', error)
    invites.value = []
  } finally {
    loading.value = false
  }
}

// ── 发起送达 ──
const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive<{ versionId?: number | string; channel: string; expiresInHours: number }>({
  versionId: undefined,
  channel: 'LINK',
  expiresInHours: 168
})

const expiresOptions = [
  { label: '1 天', value: 24 },
  { label: '3 天', value: 72 },
  { label: '7 天（默认）', value: 168 },
  { label: '30 天', value: 720 }
]

/** 可送达的版本：草稿优先（唯一送达送的通常是待对方确认的那一版） */
const versionOptions = computed(() => {
  const list = props.versions || []
  const draft = list.filter(v => v.status === 'DRAFT')
  const others = list.filter(v => v.status !== 'DRAFT')
  return [...draft, ...others].map(v => ({
    label: `第 ${v.versionNo} 版 · ${v.status === 'DRAFT' ? '待双方确认' : v.status === 'ACTIVE' ? '生效中' : '历史版本'}`,
    value: v.id
  }))
})

function openCreate() {
  if (!canCreate.value) {
    message.warning(createBlockReason.value)
    return
  }
  createForm.versionId = undefined
  createForm.channel = 'LINK'
  createForm.expiresInHours = 168
  createVisible.value = true
}

async function handleCreate() {
  creating.value = true
  try {
    const res: any = await agreementApi.createInvite(props.agreementId, {
      versionId: createForm.versionId,
      channel: createForm.channel,
      expiresInHours: createForm.expiresInHours
    })
    const vo: AgreementInvite = res?.data ?? res
    issued.value = vo
    createVisible.value = false
    message.success('邀请已生成，请立即复制链接或邀请码')
    await reload()
  } catch (error: any) {
    console.warn('[唯一送达] 发起失败', error)
  } finally {
    creating.value = false
  }
}

// ── 撤回（单独弹窗收原因，撤回原因会留痕） ──
const revokeVisible = ref(false)
const revoking = ref(false)
const revokeTarget = ref<AgreementInvite | null>(null)
const revokeReason = ref('')

function handleRevoke(record: AgreementInvite) {
  revokeTarget.value = record
  revokeReason.value = ''
  revokeVisible.value = true
}

async function submitRevoke() {
  if (!revokeTarget.value) return
  revoking.value = true
  try {
    await agreementApi.revokeInvite(revokeTarget.value.id, revokeReason.value.trim() || undefined)
    message.success('已撤回，链接立即失效')
    revokeVisible.value = false
    await reload()
  } catch (error: any) {
    console.warn('[唯一送达] 撤回失败', error)
  } finally {
    revoking.value = false
  }
}

onMounted(reload)
watch(() => props.agreementId, reload)
</script>

<style scoped>
.ainv-alert { margin-bottom: 10px; }
.ainv-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; flex-wrap: wrap; }
.ainv-hint { font-size: 12px; color: #d46b08; }
.ainv-card { margin-bottom: 12px; }
.ainv-table { margin-top: 4px; }
.ainv-muted { font-size: 12px; color: #999; line-height: 1.6; }
.ainv-copy { display: flex; align-items: center; gap: 8px; }
.ainv-copy-input { width: 380px; max-width: 100%; }
.ainv-code { font-family: Consolas, Monaco, monospace; letter-spacing: 2px; width: 220px; }
.ainv-qr-note {
  margin-top: 10px; font-size: 12px; color: #8c4a1f; line-height: 1.7;
  background: #fff7e6; border: 1px solid #ffd591; border-radius: 4px; padding: 6px 8px;
}
.ainv-usage { margin-top: 8px; font-size: 12px; color: #595959; line-height: 1.7; }
.ainv-form :deep(.ant-form-item) { margin-bottom: 12px; }
</style>
