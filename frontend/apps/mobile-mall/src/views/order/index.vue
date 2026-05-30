<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Card, Button, Cell, CellGroup, AddressList, RadioGroup, Radio, showToast, showLoadingToast, closeToast, Dialog } from 'vant'
import { api } from '@/api'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const addresses = ref<any[]>([])
const selectedAddress = ref<any>(null)
const paymentMethod = ref('wechat')
const remark = ref('')
const submitting = ref(false)

const selectedItems = computed(() => 
  cartStore.items.filter(item => item.selected)
)

const totalPrice = computed(() => 
  selectedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
)

const totalQuantity = computed(() => 
  selectedItems.value.reduce((sum, item) => sum + item.quantity, 0)
)

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  if (selectedItems.value.length === 0) {
    router.push('/cart')
    return
  }
  
  loadAddresses()
})

const loadAddresses = async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.user.getAddresses()
    addresses.value = res.data || []
    
    const defaultAddress = addresses.value.find(a => a.isDefault)
    if (defaultAddress) {
      selectedAddress.value = defaultAddress
    } else if (addresses.value.length > 0) {
      selectedAddress.value = addresses.value[0]
    }
  } finally {
    closeToast()
  }
}

const handleSelectAddress = (address: any) => {
  selectedAddress.value = address
}

const handleAddAddress = () => {
  router.push('/user/address')
}

const handleSubmitOrder = async () => {
  if (!selectedAddress.value) {
    showToast({ type: 'fail', message: '请选择收货地址' })
    return
  }
  
  Dialog.confirm({
    title: '提交订单',
    message: `订单金额 ¥${totalPrice.value.toFixed(2)}，确定提交吗？`
  }).then(async () => {
    submitting.value = true
    showLoadingToast({ message: '提交中...', forbidClick: true })
    
    try {
      const orderData = {
        addressId: selectedAddress.value.id,
        items: selectedItems.value.map(item => ({
          productId: item.productId,
          quantity: item.quantity,
          price: item.price
        })),
        paymentMethod: paymentMethod.value,
        remark: remark.value,
        totalAmount: totalPrice.value
      }
      
      const res = await api.order.create(orderData)
      
      cartStore.clearSelectedItems()
      
      showToast({ type: 'success', message: '订单创建成功' })
      
      router.push(`/order/${res.data.orderId}`)
    } finally {
      closeToast()
      submitting.value = false
    }
  }).catch((err) => { console.error('订单创建失败:', err) })
}

const handleSelectPayment = (method: string) => {
  paymentMethod.value = method
}
</script>

<template>
  <div class="order-confirm-page">
    <NavBar 
      title="确认订单" 
      left-arrow
      @click-left="router.back()"
    />
    
    <div class="order-content">
      <div class="section-title">收货地址</div>
      <div v-if="addresses.length === 0" class="no-address">
        <Button type="primary" block @click="handleAddAddress">
          添加收货地址
        </Button>
      </div>
      
      <AddressList
        v-else
        :list="addresses.map(a => ({
          id: a.id,
          name: a.name,
          tel: a.phone,
          address: `${a.province}${a.city}${a.district}${a.detail}`,
          isDefault: a.isDefault
        }))"
        :switchable="true"
        default-tag-text="默认"
        @select="handleSelectAddress"
        @add="handleAddAddress"
      />
      
      <div class="section-title">商品清单</div>
      <div class="items-list">
        <div 
          v-for="item in selectedItems"
          :key="item.id"
          class="item-card"
        >
          <img :src="item.productImage" class="item-image" />
          <div class="item-info">
            <div class="item-name">{{ item.productName }}</div>
            <div class="item-price">¥{{ item.price.toFixed(2) }}</div>
            <div class="item-quantity">x{{ item.quantity }}</div>
          </div>
          <div class="item-total">
            ¥{{ (item.price * item.quantity).toFixed(2) }}
          </div>
        </div>
      </div>
      
      <div class="section-title">支付方式</div>
      <RadioGroup v-model="paymentMethod" class="payment-methods">
        <CellGroup inset>
          <Cell title="微信支付" clickable @click="handleSelectPayment('wechat')">
            <template #right-icon>
              <Radio name="wechat" />
            </template>
          </Cell>
          <Cell title="支付宝" clickable @click="handleSelectPayment('alipay')">
            <template #right-icon>
              <Radio name="alipay" />
            </template>
          </Cell>
          <Cell title="银行转账" clickable @click="handleSelectPayment('bank')">
            <template #right-icon>
              <Radio name="bank" />
            </template>
          </Cell>
        </CellGroup>
      </RadioGroup>
      
      <div class="section-title">备注</div>
      <CellGroup inset>
        <Cell title="备注">
          <template #value>
            <input 
              v-model="remark"
              type="text"
              placeholder="请输入备注"
              class="remark-input"
            />
          </template>
        </Cell>
      </CellGroup>
      
      <div class="order-summary">
        <div class="summary-row">
          <span>商品数量</span>
          <span>{{ totalQuantity }} 件</span>
        </div>
        <div class="summary-row">
          <span>商品金额</span>
          <span class="amount">¥{{ totalPrice.toFixed(2) }}</span>
        </div>
        <div class="summary-row">
          <span>运费</span>
          <span>免运费</span>
        </div>
        <div class="summary-row total">
          <span>应付金额</span>
          <span class="amount">¥{{ totalPrice.toFixed(2) }}</span>
        </div>
      </div>
      
      <div class="submit-bar">
        <div class="total-info">
          合计: <span class="price">¥{{ totalPrice.toFixed(2) }}</span>
        </div>
        <Button 
          type="danger" 
          size="large"
          :loading="submitting"
          @click="handleSubmitOrder"
        >
          提交订单
        </Button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.order-confirm-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 80px;
}

.section-title {
  font-size: 14px;
  color: #969799;
  padding: 12px 16px 8px;
}

.no-address {
  padding: 16px;
}

.items-list {
  background: #fff;
  padding: 12px;
  
  .item-card {
    display: flex;
    align-items: center;
    padding: 8px 0;
    
    .item-image {
      width: 60px;
      height: 60px;
      border-radius: 4px;
      object-fit: cover;
    }
    
    .item-info {
      flex: 1;
      margin-left: 12px;
      
      .item-name {
        font-size: 14px;
        color: #333;
      }
      
      .item-price {
        font-size: 14px;
        color: #f44;
        margin-top: 4px;
      }
      
      .item-quantity {
        font-size: 12px;
        color: #969799;
        margin-top: 2px;
      }
    }
    
    .item-total {
      font-size: 14px;
      color: #333;
      font-weight: 600;
    }
  }
}

.payment-methods {
  background: #fff;
}

.remark-input {
  width: 100%;
  border: none;
  text-align: right;
  font-size: 14px;
  
  &:focus {
    outline: none;
  }
}

.order-summary {
  background: #fff;
  padding: 16px;
  margin-top: 12px;
  
  .summary-row {
    display: flex;
    justify-content: space-between;
    font-size: 14px;
    color: #333;
    margin-bottom: 8px;
    
    .amount {
      color: #f44;
    }
    
    &.total {
      font-size: 16px;
      font-weight: 600;
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid #ebedf0;
    }
  }
}

.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #ebedf0;
  
  .total-info {
    flex: 1;
    font-size: 14px;
    
    .price {
      font-size: 18px;
      color: #f44;
      font-weight: 600;
    }
  }
}
</style>