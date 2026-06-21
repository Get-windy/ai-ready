<template>
  <div class="contact-section">
    <div class="contact-header-row">
      <span class="contact-title">联系人信息</span>
    </div>

    <!-- 常用联系人（首行） -->
    <div class="contact-block" v-if="rows.length > 0">
      <div class="contact-block-label">
        常用联系人
        <template v-if="showMallAccount">
          <a-checkbox v-model:checked="rows[0].openMallAccount" style="margin-left: 12px; font-size: 13px; font-weight: 400; color: #8c8c8c">
            开通商城帐号
          </a-checkbox>
        </template>
      </div>
      <div class="contact-fields">
        <input class="c-input" v-model="rows[0].contactName" placeholder="联系人" />
        <input class="c-input" v-model="rows[0].contactPhone" placeholder="手机*" />
        <input class="c-input" v-model="rows[0].region" placeholder="所在地区" />
        <input class="c-input c-input-wide" v-model="rows[0].detailAddress" placeholder="详情地址" />
        <a-button size="small" class="c-locate-btn">设置定位</a-button>
        <template v-if="showMallAccount">
          <a-select v-model:value="rows[0].deliveryMethod" placeholder="配送方式" size="small" class="c-select" allow-clear>
            <a-select-option value="EXPRESS">快递</a-select-option>
            <a-select-option value="SELF_PICKUP">自提</a-select-option>
            <a-select-option value="DELIVERY">配送</a-select-option>
            <a-select-option value="LOGISTICS">物流</a-select-option>
          </a-select>
          <input class="c-input" v-model="rows[0].deliveryRoute" placeholder="配送线路" style="min-width:80px;flex:0.3" />
        </template>
      </div>
    </div>

    <!-- 其他联系人 -->
    <div v-for="(row, idx) in otherRows" :key="row._uid" class="contact-block">
      <div class="contact-block-label">
        其他联系人{{ idx + 1 }}
        <template v-if="showMallAccount">
          <a-checkbox v-model:checked="row.openMallAccount" style="margin-left: 12px; font-size: 13px; font-weight: 400; color: #8c8c8c">
            开通商城帐号
          </a-checkbox>
        </template>
        <a-button size="small" type="link" @click="setDefault(row)" :style="{ padding: '0 0 0 8px', color: row.isDefault ? '#ff4d4f' : '#595959', fontWeight: row.isDefault ? 600 : 400 }">设为常用联系人</a-button>
      </div>
      <div class="contact-fields">
        <input class="c-input" v-model="row.contactName" placeholder="联系人" />
        <input class="c-input" v-model="row.contactPhone" placeholder="电话" />
        <input class="c-input" v-model="row.region" placeholder="所在地区" />
        <input class="c-input c-input-wide" v-model="row.detailAddress" placeholder="详情地址" />
        <a-button size="small" class="c-locate-btn">设置定位</a-button>
        <template v-if="showMallAccount">
          <a-select v-model:value="row.deliveryMethod" placeholder="配送方式" size="small" class="c-select" allow-clear>
            <a-select-option value="EXPRESS">快递</a-select-option>
            <a-select-option value="SELF_PICKUP">自提</a-select-option>
            <a-select-option value="DELIVERY">配送</a-select-option>
            <a-select-option value="LOGISTICS">物流</a-select-option>
          </a-select>
          <input class="c-input" v-model="row.deliveryRoute" placeholder="配送线路" style="min-width:80px;flex:0.3" />
        </template>
        <DeleteOutlined class="c-del-icon" @click="handleDelete(row)" />
      </div>
    </div>

    <!-- 添加按钮 -->
    <a-button class="add-contact-btn" @click="addContact">
      <template #icon><PlusOutlined /></template>
      其他联系人
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { PartnerContact } from '@/api/erp/partner'

export interface ContactRowData extends PartnerContact {
  _uid: number
  region?: string
  detailAddress?: string
  deliveryMethod?: string
  deliveryRoute?: string
  openMallAccount?: boolean
}

const props = defineProps<{
  contacts: ContactRowData[]
  showMallAccount?: boolean
}>()

const emit = defineEmits<{
  (e: 'update', contacts: ContactRowData[]): void
}>()

let uidCounter = Date.now()

const rows = ref<ContactRowData[]>(
  props.contacts.length > 0
    ? props.contacts.map(c => ({ ...c, _uid: ++uidCounter }))
    : [{ _uid: ++uidCounter, contactName: '', contactPhone: '', contactEmail: '', position: '', department: '', isDefault: 1, region: '', detailAddress: '', deliveryMethod: undefined, deliveryRoute: '', openMallAccount: false } as ContactRowData]
)

watch(() => props.contacts, (val) => {
  if (val.length > 0) {
    rows.value = val.map(c => ({ ...c, _uid: ++uidCounter }))
  } else if (rows.value.length === 0) {
    rows.value = [{ _uid: ++uidCounter, contactName: '', contactPhone: '', contactEmail: '', position: '', department: '', isDefault: 1, region: '', detailAddress: '', deliveryMethod: undefined, deliveryRoute: '', openMallAccount: false } as ContactRowData]
  }
}, { deep: true })

const otherRows = computed(() => rows.value.slice(1))

function addContact() {
  rows.value.push({
    _uid: ++uidCounter,
    contactName: '', contactPhone: '', contactEmail: '',
    position: '', department: '', isDefault: 0,
    region: '', detailAddress: '', deliveryMethod: undefined, deliveryRoute: '',
  } as ContactRowData)
  emitUpdate()
}

function setDefault(row: ContactRowData) {
  const idx = rows.value.indexOf(row)
  if (idx > 0) {
    const [item] = rows.value.splice(idx, 1)
    rows.value.unshift(item)
  }
  rows.value.forEach((r, i) => { r.isDefault = i === 0 ? 1 : 0 })
  emitUpdate()
}

function handleDelete(row: ContactRowData) {
  const idx = rows.value.indexOf(row)
  if (idx >= 0) rows.value.splice(idx, 1)
  emitUpdate()
}

function emitUpdate() {
  emit('update', rows.value.map(({ _uid, ...rest }) => rest as ContactRowData))
}

defineExpose({
  getContacts: () => rows.value.map(({ _uid, ...rest }) => rest as ContactRowData),
})
</script>

<style scoped>
.contact-section { }
.contact-header-row { margin-bottom: 12px; }
.contact-title { font-size: 14px; font-weight: 600; color: #262626; }

.contact-block { margin-bottom: 12px; }
.contact-block-label {
  font-size: 13px; color: #262626; margin-bottom: 6px; font-weight: 500;
}

.contact-fields {
  display: flex; gap: 10px; align-items: center; flex-wrap: wrap;
}
.c-input {
  height: 32px; padding: 4px 10px;
  border: 1px solid #d9d9d9; border-radius: 4px;
  font-size: 13px; color: #262626; outline: none;
  transition: border-color 0.2s; background: #fff;
  min-width: 100px;
}
.c-input:focus { border-color: #40a9ff; box-shadow: 0 0 0 2px rgba(24,144,255,0.1); }
.c-input::placeholder { color: #bfbfbf; }
.c-input-wide { min-width: 160px; flex: 1; }
.c-select { min-width: 120px; }
.c-locate-btn { flex-shrink: 0; }
.c-del-icon { font-size: 18px; color: #ff4d4f; cursor: pointer; flex-shrink: 0; margin-left: 4px; }
.c-del-icon:hover { color: #ff7875; }

.add-contact-btn {
  margin-top: 4px;
  background: #ff4d4f; color: #fff; border-color: #ff4d4f;
}
.add-contact-btn:hover { background: #ff7875; border-color: #ff7875; color: #fff; }
</style>
