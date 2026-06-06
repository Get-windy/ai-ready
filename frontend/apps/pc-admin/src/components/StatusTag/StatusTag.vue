<template>
  <a-tag :color="color">{{ text }}</a-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { StatusMap } from '@/utils/statusConfig'
import { getStatusText, getStatusColor } from '@/utils/statusConfig'

const props = defineProps<{
  status: string | number | undefined | null
  map: StatusMap
}>()

const color = computed(() => getStatusColor(props.status, props.map))
const text = computed(() => getStatusText(props.status, props.map))
</script>
