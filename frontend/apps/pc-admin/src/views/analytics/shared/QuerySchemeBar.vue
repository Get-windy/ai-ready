<template>
  <!--
    查询方案条（分析模块共用）

    对标 ql361 分析域每页都有 `--查询方案--` 下拉 + 保存按钮，用于把当前查询条件
    存成具名方案并随时调用。本系统此前只有占位下拉（点了没反应），此组件把它做实：
    方案存本操作员的 localStorage（按 storageKey 隔离），不落库、不跨设备 —— 与
    对标「查询方案只对当前操作员有效」的行为一致。

    用法：
      <QuerySchemeBar storage-key="analytics-draft-query-scheme"
                      :snapshot="() => ({ ...query })"
                      @apply="(v) => Object.assign(query, v)" />
  -->
  <div class="scheme-bar">
    <a-select
      v-model:value="currentName"
      size="small"
      class="scheme-select"
      placeholder="--查询方案--"
      allow-clear
      :options="schemeOptions"
      @change="handleApply"
    />
    <a-button size="small" type="link" class="scheme-btn" @click="openSave">
      <SaveOutlined /> 保存
    </a-button>
    <a-popconfirm
      v-if="currentName"
      title="删除该查询方案？"
      ok-text="删除"
      cancel-text="取消"
      @confirm="handleRemove"
    >
      <a-button size="small" type="link" danger class="scheme-btn">
        <DeleteOutlined />
      </a-button>
    </a-popconfirm>

    <a-modal
      v-model:open="saveVisible"
      title="保存查询方案"
      :width="420"
      :confirm-loading="false"
      @ok="handleSave"
    >
      <a-input
        v-model:value="saveName"
        placeholder="方案名称（如：本月未发货订单）"
        :maxlength="30"
        @press-enter="handleSave"
      />
      <p class="scheme-tip">方案保存到本机浏览器，仅对当前操作员可见。</p>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, SaveOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  /** 方案存储键（每页一个，避免互相串） */
  storageKey: string
  /** 取当前查询条件快照（返回可 JSON 序列化的纯对象） */
  snapshot: () => Record<string, any>
}>()

const emit = defineEmits<{
  /** 调用某方案：把方案里的查询条件回填到页面 */
  apply: [value: Record<string, any>]
}>()

interface Scheme {
  name: string
  value: Record<string, any>
}

const schemes = ref<Scheme[]>([])
const currentName = ref<string | undefined>(undefined)
const saveVisible = ref(false)
const saveName = ref('')

const schemeOptions = computed(() => schemes.value.map(s => ({ label: s.name, value: s.name })))

function storageKey(): string {
  return `analytics-query-scheme:${props.storageKey}`
}

function load() {
  try {
    const raw = localStorage.getItem(storageKey())
    const parsed = raw ? JSON.parse(raw) : []
    schemes.value = Array.isArray(parsed) ? parsed.filter(s => s && typeof s.name === 'string') : []
  } catch {
    schemes.value = []
  }
}

function persist() {
  try {
    localStorage.setItem(storageKey(), JSON.stringify(schemes.value))
  } catch {
    message.warning('查询方案保存失败（浏览器存储不可用）')
  }
}

watch(() => props.storageKey, () => { currentName.value = undefined; load() }, { immediate: true })

function openSave() {
  saveName.value = currentName.value || ''
  saveVisible.value = true
}

function handleSave() {
  const name = saveName.value.trim()
  if (!name) {
    message.warning('请填写方案名称')
    return
  }
  const value = props.snapshot()
  const hit = schemes.value.find(s => s.name === name)
  if (hit) {
    // 同名视为覆盖保存（对标「保存」对同名方案即更新）
    Object.assign(hit, { value })
  } else {
    schemes.value.push({ name, value })
  }
  persist()
  currentName.value = name
  saveVisible.value = false
  message.success(`查询方案「${name}」已保存`)
}

function handleApply(name?: string) {
  if (!name) return
  const hit = schemes.value.find(s => s.name === name)
  if (!hit) return
  emit('apply', { ...hit.value })
}

function handleRemove() {
  if (!currentName.value) return
  schemes.value = schemes.value.filter(s => s.name !== currentName.value)
  persist()
  message.success('查询方案已删除')
  currentName.value = undefined
}
</script>

<style scoped>
.scheme-bar { display: inline-flex; align-items: center; gap: 2px; }
.scheme-select { width: 150px; }
.scheme-btn { padding: 0 6px; }
.scheme-tip { margin: 8px 0 0; color: #999; font-size: 12px; }
</style>
