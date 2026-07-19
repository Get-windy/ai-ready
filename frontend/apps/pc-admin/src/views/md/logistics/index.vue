<template>
  <PartnerListPage
    partner-type="logistics"
    api-partner-type="LOGISTICS"
    page-title="物流公司"
    category-title="物流分类"
    search-placeholder="请输入物流公司名称/编码/联系人/电话"
    :columns="columns"
    :tabs="tabs"
    :show-status-filter="true"
    :page-size-options="[20, 50, 100]"
    :default-page-size="20"
    form-route="/md/logistics"
    edit-form-route="/md/logistics/form/:id"
  >
    <!-- 自定义搜索栏 -->
    <template #search-fields>
      <div class="search-row">
        <div class="search-item">
          <span class="search-label">筛选条件</span>
          <a-input
            v-model:value="searchForm.keyword"
            placeholder="请输入物流公司名称/编码/联系人/电话"
            size="small"
            style="width: 220px"
            allow-clear
            @press-enter="handleSearch"
          />
        </div>
        <div class="search-item">
          <span class="search-label">物流类型</span>
          <a-select
            v-model:value="searchForm.logisticsType"
            size="small"
            style="width: 120px"
            placeholder="全部"
            allow-clear
          >
            <a-select-option value="">
              全部
            </a-select-option>
            <a-select-option value="EXPRESS">
              快递物流
            </a-select-option>
            <a-select-option value="LTL">
              零担物流
            </a-select-option>
            <a-select-option value="FTL">
              整车运输
            </a-select-option>
            <a-select-option value="COLD">
              冷链物流
            </a-select-option>
            <a-select-option value="HAZMAT">
              危化品运输
            </a-select-option>
            <a-select-option value="COMPREHENSIVE">
              综合物流
            </a-select-option>
          </a-select>
        </div>
        <div class="search-item">
          <span class="search-label">服务区域</span>
          <a-input
            v-model:value="searchForm.serviceArea"
            placeholder=""
            size="small"
            style="width: 120px"
            allow-clear
          />
        </div>
        <div class="search-item">
          <span class="search-label">显示状态</span>
          <a-select
            v-model:value="searchForm.status"
            size="small"
            style="width: 120px"
            placeholder="全部"
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
      </div>
    </template>
  </PartnerListPage>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'

const tabs = [
  { key: 'all', label: '全部物流公司' },
]

const searchForm = reactive({
  keyword: '',
  logisticsType: '' as string,
  serviceArea: '',
  status: '' as string,
})

function handleSearch() {
  window.dispatchEvent(new CustomEvent('partner-search', { detail: searchForm }))
}

const logisticsTypeMap: Record<string, string> = {
  EXPRESS: '快递物流',
  LTL: '零担物流',
  FTL: '整车运输',
  COLD: '冷链物流',
  HAZMAT: '危化品运输',
  COMPREHENSIVE: '综合物流',
}

const columns: DetailColumnConfig[] = [
  { key: 'partnerCode', title: '公司编码', type: 'input', width: 120, sortable: true },
  { key: 'partnerName', title: '公司名称', type: 'slot', slotName: 'nameCell', width: 180, sortable: true },
  {
    key: 'logisticsType', title: '物流类型', type: 'input', width: 100,
    formatter: (v: any) => logisticsTypeMap[v] || v || '-',
  },
  { key: 'serviceArea', title: '服务区域', type: 'input', width: 140 },
  { key: 'contactPerson', title: '联系人', type: 'input', width: 100 },
  { key: 'contactPhone', title: '联系电话', type: 'input', width: 130 },
  { key: 'province', title: '省份', type: 'input', width: 80 },
  { key: 'city', title: '城市', type: 'input', width: 80 },
  {
    key: 'settleType', title: '结算方式', type: 'input', width: 100,
    formatter: (v: any) => {
      const map: Record<string, string> = { MONTHLY: '月结', WEEKLY: '周结', CASH: '现结', ADVANCE: '预付' }
      return map[v] || v || '-'
    },
  },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 80 },
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
</style>
