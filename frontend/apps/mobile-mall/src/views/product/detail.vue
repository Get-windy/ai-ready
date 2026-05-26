<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Swipe, SwipeItem, Cell, CellGroup, Button, Stepper, ActionSheet, showToast, showLoadingToast, closeToast, Dialog } from 'vant'
import { api } from '@/api'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()

const productId = route.params.id as string
const product = ref<any>(null)
const quantity = ref(1)
const showSkuSelector = ref(false)
const selectedSku = ref<any>(null)

const currentPrice = computed(() => {
  if (selectedSku.value) {
    return selectedSku.value.price
  }
  return product.value?.price || 0
})

const currentStock = computed(() => {
  if (selectedSku.value) {
    return selectedSku.value.stock
  }
  return product.value?.stock || 0
})

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.product.getDetail(productId)
    product.value = res.data
  } finally {
    closeToast()
  }
})

const handleAddToCart = () => {
  if (!product.value) return
  
  if (product.value.skus && product.value.skus.length > 0 && !selectedSku.value) {
    showSkuSelector.value = true
    return
  }
  
  cartStore.addItem({
    id: product.value.id,
    name: product.value.name,
    image: product.value.images?.[0] || '',
    price: currentPrice.value,
    stock: currentStock.value
  }, quantity.value)
  
  showToast({ type: 'success', message: '已添加到购物车' })
}

const handleBuyNow = () => {
  if (!product.value) return
  
  if (product.value.skus && product.value.skus.length > 0 && !selectedSku.value) {
    showSkuSelector.value = true
    return
  }
  
  cartStore.clearCart()
  cartStore.addItem({
    id: product.value.id,
    name: product.value.name,
    image: product.value.images?.[0] || '',
    price: currentPrice.value,
    stock: currentStock.value
  }, quantity.value)
  
  router.push('/order')
}

const handleSelectSku = (sku: any) => {
  selectedSku.value = sku
  showSkuSelector.value = false
}

const handleQuantityChange = (val: number) => {
  quantity.value = val
}
</script>

<template>
  <div class="product-detail-page">
    <NavBar 
      title="商品详情" 
      left-arrow
      @click-left="router.back()"
    />
    
    <div v-if="product" class="detail-content">
      <Swipe class="product-swipe" :autoplay="3000" indicator-color="white">
        <SwipeItem v-for="(image, index) in product.images" :key="index">
          <img :src="image" class="swipe-image" />
        </SwipeItem>
      </Swipe>
      
      <div class="product-info">
        <div class="product-name">{{ product.name }}</div>
        <div class="product-desc">{{ product.description }}</div>
        <div class="product-price">
          <span class="price-symbol">¥</span>
          <span class="price-value">{{ currentPrice.toFixed(2) }}</span>
          <span v-if="product.originalPrice" class="original-price">
            ¥{{ product.originalPrice.toFixed(2) }}
          </span>
        </div>
        
        <div class="product-tags">
          <span v-if="product.isHot" class="tag hot">热销</span>
          <span v-if="product.isNew" class="tag new">新品</span>
          <span v-if="product.isPromotion" class="tag promotion">促销</span>
        </div>
      </div>
      
      <CellGroup inset>
        <Cell 
          v-if="product.skus && product.skus.length > 0"
          title="规格" 
          :value="selectedSku?.name || '请选择'"
          is-link
          @click="showSkuSelector = true"
        />
        <Cell title="库存" :value="`${currentStock} 件`" />
        <Cell title="销量" :value="`${product.salesCount || 0} 件`" />
        <Cell title="发货" value="预计24小时内发货" />
      </CellGroup>
      
      <CellGroup inset title="商品详情">
        <div class="detail-images">
          <img 
            v-for="(image, index) in product.detailImages"
            :key="index"
            :src="image"
            class="detail-image"
          />
        </div>
      </CellGroup>
      
      <div class="bottom-bar">
        <div class="quantity-selector">
          <span class="label">数量</span>
          <Stepper 
            :model-value="quantity"
            :min="1"
            :max="currentStock"
            @change="handleQuantityChange"
          />
        </div>
        
        <div class="action-buttons">
          <Button type="warning" @click="handleAddToCart">
            加入购物车
          </Button>
          <Button type="danger" @click="handleBuyNow">
            立即购买
          </Button>
        </div>
      </div>
      
      <ActionSheet 
        v-model:show="showSkuSelector"
        title="选择规格"
      >
        <div class="sku-selector">
          <div class="sku-list">
            <Button 
              v-for="sku in product.skus"
              :key="sku.id"
              :type="selectedSku?.id === sku.id ? 'primary' : 'default'"
              size="small"
              @click="handleSelectSku(sku)"
            >
              {{ sku.name }}
            </Button>
          </div>
          
          <div class="sku-info">
            <div class="sku-price">¥{{ selectedSku?.price || product.price }}</div>
            <div class="sku-stock">库存: {{ selectedSku?.stock || product.stock }} 件</div>
          </div>
          
          <Button 
            type="primary" 
            block
            @click="handleBuyNow"
          >
            确定
          </Button>
        </div>
      </ActionSheet>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.product-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.product-swipe {
  height: 375px;
  
  .swipe-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.product-info {
  padding: 16px;
  background: #fff;
  
  .product-name {
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }
  
  .product-desc {
    font-size: 14px;
    color: #969799;
    margin-top: 8px;
  }
  
  .product-price {
    margin-top: 12px;
    
    .price-symbol {
      font-size: 14px;
      color: #f44;
    }
    
    .price-value {
      font-size: 24px;
      color: #f44;
      font-weight: 600;
    }
    
    .original-price {
      font-size: 14px;
      color: #969799;
      margin-left: 8px;
      text-decoration: line-through;
    }
  }
  
  .product-tags {
    margin-top: 8px;
    
    .tag {
      font-size: 12px;
      padding: 2px 6px;
      border-radius: 2px;
      margin-right: 8px;
      
      &.hot {
        background: #fff1e6;
        color: #ff976a;
      }
      
      &.new {
        background: #e6f7ff;
        color: #1988fa;
      }
      
      &.promotion {
        background: #ffe6e6;
        color: #f44;
      }
    }
  }
}

.detail-images {
  padding: 12px;
  
  .detail-image {
    width: 100%;
    display: block;
  }
}

.bottom-bar {
  position: fixed;
  bottom: 50px;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #ebedf0;
  
  .quantity-selector {
    display: flex;
    align-items: center;
    
    .label {
      font-size: 14px;
      color: #333;
      margin-right: 12px;
    }
  }
  
  .action-buttons {
    flex: 1;
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}

.sku-selector {
  padding: 16px;
  
  .sku-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 16px;
  }
  
  .sku-info {
    margin-bottom: 16px;
    
    .sku-price {
      font-size: 20px;
      color: #f44;
      font-weight: 600;
    }
    
    .sku-stock {
      font-size: 14px;
      color: #969799;
      margin-top: 4px;
    }
  }
}
</style>