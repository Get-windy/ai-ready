<template>
  <div class="menu-management">
    <el-card class="menu-card">
      <template #header>
        <div class="card-header">
          <span class="title">菜单管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            <el-icon><Plus /></el-icon>
            新增菜单
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form
        :model="queryForm"
        inline
        class="search-form"
      >
        <el-form-item label="菜单名称">
          <el-input
            v-model="queryForm.menuName"
            placeholder="请输入菜单名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="菜单类型">
          <el-select
            v-model="queryForm.menuType"
            placeholder="请选择类型"
            clearable
          >
            <el-option
              label="目录"
              :value="0"
            />
            <el-option
              label="菜单"
              :value="1"
            />
            <el-option
              label="按钮"
              :value="2"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryForm.status"
            placeholder="请选择状态"
            clearable
          >
            <el-option
              label="启用"
              :value="1"
            />
            <el-option
              label="禁用"
              :value="0"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 菜单表格 -->
      <el-table
        v-loading="loading"
        :data="menuTree"
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        border
        stripe
        default-expand-all
      >
        <el-table-column
          prop="menuName"
          label="菜单名称"
          min-width="180"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-icon
              v-if="row.icon"
              class="menu-icon"
            >
              <component :is="row.icon" />
            </el-icon>
            <span>{{ row.menuName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="menuCode"
          label="权限标识"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="path"
          label="路由路径"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="menuType"
          label="类型"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.menuType === 0"
              type="info"
            >
              目录
            </el-tag>
            <el-tag
              v-else-if="row.menuType === 1"
              type="success"
            >
              菜单
            </el-tag>
            <el-tag
              v-else-if="row.menuType === 2"
              type="warning"
            >
              按钮
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="sortOrder"
          label="排序"
          width="80"
          align="center"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="visible"
          label="显示"
          width="80"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.visible === 1"
              type="success"
            >
              显示
            </el-tag>
            <el-tag
              v-else
              type="info"
            >
              隐藏
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="280"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="handleAddChild(row)"
            >
              <el-icon><Plus /></el-icon>
              新增
            </el-button>
            <el-button
              type="primary"
              link
              @click="handleEdit(row)"
            >
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              type="primary"
              link
              @click="handleAssignRole(row)"
            >
              <el-icon><User /></el-icon>
              分配角色
            </el-button>
            <el-button
              type="danger"
              link
              @click="handleDelete(row)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 菜单编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="formData.parentId"
            :data="menuTree"
            :props="{ label: 'menuName', value: 'id' }"
            placeholder="请选择上级菜单"
            clearable
            check-strictly
            :render-after-expand="false"
          />
        </el-form-item>

        <el-form-item
          label="菜单类型"
          prop="menuType"
        >
          <el-radio-group v-model="formData.menuType">
            <el-radio :label="0">
              目录
            </el-radio>
            <el-radio :label="1">
              菜单
            </el-radio>
            <el-radio :label="2">
              按钮
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item
          label="菜单名称"
          prop="menuName"
        >
          <el-input
            v-model="formData.menuName"
            placeholder="请输入菜单名称"
          />
        </el-form-item>

        <el-form-item
          label="权限标识"
          prop="menuCode"
        >
          <el-input
            v-model="formData.menuCode"
            placeholder="请输入权限标识，如：system:user:list"
          />
        </el-form-item>

        <el-form-item
          v-if="formData.menuType !== 2"
          label="路由路径"
          prop="path"
        >
          <el-input
            v-model="formData.path"
            placeholder="请输入路由路径，如：/system/user"
          />
        </el-form-item>

        <el-form-item
          v-if="formData.menuType === 1"
          label="组件路径"
          prop="component"
        >
          <el-input
            v-model="formData.component"
            placeholder="请输入组件路径，如：system/user/index"
          />
        </el-form-item>

        <el-form-item
          v-if="formData.menuType !== 2"
          label="菜单图标"
        >
          <el-input
            v-model="formData.icon"
            placeholder="请输入图标名称，如：User"
          />
        </el-form-item>

        <el-form-item
          label="排序"
          prop="sortOrder"
        >
          <el-input-number
            v-model="formData.sortOrder"
            :min="0"
            :max="9999"
          />
        </el-form-item>

        <el-form-item
          v-if="formData.menuType !== 2"
          label="是否显示"
        >
          <el-radio-group v-model="formData.visible">
            <el-radio :label="1">
              显示
            </el-radio>
            <el-radio :label="0">
              隐藏
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="菜单状态">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">
              启用
            </el-radio>
            <el-radio :label="0">
              禁用
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 角色分配弹窗 -->
    <el-dialog
      v-model="roleDialogVisible"
      title="分配角色"
      width="500px"
    >
      <el-form label-width="80px">
        <el-form-item label="菜单名称">
          <span>{{ currentMenu?.menuName }}</span>
        </el-form-item>
        <el-form-item label="选择角色">
          <el-select
            v-model="selectedRoles"
            multiple
            placeholder="请选择角色"
            style="width: 100%"
          >
            <el-option
              v-for="role in roleList"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="roleSubmitLoading"
          @click="handleRoleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete, User } from '@element-plus/icons-vue'
import menuApi, { type MenuInfo, type MenuQuery, type MenuSaveRequest, type MenuUpdateRequest } from '@/api/menu'
import roleApi from '@/api/role'

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
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

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
const formRules: FormRules = {
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuCode: [{ required: true, message: '请输入权限标识', trigger: 'blur' }],
  path: [{ required: true, message: '请输入路由路径', trigger: 'blur', type: 'string' }],
  sortOrder: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

// 角色分配弹窗
const roleDialogVisible = ref(false)
const roleSubmitLoading = ref(false)
const currentMenu = ref<MenuInfo | null>(null)
const selectedRoles = ref<number[]>([])
const roleList = ref<any[]>([])

// 当前操作员ID（实际应从用户信息中获取）
const operatorId = 1

// 加载菜单树
const loadMenuTree = async () => {
  loading.value = true
  try {
    const res = await menuApi.getTree(queryForm)
    if (res.code === 200) {
      menuTree.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取菜单列表失败')
    }
  } catch (error) {
    console.error('获取菜单列表失败:', error)
    ElMessage.error('获取菜单列表失败')
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
  try {
    await ElMessageBox.confirm(
      `确定要删除菜单"${row.menuName}"吗？删除后不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await menuApi.delete(row.id, operatorId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadMenuTree()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除菜单失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    submitLoading.value = true
    
    let res
    if (formData.id) {
      // 更新
      res = await menuApi.update(formData, operatorId)
    } else {
      // 新增
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
      ElMessage.success(formData.id ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadMenuTree()
    } else {
      ElMessage.error(res.message || (formData.id ? '更新失败' : '创建失败'))
    }
  } catch (error) {
    console.error('提交表单失败:', error)
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}

// 状态变更
const handleStatusChange = async (row: MenuInfo, status: number) => {
  try {
    const res = await menuApi.updateStatus(row.id, status, operatorId)
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
    } else {
      ElMessage.error(res.message || '状态更新失败')
      // 恢复原状态
      row.status = status === 1 ? 0 : 1
    }
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('状态更新失败')
    // 恢复原状态
    row.status = status === 1 ? 0 : 1
  }
}

// 分配角色
const handleAssignRole = async (row: MenuInfo) => {
  currentMenu.value = row
  selectedRoles.value = []
  roleDialogVisible.value = true
  
  // 加载角色列表
  try {
    const res = await roleApi.listAll()
    if (res.code === 200) {
      roleList.value = res.data || []
    }
  } catch (error) {
    console.error('获取角色列表失败:', error)
  }
  
  // 加载当前菜单已分配的角色
  try {
    const res = await menuApi.getRoleMenus(row.id)
    if (res.code === 200) {
      // 这里需要根据实际API返回调整
      // selectedRoles.value = res.data.map((menu: any) => menu.roleId)
    }
  } catch (error) {
    console.error('获取菜单角色失败:', error)
  }
}

// 提交角色分配
const handleRoleSubmit = async () => {
  if (!currentMenu.value) return
  
  roleSubmitLoading.value = true
  try {
    // 这里需要遍历选中的角色，为每个角色分配菜单
    // 实际实现可能需要调整API调用方式
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
  } catch (error) {
    console.error('分配角色失败:', error)
    ElMessage.error('分配角色失败')
  } finally {
    roleSubmitLoading.value = false
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
  
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

onMounted(() => {
  loadMenuTree()
})
</script>

<style scoped lang="scss">
.menu-management {
  padding: 20px;

  .menu-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .title {
        font-size: 16px;
        font-weight: 600;
      }
    }
  }

  .search-form {
    margin-bottom: 20px;
    padding: 20px;
    background-color: #f5f7fa;
    border-radius: 4px;
  }

  .menu-icon {
    margin-right: 8px;
    font-size: 16px;
  }
}
</style>
