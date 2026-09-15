<template>
  <!--
    物流退货收货（配发收 → 收货业务 → 物流退货收货，菜单 70160，menu_code dispatch:return-receive）
    入口直达型：ql361 实测点击「物流退货收货」打开的就是「销售退货申请-历史」（menuId 300401 / billType 2734），
    因此本页**不重复造列表**，直接复用《销售退货申请》的列表视图组件：
      · 同一视图 = 同一份列配置（storage-key `sale-return-apply-table-columns-*`）与页面配置，跨入口口径一致；
      · 新增/编辑仍走销售域表单（/sales/return-apply/form），与本系统既有单据链路一致。
    见《物流退货收货开发文档》§1 与《配送模块 README》§4「入口直达现象」。
  -->
  <SalesReturnApplyList />
</template>

<script setup lang="ts">
import SalesReturnApplyList from '@/views/sales/return-apply/index.vue'

defineOptions({ name: 'DispatchReturnReceive' })
</script>
