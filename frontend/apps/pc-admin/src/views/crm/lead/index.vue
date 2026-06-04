<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'crm-lead-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    :show-export="true"
    :row-selection="rowSelection"
    add-text="新建线索"
    @add="handleAdd"
    @view="handleView"
    @edit="handleEdit"
    @delete="handleDelete"
    @refresh="fetchData"
    @search="handleSearch"
    @page-change="handlePageChange"
    @sort-change="handleSortChange"
    @filter-change="handleFilterChange"
    @export="handleExport"
  >
    <template #toolbar-actions>
      <a-button @click="handleBatchAssign"><template #icon><TeamOutlined /></template>批量分配</a-button>
      <a-button @click="handleImport"><template #icon><ImportOutlined /></template>导入线索</a-button>
    </template>
    <template #batch-actions>
      <a-button size="small" @click="handleBatchAssign">批量分配</a-button>
    </template>

    <template #name="{ record }">
      <div style="display:flex;align-items:center;font-weight:500">
        <a-avatar :size="28" style="marginRight:8px;backgroundColor:#52c41a;fontSize:12px">{{ record.name.charAt(0) }}</a-avatar>
        <div>
          <a @click="handleView(record)">{{ record.name }}</a>
          <a-badge v-if="record.score >= 80" text="高分" style="marginLeft:8px" />
        </div>
      </div>
    </template>
    <template #companyName="{ record }">
      <div><div>{{ record.companyName }}</div><a-typography-text type="secondary" style="fontSize:12px">{{ record.contactName }}</a-typography-text></div>
    </template>
    <template #phone="{ record }">
      <div><div>{{ record.phone }}</div><a v-if="record.mobile" style="fontSize:12px;color:#999">{{ record.mobile }}</a></div>
    </template>
    <template #status="{ record }">
      <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
    </template>
    <template #source="{ record }">
      <a-badge :text="sourceTextMap[record.source] || record.source" :color="sourceColorMap[record.source] || 'default'" />
    </template>
    <template #action="{ record }">
      <a-space :size="4">
        <a-tooltip title="查看"><a-button type="link" size="small" @click="handleView(record)"><template #icon><EyeOutlined /></template></a-button></a-tooltip>
        <a-tooltip title="编辑"><a-button type="link" size="small" @click="handleEdit(record)"><template #icon><EditOutlined /></template></a-button></a-tooltip>
        <a-tooltip title="分配"><a-button type="link" size="small" @click="handleAssign(record)"><template #icon><TeamOutlined /></template></a-button></a-tooltip>
        <a-tooltip v-if="record.status < 2" title="转化"><a-button type="link" size="small" @click="handleConvert(record)"><template #icon><SwapRightOutlined /></template></a-button></a-tooltip>
      </a-space>
    </template>
  </TableList>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, EditOutlined, TeamOutlined, ImportOutlined, SwapRightOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { leadApi } from '@/api/crm'
import { exportCsv } from '@/utils/exportCsv'

interface Lead { id: number; name: string; companyName: string; contactName: string; phone: string; mobile?: string; email?: string; source: string; status: number; score: number; createdAt: string }

const tableRef = ref()
const loading = ref(false)
const dataSource = ref<Lead[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '线索名称', dataIndex: 'name', key: 'name', width: 180, fixed: 'left' as const, slotName: 'name' },
  { title: '公司/联系人', dataIndex: 'companyName', key: 'companyName', width: 180, slotName: 'companyName' },
  { title: '联系方式', dataIndex: 'phone', key: 'phone', width: 150, slotName: 'phone' },
  { title: '来源渠道', dataIndex: 'source', key: 'source', width: 110, slotName: 'source' },
  { title: '评分', dataIndex: 'score', key: 'score', width: 80, sortable: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '添加时间', dataIndex: 'createdAt', key: 'createdAt', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'name', label: '线索名称', type: 'input' as const, placeholder: '输入线索名称' },
  { key: 'source', label: '来源渠道', type: 'select' as const, options: [
    { label: '官网咨询', value: 'website' }, { label: '微信公众号', value: 'weixin' },
    { label: '邮件咨询', value: 'email' }, { label: '电话咨询', value: 'phone' },
    { label: '社交媒体', value: 'social' }, { label: '其他', value: 'other' }
  ]},
  { key: 'status', label: '线索状态', type: 'select' as const, options: [
    { label: '新线索', value: 0 }, { label: '跟进中', value: 1 }, { label: '已转化', value: 2 }, { label: '已关闭', value: 3 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'blue', 1: 'orange', 2: 'green', 3: 'red' }
const statusTextMap: Record<number, string> = { 0: '新线索', 1: '跟进中', 2: '已转化', 3: '已关闭' }
const sourceColorMap: Record<string, string> = { website: 'blue', weixin: 'green', email: 'purple', phone: 'orange', social: 'lime', other: 'default' }
const sourceTextMap: Record<string, string> = { website: '官网咨询', weixin: '微信公众号', email: '邮件咨询', phone: '电话咨询', social: '社交媒体', other: '其他' }

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  const highScore = dataSource.value.filter(l => l.score >= 80).length
  const pending = dataSource.value.filter(l => l.status < 2).length
  return [
    { label: '本页数量', value: dataSource.value.length, type: 'default' as const },
    { label: '高分线索', value: highScore, type: 'primary' as const },
    { label: '待跟进', value: pending, type: 'warning' as const }
  ]
})

const rowSelection = { columnWidth: 48, selectedRowKeys: ref<number[]>([]), onChange: (keys: number[]) => { rowSelection.selectedRowKeys.value = keys } }

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await leadApi.page({
      keyword: searchFilters.keyword,
      leadStatus: searchFilters.status,
      leadLevel: searchFilters.leadLevel,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    const result = res as any
    dataSource.value = (result.records || result.data?.records || []) as Lead[]
    pagination.total = result.total ?? result.data?.total ?? 0
  } catch { message.error('获取线索数据失败') }
  finally { loading.value = false }
}

function handleView(record: Lead) { message.info(`查看线索: ${record.name}`) }
function handleEdit(record: Lead) { message.info(`编辑线索: ${record.name}`) }
function handleAdd() { message.info('新建线索') }
async function handleDelete(record: Lead) {
  try { await leadApi.delete(record.id); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
}
async function handleAssign(record: Lead) { message.info(`分配线索: ${record.name}`) }
async function handleConvert(record: Lead) {
  try { await leadApi.convertToCustomer(record.id); message.success('转化成功'); fetchData() }
  catch { message.error('转化失败') }
}
function handleBatchAssign() { message.info('批量分配线索') }
function handleImport() { message.info('导入线索') }

function handleExport() {
  const headers = ['线索名称', '公司', '联系人', '电话', '邮箱', '来源', '评分', '状态', '添加时间']
  const rows = dataSource.value.map((row: any) => [row.name, row.companyName, row.contactName, row.phone, row.email, sourceTextMap[row.source] || row.source, row.score, getStatusText(row.status), row.createdAt])
  exportCsv(headers, rows, '线索')
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }
</script>
