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
    // 用真实存在的接口：/products/categories/{id}/products
    // （原写法 api.product.getByCategory 在 api/index.ts 里**根本没有**，
    //   每次进本页都抛 TypeError 再被 catch 成"三件假商品"——见审计报告"待修"第 1 条）
    const res: any = await api.product.getCategoryProducts(categoryId.value, {
      page: page.value,
      size: pageSize
    })

    // 后端返回 PageResult（records/total），不是 { list }
    const payload = res?.data ?? res
    const newProducts: any[] = payload?.records ?? []

    if (page.value === 1) {
      products.value = newProducts as any
    } else {
      products.value.push(...(newProducts as any))
    }

    // 分类名由后端按分类树填充到每条商品上（见 MallProductServiceImpl.fillCategoryNames）
    if (newProducts.length && newProducts[0].categoryName) {
      categoryName.value = newProducts[0].categoryName
    }

    if (newProducts.length < pageSize) {
      finished.value = true
    } else {
      page.value++
    }
  } catch (err: any) {
    // ⚠️ 不再用"商品示例1/2/3"兜底：接口挂了就该显示空态，而不是让用户
    //    以为商城里有这些不存在的商品（这批假数据随本次修复一并删除）。
    console.warn('[分类详情] 加载失败', err?.response?.data?.message || err?.message)
    products.value = []
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