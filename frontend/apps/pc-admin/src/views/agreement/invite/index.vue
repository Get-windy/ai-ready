<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      full-height
      title="打开契约邀请"
    >
      <div class="ainvopen">
        <!-- ═══ 为什么还要选"我代表谁" ═══ -->
        <a-alert
          type="info"
          show-icon
          class="ao-alert"
          message="这份邀请只发给指定的一方，请先确认你代表哪个主体"
          description="邀请绑定了「目标主体 + 目标租户 + 某一版文稿 + 有效期 + 一次性」。你需要先登录（这样系统才知道你的会话租户），再声明自己代表哪个主体；两者都对得上才放行 —— 转发给别人是打不开的。"
        />

        <!-- ═══ 打开失败：如实、完整地展示原因，不吞成「打开失败」 ═══ -->
        <a-alert
          v-if="failReason"
          type="error"
          show-icon
          class="ao-alert"
          message="这份契约没有打开"
        >
          <template #description>
            <div class="ao-fail">
              <div class="ao-fail-text">{{ failReason }}</div>
              <div class="ao-fail-hint">
                · 正常原因大致有三类：这份契约不是发给你的（未登录 / 租户不匹配 / 代表主体不匹配）、
                邀请已失效（已被撤回 / 已过期 / 已被领取过）、邀请链接或邀请码不对。
                · 如果确认应该是发给你的，请让发起方重新发起一份新的邀请。
              </div>
              <div class="ao-fail-actions">
                <a-button
                  size="small"
                  @click="retry"
                >
                  重新尝试打开
                </a-button>
                <a-button
                  size="small"
                  type="link"
                  @click="goList"
                >
                  先去协议列表看看
                </a-button>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- ═══ 打开表单 ═══ -->
        <a-card
          v-if="!opened"
          size="small"
          class="ao-card"
          title="确认身份后打开"
        >
          <a-form
            layout="vertical"
            class="ao-form"
          >
            <a-form-item label="邀请链接里的令牌（从链接/二维码带过来）">
              <a-input
                v-model:value="form.token"
                size="small"
                placeholder="链接打开时已自动填入；如果是手工粘贴，请把 token 部分贴进来"
                :maxlength="128"
              />
            </a-form-item>
            <a-form-item label="或者用邀请码（可以口头念给你）">
              <a-input
                v-model:value="form.inviteCode"
                size="small"
                class="ao-code"
                placeholder="例如：K7M2QX9FTH"
                :maxlength="32"
                @input="(e: any) => (form.inviteCode = String(e.target.value || '').toUpperCase())"
              />
              <div class="ao-muted">
                令牌与邀请码二选一即可（都已从链接带过来时不用管这一栏）。
              </div>
            </a-form-item>
            <a-form-item label="我代表哪个主体（必填）">
              <a-input
                :value="representedPartyLabel"
                readonly
                placeholder="点击右侧按钮，从往来单位里选"
              >
                <template #suffix>
                  <a-button
                    type="link"
                    size="small"
                    @click="pickerVisible = true"
                  >
                    选择主体
                  </a-button>
                </template>
              </a-input>
              <div class="ao-muted">
                必须与邀请指定的主体一致；选错会被判定为「这份契约不是发给你的」。
              </div>
            </a-form-item>
            <div class="ao-actions">
              <a-button
                v-permission="'agreement:invite:accept'"
                type="primary"
                size="small"
                :loading="opening"
                :disabled="!canSubmit"
                @click="handleOpen"
              >
                打开这份契约
              </a-button>
              <span
                v-if="!canSubmit"
                class="ao-muted"
              >
                请先填上令牌或邀请码，并选择你代表的主体。
              </span>
            </div>
          </a-form>
        </a-card>

        <!-- ═══ 打开成功 ═══ -->
        <template v-else>
          <a-card
            size="small"
            class="ao-card"
            title="邀请已打开"
          >
            <template #extra>
              <a-tag color="green">
                已领取
              </a-tag>
            </template>
            <a-descriptions
              :column="2"
              size="small"
              bordered
            >
              <a-descriptions-item label="协议">
                {{ opened.agreementNo || '—' }} {{ opened.agreementTitle ? '· ' + opened.agreementTitle : '' }}
              </a-descriptions-item>
              <a-descriptions-item label="文稿版本">
                第 {{ opened.versionNo ?? '—' }} 版
              </a-descriptions-item>
              <a-descriptions-item label="这一份发给">
                {{ opened.targetPartyName || '—' }}
              </a-descriptions-item>
              <a-descriptions-item label="目标租户">
                {{ opened.targetTenantName || '—' }}
              </a-descriptions-item>
              <a-descriptions-item label="送达渠道">
                {{ opened.channelLabel || '—' }}
              </a-descriptions-item>
              <a-descriptions-item label="有效期至">
                {{ opened.expiresAt ? String(opened.expiresAt).replace('T', ' ') : '—' }}
              </a-descriptions-item>
            </a-descriptions>

            <div
              v-if="opened.usageNote"
              class="ao-usage"
            >
              {{ opened.usageNote }}
            </div>

            <a-alert
              type="warning"
              show-icon
              class="ao-alert ao-alert-top"
              message="这是一次性邀请，已经领取过了"
              description="再打开这条链接会提示「已被领取过」；后续查看与表态请直接进入协议详情页 —— 多轮协商、签署、终止都在那里。"
            />

            <div class="ao-actions">
              <a-button
                type="primary"
                size="small"
                @click="goDetail"
              >
                进入协议详情，查看内容并表态
              </a-button>
            </div>
          </a-card>
        </template>
      </div>
    </PageContainer>

    <!-- 复用仓里的往来单位选择弹窗（不自造选择器） -->
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
 * 受邀方打开契约邀请 —— §13.5「唯一送达」
 *
 * 三条必须做对的事：
 *   ① 打开链接的人**必须先登录**（本页挂在需要登录的路由下，未登录会被守卫送到登录页，
 *      登录后回到本页）；
 *   ② 必须让打开者声明「我代表哪个主体」（representedPartyId 必填）——
 *      转发给别人打不开，正是靠「会话租户 + 所代表主体」双匹配实现的；
 *   ③ 被拒绝时要把**后端给的具体原因**如实、完整地展示出来（未登录 / 租户不匹配 /
 *      代表主体不匹配 / 已撤回 / 已过期 / 已领取），不能吞成一句「打开失败」。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import PartnerSelectModal from '@/components/PartnerSelectModal/index.vue'
import { agreementApi, type AgreementInvite } from '@/api/agreement'

const route = useRoute()
const router = useRouter()

const opening = ref(false)
const opened = ref<AgreementInvite | null>(null)
const failReason = ref('')

const form = reactive({
  token: '',
  inviteCode: '',
  representedPartyId: undefined as number | string | undefined,
  channel: 'LINK'
})

const pickerVisible = ref(false)
const representedPartyLabel = ref('')

const canSubmit = computed(() =>
  (!!form.token.trim() || !!form.inviteCode.trim()) && !!form.representedPartyId
)

function initFromQuery() {
  const token = String(route.query.token || '').trim()
  const code = String(route.query.inviteCode || route.query.code || '').trim().toUpperCase()
  if (token) {
    form.token = token
    form.channel = 'QRCODE'
  }
  if (code) {
    form.inviteCode = code
  }
  // 打开链接的用户可能还没选主体，先不自动提交 —— 让他自己确认"我代表谁"
}

function handlePartySelected(record: any) {
  form.representedPartyId = record?.id
  representedPartyLabel.value = record?.partnerName || record?.partnerShortName || ''
  pickerVisible.value = false
}

async function handleOpen() {
  if (!canSubmit.value) {
    message.warning('请先填上令牌或邀请码，并选择你代表的主体')
    return
  }
  failReason.value = ''
  opening.value = true
  try {
    const res: any = await agreementApi.openInvite({
      token: form.token.trim() || undefined,
      inviteCode: form.inviteCode.trim() || undefined,
      representedPartyId: form.representedPartyId as number | string,
      channel: form.channel
    })
    opened.value = (res?.data ?? res) || null
    message.success('邀请已领取，可以查看这一版内容并表态了')
  } catch (error: any) {
    // 后端给的是中文白话原因（未登录 / 不是发给你的 / 已撤回 / 已过期 / 已领取），原样展示
    failReason.value = error?.message || '打开失败：服务没有返回原因，请稍后重试或联系发起方。'
  } finally {
    opening.value = false
  }
}

function retry() {
  failReason.value = ''
  handleOpen()
}

function goDetail() {
  const id = opened.value?.agreementId
  if (!id) return
  const vid = opened.value?.versionId ? `?versionId=${opened.value.versionId}` : ''
  router.push(`/agreement/detail/${id}${vid}`)
}

function goList() {
  router.push('/agreement')
}

const handleError = (error: Error) => {
  console.error('[契约邀请] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(initFromQuery)
</script>

<style scoped>
.ainvopen { padding: 12px 16px 24px; display: flex; flex-direction: column; gap: 12px; max-width: 900px; }
.ao-alert { margin-bottom: 0; }
.ao-alert-top { margin-top: 12px; }
.ao-card :deep(.ant-card-body) { padding: 12px 16px; }
.ao-form :deep(.ant-form-item) { margin-bottom: 12px; }
.ao-code { font-family: Consolas, Monaco, monospace; letter-spacing: 2px; max-width: 240px; }
.ao-muted { font-size: 12px; color: #999; line-height: 1.6; }
.ao-actions { display: flex; align-items: center; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.ao-fail { line-height: 1.7; }
.ao-fail-text { font-size: 13px; color: #a8071a; font-weight: 600; }
.ao-fail-hint { font-size: 12px; color: #8c4a1f; margin-top: 6px; line-height: 1.8; }
.ao-fail-actions { margin-top: 8px; display: flex; gap: 8px; }
.ao-usage { margin-top: 10px; font-size: 12px; color: #595959; line-height: 1.7; }
</style>
