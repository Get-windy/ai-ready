<template>
  <!--
    ARTableColumn: ant-design-vue 表格列配置的兼容包装器
    将模板风格的列定义转换为 a-table 的 columns prop 格式
  -->
</template>

<script setup lang="ts">
/**
 * ARTableColumn 现已升级为基于 ant-design-vue a-table 的列配置。
 *
 * 请直接使用 ARTable 的 columns prop 来定义列，而不是使用此组件。
 *
 * 示例:
 * const columns = [
 *   { title: '名称', dataIndex: 'name', key: 'name' },
 *   { title: '操作', key: 'action', slots: { customRender: 'action' } }
 * ]
 *
 * <ARTable :columns="columns" :data-source="data">
 *   <template #action="{ record }">
 *     <a-button @click="edit(record)">编辑</a-button>
 *   </template>
 * </ARTable>
 */
</script>
