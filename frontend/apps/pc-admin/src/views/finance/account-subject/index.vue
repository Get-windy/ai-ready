<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="account-subject-page-header">
        <div class="account-subject-page-header-left">
          <a-breadcrumb class="account-subject-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>科目管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="account-subject-page-header-title">科目管理</h2>
        </div>
        <div class="account-subject-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchTree)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </div>
    </template>
    <div class="account-subject-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ flattenTree.length }}</div>
          <div class="stat-card-label">科目总数</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-asset">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ assetCount }}</div>
          <div class="stat-card-label">资产类</div>
        </div>
        <DashboardOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-liability">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ liabilityCount }}</div>
          <div class="stat-card-label">负债类</div>
        </div>
        <BankOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-active">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ activeCount }}</div>
          <div class="stat-card-label">启用科目</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
    </div>

    <a-card>
      <template #title>
        <a-space>
          <FileTextOutlined /> 会计科目
        </a-space>
      </template>
      <template #extra>
        <a-button v-permission="'finance:subject:create'" type="primary" size="small" @click="handleAddRoot">
          <template #icon><PlusOutlined /></template>
          新增科目
        </a-button>
        <a-button size="small" @click="debounceClick('refresh', fetchTree)" style="margin-left: 8px">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </template>

      <a-row :gutter="16">
        <!-- 科目树 -->
        <a-col :span="8">
          <a-card title="科目结构" size="small" :bordered="false">
            <a-input-search
              v-model:value="searchKeyword"
              placeholder="搜索科目..."
              style="margin-bottom: 12px"
              size="small"
              @change="() => {}"
            />
            <a-spin :spinning="treeLoading">
              <div class="subject-tree-container">
                <a-tree
                  v-if="filteredTree.length > 0"
                  :tree-data="filteredTree as any"
                  :field-names="{
                    title: 'subjectName',
                    key: 'id',
                    children: 'children'
                  }"
                  :selected-keys="[selectedSubject?.id].filter(Boolean)"
                  :default-expand-all="true"
                  :show-line="true"
                  :show-icon="true"
                  @select="handleTreeSelect"
                >
                  <template #icon="{ subjectType }">
                    <component :is="getSubjectTypeIcon(subjectType)" />
                  </template>
                  <template #title="{ subjectCode, subjectName, subjectLevel, status }">
                    <span :class="{ 'text-disabled': status === 0 }">
                      {{ subjectCode }} {{ subjectName }}
                    </span>
                  </template>
                </a-tree>
                <a-empty v-else description="暂无科目" />
              </div>
            </a-spin>
          </a-card>
        </a-col>

        <!-- 科目详情 -->
        <a-col :span="16">
          <a-card title="科目详情" size="small" :bordered="false">
            <template v-if="selectedSubject">
              <a-descriptions :column="2" size="small" bordered>
                <a-descriptions-item label="科目编码" :span="2">
                  <a-tag color="blue">{{ selectedSubject.subjectCode }}</a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="科目名称" :span="2">
                  {{ selectedSubject.subjectName }}
                </a-descriptions-item>
                <a-descriptions-item label="科目类别">
                  <a-tag :color="getSubjectTypeColor(selectedSubject.subjectType)">
                    {{ selectedSubject.subjectTypeDesc }}
                  </a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="科目级别">
                  第 {{ selectedSubject.subjectLevel }} 级
                </a-descriptions-item>
                <a-descriptions-item label="借贷方向">
                  {{ selectedSubject.balanceDirectionDesc || (selectedSubject.balanceDirection === 1 ? '借方' : '贷方') }}
                </a-descriptions-item>
                <a-descriptions-item label="状态">
                  <a-tag :color="selectedSubject.status === 1 ? 'success' : 'default'">
                    {{ selectedSubject.status === 1 ? '启用' : '禁用' }}
                  </a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="是否明细">
                  {{ selectedSubject.isDetail === 1 ? '明细科目' : '非明细科目' }}
                </a-descriptions-item>
                <a-descriptions-item label="当前余额" :span="2">
                  <span class="text-primary font-bold">
                    ¥{{ (selectedSubject.currentBalance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 }) }}
                  </span>
                </a-descriptions-item>
                <a-descriptions-item label="期初余额" :span="2">
                  ¥{{ (selectedSubject.openingBalance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 }) }}
                </a-descriptions-item>
                <a-descriptions-item v-if="selectedSubject.remark" label="备注" :span="2">
                  {{ selectedSubject.remark }}
                </a-descriptions-item>
              </a-descriptions>
              <a-space style="margin-top: 16px">
                <a-button v-permission="'finance:subject:edit'" size="small" @click="handleEdit(selectedSubject)">
                  <template #icon><EditOutlined /></template>
                  编辑
                </a-button>
                <a-button v-permission="'finance:subject:create'" size="small" @click="handleAddChild(selectedSubject)">
                  <template #icon><PlusOutlined /></template>
                  新增下级
                </a-button>
                <a-popconfirm
                  title="确定删除该科目？"
                  @confirm="handleDelete(selectedSubject)"
                >
                  <a-button v-permission="'finance:subject:delete'" size="small" danger>
                    <template #icon><DeleteOutlined /></template>
                    删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
            <a-empty v-else description="请从左侧选择一个科目" />
          </a-card>
        </a-col>
      </a-row>
    </a-card>

    <!-- 科目编辑弹窗 -->
    <FullScreenDetail
      :visible="formVisible"
      :title="isEdit ? '编辑科目' : '新增科目'"
      :save-loading="formSubmitting"
      :show-save-and-new="!isEdit"
      @save="handleFormSubmit"
      @close="handleFormClose"
      @save-and-new="handleFormSaveAndNew"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        size="small"
      >
        <a-form-item label="科目编码" name="subjectCode">
          <a-input v-model:value="formState.subjectCode" placeholder="如：1001" size="small" />
        </a-form-item>
        <a-form-item label="科目名称" name="subjectName">
          <a-input v-model:value="formState.subjectName" placeholder="如：库存现金" size="small" />
        </a-form-item>
        <a-form-item label="科目类别" name="subjectType">
          <a-select v-model:value="formState.subjectType" :options="subjectTypeOptions" size="small" />
        </a-form-item>
        <a-form-item label="上级科目">
          <a-input :value="formState.parentSubjectName" disabled placeholder="无（一级科目）" />
        </a-form-item>
        <a-form-item label="借贷方向" name="balanceDirection">
          <a-radio-group v-model:value="formState.balanceDirection">
            <a-radio :value="1">借方</a-radio>
            <a-radio :value="2">贷方</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="是否明细">
          <a-switch v-model:checked="formState.isDetailBool" />
        </a-form-item>
        <a-form-item label="期初余额">
          <a-input-number v-model:value="formState.openingBalance" :precision="2" style="width: 100%" :min="0" size="small" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="formState.statusBool" checked-children="启用" un-checked-children="禁用" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="formState.sortOrder" :min="0" style="width: 100%" size="small" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="formState.remark" :rows="2" size="small" />
        </a-form-item>
      </a-form>
    </FullScreenDetail>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FileTextOutlined,
  ReloadOutlined,
  SyncOutlined,
  BankOutlined,
  DollarOutlined,
  GoldOutlined,
  ToolOutlined,
  PercentageOutlined,
  CheckCircleOutlined,
  DashboardOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { accountingApi, type AccountSubject, type AccountSubjectSave } from '@/api/finance/accounting'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

const treeLoading = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const searchKeyword = ref('')
const subjectTree = ref<AccountSubject[]>([])
const selectedSubject = ref<AccountSubject | null>(null)
const formVisible = ref(false)
const formSubmitting = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref()

const subjectTypeOptions = [
  { label: '资产类', value: 1 },
  { label: '负债类', value: 2 },
  { label: '权益类', value: 3 },
  { label: '成本类', value: 4 },
  { label: '损益类', value: 5 }
]

const filteredTree = computed(() => {
  if (!searchKeyword.value) return subjectTree.value
  const kw = searchKeyword.value.toLowerCase()
  const filterNodes = (nodes: AccountSubject[]): AccountSubject[] => {
    return nodes.reduce((acc: AccountSubject[], node) => {
      const match = node.subjectCode?.toLowerCase().includes(kw) || node.subjectName?.toLowerCase().includes(kw)
      const filteredChildren = node.children ? filterNodes(node.children) : []
      if (match || filteredChildren.length > 0) {
        acc.push({ ...node, children: filteredChildren })
      }
      return acc
    }, [])
  }
  return filterNodes(subjectTree.value)
})

// ── 统计数据 ────────────────────────────────────────────
const flattenTree = computed(() => {
  const result: AccountSubject[] = []
  const traverse = (nodes: AccountSubject[]) => {
    for (const node of nodes) {
      result.push(node)
      if (node.children?.length) traverse(node.children)
    }
  }
  traverse(subjectTree.value)
  return result
})

const assetCount = computed(() => flattenTree.value.filter(r => r.subjectType === 1).length)
const liabilityCount = computed(() => flattenTree.value.filter(r => r.subjectType === 2).length)
const activeCount = computed(() => flattenTree.value.filter(r => r.status === 1).length)

const formState = reactive({
  subjectCode: '',
  subjectName: '',
  subjectType: undefined as number | undefined,
  balanceDirection: 1,
  isDetailBool: true,
  openingBalance: 0,
  statusBool: true,
  sortOrder: 0,
  remark: '',
  parentId: undefined as number | undefined,
  parentSubjectName: ''
})

const formRules = {
  subjectCode: [{ required: true, message: '请输入科目编码', trigger: 'blur' }],
  subjectName: [{ required: true, message: '请输入科目名称', trigger: 'blur' }],
  subjectType: [{ required: true, message: '请选择科目类别', trigger: 'change' }],
  balanceDirection: [{ required: true, message: '请选择借贷方向', trigger: 'change' }]
} as any

const initialFormSnapshot = ref('')
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify({ ...formState })
}
const formDirty = computed(() => {
  return JSON.stringify({ ...formState }) !== initialFormSnapshot.value
})

onBeforeRouteLeave((to, from, next) => {
  if (formVisible.value && formDirty.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前表单有未保存的修改，确定要离开吗？',
      onOk: () => next(),
      onCancel: () => next(false)
    })
  } else {
    next()
  }
})

function handleParentCreate() {
  handleAdd()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchTree); return }
  if (e.ctrlKey && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

function handleAdd() {
  handleAddRoot()
}

function getSubjectTypeColor(type?: number): string {
  const colors: Record<number, string> = { 1: 'blue', 2: 'orange', 3: 'purple', 4: 'cyan', 5: 'green' }
  return colors[type || 0] || 'default'
}

function getSubjectTypeIcon(type?: number) {
  const icons: Record<number, any> = { 1: BankOutlined, 2: DollarOutlined, 3: GoldOutlined, 4: ToolOutlined, 5: PercentageOutlined }
  return icons[type || 0] || FileTextOutlined
}

async function fetchTree() {
  treeLoading.value = true
  refreshLoading.value = true
  try {
    const res = await accountingApi.getSubjectTree()
    subjectTree.value = res.data || []
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    console.warn('获取科目树失败', err)
    message.error('获取科目树失败')
  } finally {
    treeLoading.value = false
    refreshLoading.value = false
  }
}

function handleTreeSelect(keys: any[]) {
  if (keys.length === 0) return
  const findById = (nodes: AccountSubject[], id: number): AccountSubject | null => {
    for (const node of nodes) {
      if (node.id === id) return node
      if (node.children) {
        const found = findById(node.children, id)
        if (found) return found
      }
    }
    return null
  }
  selectedSubject.value = findById(subjectTree.value, keys[0])
}

function resetForm() {
  formState.subjectCode = ''
  formState.subjectName = ''
  formState.subjectType = undefined
  formState.balanceDirection = 1
  formState.isDetailBool = true
  formState.openingBalance = 0
  formState.statusBool = true
  formState.sortOrder = 0
  formState.remark = ''
  formState.parentId = undefined
  formState.parentSubjectName = ''
  editId.value = null
  isEdit.value = false
  formRef.value?.clearValidate()
}

function handleAddRoot() {
  resetForm()
  formVisible.value = true
  nextTick(() => saveFormSnapshot())
}

function handleAddChild(parent: AccountSubject) {
  resetForm()
  formState.parentId = parent.id
  formState.parentSubjectName = `${parent.subjectCode} ${parent.subjectName}`
  formState.subjectType = parent.subjectType
  formVisible.value = true
  nextTick(() => saveFormSnapshot())
}

function handleEdit(subject: AccountSubject) {
  isEdit.value = true
  editId.value = subject.id
  formState.subjectCode = subject.subjectCode
  formState.subjectName = subject.subjectName
  formState.subjectType = subject.subjectType
  formState.balanceDirection = subject.balanceDirection
  formState.isDetailBool = subject.isDetail === 1
  formState.openingBalance = subject.openingBalance || 0
  formState.statusBool = subject.status === 1
  formState.sortOrder = subject.sortOrder || 0
  formState.remark = subject.remark || ''
  formState.parentId = subject.parentId

  // 查找上级科目名称
  if (subject.parentId) {
    const findParent = (nodes: AccountSubject[]): string => {
      for (const node of nodes) {
        if (node.id === subject.parentId) return `${node.subjectCode} ${node.subjectName}`
        if (node.children) {
          const found = findParent(node.children)
          if (found) return found
        }
      }
      return ''
    }
    formState.parentSubjectName = findParent(subjectTree.value)
  }
  formVisible.value = true
  nextTick(() => saveFormSnapshot())
}

async function handleDelete(subject: AccountSubject) {
  try {
    await accountingApi.deleteSubject(subject.id)
    message.success('科目已删除')
    selectedSubject.value = null
    await fetchTree()
  } catch (err) {
    console.warn('[科目管理] 删除科目失败', err)
    message.error('删除失败')
  }
}

async function handleFormSubmit() {
  try {
    await formRef.value?.validate()
  } catch (err) {
    console.warn('[科目管理] 表单校验失败', err)
    return
  }
  formSubmitting.value = true
  try {
    const data: AccountSubjectSave = {
      subjectCode: formState.subjectCode,
      subjectName: formState.subjectName,
      subjectType: formState.subjectType!,
      subjectLevel: (formState.parentId ? 2 : 1),
      parentId: formState.parentId,
      balanceDirection: formState.balanceDirection,
      openingBalance: formState.openingBalance,
      isDetail: formState.isDetailBool ? 1 : 0,
      status: formState.statusBool ? 1 : 0,
      sortOrder: formState.sortOrder,
      remark: formState.remark
    }

    if (isEdit.value && editId.value) {
      await accountingApi.updateSubject(editId.value, data)
      message.success('科目已更新')
    } else {
      await accountingApi.createSubject(data)
      message.success('科目已创建')
    }
    formVisible.value = false
    await fetchTree()
  } catch (e: any) {
    message.error(e?.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    formSubmitting.value = false
  }
}

function handleFormClose() {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '当前表单有未保存的修改，确定要关闭吗？',
      onOk: () => { formVisible.value = false }
    })
  } else {
    formVisible.value = false
  }
}

let _savedAndNew = false
function handleFormSaveAndNew() {
  _savedAndNew = true
  handleFormSubmit().then(() => {
    if (_savedAndNew && !formVisible.value) {
      _savedAndNew = false
      resetForm()
      formVisible.value = true
      nextTick(() => saveFormSnapshot())
    }
  })
}

// 定时刷新（30s）
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchTree()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', fetchTree)
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
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', fetchTree)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchTree })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.account-subject-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.account-subject-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.account-subject-breadcrumb {
  font-size: 13px;
}
.account-subject-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.account-subject-page-header-right {
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

.account-subject-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.account-subject-page :deep(.ant-card) {
  height: 100%;
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
.stat-asset { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-liability { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-active { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

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

.subject-tree-container {
  max-height: 520px;
  overflow-y: auto;
}

.text-disabled {
  color: var(--color-text-disabled, #bbb);
  text-decoration: line-through;
}

.font-bold {
  font-weight: 600;
}

/* Compact mode overrides */
:deep(.ant-table-thead > tr > th) {
  padding: 6px 8px !important;
  font-size: 12px;
}
:deep(.ant-table-tbody > tr > td) {
  padding: 4px 8px !important;
  font-size: 12px;
}
:deep(.ant-card-body) {
  padding: 12px;
}
:deep(.ant-form-item) {
  margin-bottom: 8px;
}

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
