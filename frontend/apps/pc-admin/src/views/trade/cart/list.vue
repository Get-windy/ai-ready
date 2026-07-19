<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      title="购物车管理"
      full-height
    >
      <template #headerExtra>
        <a-space :size="12">
          <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
          <span class="data-status">购物车商品总数: <b>{{ totalItems }}</b></span>
          <a-button
            size="small"
            @click="fetchData"
          >
            <template #icon>
              <ReloadOutlined />
            </template>
          </a-button>
        </a-space>
      </template>
      <div class="table-area">
        <BillTableList
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          row-key="id"
          :pagination="false as any"
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :show-export="false"
          :show-batch-delete="false"
          :selectable="false"
        >
          <template #totalPriceCell="{ record }">
            <span class="currency-value">¥{{ formatAmount(record.totalPrice) }}</span>
          </template>
          <template #actionCell="{ record }">
            <a-popconfirm
              title="确定删除该商品？"
              @confirm="handleDelete(record)"
            >
              <a-button
                type="link"
                size="small"
                danger
              >
                删除
              </a-button>
            </a-popconfirm>
          </template>
        </BillTableList>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import request from '@/utils/request'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const totalItems = ref(0)

const columns = [
  { title: '商品名称', field: 'productName', key: 'productName', width: 200 },
  { title: '商品编码', field: 'productCode', key: 'productCode', width: 120 },
  { title: '规格', field: 'specification', key: 'specification', width: 100 },
  { title: '数量', field: 'quantity', key: 'quantity', width: 80, align: 'right' },
  { title: '单价', field: 'price', key: 'price', width: 100, align: 'right' },
  { title: '小计', field: 'totalPrice', key: 'totalPrice', width: 120, align: 'right', type: 'slot', slotName: 'totalPriceCell' },
  { title: '操作', key: 'action', type: 'action', width: 80, fixed: 'right', slotName: 'actionCell' }
]

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res: any = await request.get('/api/v1/mall/cart')
    const data = res.data || res || []
    tableData.value = Array.isArray(data) ? data : []
    totalItems.value = tableData.value.reduce((s: number, i: any) => s + (i.quantity || 0), 0)
  } catch (e: any) {
    hasError.value = true
    console.warn('[购物车] 获取列表失败', e)
  } finally { loading.value = false }
}

const handleDelete = async (record: any) => {
  try {
    await request.delete(`/api/v1/mall/cart/${record.id}`)
    message.success('已删除')
    fetchData()
  } catch { message.error('删除失败') }
}

const handleError = (e: Error) => { hasError.value = true; console.error(e) }
function formatAmount(v: number) { return (v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }

onMounted(fetchData)
</script>
<style scoped>
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.data-status { font-size: 12px; color: #666; }
.currency-value { font-family: 'SFMono-Regular', Consolas, monospace; font-variant-numeric: tabular-nums; }
</style>
