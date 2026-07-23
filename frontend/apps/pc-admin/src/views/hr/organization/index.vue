<template>
  <ErrorBoundary>
    <PageContainer title="组织架构">
      <div class="org-layout">
        <!-- 左侧部门树 -->
        <div class="org-sidebar">
          <div class="sidebar-header">
            <span style="font-weight:600">部门结构</span>
            <a-button type="primary" size="small" @click="showDeptModal">
              <template #icon><PlusOutlined /></template>新增部门
            </a-button>
          </div>
          <a-tree
            :tree-data="deptTree"
            :default-expand-all="true"
            @select="onDeptSelect"
          >
            <template #title="{ title }">
              <span>{{ title }}</span>
            </template>
          </a-tree>
        </div>

        <!-- 右侧岗位列表 -->
        <div class="org-main">
          <div class="toolbar">
            <span style="font-weight:600">{{ selectedDeptName || '全部岗位' }}</span>
            <a-button type="primary" size="small" @click="showPositionModal">
              <template #icon><PlusOutlined /></template>新增岗位
            </a-button>
          </div>
          <a-table
            :columns="positionColumns"
            :data-source="positionData"
            :loading="positionLoading"
            :pagination="positionPagination"
            row-key="id"
            size="small"
            @change="handlePositionChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'positionLevel'">
                <span>{{ POSITION_LEVEL_MAP[record.positionLevel] || '基层' }}</span>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 1 ? 'success' : 'error'">{{ record.status === 1 ? '启用' : '禁用' }}</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="showEditPosition(record)">编辑</a-button>
                  <a-popconfirm title="确认删除？" @confirm="handleDeletePosition(record)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </div>
    </PageContainer>

    <!-- 部门弹窗 -->
    <a-modal v-model:open="deptModalVisible" :title="deptEditingId ? '编辑部门' : '新增部门'" width="500px" :confirm-loading="deptSaving" @ok="handleSaveDept">
      <a-form ref="deptFormRef" :model="deptForm" :rules="deptRules" layout="vertical">
        <a-form-item label="部门名称" name="departmentName" required>
          <a-input v-model:value="deptForm.departmentName" placeholder="请输入" />
        </a-form-item>
        <a-form-item label="部门编码" name="departmentCode">
          <a-input v-model:value="deptForm.departmentCode" placeholder="请输入" :disabled="!!deptEditingId" />
        </a-form-item>
        <a-form-item label="上级部门">
          <a-tree-select v-model:value="deptForm.parentId" :tree-data="deptTree" placeholder="顶级部门" allow-clear tree-default-expand-all />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="deptForm.sort" :min="0" style="width:100%" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="deptForm.description" :rows="2" placeholder="请输入" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 岗位弹窗 -->
    <a-modal v-model:open="positionModalVisible" :title="positionEditingId ? '编辑岗位' : '新增岗位'" width="500px" :confirm-loading="positionSaving" @ok="handleSavePosition">
      <a-form ref="positionFormRef" :model="positionForm" :rules="positionRules" layout="vertical">
        <a-form-item label="岗位名称" name="positionName" required>
          <a-input v-model:value="positionForm.positionName" placeholder="请输入" />
        </a-form-item>
        <a-form-item label="岗位编码" name="positionCode">
          <a-input v-model:value="positionForm.positionCode" placeholder="请输入" :disabled="!!positionEditingId" />
        </a-form-item>
        <a-form-item label="岗位级别">
          <a-select v-model:value="positionForm.positionLevel">
            <a-select-option :value="1">高管</a-select-option>
            <a-select-option :value="2">中层</a-select-option>
            <a-select-option :value="3">基层</a-select-option>
            <a-select-option :value="4">普通</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="编制人数">
          <a-input-number v-model:value="positionForm.quotaCount" :min="1" style="width:100%" />
        </a-form-item>
        <a-form-item label="岗位职责">
          <a-textarea v-model:value="positionForm.responsibility" :rows="3" placeholder="请输入" />
        </a-form-item>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { departmentApi, type DepartmentInfo } from '@/api/department'
import { hrPositionApi, POSITION_LEVEL_MAP } from '@/api/hr'

// ── 部门树 ──
const deptTree = ref<any[]>([])
const selectedDeptId = ref<number | undefined>(undefined)
const selectedDeptName = ref('')

async function loadDeptTree() {
  try {
    const res = (await departmentApi.getList()) as any
    const list: DepartmentInfo[] = Array.isArray(res) ? res : res?.data || []
    deptTree.value = buildTree(list)
  } catch { deptTree.value = [] }
}

function buildTree(list: DepartmentInfo[]): any[] {
  const map = new Map<number, any>()
  const roots: any[] = []
  for (const d of list) {
    map.set(d.id, { ...d, title: d.departmentName, key: d.id, value: d.id, children: [] })
  }
  for (const item of map.values()) {
    if (item.parentId && map.has(item.parentId)) {
      map.get(item.parentId)?.children.push(item)
    } else {
      roots.push(item)
    }
  }
  return roots
}

function onDeptSelect(keys: any[]) {
  if (keys.length) {
    selectedDeptId.value = keys[0] as number
    selectedDeptName.value = (deptTree.value as any).find((t: any) => t.key === keys[0])?.title || ''
  } else {
    selectedDeptId.value = undefined
    selectedDeptName.value = ''
  }
  loadPositions()
}

// ── 岗位列表 ──
const positionLoading = ref(false)
const positionData = ref<any[]>([])
const positionPagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })

const positionColumns: any[] = [
  { title: '岗位编码', dataIndex: 'positionCode', key: 'positionCode', width: 120 },
  { title: '岗位名称', dataIndex: 'positionName', key: 'positionName', width: 150 },
  { title: '岗位级别', dataIndex: 'positionLevel', key: 'positionLevel', width: 100 },
  { title: '编制人数', dataIndex: 'quotaCount', key: 'quotaCount', width: 80 },
  { title: '在岗人数', dataIndex: 'currentCount', key: 'currentCount', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
]

async function loadPositions() {
  positionLoading.value = true
  try {
    const params: any = { pageNum: positionPagination.current, pageSize: positionPagination.pageSize }
    if (selectedDeptId.value) params.deptId = selectedDeptId.value
    const result = await hrPositionApi.page(params)
    positionData.value = result.records
    positionPagination.total = result.total
  } finally { positionLoading.value = false }
}

function handlePositionChange(p: any) { positionPagination.current = p.current; positionPagination.pageSize = p.pageSize; loadPositions() }

// ── 部门弹窗 ──
const deptModalVisible = ref(false)
const deptEditingId = ref<number | null>(null)
const deptSaving = ref(false)
const deptFormRef = ref()
const deptForm = reactive({ departmentName: '', departmentCode: '', parentId: undefined as number | undefined, sort: 0, description: '', status: 1 })
const deptRules: Record<string, any> = { departmentName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

function showDeptModal() {
  deptEditingId.value = null
  deptForm.departmentName = ''; deptForm.departmentCode = ''; deptForm.parentId = undefined; deptForm.sort = 0; deptForm.description = ''
  deptModalVisible.value = true
}

async function handleSaveDept() {
  try { await deptFormRef.value?.validate() } catch { return }
  deptSaving.value = true
  try {
    if (deptEditingId.value) {
      await departmentApi.update(deptEditingId.value, { ...deptForm })
      message.success('部门更新成功')
    } else {
      await departmentApi.create({ ...deptForm })
      message.success('部门创建成功')
    }
    deptModalVisible.value = false
    loadDeptTree()
  } catch { message.error('操作失败') }
  finally { deptSaving.value = false }
}

// ── 岗位弹窗 ──
const positionModalVisible = ref(false)
const positionEditingId = ref<number | null>(null)
const positionSaving = ref(false)
const positionFormRef = ref()
const positionForm = reactive({ positionCode: '', positionName: '', positionLevel: 3, quotaCount: 1, responsibility: '' })
const positionRules: Record<string, any> = { positionName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }] }

function showPositionModal() {
  positionEditingId.value = null
  positionForm.positionCode = ''; positionForm.positionName = ''; positionForm.positionLevel = 3; positionForm.quotaCount = 1; positionForm.responsibility = ''
  positionModalVisible.value = true
}

function showEditPosition(record: any) {
  positionEditingId.value = record.id
  Object.assign(positionForm, { positionCode: record.positionCode, positionName: record.positionName, positionLevel: record.positionLevel, quotaCount: record.quotaCount, responsibility: record.responsibility || '' })
  positionModalVisible.value = true
}

async function handleSavePosition() {
  try { await positionFormRef.value?.validate() } catch { return }
  positionSaving.value = true
  try {
    if (positionEditingId.value) {
      await hrPositionApi.update(positionEditingId.value, { ...positionForm })
      message.success('岗位更新成功')
    } else {
      await hrPositionApi.create({ ...positionForm })
      message.success('岗位创建成功')
    }
    positionModalVisible.value = false
    loadPositions()
  } catch { message.error('操作失败') }
  finally { positionSaving.value = false }
}

async function handleDeletePosition(record: any) {
  try {
    await hrPositionApi.delete(record.id)
    message.success('删除成功')
    loadPositions()
  } catch { message.error('删除失败') }
}

onMounted(() => { loadDeptTree(); loadPositions() })
</script>

<style scoped>
.org-layout { display: flex; height: calc(100vh - 140px); gap: 16px; }
.org-sidebar { width: 280px; background: #fff; border-radius: 8px; padding: 16px; overflow-y: auto; }
.sidebar-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.org-main { flex: 1; background: #fff; border-radius: 8px; padding: 16px; overflow-y: auto; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
</style>
