<template>
  <a-button type="primary" @click="handlePrint">
    <template #icon>
      <PrinterOutlined />
    </template>
    打印
  </a-button>
</template>

<script setup lang="ts">
import { PrinterOutlined } from '@ant-design/icons-vue'

const handlePrint = () => {
  console.log('打印功能')
}
</script>