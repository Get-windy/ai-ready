<template>
  <!--
    配送 → 人车管理 → 人员核验
    人的合规：① 实名认证（身份/审核流/接单资质/外部平台背书）② 证照核验（驾驶证/健康证/从业资格证/渠道背书到期清单）
    边界：**不含车辆检查**——车况检查（出车前/收车后/抽检/定检）归《用车管理 → 车辆巡检》，
         车辆证照（行驶证/营运证/保险/年检）归《车辆管理》，避免同一份数据两个入口。
    实现：复用《用车管理》页的同一套组件（VerificationTabs），按 scope 过滤 Tab，不复制代码。
  -->
  <VerificationTabs scope="kyc" />
</template>

<script setup lang="ts">
import VerificationTabs from './components/VerificationTabs.vue'

defineOptions({ name: 'DmsVerification' })
</script>
