<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showLoadingToast, closeToast } from 'vant'
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

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  
  try {
    const [bannerRes, categoryRes, hotRes, recommendRes] = await Promise.all([
      api.product.getRecommendations(),
      api.product.getCategories(),
      api.product.getHotProducts(),
      api.product.getRecommendations()
    ])
    
    banners.value = [
      { id: 1, image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=促销活动海报&image_size=landscape_16_9', url: '/product/1' },
      { id: 2, image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=新品上市海报&image_size=landscape_16_9', url: '/product/2' },
      { id: 3, image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=限时特惠海报&image_size=landscape_16_9', url: '/product/3' }
    ]
    categories.value = categoryRes.data || []
    hotProducts.value = hotRes.data || []
    recommendations.value = recommendRes.data || []
  } finally {
    closeToast()
  }
})

const handleSearch = (keyword: string) => {
  router.push({ path: '/search', query: { keyword } })
}

const handleCategoryClick = (categoryId: string) => {
  router.push(`/category/${categoryId}`)
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
      <div class="section-title">热销商品</div>
      <ProductGrid :products="hotProducts" />
    </div>
    
    <div class="section">
      <div class="section-title">为你推荐</div>
      <ProductGrid :products="recommendations" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.home-page {
  padding-bottom: 60px;
}

.section {
  margin: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 12px;
  }
}
</style>