/**
 * 资料模块列表布局（PartnerListPage）的适配器类型
 *
 * 让「往来单位」以外的资料模块页面（如商品）也能复用同一套页面布局。
 */

/** 列表数据适配器 */
export interface MdListAdapter {
  /** 分页查询，返回 { records, total } */
  load: (params: Record<string, any>) => Promise<{ records: any[]; total: number }>
  /** 启用/停用（可选） */
  updateStatus?: (id: any, status: string) => Promise<any>
  /** 删除（可选） */
  remove?: (id: any) => Promise<any>
}

/** 分类树适配器 */
export interface MdCategoryAdapter {
  load: () => Promise<any[]>
  create: (payload: any) => Promise<any>
  update: (id: any, payload: any) => Promise<any>
  remove?: (id: any) => Promise<any>
}
