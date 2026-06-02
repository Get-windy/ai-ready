<template>
  <div class="finance-subject-page">
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
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons-vue'
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
  } catch {
    message.error('获取科目树失败')
  } finally {
    treeLoading.value = false
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
  } catch {
    message.error('删除失败')
  }
}

const handleToggleEnabled = async (subject: SubjectNode) => {
  try {
    await accountSubjectApi.toggleEnabled(subject.id)
    subject.enabled = !subject.enabled
    message.success(subject.enabled ? '科目已启用' : '科目已禁用')
  } catch {
    message.error('操作失败')
  }
}

const handleFormSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
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
  } catch {
    message.error(isEditing.value ? '更新失败' : '创建失败')
  } finally {
    formSubmitting.value = false
  }
}

const handleFormCancel = () => {
  formVisible.value = false
}

onMounted(() => {
  fetchTree()
})
</script>

<style scoped>
.finance-subject-page {
  padding: 16px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-card :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

.content-row {
  height: calc(100vh - 240px);
}

.tree-card,
.detail-card {
  height: 100%;
}

.tree-container {
  max-height: 520px;
  overflow-y: auto;
}

.text-disabled {
  color: #bbb;
  text-decoration: line-through;
}
</style>
