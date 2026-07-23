<template>
  <a-modal
    :open="open"
    title="选择商品"
    :width="640"
    :footer="null"
    @cancel="emit('update:open', false)"
  >
    <div class="picker-tip">
      “{{ keyword }}” 命中 {{ products.length }} 个商品，请选择：
    </div>
    <div class="picker-list">
      <div
        v-for="p in products"
        :key="p.id"
        class="picker-item"
        @click="handleSelect(p)"
      >
        <div class="picker-info">
          <div class="picker-name">{{ p.productName }}</div>
          <div class="picker-meta">
            编码 {{ p.productCode || '-' }}　条码 {{ p.barcode || '-' }}　库存 {{ p.availableStock ?? '-' }}
          </div>
        </div>
        <div class="picker-price">¥{{ fmtMoney(p.retailPrice ?? p.unitPrice) }}</div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import type { QuickProduct } from '@/api/retail'
import { fmtMoney } from './pos-shared'

defineProps<{
  open: boolean
  products: QuickProduct[]
  keyword: string
}>()

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'select', product: QuickProduct): void
}>()

function handleSelect(p: QuickProduct) {
  emit('select', p)
  emit('update:open', false)
}
</script>

<style scoped>
.picker-tip {
  color: #999;
  font-size: 12px;
  margin-bottom: 8px;
}
.picker-list {
  max-height: 400px;
  overflow-y: auto;
}
.picker-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.picker-item:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
}
.picker-name {
  font-size: 14px;
  color: #333;
}
.picker-meta {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.picker-price {
  font-size: 16px;
  font-weight: 600;
  color: #ff4d4f;
  font-family: 'SFMono-Regular', Consolas, monospace;
  white-space: nowrap;
  margin-left: 12px;
}
</style>
