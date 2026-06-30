<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="menu-page-header">
        <div class="menu-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>菜单管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="menu-page-header-title">菜单管理</h2>
        </div>
        <div class="menu-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl</kbd> + <kbd>N</kbd> 新增</span>
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', loadMenuTree)()">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="menu-management">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ menuCount }}</div>
            <div class="stat-card-label">菜单总数</div>
          </div>
          <AppstoreOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-enabled">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ enabledCount }}</div>
            <div class="stat-card-label">启用菜单</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-disabled">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ disabledCount }}</div>
            <div class="stat-card-label">禁用菜单</div>
          </div>
          <StopOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-button">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ buttonCount }}</div>
            <div class="stat-card-label">按钮数量</div>
          </div>
          <ControlOutlined class="stat-card-icon" />
        </div>
      </div>

      <!-- 工具栏 -->
      <div class="menu-toolbar">
        <div class="menu-toolbar-left">
          <a-space>
            <a-button type="primary" :loading="submitLoading" v-permission="'system:permission:create'" @click="handleAdd">
              <template #icon><PlusOutlined /></template>
              新增菜单
            </a-button>
            <a-button @click="toggleExpandAll">
              <template #icon><NodeExpandOutlined v-if="!isExpandAll" /><NodeCollapseOutlined v-else /></template>
              {{ isExpandAll ? '折叠全部' : '展开全部' }}
            </a-button>
            <a-button @click="handleRefreshCache">
              <template #icon><SyncOutlined /></template>
              刷新缓存
            </a-button>
          </a-space>
        </div>
        <div class="menu-toolbar-right">
          <a-space>
            <a-select
              v-model:value="queryForm.menuType"
              placeholder="菜单类型"
              allow-clear
              style="width: 110px"
              size="small"
              @change="loadMenuTree"
            >
              <a-select-option v-for="opt in menuTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
            </a-select>
            <a-select
              v-model:value="queryForm.status"
              placeholder="状态"
              allow-clear
              style="width: 90px"
              size="small"
              @change="loadMenuTree"
            >
              <a-select-option :value="1">启用</a-select-option>
              <a-select-option :value="0">禁用</a-select-option>
            </a-select>
            <a-input-search
              v-model:value="queryForm.menuName"
              placeholder="搜索菜单名称"
              style="width: 180px"
              size="small"
              allow-clear
              @search="loadMenuTree"
            />
            <a-tooltip title="刷新 (F5)">
              <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', loadMenuTree)()">
                <template #icon><ReloadOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-space>
        </div>
      </div>

      <a-skeleton active v-if="loading && menuTree.length === 0" :paragraph="{ rows: 8 }" style="padding: 24px;" />

      <!-- 表格 -->
      <a-table
        v-else
        :columns="tableColumns"
        :data-source="paginatedRows"
        :loading="loading"
        :pagination="false as any"
        size="small"
        row-key="id"
        :bordered="true"
        class="menu-tree-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'menuName'">
            <span class="tree-indent" :style="{ paddingLeft: (record._level || 0) * 20 + 'px' }">
              <span
                v-if="record._hasChildren"
                class="tree-expand-icon"
                @click.stop="toggleRowExpand(record.id)"
              >
                <CaretDownOutlined v-if="expandedRowKeys.includes(record.id)" />
                <CaretRightOutlined v-else />
              </span>
              <span v-else class="tree-expand-placeholder" />
              <component v-if="record.icon" :is="iconComponent(record.icon)" class="menu-icon" />
              <span
                :class="{ 'tree-node-clickable': record._hasChildren }"
                @click="record._hasChildren && toggleRowExpand(record.id)"
              >{{ record.menuName }}</span>
            </span>
          </template>
          <template v-else-if="column.key === 'menuType'">
            <a-tag v-if="record.menuType === 0">目录</a-tag>
            <a-tag v-else-if="record.menuType === 1" color="green">菜单</a-tag>
            <a-tag v-else-if="record.menuType === 2" color="orange">按钮</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 1"
              @change="(checked: boolean) => handleStatusChange(record, checked ? 1 : 0)"
            />
          </template>
          <template v-else-if="column.key === 'visible'">
            <a-tag v-if="record.visible === 1" color="green">显示</a-tag>
            <a-tag v-else>隐藏</a-tag>
          </template>
          <template v-else-if="column.key === 'bizFlowTag'">
            <a-tag v-if="record.bizFlowTag" :color="getBizFlowTagColor(record.bizFlowTag)">
              {{ getBizFlowTagLabel(record.bizFlowTag) }}
            </a-tag>
            <span v-else class="text-muted">—</span>
          </template>
          <template v-else-if="column.key === 'displayMode'">
            <a-tag v-if="record.displayMode === 1" color="purple">双入口</a-tag>
            <span v-else class="text-muted">默认</span>
          </template>
          <template v-else-if="column.key === 'menuLevel'">
            <a-tag v-if="record.menuLevel === 1" color="red">系统</a-tag>
            <a-tag v-else-if="record.menuLevel === 0" color="green">租户</a-tag>
            <span v-else class="text-muted">—</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" v-permission="'system:permission:create'" @click="handleAddChild(record)">
              <template #icon><PlusOutlined /></template>新增
            </a-button>
            <a-button type="link" size="small" v-permission="'system:permission:update'" @click="handleEdit(record)">
              <template #icon><EditOutlined /></template>编辑
            </a-button>
            <a-button type="link" size="small" v-permission="'system:permission:assign'" @click="handleAssignRole(record)">
              <template #icon><UserOutlined /></template>角色
            </a-button>
            <a-button type="link" size="small" danger v-permission="'system:permission:delete'" @click="handleDelete(record)">
              <template #icon><DeleteOutlined /></template>删除
            </a-button>
          </template>
        </template>
        <template #emptyText>
          <a-empty v-if="!hasError" description="暂无数据" />
          <a-result v-else status="error" title="数据加载失败">
            <template #extra>
              <a-button type="primary" @click="debounceClick('refresh', loadMenuTree)()">
                <template #icon><ReloadOutlined /></template>
                重新加载
              </a-button>
            </template>
          </a-result>
        </template>
      </a-table>

      <!-- 分页 -->
      <div class="table-pagination" v-if="flatMenuRows.length > 0">
        <a-pagination
          v-model:current="currentPage"
          v-model:pageSize="pageSize"
          :total="flatMenuRows.length"
          :show-size-changer="true"
          :show-quick-jumper="true"
          :page-size-options="['20', '50', '100', '200']"
          :show-total="(total: number) => `共 ${total} 条`"
          size="small"
        />
      </div>

    <!-- 菜单编辑弹窗 -->
    <FullScreenDetail
      :visible="dialogVisible"
      :title="dialogTitle"
      :dirty="formDirty"
      :save-loading="submitLoading"
      :show-save-and-new="!formData.id"
      @save="handleSubmit"
      @close="handleFormClose"
      @save-and-new="handleFormSaveAndNew"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ style: { width: '100px' } }"
      >
        <a-form-item label="上级菜单">
          <a-tree-select
            v-model:value="formData.parentId"
            :tree-data="parentMenuTree"
            :field-names="{ label: 'menuName', value: 'id', children: 'children' }"
            placeholder="请选择上级菜单（留空则为顶级）"
            allow-clear
            tree-check-strictly
            tree-default-expand-all
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="菜单类型" name="menuType">
          <a-radio-group v-model:value="formData.menuType">
            <a-radio
              v-for="opt in menuTypeOptions"
              :key="opt.value"
              :value="opt.value"
            >{{ opt.label }}</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="菜单名称" name="menuName">
          <a-input
            v-model:value="formData.menuName"
            placeholder="请输入菜单名称"
          />
        </a-form-item>

        <a-form-item label="权限标识" name="menuCode">
          <a-input
            v-model:value="formData.menuCode"
            placeholder="请输入权限标识，如：system:user:list"
          />
        </a-form-item>

        <a-form-item v-if="formData.menuType !== 2" label="路由路径" name="path">
          <a-input
            v-model:value="formData.path"
            :placeholder="formData.menuType === 0 ? '顶层目录以 / 开头，如：/system；子目录不以 / 开头，如：user' : '请输入路由路径，如：orders'"
          />
        </a-form-item>

        <a-form-item v-if="formData.menuType === 0" label="组件路径">
          <a-input
            v-model:value="formData.component"
            placeholder="可选，顶层目录填写如 layouts/BasicLayout.vue；子目录留空由系统自动推断"
          />
        </a-form-item>

        <a-form-item v-if="formData.menuType === 1" label="组件路径" name="component">
          <a-input
            v-model:value="formData.component"
            placeholder="views/ 下的相对路径，如：views/erp/sale/index"
          />
        </a-form-item>

        <a-form-item v-if="formData.menuType !== 2" label="菜单图标">
          <a-auto-complete
            v-model:value="formData.icon"
            :options="iconOptions"
            placeholder="输入或选择图标名，如：UserOutlined"
            :filter-option="iconFilterOption"
          />
        </a-form-item>

        <a-form-item label="排序" name="sortOrder">
          <a-input-number
            v-model:value="formData.sortOrder"
            :min="0"
            :max="9999"
          />
        </a-form-item>

        <a-form-item v-if="formData.menuType !== 2" label="是否显示">
          <a-radio-group v-model:value="formData.visible">
            <a-radio :value="1">显示</a-radio>
            <a-radio :value="0">隐藏</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="菜单状态">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="备注">
          <a-textarea
            v-model:value="formData.remark"
            :rows="3"
            placeholder="请输入备注"
          />
        </a-form-item>

        <a-form-item label="业务流向">
          <a-select
            v-model:value="formData.bizFlowTag"
            placeholder="请选择业务流向（可选）"
            allow-clear
          >
            <a-select-option
              v-for="opt in bizFlowTagOptions"
              :key="opt.value"
              :value="opt.value"
            >{{ opt.label }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="展示分组">
          <a-radio-group v-model:value="formData.displayGroup">
            <a-radio :value="0">正常路由目录</a-radio>
            <a-radio :value="1">纯展示分组（Sidebar分组标题，不生成路由嵌套）</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="链接图标">
          <a-input
            v-model:value="formData.linkIcon"
            placeholder="外链/快捷方式图标名，如 LinkOutlined"
          />
        </a-form-item>

        <a-form-item label="菜单层级">
          <a-radio-group v-model:value="formData.menuLevel">
            <a-radio :value="0">租户级</a-radio>
            <a-radio :value="1">系统级</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="展示模式">
          <a-radio-group v-model:value="formData.displayMode">
            <a-radio :value="0">默认（单入口）</a-radio>
            <a-radio :value="1">双入口（含标签按钮）</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item v-if="formData.displayMode === 1" label="列表路径">
          <a-input
            v-model:value="formData.listPath"
            placeholder="双入口时，标签按钮跳转的列表页路由路径"
          />
        </a-form-item>

        <a-form-item label="标签文案">
          <a-select
            v-model:value="formData.tagLabel"
            placeholder="请选择标签文案（可选）"
            allow-clear
          >
            <a-select-option value="历史">历史</a-select-option>
            <a-select-option value="列表">列表</a-select-option>
            <a-select-option value="添加">添加</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </FullScreenDetail>

    <!-- 角色分配弹窗 -->
    <FullScreenDetail
      :visible="roleDialogVisible"
      title="分配角色"
      :save-loading="roleSubmitLoading"
      @save="handleRoleSubmit"
      @close="roleDialogVisible = false"
    >
      <a-form :label-col="{ style: { width: '80px' } }">
        <a-form-item label="菜单名称">
          <span>{{ currentMenu?.menuName }}</span>
        </a-form-item>
        <a-form-item label="选择角色">
          <a-select
            v-model:value="selectedRoles"
            mode="multiple"
            placeholder="请选择角色"
            size="small"
            style="width: 100%"
          >
            <a-select-option
              v-for="role in roleList"
              :key="role.id"
              :value="role.id"
            >
              {{ role.roleName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </FullScreenDetail>
  </div>
</PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
// Rule type not available, using any
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  AppstoreOutlined,
  CheckCircleOutlined,
  StopOutlined,
  ControlOutlined,
  ReloadOutlined,
  SyncOutlined,
  WarningOutlined,
  NodeExpandOutlined,
  NodeCollapseOutlined,
  CaretDownOutlined,
  CaretRightOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import menuApi, { type MenuInfo, type MenuQuery, type MenuSaveRequest, type MenuUpdateRequest } from '@/api/menu'
import roleApi from '@/api/role'
import { dictItemApi } from '@/api/dict'
import { useSubmitLock } from '@/composables'
import * as Icons from '@ant-design/icons-vue'

const lastUpdateTime = ref('')
const refreshLoading = ref(false)
const hasError = ref(false)

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

// 查询表单
const queryForm = reactive<MenuQuery>({
  menuName: '',
  menuType: undefined,
  status: undefined
})

// 菜单树数据
const menuTree = ref<MenuInfo[]>([])
const loading = ref(false)
const isExpandAll = ref(false)

// ── 分页状态 ─────────────────────────────────────────────
const currentPage = ref(1)
const pageSize = ref(50)

// ── 树展开状态（扁平化方案）─────────────────────────────
const expandedRowKeys = ref<number[]>([])

function toggleRowExpand(id: number) {
  const idx = expandedRowKeys.value.indexOf(id)
  if (idx === -1) {
    expandedRowKeys.value = [...expandedRowKeys.value, id]
  } else {
    expandedRowKeys.value = expandedRowKeys.value.filter(k => k !== id)
  }
}

function collectAllParentIds(nodes: MenuInfo[]): number[] {
  const ids: number[] = []
  const walk = (list: MenuInfo[]) => {
    for (const n of list) {
      if (n.children?.length) { ids.push(n.id); walk(n.children) }
    }
  }
  walk(nodes)
  return ids
}

// 扁平化：根据展开状态生成当前可见行
interface FlatRow extends MenuInfo {
  _level: number
  _hasChildren: boolean
}

const flatMenuRows = computed<FlatRow[]>(() => {
  const result: FlatRow[] = []
  const expandedSet = new Set(expandedRowKeys.value)
  const walk = (nodes: MenuInfo[], level: number) => {
    for (const node of nodes) {
      const hasChildren = !!(node.children?.length)
      // 剔除 children，避免 Ant Design 检测到 children 自动生成展开列
      const { children: _children, ...rest } = node
      result.push({ ...rest, _level: level, _hasChildren: hasChildren } as FlatRow)
      if (hasChildren && expandedSet.has(node.id)) {
        walk(node.children!, level + 1)
      }
    }
  }
  walk(menuTree.value, 0)
  return result
})

// 分页基于实际显示行数
const paginatedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return flatMenuRows.value.slice(start, start + pageSize.value)
})

// 上级菜单树（过滤掉按钮，添加虚拟根节点）
const parentMenuTree = computed(() => {
  const filterButtons = (nodes: MenuInfo[]): MenuInfo[] => {
    return nodes
      .filter(n => n.menuType !== 2)
      .map(n => ({
        ...n,
        children: n.children ? filterButtons(n.children) : undefined
      }))
  }
  return [
    { id: 0, menuName: '主类目（顶级）', children: filterButtons(menuTree.value) } as unknown as MenuInfo
  ]
})

// 图标选项列表（常用图标）
const commonIconNames = Object.keys(Icons).filter(name => name.endsWith('Outlined')).slice(0, 60)
const iconOptions = computed(() => commonIconNames.map(name => ({ value: name })))
const iconFilterOption = (inputValue: string, option: { value: string }) => {
  return option.value.toLowerCase().includes(inputValue.toLowerCase())
}

// ── 统计数据 ────────────────────────────────────────────
const flattenMenuTree = (tree: MenuInfo[]): MenuInfo[] => {
  const result: MenuInfo[] = []
  const traverse = (nodes: MenuInfo[]) => {
    for (const node of nodes) {
      result.push(node)
      if (node.children?.length) traverse(node.children)
    }
  }
  traverse(tree)
  return result
}
const menuCount = computed(() => flattenMenuTree(menuTree.value).filter(m => m.menuType !== 2).length)
const buttonCount = computed(() => flattenMenuTree(menuTree.value).filter(m => m.menuType === 2).length)
const enabledCount = computed(() => flattenMenuTree(menuTree.value).filter(m => m.status === 1).length)
const disabledCount = computed(() => flattenMenuTree(menuTree.value).filter(m => m.status === 0).length)

// 弹窗控制
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const { isSubmitting: submitLoading, withSubmitLock } = useSubmitLock()
const formRef = ref<FormInstance>()

// ── 表单脏检测 ──────────────────────────────────────────
const initialFormSnapshot = ref('')
let watchReady = false
const formDirty = computed(() => {
  if (!watchReady) return false
  return JSON.stringify(formData) !== initialFormSnapshot.value
})
function saveFormSnapshot() { initialFormSnapshot.value = JSON.stringify(formData) }

// ── 离开守卫 ────────────────────────────────────────────
onBeforeRouteLeave((to, from, next) => {
  if (!formDirty.value) { next(); return }
  Modal.confirm({
    title: '确认离开',
    content: '您有未保存的修改，确定要离开吗？',
    okText: '离开',
    cancelText: '继续编辑',
    onOk: () => next(),
    onCancel: () => next(false),
  })
})

// 表格列配置（Ant Design Vue 格式）
const tableColumns: any[] = [
  { title: '菜单名称', dataIndex: 'menuName', key: 'menuName', width: 220, ellipsis: true },
  { title: '上级菜单', dataIndex: 'parentName', key: 'parentName', width: 100, ellipsis: true },
  { title: '权限标识', dataIndex: 'menuCode', key: 'menuCode', width: 150, ellipsis: true },
  { title: '路由路径', dataIndex: 'path', key: 'path', width: 120, ellipsis: true },
  { title: '组件路径', dataIndex: 'component', key: 'component', width: 150, ellipsis: true },
  { title: '类型', dataIndex: 'menuType', key: 'menuType', width: 60, align: 'center' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 50, align: 'center' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 60, align: 'center' },
  { title: '显示', dataIndex: 'visible', key: 'visible', width: 55, align: 'center' },
  { title: '流向', dataIndex: 'bizFlowTag', key: 'bizFlowTag', width: 75, align: 'center' },
  { title: '模式', dataIndex: 'displayMode', key: 'displayMode', width: 65, align: 'center' },
  { title: '层级', dataIndex: 'menuLevel', key: 'menuLevel', width: 55, align: 'center' },
  { title: '操作', key: 'action', width: 240 },
]

// ── 菜单类型选项（从API加载） ──────────────────────────
const menuTypeOptions = ref<{ label: string; value: number }[]>([])

async function loadMenuTypes() {
  try {
    const data = await dictItemApi.getByDictCode('MENU_TYPE')
    if (data) {
      menuTypeOptions.value = (data as any)
        .sort((a: any, b: any) => a.sortOrder - b.sortOrder)
        .map((item: any) => ({ label: item.itemText, value: Number(item.itemValue) }))
    }
  } catch (err) {
    console.warn('[菜单管理] 加载菜单类型失败', err)
  }
}

// 将图标字符串转为组件
const iconComponent = (iconName: string) => {
  if (!iconName) return null
  return (Icons as any)[iconName] || null
}

// 业务流向选项（供表单下拉选择）
const bizFlowTagOptions = [
  { value: '', label: '无' },
  { value: 'sales', label: '销售管理' },
  { value: 'purchase', label: '采购管理' },
  { value: 'warehouse', label: '仓库管理' },
  { value: 'wms', label: 'WMS仓储' },
  { value: 'delivery', label: '配送管理' },
  { value: 'customer', label: '客户关系' },
  { value: 'finance', label: '财务管理' },
  { value: 'expense', label: '费用管理' },
  { value: 'asset', label: '资产管理' },
  { value: 'budget', label: '预算管理' },
  { value: 'mall', label: '商城管理' },
  { value: 'orders', label: '订单中心' },
  { value: 'workflow', label: '工作流' },
  { value: 'product', label: '产品数据' },
  { value: 'printing', label: '打印管理' },
  { value: 'system', label: '系统管理' }
]

// 业务流向标签颜色映射
const bizFlowTagColorMap: Record<string, string> = {
  sales: 'blue',
  purchase: 'cyan',
  warehouse: 'orange',
  wms: 'purple',
  delivery: 'geekblue',
  customer: 'green',
  finance: 'red',
  expense: 'volcano',
  asset: 'gold',
  budget: 'lime',
  mall: 'magenta',
  orders: 'blue',
  workflow: 'cyan',
  product: 'green',
  printing: 'purple',
  system: 'geekblue'
}

function getBizFlowTagLabel(tag: string): string {
  return bizFlowTagOptions.find(o => o.value === tag)?.label || tag
}

function getBizFlowTagColor(tag: string): string {
  return bizFlowTagColorMap[tag] || 'default'
}

// 表单数据
const formData = reactive<MenuUpdateRequest>({
  id: 0,
  parentId: undefined as unknown as number,
  menuName: '',
  menuCode: '',
  menuType: 1,
  icon: '',
  path: '',
  component: '',
  permissions: '',
  sortOrder: 0,
  status: 1,
  visible: 1,
  keepAlive: 0,
  external: 0,
  remark: '',
  bizFlowTag: '',
  displayGroup: 0,
  linkIcon: '',
  displayMode: 0,
  listPath: '',
  tagLabel: '',
  menuLevel: 0
})

// 表单验证规则
const formRules = computed<any>(() => ({
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuCode: [{ required: true, message: '请输入权限标识', trigger: 'blur' }],
  path: formData.menuType === 1 ? [{ required: true, message: '请输入路由路径', trigger: 'blur' }] : [],
  sortOrder: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}))

// 角色分配弹窗
const roleDialogVisible = ref(false)
const { isSubmitting: roleSubmitLoading, withSubmitLock: withRoleSubmitLock } = useSubmitLock()
const currentMenu = ref<MenuInfo | null>(null)
const selectedRoles = ref<number[]>([])
const roleList = ref<any[]>([])

// 刷新所有菜单（侧边栏 + 管理页树）
const refreshAllMenus = async () => {
  try {
    // 清除菜单缓存，强制从后端重新拉取
    Object.keys(localStorage).forEach(key => {
      if (key.startsWith('menu_cache')) localStorage.removeItem(key)
    })
    const { setupDynamicRoutes } = await import('@/router')
    await setupDynamicRoutes()
  } catch { /* 静默 */ }
  loadMenuTree()
}

// 加载菜单树
const loadMenuTree = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await menuApi.getTree(queryForm)
    if (res) {
      const rawTree = Array.isArray(res) ? res : [res]
      // 注入 parentName：构建 id→menuName 映射，遍历树填充
      const nameMap = new Map<number, string>()
      const buildNameMap = (nodes: MenuInfo[]) => {
        for (const node of nodes) {
          nameMap.set(node.id, node.menuName)
          if (node.children?.length) buildNameMap(node.children)
        }
      }
      buildNameMap(rawTree)
      const injectParentName = (nodes: MenuInfo[]) => {
        for (const node of nodes) {
          ;(node as any).parentName = node.parentId && nameMap.has(node.parentId)
            ? nameMap.get(node.parentId)!
            : node.parentId === 0 ? '顶级菜单' : '-'
          if (node.children?.length) injectParentName(node.children)
        }
      }
      injectParentName(rawTree)
      menuTree.value = rawTree
      // 默认展开第一层
      const firstLevelIds: number[] = []
      for (const node of rawTree) {
        if (node.children?.length) firstLevelIds.push(node.id)
      }
      expandedRowKeys.value = firstLevelIds
    } else {
      message.error('获取菜单列表失败：返回空数据')
      menuTree.value = []
    }
  } catch (error) {
    hasError.value = true
    console.error('[菜单管理] 获取菜单列表失败', error)
    message.error('获取菜单列表失败')
    menuTree.value = []
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

// 展开/折叠全部
const toggleExpandAll = () => {
  isExpandAll.value = !isExpandAll.value
  if (isExpandAll.value) {
    expandedRowKeys.value = collectAllParentIds(menuTree.value)
  } else {
    expandedRowKeys.value = []
  }
}

// 刷新菜单缓存（清除路由缓存并重新加载）
const handleRefreshCache = async () => {
  await refreshAllMenus()
  message.success('菜单缓存已刷新')
}

// 新增菜单
const handleAdd = () => {
  dialogTitle.value = '新增菜单'
  resetForm()
  dialogVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 新增子菜单
const handleAddChild = (row: any) => {
  dialogTitle.value = `新增子菜单 - ${row.menuName}`
  resetForm()
  formData.parentId = row.id
  dialogVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 编辑菜单
const handleEdit = (row: any) => {
  dialogTitle.value = '编辑菜单'
  resetForm()
  Object.assign(formData, {
    id: row.id,
    parentId: row.parentId || undefined,
    menuName: row.menuName,
    menuCode: row.menuCode,
    menuType: row.menuType,
    icon: row.icon || '',
    path: row.path || '',
    component: row.component || '',
    permissions: row.permissions || '',
    sortOrder: row.sortOrder,
    status: row.status,
    visible: row.visible,
    keepAlive: row.keepAlive || 0,
    external: row.external || 0,
    remark: row.remark || '',
    bizFlowTag: row.bizFlowTag || '',
    displayGroup: row.displayGroup ?? 0,
    linkIcon: row.linkIcon || '',
    displayMode: row.displayMode ?? 0,
    listPath: row.listPath || '',
    tagLabel: row.tagLabel || '',
    menuLevel: row.menuLevel ?? 0
  })
  dialogVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 删除菜单
const handleDelete = async (row: any) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除菜单"${row.menuName}"吗？删除后不可恢复！`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      await menuApi.delete(row.id)
      message.success('删除成功')
      await refreshAllMenus()
    },
    onCancel: () => { /* noop */ }
  })
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    const result = await withSubmitLock(async () => {
      if (formData.id) {
        await menuApi.update(formData.id, { ...formData, parentId: formData.parentId ?? 0 })
      } else {
        const saveData: MenuSaveRequest = {
          parentId: formData.parentId ?? 0,
          menuName: formData.menuName,
          menuCode: formData.menuCode,
          menuType: formData.menuType,
          icon: formData.icon,
          path: formData.path,
          component: formData.component,
          permissions: formData.permissions,
          sortOrder: formData.sortOrder,
          status: formData.status,
          visible: formData.visible,
          keepAlive: formData.keepAlive,
          external: formData.external,
          remark: formData.remark
        }
        if (formData.bizFlowTag) saveData.bizFlowTag = formData.bizFlowTag
        if (formData.displayGroup !== undefined) saveData.displayGroup = formData.displayGroup
        if (formData.linkIcon) saveData.linkIcon = formData.linkIcon
        saveData.displayMode = formData.displayMode ?? 0
        if (formData.listPath) saveData.listPath = formData.listPath
        if (formData.tagLabel) saveData.tagLabel = formData.tagLabel
        saveData.menuLevel = formData.menuLevel ?? 0
        await menuApi.create(saveData)
      }

      message.success(formData.id ? '更新成功' : '创建成功')
      dialogVisible.value = false
      await refreshAllMenus()
    })
    void result
  } catch (error: any) {
    if (error) {
      console.warn('[系统管理] 提交表单失败', error)
      message.error(error?.message || '提交失败')
    }
  }
}

const handleFormClose = () => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '您有未保存的修改，确定要关闭吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => { dialogVisible.value = false },
    })
  } else {
    dialogVisible.value = false
  }
}

const handleFormSaveAndNew = () => {
  handleSubmit()
}

// 状态变更
const handleStatusChange = async (row: any, status: number) => {
  try {
    await menuApi.updateStatus(row.id, status)
    message.success('状态更新成功')
    await refreshAllMenus()
  } catch (error) {
    console.warn('[系统管理] 更新状态失败', error)
    message.error('状态更新失败')
    row.status = status === 1 ? 0 : 1
  }
}

// 分配角色（正向逻辑：遍历所选角色，逐一更新其菜单列表）
const handleAssignRole = async (row: any) => {
  currentMenu.value = row
  selectedRoles.value = []
  roleDialogVisible.value = true

  try {
    const data = await roleApi.listAll()
    roleList.value = (data as any) || []
  } catch (error) {
    console.warn('[系统管理] 获取角色列表失败', error)
  }
}

// 提交角色分配
const handleRoleSubmit = async () => {
  if (!currentMenu.value) return
  try {
    const menuId = currentMenu.value.id
    const result = await withRoleSubmitLock(async () => {
      // 对每个选中的角色：获取其当前菜单列表，确保包含本菜单 ID，然后更新
      for (const roleId of selectedRoles.value) {
        const currentMenuIds: number[] = (await roleApi.getMenus(roleId) as any) || []
        if (!currentMenuIds.includes(menuId)) {
          await roleApi.assignMenus(roleId, [...currentMenuIds, menuId])
        }
      }
      message.success('角色分配成功')
      roleDialogVisible.value = false
    })
    void result
  } catch (error: any) {
    if (error) {
      console.warn('[系统管理] 分配角色失败', error)
      message.error(error?.message || '分配角色失败')
    }
  }
}

// 重置表单
const resetForm = () => {
  formData.id = 0
  formData.parentId = undefined as unknown as number
  formData.menuName = ''
  formData.menuCode = ''
  formData.menuType = 1
  formData.icon = ''
  formData.path = ''
  formData.component = ''
  formData.permissions = ''
  formData.sortOrder = 0
  formData.status = 1
  formData.visible = 1
  formData.keepAlive = 0
  formData.external = 0
  formData.remark = ''
  formData.bizFlowTag = ''
  formData.displayGroup = 0
  formData.linkIcon = ''
  formData.displayMode = 0
  formData.listPath = ''
  formData.tagLabel = ''
  formData.menuLevel = 0
  formRef.value?.clearValidate()
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT'
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', loadMenuTree)()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !isInput) {
    e.preventDefault()
    handleAdd()
  }
}

// 初始化
onMounted(() => {
  loadMenuTree()
  loadMenuTypes()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery: loadMenuTree })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.menu-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.menu-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.menu-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.menu-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.menu-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.menu-management > :deep(.ant-table-wrapper) {
  flex: 1;
  min-height: 0;
  overflow: auto;          /* 唯一的滚动容器，sticky th 相对它定位 */
}

/* ── a-table 仿 BillDetailTable 样式 ─────────────────────────── */
/* 表格本体：必须 separate + spacing:0，sticky 才能正常工作 */
.menu-tree-table :deep(.ant-table) {
  font-size: 13px;
  border: 1px solid #e8e8e8;
  border-bottom: none;
}
.menu-tree-table :deep(.ant-table .ant-table-content) {
  border: 1px solid #e8e8e8;
  border-bottom: none;
}
.menu-tree-table :deep(.ant-table table) {
  border-collapse: separate;
  border-spacing: 0;
}
/* 表头行 — sticky 吸顶，与 BillDetailTable .ss-grid th 一致 */
.menu-tree-table :deep(.ant-table-thead > tr > th) {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #fafafa !important;
  border-right: 1px solid #e8e8e8;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 6px !important;
  text-align: center;
  font-weight: 600;
  color: #262626;
  font-size: 13px;
  height: 32px !important;
  line-height: 32px !important;
  white-space: nowrap;
  vertical-align: middle;
}
.menu-tree-table :deep(.ant-table-thead > tr > th:last-child) {
  border-right: none;
}
/* 数据行 — BillDetailTable .ss-grid td 参数
 * ⚠️ Ant Design Vue 4.x CSS-in-JS 注入 padding，必须 !important 覆盖 */
.menu-tree-table :deep(.ant-table-tbody > tr),
.menu-tree-table :deep(.ant-table-row) {
  height: 28px !important;
}
.menu-tree-table :deep(.ant-table-tbody > tr > td),
.menu-tree-table :deep(.ant-table-cell) {
  border-right: 1px solid #e8e8e8;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 6px !important;
  height: 28px !important;
  max-height: 28px !important;
  line-height: 28px !important;
  font-size: 13px;
  vertical-align: middle;
  box-sizing: border-box;
  overflow: hidden;
}
/* 行内组件尺寸约束，防止撑高行 */
.menu-tree-table :deep(.ant-table-tbody .ant-switch) {
  height: 18px !important;
  line-height: 18px !important;
  min-width: 36px;
  margin: 0;
  vertical-align: middle;
}
.menu-tree-table :deep(.ant-table-tbody .ant-switch .ant-switch-handle) {
  width: 14px;
  height: 14px;
  top: 2px;
  inset-inline-start: 2px;
}
.menu-tree-table :deep(.ant-table-tbody .ant-switch-checked .ant-switch-handle) {
  inset-inline-start: calc(100% - 14px - 2px);
}
.menu-tree-table :deep(.ant-table-tbody .ant-tag) {
  margin: 0 !important;
  padding: 0 4px !important;
  line-height: 18px !important;
  height: 18px !important;
  font-size: 12px;
  vertical-align: middle;
}
/* 操作按钮尺寸约束 */
.menu-tree-table :deep(.ant-table-tbody .ant-btn-link) {
  height: 22px !important;
  line-height: 22px !important;
  padding: 0 4px !important;
  margin: 0 !important;
  font-size: 13px;
  vertical-align: middle;
}
/* 操作列禁止换行 */
.menu-tree-table :deep(.ant-table-tbody > tr > td:last-child) {
  border-right: none;
  white-space: nowrap !important;
}
/* 全局约束：所有行内元素垂直居中，防止撑高 */
.menu-tree-table :deep(.ant-table-tbody > tr > td *) {
  vertical-align: middle;
}
/* 行悬浮 — BillDetailTable .ss-row:hover td */
.menu-tree-table :deep(.ant-table-tbody > tr:hover > td),
.menu-tree-table :deep(.ant-table-tbody > tr:hover > .ant-table-cell) {
  background: #f5f7fa !important;
}
.menu-tree-table :deep(.ant-table-tbody > tr.ant-table-row-selected > td),
.menu-tree-table :deep(.ant-table-tbody > tr.ant-table-row-selected > .ant-table-cell) {
  background: #e6f7ff !important;
}
/* 空数据提示 */
.menu-tree-table :deep(.ant-table-placeholder) {
  padding: 40px 0;
  font-size: 13px;
  color: #999;
}
/* 操作列按钮垂直居中 */
.menu-tree-table :deep(.ant-table-tbody > tr > td .ant-btn-link) {
  vertical-align: middle;
  padding: 0 4px;
  height: 24px;
  font-size: 13px;
  line-height: 1.4;
}
/* 表格内容溢出省略 */
.menu-tree-table :deep(.ant-table-cell-ellipsis) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-enabled { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-disabled { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-button { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

.menu-icon {
  margin-right: 4px;
  font-size: 14px;
  vertical-align: middle;
}

/* ── 树缩进与展开图标 ──────────────────────────── */
.tree-indent {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
}
.tree-expand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-right: 4px;
  cursor: pointer;
  border-radius: 3px;
  font-size: 10px;
  color: #606266;
  transition: background 0.15s;
  flex-shrink: 0;
}
.tree-expand-icon:hover {
  background: #e8e8e8;
}
.tree-expand-placeholder {
  display: inline-block;
  width: 16px;
  height: 16px;
  margin-right: 4px;
  flex-shrink: 0;
}
.tree-node-clickable {
  cursor: pointer;
}
.tree-node-clickable:hover {
  color: #1890ff;
}

/* ── 工具栏 ─────────────────────────────────── */
.menu-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 12px 0;
}

/* ── 分页 ───────────────────────────────────── */
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 10px 0 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}

/* ── FullScreenDetail 内部紧凑样式 ────────────────────── */
:deep(.fsd-body .ant-form-item) { margin-bottom: 8px; }
:deep(.fsd-body .ant-form-item-label > label) { font-size: 12px; height: 28px; }
:deep(.fsd-body .ant-input), :deep(.fsd-body .ant-input-number), :deep(.fsd-body .ant-select), :deep(.fsd-body .ant-picker), :deep(.fsd-body .ant-cascader-picker) { font-size: 12px; }
:deep(.fsd-body .ant-input-number-input) { font-size: 12px; }
:deep(.fsd-body .ant-select-selection-item) { font-size: 12px; }
:deep(.fsd-body .ant-btn) { font-size: 12px; }

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
