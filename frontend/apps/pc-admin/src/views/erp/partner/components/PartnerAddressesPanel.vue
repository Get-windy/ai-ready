<template>
  <div>
    <div class="panel-toolbar">
      <a-button size="small" type="primary" @click="showAddModal"><PlusOutlined /> 添加地址</a-button>
    </div>
    <vxe-table :data="list" border size="small" max-height="300" align="center">
      <vxe-column type="seq" title="#" width="50" />
      <vxe-column field="addressType" title="类型" width="80">
        <template #default="{ row }">{{ typeLabel(row.addressType) }}</template>
      </vxe-column>
      <vxe-column field="detailAddress" title="详细地址" min-width="200" />
      <vxe-column field="contactName" title="联系人" />
      <vxe-column field="contactPhone" title="电话" />
      <vxe-column field="isDefault" title="默认" width="60">
        <template #default="{ row }"><a-tag v-if="row.isDefault" color="green">默认</a-tag></template>
      </vxe-column>
      <vxe-column title="操作" width="160">
        <template #default="{ row }">
          <a-button type="link" size="small" @click="editRow(row)">编辑</a-button>
          <a-button v-if="!row.isDefault" type="link" size="small" @click="handleSetDefault(row)">设默认</a-button>
          <a-button type="link" size="small" danger @click="handleDelete(row.id)">删除</a-button>
        </template>
      </vxe-column>
    </vxe-table>

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑地址' : '添加地址'" width="600px" @ok="handleSave">
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="地址类型">
          <a-select v-model:value="form.addressType" size="small">
            <a-select-option value="DELIVERY">发货地址</a-select-option>
            <a-select-option value="BILLING">开票地址</a-select-option>
            <a-select-option value="RECEIVING">收货地址</a-select-option>
            <a-select-option value="RETURN">退货地址</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="所在地区">
          <a-space>
            <a-input v-model:value="form.province" placeholder="省" style="width:120px" size="small" />
            <a-input v-model:value="form.city" placeholder="市" style="width:120px" size="small" />
            <a-input v-model:value="form.district" placeholder="区" style="width:120px" size="small" />
          </a-space>
        </a-form-item>
        <a-form-item label="详细地址" required>
          <a-input v-model:value="form.detailAddress" size="small" />
        </a-form-item>
        <a-form-item label="联系人"><a-input v-model:value="form.contactName" size="small" /></a-form-item>
        <a-form-item label="联系电话"><a-input v-model:value="form.contactPhone" size="small" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { partnerAddressApi, type PartnerAddress } from '@/api/erp/partner'

const props = defineProps<{ partnerId: number }>()
const list = ref<PartnerAddress[]>([])
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ addressType: 'DELIVERY', province: '', city: '', district: '', detailAddress: '', zipCode: '', contactName: '', contactPhone: '' })

function typeLabel(t: string) { return { DELIVERY: '发货', BILLING: '开票', RECEIVING: '收货', RETURN: '退货' }[t] || t }

async function load() { list.value = await partnerAddressApi.getByPartner(props.partnerId) }
function showAddModal() { editingId.value = null; Object.assign(form, { addressType: 'DELIVERY', province: '', city: '', district: '', detailAddress: '', zipCode: '', contactName: '', contactPhone: '' }); modalVisible.value = true }
function editRow(row: PartnerAddress) { editingId.value = row.id; Object.assign(form, { addressType: row.addressType, province: row.province || '', city: row.city || '', district: row.district || '', detailAddress: row.detailAddress, zipCode: row.zipCode || '', contactName: row.contactName || '', contactPhone: row.contactPhone || '' }); modalVisible.value = true }
async function handleSave() {
  if (!form.detailAddress) { message.warning('请输入详细地址'); return }
  const data = { ...form, partnerId: props.partnerId, isDefault: 0 }
  try {
    if (editingId.value) { await partnerAddressApi.update(editingId.value, data) } else { await partnerAddressApi.create(data) }
    message.success('保存成功'); modalVisible.value = false; await load()
  } catch { message.error('操作失败') }
}
async function handleSetDefault(row: PartnerAddress) { try { await partnerAddressApi.setDefault(row.id, props.partnerId); message.success('已设为默认'); await load() } catch { message.error('操作失败') } }
async function handleDelete(id: number) { try { await partnerAddressApi.delete(id); message.success('删除成功'); await load() } catch { message.error('删除失败') } }
onMounted(load)
</script>
<style scoped>.panel-toolbar { margin-bottom: 8px; }</style>
