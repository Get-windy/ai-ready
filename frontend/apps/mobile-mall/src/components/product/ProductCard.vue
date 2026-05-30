<script setup lang="ts">
import { computed } from 'vue'
import { Image as VanImage } from 'vant'

interface Product {
  id: number
  name: string
  price: number
  originalPrice?: number
  image?: string
  images?: string[]
  sales?: number
  salesCount?: number
}

const props = defineProps<{
  product: Product
}>()

defineEmits<{
  click: []
}>()

const displayImage = computed(() => {
  return props.product.image || props.product.images?.[0] || ''
})

const salesText = computed(() => {
  const sales = props.product.sales ?? props.product.salesCount ?? 0
  if (sales >= 10000) {
    return `已售 ${(sales / 10000).toFixed(1)}万+`
  }
  if (sales > 0) {
    return `已售 ${sales}`
  }
  return ''
})

const formattedPrice = computed(() => {
  return props.product.price.toFixed(2)
})
</script>

<template>
  <div class="product-card" @click="$emit('click')">
    <div class="product-image">
      <VanImage
        :src="displayImage"
        fit="cover"
        lazy-load
        class="product-img"
      >
        <template #loading>
          <div class="image-placeholder">
            <van-icon name="photo-o" size="24" color="#c8c9cc" />
          </div>
        </template>
        <template #error>
          <div class="image-placeholder">
            <van-icon name="photo-fail" size="24" color="#c8c9cc" />
          </div>
        </template>
      </VanImage>
    </div>

    <div class="product-info">
      <div class="product-name">{{ product.name }}</div>

      <div class="product-price-row">
        <span class="product-price">
          <span class="price-symbol">&yen;</span>
          {{ formattedPrice }}
        </span>
        <span v-if="product.originalPrice" class="original-price">
          &yen;{{ product.originalPrice.toFixed(2) }}
        </span>
      </div>

      <div v-if="salesText" class="product-sales">{{ salesText }}</div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.15s ease;

  &:active {
    transform: scale(0.98);
  }
}

.product-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f0f1f3;

  .product-img {
    width: 100%;
    height: 100%;
    display: block;
  }
}

.image-placeholder {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f0f1f3;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-info {
  padding: 8px 10px 10px;
}

.product-name {
  font-size: 13px;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
  min-height: 36px;
}

.product-price-row {
  display: flex;
  align-items: baseline;
  margin-top: 6px;

  .product-price {
    font-size: 16px;
    font-weight: 700;
    color: #f44;

    .price-symbol {
      font-size: 12px;
    }
  }

  .original-price {
    font-size: 11px;
    color: #969799;
    text-decoration: line-through;
    margin-left: 6px;
  }
}

.product-sales {
  font-size: 11px;
  color: #969799;
  margin-top: 4px;
}
</style>
