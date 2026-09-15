/**
 * 人员核验 / 用车管理 共用配置（字典 / 查询字段 / 列定义）
 * 自 VerificationTabs.vue 抽出：纯常量搬迁，行为不变。
 */

import dayjs from 'dayjs'
import type { VehicleCheckData } from '../components/VehicleCheckForm.vue'

/** 时间格式化：列定义 formatter 共用（随列定义一起自 VerificationTabs.vue 抽出） */
export function formatDateTime(value?: string): string {
  if (!value) return '-'
  const d = dayjs(value)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : String(value)
}

// ═══ 字典 ═══
export const VERIFY_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待提交', color: 'default' },
  1: { text: '待审核', color: 'orange' },
  2: { text: '已通过', color: 'green' },
  3: { text: '已驳回', color: 'red' },
  4: { text: '已过期', color: 'volcano' },
}
export const BINDING_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '绑定中', color: 'blue' },
  1: { text: '已交车', color: 'default' },
  2: { text: '异常解绑', color: 'red' },
}
export const ALERT_TYPE_MAP: Record<number, { text: string; color: string }> = {
  1: { text: '人车分离', color: 'red' },
  2: { text: '异常滞留', color: 'orange' },
  3: { text: '速度异常', color: 'gold' },
  4: { text: '偏离路线', color: 'purple' },
  5: { text: '绑定超时', color: 'volcano' },
  6: { text: '非工作时段用车', color: 'magenta' },
  7: { text: '证照/资质到期', color: 'cyan' },
}
export const ALERT_LEVEL_MAP: Record<number, { text: string; color: string }> = {
  1: { text: '提示', color: 'green' },
  2: { text: '警告', color: 'orange' },
  3: { text: '严重', color: 'red' },
}
export const HANDLE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'orange' },
  1: { text: '已确认', color: 'blue' },
  2: { text: '已忽略', color: 'default' },
  3: { text: '已处理', color: 'green' },
}
export const INSPECTION_TYPE_MAP: Record<number, { text: string; color: string }> = {
  1: { text: '出车前检查', color: 'blue' },
  2: { text: '收车后检查', color: 'purple' },
  3: { text: '随机抽检', color: 'cyan' },
  4: { text: '定期检查', color: 'green' },
}
export const CERT_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待核验', color: 'default' },
  1: { text: '有效', color: 'green' },
  2: { text: '已过期', color: 'red' },
  3: { text: '无效', color: 'default' },
}
export const CARD_TYPE_OPTIONS = [
  { value: 1, label: '油卡' },
  { value: 2, label: '电卡' },
  { value: 3, label: '换电套餐' },
  { value: 4, label: '充电套餐' },
  { value: 5, label: '加气卡' },
]
export const ENERGY_TYPE_OPTIONS = [
  { value: 1, label: '汽油' },
  { value: 2, label: '柴油' },
  { value: 3, label: '充电' },
  { value: 4, label: '换电' },
  { value: 5, label: '加气' },
]
export const PAY_MODE_OPTIONS = [
  { value: 1, label: '现金' },
  { value: 2, label: '油卡' },
  { value: 3, label: '电卡' },
  { value: 4, label: '月租套餐' },
  { value: 5, label: '平台代扣' },
]

export const CERT_TYPE_OPTIONS = [
  { value: 1, label: '驾驶证' },
  { value: 2, label: '行驶证' },
  { value: 3, label: '健康证' },
  { value: 4, label: '从业资格证' },
  { value: 5, label: '其他' },
]
export const RIDER_TYPE_MAP: Record<number, string> = {
  1: '企业员工', 2: '众包兼职', 3: '外部平台配送员', 4: '社会车辆司机',
}

// ═══ 查询字段定义（每 Tab 独立） ═══
export interface SearchFieldDef {
  key: string
  label: string
  type: 'input' | 'select' | 'dateRange' | 'checkbox'
  placeholder?: string
  options?: Array<{ label: string; value: any }>
}

export const KYC_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'date', label: '创建日期', type: 'dateRange' },
  { key: 'riderName', label: '配送员', type: 'input', placeholder: '配送员姓名' },
  { key: 'riderPhone', label: '手机号', type: 'input', placeholder: '手机号' },
  {
    key: 'riderType', label: '身份类型', type: 'select',
    options: [
      { label: '企业员工', value: 1 }, { label: '众包兼职', value: 2 },
      { label: '外部平台配送员', value: 3 }, { label: '社会车辆司机', value: 4 },
    ],
  },
  {
    key: 'verifyStatus', label: '认证状态', type: 'select',
    options: [
      { label: '待提交', value: 0 }, { label: '待审核', value: 1 }, { label: '已通过', value: 2 },
      { label: '已驳回', value: 3 }, { label: '已过期', value: 4 },
    ],
  },
  {
    key: 'eligible', label: '接单资质', type: 'select',
    options: [{ label: '具备', value: true }, { label: '不具备', value: false }],
  },
  { key: 'expiring', label: '仅看到期预警', type: 'checkbox' },
]

export const BINDING_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'date', label: '绑定日期', type: 'dateRange' },
  { key: 'riderName', label: '配送员', type: 'input', placeholder: '配送员姓名' },
  { key: 'plateNo', label: '车牌号', type: 'input', placeholder: '车牌号' },
  {
    key: 'status', label: '状态', type: 'select',
    options: [{ label: '绑定中', value: 0 }, { label: '已交车', value: 1 }, { label: '异常解绑', value: 2 }],
  },
]

export const ALERT_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'date', label: '发生日期', type: 'dateRange' },
  {
    key: 'alertType', label: '预警类型', type: 'select',
    options: [
      { label: '人车分离', value: 1 }, { label: '异常滞留', value: 2 }, { label: '速度异常', value: 3 },
      { label: '偏离路线', value: 4 }, { label: '绑定超时', value: 5 }, { label: '非工作时段用车', value: 6 },
      { label: '证照/资质到期', value: 7 },
    ],
  },
  {
    key: 'alertLevel', label: '级别', type: 'select',
    options: [{ label: '提示', value: 1 }, { label: '警告', value: 2 }, { label: '严重', value: 3 }],
  },
  {
    key: 'handleStatus', label: '处理状态', type: 'select',
    options: [
      { label: '待处理', value: 0 }, { label: '已确认', value: 1 },
      { label: '已忽略', value: 2 }, { label: '已处理', value: 3 },
    ],
  },
  { key: 'riderName', label: '配送员', type: 'input', placeholder: '配送员姓名' },
  { key: 'plateNo', label: '车牌号', type: 'input', placeholder: '车牌号' },
]

export const INSPECTION_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'date', label: '巡检日期', type: 'dateRange' },
  { key: 'plateNo', label: '车牌号', type: 'input', placeholder: '车牌号' },
  { key: 'riderName', label: '配送员', type: 'input', placeholder: '配送员姓名' },
  {
    key: 'inspectionType', label: '巡检类型', type: 'select',
    options: [
      { label: '出车前检查', value: 1 }, { label: '收车后检查', value: 2 },
      { label: '随机抽检', value: 3 }, { label: '定期检查', value: 4 },
    ],
  },
  {
    key: 'result', label: '结果', type: 'select',
    options: [{ label: '通过', value: 1 }, { label: '不通过', value: 2 }],
  },
]

export const ENERGY_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'date', label: '补能日期', type: 'dateRange' },
  {
    key: 'subjectType', label: '主体', type: 'select',
    options: [{ label: '四轮车', value: 'VEHICLE' }, { label: '骑手两轮车', value: 'RIDER' }],
  },
  {
    key: 'energyType', label: '补能类型', type: 'select',
    options: [
      { label: '汽油', value: 1 }, { label: '柴油', value: 2 }, { label: '充电', value: 3 },
      { label: '换电', value: 4 }, { label: '加气', value: 5 },
    ],
  },
  {
    key: 'payMode', label: '支付方式', type: 'select',
    options: [
      { label: '现金', value: 1 }, { label: '油卡', value: 2 }, { label: '电卡', value: 3 },
      { label: '月租套餐', value: 4 }, { label: '平台代扣', value: 5 },
    ],
  },
  { key: 'keyword', label: '关键字', type: 'input', placeholder: '车牌/姓名/站点/卡号' },
  { key: 'abnormalOnly', label: '仅看异常', type: 'checkbox' },
]

export const CERT_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'riderName', label: '配送员', type: 'input', placeholder: '持证人姓名' },
  {
    key: 'certType', label: '证照类型', type: 'select',
    options: [
      { label: '驾驶证', value: 1 }, { label: '行驶证', value: 2 }, { label: '健康证', value: 3 },
      { label: '从业资格证', value: 4 }, { label: '其他', value: 5 },
    ],
  },
  {
    key: 'verifyStatus', label: '证照状态', type: 'select',
    options: [
      { label: '待核验', value: 0 }, { label: '有效', value: 1 },
      { label: '已过期', value: 2 }, { label: '无效', value: 3 },
    ],
  },
  { key: 'expiring', label: '仅看到期预警', type: 'checkbox' },
]

export const ENERGY_CARD_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'keyword', label: '关键字', type: 'input', placeholder: '卡号/卡名称/发卡方' },
  {
    key: 'cardType', label: '卡类型', type: 'select',
    options: [
      { label: '油卡', value: 1 }, { label: '电卡', value: 2 }, { label: '换电套餐', value: 3 },
      { label: '充电套餐', value: 4 }, { label: '加气卡', value: 5 },
    ],
  },
  {
    key: 'status', label: '状态', type: 'select',
    options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }],
  },
]

// 能耗报表按日期区间看
export const ENERGY_STATS_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'date', label: '统计区间', type: 'dateRange' },
  {
    key: 'subjectType', label: '主体', type: 'select',
    options: [{ label: '四轮车', value: 'VEHICLE' }, { label: '骑手两轮车', value: 'RIDER' }],
  },
]

export const ONBOARDING_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'keyword', label: '配送员', type: 'input', placeholder: '姓名 / 手机号' },
  { key: 'onlyBlocked', label: '仅看不可出车', type: 'checkbox' },
]


export interface ColumnDef {
  title: string
  key: string
  field?: string
  type?: string
  slotName?: string
  width?: number
  fixed?: string
  align?: string
  defaultHidden?: boolean
  sortable?: boolean
  formatter?: (value: any, record?: any) => string
}

export const KYC_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 140, fixed: 'right', slotName: 'actionCell' },
  { title: '配送员', field: 'riderName', key: 'riderName', width: 110, sortable: true },
  { title: '手机号', field: 'riderPhone', key: 'riderPhone', width: 120, defaultHidden: true },
  { title: '身份类型', field: 'riderTypeText', key: 'riderTypeText', width: 120 },
  { title: '所属渠道', field: 'channelName', key: 'channelName', width: 130, defaultHidden: true },
  { title: '认证状态', field: 'verifyStatus', key: 'verifyStatus', width: 100, type: 'slot', slotName: 'verifyStatusCell' },
  { title: '接单资质', field: 'eligible', key: 'eligible', width: 100, type: 'slot', slotName: 'eligibleCell' },
  { title: '证件姓名', field: 'realName', key: 'realName', width: 100, defaultHidden: true },
  { title: '身份证号', field: 'idCardNo', key: 'idCardNo', width: 160 },
  { title: '背书/审查机构', field: 'endorseOrg', key: 'endorseOrg', width: 150, defaultHidden: true },
  { title: '背书结论', field: 'endorseResult', key: 'endorseResult', width: 90, defaultHidden: true },
  { title: '有效期至', field: 'endorseExpireDate', key: 'endorseExpireDate', width: 110 },
  { title: '证照数', field: 'certCount', key: 'certCount', width: 80, align: 'right' },
  { title: '到期预警', field: 'expiringCertCount', key: 'expiringCertCount', width: 110, type: 'slot', slotName: 'expiringCell' },
  { title: '审核人', field: 'auditBy', key: 'auditBy', width: 100, defaultHidden: true },
  { title: '审核时间', field: 'auditTime', key: 'auditTime', width: 160, defaultHidden: true, formatter: (v: any) => formatDateTime(v) },
  { title: '生效时间', field: 'effectiveTime', key: 'effectiveTime', width: 160, defaultHidden: true, formatter: (v: any) => formatDateTime(v) },
  { title: '审核意见', field: 'auditRemark', key: 'auditRemark', width: 150, defaultHidden: true },
  { title: '备注', field: 'remark', key: 'remark', width: 140, defaultHidden: true },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 160, defaultHidden: true, formatter: (v: any) => formatDateTime(v) },
]

export const BINDING_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right', slotName: 'actionCell' },
  { title: '配送员', field: 'riderName', key: 'riderName', width: 110, sortable: true },
  { title: '手机号', field: 'riderPhone', key: 'riderPhone', width: 120, defaultHidden: true },
  { title: '车牌号', field: 'plateNo', key: 'plateNo', width: 110, sortable: true },
  { title: '绑定时间', field: 'bindTime', key: 'bindTime', width: 160, formatter: (v: any) => formatDateTime(v) },
  { title: '绑定里程(km)', field: 'bindMileage', key: 'bindMileage', width: 110, align: 'right' },
  { title: '交车时间', field: 'handoverTime', key: 'handoverTime', width: 160, formatter: (v: any) => formatDateTime(v) },
  { title: '交车里程(km)', field: 'handoverMileage', key: 'handoverMileage', width: 110, align: 'right' },
  { title: '交车地点', field: 'handoverLocation', key: 'handoverLocation', width: 160 },
  { title: '状态', field: 'status', key: 'status', width: 90, type: 'slot', slotName: 'statusCell' },
  { title: '绑定原因', field: 'bindReason', key: 'bindReason', width: 150, defaultHidden: true },
  { title: '备注', field: 'remark', key: 'remark', width: 140, defaultHidden: true },
]

export const ALERT_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 150, fixed: 'right', slotName: 'actionCell' },
  { title: '预警类型', field: 'alertType', key: 'alertType', width: 130, type: 'slot', slotName: 'alertTypeCell' },
  { title: '级别', field: 'alertLevel', key: 'alertLevel', width: 80, type: 'slot', slotName: 'alertLevelCell' },
  { title: '预警内容', field: 'alertContent', key: 'alertContent', width: 300 },
  { title: '配送员', field: 'riderName', key: 'riderName', width: 110 },
  { title: '车牌号', field: 'plateNo', key: 'plateNo', width: 110 },
  { title: '人车距离(米)', field: 'distanceMeters', key: 'distanceMeters', width: 120, align: 'right', defaultHidden: true },
  { title: '滞留时长(秒)', field: 'stayDuration', key: 'stayDuration', width: 120, align: 'right', defaultHidden: true },
  { title: '处理状态', field: 'handleStatus', key: 'handleStatus', width: 100, type: 'slot', slotName: 'handleStatusCell' },
  { title: '处理人', field: 'handler', key: 'handler', width: 100, defaultHidden: true },
  { title: '处理时间', field: 'handleTime', key: 'handleTime', width: 160, defaultHidden: true, formatter: (v: any) => formatDateTime(v) },
  { title: '处理备注', field: 'handleRemark', key: 'handleRemark', width: 150, defaultHidden: true },
  { title: '发生时间', field: 'createTime', key: 'createTime', width: 160, formatter: (v: any) => formatDateTime(v) },
]

export const INSPECTION_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 90, fixed: 'right', slotName: 'actionCell' },
  { title: '车牌号', field: 'plateNo', key: 'plateNo', width: 110, sortable: true },
  { title: '配送员', field: 'riderName', key: 'riderName', width: 110 },
  { title: '巡检类型', field: 'inspectionType', key: 'inspectionType', width: 120, type: 'slot', slotName: 'inspectionTypeCell' },
  { title: '结果', field: 'result', key: 'result', width: 90, type: 'slot', slotName: 'resultCell' },
  { title: '巡检时间', field: 'inspectionTime', key: 'inspectionTime', width: 160, formatter: (v: any) => formatDateTime(v) },
  { title: '里程(km)', field: 'mileage', key: 'mileage', width: 100, align: 'right' },
  { title: '油量(%)', field: 'fuelLevel', key: 'fuelLevel', width: 90, align: 'right' },
  { title: '外观', field: 'exteriorStatus', key: 'exteriorStatus', width: 80, type: 'slot', slotName: 'exteriorCell', defaultHidden: true },
  { title: '轮胎', field: 'tireStatus', key: 'tireStatus', width: 80, type: 'slot', slotName: 'tireCell', defaultHidden: true },
  { title: '灯光', field: 'lightStatus', key: 'lightStatus', width: 80, type: 'slot', slotName: 'lightCell', defaultHidden: true },
  { title: '刹车', field: 'brakeStatus', key: 'brakeStatus', width: 80, type: 'slot', slotName: 'brakeCell', defaultHidden: true },
  { title: '清洁', field: 'cleanlinessStatus', key: 'cleanlinessStatus', width: 80, type: 'slot', slotName: 'cleanlinessCell', defaultHidden: true },
  { title: '灭火器', field: 'fireExtinguisher', key: 'fireExtinguisher', width: 100, type: 'slot', slotName: 'extinguisherCell', defaultHidden: true },
  { title: '三角警示牌', field: 'warningTriangle', key: 'warningTriangle', width: 110, type: 'slot', slotName: 'triangleCell', defaultHidden: true },
  { title: '巡检地点', field: 'inspectionLocation', key: 'inspectionLocation', width: 150 },
  { title: '审核人', field: 'reviewer', key: 'reviewer', width: 100, defaultHidden: true },
  { title: '审核时间', field: 'reviewTime', key: 'reviewTime', width: 160, defaultHidden: true, formatter: (v: any) => formatDateTime(v) },
  { title: '审核意见', field: 'reviewRemark', key: 'reviewRemark', width: 150, defaultHidden: true },
  { title: '备注', field: 'remark', key: 'remark', width: 140, defaultHidden: true },
]

export const ENERGY_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right', slotName: 'actionCell' },
  { title: '补能时间', field: 'occurredAt', key: 'occurredAt', width: 160, sortable: true, formatter: (v: any) => formatDateTime(v) },
  { title: '主体类型', field: 'subjectType', key: 'subjectType', width: 110, type: 'slot', slotName: 'subjectTypeCell' },
  { title: '车牌号/配送员', field: 'subjectName', key: 'subjectName', width: 150, sortable: true },
  { title: '补能类型', field: 'energyTypeText', key: 'energyTypeText', width: 100 },
  { title: '支付方式', field: 'payModeText', key: 'payModeText', width: 100, defaultHidden: true },
  { title: '数量', field: 'quantity', key: 'quantity', width: 90, align: 'right' },
  { title: '单价', field: 'unitPrice', key: 'unitPrice', width: 90, align: 'right' },
  { title: '金额(元)', field: 'amountYuan', key: 'amountYuan', width: 100, align: 'right' },
  { title: '仪表里程(km)', field: 'odometer', key: 'odometer', width: 110, align: 'right' },
  { title: '区间里程(km)', field: 'mileageSinceLast', key: 'mileageSinceLast', width: 110, align: 'right' },
  { title: '每公里成本(元)', field: 'unitCost', key: 'unitCost', width: 120, align: 'right' },
  { title: '百公里油耗(L)', field: 'fuelPer100Km', key: 'fuelPer100Km', width: 120, align: 'right', defaultHidden: true },
  { title: '站点/商户', field: 'station', key: 'station', width: 150 },
  { title: '卡号/套餐', field: 'cardNo', key: 'cardNo', width: 130, defaultHidden: true },
  { title: '异常', field: 'abnormalFlag', key: 'abnormalFlag', width: 90, type: 'slot', slotName: 'abnormalCell' },
  { title: '异常原因', field: 'abnormalReason', key: 'abnormalReason', width: 200, defaultHidden: true },
  { title: '凭证', field: 'voucherUrl', key: 'voucherUrl', width: 90, defaultHidden: true },
  { title: '备注', field: 'remark', key: 'remark', width: 140, defaultHidden: true },
]

export const CERT_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 100, fixed: 'right', slotName: 'actionCell' },
  { title: '持证人', field: 'riderName', key: 'riderName', width: 110, sortable: true },
  { title: '手机号', field: 'riderPhone', key: 'riderPhone', width: 120, defaultHidden: true },
  { title: '证照类型', field: 'certTypeText', key: 'certTypeText', width: 110 },
  { title: '证照编号', field: 'certNo', key: 'certNo', width: 160 },
  { title: '发证日期', field: 'issueDate', key: 'issueDate', width: 110 },
  { title: '有效期至', field: 'expireDate', key: 'expireDate', width: 110, sortable: true },
  { title: '剩余天数', field: 'daysToExpire', key: 'daysToExpire', width: 110, type: 'slot', slotName: 'certExpireCell' },
  { title: '证照状态', field: 'verifyStatusText', key: 'verifyStatusText', width: 100, type: 'slot', slotName: 'certStatusCell' },
  { title: '附件', field: 'certUrl', key: 'certUrl', width: 90, defaultHidden: true },
  { title: '备注', field: 'remark', key: 'remark', width: 140, defaultHidden: true },
]

export const ENERGY_CARD_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 170, fixed: 'right', slotName: 'actionCell' },
  { title: '卡号/套餐号', field: 'cardNo', key: 'cardNo', width: 150, sortable: true },
  { title: '卡名称', field: 'cardName', key: 'cardName', width: 140 },
  { title: '卡类型', field: 'cardTypeText', key: 'cardTypeText', width: 110 },
  { title: '绑定主体', field: 'subjectName', key: 'subjectName', width: 140, type: 'slot', slotName: 'cardSubjectCell' },
  { title: '发卡方', field: 'issuer', key: 'issuer', width: 130 },
  { title: '月费(元)', field: 'monthlyFee', key: 'monthlyFee', width: 100, align: 'right' },
  { title: '余额(元)', field: 'balance', key: 'balance', width: 100, align: 'right' },
  { title: '额度/已用', field: 'quota', key: 'quota', width: 140, type: 'slot', slotName: 'cardQuotaCell' },
  { title: '生效日期', field: 'startDate', key: 'startDate', width: 110 },
  { title: '有效期至', field: 'expireDate', key: 'expireDate', width: 110, sortable: true },
  { title: '到期/逾期', field: 'daysToExpire', key: 'daysToExpire', width: 110, type: 'slot', slotName: 'cardExpireCell' },
  { title: '状态', field: 'status', key: 'status', width: 90, type: 'slot', slotName: 'cardStatusCell' },
  { title: '备注', field: 'remark', key: 'remark', width: 140, defaultHidden: true },
]

export const ONBOARDING_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 110, fixed: 'right', slotName: 'actionCell' },
  { title: '配送员', field: 'riderName', key: 'riderName', width: 120, sortable: true },
  { title: '手机号', field: 'riderPhone', key: 'riderPhone', width: 120, defaultHidden: true },
  { title: '身份类型', field: 'riderTypeText', key: 'riderTypeText', width: 120 },
  { title: '实名状态', field: 'verifyStatusText', key: 'verifyStatusText', width: 100 },
  { title: '经办照', field: 'expiredCertCount', key: 'expiredCertCount', width: 90, align: 'right', defaultHidden: true },
  { title: '可否接单', field: 'eligible', key: 'eligible', width: 100, type: 'slot', slotName: 'onboardEligibleCell' },
  { title: '绑定车辆', field: 'plateNo', key: 'plateNo', width: 110 },
  { title: '车辆状态', field: 'vehicleStatusText', key: 'vehicleStatusText', width: 100 },
  { title: '车证最早到期', field: 'vehicleCertEarliestExpire', key: 'vehicleCertEarliestExpire', width: 130 },
  { title: '最近出车检查', field: 'lastInspectionResult', key: 'lastInspectionResult', width: 130, type: 'slot', slotName: 'onboardInspectionCell' },
  { title: '可否出车', field: 'canDrive', key: 'canDrive', width: 100, type: 'slot', slotName: 'onboardCanDriveCell' },
  { title: '阻塞原因', field: 'blockReasons', key: 'blockReasons', width: 320, defaultHidden: true },
]

export const INSPECTION_ITEM_LABELS = [
  { key: 'exteriorStatus', label: '车辆外观', remarkKey: 'exteriorRemark' },
  { key: 'tireStatus', label: '轮胎', remarkKey: 'tireRemark' },
  { key: 'lightStatus', label: '灯光', remarkKey: 'lightRemark' },
  { key: 'brakeStatus', label: '刹车', remarkKey: 'brakeRemark' },
  { key: 'cleanlinessStatus', label: '车内清洁', remarkKey: 'cleanlinessRemark' },
  { key: 'fireExtinguisher', label: '灭火器', remarkKey: null },
  { key: 'warningTriangle', label: '三角警示牌', remarkKey: null },
]

export function emptyCheck(): VehicleCheckData {
  return {
    mileage: undefined,
    fuelLevel: undefined,
    exteriorStatus: 0,
    exteriorRemark: '',
    exteriorPhotos: '',
    tireStatus: 0,
    tireRemark: '',
    lightStatus: 0,
    lightRemark: '',
    brakeStatus: 0,
    brakeRemark: '',
    cleanlinessStatus: 0,
    cleanlinessRemark: '',
    fireExtinguisher: 0,
    warningTriangle: 0,
    inspectionLocation: '',
    remark: '',
  }
}
