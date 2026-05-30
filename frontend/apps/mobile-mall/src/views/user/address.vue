<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, AddressList, AddressEdit, Button, Dialog, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'

const router = useRouter()

const addresses = ref<any[]>([])
const showEdit = ref(false)
const editingAddress = ref<any>(null)
const chosenAddressId = ref<string>('')

onMounted(async () => {
  loadAddresses()
})

const loadAddresses = async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true, duration: 0 })
  
  try {
    const res = await api.user.getAddresses()
    addresses.value = res.data || [
      {
        id: '1',
        name: '张三',
        tel: '13800138000',
        address: '北京市朝阳区建国路88号',
        isDefault: true
      },
      {
        id: '2',
        name: '李四',
        tel: '13900139000',
        address: '上海市浦东新区陆家嘴环路1000号',
        isDefault: false
      }
    ]
    
    const defaultAddr = addresses.value.find(a => a.isDefault)
    if (defaultAddr) {
      chosenAddressId.value = defaultAddr.id
    }
  } catch {
    addresses.value = [
      {
        id: '1',
        name: '张三',
        tel: '13800138000',
        address: '北京市朝阳区建国路88号',
        isDefault: true
      }
    ]
  } finally {
    closeToast()
  }
}

const handleAdd = () => {
  editingAddress.value = null
  showEdit.value = true
}

const handleEdit = (address: any) => {
  editingAddress.value = address
  showEdit.value = true
}

const handleDelete = (address: any) => {
  Dialog.confirm({
    title: '提示',
    message: '确定要删除该地址吗？'
  }).then(async () => {
    showLoadingToast({ message: '删除中...', forbidClick: true, duration: 0 })
    
    try {
      await api.user.deleteAddress(address.id)
      addresses.value = addresses.value.filter(a => a.id !== address.id)
    } finally {
      closeToast()
    }
  }).catch((err) => { console.error('删除地址操作失败:', err) })
}

const handleSelect = (address: any) => {
  chosenAddressId.value = address.id
}

const handleSave = async (content: any) => {
  showLoadingToast({ message: '保存中...', forbidClick: true, duration: 0 })
  
  try {
    if (editingAddress.value) {
      await api.user.updateAddress(editingAddress.value.id, content)
      const index = addresses.value.findIndex(a => a.id === editingAddress.value.id)
      if (index !== -1) {
        addresses.value[index] = { ...content, id: editingAddress.value.id }
      }
    } else {
      const res = await api.user.addAddress(content)
      addresses.value.push({ ...content, id: res.data?.id || Date.now().toString() })
    }
    
    showEdit.value = false
  } catch {
    const newAddress = {
      ...content,
      id: Date.now().toString()
    }
    
    if (editingAddress.value) {
      const index = addresses.value.findIndex(a => a.id === editingAddress.value.id)
      if (index !== -1) {
        addresses.value[index] = newAddress
      }
    } else {
      addresses.value.push(newAddress)
    }
    
    showEdit.value = false
  } finally {
    closeToast()
  }
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="address-page">
    <NavBar 
      title="收货地址"
      left-arrow
      @click-left="goBack"
    />
    
    <div class="address-content">
      <AddressList
        v-model="chosenAddressId"
        :list="addresses"
        default-tag-text="默认"
        @add="handleAdd"
        @edit="handleEdit"
        @delete="handleDelete"
        @select="handleSelect"
      />
    </div>
    
    <van-popup
      v-model:show="showEdit"
      position="bottom"
      :style="{ height: '80%' }"
      round
    >
      <AddressEdit
        :address-info="editingAddress"
        :show-delete="!!editingAddress"
        show-set-default
        show-search-result
        :search-result="[]"
        @save="handleSave"
        @delete="handleDelete"
        @cancel="showEdit = false"
      />
    </van-popup>
  </div>
</template>

<style lang="scss" scoped>
.address-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.address-content {
  padding: 12px;
}
</style>