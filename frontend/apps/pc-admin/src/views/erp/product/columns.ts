/**
 * 商品列表 4 个子 Tab 的列定义（对标 ql361 商品页 列配置全量列）
 *
 * 对标口径：
 *  - 全部商品 28 列（默认显示 14：图片/商品名称/商品货号/所属行业类别/条码/规格/型号/产地/品牌/单位/可用库存/换算关系/餐饮店/备注）
 *  - 套餐      7 列（全部默认显示）
 *  - 商品上架 48 列（默认显示 34：14 个基础列 + 20 个行业标签列）
 *  - 商品授权 11 列（默认显示 8：图片/商品名称/商品货号/屏蔽客户/屏蔽级别/屏蔽区域/条码/规格）
 */
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'

/**
 * 商品标签 = **标准槽位 + 用户自定义昵称**（数据源：资料 → 商品辅助资料 → 商品标签，即 `erp_mall_tag`）。
 *
 * - 槽位固定 `TAG_1..TAG_20`，默认昵称「标签1…标签20」，用户可改成「早餐面点」等业务别名；
 * - 商品侧 `erp_product.mall_tags` 存**槽位编码**，改昵称不影响历史数据；
 * - 「商品上架」子标签的标签列与表单「商城信息」的标签勾选项**同源**（前者按列展开、后者按复选框）。
 * 故两处均按标签字典动态渲染，不再硬编码任何昵称（原 MALL_TAGS / MALL_INFO_TAGS 已删除）。
 */
export interface MallTagLite {
  tagCode: string
  tagName: string
}

/**
 * 价格等级标准槽位（固定 8 个，系统内部一律以 GRADE_1..GRADE_8 为键）。
 * 界面上显示的「餐饮店 / 食堂团餐 / …」是**用户对价格等级自定义的昵称**，
 * 存在 erp_product_grade.grade_name，按 grade_code 关联；未配置昵称时回落到标准名（价格等级N）。
 */
export const GRADE_SLOT_CODES = [
  'GRADE_1', 'GRADE_2', 'GRADE_3', 'GRADE_4', 'GRADE_5', 'GRADE_6', 'GRADE_7', 'GRADE_8',
] as const

/** 价格等级标准名（未自定义昵称时的兜底显示名） */
export const DEFAULT_GRADE_NAMES = [
  '价格等级1', '价格等级2', '价格等级3', '价格等级4',
  '价格等级5', '价格等级6', '价格等级7', '价格等级8',
]

/** 取第 level 个标准价格等级的显示名（昵称优先，缺省用标准名） */
export function gradeNameOfLevel(
  grades: { gradeCode?: string; gradeName?: string }[],
  level: number,
): string {
  const code = GRADE_SLOT_CODES[level - 1]
  const g = (grades || []).find(x => String(x?.gradeCode || '').toUpperCase() === code)
  return g?.gradeName || DEFAULT_GRADE_NAMES[level - 1]
}

export const PRODUCT_TYPE_MAP: Record<string, string> = {
  SINGLE: '单品',
  KIT: '套餐',
  SERVICE: '服务',
}

export const SHELF_LEVEL_MAP: Record<string, { label: string; color: string }> = {
  ALL: { label: '完全屏蔽', color: 'red' },
  CONSULT: { label: '需询价', color: 'orange' },
  HIDDEN_PRICE: { label: '隐藏价格', color: 'blue' },
}

export const money = (v: any) => (v == null || v === '' ? '-' : Number(v).toFixed(2))
export const qty = (v: any) => (v == null || v === '' ? '0' : String(Number(v)))

/** 8 个价格等级列（key = _grade1.._grade8，值为 gradePriceMap[GRADE_N]） */
export function buildGradeColumns(grades: { gradeCode: string; gradeName: string }[]): DetailColumnConfig[] {
  return GRADE_SLOT_CODES.map((code, i) => ({
    key: `_grade${i + 1}`,
    title: gradeNameOfLevel(grades, i + 1),
    type: 'input' as const,
    width: 110,
    align: 'right' as const,
    readonly: true,
    hideable: true,
    // 对标默认仅显示第 1 个价格等级；槽位固定，昵称变化不影响列位
    defaultHidden: i !== 0,
    formatter: (v: any) => money(v),
  }))
}

/** 全部商品 tab：28 列 */
export function buildAllColumns(grades: { gradeCode: string; gradeName: string }[]): DetailColumnConfig[] {
  return [
    { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageCell', width: 60, hideable: true },
    { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 220, sortable: true, hideable: true },
    { key: 'productCodeAlias', title: '商品货号', width: 130, sortable: true, readonly: true },
    { key: 'industryCategory', title: '所属行业类别', width: 120, readonly: true },
    { key: 'barcode', title: '条码', width: 140, sortable: true, readonly: true },
    { key: 'defaultWarehouseName', title: '默认仓库', width: 110, readonly: true, hideable: true, defaultHidden: true },
    { key: 'spec', title: '规格', width: 130, sortable: true, readonly: true },
    { key: 'model', title: '型号', width: 100, readonly: true },
    { key: 'origin', title: '产地', width: 90, readonly: true },
    { key: 'brand', title: '品牌', width: 100, readonly: true },
    { key: 'unit', title: '单位', width: 70, readonly: true },
    { key: 'productType', title: '商品类型', width: 90, readonly: true, hideable: true, defaultHidden: true, formatter: (v: any) => PRODUCT_TYPE_MAP[v] || v || '-' },
    { key: 'availableStock', title: '可用库存', width: 100, align: 'right', readonly: true, formatter: qty },
    { key: 'conversionRelation', title: '换算关系', width: 140, readonly: true },
    { key: 'retailPrice', title: '零售价', width: 90, align: 'right', readonly: true, hideable: true, defaultHidden: true, formatter: money },
    { key: 'wholesalePrice', title: '批发价', width: 90, align: 'right', readonly: true, hideable: true, defaultHidden: true, formatter: money },
    { key: 'purchasePrice', title: '预设进价', width: 90, align: 'right', readonly: true, hideable: true, defaultHidden: true, formatter: money },
    ...buildGradeColumns(grades),
    { key: 'mallPoints', title: '商品积分', width: 90, align: 'right', readonly: true, hideable: true, defaultHidden: true },
    { key: 'createTime', title: '新增时间', width: 150, readonly: true, hideable: true, defaultHidden: true },
    { key: 'remark', title: '备注', width: 150, readonly: true },
  ]
}

/** 套餐 tab：7 列（全部默认显示） */
export function buildKitColumns(): DetailColumnConfig[] {
  return [
    { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageCell', width: 60, hideable: true },
    { key: 'kitName', title: '套餐名称', type: 'slot', slotName: 'kitNameCell', width: 220, sortable: true, hideable: true },
    { key: 'kitCode', title: '套餐编号', width: 140, readonly: true },
    { key: 'kitPrice', title: '套餐金额', width: 110, align: 'right', readonly: true, formatter: money },
    { key: 'barcode', title: '套餐条码', width: 150, readonly: true },
    { key: 'bundleSale', title: '捆绑销售', width: 100, readonly: true, formatter: (v: any) => (v === false ? '否' : '是') },
    { key: 'itemsSummary', title: '商品明细', width: 320, readonly: true },
  ]
}

/** 商品上架 tab：基础 28 列 + 标签列（数量 = 标签字典槽位数的动态列） */
export function buildShelfColumns(
  grades: { gradeCode: string; gradeName: string }[],
  tags: MallTagLite[] = [],
): DetailColumnConfig[] {
  const tagColumns: DetailColumnConfig[] = (tags || []).map(t => ({
    key: `tag_${t.tagCode}`,
    title: t.tagName,
    width: 96,
    readonly: true,
    hideable: true,
    formatter: (v: any) => (v ? '√' : ''),
  }))
  return [
    { key: 'mallShelfStatus', title: '上架', type: 'slot', slotName: 'shelfCell', width: 70, hideable: true },
    { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageCell', width: 60, hideable: true },
    { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 220, sortable: true, hideable: true },
    { key: 'productCodeAlias', title: '商品货号', width: 130, readonly: true },
    { key: 'barcode', title: '条码', width: 140, readonly: true },
    { key: 'spec', title: '规格', width: 120, readonly: true },
    { key: 'model', title: '型号', width: 100, readonly: true, hideable: true, defaultHidden: true },
    { key: 'origin', title: '产地', width: 90, readonly: true, hideable: true, defaultHidden: true },
    { key: 'brand', title: '品牌', width: 100, readonly: true, hideable: true, defaultHidden: true },
    { key: 'unit', title: '单位', width: 70, readonly: true },
    { key: 'availableStock', title: '可用库存', width: 100, align: 'right', readonly: true, formatter: qty },
    { key: 'retailPrice', title: '零售价', width: 90, align: 'right', readonly: true, formatter: money },
    { key: 'wholesalePrice', title: '批发价', width: 90, align: 'right', readonly: true, formatter: money },
    { key: 'purchasePrice', title: '预设进价', width: 90, align: 'right', readonly: true, hideable: true, defaultHidden: true, formatter: money },
    ...buildGradeColumns(grades).map(c => ({ ...c, defaultHidden: true })),
    {
      key: 'mallSortType', title: '排序', width: 110, type: 'select', hideable: true,
      options: [
        { label: '默认', value: 'DEFAULT' },
        { label: '按销量', value: 'SALES' },
        { label: '手动排序', value: 'MANUAL' },
      ],
    },
    { key: 'mallSortOrder', title: '排序值', width: 90, type: 'number', precision: 0, min: 0 },
    { key: 'mallMinOrderQty', title: '起订量', width: 90, type: 'number', precision: 0, min: 0 },
    { key: 'mallPoints', title: '商品积分', width: 90, align: 'right', type: 'number', precision: 2, min: 0, hideable: true, defaultHidden: true },
    { key: 'remark', title: '备注', width: 150, readonly: true },
    ...tagColumns,
    { key: 'keywords', title: '关键字', width: 160, type: 'input', hideable: true, defaultHidden: true },
  ]
}

/** 商品授权 tab：11 列（默认显示 8） */
export function buildShieldColumns(): DetailColumnConfig[] {
  return [
    { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageCell', width: 60, hideable: true },
    { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 220, sortable: true, hideable: true },
    { key: 'productCodeAlias', title: '商品货号', width: 130, readonly: true },
    { key: 'partnerName', title: '屏蔽客户', width: 180, readonly: true },
    { key: 'shieldLevel', title: '屏蔽级别', width: 110, readonly: true, formatter: (v: any) => SHELF_LEVEL_MAP[v]?.label || v || '-' },
    { key: 'region', title: '屏蔽区域', width: 140, readonly: true },
    { key: 'barcode', title: '条码', width: 140, readonly: true },
    { key: 'spec', title: '规格', width: 120, readonly: true },
    { key: 'model', title: '型号', width: 100, readonly: true, hideable: true, defaultHidden: true },
    { key: 'origin', title: '产地', width: 90, readonly: true, hideable: true, defaultHidden: true },
    { key: 'brand', title: '品牌', width: 100, readonly: true, hideable: true, defaultHidden: true },
  ]
}
