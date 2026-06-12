<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="dict-page-header">
        <div class="dict-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>字典管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="dict-page-header-title">字典管理</h2>
        </div>
        <div class="dict-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl</kbd> + <kbd>N</kbd> 新增</span>
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchTypeData)()">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="dict-management">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ typePagination.total }}</div>
            <div class="stat-card-label">类型总数</div>
          </div>
          <BookOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-items">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ totalItemCount }}</div>
            <div class="stat-card-label">字典项总数</div>
          </div>
          <UnorderedListOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-enabled">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ enabledTypeCount }}</div>
            <div class="stat-card-label">启用类型</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-disabled">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ disabledTypeCount }}</div>
            <div class="stat-card-label">停用类型</div>
          </div>
          <StopOutlined class="stat-card-icon" />
        </div>
      </div>

      <a-skeleton active v-if="typeLoading && typeTableData.length === 0" :paragraph="{ rows: 8 }" style="padding: 24px;" />

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="typeTableData"
        :loading="typeLoading"
        :pagination="typePagination"
        :row-key="'id'"
        :min-empty-rows="12"
        :filter-fields="filterFields"
        :show-search="false"
        :show-add="false"
        :show-edit="false"
        :show-delete="false"
        :show-batch-delete="false"
        :selectable="false"
        add-text="新增类型"
        @add="handleAddType"
        @refresh="debounceClick('refresh', fetchTypeData)"
        @page-change="handleTypePageChange"
        @filter-change="handleFilterChange"
        @cell-dblclick="handleView"
      >
        <template #toolbar-actions>
          <a-button type="primary" v-permission="'system:dict:create'" @click="handleAddType">
            <template #icon><PlusOutlined /></template>
            新增类型
          </a-button>
        </template>

        <template #empty>
          <a-empty v-if="!hasError" description="暂无数据" />
          <a-result v-else status="error" title="数据加载失败">
            <template #extra>
              <a-button type="primary" @click="debounceClick('refresh', fetchTypeData)()">
                <template #icon><ReloadOutlined /></template>
                重新加载
              </a-button>
            </template>
          </a-result>
        </template>

        <template #statusCell="{ record }">
          <a-tag :color="record.status === 'ENABLED' ? 'success' : 'error'">
            {{ record.status === 'ENABLED' ? '启用' : '停用' }}
          </a-tag>
        </template>

        <template #action="{ record }">
          <a-space>
            <a-button type="link" size="small" v-permission="'system:dict:update'" @click="handleEditType(record)">
              编辑
            </a-button>
            <a-button type="link" size="small" danger v-permission="'system:dict:delete'" @click="handleDeleteTypeConfirm(record)">
              删除
            </a-button>
          </a-space>
        </template>

        <template #expandedRowRender="{ record }">
          <div class="expanded-content">
            <div class="expanded-header">
              <span class="expanded-title">字典项列表</span>
              <a-button type="primary" size="small" v-permission="'system:dict:create'" @click="handleAddItem(record)">
                <template #icon><PlusOutlined /></template>
                新增字典项
              </a-button>
            </div>
            <VxeTableList
              :data-source="dictItemMap[record.id] || []"
              :loading="itemLoadingMap[record.id]"
              :pagination="false as any"
              row-key="id"
              :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false"
              :show-export="false" :show-batch-delete="false"
              :columns="itemColumns"
            >
              <template #statusCell="{ record: itemRecord }">
                <a-tag :color="itemRecord.status === 'ENABLED' ? 'success' : 'error'">
                  {{ itemRecord.status === 'ENABLED' ? '启用' : '停用' }}
                </a-tag>
              </template>

              <template #actionCell="{ record: itemRecord }">
                <a-space>
                  <a-button type="link" size="small" v-permission="'system:dict:update'" @click="handleEditItem(record, itemRecord)">
                    编辑
                  </a-button>
                  <a-button type="link" size="small" danger v-permission="'system:dict:delete'" @click="handleDeleteItemConfirm(record, itemRecord)">
                    删除
                  </a-button>
                </a-space>
              </template>
            </VxeTableList>
          </div>
        </template>
      </VxeTableList>

      <!-- 字典类型表单弹窗 -->
      <FullScreenDetail
        :visible="typeModalVisible"
        :title="typeModalTitle"
        :dirty="typeFormDirty"
        :save-loading="typeModalLoading"
        :show-save-and-new="!isTypeEdit"
        @save="handleTypeModalOk"
        @close="handleTypeFormClose"
        @save-and-new="handleTypeFormSaveAndNew"
      >
        <a-form
          ref="typeFormRef"
          :model="typeFormState"
          :rules="typeFormRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="类型编码" name="dictCode">
            <a-input
              v-model:value="typeFormState.dictCode"
              placeholder="请输入类型编码"
              :disabled="isTypeEdit"
            />
          </a-form-item>
          <a-form-item label="类型名称" name="dictName">
            <a-input
              v-model:value="typeFormState.dictName"
              placeholder="请输入类型名称"
            />
          </a-form-item>
          <a-form-item label="状态" name="status">
            <a-radio-group v-model:value="typeFormState.status">
              <a-radio value="ENABLED">启用</a-radio>
              <a-radio value="DISABLED">停用</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="备注" name="remark">
            <a-textarea
              v-model:value="typeFormState.remark"
              placeholder="请输入备注"
              :rows="3"
            />
          </a-form-item>
        </a-form>
      </FullScreenDetail>

      <!-- 字典项表单弹窗 -->
      <FullScreenDetail
        :visible="itemModalVisible"
        :title="itemModalTitle"
        :dirty="itemFormDirty"
        :save-loading="itemModalLoading"
        :show-save-and-new="!isItemEdit"
        @save="handleItemModalOk"
        @close="handleItemFormClose"
        @save-and-new="handleItemFormSaveAndNew"
      >
        <a-form
          ref="itemFormRef"
          :model="itemFormState"
          :rules="itemFormRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="字典项值" name="itemValue">
            <a-input
              v-model:value="itemFormState.itemValue"
              placeholder="请输入字典项值"
            />
          </a-form-item>
          <a-form-item label="字典项文本" name="itemText">
            <a-input
              v-model:value="itemFormState.itemText"
              placeholder="请输入字典项文本"
            />
          </a-form-item>
          <a-form-item label="排序" name="sortOrder">
            <a-input-number
              v-model:value="itemFormState.sortOrder"
              :min="0"
              :max="9999"
              style="width: 100%"
              placeholder="请输入排序号"
            />
          </a-form-item>
          <a-form-item label="状态" name="status">
            <a-radio-group v-model:value="itemFormState.status">
              <a-radio value="ENABLED">启用</a-radio>
              <a-radio value="DISABLED">停用</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-form>
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
import { PlusOutlined, BookOutlined, UnorderedListOutlined, CheckCircleOutlined, StopOutlined, SyncOutlined, ReloadOutlined, WarningOutlined } from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { dictTypeApi, dictItemApi, type DictType, type DictItem } from '@/api/dict'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

// ── 离开守卫 ────────────────────────────────────────────
const formDirty = ref(false)
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

// ==================== 字典类型相关 ====================

// 搜索
const typeSearchForm = reactive({
  dictCode: '',
  dictName: '',
  status: undefined as string | undefined
})

// 表格
const typeTableData = ref<DictType[]>([])
const typeLoading = ref(false)
const typePagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 统计数据 ────────────────────────────────────────────
const enabledTypeCount = computed(() => typeTableData.value.filter(r => r.status === 'ENABLED').length)
const disabledTypeCount = computed(() => typeTableData.value.filter(r => r.status === 'DISABLED').length)
const totalItemCount = computed(() => {
  let count = 0
  for (const items of Object.values(dictItemMap)) {
    count += items.length
  }
  return count
})

const vxeColumns = computed(() => [
  { field: 'dictCode', title: '类型编码', width: 160 },
  { field: 'dictName', title: '类型名称', width: 160 },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { field: 'remark', title: '备注', width: 200, showOverflow: 'tooltip' },
  { field: 'createTime', title: '创建时间', width: 160 },
  { type: 'action', title: '操作', width: 140, fixed: 'right' }
])

// 筛选字段
const filterFields: FilterField[] = [
  { key: 'dictCode', label: '类型编码', type: 'input', placeholder: '请输入类型编码' },
  { key: 'dictName', label: '类型名称', type: 'input', placeholder: '请输入类型名称' },
  { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 'ENABLED' }, { label: '停用', value: 'DISABLED' }] },
]

// 弹窗
const typeModalVisible = ref(false)
const typeModalLoading = ref(false)
const isTypeEdit = ref(false)
const typeFormRef = ref<FormInstance>()
const typeModalTitle = computed(() => isTypeEdit.value ? '编辑字典类型' : '新增字典类型')

const typeFormState = reactive({
  id: 0,
  dictCode: '',
  dictName: '',
  status: 'ENABLED',
  remark: ''
})

// ── 类型表单脏检测 ─────────────────────────────────────
const initialTypeFormSnapshot = ref('')
let watchReadyType = false
const typeFormDirty = computed(() => {
  if (!watchReadyType) return false
  return JSON.stringify(typeFormState) !== initialTypeFormSnapshot.value
})
function saveTypeFormSnapshot() { initialTypeFormSnapshot.value = JSON.stringify(typeFormState) }

const typeFormRules: any = {
  dictCode: { required: true, message: '请输入类型编码', trigger: 'blur' },
  dictName: { required: true, message: '请输入类型名称', trigger: 'blur' }
}

// 数据加载
const fetchTypeData = async () => {
  typeLoading.value = true
  hasError.value = false
  try {
    const res = await dictTypeApi.getPage({
      ...typeSearchForm,
      pageNum: typePagination.current,
      pageSize: typePagination.pageSize
    })
    if (res.data) {
      typeTableData.value = res.records
      typePagination.total = res.total
    }
  } catch (error) {
    hasError.value = true
    typeTableData.value = []
    typePagination.total = 0
    console.warn('[字典管理] 加载字典类型失败')
    message.error('加载字典类型失败')
  } finally {
    typeLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

const handleTypeSearch = () => {
  typePagination.current = 1
  fetchTypeData()
}

const handleTypeReset = () => {
  Object.assign(typeSearchForm, { dictCode: '', dictName: '', status: undefined })
  handleTypeSearch()
}

// 筛选变化
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(typeSearchForm, { dictCode: '', dictName: '', status: undefined })
  } else {
    Object.assign(typeSearchForm, filters)
  }
  typePagination.current = 1
  fetchTypeData()
}

// 分页变化
const handleTypePageChange = (page: number, pageSize: number) => {
  typePagination.current = page
  typePagination.pageSize = pageSize
  fetchTypeData()
}

// 新增类型
const handleAddType = () => {
  isTypeEdit.value = false
  Object.assign(typeFormState, { id: 0, dictCode: '', dictName: '', status: 'ENABLED', remark: '' })
  typeModalVisible.value = true
  nextTick(() => { saveTypeFormSnapshot(); watchReadyType = true })
  updateFormDirty()
}

// 编辑类型
const handleEditType = (record: DictType) => {
  isTypeEdit.value = true
  Object.assign(typeFormState, {
    id: record.id,
    dictCode: record.dictCode,
    dictName: record.dictName,
    status: record.status,
    remark: record.remark
  })
  typeModalVisible.value = true
  nextTick(() => { saveTypeFormSnapshot(); watchReadyType = true })
  updateFormDirty()
}

// 删除类型
const handleDeleteTypeConfirm = (record: DictType) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除字典类型 "${record.dictName}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await dictTypeApi.delete(record.id)
        message.success('删除成功')
        fetchTypeData()
      } catch (err) {
        console.warn('[系统管理] 删除字典类型失败', err)
        message.error('删除失败')
      }
    }
  })
}

// 提交类型表单
const handleTypeModalOk = async () => {
  try {
    await typeFormRef.value?.validate()
    typeModalLoading.value = true

    if (isTypeEdit.value) {
      await dictTypeApi.update(typeFormState as any)
      message.success('更新成功')
    } else {
      await dictTypeApi.create(typeFormState as any)
      message.success('创建成功')
    }

    typeModalVisible.value = false
    fetchTypeData()
  } catch (error) {
    message.error('操作失败')
  } finally {
    typeModalLoading.value = false
  }
}

const handleTypeFormClose = () => {
  if (typeFormDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '您有未保存的修改，确定要关闭吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => { typeModalVisible.value = false; typeFormRef.value?.resetFields(); updateFormDirty() },
    })
  } else {
    typeModalVisible.value = false
    typeFormRef.value?.resetFields()
    updateFormDirty()
  }
}

const handleTypeFormSaveAndNew = () => {
  handleTypeModalOk()
}

function updateFormDirty() {
  formDirty.value = typeFormDirty.value || itemFormDirty.value
}

// ==================== 字典项相关 ====================

const dictItemMap = reactive<Record<number, DictItem[]>>({})
const itemLoadingMap = reactive<Record<number, boolean>>({})

const itemColumns: any[] = [
  { title: '字典项值', field: 'itemValue', width: 150 },
  { title: '字典项文本', field: 'itemText', width: 180 },
  { title: '排序', field: 'sortOrder', width: 80 },
  { title: '状态', field: 'status', width: 80, slotName: 'statusCell' },
  { title: '操作', field: 'action', width: 130, slotName: 'actionCell' }
]

const itemModalVisible = ref(false)
const itemModalLoading = ref(false)
const currentDictType = ref<DictType | null>(null)
const isItemEdit = ref(false)
const itemFormRef = ref<FormInstance>()
const itemModalTitle = computed(() => isItemEdit.value ? '编辑字典项' : '新增字典项')

const itemFormState = reactive({
  id: 0,
  dictTypeId: 0,
  itemValue: '',
  itemText: '',
  sortOrder: 0,
  status: 'ENABLED'
})

// ── 字典项表单脏检测 ─────────────────────────────────
const initialItemFormSnapshot = ref('')
let watchReadyItem = false
const itemFormDirty = computed(() => {
  if (!watchReadyItem) return false
  return JSON.stringify(itemFormState) !== initialItemFormSnapshot.value
})
function saveItemFormSnapshot() { initialItemFormSnapshot.value = JSON.stringify(itemFormState) }

const itemFormRules: any = {
  itemValue: { required: true, message: '请输入字典项值', trigger: 'blur' },
  itemText: { required: true, message: '请输入字典项文本', trigger: 'blur' },
  sortOrder: { required: true, message: '请输入排序号', trigger: 'blur' }
}

// 展开行 - 加载字典项
const handleExpand = async (expanded: boolean, record: DictType) => {
  if (!expanded) return
  const typeId = record.id as number
  if (dictItemMap[typeId]) return

  itemLoadingMap[typeId] = true
  try {
    const res = await dictItemApi.getByDictTypeId(typeId)
    if (res.data) {
      dictItemMap[typeId] = res.data
    }
  } catch (err) {
    dictItemMap[typeId] = []
    console.warn('[系统管理] 加载字典项失败', err)
    message.error('加载字典项失败')
  } finally {
    itemLoadingMap[typeId] = false
  }
}

// 新增字典项
const handleAddItem = (typeRecord: DictType) => {
  isItemEdit.value = false
  currentDictType.value = typeRecord
  Object.assign(itemFormState, {
    id: 0,
    dictTypeId: typeRecord.id,
    itemValue: '',
    itemText: '',
    sortOrder: 0,
    status: 'ENABLED'
  })
  itemModalVisible.value = true
  nextTick(() => { saveItemFormSnapshot(); watchReadyItem = true })
  updateFormDirty()
}

// 编辑字典项
const handleEditItem = (typeRecord: DictType, itemRecord: DictItem) => {
  isItemEdit.value = true
  currentDictType.value = typeRecord
  Object.assign(itemFormState, {
    id: itemRecord.id,
    dictTypeId: typeRecord.id,
    itemValue: itemRecord.itemValue,
    itemText: itemRecord.itemText,
    sortOrder: itemRecord.sortOrder,
    status: itemRecord.status
  })
  itemModalVisible.value = true
  nextTick(() => { saveItemFormSnapshot(); watchReadyItem = true })
  updateFormDirty()
}

// 删除字典项
const handleDeleteItemConfirm = (typeRecord: DictType, itemRecord: DictItem) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除字典项 "${itemRecord.itemText}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await dictItemApi.delete(itemRecord.id)
        message.success('删除成功')
        // 刷新当前类型的字典项
        const res = await dictItemApi.getByDictTypeId(typeRecord.id)
        if (res.data) {
          dictItemMap[typeRecord.id] = res.data
        }
      } catch (err) {
        console.warn('[系统管理] 删除字典项失败', err)
        message.error('删除失败')
      }
    }
  })
}

// 提交字典项表单
const handleItemModalOk = async () => {
  try {
    await itemFormRef.value?.validate()
    itemModalLoading.value = true

    if (isItemEdit.value) {
      await dictItemApi.update(itemFormState as any)
      message.success('更新成功')
    } else {
      await dictItemApi.create(itemFormState as any)
      message.success('创建成功')
    }

    itemModalVisible.value = false

    // 刷新字典项
    if (currentDictType.value) {
      const res = await dictItemApi.getByDictTypeId(currentDictType.value.id)
      if (res.data) {
        dictItemMap[currentDictType.value.id] = res.data
      }
    }
  } catch (error) {
    message.error('操作失败')
  } finally {
    itemModalLoading.value = false
  }
}

const handleItemFormClose = () => {
  if (itemFormDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '您有未保存的修改，确定要关闭吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => { itemModalVisible.value = false; itemFormRef.value?.resetFields(); updateFormDirty() },
    })
  } else {
    itemModalVisible.value = false
    itemFormRef.value?.resetFields()
    updateFormDirty()
  }
}

const handleItemFormSaveAndNew = () => {
  handleItemModalOk()
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT'
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', fetchTypeData)()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !isInput) {
    e.preventDefault()
    handleAddType()
  }
}

onMounted(() => {
  fetchTypeData()
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchTypeData()
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

defineExpose({ handleQuery: fetchTypeData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
// 查看详情
const handleView = (record: any) => {}
</script>

<style scoped>
.dict-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.dict-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.dict-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.dict-page-header-right {
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

.dict-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.dict-management > :deep(.vxe-table-list-container) {
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
.stat-items { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.stat-enabled { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-disabled { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

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


.expanded-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.expanded-header .expanded-title {
  font-size: 14px;
  font-weight: 500;
  color: #1890ff;
}

/* 嵌套表格网格边框 */
:deep(.expanded-content .ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #f5f5f5 !important;
  padding: 6px 10px !important;
  font-weight: 600 !important;
}

:deep(.expanded-content .ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.expanded-content .ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 6px 10px !important;
}

:deep(.expanded-content .ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}

/* ── VxeTable 表头边框线 2px ─────────────────────────── */
:deep(.vxe-table .vxe-header--row th) {
  border-bottom: 2px solid #e8e8e8 !important;
}
:deep(.vxe-table .vxe-header--row th:not(:last-child)) {
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
