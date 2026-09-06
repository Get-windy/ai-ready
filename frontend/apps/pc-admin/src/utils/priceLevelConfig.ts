/**
 * 标准化产品价格等级（8 个）—— 全系统共享，勿重复定义
 *
 * 文档约定：这 8 个字段是用户对价格等级的自定义昵称，需按标准化产品价格等级处理。
 * 多页面（销售订单/采购退货/采购入库/移库/价格跟踪等）都用到，统一在此定义，避免重复开发。
 * 列表页用作展示列（key/title），表单页用作 dropdown/checkbox 列（key/title/type）。
 */
export interface PriceLevelDef {
  key: string
  title: string
}

export const PRICE_LEVELS: PriceLevelDef[] = [
  { key: 'restaurant', title: '餐饮店' },
  { key: 'canteen', title: '食堂团餐' },
  { key: 'outRestaurant', title: '外围餐饮店' },
  { key: 'vipSelf', title: '自助vip' },
  { key: 'largeGroup', title: '大团餐' },
  { key: 'vipLevel1', title: '重点|vip01' },
  { key: 'vipLevel2', title: '连锁|vip' },
  { key: 'specialCustomer', title: '特价客户' },
]

/**
 * 价格等级列定义（供列表页 BillTableList / 表单页 BillDetailTable 复用，默认隐藏，可在列配置中显示）
 * @param type 列类型：列表展示用 'input'（只读），表单输入用 'checkbox'
 */
export function buildPriceLevelColumns(type: 'input' | 'checkbox' = 'input') {
  return PRICE_LEVELS.map((p) => ({
    key: p.key,
    title: p.title,
    type,
    width: 70,
    defaultHidden: true,
  }))
}
