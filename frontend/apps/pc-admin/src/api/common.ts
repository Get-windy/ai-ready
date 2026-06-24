/**
 * 公共类型定义
 */

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface TreeNode {
  id: number
  name: string
  parentId: number
  children?: TreeNode[]
}