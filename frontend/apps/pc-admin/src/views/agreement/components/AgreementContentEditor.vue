<template>
  <div class="acontent">
    <a-spin :spinning="loading">
      <!-- ═══ 没有查看权限：说清楚，而不是把空列表当成"平台没定义" ═══ -->
      <a-alert
        v-if="noPerm"
        type="warning"
        show-icon
        class="ac-alert"
        message="没有查看「协议内容」的权限"
        description="当前账号缺少协议内容查看权限，因此看不到字段设定版与文字条款。这不影响你查看条款、签署与终止状态；需要查看或编辑内容请联系管理员开通。"
      />

      <!-- ═══ 只读原因 ═══ -->
      <a-alert
        v-else-if="content && !content.editable"
        type="info"
        show-icon
        class="ac-alert"
        message="本版内容只读，不能再修改"
        :description="content.editableHint || '已生效的版本不能再改。要调整约定，请发起变更：系统会复制出一份新的草稿版本，双方都确认后才生效，现行版本在此之前继续有效。'"
      />

      <!-- ═══ 是草稿但当前账号没有编辑权限 ═══ -->
      <a-alert
        v-else-if="content && content.editable && !canEdit"
        type="warning"
        show-icon
        class="ac-alert"
        message="这一版还是草稿，但当前账号没有编辑「协议内容」的权限"
        description="可以看到约定情况，不能修改。需要改内容请联系有权限的同事，或让管理员开通协议内容编辑权限。"
      />

      <!-- ═══ 模板来源：模板是起点，不是默认值 ═══ -->
      <a-alert
        v-if="content?.templateSourceText"
        type="warning"
        show-icon
        class="ac-alert"
        message="本版内容来自模板预填 —— 模板不是默认值，仍需双方在本次这一版上确认"
        :description="templateNotice"
      />

      <!-- ═══ 必填项未清：明确列出还缺哪几项 ═══ -->
      <a-alert
        v-if="!noPerm && missingNames.length"
        type="error"
        show-icon
        class="ac-alert"
        message="还有必填设定项没有约定："
      >
        <template #description>
          <div class="ac-missing">
            <div>· {{ missingNames.join('、') }}</div>
            <div class="ac-missing-note">
              这些项系统不会按默认值执行：下游环节（订单路由 / 发货 / 库存 / 定价 / 结算 / 开票 / 风控）
              在真正用到它们时会拦下并提示明确原因（例如「本协议未约定账期，无法生成应收到期日」）。
              草稿阶段允许先空着，但要能用，得先约定。
            </div>
          </div>
        </template>
      </a-alert>

      <!-- ═══ 一、字段设定版：运行时配置，不是普通表单 ═══ -->
      <a-card
        v-if="!noPerm"
        size="small"
        class="ac-card"
        title="字段设定版"
      >
        <template #extra>
          <span class="ac-card-sub">这一版约定后会直接改变双方的物流 / 钱流 / 货流</span>
        </template>

        <a-alert
          type="info"
          show-icon
          class="ac-alert"
          message="这些是「运行时配置」，不是写在纸上的话"
          description="下面每一项都会被系统在业务发生的那一刻直接执行。所以每一项都标出了「会影响什么」；留空就是「未约定」，系统不会替你按默认值执行。"
        />

        <div
          v-if="!settingRows.length"
          class="ac-empty"
        >
          平台还没有定义可约定的字段（字段元数据为空）。请联系平台维护「协议设定字段」。
        </div>

        <div
          v-for="def in settingRows"
          :key="def.settingKey"
          class="ac-setting"
        >
          <div class="ac-setting-head">
            <span class="ac-setting-label">
              <span
                v-if="def.required"
                class="ac-star"
              >*</span>
              {{ def.label || def.settingKey }}
            </span>
            <a-tag color="default">
              {{ def.valueTypeLabel || SETTING_VALUE_TYPE_TEXT[def.valueType || ''] || '取值' }}
            </a-tag>
            <a-tag
              v-if="def.required"
              color="red"
            >
              必填
            </a-tag>
            <a-tag
              v-if="isTemplatePrefilled(def.settingKey)"
              color="orange"
            >
              模板预填，仍需双方确认
            </a-tag>
            <a-tag
              v-if="!isAgreed(def.settingKey)"
              color="volcano"
            >
              未约定
            </a-tag>
          </div>

          <div class="ac-setting-control">
            <!-- ENUM → 下拉（候选值来自平台字典，不给默认值） -->
            <a-select
              v-if="def.valueType === 'ENUM'"
              :value="values[def.settingKey] || undefined"
              size="small"
              class="ac-control"
              allow-clear
              :disabled="!canEdit"
              placeholder="未约定（不做这项约定）"
              :options="enumOptions(def)"
              @change="(v: any) => setValue(def.settingKey, v)"
            />
            <!-- NUMBER → 数字 -->
            <a-input-number
              v-else-if="def.valueType === 'NUMBER'"
              :value="numberValue(def.settingKey)"
              size="small"
              class="ac-control"
              :disabled="!canEdit"
              :placeholder="'未约定'"
              @change="(v: any) => setNumberValue(def.settingKey, v)"
            />
            <!-- DURATION → 天数 -->
            <a-input-number
              v-else-if="def.valueType === 'DURATION'"
              :value="numberValue(def.settingKey)"
              size="small"
              class="ac-control"
              :min="0"
              :precision="0"
              :disabled="!canEdit"
              placeholder="未约定"
              addon-after="天"
              @change="(v: any) => setNumberValue(def.settingKey, v)"
            />
            <!-- BOOL → 开关（并显式区分「未约定」与「否」） -->
            <template v-else-if="def.valueType === 'BOOL'">
              <a-switch
                :checked="values[def.settingKey] === '是'"
                size="small"
                :disabled="!canEdit"
                checked-children="是"
                un-checked-children="否"
                @change="(v: any) => setBoolValue(def.settingKey, v)"
              />
              <span
                v-if="values[def.settingKey] !== '是' && values[def.settingKey] !== '否'"
                class="ac-bool-hint"
              >
                未约定（系统不会按「否」执行，用到时会拦下）
              </span>
            </template>
            <!-- DATE → 日期 -->
            <a-date-picker
              v-else-if="def.valueType === 'DATE'"
              :value="dateValue(def.settingKey)"
              size="small"
              class="ac-control"
              value-format="YYYY-MM-DD"
              :disabled="!canEdit"
              placeholder="未约定"
              @change="(v: any) => setValue(def.settingKey, v)"
            />
            <!-- TEXT → 文本 -->
            <a-input
              v-else
              :value="values[def.settingKey] || ''"
              size="small"
              class="ac-control"
              :disabled="!canEdit"
              placeholder="未约定"
              @change="(e: any) => setValue(def.settingKey, e.target.value)"
            />

            <a-input
              :value="remarks[def.settingKey] || ''"
              size="small"
              class="ac-remark"
              :disabled="!canEdit"
              placeholder="备注（选填）"
              @change="(e: any) => (remarks[def.settingKey] = e.target.value)"
            />
          </div>

          <!-- ⚠️ 这一项会影响什么：字段设定版是运行时配置，填写的人必须先知道后果 -->
          <div class="ac-impact">
            <InfoCircleOutlined class="ac-impact-icon" />
            <span>
              <b>会影响什么：</b>
              <template v-if="def.consumerPointLabel">
                {{ def.consumerPointLabel }}
              </template>
              <template v-else>
                未登记消费方（请让平台补登记 —— 没有消费方的字段不该出现在这里）
              </template>
              <template v-if="def.consumerSemantics">
                —— {{ def.consumerSemantics }}
              </template>
            </span>
          </div>
          <div
            v-if="def.semantics"
            class="ac-semantics"
          >
            字段说明：{{ def.semantics }}
          </div>
        </div>

        <div class="ac-actions">
          <a-button
            v-if="canEdit"
            type="primary"
            size="small"
            :loading="saving"
            @click="handleSave"
          >
            保存本版内容
          </a-button>
          <span
            v-if="canEdit"
            class="ac-actions-hint"
          >
            保存会清空双方已有的确认痕迹（内容变了，原先的确认就不再对得上）。
          </span>
        </div>
      </a-card>

      <!-- ═══ 二、履约方式集合：可多种并存（不是单选） ═══ -->
      <a-card
        v-if="!noPerm"
        size="small"
        class="ac-card"
        title="履约方式集合"
      >
        <template #extra>
          <span class="ac-card-sub">可多选并存，空 = 本版未约定履约方式</span>
        </template>
        <a-checkbox-group
          :value="selectedModes"
          :disabled="!canEdit"
          class="ac-modes"
          @change="(v: any) => (selectedModes = v as string[])"
        >
          <div
            v-for="m in FULFILLMENT_MODE_OPTIONS"
            :key="m.value"
            class="ac-mode"
          >
            <a-checkbox :value="m.value">
              <span class="ac-mode-label">{{ m.label }}</span>
              <span class="ac-mode-desc">{{ m.desc }}</span>
            </a-checkbox>
          </div>
        </a-checkbox-group>
        <div class="ac-semantics">
          同一份协议里「同城直发 + 异地中转」可以并存 —— 这才是真实交易。
          下单时在这个集合里确定本单具体用哪一种。留空不表示「随便用哪种」，而是「本版没有约定履约方式」。
        </div>
      </a-card>

      <!-- ═══ 三、文字版：系统不会自动执行（硬提示） ═══ -->
      <a-card
        v-if="!noPerm"
        size="small"
        class="ac-card"
        title="文字条款"
      >
        <template #extra>
          <span class="ac-card-sub">只做双方确认与留痕举证，系统不解析、不自动执行</span>
        </template>

        <a-alert
          type="warning"
          show-icon
          class="ac-alert"
          message="此类条款系统不会自动执行，需人工处理"
          :description="narrativeNotice"
        />

        <div
          v-for="sec in NARRATIVE_SECTION_OPTIONS"
          :key="sec.value"
          class="ac-narrative"
        >
          <div class="ac-narrative-head">
            <span class="ac-setting-label">{{ sec.label }}</span>
            <a-tag
              v-if="isTemplatePrefilledNarrative(sec.value)"
              color="orange"
            >
              模板预填，仍需双方确认
            </a-tag>
            <span
              v-if="narrativeHash(sec.value)"
              class="ac-hash"
              :title="'正文哈希（证明确认之后这段文字没被改过）：' + narrativeHash(sec.value)"
            >
              已留痕哈希 {{ String(narrativeHash(sec.value)).slice(0, 12) }}…
            </span>
          </div>
          <a-textarea
            v-model:value="narratives[sec.value]"
            :rows="3"
            :maxlength="2000"
            show-count
            :disabled="!canEdit"
            :placeholder="sec.placeholder"
          />
          <div class="ac-semantics">
            这里写的内容<strong>不会</strong>被系统执行，也不会自动扣款 / 判责 —— 出事时它是双方举证与
            司法解决的依据。要让系统自动执行，请用上面的「字段设定版」。
          </div>
        </div>

        <div class="ac-actions">
          <a-button
            v-if="canEdit"
            type="primary"
            size="small"
            :loading="saving"
            @click="handleSave"
          >
            保存本版内容
          </a-button>
        </div>
      </a-card>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
/**
 * 协议内容编辑器：字段设定版 + 文字版 + 履约方式集合（§13.1 ~ §13.2、§13.10）
 *
 * 这个组件刻意不按「普通表单」做，理由（§13.1 最容易做错的地方）：
 *   · 字段设定版是**运行时配置**，会被订单路由 / 发货 / 库存 / 定价 / 结算 / 开票 / 风控直接消费，
 *     所以每一项都必须显示「会影响什么」（consumerPointLabel + consumerSemantics）；
 *   · 文字条款**永不自动执行**，界面必须显式告知「此类条款系统不会自动执行，需人工处理」——
 *     这句提示由服务端下发（autoExecutable / manualNotice），不在前端写死，口径只有一处；
 *   · 「未约定」是三态之一，必须显示成「未约定」，不能显示成 0 或空；
 *   · 履约方式是**集合**（可多种并存），用多选控件，不能做成单选。
 */
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { InfoCircleOutlined } from '@ant-design/icons-vue'
import { usePermission } from '@/composables/usePermission'
import {
  agreementApi,
  FULFILLMENT_MODE_OPTIONS,
  NARRATIVE_SECTION_OPTIONS,
  NARRATIVE_MANUAL_NOTICE_FALLBACK,
  SETTING_VALUE_TYPE_TEXT,
  settingEnumOptionLabel,
  type AgreementContent,
  type AgreementContentSaveResult,
  type AgreementSettingDef
} from '@/api/agreement'

const { checkPermission } = usePermission()

const props = withDefaults(defineProps<{
  /** 要编辑的版本 ID（草稿才可改） */
  versionId?: number | string | null
  /** 从模板发起时：模板带过来的设定项编码（用于打「模板预填，仍需双方确认」标） */
  templateSettingKeys?: string[]
  /** 从模板发起时：模板带过来的文字段落编码 */
  templateNarrativeKeys?: string[]
  /** 模板来源人读说明（服务端下发，如「基于模板「X」（平台模板）起草，第 1 版」） */
  templateSourceText?: string
}>(), {
  versionId: null,
  templateSettingKeys: () => [],
  templateNarrativeKeys: () => [],
  templateSourceText: ''
})

const emit = defineEmits<{
  'saved': [result: AgreementContentSaveResult | null]
}>()

const loading = ref(false)
const saving = ref(false)
const content = ref<AgreementContent | null>(null)

/** 设定字段编码 → 取值（**字符串形态**，'' = 未约定）；未约定的项必须显示成「未约定」 */
const values = reactive<Record<string, string>>({})
const remarks = reactive<Record<string, string>>({})
/** 文字条款：段落编码 → 正文 */
const narratives = reactive<Record<string, string>>({})
const narrativeHashes = reactive<Record<string, string>>({})
/** 履约方式集合（多选） */
const selectedModes = ref<string[]>([])

/**
 * 履约方式这一项在设定字典里同时存在一个 ENUM 字段，但**集合语义的值落在履约方式表**
 * （§13.10：一版多行并存）。因此它不在这里当单选渲染，统一由下面的「履约方式集合」多选承担，
 * 避免出现"两个地方各存一份、以哪个为准"。
 */
const FULFILLMENT_MODES_KEY = 'FULFILLMENT_MODES'

/** 没有「协议内容查看」权限：不发请求，直接说明原因（避免只见空表单以为平台没定义） */
const noPerm = ref(false)
const canView = computed(() => checkPermission('agreement:content:view'))
/** 版本是可改的草稿 */
const editable = computed(() => !!content.value?.editable)
/** 能不能真的动手改：草稿 + 有编辑权限（后端也会各自再校验一次） */
const canEdit = computed(() => editable.value && checkPermission('agreement:content:edit'))

/** 设定项渲染顺序：按元数据 sort 排，并剔除由「履约方式集合」承担的那一项 */
const settingRows = computed<AgreementSettingDef[]>(() => {
  const defs = content.value?.settingDefs || []
  return [...defs]
    .filter(d => d.settingKey !== FULFILLMENT_MODES_KEY)
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
})

const narrativeNotice = computed(() =>
  content.value?.narratives?.find(n => !!n.manualNotice)?.manualNotice
  || NARRATIVE_MANUAL_NOTICE_FALLBACK
)

/** 模板来源提示：把"模板不是默认值"这句话讲到点上 */
const templateNotice = computed(() =>
  `${props.templateSourceText || content.value?.templateSourceText || ''}。`
  + '模板只是替你填好了起点，模板里有**不等于**已经约定 —— 是否算约定，只以双方在本次这一版上确认并签署的内容为准。'
)

function isTemplatePrefilled(key: string): boolean {
  return props.templateSettingKeys.includes(key)
}
function isTemplatePrefilledNarrative(code: string): boolean {
  return props.templateNarrativeKeys.includes(code)
}

function isAgreed(key: string): boolean {
  const v = values[key]
  return v !== undefined && v !== null && String(v).trim() !== ''
}

/** ENUM 的候选值：编码 → 中文白话（口径在 api/agreement.ts 的 SETTING_ENUM_OPTION_TEXT，一处收口） */
function enumOptions(def: AgreementSettingDef) {
  return (def.options || []).map(code => ({
    value: code,
    label: settingEnumOptionLabel(def.settingKey, code)
  }))
}

// ── 取值读写 ──
function setValue(key: string, v: any) {
  values[key] = v === undefined || v === null ? '' : String(v)
}
function setNumberValue(key: string, v: any) {
  values[key] = v === undefined || v === null || v === '' ? '' : String(v)
}
function setBoolValue(key: string, checked: any) {
  values[key] = checked ? '是' : '否'
}
function numberValue(key: string): number | undefined {
  const v = values[key]
  if (v === undefined || v === '' || v === null) return undefined
  const n = Number(v)
  return Number.isNaN(n) ? undefined : n
}
function dateValue(key: string): string | undefined {
  return values[key] || undefined
}
function narrativeHash(code: string): string {
  return narrativeHashes[code] || ''
}

/**
 * 必填但还没约定的项（明确列出名字，不要说「条件不满足」）。
 *
 * ⚠️ 口径说明（§13.3 补充口径 2）：**必填设定不阻断签署，阻断的是「使用」**。
 * 所以这里只用于「提示 + 发起向导里拦住下一步」，不用来拦「置为生效」。
 */
const missingNames = computed<string[]>(() => {
  const out: string[] = []
  for (const def of settingRows.value) {
    if (!def.required) continue
    if (!isAgreed(def.settingKey)) out.push(def.label || def.settingKey)
  }
  // 履约方式集合承担了字典里那一项（集合语义落在履约方式表），它空着就是没约定；
  // 名字取字典上的字段名，不在前端另起一个叫法
  if (!selectedModes.value.length) {
    const modeDef = (content.value?.settingDefs || []).find(d => d.settingKey === FULFILLMENT_MODES_KEY)
    out.push(modeDef?.label || '履约方式集合')
  }
  return out
})

// ── 加载 ──
function syncFromContent(c: AgreementContent) {
  for (const key of Object.keys(values)) delete values[key]
  for (const key of Object.keys(remarks)) delete remarks[key]
  for (const key of Object.keys(narratives)) delete narratives[key]
  for (const key of Object.keys(narrativeHashes)) delete narrativeHashes[key]

  for (const item of c.settings || []) {
    values[item.settingKey] = item.value === null || item.value === undefined ? '' : String(item.value)
    if (item.remark) remarks[item.settingKey] = item.remark
  }
  for (const n of c.narratives || []) {
    narratives[n.sectionCode] = n.contentText || ''
    if (n.contentHash) narrativeHashes[n.sectionCode] = n.contentHash
  }
  selectedModes.value = (c.fulfillmentModes || []).map(m => m.mode)
}

async function reload() {
  if (!props.versionId) {
    content.value = null
    return
  }
  // 没有查看权限时不发请求：否则每次进页面都会弹一个"没有操作权限"的错误，
  // 而这不是故障、是权限边界，应当用一句说明代替报错
  if (!canView.value) {
    noPerm.value = true
    content.value = null
    return
  }
  noPerm.value = false
  loading.value = true
  try {
    const res: any = await agreementApi.getContent(props.versionId)
    const c: AgreementContent = res?.data ?? res ?? null
    content.value = c
    if (c) syncFromContent(c)
  } catch (error: any) {
    console.warn('[协议内容] 加载失败', error)
    content.value = null
  } finally {
    loading.value = false
  }
}

// ── 保存（整份覆盖） ──
async function handleSave(): Promise<boolean> {
  if (!props.versionId) {
    message.warning('还没有可编辑的版本：请先在详情页「发起变更」新建草稿版本')
    return false
  }
  if (!editable.value) {
    message.warning(content.value?.editableHint || '已生效的版本不能再改，请先发起变更')
    return false
  }
  if (!canEdit.value) {
    message.warning('当前账号没有编辑协议内容的权限')
    return false
  }
  saving.value = true
  try {
    const body = {
      // 未出现 / 留空 ⇒ 回到「未约定」（不是保持原值）
      settings: settingRows.value.map(def => ({
        settingKey: def.settingKey,
        value: values[def.settingKey] === undefined ? '' : values[def.settingKey],
        remark: remarks[def.settingKey] || undefined
      })),
      narratives: NARRATIVE_SECTION_OPTIONS.map(s => ({
        sectionCode: s.value,
        contentText: narratives[s.value] || ''
      })),
      fulfillmentModes: [...selectedModes.value]
    }
    const res: any = await agreementApi.saveContent(props.versionId, body)
    const result: AgreementContentSaveResult = res?.data ?? res ?? null
    message.success('本版内容已保存')
    if (result?.narrativeNotice) {
      // 保存成功也把「文字条款不会自动执行」再说一次（§13.2 要求界面显式告知）
      message.info(result.narrativeNotice, 6)
    }
    await reload()
    emit('saved', result)
    return true
  } catch (error: any) {
    console.warn('[协议内容] 保存失败', error)
    return false
  } finally {
    saving.value = false
  }
}

onMounted(reload)
watch(() => props.versionId, reload)

/** 供父级（发起向导 / 详情页）读取缺项与触发保存 */
defineExpose({
  /** 还缺哪几项必填（明确的名字清单） */
  missingNames,
  /** 保存（返回是否成功） */
  save: handleSave,
  /** 重新加载 */
  reload
})

defineOptions({ name: 'AgreementContentEditor' })
</script>

<style scoped>
.ac-alert { margin-bottom: 10px; }
.ac-card { margin-bottom: 12px; }
.ac-card :deep(.ant-card-body) { padding: 12px 16px; }
.ac-card-sub { font-size: 12px; color: #999; }
.ac-empty { font-size: 13px; color: #999; line-height: 1.7; }
.ac-missing { line-height: 1.7; }
.ac-missing-note { font-size: 12px; color: #8c4a1f; margin-top: 4px; }

/* 设定项：一项一块，右侧写清"会影响什么" */
.ac-setting { border-bottom: 1px dashed #f0f0f0; padding: 10px 0; }
.ac-setting:last-of-type { border-bottom: none; }
.ac-setting-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 6px; }
.ac-setting-label { font-size: 13px; font-weight: 600; color: #333; }
.ac-star { color: #f5222d; margin-right: 2px; }
.ac-setting-control { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.ac-control { width: 280px; min-width: 0; }
.ac-remark { width: 240px; flex-shrink: 0; }
.ac-bool-hint { font-size: 12px; color: #d46b08; }
.ac-impact {
  display: flex; align-items: flex-start; gap: 6px; margin-top: 6px;
  font-size: 12px; color: #0958d9; background: #f0f7ff;
  border: 1px solid #bae0ff; border-radius: 4px; padding: 6px 8px; line-height: 1.6;
}
.ac-impact-icon { color: #1677ff; margin-top: 2px; }
.ac-semantics { font-size: 12px; color: #8c8c8c; line-height: 1.6; margin-top: 4px; }

/* 履约方式：多选并存 */
.ac-modes { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 8px; }
.ac-mode { border: 1px solid #f0f0f0; border-radius: 4px; padding: 6px 8px; background: #fafafa; }
.ac-mode :deep(.ant-checkbox-wrapper) { align-items: flex-start; }
.ac-mode-label { font-size: 13px; color: #333; font-weight: 600; margin-right: 6px; }
.ac-mode-desc { font-size: 12px; color: #8c8c8c; }

/* 文字条款 */
.ac-narrative { padding: 10px 0; border-bottom: 1px dashed #f0f0f0; }
.ac-narrative:last-of-type { border-bottom: none; }
.ac-narrative-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; flex-wrap: wrap; }
.ac-hash { font-size: 12px; color: #999; }

.ac-actions { display: flex; align-items: center; gap: 10px; margin-top: 12px; flex-wrap: wrap; }
.ac-actions-hint { font-size: 12px; color: #999; }
</style>
