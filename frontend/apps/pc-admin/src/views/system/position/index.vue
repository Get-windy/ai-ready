<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="position-page-header">
        <div class="position-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>岗位管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="position-page-header-title">岗位管理</h2>
        </div>
        <div class="position-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl</kbd> + <kbd>N</kbd> 新增</span>
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)()">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="position-management">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">岗位总数</div>
          </div>
          <SolutionOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-active">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ activeCount }}</div>
            <div class="stat-card-label">正常岗位</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-disabled">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ disabledCount }}</div>
            <div class="stat-card-label">停用岗位</div>
          </div>
          <StopOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-categories">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ categoryList.length }}</div>
            <div class="stat-card-label">分类数量</div>
          </div>
          <AppstoreOutlined class="stat-card-icon" />
        </div>
      </div>

      <a-skeleton active v-if="loading && tableData.length === 0" :paragraph="{ rows: 8 }" style="padding: 24px;" />

      <VxeTableList
        ref="tableRef"
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :table-key="'system-position-list'"
        :min-empty-rows="12"
        :filter-fields="filterFields"
        :show-search="false"
        :show-toolbar="false"
        :selectable="true"
        :show-add="false"
        :show-export="false"
        :show-batch-delete="false"
        add-text="新增岗位"
        add-permission="position:create"
        edit-permission="position:edit"
        delete-permission="position:delete"
        @add="handleAdd"
        @edit="handleEdit"
        @delete="handleDelete"
        @batch-delete="handleBatchDelete"
        @refresh="debounceClick('refresh', fetchData)"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="(keys: any) => { (selectedRowKeys as any) = keys }"
        @cell-dblclick="handleView"
      >
        <template #toolbar-actions>
          <a-button @click="handleCategoryManage">
            <template #icon><AppstoreOutlined /></template>
            分类管理
          </a-button>
        </template>

        <template #empty>
          <a-empty v-if="!hasError" description="暂无数据" />
          <a-result v-else status="error" title="数据加载失败">
            <template #extra>
              <a-button type="primary" @click="debounceClick('refresh', fetchData)()">
                <template #icon><ReloadOutlined /></template>
                重新加载
              </a-button>
            </template>
          </a-result>
        </template>

        <template #levelCell="{ record }">
          <a-tag :color="getLevelColor(record.level)">
            {{ getLevelName(record.level) }}
          </a-tag>
        </template>

        <template #statusCell="{ record }">
          <a-tag :color="record.status === 0 ? 'success' : 'error'">
            {{ record.status === 0 ? '正常' : '停用' }}
          </a-tag>
        </template>

        <template #actionCell="{ record }">
          <a-space>
            <a-button
              type="link"
              size="small"
              v-permission="'position:edit'"
              @click="handleEdit(record)"
            >
              编辑
            </a-button>
            <a-button
              type="link"
              size="small"
              v-permission="'position:edit'"
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
                  <a-menu-item @click="handleToggleStatus(record)" v-permission="'position:edit'">
                    <StopOutlined /> {{ record.status === 0 ? '停用' : '启用' }}
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item
                    danger
                    @click="handleDelete(record)"
                    v-permission="'position:delete'"
                  >
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 岗位表单弹窗 -->
      <FullScreenDetail
        :visible="modalVisible"
        :title="modalTitle"
        :dirty="formDirty"
        :save-loading="submittingLoading"
        :show-save-and-new="!isEdit"
        @save="handleModalOk"
        @close="handleFormClose"
        @save-and-new="handleFormSaveAndNew"
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
              size="small"
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
              size="small"
            >
              <a-select-option
                v-for="opt in levelOptions"
                :key="opt.value"
                :value="opt.value"
              >{{ opt.label }}</a-select-option>
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
      </FullScreenDetail>

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
          <VxeTableList
            :data-source="categoryData"
            :loading="categoryLoading"
            :pagination="{ pageSize: 10, current: 1, total: 0 } as any"
            row-key="id"
            :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false"
            :show-export="false" :show-batch-delete="false"
            :columns="categoryColumns"
          >
            <template #statusCell="{ record }">
              <a-tag :color="record.status === 0 ? 'success' : 'error'">
                {{ record.status === 0 ? '正常' : '停用' }}
              </a-tag>
            </template>
            <template #actionCell="{ record }">
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
          </VxeTableList>
        </div>
      </a-modal>

      <!-- 分类表单弹窗 -->
      <FullScreenDetail
        :visible="categoryFormModalVisible"
        :title="categoryFormTitle"
        :dirty="categoryFormDirty"
        :save-loading="categoryFormModalLoading"
        :show-save-and-new="!isCategoryEdit"
        @save="handleCategoryFormModalOk"
        @close="handleCategoryFormClose"
        @save-and-new="handleCategoryFormSaveAndNew"
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
      </FullScreenDetail>

      <!-- 部门关联弹窗 -->
      <FullScreenDetail
        :visible="departmentModalVisible"
        title="岗位与部门关联"
        :save-loading="departmentModalLoading"
        @save="handleDepartmentModalOk"
        @close="departmentModalVisible = false"
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
import {
  DownOutlined,
  StopOutlined,
  DeleteOutlined,
  AppstoreOutlined,
  PlusOutlined,
  SolutionOutlined,
  CheckCircleOutlined,
  SyncOutlined,
  ReloadOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { positionApi, type PositionInfo, type PositionCategory, type PositionQuery } from '@/api/position'
import { departmentApi, type DepartmentInfo } from '@/api/department'
import { useSubmitLock } from '@/composables'
import { useUserStore } from '@/stores/user'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { dictItemApi } from '@/api/dict'

// ── 岗位级别选项（从API加载） ──────────────────────────
const levelOptions = ref<{ label: string; value: number }[]>([])

async function loadLevelOptions() {
  try {
    const res = await dictItemApi.getByDictCode('POSITION_LEVEL')
    if (res.data) {
      levelOptions.value = res.data
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map(item => ({ label: item.itemText, value: Number(item.itemValue) }))
    }
  } catch (err) {
    console.warn('[岗位管理] 加载岗位级别失败', err)
  }
}

// 搜索表单
const userStore = useUserStore()
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

// ── 统计数据 ────────────────────────────────────────────
const activeCount = computed(() => tableData.value.filter(r => r.status === 0).length)
const disabledCount = computed(() => tableData.value.filter(r => r.status === 1).length)

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
const columns: any[] = [
  { title: '岗位编码', field: 'positionCode', width: 150 },
  { title: '岗位名称', field: 'positionName', width: 150 },
  { title: '岗位分类', field: 'categoryName', width: 120 },
  { title: '所属部门', field: 'departmentName', width: 150 },
  { title: '岗位级别', field: 'level', width: 100, slotName: 'levelCell' },
  { title: '排序', field: 'sort', width: 80 },
  { title: '状态', field: 'status', width: 80, slotName: 'statusCell' },
  { title: '描述', field: 'description', ellipsis: true },
  { title: '创建时间', field: 'createTime', width: 160 },
  { title: '操作', field: 'action', width: 180, fixed: 'right', slotName: 'actionCell' }
]

// 筛选字段
const categoryList = ref<PositionCategory[]>([])

const filterFields = computed<FilterField[]>(() => [
  { key: 'positionName', label: '岗位名称', type: 'input', placeholder: '请输入岗位名称' },
  { key: 'positionCode', label: '岗位编码', type: 'input', placeholder: '请输入岗位编码' },
  { key: 'categoryId', label: '岗位分类', type: 'select', options: categoryList.value.map(c => ({ label: c.categoryName, value: c.id })) },
])

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

// ── 表单脏检测 ──────────────────────────────────────────
const initialFormSnapshot = ref('')
let watchReady = false
const formDirty = computed(() => {
  if (!watchReady) return false
  return JSON.stringify(formState) !== initialFormSnapshot.value
})
function saveFormSnapshot() { initialFormSnapshot.value = JSON.stringify(formState) }

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

const formRules: any = {
  positionName: { required: true, message: '请输入岗位名称', trigger: 'blur' },
  positionCode: { required: true, message: '请输入岗位编码', trigger: 'blur' },
  level: { required: true, message: '请选择岗位级别', trigger: 'change' },
}

// 分类管理相关
const categoryModalVisible = ref(false)
const categoryLoading = ref(false)
const categoryData = ref<PositionCategory[]>([])
const categoryColumns: any[] = [
  { title: '分类编码', field: 'categoryCode', width: 150 },
  { title: '分类名称', field: 'categoryName', width: 150 },
  { title: '排序', field: 'sort', width: 80 },
  { title: '状态', field: 'status', width: 80, slotName: 'statusCell' },
  { title: '描述', field: 'description', ellipsis: true },
  { title: '创建时间', field: 'createTime', width: 160 },
  { title: '操作', field: 'action', width: 120, fixed: 'right', slotName: 'actionCell' }
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

// ── 分类表单脏检测 ──────────────────────────────────────
const initialCategoryFormSnapshot = ref('')
let watchReadyCategory = false
const categoryFormDirty = computed(() => {
  if (!watchReadyCategory) return false
  return JSON.stringify(categoryFormState) !== initialCategoryFormSnapshot.value
})
function saveCategoryFormSnapshot() { initialCategoryFormSnapshot.value = JSON.stringify(categoryFormState) }

const categoryFormRules: any = {
  categoryName: { required: true, message: '请输入分类名称', trigger: 'blur' },
  categoryCode: { required: true, message: '请输入分类编码', trigger: 'blur' },
}

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
  hasError.value = false
  try {
    const res = await positionApi.getPage({
      tenantId: userStore.tenantId,
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      tableData.value = res.records
      pagination.total = res.total
    }
  } catch (error) {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[岗位管理] 加载岗位数据失败')
    message.error('加载数据失败')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

// 加载岗位分类列表
const fetchCategoryList = async () => {
  try {
    const res = await positionApi.getCategoryList({ tenantId: userStore.tenantId, status: 0 })
    if (res.data) {
      categoryList.value = res.data
    }
  } catch (error) {
    console.warn('[岗位管理] 加载岗位分类失败')
  }
}

// 加载部门列表
const fetchDepartmentList = async () => {
  try {
    const res = await departmentApi.getList({ tenantId: userStore.tenantId, status: 0 })
    if (res.data) {
      departmentList.value = res.data
    }
  } catch (error) {
    console.warn('[岗位管理] 加载部门列表失败')
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

// 筛选变化
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, {
      positionCode: '',
      positionName: '',
      categoryId: undefined,
      departmentId: undefined,
      level: undefined,
      status: undefined
    })
  } else {
    Object.assign(searchForm, filters)
  }
  pagination.current = 1
  fetchData()
}

// 分页变化
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
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
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 编辑岗位
const handleEdit = (record: PositionInfo) => {
  isEdit.value = true
  Object.assign(formState, record)
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
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

const handleFormClose = () => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '您有未保存的修改，确定要关闭吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => { modalVisible.value = false; formRef.value?.resetFields() },
    })
  } else {
    modalVisible.value = false
    formRef.value?.resetFields()
  }
}

const handleFormSaveAndNew = () => {
  handleModalOk()
}

// 删除岗位
const handleDelete = (record: PositionInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除岗位 "${record.positionName}" 吗？`,
    async onOk() {
      try {
        await positionApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (err) {
        console.warn('[系统管理] 删除岗位失败', err)
        message.error('删除失败')
      }
    }
  })
}

// 批量删除
const handleBatchDelete = (deleteKeys?: number[]) => {
  if (batchDeleteLoading.value) return
  const ids = deleteKeys || selectedRowKeys.value
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${ids.length} 个岗位吗？`,
    async onOk() {
      batchDeleteLoading.value = true
      try {
        await positionApi.batchDelete(ids)
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
  try {
    await positionApi.updateStatus(record.id, newStatus)
    message.success('状态更新成功')
    fetchData()
  } catch (err) {
    console.warn('[系统管理] 更新岗位状态失败', err)
    message.error('状态更新失败')
  }
}

// 分类管理
const handleCategoryManage = () => {
  categoryModalVisible.value = true
  fetchCategoryData()
}

const fetchCategoryData = async () => {
  categoryLoading.value = true
  try {
    const res = await positionApi.getCategoryPage({ tenantId: userStore.tenantId, size: 100 } as any)
    if (res.data) {
      categoryData.value = res.records
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
  nextTick(() => { saveCategoryFormSnapshot(); watchReadyCategory = true })
}

const handleEditCategory = (record: PositionCategory) => {
  isCategoryEdit.value = true
  Object.assign(categoryFormState, record)
  categoryFormModalVisible.value = true
  nextTick(() => { saveCategoryFormSnapshot(); watchReadyCategory = true })
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

const handleCategoryFormClose = () => {
  if (categoryFormDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '您有未保存的修改，确定要关闭吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => { categoryFormModalVisible.value = false; categoryFormRef.value?.resetFields() },
    })
  } else {
    categoryFormModalVisible.value = false
    categoryFormRef.value?.resetFields()
  }
}

const handleCategoryFormSaveAndNew = () => {
  handleCategoryFormModalOk()
}

const handleDeleteCategory = (record: PositionCategory) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除分类 "${record.categoryName}" 吗？`,
    async onOk() {
      try {
        await positionApi.deleteCategory(record.id)
        message.success('删除成功')
        fetchCategoryData()
        fetchCategoryList()
      } catch (err) {
        console.warn('[系统管理] 删除岗位分类失败', err)
        message.error('删除失败')
      }
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
const levelColorPalette = ['green', 'blue', 'orange', 'red', 'purple']

const getLevelColor = (level: number) => {
  const idx = levelOptions.value.findIndex(o => o.value === level)
  return idx >= 0 ? levelColorPalette[idx % levelColorPalette.length] : 'default'
}

const getLevelName = (level: number) => {
  const found = levelOptions.value.find(o => o.value === level)
  return found ? found.label : '未知'
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT'
  if (e.key === 'F5') {
    e.preventDefault()
    debounceClick('refresh', fetchData)()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !isInput) {
    e.preventDefault()
    handleAdd()
  }
}

onMounted(() => {
  fetchData()
  fetchCategoryList()
  fetchDepartmentList()
  loadLevelOptions()
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
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

defineExpose({ handleQuery: fetchData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
// 查看详情
const handleView = (record: any) => {}
</script>

<style scoped>
.position-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.position-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.position-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.position-page-header-right {
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

.position-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.position-management > :deep(.vxe-table-list-container) {
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
.stat-active { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-disabled { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-categories { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

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

/* ── VxeTable 表头边框线 2px ─────────────────────────── */
.position-management :deep(.vxe-table .vxe-header--row th) {
  border-bottom: 2px solid #e8e8e8 !important;
}
.position-management :deep(.vxe-table .vxe-header--row th:not(:last-child)) {
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

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px; line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }

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

</style>
