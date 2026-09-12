<template>
  <div class="contact-section">
    <div class="contact-header-row">
      <span class="contact-title">联系人信息</span>
    </div>

    <!-- 常用联系人（首行） -->
    <div
      v-if="firstRow"
      class="contact-block"
    >
      <div class="contact-block-label">
        <span class="contact-label-text">常用联系人</span>
        <a-tag
          v-if="firstRow.isDefault"
          color="red"
          style="margin-left:6px;font-size:11px"
        >
          默认
        </a-tag>
        <template v-if="showMallAccount">
          <a-checkbox
            :checked="!!firstRow.openMallAccount"
            style="margin-left: 12px; font-size: 13px; font-weight: 400; color: #8c8c8c"
            @update:checked="(checked) => firstRow.openMallAccount = checked ? 1 : 0"
          >
            开通商城帐号
          </a-checkbox>
        </template>
      </div>
      <div class="contact-fields">
        <input
          v-model="firstRow.contactName"
          class="c-input"
          placeholder="联系人*"
        >
        <input
          v-model="firstRow.contactPhone"
          class="c-input"
          placeholder="手机*"
        >
        <input
          v-model="firstRow.contactEmail"
          class="c-input"
          placeholder="邮箱"
        >
        <input
          v-model="firstRow.position"
          class="c-input"
          placeholder="职位"
        >
        <input
          v-model="firstRow.department"
          class="c-input"
          placeholder="部门"
        >
      </div>
      <div
        class="contact-fields"
        style="margin-top:6px"
      >
        <RegionCascader
          v-model="firstRow._areaPath"
          class="c-region"
          placeholder="所在地区"
          @change="(path) => applyArea(firstRow, path)"
        />
        <input
          v-model="firstRow.detailAddress"
          class="c-input c-input-wide"
          placeholder="详情地址"
        >
        <a-button
          size="small"
          class="c-locate-btn"
        >
          设置定位
        </a-button>
        <template v-if="showMallAccount">
          <a-select
            v-model:value="firstRow.deliveryMethod"
            placeholder="配送方式"
            size="small"
            class="c-select"
            allow-clear
          >
            <a-select-option value="EXPRESS">
              快递
            </a-select-option>
            <a-select-option value="SELF_PICKUP">
              自提
            </a-select-option>
            <a-select-option value="DELIVERY">
              配送
            </a-select-option>
            <a-select-option value="LOGISTICS">
              物流
            </a-select-option>
          </a-select>
          <input
            v-model="firstRow.deliveryRoute"
            class="c-input"
            placeholder="配送线路"
            style="min-width:80px;flex:0.3"
          >
        </template>
      </div>
    </div>

    <!-- 其他联系人 -->
    <div
      v-for="(row, idx) in otherRows"
      :key="row._uid"
      class="contact-block"
    >
      <div class="contact-block-label">
        <span class="contact-label-text">其他联系人{{ idx + 1 }}</span>
        <template v-if="showMallAccount">
          <a-checkbox
            :checked="!!row.openMallAccount"
            style="margin-left: 12px; font-size: 13px; font-weight: 400; color: #8c8c8c"
            @update:checked="(checked) => row.openMallAccount = checked ? 1 : 0"
          >
            开通商城帐号
          </a-checkbox>
        </template>
        <a-button
          size="small"
          type="link"
          :style="{ padding: '0 0 0 8px', color: row.isDefault ? '#ff4d4f' : '#595959', fontWeight: row.isDefault ? 600 : 400 }"
          @click="setDefault(row)"
        >
          设为常用联系人
        </a-button>
      </div>
      <div class="contact-fields">
        <input
          v-model="row.contactName"
          class="c-input"
          placeholder="联系人"
        >
        <input
          v-model="row.contactPhone"
          class="c-input"
          placeholder="电话"
        >
        <input
          v-model="row.contactEmail"
          class="c-input"
          placeholder="邮箱"
        >
        <input
          v-model="row.position"
          class="c-input"
          placeholder="职位"
        >
        <input
          v-model="row.department"
          class="c-input"
          placeholder="部门"
        >
      </div>
      <div
        class="contact-fields"
        style="margin-top:6px"
      >
        <RegionCascader
          v-model="row._areaPath"
          class="c-region"
          placeholder="所在地区"
          @change="(path) => applyArea(row, path)"
        />
        <input
          v-model="row.detailAddress"
          class="c-input c-input-wide"
          placeholder="详情地址"
        >
        <a-button
          size="small"
          class="c-locate-btn"
        >
          设置定位
        </a-button>
        <template v-if="showMallAccount">
          <a-select
            v-model:value="row.deliveryMethod"
            placeholder="配送方式"
            size="small"
            class="c-select"
            allow-clear
          >
            <a-select-option value="EXPRESS">
              快递
            </a-select-option>
            <a-select-option value="SELF_PICKUP">
              自提
            </a-select-option>
            <a-select-option value="DELIVERY">
              配送
            </a-select-option>
            <a-select-option value="LOGISTICS">
              物流
            </a-select-option>
          </a-select>
          <input
            v-model="row.deliveryRoute"
            class="c-input"
            placeholder="配送线路"
            style="min-width:80px;flex:0.3"
          >
        </template>
        <DeleteOutlined
          class="c-del-icon"
          @click="handleDelete(row)"
        />
      </div>
    </div>

    <!-- 添加按钮 -->
    <a-button
      class="add-contact-btn"
      @click="addContact"
    >
      <template #icon>
        <PlusOutlined />
      </template>
      其他联系人
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { PartyContact } from '@/api/erp/partner'
import RegionCascader from '@/components/RegionCascader/RegionCascader.vue'

export interface ContactRowData extends PartyContact {
  _uid: number
  _areaPath?: string[]
  region?: string
  province?: string
  city?: string
  district?: string
  detailAddress?: string
  deliveryMethod?: string
  deliveryRoute?: string
  openMallAccount?: number
  // 扩展字段（兼容旧数据）
  isDefault?: number
  contactPhone?: string
  contactEmail?: string
}

/** 省市区路径 → 回写 region 文本 + 三级字段（行政区划组件统一出口） */
function applyArea(row: ContactRowData, path: string[]) {
  row.province = path[0] || ''
  row.city = path[1] || ''
  row.district = path[2] || ''
  row.region = path.filter(Boolean).join('/')
}

/** 从已有 province/city/district 还原级联回显路径 */
function areaPathOf(row: ContactRowData): string[] {
  if (row.province || row.city || row.district) {
    return [row.province, row.city, row.district].filter(Boolean) as string[]
  }
  // 兼容历史数据：region 为 '省/市/区' 形态时直接拆
  if (row.region && row.region.includes('/')) return row.region.split('/').filter(Boolean)
  return []
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
    ? props.contacts.map(c => ({
      ...c,
      _uid: ++uidCounter,
      contactName: c.contactName || c.contactName || '',
      phone: c.contactPhone || c.phone || '',
      mobile: c.contactPhone || c.mobile || '',
      email: c.contactEmail || c.email || '',
      isDefault: c.isPrimary || c.isDefault || 0,
      _areaPath: areaPathOf(c),
    }))
    : [{
        _uid: ++uidCounter,
        contactName: '',
        contactPhone: '',
        contactEmail: '',
        position: '',
        department: '',
        isDefault: 1,
        region: '',
        province: '',
        city: '',
        district: '',
        _areaPath: [],
        detailAddress: '',
        deliveryMethod: undefined,
        deliveryRoute: '',
        openMallAccount: 0,
        phone: '',
        mobile: '',
        email: '',
        partyId: undefined,
      } as ContactRowData]
)

watch(() => props.contacts, (val) => {
  if (val.length > 0) {
    rows.value = val.map(c => ({
      ...c,
      _uid: ++uidCounter,
      contactName: c.contactName || c.contactName || '',
      phone: c.contactPhone || c.phone || '',
      mobile: c.contactPhone || c.mobile || '',
      email: c.contactEmail || c.email || '',
      isDefault: c.isPrimary || c.isDefault || 0,
      _areaPath: areaPathOf(c),
    }))
  } else if (rows.value.length === 0) {
    rows.value = [{
      _uid: ++uidCounter,
      contactName: '',
      contactPhone: '',
      contactEmail: '',
      position: '',
      department: '',
      isDefault: 1,
      region: '',
      detailAddress: '',
      deliveryMethod: undefined,
      deliveryRoute: '',
      openMallAccount: 0,
      phone: '',
      mobile: '',
      email: '',
      partyId: undefined,
    } as ContactRowData]
  }
}, { deep: true })

const otherRows = computed(() => rows.value.slice(1))

// 首行联系人（常用联系人）
const firstRow = computed(() => rows.value.length > 0 ? rows.value[0] : null)

function addContact() {
  rows.value.push({
    _uid: ++uidCounter,
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    position: '',
    department: '',
    isDefault: 0,
    region: '',
    province: '',
    city: '',
    district: '',
    _areaPath: [],
    detailAddress: '',
    deliveryMethod: undefined,
    deliveryRoute: '',
    openMallAccount: 0,
    phone: '',
    mobile: '',
    email: '',
    partyId: undefined,
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
  emit('update', rows.value.map(({ _uid, _areaPath, ...rest }) => {
    // 确保将新字段转换为正确的数据类型
    return {
      ...rest,
      contactName: rest.contactName || rest.contactName || '',
      phone: rest.contactPhone || rest.phone || '',
      mobile: rest.contactPhone || rest.mobile || '',
      email: rest.contactEmail || rest.email || '',
      position: rest.position || '',
      department: rest.department || '',
      deliveryMethod: rest.deliveryMethod || undefined,
      deliveryRoute: rest.deliveryRoute || '',
      openMallAccount: rest.openMallAccount ? 1 : 0, // 转换布尔值为数字
      isPrimary: rest.isDefault || 0, // 将isDefault映射到isPrimary
    } as ContactRowData
  }))
}

defineExpose({
  getContacts: () => rows.value.map(({ _uid, _areaPath, ...rest }) => {
    return {
      ...rest,
      contactName: rest.contactName || rest.contactName || '',
      phone: rest.contactPhone || rest.phone || '',
      mobile: rest.contactPhone || rest.mobile || '',
      email: rest.contactEmail || rest.email || '',
      position: rest.position || '',
      department: rest.department || '',
      deliveryMethod: rest.deliveryMethod || undefined,
      deliveryRoute: rest.deliveryRoute || '',
      openMallAccount: typeof rest.openMallAccount === 'boolean' ? (rest.openMallAccount ? 1 : 0) : (rest.openMallAccount || 0),
      isPrimary: rest.isDefault || 0, // 将isDefault映射到isPrimary
    } as ContactRowData
  }),
})
</script>

<style scoped>
.contact-section { }
.contact-header-row { margin-bottom: 12px; }
.contact-title { font-size: 14px; font-weight: 600; color: #262626; }

.contact-block { margin-bottom: 12px; }
.contact-block-label {
  font-size: 13px; color: #262626; margin-bottom: 6px; font-weight: 500;
  display: flex; align-items: center;
}
.contact-label-text { font-weight: 600; }

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
.c-region { min-width: 170px; flex: 0.55; }
.c-locate-btn { flex-shrink: 0; }
.c-del-icon { font-size: 18px; color: #ff4d4f; cursor: pointer; flex-shrink: 0; margin-left: 4px; }
.c-del-icon:hover { color: #ff7875; }

.add-contact-btn {
  margin-top: 4px;
  background: #ff4d4f; color: #fff; border-color: #ff4d4f;
}
.add-contact-btn:hover { background: #ff7875; border-color: #ff7875; color: #fff; }
</style>
