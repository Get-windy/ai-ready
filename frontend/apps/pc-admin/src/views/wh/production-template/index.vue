<template>
  <ErrorBoundary
    @reset="fetchData"
    @error="handleError"
  >
    <PageContainer full-height>
      <template #header>
        <div class="header">
          <div class="header__left">
            <span class="header__breadcrumb">生产 / BOM管理</span>
            <h2 class="header__title">生产模板（BOM）</h2>
          </div>
          <div class="header__right">
            <a-button
              type="primary"
              size="small"
              @click="handleCreate"
            >
              <template #icon><PlusOutlined /></template>
              新建BOM
            </a-button>
          </div>
        </div>
      </template>

      <BillTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        selectable
        :filter-fields="filterFields"
        add-text="新建BOM"
        style="flex: 1;"
        @add="handleCreate"
        @refresh="fetchData"
        @search="handleSearch"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @cell-dblclick="handleView"
      >
        <template #statusCell="{ record }">
          <StatusTag
            :status="record.status"
            :map="BOM_TEMPLATE_STATUS"
          />
        </template>

        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="编辑">
              <a-button
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-space>
        </template>
      </BillTableList>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, EditOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import request from '@/utils/request'

defineOptions({ name: 'ProductionTemplateList' })

// 自定义状态映射（0=草稿 1=已发布 2=已作废）
const BOM_TEMPLATE_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已发布', color: 'success' },
  2: { text: '已作废', color: 'error' },
}

const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const tableRef = ref()
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const filterFields = computed(() => [
  { key: 'keyword', label: '关键字', type: 'input' as const, placeholder: 'BOM名称/编号' },
  { key: 'status', label: '状态', type: 'select' as const, options: Object.entries(BOM_TEMPLATE_STATUS).map(([k, v]) => ({ label: v.text, value: Number(k) })) },
])

const vxeColumns: any = computed(() => [
  { field: 'bomNo', title: 'BOM编号', width: 150 },
  { field: 'bomName', title: 'BOM名称', width: 200 },
  { field: 'productCode', title: '成品编码', width: 120 },
  { field: 'productName', title: '成品名称', width: 150 },
  { field: 'version', title: '版本', width: 80, align: 'center' },
  { field: 'outputQuantity', title: '产出数量', width: 90, align: 'center' },
  { field: 'totalCost', title: '总成本', width: 120 },
  { field: 'status', title: '状态', width: 90, slotName: 'statusCell' },
  { field: 'createTime', title: '创建时间', width: 160 },
  { field: 'action', title: '操作', width: 80, fixed: 'right', type: 'action' },
])

function handleError(err: any) { console.warn('[生产模板] ErrorBoundary:', err) }

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/wh/production-template/page', {
      params: {
        keyword: searchFilters.keyword || undefined,
        status: searchFilters.status,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      },
    })
    const data = res.data || res
    tableData.value = data?.records || []
    pagination.total = data?.total || 0
  } catch {
    message.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.current = 1; fetchData() }

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleCreate() {
  router.push('/wh/production-template/form/new')
}

function handleEdit(record: any) {
  router.push(`/wh/production-template/form/${record.id}`)
}

function handleView(record: any) {
  router.push(`/wh/production-template/form/${record.id}`)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.header__breadcrumb {
  font-size: 12px;
  color: #999;
}
.header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
