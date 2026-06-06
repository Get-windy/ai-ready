<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'sale-exchange-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    :show-export="true"
    add-text="新建换货"
    add-permission="'sale:exchange:create'"
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
    <template #toolbar-actions>
      <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
        更新 {{ dayjs(lastUpdated).format('HH:mm') }}
      </span>
    </template>

    <template #empty>
      <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配换货单">
        <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
        <a-button @click="handleResetFilters">清除筛选</a-button>
      </a-empty>
      <a-empty v-else description="暂无换货单">
        <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
        <a-button type="primary" @click="handleAdd">新建换货单</a-button>
      </a-empty>
    </template>

    <template #batch-actions>
      <a-button v-permission="'sale:exchange:approve'" size="small" @click="handleBatchApprove">批量审批</a-button>
    </template>

    <template #status="{ record }">
      <a-tag :color="getStatusColor(record.status)">
        {{ getStatusText(record.status) }}
      </a-tag>
    </template>

    <template #action="{ record }">
      <a-space :size="0" class="action-cell-inner">
        <a-tooltip title="查看详情">
          <a-button type="link" size="small" @click="handleView(record)">
            <template #icon><EyeOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip v-if="record.status === 0" title="编辑">
          <a-button v-permission.disabled="'sale:exchange:update'" type="link" size="small" @click="handleEdit(record)">
            <template #icon><EditOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-dropdown trigger="click">
          <a-button type="link" size="small" class="action-more-btn">
            <template #icon><EllipsisOutlined /></template>
          </a-button>
          <template #overlay>
            <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
              <a-menu-item v-if="record.status === 0" key="submit">
                <SendOutlined /> 提交
              </a-menu-item>
              <a-menu-item v-if="record.status === 1" key="approve">
                <CheckCircleOutlined /> 审批
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item key="delete" danger>
                <DeleteOutlined /> 删除
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-space>
    </template>
  </TableList>

  <a-modal v-model:open="detailVisible" title="换货单详情" width="700px" :footer="null">
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="换货单号">{{ currentRecord.exchangeNo }}</a-descriptions-item>
      <a-descriptions-item label="关联订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
      <a-descriptions-item label="换货日期">{{ currentRecord.exchangeDate }}</a-descriptions-item>
      <a-descriptions-item label="状态"><a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="创建人">{{ currentRecord.creatorName }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="更新时间">{{ currentRecord.updateTime || '-' }}</a-descriptions-item>
      <a-descriptions-item label="换货原因" :span="2">{{ currentRecord.reason || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div class="detail-modal-footer"><a-button @click="detailVisible = false">关闭</a-button></div>
  </a-modal>

  <a-modal v-model:open="formModalVisible" :title="formMode === 'add' ? '新建换货单' : '编辑换货单'" width="700px" centered
    :confirm-loading="formSubmitting" ok-text="确认" cancel-text="取消"
    @ok="handleFormSubmit" @cancel="formModalVisible = false">
    <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
      <a-form-item label="关联订单" name="orderNo"><a-input v-model:value="formData.orderNo" placeholder="请输入销售订单号" /></a-form-item>
      <a-form-item label="客户" name="customerName"><a-input v-model:value="formData.customerName" placeholder="请输入客户名称" /></a-form-item>
      <a-row>
        <a-col :span="12">
          <a-form-item label="换货原因" name="reason" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="formData.reason" placeholder="请选择换货原因">
              <a-select-option value="质量问题">质量问题</a-select-option>
              <a-select-option value="规格不符">规格不符</a-select-option>
              <a-select-option value="数量错误">数量错误</a-select-option>
              <a-select-option value="客户要求">客户要求</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="换货日期" name="exchangeDate" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-date-picker v-model:value="formData.exchangeDate" style="width: 100%" /></a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="退换产品" name="outItem"><a-input v-model:value="formData.outItem" placeholder="请输入需要退换的产品名称" /></a-form-item>
      <a-form-item label="替换产品" name="inItem"><a-input v-model:value="formData.inItem" placeholder="请输入替换的产品名称" /></a-form-item>
      <a-form-item label="备注" name="remark"><a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
defineOptions({ name: 'SaleExchangeTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SendOutlined, CheckCircleOutlined, InboxOutlined, SearchOutlined, EllipsisOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { saleExchangeApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { executeBatch } from '@/utils/batchOperations'
import { useExport } from '@/composables/useExport'

const { execute: executeExport } = useExport()
const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const columns = [
  { title: '换货单号', dataIndex: 'exchangeNo', key: 'exchangeNo', width: 160, sortable: true },
  { title: '关联订单', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 140 },
  { title: '换货日期', dataIndex: 'exchangeDate', key: 'exchangeDate', width: 110, type: 'date' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建人', dataIndex: 'creatorName', key: 'creatorName', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 130, fixed: 'right' as const, type: 'action' as const }
]
const filterFields = [
  { key: 'exchangeNo', label: '换货单号', type: 'input' as const, placeholder: '输入换货单号' },
  { key: 'customerName', label: '客户', type: 'input' as const, placeholder: '输入客户' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已审批', value: 2 },
    { label: '换货中', value: 3 }, { label: '完成', value: 4 }, { label: '已取消', value: 5 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]
const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'blue', 4: 'success', 5: 'red' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已审批', 3: '换货中', 4: '完成', 5: '已取消' }
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const formData = reactive({ id: undefined as number | undefined, orderNo: '', customerName: '', reason: undefined as string | undefined, exchangeDate: undefined as any, outItem: '', inItem: '', remark: '' })
const formRules = {
  orderNo: [{ required: true, message: '请输入关联订单号', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  reason: [{ required: true, message: '请选择换货原因', trigger: 'change' }],
  exchangeDate: [{ required: true, message: '请选择换货日期', trigger: 'change' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await saleExchangeApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData?.records || []; pagination.total = pageData?.total || 0
    lastUpdated.value = new Date().toISOString()
  } catch { message.error('获取换货单列表失败') }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  formMode.value = 'add'; formData.id = undefined; formData.orderNo = ''; formData.customerName = ''
  formData.reason = undefined; formData.exchangeDate = undefined; formData.outItem = ''; formData.inItem = ''; formData.remark = ''
  formModalVisible.value = true
}
function handleEdit(record: any) {
  formMode.value = 'edit'; formData.id = record.id; formData.orderNo = record.orderNo || ''
  formData.customerName = record.customerName || ''; formData.reason = record.reason || undefined
  formData.exchangeDate = record.exchangeDate || undefined; formData.outItem = record.outItem || ''
  formData.inItem = record.inItem || ''; formData.remark = record.remark || ''
  formModalVisible.value = true
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'submit': handleSubmit(record); break
    case 'approve': handleApprove(record); break
    case 'delete': handleDelete(record); break
  }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除换货单「${record.exchangeNo}」吗？删除后数据不可恢复。`,
    okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    onOk: async () => {
      try { await saleExchangeApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1; fetchData()
}
async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => saleExchangeApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    if (formMode.value === 'add') {
      await saleExchangeApi.create({ ...formData })
    } else {
      await saleExchangeApi.update(formData.id!, { ...formData })
    }
    message.success(formMode.value === 'add' ? '新建换货单成功' : '编辑换货单成功')
    formModalVisible.value = false; pagination.current = 1; fetchData()
  } catch { message.error('操作失败') }
  finally { formSubmitting.value = false }
}

function handleSubmit(record: any) {
  Modal.confirm({
    title: '提交换货单', content: `提交换货单 "${record.exchangeNo}" ？`, okText: '确认提交', centered: true,
    async onOk() { try { await saleExchangeApi.submit(record.id); message.success('提交成功'); fetchData() } catch { message.error('提交失败') } }
  })
}
function handleApprove(record: any) {
  Modal.confirm({
    title: '审批换货单', content: `审批换货单 "${record.exchangeNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await saleExchangeApi.approve(record.id); message.success('审批成功'); fetchData() } catch { message.error('审批失败') } }
  })
}
function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择换货单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => saleExchangeApi.approve(id), '批量审批')
      if (result.successCount > 0) fetchData()
    }
  })
}

function handleExport() {
  executeExport({
    fileName: '换货单',
    headers: ['换货单号', '关联订单', '客户', '换货日期', '状态', '创建人', '创建时间'],
    fetchAll: () => saleExchangeApi.page({ pageNum: 1, pageSize: pagination.total, tenantId: userStore.tenantId, ...searchFilters }),
    mapToRows: (list: any[]) => list.map((row: any) => [
      row.exchangeNo || '', row.orderNo || '', row.customerName || '', row.exchangeDate || '',
      getStatusText(row.status), row.creatorName || '', row.createTime || ''
    ]),
    fallbackRows: () => dataSource.value.map((row: any) => [
      row.exchangeNo || '', row.orderNo || '', row.customerName || '', row.exchangeDate || '',
      getStatusText(row.status), row.creatorName || '', row.createTime || ''
    ]),
    total: pagination.total,
  })
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
const debouncedFetch = ref(0)
function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters); pagination.current = 1
  clearTimeout(debouncedFetch.value)
  debouncedFetch.value = window.setTimeout(() => fetchData(), 400)
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('sale:refresh', fetchData)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('sale:refresh', fetchData)
})

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}
</script>

<style scoped>
.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.detail-modal-footer { text-align: right; margin-top: 16px; }
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
</style>
