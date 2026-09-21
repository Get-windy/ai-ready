<template>
  <div class="record-rule-tab">
    <!-- 白话说明：原版一上来就讲 domain 表达式、全局/本组取交集并集，属开发语义 -->
    <a-alert
      type="info"
      show-icon
      class="tab-hint"
    >
      <template #message>
        <div class="hint-title">
          更精细地限定「能看/能改哪些单据」，比如：只能看自己创建的销售订单。
        </div>
        <div class="hint-body">
          用「业务对象 + 条件」的方式描述即可，不需要写表达式。
          多条条件同时满足才生效。
        </div>
      </template>
    </a-alert>

    <div class="tab-toolbar">
      <a-space :size="8">
        <a-button
          size="small"
          type="primary"
          :disabled="readonly"
          @click="openCreate"
        >
          <template #icon>
            <PlusOutlined />
          </template>
          新增一条
        </a-button>
        <a-button
          size="small"
          :disabled="readonly"
          @click="load"
        >
          刷新
        </a-button>
      </a-space>
      <span class="tab-toolbar__count">共 <b>{{ rows.length }}</b> 条规则</span>
    </div>

    <a-spin :spinning="loading">
      <a-table
        :columns="columns"
        :data-source="rows"
        :pagination="false"
        :scroll="{ x: 1000 }"
        size="small"
        row-key="id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ruleName'">
            <div>{{ record.ruleName }}</div>
            <code class="rule-code">{{ record.ruleCode }}</code>
          </template>
          <template v-else-if="column.key === 'modelName'">
            <div>{{ objectLabel(record.modelName) }}</div>
            <code class="rule-code">{{ record.modelName }}</code>
          </template>
          <!-- 筛选条件用人话展示；手写的复杂表达式解析不了时回退显示原文 -->
          <template v-else-if="column.key === 'domain'">
            <span
              class="rule-human"
              :class="{ 'rule-human--raw': !isParseable(record.domain) }"
            >{{ humanDomain(record.domain, record.modelName) }}</span>
          </template>
          <template v-else-if="column.key === 'perms'">
            <a-space :size="4">
              <a-tag
                v-for="p in permTags(record)"
                :key="p"
                size="small"
              >
                {{ p }}
              </a-tag>
              <span v-if="!permTags(record).length">-</span>
            </a-space>
          </template>
          <template v-else-if="column.key === 'global'">
            <a-tag :color="record.global ? 'orange' : 'blue'">
              {{ record.global ? '强制规则' : '本岗位' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'active'">
            <a-switch
              :checked="!!record.active"
              size="small"
              :disabled="readonly"
              @change="(v: any) => toggleActive(record, !!v)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button
                type="link"
                size="small"
                :disabled="readonly"
                @click="openEdit(record)"
              >
                编辑
              </a-button>
              <a-button
                type="link"
                size="small"
                danger
                :disabled="readonly"
                @click="handleRemove(record)"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </template>
        <template #emptyText>
          <a-empty description="还没有设置精细数据规则" />
        </template>
      </a-table>
    </a-spin>

    <!-- 新增/编辑规则 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑数据规则' : '新增数据规则'"
      width="760px"
      :confirm-loading="saving"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSubmit"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 19 }"
      >
        <a-form-item
          label="规则名称"
          name="ruleName"
        >
          <a-input
            v-model:value="form.ruleName"
            placeholder="如：业务员只能看自己的销售订单"
          />
        </a-form-item>
        <a-form-item
          label="业务对象"
          name="modelName"
        >
          <a-select
            v-model:value="form.modelName"
            show-search
            allow-clear
            placeholder="选择这条规则管哪种单据/资料"
            :options="objectOptions"
            :filter-option="filterByLabel"
            @change="onObjectChange"
          />
        </a-form-item>

        <!-- 条件构建器：替代原来的 domain 表达式输入框 -->
        <a-form-item label="筛选条件">
          <template v-if="!advancedMode">
            <div
              v-for="(c, i) in conditions"
              :key="c._key"
              class="cond-row"
            >
              <span class="cond-lead">{{ i === 0 ? '当' : '并且' }}</span>
              <a-select
                v-model:value="c.field"
                size="small"
                show-search
                style="width: 170px"
                placeholder="选择字段"
                :options="conditionFieldOptions"
                :filter-option="filterByLabel"
              />
              <a-select
                v-model:value="c.operator"
                size="small"
                style="width: 150px"
                :options="DOMAIN_OPERATORS"
              />
              <a-select
                v-model:value="c.source"
                size="small"
                style="width: 190px"
                :options="DOMAIN_VALUE_SOURCES"
              />
              <a-input
                v-if="c.source === 'CONST'"
                v-model:value="c.constValue"
                size="small"
                style="width: 140px"
                placeholder="填写具体的值"
              />
              <a-button
                type="link"
                size="small"
                danger
                :disabled="conditions.length <= 1"
                @click="removeCondition(i)"
              >
                删除
              </a-button>
            </div>
            <a-button
              type="dashed"
              size="small"
              block
              @click="addCondition"
            >
              <template #icon>
                <PlusOutlined />
              </template>
              添加条件
            </a-button>
            <div class="form-hint">
              多条条件需要同时满足。例：创建人 等于 「当前登录用户本人」
            </div>
          </template>
          <template v-else>
            <a-textarea
              v-model:value="domainText"
              :rows="3"
              placeholder="[('create_by','=',user.id)]"
            />
            <div class="form-hint">
              高级模式：直接书写筛选表达式（对标 Odoo 记录规则），请确认语法正确。
            </div>
          </template>
        </a-form-item>

        <a-form-item label="适用操作">
          <a-checkbox-group v-model:value="form.perms">
            <a-checkbox value="read">
              查看
            </a-checkbox>
            <a-checkbox value="write">
              修改
            </a-checkbox>
            <a-checkbox value="create">
              新增
            </a-checkbox>
            <a-checkbox value="delete">
              删除
            </a-checkbox>
          </a-checkbox-group>
          <span class="form-hint form-hint--inline">
            这条限制对哪些操作生效
          </span>
        </a-form-item>

        <a-form-item label="生效范围">
          <a-switch
            v-model:checked="form.global"
            checked-children="强制规则"
            un-checked-children="本岗位规则"
          />
          <span class="form-hint form-hint--inline">
            强制规则对所有岗位都生效，多条之间只会让可见范围更小；本岗位规则只限制当前岗位
          </span>
        </a-form-item>

        <a-form-item label="说明">
          <a-textarea
            v-model:value="form.description"
            :rows="2"
            placeholder="这条规则解决什么问题（选填，便于日后维护）"
          />
        </a-form-item>

        <a-form-item
          label=" "
          :colon="false"
        >
          <a-switch
            v-model:checked="advancedMode"
            size="small"
            @change="onAdvancedToggle"
          />
          <span class="form-hint form-hint--inline">
            高级模式（直接书写筛选表达式）
          </span>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
/**
 * 精细数据规则 Tab（sys_record_rule，对标 Odoo ir.rule）
 *
 * 挂在「岗位权限 → 设置权限」弹窗下，维护 groupId = 当前角色的记录级规则。
 *
 * 2026-09-20 易用性改造：
 *   · domain 表达式输入框 → 「字段 + 条件 + 值」条件构建器（普通管理员无需懂语法）
 *   · 模型/domain → 业务对象/筛选条件；全局/本组 → 强制规则/本岗位规则
 *   · 保留「高级模式」，原有手写表达式仍可编辑；解析不了的历史规则自动落到高级模式，
 *     不会因为改造而丢失既有配置
 */
import { ref, reactive, computed, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { recordRuleApi, type RecordRule } from '@/api/recordRule'
import {
  objectSelectOptions, objectLabel, ruleFieldOptions,
  DOMAIN_OPERATORS, DOMAIN_VALUE_SOURCES, valueSourceExpr,
} from '@/utils/businessObjects'

const props = defineProps<{
  roleId: string
  readonly?: boolean
}>()

/**
 * 表格 bodyCell 插槽传入的 record 被推断为 Record<string, any>，
 * 用 RecordRule 接收会报类型不匹配，故这里用属性全可选的宽松类型。
 */
type RuleLike = Partial<RecordRule>

/** 一行筛选条件 */
interface ConditionRow {
  _key: string
  field: string
  operator: string
  /** 取值来源：DOMAIN_VALUE_SOURCES 的 value */
  source: string
  /** source 为 CONST 时手填的值 */
  constValue: string
}

const rows = ref<RecordRule[]>([])
const loading = ref(false)
const saving = ref(false)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const conditions = ref<ConditionRow[]>([])
/** 高级模式：直接编辑 domain 原文（保留给技术人员 / 历史复杂规则） */
const advancedMode = ref(false)
const domainText = ref('')

let condSeed = 0
function nextCondKey() {
  condSeed += 1
  return `cond_${Date.now()}_${condSeed}`
}

const form = reactive({
  ruleName: '',
  modelName: '',
  global: false,
  description: '',
  perms: ['read', 'write'] as string[],
})

// trigger 用字面量类型，否则会被推断成 string 而与 a-form 的 RuleObject 不兼容
const formRules = {
  ruleName: { required: true, message: '请输入规则名称', trigger: 'blur' as const },
  modelName: { required: true, message: '请选择业务对象', trigger: 'change' as const },
}

const columns = [
  { title: '规则名称', key: 'ruleName', width: 190 },
  { title: '业务对象', key: 'modelName', width: 150 },
  { title: '筛选条件', key: 'domain', width: 260 },
  { title: '适用操作', key: 'perms', width: 150 },
  { title: '生效范围', key: 'global', width: 100 },
  { title: '启用', key: 'active', width: 70 },
  { title: '操作', key: 'action', width: 110, fixed: 'right' as const },
]

const objectOptions = computed(() =>
  objectSelectOptions().map(o => ({
    label: `${o.label}（${o.value}）`,
    value: o.value,
  }))
)

/** 条件行的字段下拉：随所选业务对象变化 */
const conditionFieldOptions = computed(() =>
  ruleFieldOptions(form.modelName).map(f => ({
    label: `${f.label}（${f.value}）`,
    value: f.value,
  }))
)

function filterByLabel(input: string, option: any) {
  if (!input) return true
  const kw = input.toLowerCase()
  return String(option.label).toLowerCase().includes(kw)
    || String(option.value).toLowerCase().includes(kw)
}

/** 后端 perm* 是 0/1 整数，这里翻成可读标签 */
function permTags(record: RuleLike): string[] {
  const tags: string[] = []
  if (record.permRead === 1) tags.push('查看')
  if (record.permWrite === 1) tags.push('修改')
  if (record.permCreate === 1) tags.push('新增')
  if (record.permDelete === 1) tags.push('删除')
  return tags
}

// ── domain 与条件的互转 ────────────────────────────────────────────────
/** 常量值 → 表达式片段：数字与 True/False/None 原样，其余加引号 */
function formatConst(raw: string): string {
  const t = (raw ?? '').trim()
  if (t === '') return "''"
  if (/^-?\d+(\.\d+)?$/.test(t)) return t
  if (/^(True|False|None)$/.test(t)) return t
  return `'${t.replace(/'/g, "\\'")}'`
}

/** 条件行 → domain 字符串 */
function buildDomain(list: ConditionRow[]): string {
  const parts = list
    .filter(c => c.field && c.operator)
    .map(c => {
      const expr = valueSourceExpr(c.source)
      const val = expr || formatConst(c.constValue)
      return `('${c.field}','${c.operator}',${val})`
    })
  return `[${parts.join(',')}]`
}

/**
 * domain 字符串 → 条件行。
 * 只认本构建器生成的 `[('字段','运算符',值),...]` 形式；
 * 解析不了（手写的复杂表达式）返回 null，由调用方回退到高级模式，保证既有配置不丢。
 */
function parseDomain(domain: string): ConditionRow[] | null {
  const s = (domain || '').trim()
  if (!s.startsWith('[') || !s.endsWith(']')) return null
  const inner = s.slice(1, -1).trim()
  if (!inner) return []
  const out: ConditionRow[] = []
  for (const raw of inner.split(/\)\s*,\s*\(/)) {
    const chunk = raw.trim().replace(/^\(/, '').replace(/\)$/, '')
    const m = chunk.match(/^'?([A-Za-z_][\w.]*)'?\s*,\s*'([^']+)'\s*,\s*([\s\S]+)$/)
    if (!m) return null
    const [, field, operator, rawValue] = m
    const val = rawValue.trim()
    if (val.startsWith('[') || val.includes('(')) return null // 嵌套 domain，交给高级模式
    const sysSource = DOMAIN_VALUE_SOURCES.find(x => x.expr && x.expr === val)
    if (sysSource) {
      out.push({ _key: nextCondKey(), field, operator, source: sysSource.value, constValue: '' })
    } else {
      const constValue = /^'.*'$/.test(val) ? val.slice(1, -1).replace(/\\'/g, "'") : val
      out.push({ _key: nextCondKey(), field, operator, source: 'CONST', constValue })
    }
  }
  return out
}

/** domain 能否被条件构建器理解 */
function isParseable(domain: string): boolean {
  return parseDomain(domain) !== null
}

/** domain → 人话（列表列展示用） */
function humanDomain(domain: string, modelName?: string): string {
  const parsed = parseDomain(domain)
  if (parsed === null) return domain || '-'
  if (!parsed.length) return '不做限制'
  const fieldLabelOf = (f: string) =>
    ruleFieldOptions(modelName).find(x => x.value === f)?.label || f
  return parsed.map(c => {
    const op = DOMAIN_OPERATORS.find(o => o.value === c.operator)?.label || c.operator
    const v = c.source === 'CONST'
      ? c.constValue
      : (DOMAIN_VALUE_SOURCES.find(s => s.value === c.source)?.label || c.constValue)
    return `${fieldLabelOf(c.field)} ${op} ${v}`
  }).join('，并且 ')
}

// ── 弹窗 ──────────────────────────────────────────────────────────────
async function load() {
  if (!props.roleId) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    const res = await recordRuleApi.listByGroup(props.roleId)
    const list = (res as unknown as RecordRule[]) || []
    rows.value = Array.isArray(list) ? list : []
  } catch (err) {
    console.warn('[岗位权限] 加载数据规则失败', err)
    rows.value = []
  } finally {
    loading.value = false
  }
}

/** 业务对象变更：清空字段已选项，避免出现「对象与字段对不上」的条件 */
function onObjectChange() {
  for (const c of conditions.value) {
    c.field = ''
  }
}

function addCondition() {
  conditions.value.push({
    _key: nextCondKey(),
    field: form.modelName ? ruleFieldOptions(form.modelName)[0]?.value || '' : '',
    operator: '=',
    source: 'USER_SELF',
    constValue: '',
  })
}

function removeCondition(index: number) {
  conditions.value.splice(index, 1)
}

function resetForm() {
  form.ruleName = ''
  form.modelName = ''
  form.global = false
  form.description = ''
  form.perms = ['read', 'write']
  conditions.value = []
  advancedMode.value = false
  domainText.value = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  addCondition()
  modalVisible.value = true
}

function openEdit(record: RuleLike) {
  editingId.value = record.id ?? null
  form.ruleName = record.ruleName
  form.modelName = record.modelName
  form.global = !!record.global
  form.description = record.description || ''
  form.perms = []
  if (record.permRead === 1) form.perms.push('read')
  if (record.permWrite === 1) form.perms.push('write')
  if (record.permCreate === 1) form.perms.push('create')
  if (record.permDelete === 1) form.perms.push('delete')

  // 能解析成条件就用构建器展示，否则退回高级模式（保留原文，不丢配置）
  const parsed = parseDomain(record.domain)
  if (parsed === null) {
    advancedMode.value = true
    domainText.value = record.domain
    conditions.value = []
  } else {
    advancedMode.value = false
    domainText.value = ''
    conditions.value = parsed.length ? parsed : []
    if (!parsed.length) addCondition()
  }
  modalVisible.value = true
}

/** 切换高级模式：开→把条件导出成表达式；关→尝试把表达式解析回条件 */
function onAdvancedToggle(checked: boolean) {
  if (checked) {
    domainText.value = buildDomain(conditions.value)
    return
  }
  const parsed = parseDomain(domainText.value)
  if (parsed === null) {
    message.warning('当前表达式较复杂，无法用条件方式编辑，请继续使用高级模式')
    advancedMode.value = true
    return
  }
  conditions.value = parsed.length ? parsed : []
  if (!parsed.length) addCondition()
}

function buildPayload() {
  const domain = advancedMode.value ? domainText.value.trim() : buildDomain(conditions.value)
  // perm* 后端是 Integer（1-适用 0-不适用），必须提交 1/0 而非布尔值，
  // 否则 Jackson 反序列化到 Integer 会失败（该接口此前零前端入口，这条路径从未被验证过）
  return {
    ruleName: form.ruleName.trim(),
    modelName: form.modelName.trim(),
    domain,
    global: form.global,
    description: form.description,
    groupId: Number(props.roleId),
    active: true,
    permRead: form.perms.includes('read') ? 1 : 0,
    permWrite: form.perms.includes('write') ? 1 : 0,
    permCreate: form.perms.includes('create') ? 1 : 0,
    permDelete: form.perms.includes('delete') ? 1 : 0,
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (!advancedMode.value && !conditions.value.some(c => c.field && c.operator)) {
    message.warning('请至少添加一个有效条件，或切换到高级模式填写表达式')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await recordRuleApi.update(editingId.value, buildPayload())
      message.success('规则已更新')
    } else {
      await recordRuleApi.create(buildPayload())
      message.success('规则已创建')
    }
    modalVisible.value = false
    await load()
  } catch (err: any) {
    message.error(err?.message || '保存规则失败')
  } finally {
    saving.value = false
  }
}

async function toggleActive(record: RuleLike, active: boolean) {
  if (!record.id) return
  try {
    if (active) await recordRuleApi.activate(record.id)
    else await recordRuleApi.deactivate(record.id)
    record.active = active
    message.success(active ? '规则已启用' : '规则已停用')
  } catch (err: any) {
    message.error(err?.message || '操作失败')
  }
}

function handleRemove(record: RuleLike) {
  if (!record.id) return
  Modal.confirm({
    title: '删除数据规则',
    content: `确定删除规则「${record.ruleName}」吗？删除后该限制不再生效。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await recordRuleApi.remove(record.id as number)
      message.success('已删除')
      await load()
    },
  })
}

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

.rule-code {
  font-size: 11px;
  color: #909399;
}

.rule-human {
  font-size: 12px;
  color: #531dab;
  word-break: break-all;
}

.rule-human--raw {
  font-family: 'Consolas', 'Monaco', monospace;
  color: #909399;
}

.cond-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.cond-lead {
  width: 32px;
  font-size: 12px;
  color: #595959;
  flex-shrink: 0;
}

.form-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.form-hint--inline {
  margin-top: 0;
  margin-left: 8px;
}
</style>
