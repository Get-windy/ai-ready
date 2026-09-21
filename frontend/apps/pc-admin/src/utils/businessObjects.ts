/**
 * 业务对象字典：把物理表名 / 字段名翻译成业务语言，供「岗位权限」下的三个配置面板做下拉选择。
 *
 * 解决的问题：字段权限、数据范围、数据规则原先要求管理员手打
 * `erp_customer`、`phone`、`[('create_by','=',user.id)]` 这类物理名与表达式，
 * 只有开发能看懂，且拼错不会报错、只会静默失效
 * （典型例证：api/fieldPermission.ts 的示例注释写的 `erp_customer` 就是个不存在的表）。
 *
 * ⚠️ **收录范围有硬约束**：只有后端实体字段上打过 `@DataMask` / `@DataMaskNumber` 注解的字段，
 * 在这里配了才会真的生效（注解声明该字段属于哪张表，见 core-base 的 DataMaskSerializer）。
 * 因此本字典**只收录已接入的字段**，宁可少列也不列"配了不生效"的假选项。
 *
 * 接入新对象/字段的步骤：① 给后端实体字段打 `@DataMask(table = "表名")`；
 * ② 在这里补一条（字段名必须与实体属性名一致）；③ 前端无需其他改动。
 *
 * 下拉一律"可选可填"：字典没收录的对象/字段仍允许手工输入，不会因为字典不全而卡住配置。
 */

/** 字段选项：value 为物理字段名，label 为业务名 */
export interface FieldOption {
  value: string
  label: string
}

/** 业务对象选项 */
export interface BusinessObjectOption {
  /** 物理表名（提交给后端） */
  value: string
  /** 业务名（展示给用户） */
  label: string
  /** 下拉分组 */
  group: string
  /** 可管控字段（均已打脱敏注解） */
  fields: FieldOption[]
  /** 「归属人」字段：数据范围选「只看自己负责的」时用它过滤 */
  ownerField?: string
  /** 「所属部门」字段：数据范围选「按部门」时用它过滤 */
  deptField?: string
}

/**
 * 已接入脱敏的字段。
 *
 * 金额类字段（信用额度、当前欠款）只能设「能看到 / 看不到」——数字没有部分打码的通用形式，
 * 故在其业务名里直接标注，避免管理员配了打码方式却不生效。
 */
export const BUSINESS_OBJECTS: BusinessObjectOption[] = [
  {
    value: 'biz_party',
    label: '往来单位（客户/供应商）',
    group: '基础资料',
    ownerField: 'defaultHandlerId',
    fields: [
      { value: 'phone', label: '手机号' },
      { value: 'email', label: '邮箱' },
      { value: 'unifiedCode', label: '统一社会信用代码' },
      { value: 'taxNumber', label: '税号' },
      { value: 'bankName', label: '开户行' },
      { value: 'bankAccount', label: '银行账号' },
      { value: 'legalPerson', label: '法人代表' },
      { value: 'legalPersonPhone', label: '法人电话' },
      { value: 'creditLimit', label: '信用额度（金额，仅支持隐藏）' },
      { value: 'currentDebt', label: '当前欠款（金额，仅支持隐藏）' },
    ],
  },
  {
    value: 'hr_employee',
    label: '员工',
    group: '基础资料',
    fields: [
      { value: 'phone', label: '手机号' },
      { value: 'email', label: '邮箱' },
      { value: 'idCard', label: '身份证号' },
    ],
  },
]

/** 表名 → 业务名（查不到时原样返回表名，保证界面不缺信息） */
export function objectLabel(table?: string): string {
  if (!table) return ''
  const hit = BUSINESS_OBJECTS.find(o => o.value === table)
  return hit ? hit.label : table
}

/** 字段名 → 业务名（需先定位所属对象；查不到时原样返回字段名） */
export function fieldLabel(table: string | undefined, field?: string): string {
  if (!field) return ''
  const obj = BUSINESS_OBJECTS.find(o => o.value === table)
  return obj?.fields.find(f => f.value === field)?.label || field
}

/** 业务对象下拉选项 */
export function objectSelectOptions() {
  return BUSINESS_OBJECTS.map(o => ({
    label: `${o.label}（${o.value}）`,
    value: o.value,
    group: o.group,
  }))
}

/** 某业务对象的字段下拉选项；未收录的对象返回空数组（调用方退化为可自由输入） */
export function fieldSelectOptions(table?: string): FieldOption[] {
  if (!table) return []
  return BUSINESS_OBJECTS.find(o => o.value === table)?.fields || []
}

/** 该对象用于「只看自己负责的」过滤的字段名 */
export function ownerFieldOf(table?: string): string {
  if (!table) return 'create_by'
  return BUSINESS_OBJECTS.find(o => o.value === table)?.ownerField || 'create_by'
}

/** 该对象用于「按部门」过滤的字段名 */
export function deptFieldOf(table?: string): string {
  if (!table) return 'dept_id'
  return BUSINESS_OBJECTS.find(o => o.value === table)?.deptField || 'dept_id'
}

/**
 * 数据规则的条件构建器用「判断字段」下拉。
 *
 * 与字段权限用的 fields 不同：这里是**按什么条件筛数据**（创建人、所属部门、金额…），
 * 而不是敏感字段。故先把归属类字段放前面（最常用），再补该对象的业务字段。
 */
export function ruleFieldOptions(table?: string): FieldOption[] {
  const obj = BUSINESS_OBJECTS.find(o => o.value === table)
  const list: FieldOption[] = [
    { value: 'create_by', label: '创建人' },
    { value: 'dept_id', label: '所属部门' },
  ]
  const owner = obj?.ownerField
  if (owner && !list.some(f => f.value === owner)) {
    list.push({ value: owner, label: '负责人' })
  }
  const dept = obj?.deptField
  if (dept && !list.some(f => f.value === dept)) {
    list.push({ value: dept, label: '所属部门' })
  }
  for (const f of obj?.fields || []) {
    if (!list.some(x => x.value === f.value)) list.push(f)
  }
  return list
}

// ── 数据规则的条件构建器用的选项 ──────────────────────────────────────
// 目的：让管理员点几下就能生成 `[('create_by','=',user.id)]`，不需要懂表达式语法。

/** 比较符号 */
export const DOMAIN_OPERATORS = [
  { value: '=', label: '等于' },
  { value: '!=', label: '不等于' },
  { value: 'in', label: '是其中之一（多选）' },
  { value: 'not in', label: '不是其中任何一个' },
  { value: 'like', label: '包含文字' },
  { value: '>', label: '大于' },
  { value: '>=', label: '大于或等于' },
  { value: '<', label: '小于' },
  { value: '<=', label: '小于或等于' },
]

/**
 * 比较值的来源。
 * expr 非空表示「系统变量」，生成的 domain 里直接写变量名；
 * expr 为空表示由管理员手填常量（如部门 ID、金额）。
 */
export const DOMAIN_VALUE_SOURCES = [
  { value: 'USER_SELF', label: '当前登录用户本人', expr: 'user.id' },
  { value: 'USER_DEPT', label: '当前用户所属部门', expr: 'user.deptId' },
  { value: 'USER_COMPANY', label: '当前用户所属公司', expr: 'user.companyId' },
  { value: 'COMPANY_IDS', label: '当前用户可见的全部组织', expr: 'company_ids' },
  { value: 'CONST', label: '手动填写一个值…', expr: '' },
]

/** 值来源 → 表达式（供 domain 拼装） */
export function valueSourceExpr(source: string): string {
  return DOMAIN_VALUE_SOURCES.find(s => s.value === source)?.expr ?? ''
}
