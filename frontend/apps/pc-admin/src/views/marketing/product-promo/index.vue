<template>
  <!--
    商品促销（营销 → 营销活动 → 商品促销，菜单 80312）
    对标 ql361：单视图列表页，13 列（全部默认可见）
    与「整单促销」列口径完全一致，共用 PromoActivityPage（促销方式=商品促销）
    对标无「页面配置」弹窗；行级 查看 / 修改 / 更多
  -->
  <PromoActivityPage
    activity-type="PRODUCT"
    method-label="商品促销"
    table-storage-key="marketing-product-promo-table-columns"
  />
</template>

<script setup lang="ts">
import PromoActivityPage from '@/views/marketing/components/PromoActivityPage.vue'

defineOptions({ name: 'MarketingProductPromo' })
</script>
