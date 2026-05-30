<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { List, Empty, Loading } from 'vant'
import ProductCard from '@/components/product/ProductCard.vue'

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

const props = withDefaults(defineProps<{
  products: Product[]
  columns?: number
  loading?: boolean
  finished?: boolean
  error?: boolean
}>(), {
  columns: 2,
  loading: false,
  finished: false,
  error: false
})

const emit = defineEmits<{
  load: []
}>()

const loadingProxy = computed({
  get: () => props.loading,
  set: () => {
    // Parent controls loading state via the @load event flow.
    // The List component may try to set this, which we ignore.
  }
})

const router = useRouter()

const handleProductClick = (product: Product) => {
  router.push(`/product/${product.id}`)
}
</script>

<template>
  <div class="product-grid">
    <!-- Empty state -->
    <Empty
      v-if="!loading && products.length === 0 && !error"
      description="暂无商品"
    />

    <!-- Error state -->
    <div v-else-if="error && products.length === 0" class="grid-state">
      <Empty description="加载失败，请重试" />
    </div>

    <!-- Product grid -->
    <template v-else>
      <List
        v-model:loading="loadingProxy"
        :finished="finished"
        finished-text="没有更多了"
        :immediate-check="false"
        @load="emit('load')"
      >
        <div class="grid-container" :class="`grid-columns-${columns}`">
          <ProductCard
            v-for="product in products"
            :key="product.id"
            :product="product"
            class="grid-item"
            @click="handleProductClick(product)"
          />
        </div>

        <!-- Inline loading spinner when products exist -->
        <div v-if="loading && products.length > 0" class="grid-loading">
          <Loading type="spinner" size="20" />
          <span class="loading-text">加载中...</span>
        </div>
      </List>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.product-grid {
  min-height: 80px;
}

.grid-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 0;

  &.grid-columns-2 {
    .grid-item {
      width: calc(50% - 5px);
    }
  }

  &.grid-columns-3 {
    .grid-item {
      width: calc(33.33% - 7px);
    }
  }

  &.grid-columns-1 {
    .grid-item {
      width: 100%;
    }
  }
}

.grid-state {
  padding: 40px 0;
}

.grid-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 0;
  gap: 8px;

  .loading-text {
    font-size: 13px;
    color: #969799;
  }
}
</style>
