<template>
  <ErrorBoundary>
    <PageContainer title="按单收款">
      <div class="toolbar">
        <a-input v-model:value="keyword" placeholder="客户名称/单号" style="width: 200px" allow-clear @press-enter="handleSearch" />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleRefresh">刷新</a-button>
        <span class="toolbar-hint">勾选应收单，点击「生成收款单」</span>
      </div>

      <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id"
        :pagination="pagination" size="small" :scroll="{ x: 1000 }"
        @change="handleTableChange"
        :row-selection="{ selectedRowKeys, onChange: onSelectChange }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'remainingAmount'">
            <span :class="{ 'text-danger': record.status === 'overdue' }">¥{{ formatMoney(record.remainingAmount) }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 'overdue' ? 'red' : record.status === 'normal' ? 'blue' : 'default'">
              {{ record.status === 'normal' ? '正常' : record.status === 'overdue' ? '逾期' : record.status }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'dueDate'">
            <span :class="{ 'text-danger': record.status === 'overdue' }">{{ record.dueDate || '-' }}</span>
          </template>
        </template>
      </a-table>

      <div class="bottom-actions">
        <span>已选 {{ selectedRowKeys.length }} 笔，合计应收 ¥{{ formatMoney(selectedTotal) }}</span>
        <a-button type="primary" :disabled="selectedRowKeys.length === 0" @click="handleGenerate">生成收款单</a-button>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { receivableApi } from '@/api/finance'
import { customerApi } from '@/api/customer'

defineOptions({ name: 'ReceiptByDoc' })
const router = useRouter()

const keyword = ref('')
const list = ref<any[]>([])
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 20, total: 0 })
const selectedRowKeys = ref<number[]>([])

const selectedTotal = computed(() =>
  list.value.filter(r => selectedRowKeys.value.includes(r.id))
    .reduce((s, r) => s + Number(r.remainingAmount || 0), 0)
)

const columns = [
  { title: '来源单号', dataIndex: 'sourceNo', key: 'sourceNo', width: 170 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 150, ellipsis: true },
  { title: '应收金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, align: 'right' },
  { title: '已收金额', dataIndex: 'paidAmount', key: 'paidAmount', width: 100, align: 'right' },
  { title: '未收金额', dataIndex: 'remainingAmount', key: 'remainingAmount', width: 120, align: 'right' },
  { title: '到期日', dataIndex: 'dueDate', key: 'dueDate', width: 110 },
  { title: '发票号', dataIndex: 'invoiceNo', key: 'invoiceNo', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
]

function formatMoney(v: any) { return v ? Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '0.00' }

function onSelectChange(keys: number[]) { selectedRowKeys.value = keys }

async function fetchData() {
  loading.value = true
  try {
    const res: any = await receivableApi.getPage({ keyword: keyword.value || undefined, pageNum: pagination.value.current, pageSize: pagination.value.pageSize })
    list.value = res?.records || []
    pagination.value.total = res?.total || 0
  } catch { list.value = [] }
  finally { loading.value = false }
}

function handleSearch() { pagination.value.current = 1; fetchData() }
function handleRefresh() { fetchData() }
function handleTableChange(pag: any) { pagination.value.current = pag.current || 1; fetchData() }

async function handleGenerate() {
  message.loading('生成收款单中...')
  try {
    const id = selectedRowKeys.value[0]
    const res: any = await receivableApi.getById(id)
    if (res) router.push(`/finance/receipt-doc/form?customerId=${res.customerId || ''}`)
  } catch { message.error('生成失败，请手动创建收款单') }
}

fetchData()
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; padding: 12px 16px; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.toolbar-hint { margin-left: auto; color: #999; font-size: 13px; }
.bottom-actions { margin-top: 16px; padding: 12px 16px; background: #fff; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.text-danger { color: #f5222d; font-weight: 600; }
</style>
