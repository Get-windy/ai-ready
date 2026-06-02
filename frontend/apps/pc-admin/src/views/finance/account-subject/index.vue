<template>
  <div class="account-subject-page">
    <a-card>
      <template #title>
        <a-space>
          <FileTextOutlined /> 会计科目
        </a-space>
      </template>
      <template #extra>
        <a-button type="primary" size="small" @click="handleAddRoot">
          <template #icon><PlusOutlined /></template>
          新增科目
        </a-button>
        <a-button size="small" @click="fetchTree" style="margin-left: 8px">
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
              @change="filterTree"
            />
            <a-spin :spinning="treeLoading">
              <div class="subject-tree-container">
                <a-tree
                  v-if="filteredTree.length > 0"
                  :tree-data="filteredTree"
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
                <a-button size="small" @click="handleEdit(selectedSubject)">
                  <template #icon><EditOutlined /></template>
                  编辑
                </a-button>
                <a-button size="small" @click="handleAddChild(selectedSubject)">
                  <template #icon><PlusOutlined /></template>
                  新增下级
                </a-button>
                <a-popconfirm
                  title="确定删除该科目？"
                  @confirm="handleDelete(selectedSubject)"
                >
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
    </a-card>

    <!-- 科目编辑弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑科目' : '新增科目'"
      :width="560"
      :confirm-loading="formSubmitting"
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
        <a-form-item label="科目编码" name="subjectCode">
          <a-input v-model:value="formState.subjectCode" placeholder="如：1001" />
        </a-form-item>
        <a-form-item label="科目名称" name="subjectName">
          <a-input v-model:value="formState.subjectName" placeholder="如：库存现金" />
        </a-form-item>
        <a-form-item label="科目类别" name="subjectType">
          <a-select v-model:value="formState.subjectType" :options="subjectTypeOptions" />
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
          <a-input-number v-model:value="formState.openingBalance" :precision="2" style="width: 100%" :min="0" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="formState.statusBool" checked-children="启用" un-checked-children="禁用" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="formState.sortOrder" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="formState.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FileTextOutlined,
  ReloadOutlined,
  BankOutlined,
  DollarOutlined,
  GoldOutlined,
  ToolOutlined,
  PercentageOutlined
} from '@ant-design/icons-vue'
import { accountingApi, type AccountSubject, type AccountSubjectSave } from '@/api/finance/accounting'

const treeLoading = ref(false)
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
  try {
    const res = await accountingApi.getSubjectTree()
    subjectTree.value = res.data || []
  } catch {
    message.error('获取科目树失败')
  } finally {
    treeLoading.value = false
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
}

function handleAddChild(parent: AccountSubject) {
  resetForm()
  formState.parentId = parent.id
  formState.parentSubjectName = `${parent.subjectCode} ${parent.subjectName}`
  formState.subjectType = parent.subjectType
  formVisible.value = true
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
}

async function handleDelete(subject: AccountSubject) {
  try {
    await accountingApi.deleteSubject(subject.id)
    message.success('科目已删除')
    selectedSubject.value = null
    await fetchTree()
  } catch {
    message.error('删除失败')
  }
}

async function handleFormSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
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

function handleFormCancel() {
  formVisible.value = false
}

onMounted(() => {
  fetchTree()
})
</script>

<style scoped>
.account-subject-page :deep(.ant-card) {
  height: 100%;
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
</style>
