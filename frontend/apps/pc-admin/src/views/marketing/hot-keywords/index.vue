<template>
  <ErrorBoundary>
    <PageContainer title="热搜关键词">
      <a-alert
        type="warning"
        show-icon
        message="热搜词管理（置顶/屏蔽/权重调整）后端端点待补全"
        description="当前页对接搜索服务（/api/search/hot）真实端点，只读展示系统统计出的热门搜索词；人工干预配置将在后端提供端点后接入。"
        style="margin-bottom: 16px"
      />
      <div class="table-area">
        <div class="table-toolbar">
          <a-space>
            <span class="toolbar-label">展示数量</span>
            <a-select
              v-model:value="limit"
              :options="limitOptions"
              style="width: 110px"
              @change="loadData"
            />
            <a-button
              size="small"
              :loading="loading"
              @click="loadData"
            >
              <template #icon>
                <ReloadOutlined />
              </template>刷新
            </a-button>
          </a-space>
        </div>
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          row-key="rank"
          size="small"
          :pagination="false"
          :locale="{ emptyText: '暂无热搜数据' }"
        >
          <template #bodyCell="{ column, record, text }">
            <template v-if="column.dataIndex === 'rank'">
              <a-tag :color="record.rank <= 3 ? 'red' : 'default'">
                {{ record.rank }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'keyword'">
              <span class="keyword-text">{{ text }}</span>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { searchHotApi } from '@/api/marketing'

// ═══ 展示数量 ═══
const limit = ref(20)
const limitOptions = [
  { label: '10 条', value: 10 },
  { label: '20 条', value: 20 },
  { label: '50 条', value: 50 }
]

// ═══ 表格 ═══
interface HotKeywordRow {
  rank: number
  keyword: string
}

const loading = ref(false)
const tableData = ref<HotKeywordRow[]>([])

const columns: any[] = [
  { title: '排名', dataIndex: 'rank', key: 'rank', width: 90 },
  { title: '热搜关键词', dataIndex: 'keyword', key: 'keyword' }
]

async function loadData() {
  loading.value = true
  try {
    const res = await searchHotApi.hot(limit.value)
    tableData.value = res.hotSearches.map((keyword, index) => ({ rank: index + 1, keyword }))
  } catch (e) {
    tableData.value = []
    console.warn('[热搜关键词] 获取失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.table-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  max-width: 720px;
}
.table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.toolbar-label {
  color: rgba(0, 0, 0, 0.65);
}
.keyword-text {
  font-weight: 500;
}
</style>
