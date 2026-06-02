<template>
  <div class="fixed-asset-module">
    <a-card>
      <a-tabs v-model:activeKey="activeKey" @change="onTabChange">
        <a-tab-pane key="asset" tab="资产列表">
          <AssetList />
        </a-tab-pane>
        <a-tab-pane key="category" tab="分类管理">
          <CategoryList />
        </a-tab-pane>
        <a-tab-pane key="depreciation" tab="折旧管理">
          <DepreciationList />
        </a-tab-pane>
        <a-tab-pane key="transfer" tab="资产转移">
          <TransferList />
        </a-tab-pane>
        <a-tab-pane key="disposal" tab="资产处置">
          <DisposalList />
        </a-tab-pane>
        <a-tab-pane key="inventory" tab="资产盘点">
          <InventoryList />
        </a-tab-pane>
        <a-tab-pane key="report" tab="统计报表">
          <ReportPage />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AssetList from './asset/index.vue'
import CategoryList from './category/index.vue'
import DepreciationList from './depreciation/index.vue'
import TransferList from './transfer/index.vue'
import DisposalList from './disposal/index.vue'
import InventoryList from './inventory/index.vue'
import ReportPage from './report/index.vue'

const activeKey = ref('asset')

function onTabChange(key: string) {
  // Tab changed
}
</script>

<style scoped>
.fixed-asset-module {
  padding: 16px;
}
</style>
