import request from '@/utils/request'

/**
 * 设置 → 系统配置 → 菜单配置（菜单 80620）—— 租户级「菜单显隐」。
 *
 * · 读：全局菜单定义（sys_menu 的 tenant-admin 侧「目录 + 页面」）+ 本租户显隐状态
 * · 写：只落在本租户的配置行（sys_project_config，tenant_id = 当前会话租户）
 * · 路径一律写相对路径（axios baseURL 已是 /api，写成 /api/... 会双前缀 404）
 */

/** 菜单配置行（页面级菜单） */
export interface SetMenuConfigItem {
  /** 菜单 ID（雪花 ID，一律按字符串处理，禁止 Number()） */
  id: string
  /** 直接父目录 ID（= 所属分组） */
  parentId: string | null
  /** 所属分组名（直接父目录名） */
  groupName: string
  /** 一级域 ID（左侧「菜单分组」面板节点） */
  domainId: string | null
  /** 一级域名 */
  domainName: string
  /** 一级域排序（左侧分组面板排序用） */
  domainSort: number | null
  /** 菜单名称（页面名） */
  menuName: string
  /** 菜单编码 —— 页面上的「权限标识」列（sys_menu 实际列名是 menu_code，没有 perms 列） */
  menuCode: string
  /** 路由路径 */
  path: string | null
  /** 组件路径 */
  component: string | null
  /** 图标名 */
  icon: string | null
  /** 排序 */
  sort: number | null
  /** 全局菜单状态：1 = 启用，0 = 停用（后端 getUserMegaMenus 以 status = 1 过滤） */
  status: number | null
  /** 本租户是否显示该菜单（开关值） */
  visible: boolean
  /** 是否禁止关闭（「菜单配置」页自身：自锁保护） */
  locked: boolean
}

export const setMenuConfigApi = {
  /** 本租户可配置的页面级菜单清单（含当前已隐藏的菜单；拦截器已拆包，直接得到数组） */
  list(): Promise<SetMenuConfigItem[]> {
    return request.get('/set/menu-config/list')
  },

  /** 设置某个菜单在本租户的显隐（true = 显示 / false = 隐藏） */
  updateVisible(menuId: string, visible: boolean): Promise<any> {
    return request.put('/set/menu-config/visible', null, { params: { menuId, visible } })
  }
}

export default setMenuConfigApi
