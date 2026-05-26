/**
 * 角色权限管理模块主入口
 */
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 创建Vue应用
export function createRoleManagementApp(rootComponent: any, container?: string | Element) {
  const app = createApp(rootComponent)
  
  // 安装Element Plus
  app.use(ElementPlus, {
    locale: zhCn,
    size: 'default'
  })
  
  // 安装Pinia
  const pinia = createPinia()
  app.use(pinia)
  
  // 注册所有Element Plus图标
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }
  
  // 挂载到指定容器或创建新容器
  if (container) {
    app.mount(container)
  }
  
  return app
}

// 导出组件
export { default as RoleListView } from './views/RoleListView.vue'
export { default as RoleFormView } from './views/RoleFormView.vue'
export { default as PermissionManagementView } from './views/PermissionManagementView.vue'
export { default as UserRoleAssignmentView } from './views/UserRoleAssignmentView.vue'

// 导出组件
export { default as RoleList } from './components/RoleList/RoleList.vue'
export { default as RoleFormDialog } from './components/RoleForm/RoleFormDialog.vue'
export { default as PermissionTree } from './components/PermissionTree/PermissionTree.vue'
export { default as UserRoleAssign } from './components/UserRoleAssign/UserRoleAssign.vue'

// 导出Store
export { useRoleStore } from './store/roleStore'
export { usePermissionStore } from './store/permissionStore'
export { useUserRoleStore } from './store/userRoleStore'

// 导出API
export { roleManagementApi } from './api'

// 导出类型
export * from './types'

// 工具函数
export * from './utils'