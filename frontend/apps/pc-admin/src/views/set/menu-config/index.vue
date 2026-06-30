<template>
  <ErrorBoundary>
    <PageContainer title="菜单配置">
      <div class="content-card">
        <div class="toolbar">
          <a-button type="primary" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            新增菜单
          </a-button>
          <a-button @click="handleExpandAll">{{ expandAll ? '折叠全部' : '展开全部' }}</a-button>
          <a-button @click="handleRefresh">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
        <a-spin :spinning="loading">
          <a-table
            :columns="columns"
            :data-source="menuTree"
            :pagination="false"
            row-key="id"
            size="small"
            :expandable="{ defaultExpandAllRows: expandAll }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'menuName'">
                <span :style="{ fontWeight: record.menuType === 0 ? 600 : 'normal' }">
                  {{ record.menuName }}
                </span>
              </template>
              <template v-if="column.key === 'menuType'">
                <a-tag :color="record.menuType === 0 ? 'blue' : record.menuType === 1 ? 'green' : 'orange'">
                  {{ record.menuType === 0 ? '目录' : record.menuType === 1 ? '菜单' : '按钮' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 0 ? 'success' : 'error'">{{ record.status === 0 ? '正常' : '停用' }}</a-tag>
              </template>
              <template v-if="column.key === 'visible'">
                <a-tag :color="record.visible === 0 ? 'success' : 'warning'">{{ record.visible === 0 ? '显示' : '隐藏' }}</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
                <a-button type="link" size="small" @click="handleAddChild(record)">添加子级</a-button>
                <a-button type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
              </template>
            </template>
          </a-table>
        </a-spin>
      </div>
    </PageContainer>

    <a-modal v-model:open="editVisible" :title="isEditing ? '编辑菜单' : '新增菜单'" width="600px" @ok="handleSave" :confirm-loading="saving">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="上级菜单">
          <a-tree-select
            v-model:value="editForm.parentId"
            :tree-data="menuTreeSelect"
            :field-names="{ children: 'children', label: 'menuName', value: 'id' }"
            placeholder="顶级菜单"
            allow-clear
            tree-default-expand-all
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="菜单名称" required>
              <a-input v-model:value="editForm.menuName" placeholder="请输入菜单名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单类型" required>
              <a-select v-model:value="editForm.menuType">
                <a-select-option :value="0">目录</a-select-option>
                <a-select-option :value="1">菜单</a-select-option>
                <a-select-option :value="2">按钮</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="路由路径">
          <a-input v-model:value="editForm.path" placeholder="请输入路由路径（如 set/sys-params）" />
        </a-form-item>
        <a-form-item label="组件路径" v-if="editForm.menuType === 1">
          <a-input v-model:value="editForm.component" placeholder="请输入组件路径（如 views/set/sys-params/index.vue）" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="排序">
              <a-input-number v-model:value="editForm.sort" style="width: 100%" :min="0" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="状态">
              <a-select v-model:value="editForm.status">
                <a-select-option :value="0">正常</a-select-option>
                <a-select-option :value="1">停用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="是否显示">
              <a-select v-model:value="editForm.visible">
                <a-select-option :value="0">显示</a-select-option>
                <a-select-option :value="1">隐藏</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="权限标识">
          <a-input v-model:value="editForm.perms" placeholder="请输入权限标识（如 system:config:list）" />
        </a-form-item>
        <a-form-item label="图标">
          <a-input v-model:value="editForm.icon" placeholder="请输入图标名称（如 SettingOutlined）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const expandAll = ref(false)
const menuTree = ref<any[]>([])
const menuTreeSelect = ref<any[]>([])

const editVisible = ref(false)
const isEditing = ref(false)
const editForm = reactive<any>({
  parentId: undefined,
  menuName: '',
  menuType: 1,
  path: '',
  component: '',
  sort: 0,
  status: 0,
  visible: 0,
  perms: '',
  icon: ''
})

const columns: any[] = [
  { title: '菜单名称', dataIndex: 'menuName', key: 'menuName' },
  { title: '图标', dataIndex: 'icon', key: 'icon', width: 80 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 60 },
  { title: '类型', dataIndex: 'menuType', key: 'menuType', width: 70 },
  { title: '路由路径', dataIndex: 'path', key: 'path', width: 180 },
  { title: '组件', dataIndex: 'component', key: 'component', width: 220, ellipsis: true },
  { title: '权限标识', dataIndex: 'perms', key: 'perms', width: 180, ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 70 },
  { title: '是否显示', dataIndex: 'visible', key: 'visible', width: 80 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

async function loadMenuTree() {
  loading.value = true
  try {
    const result = await request.get('/menu/tree')
    if (Array.isArray(result)) {
      menuTree.value = result
      // 构建 TreeSelect 数据
      menuTreeSelect.value = buildTreeSelect(result)
    }
  } catch (e) {
    message.error('加载菜单树失败')
  } finally {
    loading.value = false
  }
}

function buildTreeSelect(data: any[]): any[] {
  return data.map(item => ({
    id: item.id,
    menuName: item.menuName,
    children: item.children ? buildTreeSelect(item.children) : undefined
  }))
}

function handleExpandAll() { expandAll.value = !expandAll.value }
function handleRefresh() { loadMenuTree() }

function handleAdd() {
  isEditing.value = false
  editForm.parentId = undefined
  editForm.menuName = ''
  editForm.menuType = 1
  editForm.path = ''
  editForm.component = ''
  editForm.sort = 0
  editForm.status = 0
  editForm.visible = 0
  editForm.perms = ''
  editForm.icon = ''
  editVisible.value = true
}

function handleAddChild(record: any) {
  isEditing.value = false
  editForm.parentId = record.id
  editForm.menuName = ''
  editForm.menuType = 1
  editForm.path = ''
  editForm.component = ''
  editForm.sort = (record.sort || 0) + 1
  editForm.status = 0
  editForm.visible = 0
  editForm.perms = ''
  editForm.icon = ''
  editVisible.value = true
}

function handleEdit(record: any) {
  isEditing.value = true
  Object.assign(editForm, {
    parentId: record.parentId,
    menuName: record.menuName,
    menuType: record.menuType,
    path: record.path,
    component: record.component,
    sort: record.sort,
    status: record.status,
    visible: record.visible,
    perms: record.perms,
    icon: record.icon
  })
  editForm._id = record.id
  editVisible.value = true
}

async function handleSave() {
  if (!editForm.menuName) {
    message.warning('请输入菜单名称')
    return
  }
  saving.value = true
  try {
    if (isEditing.value) {
      await request.put(`/menu/${editForm._id}`, editForm)
    } else {
      await request.post('/menu', editForm)
    }
    message.success('保存成功')
    editVisible.value = false
    loadMenuTree()
  } catch (e) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除菜单 "${record.menuName}" 吗？`,
    okType: 'danger',
    onOk: async () => {
      await request.delete(`/menu/${record.id}`)
      message.success('删除成功')
      loadMenuTree()
    }
  })
}

onMounted(loadMenuTree)
</script>

<style scoped>
.content-card { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.toolbar { margin-bottom: 16px; display: flex; gap: 8px; }
</style>
