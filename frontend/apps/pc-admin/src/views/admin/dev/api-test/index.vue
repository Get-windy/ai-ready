<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        API测试（开发工具 → API测试，菜单 62404）
        · 工具页形态（请求配置 + 响应结果），不适用列表骨架（CategoryListLayout / BillDetailTable）
        · 2026-09-19 起：**页面不再由浏览器直发请求**，改为调用后端受控端点
          POST /api/dev/api-test/send（服务端做 allowlist + 屏蔽内网网段 + 凭据剥离 + 调用审计 + 限流）。
          历史形态是「前端 fetch 直发 + 仅前端校验」—— 开发者工具即可绕过，故本轮改为服务端兜底。
        · 安全边界的事实来源是服务端 GET /api/dev/api-test/policy（页面如实展示，不再硬编码文案）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/API测试开发文档.md
      -->
      <div class="api-test-page">
        <!-- ═══ 安全边界说明（内容来自服务端 policy 接口，不是前端自述） ═══ -->
        <a-alert
          class="security-alert"
          :type="policyLoaded ? 'success' : 'info'"
          show-icon
          :message="policyMessage"
        >
          <template #description>
            <ul class="security-list">
              <li>
                所有请求由后端 <code>POST /api/dev/api-test/send</code> 代发（权限码
                <code>system:dev:api-test:send</code>，每次调用写 <code>sys_oper_log</code> 审计）。
                <b>浏览器直发通道已移除</b> —— 前端校验不再是唯一防线。
              </li>
              <li>
                服务端闸门：协议白名单（仅 http/https，禁 file:/ftp:/gopher:/jar: 等）→
                目标 allowlist（默认只放行本系统自身同源；外部地址须由服务端
                <code>app.api-test.allowed-hosts</code> 配置，<b>不接受前端传入</b>）→
                屏蔽内网与本机网段（<code>127/8</code>、<code>10/8</code>、<code>172.16/12</code>、
                <code>192.168/16</code>、<code>169.254/16</code> 含云元数据 <code>169.254.169.254</code>、
                <code>::1</code>、<code>fc00::/7</code>、<code>fe80::/10</code>、<code>0.0.0.0</code>）→
                <b>按已校验的 IP 发起连接</b>（防 DNS Rebinding）→ 禁跟随重定向 → {{ timeoutText }}
                → 响应体 {{ maxResponseText }} 流式截断 → 限流 {{ rateLimitText }}。
              </li>
              <li>
                <b>平台凭据已剥离</b>：本系统绝不把调用方的 <code>Authorization</code>/<code>Sa-Token</code>/
                <code>Cookie</code>/<code>X-Tenant-Id</code> 等会话凭据转发给目标；只有你在下方
                <b>显式填写</b>的请求头才会被转发，且 <code>Host</code>/<code>Content-Length</code>/
                <code>Connection</code>/<code>Transfer-Encoding</code> 等逐跳头一律丢弃。
                因此调用<b>需鉴权的接口会返回 401 —— 这是本系统安全设计的预期结果</b>，不是故障。
              </li>
              <li v-if="policyLoaded">
                当前服务端生效值：self-base-url =
                <code>{{ policy.selfBaseUrl || '（未配置）' }}</code>；外部地址
                <b>{{ policy.allowExternal ? '已放行（受白名单限制）' : '未放行' }}</b>；自带 Authorization
                <b>{{ policy.allowUserAuthorization ? '允许' : '禁止' }}</b>；跟随重定向
                <b>禁止</b>；按已校验 IP 连接 <b>已启用</b>。
              </li>
              <li v-else>
                正在读取服务端安全边界…（读取失败时以服务端返回的拒绝原因为准）
              </li>
            </ul>
          </template>
        </a-alert>

        <a-row :gutter="16">
          <!-- ═══ 左栏：请求配置 ═══ -->
          <a-col
            :xs="24"
            :lg="10"
          >
            <a-card
              :bordered="false"
              title="请求配置"
            >
              <a-form layout="vertical">
                <a-form-item label="请求方法">
                  <a-select
                    v-model:value="method"
                    :options="METHOD_OPTIONS"
                  />
                </a-form-item>

                <a-form-item>
                  <template #label>
                    <span>请求地址</span>
                    <span class="field-hint">同源相对路径（<code>/api/</code> 开头）或完整 http(s) URL，最终以服务端 allowlist 为准</span>
                  </template>
                  <a-input
                    v-model:value="path"
                    placeholder="/api/auth/check"
                    allow-clear
                    @press-enter="sendRequest"
                  />
                </a-form-item>

                <a-form-item>
                  <template #label>
                    <span>请求头</span>
                    <span class="field-hint">平台凭据一律剥离；仅下发白名单内的自定义头</span>
                  </template>

                  <!-- 被剥离的凭据头：只读展示，让使用者明确「这些不会发出去」 -->
                  <div class="header-row platform-row">
                    <span class="header-key">Authorization / Sa-Token</span>
                    <span class="header-value">已剥离，不转发（详见上方安全边界）</span>
                    <a-tag color="default">
                      剥离
                    </a-tag>
                  </div>
                  <div class="header-row platform-row">
                    <span class="header-key">Cookie</span>
                    <span class="header-value">已剥离，不转发</span>
                    <a-tag color="default">
                      剥离
                    </a-tag>
                  </div>
                  <div class="header-row platform-row">
                    <span class="header-key">X-Tenant-Id / tenantId</span>
                    <span class="header-value">平台内部头，禁止注入（服务端黑名单）</span>
                    <a-tag color="default">
                      剥离
                    </a-tag>
                  </div>
                  <div class="header-row platform-row">
                    <span class="header-key">Content-Type</span>
                    <span class="header-value">{{ hasBody ? '有请求体时由服务端按 application/json 设置' : '（无请求体时不发送）' }}</span>
                    <a-tag color="default">
                      服务端
                    </a-tag>
                  </div>

                  <!-- 自定义头：显式逐行列出，可删除；名称受安全白名单约束 -->
                  <div
                    v-for="row in customHeaders"
                    :key="row.id"
                    class="header-row"
                  >
                    <a-input
                      v-model:value="row.key"
                      size="small"
                      class="header-key-input"
                      placeholder="请求头名称"
                    />
                    <a-input
                      v-model:value="row.value"
                      size="small"
                      class="header-value-input"
                      placeholder="值"
                    />
                    <a-button
                      type="text"
                      size="small"
                      danger
                      title="删除该请求头"
                      @click="removeHeaderRow(row.id)"
                    >
                      <DeleteOutlined />
                    </a-button>
                  </div>

                  <a-select
                    class="header-add"
                    size="small"
                    placeholder="从安全白名单中选择要添加的请求头"
                    :value="undefined"
                    :options="availableHeaderOptions"
                    @select="addHeaderRow"
                  />
                </a-form-item>

                <a-form-item>
                  <template #label>
                    <span>请求体（JSON）</span>
                    <span class="field-hint">{{ bodyHint }}</span>
                  </template>
                  <a-textarea
                    v-model:value="bodyText"
                    :rows="8"
                    :disabled="!hasBody"
                    placeholder='{"key": "value"}'
                  />
                </a-form-item>

                <a-form-item>
                  <a-space>
                    <a-button
                      type="primary"
                      :loading="sending"
                      :disabled="sending"
                      @click="sendRequest"
                    >
                      <template #icon>
                        <SendOutlined />
                      </template>
                      发送请求
                    </a-button>
                  </a-space>
                </a-form-item>
              </a-form>
            </a-card>
          </a-col>

          <!-- ═══ 右栏：响应结果 ═══ -->
          <a-col
            :xs="24"
            :lg="14"
          >
            <a-card
              :bordered="false"
              title="响应结果"
            >
              <template #extra>
                <a-space :size="8">
                  <a-tag v-if="requestMethod">
                    {{ requestMethod }} {{ requestPath }}
                  </a-tag>
                  <a-tag v-if="statusCode !== null">
                    <span :class="statusClass">{{ statusCode }}</span>
                  </a-tag>
                  <a-tag v-if="elapsed !== null">
                    {{ elapsed }}ms
                  </a-tag>
                  <a-tag v-if="responseBytes !== null">
                    {{ responseBytes }}B
                  </a-tag>
                </a-space>
              </template>

              <a-alert
                v-if="resultNotice"
                :type="resultNoticeType"
                show-icon
                class="result-notice"
                :message="resultNotice"
              />

              <!-- 响应头（服务端已脱敏：Set-Cookie / WWW-Authenticate 等不回显原值） -->
              <div
                v-if="responseHeaderRows.length"
                class="resp-headers"
              >
                <div class="resp-headers-title">
                  响应头（{{ responseHeaderRows.length }}，已脱敏）
                </div>
                <div
                  v-for="h in responseHeaderRows"
                  :key="h.name"
                  class="resp-header-row"
                >
                  <span class="resp-header-name">{{ h.name }}</span>
                  <span class="resp-header-value">{{ h.value }}</span>
                </div>
              </div>

              <pre class="response-body">{{ responseText }}</pre>
            </a-card>
          </a-col>
        </a-row>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SendOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'

defineOptions({ name: 'AdminDevApiTest' })

// ═══ 后端端点（axios baseURL 已是 /api，此处写相对路径） ═══
const SEND_ENDPOINT = '/dev/api-test/send'
const POLICY_ENDPOINT = '/dev/api-test/policy'

// ═══ 前端预检常量（**只做快速反馈**；真正的安全边界在服务端，前端结论不作数） ═══
/** 请求地址最大长度（服务端 app.api-test.max-url-length 亦为 2048） */
const MAX_PATH_LENGTH = 2048
/** 请求头名称必须符合 RFC 7230 token 语法，否则可能存在头注入风险 */
const HEADER_NAME_RE = /^[!#$%&'*+\-.^_`|~0-9A-Za-z]+$/

/** 允许自定义的请求头白名单：全部为非凭据、非网络控制类头 */
const ALLOWED_CUSTOM_HEADERS = [
  'Accept',
  'Accept-Language',
  'Cache-Control',
  'Pragma',
  'If-None-Match',
  'If-Modified-Since',
  'X-Request-Id',
  'X-Trace-Id',
  'X-Correlation-Id',
  'X-Client-Version',
]
/** 明确拒绝的请求头 → 中文拒绝原因（与后端黑名单同口径：逐跳头 + 平台内部头） */
const BLOCKED_HEADERS: Record<string, string> = {
  authorization: '平台会话凭据一律剥离，不代为转发（如确需，由运维开启 app.api-test.allow-user-authorization）',
  'sa-token': '平台会话凭据一律剥离，不代为转发',
  cookie: 'Cookie 属凭据类头，禁止由本页注入',
  'set-cookie': '禁止注入响应语义的请求头',
  host: 'Host 由服务端按目标地址决定，禁止改写',
  origin: 'Origin 禁止改写',
  referer: 'Referer 禁止改写',
  'user-agent': 'User-Agent 由服务端标识，禁止改写',
  'content-length': '长度由运行时计算，禁止手工指定',
  'transfer-encoding': '禁止改写传输编码',
  connection: '禁止改写连接控制头',
  'keep-alive': '禁止改写连接控制头',
  upgrade: '禁止改写协议升级头',
  te: '禁止改写传输编码协商头',
  trailer: '禁止改写传输头',
  expect: '禁止改写 Expect 头',
  'x-forwarded-for': '禁止伪造来源 IP',
  'x-forwarded-host': '禁止伪造来源主机',
  'x-forwarded-proto': '禁止伪造来源协议',
  'x-real-ip': '禁止伪造来源 IP',
  'x-tenant-id': '租户凭据属平台内部头，禁止注入',
  tenantid: '租户凭据属平台内部头，禁止注入',
  'x-user-id': '用户标识属平台内部头，禁止注入',
}

const METHOD_OPTIONS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'].map(m => ({ label: m, value: m }))
/** GET/HEAD 不携带请求体 */
const BODYLESS_METHODS = ['GET', 'HEAD']

// ═══ 请求配置状态 ═══
const method = ref('GET')
/**
 * 默认目标：`/api/auth/check`。
 * 它在 SaTokenConfig 里被 excludePathPatterns 排除，**无需凭据即返回 200**，
 * 且响应体里的 `valid:false` 恰好证明「平台凭据确实没有被转发」—— 是验证本页链路的最佳默认值。
 */
const path = ref('/api/auth/check')
const bodyText = ref('')
interface HeaderRow { id: number; key: string; value: string }
const customHeaders = ref<HeaderRow[]>([])
let headerRowSeq = 0

// ═══ 响应状态 ═══
const sending = ref(false)
const responseText = ref('点击「发送请求」查看响应')
const statusCode = ref<number | null>(null)
const elapsed = ref<number | null>(null)
const responseBytes = ref<number | null>(null)
const resultNotice = ref('')
const resultNoticeType = ref<'success' | 'info' | 'warning' | 'error'>('info')
const requestMethod = ref('')
const requestPath = ref('')
const responseHeaders = ref<Record<string, string>>({})

// ═══ 服务端安全边界（来自 policy 接口，页面只做展示） ═══
interface ApiTestPolicy {
  enabled?: boolean
  selfBaseUrl?: string
  allowExternal?: boolean
  allowedHosts?: string[]
  allowUserAuthorization?: boolean
  exemptSelfOriginFromPrivateIpCheck?: boolean
  blockedIpRanges?: string[]
  blockedHeaders?: string[]
  connectTimeoutMs?: number
  readTimeoutMs?: number
  maxResponseBytes?: number
  maxRequestBodyBytes?: number
  rateLimitPerMinute?: number
  followRedirects?: boolean
  pinResolvedIp?: boolean
}
const policy = ref<ApiTestPolicy>({})
const policyLoaded = ref(false)

/** 后端 /send 的响应体（ApiTestSendResult 的扁平结构） */
interface ApiTestSendResult {
  success: boolean
  message?: string
  rejectReason?: string
  targetUrl?: string
  status?: number | null
  elapsedMs?: number | null
  responseBytes?: number | null
  truncated?: boolean
  redirectBlocked?: boolean
  responseHeaders?: Record<string, string>
  responseBody?: string | null
}

const hasBody = computed(() => !BODYLESS_METHODS.includes(method.value))
const bodyHint = computed(() => (hasBody.value
  ? '留空表示不发送请求体；非空时必须是合法 JSON'
  : `${method.value} 请求不携带请求体`))

const availableHeaderOptions = computed(() =>
  ALLOWED_CUSTOM_HEADERS.map(name => ({ label: name, value: name })))

const policyMessage = computed(() => (policyLoaded.value
  ? '安全边界：由服务端强制（allowlist / 屏蔽内网网段 / 凭据剥离 / 调用审计 / 限流），前端不再直发请求'
  : '安全边界：由服务端强制（正在读取服务端生效策略…）'))

const responseHeaderRows = computed(() =>
  Object.entries(responseHeaders.value).map(([name, value]) => ({ name, value })))

// 说明文案里的数字一律取自服务端 policy，不在前端写死
const timeoutText = computed(() => {
  const connect = Math.round((policy.value.connectTimeoutMs || 5000) / 1000)
  const read = Math.round((policy.value.readTimeoutMs || 5000) / 1000)
  return `连接/读取各 ${connect}/${read} 秒超时`
})
const maxResponseText = computed(() => `${Math.round((policy.value.maxResponseBytes || 262144) / 1024)}KB`)
const rateLimitText = computed(() => `${policy.value.rateLimitPerMinute ?? 0} 次/分钟/人`)

const statusClass = computed(() => {
  const code = statusCode.value
  if (code === null) return ''
  if (code >= 200 && code < 300) return 'status-ok'
  if (code >= 300 && code < 400) return 'status-warn'
  return 'status-error'
})

// ═══ 请求头行操作 ═══
function addHeaderRow(name: string) {
  customHeaders.value.push({ id: ++headerRowSeq, key: name, value: '' })
}
function removeHeaderRow(id: number) {
  customHeaders.value = customHeaders.value.filter(row => row.id !== id)
}

// ═══ 前端预检（快速反馈；判定权在服务端） ═══
/** 预检请求地址：语法层面为「/ 相对路径」或「http(s) 绝对地址」 */
function validatePath(raw: string): string | null {
  if (!raw) return '请输入请求地址'
  if (raw.length > MAX_PATH_LENGTH) return `请求地址过长（上限 ${MAX_PATH_LENGTH} 字符）`
  if (/[\s\\]/.test(raw) || [...raw].some(ch => ch.charCodeAt(0) < 0x20)) {
    return '请求地址不能包含空格、反斜杠或控制字符'
  }
  if (raw.includes('..')) return '请求地址不能包含 ".." 上级目录跳转'
  if (raw.startsWith('/')) {
    if (raw.startsWith('//')) {
      return '禁止协议相对地址（//host/...）；请写 /api/ 开头的同源相对路径，或完整的 http(s) URL'
    }
    if (!raw.startsWith('/api/')) return '同源相对路径必须以 /api/ 开头'
    return null
  }
  if (!/^https?:\/\//i.test(raw)) {
    return '只允许 http:// 或 https:// 开头的完整 URL（file:/ftp:/gopher:/jar: 等协议被服务端拒绝）'
  }
  return null
}

/** 预检自定义请求头：名称必须命中白名单且合法，值不得含换行（防头注入） */
function validateHeaderRow(row: HeaderRow): string | null {
  const key = (row.key || '').trim()
  const value = row.value ?? ''
  if (!key && !value) return null // 空行直接忽略
  if (!key) return '存在只填了值、未填名称的请求头行，请补全或删除该行'
  if (!HEADER_NAME_RE.test(key)) return `请求头名称「${key}」含非法字符（仅允许 RFC 7230 token 字符）`
  const lower = key.toLowerCase()
  if (BLOCKED_HEADERS[lower]) return `禁止设置请求头「${key}」：${BLOCKED_HEADERS[lower]}`
  if (lower.startsWith('proxy-')) return `禁止设置请求头「${key}」：代理控制类头不可由页面注入`
  if (!ALLOWED_CUSTOM_HEADERS.some(name => name.toLowerCase() === lower)) {
    return `请求头「${key}」不在安全白名单内；可选：${ALLOWED_CUSTOM_HEADERS.join('、')}`
  }
  if (/[\r\n]/.test(value)) return `请求头「${key}」的值不能包含换行符（防请求头注入）`
  if (value.length > 512) return `请求头「${key}」的值过长（上限 512 字符）`
  return null
}

/** 响应体美化：JSON 可解析则缩进展示，否则原样 */
function presentBody(raw: string): string {
  if (!raw) return '(空响应体)'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

function setNotice(type: 'success' | 'info' | 'warning' | 'error', text: string) {
  resultNoticeType.value = type
  resultNotice.value = text
}

/** 校验失败/被拒绝时清空结果态，不做任何假数据兜底 */
function resetResult(type: 'success' | 'info' | 'warning' | 'error', text: string) {
  statusCode.value = null
  elapsed.value = null
  responseBytes.value = null
  requestMethod.value = ''
  requestPath.value = ''
  responseHeaders.value = {}
  responseText.value = '(未发送请求)'
  setNotice(type, text)
}

// ═══ 读取服务端安全边界 ═══
async function loadPolicy() {
  try {
    const res = await request.get(POLICY_ENDPOINT)
    if (res && typeof res === 'object') {
      policy.value = res as ApiTestPolicy
      policyLoaded.value = true
    }
  } catch (e: unknown) {
    // 读不到就如实说「读不到」，不假装边界已生效
    console.error('[API测试] 读取服务端安全边界失败', e)
    policyLoaded.value = false
  }
}

// ═══ 发送请求（改为调用后端受控端点） ═══
async function sendRequest() {
  if (sending.value) return

  // 1) 地址预检
  const target = path.value.trim()
  const pathError = validatePath(target)
  if (pathError) {
    message.warning(pathError)
    resetResult('warning', pathError)
    return
  }

  // 2) 请求体预检（独立 try/catch，避免非法 JSON 被误报成服务端错误）
  let payload: string | undefined
  if (hasBody.value && bodyText.value.trim()) {
    try {
      JSON.parse(bodyText.value)
      payload = bodyText.value
    } catch (e: any) {
      const reason = `请求体不是合法 JSON：${e?.message || '解析失败'}（未发送请求）`
      console.error('[API测试] 请求体不是合法 JSON（未发送请求）', e)
      message.warning('请求体不是合法 JSON，请修正后再发送')
      resetResult('warning', reason)
      return
    }
  }

  // 3) 请求头预检
  const headerErrors: string[] = []
  for (const row of customHeaders.value) {
    const headerError = validateHeaderRow(row)
    if (headerError) headerErrors.push(headerError)
  }
  if (headerErrors.length) {
    const reason = headerErrors.join('；')
    message.warning('请求头校验未通过，未发送请求')
    resetResult('warning', reason)
    return
  }
  // 仅发送「名称与值都已填写」的行；未填完的行直接忽略
  const headersToSend: Record<string, string> = {}
  for (const row of customHeaders.value) {
    const key = (row.key || '').trim()
    const value = (row.value ?? '').trim()
    if (key && value) headersToSend[key] = value
  }

  // 4) 交给后端（服务端会重新做一遍全部闸门；平台凭据不由前端注入，也无从注入）
  sending.value = true
  requestMethod.value = method.value
  requestPath.value = target
  statusCode.value = null
  elapsed.value = null
  responseBytes.value = null
  responseHeaders.value = {}
  resultNotice.value = ''
  responseText.value = '请求中…'

  try {
    const res = await request.post(SEND_ENDPOINT, {
      url: target,
      method: method.value,
      headers: headersToSend,
      body: payload,
    }) as unknown as ApiTestSendResult

    // 被闸门拒绝 / 请求没有发出去：如实展示服务端给出的拒绝原因
    if (!res || res.success !== true) {
      const reason = res?.rejectReason || res?.message || '请求未被服务端发出（未返回原因）'
      console.warn('[API测试] 请求被服务端拒绝', { target, reason })
      resetResult('warning', `服务端未发出该请求：${reason}`)
      return
    }

    // 请求已发出：展示目标的真实状态码/耗时/响应体
    requestPath.value = res.targetUrl || target
    statusCode.value = res.status ?? null
    elapsed.value = res.elapsedMs ?? null
    responseBytes.value = res.responseBytes ?? null
    responseHeaders.value = res.responseHeaders || {}
    responseText.value = presentBody(res.responseBody || '')

    if (res.redirectBlocked) {
      setNotice('warning', res.message || '目标返回了重定向，已按安全策略阻止跟随')
    } else if (res.truncated) {
      setNotice('warning', res.message || '响应体超过上限，已流式截断展示')
    } else if ((res.status ?? 0) === 401 || (res.status ?? 0) === 403) {
      setNotice('info', `${res.message || ''}（平台凭据已按安全策略剥离，未转发给目标，故需鉴权接口会返回 ${res.status}）`)
    } else if (res.status && res.status >= 200 && res.status < 300) {
      setNotice('success', res.message || `请求成功（HTTP ${res.status}）`)
    } else {
      setNotice('warning', res.message || `服务端返回 HTTP ${res.status ?? '-'}：响应体已原样展示`)
    }
  } catch (e: any) {
    console.error('[API测试] 调用后端受控端点失败', { target, error: e })
    resetResult('error', `调用后端受控端点失败：${e?.message || '网络错误'}（该请求没有发出）`)
  } finally {
    sending.value = false
  }
}

function handleError(error: Error) {
  console.error('[API测试] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(loadPolicy)
</script>

<style scoped>
.api-test-page { display: flex; flex-direction: column; gap: 12px; }
.security-alert { flex-shrink: 0; }
.security-list { margin: 0; padding-left: 18px; }
.security-list li { margin-bottom: 4px; line-height: 1.6; }
.security-list code { background: rgba(0, 0, 0, 0.06); padding: 0 4px; border-radius: 2px; }
.field-hint { margin-left: 8px; font-size: 12px; color: #8c8c8c; font-weight: 400; }
.header-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.platform-row { background: #fafafa; border: 1px dashed #d9d9d9; border-radius: 4px; padding: 4px 8px; }
.header-key { width: 170px; font-size: 12px; color: #595959; flex-shrink: 0; }
.header-value { flex: 1; font-size: 12px; color: #8c8c8c; word-break: break-all; }
.header-key-input { width: 170px; flex-shrink: 0; }
.header-value-input { flex: 1; }
.header-add { width: 100%; margin-top: 4px; }
.result-notice { margin-bottom: 12px; }
.resp-headers { margin-bottom: 12px; border: 1px solid #f0f0f0; border-radius: 4px; padding: 8px 12px; background: #fafafa; }
.resp-headers-title { font-size: 12px; color: #8c8c8c; margin-bottom: 4px; }
.resp-header-row { display: flex; gap: 8px; font-size: 12px; line-height: 1.6; }
.resp-header-name { width: 190px; flex-shrink: 0; color: #595959; word-break: break-all; }
.resp-header-value { flex: 1; color: #8c8c8c; word-break: break-all; }
.status-ok { color: #52c41a; }
.status-warn { color: #faad14; }
.status-error { color: #ff4d4f; }
.response-body {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 4px;
  min-height: 400px;
  max-height: 600px;
  overflow: auto;
  font-size: 13px;
  line-height: 1.5;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
