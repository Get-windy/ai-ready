/**
 * 权限使用示例
 * 演示如何在组件中使用权限控制功能
 */

<template>
  <div class="permission-example">
    <!-- 示例1: 使用 v-permission 指令控制按钮显示 -->
    <a-space>
      <a-button
        v-permission="'system:user:add'"
        type="primary"
        @click="handleAddUser"
      >
        添加用户
      </a-button>

      <a-button
        v-permission="'system:user:edit'"
        @click="handleEditUser"
      >
        编辑用户
      </a-button>

      <a-button
        v-permission="'system:user:delete'"
        danger
        @click="handleDeleteUser"
      >
        删除用户
      </a-button>
    </a-space>

    <a-divider />

    <!-- 示例2: 使用 v-role 指令控制按钮显示 -->
    <a-space>
      <a-button
        v-role="'admin'"
        type="primary"
        @click="handleAdminAction"
      >
        管理员操作
      </a-button>

      <a-button
        v-role="'user'"
        @click="handleUserAction"
      >
        用户操作
      </a-button>
    </a-space>

    <a-divider />

    <!-- 示例3: 使用组合式函数进行权限控制 -->
    <div>
      <h3>使用组合式函数</h3>
      <a-button
        v-if="canOperate('system:user:add')"
        type="primary"
        @click="handleAddUser"
      >
        组合式函数控制按钮
      </a-button>
    </div>

    <a-divider />

    <!-- 示例4: 批量权限检查 -->
    <div>
      <h3>批量权限检查</h3>
      <a-row :gutter="16">
        <a-col :span="8">
          <a-card title="用户管理权限">
            <p>添加用户: {{ checkPermission('system:user:add') ? '✓' : '✗' }}</p>
            <p>编辑用户: {{ checkPermission('system:user:edit') ? '✓' : '✗' }}</p>
            <p>删除用户: {{ checkPermission('system:user:delete') ? '✓' : '✗' }}</p>
            <p>查看用户: {{ checkPermission('system:user:view') ? '✓' : '✗' }}</p>
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card title="角色管理权限">
            <p>添加角色: {{ checkPermission('system:role:add') ? '✓' : '✗' }}</p>
            <p>编辑角色: {{ checkPermission('system:role:edit') ? '✓' : '✗' }}</p>
            <p>删除角色: {{ checkPermission('system:role:delete') ? '✓' : '✗' }}</p>
            <p>分配权限: {{ checkPermission('system:role:assign') ? '✓' : '✗' }}</p>
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card title="当前用户信息">
            <p>用户名: {{ userStore.userInfo?.username }}</p>
            <p>用户类型: {{ userStore.userInfo?.userType }}</p>
            <p>角色数: {{ roles.length }}</p>
            <p>权限数: {{ permissions.length }}</p>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <a-divider />

    <!-- 示例5: 条件权限操作 -->
    <div>
      <h3>条件权限操作</h3>
      <a-button
        :disabled="getButtonDisabled('system:user:add')"
        type="primary"
        @click="handleAddUser"
      >
        添加用户（禁用状态）: {{ getButtonDisabled('system:user:add') ? '禁用' : '启用' }}
      </a-button>
    </div>

    <a-divider />

    <!-- 示例6: 菜单权限过滤 -->
    <div>
      <h3>菜单权限过滤</h3>
      <a-menu mode="vertical">
        <a-menu-item
          v-for="menu in accessibleMenus"
          :key="menu.id"
        >
          {{ menu.name }}
        </a-menu-item>
      </a-menu>
    </div>

    <a-divider />

    <!-- 示例7: 数据权限 -->
    <div>
      <h3>数据权限范围</h3>
      <p>当前数据权限范围: {{ dataScope }}</p>
      <VxeTableList
        :columns="vxeColumns"
        :data-source="filteredUsers"
        :pagination="false"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { usePermission, useButtonPermission, useMenuPermission, useDataPermission } from '@/composables/usePermission'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 基础权限控制
const {
  checkPermission,
  checkAnyPermission,
  checkAllPermissions,
  checkRole,
  checkAnyRole,
  permissions,
  roles,
  isSuperAdminUser,
  isAdminUser
} = usePermission()

// 按钮权限控制
const {
  canOperate,
  getButtonDisabled
} = useButtonPermission()

// 菜单权限控制
const {
  canAccessMenu,
  canAccessAnyMenu,
  filterMenus
} = useMenuPermission()

// 数据权限控制
const {
  checkDataScope,
  getDataScope,
  filterByDataScope
} = useDataPermission()

// 示例菜单数据
const allMenus = ref([
  { id: 1, name: '用户管理', permissions: ['menu:user'] },
  { id: 2, name: '角色管理', permissions: ['menu:role'] },
  { id: 3, name: '权限管理', permissions: ['menu:permission'] },
  { id: 4, name: '系统设置', permissions: ['menu:system'] }
])

// 可访问的菜单
const accessibleMenus = computed(() => filterMenus(allMenus.value))

// 数据权限范围
const dataScope = computed(() => getDataScope())

// 示例用户数据
const allUsers = ref([
  { id: 1, name: '张三', userId: 1001, deptId: 1 },
  { id: 2, name: '李四', userId: 1002, deptId: 2 },
  { id: 3, name: '王五', userId: 1003, deptId: 1 }
])

// 根据数据权限过滤用户
const filteredUsers = computed(() => {
  const currentUserId = userStore.userInfo?.id || 0
  const currentDeptId = userStore.userInfo?.deptId || 0
  return filterByDataScope(allUsers.value, currentUserId, currentDeptId)
})

// 表格列定义
const vxeColumns = [
  { field: 'id', title: 'ID' },
  { field: 'name', title: '姓名' }
]

// 操作方法
const handleAddUser = () => {
  message.success('执行添加用户操作')
}

const handleEditUser = () => {
  message.success('执行编辑用户操作')
}

const handleDeleteUser = () => {
  message.success('执行删除用户操作')
}

const handleAdminAction = () => {
  message.success('执行管理员操作')
}

const handleUserAction = () => {
  message.success('执行普通用户操作')
}
</script>

<style scoped>
.permission-example {
  padding: 24px;
  background: #fff;
  min-height: 100vh;
}
</style>