<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'
import SearchBar from '@/components/common/SearchBar.vue'
import CategoryNav from '@/components/home/CategoryNav.vue'
import BannerSwiper from '@/components/home/BannerSwiper.vue'
import ProductGrid from '@/components/product/ProductGrid.vue'

const router = useRouter()

const banners = ref<any[]>([])
const categories = ref<any[]>([])
const hotProducts = ref<any[]>([])
const recommendations = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })

  try {
    const [bannerRes, categoryRes, hotRes, recommendRes] = await Promise.all([
      api.product.getBanners(),
      api.product.getCategories(),
      api.product.getHotProducts(),
      api.product.getRecommendations()
    ])

    banners.value = (bannerRes?.data?.length > 0) ? bannerRes.data : (Array.isArray(bannerRes) ? bannerRes : [])
    categories.value = categoryRes?.data || (Array.isArray(categoryRes) ? categoryRes : [])
    hotProducts.value = hotRes?.data || (Array.isArray(hotRes) ? hotRes : [])
    recommendations.value = recommendRes?.data || (Array.isArray(recommendRes) ? recommendRes : [])
  } catch (err: any) {
    banners.value = []
    categories.value = []
    hotProducts.value = []
    recommendations.value = []
  } finally {
    loading.value = false
    closeToast()
  }
})

const handleSearch = (keyword: string) => {
  router.push({ path: '/search', query: { keyword } })
}

const handleCategoryClick = (category: any) => {
  router.push({ path: '/category/' + category.id })
}

const handleProductClick = (product: any) => {
  router.push({ path: '/product/' + product.id })
}
</script>

<template>
  <div class="home-page">
    <SearchBar @search="handleSearch" />

    <BannerSwiper :banners="banners" />

    <CategoryNav
      :categories="categories"
      @click="handleCategoryClick"
    />

    <div class="section">
      <div class="section-header"><h3>热门商品</h3></div>
      <ProductGrid :products="hotProducts" @click="handleProductClick" />
    </div>

    <div class="section">
      <div class="section-header"><h3>推荐商品</h3></div>
      <ProductGrid :products="recommendations" @click="handleProductClick" />
    </div>
  </div>
</template>

<style scoped>
.home-page { padding-bottom: 60px; background: #f5f5f5; min-height: 100vh }
.section { margin-top: 12px; background: #fff; padding: 12px 0 }
.section-header { padding: 0 16px; margin-bottom: 8px }
.section-header h3 { margin: 0; font-size: 16px; color: #333 }
</style>
