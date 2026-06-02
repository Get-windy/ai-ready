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
  >
    <template #toolbar-actions>
      <a-button @click="handleBatchAssign"><template #icon><TeamOutlined /></template>批量分配</a-button>
      <a-button @click="handleImport"><template #icon><ImportOutlined /></template>导入线索</a-button>
      <a-button @click="handleExport"><template #icon><ExportOutlined /></template>导出</a-button>
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
import { PlusOutlined, ExportOutlined, EyeOutlined, EditOutlined, TeamOutlined, ImportOutlined, SwapRightOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'

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
    await new Promise(r => setTimeout(r, 300))
    dataSource.value = Array.from({ length: 25 }).map((_, i) => ({
      id: i + 1,
      name: `线索${i + 1}`,
      companyName: `公司${i + 1}`,
      contactName: `联系人${i + 1}`,
      phone: `13${Math.floor(Math.random() * 100000000).toString().padStart(8, '0')}`,
      email: `lead${i + 1}@example.com`,
      source: ['website', 'weixin', 'email', 'phone'][Math.floor(Math.random() * 4)],
      status: [0, 1, 2][Math.floor(Math.random() * 3)] as number,
      score: Math.floor(Math.random() * 100),
      createdAt: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 3600000).toISOString().slice(0, 10)
    }))
    pagination.total = 25
  } catch { message.error('获取数据失败') }
  finally { loading.value = false }
}

function handleView(record: Lead) { message.info(`查看线索: ${record.name}`) }
function handleEdit(record: Lead) { message.info(`编辑线索: ${record.name}`) }
function handleAdd() { message.info('新建线索') }
function handleDelete(record: Lead) { message.info(`删除线索: ${record.name}`) }
function handleAssign(record: Lead) { message.info(`分配线索: ${record.name}`) }
function handleConvert(record: Lead) { message.info(`转化线索: ${record.name}`) }
function handleBatchAssign() { message.info('批量分配线索') }
function handleImport() { message.info('导入线索') }

function handleExport() {
  const hideLoading = message.loading('正在生成导出文件...', 0)
  try {
    const headers = ['线索名称', '公司', '联系人', '电话', '邮箱', '来源', '评分', '状态', '添加时间']
    const rows = dataSource.value.map((row: any) => [row.name, row.companyName, row.contactName, row.phone, row.email, sourceTextMap[row.source] || row.source, row.score, getStatusText(row.status), row.createdAt])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = `线索_${new Date().toISOString().slice(0, 10)}.csv`
    link.click(); URL.revokeObjectURL(link); hideLoading(); message.success('导出成功')
  } catch { hideLoading(); message.error('导出失败') }
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }
</script>
