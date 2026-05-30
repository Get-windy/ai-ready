<template>
  <div class="position-management">
    <!-- 搜索区域 -->
    <a-card
      class="search-card"
      :bordered="false"
    >
      <a-form
        layout="inline"
        :model="searchForm"
        class="search-form"
      >
        <a-row
          :gutter="16"
          style="width: 100%"
        >
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item label="岗位名称">
              <a-input
                v-model:value="searchForm.positionName"
                placeholder="请输入岗位名称"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item label="岗位编码">
              <a-input
                v-model:value="searchForm.positionCode"
                placeholder="请输入岗位编码"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item label="岗位分类">
              <a-select
                v-model:value="searchForm.categoryId"
                placeholder="请选择岗位分类"
                allow-clear
                style="width: 100%"
              >
                <a-select-option
                  v-for="cat in categoryList"
                  :key="cat.id"
                  :value="cat.id"
                >
                  {{ cat.categoryName }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item>
              <a-space>
                <a-button
                  type="primary"
                  @click="handleSearch"
                >
                  <template #icon>
                    <SearchOutlined />
                  </template>
                  搜索
                </a-button>
                <a-button @click="handleReset">
                  <template #icon>
                    <ReloadOutlined />
                  </template>
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 表格区域 -->
    <a-card
      class="table-card"
      :bordered="false"
    >
      <template #title>
        <div class="table-header">
          <span class="title">岗位列表</span>
          <a-space>
            <a-button
              type="primary"
              :loading="submittingLoading"
              @click="handleAdd"
            >
              <template #icon>
                <PlusOutlined />
              </template>
              新增岗位
            </a-button>
            <a-button @click="handleCategoryManage">
              <template #icon>
                <AppstoreOutlined />
              </template>
              分类管理
            </a-button>
            <a-button
              danger
              :loading="batchDeleteLoading"
              :disabled="!selectedRowKeys.length || batchDeleteLoading"
              @click="handleBatchDelete"
            >
              <template #icon>
                <DeleteOutlined />
              </template>
              批量删除
            </a-button>
          </a-space>
        </div>
      </template>
      
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{ selectedRowKeys, onChange: onSelectChange }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'level'">
            <a-tag :color="getLevelColor(record.level)">
              {{ getLevelName(record.level) }}
            </a-tag>
          </template>
          
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 0 ? 'success' : 'error'">
              {{ record.status === 0 ? '正常' : '停用' }}
            </a-tag>
          </template>
          
          <template v-else-if="column.key === 'action'">
            <a-space>
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
                @click="handleAssignDepartment(record)"
              >
                部门关联
              </a-button>
              <a-dropdown>
                <a-button
                  type="link"
                  size="small"
                >
                  更多<DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleToggleStatus(record)">
                      <StopOutlined /> {{ record.status === 0 ? '停用' : '启用' }}
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item
                      danger
                      @click="handleDelete(record)"
                    >
                      <DeleteOutlined /> 删除
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 岗位表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="submittingLoading"
      width="600px"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="岗位名称"
          name="positionName"
        >
          <a-input
            v-model:value="formState.positionName"
            placeholder="请输入岗位名称"
          />
        </a-form-item>
        <a-form-item
          label="岗位编码"
          name="positionCode"
        >
          <a-input
            v-model:value="formState.positionCode"
            placeholder="请输入岗位编码"
            :disabled="isEdit"
          />
        </a-form-item>
        <a-form-item
          label="岗位分类"
          name="categoryId"
        >
          <a-select
            v-model:value="formState.categoryId"
            placeholder="请选择岗位分类"
            allow-clear
          >
            <a-select-option
              v-for="cat in categoryList"
              :key="cat.id"
              :value="cat.id"
            >
              {{ cat.categoryName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          label="岗位级别"
          name="level"
        >
          <a-select
            v-model:value="formState.level"
            placeholder="请选择岗位级别"
          >
            <a-select-option :value="1">
              初级
            </a-select-option>
            <a-select-option :value="2">
              中级
            </a-select-option>
            <a-select-option :value="3">
              高级
            </a-select-option>
            <a-select-option :value="4">
              专家
            </a-select-option>
            <a-select-option :value="5">
              首席
            </a-select-option>
          </a-select>
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
          label="描述"
          name="description"
        >
          <a-textarea
            v-model:value="formState.description"
            :rows="4"
            placeholder="请输入岗位描述"
          />
        </a-form-item>
        <a-form-item
          label="状态"
          name="status"
        >
          <a-radio-group v-model:value="formState.status">
            <a-radio :value="0">
              正常
            </a-radio>
            <a-radio :value="1">
              停用
            </a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分类管理弹窗 -->
    <a-modal
      v-model:open="categoryModalVisible"
      title="岗位分类管理"
      :footer="null"
      width="800px"
    >
      <div class="category-management">
        <div class="category-header">
          <a-button
            type="primary"
            size="small"
            @click="handleAddCategory"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            新增分类
          </a-button>
        </div>
        <a-table
          :columns="categoryColumns"
          :data-source="categoryData"
          :loading="categoryLoading"
          :pagination="{ pageSize: 10 }"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 0 ? 'success' : 'error'">
                {{ record.status === 0 ? '正常' : '停用' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button
                  type="link"
                  size="small"
                  @click="handleEditCategory(record)"
                >
                  编辑
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  danger
                  @click="handleDeleteCategory(record)"
                >
                  删除
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </a-modal>

    <!-- 分类表单弹窗 -->
    <a-modal
      v-model:open="categoryFormModalVisible"
      :title="categoryFormTitle"
      :confirm-loading="categoryFormModalLoading"
      @ok="handleCategoryFormModalOk"
      @cancel="handleCategoryFormModalCancel"
    >
      <a-form
        ref="categoryFormRef"
        :model="categoryFormState"
        :rules="categoryFormRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="分类名称"
          name="categoryName"
        >
          <a-input
            v-model:value="categoryFormState.categoryName"
            placeholder="请输入分类名称"
          />
        </a-form-item>
        <a-form-item
          label="分类编码"
          name="categoryCode"
        >
          <a-input
            v-model:value="categoryFormState.categoryCode"
            placeholder="请输入分类编码"
            :disabled="isCategoryEdit"
          />
        </a-form-item>
        <a-form-item
          label="排序"
          name="sort"
        >
          <a-input-number
            v-model:value="categoryFormState.sort"
            :min="0"
            :max="9999"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item
          label="描述"
          name="description"
        >
          <a-textarea
            v-model:value="categoryFormState.description"
            :rows="3"
            placeholder="请输入分类描述"
          />
        </a-form-item>
        <a-form-item
          label="状态"
          name="status"
        >
          <a-radio-group v-model:value="categoryFormState.status">
            <a-radio :value="0">
              正常
            </a-radio>
            <a-radio :value="1">
              停用
            </a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 部门关联弹窗 -->
    <a-modal
      v-model:open="departmentModalVisible"
      title="岗位与部门关联"
      :confirm-loading="departmentModalLoading"
      width="600px"
      @ok="handleDepartmentModalOk"
    >
      <div class="department-modal-content">
        <p>当前岗位: <strong>{{ currentPositionName }}</strong></p>
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="所属部门">
            <a-select
              v-model:value="targetDepartmentId"
              placeholder="请选择部门"
              allow-clear
            >
              <a-select-option
                v-for="dept in departmentList"
                :key="dept.id"
                :value="dept.id"
              >
                {{ dept.departmentName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
        <a-alert
          message="提示"
          description="选择部门后，该岗位将关联到对应部门。一个岗位可以关联多个部门。"
          type="info"
          show-icon
        />
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableProps, FormInstance } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  DeleteOutlined,
  DownOutlined,
  StopOutlined,
  AppstoreOutlined
} from '@ant-design/icons-vue'
import { positionApi, type PositionInfo, type PositionCategory, type PositionQuery } from '@/api/position'
import { departmentApi, type DepartmentInfo } from '@/api/department'
import { useSubmitLock } from '@/composables'

// 搜索表单
const searchForm = reactive<PositionQuery>({
  positionCode: '',
  positionName: '',
  categoryId: undefined,
  departmentId: undefined,
  level: undefined,
  status: undefined
})

// 表格数据
const tableData = ref<PositionInfo[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 表格列定义
const columns: TableProps['columns'] = [
  { title: '岗位编码', dataIndex: 'positionCode', width: 150 },
  { title: '岗位名称', dataIndex: 'positionName', width: 150 },
  { title: '岗位分类', dataIndex: 'categoryName', width: 120 },
  { title: '所属部门', dataIndex: 'departmentName', width: 150 },
  { title: '岗位级别', key: 'level', width: 100 },
  { title: '排序', dataIndex: 'sort', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]

// 弹窗相关
const modalVisible = ref(false)
const { isSubmitting: submittingLoading, withSubmitLock } = useSubmitLock()
const { isSubmitting: batchDeleteLoading } = useSubmitLock()
const modalTitle = computed(() => isEdit.value ? '编辑岗位' : '新增岗位')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive<Partial<PositionInfo>>({
  id: 0,
  positionCode: '',
  positionName: '',
  categoryId: undefined,
  departmentId: undefined,
  level: 2,
  sort: 0,
  description: '',
  status: 0
})

const formRules = {
  positionName: { required: true, message: '请输入岗位名称', trigger: 'blur' },
  positionCode: { required: true, message: '请输入岗位编码', trigger: 'blur' },
  level: { required: true, message: '请选择岗位级别', trigger: 'change' },
}

// 分类管理相关
const categoryModalVisible = ref(false)
const categoryLoading = ref(false)
const categoryData = ref<PositionCategory[]>([])
const categoryColumns: TableProps['columns'] = [
  { title: '分类编码', dataIndex: 'categoryCode', width: 150 },
  { title: '分类名称', dataIndex: 'categoryName', width: 150 },
  { title: '排序', dataIndex: 'sort', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

const categoryFormModalVisible = ref(false)
const { isSubmitting: categoryFormModalLoading, withSubmitLock: withCategoryFormSubmitLock } = useSubmitLock()
const categoryFormTitle = computed(() => isCategoryEdit.value ? '编辑分类' : '新增分类')
const isCategoryEdit = ref(false)
const categoryFormRef = ref<FormInstance>()

const categoryFormState = reactive<Partial<PositionCategory>>({
  id: 0,
  categoryCode: '',
  categoryName: '',
  sort: 0,
  description: '',
  status: 0
})

const categoryFormRules = {
  categoryName: { required: true, message: '请输入分类名称', trigger: 'blur' },
  categoryCode: { required: true, message: '请输入分类编码', trigger: 'blur' },
}

// 分类列表（用于下拉选择）
const categoryList = ref<PositionCategory[]>([])

// 部门关联相关
const departmentModalVisible = ref(false)
const { isSubmitting: departmentModalLoading, withSubmitLock: withDepartmentSubmitLock } = useSubmitLock()
const departmentList = ref<DepartmentInfo[]>([])
const targetDepartmentId = ref<number>()
const currentPositionId = ref(0)
const currentPositionName = ref('')

// 数据加载
const fetchData = async () => {
  loading.value = true
  try {
    const res = await positionApi.getPage({
      tenantId: 1,
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 加载岗位分类列表
const fetchCategoryList = async () => {
  try {
    const res = await positionApi.getCategoryList({ tenantId: 1, status: 0 })
    if (res.data) {
      categoryList.value = res.data
    }
  } catch (error) {
    console.error('加载岗位分类失败:', error)
  }
}

// 加载部门列表
const fetchDepartmentList = async () => {
  try {
    const res = await departmentApi.getList({ tenantId: 1, status: 0 })
    if (res.data) {
      departmentList.value = res.data
    }
  } catch (error) {
    console.error('加载部门列表失败:', error)
  }
}

// 搜索相关
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    positionCode: '',
    positionName: '',
    categoryId: undefined,
    departmentId: undefined,
    level: undefined,
    status: undefined
  })
  handleSearch()
}

// 表格操作
const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  fetchData()
}

const onSelectChange = (keys: (string | number)[]) => {
  selectedRowKeys.value = keys
}

// 新增岗位
const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, {
    id: 0,
    positionCode: '',
    positionName: '',
    categoryId: undefined,
    departmentId: undefined,
    level: 2,
    sort: 0,
    description: '',
    status: 0
  })
  modalVisible.value = true
}

// 编辑岗位
const handleEdit = (record: PositionInfo) => {
  isEdit.value = true
  Object.assign(formState, record)
  modalVisible.value = true
}

// 提交表单
const handleModalOk = async () => {
  try {
    const result = await withSubmitLock(async () => {
      await formRef.value?.validate()

      if (isEdit.value) {
        await positionApi.update(formState.id!, formState)
        message.success('更新成功')
      } else {
        await positionApi.create(formState)
        message.success('创建成功')
      }

      modalVisible.value = false
      fetchData()
    })
    void result
  } catch (error: any) {
    if (error) {
      message.error(error?.message || '操作失败')
    }
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// 删除岗位
const handleDelete = (record: PositionInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除岗位 "${record.positionName}" 吗？`,
    async onOk() {
      await positionApi.delete(record.id)
      message.success('删除成功')
      fetchData()
    }
  })
}

// 批量删除
const handleBatchDelete = () => {
  if (batchDeleteLoading.value) return
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个岗位吗？`,
    async onOk() {
      batchDeleteLoading.value = true
      try {
        await positionApi.batchDelete(selectedRowKeys.value)
        message.success('删除成功')
        selectedRowKeys.value = []
        fetchData()
      } catch (error: any) {
        message.error(error?.message || '删除失败')
      } finally {
        batchDeleteLoading.value = false
      }
    }
  })
}

// 切换状态
const handleToggleStatus = async (record: PositionInfo) => {
  const newStatus = record.status === 0 ? 1 : 0
  await positionApi.updateStatus(record.id, newStatus)
  message.success('状态更新成功')
  fetchData()
}

// 分类管理
const handleCategoryManage = () => {
  categoryModalVisible.value = true
  fetchCategoryData()
}

const fetchCategoryData = async () => {
  categoryLoading.value = true
  try {
    const res = await positionApi.getCategoryPage({ tenantId: 1, size: 100 })
    if (res.data) {
      categoryData.value = res.data.records
    }
  } catch (error) {
    message.error('加载分类数据失败')
  } finally {
    categoryLoading.value = false
  }
}

const handleAddCategory = () => {
  isCategoryEdit.value = false
  Object.assign(categoryFormState, {
    id: 0,
    categoryCode: '',
    categoryName: '',
    sort: 0,
    description: '',
    status: 0
  })
  categoryFormModalVisible.value = true
}

const handleEditCategory = (record: PositionCategory) => {
  isCategoryEdit.value = true
  Object.assign(categoryFormState, record)
  categoryFormModalVisible.value = true
}

const handleCategoryFormModalOk = async () => {
  try {
    const result = await withCategoryFormSubmitLock(async () => {
      await categoryFormRef.value?.validate()

      if (isCategoryEdit.value) {
        await positionApi.updateCategory(categoryFormState.id!, categoryFormState)
        message.success('更新成功')
      } else {
        await positionApi.createCategory(categoryFormState)
        message.success('创建成功')
      }

      categoryFormModalVisible.value = false
      fetchCategoryData()
      fetchCategoryList()
    })
    void result
  } catch (error: any) {
    if (error) {
      message.error(error?.message || '操作失败')
    }
  }
}

const handleCategoryFormModalCancel = () => {
  categoryFormModalVisible.value = false
  categoryFormRef.value?.resetFields()
}

const handleDeleteCategory = (record: PositionCategory) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除分类 "${record.categoryName}" 吗？`,
    async onOk() {
      await positionApi.deleteCategory(record.id)
      message.success('删除成功')
      fetchCategoryData()
      fetchCategoryList()
    }
  })
}

// 部门关联
const handleAssignDepartment = (record: PositionInfo) => {
  currentPositionId.value = record.id
  currentPositionName.value = record.positionName
  targetDepartmentId.value = record.departmentId
  departmentModalVisible.value = true
}

const handleDepartmentModalOk = async () => {
  try {
    const result = await withDepartmentSubmitLock(async () => {
      await positionApi.update(currentPositionId.value, { departmentId: targetDepartmentId.value })
      message.success('部门关联成功')
      departmentModalVisible.value = false
      fetchData()
    })
    void result
  } catch (error: any) {
    if (error) {
      message.error(error?.message || '操作失败')
    }
  }
}

// 辅助函数
const getLevelColor = (level: number) => {
  const colors: Record<number, string> = { 1: 'green', 2: 'blue', 3: 'orange', 4: 'red', 5: 'purple' }
  return colors[level] || 'default'
}

const getLevelName = (level: number) => {
  const names: Record<number, string> = { 1: '初级', 2: '中级', 3: '高级', 4: '专家', 5: '首席' }
  return names[level] || '未知'
}

onMounted(() => {
  fetchData()
  fetchCategoryList()
  fetchDepartmentList()
})
</script>

<style scoped>
.position-management {
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

.category-management {
  padding: 16px 0;
}

.category-header {
  margin-bottom: 16px;
}

.department-modal-content {
  padding: 16px 0;
}

.department-modal-content p {
  margin-bottom: 16px;
  font-size: 14px;
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
