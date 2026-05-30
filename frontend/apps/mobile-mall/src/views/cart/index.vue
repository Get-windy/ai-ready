<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Checkbox, Stepper, Button, Empty, Dialog } from 'vant'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const cartStore = useCartStore()

const handleSelectAll = () => {
  cartStore.toggleAllSelected()
}

const handleQuantityChange = (id: string, quantity: number) => {
  cartStore.updateQuantity(id, quantity)
}

const handleRemoveItem = (id: string) => {
  Dialog.confirm({
    title: '提示',
    message: '确定要删除该商品吗？'
  }).then(() => {
    cartStore.removeItem(id)
  }).catch((err) => { console.error('删除商品操作失败:', err) })
}

const handleCheckout = () => {
  if (cartStore.selectedCount === 0) {
    Dialog.alert({ message: '请选择要结算的商品' })
    return
  }
  router.push('/order')
}

const goToProduct = (productId: string) => {
  router.push(`/product/${productId}`)
}
</script>

<template>
  <div class="cart-page">
    <NavBar title="购物车" />
    
    <div v-if="cartStore.items.length === 0" class="empty-container">
      <Empty description="购物车空空如也" />
      <Button type="primary" block @click="router.push('/')">
        去逛逛
      </Button>
    </div>
    
    <div v-else class="cart-list">
      <div class="cart-header">
        <Checkbox 
          :checked="cartStore.isAllSelected"
          @change="handleSelectAll"
        >
          全选
        </Checkbox>
        <span class="total-count">共 {{ cartStore.totalCount }} 件</span>
      </div>
      
      <div class="cart-items">
        <div 
          v-for="item in cartStore.items" 
          :key="item.id"
          class="cart-item"
        >
          <Checkbox 
            :checked="item.selected"
            @change="cartStore.toggleSelected(item.id)"
          />
          
          <div class="item-image" @click="goToProduct(item.productId)">
            <img :src="item.productImage" :alt="item.productName" />
          </div>
          
          <div class="item-info">
            <div class="item-name" @click="goToProduct(item.productId)">
              {{ item.productName }}
            </div>
            <div class="item-price">¥{{ item.price.toFixed(2) }}</div>
            <div class="item-actions">
              <Stepper 
                :model-value="item.quantity"
                :max="item.stock"
                @change="(val: number) => handleQuantityChange(item.id, val)"
              />
              <Button 
                size="small" 
                type="danger" 
                plain
                @click="handleRemoveItem(item.id)"
              >
                删除
              </Button>
            </div>
          </div>
        </div>
      </div>
      
      <div class="cart-footer">
        <div class="footer-left">
          <Checkbox 
            :checked="cartStore.isAllSelected"
            @change="handleSelectAll"
          >
            全选
          </Checkbox>
          <span class="selected-count">
            已选 {{ cartStore.selectedCount }} 件
          </span>
        </div>
        <div class="footer-right">
          <div class="total-price">
            合计: <span class="price">¥{{ cartStore.totalPrice.toFixed(2) }}</span>
          </div>
          <Button 
            type="primary" 
            :disabled="cartStore.selectedCount === 0"
            @click="handleCheckout"
          >
            结算
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.cart-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.empty-container {
  padding: 60px 20px;
  text-align: center;
}

.cart-list {
  padding-bottom: 100px;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  
  .total-count {
    color: #969799;
    font-size: 14px;
  }
}

.cart-items {
  margin-top: 8px;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  margin-bottom: 8px;
  
  .item-image {
    width: 80px;
    height: 80px;
    margin-left: 12px;
    border-radius: 4px;
    overflow: hidden;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
  .item-info {
    flex: 1;
    margin-left: 12px;
    
    .item-name {
      font-size: 14px;
      color: #333;
      line-height: 1.4;
      margin-bottom: 8px;
    }
    
    .item-price {
      font-size: 16px;
      color: #f44;
      font-weight: 600;
      margin-bottom: 8px;
    }
    
    .item-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
}

.cart-footer {
  position: fixed;
  bottom: 50px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #ebedf0;
  
  .footer-left {
    display: flex;
    align-items: center;
    
    .selected-count {
      margin-left: 12px;
      color: #969799;
      font-size: 14px;
    }
  }
  
  .footer-right {
    display: flex;
    align-items: center;
    
    .total-price {
      margin-right: 12px;
      font-size: 14px;
      
      .price {
        font-size: 18px;
        color: #f44;
        font-weight: 600;
      }
    }
  }
}
</style>