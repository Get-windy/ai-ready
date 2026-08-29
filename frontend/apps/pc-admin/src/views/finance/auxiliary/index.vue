<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="aux-page-header">
          <div class="aux-page-header-left">
            <a-breadcrumb class="aux-breadcrumb">
              <a-breadcrumb-item>
                <router-link to="/">首页</router-link>
              </a-breadcrumb-item>
              <a-breadcrumb-item>财务管理</a-breadcrumb-item>
              <a-breadcrumb-item>辅助核算</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="aux-page-header-title">辅助核算</h2>
          </div>
          <div class="aux-page-header-right">
            <a-button
              size="small"
              :loading="refreshLoading"
              @click="handleRefresh"
            >
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </div>
        </div>
      </template>

      <div class="auxiliary-page">
        <a-tabs v-model:active-key="activeTab" @change="handleTabChange">
          <!-- Tab 1: 辅助核算类型 -->
          <a-tab-pane key="type" tab="辅助核算类型">
            <div class="tab-toolbar">
              <a-space>
                <a-button type="primary" size="small" @click="handleAddType">
                  <template #icon><PlusOutlined /></template>
                  新增类型
                </a-button>
              </a-space>
              <a-input-search
                v-model:value="typeSearch"
                placeholder="搜索类型编码/名称"
                style="width: 240px"
                size="small"
                allow-clear
              />
            </div>
            <a-table
              :data-source="filteredTypes"
              :columns="typeColumns"
              :pagination="{ pageSize: 20, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
              :loading="typeLoading"
              size="small"
              row-key="id"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'enabled'">
                  <a-switch
                    :checked="record.enabled"
                    checked-children="启用"
                    un-checked-children="禁用"
                    size="small"
                    @change="(val: boolean) => handleToggleType(record, val)"
                  />
                </template>
                <template v-if="column.key === 'action'">
                  <a-space>
                    <a-button type="link" size="small" @click="handleEditType(record)">
                      编辑
                    </a-button>
                    <a-popconfirm
                      title="确定删除该辅助核算类型？"
                      @confirm="handleDeleteType(record)"
                    >
                      <a-button type="link" size="small" danger>删除</a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- Tab 2: 辅助核算项目 -->
          <a-tab-pane key="item" tab="辅助核算项目">
            <div class="tab-toolbar">
              <a-space>
                <a-select
                  v-model:value="selectedTypeId"
                  placeholder="请选择辅助核算类型"
                  style="width: 200px"
                  size="small"
                  allow-clear
                  @change="handleTypeFilterChange"
                >
                  <a-select-option
                    v-for="t in typeList"
                    :key="t.id"
                    :value="t.id"
                  >
                    {{ t.typeName }}
                  </a-select-option>
                </a-select>
                <a-button
                  type="primary"
                  size="small"
                  :disabled="!selectedTypeId"
                  @click="handleAddItem"
                >
                  <template #icon><PlusOutlined /></template>
                  新增项目
                </a-button>
              </a-space>
              <a-input-search
                v-model:value="itemSearch"
                placeholder="搜索项目编码/名称"
                style="width: 240px"
                size="small"
                allow-clear
              />
            </div>
            <a-table
              :data-source="filteredItems"
              :columns="itemColumns"
              :pagination="{ pageSize: 20, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
              :loading="itemLoading"
              size="small"
              row-key="id"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'auxiliaryTypeId'">
                  {{ getTypeName(record.auxiliaryTypeId) }}
                </template>
                <template v-if="column.dataIndex === 'enabled'">
                  <a-switch
                    :checked="record.enabled"
                    checked-children="启用"
                    un-checked-children="禁用"
                    size="small"
                    @change="(val: boolean) => handleToggleItem(record, val)"
                  />
                </template>
                <template v-if="column.key === 'action'">
                  <a-space>
                    <a-button type="link" size="small" @click="handleEditItem(record)">
                      编辑
                    </a-button>
                    <a-popconfirm
                      title="确定删除该辅助核算项目？"
                      @confirm="handleDeleteItem(record)"
                    >
                      <a-button type="link" size="small" danger>删除</a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>

        <!-- 类型编辑弹窗 -->
        <a-modal
          v-model:open="typeModalVisible"
          :title="typeFormIsEdit ? '编辑辅助核算类型' : '新增辅助核算类型'"
          :confirm-loading="typeFormLoading"
          @ok="handleTypeFormSubmit"
          @cancel="typeModalVisible = false"
        >
          <a-form
            ref="typeFormRef"
            :model="typeFormState"
            :rules="typeFormRules"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
            size="small"
            style="margin-top: 16px"
          >
            <a-form-item label="类型编码" name="typeCode">
              <a-input v-model:value="typeFormState.typeCode" placeholder="如：DEPT" />
            </a-form-item>
            <a-form-item label="类型名称" name="typeName">
              <a-input v-model:value="typeFormState.typeName" placeholder="如：部门" />
            </a-form-item>
            <a-form-item label="排序号" name="sort">
              <a-input-number v-model:value="typeFormState.sort" :min="0" style="width: 100%" />
            </a-form-item>
            <a-form-item label="启用" name="enabled">
              <a-switch v-model:checked="typeFormState.enabled" checked-children="启用" un-checked-children="禁用" />
            </a-form-item>
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="typeFormState.remark" :rows="3" placeholder="备注信息" />
            </a-form-item>
          </a-form>
        </a-modal>

        <!-- 项目编辑弹窗 -->
        <a-modal
          v-model:open="itemModalVisible"
          :title="itemFormIsEdit ? '编辑辅助核算项目' : '新增辅助核算项目'"
          :confirm-loading="itemFormLoading"
          @ok="handleItemFormSubmit"
          @cancel="itemModalVisible = false"
        >
          <a-form
            ref="itemFormRef"
            :model="itemFormState"
            :rules="itemFormRules"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
            size="small"
            style="margin-top: 16px"
          >
            <a-form-item label="所属类型" name="auxiliaryTypeId">
              <a-select
                v-model:value="itemFormState.auxiliaryTypeId"
                placeholder="请选择类型"
              >
                <a-select-option
                  v-for="t in typeList"
                  :key="t.id"
                  :value="t.id"
                >
                  {{ t.typeName }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="项目编码" name="itemCode">
              <a-input v-model:value="itemFormState.itemCode" placeholder="如：D001" />
            </a-form-item>
            <a-form-item label="项目名称" name="itemName">
              <a-input v-model:value="itemFormState.itemName" placeholder="如：财务部" />
            </a-form-item>
            <a-form-item label="上级项目" name="parentId">
              <a-tree-select
                v-model:value="itemFormState.parentId"
                :tree-data="itemTreeData"
                :field-names="{ label: 'itemName', value: 'id', children: 'children' }"
                placeholder="无（顶级项目）"
                allow-clear
                tree-default-expand-all
              />
            </a-form-item>
            <a-form-item label="排序号" name="sort">
              <a-input-number v-model:value="itemFormState.sort" :min="0" style="width: 100%" />
            </a-form-item>
            <a-form-item label="启用" name="enabled">
              <a-switch v-model:checked="itemFormState.enabled" checked-children="启用" un-checked-children="禁用" />
            </a-form-item>
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="itemFormState.remark" :rows="3" placeholder="备注信息" />
            </a-form-item>
          </a-form>
        </a-modal>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { auxiliaryTypeApi, auxiliaryItemApi } from '@/api/finance/auxiliary'

// ── 通用状态 ──
const activeTab = ref('type')
const refreshLoading = ref(false)

// ── 辅助核算类型 ──
const typeLoading = ref(false)
const typeList = ref<any[]>([])
const typeSearch = ref('')

const typeColumns = [
  { title: '类型编码', dataIndex: 'typeCode', key: 'typeCode', width: 120 },
  { title: '类型名称', dataIndex: 'typeName', key: 'typeName', width: 150 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '启用状态', dataIndex: 'enabled', key: 'enabled', width: 120 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const }
]

const filteredTypes = computed(() => {
  if (!typeSearch.value) return typeList.value
  const kw = typeSearch.value.toLowerCase()
  return typeList.value.filter((t: any) =>
    (t.typeCode || '').toLowerCase().includes(kw) ||
    (t.typeName || '').toLowerCase().includes(kw)
  )
})

const fetchTypes = async () => {
  typeLoading.value = true
  try {
    const res = await auxiliaryTypeApi.getList()
    typeList.value = res?.data || res || []
  } catch (err) {
    console.warn('[辅助核算] 获取类型列表失败', err)
    message.error('获取辅助核算类型失败')
  } finally {
    typeLoading.value = false
  }
}

// 类型表单
const typeModalVisible = ref(false)
const typeFormLoading = ref(false)
const typeFormIsEdit = ref(false)
const typeFormEditId = ref<number | null>(null)
const typeFormRef = ref<FormInstance>()

const typeFormState = reactive({
  typeCode: '',
  typeName: '',
  sort: 0,
  enabled: true,
  remark: ''
})

const typeFormRules = {
  typeCode: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }]
} as any

const resetTypeForm = () => {
  typeFormState.typeCode = ''
  typeFormState.typeName = ''
  typeFormState.sort = 0
  typeFormState.enabled = true
  typeFormState.remark = ''
  typeFormIsEdit.value = false
  typeFormEditId.value = null
  typeFormRef.value?.clearValidate()
}

const handleAddType = () => {
  resetTypeForm()
  typeModalVisible.value = true
}

const handleEditType = (record: any) => {
  resetTypeForm()
  typeFormIsEdit.value = true
  typeFormEditId.value = record.id
  typeFormState.typeCode = record.typeCode || ''
  typeFormState.typeName = record.typeName || ''
  typeFormState.sort = record.sort || 0
  typeFormState.enabled = record.enabled !== undefined ? record.enabled : true
  typeFormState.remark = record.remark || ''
  typeModalVisible.value = true
}

const handleTypeFormSubmit = async () => {
  try {
    await typeFormRef.value?.validate()
  } catch {
    return
  }
  typeFormLoading.value = true
  try {
    const data = { ...typeFormState }
    if (typeFormIsEdit.value && typeFormEditId.value) {
      await auxiliaryTypeApi.update(typeFormEditId.value, data)
      message.success('类型已更新')
    } else {
      await auxiliaryTypeApi.create(data)
      message.success('类型已创建')
    }
    typeModalVisible.value = false
    await fetchTypes()
  } catch (err) {
    console.warn('[辅助核算] 提交类型失败', err)
    message.error(typeFormIsEdit.value ? '更新失败' : '创建失败')
  } finally {
    typeFormLoading.value = false
  }
}

const handleDeleteType = async (record: any) => {
  try {
    await auxiliaryTypeApi.delete(record.id)
    message.success('类型已删除')
    await fetchTypes()
  } catch (err) {
    console.warn('[辅助核算] 删除类型失败', err)
    message.error('删除失败')
  }
}

const handleToggleType = async (record: any, val: boolean) => {
  try {
    await auxiliaryTypeApi.toggleEnabled(record.id, val)
    record.enabled = val
    message.success(val ? '已启用' : '已禁用')
  } catch (err) {
    console.warn('[辅助核算] 切换类型状态失败', err)
    message.error('操作失败')
  }
}

// ── 辅助核算项目 ──
const itemLoading = ref(false)
const itemList = ref<any[]>([])
const itemSearch = ref('')
const selectedTypeId = ref<number | undefined>(undefined)

const itemColumns = [
  { title: '所属类型', dataIndex: 'auxiliaryTypeId', key: 'auxiliaryTypeId', width: 120 },
  { title: '项目编码', dataIndex: 'itemCode', key: 'itemCode', width: 120 },
  { title: '项目名称', dataIndex: 'itemName', key: 'itemName', width: 180 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '启用状态', dataIndex: 'enabled', key: 'enabled', width: 120 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const }
]

const filteredItems = computed(() => {
  let list = itemList.value
  if (selectedTypeId.value) {
    list = list.filter((i: any) => i.auxiliaryTypeId === selectedTypeId.value)
  }
  if (itemSearch.value) {
    const kw = itemSearch.value.toLowerCase()
    list = list.filter((i: any) =>
      (i.itemCode || '').toLowerCase().includes(kw) ||
      (i.itemName || '').toLowerCase().includes(kw)
    )
  }
  return list
})

const itemTreeData = computed(() => {
  if (!selectedTypeId.value) return itemList.value
  return itemList.value.filter((i: any) => i.auxiliaryTypeId === selectedTypeId.value)
})

const getTypeName = (typeId: number) => {
  const t = typeList.value.find((x: any) => x.id === typeId)
  return t ? t.typeName : '-'
}

const fetchItems = async () => {
  itemLoading.value = true
  try {
    const params: any = {}
    if (selectedTypeId.value) {
      params.auxiliaryTypeId = selectedTypeId.value
    }
    const res = await auxiliaryItemApi.getList(params)
    itemList.value = res?.data || res || []
  } catch (err) {
    console.warn('[辅助核算] 获取项目列表失败', err)
    message.error('获取辅助核算项目失败')
  } finally {
    itemLoading.value = false
  }
}

// 项目表单
const itemModalVisible = ref(false)
const itemFormLoading = ref(false)
const itemFormIsEdit = ref(false)
const itemFormEditId = ref<number | null>(null)
const itemFormRef = ref<FormInstance>()

const itemFormState = reactive({
  auxiliaryTypeId: undefined as number | undefined,
  itemCode: '',
  itemName: '',
  parentId: undefined as number | undefined,
  sort: 0,
  enabled: true,
  remark: ''
})

const itemFormRules = {
  auxiliaryTypeId: [{ required: true, message: '请选择所属类型', trigger: 'change' }],
  itemCode: [{ required: true, message: '请输入项目编码', trigger: 'blur' }],
  itemName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }]
} as any

const resetItemForm = () => {
  itemFormState.auxiliaryTypeId = selectedTypeId.value
  itemFormState.itemCode = ''
  itemFormState.itemName = ''
  itemFormState.parentId = undefined
  itemFormState.sort = 0
  itemFormState.enabled = true
  itemFormState.remark = ''
  itemFormIsEdit.value = false
  itemFormEditId.value = null
  itemFormRef.value?.clearValidate()
}

const handleAddItem = () => {
  resetItemForm()
  itemModalVisible.value = true
}

const handleEditItem = (record: any) => {
  resetItemForm()
  itemFormIsEdit.value = true
  itemFormEditId.value = record.id
  itemFormState.auxiliaryTypeId = record.auxiliaryTypeId
  itemFormState.itemCode = record.itemCode || ''
  itemFormState.itemName = record.itemName || ''
  itemFormState.parentId = record.parentId || undefined
  itemFormState.sort = record.sort || 0
  itemFormState.enabled = record.enabled !== undefined ? record.enabled : true
  itemFormState.remark = record.remark || ''
  itemModalVisible.value = true
}

const handleItemFormSubmit = async () => {
  try {
    await itemFormRef.value?.validate()
  } catch {
    return
  }
  itemFormLoading.value = true
  try {
    const data = {
      ...itemFormState,
      parentId: itemFormState.parentId || 0
    }
    if (itemFormIsEdit.value && itemFormEditId.value) {
      await auxiliaryItemApi.update(itemFormEditId.value, data)
      message.success('项目已更新')
    } else {
      await auxiliaryItemApi.create(data)
      message.success('项目已创建')
    }
    itemModalVisible.value = false
    await fetchItems()
  } catch (err) {
    console.warn('[辅助核算] 提交项目失败', err)
    message.error(itemFormIsEdit.value ? '更新失败' : '创建失败')
  } finally {
    itemFormLoading.value = false
  }
}

const handleDeleteItem = async (record: any) => {
  try {
    await auxiliaryItemApi.delete(record.id)
    message.success('项目已删除')
    await fetchItems()
  } catch (err) {
    console.warn('[辅助核算] 删除项目失败', err)
    message.error('删除失败')
  }
}

const handleToggleItem = async (record: any, val: boolean) => {
  try {
    await auxiliaryItemApi.toggleEnabled(record.id, val)
    record.enabled = val
    message.success(val ? '已启用' : '已禁用')
  } catch (err) {
    console.warn('[辅助核算] 切换项目状态失败', err)
    message.error('操作失败')
  }
}

// ── Tab 切换 & 刷新 ──
const handleTabChange = (key: string | number) => {
  if (key === 'type') {
    fetchTypes()
  } else if (key === 'item') {
    fetchItems()
  }
}

const handleTypeFilterChange = () => {
  fetchItems()
}

const handleRefresh = async () => {
  refreshLoading.value = true
  try {
    await fetchTypes()
    if (activeTab.value === 'item') {
      await fetchItems()
    }
  } finally {
    refreshLoading.value = false
  }
}

// ── 生命周期 ──
onMounted(() => {
  fetchTypes()
  window.addEventListener('finance:refresh', handleRefresh as any)
})

function handleError(err: any) {
  console.warn('[ErrorBoundary]', err)
}
</script>

<style scoped>
.aux-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.aux-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.aux-breadcrumb {
  font-size: 13px;
}
.aux-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.aux-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auxiliary-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tab-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
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
:deep(.ant-form-item) {
  margin-bottom: 8px;
}
</style>
