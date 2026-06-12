/**
 * WMS PDA 类型定义
 * 与后端控制器 DTO/Entity 对齐
 */

// ── 任务聚合 (PdaTaskController.TaskDTO) ────────────
export interface PdaTaskItem {
  taskId: number
  taskNo: string
  taskType: 'RECEIPT' | 'PUTAWAY' | 'PICK' | 'MOVE' | 'CHECK'
  status: number        // 0=待处理 1=处理中 2=已完成 3=已取消 4=异常
  warehouseName: string
  createTime: string
}

// ── API 通用响应 ────────────────────────────────────
export interface ApiResult<T> {
  code: number
  data: T
  message: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

// ── 收货 (WmsReceiptTask / WmsReceiptDetail) ─────────
export interface ReceiptTask {
  id: number
  taskNo: string
  sourceType: number
  sourceNo: string
  warehouseId: number
  warehouseName: string
  status: number
  createTime: string
}

export interface ReceiptDetail {
  id: number
  taskId: number
  productCode: string
  productName: string
  expectedQty: number
  receivedQty: number
  status: number
}

// ── 盘点 (WmsCheckTask / WmsCheckResult) ────────────
export interface CheckTask {
  id: number
  taskNo: string
  warehouseName: string
  status: number
  createTime: string
}

// ── 移库 (WmsMoveTask) ─────────────────────────────
export interface MoveTask {
  id: number
  taskNo: string
  status: number
  createTime: string
}

// ── 库存查询 (WmsInventory) ────────────────────────
export interface InventoryRecord {
  productCode: string
  productName: string
  locationCode: string
  quantity: number
  availableQuantity: number
  frozenQuantity: number
  batchNo: string
}
