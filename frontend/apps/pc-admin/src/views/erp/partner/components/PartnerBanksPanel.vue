<template>
  <div>
    <div class="panel-toolbar">
      <a-button size="small" type="primary" @click="showAddModal"><PlusOutlined /> 添加银行账户</a-button>
    </div>
    <vxe-table :data="list" border size="small" max-height="300" align="center">
      <vxe-column type="seq" title="#" width="50" />
      <vxe-column field="accountName" title="开户名" />
      <vxe-column field="bankName" title="开户银行" />
      <vxe-column field="bankBranch" title="支行" />
      <vxe-column field="accountNo" title="账号" />
      <vxe-column field="isDefault" title="默认" width="60">
        <template #default="{ row }"><a-tag v-if="row.isDefault" color="green">默认</a-tag></template>
      </vxe-column>
      <vxe-column title="操作" width="120">
        <template #default="{ row }">
          <a-button type="link" size="small" @click="editRow(row)">编辑</a-button>
          <a-button type="link" size="small" danger @click="handleDelete(row.id)">删除</a-button>
        </template>
      </vxe-column>
    </vxe-table>

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑银行账户' : '添加银行账户'" width="600px" @ok="handleSave">
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="开户名" required><a-input v-model:value="form.accountName" size="small" /></a-form-item>
        <a-form-item label="开户银行" required><a-input v-model:value="form.bankName" size="small" /></a-form-item>
        <a-form-item label="支行"><a-input v-model:value="form.bankBranch" size="small" /></a-form-item>
        <a-form-item label="银行账号" required><a-input v-model:value="form.accountNo" size="small" /></a-form-item>
        <a-form-item label="设为默认"><a-switch v-model:checked="form.isDefault" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { partnerBankAccountApi, type PartnerBankAccount } from '@/api/erp/partner'

const props = defineProps<{ partnerId: number }>()
const list = ref<PartnerBankAccount[]>([])
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ accountName: '', bankName: '', bankBranch: '', accountNo: '', currency: 'CNY', isDefault: false })

async function load() { list.value = await partnerBankAccountApi.getByPartner(props.partnerId) }
function showAddModal() { editingId.value = null; Object.assign(form, { accountName: '', bankName: '', bankBranch: '', accountNo: '', currency: 'CNY', isDefault: false }); modalVisible.value = true }
function editRow(row: PartnerBankAccount) { editingId.value = row.id; Object.assign(form, { accountName: row.accountName, bankName: row.bankName, bankBranch: row.bankBranch || '', accountNo: row.accountNo, currency: row.currency, isDefault: !!row.isDefault }); modalVisible.value = true }
async function handleSave() {
  if (!form.accountName || !form.bankName || !form.accountNo) { message.warning('请填写完整信息'); return }
  const data = { ...form, partnerId: props.partnerId, isDefault: form.isDefault ? 1 : 0 }
  try {
    if (editingId.value) { await partnerBankAccountApi.update(editingId.value, data) } else { await partnerBankAccountApi.create(data) }
    message.success('保存成功'); modalVisible.value = false; await load()
  } catch { message.error('操作失败') }
}
async function handleDelete(id: number) { try { await partnerBankAccountApi.delete(id); message.success('删除成功'); await load() } catch { message.error('删除失败') } }
onMounted(load)
</script>
<style scoped>.panel-toolbar { margin-bottom: 8px; }</style>
