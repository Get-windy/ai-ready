<template>
  <PageContainer>
    <!-- ═══ 顶栏：标题 + 方向 Tab + 日期快捷 + 操作 ═══ -->
    <div class="query-header">
      <div class="query-header__left">
        <span class="query-header__breadcrumb">仓储 / 借进借出 / 借进借出查询</span>
        <h2 class="query-header__title">借进借出查询</h2>
      </div>
      <div class="query-header__right">
        <a-tabs
          v-model:active-key="activeTab"
          size="small"
          @change="handleTabChange"
        >
          <a-tab-pane
            key="in"
            tab="借进商品查询"
          />
          <a-tab-pane
            key="out"
            tab="借出商品查询"
          />
        </a-tabs>
        <div class="date-shortcuts">
          <a
            v-for="s in dateShortcuts"
            :key="s.key"
            class="date-shortcut"
            :class="{ active: activeShortcut === s.key }"
            @click="pickShortcut(s.key)"
          >
            {{ s.label }}
          </a>
        </div>
        <a-space :size="8">
          <a-button
            size="small"
            :loading="loading"
            @click="loadData"
          >
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button
            size="small"
            @click="handleExport"
          >
            <template #icon><DownloadOutlined /></template>
            导出
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- ═══ 查询区 ═══ -->
    <div class="query-bar">
      <a-form layout="inline">
        <a-form-item label="商品">
          <a-input
            v-model:value="queryValues.keyword"
            placeholder="商品名称/货号"
            style="width: 180px"
            allow-clear
            @press-enter="loadData"
          />
        </a-form-item>
        <a-form-item label="往来单位">
          <a-input
            v-model:value="queryValues.partnerName"
            placeholder="往来单位"
            style="width: 180px"
            allow-clear
            @press-enter="loadData"
          />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            size="small"
            @click="loadData"
          >
            查询
          </a-button>
        </a-form-item>
      </a-form>
    </div>

    <!-- ═══ 统计卡 ═══ -->
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-label">{{ activeTab === 'in' ? '借进未还库存' : '借出未还数量' }}</div>
        <div class="stat-value">{{ formatQty(totals.stock) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ activeTab === 'in' ? '借进金额' : '借出金额' }}</div>
        <div class="stat-value">¥{{ totals.amount.toFixed(2) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已还数量</div>
        <div class="stat-value">{{ formatQty(totals.returnedQty) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已还金额</div>
        <div class="stat-value">¥{{ totals.returnedAmount.toFixed(2) }}</div>
      </div>
    </div>

    <!-- ═══ 商品×往来单位 聚合台账 ═══ -->
    <div class="table-card">
      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        row-key="rowKey"
        size="small"
        bordered
        :scroll="{ x: 1500 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'borrowStock'">
            <a-tag :color="Number(record.borrowStock) > 0 ? (activeTab === 'in' ? 'orange' : 'blue') : 'default'">
              {{ formatQty(record.borrowStock) }}
            </a-tag>
          </template>
          <template v-else-if="['borrowQty', 'returnedQty'].includes(String(column.dataIndex))">
            {{ formatQty(record[String(column.dataIndex)]) }}
          </template>
          <template v-else-if="['borrowAmount', 'returnedAmount'].includes(String(column.dataIndex))">
            ¥{{ (Number(record[String(column.dataIndex)]) || 0).toFixed(2) }}
          </template>
          <template v-else-if="column.dataIndex === 'transferPurchaseQty'">
            {{ formatQty(record.transferPurchaseQty) }}
          </template>
          <template v-else-if="column.dataIndex === 'transferPurchaseAmount'">
            ¥{{ (Number(record.transferPurchaseAmount) || 0).toFixed(2) }}
          </template>
        </template>
      </a-table>
      <div class="table-footer-note">
        注：借转采购为对标系统独有功能（借进转采购入库），本系统暂未实现，相关列恒为 0。
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ReloadOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'
import { formatQty } from '../whTask'
import dayjs from 'dayjs'

defineOptions({ name: 'WhBorrowQuery' })

// ═══ 方向 Tab ═══
const activeTab = ref<'in' | 'out'>('in')

// ═══ 日期快捷 ═══
const dateShortcuts = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'thisWeek', label: '本周' },
  { key: 'thisWeek2', label: '近一周' },
  { key: 'thisMonth', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' },
  { key: 'thisYear', label: '本年' },
]
const activeShortcut = ref('thisWeek2')
const dateRange = ref<[dayjs.Dayjs, dayjs.Dayjs]>([dayjs().subtract(6, 'day'), dayjs()])

function calcRange(key: string): [dayjs.Dayjs, dayjs.Dayjs] {
  const now = dayjs()
  switch (key) {
    case 'yesterday': {
      const y = now.subtract(1, 'day')
      return [y, y]
    }
    case 'today': return [now, now]
    case 'thisWeek': return [now.startOf('week').add(1, 'day'), now]
    case 'thisWeek2': return [now.subtract(6, 'day'), now]
    case 'thisMonth': return [now.startOf('month'), now]
    case 'lastMonth': {
      const first = now.subtract(1, 'month').startOf('month')
      return [first, first.endOf('month')]
    }
    case 'last3Month': return [now.subtract(2, 'month').startOf('month'), now]
    case 'thisYear': return [now.startOf('year'), now]
    default: return [now.subtract(6, 'day'), now]
  }
}

function pickShortcut(key: string) {
  activeShortcut.value = key
  dateRange.value = calcRange(key)
  loadData()
}

// ═══ 查询条件 ═══
const queryValues = ref({ keyword: '', partnerName: '' })

// ═══ 数据状态 ═══
const rows = ref<any[]>([])
const loading = ref(false)
const totals = ref({ stock: 0, amount: 0, returnedQty: 0, returnedAmount: 0 })

// ═══ 表格列（对标：商品×往来单位 聚合台账） ═══
const columns = computed(() => [
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 170, fixed: 'left' as const, ellipsis: true },
  { title: '货号', dataIndex: 'productCode', key: 'productCode', width: 100 },
  { title: '规格', dataIndex: 'productSpec', key: 'productSpec', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
  { title: '往来单位', dataIndex: 'partnerName', key: 'partnerName', width: 160, ellipsis: true },
  { title: activeTab.value === 'in' ? '借进库存' : '借出未还', dataIndex: 'borrowStock', key: 'borrowStock', width: 100, align: 'right' as const },
  { title: activeTab.value === 'in' ? '借进数量' : '借出数量', dataIndex: 'borrowQty', key: 'borrowQty', width: 100, align: 'right' as const },
  { title: activeTab.value === 'in' ? '借进金额' : '借出金额', dataIndex: 'borrowAmount', key: 'borrowAmount', width: 110, align: 'right' as const },
  { title: '还出数量', dataIndex: 'returnedQty', key: 'returnedQty', width: 90, align: 'right' as const },
  { title: '还出金额', dataIndex: 'returnedAmount', key: 'returnedAmount', width: 100, align: 'right' as const },
  { title: '借转采购数量', dataIndex: 'transferPurchaseQty', key: 'transferPurchaseQty', width: 110, align: 'right' as const },
  { title: '借转采购金额', dataIndex: 'transferPurchaseAmount', key: 'transferPurchaseAmount', width: 110, align: 'right' as const },
])

// ═══ 数据加载 ═══
async function loadData() {
  loading.value = true
  try {
    const direction = activeTab.value === 'in' ? 1 : 2
    const res: any = await request.get('/wms/borrow/aggregate', {
      params: {
        direction,
        productName: queryValues.value.keyword || undefined,
        partnerName: queryValues.value.partnerName || undefined,
        dateStart: dateRange.value[0].format('YYYY-MM-DD'),
        dateEnd: dateRange.value[1].format('YYYY-MM-DD'),
      },
    })
    // request 拦截器已解包 data，res 为数组
    const data: any[] = Array.isArray(res) ? res : (res?.data || [])
    rows.value = data.map((r: any, i: number) => ({
      ...r,
      rowKey: `${r.productId}-${r.partnerId}-${i}`,
      // 借转采购为我方未实现功能（对标独有），恒为 0
      transferPurchaseQty: 0,
      transferPurchaseAmount: 0,
    }))
    totals.value = {
      stock: data.reduce((s: number, r: any) => s + (Number(r.borrowStock) || 0), 0),
      amount: data.reduce((s: number, r: any) => s + (Number(r.borrowAmount) || 0), 0),
      returnedQty: data.reduce((s: number, r: any) => s + (Number(r.returnedQty) || 0), 0),
      returnedAmount: data.reduce((s: number, r: any) => s + (Number(r.returnedAmount) || 0), 0),
    }
  } catch {
    rows.value = []
    totals.value = { stock: 0, amount: 0, returnedQty: 0, returnedAmount: 0 }
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  queryValues.value = { keyword: '', partnerName: '' }
  loadData()
}

// ═══ 导出 CSV ═══
function handleExport() {
  const header = columns.value.map((c: any) => c.title).join(',')
  const lines = rows.value.map((r: any) => [
    r.productName || '',
    r.productCode || '',
    r.productSpec || '',
    r.unit || '',
    r.partnerName || '',
    r.borrowStock ?? 0,
    r.borrowQty ?? 0,
    r.borrowAmount ?? 0,
    r.returnedQty ?? 0,
    r.returnedAmount ?? 0,
    r.transferPurchaseQty ?? 0,
    r.transferPurchaseAmount ?? 0,
  ].join(','))
  const csv = '\uFEFF' + [header, ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `借进借出查询-${activeTab.value === 'in' ? '借进' : '借出'}-${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.query-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: #fff;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
  gap: 8px;
}
.query-header__breadcrumb { font-size: 12px; color: #909399; }
.query-header__title { font-size: 18px; font-weight: 600; color: #303133; margin: 4px 0 0; }
.query-header__right { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.date-shortcuts { display: flex; gap: 4px; align-items: center; }
.date-shortcut {
  font-size: 12px;
  color: #606266;
  padding: 2px 8px;
  border-radius: 4px;
  cursor: pointer;
}
.date-shortcut:hover { color: #1890ff; }
.date-shortcut.active { background: #e6f4ff; color: #1890ff; font-weight: 600; }
.query-bar {
  background: #fff;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.stat-row { display: flex; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
.stat-card {
  flex: 1;
  min-width: 180px;
  background: #fff;
  border-radius: 8px;
  padding: 14px 18px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.stat-label { font-size: 12px; color: #909399; }
.stat-value { font-size: 22px; font-weight: 700; color: #303133; margin-top: 4px; }
.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.table-footer-note {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}
</style>
