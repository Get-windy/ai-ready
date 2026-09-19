<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        存储配置（系统 → 平台设置 → 存储配置，菜单 62504）
        · 平台控制台页面：ql361 无对标 → 按 Odoo 「Filestore / Storage」+ 对象存储控制台建模
        · 后端 cn.aiedge.platform.controller.StorageConfigController（前缀 /api/storage-config），表 sys_storage_config
          🔴 历史实现的三个请求打的是 /api/storage/**（后端是 /api/storage-config/**，且前者是活的文件读写端点）
             → 整页 100% 404；本次已改为正确前缀。
        · 本页是**配置表单页**（单实例配置），不是列表页 → PageContainer + FormSection 分区卡片
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/存储配置开发文档.md
        · 与文档的时点差异（2026-09-19 实测）：文档 §6 记的「3 个端点全部 404」是「路径错 + 包未装配」
          双缺陷时期的旧结论；装配补齐后改用 /storage-config 前缀即 200
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
                    <b>本页配置基本不影响真实存储</b>：只有 <code>local_path</code> 有唯一一个消费方 ——
                    文件访问端点 <code>FileAccessController</code> 的 <code>getBasePath()</code>
                    （按会话租户查 DB，查不到才回退 yml）。
                    <b>上传落盘走的是 LocalFileStorageService，只读 yml 的 storage.local.base-path，不读本表</b>。
                    <code>storage_type</code> / <code>endpoint</code> / <code>bucket</code> /
                    <code>access_key</code> / <code>access_secret</code> / <code>local_url_prefix</code>
                    <b>全仓无任何消费方</b>（对象存储策略读的是 yml <code>StorageProperties</code>）。
                  </li>
                  <li>
                    <b>「测试连接」已改为真实探测</b>（2026-09-19），按存储类型分流：
                    本地存储 —— 对 <code>local_path</code> 做<b>真实写入探测</b>（目录不存在则创建，
                    写入随机探测文件后删除）；只有真写得进去才通过（仅判断"目录存在"会漏掉只读/磁盘满）。
                    对象存储 —— 解析 <code>endpoint</code> 做 <b>TCP 建连探测</b>（5s 超时）。
                    边界：本系统<b>未集成对象存储 SDK</b>，故不校验 AccessKey/Secret 与 bucket 是否存在，
                    返回文案会写明这一点。
                  </li>
                  <li>
                    <b>切换存储方式是高风险动作</b>：已上传文件不会随之迁移，且本页的 <code>storage_type</code>
                    无消费方 → 切换后「看起来生效、实际不生效」。本页保存时会对「存储方式发生变化」做二次确认。
                  </li>
                  <li>
                    <b>凭据</b>：GET 会把 <code>accessKey</code>/<code>accessSecret</code> 明文放进响应，
                    历史实现还把 AccessKey 放在明文输入框；本页两者统一掩码回显，
                    <b>只有点「重新输入」并填了值才提交该字段</b>。
                  </li>
                  <li>
                    <b><code>local_path</code> 明文展示</b>：该值是服务器本地绝对路径（含基础设施信息），
                    本页不做脱敏（它对运维排障是必要信息），但请注意本页的可见范围。
                  </li>
                  <li>
                    <b>备份范围提示</b>：平台的「备份管理」备份范围<b>包含文件存储目录</b>，
                    改这里的根路径前请先确认备份策略与磁盘容量。
                  </li>
                  <li>
                    <b>原「存储概览」三卡已移除</b>：历史实现是三个硬编码常量
                    （<code>12.5 GB</code> / <code>45230</code> / <code>156</code>），后端无任何统计端点，属纯假数据。
                  </li>
                  <li>
                    <b>无审计</b>：保存存储配置（含凭据与路径）不写 <code>sys_audit_log</code>。
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
                title="存储方式"
                tip="切换存储方式属高风险动作：已上传文件不会随之迁移，请先确认迁移方案"
              >
                <a-row :gutter="24">
                  <a-col :span="24">
                    <a-form-item
                      label="存储方式"
                      name="storageType"
                    >
                      <a-radio-group v-model:value="form.storageType">
                        <a-radio
                          v-for="item in STORAGE_TYPE_OPTIONS"
                          :key="item.value"
                          :value="item.value"
                        >
                          {{ item.label }}
                        </a-radio>
                      </a-radio-group>
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
                      存储类型值域不一致：前端历史 5 项、后端 Schema 注释只声明 4 项
                      （<code>local/oss/cos/s3</code>，且与前端命名不同）。为不丢历史配置本页保留 5 项，
                      但后端不做校验、也无消费方 —— 选择结果只落库。
                    </div>
                  </a-col>
                </a-row>
              </FormSection>

              <FormSection
                v-if="form.storageType === 'local'"
                title="本地存储"
                tip="根路径须为服务器上的绝对路径；它同时决定备份范围中的文件目录"
              >
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="存储路径"
                      name="localPath"
                    >
                      <a-input
                        v-model:value="form.localPath"
                        placeholder="如 I:\AI-Ready-file 或 /data/files"
                        :maxlength="255"
                        allow-clear
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="访问URL前缀"
                      name="localUrlPrefix"
                    >
                      <a-input
                        v-model:value="form.localUrlPrefix"
                        placeholder="如 /files"
                        :maxlength="128"
                        allow-clear
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="24">
                  <a-col :span="24">
                    <div class="field-tip">
                      ⚠ 本页读取配置时会把 <code>local_path</code> 用于文件访问端点，
                      但上传落盘目录取的是 yml 的 <code>storage.local.base-path</code>
                      → 两处不一致时会出现「上传成功但预览 404」（或反之）。改这里之前请同步确认 yml。
                    </div>
                  </a-col>
                </a-row>
              </FormSection>

              <FormSection
                v-else
                title="对象存储"
                tip="以下四项按后端语义为条件必填：切换到对象存储后必须完整填写"
              >
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="Endpoint"
                      name="endpoint"
                    >
                      <a-input
                        v-model:value="form.endpoint"
                        placeholder="如 oss-cn-hangzhou.aliyuncs.com"
                        :maxlength="255"
                        allow-clear
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="Bucket"
                      name="bucket"
                    >
                      <a-input
                        v-model:value="form.bucket"
                        placeholder="存储桶名称"
                        :maxlength="128"
                        allow-clear
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

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
                      ⚠ 本页只做<b>配置落库</b>：对象存储的读写策略由 yml <code>StorageProperties</code> 决定，
                      改这里不会切换真实存储引擎。凭据落库为明文列（后端无加密注解），
                      <code>ENCv1:</code> 加密落库属后端改造。
                    </div>
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
              title="用当前输入请求后端 /storage-config/test（本地存储做写入探测，对象存储做端点连通探测）"
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
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SaveOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FormSection from '@/components/FormSection/index.vue'
import SecretInput from '@/components/SecretInput/index.vue'
import { requiredRule, requiredSelectRule } from '@/utils/formRules'
import { platformStorageConfigApi, type PlatformStorageConfig } from '@/api/admin'

defineOptions({ name: 'AdminPlatformStorage' })

/** 掩码串（与 SecretInput 内部常量一致）：提交前最后一道拦截，掩码绝不落库 */
const SECRET_MASK = '******'

/** 存储方式选项（保留历史 5 项，避免丢配置；后端 Schema 注释为 local/oss/cos/s3） */
const STORAGE_TYPE_OPTIONS = [
  { label: '本地存储', value: 'local' },
  { label: '阿里云OSS', value: 'aliyun' },
  { label: '腾讯云COS', value: 'tencent' },
  { label: '七牛云Kodo', value: 'qiniu' },
  { label: 'MinIO', value: 'minio' }
]

/** 凭据字段状态：hasStored（后端有没有值）+ editing（用户是否主动重录）+ draft（草稿） */
interface SecretState { hasStored: boolean; editing: boolean; draft: string }

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const loadError = ref('')
const formRef = ref<FormInstance>()
/** 最近一次成功读取到的存储方式（用于保存前的「高风险变更」判定） */
const loadedStorageType = ref<string | undefined>(undefined)

/** 表单字段（= 后端 StorageConfig 的可编辑列） */
const form = reactive({
  storageType: 'local' as string | undefined,
  localPath: '',
  localUrlPrefix: '',
  endpoint: '',
  bucket: '',
  enabled: false
})

const secret = reactive<{ accessKey: SecretState; accessSecret: SecretState }>({
  accessKey: { hasStored: false, editing: false, draft: '' },
  accessSecret: { hasStored: false, editing: false, draft: '' }
})

/** 是否走对象存储分支（决定条件必填是否生效） */
function isRemoteStorage(): boolean {
  return form.storageType !== 'local'
}

/**
 * 校验规则：对象存储分支的四项按「条件必填」处理 —— 用 computed 让规则随分支切换立即重算，
 * 否则切到对象存储后仍按非必填放过空值（历史实现就是零校验，见开发文档 §12 P1-⑬）。
 */
function buildRules(): Record<string, Rule[]> {
  const remote = isRemoteStorage()
  return {
    storageType: [requiredSelectRule('存储方式')],
    localPath: remote ? [] : [requiredRule('存储路径'), { max: 255, message: '存储路径最长 255 个字符', trigger: 'blur' }],
    localUrlPrefix: remote ? [] : [{ max: 128, message: '访问URL前缀最长 128 个字符', trigger: 'blur' }],
    endpoint: remote ? [requiredRule('Endpoint'), { max: 255, message: 'Endpoint 最长 255 个字符', trigger: 'blur' }] : [],
    bucket: remote ? [requiredRule('Bucket'), { max: 128, message: 'Bucket 最长 128 个字符', trigger: 'blur' }] : []
  }
}

const rules = computed<Record<string, Rule[]>>(() => buildRules())

// 切换分支后清掉另一分支残留的标红（否则「存储路径」的必填红字会跟着到对象存储分支）
watch(() => form.storageType, () => {
  formRef.value?.clearValidate()
})

/** 服务端值 → 表单；凭据只取「有没有」，明文一律不进表单（掩码回显） */
function applyConfig(data: PlatformStorageConfig) {
  form.storageType = data.storageType || 'local'
  form.localPath = data.localPath ?? ''
  form.localUrlPrefix = data.localUrlPrefix ?? ''
  form.endpoint = data.endpoint ?? ''
  form.bucket = data.bucket ?? ''
  form.enabled = data.enabled === true
  loadedStorageType.value = data.storageType || 'local'
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

/**
 * 表单 → 提交体（字段名与后端 StorageConfig 逐字一致）。
 * 按当前分支只提交相关字段：切换存储方式时把无关字段显式提交为空串（不是省略），
 * 避免「切回本地后上一轮的对象存储凭据仍留在库里」的字段互相污染（开发文档 §12 P1-⑬）。
 */
function buildPayload(): Record<string, any> {
  const remote = isRemoteStorage()
  const payload: Record<string, any> = {
    storageType: form.storageType,
    // enabled 必须显式回传：后端是原始 boolean，缺省即 false 且 updateById 不会跳过
    enabled: form.enabled,
    localPath: remote ? '' : form.localPath.trim(),
    localUrlPrefix: remote ? '' : form.localUrlPrefix.trim(),
    endpoint: remote ? form.endpoint.trim() : '',
    bucket: remote ? form.bucket.trim() : ''
  }
  // 凭据只在录入态提交；切到非对象存储分支时，若用户没有重新输入则保留库中原值（不主动清空凭据，
  // 因为凭据是运维资产，清空会造成不可逆损失）
  const accessKey = resolveSecret(secret.accessKey)
  if (accessKey !== undefined) payload.accessKey = accessKey
  const accessSecret = resolveSecret(secret.accessSecret)
  if (accessSecret !== undefined) payload.accessSecret = accessSecret
  return payload
}

/** 读取配置；失败返回 null（此时保留旧值，绝不用假数据兜底） */
async function loadConfig(options?: { silent?: boolean }): Promise<PlatformStorageConfig | null> {
  loading.value = true
  try {
    const data = (await platformStorageConfigApi.load()) as PlatformStorageConfig
    applyConfig(data || {})
    loadError.value = ''
    return data
  } catch (error: any) {
    console.error('[存储配置] 读取配置失败', error)
    const msg = error?.message || '读取存储配置失败'
    loadError.value = msg
    if (!options?.silent) message.error(msg)
    return null
  } finally {
    loading.value = false
  }
}

/** 提交值 vs 回读值逐字段比对，返回不一致项的中文描述 */
function diffAgainstReadback(payload: Record<string, any>, back: PlatformStorageConfig): string[] {
  const diffs: string[] = []
  if ((back.storageType ?? '') !== (payload.storageType ?? '')) diffs.push(`存储方式 提交「${payload.storageType ?? ''}」/ 回读「${back.storageType ?? ''}」`)
  if ((back.enabled === true) !== (payload.enabled === true)) diffs.push('启用状态未按提交值保存')
  // 分支字段：只比对本次真正提交（非空）的那些
  if (payload.localPath && (back.localPath ?? '') !== payload.localPath) diffs.push(`存储路径 提交「${payload.localPath}」/ 回读「${back.localPath ?? ''}」`)
  if (payload.localUrlPrefix && (back.localUrlPrefix ?? '') !== payload.localUrlPrefix) diffs.push(`访问URL前缀 提交「${payload.localUrlPrefix}」/ 回读「${back.localUrlPrefix ?? ''}」`)
  if (payload.endpoint && (back.endpoint ?? '') !== payload.endpoint) diffs.push(`Endpoint 提交「${payload.endpoint}」/ 回读「${back.endpoint ?? ''}」`)
  if (payload.bucket && (back.bucket ?? '') !== payload.bucket) diffs.push(`Bucket 提交「${payload.bucket}」/ 回读「${back.bucket ?? ''}」`)
  if (payload.accessKey !== undefined && (back.accessKey ?? '') !== payload.accessKey) diffs.push('AccessKey 未被后端保存为新提交的值')
  if (payload.accessSecret !== undefined && (back.accessSecret ?? '') !== payload.accessSecret) diffs.push('AccessSecret 未被后端保存为新提交的值')
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

  // 存储方式变化属高风险动作 → 二次确认（已上传文件不会随配置迁移）
  if (loadedStorageType.value && form.storageType !== loadedStorageType.value) {
    const from = STORAGE_TYPE_OPTIONS.find(o => o.value === loadedStorageType.value)?.label || loadedStorageType.value
    const to = STORAGE_TYPE_OPTIONS.find(o => o.value === form.storageType)?.label || form.storageType
    const confirmed = await new Promise<boolean>((resolve) => {
      Modal.confirm({
        title: '确认切换存储方式？',
        content: `即将把存储方式从「${from}」改为「${to}」。已上传的文件不会随之迁移，且该字段当前无消费方（真实存储引擎由 yml 决定）—— 请确认已有人工迁移方案。`,
        okText: '确认切换',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => resolve(true),
        onCancel: () => resolve(false)
      })
    })
    if (!confirmed) return
  }

  saving.value = true
  try {
    const payload = buildPayload()
    const result = await platformStorageConfigApi.save(payload)
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
        message.success('存储配置已保存（已回读核对一致）')
      }
    }
  } catch (error: any) {
    console.error('[存储配置] 保存失败', error)
    message.error(error?.message || '保存存储配置失败')
  } finally {
    saving.value = false
  }
}

/** 测试连接：不做表单校验（允许测当前输入），但必须读 success 并如实提示 */
async function handleTest() {
  testing.value = true
  try {
    const result = await platformStorageConfigApi.test(buildPayload())
    if (result.success) {
      // 后端已于 2026-09-19 改为真实探测（本地存储做写入探测 / 对象存储做端点 TCP 建连）
      message.success(`测试通过：${result.message}`)
    } else {
      message.error(`测试失败：${result.message}`)
    }
  } catch (error: any) {
    console.error('[存储配置] 测试连接请求失败', error)
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
  console.error('[存储配置] 页面错误', error)
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
