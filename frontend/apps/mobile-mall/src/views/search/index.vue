<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, List, PullRefresh, Empty, Tag, showLoadingToast, closeToast } from 'vant'
import { api, type ProductItem } from '@/api'
import ProductCard from '@/components/product/ProductCard.vue'

const router = useRouter()
const route = useRoute()

const keyword = computed(() => route.query.keyword as string || '')
const searchValue = ref('')
const products = ref<ProductItem[]>([])
const loading = ref(false)
const refreshing = ref(false)
const finished = ref(false)
const page = ref(1)
const pageSize = 20

const hotKeywords = ref([
  '手机', '电脑', '耳机', '键盘', '显示器', '充电器', '数据线', '鼠标'
])

const historyKeywords = ref<string[]>([])

onMounted(() => {
  searchValue.value = keyword.value
  loadHistoryKeywords()
  
  if (keyword.value) {
    searchProducts()
  }
})

const loadHistoryKeywords = () => {
  const history = localStorage.getItem('searchHistory')
  if (history) {
    historyKeywords.value = JSON.parse(history)
  }
}

const saveHistoryKeyword = (kw: string) => {
  if (!kw) return
  
  const history = historyKeywords.value.filter(h => h !== kw)
  history.unshift(kw)
  historyKeywords.value = history.slice(0, 10)
  localStorage.setItem('searchHistory', JSON.stringify(historyKeywords.value))
}

const clearHistory = () => {
  historyKeywords.value = []
  localStorage.removeItem('searchHistory')
}

const searchProducts = async () => {
  if (!searchValue.value) return
  
  if (loading.value) return
  
  loading.value = true
  showLoadingToast({ message: '搜索中...', forbidClick: true, duration: 0 })
  
  try {
    const res = await api.product.search({
      keyword: searchValue.value,
      page: page.value,
      pageSize
    })
    
    const newProducts = res.data?.list || []
    
    if (page.value === 1) {
      products.value = newProducts
      saveHistoryKeyword(searchValue.value)
    } else {
      products.value.push(...newProducts)
    }
    
    if (newProducts.length < pageSize) {
      finished.value = true
    } else {
      page.value++
    }
  } catch {
    products.value = [
      { id: 1, name: '搜索结果示例1', price: 99.00, image: '', sales: 100 },
      { id: 2, name: '搜索结果示例2', price: 199.00, image: '', sales: 50 },
      { id: 3, name: '搜索结果示例3', price: 299.00, image: '', sales: 30 }
    ]
    finished.value = true
    saveHistoryKeyword(searchValue.value)
  } finally {
    loading.value = false
    closeToast()
  }
}

const onRefresh = async () => {
  refreshing.value = true
  page.value = 1
  finished.value = false
  await searchProducts()
  refreshing.value = false
}

const handleSearch = () => {
  page.value = 1
  finished.value = false
  products.value = []
  searchProducts()
}

const handleKeywordClick = (kw: string) => {
  searchValue.value = kw
  handleSearch()
}

const handleProductClick = (product: any) => {
  router.push(`/product/${product.id}`)
}
</script>

<template>
  <div class="search-page">
    <div class="search-header">
      <Search
        v-model="searchValue"
        placeholder="搜索商品"
        show-action
        @search="handleSearch"
        @cancel="router.back()"
      />
    </div>
    
    <div v-if="!searchValue && products.length === 0" class="search-suggest">
      <div class="suggest-section">
        <div class="section-header">
          <span class="section-title">热门搜索</span>
        </div>
        <div class="keyword-list">
          <Tag 
            v-for="kw in hotKeywords"
            :key="kw"
            type="primary"
            plain
            size="medium"
            @click="handleKeywordClick(kw)"
          >
            {{ kw }}
          </Tag>
        </div>
      </div>
      
      <div v-if="historyKeywords.length > 0" class="suggest-section">
        <div class="section-header">
          <span class="section-title">搜索历史</span>
          <span class="clear-btn" @click="clearHistory">清空</span>
        </div>
        <div class="keyword-list">
          <Tag 
            v-for="kw in historyKeywords"
            :key="kw"
            plain
            size="medium"
            @click="handleKeywordClick(kw)"
          >
            {{ kw }}
          </Tag>
        </div>
      </div>
    </div>
    
    <PullRefresh v-else v-model="refreshing" @refresh="onRefresh">
      <List
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="searchProducts"
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
          description="未找到相关商品"
        />
      </List>
    </PullRefresh>
  </div>
</template>

<style lang="scss" scoped>
.search-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.search-header {
  background: #fff;
}

.search-suggest {
  padding: 12px;
  
  .suggest-section {
    margin-bottom: 16px;
    
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      
      .section-title {
        font-size: 14px;
        font-weight: 600;
        color: #333;
      }
      
      .clear-btn {
        font-size: 12px;
        color: #969799;
      }
    }
    
    .keyword-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
  }
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