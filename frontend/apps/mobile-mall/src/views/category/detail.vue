<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, List, PullRefresh, showLoadingToast, closeToast, Empty } from 'vant'
import { api, type ProductItem } from '@/api'
import ProductCard from '@/components/product/ProductCard.vue'

const router = useRouter()
const route = useRoute()

const categoryId = computed(() => route.params.id as string)
const categoryName = ref('分类详情')

const products = ref<ProductItem[]>([])
const loading = ref(false)
const refreshing = ref(false)
const finished = ref(false)
const page = ref(1)
const pageSize = 20

onMounted(async () => {
  loadProducts()
})

const loadProducts = async () => {
  if (loading.value) return
  
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true, duration: 0 })
  
  try {
    const res = await api.product.getByCategory(categoryId.value, {
      page: page.value,
      pageSize
    })
    
    const newProducts = res.data?.list || []
    
    if (page.value === 1) {
      products.value = newProducts
    } else {
      products.value.push(...newProducts)
    }
    
    categoryName.value = res.data?.categoryName || '分类详情'
    
    if (newProducts.length < pageSize) {
      finished.value = true
    } else {
      page.value++
    }
  } catch {
    products.value = [
      { id: 1, name: '商品示例1', price: 99.00, image: '', sales: 100 },
      { id: 2, name: '商品示例2', price: 199.00, image: '', sales: 50 },
      { id: 3, name: '商品示例3', price: 299.00, image: '', sales: 30 }
    ]
    finished.value = true
  } finally {
    loading.value = false
    closeToast()
  }
}

const onRefresh = async () => {
  refreshing.value = true
  page.value = 1
  finished.value = false
  await loadProducts()
  refreshing.value = false
}

const handleProductClick = (product: any) => {
  router.push(`/product/${product.id}`)
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="category-detail-page">
    <NavBar 
      :title="categoryName"
      left-arrow
      @click-left="goBack"
    />
    
    <PullRefresh v-model="refreshing" @refresh="onRefresh">
      <List
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadProducts"
      >
        <div v-if="products.length > 0" class="product-list">
          <ProductCard 
            v-for="product in products"
            :key="product.id"
            :product="product"
            @click="handleProductClick(product)"
          />
        </div>
        
        <Empty 
          v-else-if="!loading"
          description="暂无商品"
        />
      </List>
    </PullRefresh>
  </div>
</template>

<style lang="scss" scoped>
.category-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.product-list {
  padding: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  
  .product-card {
    width: calc(50% - 6px);
  }
}
</style>