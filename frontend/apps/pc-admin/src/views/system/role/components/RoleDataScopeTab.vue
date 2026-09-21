<template>
  <div class="data-scope-tab">
    <!-- 白话说明：讲清「这是管哪一行数据」的，而不是「行级可见范围规则」 -->
    <a-alert
      type="info"
      show-icon
      class="tab-hint"
    >
      <template #message>
        <div class="hint-title">
          决定这个岗位能看到<b>谁的数据</b>。
        </div>
        <div class="hint-body">
          例如：让「业务员」只看自己负责的客户；让「部门主管」看到本部门及下属部门的数据。
          不在这里列出的业务对象，默认不受限制。
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
        :scroll="{ x: 900 }"
        size="small"
        row-key="_key"
        bordered
      >
        <template #bodyCell="{ column, record, index }">
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
              :filter-option="filterByLabel"
              :disabled="readonly"
              @change="(v: any) => { record.targetTable = v || ''; markDirty() }"
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

          <!-- 可见范围：原来叫「规则类型」，选项里还有 CUSTOM_SQL 这种开发词 -->
          <template v-else-if="column.key === 'ruleType'">
            <a-select
              v-model:value="record.ruleType"
              size="small"
              style="width: 165px"
              :disabled="readonly"
              :options="ruleTypeOptions"
              @change="markDirty"
            />
          </template>

          <!-- 范围明细：按「可见范围」给不同控件。业务对象为空表示对所有对象生效 -->
          <template v-else-if="column.key === 'ruleValue'">
            <a-tree-select
              v-if="record.ruleType === 'DEPT' || record.ruleType === 'DEPT_AND_CHILD'"
              :value="parseDeptIds(record.deptIds)"
              :tree-data="deptTree"
              :field-names="{ label: 'deptName', value: 'id', children: 'children' }"
              tree-checkable
              :show-checked-strategy="SHOW_PARENT"
              size="small"
              style="width: 100%"
              placeholder="选择可以看到的部门"
              :disabled="readonly"
              @change="(v: any) => { record.deptIds = JSON.stringify(v || []); markDirty() }"
            />
            <a-input
              v-else-if="record.ruleType === 'CUSTOM_SQL'"
              v-model:value="record.customSql"
              size="small"
              placeholder="请交由技术人员填写筛选条件"
              :disabled="readonly"
              @change="markDirty"
            />
            <span
              v-else
              class="rule-value-none"
            >{{ ruleTypeHint(record.ruleType) }}</span>
          </template>

          <!-- 判断依据：不再让用户填字段名，直接按「可见范围」自动选好并展示出来 -->
          <template v-else-if="column.key === 'basis'">
            <a-tooltip :title="`系统按 ${autoFieldOf(record)} 字段判断数据归属`">
              <span class="basis-text">{{ basisText(record) }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.key === 'remark'">
            <a-input
              v-model:value="record.remark"
              size="small"
              placeholder="备注（选填）"
              :disabled="readonly"
              @change="markDirty"
            />
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
          <a-empty description="还没有设置数据可见范围" />
        </template>
      </a-table>
    </a-spin>

    <div class="tab-footnote">
      说明：保存后以当前列表为准。「全部数据」表示不做限制；未在此列出的业务对象一律不限制；
      业务对象留空表示对所有对象都按该范围生效。
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 数据可见范围 Tab（sys_data_scope）
 *
 * 挂在「岗位权限 → 设置权限」弹窗下，按角色配置行级数据范围。
 *
 * 2026-09-20 易用性改造：
 *   · 术语业务化：目标表→业务对象、规则类型→可见范围、规则取值→范围明细
 *   · 表名改为下拉选择（字典见 utils/businessObjects.ts）
 *   · **去掉「目标字段」列**：判断字段由「可见范围」自动推导（本部门→dept_id、仅本人→负责人字段），
 *     管理员不必知道 dept_id 这类物理字段名；原值在「判断依据」列以白话展示
 *   · 「自定义 SQL」标注为需技术人员填写
 */
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { TreeSelect } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { dataScopeApi, RULE_TYPE_OPTIONS, type DataScopeRule } from '@/api/dataScope'
import { departmentApi } from '@/api/department'
import { objectSelectOptions, ownerFieldOf, deptFieldOf } from '@/utils/businessObjects'

const SHOW_PARENT = TreeSelect.SHOW_PARENT

const props = defineProps<{
  roleId: string
  readonly?: boolean
}>()

type EditableRule = DataScopeRule & {
  _key: string
  /** 手工输入表名模式（字典未收录的对象） */
  _customTable?: boolean
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
const deptTree = ref<any[]>([])

let keySeed = 0
function nextKey() {
  keySeed += 1
  return `ds_${Date.now()}_${keySeed}`
}

const columns = [
  { title: '业务对象', key: 'targetTable', width: 210 },
  { title: '可见范围', key: 'ruleType', width: 175 },
  { title: '范围明细', key: 'ruleValue', width: 250 },
  { title: '判断依据', key: 'basis', width: 150 },
  { title: '备注', key: 'remark', width: 130 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const },
]

const objectOptions = computed(() =>
  objectSelectOptions().map(o => ({
    label: `${o.label}（${o.value}）`,
    value: o.value,
  }))
)

/** 原选项里的「自定义 SQL」是开发词，这里统一换成业务口径的说明 */
const ruleTypeOptions = computed(() =>
  RULE_TYPE_OPTIONS.map(o => {
    if (o.value === 'ALL') return { ...o, label: '全部数据（不限制）' }
    if (o.value === 'DEPT') return { ...o, label: '本部门的数据' }
    if (o.value === 'DEPT_AND_CHILD') return { ...o, label: '本部门及下属部门' }
    if (o.value === 'SELF') return { ...o, label: '只看自己负责的' }
    if (o.value === 'CUSTOM_SQL') return { ...o, label: '自定义条件（技术人员）' }
    return o
  })
)

function filterByLabel(input: string, option: any) {
  if (!input) return true
  const kw = input.toLowerCase()
  return String(option.label).toLowerCase().includes(kw)
    || String(option.value).toLowerCase().includes(kw)
}

function markDirty() {
  dirty.value = true
}

/**
 * 判断字段由「可见范围」自动推导：
 *   · 只看自己负责的 → 该对象的负责人字段（默认 create_by）
 *   · 按部门         → 该对象的部门字段（默认 dept_id）
 * 字典里没收录的对象也能用，回退到通用字段名。
 */
function autoFieldOf(record: RuleLike): string {
  if (record.ruleType === 'SELF') return ownerFieldOf(record.targetTable)
  return deptFieldOf(record.targetTable)
}

/** 「判断依据」列的白话文案 */
function basisText(record: RuleLike): string {
  if (record.ruleType === 'ALL') return '不做限制'
  if (record.ruleType === 'CUSTOM_SQL') return '自定义条件'
  return autoFieldOf(record)
}

/** deptIds 后端存 JSON 数组字符串，这里容错解析 */
function parseDeptIds(raw?: string): (string | number)[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function ruleTypeHint(type: string): string {
  if (type === 'ALL') return '全部数据，不做限制'
  if (type === 'SELF') return '只看自己创建或负责的数据'
  return '-'
}

async function loadDeptTree() {
  if (deptTree.value.length) return
  try {
    const tree = await departmentApi.getTree({})
    deptTree.value = Array.isArray(tree) ? tree : []
  } catch (err) {
    console.warn('[岗位权限] 加载部门树失败', err)
  }
}

async function load() {
  if (!props.roleId) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    const res = await dataScopeApi.list(props.roleId)
    const list = (res as unknown as DataScopeRule[]) || []
    rows.value = (Array.isArray(list) ? list : []).map(r => ({
      ...r,
      _key: nextKey(),
      _customTable: !!r.targetTable && !objectSelectOptions().some(o => o.value === r.targetTable),
    }))
    dirty.value = false
  } catch (err) {
    console.warn('[岗位权限] 加载数据范围失败', err)
    rows.value = []
  } finally {
    loading.value = false
  }
}

function addRow() {
  rows.value.push({
    _key: nextKey(),
    ruleType: 'DEPT',
    targetTable: '',
    targetField: 'dept_id',
    deptIds: '[]',
    status: 1,
  })
  markDirty()
}

function removeRow(index: number) {
  rows.value.splice(index, 1)
  markDirty()
}

function toPayload(): DataScopeRule[] | null {
  const payload: DataScopeRule[] = []
  for (const r of rows.value) {
    if (!r.ruleType) {
      message.warning('请先选择「可见范围」')
      return null
    }
    if (r.ruleType === 'CUSTOM_SQL' && !r.customSql?.trim()) {
      message.warning('「自定义条件」需要填写筛选条件')
      return null
    }
    if ((r.ruleType === 'DEPT' || r.ruleType === 'DEPT_AND_CHILD') && parseDeptIds(r.deptIds).length === 0) {
      message.warning('按部门限制时，请至少选择一个部门')
      return null
    }
    const { _key, _customTable, ...rest } = r
    void _key
    void _customTable
    // 判断字段不由用户填：按「可见范围」自动推导（自定义条件保留用户原值）
    const targetField = r.ruleType === 'CUSTOM_SQL'
      ? (r.targetField?.trim() || 'dept_id')
      : autoFieldOf(r)
    payload.push({
      ...rest,
      targetTable: r.targetTable?.trim() || undefined,
      targetField,
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
    await dataScopeApi.save(props.roleId, payload)
    message.success('数据可见范围已保存')
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

defineExpose({ load, save: handleSave, hasChanges: computed(() => dirty.value) })

watch(() => props.roleId, () => {
  void load()
  void loadDeptTree()
}, { immediate: true })
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

.rule-value-none {
  font-size: 12px;
  color: #909399;
}

.basis-text {
  font-size: 12px;
  color: #531dab;
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
