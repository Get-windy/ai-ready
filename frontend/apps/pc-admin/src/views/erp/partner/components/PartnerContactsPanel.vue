<template>
  <div>
    <div class="panel-toolbar">
      <a-button size="small" type="primary" @click="showAddModal"><PlusOutlined /> 添加联系人</a-button>
    </div>
    <vxe-table :data="list" border size="small" max-height="300" align="center">
      <vxe-column type="seq" title="#" width="50" />
      <vxe-column field="contactName" title="姓名" />
      <vxe-column field="contactPhone" title="电话" />
      <vxe-column field="contactEmail" title="邮箱" />
      <vxe-column field="position" title="职位" />
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

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑联系人' : '添加联系人'" width="500px" @ok="handleSave">
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="姓名" required><a-input v-model:value="form.contactName" size="small" /></a-form-item>
        <a-form-item label="电话"><a-input v-model:value="form.contactPhone" size="small" /></a-form-item>
        <a-form-item label="邮箱"><a-input v-model:value="form.contactEmail" size="small" /></a-form-item>
        <a-form-item label="职位"><a-input v-model:value="form.position" size="small" /></a-form-item>
        <a-form-item label="部门"><a-input v-model:value="form.department" size="small" /></a-form-item>
        <a-form-item label="设为默认"><a-switch v-model:checked="form.isDefault" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { partnerContactApi, type PartnerContact } from '@/api/erp/partner'

const props = defineProps<{ partnerId: number }>()
const list = ref<PartnerContact[]>([])
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ contactName: '', contactPhone: '', contactEmail: '', position: '', department: '', isDefault: false })

async function load() { list.value = await partnerContactApi.getByPartner(props.partnerId) }
function showAddModal() { editingId.value = null; Object.assign(form, { contactName: '', contactPhone: '', contactEmail: '', position: '', department: '', isDefault: false }); modalVisible.value = true }
function editRow(row: PartnerContact) { editingId.value = row.id; Object.assign(form, { contactName: row.contactName, contactPhone: row.contactPhone || '', contactEmail: row.contactEmail || '', position: row.position || '', department: row.department || '', isDefault: !!row.isDefault }); modalVisible.value = true }
async function handleSave() {
  if (!form.contactName) { message.warning('请输入姓名'); return }
  const data = { ...form, partnerId: props.partnerId, isDefault: form.isDefault ? 1 : 0 }
  try {
    if (editingId.value) { await partnerContactApi.update(editingId.value, data) } else { await partnerContactApi.create(data) }
    message.success('保存成功'); modalVisible.value = false; await load()
  } catch { message.error('操作失败') }
}
async function handleDelete(id: number) { try { await partnerContactApi.delete(id); message.success('删除成功'); await load() } catch { message.error('删除失败') } }
onMounted(load)
</script>
<style scoped>.panel-toolbar { margin-bottom: 8px; }</style>
