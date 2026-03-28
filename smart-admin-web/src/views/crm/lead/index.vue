<template>
  <div class="lead-page">
    <a-card title="线索管理">
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleAssign(record)">分配</a>
              <a @click="handleConvert(record)" v-if="record.status < 2">转化</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'

const loading = ref(false)
const dataSource = ref([
  { id: 1, name: '线索A', companyName: '公司A', contactName: '张三', phone: '13800138000', status: 0, score: 80 },
  { id: 2, name: '线索B', companyName: '公司B', contactName: '李四', phone: '13900139000', status: 1, score: 60 }
])

const columns = [
  { title: '线索名称', dataIndex: 'name', key: 'name' },
  { title: '公司', dataIndex: 'companyName', key: 'companyName' },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName' },
  { title: '电话', dataIndex: 'phone', key: 'phone' },
  { title: '评分', dataIndex: 'score', key: 'score' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' }
]

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'blue', 1: 'orange', 2: 'green', 3: 'red' }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '新线索', 1: '跟进中', 2: '已转化', 3: '已关闭' }
  return texts[status] || '未知'
}

const handleView = (record: any) => message.info('查看: ' + record.name)
const handleAssign = (record: any) => message.info('分配: ' + record.name)
const handleConvert = (record: any) => message.info('转化: ' + record.name)
</script>