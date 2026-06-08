<template>
  <div class="permission-management">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ permissionCount }}</div>
          <div class="stat-card-label">权限总数</div>
        </div>
        <SafetyOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-menu">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ menuCount }}</div>
          <div class="stat-card-label">菜单权限</div>
        </div>
        <MenuOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-button">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ buttonCount }}</div>
          <div class="stat-card-label">按钮权限</div>
        </div>
        <ControlOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-api">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ apiCount }}</div>
          <div class="stat-card-label">API权限</div>
        </div>
        <ApiOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableData"
      :loading="loading"
      :pagination="null as any"
      :row-key="'id'"
      :filter-fields="filterFields"
      :show-search="false"
      :show-add="false"
      :show-edit="false"
      :show-delete="false"
      :show-batch-delete="false"
      :selectable="false"
      @refresh="fetchData"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-actions>
        <a-button type="primary" @click="handleAdd(null)">
          <template #icon><PlusOutlined /></template>
          新增顶级权限
        </a-button>
        <a-button @click="handleExpandAll">
          <template #icon><ExpandOutlined /></template>
          展开/折叠
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.field === 'permissionName'">
          <a-space>
            <component
              :is="getIcon(record.icon)"
              v-if="record.icon"
            />
            <span>{{ record.permissionName }}</span>
            <a-tag
              v-if="record.permissionType === 0"
              color="blue"
            >
              目录
            </a-tag>
            <a-tag
              v-else-if="record.permissionType === 1"
              color="green"
            >
              菜单
            </a-tag>
            <a-tag
              v-else-if="record.permissionType === 2"
              color="orange"
            >
              按钮
            </a-tag>
            <a-tag
              v-else-if="record.permissionType === 3"
              color="purple"
            >
              API
            </a-tag>
          </a-space>
        </template>

        <template v-else-if="column.field === 'status'">
          <a-tag :color="record.status === 0 ? 'success' : 'error'">
            {{ record.status === 0 ? '启用' : '停用' }}
          </a-tag>
        </template>

        <template v-else-if="column.field === 'visible'">
          <a-tag :color="record.visible === 1 ? 'success' : 'default'">
            {{ record.visible === 1 ? '显示' : '隐藏' }}
          </a-tag>
        </template>
      </template>

      <template #action="{ record }">
        <a-space>
          <a-button
            type="link"
            size="small"
            @click="handleAdd(record)"
          >
            新增子权限
          </a-button>
          <a-button
            type="link"
            size="small"
            @click="handleEdit(record)"
          >
            编辑
          </a-button>
          <a-button
            type="link"
            size="small"
            danger
            @click="handleDeleteConfirm(record)"
          >
            删除
          </a-button>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 权限表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      width="700px"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 17 }"
      >
        <a-form-item
          v-if="!isTopLevel"
          label="父级权限"
          name="parentId"
        >
          <a-tree-select
            v-model:value="formState.parentId"
            :tree-data="parentTreeData"
            placeholder="请选择父级权限"
            :field-names="{ label: 'permissionName', value: 'id' }"
            tree-default-expand-all
          />
        </a-form-item>

        <a-form-item
          label="权限名称"
          name="permissionName"
        >
          <a-input
            v-model:value="formState.permissionName"
            placeholder="请输入权限名称"
          />
        </a-form-item>

        <a-form-item
          label="权限编码"
          name="permissionCode"
        >
          <a-input
            v-model:value="formState.permissionCode"
            placeholder="请输入权限编码，如：system:user:list"
          />
        </a-form-item>

        <a-form-item
          label="权限类型"
          name="permissionType"
        >
          <a-select
            v-model:value="formState.permissionType"
            placeholder="请选择权限类型"
          >
            <a-select-option :value="0">
              目录
            </a-select-option>
            <a-select-option :value="1">
              菜单
            </a-select-option>
            <a-select-option :value="2">
              按钮
            </a-select-option>
            <a-select-option :value="3">
              API
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          v-if="formState.permissionType <= 1"
          label="路由路径"
          name="path"
        >
          <a-input
            v-model:value="formState.path"
            placeholder="请输入路由路径，如：/system/user"
          />
        </a-form-item>

        <a-form-item
          v-if="formState.permissionType <= 1"
          label="组件路径"
          name="component"
        >
          <a-input
            v-model:value="formState.component"
            placeholder="请输入组件路径，如：@/views/system/user/index"
          />
        </a-form-item>

        <a-form-item
          v-if="formState.permissionType === 3"
          label="API路径"
          name="apiPath"
        >
          <a-input
            v-model:value="formState.apiPath"
            placeholder="请输入API路径，如：/api/user/list"
          />
        </a-form-item>

        <a-form-item
          v-if="formState.permissionType === 3"
          label="请求方法"
          name="method"
        >
          <a-select
            v-model:value="formState.method"
            placeholder="请选择请求方法"
          >
            <a-select-option value="GET">
              GET
            </a-select-option>
            <a-select-option value="POST">
              POST
            </a-select-option>
            <a-select-option value="PUT">
              PUT
            </a-select-option>
            <a-select-option value="DELETE">
              DELETE
            </a-select-option>
            <a-select-option value="PATCH">
              PATCH
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          v-if="formState.permissionType <= 1"
          label="图标"
          name="icon"
        >
          <a-input
            v-model:value="formState.icon"
            placeholder="请输入图标名称，如：UserOutlined"
          />
        </a-form-item>

        <a-form-item
          v-if="formState.permissionType <= 1"
          label="是否显示"
          name="visible"
        >
          <a-radio-group v-model:value="formState.visible">
            <a-radio :value="1">
              显示
            </a-radio>
            <a-radio :value="0">
              隐藏
            </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item
          label="排序"
          name="sort"
        >
          <a-input-number
            v-model:value="formState.sort"
            :min="0"
            :max="9999"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item
          label="状态"
          name="status"
        >
          <a-radio-group v-model:value="formState.status">
            <a-radio :value="0">
              启用
            </a-radio>
            <a-radio :value="1">
              停用
            </a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined,
  ExpandOutlined,
  UserOutlined,
  SettingOutlined,
  DashboardOutlined,
  FileTextOutlined,
  ApiOutlined,
  SafetyOutlined,
  MenuOutlined,
  ControlOutlined
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { permissionApi, type PermissionInfo } from '@/api/permission'
import { useUserStore } from '@/stores/user'

// 搜索表单
const userStore = useUserStore()
const searchForm = reactive({
  permissionName: '',
  permissionType: undefined as number | undefined,
  status: undefined as number | undefined
})

// 表格数据
const tableData = ref<PermissionInfo[]>([])
const loading = ref(false)
const expandedKeys = ref<number[]>([])

// ── 统计数据 ────────────────────────────────────────────
const flattenPermissions = (tree: PermissionInfo[]): PermissionInfo[] => {
  const result: PermissionInfo[] = []
  const traverse = (nodes: PermissionInfo[]) => {
    for (const node of nodes) {
      result.push(node)
      if (node.children?.length) traverse(node.children)
    }
  }
  traverse(tree)
  return result
}
const permissionCount = computed(() => flattenPermissions(tableData.value).length)
const menuCount = computed(() => flattenPermissions(tableData.value).filter(p => p.permissionType === 0 || p.permissionType === 1).length)
const buttonCount = computed(() => flattenPermissions(tableData.value).filter(p => p.permissionType === 2).length)
const apiCount = computed(() => flattenPermissions(tableData.value).filter(p => p.permissionType === 3).length)

// 表格列定义
const vxeColumns = computed(() => [
  { field: 'permissionName', title: '权限名称', width: 250 },
  { field: 'permissionCode', title: '权限编码', width: 200, showOverflow: 'tooltip' },
  { field: 'path', title: '路由/API路径', width: 200, showOverflow: 'tooltip' },
  { field: 'sort', title: '排序', width: 80 },
  { field: 'status', title: '状态', width: 80 },
  { field: 'visible', title: '显示', width: 80 },
  { type: 'action', title: '操作', width: 220, fixed: 'right' }
])

// 筛选字段
const filterFields: FilterField[] = [
  { key: 'permissionName', label: '权限名称', type: 'input', placeholder: '请输入权限名称' },
  { key: 'permissionType', label: '权限类型', type: 'select', options: [
    { label: '目录', value: 0 }, { label: '菜单', value: 1 }, { label: '按钮', value: 2 }, { label: 'API', value: 3 }
  ]},
  { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 0 }, { label: '停用', value: 1 }] },
]

// 弹窗相关
const modalVisible = ref(false)
const modalLoading = ref(false)
const modalTitle = computed(() => isEdit.value ? '编辑权限' : '新增权限')
const isEdit = ref(false)
const isTopLevel = ref(false)
const formRef = ref<FormInstance>()
const parentTreeData = ref<PermissionInfo[]>([])

const formState = reactive({
  id: 0,
  parentId: 0,
  tenantId: userStore.tenantId,
  permissionName: '',
  permissionCode: '',
  permissionType: 1,
  path: '',
  component: '',
  icon: '',
  apiPath: '',
  method: 'GET',
  sort: 0,
  visible: 1,
  status: 0
})

const formRules = {
  permissionName: { required: true, message: '请输入权限名称', trigger: 'blur' },
  permissionCode: [
    { required: true, message: '请输入权限编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9:_-]+$/, message: '编码只能包含字母、数字、冒号、下划线和短横线', trigger: 'blur' }
  ],
  permissionType: { required: true, message: '请选择权限类型', trigger: 'change' },
  path: { required: true, message: '请输入路由路径', trigger: 'blur' },
  apiPath: { required: true, message: '请输入API路径', trigger: 'blur' },
}

// 图标映射
const iconMap: Record<string, any> = {
  UserOutlined,
  SettingOutlined,
  DashboardOutlined,
  FileTextOutlined,
  ApiOutlined
}

const getIcon = (iconName: string) => {
  return iconMap[iconName] || null
}

// 数据加载
const fetchData = async () => {
  loading.value = true
  try {
    const res = await permissionApi.getTree(1)
    if (res.data) {
      tableData.value = res.data
      // 默认展开第一层
      expandedKeys.value = res.data.filter(item => item.permissionType === 0).map(item => item.id)
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 加载父级权限树
const loadParentTree = async () => {
  try {
    const res = await permissionApi.getTree(1)
    if (res.data) {
      parentTreeData.value = res.data
    }
  } catch (error) {
    message.error('加载父级权限失败')
  }
}

// 搜索相关
const handleSearch = () => {
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, { permissionName: '', permissionType: undefined, status: undefined })
  handleSearch()
}

// 筛选变化
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, { permissionName: '', permissionType: undefined, status: undefined })
  } else {
    Object.assign(searchForm, filters)
  }
  fetchData()
}

// 展开/折叠
const handleExpandAll = () => {
  if (expandedKeys.value.length === 0) {
    const getAllKeys = (items: PermissionInfo[]): number[] => {
      const keys: number[] = []
      items.forEach(item => {
        keys.push(item.id)
        if (item.children && item.children.length > 0) {
          keys.push(...getAllKeys(item.children))
        }
      })
      return keys
    }
    expandedKeys.value = getAllKeys(tableData.value)
  } else {
    expandedKeys.value = []
  }
}

// 新增权限
const handleAdd = (record: PermissionInfo | null) => {
  isEdit.value = false
  isTopLevel.value = record === null

  Object.assign(formState, {
    id: 0,
    parentId: record ? record.id : 0,
    tenantId: userStore.tenantId,
    permissionName: '',
    permissionCode: '',
    permissionType: record ? (record.permissionType === 0 ? 1 : 2) : 0,
    path: '',
    component: '',
    icon: '',
    apiPath: '',
    method: 'GET',
    sort: 0,
    visible: 1,
    status: 0
  })

  modalVisible.value = true
}

// 编辑权限
const handleEdit = (record: PermissionInfo) => {
  isEdit.value = false
  isTopLevel.value = record.parentId === 0

  Object.assign(formState, {
    id: record.id,
    parentId: record.parentId,
    tenantId: record.tenantId,
    permissionName: record.permissionName,
    permissionCode: record.permissionCode,
    permissionType: record.permissionType,
    path: record.path,
    component: record.component,
    icon: record.icon,
    apiPath: record.apiPath,
    method: record.method,
    sort: record.sort,
    visible: record.visible,
    status: record.status
  })

  modalVisible.value = true
}

// 提交表单
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    if (formState.id) {
      await permissionApi.update(formState.id, formState)
      message.success('更新成功')
    } else {
      await permissionApi.create(formState)
      message.success('创建成功')
    }

    modalVisible.value = false
    fetchData()
  } catch (error) {
    message.error('操作失败')
  } finally {
    modalLoading.value = false
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// 删除权限
const handleDeleteConfirm = (record: PermissionInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除权限 "${record.permissionName}" 及其子权限吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await permissionApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error: any) {
        message.error(error.message || '删除失败')
      }
    }
  })
}

onMounted(() => {
  fetchData()
  loadParentTree()
})
</script>

<style scoped>
.permission-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
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
.stat-menu { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-button { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-api { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

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

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>
