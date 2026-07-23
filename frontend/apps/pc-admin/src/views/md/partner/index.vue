<template>
  <PartnerListPage
    partner-type="partner"
    api-partner-type="OTHER"
    page-title="其他往来单位"
    category-title="单位分类"
    search-placeholder="单位名称/编码/联系人"
    :columns="columns"
    :tabs="tabs"
    :show-status-filter="true"
    form-route="/md/partner"
    edit-form-route="/md/partner/form/:id"
  >
    <!-- 自定义搜索栏 -->
    <template #search-fields>
      <div class="search-row">
        <div class="search-item">
          <span class="search-label">关键字</span>
          <a-input
            v-model:value="searchForm.keyword"
            placeholder="单位名称/编码/联系人"
            size="small"
            style="width: 200px"
            allow-clear
            @press-enter="handleSearch"
          />
        </div>
        <div class="search-item">
          <span class="search-label">类别</span>
          <a-select
            v-model:value="searchForm.category"
            size="small"
            style="width: 140px"
            placeholder="全部"
            allow-clear
          >
            <a-select-option
              v-for="(label, key) in categoryMap"
              :key="key"
              :value="key"
            >
              {{ label }}
            </a-select-option>
          </a-select>
        </div>
        <div class="search-item">
          <span class="search-label">状态</span>
          <a-select
            v-model:value="searchForm.status"
            size="small"
            style="width: 100px"
            placeholder="全部"
            allow-clear
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="ENABLED">已启用</a-select-option>
            <a-select-option value="DISABLED">已停用</a-select-option>
          </a-select>
        </div>
        <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
      </div>
    </template>

    <!-- 操作列 -->
    <template #actionCell="{ record }">
      <a-space :size="0">
        <a-button type="link" size="small" @click="handleEdit(record)">修改</a-button>
        <a-dropdown>
          <a-button type="link" size="small">更多</a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item @click="handleView(record)">查看详情</a-menu-item>
              <a-menu-item danger @click="handleDelete(record)">删除</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-space>
    </template>

    <!-- 名称列 -->
    <template #nameCell="{ record }">
      <a class="cell-link" @click="handleView(record)">{{ record.partnerName }}</a>
    </template>
  </PartnerListPage>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import PartnerListPage from '../components/PartnerListPage.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { partnerApi } from '@/api/erp/partner'

const router = useRouter()

const tabs = [
  { key: 'all', label: '全部往来单位' },
]

const categoryMap: Record<string, string> = {
  REPAIR: '维修合作方',
  TRAINING: '培训公司',
  FINANCE: '财务公司',
  LEGAL: '法律服务',
  INSURANCE: '保险公司',
  IT: 'IT服务商',
  CONSULTING: '咨询公司',
  WAREHOUSE: '仓储合作方',
  INSPECTION: '质检机构',
  OTHER: '其他',
}

const searchForm = reactive({
  keyword: '',
  category: undefined as string | undefined,
  status: '' as string,
})

const editFormRoute = '/md/partner/form/:id'

function handleSearch() {
  window.dispatchEvent(new CustomEvent('partner-search', { detail: { ...searchForm } }))
}

function handleEdit(record: any) {
  const route = editFormRoute.replace(':id', String(record.id))
  router.push(route)
}

function handleView(record: any) {
  handleEdit(record)
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除 "${record.partnerName}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await partnerApi.delete(record.id)
        message.success('删除成功')
        window.dispatchEvent(new CustomEvent('partner-refresh'))
      } catch {
        message.error('删除失败')
      }
    },
  })
}

const columns: DetailColumnConfig[] = [
  { key: 'partnerCode', title: '编码', type: 'input', width: 120, sortable: true },
  { key: 'partnerName', title: '单位名称', type: 'slot', slotName: 'nameCell', width: 200, sortable: true },
  { key: 'categoryName', title: '类别', type: 'input', width: 120, formatter: (v: any) => categoryMap[v] || v || '-' },
  { key: 'contactPerson', title: '联系人', type: 'input', width: 100 },
  { key: 'contactPhone', title: '联系电话', type: 'input', width: 130 },
  { key: 'detailAddress', title: '地址', type: 'input', width: 200 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 80 },
]
</script>

<style scoped>
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
</style>
