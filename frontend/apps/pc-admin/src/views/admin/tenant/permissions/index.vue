<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>租户权限配置</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">租户权限配置</h2>
        </div>
        <div class="page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </div>
    </template>

    <PermissionConfigPanel
      :fetch-roles="fetchRoles"
      :fetch-permission-tree="fetchPermissionTree"
      :fetch-role-permissions="fetchRolePermissions"
      :save-role-permissions="saveRolePermissions"
      :group-filter="groupFilter"
      @save-success="onSaveSuccess"
      @loaded="onPanelLoaded"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import PermissionConfigPanel from '@/components/PermissionConfigPanel/index.vue'
import { permissionApi } from '@/api/permission'
import { roleApi } from '@/api/role'
import { tenantModuleApi } from '@/api/tenantModule'
import { useUserStore } from '@/stores/user'
import PageContainer from '@/components/PageContainer/PageContainer.vue'

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    window.location.reload()
  }
}

onMounted(() => { document.addEventListener('keydown', handleKeydown) })
onUnmounted(() => { document.removeEventListener('keydown', handleKeydown) })

// ==================== 通用 ====================
const userStore = useUserStore()
const lastUpdateTime = ref('')

// ==================== PermissionConfigPanel Props ====================
const fetchRoles = async () => roleApi.listAll('TENANT')
const fetchPermissionTree = async () => permissionApi.getTree(userStore.tenantId || 1)
const fetchRolePermissions = async (roleId: number) => roleApi.getPermissions(roleId)
const saveRolePermissions = async (roleId: number, permissionIds: number[]) => roleApi.assignPermissions(roleId, permissionIds)

// 租户模块过滤：只显示该租户已购买模块对应的权限分组
const validModuleCodes = ref<Set<string>>(new Set())

const loadValidModules = async () => {
  try {
    const res = await tenantModuleApi.getValidModuleCodes(userStore.tenantId || 1)
    if (res.data) {
      validModuleCodes.value = new Set(res.data)
    }
  } catch (err) {
    console.warn('[租户权限] 加载模块失败', err)
    message.warning('加载租户模块信息失败，将显示全部权限')
  }
}

interface PermissionGroup {
  id: number
  name: string
  code: string
  moduleKey: string
  permissions: any[]
}

const groupFilter = (groups: PermissionGroup[]): PermissionGroup[] => {
  if (validModuleCodes.value.size === 0) return groups
  // 检查模块编码是否在租户已购模块中（精确 + 前缀匹配）
  return groups.filter(g => {
    const codes = validModuleCodes.value
    if (codes.has(g.moduleKey)) return true
    // 前缀匹配：如 "sale:order" 匹配 "sale"
    let prefix = g.moduleKey
    while (prefix.includes(':')) {
      prefix = prefix.substring(0, prefix.lastIndexOf(':'))
      if (codes.has(prefix)) return true
    }
    return false
  })
}

const onSaveSuccess = () => { lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN') }
const onPanelLoaded = () => {
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  loadValidModules()
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header-left { display: flex; align-items: center; gap: 12px; }
.page-header-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header-right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>
