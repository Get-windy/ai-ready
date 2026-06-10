<template>
  <slot v-if="hasAccess" />
  <slot v-else name="noPermission">
    <template v-if="mode === 'hide'">
      <!-- 完全隐藏 -->
    </template>
    <template v-else>
      <!-- 禁用态 -->
      <span class="permission-disabled-wrapper" :title="tooltip">
        <slot name="disabled" />
      </span>
    </template>
  </slot>
</template>

<script setup lang="ts">
/**
 * 权限包装组件
 *
 * 提供统一的权限控制 UI 封装，支持两种模式：
 *   - hide（默认）：无权限时隐藏内容
 *   - disabled：无权限时显示禁用态
 *
 * 支持三种权限检查类型：
 *   - permission：普通权限码（如 "btn:user:add"）
 *   - role：角色（如 "admin"）
 *   - bill：单据类型权限（如 "601:edit"）
 *
 * 使用示例：
 *   <Permission permission="btn:user:add">
 *     <a-button>新增用户</a-button>
 *   </Permission>
 *
 *   <Permission :permission="['btn:user:add', 'btn:user:edit']" mode="disabled">
 *     <template #disabled>
 *       <a-button disabled>操作</a-button>
 *     </template>
 *   </Permission>
 *
 *   <Permission bill="601:edit" mode="disabled">
 *     <a-button>编辑销售出库单</a-button>
 *   </Permission>
 *
 *   <Permission role="admin">
 *     <a-button>管理后台</a-button>
 *   </Permission>
 */
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const props = withDefaults(defineProps<{
  /** 权限码（单个或多个） */
  permission?: string | string[]
  /** 角色（单个或多个） */
  role?: string | string[]
  /** 单据类型权限（如 "601:view" 或 ["601:edit", "604:view"]） */
  bill?: string | string[]
  /** 无权限时的显示模式 */
  mode?: 'hide' | 'disabled'
  /** 禁用态提示文字 */
  tooltip?: string
}>(), {
  mode: 'hide',
  tooltip: '暂无操作权限',
})

const userStore = useUserStore()

const hasAccess = computed(() => {
  // 超级管理员放行
  if (userStore.permissions.includes('*')) return true

  // 检查普通权限
  if (props.permission) {
    const perms = Array.isArray(props.permission) ? props.permission : [props.permission]
    if (!perms.some(p => userStore.hasPermission(p))) return false
  }

  // 检查角色
  if (props.role) {
    const roles = Array.isArray(props.role) ? props.role : [props.role]
    if (!roles.some(r => userStore.hasRole(r))) return false
  }

  // 检查单据类型权限
  if (props.bill) {
    const billCodes = Array.isArray(props.bill) ? props.bill : [props.bill]
    const userBillTypes = new Set(userStore.billTypes || [])

    const hasBillAccess = billCodes.some(code => {
      const parts = code.split(':')
      const billType = parts[0]
      const action = parts[1] || 'view'
      if (!userBillTypes.has(billType)) return false
      return userStore.hasPermission(`bill:${billType}:${action}`) ||
             userStore.hasPermission(`bill:${billType}`)
    })

    if (!hasBillAccess) return false
  }

  return true
})
</script>

<style scoped>
.permission-disabled-wrapper {
  display: inline-block;
  cursor: not-allowed;
}
</style>
