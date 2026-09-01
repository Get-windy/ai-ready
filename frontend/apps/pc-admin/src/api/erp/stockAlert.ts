/**
 * 预警查询 API 模块
 *
 * 对应后端 cn.aiedge.erp.stock.controller.StockAlertQueryController（/api/erp/stock-alert）
 */
import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/api/erp'

/** 预警查询行（触发上下限预警的商品清单） */
export interface StockAlertRow {
  id: string
  productId: string
  /** 货号 */
  productCode: string
  /** 商品名称 */
  productName: string
  /** 口味 */
  taste: string
  /** 型号 */
  model: string
  /** 单位 */
  unit: string
  /** 小单位 */
  smallUnit: string
  /** 条码 */
  barcode: string
  /** 规格 */
  spec: string
  /** 产地 */
  origin: string
  /** 品牌 */
  brand: string
  /** 预警仓库ID */
  warehouseId: string
  /** 预警仓库 */
  warehouseName: string
  /** 库存上限 */
  maxStock: number
  /** 库存下限 */
  minStock: number
  /** 安全库存 */
  safetyStock: number
  /** 账面库存 */
  bookQty: number
  /** 差异数量 */
  diffQty: number
  /** 预警类型：LOW_STOCK=下限预警，OVER_STOCK=上限预警 */
  alertType: 'LOW_STOCK' | 'OVER_STOCK'
  /** 说明 */
  remark: string
}

export const stockAlertQueryApi = {
  page(params: PageQuery): Promise<PageResult<StockAlertRow>> {
    return request.get('/erp/stock-alert/page', params)
  }
}
