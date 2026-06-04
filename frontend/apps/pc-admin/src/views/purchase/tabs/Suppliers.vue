<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'purchase-supplier-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    :show-export="true"
    add-text="新建供应商"
    @add="handleAdd"
    @edit="handleEdit"
    @view="handleView"
    @delete="handleDelete"
    @batch-delete="handleBatchDelete"
    @refresh="fetchData"
    @search="handleSearch"
    @page-change="handlePageChange"
    @sort-change="handleSortChange"
    @filter-change="handleFilterChange"
    @export="handleExport"
  >

    <template #batch-actions>
      <a-button size="small" @click="handleBatchEdit">批量编辑</a-button>
    </template>

    <template #status="{ record }">
      <a-tag :color="record.status === 1 ? 'green' : 'default'">
        {{ record.status === 1 ? '正常' : '停用' }}
      </a-tag>
    </template>

    <template #action="{ record }">
      <a-space :size="4">
        <a-tooltip title="查看">
          <a-button type="link" size="small" @click="handleView(record)">
            <template #icon><EyeOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="编辑">
          <a-button type="link" size="small" @click="handleEdit(record)">
            <template #icon><EditOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-popconfirm title="确定删除该供应商？" @confirm="handleDelete(record)">
          <a-tooltip title="删除">
            <a-button type="link" size="small" danger>
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-popconfirm>
      </a-space>
    </template>
  </TableList>

  <!-- 批量编辑弹窗 -->
  <a-modal
    v-model:open="batchEditModalVisible"
    title="批量编辑供应商"
    width="700px"
    centered
    :confirm-loading="batchEditSubmitting"
    ok-text="确认修改"
    cancel-text="取消"
    @ok="handleBatchEditConfirm"
    @cancel="batchEditModalVisible = false"
  >
    <a-alert :message="`已选择 ${selectedRowKeys.length} 个供应商`" type="info" show-icon style="margin-bottom: 16px;" />
    <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
      <a-form-item label="供应商等级">
        <a-select v-model:value="batchEditData.level" placeholder="请选择等级（留空不修改）" allow-clear>
          <a-select-option :value="1">A级</a-select-option>
          <a-select-option :value="2">B级</a-select-option>
          <a-select-option :value="3">C级</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="合作状态">
        <a-select v-model:value="batchEditData.status" placeholder="请选择状态（留空不修改）" allow-clear>
          <a-select-option :value="1">正常</a-select-option>
          <a-select-option :value="0">停用</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="类别标签">
        <a-transfer
          :data-source="transferData"
          :titles="['可选标签', '已选标签']"
          :target-keys="batchEditData.tags"
          :render="(item: any) => item.title"
          @change="(nextTargetKeys: string[]) => { batchEditData.tags = nextTargetKeys }"
        />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="batchEditData.remark" placeholder="批量备注（将覆盖原有备注）" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import TableList from '@/components/TableList/TableList.vue'
import { supplierApi } from '@/api/supplier'
import { useUserStore } from '@/stores/user'
import { exportCsv } from '@/utils/exportCsv'
import { executeBatch } from '@/utils/batchOperations'

const userStore = useUserStore()
const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const selectedRowKeys = computed(() => tableRef.value?.selectedRowKeys || [])

const columns = [
  { title: '供应商编码', dataIndex: 'supplierCode', key: 'supplierCode', width: 130, sortable: true },
  { title: '供应商名称', dataIndex: 'supplierName', key: 'supplierName', width: 160 },
  { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson', width: 100 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 120 },
  { title: '等级', dataIndex: 'supplierLevel', key: 'supplierLevel', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'supplierCode', label: '供应商编码', type: 'input' as const, placeholder: '输入编码' },
  { key: 'supplierName', label: '供应商名称', type: 'input' as const, placeholder: '输入名称' },
  { key: 'keyword', label: '关键词', type: 'input' as const, placeholder: '搜索关键词' },
  { key: 'supplierLevel', label: '等级', type: 'select' as const, options: [
    { label: 'A级', value: 'A' }, { label: 'B级', value: 'B' }, { label: 'C级', value: 'C' }
  ]},
  { key: 'cooperationStatus', label: '状态', type: 'select' as const, options: [
    { label: '正常', value: 1 }, { label: '停用', value: 0 }
  ]}
]

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [
    { label: '本页数量', value: dataSource.value.length, type: 'default' as const }
  ]
})

function getLevelText(level: string): string {
  const texts: Record<string, string> = { 'A': 'A级', 'B': 'B级', 'C': 'C级' }
  return texts[level] || level
}

// ── 批量编辑状态 ──────────────────────────────────────
const batchEditModalVisible = ref(false)
const batchEditSubmitting = ref(false)
const batchEditData = reactive({
  level: undefined as number | undefined,
  status: undefined as number | undefined,
  tags: [] as string[],
  remark: ''
})

const transferData = ref([
  { key: '1', title: '优质供应商' }, { key: '2', title: '长期合作' },
  { key: '3', title: '战略伙伴' }, { key: '4', title: '紧急备用' },
  { key: '5', title: '新品开发' }, { key: '6', title: '低优先级' }
])

async function fetchData() {
  loading.value = true
  try {
    const res = await supplierApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || []
    pagination.total = pageData.total || 0
  } catch {
    message.error('获取供应商列表失败')
  } finally { loading.value = false }
}

function handleView(record: any) { router.push(`/supplier/detail/${record.id}`) }
function handleAdd() { router.push('/supplier/create') }
function handleEdit(record: any) { router.push(`/supplier/edit/${record.id}`) }

async function handleDelete(record: any) {
  try { await supplierApi.delete(record.id); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
}

async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => supplierApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

function handleExport() {
  const headers = ['供应商编码', '供应商名称', '联系人', '联系电话', '等级', '状态', '创建时间']
  const rows = dataSource.value.map((row: any) => [
    row.supplierCode || '', row.supplierName || '', row.contactPerson || '', row.contactPhone || '',
    getLevelText(row.supplierLevel), row.status === 1 ? '正常' : '停用', row.createTime || ''
  ])
  exportCsv(headers, rows, '供应商')
}

function handleBatchEdit() {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择供应商'); return }
  batchEditData.level = undefined; batchEditData.status = undefined
  batchEditData.tags = []; batchEditData.remark = ''
  batchEditModalVisible.value = true
}

const handleBatchEditConfirm = async () => {
  const updateData: any = {}
  if (batchEditData.level !== undefined) updateData.supplierLevel = batchEditData.level
  if (batchEditData.status !== undefined) updateData.cooperationStatus = batchEditData.status
  if (batchEditData.tags.length > 0) updateData.tags = batchEditData.tags
  if (batchEditData.remark) updateData.remark = batchEditData.remark
  if (Object.keys(updateData).length === 0) { message.warning('请至少选择一个要修改的字段'); return }

  batchEditSubmitting.value = true
  const result = await executeBatch(selectedRowKeys.value, (id) => supplierApi.update(id, updateData), '批量编辑')
  batchEditSubmitting.value = false
  if (result.successCount > 0) fetchData()
  batchEditModalVisible.value = false
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => fetchData())
</script>
