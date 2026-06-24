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
            <a-select-option value="ENABLED">启用</a-select-option>
            <a-select-option value="DISABLED">停用</a-select-option>
          </a-select>
        </a-space>
        <a-button type="primary" size="small" @click="router.push('/md/logistics')">
          <template #icon><PlusOutlined /></template>
          新增物流公司
        </a-button>
      </div>

      <BillTableList
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
          <a-tag :color="record.status === 'ENABLED' ? 'green' : 'red'">{{ record.status === 'ENABLED' ? '启用' : '停用' }}</a-tag>
        </template>
        <template #action="{ record }">
          <a-space :size="4">
            <a-button type="link" size="small" @click="router.push(`/md/logistics/form?id=${record.id}`)">查看</a-button>
            <a-button type="link" size="small" @click="router.push(`/md/logistics/form?id=${record.id}`)">编辑</a-button>
            <a-popconfirm title="确定删除此物流公司？" @confirm="handleDelete(record.id)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </BillTableList>
    </div>
  </PartnerFormLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import PartnerFormLayout from '../components/PartnerFormLayout.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { partnerApi } from '@/api/erp/partner'
import type { Partner } from '@/api/erp/partner'

const router = useRouter()
const loading = ref(false)
const searchKeyword = ref('')
const searchStatus = ref('')
const list = ref<Partner[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: true,
})

const columns = [
  { type: 'seq', title: '#', width: 50 },
  { field: 'partnerCode', title: '物流公司编码', width: 120 },
  { field: 'partnerName', title: '物流公司名称', minWidth: 140 },
  { field: 'contactPerson', title: '联系人', width: 100 },
  { field: 'contactPhone', title: '联系电话', width: 120 },
  { field: 'detailAddress', title: '公司地址', width: 140 },
  { field: 'status', title: '状态', width: 80, slots: { default: 'statusCell' } },
  { title: '操作', width: 160, fixed: 'right', slots: { default: 'action' } },
]

async function fetchList() {
  loading.value = true
  try {
    const res = await partnerApi.page({
      keyword: searchKeyword.value || undefined,
      partnerType: 'LOGISTICS',
      status: searchStatus.value ? Number(searchStatus.value) : undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    list.value = res?.records || []
    pagination.total = res?.total || 0
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await partnerApi.delete(id)
    message.success('删除成功')
    fetchList()
  } catch {
    message.error('删除失败')
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
