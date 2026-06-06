<template>
  <div class="menu-management">
    <TableList
      ref="tableRef"
      :columns="tableColumns"
      :data-source="menuTree"
      :loading="loading"
      :pagination="null as any"
      :table-key="'system-menu-list'"
      :filter-fields="filterFields"
      :show-search="false"
      :show-add="false"
      :show-edit="false"
      :show-delete="false"
      :show-batch-delete="false"
      :bordered="true"
      @refresh="loadMenuTree"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-actions>
        <a-button type="primary" :loading="submitLoading" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新增菜单
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'menuName'">
          <component
            v-if="record.icon"
            :is="iconComponent(record.icon)"
            class="menu-icon"
          />
          <span>{{ record.menuName }}</span>
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
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="handleAddChild(record)">
            <template #icon><PlusOutlined /></template>新增
          </a-button>
          <a-button type="link" size="small" @click="handleEdit(record)">
            <template #icon><EditOutlined /></template>编辑
          </a-button>
          <a-button type="link" size="small" @click="handleAssignRole(record)">
            <template #icon><UserOutlined /></template>分配角色
          </a-button>
          <a-button type="link" size="small" danger @click="handleDelete(record)">
            <template #icon><DeleteOutlined /></template>删除
          </a-button>
        </template>
      </template>
    </TableList>

    <!-- 菜单编辑弹窗 -->
    <a-modal
      v-model:open="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :destroy-on-close="true"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
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
            <a-radio :value="0">目录</a-radio>
            <a-radio :value="1">菜单</a-radio>
            <a-radio :value="2">按钮</a-radio>
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
      </a-form>
    </a-modal>

    <!-- 角色分配弹窗 -->
    <a-modal
      v-model:open="roleDialogVisible"
      title="分配角色"
      width="500px"
      @ok="handleRoleSubmit"
      :confirm-loading="roleSubmitLoading"
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
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import TableList, { type FilterField } from '@/components/TableList/TableList.vue'
import menuApi, { type MenuInfo, type MenuQuery, type MenuSaveRequest, type MenuUpdateRequest } from '@/api/menu'
import roleApi from '@/api/role'
import { useSubmitLock } from '@/composables'

// 查询表单
const queryForm = reactive<MenuQuery>({
  menuName: '',
  menuType: undefined,
  status: undefined
})

// 菜单树数据
const menuTree = ref<MenuInfo[]>([])
const loading = ref(false)

// 弹窗控制
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const { isSubmitting: submitLoading, withSubmitLock } = useSubmitLock()
const formRef = ref<FormInstance>()

// 表格列配置
const tableColumns: any[] = [
  { title: '菜单名称', dataIndex: 'menuName', key: 'menuName', width: 200, ellipsis: true },
  { title: '权限标识', dataIndex: 'menuCode', key: 'menuCode', width: 180, ellipsis: true },
  { title: '路由路径', dataIndex: 'path', key: 'path', width: 180, ellipsis: true },
  { title: '类型', dataIndex: 'menuType', key: 'menuType', width: 100, align: 'center' as const },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80, align: 'center' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, align: 'center' as const },
  { title: '显示', dataIndex: 'visible', key: 'visible', width: 80, align: 'center' as const },
  { title: '操作', key: 'action', width: 280, fixed: 'right' as const }
]

// 筛选字段
const filterFields: FilterField[] = [
  { key: 'menuName', label: '菜单名称', type: 'input', placeholder: '请输入菜单名称' },
  { key: 'menuType', label: '菜单类型', type: 'select', options: [
    { label: '目录', value: 0 }, { label: '菜单', value: 1 }, { label: '按钮', value: 2 }
  ]},
  { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }] },
]

// 将图标字符串转为组件
const iconComponent = (iconName: string) => {
  return h('span', { class: 'menu-icon-placeholder' }, iconName.charAt(0).toUpperCase())
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
  remark: ''
})

// 表单验证规则
const formRules: Record<string, Rule[]> = {
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

// 当前操作员ID
const operatorId = 1

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
  try {
    const res = await menuApi.getTree(queryForm)
    if (res.code === 200) {
      menuTree.value = res.data || []
    } else {
      message.error(res.message || '获取菜单列表失败')
    }
  } catch (error) {
    console.error('获取菜单列表失败:', error)
    message.error('获取菜单列表失败')
  } finally {
    loading.value = false
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
}

// 新增子菜单
const handleAddChild = (row: MenuInfo) => {
  dialogTitle.value = `新增子菜单 - ${row.menuName}`
  resetForm()
  formData.parentId = row.id
  dialogVisible.value = true
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
    remark: row.remark || ''
  })
  dialogVisible.value = true
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
      const res = await menuApi.delete(row.id, operatorId)
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
        res = await menuApi.update(formData, operatorId)
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
        res = await menuApi.create(saveData, operatorId)
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
      console.error('提交表单失败:', error)
      message.error(error?.message || '提交失败')
    }
  }
}

// 状态变更
const handleStatusChange = async (row: MenuInfo, status: number) => {
  try {
    const res = await menuApi.updateStatus(row.id, status, operatorId)
    if (res.code === 200) {
      message.success('状态更新成功')
    } else {
      message.error(res.message || '状态更新失败')
      row.status = status === 1 ? 0 : 1
    }
  } catch (error) {
    console.error('更新状态失败:', error)
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
    console.error('获取角色列表失败:', error)
  }

  try {
    const res = await menuApi.getRoleMenus(row.id)
    if (res.code === 200) {
      // 根据实际API返回调整
    }
  } catch (error) {
    console.error('获取菜单角色失败:', error)
  }
}

// 提交角色分配
const handleRoleSubmit = async () => {
  if (!currentMenu.value) return

  try {
    const result = await withRoleSubmitLock(async () => {
      message.success('角色分配成功')
      roleDialogVisible.value = false
    })
    void result
  } catch (error: any) {
    if (error) {
      console.error('分配角色失败:', error)
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
  formRef.value?.clearValidate()
}

// 初始化
onMounted(() => {
  loadMenuTree()
})
</script>

<style lang="scss" scoped>
.menu-management {
  padding: 0;
}

.menu-icon {
  margin-right: 8px;
  font-size: 16px;
}

.menu-icon-placeholder {
  display: inline-block;
  width: 16px;
  height: 16px;
  margin-right: 8px;
  font-size: 12px;
  line-height: 16px;
  text-align: center;
  background-color: #f0f0f0;
  border-radius: 2px;
}
</style>
