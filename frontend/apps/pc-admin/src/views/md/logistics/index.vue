<template>
  <PartnerFormLayout active-key="logistics" page-title="物流公司">
    <div class="list-card">
      <div class="list-toolbar">
        <a-space>
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索物流公司名称/编码/联系人"
            style="width: 280px"
            size="small"
            @search="handleSearch"
          />
          <a-select v-model:value="searchStatus" size="small" style="width: 100px" @change="handleSearch">
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="1">启用</a-select-option>
            <a-select-option value="0">停用</a-select-option>
          </a-select>
        </a-space>
        <a-button type="primary" size="small" @click="router.push('/md/logistics')">
          <template #icon><PlusOutlined /></template>
          新增物流公司
        </a-button>
      </div>

      <VxeTableList
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :show-toolbar="false"
        :show-add="false"
        :show-search="false"
        :show-export="true"
        @page-change="handlePageChange"
      >
        <template #empty>
          <EmptyState image="no-data" title="暂无物流公司" description="点击新增物流公司按钮添加" add-text="新增物流公司" size="small" @add="router.push('/md/logistics')" />
        </template>
        <template #statusCell="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
        </template>
        <template #action="{ record }">
          <a-space :size="4">
            <a-button type="link" size="small">查看</a-button>
            <a-button type="link" size="small">编辑</a-button>
            <a-button type="link" size="small" danger>删除</a-button>
          </a-space>
        </template>
      </VxeTableList>
    </div>
  </PartnerFormLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PartnerFormLayout from '../components/PartnerFormLayout.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const searchKeyword = ref('')
const searchStatus = ref('')
const list = ref<any[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: true,
})

const columns = [
  { type: 'seq', title: '#', width: 50 },
  { field: 'logisticsCode', title: '物流公司编码', width: 120 },
  { field: 'logisticsName', title: '物流公司名称', minWidth: 140 },
  { field: 'contactPerson', title: '联系人', width: 100 },
  { field: 'contactPhone', title: '联系电话', width: 120 },
  { field: 'serviceArea', title: '服务区域', width: 120 },
  { field: 'logisticsType', title: '物流类型', width: 100 },
  { field: 'status', title: '状态', width: 80, slots: { default: 'statusCell' } },
  { title: '操作', width: 160, fixed: 'right', slots: { default: 'action' } },
]

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/logistics/page', {
      params: {
        keyword: searchKeyword.value || undefined,
        status: searchStatus.value || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      },
    })
    list.value = res?.records || []
    pagination.total = res?.total || 0
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.list-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
