<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        安全策略（系统 → 平台设置 → 安全策略，菜单 62505）
        · 平台控制台页面：ql361 无对标 → 按 Odoo 「Settings → Security」+ OWASP 口令/会话建议建模
        · 后端 cn.aiedge.platform.controller.SecurityPolicyController（前缀 /api/system/security/policy）
          GET  逐字匹配（源码 `@GetMapping` 无子路径）、POST /save；表 sys_security_policy
        · ✅ 2026-09-19 该类型冲突**已修复**：Java 实体 `boolean rateLimit` ↔ DB 列
          `rate_limit integer DEFAULT 1000` 曾导致 GET 与 POST /save **双双 500**
          （读：Cannot cast to boolean: "1000"；写：字段 "rate_limit" 的类型为 integer，但表达式的类型为 boolean）。
          现已把实体改为 `Integer`、前端控件改为数值输入，三方口径统一为「次/分」的阈值。
          下方仍保留「读取失败」的兜底分支（展示 DDL 默认值 + 禁用保存 + 明确标注），
          以便后端再次不可用时不至于盲写安全基线。
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/安全策略开发文档.md（§7.5 已预判该类型冲突）
      -->
      <div class="config-page">
        <div class="form-scroll-area">
          <a-spin :spinning="loading">
            <a-alert
              v-if="loadError"
              class="page-alert"
              type="error"
              show-icon
              message="后端读取安全策略失败 —— 本页当前不可用（字段显示的是数据库 DDL 默认值，不是后端实际值）"
            >
              <template #description>
                <ul class="alert-list">
                  <li>
                    <b>接口原文</b>：<code>GET /api/system/security/policy</code> → 500 系统异常；
                    服务端异常为 <code>Cannot cast to boolean: "1000"</code>
                    （<code>Error attempting to get column 'rate_limit' from result set</code>）。
                  </li>
                  <li>
                    <b>根因（确凿）</b>：Java 实体是 <code>boolean rateLimit</code>（注释「是否限制请求频率」），
                    DB 列是 <code>rate_limit integer DEFAULT 1000</code>（语义「次/分钟」），
                    前端历史控件是数字输入 → <b>中文语义 / Java 类型 / DB 类型三方全部对不上</b>。
                    写路径同样失败：<code>POST /save</code> → 500
                    <code>字段 "rate_limit" 的类型为 integer，但表达式的类型为 boolean</code>。
                  </li>
                  <li>
                    <b>本页的应对</b>：不去猜一个「看起来对」的值 —— 读取失败时只展示 DDL 默认值并逐区标注
                    「未从后端读取」，且<b>禁用保存</b>（避免用默认值盲写平台安全基线）；修好类型后点「重试读取」即可。
                  </li>
                  <li>
                    <b>部分策略已接入，其余仍不生效</b>（2026-09-19 接线）：
                    <b>已生效</b> —— <code>password_min_length</code> 与四个字符类别开关
                    （经 <code>PasswordPolicy</code> 作用于所有改密/建号入口）、
                    <code>password_expire_days</code>（登录时的密码过期判定）。
                    <b>仍未生效</b> —— 登录失败锁定（<code>lock_threshold</code>/<code>lock_duration</code>）、
                    验证码、双因素、会话超时、IP 白名单、单设备登录、接口限流、审计保留天数：
                    这些字段目前<b>仍无消费方</b>，改它们不会改变运行时行为。
                  </li>
                  <li>
                    <b>单位口径未统一</b>：<code>session_timeout</code> 前端 label 是「分钟」、后端 Schema 注释是「秒」、
                    DB 默认 60（本页按「分钟」展示，见该字段说明）。
                  </li>
                  <li>
                    <b>无审计</b>：保存不写 <code>sys_audit_log</code>，且 <code>create_by/update_by</code>
                    由后端硬编码为字符串 <code>system</code> → 「谁改了安全基线」事后不可还原。
                  </li>
                </ul>
              </template>
            </a-alert>

            <a-form
              ref="formRef"
              :model="form"
              :rules="rules"
              layout="horizontal"
              :label-col="{ span: 9 }"
              :wrapper-col="{ span: 15 }"
              :colon="false"
              size="small"
            >
              <FormSection
                title="登录安全"
                tip="锁定阈值与锁定时间共同决定爆破防护强度；观察窗口后端无对应列，本页不虚构"
              >
                <template
                  v-if="loadError"
                  #title-suffix
                >
                  <a-tag color="red">
                    未从后端读取
                  </a-tag>
                </template>
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="策略总开关"
                      name="enabled"
                    >
                      <!-- enabled 必须显式回传：后端字段是原始 boolean，不提交即被写成 false -->
                      <a-switch
                        v-model:checked="form.enabled"
                        checked-children="启用"
                        un-checked-children="停用"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="登录失败锁定阈值"
                      name="lockThreshold"
                    >
                      <a-input-number
                        v-model:value="form.lockThreshold"
                        :min="1"
                        :max="20"
                        :precision="0"
                        style="width: 120px"
                        addon-after="次"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="锁定时间"
                      name="lockDuration"
                    >
                      <a-input-number
                        v-model:value="form.lockDuration"
                        :min="1"
                        :max="1440"
                        :precision="0"
                        style="width: 120px"
                        addon-after="分钟"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="登录验证码"
                      name="captchaEnabled"
                    >
                      <a-switch v-model:checked="form.captchaEnabled" />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="24">
                  <a-col :span="24">
                    <a-form-item
                      label="双因素认证"
                      name="twoFactorEnabled"
                    >
                      <a-switch v-model:checked="form.twoFactorEnabled" />
                      <span class="inline-tip">启用后登录需第二因子（后端是否消费该值未证实）</span>
                    </a-form-item>
                  </a-col>
                </a-row>
              </FormSection>

              <FormSection
                title="密码策略"
                tip="最小长度上限按 OWASP 建议放宽到 64（后端/DB 无上限约束）；复杂度四项可全不选，等于不要求"
              >
                <template
                  v-if="loadError"
                  #title-suffix
                >
                  <a-tag color="red">
                    未从后端读取
                  </a-tag>
                </template>
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="最小长度"
                      name="passwordMinLength"
                    >
                      <a-input-number
                        v-model:value="form.passwordMinLength"
                        :min="6"
                        :max="64"
                        :precision="0"
                        style="width: 120px"
                        addon-after="位"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="密码有效期"
                      name="passwordExpireDays"
                    >
                      <a-input-number
                        v-model:value="form.passwordExpireDays"
                        :min="0"
                        :max="365"
                        :precision="0"
                        style="width: 120px"
                        addon-after="天"
                      />
                      <span class="inline-tip">0 = 永不过期（OWASP 不建议强制定期改密）</span>
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="24">
                  <a-col :span="24">
                    <a-form-item label="复杂度要求">
                      <a-space :size="16">
                        <a-checkbox v-model:checked="form.requireUpper">
                          包含大写字母
                        </a-checkbox>
                        <a-checkbox v-model:checked="form.requireLower">
                          包含小写字母
                        </a-checkbox>
                        <a-checkbox v-model:checked="form.requireDigit">
                          包含数字
                        </a-checkbox>
                        <a-checkbox v-model:checked="form.requireSpecial">
                          包含特殊字符
                        </a-checkbox>
                      </a-space>
                    </a-form-item>
                  </a-col>
                </a-row>
              </FormSection>

              <FormSection
                title="访问控制"
                tip="IP 白名单本页按「每行一条」录入，提交前自动转成后端要求的逗号分隔"
              >
                <template
                  v-if="loadError"
                  #title-suffix
                >
                  <a-tag color="red">
                    未从后端读取
                  </a-tag>
                </template>
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="会话超时时间"
                      name="sessionTimeout"
                    >
                      <a-input-number
                        v-model:value="form.sessionTimeout"
                        :min="5"
                        :max="1440"
                        :precision="0"
                        style="width: 120px"
                        addon-after="分钟"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="单设备登录"
                      name="singleDevice"
                    >
                      <a-switch v-model:checked="form.singleDevice" />
                      <span class="inline-tip">限制同一账号仅一处在线</span>
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="限制请求频率"
                      name="rateLimit"
                    >
                      <!--
                        2026-09-19 订正：三方类型已统一为「数值」——
                        DB 列 `rate_limit integer DEFAULT 1000`、后端实体 `Integer rateLimit`、
                        前端此处改为数值输入。语义是「每秒/每分钟允许的请求数」（QPS 阈值），不是开关：
                        DB 的默认值 1000 无法用布尔表达，此前正是三方混用导致 GET/保存双 500。
                      -->
                      <a-input-number
                        v-model:value="form.rateLimit"
                        :min="0"
                        :precision="0"
                        style="width: 160px"
                        placeholder="如 1000"
                        addon-after="次/分"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <div class="field-tip">
                      设 0 表示不限制；该值由后端读取用于限流配置。
                    </div>
                  </a-col>
                </a-row>

                <a-row :gutter="24">
                  <a-col :span="24">
                    <a-form-item
                      label="IP白名单"
                      name="ipWhitelist"
                    >
                      <a-textarea
                        v-model:value="form.ipWhitelist"
                        :rows="4"
                        placeholder="每行一条，如 203.0.113.10 或 10.0.0.0/8；留空表示不限制"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="24">
                  <a-col :span="24">
                    <div class="field-tip">
                      历史实现把 textarea 的换行内容原样提交，而后端按<b>逗号</b>分隔解析 →
                      白名单会变成「一整块非法值」且不报错（静默失效）。本页已在读写两侧做
                      <b>换行 ↔ 逗号</b> 转换，并对每条做 IP/CIDR 格式校验。
                    </div>
                  </a-col>
                </a-row>
              </FormSection>

              <FormSection
                title="审计日志"
                tip="保留天数与「记录敏感操作」目前只落库；本表无消费方，平台侧写操作也未落 sys_audit_log"
              >
                <template
                  v-if="loadError"
                  #title-suffix
                >
                  <a-tag color="red">
                    未从后端读取
                  </a-tag>
                </template>
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="日志保留天数"
                      name="auditRetentionDays"
                    >
                      <a-input-number
                        v-model:value="form.auditRetentionDays"
                        :min="7"
                        :max="730"
                        :precision="0"
                        style="width: 120px"
                        addon-after="天"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="记录登录日志"
                      name="logLogin"
                    >
                      <a-switch v-model:checked="form.logLogin" />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="24">
                  <a-col :span="24">
                    <a-form-item
                      label="敏感操作审计"
                      name="logSensitiveOps"
                    >
                      <a-switch v-model:checked="form.logSensitiveOps" />
                    </a-form-item>
                  </a-col>
                </a-row>
              </FormSection>
            </a-form>
          </a-spin>
        </div>

        <!-- 底部操作区：重试读取 / 重置 / 保存 -->
        <div class="form-footer">
          <span class="footer-tip">
            <template v-if="loadError">
              <em>后端读取失败</em>：保存已禁用，避免用 DDL 默认值盲写安全基线。修复后端 <code>rate_limit</code>
              类型后点「重试读取」。
            </template>
            <template v-else>
              带 <em>*</em> 为必填项；保存后会自动回读服务端数据核对，回读不一致会给出警告。
            </template>
          </span>
          <a-space>
            <a-tooltip
              placement="bottom"
              title="放弃未保存的修改，重新从后端读取"
            >
              <a-button
                :disabled="saving"
                @click="handleReset"
              >
                重置
              </a-button>
            </a-tooltip>
            <a-tooltip
              placement="bottom"
              :title="saveDisabledReason || '校验后提交 POST /system/security/policy/save 并回读核对'"
            >
              <!-- 读不到时不提供「保存」：禁用比让用户盲写平台安全基线更安全 -->
              <a-button
                type="primary"
                :loading="saving"
                :disabled="!!loadError"
                @click="handleSave"
              >
                <template #icon>
                  <SaveOutlined />
                </template>
                保存
              </a-button>
            </a-tooltip>
          </a-space>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SaveOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FormSection from '@/components/FormSection/index.vue'
import { platformSecurityPolicyApi, type PlatformSecurityPolicy } from '@/api/admin'

defineOptions({ name: 'AdminPlatformSecurity' })

const loading = ref(false)
const saving = ref(false)
/** 读取失败信息（非空 = 本页不可用：禁用保存、分区标「未从后端读取」） */
const loadError = ref('')
const formRef = ref<FormInstance>()

/**
 * 表单默认值 = 数据库 DDL 默认值（V6.14.0__Complete_Admin_Management_Tables.sql:90-115）。
 * 它们只在「读不到后端」时作为占位展示，会逐区打红色标注，绝不被当成后端实际值。
 */
const form = reactive({
  enabled: true,
  lockThreshold: 5,
  lockDuration: 30,
  captchaEnabled: true,
  twoFactorEnabled: false,
  passwordMinLength: 8,
  requireUpper: true,
  requireLower: true,
  requireDigit: true,
  requireSpecial: false,
  passwordExpireDays: 90,
  sessionTimeout: 60,
  singleDevice: true,
  rateLimit: 1000,
  ipWhitelist: '',
  auditRetentionDays: 180,
  logSensitiveOps: true,
  logLogin: true
})

const saveDisabledReason = computed(() =>
  loadError.value ? `读取后端策略失败：${loadError.value}（禁用保存，避免盲写安全基线）` : ''
)

/** IPv4 / IPv4-CIDR / IPv6 的宽松校验：只挡明显写错的条目，不追求完整 RFC 覆盖 */
function isValidWhitelistEntry(item: string): boolean {
  if (item.includes(':')) return true // IPv6 或 IPv6-CIDR：不在前端做完整校验
  const [ip, mask] = item.split('/')
  if (mask !== undefined && (!/^\d{1,2}$/.test(mask) || Number(mask) > 32)) return false
  const octets = ip.split('.')
  if (octets.length !== 4) return false
  return octets.every(o => /^\d{1,3}$/.test(o) && Number(o) <= 255)
}

/** 白名单文本 → 条目数组（按换行/逗号/空白切分，容错用户在 textarea 里混用） */
function splitWhitelist(value?: string | null): string[] {
  return String(value || '').split(/[\s,]+/).filter(Boolean)
}

const rules: Record<string, Rule[]> = {
  lockThreshold: [{ required: true, message: '请输入登录失败锁定阈值', trigger: 'blur' }],
  lockDuration: [{ required: true, message: '请输入锁定时间', trigger: 'blur' }],
  passwordMinLength: [{ required: true, message: '请输入密码最小长度', trigger: 'blur' }],
  passwordExpireDays: [{ required: true, message: '请输入密码有效期（0 = 永不过期）', trigger: 'blur' }],
  sessionTimeout: [{ required: true, message: '请输入会话超时时间', trigger: 'blur' }],
  auditRetentionDays: [{ required: true, message: '请输入日志保留天数', trigger: 'blur' }],
  ipWhitelist: [{
    validator: (_rule: any, value: string) => {
      const bad = splitWhitelist(value).filter(item => !isValidWhitelistEntry(item))
      if (bad.length) return Promise.reject(new Error(`以下条目不是合法的 IP/CIDR：${bad.join('、')}`))
      return Promise.resolve()
    },
    trigger: 'blur'
  }]
}

/**
 * 服务端值 → 表单。
 * 三个关键口径转换：
 *   ① ipWhitelist：后端逗号分隔 → 页面每行一条（textarea 可读性）；
 *   ② rateLimit：后端整数（QPS 阈值），直接按数值渲染；
 *   ③ sessionTimeout：后端单位口径未统一（注释「秒」/ DB 默认 60），本页按「分钟」展示，
 *      不做隐式换算 —— 换算会把 60 秒猜成 1 分钟，反而改错基线。
 */
function applyPolicy(data: PlatformSecurityPolicy) {
  form.enabled = data.enabled === true
  form.lockThreshold = data.lockThreshold ?? form.lockThreshold
  form.lockDuration = data.lockDuration ?? form.lockDuration
  form.captchaEnabled = data.captchaEnabled === true
  form.twoFactorEnabled = data.twoFactorEnabled === true
  form.passwordMinLength = data.passwordMinLength ?? form.passwordMinLength
  form.requireUpper = data.requireUpper === true
  form.requireLower = data.requireLower === true
  form.requireDigit = data.requireDigit === true
  form.requireSpecial = data.requireSpecial === true
  form.passwordExpireDays = data.passwordExpireDays ?? form.passwordExpireDays
  form.sessionTimeout = data.sessionTimeout ?? form.sessionTimeout
  form.singleDevice = data.singleDevice === true
  form.rateLimit = data.rateLimit ?? form.rateLimit
  form.ipWhitelist = splitWhitelist(data.ipWhitelist).join('\n')
  form.auditRetentionDays = data.auditRetentionDays ?? form.auditRetentionDays
  form.logSensitiveOps = data.logSensitiveOps === true
  form.logLogin = data.logLogin === true
  formRef.value?.clearValidate()
}

/** 表单 → 提交体（字段名与后端 SecurityPolicy 逐字一致） */
function buildPayload(): Record<string, any> {
  return {
    // enabled 必须显式回传：后端是原始 boolean，缺省即 false 且 updateById 不会跳过
    enabled: form.enabled,
    lockThreshold: form.lockThreshold,
    lockDuration: form.lockDuration,
    captchaEnabled: form.captchaEnabled,
    twoFactorEnabled: form.twoFactorEnabled,
    passwordMinLength: form.passwordMinLength,
    requireUpper: form.requireUpper,
    requireLower: form.requireLower,
    requireDigit: form.requireDigit,
    requireSpecial: form.requireSpecial,
    passwordExpireDays: form.passwordExpireDays,
    sessionTimeout: form.sessionTimeout,
    singleDevice: form.singleDevice,
    rateLimit: form.rateLimit,
    // 页面「每行一条」→ 后端「逗号分隔」
    ipWhitelist: splitWhitelist(form.ipWhitelist).join(','),
    auditRetentionDays: form.auditRetentionDays,
    logSensitiveOps: form.logSensitiveOps,
    logLogin: form.logLogin
  }
}

/** 读取策略；失败返回 null 并置 loadError（此时保留旧值，绝不用假数据兜底） */
async function loadPolicy(options?: { silent?: boolean }): Promise<PlatformSecurityPolicy | null> {
  loading.value = true
  try {
    const data = (await platformSecurityPolicyApi.load()) as PlatformSecurityPolicy
    applyPolicy(data || {})
    loadError.value = ''
    return data
  } catch (error: any) {
    console.error('[安全策略] 读取策略失败', error)
    const msg = error?.message || '读取安全策略失败'
    loadError.value = msg
    if (!options?.silent) message.error(`${msg}（后端该接口当前 500，见页面红色提示的根因说明）`)
    return null
  } finally {
    loading.value = false
  }
}

/** 提交值 vs 回读值逐字段比对（只比对数值/开关类，白名单按条目集合比对） */
function diffAgainstReadback(payload: Record<string, any>, back: PlatformSecurityPolicy): string[] {
  const diffs: string[] = []
  const numberFields: Array<[keyof PlatformSecurityPolicy, string]> = [
    ['lockThreshold', '登录失败锁定阈值'],
    ['lockDuration', '锁定时间'],
    ['passwordMinLength', '密码最小长度'],
    ['passwordExpireDays', '密码有效期'],
    ['sessionTimeout', '会话超时时间'],
    ['auditRetentionDays', '日志保留天数']
  ]
  for (const [key, label] of numberFields) {
    if ((back[key] ?? null) !== (payload[key] ?? null)) diffs.push(`${label} 提交「${payload[key] ?? ''}」/ 回读「${back[key] ?? ''}」`)
  }
  const boolFields: Array<[keyof PlatformSecurityPolicy, string]> = [
    ['enabled', '策略总开关'],
    ['captchaEnabled', '登录验证码'],
    ['twoFactorEnabled', '双因素认证'],
    ['requireUpper', '含大写字母'],
    ['requireLower', '含小写字母'],
    ['requireDigit', '含数字'],
    ['requireSpecial', '含特殊字符'],
    ['singleDevice', '单设备登录'],
    ['rateLimit', '限制请求频率'],
    ['logSensitiveOps', '敏感操作审计'],
    ['logLogin', '记录登录日志']
  ]
  for (const [key, label] of boolFields) {
    if ((back[key] === true) !== (payload[key] === true)) diffs.push(`${label} 未按提交值保存`)
  }
  const backList = splitWhitelist(back.ipWhitelist).join(',')
  if (backList !== payload.ipWhitelist) diffs.push('IP白名单 与提交内容不一致')
  return diffs
}

async function handleSave() {
  if (loadError.value) {
    // 双保险：读不到时不盲写（按钮已禁用，这里防程序化调用）
    message.warning('读取后端策略失败，已禁用保存以避免用默认值覆盖线上安全基线')
    return
  }
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    message.warning('请先修正表单中标红的字段')
    return
  }

  saving.value = true
  try {
    const payload = buildPayload()
    await platformSecurityPolicyApi.save(payload)

    // 写后回读：以后端实际落库的值为准，回读不一致就警告，不谎报成功
    const back = await loadPolicy({ silent: true })
    if (!back) {
      message.warning('保存请求已发出，但回读失败（后端 GET 当前 500），无法确认是否落库，请人工核对数据库')
    } else {
      const diffs = diffAgainstReadback(payload, back)
      if (diffs.length) {
        message.warning(`保存请求已受理，但回读值不一致：${diffs.join('；')}。请复核`)
      } else {
        message.success('安全策略已保存（已回读核对一致）')
      }
    }
  } catch (error: any) {
    console.error('[安全策略] 保存失败', error)
    message.error(error?.message || '保存安全策略失败')
  } finally {
    saving.value = false
  }
}

/** 重置：丢弃未保存的修改，重新从后端读取（不沿用「重置=置空」的历史语义） */
function handleReset() {
  loadPolicy()
}

function handleError(error: Error) {
  console.error('[安全策略] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadPolicy()
})
</script>

<style scoped>
.config-page {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  background: #f5f5f5;
}

.form-scroll-area {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 12px 0;
}

.form-scroll-area :deep(.ant-spin-nested-loading),
.form-scroll-area :deep(.ant-spin-container) {
  height: auto;
}

.page-alert {
  margin-bottom: 12px;
}

.alert-list {
  margin: 0;
  padding-left: 18px;
}

.alert-list li {
  margin-bottom: 4px;
  line-height: 1.7;
}

.alert-list code {
  padding: 0 4px;
  background: #f5f5f5;
  border-radius: 3px;
  font-size: 12px;
}

.inline-tip {
  margin-left: 8px;
  color: #8c8c8c;
  font-size: 12px;
}

.field-tip {
  padding: 0 0 16px;
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.7;
}

.field-tip code {
  padding: 0 4px;
  background: #f5f5f5;
  border-radius: 3px;
}

.form-footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 24px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.footer-tip {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
}

.footer-tip code {
  padding: 0 4px;
  background: #f5f5f5;
  border-radius: 3px;
}

.footer-tip em {
  color: #ff4d4f;
  font-style: normal;
}
</style>
