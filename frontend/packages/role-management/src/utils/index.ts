/**
 * 角色权限管理工具函数
 */

/**
 * 格式化日期时间
 */
export function formatDateTime(date: Date | string | number, format = 'YYYY-MM-DD HH:mm:ss'): string {
  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date
  
  if (isNaN(d.getTime())) {
    return String(date)
  }

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 将权限列表转换为树形结构
 */
export function buildPermissionTree(
  permissions: Array<{ id: string; parentId?: string; [key: string]: any }>,
  parentId?: string
): any[] {
  const tree: any[] = []
  
  permissions
    .filter(permission => permission.parentId === parentId)
    .forEach(permission => {
      const children = buildPermissionTree(permissions, permission.id)
      const node = {
        ...permission,
        children: children.length > 0 ? children : undefined
      }
      tree.push(node)
    })
  
  return tree
}

/**
 * 获取权限树的所有叶子节点
 */
export function getPermissionTreeLeaves(tree: any[]): any[] {
  const leaves: any[] = []
  
  function traverse(node: any) {
    if (!node.children || node.children.length === 0) {
      leaves.push(node)
    } else {
      node.children.forEach((child: any) => traverse(child))
    }
  }
  
  tree.forEach(node => traverse(node))
  return leaves
}

/**
 * 获取权限树中选中的节点
 */
export function getSelectedPermissionNodes(
  tree: any[],
  selectedKeys: string[]
): { selected: any[]; halfSelected: any[] } {
  const selected: any[] = []
  const halfSelected: any[] = []
  
  function traverse(node: any): { selected: boolean; halfSelected: boolean } {
    let nodeSelected = selectedKeys.includes(node.id)
    let hasSelectedChild = false
    let hasUnselectedChild = false
    
    if (node.children && node.children.length > 0) {
      node.children.forEach((child: any) => {
        const childResult = traverse(child)
        if (childResult.selected) hasSelectedChild = true
        if (!childResult.selected) hasUnselectedChild = true
      })
    }
    
    // 节点本身被选中
    if (nodeSelected) {
      selected.push(node)
      return { selected: true, halfSelected: false }
    }
    
    // 部分子节点被选中
    if (hasSelectedChild && hasUnselectedChild) {
      halfSelected.push(node)
      return { selected: false, halfSelected: true }
    }
    
    // 所有子节点都被选中
    if (hasSelectedChild && !hasUnselectedChild) {
      selected.push(node)
      return { selected: true, halfSelected: false }
    }
    
    return { selected: false, halfSelected: false }
  }
  
  tree.forEach(node => traverse(node))
  return { selected, halfSelected }
}

/**
 * 验证角色编码
 */
export function validateRoleCode(code: string): { valid: boolean; message?: string } {
  if (!code) {
    return { valid: false, message: '角色编码不能为空' }
  }
  
  if (code.length < 3 || code.length > 50) {
    return { valid: false, message: '角色编码长度必须在3-50个字符之间' }
  }
  
  if (!/^[a-zA-Z][a-zA-Z0-9_-]*$/.test(code)) {
    return { valid: false, message: '角色编码必须以字母开头，只能包含字母、数字、下划线和横线' }
  }
  
  return { valid: true }
}

/**
 * 验证角色名称
 */
export function validateRoleName(name: string): { valid: boolean; message?: string } {
  if (!name) {
    return { valid: false, message: '角色名称不能为空' }
  }
  
  if (name.length < 2 || name.length > 100) {
    return { valid: false, message: '角色名称长度必须在2-100个字符之间' }
  }
  
  return { valid: true }
}

/**
 * 计算角色权限统计
 */
export function calculateRolePermissionStats(role: any, allPermissions: any[]): {
  total: number
  menu: number
  button: number
  api: number
  data: number
} {
  const stats = { total: 0, menu: 0, button: 0, api: 0, data: 0 }
  
  if (!role?.permissionIds || !Array.isArray(role.permissionIds)) {
    return stats
  }
  
  role.permissionIds.forEach((permissionId: string) => {
    const permission = allPermissions.find(p => p.id === permissionId)
    if (permission) {
      stats.total++
      switch (permission.type) {
        case 'menu': stats.menu++; break
        case 'button': stats.button++; break
        case 'api': stats.api++; break
        case 'data': stats.data++; break
      }
    }
  })
  
  return stats
}

/**
 * 检查权限冲突
 */
export function checkPermissionConflicts(
  userPermissions: any[],
  rolePermissions: any[]
): Array<{ type: string; description: string; severity: 'low' | 'medium' | 'high' }> {
  const conflicts: Array<{ type: string; description: string; severity: 'low' | 'medium' | 'high' }> = []
  
  // 检查重复权限
  const userPermissionIds = new Set(userPermissions.map(p => p.id))
  const duplicatePermissions = rolePermissions.filter(p => userPermissionIds.has(p.id))
  
  if (duplicatePermissions.length > 0) {
    conflicts.push({
      type: 'duplicate',
      description: `发现 ${duplicatePermissions.length} 个重复权限`,
      severity: 'low'
    })
  }
  
  // 检查父子权限冲突（如果同时拥有父权限和子权限）
  const permissionMap = new Map(userPermissions.concat(rolePermissions).map(p => [p.id, p]))
  
  userPermissions.concat(rolePermissions).forEach(permission => {
    if (permission.parentId) {
      const parentPermission = permissionMap.get(permission.parentId)
      if (parentPermission && userPermissionIds.has(permission.id) && userPermissionIds.has(parentPermission.id)) {
        conflicts.push({
          type: 'parent-child',
          description: `权限 "${permission.name}" 与其父权限 "${parentPermission.name}" 同时存在`,
          severity: 'medium'
        })
      }
    }
  })
  
  return conflicts
}

/**
 * 生成随机ID
 */
export function generateId(prefix = ''): string {
  return prefix + Date.now().toString(36) + Math.random().toString(36).substring(2)
}

/**
 * 深拷贝对象
 */
export function deepClone<T>(obj: T): T {
  if (obj === null || typeof obj !== 'object') {
    return obj
  }
  
  if (obj instanceof Date) {
    return new Date(obj.getTime()) as any
  }
  
  if (obj instanceof Array) {
    return obj.map(item => deepClone(item)) as any
  }
  
  if (typeof obj === 'object') {
    const cloned: any = {}
    Object.keys(obj).forEach(key => {
      cloned[key] = deepClone((obj as any)[key])
    })
    return cloned
  }
  
  return obj
}

/**
 * 防抖函数
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout | null = null
  
  return function(...args: Parameters<T>) {
    if (timeout) {
      clearTimeout(timeout)
    }
    
    timeout = setTimeout(() => {
      func.apply(this, args)
    }, wait)
  }
}

/**
 * 节流函数
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle: boolean
  
  return function(...args: Parameters<T>) {
    if (!inThrottle) {
      func.apply(this, args)
      inThrottle = true
      setTimeout(() => (inThrottle = false), limit)
    }
  }
}

/**
 * 下载文件
 */
export function downloadFile(content: Blob | string, filename: string): void {
  const blob = typeof content === 'string' ? new Blob([content]) : content
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * 导出为CSV
 */
export function exportToCSV(data: any[], filename: string): void {
  if (!data.length) {
    console.warn('没有数据可导出')
    return
  }
  
  const headers = Object.keys(data[0])
  const csvContent = [
    headers.join(','),
    ...data.map(row => headers.map(header => {
      const value = row[header]
      // 处理包含逗号、引号或换行符的值
      if (typeof value === 'string' && (value.includes(',') || value.includes('"') || value.includes('\n'))) {
        return `"${value.replace(/"/g, '""')}"`
      }
      return value
    }).join(','))
  ].join('\n')
  
  downloadFile(csvContent, `${filename}.csv`)
}

/**
 * 检查浏览器支持
 */
export function checkBrowserSupport(): {
  supported: boolean
  issues: string[]
} {
  const issues: string[] = []
  
  // 检查fetch支持
  if (!window.fetch) {
    issues.push('浏览器不支持fetch API')
  }
  
  // 检查ES6支持
  try {
    new Function('const a = 1; let b = 2; class C {}; () => {}')
  } catch {
    issues.push('浏览器不支持ES6语法')
  }
  
  // 检查Promise支持
  if (!window.Promise) {
    issues.push('浏览器不支持Promise')
  }
  
  return {
    supported: issues.length === 0,
    issues
  }
}