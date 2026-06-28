<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="零售收银" full-height>
      <template #headerExtra>
        <a-space>
          <a-badge :status="loading ? 'processing' : 'success'" />
          <a-tooltip title="刷新"><a-button size="small" @click="fetchData"><template #icon><ReloadOutlined /></template></a-button></a-tooltip>
        </a-space>
      </template>
      <div class="pos-layout">
        <div class="pos-main">
          <div class="search-bar">
            <a-input-search v-model:value="searchText" placeholder="搜索商品（编码/名称/条码）" enter-button @search="handleSearch" />
          </div>
          <a-spin :spinning="loading">
            <div class="product-grid">
              <div v-for="p in productList" :key="p.id" class="product-card" @click="handleAddToCart(p)">
                <div class="product-name">{{ p.productName || p.name }}</div>
                <div class="product-price">¥{{ formatAmount(p.salePrice || p.price) }}</div>
                <div class="product-stock">库存: {{ p.stock || p.quantity || 0 }}</div>
              </div>
              <a-empty v-if="!loading && productList.length === 0" description="暂无商品数据" />
            </div>
          </a-spin>
        </div>
        <div class="pos-sidebar">
          <div class="cart-header">
            <h3>购物车</h3>
            <span class="cart-count">{{ cartItems.length }} 项</span>
            <a-button type="link" size="small" danger @click="handleClearCart">清空</a-button>
          </div>
          <div class="cart-list">
            <div v-for="(item, index) in cartItems" :key="item.productId || index" class="cart-item">
              <div class="cart-item-info">
                <div class="cart-item-name">{{ item.productName }}</div>
                <div class="cart-item-meta">
                  ¥{{ formatAmount(item.price) }} x {{ item.quantity }}
                </div>
              </div>
              <div class="cart-item-amount">¥{{ formatAmount(item.price * item.quantity) }}</div>
              <a-button type="link" size="small" danger @click="cartItems.splice(index, 1)">×</a-button>
            </div>
            <a-empty v-if="cartItems.length === 0" description="请添加商品" />
          </div>
          <div class="cart-footer">
            <div class="cart-total">
              <span>合计:</span>
              <span class="total-amount">¥{{ formatAmount(cartTotal) }}</span>
            </div>
            <a-button type="primary" block :disabled="cartItems.length === 0" @click="handleCheckout">结算</a-button>
          </div>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'

const loading = ref(false)
const searchText = ref('')
const productList = ref<any[]>([])
const cartItems = ref<{ productId: number; productName: string; price: number; quantity: number }[]>([])

const cartTotal = computed(() => cartItems.value.reduce((s, i) => s + i.price * i.quantity, 0))

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/api/v1/mall/products', { params: { page: 1, size: 50 } })
    const data = res.data || res || {}
    productList.value = data.records || data.content || data.list || []
  } catch (e) {
    console.warn('[POS] 获取商品列表失败', e)
    productList.value = []
  } finally { loading.value = false }
}

const handleSearch = () => { fetchData() }
const handleAddToCart = (p: any) => {
  const exist = cartItems.value.find(i => i.productId === (p.id || p.productId))
  if (exist) { exist.quantity++ } else {
    cartItems.value.push({ productId: p.id || p.productId, productName: p.productName || p.name, price: p.salePrice || p.price || 0, quantity: 1 })
  }
}
const handleClearCart = () => { cartItems.value = [] }
const handleCheckout = async () => {
  try {
    await request.post('/api/v1/mall/orders', {
      items: cartItems.value.map(i => ({ productId: i.productId, quantity: i.quantity }))
    })
    message.success('下单成功')
    cartItems.value = []
  } catch { message.error('下单失败') }
}
const handleError = (e: Error) => console.error(e)
function formatAmount(v: number) { return (v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }
onMounted(fetchData)
</script>
<style scoped>
.pos-layout { display: flex; gap: 16px; height: calc(100vh - 200px); }
.pos-main { flex: 1; display: flex; flex-direction: column; }
.pos-sidebar { width: 340px; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); display: flex; flex-direction: column; }
.search-bar { margin-bottom: 12px; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 10px; overflow-y: auto; flex: 1; }
.product-card { background: #fff; border: 1px solid #f0f0f0; border-radius: 8px; padding: 12px; cursor: pointer; transition: all .2s; }
.product-card:hover { border-color: #1890ff; box-shadow: 0 2px 8px rgba(24,144,255,.15); }
.product-name { font-size: 13px; color: #333; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-price { font-size: 18px; font-weight: 600; color: #ff4d4f; font-family: 'SFMono-Regular', Consolas, monospace; }
.product-stock { font-size: 11px; color: #999; margin-top: 4px; }
.cart-header { display: flex; align-items: center; gap: 8px; padding: 12px 16px; border-bottom: 1px solid #f0f0f0; }
.cart-header h3 { margin: 0; flex: 1; }
.cart-count { font-size: 12px; color: #999; }
.cart-list { flex: 1; overflow-y: auto; padding: 8px 0; }
.cart-item { display: flex; align-items: center; gap: 8px; padding: 8px 16px; border-bottom: 1px solid #f5f5f5; }
.cart-item-info { flex: 1; }
.cart-item-name { font-size: 13px; color: #333; }
.cart-item-meta { font-size: 11px; color: #999; }
.cart-item-amount { font-size: 14px; font-weight: 600; color: #ff4d4f; font-family: 'SFMono-Regular', Consolas, monospace; white-space: nowrap; }
.cart-footer { padding: 12px 16px; border-top: 1px solid #f0f0f0; }
.cart-total { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 16px; }
.total-amount { font-size: 22px; font-weight: 700; color: #ff4d4f; font-family: 'SFMono-Regular', Consolas, monospace; }
</style>
