<template>
  <div class="dict-management">
    <TableList
      ref="tableRef"
      :columns="typeColumns"
      :data-source="typeTableData"
      :loading="typeLoading"
      :pagination="typePagination"
      :table-key="'system-dict-list'"
      :filter-fields="filterFields"
      :show-search="false"
      :show-add="false"
      :show-edit="false"
      :show-delete="false"
      :show-batch-delete="false"
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
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'ENABLED' ? 'success' : 'error'">
            {{ record.status === 'ENABLED' ? '启用' : '停用' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEditType(record)">
              编辑
            </a-button>
            <a-button type="link" size="small" danger @click="handleDeleteTypeConfirm(record)">
              删除
            </a-button>
          </a-space>
        </template>
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
    </TableList>

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
import { PlusOutlined } from '@ant-design/icons-vue'
import TableList, { type FilterField } from '@/components/TableList/TableList.vue'
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

const typeColumns: any[] = [
  { title: '类型编码', dataIndex: 'dictCode', width: 160 },
  { title: '类型名称', dataIndex: 'dictName', width: 160 },
  { title: '状态', key: 'status', width: 80 },
  { title: '备注', dataIndex: 'remark', width: 200, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

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
    message.error('加载字典类型失败')
  } finally {
    typeLoading.value = false
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
  if (!expanded) return
  const typeId = record.id
  if (dictItemMap[typeId]) return

  itemLoadingMap[typeId] = true
  try {
    const res = await dictItemApi.getByDictTypeId(typeId)
    if (res.data) {
      dictItemMap[typeId] = res.data
    }
  } catch {
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
  padding: 0;
}

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
</style>
