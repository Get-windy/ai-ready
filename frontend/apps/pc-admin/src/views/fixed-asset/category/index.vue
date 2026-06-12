<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="category-page-header">
        <div class="category-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>固定资产</a-breadcrumb-item>
            <a-breadcrumb-item>分类管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="category-page-header-title">分类管理</h2>
        </div>
        <div class="category-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <PrintButton business-type="fixed_asset_category" button-type="link" button-size="small" tooltip="打印分类" />
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchTree)()" v-permission="'erp:fixed-asset:category:list'">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
<span class="shortcut-hints">
                                                <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
                                                <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
                                              </span>
        </div>

      </div>
    </template>

    <div class="category-list-page">
      <!-- 错误态 -->
      <template v-if="hasError && !refreshLoading">
        <a-result status="error" title="加载失败" sub-title="获取分类数据时发生错误">
          <template #extra>
            <a-button size="small" type="primary" @click="fetchTree">重新加载</a-button>
          </template>
        </a-result>
      </template>

      <!-- 正常内容 -->
      <template v-else>
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ treeData.length || 0 }}</div>
            <div class="stat-card-label">分类总数</div>
          </div>
          <FolderOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-root">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ rootCount }}</div>
            <div class="stat-card-label">根分类数</div>
          </div>
          <ApartmentOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-depth">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ maxDepth }}</div>
            <div class="stat-card-label">最大层级</div>
          </div>
          <ClusterOutlined class="stat-card-icon" />
        </div>
      </div>

      <a-row :gutter="16" class="category-content">
        <a-col :span="10">
          <a-card title="分类树" class="category-card">
            <template #extra>
              <a-button type="primary" size="small" @click="showAddRootModal" v-permission="'erp:fixed-asset:category:create'">添加根分类</a-button>
            </template>
            <a-tree
              v-if="treeData.length > 0"
              :tree-data="treeData as any"
              :default-expand-all="true"
              @select="onSelect"
            />
            <a-empty v-else description="暂无分类数据" />
          </a-card>
        </a-col>
        <a-col :span="14">
          <a-card :title="selectedCategory ? '分类详情' : '选择分类'" class="category-card">
            <template v-if="selectedCategory">
              <a-descriptions :column="1" bordered :label-style="{ fontWeight: 'bold' }">
                <a-descriptions-item label="分类编码">{{ selectedCategory.categoryCode }}</a-descriptions-item>
                <a-descriptions-item label="分类名称">{{ selectedCategory.categoryName }}</a-descriptions-item>
                <a-descriptions-item label="排序">{{ selectedCategory.sortOrder }}</a-descriptions-item>
                <a-descriptions-item label="默认折旧方法">{{ methodMap[selectedCategory.defaultDepreciationMethod] }}</a-descriptions-item>
                <a-descriptions-item label="默认使用年限(月)">{{ selectedCategory.defaultUsefulLife }}</a-descriptions-item>
                <a-descriptions-item label="描述">{{ selectedCategory.description }}</a-descriptions-item>
              </a-descriptions>
              <a-space style="margin-top: 16px">
                <a-button type="primary" @click="showEditModal" v-permission="'erp:fixed-asset:category:update'">编辑</a-button>
                <a-button @click="showAddChildModal" v-permission="'erp:fixed-asset:category:create'">添加子分类</a-button>
                <a-popconfirm title="确认删除?" @confirm="handleDelete" v-permission="'erp:fixed-asset:category:delete'">
                  <a-button danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
            <a-empty v-else description="请在左侧选择一个分类" />
          </a-card>
        </a-col>
      </a-row>

      <!-- Category Form Modal -->
      <FullScreenDetail
        :visible="modalVisible"
        :title="modalTitle"
        @save="handleModalOk"
        :save-loading="modalLoading"
        :show-save-and-new="!isEdit"
        @close="handleFormClose"
        @save-and-new="handleFormSaveAndNew"
      >
        <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="分类编码">
            <a-input v-model:value="formData.categoryCode" placeholder="分类编码" size="small" />
          </a-form-item>
          <a-form-item label="分类名称" required>
            <a-input v-model:value="formData.categoryName" placeholder="分类名称" size="small" />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number v-model:value="formData.sortOrder" :min="0" style="width: 100%" size="small" />
          </a-form-item>
          <a-form-item label="默认折旧方法">
            <a-select v-model:value="formData.defaultDepreciationMethod" placeholder="选择折旧方法" size="small">
              <a-select-option value="straight_line">直线法</a-select-option>
              <a-select-option value="double_declining">双倍余额递减法</a-select-option>
              <a-select-option value="sum_of_years">年数总和法</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="默认使用年限(月)">
            <a-input-number v-model:value="formData.defaultUsefulLife" :min="1" style="width: 100%" size="small" />
          </a-form-item>
          <a-form-item label="描述">
            <a-textarea v-model:value="formData.description" :rows="2" size="small" />
          </a-form-item>
        </a-form>
      </FullScreenDetail>
      </template>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { onBeforeRouteLeave } from 'vue-router'
import { fixedAssetCategoryApi } from '@/api/fixed-asset'
import { message, Modal } from 'ant-design-vue'
import { FolderOutlined, ApartmentOutlined, ClusterOutlined, SyncOutlined, ReloadOutlined, WarningOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'

interface Category {
  id: number
  categoryCode: string
  categoryName: string
  parentId: number
  sortOrder: number
  defaultDepreciationMethod: string
  defaultUsefulLife: number
  description: string
  children?: Category[]
}

const treeData = ref<Category[]>([])
const selectedCategory = ref<Category | null>(null)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const isAddChild = ref(false)
const editId = ref<number | null>(null)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 统计数据 ────────────────────────────────────────────
const rootCount = computed(() => {
  return treeData.value.filter(node => !node.parentId || node.parentId === 0).length
})

const maxDepth = computed(() => {
  function getDepth(nodes: Category[], depth = 1): number {
    if (!nodes || nodes.length === 0) return depth
    let max = depth
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        max = Math.max(max, getDepth(node.children, depth + 1))
      }
    }
    return max
  }
  return getDepth(treeData.value)
})

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

// ── 表单数据 ────────────────────────────────────────────
const formData = reactive({
  categoryCode: '',
  categoryName: '',
  parentId: undefined as number | undefined,
  sortOrder: 0,
  defaultDepreciationMethod: 'straight_line',
  defaultUsefulLife: 60,
  description: '',
})

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

const methodMap: Record<string, string> = {
  straight_line: '直线法', double_declining: '双倍余额递减法', sum_of_years: '年数总和法',
}

const modalTitle = computed(() => {
  if (isEdit.value) return '编辑分类'
  if (isAddChild.value) return '添加子分类'
  return '添加根分类'
})

function handleParentCreate() {
  showAddRootModal()
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT'
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !isInput) {
    e.preventDefault()
    debounceClick('refresh', fetchTree)()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !isInput) {
    e.preventDefault()
    showAddRootModal()
  }
}

onMounted(() => {
  fetchTree()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('fixed-asset:create', handleParentCreate)
  window.addEventListener('fixed-asset:refresh', fetchTree)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchTree()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('fixed-asset:create', handleParentCreate)
  window.removeEventListener('fixed-asset:refresh', fetchTree)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchTree })

async function fetchTree() {
  hasError.value = false
  refreshLoading.value = true
  try {
    const res = await fixedAssetCategoryApi.getTree()
    treeData.value = res.data || []
  } catch {
    hasError.value = true
    console.warn('[分类管理] 加载分类树失败')
    message.error('加载分类树失败')
  } finally {
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

function onSelect(keys: any[], info: any) {
  if (keys.length > 0) {
    const id = Number(keys[0])
    fixedAssetCategoryApi.getById(id).then((res: any) => {
      selectedCategory.value = res.data
    }).catch(() => {
      console.warn('[分类管理] 加载分类详情失败')
      message.error('加载分类详情失败')
    })
  }
}

function showAddRootModal() {
  isEdit.value = false
  isAddChild.value = false
  editId.value = null
  Object.assign(formData, {
    categoryCode: '',
    categoryName: '',
    parentId: undefined,
    sortOrder: 0,
    defaultDepreciationMethod: 'straight_line',
    defaultUsefulLife: 60,
    description: '',
  })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

function showAddChildModal() {
  if (!selectedCategory.value) return
  isEdit.value = false
  isAddChild.value = true
  editId.value = null
  Object.assign(formData, {
    categoryCode: '',
    categoryName: '',
    parentId: selectedCategory.value.id,
    sortOrder: 0,
    defaultDepreciationMethod: selectedCategory.value.defaultDepreciationMethod || 'straight_line',
    defaultUsefulLife: selectedCategory.value.defaultUsefulLife || 60,
    description: '',
  })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

function showEditModal() {
  if (!selectedCategory.value) return
  isEdit.value = true
  isAddChild.value = false
  editId.value = selectedCategory.value.id
  Object.assign(formData, selectedCategory.value)
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

function handleModalOk() {
  modalLoading.value = true
  const apiCall = isEdit.value && editId.value
    ? fixedAssetCategoryApi.update(editId.value, formData)
    : fixedAssetCategoryApi.create(formData)

  apiCall.then(() => {
    message.success(isEdit.value ? '更新成功' : '创建成功')
    modalVisible.value = false
    fetchTree()
    selectedCategory.value = null
  }).catch((err: any) => {
    console.warn('[分类管理] 操作失败', err)
    message.error(err.message || '操作失败')
  }).finally(() => {
    modalLoading.value = false
  })
}

function handleFormClose() {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '您有未保存的修改，确定要关闭吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => { modalVisible.value = false },
    })
  } else {
    modalVisible.value = false
  }
}

function handleFormSaveAndNew() {
  handleModalOk()
}

function handleDelete() {
  if (!selectedCategory.value) return
  fixedAssetCategoryApi.delete(selectedCategory.value.id).then(() => {
    message.success('删除成功')
    selectedCategory.value = null
    fetchTree()
  }).catch((err: any) => {
    console.warn('[分类管理] 删除失败', err)
    message.error(err.message || '删除失败')
  })
}

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.category-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.category-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.category-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.category-page-header-right {
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

.category-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  background: #fff;
  border-radius: 8px;
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

.stat-total { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.stat-root { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-depth { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
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

/* 内容区域 */
.category-content {
  flex: 1;
  min-height: 0;
}

.category-card {
  height: 100%;
}

:deep(.ant-card-body) {
  height: calc(100% - 57px);
  overflow-y: auto;
}

/* 响应式 */
@media (max-width: 768px) {
  .category-content {
    flex-direction: column;
  }
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
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

/* ── vxe-table 表头边框 ──────────────────────── */
:deep(.vxe-table--header-border) {
  border-bottom: 2px solid #e8e8e8 !important;
}

/* ── 空状态容器 ──────────────────────── */
:deep(.empty-state-wrapper) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 200px;
}

</style>
