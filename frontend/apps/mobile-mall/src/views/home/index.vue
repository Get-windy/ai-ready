<script setup lang="ts">
/**
 * 首页 —— 《商城App设计方案》§4.1（自上而下 8 个区块）。
 *
 *  1 标题栏（店铺名/主题色）  2 搜索栏（占位=热词）  3 顶部二级 Tab（推荐/新品/热销）
 *  4 公告滚动条              5 轮播               6 商品分类宫格
 *  7 双列瀑布流商品          8 未认证时的底部价格提示条
 *
 * 数据来源全部是**已就绪**的 C 端接口；未就绪的（装修下发、活动位、热词库搜索）
 * 用注释标出对应的设计文档编号，不造数据。
 */
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api'
import { useShopStore } from '@/stores/shop'
import { useUserStore } from '@/stores/user'
import SearchBar from '@/components/common/SearchBar.vue'
import BannerSwiper from '@/components/home/BannerSwiper.vue'
import TopTabs, { type TopTabItem } from '@/components/layout/TopTabs.vue'
import ProductCard from '@/components/product/ProductCard.vue'

const router = useRouter()
const shop = useShopStore()
const userStore = useUserStore()

/** 顶部二级 Tab：三期都是真实接口；「活动」需促销数据源（二期）故暂不上线 */
const topTabs: TopTabItem[] = [
  { key: 'recommend', name: '推荐' },
  { key: 'new', name: '新品' },
  { key: 'hot', name: '热销' }
]
const activeTab = ref('recommend')

const banners = ref<any[]>([])
// 后端 getCategories() 返回 Map 结构（id/name/icon），与 CategoryItem 类型不完全一致，
// 按实际结构用 any[] 承接，避免"类型对不上就到处加断言"的连锁改动。
const categories = ref<any[]>([])
const products = ref<any[]>([])
const listLoading = ref(false)

/** 店铺名与主题色（后台「商城设置 → 店铺设置」下发；见设计文档 §五 5.1） */
const shopName = computed(() => shop.config?.shopName || '企智连商城')
const themeColor = computed(() => shop.config?.themeColor || 'var(--mall-primary, #1988fa)')

/** 搜索栏占位：取公告之外的"热词"——一期先用店铺名兜底，待 F-15 接关键词库 */
const searchPlaceholder = computed(() => '搜索商品')

/** 公告滚动条文案（← mall_notice，已发布） */
const noticeText = computed(() => shop.notices.map(n => n.title).join('　·　'))

/** 分类宫格最多展示 10 个（5 列 × 2 行，与对标截图一致） */
const topCategories = computed(() => (categories.value || []).slice(0, 10))

/**
 * 分类图标是否是可用的图片地址。
 * 后端 `getCategories()` 目前把 icon 固定写死成字符串 `"default"`（不是 URL），
 * 直接塞进 `<img src>` 会发一个必然 404 的请求；故只认 http/https/`/` 开头的值，
 * 其余落到文字占位（取分类名前两字）。
 */
/**
 * 分类显示名。
 *
 * 后端 `getCategories()` 的 name 取值是 `COALESCE(mall_category_name, category)`，
 * 而真库这两个列**为空**时会回落成**分类 id**（一长串数字）—— 直接渲染就是
 * 「20 / 2072844513319059457」这种既不可读、又像 bug 的样子。
 * 这里统一兜成「未分类」：宁可显示"未分类"，也不显示裸 id。
 */
const catName = (c: any) => {
  const n = String(c?.name ?? '').trim()
  return !n || /^\d+$/.test(n) ? '未分类' : n
}

const isImageUrl = (v?: string) => !!v && /^(https?:)?\/\//.test(v) || !!v && v.startsWith('/')

/** 商品卡：把 tags 的 code 翻译成标签名（← erp_mall_tag） */
const decorate = (p: any) => ({
  id: p.id,
  name: p.productName || p.name || '',
  price: p.salePrice ?? p.price ?? null,
  originalPrice: p.marketPrice ?? p.originalPrice ?? null,
  image: p.imageUrl || p.image,
  stock: p.stockQuantity ?? p.stock,
  salesCount: p.salesCount ?? p.sales,
  spec: p.specification || p.spec,
  categoryTag: p.industryCategory || p.categoryName,
  tagNames: String(p.productTag || '')
    .split(',')
    .map((c: string) => c.trim())
    .filter(Boolean)
    .map((c: string) => shop.tagNameOf(c)),
  minOrderQty: p.minOrderQuantity,
  sellingPoint: p.description
})

async function loadTab(key: string) {
  listLoading.value = true
  try {
    let res: any
    if (key === 'recommend') {
      res = await api.product.getRecommendations()
    } else if (key === 'hot') {
      res = await api.product.getHotProducts()
    } else {
      // 新品：走商品列表（默认按商城排序 mall_sort_type 取数）
      res = await api.product.getList({ page: 1, size: 20 })
    }
    const payload = res?.data ?? res
    products.value = (payload?.records ?? payload ?? []).map(decorate)
  } catch (err: any) {
    console.warn('[首页] 商品加载失败', err?.response?.data?.message || err?.message)
    products.value = []
  } finally {
    listLoading.value = false
  }
}

async function loadCategories() {
  try {
    const res: any = await api.product.getCategories()
    const payload = res?.data ?? res
    categories.value = Array.isArray(payload) ? payload : (payload?.records ?? [])
  } catch {
    categories.value = []
  }
}

async function loadBanners() {
  try {
    const res: any = await api.product.getBanners()
    const payload = res?.data ?? res
    banners.value = Array.isArray(payload) ? payload : (payload?.records ?? [])
  } catch {
    banners.value = []
  }
}

// 首屏：分类/轮播一次；商品随 Tab 切换重取（不重建页面，只换列表）
loadCategories()
loadBanners()
loadTab(activeTab.value)
watch(activeTab, loadTab)

const handleSearch = (keyword: string) => {
  router.push({ path: '/search', query: keyword ? { keyword } : {} })
}
const goCategory = (c: any) => router.push(`/category/${c.id ?? c.categoryId}`)
const goProduct = (p: any) => router.push(`/product/${p.id}`)
const goLogin = () => router.push({ path: '/login', query: { redirect: '/' } })
</script>

<template>
  <div class="home-page">
    <!-- ① 标题栏 -->
    <div class="shop-header" :style="{ background: themeColor }">
      <img v-if="shop.config?.shopLogo" :src="shop.config.shopLogo" class="shop-logo" alt="" />
      <span class="shop-name">{{ shopName }}</span>
      <van-icon name="arrow-down" size="14" color="#fff" />
    </div>

    <!-- ② 搜索栏 -->
    <SearchBar :placeholder="searchPlaceholder" :background="themeColor" @search="handleSearch" />

    <!-- ③ 顶部二级 Tab -->
    <TopTabs v-model="activeTab" :tabs="topTabs" :active-color="themeColor" />

    <!-- ④ 公告滚动条 -->
    <div v-if="noticeText" class="notice-bar" @click="router.push('/notice')">
      <van-icon name="volume-o" color="#ed6a0c" />
      <span class="notice-text">{{ noticeText }}</span>
    </div>

    <!-- ⑤ 轮播 -->
    <BannerSwiper v-if="banners.length" :banners="banners as any" />

    <!-- ⑥ 商品分类宫格（= 商品分类一级，用户 2026-09-26 口径：分组即分类） -->
    <div v-if="topCategories.length" class="category-grid">
      <div
        v-for="c in topCategories"
        :key="c.id ?? c.categoryId"
        class="category-grid__item"
        @click="goCategory(c)"
      >
        <img
          v-if="isImageUrl(c.icon) || isImageUrl(c.image)"
          :src="isImageUrl(c.icon) ? c.icon : c.image"
          class="category-icon"
          alt=""
        />
        <div v-else class="category-icon category-icon--text">{{ catName(c).slice(0, 2) }}</div>
        <div class="category-name">{{ catName(c) }}</div>
      </div>
    </div>

    <!-- ⑦ 双列瀑布流 -->
    <div class="product-grid">
      <div v-for="p in products" :key="p.id" class="product-grid__cell">
        <ProductCard :product="p as any" @click="goProduct(p)" />
      </div>
      <van-empty v-if="!listLoading && !products.length" description="暂无商品" class="empty" />
    </div>

    <!-- ⑧ 未认证时的底部价格提示条（价格三态，见设计文档 §七） -->
    <div v-if="shop.priceHint" class="price-hint">
      <span class="price-hint__text">{{ shop.priceHint }}</span>
      <van-button
        v-if="!userStore.isLoggedIn"
        type="danger"
        size="small"
        round
        class="price-hint__btn"
        @click="goLogin"
      >
        去登录
      </van-button>
      <van-button v-else type="danger" size="small" round class="price-hint__btn" @click="router.push('/user')">
        去认证
      </van-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.shop-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px 4px;
  color: #fff;

  .shop-logo { width: 24px; height: 24px; border-radius: 4px; object-fit: cover; }
  .shop-name { font-size: 17px; font-weight: 600; }
}

.notice-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 8px 12px 0;
  padding: 8px 12px;
  background: #fffbe8;
  border-radius: 6px;

  .notice-text {
    flex: 1;
    font-size: 12px;
    color: #ed6a0c;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px 4px;
  margin: 12px;
  padding: 12px 8px;
  background: #fff;
  border-radius: 8px;

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    cursor: pointer;
  }

  .category-icon {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    object-fit: cover;
    background: #f2f3f5;

    &--text {
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      color: #646566;
      background: #f2f3f5;
    }
  }

  .category-name {
    margin-top: 6px;
    font-size: 12px;
    color: #323233;
    text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
  }
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  padding: 0 12px;

  &__cell { min-width: 0; }

  .empty { grid-column: span 2; }
}

/* 底部固定价格提示条：位于底部 Tab 栏之上 */
.price-hint {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 50px;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: rgba(0, 0, 0, 0.82);
  color: #fff;

  &__text { flex: 1; font-size: 13px; }
  &__btn { flex: 0 0 auto; }
}
</style>
