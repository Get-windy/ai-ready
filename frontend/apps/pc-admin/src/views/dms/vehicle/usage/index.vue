<template>
  <!--
    配送 → 人车管理 → 用车管理
    一次用车的全过程：用车登记（出车登记+出车前检查 / 交车+收车后检查）→ 车辆巡检 → 车辆补能 → 核验预警
    说明：车辆补能是「一车/一人多条的从属流水」，故作为本页 Tab，不单独占菜单项；
         与《车辆维护》这类有维保号、有供应商/下次保养推算的**单据**区分开。
    实现：复用《人员核验》页的同一套组件（VerificationTabs），按 scope 过滤 Tab，不复制代码。
  -->
  <VerificationTabs scope="usage" />
</template>

<script setup lang="ts">
import VerificationTabs from '@/views/dms/verification/components/VerificationTabs.vue'

defineOptions({ name: 'DmsVehicleUsage' })
</script>
