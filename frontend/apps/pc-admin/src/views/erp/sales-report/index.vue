<template>
  <div class="sales-report-page">
    <a-card title="销售报表">
      <!-- Tab切换 -->
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane
          key="statistics"
          tab="销售统计"
        >
          <SalesStatistics />
        </a-tab-pane>
        <a-tab-pane
          key="customer"
          tab="客户排行"
        >
          <CustomerRanking />
        </a-tab-pane>
        <a-tab-pane
          key="product"
          tab="商品排行"
        >
          <ProductRanking />
        </a-tab-pane>
        <a-tab-pane
          key="trend"
          tab="销售趋势"
        >
          <SalesTrend />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import SalesStatistics from './components/SalesStatistics.vue'
import CustomerRanking from './components/CustomerRanking.vue'
import ProductRanking from './components/ProductRanking.vue'
import SalesTrend from './components/SalesTrend.vue'

const activeTab = ref('statistics')
</script>

<style scoped>
.sales-report-page {
  padding: 24px;
}
</style>