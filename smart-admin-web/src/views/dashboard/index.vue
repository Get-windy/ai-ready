<template>
  <div class="dashboard-page">
    <!-- 统计卡片区域 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :xs="24" :sm="12" :md="6" v-for="item in stats" :key="item.title">
        <a-card class="stats-card" hoverable>
          <a-statistic
            :title="item.title"
            :value="item.value"
            :prefix="item.prefix"
            :suffix="item.suffix"
          >
            <template #prefix>
              <div class="stat-icon">
                <component :is="item.icon" />
              </div>
            </template>
          </a-statistic>
          <div class="stat-trend" v-if="item.trend">
            <span :class="['trend-badge', item.trend.type]">
              {{ item.trend.type === 'up' ? '↑' : '↓' }} {{ item.trend.value }}
            </span>
            <span class="trend-text">{{ item.trend.label }}</span>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :xs="24" :md="16">
        <a-card class="chart-card" title="销售趋势" :loading="chartLoading">
          <div class="chart-container">
            <LineChartOutlined style="font-size: 64px; color: #1890ff; margin-bottom: 16px" />
            <a-typography-text type="secondary">
              暂无数据，请先配置数据源
            </a-typography-text>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :md="8">
        <a-card class="chart-card" title="客户分布">
          <div class="chart-container">
            <PieChartOutlined style="font-size: 64px; color: #52c41a; margin-bottom: 16px" />
            <a-typography-text type="secondary">
              暂无数据，请先配置数据源
            </a-typography-text>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 动态区域 -->
    <a-row :gutter="16">
      <a-col :xs="24" :md="16">
        <a-card title="待办事项" extra="<a class='view-all' href='javascript:void(0)'>查看全部</a>">
          <a-list :data-source="todos" size="large" :pagination="pagination">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <div class="todo-title">
                      <a-space>
                        <a-badge :status="item.priority === 'high' ? 'error' : item.priority === 'medium' ? 'warning' : 'default'" />
                        <a @click="handleTodo(item)">{{ item.title }}</a>
                      </a-space>
                    </div>
                  </template>
                  <template #description>
                    <div class="todo-meta">
                      <span class="todo-time">{{ item.time }}</span>
                      <span class="todo-priority" v-if="item.priority">
                        {{ item.priority === 'high' ? '高频' : item.priority === 'medium' ? '中频' : '低频' }}
                      </span>
                    </div>
                    <a-typography-paragraph
                      :ellipsis="{ rows: 1, tooltip: true }"
                      style="margin-bottom: 0"
                    >
                      {{ item.description }}
                    </a-typography-paragraph>
                  </template>
                </a-list-item-meta>
                <div class="todo-actions">
                  <a-button type="link" size="small" @click="handleComplete(item)">
                    完成
                  </a-button>
                  <a-button type="link" size="small" @click="handleEdit(item)">
                    编辑
                  </a-button>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :xs="24" :md="8">
        <a-card title="系统状态">
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="API服务">
              <a-badge status="success" text="运行中" />
            </a-descriptions-item>
            <a-descriptions-item label="数据库">
              <a-badge status="success" text="正常" />
            </a-descriptions-item>
            <a-descriptions-item label="缓存服务">
              <a-badge status="processing" text="连接中" />
            </a-descriptions-item>
            <a-descriptions-item label="消息队列">
              <a-badge status="default" text="已启用" />
            </a-descriptions-item>
          </a-descriptions>
          
          <div class="system-danger">
            <a-alert type="warning" message="待处理告警" :show-icon="false">
              <template #description>
                <a-list :data-source="alarms" size="small">
                  <template #renderItem="{ item }">
                    <a-list-item>
                      <a-space>
                        <a-icon :type="item.type === 'error' ? 'warning' : 'info'" :style="{ color: item.color }" />
                        <span>{{ item.message }}</span>
                      </a-space>
                    </a-list-item>
                  </template>
                </a-list>
              </template>
            </a-alert>
          </div>
        </a-card>
        
        <a-card title="快捷操作" style="margin-top: 16px">
          <a-grid :gutter="8">
            <a-grid-row>
              <a-grid-col :span="8" v-for="action in quickActions" :key="action.label">
                <a-button 
                  type="default" 
                  block 
                  @click="handleQuickAction(action)"
                  :icon="action.icon"
                >
                  {{ action.label }}
                </a-button>
              </a-grid-col>
            </a-grid-row>
          </a-grid>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  ShoppingCartOutlined,
  UserOutlined,
  TeamOutlined,
  DollarOutlined,
  LineChartOutlined,
  PieChartOutlined,
  WarningOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { ListProps } from 'ant-design-vue'

interface Todo {
  id: number
  title: string
  time: string
  description?: string
  priority?: 'high' | 'medium' | 'low'
  status?: 'pending' | 'completed'
}

const chartLoading = ref(false)
const stats = ref([
  { 
    title: '今日订单', 
    value: 128, 
    icon: ShoppingCartOutlined,
    trend: { type: 'up', value: '12%', label: '较昨日' }
  },
  { 
    title: '新增客户', 
    value: 32, 
    icon: UserOutlined,
    trend: { type: 'up', value: '8%', label: '较昨日' }
  },
  { 
    title: '待处理线索', 
    value: 15, 
    icon: TeamOutlined,
    trend: { type: 'down', value: '3', label: '较昨日' }
  },
  { 
    title: '本月销售额', 
    value: 125600, 
    prefix: '¥', 
    icon: DollarOutlined,
    trend: { type: 'up', value: '15%', label: '较上月' }
  }
])

const todos = ref<Todo[]>([
  { id: 1, title: '审批采购订单 #[REDACTED]', time: '10分钟前', description: '供应商A的采购订单，金额 ¥45,000', priority: 'high' },
  { id: 2, title: '跟进客户"张三"的询价', time: '30分钟前', description: '客户有意向购买企业版套餐', priority: 'medium' },
  { id: 3, title: '处理库存预警', time: '1小时前', description: '产品编号P001库存低于安全线', priority: 'high' },
  { id: 4, title: '确认销售订单发货', time: '2小时前', description: '订单 #[REDACTED] 待发货', priority: 'medium' },
  { id: 5, title: '生成月度销售报告', time: '3小时前', description: '统计4月份销售数据', priority: 'low' },
  { id: 6, title: '更新产品价格表', time: '半天前', description: '根据市场情况调整价格', priority: 'medium' }
])

const pagination = ref<ListProps['pagination']>({
  pageSize: 4,
  showSizeChanger: false,
  showTotal: (total: number) => `共 ${total} 条待办`
})

const alarms = ref([
  { type: 'warning', message: '客户"李四"的线索已超时24小时', color: '#faad14' },
  { type: 'error', message: '数据库连接池接近上限 (85%)', color: '#ff4d4f' }
])

const quickActions = ref([
  { label: '新建订单', icon: ShoppingCartOutlined, action: 'newOrder' },
  { label: '添加客户', icon: UserOutlined, action: 'addCustomer' },
  { label: '创建线索', icon: TeamOutlined, action: 'createLead' },
  { label: '查看报表', icon: LineChartOutlined, action: 'viewReport' }
])

const handleTodo = (item: Todo) => {
  message.info(`处理: ${item.title}`)
}

const handleComplete = (item: Todo) => {
  message.success(`完成: ${item.title}`)
  // 标记为完成
  const index = todos.value.findIndex(t => t.id === item.id)
  if (index !== -1) {
    todos.value[index].status = 'completed'
  }
}

const handleEdit = (item: Todo) => {
  message.info(`编辑: ${item.title}`)
}

const handleQuickAction = (action: any) => {
  message.info(`快捷操作: ${action.label}`)
  // 这里可以添加实际的导航或操作逻辑
}

onMounted(() => {
  chartLoading.value = false
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

.stats-card {
  height: 100%;
}

.stat-icon {
  font-size: 24px;
  margin-right: 8px;
}

.stat-trend {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.trend-badge {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.trend-badge.up {
  background-color: #f6ffed;
  color: #52c41a;
}

.trend-badge.down {
  background-color: #fff2f0;
  color: #ff4d4f;
}

.chart-card {
  min-height: 200px;
}

.chart-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: #999;
}

.todo-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.todo-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  margin-bottom: 4px;
}

.todo-time {
  color: #999;
}

.todo-priority {
  padding: 2px 6px;
  border-radius: 3px;
  background-color: #f5f5f5;
}

.system-danger {
  margin-top: 16px;
}

.system-danger .ant-alert {
  border: none;
  background-color: #fffbe6;
}

.system-danger .ant-list-item {
  padding: 8px 12px;
}

.view-all {
  font-size: 13px;
}
</style>