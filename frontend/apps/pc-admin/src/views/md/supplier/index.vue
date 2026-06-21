<template>
  <PartnerFormLayout active-key="supplier" page-title="供应商">
    <div class="list-card">
      <div class="list-toolbar">
        <a-space>
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索供应商名称/编码/联系人"
            style="width: 280px"
            size="small"
            @search="handleSearch"
          />
          <a-select v-model:value="searchStatus" size="small" style="width: 100px" @change="handleSearch">
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="1">正常</a-select-option>
            <a-select-option value="0">停用</a-select-option>
          </a-select>
        </a-space>
        <a-button type="primary" size="small" @click="router.push('/md/supplier')">
          <template #icon><PlusOutlined /></template>
          新增供应商
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
          <EmptyState image="no-data" title="暂无供应商" description="点击新增供应商按钮添加" add-text="新增供应商" size="small" @add="router.push('/md/supplier')" />
        </template>
        <template #statusCell="{ record }">
          <a-tag :color="record.cooperationStatus === 1 ? 'green' : record.cooperationStatus === 2 ? 'orange' : 'red'">
            {{ record.cooperationStatus === 1 ? '正常合作' : record.cooperationStatus === 2 ? '暂停合作' : record.cooperationStatus === 3 ? '终止合作' : '潜在' }}
          </a-tag>
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
import { supplierApi } from '@/api/supplier'
import type { Supplier } from '@/api/supplier'

const router = useRouter()
const loading = ref(false)
const searchKeyword = ref('')
const searchStatus = ref('')
const list = ref<Supplier[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: true,
})

const columns = [
  { type: 'seq', title: '#', width: 50 },
  { field: 'supplierCode', title: '供应商编码', width: 120 },
  { field: 'supplierName', title: '供应商名称', minWidth: 140 },
  { field: 'contactPerson', title: '联系人', width: 100 },
  { field: 'contactPhone', title: '联系电话', width: 120 },
  { field: 'supplierLevel', title: '等级', width: 80 },
  { field: 'cooperationStatus', title: '合作状态', width: 100, slots: { default: 'statusCell' } },
  { title: '操作', width: 160, fixed: 'right', slots: { default: 'action' } },
]

async function fetchList() {
  loading.value = true
  try {
    const res = await supplierApi.page({
      keyword: searchKeyword.value || undefined,
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
