<script setup lang="ts">
/**
 * 分类页 —— 《商城App设计方案》§4.2。
 *
 * 结构：
 *  · **最顶部 = 商品标签 Tab**（用户 2026-09-26 口径）：「全部」+ `erp_mall_tag` 启用的标签；
 *  · 「全部」→ 商品分类宫格（= 商品分类，用户口径：**商品分组即商品分类**），点进 `/category/:id`；
 *  · 选中某标签 → 该标签下的商品双列列表。
 *
 * 2026-09-26 同时修掉两个既有缺陷：
 *  1. 原实现把「电子产品/服装鞋帽/家居用品…」8 条**硬编码分类**当兜底数据，
 *     接口失败时用户看到的是**假分类**（点进去必然空）。现改为失败即空 + 明确空态。
 *  2. 原实现调用了**并不存在**的 `api.product.getSubCategories()`（api/index.ts 里没有这个方法）
 *     ⇒ 每次进分类页都抛 `TypeError: getSubCategories is not a function`，
 *     再被 catch 成"用假数据渲染"。现改为调用真实存在的 `getCategoryProducts`。
 */
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api'
import { useShopStore } from '@/stores/shop'
import TopTabs, { type TopTabItem } from '@/components/layout/TopTabs.vue'
import ProductCard from '@/components/product/ProductCard.vue'

const router = useRouter()
const shop = useShopStore()

/** 顶部标签栏：「全部」+ 后台启用的商品标签 */
const topTabs = computed<TopTabItem[]>(() => [
  { key: 'all', name: '全部' },
  ...shop.tags.map(t => ({ key: t.tagCode, name: t.tagName }))
])
const activeTab = ref('all')

const categories = ref<any[]>([])
const products = ref<any[]>([])
const loading = ref(false)

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
    .split(',').map((c: string) => c.trim()).filter(Boolean)
    .map((c: string) => shop.tagNameOf(c)),
  minOrderQty: p.minOrderQuantity,
  sellingPoint: p.description
})

async function loadCategories() {
  try {
    const res: any = await api.product.getCategories()
    const payload = res?.data ?? res
    categories.value = Array.isArray(payload) ? payload : []
  } catch (err: any) {
    console.warn('[分类] 分类加载失败', err?.response?.data?.message || err?.message)
    categories.value = []
  }
}

/** 切到某标签 → 拉该标签下的商品（后端 `GET /products?tagCode=` 支持） */
async function loadByTag(tagCode: string) {
  if (tagCode === 'all') {
    products.value = []
    return
  }
  loading.value = true
  try {
    const res: any = await api.product.getList({ page: 1, size: 20, tagCode })
    const payload = res?.data ?? res
    products.value = (payload?.records ?? payload ?? []).map(decorate)
  } catch (err: any) {
    console.warn('[分类] 标签商品加载失败', err?.response?.data?.message || err?.message)
    products.value = []
  } finally {
    loading.value = false
  }
}

loadCategories()
// 标签是异步从店铺 store 拿的，拿到后如果当前就在某个标签上，补一次查询
watch(() => shop.tags.length, () => { if (activeTab.value !== 'all') loadByTag(activeTab.value) })
watch(activeTab, loadByTag)

const goCategory = (c: any) => router.push(`/category/${c.id ?? c.categoryId}`)
const goProduct = (p: any) => router.push(`/product/${p.id}`)
</script>

<template>
  <div class="category-page">
    <!-- 最顶部：商品标签 Tab（用户口径） -->
    <TopTabs v-model="activeTab" :tabs="topTabs" />

    <!-- 「全部」→ 商品分类宫格 -->
    <div v-show="activeTab === 'all'" class="category-grid">
      <div
        v-for="c in categories"
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
      <van-empty v-if="!categories.length" description="暂无分类" class="empty" />
    </div>

    <!-- 选中标签 → 该标签下的商品 -->
    <div v-show="activeTab !== 'all'" class="tag-products">
      <div class="product-grid">
        <div v-for="p in products" :key="p.id" class="product-grid__cell">
          <ProductCard :product="p" @click="goProduct(p)" />
        </div>
      </div>
      <van-empty
        v-if="!loading && !products.length"
        :description="`标签「${shop.tagNameOf(activeTab)}」下暂无商品`"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.category-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px 8px;
  margin: 12px;
  padding: 16px 8px;
  background: #fff;
  border-radius: 8px;

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    cursor: pointer;
  }

  .category-icon {
    width: 52px;
    height: 52px;
    border-radius: 50%;
    object-fit: cover;
    background: #f2f3f5;

    &--text {
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      color: #646566;
    }
  }

  .category-name {
    margin-top: 6px;
    font-size: 12px;
    color: #323233;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .empty { grid-column: span 4; }
}

.tag-products { padding-top: 12px; }

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  padding: 0 12px;

  &__cell { min-width: 0; }
}
</style>
