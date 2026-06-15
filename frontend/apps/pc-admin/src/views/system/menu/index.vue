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
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
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

      <a-skeleton active v-if="loading && menuTree.length === 0" :paragraph="{ rows: 8 }" style="padding: 24px;" />

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="menuTree"
      :loading="loading"
      :pagination="null as any"
      :row-key="'id'"
      :min-empty-rows="12"
      :filter-fields="filterFields"
      :show-search="false"
      :show-add="false"
      :show-edit="false"
      :show-delete="false"
      :show-batch-delete="false"
      :selectable="false"
      :bordered="true"
      @refresh="debounceClick('refresh', loadMenuTree)"
      @filter-change="handleFilterChange"
      @cell-dblclick="handleView"
    >
      <template #toolbar-actions>
        <a-button type="primary" :loading="submitLoading" v-permission="'system:permission:create'" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新增菜单
        </a-button>
      </template>

      <template #empty>
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

      <template #menuNameCell="{ record }">
        <component
          v-if="record.icon"
          :is="iconComponent(record.icon)"
          class="menu-icon"
        />
        <span>{{ record.menuName }}</span>
      </template>
      <template #menuTypeCell="{ record }">
        <a-tag v-if="record.menuType === 0">目录</a-tag>
        <a-tag v-else-if="record.menuType === 1" color="green">菜单</a-tag>
        <a-tag v-else-if="record.menuType === 2" color="orange">按钮</a-tag>
      </template>
      <template #statusCell="{ record }">
        <a-switch
          :checked="record.status === 1"
          @change="(checked: boolean) => handleStatusChange(record, checked ? 1 : 0)"
        />
      </template>
      <template #visibleCell="{ record }">
        <a-tag v-if="record.visible === 1" color="green">显示</a-tag>
        <a-tag v-else>隐藏</a-tag>
      </template>
      <template #bizFlowTagCell="{ record }">
        <a-tag v-if="record.bizFlowTag" :color="getBizFlowTagColor(record.bizFlowTag)">
          {{ getBizFlowTagLabel(record.bizFlowTag) }}
        </a-tag>
        <span v-else class="text-muted">—</span>
      </template>
      <template #displayGroupCell="{ record }">
        <a-tag v-if="record.displayGroup === 1" color="blue">展示分组</a-tag>
        <a-tag v-else color="green">路由目录</a-tag>
      </template>

      <template #action="{ record }">
        <a-button type="link" size="small" v-permission="'system:permission:create'" @click="handleAddChild(record)">
          <template #icon><PlusOutlined /></template>新增
        </a-button>
        <a-button type="link" size="small" v-permission="'system:permission:update'" @click="handleEdit(record)">
          <template #icon><EditOutlined /></template>编辑
        </a-button>
        <a-button type="link" size="small" v-permission="'system:permission:assign'" @click="handleAssignRole(record)">
          <template #icon><UserOutlined /></template>分配角色
        </a-button>
        <a-button type="link" size="small" danger v-permission="'system:permission:delete'" @click="handleDelete(record)">
          <template #icon><DeleteOutlined /></template>删除
        </a-button>
      </template>
    </VxeTableList>

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
            :tree-data="menuTree"
            :field-names="{ label: 'menuName', value: 'id', children: 'children' }"
            placeholder="请选择上级菜单"
            allow-clear
            tree-check-strictly
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
            placeholder="请输入路由路径，如：/system/user"
          />
        </a-form-item>

        <a-form-item v-if="formData.menuType === 1" label="组件路径" name="component">
          <a-input
            v-model:value="formData.component"
            placeholder="请输入组件路径，如：system/user/index"
          />
        </a-form-item>

        <a-form-item v-if="formData.menuType !== 2" label="菜单图标">
          <a-input
            v-model:value="formData.icon"
            placeholder="请输入图标名称，如：UserOutlined"
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
  WarningOutlined
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import menuApi, { type MenuInfo, type MenuQuery, type MenuSaveRequest, type MenuUpdateRequest } from '@/api/menu'
import roleApi from '@/api/role'
import { dictItemApi } from '@/api/dict'
import { useSubmitLock } from '@/composables'
import * as Icons from '@ant-design/icons-vue'

const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

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

// 表格列配置
const vxeColumns = computed(() => [
  { field: 'menuName', title: '菜单名称', width: 200, showOverflow: 'tooltip', slotName: 'menuNameCell' },
  { field: 'menuCode', title: '权限标识', width: 180, showOverflow: 'tooltip' },
  { field: 'path', title: '路由路径', width: 180, showOverflow: 'tooltip' },
  { field: 'menuType', title: '类型', width: 100, align: 'center', slotName: 'menuTypeCell' },
  { field: 'sortOrder', title: '排序', width: 80, align: 'center' },
  { field: 'status', title: '状态', width: 100, align: 'center', slotName: 'statusCell' },
  { field: 'visible', title: '显示', width: 80, align: 'center', slotName: 'visibleCell' },
  { field: 'bizFlowTag', title: '业务流向', width: 100, align: 'center', slotName: 'bizFlowTagCell' },
  { field: 'displayGroup', title: '展示分组', width: 100, align: 'center', slotName: 'displayGroupCell' },
  { field: 'linkIcon', title: '链接图标', width: 100, showOverflow: 'tooltip' },
  { type: 'action', title: '操作', width: 280, fixed: 'right' }
])

// ── 菜单类型选项（从API加载） ──────────────────────────
const menuTypeOptions = ref<{ label: string; value: number }[]>([])

async function loadMenuTypes() {
  try {
    const res = await dictItemApi.getByDictCode('MENU_TYPE')
    if (res.data) {
      menuTypeOptions.value = res.data
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map(item => ({ label: item.itemText, value: Number(item.itemValue) }))
    }
  } catch (err) {
    console.warn('[菜单管理] 加载菜单类型失败', err)
  }
}

// 筛选字段
const filterFields = computed<FilterField[]>(() => [
  { key: 'menuName', label: '菜单名称', type: 'input', placeholder: '请输入菜单名称' },
  { key: 'menuType', label: '菜单类型', type: 'select', options: menuTypeOptions.value },
  { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }] },
])

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
  parentId: 0,
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
  linkIcon: ''
})

// 表单验证规则
const formRules: any = {
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuCode: [{ required: true, message: '请输入权限标识', trigger: 'blur' }],
  path: [{ required: true, message: '请输入路由路径', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

// 角色分配弹窗
const roleDialogVisible = ref(false)
const { isSubmitting: roleSubmitLoading, withSubmitLock: withRoleSubmitLock } = useSubmitLock()
const currentMenu = ref<MenuInfo | null>(null)
const selectedRoles = ref<number[]>([])
const roleList = ref<any[]>([])

// 筛选变化
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(queryForm, { menuName: '', menuType: undefined, status: undefined })
  } else {
    Object.assign(queryForm, filters)
  }
  loadMenuTree()
}

// 加载菜单树
const loadMenuTree = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await menuApi.getTree(queryForm)
    if (res) {
      menuTree.value = res
    } else {
      message.error('获取菜单列表失败')
      menuTree.value = []
    }
  } catch (error) {
    hasError.value = true
    console.warn('[系统管理] 获取菜单列表失败', error)
    message.error('获取菜单列表失败')
    menuTree.value = []
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

// 搜索
const handleSearch = () => {
  loadMenuTree()
}

// 重置
const handleReset = () => {
  queryForm.menuName = ''
  queryForm.menuType = undefined
  queryForm.status = undefined
  loadMenuTree()
}

// 新增菜单
const handleAdd = () => {
  dialogTitle.value = '新增菜单'
  resetForm()
  dialogVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 新增子菜单
const handleAddChild = (row: MenuInfo) => {
  dialogTitle.value = `新增子菜单 - ${row.menuName}`
  resetForm()
  formData.parentId = row.id
  dialogVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 编辑菜单
const handleEdit = (row: MenuInfo) => {
  dialogTitle.value = '编辑菜单'
  resetForm()
  Object.assign(formData, {
    id: row.id,
    parentId: row.parentId,
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
    linkIcon: row.linkIcon || ''
  })
  dialogVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 删除菜单
const handleDelete = async (row: MenuInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除菜单"${row.menuName}"吗？删除后不可恢复！`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      const res = await menuApi.delete(row.id)
      if (res.code === 200) {
        message.success('删除成功')
        loadMenuTree()
      } else {
        message.error(res.message || '删除失败')
      }
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
      let res
      if (formData.id) {
        res = await menuApi.update(formData.id, formData)
      } else {
        const saveData: MenuSaveRequest = {
          parentId: formData.parentId,
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
        res = await menuApi.create(saveData)
      }

      if (res.code === 200) {
        message.success(formData.id ? '更新成功' : '创建成功')
        dialogVisible.value = false
        loadMenuTree()
      } else {
        throw new Error(res.message || (formData.id ? '更新失败' : '创建失败'))
      }
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
const handleStatusChange = async (row: MenuInfo, status: number) => {
  try {
    const res = await menuApi.updateStatus(row.id, status)
    if (res.code === 200) {
      message.success('状态更新成功')
    } else {
      message.error(res.message || '状态更新失败')
      row.status = status === 1 ? 0 : 1
    }
  } catch (error) {
    console.warn('[系统管理] 更新状态失败', error)
    message.error('状态更新失败')
    row.status = status === 1 ? 0 : 1
  }
}

// 分配角色
const handleAssignRole = async (row: MenuInfo) => {
  currentMenu.value = row
  selectedRoles.value = []
  roleDialogVisible.value = true

  try {
    const res = await roleApi.listAll()
    if (res.code === 200) {
      roleList.value = res.data || []
    }
  } catch (error) {
    console.warn('[系统管理] 获取角色列表失败', error)
  }

  // TODO: 获取已分配此菜单的角色ID列表用于预选中
  // 需要后端提供 GET /api/role/menu/{menuId} 端点
}

// 提交角色分配
const handleRoleSubmit = async () => {
  if (!currentMenu.value) return
  try {
    const result = await withRoleSubmitLock(async () => {
      await roleApi.assignMenus(currentMenu.value.id, selectedRoles.value)
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
  formData.parentId = 0
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
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadMenuTree()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: loadMenuTree })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
// 查看详情
const handleView = (record: any) => {}
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

.menu-management > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
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
  margin-right: 8px;
  font-size: 16px;
}

/* ── VxeTable 表头边框线 2px ─────────────────────────── */
.menu-management :deep(.vxe-table .vxe-header--row th) {
  border-bottom: 2px solid #e8e8e8 !important;
}
.menu-management :deep(.vxe-table .vxe-header--row th:not(:last-child)) {
  border-right: 1px solid #e8e8e8 !important;
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
