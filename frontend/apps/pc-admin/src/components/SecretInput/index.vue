<template>
  <div class="secret-input">
    <!--
      未处于「重新输入」态且后端已有存量值 → 只显示掩码，绝不把明文回填进输入框。
      背景：平台设置 4 页的 GET 接口会把凭据明文放进响应（实体无 @JsonIgnore、无脱敏），
      历史实现在页面层把明文直接回填，等于把凭据摊在屏幕上（开发文档 §12 P0-④ 登记）。
      本组件把「回显」与「录入」拆成两个互斥状态：
        · 回显态：只渲染掩码，不可编辑；
        · 录入态：用户主动点「重新输入 / 设置」后才渲染可编辑的密码框。
      父页面提交时按「草稿非空且不是掩码串才提交该字段」的规则拼载荷，未改动则整键省略
      （后端 MyBatis-Plus updateById 忽略 null → 保留原值），因此掩码永远不会覆盖真实凭据。
    -->
    <template v-if="!editing && hasStored">
      <a-input
        class="secret-input__mask"
        :value="MASK"
        readonly
        :disabled="disabled"
      />
      <a-button
        type="link"
        size="small"
        :disabled="disabled"
        @click="editing = true"
      >
        重新输入
      </a-button>
    </template>
    <template v-else>
      <a-input-password
        v-model:value="draft"
        :placeholder="hasStored ? '留空表示不修改，保留后端原值' : '请输入'"
        :disabled="disabled"
      />
      <a-button
        v-if="hasStored"
        type="link"
        size="small"
        :disabled="disabled"
        @click="editing = false"
      >
        取消
      </a-button>
    </template>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'

/**
 * SecretInput —— 凭据（密码 / AccessKey / AccessSecret）的「掩码回显 + 主动重录」输入框
 *
 * 用法（父页面持有 hasStored + editing + draft 三份状态）：
 *   <SecretInput
 *     v-model:value="secret.password.draft"
 *     v-model:editing="secret.password.editing"
 *     :has-stored="secret.password.hasStored"
 *   />
 *
 * 提交规则见父页面 buildPayload() / resolveSecret()：**草稿非空且不等于 MASK 才提交该字段**，
 * 否则整键省略（禁止提交 MASK，也不要用空串覆盖后端真实值）。
 *
 * 注意「后端没有存量值」时本组件**直接渲染可编辑框**（此时 editing 仍为 false）——
 * 父页面若按 editing 判定是否提交，用户敲进去的凭据会被静默丢弃，必须按「值」判定。
 */
defineOptions({ name: 'SecretInput' })

/**
 * 掩码常量：与文档口径一致（六星号）。
 * 父页面各有一份同名常量用于提交前拦截（`SECRET_MASK`）；两处必须一致 ——
 * 之所以不导出，是因为本仓 `env.d.ts` 的 `*.vue` 声明只给默认导出，具名导入过不了 typecheck。
 */
const MASK = '******'

defineProps<{
  /** 后端是否已有存量值（决定回显态是否渲染掩码） */
  hasStored?: boolean
  /** 整体禁用（如配置读取失败时不允许改凭据） */
  disabled?: boolean
}>()

/** 录入态下的草稿值；回显态下父页面应为空串 */
const draft = defineModel<string>('value', { default: '' })
/** 是否处于录入态（用户主动点「重新输入」后才为 true） */
const editing = defineModel<boolean>('editing', { default: false })

// 进入编辑态时清空草稿并拦截粘进来的掩码串，避免掩码被当成真实凭据提交
watch(editing, (val) => {
  if (val) draft.value = ''
})
watch(draft, (val) => {
  if (val === MASK) draft.value = ''
})
</script>

<style scoped>
.secret-input {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
}

.secret-input :deep(.ant-input-affix-wrapper),
.secret-input :deep(.ant-input) {
  flex: 1;
  min-width: 0;
}

/* 掩码态：去掉输入框的「可编辑」观感，只作展示 */
.secret-input__mask {
  background: #fafafa;
  color: #8c8c8c;
  letter-spacing: 2px;
}
</style>
