import type { App } from 'vue'
import SupplierTable from './SupplierTable.vue'
import type { Supplier, TableColumn } from '../../../types'

export * from './SupplierTable.vue'
export { SupplierTable }

export interface SupplierTableProps {
  data: Supplier[]
  loading?: boolean
  columns?: TableColumn<Supplier>[]
  showActions?: boolean
  selectable?: boolean
  pagination?: boolean
  pageSize?: number
  total?: number
  currentPage?: number
}

export interface SupplierTableEmits {
  (e: 'selection-change', selection: Supplier[]): void
  (e: 'edit', row: Supplier): void
  (e: 'delete', row: Supplier): void
  (e: 'view', row: Supplier): void
  (e: 'page-change', page: number): void
  (e: 'sort-change', sort: { prop: string; order: 'ascending' | 'descending' }): void
}

export default {
  install(app: App) {
    app.component('SupplierTable', SupplierTable)
  }
}

export const defaultColumns: TableColumn<Supplier>[] = [
  {
    prop: 'name',
    label: '供应商名称',
    width: 180,
    fixed: 'left',
    sortable: true,
  },
  {
    prop: 'code',
    label: '供应商代码',
    width: 120,
    sortable: true,
  },
  {
    prop: 'type',
    label: '供应商类型',
    width: 120,
    formatter: (row) => {
      const types = {
        'manufacturer': '生产商',
        'distributor': '经销商',
        'retailer': '零售商',
        'service': '服务商',
        'other': '其他',
      }
      return types[row.type as keyof typeof types] || row.type
    },
  },
  {
    prop: 'contact',
    label: '联系人',
    width: 120,
  },
  {
    prop: 'phone',
    label: '联系电话',
    width: 130,
  },
  {
    prop: 'email',
    label: '邮箱',
    width: 180,
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    formatter: (row) => {
      const statusMap = {
        'active': { text: '活跃', color: 'success' },
        'inactive': { text: '停用', color: 'danger' },
        'pending': { text: '待审核', color: 'warning' },
      }
      const status = statusMap[row.status as keyof typeof statusMap]
      return status ? status.text : row.status
    },
  },
  {
    prop: 'rating',
    label: '评分',
    width: 100,
    formatter: (row) => {
      if (!row.rating) return '暂无评分'
      return `${row.rating}/5.0`
    },
  },
  {
    prop: 'createdAt',
    label: '创建时间',
    width: 160,
    formatter: (row) => {
      if (!row.createdAt) return '-'
      const date = new Date(row.createdAt)
      return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour12: false })
    },
    sortable: true,
  },
]