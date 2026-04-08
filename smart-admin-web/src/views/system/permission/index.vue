<template>
  <div class="permission-management">
    <!-- 搜索区域 -->
    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="searchForm" class="search-form">
        <a-row :gutter="16" style="width: 100%">
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="权限名称">
              <a-input v-model:value="searchForm.permissionName" placeholder="请输入权限名称" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="权限类型">
              <a-select v-model:value="searchForm.permissionType" placeholder="请选择权限类型" allow-clear style="width: 100%">
                <a-select-option :value="0">目录</a-select-option>
                <a-select-option :value="1">菜单</a-select-option>
                <a-select-option :value="2">按钮</a-select-option>
                <a-select-option :value="3">API</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="状态">
              <a-select v-model:value="searchForm.status" placeholder="请选择状态" allow-clear style="width: 100%">
                <a-select-option :value="0">启用</a-select-option>
                <a-select-option :value="1">停用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="handleSearch">
                  <template #icon><SearchOutlined /></template>
                  搜索
                </a-button>
                <a-button @click="handleReset">
                  <template #icon><ReloadOutlined /></template>
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="table-card" :bordered="false">
      <template #title>
        <div class="table-header">
          <span class="title">权限列表</span>
          <a-space>
            <a-button type="primary" @click="handleAdd(null)">
              <template #icon><PlusOutlined /></template>
              新增顶级权限
            </a-button>
            <a-button @click="handleExpandAll">
              <template #icon><ExpandOutlined /></template>
              展开/折叠
            </a-button>
          </a-space>
        </div>
      </template>
      
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="false"
        row-key="id"
        :expand-row-by-click="true"
        :expanded-row-keys="expandedKeys"
        @expand="(expanded: boolean, record: PermissionInfo) => handleExpand(expanded, record)"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'permissionName'">
            <a-space>
              <component :is="getIcon(record.icon)" v-if="record.icon" />
              <span>{{ record.permissionName }}</span>
              <a-tag v-if="record.permissionType === 0" color="blue">目录</a-tag>
              <a-tag v-else-if="record.permissionType === 1" color="green">菜单</a-tag>
              <a-tag v-else-if="record.permissionType === 2" color="orange">按钮</a-tag>
              <a-tag v-else-if="record.permissionType === 3" color="purple">API</a-tag>
            </a-space>
          </template>
          
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 0 ? 'success' : 'error'">
              {{ record.status === 0 ? '启用' : '停用' }}
            </a-tag>
          </template>
          
          <template v-else-if="column.key === 'visible'">
            <a-tag :color="record.visible === 1 ? 'success' : 'default'">
              {{ record.visible === 1 ? '显示' : '隐藏' }}
            </a-tag>
          </template>
          
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleAdd(record)">
                新增子权限
              </a-button>
              <a-button type="link" size="small" @click="handleEdit(record)">
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除此权限及其子权限吗？"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 权限表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
      width="700px"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 17 }"
      >
        <a-form-item v-if="!isTopLevel" label="父级权限" name="parentId">
          <a-tree-select
            v-model:value="formState.parentId"
            :tree-data="parentTreeData"
            placeholder="请选择父级权限"
            :field-names="{ label: 'permissionName', value: 'id' }"
            tree-default-expand-all
          />
        </a-form-item>
        
        <a-form-item label="权限名称" name="permissionName">
          <a-input v-model:value="formState.permissionName" placeholder="请输入权限名称" />
        </a-form-item>
        
        <a-form-item label="权限编码" name="permissionCode">
          <a-input v-model:value="formState.permissionCode" placeholder="请输入权限编码，如：system:user:list" />
        </a-form-item>
        
        <a-form-item label="权限类型" name="permissionType">
          <a-select v-model:value="formState.permissionType" placeholder="请选择权限类型">
            <a-select-option :value="0">目录</a-select-option>
            <a-select-option :value="1">菜单</a-select-option>
            <a-select-option :value="2">按钮</a-select-option>
            <a-select-option :value="3">API</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item v-if="formState.permissionType <= 1" label="路由路径" name="path">
          <a-input v-model:value="formState.path" placeholder="请输入路由路径，如：/system/user" />
        </a-form-item>
        
        <a-form-item v-if="formState.permissionType <= 1" label="组件路径" name="component">
          <a-input v-model:value="formState.component" placeholder="请输入组件路径，如：@/views/system/user/index" />
        </a-form-item>
        
        <a-form-item v-if="formState.permissionType === 3" label="API路径" name="apiPath">
          <a-input v-model:value="formState.apiPath" placeholder="请输入API路径，如：/api/user/list" />
        </a-form-item>
        
        <a-form-item v-if="formState.permissionType === 3" label="请求方法" name="method">
          <a-select v-model:value="formState.method" placeholder="请选择请求方法">
            <a-select-option value="GET">GET</a-select-option>
            <a-select-option value="POST">POST</a-select-option>
            <a-select-option value="PUT">PUT</a-select-option>
            <a-select-option value="DELETE">DELETE</a-select-option>
            <a-select-option value="PATCH">PATCH</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item v-if="formState.permissionType <= 1" label="图标" name="icon">
          <a-input v-model:value="formState.icon" placeholder="请输入图标名称，如：UserOutlined" />
        </a-form-item>
        
        <a-form-item v-if="formState.permissionType <= 1" label="是否显示" name="visible">
          <a-radio-group v-model:value="formState.visible">
            <a-radio :value="1">显示</a-radio>
            <a-radio :value="0">隐藏</a-radio>
          </a-radio-group>
        </a-form-item>
        
        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="formState.sort" :min="0" :max="9999" style="width: 100%" />
        </a-form-item>
        
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formState.status">
            <a-radio :value="0">启用</a-radio>
            <a-radio :value="1">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  ExpandOutlined,
  UserOutlined,
  SettingOutlined,
  DashboardOutlined,
  FileTextOutlined,
  ApiOutlined
} from '@ant-design/icons-vue'
import { permissionApi, type PermissionInfo } from '@/api/permission'

// 搜索表单
const searchForm = reactive({
  permissionName: '',
  permissionType: undefined as number | undefined,
  status: undefined as number | undefined
})

// 表格数据
const tableData = ref<PermissionInfo[]>([])
const loading = ref(false)
const expandedKeys = ref<number[]>([])

// 表格列定义
const columns = [
  { title: '权限名称', key: 'permissionName', width: 250 },
  { title: '权限编码', dataIndex: 'permissionCode', width: 200, ellipsis: true },
  { title: '路由/API路径', key: 'path', width: 200, ellipsis: true },
  { title: '排序', dataIndex: 'sort', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '显示', key: 'visible', width: 80 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
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
  tenantId: 1,
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
  permissionName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  permissionCode: [
    { required: true, message: '请输入权限编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9:_-]+$/, message: '编码只能包含字母、数字、冒号、下划线和短横线', trigger: 'blur' }
  ],
  permissionType: [{ required: true, message: '请选择权限类型', trigger: 'change' }],
  path: [{ required: true, message: '请输入路由路径', trigger: 'blur' }],
  apiPath: [{ required: true, message: '请输入API路径', trigger: 'blur' }]
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

const handleExpand = (expanded: boolean, record: PermissionInfo) => {
  if (expanded) {
    expandedKeys.value.push(record.id)
  } else {
    const index = expandedKeys.value.indexOf(record.id)
    if (index > -1) {
      expandedKeys.value.splice(index, 1)
    }
  }
}

// 新增权限
const handleAdd = (record: PermissionInfo | null) => {
  isEdit.value = false
  isTopLevel.value = record === null
  
  Object.assign(formState, {
    id: 0,
    parentId: record ? record.id : 0,
    tenantId: 1,
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
const handleDelete = async (record: PermissionInfo) => {
  try {
    await permissionApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

onMounted(() => {
  fetchData()
  loadParentTree()
})
</script>

<style scoped>
.permission-management {
  padding: 0;
}

.search-card {
  margin-bottom: 16px;
}

.search-form {
  margin-bottom: -24px;
}

.table-card :deep(.ant-card-head) {
  border-bottom: none;
  padding-bottom: 0;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.table-header .title {
  font-size: 16px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .search-form :deep(.ant-form-item) {
    margin-bottom: 16px;
  }
  
  .table-header {
    flex-direction: column;
    gap: 12px;
  }
}
</style>