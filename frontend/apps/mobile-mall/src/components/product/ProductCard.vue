<script setup lang="ts">
/**
 * 商品卡（B2B 订货口径）——《商城App设计方案》§4.1 商品卡字段表。
 *
 * 2026-09-26 重写，改动点：
 *  1. **价格三态**：直接读 `useShopStore().pricePlaceholder` —— 不要求各调用页
 *     逐个传占位文案（分类/搜索/详情等多处使用，靠 props 传递迟早漏一处）。
 *     原实现 `product.price.toFixed(2)` 在价格为 null 时**直接抛异常**，
 *     而"隐藏价格"恰恰会把 price 置空 ⇒ 一开未认证就白屏。
 *  2. 补齐 B2B 字段：标签角标、规格、卖点条、起订量、净单价、供应商。
 *  3. 向后兼容：只传旧的 `{id,name,price,image,sales}` 也能正常渲染。
 */
import { computed } from 'vue'
import { Image as VanImage } from 'vant'
import { useShopStore } from '@/stores/shop'

interface Product {
  id: number | string
  /** 允许缺省：上游 DTO 的 name 是可选时，放宽可避免整页类型报错 */
  name?: string
  price?: number | null
  originalPrice?: number | null
  image?: string
  images?: string[]
  sales?: number
  salesCount?: number

  // ── B2B 补充字段（都可缺省）──
  /** 已解析的标签名数组（父组件用 shopStore.tagNameOf 翻译 tagCode 后传入） */
  tagNames?: string[]
  /** 分类角标（左上，如「预制菜」） */
  categoryTag?: string
  /** 规格，如「3成熟 净重10kg/箱」 */
  spec?: string
  /** 卖点条文案（源自 erp_product.mall_description 首行） */
  sellingPoint?: string
  /** 起订量 */
  minOrderQty?: number
  /** 净单价文案，如「净单价约 3.46元/斤」 */
  unitPriceText?: string
  /** 供应商/供货单位名 */
  supplierName?: string
  /** 库存（由后台 stock_display 开关控制是否显示） */
  stock?: number
}

const props = defineProps<{ product: Product }>()
defineEmits<{ click: [] }>()

const shop = useShopStore()

const displayImage = computed(() => props.product.image || props.product.images?.[0] || '')

/** 是否允许显示销量（后台 `show_sales` 开关） */
const showSales = computed(() => shop.config?.showSales !== 0)

/** 是否显示库存数字（后台 `stock_display`） */
const showStock = computed(() => shop.config?.stockDisplay !== 'HIDE')

const salesText = computed(() => {
  if (!showSales.value) return ''
  const sales = props.product.sales ?? props.product.salesCount ?? 0
  if (sales >= 10000) return `近7日疯抢 ${(sales / 10000).toFixed(1)}万+`
  if (sales > 0) return `近7日疯抢 ${sales}`
  return ''
})

// ⚠️ Pinia setup store 暴露的 computed 在 store 实例上**已自动解包**：
//    `shop.pricePlaceholder` 本身就是 `string | null`，再写 `.value` 会报错。
const pricePlaceholder = computed(() => shop.pricePlaceholder)

/**
 * 价格位的三种呈现，**必须分开判断**（2026-09-26 真机踩到）：
 *  ① 被规则隐藏（游客不可见价 / 未认证）→ 显示 shop 的占位文案，如「登录可见价」；
 *  ② 规则允许看价，但商品**本身没维护价格**（erp_product.retail_price 为空）→ 显示「暂无价格」；
 *  ③ 正常 → 显示数字。
 * 早先只判 ①③，把 ② 也当成了 ① —— 而真库里 6 个商品的 retail_price **全为空**，
 * 于是已开放看价的游客看到的却是「登录可见价」，属误导性文案。
 */
const priceHidden = computed(() => pricePlaceholder.value !== null)
const hasPrice = computed(() => props.product.price !== null && props.product.price !== undefined)
const priceVisible = computed(() => !priceHidden.value && hasPrice.value)
const priceText = computed(() => Number(props.product.price ?? 0).toFixed(2))
/** 划线价：受 `enable_retail_price` 开关控制 */
const originalPriceText = computed(() => {
  const p = props.product.originalPrice
  if (!p || shop.config?.enableRetailPrice === 0) return ''
  return Number(p).toFixed(2)
})

const outOfStock = computed(() => props.product.stock === 0)
/** 缺货展示口径：HIDE = 直接隐藏整卡，其它 = 置灰并标「缺货」 */
const hideWhenOutOfStock = computed(() => shop.config?.outOfStockDisplay === 'HIDE')

const tags = computed(() => (props.product.tagNames || []).slice(0, 2))
</script>

<template>
  <div
    v-if="!(outOfStock && hideWhenOutOfStock)"
    class="product-card"
    :class="{ 'is-out-of-stock': outOfStock }"
    @click="$emit('click')"
  >
    <div class="product-image">
      <VanImage :src="displayImage" fit="cover" lazy-load class="product-img">
        <template #loading>
          <div class="image-placeholder"><van-icon name="photo-o" size="24" color="#c8c9cc" /></div>
        </template>
        <template #error>
          <div class="image-placeholder"><van-icon name="photo-fail" size="24" color="#c8c9cc" /></div>
        </template>
      </VanImage>

      <!-- 左上：分类角标 -->
      <div v-if="product.categoryTag" class="corner corner--category">{{ product.categoryTag }}</div>
      <!-- 右上：商品标签（后台维护，最多 2 个） -->
      <div v-if="tags.length" class="corner corner--tags">
        <span v-for="t in tags" :key="t" class="tag-chip">{{ t }}</span>
      </div>
      <!-- 缺货遮罩 -->
      <div v-if="outOfStock" class="sold-out-mask"><span>缺货</span></div>
    </div>

    <div class="product-info">
      <div class="product-name">{{ product.name || '—' }}</div>

      <div v-if="product.spec" class="product-spec">{{ product.spec }}</div>

      <!-- 卖点条（红底一行，对应对标截图里「3成熟、已处理干净、出9成」那条） -->
      <div v-if="product.sellingPoint" class="product-selling-point">{{ product.sellingPoint }}</div>

      <div class="product-price-row">
        <span v-if="priceVisible" class="product-price">
          <span class="price-symbol">&yen;</span>{{ priceText }}
        </span>
        <span v-else class="product-price-placeholder">{{ priceHidden ? pricePlaceholder : '暂无价格' }}</span>
        <span v-if="priceVisible && originalPriceText" class="original-price">&yen;{{ originalPriceText }}</span>
      </div>

      <div v-if="product.unitPriceText" class="product-unit-price">{{ product.unitPriceText }}</div>

      <div class="product-meta">
        <span v-if="product.minOrderQty && product.minOrderQty > 1" class="meta-item">
          {{ product.minOrderQty }} 起订
        </span>
        <span v-if="showStock && product.stock !== undefined" class="meta-item">库存 {{ product.stock }}</span>
        <span v-if="salesText" class="meta-item meta-item--sales">{{ salesText }}</span>
      </div>

      <div v-if="product.supplierName" class="product-supplier">{{ product.supplierName }}</div>
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

  &:active { transform: scale(0.98); }
  &.is-out-of-stock { opacity: 0.6; }
}

.product-image {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f0f1f3;

  .product-img { width: 100%; height: 100%; display: block; }
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.corner {
  position: absolute;
  top: 6px;

  &--category {
    left: 6px;
    background: rgba(0, 0, 0, 0.55);
    color: #fff;
    font-size: 11px;
    padding: 1px 6px;
    border-radius: 3px;
  }

  &--tags {
    right: 6px;
    display: flex;
    gap: 4px;

    .tag-chip {
      background: #ee0a24;
      color: #fff;
      font-size: 11px;
      padding: 1px 6px;
      border-radius: 3px;
    }
  }
}

.sold-out-mask {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;

  span {
    background: rgba(0, 0, 0, 0.55);
    color: #fff;
    font-size: 12px;
    padding: 2px 10px;
    border-radius: 12px;
  }
}

.product-info { padding: 8px 10px 10px; }

.product-name {
  font-size: 14px;
  color: #323233;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-spec {
  margin-top: 4px;
  font-size: 12px;
  color: #969799;
}

.product-selling-point {
  margin-top: 6px;
  background: #ffece8;
  color: #ee0a24;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-price-row {
  margin-top: 6px;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.product-price {
  color: #ee0a24;
  font-size: 17px;
  font-weight: 600;
  .price-symbol { font-size: 12px; }
}

.product-price-placeholder {
  color: #969799;
  font-size: 13px;
  background: #f2f3f5;
  padding: 2px 8px;
  border-radius: 3px;
}

.original-price {
  color: #c8c9cc;
  font-size: 12px;
  text-decoration: line-through;
}

.product-unit-price {
  margin-top: 2px;
  font-size: 12px;
  color: #646566;
}

.product-meta {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
  color: #969799;

  .meta-item--sales { color: #ff976a; }
}

.product-supplier {
  margin-top: 4px;
  font-size: 11px;
  color: #969799;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
