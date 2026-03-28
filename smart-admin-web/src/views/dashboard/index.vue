<template>
  <div class="dashboard-page">
    <a-row :gutter="16">
      <a-col :span="6" v-for="item in stats" :key="item.title">
        <a-card>
          <a-statistic
            :title="item.title"
            :value="item.value"
            :prefix="item.prefix"
            :suffix="item.suffix"
          >
            <template #prefix>
              <component :is="item.icon" />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="16">
        <a-card title="销售趋势">
          <div class="chart-placeholder">
            <LineChartOutlined style="font-size: 48px; color: #d9d9d9" />
            <p>图表区域</p>
          </div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="待办事项">
          <a-list :data-source="todos" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :description="item.time">
                  <template #title>
                    <a @click="handleTodo(item)">{{ item.title }}</a>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  ShoppingCartOutlined,
  UserOutlined,
  TeamOutlined,
  DollarOutlined,
  LineChartOutlined
} from '@ant-design/icons-vue'

const stats = ref([
  { title: '今日订单', value: 128, icon: ShoppingCartOutlined },
  { title: '新增客户', value: 32, icon: UserOutlined },
  { title: '待处理线索', value: 15, icon: TeamOutlined },
  { title: '本月销售额', value: 125600, prefix: '¥', icon: DollarOutlined }
])

const todos = ref([
  { title: '审批采购订单 #PO2026032801', time: '10分钟前' },
  { title: '跟进客户"张三"的询价', time: '30分钟前' },
  { title: '处理库存预警', time: '1小时前' },
  { title: '确认销售订单发货', time: '2小时前' }
])

const handleTodo = (item: any) => {
  console.log('handle todo:', item)
}
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

.chart-placeholder {
  height: 300px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #999;
}
</style>