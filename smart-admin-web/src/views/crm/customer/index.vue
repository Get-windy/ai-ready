<template>
  <div class="crm-customer-page">
    <!-- 搜索表单 -->
    <a-card class="search-card" title="客户搜索" :bordered="false">
      <a-form layout="inline" :model="queryParams" @finish="handleSearch">
        <a-form-item label="客户名称">
          <a-input v-model:value="queryParams.name" placeholder="请输入客户名称" allow-clear />
        </a-form-item>
        <a-form-item label="客户来源">
          <a-select v-model:value="queryParams.source" placeholder="请选择客户来源" allow-clear style="width: 150px">
            <a-select-option value="online">在线广告</a-select-option>
            <a-select-option value="referer">网站引荐</a-select-option>
            <a-select-option value="social">社交媒体</a-select-option>
            <a-select-option value="recommend">口碑推荐</a-select-option>
            <a-select-option value="expo">展会会议</a-select-option>
            <a-select-option value="other">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="客户等级">
          <a-select v-model:value="queryParams.level" placeholder="请选择客户等级" allow-clear style="width: 120px">
            <a-select-option value="A">A - 重要客户</a-select-option>
            <a-select-option value="B">B - 潜在客户</a-select-option>
            <a-select-option value="C">C - 普通客户</a-select-option>
            <a-select-option value="D">D - 低价值客户</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="queryParams.status" placeholder="请选择状态" allow-clear style="width: 100px">
            <a-select-option value="active">活跃</a-select-option>
            <a-select-option value="inactive">休眠</a-select-option>
            <a-select-option value="lost">流失</a-select-option>
          </a-select>
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
          新建客户
        </a-button>
        <a-button @click="handleImport">
          <template #icon><ImportOutlined /></template>
          导入客户
        </a-button>
        <a-button @click="handleExport">
          <template #icon><ExportOutlined /></template>
          导出客户
        </a-button>
      </div>
      <div class="action-right">
        <a-space>
          <a-select v-model:value="queryParams.viewType" style="width: 120px">
            <a-select-option value="list">列表视图</a-select-option>
            <a-select-option value="grid">网格视图</a-select-option>
          </a-select>
          <a-switch checked-children="仅活跃" un-checked-children="全部" v-model:checked="queryParams.activeOnly" />
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
        :scroll="{ x: 1200 }"
        @change="handleTableChange"
        row-key="id"
        :row-selection="rowSelection"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="customer-name-cell">
              <a-avatar
                :size="32"
                :src="getAvatarUrl(record.name)"
                :style="{ marginRight: '8px', backgroundColor: '#1890ff' }"
              >
                {{ record.name.charAt(0) }}
              </a-avatar>
              <div>
                <a @click="handleView(record)">{{ record.name }}</a>
                <a-tag v-if="record.level" :color="getLevelColor(record.level)" style="margin-left: 4px">
                  {{ getLevelText(record.level) }}
                </a-tag>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'phone'">
            <div>
              <div>{{ record.phone }}</div>
              <a v-if="record.mobile" style="font-size: 12px; color: #999">{{ record.mobile }}</a>
            </div>
          </template>
          <template v-else-if="column.key === 'source'">
            <a-badge :text="getSourceText(record.source)" :color="getSourceColor(record.source)" />
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'lastFollowUp'">
            <div>
              <div>{{ formatDate(record.lastFollowUp) }}</div>
              <a-typography-text type="secondary" style="font-size: 12px">
                {{ getFollowUpTimeAgo(record.lastFollowUp) }}
              </a-typography-text>
            </div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleEdit(record)">编辑</a>
              <a @click="handleAssign(record)">分配</a>
              <a @click="handleFollow(record)">跟进</a>
              <a-dropdown>
                <a>
                  更多
                  <DownOutlined />
                </a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="delete">
                      <a-popconfirm title="确定要删除吗？" @confirm="handleDelete(record)">
                        <span class="danger">删除</span>
                      </a-popconfirm>
                    </a-menu-item>
                    <a-menu-item key="merge">
                      <a @click="handleMerge(record)">合并</a>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
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
  ImportOutlined,
  ExportOutlined,
  DownOutlined
} from '@ant-design/icons-vue'

interface Customer {
  id: number
  name: string
  phone: string
  mobile?: string
  email?: string
  source: string
  level: string
  status: string
  lastFollowUp?: string
  score?: number
  contactPerson?: string
  companyName?: string
}

const loading = ref(false)
const dataSource = ref<Customer[]>([])

const queryParams = reactive({
  name: '',
  source: undefined as string | undefined,
  level: undefined as string | undefined,
  status: undefined as string | undefined,
  viewType: 'list' as 'list' | 'grid',
  activeOnly: false
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条客户`
})

const columns = [
  { 
    title: '客户名称', 
    dataIndex: 'name', 
    key: 'name',
    width: 200,
    fixed: 'left'
  },
  { title: '联系方式', dataIndex: 'phone', key: 'phone', width: 150 },
  { title: '客户来源', dataIndex: 'source', key: 'source', width: 120 },
  { title: '客户等级', dataIndex: 'level', key: 'level', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '最后跟进', dataIndex: 'lastFollowUp', key: 'lastFollowUp', width: 160 },
  { title: '负责人', dataIndex: 'contactPerson', key: 'contactPerson', width: 120 },
  { title: '公司', dataIndex: 'companyName', key: 'companyName' },
  { title: '操作', key: 'action', fixed: 'right', width: 220 }
]

const rowSelection = computed(() => ({
  columnWidth: 48,
  selectedRowKeys: [],
  onChange: (selectedRowKeys: number[]) => {
    console.log('selectedRowKeys:', selectedRowKeys)
  }
}))

// 辅助函数
const getAvatarUrl = (name: string) => {
  return `https://api.dicebear.com/7.x/avataaars/svg?name=${name}`
}

const getLevelColor = (level: string) => {
  const colors = { A: 'gold', B: 'blue', C: 'green', D: 'grey' }
  return colors[level] || 'default'
}

const getLevelText = (level: string) => {
  const texts = { A: '重要客户', B: '潜在客户', C: '普通客户', D: '低价值客户' }
  return texts[level] || level
}

const getSourceColor = (source: string) => {
  const colors: Record<string, string> = {
    online: 'blue',
    referer: 'purple',
    social: 'lime',
    recommend: 'geekblue',
    expo: 'orange',
    other: 'default'
  }
  return colors[source] || 'default'
}

const getSourceText = (source: string) => {
  const texts: Record<string, string> = {
    online: '在线广告',
    referer: '网站引荐',
    social: '社交媒体',
    recommend: '口碑推荐',
    expo: '展会会议',
    other: '其他'
  }
  return texts[source] || source
}

const getStatusColor = (status: string) => {
  const colors = { active: 'success', inactive: 'warning', lost: 'error' }
  return colors[status] || 'default'
}

const getStatusText = (status: string) => {
  const texts = { active: '活跃', inactive: '休眠', lost: '流失' }
  return texts[status] || status
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const getFollowUpTimeAgo = (dateStr: string) => {
  if (!dateStr) return '从未跟进'
  const date = new Date(dateStr)
  const now = Date.now()
  const diff = now - date.getTime()
  const minutes = Math.floor(diff / 60000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  return `${days}天前`
}

// 处理函数
const fetchData = async () => {
  loading.value = true
  try {
    // 模拟数据
    const mockData: Customer[] = Array.from({ length: 20 }).map((_, i) => ({
      id: i + 1,
      name: `客户${i + 1}`,
      phone: `13${Math.floor(Math.random() * 100000000).toString().padStart(8, '0')}`,
      email: `customer${i + 1}@example.com`,
      source: ['online', 'referer', 'social', 'recommend'][Math.floor(Math.random() * 4)],
      level: ['A', 'B', 'C'][Math.floor(Math.random() * 3)],
      status: ['active', 'inactive', 'lost'][Math.floor(Math.random() * 3)],
      lastFollowUp: new Date(Date.now() - Math.floor(Math.random() * 7) * 24 * 3600000).toISOString(),
      score: Math.floor(Math.random() * 100),
      contactPerson: `负责人${i + 1}`
    }))
    dataSource.value = mockData
    pagination.total = 20
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
  queryParams.level = undefined
  queryParams.status = undefined
  handleSearch()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const handleAdd = () => {
  message.info('新建客户')
}

const handleView = (record: Customer) => {
  message.info(`查看客户: ${record.name}`)
}

const handleEdit = (record: Customer) => {
  message.info(`编辑客户: ${record.name}`)
}

const handleAssign = (record: Customer) => {
  message.info(`分配客户: ${record.name}`)
}

const handleFollow = (record: Customer) => {
  message.info(`跟进客户: ${record.name}`)
}

const handleImport = () => {
  message.info('导入客户')
}

const handleExport = () => {
  message.info('导出客户')
}

const handleDelete = async (record: Customer) => {
  try {
    // 模拟删除
    dataSource.value = dataSource.value.filter(c => c.id !== record.id)
    pagination.total--
    message.success('删除成功')
  } catch (error) {
    message.error('删除失败')
  }
}

const handleMerge = (record: Customer) => {
  message.info(`合并客户: ${record.name}`)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.crm-customer-page {
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

.customer-name-cell {
  display: flex;
  align-items: center;
  font-weight: 500;
}

.danger {
  color: #ff4d4f;
}
</style>