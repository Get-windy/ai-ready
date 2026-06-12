<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="dept-page-header">
        <div class="dept-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>部门管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="dept-page-header-title">部门管理</h2>
        </div>
        <div class="dept-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl</kbd> + <kbd>N</kbd> 新增</span>
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchTreeData)()">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="department-management">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ departmentCount }}</div>
            <div class="stat-card-label">部门总数</div>
          </div>
          <ApartmentOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-active">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ activeCount }}</div>
            <div class="stat-card-label">正常部门</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-disabled">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ disabledCount }}</div>
            <div class="stat-card-label">停用部门</div>
          </div>
          <StopOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-leaders">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ leaderCount }}</div>
            <div class="stat-card-label">有负责人</div>
          </div>
          <UserOutlined class="stat-card-icon" />
        </div>
      </div>

    <!-- 顶部工具栏 -->
    <a-card
      class="toolbar-card"
      :bordered="false"
    >
      <a-row
        :gutter="16"
        align="middle"
      >
        <a-col :span="16">
          <a-space>
            <a-input-search
              v-model:value="searchKeyword"
              placeholder="搜索部门名称..."
              style="width: 300px"
              allow-clear
              @search="handleSearch"
            />
            <a-button @click="handleExpandAll">
              <template #icon>
                <ExpandOutlined />
              </template>
              展开全部
            </a-button>
            <a-button @click="handleCollapseAll">
              <template #icon>
                <CompressOutlined />
              </template>
              折叠全部
            </a-button>
          </a-space>
        </a-col>
        <a-col
          :span="8"
          style="text-align: right"
        >
          <a-space>
            <a-button
              type="primary"
              :loading="submittingLoading"
              v-permission="'department:create'"
              @click="handleAddRoot"
            >
              <template #icon>
                <PlusOutlined />
              </template>
              新增根部门
            </a-button>
            <a-button @click="handleManagePersonnel">
              <template #icon>
                <TeamOutlined />
              </template>
              人员管理
            </a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>

    <!-- 部门树 -->
    <a-card
      class="tree-card"
      :bordered="false"
    >
      <a-skeleton active v-if="treeLoading && treeData.length === 0" :paragraph="{ rows: 8 }" style="padding: 24px;" />
      <a-spin :spinning="treeLoading">
        <a-tree
          v-model:expanded-keys="expandedKeys"
          v-model:selected-keys="selectedKeys"
          :tree-data="(filteredTreeData as any)"
          :field-names="{ children: 'children', title: 'departmentName', key: 'id' }"
          show-line
          draggable
          block-node
          @drop="handleDrop"
          @select="handleSelect"
        >
          <template #title="{ departmentName, status, leaderName }">
            <span class="tree-node-title">
              <a-tag
                v-if="status === 1"
                color="error"
                size="small"
              >停用</a-tag>
              {{ departmentName }}
              <span
                v-if="leaderName"
                class="leader-name"
              >({{ leaderName }})</span>
            </span>
          </template>

          <template #switcherIcon="{ expanded }">
            <DownOutlined v-if="expanded" />
            <RightOutlined v-else />
          </template>
        </a-tree>

        <a-empty
          v-if="!filteredTreeData.length && !treeLoading && !hasError"
          :description="searchKeyword ? '未找到匹配的部门' : '暂无部门数据'"
        >
          <template #extra v-if="!searchKeyword">
            <a-button type="primary" size="small" v-permission="'department:create'" @click="handleAddRoot">
              <template #icon><PlusOutlined /></template>
              新增根部门
            </a-button>
          </template>
        </a-empty>
        <a-result v-else-if="!filteredTreeData.length && !treeLoading && hasError" status="error" title="数据加载失败">
          <template #extra>
            <a-button type="primary" @click="debounceClick('refresh', fetchTreeData)()">
              <template #icon><ReloadOutlined /></template>
              重新加载
            </a-button>
          </template>
        </a-result>
      </a-spin>
    </a-card>

    <!-- 部门表单弹窗 -->
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
        :rules="formRules as any"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="上级部门"
          name="parentId"
        >
          <a-tree-select
            v-model:value="formState.parentId"
            :tree-data="parentTreeData"
            :field-names="{ children: 'children', label: 'departmentName', value: 'id' }"
            placeholder="请选择上级部门（不选择则为根部门）"
            allow-clear
            show-search
            tree-default-expand-all
            tree-node-filter-prop="departmentName"
          />
        </a-form-item>
        <a-form-item
          label="部门编码"
          name="departmentCode"
        >
          <a-input
            v-model:value="formState.departmentCode"
            placeholder="请输入部门编码"
            :disabled="isEdit"
          />
        </a-form-item>
        <a-form-item
          label="部门名称"
          name="departmentName"
        >
          <a-input
            v-model:value="formState.departmentName"
            placeholder="请输入部门名称"
          />
        </a-form-item>
        <a-form-item
          label="负责人"
          name="leaderId"
        >
          <a-select
            v-model:value="formState.leaderId"
            placeholder="请选择负责人"
            size="small"
            allow-clear
            show-search
            :filter-option="filterLeaderOption"
          >
            <a-select-option
              v-for="leader in leaderList"
              :key="leader.id"
              :value="leader.id"
            >
              {{ leader.nickname || leader.username }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          label="联系电话"
          name="phone"
        >
          <a-input
            v-model:value="formState.phone"
            placeholder="请输入联系电话"
          />
        </a-form-item>
        <a-form-item
          label="邮箱"
          name="email"
        >
          <a-input
            v-model:value="formState.email"
            placeholder="请输入邮箱"
          />
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
            placeholder="请输入部门描述"
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

    <!-- 右键菜单 -->
    <a-dropdown
      v-model:open="contextMenuVisible"
      :trigger="['contextmenu']"
    >
      <div class="context-menu-placeholder" />
      <template #overlay>
        <a-menu @click="handleContextMenuClick as any">
          <a-menu-item key="add" v-permission="'department:create'">
            <PlusOutlined /> 新增子部门
          </a-menu-item>
          <a-menu-item key="edit" v-permission="'department:edit'">
            <EditOutlined /> 编辑部门
          </a-menu-item>
          <a-menu-item key="move">
            <DragOutlined /> 移动部门
          </a-menu-item>
          <a-menu-divider />
          <a-menu-item key="toggle-status" v-permission="'department:edit'">
            <StopOutlined /> {{ contextMenuNode?.status === 0 ? '停用' : '启用' }}
          </a-menu-item>
          <a-menu-item
            key="delete"
            danger
            v-permission="'department:delete'"
          >
            <DeleteOutlined /> 删除部门
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { onBeforeRouteLeave } from 'vue-router'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined,
  TeamOutlined,
  ExpandOutlined,
  CompressOutlined,
  DownOutlined,
  RightOutlined,
  EditOutlined,
  DeleteOutlined,
  StopOutlined,
  DragOutlined,
  ApartmentOutlined,
  CheckCircleOutlined,
  UserOutlined,
  ReloadOutlined,
  SyncOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { departmentApi, type DepartmentInfo } from '@/api/department'
import { userApi, type UserInfo } from '@/api/user'
import { useSubmitLock } from '@/composables'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const router = useRouter()

// 自动刷新
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

// 搜索关键词
const searchKeyword = ref('')

// 树数据
const treeData = ref<DepartmentInfo[]>([])
const treeLoading = ref(false)
const expandedKeys = ref<number[]>([])
const selectedKeys = ref<number[]>([])

// ── 统计数据 ────────────────────────────────────────────
const flattenDepartments = (tree: DepartmentInfo[]): DepartmentInfo[] => {
  const result: DepartmentInfo[] = []
  const traverse = (nodes: DepartmentInfo[]) => {
    for (const node of nodes) {
      result.push(node)
      if (node.children?.length) traverse(node.children)
    }
  }
  traverse(tree)
  return result
}
const departmentCount = computed(() => flattenDepartments(treeData.value).length)
const activeCount = computed(() => flattenDepartments(treeData.value).filter(d => d.status === 0).length)
const disabledCount = computed(() => flattenDepartments(treeData.value).filter(d => d.status === 1).length)
const leaderCount = computed(() => flattenDepartments(treeData.value).filter(d => d.leaderId).length)

// 根据搜索关键词过滤树数据（保留匹配节点的父级路径）
const filterTree = (nodes: DepartmentInfo[], keyword: string): DepartmentInfo[] => {
  if (!keyword) return nodes
  const lowerKeyword = keyword.toLowerCase()
  const result: DepartmentInfo[] = []
  for (const node of nodes) {
    const nameMatch = node.departmentName?.toLowerCase().includes(lowerKeyword)
    const filteredChildren = node.children ? filterTree(node.children, keyword) : []
    if (nameMatch || filteredChildren.length > 0) {
      result.push({ ...node, children: filteredChildren.length > 0 ? filteredChildren : node.children })
    }
  }
  return result
}

// 收集所有匹配搜索关键词的节点ID（用于自动展开）
const collectMatchedIds = (nodes: DepartmentInfo[], keyword: string): number[] => {
  if (!keyword) return []
  const ids: number[] = []
  const lowerKeyword = keyword.toLowerCase()
  const traverse = (items: DepartmentInfo[]) => {
    items.forEach(item => {
      const nameMatch = item.departmentName?.toLowerCase().includes(lowerKeyword)
      const childMatches = item.children ? collectMatchedIds(item.children, keyword) : []
      if (nameMatch || childMatches.length > 0) {
        ids.push(item.id, ...childMatches)
      }
    })
  }
  traverse(nodes)
  return ids
}

// 过滤后的树数据
const filteredTreeData = computed(() => {
  return filterTree(treeData.value, searchKeyword.value)
})

// 弹窗相关
const modalVisible = ref(false)
const { isSubmitting: submittingLoading, withSubmitLock } = useSubmitLock()
const modalTitle = computed(() => isEdit.value ? '编辑部门' : '新增部门')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive<Partial<DepartmentInfo>>({
  id: 0,
  parentId: undefined,
  departmentCode: '',
  departmentName: '',
  leaderId: undefined,
  phone: '',
  email: '',
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
  departmentName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  departmentCode: [{ required: true, message: '请输入部门编码', trigger: 'blur' }]
}

// 父部门树数据（用于选择上级部门）
const parentTreeData = ref<DepartmentInfo[]>([])

// 负责人列表
const leaderList = ref<UserInfo[]>([])

// 右键菜单
const contextMenuVisible = ref(false)
const contextMenuNode = ref<DepartmentInfo | null>(null)

// 数据加载
const fetchTreeData = async () => {
  treeLoading.value = true
  hasError.value = false
  try {
    const res = await departmentApi.getTree({ tenantId: userStore.tenantId })
    if (res.data) {
      treeData.value = res.data
      // 默认展开所有节点
      expandedKeys.value = getAllNodeIds(res.data)
    }
  } catch (error) {
    hasError.value = true
    console.warn('[部门管理] 加载部门数据失败', error)
    message.error('加载部门数据失败')
  } finally {
    treeLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

// 获取所有节点ID
const getAllNodeIds = (nodes: DepartmentInfo[]): number[] => {
  const ids: number[] = []
  const traverse = (items: DepartmentInfo[]) => {
    items.forEach(item => {
      ids.push(item.id)
      if (item.children && item.children.length > 0) {
        traverse(item.children)
      }
    })
  }
  traverse(nodes)
  return ids
}

// 加载父部门树
const fetchParentTreeData = async () => {
  try {
    const res = await departmentApi.getTree({ tenantId: userStore.tenantId, status: 0 })
    if (res.data) {
      parentTreeData.value = res.data
    }
  } catch (error) {
    console.warn('[系统管理] 加载父部门数据失败', error)
  }
}

// 加载负责人列表
const fetchLeaderList = async () => {
  try {
    const res = await userApi.getList({ tenantId: userStore.tenantId, status: 0, pageSize: 1000 })
    if (res.data) {
      leaderList.value = res.data
    }
  } catch (error) {
    console.warn('[系统管理] 加载负责人列表失败', error)
  }
}

// 搜索
const handleSearch = () => {
  if (searchKeyword.value) {
    // 自动展开匹配关键词的节点
    expandedKeys.value = collectMatchedIds(treeData.value, searchKeyword.value)
  } else {
    // 无关键词时展开全部
    expandedKeys.value = getAllNodeIds(treeData.value)
  }
}

// 展开全部
const handleExpandAll = () => {
  expandedKeys.value = getAllNodeIds(treeData.value)
}

// 折叠全部
const handleCollapseAll = () => {
  expandedKeys.value = []
}

// 选择节点
const handleSelect = (keys: any) => {
  selectedKeys.value = keys
}

// 新增根部门
const handleAddRoot = () => {
  isEdit.value = false
  Object.assign(formState, {
    id: 0,
    parentId: undefined,
    departmentCode: '',
    departmentName: '',
    leaderId: undefined,
    phone: '',
    email: '',
    sort: 0,
    description: '',
    status: 0
  })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 拖拽处理
const handleDrop = async (info: any) => {
  const { dragNode, node, dropPosition } = info
  const dragId = dragNode.key as number
  const targetId = node.key as number

  // 确定移动位置
  let position: 'before' | 'after' | 'inner' = 'inner'
  if (dropPosition === -1) {
    position = 'inner'
  } else if (dropPosition === 1) {
    position = 'after'
  } else {
    position = 'before'
  }

  try {
    await departmentApi.move(dragId, targetId, position)
    message.success('移动成功')
    fetchTreeData()
  } catch (error) {
    message.error('移动失败')
  }
}

// 右键菜单点击
const handleContextMenuClick = async ({ key }: { key: string }) => {
  contextMenuVisible.value = false

  if (!contextMenuNode.value) return

  switch (key) {
    case 'add':
      handleAddChild(contextMenuNode.value)
      break
    case 'edit':
      handleEdit(contextMenuNode.value)
      break
    case 'move':
      message.info('拖拽节点即可移动部门')
      break
    case 'toggle-status':
      await handleToggleStatus(contextMenuNode.value)
      break
    case 'delete':
      handleDelete(contextMenuNode.value)
      break
  }
}

// 新增子部门
const handleAddChild = (parentNode: DepartmentInfo) => {
  isEdit.value = false
  Object.assign(formState, {
    id: 0,
    parentId: parentNode.id,
    departmentCode: '',
    departmentName: '',
    leaderId: undefined,
    phone: '',
    email: '',
    sort: 0,
    description: '',
    status: 0
  })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 编辑部门
const handleEdit = (node: DepartmentInfo) => {
  isEdit.value = true
  Object.assign(formState, node)
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// 提交表单
const handleModalOk = async () => {
  try {
    const result = await withSubmitLock(async () => {
      await formRef.value?.validate()

      if (isEdit.value) {
        await departmentApi.update(formState.id!, formState)
        message.success('更新成功')
      } else {
        await departmentApi.create(formState)
        message.success('创建成功')
      }

      modalVisible.value = false
      fetchTreeData()
      fetchParentTreeData()
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

// 切换状态
const handleToggleStatus = async (node: DepartmentInfo) => {
  const newStatus = node.status === 0 ? 1 : 0
  await departmentApi.updateStatus(node.id, newStatus)
  message.success('状态更新成功')
  fetchTreeData()
  fetchParentTreeData()
}

// 删除部门
const handleDelete = (node: DepartmentInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除部门 "${node.departmentName}" 吗？${node.children?.length ? '删除后子部门也会被删除。' : ''}`,
    async onOk() {
      await departmentApi.delete(node.id)
      message.success('删除成功')
      fetchTreeData()
      fetchParentTreeData()
    }
  })
}

// 人员管理
const handleManagePersonnel = () => {
  if (selectedKeys.value.length === 0) {
    message.warning('请先选择一个部门')
    return
  }
  router.push({ path: '/system/department/personnel', query: { deptId: selectedKeys.value[0] } })
}

// 过滤负责人
const filterLeaderOption = (input: string, option: any) => {
  const leader = leaderList.value.find(l => l.id === option.value)
  if (!leader) return false
  const name = leader.nickname || leader.username || ''
  return name.toLowerCase().includes(input.toLowerCase())
}

// 处理右键菜单 (保留以备后用)
// @ts-ignore
const handleRightClick = ({ node }: any, event: MouseEvent) => {
  event.preventDefault()
  event.stopPropagation()
  contextMenuNode.value = node.dataRef
  contextMenuVisible.value = true
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT'
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', fetchTreeData)()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !isInput) {
    e.preventDefault()
    handleAddRoot()
  }
}

onMounted(() => {
  fetchTreeData()
  fetchParentTreeData()
  fetchLeaderList()
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchTreeData()
    fetchParentTreeData()
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

defineExpose({ handleQuery: fetchTreeData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.dept-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.dept-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.dept-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.dept-page-header-right {
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

.department-management {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
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
.stat-leaders { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

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

.toolbar-card {
  margin-bottom: 16px;
}

.tree-card {
  flex: 1;
  min-height: 400px;
}

.tree-node-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.leader-name {
  color: #8c8c8c;
  font-size: 12px;
}

.context-menu-placeholder {
  position: absolute;
  visibility: hidden;
}

:deep(.ant-tree) {
  background: transparent;
}

:deep(.ant-tree-node-content-wrapper) {
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

:deep(.ant-tree-node-content-wrapper:hover) {
  background-color: #f5f5f5;
}

:deep(.ant-tree-node-selected .ant-tree-node-content-wrapper) {
  background-color: #e6f7ff;
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
