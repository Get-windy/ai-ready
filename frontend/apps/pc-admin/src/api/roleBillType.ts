import request from '@/utils/request'

/** 单据类型分配项 */
export interface BillTypeAssignment {
  billType: string
  permissionLevel: number
}

/** 单据类型详情（含中文名） */
export interface BillTypeDetail {
  billType: string
  billTypeName: string
  permissionLevel: number
}

/** 权限级别选项 */
export const PERMISSION_LEVEL_OPTIONS = [
  { value: 0, label: '无权限' },
  { value: 1, label: '查看' },
  { value: 2, label: '编辑' },
  { value: 3, label: '审核' },
]

export const roleBillTypeApi = {
  /** 获取角色的单据类型权限列表 */
  getRoleBillTypes(roleId: number) {
    return request.get<BillTypeDetail[]>(`/role-bill-type/${roleId}`)
  },

  /** 获取用户可访问的所有单据类型 */
  getUserBillTypes(userId: number) {
    return request.get<string[]>(`/role-bill-type/user/${userId}`)
  },

  /** 分配角色的单据类型权限 */
  assignBillTypes(roleId: number, billTypes: BillTypeAssignment[]) {
    return request.post(`/role-bill-type/${roleId}`, billTypes)
  },
}
