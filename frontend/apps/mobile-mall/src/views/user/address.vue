<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, AddressList, AddressEdit, Button, Dialog, Popup, showLoadingToast, closeToast } from 'vant'
import { api, type AddressItem } from '@/api'

const router = useRouter()

const addresses = ref<AddressItem[]>([])
const showEdit = ref(false)
const editingAddress = ref<AddressItem | null>(null)
const chosenAddressId = ref<string>('')

onMounted(async () => {
  loadAddresses()
})

const loadAddresses = async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true, duration: 0 })
  
  try {
    const res = await api.user.getAddresses()
    addresses.value = res.data || []

    const defaultAddr = addresses.value.find(a => a.isDefault)
    if (defaultAddr) {
      chosenAddressId.value = defaultAddr.id
    }
  } catch (err) {
    console.warn('[地址] 加载地址失败', err)
    addresses.value = []
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

const handleDelete = async (address: any) => {
  const confirmed = await Dialog.confirm({
    title: '提示',
    message: '确定要删除该地址吗？'
  }).catch(() => false)
  if (!confirmed) return

  showLoadingToast({ message: '删除中...', forbidClick: true, duration: 0 })

  try {
    await api.user.deleteAddress(address.id)
    addresses.value = addresses.value.filter(a => a.id !== address.id)
  } catch (err) {
    console.warn('[地址] 删除失败', err)
  } finally {
    closeToast()
  }
}

const handleSelect = (address: any) => {
  chosenAddressId.value = address.id
}

const handleSave = async (content: AddressItem) => {
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
  } catch (err) {
    console.warn('[地址] 保存失败', err)
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
    
    <Popup
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
    </Popup>
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