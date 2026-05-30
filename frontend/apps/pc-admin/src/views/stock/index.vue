<template>
  <div class="stock-module">
    <div class="stock-module-header">
      <div class="stock-module-header-left">
        <h1 class="stock-module-title">库存管理</h1>
      </div>
    </div>

    <a-tabs
      v-model:activeKey="activeTab"
      class="stock-module-tabs"
      @change="handleTabChange"
    >
      <a-tab-pane key="stock" tab="库存查询">
        <StockTab />
      </a-tab-pane>
      <a-tab-pane key="inbound" tab="入库管理">
        <InboundTab />
      </a-tab-pane>
      <a-tab-pane key="outbound" tab="出库管理">
        <OutboundTab />
      </a-tab-pane>
      <a-tab-pane key="check" tab="库存盘点">
        <CheckTab />
      </a-tab-pane>
      <a-tab-pane key="transfer" tab="库存调拨">
        <TransferTab />
      </a-tab-pane>
      <a-tab-pane key="batch" tab="批次管理">
        <BatchTab />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import StockTab from './tabs/Stock.vue'
import InboundTab from './tabs/Inbound.vue'
import OutboundTab from './tabs/Outbound.vue'
import CheckTab from './tabs/Check.vue'
import TransferTab from './tabs/Transfer.vue'
import BatchTab from './tabs/Batch.vue'

const router = useRouter()
const route = useRoute()

const VALID_TABS = ['stock', 'inbound', 'outbound', 'check', 'transfer', 'batch']
const activeTab = ref('stock')

const handleTabChange = (key: string) => {
  router.replace({ query: { ...route.query, tab: key } })
}

onMounted(() => {
  const tabFromQuery = route.query.tab as string
  if (tabFromQuery && VALID_TABS.includes(tabFromQuery)) {
    activeTab.value = tabFromQuery
  }
})
</script>

<style scoped>
.stock-module {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
}

.stock-module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.stock-module-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stock-module-title {
  font-size: 20px;
  font-weight: 500;
  color: #303133;
  margin: 0;
}

.stock-module-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  margin: 16px;
  border-radius: 4px;
}

:deep(.stock-module-tabs .ant-tabs-nav) {
  margin-bottom: 0;
  padding: 0 24px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.stock-module-tabs .ant-tabs-content) {
  flex: 1;
  overflow: hidden;
}

:deep(.stock-module-tabs .ant-tabs-tabpane) {
  height: 100%;
  overflow: auto;
}
</style>
