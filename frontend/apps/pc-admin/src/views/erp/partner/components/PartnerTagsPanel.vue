<template>
  <div>
    <div class="panel-toolbar">
      <a-space>
        <a-select v-model:value="selectedIds" mode="multiple" placeholder="选择标签" style="min-width: 300px" size="small" :max-tag-count="5">
          <a-select-option v-for="t in allTags" :key="t.id" :value="t.id">
            <a-tag :color="t.tagColor || undefined">{{ t.tagName }}</a-tag>
          </a-select-option>
        </a-select>
        <a-button size="small" type="primary" @click="handleSave">保存标签</a-button>
      </a-space>
    </div>
    <div class="current-tags">
      <a-tag v-for="t in selectedTags" :key="t.id" :color="t.tagColor || undefined" closable @close="removeTag(t.id)">
        {{ t.tagName }}
      </a-tag>
      <span v-if="selectedTags.length === 0" style="color:#999">暂无标签</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { partnerTagApi, type PartnerTag } from '@/api/erp/partner'

const props = defineProps<{ partnerId: number }>()
const allTags = ref<PartnerTag[]>([])
const selectedIds = ref<number[]>([])

const selectedTags = computed(() => allTags.value.filter(t => selectedIds.value.includes(t.id)))

async function load() {
  const [tags, tagIds] = await Promise.all([
    partnerTagApi.list(),
    partnerTagApi.getTagIds(props.partnerId)
  ])
  allTags.value = tags
  selectedIds.value = tagIds || []
}

async function handleSave() {
  try {
    await partnerTagApi.attachTags(props.partnerId, selectedIds.value)
    message.success('标签已保存')
  } catch { message.error('保存失败') }
}

function removeTag(id: number) {
  selectedIds.value = selectedIds.value.filter(v => v !== id)
}

onMounted(load)
</script>
<style scoped>
.panel-toolbar { margin-bottom: 8px; }
.current-tags { min-height: 32px; }
</style>
