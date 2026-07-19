<template>
  <PartnerListPage
    partner-type="supplier"
    api-partner-type="SUPPLIER"
    page-title="供应商"
    category-title="供应商分类"
    search-placeholder="请输入供应商编号/名称"
    :columns="columns"
    :tabs="tabs"
    :show-status-filter="true"
    :page-size-options="[20, 50, 100]"
    :default-page-size="20"
    form-route="/md/supplier"
    edit-form-route="/md/supplier/form/:id"
  >
    <!-- 自定义搜索栏 -->
    <template #search-fields>
      <div class="search-row">
        <div class="search-item">
          <span class="search-label">筛选条件</span>
          <a-input
            v-model:value="searchForm.keyword"
            placeholder="请输入供应商编号/名称"
            size="small"
            style="width: 200px"
            allow-clear
            @press-enter="handleSearch"
          />
        </div>
        <div class="search-item">
          <span class="search-label">显示状态</span>
          <a-select
            v-model:value="searchForm.status"
            size="small"
            style="width: 120px"
            placeholder="已启用"
            allow-clear
          >
            <a-select-option value="">
              全部
            </a-select-option>
            <a-select-option value="ENABLED">
              已启用
            </a-select-option>
            <a-select-option value="DISABLED">
              已停用
            </a-select-option>
          </a-select>
        </div>
        <a-button
          type="primary"
          size="small"
          class="btn-search"
          @click="handleSearch"
        >
          查询
        </a-button>
        <a-checkbox
          v-model:checked="showHierarchy"
          size="small"
          style="margin-left: 12px"
        >
          显示层次结构
        </a-checkbox>
      </div>
    </template>

    <!-- 自定义操作列 -->
    <template #actionCell="{ record }">
      <a-space :size="0">
        <a-button
          type="link"
          size="small"
          @click="handleOrder(record)"
        >
          订货
        </a-button>
        <a-button
          type="link"
          size="small"
          @click="handleEdit(record)"
        >
          修改
        </a-button>
        <a-dropdown>
          <a-button
            type="link"
            size="small"
          >
            更多
          </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item @click="handleView(record)">
                查看详情
              </a-menu-item>
              <a-menu-item
                danger
                @click="handleDelete(record)"
              >
                删除
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-space>
    </template>

    <!-- 名称列 -->
    <template #nameCell="{ record }">
      <a
        class="cell-link"
        @click="handleView(record)"
      >{{ record.partnerName }}</a>
    </template>

    <!-- 复选框过滤 -->
    <template #checkbox-filters>
      <a-checkbox v-model:checked="showAsCustomer">
        显示客户中的供应商
      </a-checkbox>
    </template>
  </PartnerListPage>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { partnerApi } from '@/api/erp/partner'

const router = useRouter()

const tabs = [
  { key: 'all', label: '全部供应商' },
]

const searchForm = reactive({
  keyword: '',
  status: 'ENABLED' as string,
})

const showHierarchy = ref(false)
const showAsCustomer = ref(false)

function handleSearch() {
  window.dispatchEvent(new CustomEvent('partner-search', { detail: { ...searchForm, showHierarchy: showHierarchy.value, showAsCustomer: showAsCustomer.value } }))
}

function handleOrder(record: any) {
  // 订货：跳转到采购订单新增页，预填供应商信息
  // 打开采购单弹窗或跳转至采购订单页
  router.push({ path: '/erp/purchase/form', query: { supplierId: record.id, supplierName: record.partnerName } })
}

const editFormRoute = '/md/supplier/form/:id'

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
  { key: 'partnerCode', title: '供应商编号', type: 'input', width: 120, sortable: true },
  { key: 'partnerName', title: '供应商名称', type: 'slot', slotName: 'nameCell', width: 200, sortable: true },
  { key: 'contactPerson', title: '联系人', type: 'input', width: 100 },
  { key: 'contactPhone', title: '联系电话', type: 'input', width: 130 },
  { key: 'createTime', title: '新增时间', type: 'input', width: 110, sortable: true },
  { key: 'attachmentCount', title: '附件', type: 'slot', slotName: 'attachmentCell', width: 70 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  { key: 'businessSeries', title: '经营系列', type: 'input', width: 120 },
  { key: 'businessArea', title: '经营面积', type: 'input', width: 100 },
]

</script>

<style scoped>
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.btn-search {
  margin-left: 8px;
}
.cell-link {
  color: #1890ff;
  cursor: pointer;
}
.cell-link:hover {
  text-decoration: underline;
}
</style>
