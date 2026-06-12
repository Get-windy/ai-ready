<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item><router-link to="/dms/verification">核验管理</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>绑定详情</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>绑定详情</h2>
        </div>
        <div class="page-header__right">
          <a-button size="small" :loading="loading" @click="initData">
            <ReloadOutlined /> 刷新
          </a-button>
          <a-button size="small" @click="goBack"><ArrowLeftOutlined /> 返回</a-button>
        </div>
      </div>
    </template>

    <template #default>
      <div v-if="loading" class="loading-wrapper">
        <a-spin size="large" />
      </div>
      <div v-else class="page-body">
        <!-- 绑定信息 -->
        <a-descriptions title="绑定信息" :column="2" bordered size="small" style="background:#fff;">
          <a-descriptions-item label="骑手ID" :span="1">{{ bindingInfo.riderId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="骑手姓名" :span="1">{{ bindingInfo.riderName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="车辆ID" :span="1">{{ bindingInfo.vehicleId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="车牌号" :span="1">{{ bindingInfo.plateNo || bindingInfo.vehiclePlate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="绑定时间" :span="1">{{ bindingInfo.bindTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="交车时间" :span="1">{{ bindingInfo.handoverTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态" :span="1">
            <a-tag :color="bindingInfo.status === 1 ? 'blue' : 'default'">
              {{ bindingInfo.status === 1 ? '绑定中' : '已交车' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="备注" :span="1">{{ bindingInfo.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <!-- 核验历史 -->
        <h4 style="margin-top:20px; margin-bottom:12px; font-size:14px; font-weight:600; color:#303133;">核验历史</h4>
        <a-table
          :dataSource="verifyHistory"
          :columns="verifyColumns"
          :loading="verifyLoading"
          rowKey="id"
          size="small"
          bordered
          :pagination="false as any"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'verificationType'">
              <a-tag :color="verifyTypeMap[record.verificationType]?.color || 'default'">
                {{ verifyTypeMap[record.verificationType]?.text || record.verificationType }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'result'">
              <a-tag :color="record.result === 1 ? 'green' : 'red'">
                {{ record.result === 1 ? '通过' : '不通过' }}
              </a-tag>
            </template>
          </template>
        </a-table>
        <div v-if="!verifyLoading && (!verifyHistory || verifyHistory.length === 0)" class="empty-hint">
          暂无核验记录
        </div>
      </div>
    </template>
  </PageContainer></ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { verificationApi } from '@/api/dms/verification'
import {
  ReloadOutlined, ArrowLeftOutlined
} from '@ant-design/icons-vue'
import { useRoute, useRouter } from 'vue-router'

function handleError(err: any) { console.warn('[DMS绑定详情]', err) }

const route = useRoute()
const router = useRouter()
const bindingId = Number(route.params.id || route.query.id)

const loading = ref(false)
const bindingInfo = reactive<Record<string, any>>({})

const verifyLoading = ref(false)
const verifyHistory = ref<any[]>([])

const verifyTypeMap: Record<number, { text: string; color: string }> = {
  1: { text: '出车前核验', color: 'blue' },
  2: { text: '收车后核验', color: 'purple' },
  3: { text: '随机核验', color: 'cyan' },
  4: { text: '交接核验', color: 'green' },
}

const verifyColumns = [
  { title: '核验类型', dataIndex: 'verificationType', width: 110 },
  { title: '结果', dataIndex: 'result', width: 70 },
  { title: '核验时间', dataIndex: 'verificationTime', width: 170 },
  { title: '位置', dataIndex: 'location', width: 130 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
]

async function fetchBindingDetail() {
  if (!bindingId) return
  loading.value = true
  try {
    const res: any = await verificationApi.bindingDetail(bindingId)
    const data = res?.data ?? res
    if (data) {
      // 绑定信息
      Object.assign(bindingInfo, data.binding || data)
      // 核验历史
      verifyHistory.value = data.verifyHistory || data.verificationRecords || []
    }
  } catch (err: any) {
    message.error(err?.message || '获取绑定详情失败')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

function initData() {
  fetchBindingDetail()
}

onMounted(() => {
  initData()
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; align-items: center; gap: 12px; }
.page-header__left h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.page-body { padding: 0; }
.loading-wrapper { display: flex; justify-content: center; align-items: center; min-height: 200px; }
.empty-hint { text-align: center; padding: 24px 0; color: #999; font-size: 13px; }
</style>
