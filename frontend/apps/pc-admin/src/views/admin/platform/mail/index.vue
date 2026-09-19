<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        邮件配置（系统 → 平台设置 → 邮件配置，菜单 62502）
        · 平台控制台页面：ql361 无对标 → 按 Odoo 「Outgoing Mail Server」（ir.mail_server）建模
        · 后端 cn.aiedge.platform.controller.MailConfigController（前缀 /api/mail），表 sys_mail_config
        · 本页是**配置表单页**（单实例配置），不是列表页 → 用 PageContainer + FormSection 分区卡片，
          不上 CategoryListLayout（无「多条记录需要筛选」的场景）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/邮件配置开发文档.md
        · 与文档的时点差异（2026-09-19 实测）：文档 §6 记的「3 个端点全部 404」是
          `cn.aiedge.platform` 未装配时的旧结论；装配补齐后 GET/POST /config、POST /test 均 200
      -->
      <div class="config-page">
        <div class="form-scroll-area">
          <a-spin :spinning="loading">
            <a-alert
              v-if="loadError"
              class="page-alert"
              type="error"
              show-icon
              :message="`读取后端配置失败：${loadError}`"
              description="下方表单保留上一次成功读取的值（首次进入时为空表单），不是后端返回的数据。请排查后点「重置」重新读取，不要在此状态下直接保存。"
            />

            <a-alert
              class="page-alert"
              type="warning"
              show-icon
              message="本页真实能力边界（按 2026-09-19 源码 + 实测如实标注，未接线项不做包装）"
            >
              <template #description>
                <ul class="alert-list">
                  <li>
                    <b>「测试连接」已改为真实 SMTP 测试</b>（2026-09-19）：后端按 SMTP 协议
                    <b>真实建连并完成认证</b>（连接/读取超时各 5s），失败时返回具体原因
                    （主机不可达 / 认证失败 / 缺字段）—— 不再是恒返回成功的桩。
                    边界：只做「连接 + 认证」，<b>不发信</b>。
                  </li>
                  <li>
                    <b>保存的配置已接入真实发信链路</b>（2026-09-19）：<code>EmailSenderImpl</code>
                    与通知模块的 <code>EmailChannel</code> 现在都<b>优先读本表</b>
                    （经 <code>PlatformMailSettingsProvider</code>），未配置或读取失败才回退到 yml 的
                    <code>spring.mail.*</code>。<b>保存即生效</b>（每次发信实时读库，无缓存）。
                    接线前的问题：yml 里根本没有 <code>spring.mail.*</code> →
                    <code>JavaMailSender</code> bean 不存在 → 邮件<b>恒发不出</b>，且本表无人读。
                  </li>
                  <li>
                    <b>落库租户口径</b>：保存按当前会话租户（请求头 <code>X-Tenant-Id</code>）落一行；
                    平台级行（<code>tenant_id=0</code>）<b>不会被就地更新</b> —— 后端
                    <code>getConfig</code> 的租户回退与 <code>saveConfig</code> 的租户主查口径不一致（首次保存会新建一行）。
                  </li>
                  <li>
                    <b>凭据</b>：GET 接口会把 <code>password</code> 明文放进响应（实体无 <code>@JsonIgnore</code>）；
                    本页已改为掩码回显，<b>只有点「重新输入」并填了值才提交该字段</b>，未改动则整键省略以保留后端原值。
                  </li>
                  <li>
                    <b>无审计</b>：保存不写 <code>sys_audit_log</code>，「谁把 SMTP 改成了什么」事后不可还原。
                  </li>
                  <li>
                    <b>原「邮件模板」卡片已移除</b>：<code>GET /api/mail/templates</code> 后端零实现（实测 404），
                    原实现的模板表恒空且失败被静默吞掉；待后端裁定用哪张模板表后再补。
                  </li>
                </ul>
              </template>
            </a-alert>

            <a-form
              ref="formRef"
              :model="form"
              :rules="rules"
              layout="horizontal"
              :label-col="{ span: 7 }"
              :wrapper-col="{ span: 17 }"
              :colon="false"
              size="small"
            >
              <FormSection
                title="SMTP 服务器"
                tip="服务器地址、端口、加密方式三者必须与邮件服务商给出的口径一致"
              >
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="SMTP服务器"
                      name="host"
                    >
                      <a-input
                        v-model:value="form.host"
                        placeholder="如 smtp.example.com"
                        :maxlength="255"
                        allow-clear
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="端口"
                      name="port"
                    >
                      <a-input-number
                        v-model:value="form.port"
                        :min="1"
                        :max="65535"
                        :precision="0"
                        style="width: 100%"
                        placeholder="SSL 常用 465，STARTTLS 常用 587"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="加密方式"
                      name="encryption"
                    >
                      <a-select
                        v-model:value="form.encryption"
                        placeholder="请选择加密方式"
                        allow-clear
                        :options="ENCRYPTION_OPTIONS"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="启用状态"
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
                </a-row>

                <a-row :gutter="24">
                  <a-col :span="24">
                    <div class="field-tip">
                      加密方式值域三方写法不一致（前端/DB 小写 <code>none/ssl/tls</code>、后端 Schema 注释大写
                      <code>NONE/SSL/TLS</code>）；本页统一按 DB 口径提交小写，回显时也做小写归一。
                    </div>
                  </a-col>
                </a-row>
              </FormSection>

              <FormSection
                title="发件身份"
                tip="用户名通常是完整邮箱地址；发件人地址为空时由后端沿用历史值（本页不提交空值）"
              >
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="邮箱地址"
                      name="username"
                    >
                      <a-input
                        v-model:value="form.username"
                        placeholder="如 noreply@example.com"
                        :maxlength="128"
                        allow-clear
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="发件人地址"
                      name="fromAddress"
                    >
                      <a-input
                        v-model:value="form.fromAddress"
                        placeholder="收件人看到的发件地址"
                        :maxlength="128"
                        allow-clear
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="24">
                  <a-col :span="24">
                    <a-form-item
                      label="密码/授权码"
                      name="password"
                    >
                      <SecretInput
                        v-model:value="secret.password.draft"
                        v-model:editing="secret.password.editing"
                        :has-stored="secret.password.hasStored"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>
              </FormSection>
            </a-form>
          </a-spin>
        </div>

        <!-- 底部操作区：保存 / 重置 / 测试连接 -->
        <div class="form-footer">
          <span class="footer-tip">
            带 <em>*</em> 为必填项；保存后会自动回读服务端数据核对，回读不一致会给出警告。
            凭据未点「重新输入」时不会被提交，也不会被掩码覆盖。
          </span>
          <a-space>
            <a-tooltip
              placement="bottom"
              title="放弃未保存的修改，重新从后端读取"
            >
              <a-button
                :disabled="saving || testing"
                @click="handleReset"
              >
                重置
              </a-button>
            </a-tooltip>
            <a-tooltip
              placement="bottom"
              title="用当前输入请求后端 /mail/test（真实建立 SMTP 连接并认证，不发信）"
            >
              <a-button
                :loading="testing"
                :disabled="saving"
                @click="handleTest"
              >
                测试连接
              </a-button>
            </a-tooltip>
            <a-button
              type="primary"
              :loading="saving"
              :disabled="testing"
              @click="handleSave"
            >
              <template #icon>
                <SaveOutlined />
              </template>
              保存
            </a-button>
          </a-space>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SaveOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FormSection from '@/components/FormSection/index.vue'
import SecretInput from '@/components/SecretInput/index.vue'
import { requiredRule, requiredSelectRule, emailRule } from '@/utils/formRules'
import { platformMailConfigApi, type PlatformMailConfig } from '@/api/admin'

defineOptions({ name: 'AdminPlatformMail' })

/** 掩码串（与 SecretInput 内部常量一致）：提交前最后一道拦截，掩码绝不落库 */
const SECRET_MASK = '******'

/** 加密方式选项（值按 DB 口径小写，label 与后端 Schema 注释对齐） */
const ENCRYPTION_OPTIONS = [
  { label: '无', value: 'none' },
  { label: 'SSL', value: 'ssl' },
  { label: 'TLS', value: 'tls' }
]

/** 凭据字段状态：hasStored（后端有没有值）+ editing（用户是否主动重录）+ draft（草稿） */
interface SecretState { hasStored: boolean; editing: boolean; draft: string }

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const loadError = ref('')
const formRef = ref<FormInstance>()

/** 表单字段（= 后端 MailConfig 的可编辑列） */
const form = reactive({
  host: '',
  port: undefined as number | undefined,
  encryption: undefined as string | undefined,
  username: '',
  fromAddress: '',
  enabled: false
})

const secret = reactive<{ password: SecretState }>({
  password: { hasStored: false, editing: false, draft: '' }
})

const rules: Record<string, Rule[]> = {
  host: [requiredRule('SMTP服务器'), { max: 255, message: '服务器地址最长 255 个字符', trigger: 'blur' }],
  port: [requiredRule('端口')],
  encryption: [requiredSelectRule('加密方式')],
  // 用户名**不**套 emailRule：不少服务商的 SMTP 用户名是 API key（如 SendGrid 的 apikey），
  // 强校验邮箱会把合法配置挡在门外；只要求非空。
  username: [requiredRule('邮箱地址'), { max: 128, message: '邮箱地址最长 128 个字符', trigger: 'blur' }],
  fromAddress: [emailRule, { max: 128, message: '发件人地址最长 128 个字符', trigger: 'blur' }]
}

/** 加密方式小写归一：后端 Schema 注释写的是大写，DB 里是小写，回显时统一 */
function normalizeEncryption(value?: string | null): string | undefined {
  return value ? String(value).toLowerCase() : undefined
}

/** 服务端值 → 表单；凭据只取「有没有」，明文一律不进表单（掩码回显） */
function applyConfig(data: PlatformMailConfig) {
  form.host = data.host ?? ''
  form.port = data.port ?? undefined
  form.encryption = normalizeEncryption(data.encryption)
  form.username = data.username ?? ''
  form.fromAddress = data.fromAddress ?? ''
  form.enabled = data.enabled === true
  secret.password.hasStored = !!(data.password && data.password !== '')
  secret.password.draft = ''
  secret.password.editing = false
  // 回读后清掉上一轮的标红残留
  formRef.value?.clearValidate()
}

/**
 * 凭据提交口径：**只有填了非空值才提交**该字段。
 *
 * 为什么按「值」而不是按「是否处于重录态」判定：
 *   SecretInput 在「后端没有存量值」时直接渲染可编辑框（没有掩码要保护，不必多点一次「重新输入」），
 *   此时 editing 仍是 false —— 若按 editing 判定，用户敲进去的凭据会被静默丢弃（实测踩到）。
 * 返回 undefined 时调用方**整键省略**：后端 MyBatis-Plus `updateById` 忽略 null，省略即保留 DB 原值；
 * 提交空串会把真实凭据清掉，提交掩码串会把凭据写成六个星号 —— 两种情况都必须挡掉。
 */
function resolveSecret(state: SecretState): string | undefined {
  const value = (state.draft || '').trim()
  if (!value || value === SECRET_MASK) return undefined
  return value
}

/** 表单 → 提交体（字段名与后端 MailConfig 逐字一致） */
function buildPayload(): Record<string, any> {
  const payload: Record<string, any> = {
    host: form.host.trim(),
    port: form.port,
    encryption: form.encryption,
    username: form.username.trim(),
    fromAddress: form.fromAddress.trim(),
    // enabled 必须显式回传：后端是原始 boolean，缺省即 false 且 updateById 不会跳过
    enabled: form.enabled
  }
  const password = resolveSecret(secret.password)
  if (password !== undefined) payload.password = password
  return payload
}

/**
 * 读取配置。
 * @returns 读到的实体；失败返回 null（此时**保留旧值**，绝不用假数据兜底）
 */
async function loadConfig(options?: { silent?: boolean }): Promise<PlatformMailConfig | null> {
  loading.value = true
  try {
    const data = (await platformMailConfigApi.load()) as PlatformMailConfig
    applyConfig(data || {})
    loadError.value = ''
    return data
  } catch (error: any) {
    console.error('[邮件配置] 读取配置失败', error)
    const msg = error?.message || '读取邮件配置失败'
    loadError.value = msg
    if (!options?.silent) message.error(msg)
    // 保持旧值：不清空表单
    return null
  } finally {
    loading.value = false
  }
}

/** 提交值 vs 回读值逐字段比对，返回不一致项的中文描述 */
function diffAgainstReadback(payload: Record<string, any>, back: PlatformMailConfig): string[] {
  const diffs: string[] = []
  if ((back.host ?? '') !== payload.host) diffs.push(`SMTP服务器 提交「${payload.host}」/ 回读「${back.host ?? ''}」`)
  if ((back.port ?? null) !== (payload.port ?? null)) diffs.push(`端口 提交「${payload.port ?? ''}」/ 回读「${back.port ?? ''}」`)
  if (normalizeEncryption(back.encryption) !== payload.encryption) {
    diffs.push(`加密方式 提交「${payload.encryption ?? ''}」/ 回读「${back.encryption ?? ''}」`)
  }
  if ((back.username ?? '') !== payload.username) diffs.push(`邮箱地址 提交「${payload.username}」/ 回读「${back.username ?? ''}」`)
  if ((back.fromAddress ?? '') !== payload.fromAddress) diffs.push(`发件人地址 提交「${payload.fromAddress}」/ 回读「${back.fromAddress ?? ''}」`)
  if ((back.enabled === true) !== (payload.enabled === true)) diffs.push('启用状态未按提交值保存')
  if (payload.password !== undefined) {
    // 提交了新凭据 → 必须能读到同一个值（后端明文返回，可直接比对）
    if ((back.password ?? '') !== payload.password) diffs.push('密码未被后端保存为新提交的值')
  } else if (!!(back.password && back.password !== '') !== secret.password.hasStored) {
    // 未提交凭据 → 只校验「有无」是否与提交前一致
    diffs.push('密码存在性在保存前后发生变化')
  }
  return diffs
}

async function handleSave() {
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
    const result = await platformMailConfigApi.save(payload)
    if (result.success === false) throw new Error('后端未返回保存成功')

    // 写后回读：以后端实际落库的值为准，回读不一致就警告，不谎报成功
    const back = await loadConfig({ silent: true })
    if (!back) {
      message.warning('保存请求已受理，但回读失败，无法确认是否落库，请点「重置」重新读取核对')
    } else {
      const diffs = diffAgainstReadback(payload, back)
      if (diffs.length) {
        message.warning(`保存请求已受理，但回读值不一致：${diffs.join('；')}。请点「重置」复核`)
      } else {
        message.success('邮件配置已保存（已回读核对一致）')
      }
    }
  } catch (error: any) {
    console.error('[邮件配置] 保存失败', error)
    message.error(error?.message || '保存邮件配置失败')
  } finally {
    saving.value = false
  }
}

/** 测试连接：不做表单校验（允许测当前输入），但必须读 success 并如实提示 */
async function handleTest() {
  testing.value = true
  try {
    const result = await platformMailConfigApi.test(buildPayload())
    if (result.success) {
      // 后端已于 2026-09-19 改为真实 SMTP 建连并认证 → 成功即可信
      message.success(`测试通过：${result.message}`)
    } else {
      message.error(`测试失败：${result.message}`)
    }
  } catch (error: any) {
    console.error('[邮件配置] 测试连接请求失败', error)
    message.error(error?.message || '测试连接请求失败')
  } finally {
    testing.value = false
  }
}

/** 重置：丢弃未保存的修改，重新从后端读取（不沿用「重置=置空」的历史语义） */
function handleReset() {
  loadConfig()
}

function handleError(error: Error) {
  console.error('[邮件配置] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadConfig()
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

/* a-form 不参与滚动容器高度计算，交给内部卡片撑开 */
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

.footer-tip em {
  color: #ff4d4f;
  font-style: normal;
}
</style>
