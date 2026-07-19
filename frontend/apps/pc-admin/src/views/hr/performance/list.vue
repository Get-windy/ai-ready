<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">
        绩效管理
      </h2>
    </div>
    <div class="page-container__body">
      <a-card
        :bordered="false"
        class="table-card"
      >
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'score'">
              <span style="font-weight: bold">{{ record.score }}</span>
            </template>
            <template v-if="column.key === 'level'">
              <a-tag :color="PERFORMANCE_LEVEL_MAP[record.level]?.color">
                {{ PERFORMANCE_LEVEL_MAP[record.level]?.text }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { hrPerformanceApi, type HrPerformance, PERFORMANCE_LEVEL_MAP } from '@/api/hr'

const loading = ref(false)
const tableData = ref<HrPerformance[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const columns: any[] = [
  { title: '考核周期', dataIndex: 'reviewPeriod', key: 'reviewPeriod', width: 100 },
  { title: '考核类型', dataIndex: 'reviewType', key: 'reviewType', width: 100 },
  { title: '考核评分', dataIndex: 'score', key: 'score', width: 80 },
  { title: '考核等级', dataIndex: 'level', key: 'level', width: 100 },
  { title: '工作态度', dataIndex: 'attitudeScore', key: 'attitudeScore', width: 80 },
  { title: '工作能力', dataIndex: 'abilityScore', key: 'abilityScore', width: 80 },
  { title: '工作业绩', dataIndex: 'achievementScore', key: 'achievementScore', width: 80 },
  { title: '考核人', dataIndex: 'reviewerName', key: 'reviewerName', width: 100 }
]

async function loadData() {
  loading.value = true
  try {
    const result = await hrPerformanceApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.records
    pagination.total = result.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }
onMounted(loadData)
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.table-card { background: #fff; }
</style>