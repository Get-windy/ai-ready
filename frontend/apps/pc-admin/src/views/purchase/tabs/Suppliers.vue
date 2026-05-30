<template>
  <ModuleLayout
    :breadcrumb-items="breadcrumbItems"
    :current-view="currentView"
    :selected-count="selectedRowKeys.length"
    :current-page="pagination.current"
    :total-pages="Math.ceil(pagination.total / pagination.pageSize)"
    :page-size="pagination.pageSize"
    :total-items="pagination.total"
    @view-change="handleViewChange"
    @clear-selection="handleClearSelection"
    @page-change="handlePageChange"
    @page-size-change="handlePageSizeChange"
  >
    <template #actions>
      <a-button type="primary" @click="handleAdd">
        <template #icon><PlusOutlined /></template>
        新建供应商
      </a-button>
      <a-button @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
    </template>

    <template #batch-actions>
      <a-button @click="handleBatchEdit">批量编辑</a-button>
      <a-button danger @click="handleBatchDelete">批量删除</a-button>
    </template>

    <template #list-view>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '正常' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'level'">
            <a-tag :color="getLevelColor(record.level)">
              {{ getLevelText(record.level) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleEdit(record)">编辑</a>
              <a @click="handleDeleteConfirm(record)" class="danger">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>
  </ModuleLayout>

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
    <a-alert
      :message="`已选择 ${selectedRowKeys.length} 个供应商`"
      type="info"
      show-icon
      style="margin-bottom: 16px;"
    />
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
          @change="handleTransferChange"
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
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { ModuleLayout } from '@ai-ready/components'
import { supplierApi } from '@/api/supplier'

const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])
const currentView = ref('list')

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
  { key: '1', title: '优质供应商' },
  { key: '2', title: '长期合作' },
  { key: '3', title: '战略伙伴' },
  { key: '4', title: '紧急备用' },
  { key: '5', title: '新品开发' },
  { key: '6', title: '低优先级' }
])

const handleTransferChange = (nextTargetKeys: string[]) => {
  batchEditData.tags = nextTargetKeys
}

const breadcrumbItems = computed(() => [
  { text: '采购管理', path: '/purchase' },
  { text: '供应商' }
])

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const columns = [
  { title: '供应商编码', dataIndex: 'supplierCode', key: 'supplierCode', width: 150 },
  { title: '供应商名称', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 100 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 120 },
  { title: '等级', dataIndex: 'level', key: 'level', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 150 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => { selectedRowKeys.value = keys }
}))

const getLevelColor = (level: number) => {
  const colors: Record<number, string> = { 1: 'gold', 2: 'blue', 3: 'default' }
  return colors[level] || 'default'
}

const getLevelText = (level: number) => {
  const texts: Record<number, string> = { 1: 'A级', 2: 'B级', 3: 'C级' }
  return texts[level] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await supplierApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: 1 })
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch {
    error.value = '获取数据失败'
  } finally {
    loading.value = false
  }
}

const handleViewChange = (view: string) => { currentView.value = view }
const handleClearSelection = () => { selectedRowKeys.value = [] }
const handlePageChange = (page: number) => { pagination.current = page; fetchData() }
const handlePageSizeChange = (size: number) => { pagination.pageSize = size; pagination.current = 1; fetchData() }

const handleAdd = () => {
  router.push('/supplier/create')
}

const handleView = (record: any) => {
  router.push(`/supplier/detail/${record.id}`)
}

const handleEdit = (record: any) => {
  router.push(`/supplier/edit/${record.id}`)
}

const handleDeleteConfirm = (record: any) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除供应商 "${record.supplierName}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await supplierApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch { message.error('删除失败') }
    }
  })
}

// ── 导出功能 ──────────────────────────────────────────
const handleExport = () => {
  const hideLoading = message.loading('正在生成导出文件...', 0)
  try {
    const headers = ['供应商编码', '供应商名称', '联系人', '联系电话', '等级', '状态', '创建时间']
    const rows = dataSource.value.map(row => [
      row.supplierCode || '',
      row.supplierName || '',
      row.contactName || '',
      row.contactPhone || '',
      getLevelText(row.level),
      row.status === 1 ? '正常' : '停用',
      row.createTime || ''
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const BOM = '﻿'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `供应商_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
    hideLoading()
    message.success('导出成功，文件下载中')
  } catch {
    hideLoading()
    message.error('导出失败')
  }
}

// ── 批量编辑 ──────────────────────────────────────────
const handleBatchEdit = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要编辑的供应商')
    return
  }
  batchEditData.level = undefined
  batchEditData.status = undefined
  batchEditData.tags = []
  batchEditData.remark = ''
  batchEditModalVisible.value = true
}

const handleBatchEditConfirm = async () => {
  const updateData: any = {}
  if (batchEditData.level !== undefined) updateData.supplierLevel = batchEditData.level
  if (batchEditData.status !== undefined) updateData.cooperationStatus = batchEditData.status
  if (batchEditData.tags.length > 0) updateData.tags = batchEditData.tags
  if (batchEditData.remark) updateData.remark = batchEditData.remark

  if (Object.keys(updateData).length === 0) {
    message.warning('请至少选择一个要修改的字段')
    return
  }

  batchEditSubmitting.value = true
  let successCount = 0
  let failCount = 0
  for (const id of selectedRowKeys.value) {
    try {
      await supplierApi.update(id, updateData)
      successCount++
    } catch {
      failCount++
    }
  }
  batchEditSubmitting.value = false

  if (failCount === 0) {
    message.success(`批量编辑完成，成功更新 ${successCount} 个供应商`)
  } else {
    message.warning(`批量编辑完成: 成功 ${successCount} 个, 失败 ${failCount} 个`)
  }
  batchEditModalVisible.value = false
  selectedRowKeys.value = []
  fetchData()
}

// ── 批量删除 ──────────────────────────────────────────
const handleBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要删除的供应商')
    return
  }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量删除',
    content: `确定要批量删除选中的 ${count} 个供应商吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      let successCount = 0
      let failCount = 0
      for (const id of selectedRowKeys.value) {
        try {
          await supplierApi.delete(id)
          successCount++
        } catch {
          failCount++
        }
      }
      if (failCount === 0) {
        message.success(`批量删除完成，成功 ${successCount} 个供应商`)
      } else {
        message.warning(`删除完成: 成功 ${successCount} 个, 失败 ${failCount} 个`)
      }
      selectedRowKeys.value = []
      fetchData()
    }
  })
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.danger { color: #ff4d4f; }
</style>
