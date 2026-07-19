<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      title="科目余额表"
      full-height
    >
      <template #headerExtra>
        <a-space :size="12">
          <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
          <span
            v-if="lastUpdateTime"
            class="update-time"
          >最后更新: {{ lastUpdateTime }}</span>
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
      <div class="search-area">
        <a-form
          layout="inline"
          :model="searchParams"
        >
          <a-form-item label="科目代码">
            <a-input
              v-model:value="searchParams.subjectCode"
              placeholder="请输入科目代码"
              allow-clear
              style="width: 160px"
              @press-enter="handleSearch"
            />
          </a-form-item>
          <a-form-item label="年度">
            <a-input-number
              v-model:value="searchParams.year"
              placeholder="年度"
              :min="2020"
              :max="2099"
              style="width: 120px"
            />
          </a-form-item>
          <a-form-item label="期间">
            <a-input-number
              v-model:value="searchParams.period"
              placeholder="期间"
              :min="1"
              :max="12"
              style="width: 120px"
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                :loading="loading"
                @click="handleSearch"
              >
                <template #icon>
                  <SearchOutlined />
                </template>查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon>
                  <ClearOutlined />
                </template>重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>
      <div class="table-area">
        <BillTableList
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="billPagination"
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :show-export="false"
          :show-batch-delete="false"
          :selectable="false"
          row-key="id"
          @page-change="handlePageChange"
        />
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import type { Dayjs } from 'dayjs'
import { ReloadOutlined, SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import request from '@/utils/request'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')

const searchParams = reactive({
  subjectCode: '',
  year: undefined as number | undefined,
  period: undefined as number | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const columns = [
  { title: '科目代码', field: 'subjectCode', key: 'subjectCode', width: 100 },
  { title: '科目名称', field: 'subjectName', key: 'subjectName', width: 200 },
  { title: '期初余额', field: 'openBalance', key: 'openBalance', width: 140, align: 'right' },
  { title: '本期借方', field: 'debitAmount', key: 'debitAmount', width: 140, align: 'right' },
  { title: '本期贷方', field: 'creditAmount', key: 'creditAmount', width: 140, align: 'right' },
  { title: '期末余额', field: 'closeBalance', key: 'closeBalance', width: 140, align: 'right' },
]

const fetchData = async () => {
  loading.value = true; hasError.value = false
  try {
    const res: any = await request.get('/finance/balance-sheet/page', {
      params: { page: pagination.current, size: pagination.pageSize, ...searchParams }
    })
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || []
      pagination.total = data.total || 0
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (e: any) { hasError.value = true; console.warn('[科目余额表] 获取失败', e)
  } finally { loading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { searchParams.subjectCode = ''; searchParams.year = undefined; searchParams.period = undefined; pagination.current = 1; fetchData() }
const handlePageChange = (page: number, pageSize: number) => { pagination.current = page; pagination.pageSize = pageSize; fetchData() }
const handleError = (e: Error) => { hasError.value = true; console.error(e) }
onMounted(fetchData)
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.update-time { font-size: 12px; color: #999; }
</style>
