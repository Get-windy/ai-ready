<template>
  <!--
    整单促销（营销 → 营销活动 → 整单促销，菜单 80313）
    对标 ql361：单视图列表页，13 列（全部默认可见）
    与「商品促销」列口径完全一致，共用 PromoActivityPage（促销方式=整单促销）
    对标无「页面配置」弹窗；行级 查看 / 修改 / 更多
  -->
  <PromoActivityPage
    activity-type="ORDER"
    method-label="整单促销"
    table-storage-key="marketing-order-promo-table-columns"
  />
</template>

<script setup lang="ts">
import PromoActivityPage from '@/views/marketing/components/PromoActivityPage.vue'

defineOptions({ name: 'MarketingOrderPromo' })
</script>
