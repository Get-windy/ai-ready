<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/">
                首页
              </router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>使用统计</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            使用统计
          </h2>
        </div>
      </div>
    </template>

    <a-row
      :gutter="16"
      style="margin-bottom:16px"
    >
      <a-col
        v-for="stat in moduleStats"
        :key="stat.name"
        :span="6"
      >
        <a-card
          :bordered="false"
          size="small"
        >
          <a-statistic
            :title="stat.name"
            :value="stat.count"
            :suffix="stat.unit || ''"
          >
            <template #prefix>
              <component
                :is="stat.icon"
                :style="{ color: stat.color }"
              />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <a-card
      :bordered="false"
      title="模块使用排行"
    >
      <a-table
        :data-source="list"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'rank'">
            <a-tag :color="record.rank <= 3 ? 'gold' : 'blue'">
              #{{ record.rank }}
            </a-tag>
          </template>
          <template v-if="column.key === 'usageRate'">
            <a-progress
              :percent="record.usageRate"
              size="small"
              :status="record.usageRate > 80 ? 'exception' : 'active'"
            />
          </template>
        </template>
      </a-table>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, shallowRef, onMounted } from 'vue'
import { AppstoreOutlined, ShoppingOutlined, ShoppingCartOutlined, ContainerOutlined, DollarOutlined, TeamOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const list = ref<any[]>([])

const moduleStats = shallowRef<any[]>([
  { name: '总模块数', count: 12, icon: AppstoreOutlined, color: '#1890ff' },
  { name: '活跃模块', count: 10, icon: AppstoreOutlined, color: '#52c41a' },
  { name: '总租户数', count: 48, unit: '个', icon: TeamOutlined, color: '#722ed1' },
  { name: '平均使用率', count: 78, suffix: '%', icon: AppstoreOutlined, color: '#faad14' },
])

const columns = [
  { title: '排名', dataIndex: 'rank', key: 'rank', width: 70 },
  { title: '模块名称', dataIndex: 'moduleName', key: 'moduleName' },
  { title: '模块编码', dataIndex: 'moduleCode', key: 'moduleCode', width: 130 },
  { title: '租户数量', dataIndex: 'tenantCount', key: 'tenantCount', width: 100 },
  { title: '使用率', dataIndex: 'usageRate', key: 'usageRate', width: 180 },
  { title: '月度活跃', dataIndex: 'monthlyActive', key: 'monthlyActive', width: 100 },
]

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/module/usage')
    list.value = res?.records || []
  } catch {
    list.value = [
      { id: 1, rank: 1, moduleName: '销售管理', moduleCode: 'sale', tenantCount: 42, usageRate: 95, monthlyActive: 1280 },
      { id: 2, rank: 2, moduleName: '采购管理', moduleCode: 'purchase', tenantCount: 38, usageRate: 88, monthlyActive: 960 },
      { id: 3, rank: 3, moduleName: '客户关系', moduleCode: 'crm', tenantCount: 35, usageRate: 82, monthlyActive: 720 },
      { id: 4, rank: 4, moduleName: '财务管理', moduleCode: 'finance', tenantCount: 30, usageRate: 72, monthlyActive: 540 },
      { id: 5, rank: 5, moduleName: '仓储管理', moduleCode: 'warehouse', tenantCount: 28, usageRate: 65, monthlyActive: 380 },
      { id: 6, rank: 6, moduleName: '营销管理', moduleCode: 'marketing', tenantCount: 15, usageRate: 35, monthlyActive: 120 },
    ]
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
