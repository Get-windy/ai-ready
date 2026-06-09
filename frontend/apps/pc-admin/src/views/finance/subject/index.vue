<template>
  <PageContainer full-height>
    <template #header>
      <div class="subject-page-header">
        <div class="subject-page-header-left">
          <a-breadcrumb class="subject-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>科目管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="subject-page-header-title">科目管理</h2>
        </div>
        <div class="subject-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="fetchTree">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>
    <div class="finance-subject-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ totalSubjectCount }}</div>
          <div class="stat-card-label">科目总数</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-asset">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ typeCounts[1] || 0 }}</div>
          <div class="stat-card-label">资产类</div>
        </div>
        <FundOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-liability">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ typeCounts[2] || 0 }}</div>
          <div class="stat-card-label">负债类</div>
        </div>
        <CreditCardOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-equity">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ typeCounts[3] || 0 }}</div>
          <div class="stat-card-label">权益类</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-enabled">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ enabledCount }}</div>
          <div class="stat-card-label">已启用</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
    </div>

    <!-- 类型过滤标签 -->
    <a-card :bordered="false" class="filter-card">
      <a-tabs v-model:activeKey="activeTypeTab" @change="handleTypeTabChange">
        <a-tab-pane key="all" tab="全部" />
        <a-tab-pane key="1" tab="资产类" />
        <a-tab-pane key="2" tab="负债类" />
        <a-tab-pane key="3" tab="权益类" />
        <a-tab-pane key="4" tab="成本类" />
        <a-tab-pane key="5" tab="损益类" />
      </a-tabs>
    </a-card>

    <a-row :gutter="16" class="content-row">
      <!-- 左: 科目树 -->
      <a-col :span="8">
        <a-card title="科目结构" :bordered="false" class="tree-card">
          <template #extra>
            <a-space>
              <a-button type="primary" size="small" @click="handleAddRoot">
                <template #icon><PlusOutlined /></template>
                新增一级
              </a-button>
              <a-button size="small" @click="fetchTree">
                <template #icon><ReloadOutlined /></template>
              </a-button>
            </a-space>
          </template>
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索科目编码/名称"
            style="margin-bottom: 12px"
            size="small"
          />
          <a-spin :spinning="treeLoading">
            <div class="tree-container">
              <a-tree
                v-if="filteredTree.length > 0"
                :tree-data="filteredTree"
                :field-names="{ title: 'subjectName', key: 'id', children: 'children' }"
                :selected-keys="selectedKeys"
                :default-expand-all="true"
                :show-line="true"
                @select="handleTreeSelect"
              >
                <template #title="{ subjectCode, subjectName, status }">
                  <span :class="{ 'text-disabled': status === 0 }">
                    {{ subjectCode }} - {{ subjectName }}
                  </span>
                </template>
              </a-tree>
              <a-empty v-else description="暂无科目" />
            </div>
          </a-spin>
        </a-card>
      </a-col>

      <!-- 右: 详情/编辑 -->
      <a-col :span="16">
        <a-card :bordered="false" class="detail-card">
          <template #title>
            <span>科目详情</span>
          </template>

          <template v-if="selectedSubject">
            <a-descriptions :column="2" size="small" bordered>
              <a-descriptions-item label="科目编码" :span="2">
                <a-tag color="blue">{{ selectedSubject.subjectCode }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="科目名称" :span="2">
                {{ selectedSubject.subjectName }}
              </a-descriptions-item>
              <a-descriptions-item label="科目类型">
                <a-tag :color="typeColorMap[selectedSubject.subjectType] || 'default'">
                  {{ typeLabelMap[selectedSubject.subjectType] || '未知' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="借贷方向">
                {{ selectedSubject.direction === 1 ? '借方' : '贷方' }}
              </a-descriptions-item>
              <a-descriptions-item label="状态">
                <a-switch
                  :checked="selectedSubject.enabled"
                  size="small"
                  @change="handleToggleEnabled(selectedSubject)"
                />
              </a-descriptions-item>
              <a-descriptions-item label="科目级别">
                第 {{ selectedSubject.level }} 级
              </a-descriptions-item>
            </a-descriptions>

            <a-space style="margin-top: 16px">
              <a-button type="primary" size="small" @click="handleEdit(selectedSubject)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button size="small" @click="handleAddChild(selectedSubject)">
                <template #icon><PlusOutlined /></template>
                新增下级
              </a-button>
              <a-popconfirm title="确定删除该科目？" @confirm="handleDelete(selectedSubject)">
                <a-button size="small" danger>
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

    <!-- 科目编辑弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEditing ? '编辑科目' : '新增科目'"
      :confirm-loading="formSubmitting"
      width="560px"
      @ok="handleFormSubmit"
      @cancel="handleFormCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        size="small"
      >
        <a-form-item label="科目编码" name="code">
          <a-input v-model:value="formState.code" placeholder="如：1001" />
        </a-form-item>
        <a-form-item label="科目名称" name="name">
          <a-input v-model:value="formState.name" placeholder="如：库存现金" />
        </a-form-item>
        <a-form-item label="科目类型" name="type">
          <a-select v-model:value="formState.type" :options="typeOptions" placeholder="请选择类型" />
        </a-form-item>
        <a-form-item label="上级科目">
          <a-input :value="formState.parentName" disabled placeholder="无（一级科目）" />
        </a-form-item>
        <a-form-item label="借贷方向" name="direction">
          <a-radio-group v-model:value="formState.direction">
            <a-radio :value="1">借方</a-radio>
            <a-radio :value="2">贷方</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="启用">
          <a-switch v-model:checked="formState.enabled" checked-children="启用" un-checked-children="禁用" />
        </a-form-item>
      </a-form>
    </a-modal>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined, SyncOutlined,
  FileTextOutlined, FundOutlined, CreditCardOutlined, DollarOutlined, CheckCircleOutlined
} from '@ant-design/icons-vue'
import { PageContainer } from '@/components'
import { accountSubjectApi } from '@/api/finance'

interface SubjectNode {
  id: number
  code: string
  subjectCode: string
  name: string
  subjectName: string
  subjectType: number
  type: number
  direction: number
  level: number
  parentId?: number
  enabled: boolean
  status: number
  children?: SubjectNode[]
}

const treeLoading = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const searchKeyword = ref('')
const subjectTree = ref<SubjectNode[]>([])
const selectedSubject = ref<SubjectNode | null>(null)
const selectedKeys = ref<number[]>([])
const activeTypeTab = ref('all')

const formVisible = ref(false)
const formSubmitting = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const typeOptions = [
  { label: '资产类', value: 1 },
  { label: '负债类', value: 2 },
  { label: '权益类', value: 3 },
  { label: '成本类', value: 4 },
  { label: '损益类', value: 5 }
]

const typeLabelMap: Record<number, string> = {
  1: '资产类',
  2: '负债类',
  3: '权益类',
  4: '成本类',
  5: '损益类'
}

const typeColorMap: Record<number, string> = {
  1: 'blue',
  2: 'orange',
  3: 'purple',
  4: 'cyan',
  5: 'green'
}

const formState = reactive({
  code: '',
  name: '',
  type: undefined as number | undefined,
  direction: 1,
  enabled: true,
  parentId: undefined as number | undefined,
  parentName: ''
})

const formRules = {
  code: [{ required: true, message: '请输入科目编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入科目名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择科目类型', trigger: 'change' }],
  direction: [{ required: true, message: '请选择借贷方向', trigger: 'change' }]
}

// ── 统计数据 ────────────────────────────────────────────
const totalSubjectCount = computed(() => {
  const countNodes = (nodes: SubjectNode[]): number => {
    return nodes.reduce((sum, node) => {
      return sum + 1 + (node.children ? countNodes(node.children) : 0)
    }, 0)
  }
  return countNodes(subjectTree.value)
})

const typeCounts = computed(() => {
  const counts: Record<number, number> = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 }
  const countByType = (nodes: SubjectNode[]) => {
    nodes.forEach(node => {
      const type = node.subjectType || node.type || 0
      if (type >= 1 && type <= 5) counts[type]++
      if (node.children) countByType(node.children)
    })
  }
  countByType(subjectTree.value)
  return counts
})

const enabledCount = computed(() => {
  const countEnabled = (nodes: SubjectNode[]): number => {
    return nodes.reduce((sum, node) => {
      const nodeEnabled = node.enabled !== undefined ? node.enabled : (node.status === 1)
      return sum + (nodeEnabled ? 1 : 0) + (node.children ? countEnabled(node.children) : 0)
    }, 0)
  }
  return countEnabled(subjectTree.value)
})

const filteredTree = computed(() => {
  let tree = subjectTree.value

  // Filter by type
  if (activeTypeTab.value !== 'all') {
    const typeNum = parseInt(activeTypeTab.value)
    const filterByType = (nodes: SubjectNode[]): SubjectNode[] => {
      return nodes.reduce((acc: SubjectNode[], node) => {
        const match = node.subjectType === typeNum || node.type === typeNum
        const filteredChildren = node.children ? filterByType(node.children) : []
        if (match || filteredChildren.length > 0) {
          acc.push({ ...node, children: filteredChildren })
        }
        return acc
      }, [])
    }
    tree = filterByType(subjectTree.value)
  }

  // Filter by keyword
  if (!searchKeyword.value) return tree
  const kw = searchKeyword.value.toLowerCase()
  const filterNodes = (nodes: SubjectNode[]): SubjectNode[] => {
    return nodes.reduce((acc: SubjectNode[], node) => {
      const code = (node.subjectCode || node.code || '').toLowerCase()
      const name = (node.subjectName || node.name || '').toLowerCase()
      const match = code.includes(kw) || name.includes(kw)
      const filteredChildren = node.children ? filterNodes(node.children) : []
      if (match || filteredChildren.length > 0) {
        acc.push({ ...node, children: filteredChildren })
      }
      return acc
    }, [])
  }
  return filterNodes(tree)
})

const handleTypeTabChange = () => {
  selectedSubject.value = null
  selectedKeys.value = []
}

const fetchTree = async () => {
  treeLoading.value = true
  refreshLoading.value = true
  try {
    const res = await accountSubjectApi.getTree()
    const data = res.data || []
    // Normalize field names
    const normalize = (nodes: any[]): SubjectNode[] => {
      return nodes.map((n: any) => ({
        id: n.id,
        code: n.code || n.subjectCode || '',
        subjectCode: n.subjectCode || n.code || '',
        name: n.name || n.subjectName || '',
        subjectName: n.subjectName || n.name || '',
        subjectType: n.subjectType || n.type || 0,
        type: n.type || n.subjectType || 0,
        direction: n.direction || n.balanceDirection || 1,
        level: n.level || n.subjectLevel || 1,
        parentId: n.parentId,
        enabled: n.enabled !== undefined ? n.enabled : (n.status === 1),
        status: n.status !== undefined ? n.status : (n.enabled ? 1 : 0),
        children: n.children ? normalize(n.children) : []
      }))
    }
    subjectTree.value = normalize(data)
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    console.warn('获取科目树失败', err)
    message.error('获取科目树失败')
  } finally {
    treeLoading.value = false
    refreshLoading.value = false
  }
}

const handleTreeSelect = (keys: any[]) => {
  if (keys.length === 0) return
  selectedKeys.value = [keys[0]]
  const findById = (nodes: SubjectNode[], id: number): SubjectNode | null => {
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

const resetForm = () => {
  formState.code = ''
  formState.name = ''
  formState.type = undefined
  formState.direction = 1
  formState.enabled = true
  formState.parentId = undefined
  formState.parentName = ''
  editingId.value = null
  isEditing.value = false
  formRef.value?.clearValidate()
}

const handleAddRoot = () => {
  resetForm()
  formVisible.value = true
}

const handleAddChild = (parent: SubjectNode) => {
  resetForm()
  formState.parentId = parent.id
  formState.parentName = `${parent.subjectCode || parent.code} ${parent.subjectName || parent.name}`
  formState.type = parent.subjectType || parent.type
  formVisible.value = true
}

const handleEdit = (subject: SubjectNode) => {
  isEditing.value = true
  editingId.value = subject.id
  formState.code = subject.subjectCode || subject.code
  formState.name = subject.subjectName || subject.name
  formState.type = subject.subjectType || subject.type
  formState.direction = subject.direction
  formState.enabled = subject.enabled
  formState.parentId = subject.parentId
  if (subject.parentId) {
    const findParent = (nodes: SubjectNode[]): string => {
      for (const node of nodes) {
        if (node.id === subject.parentId) return `${node.subjectCode || node.code} ${node.subjectName || node.name}`
        if (node.children) {
          const found = findParent(node.children)
          if (found) return found
        }
      }
      return ''
    }
    formState.parentName = findParent(subjectTree.value)
  }
  formVisible.value = true
}

const handleDelete = async (subject: SubjectNode) => {
  try {
    await accountSubjectApi.delete(subject.id)
    message.success('科目已删除')
    selectedSubject.value = null
    selectedKeys.value = []
    await fetchTree()
  } catch (err) {
    console.warn('[科目管理] 删除科目失败', err)
    message.error('删除失败')
  }
}

const handleToggleEnabled = async (subject: SubjectNode) => {
  try {
    await accountSubjectApi.toggleEnabled(subject.id)
    subject.enabled = !subject.enabled
    message.success(subject.enabled ? '科目已启用' : '科目已禁用')
  } catch (err) {
    console.warn('[科目管理] 切换启用状态失败', err)
    message.error('操作失败')
  }
}

const handleFormSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch (err) {
    console.warn('[科目管理] 表单校验失败', err)
    return
  }
  formSubmitting.value = true
  try {
    const data = {
      code: formState.code,
      name: formState.name,
      subjectCode: formState.code,
      subjectName: formState.name,
      type: formState.type,
      subjectType: formState.type,
      direction: formState.direction,
      balanceDirection: formState.direction,
      enabled: formState.enabled,
      status: formState.enabled ? 1 : 0,
      parentId: formState.parentId,
      level: formState.parentId ? 2 : 1
    }

    if (isEditing.value && editingId.value) {
      await accountSubjectApi.update(editingId.value, data)
      message.success('科目已更新')
    } else {
      await accountSubjectApi.create(data)
      message.success('科目已创建')
    }
    formVisible.value = false
    await fetchTree()
  } catch (err) {
    console.warn('[科目管理] 提交表单失败', err)
    message.error(isEditing.value ? '更新失败' : '创建失败')
  } finally {
    formSubmitting.value = false
  }
}

const handleFormCancel = () => {
  formVisible.value = false
}

// 定时刷新（30s）
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchTree()
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
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchTree })
</script>

<style scoped>
.subject-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.subject-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.subject-breadcrumb {
  font-size: 13px;
}
.subject-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.subject-page-header-right {
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

.finance-subject-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-asset { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-liability { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-equity { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.stat-enabled { background: linear-gradient(135deg, #e6fffb 0%, #b5f5ec 100%); }

.stat-card-value {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 24px;
  color: rgba(0, 0, 0, 0.15);
}

.filter-card {
  margin-bottom: 0;
}

.filter-card :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

.content-row {
  flex: 1;
  min-height: 0;
}

.tree-card,
.detail-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tree-card :deep(.ant-card-body),
.detail-card :deep(.ant-card-body) {
  flex: 1;
  overflow: auto;
}

.tree-container {
  flex: 1;
  overflow-y: auto;
}

.text-disabled {
  color: #bbb;
  text-decoration: line-through;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 30%;
    min-width: 100px;
  }
}
</style>
