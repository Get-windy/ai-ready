<template>
  <div class="crm-lead-page">
    <!-- 搜索表单 -->
    <a-card class="search-card" title="线索搜索" :bordered="false">
      <a-form layout="inline" :model="queryParams" @finish="handleSearch">
        <a-form-item label="线索名称">
          <a-input v-model:value="queryParams.name" placeholder="请输入线索名称" allow-clear />
        </a-form-item>
        <a-form-item label="来源渠道">
          <a-select v-model:value="queryParams.source" placeholder="请选择来源" allow-clear style="width: 150px">
            <a-select-option value="website">官网咨询</a-select-option>
            <a-select-option value="weixin">微信公众号</a-select-option>
            <a-select-option value="email">邮件咨询</a-select-option>
            <a-select-option value="phone">电话咨询</a-select-option>
            <a-select-option value="social">社交媒体</a-select-option>
            <a-select-option value="other">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="线索状态">
          <a-select v-model:value="queryParams.status" placeholder="请选择状态" allow-clear style="width: 120px">
            <a-select-option value="0">新线索</a-select-option>
            <a-select-option value="1">跟进中</a-select-option>
            <a-select-option value="2">已转化</a-select-option>
            <a-select-option value="3">已关闭</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="评分范围">
          <a-range-picker v-model:value="queryParams.scoreRange" style="width: 200px" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 工具栏 -->
    <a-card class="action-bar" :bordered="false">
      <div class="action-left">
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新建线索
        </a-button>
        <a-button @click="handleBatchAssign">
          <template #icon><TeamOutlined /></template>
          批量分配
        </a-button>
        <a-button @click="handleImport">
          <template #icon><ImportOutlined /></template>
          导入线索
        </a-button>
      </div>
      <div class="action-right">
        <a-space>
          <a-badge :count="highScoreCount" :overflowCount="99" color="red">
            <a-tag>高分线索(&gt;80)</a-tag>
          </a-badge>
          <a-badge :count="pendingCount" :overflowCount="99" color="orange">
            <a-tag>待跟进</a-tag>
          </a-badge>
        </a-space>
      </div>
    </a-card>

    <!-- 数据表格 -->
    <a-card>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
        :row-selection="rowSelection"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="lead-name-cell">
              <a-avatar
                :size="32"
                :src="getAvatarUrl(record.name)"
                :style="{ marginRight: '8px', backgroundColor: '#52c41a' }"
              >
                {{ record.name.charAt(0) }}
              </a-avatar>
              <div>
                <a @click="handleView(record)">{{ record.name }}</a>
                <a-badge v-if="record.score >= 80" text="高分" style="margin-left: 8px" />
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'companyName'">
            <div>
              <div>{{ record.companyName }}</div>
              <a-typography-text type="secondary" style="font-size: 12px">
                {{ record.contactName }}
              </a-typography-text>
            </div>
          </template>
          <template v-else-if="column.key === 'phone'">
            <div>
              <div>{{ record.phone }}</div>
              <a v-if="record.mobile" style="font-size: 12px; color: #999">{{ record.mobile }}</a>
            </div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'source'">
            <a-badge :text="getSourceText(record.source)" :color="getSourceColor(record.source)" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleEdit(record)">编辑</a>
              <a @click="handleAssign(record)">分配</a>
              <a @click="handleConvert(record)" v-if="record.status < 2">转化</a>
              <a @click="handleMerge(record)" v-if="record.status < 2">合并</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  TeamOutlined,
  ImportOutlined
} from '@ant-design/icons-vue'

interface Lead {
  id: number
  name: string
  companyName: string
  contactName: string
  phone: string
  mobile?: string
  email?: string
  source: string
  status: number
  score: number
}

const loading = ref(false)
const dataSource = ref<Lead[]>([])

const queryParams = reactive({
  name: '',
  source: undefined as string | undefined,
  status: undefined as number | undefined,
  scoreRange: [] as any[],
  activeOnly: true
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条线索`
})

const columns = [
  { 
    title: '线索名称', 
    dataIndex: 'name', 
    key: 'name',
    width: 180,
    fixed: 'left'
  },
  { title: '公司', dataIndex: 'companyName', key: 'companyName', width: 180 },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 120 },
  { title: '联系方式', dataIndex: 'phone', key: 'phone', width: 150 },
  { title: '来源渠道', dataIndex: 'source', key: 'source', width: 120 },
  { title: '评分', dataIndex: 'score', key: 'score', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '添加时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', fixed: 'right', width: 200 }
]

const rowSelection = computed(() => ({
  columnWidth: 48,
  selectedRowKeys: [],
  onChange: (selectedRowKeys: number[]) => {
    console.log('selectedRowKeys:', selectedRowKeys)
  }
}))

// 辅助函数
const highScoreCount = computed(() => {
  return dataSource.value.filter(l => l.score >= 80).length
})

const pendingCount = computed(() => {
  return dataSource.value.filter(l => l.status === 0 || l.status === 1).length
})

const getAvatarUrl = (name: string) => {
  return `https://api.dicebear.com/7.x/avataaars/svg?name=${name}`
}

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'blue', 1: 'orange', 2: 'green', 3: 'red' }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '新线索', 1: '跟进中', 2: '已转化', 3: '已关闭' }
  return texts[status] || '未知'
}

const getSourceColor = (source: string) => {
  const colors: Record<string, string> = {
    website: 'blue',
    weixin: 'green',
    email: 'purple',
    phone: 'orange',
    social: 'lime',
    other: 'default'
  }
  return colors[source] || 'default'
}

const getSourceText = (source: string) => {
  const texts: Record<string, string> = {
    website: '官网咨询',
    weixin: '微信公众号',
    email: '邮件咨询',
    phone: '电话咨询',
    social: '社交媒体',
    other: '其他'
  }
  return texts[source] || source
}

// 处理函数
const fetchData = async () => {
  loading.value = true
  try {
    // 模拟数据
    const mockData: Lead[] = Array.from({ length: 25 }).map((_, i) => ({
      id: i + 1,
      name: `线索${i + 1}`,
      companyName: `公司${i + 1}`,
      contactName: `联系人${i + 1}`,
      phone: `13${Math.floor(Math.random() * 100000000).toString().padStart(8, '0')}`,
      email: `lead${i + 1}@example.com`,
      source: ['website', 'weixin', 'email', 'phone'][Math.floor(Math.random() * 4)],
      status: [0, 1, 2][Math.floor(Math.random() * 3)] as number,
      score: Math.floor(Math.random() * 100),
      createdAt: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 3600000).toISOString()
    }))
    dataSource.value = mockData
    pagination.total = 25
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryParams.name = ''
  queryParams.source = undefined
  queryParams.status = undefined
  queryParams.scoreRange = []
  handleSearch()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const handleAdd = () => {
  message.info('新建线索')
}

const handleView = (record: Lead) => {
  message.info(`查看线索: ${record.name}`)
}

const handleEdit = (record: Lead) => {
  message.info(`编辑线索: ${record.name}`)
}

const handleAssign = (record: Lead) => {
  message.info(`分配线索: ${record.name}`)
}

const handleConvert = (record: Lead) => {
  message.info(`转化线索: ${record.name}`)
}

const handleBatchAssign = () => {
  message.info('批量分配线索')
}

const handleImport = () => {
  message.info('导入线索')
}

const handleMerge = (record: Lead) => {
  message.info(`合并线索: ${record.name}`)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.crm-lead-page {
  padding: 0;
}

.search-card {
  margin-bottom: 16px;
}

.action-bar {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-left,
.action-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.lead-name-cell {
  display: flex;
  align-items: center;
  font-weight: 500;
}
</style>