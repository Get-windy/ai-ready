<template>
  <!--
    物流发货（配发收 → 发货业务 → 物流发货，菜单 70155，menu_code dispatch:logistics-ship）
    入口直达型：ql361 实测点击「物流发货」打开的是「订单处理中心」并选中 **2.拣货/发货**（menuId 47001 / billType 2341），
    因此本页**不重复造列表**，直接复用《订单处理中心》视图，并默认落在「拣货发货」阶段。
    见《物流发货开发文档》§1 与《配送模块 README》§4「入口直达现象」。
  -->
  <OrderCenterView />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import OrderCenterView from '@/views/sales/order-center/index.vue'

defineOptions({ name: 'DispatchLogisticsShip' })

const route = useRoute()
const router = useRouter()

// 复用视图在挂载时读取 route.query.tab，这里补齐「拣货发货」并触发其 watch 同步阶段
onMounted(() => {
  if (route.query.tab !== 'picking') {
    router.replace({ query: { ...route.query, tab: 'picking' } })
  }
})
</script>
