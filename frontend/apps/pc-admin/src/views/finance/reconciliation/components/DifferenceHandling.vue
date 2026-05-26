<template>
  <div class="difference-handling">
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="handleAdjust(record)"
            >
              调整
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="handleIgnore(record)"
            >
              忽略
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'

interface DifferenceRecord {
  id: number
  type: string
  description: string
  amount: number
  status: number
}

const loading = ref(false)
const dataSource = ref<DifferenceRecord[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true
})

const columns = [
  { title: '差异类型', dataIndex: 'type', key: 'type' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', width: 150 }
]

const handleAdjust = (record: DifferenceRecord) => {
  message.info(`调整差异: ${record.description}`)
}

const handleIgnore = (record: DifferenceRecord) => {
  message.success(`已忽略差异: ${record.description}`)
}

loading.value = true
setTimeout(() => {
  dataSource.value = [
    {
      id: 1,
      type: '金额不一致',
      description: '系统与银行流水金额不符',
      amount: 100,
      status: 0
    }
  ]
  loading.value = false
}, 500)
</script>

<style scoped>
.difference-handling {
  padding: 16px;
}
</style>