import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface CartItem {
  id: string
  productId: string
  productName: string
  productImage: string
  price: number
  quantity: number
  selected: boolean
  stock: number
}

/** 商品接口（cart.addItem 入参） */
export interface CartProduct {
  id: string
  name: string
  image: string
  price: number
  stock: number
  productId?: string
  productName?: string
  productImage?: string
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const totalPrice = computed(() =>
    items.value
      .filter(item => item.selected)
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
  )
  const selectedCount = computed(() => items.value.filter(item => item.selected).length)
  const isAllSelected = computed(() =>
    items.value.length > 0 && items.value.every(item => item.selected)
  )

  const addItem = (product: CartProduct, quantity: number = 1) => {
    const existingItem = items.value.find(item => item.productId === product.id)
    if (existingItem) {
      existingItem.quantity += quantity
    } else {
      items.value.push({
        id: Date.now().toString(),
        productId: product.id,
        productName: product.productName || product.name,
        productImage: product.productImage || product.image,
        price: product.price,
        quantity,
        selected: true,
        stock: product.stock
      })
    }
    saveCart()
  }

  const removeItem = (id: string) => {
    items.value = items.value.filter(item => item.id !== id)
    saveCart()
  }

  const updateQuantity = (id: string, quantity: number) => {
    const item = items.value.find(item => item.id === id)
    if (item) {
      item.quantity = Math.max(1, Math.min(quantity, item.stock))
      saveCart()
    }
  }

  const toggleSelected = (id: string) => {
    const item = items.value.find(item => item.id === id)
    if (item) {
      item.selected = !item.selected
      saveCart()
    }
  }

  const toggleAllSelected = () => {
    const newSelected = !isAllSelected.value
    items.value.forEach(item => item.selected = newSelected)
    saveCart()
  }

  const clearCart = () => {
    items.value = []
    saveCart()
  }

  const clearSelectedItems = () => {
    items.value = items.value.filter(item => !item.selected)
    saveCart()
  }

  const saveCart = () => {
    localStorage.setItem('cart', JSON.stringify(items.value))
  }

  const loadCart = () => {
    const savedCart = localStorage.getItem('cart')
    if (savedCart) {
      items.value = JSON.parse(savedCart)
    }
  }

  return {
    items,
    totalCount,
    totalPrice,
    selectedCount,
    isAllSelected,
    addItem,
    removeItem,
    updateQuantity,
    toggleSelected,
    toggleAllSelected,
    clearCart,
    clearSelectedItems,
    loadCart
  }
})