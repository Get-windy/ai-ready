<template>
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

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="typeTableDataSource"
      :loading="typeLoading"
      :pagination="typePagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :show-search="false"
      :show-add="false"
      :show-edit="false"
      :show-delete="false"
      :show-batch-delete="false"
      :selectable="false"
      add-text="新增类型"
      @add="handleAddType"
      @refresh="fetchTypeData"
      @page-change="handleTypePageChange"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-actions>
        <a-button type="primary" @click="handleAddType">
          <template #icon><PlusOutlined /></template>
          新增类型
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <template v-else-if="column.field === 'status'">
          <a-tag :color="record.status === 'ENABLED' ? 'success' : 'error'">
            {{ record.status === 'ENABLED' ? '启用' : '停用' }}
          </a-tag>
        </template>
      </template>

      <template #action="{ record }">
        <a-space>
          <a-button type="link" size="small" @click="handleEditType(record)">
            编辑
          </a-button>
          <a-button type="link" size="small" danger @click="handleDeleteTypeConfirm(record)">
            删除
          </a-button>
        </a-space>
      </template>

      <template #expandedRowRender="{ record }">
        <div class="expanded-content">
          <div class="expanded-header">
            <span class="expanded-title">字典项列表</span>
            <a-button type="primary" size="small" @click="handleAddItem(record)">
              <template #icon><PlusOutlined /></template>
              新增字典项
            </a-button>
          </div>
          <a-table
            :columns="itemColumns"
            :data-source="dictItemMap[record.id] || []"
            :loading="itemLoadingMap[record.id]"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column: itemCol, record: itemRecord }">
              <template v-if="itemCol.key === 'status'">
                <a-tag :color="itemRecord.status === 'ENABLED' ? 'success' : 'error'">
                  {{ itemRecord.status === 'ENABLED' ? '启用' : '停用' }}
                </a-tag>
              </template>

              <template v-else-if="itemCol.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="handleEditItem(record, itemRecord)">
                    编辑
                  </a-button>
                  <a-button type="link" size="small" danger @click="handleDeleteItemConfirm(record, itemRecord)">
                    删除
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </template>
    </VxeTableList>

    <!-- 字典类型表单弹窗 -->
    <a-modal
      v-model:open="typeModalVisible"
      :title="typeModalTitle"
      :confirm-loading="typeModalLoading"
      width="500px"
      @ok="handleTypeModalOk"
      @cancel="handleTypeModalCancel"
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
    </a-modal>

    <!-- 字典项表单弹窗 -->
    <a-modal
      v-model:open="itemModalVisible"
      :title="itemModalTitle"
      :confirm-loading="itemModalLoading"
      width="500px"
      @ok="handleItemModalOk"
      @cancel="handleItemModalCancel"
    >
      <a-form
        ref="itemFormRef"
        :model="itemFormState"
        :rules="itemFormRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="字典项编码" name="itemCode">
          <a-input
            v-model:value="itemFormState.itemCode"
            placeholder="请输入字典项编码"
          />
        </a-form-item>
        <a-form-item label="字典项名称" name="itemName">
          <a-input
            v-model:value="itemFormState.itemName"
            placeholder="请输入字典项名称"
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
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, BookOutlined, UnorderedListOutlined, CheckCircleOutlined, StopOutlined } from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { dictTypeApi, dictItemApi, type DictType, type DictItem } from '@/api/dict'

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

// ── 空行填充 ────────────────────────────────────────────
const MIN_TABLE_ROWS = 20
const typeTableDataSource = computed(() => {
  const data = [...typeTableData.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` })
  }
  return data
})

const vxeColumns = computed(() => [
  { field: 'dictCode', title: '类型编码', width: 160 },
  { field: 'dictName', title: '类型名称', width: 160 },
  { field: 'status', title: '状态', width: 80 },
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

const typeFormRules = {
  dictCode: { required: true, message: '请输入类型编码', trigger: 'blur' },
  dictName: { required: true, message: '请输入类型名称', trigger: 'blur' }
}

// 数据加载
const fetchTypeData = async () => {
  typeLoading.value = true
  try {
    const res = await dictTypeApi.getPage({
      ...typeSearchForm,
      pageNum: typePagination.current,
      pageSize: typePagination.pageSize
    })
    if (res.data) {
      typeTableData.value = res.data.records
      typePagination.total = res.data.total
    }
  } catch (error) {
    typeTableData.value = mockTypeData()
    typePagination.total = mockTypeData().length
  } finally {
    typeLoading.value = false
  }
}

// Mock数据
const mockTypeData = (): DictType[] => [
  { id: 1, dictCode: 'sys_normal_disable', dictName: '系统开关', status: 'ENABLED', remark: '系统开关列表', createTime: '2024-01-01' },
  { id: 2, dictCode: 'sys_user_sex', dictName: '用户性别', status: 'ENABLED', remark: '用户性别列表', createTime: '2024-01-15' },
  { id: 3, dictCode: 'sys_show_hide', dictName: '菜单状态', status: 'ENABLED', remark: '菜单状态列表', createTime: '2024-02-01' },
  { id: 4, dictCode: 'sys_job_status', dictName: '任务状态', status: 'DISABLED', remark: '任务状态列表', createTime: '2024-02-10' },
]

const mockItemData = (typeId: number): DictItem[] => {
  if (typeId === 1) return [
    { id: 1, dictTypeId: 1, itemCode: '0', itemName: '正常', sortOrder: 1, status: 'ENABLED' },
    { id: 2, dictTypeId: 1, itemCode: '1', itemName: '停用', sortOrder: 2, status: 'ENABLED' },
  ]
  if (typeId === 2) return [
    { id: 3, dictTypeId: 2, itemCode: '0', itemName: '未知', sortOrder: 1, status: 'ENABLED' },
    { id: 4, dictTypeId: 2, itemCode: '1', itemName: '男', sortOrder: 2, status: 'ENABLED' },
    { id: 5, dictTypeId: 2, itemCode: '2', itemName: '女', sortOrder: 3, status: 'ENABLED' },
  ]
  return []
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
      } catch {
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

const handleTypeModalCancel = () => {
  typeModalVisible.value = false
  typeFormRef.value?.resetFields()
}

// ==================== 字典项相关 ====================

const dictItemMap = reactive<Record<number, DictItem[]>>({})
const itemLoadingMap = reactive<Record<number, boolean>>({})

const itemColumns: any[] = [
  { title: '字典项编码', dataIndex: 'itemCode', width: 150 },
  { title: '字典项名称', dataIndex: 'itemName', width: 180 },
  { title: '排序', dataIndex: 'sortOrder', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 130 }
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
  itemCode: '',
  itemName: '',
  sortOrder: 0,
  status: 'ENABLED'
})

const itemFormRules = {
  itemCode: { required: true, message: '请输入字典项编码', trigger: 'blur' },
  itemName: { required: true, message: '请输入字典项名称', trigger: 'blur' },
  sortOrder: { required: true, message: '请输入排序号', trigger: 'blur' }
}

// 展开行 - 加载字典项
const handleExpand = async (expanded: boolean, record: DictType) => {
  if (!expanded || record.__empty_row) return
  const typeId = record.id as number
  if (dictItemMap[typeId]) return

  itemLoadingMap[typeId] = true
  try {
    const res = await dictItemApi.getByDictTypeId(typeId)
    if (res.data) {
      dictItemMap[typeId] = res.data
    }
  } catch {
    dictItemMap[typeId] = mockItemData(typeId)
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
    itemCode: '',
    itemName: '',
    sortOrder: 0,
    status: 'ENABLED'
  })
  itemModalVisible.value = true
}

// 编辑字典项
const handleEditItem = (typeRecord: DictType, itemRecord: DictItem) => {
  isItemEdit.value = true
  currentDictType.value = typeRecord
  Object.assign(itemFormState, {
    id: itemRecord.id,
    dictTypeId: typeRecord.id,
    itemCode: itemRecord.itemCode,
    itemName: itemRecord.itemName,
    sortOrder: itemRecord.sortOrder,
    status: itemRecord.status
  })
  itemModalVisible.value = true
}

// 删除字典项
const handleDeleteItemConfirm = (typeRecord: DictType, itemRecord: DictItem) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除字典项 "${itemRecord.itemName}" 吗？此操作不可撤销。`,
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
      } catch {
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

const handleItemModalCancel = () => {
  itemModalVisible.value = false
  itemFormRef.value?.resetFields()
}

onMounted(() => {
  fetchTypeData()
})
</script>

<style scoped>
.dict-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
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

.empty-placeholder { color: transparent; }

.expanded-content {
  padding: 12px 0;
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

/* 表格网格边框 - 主表 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
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

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>
