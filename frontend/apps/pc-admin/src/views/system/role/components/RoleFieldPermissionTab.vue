<template>
  <div class="field-permission-tab">
    <!-- 白话说明：先讲「这是干什么的」，再讲怎么用。原版一上来就是「目标表/脱敏方式/全量覆盖」，
         业务管理员看不懂，这里换成场景化描述。 -->
    <a-alert
      type="info"
      show-icon
      class="tab-hint"
    >
      <template #message>
        <div class="hint-title">
          有些信息（客户手机号、商品成本价）不希望所有岗位都看到，在这里设置。
        </div>
        <div class="hint-body">
          例如：让「业务员」看到客户手机号时显示为 <b>138****1234</b>。
          需要谁看不到，把「能看到吗」关掉；需要看到但打码，选一个打码方式即可。
        </div>
        <div class="hint-body">
          目前可管控：<b>往来单位（客户/供应商）</b>的手机号、邮箱、银行账号等，<b>员工</b>的手机号、邮箱、身份证号；
          金额类字段（信用额度、当前欠款）只能设「看不到」。其他字段需先由开发在实体上接入。
        </div>
      </template>
    </a-alert>

    <div class="tab-toolbar">
      <a-space :size="8">
        <a-button
          size="small"
          type="primary"
          :disabled="readonly"
          @click="addRow"
        >
          <template #icon>
            <PlusOutlined />
          </template>
          添加一条
        </a-button>
        <a-button
          size="small"
          :loading="saving"
          :disabled="readonly || !dirty"
          @click="handleSave"
        >
          保存
        </a-button>
      </a-space>
      <span class="tab-toolbar__count">
        共 <b>{{ rows.length }}</b> 条设置
        <span
          v-if="dirty"
          class="tab-toolbar__dirty"
        >（有未保存修改）</span>
      </span>
    </div>

    <a-spin :spinning="loading">
      <a-table
        :columns="columns"
        :data-source="rows"
        :pagination="false"
        :scroll="{ x: 1080 }"
        size="small"
        row-key="_key"
        bordered
      >
        <template #bodyCell="{ column, record, index }">
          <!-- 业务对象：默认从下拉选（显示中文名，附物理表名便于实施核对），
               字典没收录的对象可以切到手工输入，不会因为字典不全而卡住 -->
          <template v-if="column.key === 'targetTable'">
            <a-select
              v-if="!record._customTable"
              :value="record.targetTable || undefined"
              size="small"
              show-search
              allow-clear
              style="width: 100%"
              placeholder="选择业务对象"
              :options="objectOptions"
              :filter-option="filterObjectOption"
              :disabled="readonly"
              @change="(v: any) => onTableChange(record, v)"
            />
            <a-input
              v-else
              v-model:value="record.targetTable"
              size="small"
              placeholder="填写数据表名，如 biz_party"
              :disabled="readonly"
              @change="markDirty"
            >
              <template #suffix>
                <a
                  class="switch-mode"
                  @click="record._customTable = false"
                >从列表选</a>
              </template>
            </a-input>
          </template>

          <!-- 字段：随业务对象联动。选了对象就从常用字段里挑，未收录的自由填写 -->
          <template v-else-if="column.key === 'targetField'">
            <a-select
              v-if="!record._customField && fieldOptionsOf(record).length"
              :value="record.targetField || undefined"
              size="small"
              show-search
              allow-clear
              style="width: 100%"
              placeholder="选择字段"
              :options="fieldOptionsOf(record)"
              :filter-option="filterFieldOption"
              :disabled="readonly"
              @change="(v: any) => onFieldChange(record, v)"
            />
            <a-input
              v-else
              v-model:value="record.targetField"
              size="small"
              :placeholder="record._customField ? '填写字段名，如 phone' : '请先选择业务对象'"
              :disabled="readonly || (!record._customField && !fieldOptionsOf(record).length)"
              @change="markDirty"
            >
              <template
                v-if="record._customField"
                #suffix
              >
                <a
                  class="switch-mode"
                  @click="onBackToFieldList(record)"
                >从列表选</a>
              </template>
            </a-input>
          </template>

          <!-- 「能看到吗」用开关而不是「可见性 1/0」：业务管理员不该看到 1 和 0 -->
          <template v-else-if="column.key === 'visible'">
            <a-switch
              :checked="record.visible === 1"
              size="small"
              checked-children="能看到"
              un-checked-children="看不到"
              :disabled="readonly"
              @change="(v: any) => { record.visible = v ? 1 : 0; markDirty() }"
            />
          </template>

          <template v-else-if="column.key === 'maskType'">
            <a-select
              v-model:value="record.maskType"
              size="small"
              style="width: 118px"
              :disabled="readonly || record.visible !== 1"
              :options="MASK_TYPE_OPTIONS"
              @change="markDirty"
            />
          </template>

          <!-- 效果预览：直接把这个字段打码后的样子给管理员看，不用想象 -->
          <template v-else-if="column.key === 'preview'">
            <span
              class="preview"
              :class="{ 'preview--hidden': record.visible !== 1 }"
            >{{ maskPreview(record) }}</span>
          </template>

          <!-- 保留位数只在「自定义打码」时才需要填，合并成一列，避免两列空栏 -->
          <template v-else-if="column.key === 'keep'">
            <div
              v-if="record.visible === 1 && record.maskType === 'CUSTOM'"
              class="keep-lens"
            >
              <span>前</span>
              <a-input-number
                v-model:value="record.maskPrefixLen"
                size="small"
                :min="0"
                :max="20"
                style="width: 54px"
                :disabled="readonly"
                @change="markDirty"
              />
              <span>后</span>
              <a-input-number
                v-model:value="record.maskSuffixLen"
                size="small"
                :min="0"
                :max="20"
                style="width: 54px"
                :disabled="readonly"
                @change="markDirty"
              />
              <span>位</span>
            </div>
            <span
              v-else
              class="preview-none"
            >-</span>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-button
              type="link"
              size="small"
              danger
              :disabled="readonly"
              @click="removeRow(index)"
            >
              移除
            </a-button>
          </template>
        </template>
        <template #emptyText>
          <a-empty description="还没有设置任何敏感信息保护" />
        </template>
      </a-table>
    </a-spin>

    <div class="tab-footnote">
      说明：保存后以当前列表为准（列表里删掉某条即取消该设置）。
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 敏感信息保护（字段级权限）Tab（sys_field_permission）
 *
 * 挂在「岗位权限 → 设置权限」弹窗下，按角色配置「表 × 字段」的可见性与打码方式。
 *
 * 2026-09-20 易用性改造：
 *   · 术语业务化：目标表→业务对象、目标字段→字段、脱敏类型→打码方式、visible 1/0→能看到吗
 *   · 表名/字段名改为下拉选择（字典见 utils/businessObjects.ts），消灭手打物理名
 *   · 新增「效果预览」列，直接展示打码后的样子
 *   · 「保留前缀/后缀长度」合并为「保留位数」，且仅在自定义打码时出现
 */
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { fieldPermissionApi, MASK_TYPE_OPTIONS, type FieldPermissionRule } from '@/api/fieldPermission'
import {
  objectSelectOptions, fieldSelectOptions,
  type FieldOption,
} from '@/utils/businessObjects'

const props = defineProps<{
  /** 当前角色 ID（弹窗选中的角色） */
  roleId: string
  /** 只读模式（查看他人配置时禁用编辑） */
  readonly?: boolean
}>()

/** 行内编辑需要一个稳定的 key（后端 id 在新增行上不存在） */
type EditableRule = FieldPermissionRule & {
  _key: string
  /** 手工输入表名模式（字典未收录的对象） */
  _customTable?: boolean
  /** 手工输入字段名模式 */
  _customField?: boolean
}

/**
 * 表格 bodyCell 插槽传入的 record 被推断为 Record<string, any>，
 * 用 EditableRule 接收会报类型不匹配，故这里用属性全可选的宽松类型。
 */
type RuleLike = Partial<EditableRule>

const rows = ref<EditableRule[]>([])
const loading = ref(false)
const saving = ref(false)
const dirty = ref(false)

let keySeed = 0
function nextKey() {
  keySeed += 1
  return `fp_${Date.now()}_${keySeed}`
}

const columns = [
  { title: '业务对象', key: 'targetTable', width: 210 },
  { title: '字段', key: 'targetField', width: 160 },
  { title: '能看到吗', key: 'visible', width: 100 },
  { title: '打码方式', key: 'maskType', width: 130 },
  { title: '效果预览', key: 'preview', width: 170 },
  { title: '保留位数', key: 'keep', width: 190 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const },
]

/** 业务对象下拉：显示中文名，括号里附物理表名（实施核对用） */
const objectOptions = computed(() =>
  objectSelectOptions().map(o => ({
    label: `${o.label}（${o.value}）`,
    value: o.value,
  }))
)

/** 字段下拉：显示中文名 + 物理字段名 */
function fieldOptionsOf(record: RuleLike): FieldOption[] {
  return fieldSelectOptions(record.targetTable).map(f => ({
    label: `${f.label}（${f.value}）`,
    value: f.value,
  }))
}

/** 下拉搜索：中文名和物理名都参与匹配，用户打中文或英文都能命中 */
function filterObjectOption(input: string, option: any) {
  if (!input) return true
  const kw = input.toLowerCase()
  return String(option.label).toLowerCase().includes(kw)
    || String(option.value).toLowerCase().includes(kw)
}
function filterFieldOption(input: string, option: any) {
  return filterObjectOption(input, option)
}

function markDirty() {
  dirty.value = true
}

/** 切换业务对象时，若字段不属于新对象则清空，避免留下「对象与字段对不上」的脏配置 */
function onTableChange(record: RuleLike, value: any) {
  const next = value || ''
  if (next !== record.targetTable) {
    record.targetField = ''
    record._customField = false
  }
  record.targetTable = next
  markDirty()
}

function onFieldChange(record: RuleLike, value: any) {
  record.targetField = value || ''
  markDirty()
}

/** 从手工输入切回下拉（仅当该对象在字典里时才允许） */
function onBackToFieldList(record: RuleLike) {
  if (!fieldSelectOptions(record.targetTable).length) {
    message.info('该业务对象暂未收录常用字段，请继续手工填写')
    return
  }
  record._customField = false
  record.targetField = ''
  markDirty()
}

// ── 效果预览 ──────────────────────────────────────────────────────────
/** 各类打码用的示例原文（选一个贴近真实格式的样本） */
const MASK_SAMPLES: Record<string, string> = {
  PHONE: '13800001234',
  EMAIL: 'zhangsan@example.com',
  ID_CARD: '110101199001011234',
  BANK_CARD: '6222021234567890123',
  CUSTOM: '13800001234',
}

/**
 * 按后端 DataMaskSerializer 的规则在前端算一遍打码结果，让管理员「所见即所得」。
 * 逻辑必须与后端保持一致，否则预览会误导人（后端若有调整，这里要同步改）。
 */
function maskPreview(record: RuleLike): string {
  if (record.visible !== 1) return '该字段不显示'
  const type = record.maskType || 'NONE'
  const src = MASK_SAMPLES[type] || MASK_SAMPLES.PHONE
  if (type === 'NONE') return src

  const ch = (record.maskChar || '*').charAt(0)
  const rep = (n: number) => String(ch).repeat(Math.max(0, n))
  const keep = (s: string, n: number) => (n > 0 ? s.slice(s.length - n) : '')

  switch (type) {
    case 'PHONE':
      return src.length < 7 ? src : src.slice(0, 3) + rep(4) + src.slice(7)
    case 'EMAIL': {
      const at = src.indexOf('@')
      if (at <= 1) return src
      return src.slice(0, 1) + rep(Math.min(at - 1, 4)) + src.slice(at)
    }
    case 'ID_CARD':
      return src.length < 10 ? src : src.slice(0, 6) + rep(src.length - 10) + keep(src, 4)
    case 'BANK_CARD':
      return src.length < 8 ? src : src.slice(0, 4) + rep(src.length - 8) + keep(src, 4)
    case 'CUSTOM': {
      const p = Number(record.maskPrefixLen ?? 0)
      const s = Number(record.maskSuffixLen ?? 0)
      if (p + s >= src.length) return rep(src.length)
      return src.slice(0, p) + rep(src.length - p - s) + keep(src, s)
    }
    default:
      return src
  }
}

async function load() {
  if (!props.roleId) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    const res = await fieldPermissionApi.list(props.roleId)
    const list = (res as unknown as FieldPermissionRule[]) || []
    rows.value = (Array.isArray(list) ? list : []).map(r => ({
      ...r,
      _key: nextKey(),
      // 已配置的对象若不在字典里，直接进手工输入模式，避免下拉显示空白
      _customTable: !!r.targetTable && !objectSelectOptions().some(o => o.value === r.targetTable),
    }))
    dirty.value = false
  } catch (err) {
    console.warn('[岗位权限] 加载字段级权限失败', err)
    rows.value = []
  } finally {
    loading.value = false
  }
}

function addRow() {
  rows.value.push({
    _key: nextKey(),
    targetTable: '',
    targetField: '',
    visible: 1,
    maskType: 'NONE',
    maskChar: '*',
    maskPrefixLen: 0,
    maskSuffixLen: 0,
  })
  markDirty()
}

function removeRow(index: number) {
  rows.value.splice(index, 1)
  markDirty()
}

/** 去掉行内编辑用的临时字段，且要求业务对象/字段必填 */
function toPayload(): FieldPermissionRule[] | null {
  const payload: FieldPermissionRule[] = []
  for (const r of rows.value) {
    if (!r.targetTable?.trim() || !r.targetField?.trim()) {
      message.warning('请先把「业务对象」和「字段」选完整')
      return null
    }
    const { _key, _customTable, _customField, ...rest } = r
    void _key
    void _customTable
    void _customField
    payload.push({
      ...rest,
      targetTable: r.targetTable.trim(),
      targetField: r.targetField.trim(),
      maskChar: r.maskChar || '*',
      status: 1,
    })
  }
  return payload
}

/**
 * 保存。返回是否成功 —— 父弹窗的「保存」按钮会调用本方法并据此汇总失败维度，
 * 校验不通过时返回 false（不能只弹提示就静默返回，否则父组件会误判为成功）。
 * 无改动时直接返回 true 且不发请求（本就是全量覆盖，没改不必重提）。
 */
async function handleSave(): Promise<boolean> {
  if (!props.roleId) return false
  if (!dirty.value) return true
  const payload = toPayload()
  if (!payload) return false
  saving.value = true
  try {
    await fieldPermissionApi.save(props.roleId, payload)
    message.success('敏感信息保护已保存')
    dirty.value = false
    await load()
    return true
  } catch (err: any) {
    message.error(err?.message || '保存失败')
    return false
  } finally {
    saving.value = false
  }
}

/** 暴露给父组件：角色切换时重新加载由 watch 负责，这里供「保存前统一提交」调用 */
defineExpose({ load, save: handleSave, hasChanges: computed(() => dirty.value) })

watch(() => props.roleId, () => { void load() }, { immediate: true })
</script>

<style scoped>
.tab-hint {
  margin-bottom: 12px;
}

.hint-title {
  font-weight: 600;
}

.hint-body {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.6;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.tab-toolbar__count {
  font-size: 12px;
  color: #909399;
}

.tab-toolbar__dirty {
  color: #d48806;
}

.preview {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  color: #531dab;
  word-break: break-all;
}

.preview--hidden {
  color: #909399;
  font-family: inherit;
}

.preview-none {
  font-size: 12px;
  color: #909399;
}

.keep-lens {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #595959;
}

.switch-mode {
  font-size: 12px;
}

.tab-footnote {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
