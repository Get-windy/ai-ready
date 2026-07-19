<template>
  <PartnerListPage
    partner-type="partner"
    api-partner-type="OTHER"
    page-title="其他往来单位"
    category-title="单位分类"
    search-placeholder="单位名称/编码/联系人"
    :columns="columns"
    :tabs="tabs"
    form-route="/md/partner"
    edit-form-route="/md/partner/form?id=:id"
  />
</template>

<script setup lang="ts">
import PartnerListPage from '../components/PartnerListPage.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'

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
