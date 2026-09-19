<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        短信配置（系统 → 平台设置 → 短信配置，菜单 62503）
        · 平台控制台页面：ql361 无对标 → 按 Odoo 「Outgoing SMS」+ 阿里云短信控制台建模
        · 后端 cn.aiedge.platform.controller.SmsConfigController（前缀 /api/sms），表 sys_sms_config
        · 本页是**配置表单页**（单实例配置），不是列表页 → PageContainer + FormSection 分区卡片
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/短信配置开发文档.md
        · 与文档的时点差异（2026-09-19 实测）：文档 §6 记的「5 个端点全部 404」是
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
                    <b>「测试短信」已改为真实探测</b>（2026-09-19）：先校验配置完整性
                    （provider / AccessKey / AccessSecret / signName 缺哪项报哪项），再对服务商
                    <b>真实发起 TCP 建连</b>（5s 超时）探测端点连通性。返回文案会明确区分
                    「网络连通」与「短信可用」——<b>不谎报</b>。
                    边界：本系统<b>未集成任何短信服务商 SDK</b>，无法校验 AccessKey 是否有效
                    （那需要调用 SendSms，会产生真实费用与真实短信）。
                  </li>
                  <li>
                    <b>保存的配置已接入发送链路</b>（2026-09-19）：<code>SmsSenderImpl</code>
                    （消息底座，营销短信走这条）与通知模块的 <code>SmsChannel</code> 现在都<b>优先读本表</b>。
                    <b>但仍发不出去</b>：本表配的是<b>厂商原生 API</b>（provider + AK/SK），
                    而代码侧是<b>通用 HTTP 网关</b>模型且未集成厂商 SDK（需请求签名）→
                    后端会<b>明确失败</b>并给出处置建议（改配 <code>sms.endpoint</code> 指向自建网关，
                    或等待 SDK 接入），<b>不会伪造成功</b>。
                  </li>
                  <li>
                    <b>表里没有任何频控字段</b>：验证码 1/分·5/时·10/天、通知 50/天、推广 50/天 + 夜间禁发
                    都是<b>运营商侧硬约束</b>，本地无限流能力 → 未来真正接入发送后，超频会「发送失败且系统不自知」。
                  </li>
                  <li>
                    <b>落库租户口径</b>：保存按当前会话租户（请求头 <code>X-Tenant-Id</code>）落一行；
                    平台级行（<code>tenant_id=0</code>）<b>不会被就地更新</b>（后端
                    <code>getConfig</code> 的租户回退与 <code>saveConfig</code> 的租户主查口径不一致，首次保存会新建一行）。
                  </li>
                  <li>
                    <b>凭据</b>：GET 会把 <code>accessKey</code>/<code>accessSecret</code> 明文放进响应，历史实现还把
                    AccessKey 放在明文输入框；本页两者统一掩码回显，<b>只有点「重新输入」并填了值才提交该字段</b>。
                  </li>
                  <li>
                    <b>原「短信模板」表格与「发送统计」三卡已移除</b>：<code>/api/sms/templates</code> 与
                    <code>/api/sms/stats</code> 后端零实现（实测 404）；且统计三卡历史上是写死的 0，属假数据。
                  </li>
                  <li>
                    <b>无审计</b>：保存短信凭据不写 <code>sys_audit_log</code>（凭据变更属高敏事件，事后不可还原）。
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
                title="短信服务商"
                tip="服务商决定凭据字段的含义与签名报备渠道；签名未报备时运营商会直接报错"
              >
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="服务商"
                      name="provider"
                    >
                      <a-select
                        v-model:value="form.provider"
                        placeholder="请选择短信服务商"
                        allow-clear
                        :options="PROVIDER_OPTIONS"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="短信签名"
                      name="signName"
                    >
                      <a-input
                        v-model:value="form.signName"
                        placeholder="如：AI-Ready（须与服务商备案一致）"
                        :maxlength="64"
                        allow-clear
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="24">
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
                  <a-col :span="12">
                    <div class="field-tip">
                      服务商值域不一致：前端历史 4 项（含 <code>qiniu</code>）、后端 Schema 注释只声明 3 项
                      （<code>aliyun/tencent/huawei</code>）。为不丢历史配置，本页保留 4 项，但后端对
                      <code>qiniu</code> 无任何校验或分支 —— 选它不会被拒绝，也不会被真正支持。
                    </div>
                  </a-col>
                </a-row>
              </FormSection>

              <FormSection
                title="访问凭据"
                tip="凭据只在服务商控制台生成；本页不回显明文，改动须整串重新输入"
              >
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="AccessKey"
                      name="accessKey"
                    >
                      <!-- 历史上 AccessKey 用的是明文 a-input（开发文档 §12 P0-⑤）→ 与 Secret 同级掩码处理 -->
                      <SecretInput
                        v-model:value="secret.accessKey.draft"
                        v-model:editing="secret.accessKey.editing"
                        :has-stored="secret.accessKey.hasStored"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="AccessSecret"
                      name="accessSecret"
                    >
                      <SecretInput
                        v-model:value="secret.accessSecret.draft"
                        v-model:editing="secret.accessSecret.editing"
                        :has-stored="secret.accessSecret.hasStored"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="24">
                  <a-col :span="24">
                    <div class="field-tip">
                      凭据落库为明文列（后端实体无加密注解、无 <code>@JsonIgnore</code>）。
                      文档建议的 <code>ENCv1:</code> AES-GCM 加密落库属<b>后端改造</b>，本页只能在展示层做掩码；
                      在加密落库落地前，请按「高敏凭据」管理该库的访问权限。
                    </div>
                  </a-col>
                </a-row>
              </FormSection>
            </a-form>
          </a-spin>
        </div>

        <!-- 底部操作区：保存 / 重置 / 测试短信 -->
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
              title="用当前输入请求后端 /sms/test（校验配置完整性 + 服务商端点 TCP 连通性，不发短信）"
            >
              <a-button
                :loading="testing"
                :disabled="saving"
                @click="handleTest"
              >
                测试短信
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
import { requiredRule, requiredSelectRule } from '@/utils/formRules'
import { platformSmsConfigApi, type PlatformSmsConfig } from '@/api/admin'

defineOptions({ name: 'AdminPlatformSms' })

/** 掩码串（与 SecretInput 内部常量一致）：提交前最后一道拦截，掩码绝不落库 */
const SECRET_MASK = '******'

/** 服务商选项（保留历史 4 项，避免丢配置；qiniu 后端未支持，见页面说明） */
const PROVIDER_OPTIONS = [
  { label: '阿里云短信', value: 'aliyun' },
  { label: '腾讯云短信', value: 'tencent' },
  { label: '华为云短信', value: 'huawei' },
  { label: '七牛云短信（后端未支持）', value: 'qiniu' }
]

/** 凭据字段状态：hasStored（后端有没有值）+ editing（用户是否主动重录）+ draft（草稿） */
interface SecretState { hasStored: boolean; editing: boolean; draft: string }

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const loadError = ref('')
const formRef = ref<FormInstance>()

/** 表单字段（= 后端 SmsConfig 的可编辑列） */
const form = reactive({
  provider: undefined as string | undefined,
  signName: '',
  enabled: false
})

const secret = reactive<{ accessKey: SecretState; accessSecret: SecretState }>({
  accessKey: { hasStored: false, editing: false, draft: '' },
  accessSecret: { hasStored: false, editing: false, draft: '' }
})

const rules: Record<string, Rule[]> = {
  provider: [requiredSelectRule('服务商')],
  signName: [requiredRule('短信签名'), { max: 64, message: '短信签名最长 64 个字符', trigger: 'blur' }]
}

/** 服务端值 → 表单；凭据只取「有没有」，明文一律不进表单（掩码回显） */
function applyConfig(data: PlatformSmsConfig) {
  form.provider = data.provider || undefined
  form.signName = data.signName ?? ''
  form.enabled = data.enabled === true
  secret.accessKey.hasStored = !!(data.accessKey && data.accessKey !== '')
  secret.accessKey.draft = ''
  secret.accessKey.editing = false
  secret.accessSecret.hasStored = !!(data.accessSecret && data.accessSecret !== '')
  secret.accessSecret.draft = ''
  secret.accessSecret.editing = false
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

/** 表单 → 提交体（字段名与后端 SmsConfig 逐字一致） */
function buildPayload(): Record<string, any> {
  const payload: Record<string, any> = {
    provider: form.provider,
    signName: form.signName.trim(),
    // enabled 必须显式回传：后端是原始 boolean，缺省即 false 且 updateById 不会跳过
    enabled: form.enabled
  }
  const accessKey = resolveSecret(secret.accessKey)
  if (accessKey !== undefined) payload.accessKey = accessKey
  const accessSecret = resolveSecret(secret.accessSecret)
  if (accessSecret !== undefined) payload.accessSecret = accessSecret
  return payload
}

/** 读取配置；失败返回 null（此时保留旧值，绝不用假数据兜底） */
async function loadConfig(options?: { silent?: boolean }): Promise<PlatformSmsConfig | null> {
  loading.value = true
  try {
    const data = (await platformSmsConfigApi.load()) as PlatformSmsConfig
    applyConfig(data || {})
    loadError.value = ''
    return data
  } catch (error: any) {
    console.error('[短信配置] 读取配置失败', error)
    const msg = error?.message || '读取短信配置失败'
    loadError.value = msg
    if (!options?.silent) message.error(msg)
    return null
  } finally {
    loading.value = false
  }
}

/** 提交值 vs 回读值逐字段比对，返回不一致项的中文描述 */
function diffAgainstReadback(payload: Record<string, any>, back: PlatformSmsConfig): string[] {
  const diffs: string[] = []
  if ((back.provider ?? '') !== (payload.provider ?? '')) diffs.push(`服务商 提交「${payload.provider ?? ''}」/ 回读「${back.provider ?? ''}」`)
  if ((back.signName ?? '') !== payload.signName) diffs.push(`短信签名 提交「${payload.signName}」/ 回读「${back.signName ?? ''}」`)
  if ((back.enabled === true) !== (payload.enabled === true)) diffs.push('启用状态未按提交值保存')
  if (payload.accessKey !== undefined && (back.accessKey ?? '') !== payload.accessKey) diffs.push('AccessKey 未被后端保存为新提交的值')
  if (payload.accessSecret !== undefined && (back.accessSecret ?? '') !== payload.accessSecret) diffs.push('AccessSecret 未被后端保存为新提交的值')
  if (payload.accessKey === undefined && !!(back.accessKey && back.accessKey !== '') !== secret.accessKey.hasStored) {
    diffs.push('AccessKey 存在性在保存前后发生变化')
  }
  if (payload.accessSecret === undefined && !!(back.accessSecret && back.accessSecret !== '') !== secret.accessSecret.hasStored) {
    diffs.push('AccessSecret 存在性在保存前后发生变化')
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
    const result = await platformSmsConfigApi.save(payload)
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
        message.success('短信配置已保存（已回读核对一致）')
      }
    }
  } catch (error: any) {
    console.error('[短信配置] 保存失败', error)
    message.error(error?.message || '保存短信配置失败')
  } finally {
    saving.value = false
  }
}

/** 测试短信：不做表单校验（允许测当前输入），但必须读 success 并如实提示 */
async function handleTest() {
  testing.value = true
  try {
    const result = await platformSmsConfigApi.test(buildPayload())
    if (result.success) {
      // 后端已于 2026-09-19 改为真实探测（配置完整性 + 服务商端点 TCP 建连）；
      // 成功文案本身已写明「仅网络连通、未校验凭据」，此处不再二次加警告，避免与后端文案重复
      message.success(`测试通过：${result.message}`)
    } else {
      message.error(`测试失败：${result.message}`)
    }
  } catch (error: any) {
    console.error('[短信配置] 测试短信请求失败', error)
    message.error(error?.message || '测试短信请求失败')
  } finally {
    testing.value = false
  }
}

/** 重置：丢弃未保存的修改，重新从后端读取（不沿用「重置=置空」的历史语义） */
function handleReset() {
  loadConfig()
}

function handleError(error: Error) {
  console.error('[短信配置] 页面错误', error)
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
