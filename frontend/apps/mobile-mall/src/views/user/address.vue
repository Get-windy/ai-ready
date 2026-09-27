<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, AddressList, AddressEdit, Dialog, Popup, showLoadingToast, closeToast } from 'vant'
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
      chosenAddressId.value = String(defaultAddr.id)
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

/**
 * Vant `AddressEdit` 的保存体 → 后端 `AddressDTO`。
 *
 * Vant 的 `AddressEditInfo` 字段是 `{name, tel, province, city, county, areaCode, addressDetail, isDefault}`，
 * 而库里是 `consignee / phone / region(省市区一个串) / address(详细)` —— 字段名和粒度都不同，
 * 必须在边界这一处转换（早先在服务层按不存在的列名读写，直接 500）。
 */
function toPayload(content: any): Omit<AddressItem, 'id'> {
  const region = [content?.province, content?.city, content?.county].filter(Boolean).join('')
  return {
    consignee: content?.name ?? '',
    phone: content?.tel ?? '',
    region,
    address: content?.addressDetail ?? content?.address ?? '',
    isDefault: !!content?.isDefault
  }
}

const handleSave = async (content: any) => {
  showLoadingToast({ message: '保存中...', forbidClick: true, duration: 0 })

  const payload = toPayload(content)
  try {
    const editing = editingAddress.value
    if (editing) {
      await api.user.updateAddress(String(editing.id), payload)
      const index = addresses.value.findIndex(a => a.id === editing.id)
      if (index !== -1) {
        addresses.value[index] = { ...payload, id: editing.id }
      }
    } else {
      const res: any = await api.user.addAddress(payload)
      // 新增后回填后端给的 id（原先用 Date.now() 兜底，但那会让"设为默认"等按 id 的操作失效）
      addresses.value.push({ ...payload, id: res?.data?.id ?? Date.now().toString() })
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
        :list="addresses.map(a => ({
          // Vant 要求 id/tel/name/address 确定类型；AddressItem 全可选 ⇒ 此处收敛
          id: String(a.id),
          name: a.consignee ?? '',
          tel: a.phone ?? '',
          address: `${a.region ?? ''} ${a.address ?? ''}`.trim(),
          isDefault: a.isDefault ?? false
        }))"
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
        :address-info="editingAddress
          ? {
              name: editingAddress.consignee ?? '',
              tel: editingAddress.phone ?? '',
              // 库里 region 是**一个字符串**（省市区合并），拆不回三级；整串放进 province，
              // 区域选择器允许只选一级 ⇒ 再次编辑时不丢数据
              province: editingAddress.region ?? '',
              city: '',
              county: '',
              areaCode: '',
              addressDetail: editingAddress.address ?? '',
              isDefault: editingAddress.isDefault ?? false
            }
          : undefined"
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