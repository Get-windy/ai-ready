<template>
  <PartnerListPage
    partner-type="customer"
    api-partner-type="CUSTOMER"
    page-title="客户"
    category-title="客户分类"
    search-placeholder="请输入客户编号/名称"
    :columns="columns"
    :tabs="tabs"
    :show-status-filter="false"
    :page-size-options="[20, 50, 100]"
    :default-page-size="50"
    form-route="/md/customer"
    edit-form-route="/md/customer/form/:id"
  >
    <!-- 自定义搜索栏 -->
    <template #search-fields>
      <div class="search-row">
        <div class="search-item">
          <span class="search-label">筛选条件</span>
          <a-input
            v-model:value="searchForm.keyword"
            placeholder="请输入客户编号/名称"
            size="small"
            style="width: 180px"
            allow-clear
            @press-enter="handleSearch"
          />
        </div>
        <div class="search-item">
          <span class="search-label">联系地址</span>
          <a-input
            v-model:value="searchForm.address"
            placeholder=""
            size="small"
            style="width: 140px"
            allow-clear
          />
        </div>
        <div class="search-item">
          <span class="search-label">新增日期（起）</span>
          <a-date-picker
            v-model:value="searchForm.createTimeStart"
            size="small"
            style="width: 140px"
            placeholder=""
            format="YYYY-MM-DD"
          />
        </div>
        <div class="search-item">
          <span class="search-label">新增日期（止）</span>
          <a-date-picker
            v-model:value="searchForm.createTimeEnd"
            size="small"
            style="width: 140px"
            placeholder=""
            format="YYYY-MM-DD"
          />
        </div>
        <div class="search-item">
          <span class="search-label">最近交易（起）</span>
          <a-date-picker
            v-model:value="searchForm.lastTradeStart"
            size="small"
            style="width: 140px"
            placeholder=""
            format="YYYY-MM-DD"
          />
        </div>
        <div class="search-item">
          <span class="search-label">最近交易（止）</span>
          <a-date-picker
            v-model:value="searchForm.lastTradeEnd"
            size="small"
            style="width: 140px"
            placeholder=""
            format="YYYY-MM-DD"
          />
        </div>
        <div class="search-item">
          <span class="search-label">结款方式</span>
          <a-select
            v-model:value="searchForm.settleType"
            size="small"
            style="width: 100px"
            placeholder="全部"
            allow-clear
          >
            <a-select-option value="">
              全部
            </a-select-option>
            <a-select-option value="CASH">
              现结
            </a-select-option>
            <a-select-option value="MONTHLY">
              月结
            </a-select-option>
            <a-select-option value="WEEKLY">
              周结
            </a-select-option>
            <a-select-option value="ADVANCE">
              预付
            </a-select-option>
          </a-select>
        </div>
      </div>
      <div class="search-row second-row">
        <div class="search-item">
          <span class="search-label">默认经手人</span>
          <a-input
            v-model:value="searchForm.handler"
            placeholder=""
            size="small"
            style="width: 120px"
            allow-clear
          >
            <template #suffix>
              <SearchOutlined style="color: #bbb; cursor: pointer;" />
            </template>
          </a-input>
        </div>
        <div class="search-item">
          <span class="search-label">所属区域</span>
          <a-input
            v-model:value="searchForm.region"
            placeholder=""
            size="small"
            style="width: 120px"
            allow-clear
          >
            <template #suffix>
              <SearchOutlined style="color: #bbb; cursor: pointer;" />
            </template>
          </a-input>
        </div>
        <div class="search-item">
          <span class="search-label">显示状态</span>
          <a-select
            v-model:value="searchForm.status"
            size="small"
            style="width: 100px"
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

    <!-- 自定义操作列 -->
    <template #actionCell="{ record }">
      <a-space :size="0">
        <a-button
          type="link"
          size="small"
          @click="handlePlaceOrderForCustomer(record)"
        >
          代客下单
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
      <a-checkbox v-model:checked="checkboxFilters.mallAccount">
        只显示开通商城账号
      </a-checkbox>
      <a-checkbox v-model:checked="checkboxFilters.noSales">
        只显示无销售记录客户
      </a-checkbox>
      <a-checkbox v-model:checked="checkboxFilters.isSupplier">
        显示供应商中的客户
      </a-checkbox>
    </template>

    <!-- 自定义工具栏左侧 -->
    <template #toolbar-left>
      <a-button
        size="small"
        @click="handleSendMessage"
      >
        <MessageOutlined /> 发短信
      </a-button>
      <a-button
        size="small"
        @click="handleSendCoupon"
      >
        <GiftOutlined /> 发优惠券
      </a-button>
    </template>
  </PartnerListPage>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, MessageOutlined, GiftOutlined } from '@ant-design/icons-vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { partnerApi } from '@/api/erp/partner'
import PartnerListPage from '../components/PartnerListPage.vue'

const router = useRouter()

const tabs = [
  { key: 'all', label: '全部客户' },
  { key: 'member', label: '会员管理' },
  { key: 'contact', label: '全部联系人' },
  { key: 'grade', label: '客户级别' },
  { key: 'region', label: '区域管理' },
]

// 复选框过滤状态
const checkboxFilters = reactive({
  mallAccount: false,
  noSales: false,
  isSupplier: false,
})

// 自定义搜索表单
const searchForm = reactive({
  keyword: '',
  address: '',
  createTimeStart: null as any,
  createTimeEnd: null as any,
  lastTradeStart: null as any,
  lastTradeEnd: null as any,
  settleType: '' as string,
  handler: '',
  region: '',
  status: '' as string,
})

function handleSearch() {
  // 搜索逻辑由 PartnerListPage 处理，这里可触发父组件事件
  window.dispatchEvent(new CustomEvent('partner-search', { detail: { ...searchForm, ...checkboxFilters } }))
}

function handlePlaceOrderForCustomer(record: any) {
  // 代客下单：跳转到销售订单新增页，预填客户信息
  // Ref: Odoo 18.0 Sale Order - Create order from partner context
  const route = router.resolve({
    path: '/erp/sale/form',
    query: { customerId: record.id, customerName: record.partnerName }
  })
  window.open(route.href, '_blank')
}

const formRoute = '/md/customer/form'
const editFormRoute = '/md/customer/form/:id'

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

// 客户级别映射
const gradeMap: Record<string, string> = {
  'A餐饮客户': 'A餐饮客户',
  '校园餐饮': '校园餐饮',
  '重点客户': '重点客户',
  '特价客户': '特价客户',
  '会员客户|个人': '会员客户|个人',
}

// 结款方式映射
const settleTypeMap: Record<string, string> = {
  CASH: '现结',
  MONTHLY: '月结',
  WEEKLY: '周结',
  ADVANCE: '预付',
}

const columns: DetailColumnConfig[] = [
  { key: 'partnerCode', title: '客户编号', type: 'input', width: 140, sortable: true },
  { key: 'partnerName', title: '客户名称', type: 'slot', slotName: 'nameCell', width: 220, sortable: true },
  {
    key: 'settleType', title: '结款方式', type: 'input', width: 90,
    formatter: (v: any) => settleTypeMap[v] || v || '-',
  },
  { key: 'gradeName', title: '客户级别', type: 'input', width: 120, sortable: true },
  { key: 'warehouseName', title: '所属仓库', type: 'input', width: 100 },
  { key: 'region', title: '所属区域', type: 'input', width: 100 },
  { key: 'handlerName', title: '默认经手人', type: 'input', width: 100 },
  { key: 'promoterName', title: '推广人', type: 'input', width: 100 },
  { key: 'contactPerson', title: '联系人', type: 'input', width: 100 },
  { key: 'mnemonicCode', title: '助记码', type: 'input', width: 100 },
  { key: 'phone', title: '联系电话', type: 'input', width: 130 },
  { key: 'address', title: '联系地址', type: 'input', width: 200 },
  { key: 'buyerAccount', title: '买家账号', type: 'input', width: 120 },
  { key: 'customerOnePass', title: '客户一票通', type: 'input', width: 120 },
  { key: 'dynamicPaymentTerm', title: '动态收款期限(天)', type: 'number', width: 140 },
  { key: 'fixedPaymentTerm', title: '固定账期', type: 'input', width: 100 },
  { key: 'settlementPeriod', title: '结算期', type: 'input', width: 100 },
  { key: 'bankName', title: '开户银行', type: 'input', width: 120 },
  { key: 'bankAccount', title: '银行账号', type: 'input', width: 140 },
  { key: 'taxNumber', title: '税号', type: 'input', width: 140 },
  { key: 'customerSource', title: '客户来源', type: 'input', width: 100 },
  { key: 'businessLicenseExpiry', title: '营业执照有效期', type: 'input', width: 130 },
  { key: 'lastTradeTime', title: '最近交易时间', type: 'input', width: 130 },
  { key: 'createTime', title: '新增时间', type: 'input', width: 130 },
  { key: 'attachment', title: '附件', type: 'input', width: 80 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
]

function handleSendMessage() {
  message.info('发短信功能待完善')
}

function handleSendCoupon() {
  message.info('发优惠券功能待完善')
}
</script>

<style scoped>
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-row.second-row {
  margin-top: 8px;
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
